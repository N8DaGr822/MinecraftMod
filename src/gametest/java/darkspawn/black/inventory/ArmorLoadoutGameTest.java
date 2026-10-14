package darkspawn.black.inventory;

import com.mojang.authlib.GameProfile;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossEmpowerment;
import darkspawn.black.boss.EquipmentPowers;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class ArmorLoadoutGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "loadout-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
		player.teleportTo(pos.x, pos.y, pos.z);
		// Mock Connection: Acknowledge loading so vanilla permits actual damage in interruption tests.
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.setOnGround(true);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	@GameTest public void bonfireUpgradesChargeInOrderAndStopAtFinalTier(GameTestHelper helper) {
		var player = player(helper); var level=helper.getLevel();
		var pos=helper.absolutePos(new BlockPos(3,3,3));
		var core=darkspawn.black.bonfire.Bonfires.CORE;
		var range=darkspawn.black.bonfire.BonfireBlock.RANGE;
		var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
		try {
			level.setBlockAndUpdate(pos,core.defaultBlockState()); player.setShiftKeyDown(true);
			level.getBlockState(pos).useWithoutItem(level,player,hit);
			helper.assertTrue(level.getBlockState(pos).getValue(range)==1,"Missing payment leaves starter tier intact");
			var materials=new net.minecraft.world.item.Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND};
			int[] costs={8,4,2};
			for(int i=0;i<3;i++) {
				player.getInventory().setItem(9,new ItemStack(materials[i],costs[i]+1));
				level.getBlockState(pos).useWithoutItem(level,player,hit);
				helper.assertTrue(level.getBlockState(pos).getValue(range)==i+2 && player.getInventory().getItem(9).getCount()==1,"Upgrade consumes exactly its cost");
			}
			level.getBlockState(pos).useWithoutItem(level,player,hit);
			helper.assertTrue(level.getBlockState(pos).getValue(range)==4 && player.getInventory().getItem(9).getCount()==1,"Final tier neither wraps nor charges");
		} finally { level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); remove(player); }
		helper.succeed();
	}
	@GameTest
	public void trashRequiresCurrentRevisionAndReturnsUnconfirmedItems(GameTestHelper helper) {
		var player = player(helper);
		try {
			var menu = player.inventoryMenu;
			var slot = menu.getSlot(InventoryExpansion.TRASH_MENU_SLOT);
			var named = new ItemStack(Items.DIAMOND, 3);
			named.set(DataComponents.CUSTOM_NAME, Component.literal("Recover me"));
			slot.set(named.copy()); menu.broadcastChanges();
			InventoryExpansion.emptyTrash(player, menu.getStateId() - 1);
			helper.assertTrue(ItemStack.matches(slot.getItem(), named), "Stale deletion cannot destroy the staged stack");
			menu.setCarried(new ItemStack(Items.APPLE));
			InventoryExpansion.emptyTrash(player, menu.getStateId());
			helper.assertTrue(slot.hasItem(), "Carried items block deletion"); menu.setCarried(ItemStack.EMPTY);
			menu.removed(player);
			helper.assertTrue(!slot.hasItem() && player.getInventory().countItem(Items.DIAMOND) == 3, "Closing returns staged items once");
			helper.assertTrue(player.getInventory().getItem(0).getHoverName().getString().equals("Recover me"), "Return preserves item components");
			slot.set(new ItemStack(Items.COBBLESTONE, 64)); menu.broadcastChanges();
			InventoryExpansion.emptyTrash(player, menu.getStateId());
			helper.assertTrue(!slot.hasItem() && player.getInventory().countItem(Items.DIAMOND) == 3, "Explicit deletion touches only the trash slot");
			player.getInventory().setItem(9, new ItemStack(Items.DIRT, 64));
			menu.quickMoveStack(player, 9);
			helper.assertTrue(!slot.hasItem(), "Shift-click never automatically trashes items");
		} finally { remove(player); }
		helper.succeed();
	}
	private static ArmorLoadoutPayload request(ServerPlayer player, int target) {
		return new ArmorLoadoutPayload(target, ArmorLoadouts.state(player).revision, player.inventoryMenu.getStateId());
	}
	private static void swap(ServerPlayer player, int target) {
		ArmorLoadouts.request(player, request(player, target));
		for (int i = 0; i < ArmorLoadouts.SWITCH_TICKS; i++) ArmorLoadouts.tick(player);
	}
	private static void upgrade(ServerPlayer player, String material) {
		var item = BuiltInRegistries.ITEM.getValue(Darkspawn.id(material + "_armor_loadout_upgrade"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
		item.use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	@GameTest
	public void upgradesRequireOrderAndConsumeOnlyOnSuccess(GameTestHelper helper) {
		var player = player(helper);
		try {
			upgrade(player, "diamond");
			helper.assertTrue(ArmorLoadouts.state(player).unlocked == 1 && !player.getMainHandItem().isEmpty(), "Third set requires second, without consuming upgrade");
			upgrade(player, "iron");
			helper.assertTrue(ArmorLoadouts.state(player).unlocked == 2 && player.getMainHandItem().isEmpty(), "Iron consumes one upgrade and unlocks second set");
			upgrade(player, "iron");
			helper.assertTrue(!player.getMainHandItem().isEmpty() && ArmorLoadouts.state(player).unlocked == 2, "Duplicate upgrade is preserved");
			upgrade(player, "diamond");
			helper.assertTrue(ArmorLoadouts.state(player).unlocked == 3 && player.getMainHandItem().isEmpty(), "Diamond unlocks third set");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest
	public void swapsConserveComponentsAndExcludeInactivePowersWithFullInventory(GameTestHelper helper) {
		var player = player(helper);
		var other = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 3;
			InventoryExpansion.setTier(player.getInventory(), 3);
			for (int slot : InventoryExpansion.sortableSlots(player.getInventory())) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
			var helmet = new ItemStack(Items.DIAMOND_HELMET);
			helmet.set(DataComponents.CUSTOM_NAME, Component.literal("Mountain Helm"));
			helmet.set(BossEmpowerment.POWER, "armor_highland");
			helmet.setDamageValue(17);
			helmet.enchant(helper.getLevel().registryAccess().getOrThrow(Enchantments.PROTECTION), 2);
			var expected = helmet.copy();
			player.setItemSlot(EquipmentSlot.HEAD, helmet);
			player.inventoryMenu.broadcastFullState();
			var stale = request(player, 1);
			swap(player, 1);
			helper.assertTrue(ArmorLoadouts.state(player).active == 1 && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Empty second set replaces worn armor");
			helper.assertTrue(ItemStack.matches(expected, ArmorLoadouts.state(player).stored(0, 0)), "Stored armor preserves all components");
			helper.assertTrue(!EquipmentPowers.wears(player, "armor_highland"), "Inactive armor cannot grant a boss power");
			ArmorLoadouts.request(player, stale);
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1, "Replayed request cannot switch again");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			player.inventoryMenu.broadcastFullState();
			swap(player, 2);
			swap(player, 0);
			helper.assertTrue(ItemStack.matches(expected, player.getItemBySlot(EquipmentSlot.HEAD)), "Round trip restores exact armor");
			helper.assertTrue(ArmorLoadouts.state(player).stored(0, 0).isEmpty() && ArmorLoadouts.state(player).stored(1, 1).is(Items.IRON_CHESTPLATE), "Active set has no stored duplicate");
			helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_SWORD) && player.getOffhandItem().is(Items.SHIELD), "Weapons remain untouched");
			for (int slot : InventoryExpansion.sortableSlots(player.getInventory())) helper.assertTrue(player.getInventory().getItem(slot).getCount() == 64, "Full main and extra inventory stay unchanged");
			helper.assertTrue(ArmorLoadouts.state(other).unlocked == 1 && other.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Other player owns independent loadouts");
		} finally { remove(player); remove(other); }
		helper.succeed();
	}

	@GameTest
	public void rejectsUnsafeStaleAndBindingRequestsAndCancelsChanges(GameTestHelper helper) {
		var player = player(helper);
		try {
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1, "Locked set rejected");
			ArmorLoadouts.state(player).unlocked = 2;
			player.setOnGround(false);
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1, "Midair change rejected");
			player.setOnGround(true);
			player.inventoryMenu.setCarried(new ItemStack(Items.DIRT));
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1, "Cursor stack prevents change");
			player.inventoryMenu.setCarried(ItemStack.EMPTY);
			var helmet = new ItemStack(Items.IRON_HELMET);
			helmet.enchant(helper.getLevel().registryAccess().getOrThrow(Enchantments.BINDING_CURSE), 1);
			player.setItemSlot(EquipmentSlot.HEAD, helmet);
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1, "Binding cannot be bypassed");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(ArmorLoadouts.state(player).pending == 1, "Safe request starts timer");
			player.inventoryMenu.broadcastFullState();
			ArmorLoadouts.tick(player);
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1 && ArmorLoadouts.state(player).active == 0, "Menu revision change cancels without moving armor");
			ArmorLoadouts.request(player, request(player, 1));
			helper.assertTrue(player.hurtServer(helper.getLevel(), player.damageSources().generic(), 1), "Fixture accepts actual damage");
			helper.assertTrue(ArmorLoadouts.state(player).pending == -1 && ArmorLoadouts.state(player).active == 0, "Actual damage cancels timer");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest
	public void playerSaveAndRespawnPreserveUnlocksAndApplyItemOwnership(GameTestHelper helper) {
		var player = player(helper);
		var restored = player(helper);
		try {
			ArmorLoadouts.state(player).unlocked = 3;
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
			swap(player, 1);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			player.saveWithoutId(output);
			player.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
			helper.assertTrue(ArmorLoadouts.state(player).active == 1 && ArmorLoadouts.state(player).stored(0, 3).is(Items.DIAMOND_BOOTS), "Player save/load restores inactive armor and selection");
			ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, restored, true);
			helper.assertTrue(ArmorLoadouts.state(restored).unlocked == 3 && ArmorLoadouts.state(restored).stored(0, 3).is(Items.DIAMOND_BOOTS), "Alive respawn copies inventory ownership");
			ArmorLoadouts.state(restored).copyFrom(ArmorLoadouts.state(player), false);
			helper.assertTrue(ArmorLoadouts.state(restored).unlocked == 3 && ArmorLoadouts.state(restored).stored(0, 3).isEmpty(), "Loss of items never removes permanent unlocks");
		} finally { remove(player); remove(restored); }
		helper.succeed();
	}

	@GameTest
	public void keepInventoryRetainsStoredArmorAcrossDeathAndRespawn(GameTestHelper helper) {
		var player = player(helper);
		var restored = player(helper);
		var rules = helper.getLevel().getGameRules();
		boolean previous = rules.get(GameRules.KEEP_INVENTORY);
		try {
			rules.set(GameRules.KEEP_INVENTORY, true, helper.getLevel().getServer());
			ArmorLoadouts.state(player).unlocked = 2;
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
			swap(player, 1);
			player.die(player.damageSources().generic());
			ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player, restored, false);
			helper.assertTrue(ArmorLoadouts.state(restored).unlocked == 2 && ArmorLoadouts.state(restored).stored(0, 2).is(Items.DIAMOND_LEGGINGS), "Keep inventory retains inactive armor and unlocks through actual death");
		} finally {
			rules.set(GameRules.KEEP_INVENTORY, previous, helper.getLevel().getServer());
			remove(player);
			remove(restored);
		}
		helper.succeed();
	}

	@GameTest
	public void deathDropsInactiveArmorOnceAndHonorsVanishing(GameTestHelper helper) {
		var player = player(helper);
		try {
			helper.assertTrue(!helper.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY), "Fixture uses normal death rules");
			ArmorLoadouts.state(player).unlocked = 2;
			var helmet = new ItemStack(Items.DIAMOND_HELMET);
			helmet.set(DataComponents.CUSTOM_NAME, Component.literal(player.getUUID().toString()));
			player.setItemSlot(EquipmentSlot.HEAD, helmet);
			var boots = new ItemStack(Items.IRON_BOOTS);
			boots.enchant(helper.getLevel().registryAccess().getOrThrow(Enchantments.VANISHING_CURSE), 1);
			player.setItemSlot(EquipmentSlot.FEET, boots);
			swap(player, 1);
			player.die(player.damageSources().generic());
			helper.assertTrue(ArmorLoadouts.state(player).stored.stream().allMatch(ItemStack::isEmpty), "Actual death clears inactive sets");
			var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8), e -> ItemStack.isSameItemSameComponents(e.getItem(), helmet));
			helper.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "One matching inactive helmet drops");
			ArmorLoadouts.dropStored(player, helper.getLevel());
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8), e -> ItemStack.isSameItemSameComponents(e.getItem(), helmet)).size() == 1, "Repeated drop cannot duplicate inactive armor");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8), e -> ItemStack.isSameItemSameComponents(e.getItem(), boots)).isEmpty(), "Vanishing inactive armor is destroyed");
		} finally { remove(player); }
		helper.succeed();
	}
}
