package darkspawn.black.forsaken;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;

public final class ForsakenToolsGameTest {
	private static ServerPlayer player(ServerLevel level, BlockPos pos) {
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tools-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, Vec3.atBottomCenterOf(pos), Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.hasChangedDimension();
		return player;
	}
	private static void chunks(ServerLevel level, BlockPos pos, boolean forced) {
        for (int x=-1; x<=1; x++) for (int z=-1; z<=1; z++) level.setChunkForced((pos.getX() >> 4)+x, (pos.getZ() >> 4)+z, forced);
    }
    private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static Infected infected(ServerLevel level, BlockPos pos) {
		var mob = ForsakenContent.MOBS.get(Infected.Kind.WANDERER).create(level, EntitySpawnReason.COMMAND);
		mob.setPos(Vec3.atBottomCenterOf(pos)); mob.setNoAi(true); mob.setNoGravity(true); level.addFreshEntity(mob); return mob;
	}
	@GameTest public void pulseRequiresChargeAndOnlyHitsVisibleNearbyInfected(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL); var pos = new BlockPos(24576, 110, 40); chunks(level, pos, true);
		for (var at : BlockPos.betweenClosed(pos.offset(-6, -1, -6), pos.offset(6, 4, 6))) level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
		var player = player(level, pos); var blade = new ItemStack(ForsakenContent.BLADE); player.setItemInHand(InteractionHand.MAIN_HAND, blade);
		var near = infected(level, pos.east(3)); var far = infected(level, pos.south(5)); var hidden = infected(level, pos.west(3));
		var pet = darkspawn.black.mount.Mounts.LEOPARD.create(level, EntitySpawnReason.COMMAND);
		pet.setPos(Vec3.atBottomCenterOf(pos.north(2))); pet.setNoAi(true); level.addFreshEntity(pet); float petHealth = pet.getHealth();
		for (var at : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(-1, 3, 1))) level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
		helper.runAfterDelay(5, () -> {
		try {
			ForsakenContent.BLADE.use(level, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(!ForsakenContent.BLADE.releaseUsing(blade, level, player, 72000 - 29) && blade.getDamageValue() == 0 && !player.getCooldowns().isOnCooldown(blade), "Early release is free and does not attack");
			helper.assertTrue(ForsakenContent.BLADE.releaseUsing(blade, level, player, 72000 - 30), "Full charge releases a pulse");
			helper.assertTrue(near.getHealth() == 14 && far.getHealth() == 24 && hidden.getHealth() == 24 && pet.getHealth() == petHealth, "Only visible infected within four blocks take exactly ten damage: " + near.getHealth() + "/" + far.getHealth() + "/" + hidden.getHealth() + "/" + pet.getHealth());
			helper.assertTrue(blade.getDamageValue() == 8 && player.getCooldowns().isOnCooldown(blade), "Pulse spends durability and starts cooldown");
			helper.assertTrue(!ForsakenContent.BLADE.releaseUsing(blade, level, player, 71970), "Cooldown prevents repeated release");
			helper.assertTrue(level.getBlockState(pos.west()).is(Blocks.STONE), "Pulse preserves cover blocks");
			far.hurtServer(level, player.damageSources().playerAttack(player), 8);
			helper.assertTrue(far.getHealth() == 14, "Blade melee adds 25 percent damage against infected");
		} finally { near.discard(); far.discard(); hidden.discard(); pet.discard(); remove(player); chunks(level, pos, false); }
		helper.succeed();
		});
	}
	@GameTest public void scannerLeavesCacheLootUnresolvedAndSharesModeCooldown(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL); var pos = new BlockPos(28672, 110, 40); chunks(level, pos, true);
		var player = player(level, pos); var scanner = new ItemStack(ForsakenContent.SCANNER); player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
		helper.runAfterDelay(5, () -> {
		// Fixture Isolation: Failed earlier runs may have saved resource drops in this remote test area.
		level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(32)).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
		level.setBlockAndUpdate(pos.east(3), Blocks.CHEST.defaultBlockState());
		var cache = (net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)level.getBlockEntity(pos.east(3));
		var table = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, darkspawn.black.Darkspawn.id("chests/forsaken_research")); cache.setLootTable(table);
		try {
			var tag = new net.minecraft.nbt.CompoundTag(); tag.putString("Preserve", "yes"); scanner.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
			NestScannerItem.cycle(scanner);
			helper.assertTrue(NestScannerItem.mode(scanner.copy()) == 1 && scanner.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("Preserve", "").equals("yes"), "Mode persists without erasing unrelated data");
			var signal = NestScannerItem.resources(level, pos);
			helper.assertTrue(signal != null && signal.kind().equals("cache") && signal.pos().equals(cache.getBlockPos()) && cache.getLootTable().equals(table), "Scan finds cache without generating its loot: " + signal + "/" + cache.getLootTable());
			helper.assertTrue(NestScannerItem.resources(level, pos.above(33)) == null, "Resources obey three-dimensional range");
			int chunks = level.getChunkSource().getLoadedChunksCount(); NestScannerItem.resources(level, new BlockPos(1000000, 100, 1000000));
			helper.assertTrue(chunks == level.getChunkSource().getLoadedChunksCount(), "Resource scan never loads chunks");
			ForsakenContent.SCANNER.use(level, player, InteractionHand.MAIN_HAND); NestScannerItem.cycle(scanner);
			helper.assertTrue(player.getCooldowns().isOnCooldown(scanner) && ForsakenContent.SCANNER.use(level, player, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.FAIL, "Switching modes cannot bypass scan cooldown");
			cache.setLootTable(null);
			helper.assertTrue(NestScannerItem.resources(level, pos) == null, "Resolved containers no longer report unopened-cache signals");
			var drop = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 2, pos.getY(), pos.getZ(), new ItemStack(ForsakenContent.CORE)); level.addFreshEntity(drop);
			helper.assertTrue(NestScannerItem.resources(level, pos).kind().equals("resource"), "Dropped exclusive materials produce resource signals"); drop.discard();
		} finally { level.setBlockAndUpdate(cache.getBlockPos(), Blocks.AIR.defaultBlockState()); remove(player); chunks(level, pos, false); }
		helper.succeed();
		});
	}
	@GameTest public void beaconFuelPersistsExpiresAndOverlapsWithoutStacking(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL); var pos = new BlockPos(32768, 110, 40);
		level.setBlockAndUpdate(pos, ForsakenContent.BEACON.defaultBlockState()); level.setBlockAndUpdate(pos.east(2), ForsakenContent.BEACON.defaultBlockState());
		var first = (ContainmentBeaconBlockEntity)level.getBlockEntity(pos); var second = (ContainmentBeaconBlockEntity)level.getBlockEntity(pos.east(2));
		var player = player(level, pos.north(2)); var fuel = new ItemStack(ForsakenContent.CORE, 3);
		try {
			helper.assertTrue(first.fuel(player, fuel) && fuel.getCount() == 2 && !first.fuel(player, fuel), "One core fuels five minutes; active top-ups are refused");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess()); first.saveAdditional(output); var saved = output.buildResult();
			helper.assertTrue(saved.getLongOr("PoweredUntil", 0) == level.getGameTime() + 6000, "Saved fuel uses an absolute five-minute deadline");
			first.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(first.active() && ContainmentBeaconBlockEntity.suppresses(level, pos.above(16)) && !ContainmentBeaconBlockEntity.suppresses(level, pos.above(17)), "Reload retains exact spherical range");
			helper.assertTrue(second.fuel(player, fuel), "Second beacon may overlap");
			saved.putLong("PoweredUntil", level.getGameTime()); first.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(!first.active() && ContainmentBeaconBlockEntity.suppresses(level, pos), "Expired first beacon does not extend its charge; overlapping second still protects");
			level.destroyBlock(pos.east(2), true);
			helper.assertTrue(!ContainmentBeaconBlockEntity.suppresses(level, pos), "Breaking final active beacon immediately ends protection");
			var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4));
			helper.assertTrue(drops.stream().filter(item -> item.getItem().is(ForsakenContent.BEACON_ITEM)).mapToInt(item -> item.getItem().getCount()).sum() == 1, "Breaking drops one reusable device");
			helper.assertTrue(drops.stream().filter(item -> item.getItem().is(ForsakenContent.BEACON_ITEM)).noneMatch(item -> item.getItem().has(DataComponents.BLOCK_ENTITY_DATA)), "Device drops cannot carry the remaining fuel"); drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
		} finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos.east(2), Blocks.AIR.defaultBlockState()); remove(player); }
		helper.succeed();
	}
	@GameTest public void pulseSmithingPreservesComponents(GameTestHelper helper) {
		var level = helper.getLevel(); var base = new ItemStack(Items.IRON_SWORD); base.setDamageValue(12);
		base.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Salvaged sword"));
		var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(ForsakenContent.MODULE), base, new ItemStack(ForsakenContent.CORE));
		var result = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level).orElseThrow().value().assemble(input);
		helper.assertTrue(result.is(ForsakenContent.BLADE) && result.getDamageValue() == 12 && result.getHoverName().getString().equals("Salvaged sword"), "Pulse upgrade retains damage and custom components");
		var crafting = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
			new ItemStack(ForsakenContent.ALLOY), new ItemStack(ForsakenContent.MODULE), new ItemStack(ForsakenContent.ALLOY),
			new ItemStack(Items.REDSTONE), new ItemStack(ForsakenContent.DATA), new ItemStack(Items.REDSTONE),
			new ItemStack(ForsakenContent.ALLOY), new ItemStack(ForsakenContent.MODULE), new ItemStack(ForsakenContent.ALLOY)));
		helper.assertTrue(level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, crafting, level).orElseThrow().value().assemble(crafting).is(ForsakenContent.BEACON_ITEM), "Beacon recipe resolves through native crafting"); helper.succeed();
	}
}
