package darkspawn.black.oneblock;

import com.mojang.authlib.GameProfile;
import darkspawn.black.inventory.ArmorLoadouts;
import darkspawn.black.inventory.InventoryExpansion;
import darkspawn.black.health.BossHearts;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class OneBlockGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "oneblock-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		var pos = helper.absolutePos(new BlockPos(1, 2, 2)); level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); ready(player);
		return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static void travel(GameTestHelper helper, ServerPlayer player) { ready(player); helper.assertTrue(OneBlock.travel(player), "Safe OneBlock travel must succeed: alive=" + player.isAlive() + ", mode=" + player.gameMode() + ", ground=" + player.onGround() + ", loading=" + player.isChangingDimension() + ", loaded=" + player.connection.hasClientLoaded() + ", hurt=" + OneBlock.state(player).hurtTicks + ", using=" + player.isUsingItem() + ", menu=" + (player.containerMenu == player.inventoryMenu)); ready(player); }

	@GameTest public void profilesPreserveFullInventoriesEquipmentAndSharedUpgrades(GameTestHelper helper) {
		var player = player(helper); var other = player(helper);
		try {
			InventoryExpansion.setTier(player.getInventory(), 3); ArmorLoadouts.state(player).unlocked = 3;
			for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			for (int slot = 43; slot < 70; slot++) player.getInventory().setItem(slot, new ItemStack(Items.IRON_INGOT, 64));
			var helmet = new ItemStack(Items.DIAMOND_HELMET); helmet.setDamageValue(17); helmet.set(DataComponents.CUSTOM_NAME, Component.literal("Outside heirloom"));
			player.setItemSlot(EquipmentSlot.HEAD, helmet); player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
			player.getEnderChestInventory().setItem(26, new ItemStack(Items.DIAMOND, 19));
			var stored = new ArrayList<ItemStack>(); for (int i = 0; i < 12; i++) stored.add(ItemStack.EMPTY);
			stored.set(4, new ItemStack(Items.NETHERITE_HELMET)); ArmorLoadouts.state(player).restoreItems(stored, 0);
			BossHearts.progress(player).restore(List.of("ancient_tree_spirit")); BossHearts.apply(player);
			player.experienceLevel = 12; player.getFoodData().setFoodLevel(14);
			var before = OneBlockItems.capture(player); var hearts = BossHearts.progress(player).savedBosses();
			travel(helper, player);
			helper.assertTrue(OneBlock.island(player.level()) && player.getInventory().isEmpty() && player.getEnderChestInventory().isEmpty(), "First arrival has no outside possessions");
			helper.assertTrue(ArmorLoadouts.state(player).stored(1, 0).isEmpty(), "Inactive loadouts are isolated too");
			helper.assertTrue(InventoryExpansion.tier(player.getInventory()) == 3 && ArmorLoadouts.state(player).unlocked == 3 && player.experienceLevel == 12 && player.getFoodData().getFoodLevel() == 14 && BossHearts.progress(player).savedBosses().equals(hearts), "Character upgrades, XP, and hunger remain shared");
			player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 7)); player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIRT, 9));
			travel(helper, other); helper.assertTrue(other.getInventory().isEmpty(), "A second arrival has an independent item profile");
			travel(helper, player);
			var restored = OneBlockItems.capture(player);
			for (int i = 0; i < before.inventory().size(); i++) helper.assertTrue(ItemStack.matches(before.inventory().get(i), restored.inventory().get(i)), "Outside slot " + i + " is restored exactly");
			helper.assertTrue(player.getEnderChestInventory().getItem(26).getCount() == 19 && ArmorLoadouts.state(player).stored(1, 0).is(Items.NETHERITE_HELMET), "Ender Chest and inactive armor return");
			travel(helper, player);
			helper.assertTrue(player.getInventory().getItem(0).is(Items.OAK_LOG) && player.getInventory().getItem(0).getCount() == 7 && player.getEnderChestInventory().getItem(0).getCount() == 9, "OneBlock possessions persist between visits without importing outside items");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}

	@GameTest public void unsafeRequestsAndExternalTeleportsCannotBypassProfiles(GameTestHelper helper) {
		var player = player(helper);
		var island = helper.getLevel().getServer().getLevel(OneBlock.LEVEL);
		try {
			helper.assertTrue(island != null, "Void dimension loads from the data pack");
			player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND)); OneBlock.request(player, 0);
			helper.assertTrue(OneBlock.state(player).pendingTicks == 0, "Cursor items block travel");
			player.inventoryMenu.setCarried(ItemStack.EMPTY); player.inventoryMenu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS)); OneBlock.request(player, 0);
			helper.assertTrue(OneBlock.state(player).pendingTicks == 0, "Crafting inputs block travel");
			player.inventoryMenu.getSlot(1).set(ItemStack.EMPTY); OneBlock.request(player, 0);
			helper.assertTrue(OneBlock.state(player).pendingTicks == 60, "Free menu travel reserves three seconds");
			player.hurtServer(player.level(), player.damageSources().magic(), 1);
			helper.assertTrue(OneBlock.state(player).pendingTicks == 0 && OneBlock.state(player).hurtTicks == 100, "Damage cancels travel and starts a combat delay");
			OneBlock.request(player, 0); helper.assertTrue(OneBlock.state(player).pendingTicks == 0, "Combat delay rejects new requests");
			var transition = new TeleportTransition(island, new Vec3(0, 81, 0), Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING);
			helper.assertTrue(player.teleport(transition) == null && !OneBlock.island(player.level()), "Commands and other portal routes cannot bypass item swapping");
			var item = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.DIAMOND)); player.level().addFreshEntity(item);
			helper.assertTrue(item.teleport(transition) == null && !item.isRemoved(), "Loose items cannot cross the boundary"); item.discard();
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void storedProfileAndReturnPointSurvivePlayerSave(GameTestHelper helper) {
		var player = player(helper); var copy = player(helper);
		try {
			player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 11)); var origin = player.position(); travel(helper, player);
			player.getInventory().setItem(0, new ItemStack(Items.DIRT, 5));
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess()); player.saveWithoutId(output);
			OneBlock.state(copy).read(TagValueInput.create(ProblemReporter.DISCARDING, copy.registryAccess(), output.buildResult()));
			helper.assertTrue(OneBlock.state(copy).inside && OneBlock.state(copy).returnPos.equals(origin) && OneBlock.state(copy).other.inventory().getFirst().getCount() == 11, "Inactive profile, marker, and outside return point persist together");
			var respawn = player.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING);
			helper.assertTrue(OneBlock.island(respawn.newLevel()) && respawn.position().y >= 80, "Death chooses the shared island even without a bed");
			ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, copy, false);
			helper.assertTrue(OneBlock.state(copy).other.inventory().getFirst().is(Items.EMERALD), "Death copying preserves only the inactive outside profile");
		} finally { remove(player); remove(copy); }
		helper.succeed();
	}

	@GameTest(maxTicks = 120) public void islandDeathUsesNativeDropRulesAndPreservesOutsidePossessions(GameTestHelper helper) {
		var player = player(helper);
		player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5)); travel(helper, player);
		// Dimension Fixture: Allow the new island chunk to become entity-visible before asserting drops.
		helper.runAfterDelay(20, () -> {
		ServerPlayer respawned = null;
		var island = helper.getLevel().getServer().getLevel(OneBlock.LEVEL); boolean keep = island.getGameRules().get(GameRules.KEEP_INVENTORY);
		try {
			island.getGameRules().set(GameRules.KEEP_INVENTORY, false, island.getServer());
			// Shared Island Fixture: Ignore drops left by earlier tests in this persistent dimension.
			var area = player.getBoundingBox().inflate(5);
			var existingDrops = island.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
			player.getInventory().setItem(0, new ItemStack(Items.APPLE, 7)); player.hurtServer(island, player.damageSources().magic(), 1000);
			helper.assertTrue(!player.isAlive() && player.getInventory().isEmpty(), "Normal death drops active OneBlock items");
			var drops = island.getEntitiesOfClass(ItemEntity.class, area).stream().filter(item -> !existingDrops.contains(item.getUUID())).toList();
			helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.APPLE)) && drops.stream().noneMatch(item -> item.getItem().is(Items.DIAMOND)), "Outside possessions never drop on the island: " + drops.stream().map(item -> item.getItem().toString()).toList());
			var connection = player.connection;
			connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			respawned = connection.player; ready(respawned);
			helper.assertTrue(OneBlock.island(respawned.level()) && respawned.getInventory().isEmpty(), "Native death respawn stays on the island without restoring dropped items");
			travel(helper, respawned);
			helper.assertTrue(respawned.getInventory().getItem(0).is(Items.DIAMOND) && respawned.getInventory().getItem(0).getCount() == 5, "Returning after death restores outside items exactly once");
			drops.forEach(ItemEntity::discard);
		} finally { island.getGameRules().set(GameRules.KEEP_INVENTORY, keep, island.getServer()); if (respawned != null) remove(respawned); else remove(player); }
		helper.succeed();
		});
	}

	@GameTest public void sourceHarvestingRegeneratesAndProgressWaitsForManualActivation(GameTestHelper helper) {
		var player = player(helper);
		try {
			travel(helper, player); var level = player.level(); var data = OneBlock.progress(level);
			data.phase = 0; data.breaks = 99; data.total = 0; data.resource = 0; data.lastBreakTick = Long.MIN_VALUE;
			level.setBlockAndUpdate(OneBlock.CORE_POS, OneBlock.CORE.defaultBlockState());
			helper.assertTrue(player.gameMode.destroyBlock(OneBlock.CORE_POS), "Native mining can break the central resource");
			helper.assertTrue(level.getBlockState(OneBlock.CORE_POS).is(OneBlock.CORE) && data.breaks == 100 && data.ready() && data.phase == 0, "Block regenerates and threshold unlocks without auto-advancing");
			player.gameMode.destroyBlock(OneBlock.CORE_POS);
			helper.assertTrue(data.breaks == 100, "Two breaks in one tick cannot double-award shared progress");
			OneBlock.request(player, 1); helper.assertTrue(data.phase == 1 && data.breaks == 0, "Explicit activation advances the whole island");
			level.setBlockAndUpdate(OneBlock.CORE_POS, Blocks.AIR.defaultBlockState()); OneBlock.ensureIsland(level);
			helper.assertTrue(level.getBlockState(OneBlock.CORE_POS).is(OneBlock.CORE) && data.breaks == 0, "Environmental recovery never awards progression");
			helper.assertTrue(level.getBlockState(OneBlock.CORE_POS).getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.IMMOVEABLE, "Source cannot be relocated by pistons");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest public void keepInventoryRetainsOnlyTheActiveIslandItems(GameTestHelper helper) {
		var player = player(helper); ServerPlayer respawned = null;
		var island = helper.getLevel().getServer().getLevel(OneBlock.LEVEL); boolean keep = island.getGameRules().get(GameRules.KEEP_INVENTORY);
		try {
			player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 3)); travel(helper, player);
			island.getGameRules().set(GameRules.KEEP_INVENTORY, true, island.getServer());
			player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 8)); player.hurtServer(island, player.damageSources().magic(), 1000);
			helper.assertTrue(!player.isAlive(), "Death occurs with keepInventory enabled");
			var connection = player.connection;
			connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			respawned = connection.player; ready(respawned);
			helper.assertTrue(OneBlock.island(respawned.level()) && respawned.getInventory().getItem(0).is(Items.OAK_LOG) && respawned.getInventory().getItem(0).getCount() == 8, "Native respawn keeps the active OneBlock inventory");
			travel(helper, respawned);
			helper.assertTrue(respawned.getInventory().getItem(0).is(Items.EMERALD) && respawned.getInventory().getItem(0).getCount() == 3, "KeepInventory does not merge the two profiles");
		} finally { island.getGameRules().set(GameRules.KEEP_INVENTORY, keep, island.getServer()); if (respawned != null) remove(respawned); else remove(player); }
		helper.succeed();
	}

	@GameTest(maxTicks = 120) public void milestoneSuppliesAndCreaturesUseFreeIslandSpace(GameTestHelper helper) {
		var player = player(helper); travel(helper, player);
		helper.runAfterDelay(40, () -> {
			var level = player.level(); var data = OneBlock.progress(level);
			try {
				for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (x != 0 || z != 0) level.setBlockAndUpdate(OneBlock.CORE_POS.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
				data.phase = 0; data.total = 24; data.lastBreakTick = Long.MIN_VALUE;
				player.gameMode.destroyBlock(OneBlock.CORE_POS);
				boolean supply = false;
				for (var pos : BlockPos.betweenClosed(OneBlock.CORE_POS.offset(-2, 1, -2), OneBlock.CORE_POS.offset(2, 1, 2))) {
					if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
						supply |= chest.getItem(0).is(Items.OAK_SAPLING) && chest.getItem(3).is(Items.WATER_BUCKET);
					}
				}
				helper.assertTrue(supply, "Supply milestones provide renewable essentials in a real shared chest");
				data.total = 49; data.lastBreakTick = Long.MIN_VALUE; player.gameMode.destroyBlock(OneBlock.CORE_POS);
				helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, new net.minecraft.world.phys.AABB(OneBlock.CORE_POS).inflate(8)).isEmpty(), "Early creature milestones spawn a vanilla farm animal on safe floor");
			} finally { remove(player); }
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 120) public void representedOreRequiresCorrectToolsForDropsAndExperience(GameTestHelper helper) {
		var player = player(helper); travel(helper, player);
		helper.runAfterDelay(30, () -> {
		try {
			var level = player.level(); var data = OneBlock.progress(level);
			var area = new net.minecraft.world.phys.AABB(OneBlock.CORE_POS).inflate(4);
			level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
			level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).forEach(net.minecraft.world.entity.ExperienceOrb::discard);
			data.lastBreakTick = Long.MIN_VALUE; data.total = 1;
			level.setBlockAndUpdate(OneBlock.CORE_POS, OneBlock.CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE, 14));
			player.gameMode.destroyBlock(OneBlock.CORE_POS);
			helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty() && level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).isEmpty(), "Hand-mined diamond ore awards neither loot nor experience");
			data.lastBreakTick = Long.MIN_VALUE;
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
			level.setBlockAndUpdate(OneBlock.CORE_POS, OneBlock.CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE, 14));
			player.gameMode.destroyBlock(OneBlock.CORE_POS);
			helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).stream().anyMatch(item -> item.getItem().is(Items.DIAMOND)), "Correct pickaxe receives native diamond loot: " + level.getEntitiesOfClass(ItemEntity.class, area).stream().map(item -> item.getItem().toString()).toList());
			helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).isEmpty(), "Correct mining awards native ore experience");
			level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
			level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).forEach(net.minecraft.world.entity.ExperienceOrb::discard);
		} finally { remove(player); }
		helper.succeed();
		});
	}
}
