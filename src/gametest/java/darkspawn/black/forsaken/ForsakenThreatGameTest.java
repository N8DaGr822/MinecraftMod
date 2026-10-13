package darkspawn.black.forsaken;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class ForsakenThreatGameTest {
	@GameTest public void spawnCapsAndMobSpawningRuleAreShared(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL);
		var pos = new BlockPos(16384, 80, 40);
		for (var at : BlockPos.betweenClosed(pos.offset(-4, -1, -24), pos.offset(4, 4, 8)))
			level.setBlockAndUpdate(at, at.getY() == 79 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "threat-test"), false);
		var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, net.minecraft.world.phys.Vec3.atBottomCenterOf(pos.north(20)), net.minecraft.world.phys.Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
		var rules = level.getGameRules(); var rule = net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS; boolean before = rules.get(rule);
		var mobs = new java.util.ArrayList<Infected>();
		try {
			rules.set(rule, false, level.getServer());
			helper.assertTrue(!ForsakenThreats.spawn(level, pos, true), "Nest spawning respects disabled mob spawning");
			rules.set(rule, true, level.getServer());
			level.setBlockAndUpdate(pos.east(3), ForsakenContent.BEACON.defaultBlockState());
			var beacon = (ContainmentBeaconBlockEntity)level.getBlockEntity(pos.east(3));
			var fuelData = new net.minecraft.nbt.CompoundTag(); fuelData.putLong("PoweredUntil", level.getGameTime() + 6000);
			beacon.loadAdditional(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), fuelData));
			helper.assertTrue(!ForsakenThreats.spawn(level, pos, true) && !ForsakenThreats.spawn(level, pos, false), "Fueled beacons suppress both nest and district spawn paths");
			level.setBlockAndUpdate(pos.east(3), Blocks.AIR.defaultBlockState());
			for (int i = 0; i < ForsakenThreats.CITY_CAP; i++) {
				var mob = ForsakenContent.MOBS.get(Infected.Kind.WANDERER).create(level, EntitySpawnReason.COMMAND);
				mob.setPos(pos.getX() + 15, 80, pos.getZ()); mob.setNoAi(true); level.addFreshEntity(mob); mobs.add(mob);
			}
			helper.assertTrue(!ForsakenThreats.spawn(level, pos, true), "Twelve living infected block another city spawn");
			mobs.forEach(Infected::discard);
			helper.assertTrue(ForsakenThreats.spawn(level, pos, true), "Eligible ground can actually spawn after the local cap is cleared");
			for (int i = 0; i < ForsakenThreats.ATTEMPT_CAP; i++) ForsakenThreats.spawn(level, pos, true);
			helper.assertTrue(!ForsakenThreats.spawn(level, pos.south(5), true), "Failed attempts consume the shared eight-attempt second budget");
		} finally {
			level.setBlockAndUpdate(pos.east(3), Blocks.AIR.defaultBlockState());
			level.getEntitiesOfClass(Infected.class, new AABB(pos).inflate(48)).forEach(Infected::discard);
			rules.set(rule, before, level.getServer()); level.getServer().getPlayerList().remove(player); player.discard();
		}
		helper.succeed();
	}
	@GameTest public void quarantineSmithingPreservesEquipmentComponents(GameTestHelper helper) {
		var level = helper.getLevel();
		for (var entry : ForsakenContent.ARMOR.entrySet()) {
			var base = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_" + entry.getKey().getName())));
			base.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Expedition heirloom"));
			base.setDamageValue(12);
			var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(ForsakenContent.MODULE), base, new ItemStack(ForsakenContent.BIOFIBER));
			var recipe = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level).orElseThrow().value();
			var output = recipe.assemble(input);
			helper.assertTrue(output.is(entry.getValue()) && output.getDamageValue() == 12 && output.getHoverName().getString().equals("Expedition heirloom"), "Smithing preserves damage and custom components for every quarantine piece");
		}
		helper.succeed();
	}
	@GameTest public void sharedPressureCapsDecaysAndSurvivesSerialization(GameTestHelper helper) {
		var data = ForsakenPressure.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("{}")).getOrThrow();
		var pos = new BlockPos(-512, 80, -480);
		for (int i = 0; i < 30; i++) data.disturb(pos, i * 20, 15);
		var zone = data.at(pos, 580);
		helper.assertTrue(zone.pressure() == 100, "Local pressure never exceeds 100");
		data.disturb(pos.east(10), 580, 15);
		helper.assertTrue(data.at(pos, 580).equals(zone), "Nearby players share the same noise throttle and surge");
		helper.assertTrue(zone.surgeEnd() == 680 && zone.nextSurge() == 2480, "Surge lasts 30 seconds and cannot restart for two minutes");
		var saved = ForsakenPressure.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
		var loaded = ForsakenPressure.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		helper.assertTrue(loaded.at(pos, 580).equals(zone), "Restart preserves pressure and cooldown instead of resetting them");
		helper.assertTrue(loaded.at(pos, 10580).pressure() == 0, "Unloaded city decays lazily to zero on world time");
		loaded.suppress(pos, 600);
		helper.assertTrue(loaded.at(pos, 600).pressure() == 75 && loaded.at(pos, 600).suppressed() == 6600 && loaded.at(pos, 600).surgeEnd() == 0, "Nest destruction lowers pressure and suppresses district spawns for five minutes");
		helper.assertTrue(ForsakenThreats.inCity(pos) && !ForsakenThreats.inCity(Forsaken.LANDING), "Negative city coordinates work and landing is excluded");
		helper.succeed();
	}
	@GameTest public void contaminationProtectionHoneyMilkAndOutsideIsolation(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL);
		var target = ForsakenContent.MOBS.get(Infected.Kind.WANDERER).create(level, EntitySpawnReason.COMMAND);
		ForsakenContent.contaminate(target);
		helper.assertTrue(target.getEffect(ForsakenContent.CONTAMINATION).getDuration() == 1200, "Exposure is temporary and non-stacking");
		new ItemStack(Items.HONEY_BOTTLE).finishUsingItem(level, target);
		helper.assertTrue(!target.hasEffect(ForsakenContent.CONTAMINATION), "Completed honey consumption cures contamination");
		ForsakenContent.contaminate(target);
		new ItemStack(Items.MILK_BUCKET).finishUsingItem(level, target);
		helper.assertTrue(!target.hasEffect(ForsakenContent.CONTAMINATION), "Native milk clears contamination");
		int count = 0;
		for (var entry : ForsakenContent.ARMOR.entrySet()) {
			target.setItemSlot(entry.getKey().getSlot(), new ItemStack(entry.getValue())); count++;
			ForsakenContent.contaminate(target);
			helper.assertTrue(count == 4 ? !target.hasEffect(ForsakenContent.CONTAMINATION) : target.getEffect(ForsakenContent.CONTAMINATION).getDuration() == 1200 - count * 300, "Each piece shortens exposure; full set prevents it");
			target.removeEffect(ForsakenContent.CONTAMINATION);
		}
		var outside = ForsakenContent.MOBS.get(Infected.Kind.WANDERER).create(helper.getLevel(), EntitySpawnReason.COMMAND);
		ForsakenContent.contaminate(outside);
		helper.assertTrue(!outside.hasEffect(ForsakenContent.CONTAMINATION), "Forsaken exposure cannot spread outside the dimension");
		helper.succeed();
	}
	@GameTest public void nestDestructionDropsOnceSuppressesAndScannerStaysBounded(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL);
		var pos = new BlockPos(8192, 80, 75); level.getChunk(pos);
		level.setBlockAndUpdate(pos, ForsakenContent.NEST.defaultBlockState());
		try {
			helper.assertTrue(pos.equals(NestScannerItem.nearest(level, pos.west(30))), "Scanner finds a nearby loaded nest");
			helper.assertTrue(NestScannerItem.nearest(level, pos.above(33)) == null, "Scanner enforces its three-dimensional 32-block range");
			int before = level.getChunkSource().getLoadedChunksCount();
			NestScannerItem.nearest(level, new BlockPos(1000000, 80, 1000000));
			helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == before, "Scanner never loads distant chunks");
			var box = new AABB(pos).inflate(2);
			level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
			level.destroyBlock(pos, true); level.destroyBlock(pos, true);
			int cores = level.getEntitiesOfClass(ItemEntity.class, box).stream().filter(item -> item.getItem().is(ForsakenContent.CORE)).mapToInt(item -> item.getItem().getCount()).sum();
			helper.assertTrue(cores == 1, "Repeated removal cannot duplicate the nest core");
			helper.assertTrue(ForsakenPressure.get(level).at(pos, level.getGameTime()).suppressed() == level.getGameTime() + 6000, "Removing a nest applies the local suppression window");
			helper.assertTrue(NestScannerItem.nearest(level, pos) == null, "Destroyed nests disappear from scanner immediately");
			level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
		} finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
		helper.succeed();
	}
	@GameTest public void cityTemplateContainsFourPermanentNestsAndUsefulRewards(GameTestHelper helper) {
		var template = helper.getLevel().getStructureTemplateManager().getOrCreate(darkspawn.black.Darkspawn.id("forsaken/city_slice"));
		var nests = template.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), ForsakenContent.NEST);
		helper.assertTrue(nests.size() == 4 && nests.stream().allMatch(nest -> nest.pos().getZ() >= 42), "Four nests are authored beyond quarantine line");
		helper.assertTrue(ForsakenContent.MOBS.size() == 4 && ForsakenContent.ARMOR.size() == 4, "All four enemy roles and armor pieces are registered");
		for (var kind : Infected.Kind.values()) {
			var mob = ForsakenContent.MOBS.get(kind).create(helper.getLevel(), EntitySpawnReason.COMMAND);
			helper.assertTrue(mob.kind() == kind && mob.getMaxHealth() == kind.health, "Each entity retains its own role and attributes");
		}
		helper.assertTrue(!ForsakenThreats.spawn(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), true), "Managed infected spawning is Forsaken-only");
		helper.succeed();
	}
}
