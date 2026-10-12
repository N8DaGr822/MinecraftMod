package darkspawn.black.storage;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import darkspawn.black.inventory.InventoryExpansion;

public final class StorageGameTest {
	private static StorageBlockEntity chest(GameTestHelper helper, int x, StorageBlock block) {
		BlockPos pos = helper.absolutePos(new BlockPos(x, 2, 1));
		helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(SortingTubeBlock.FACING, Direction.EAST));
		return (StorageBlockEntity) helper.getLevel().getBlockEntity(pos);
	}
	private static void tube(GameTestHelper helper, int x, Direction output) {
		helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 2, 1)), Storage.TUBE.defaultBlockState().setValue(SortingTubeBlock.FACING, output));
	}
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "storage-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.getAbilities().invulnerable = true;
		Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
		player.teleportTo(pos.x, pos.y, pos.z);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

	@GameTest
	public void physicalRouteMergesBeforeOverflowAndKeepsUnmatchedItems(GameTestHelper helper) {
		var dump = chest(helper, 1, Storage.DUMP); tube(helper, 2, Direction.EAST);
		var first = chest(helper, 3, Storage.SORTING); tube(helper, 4, Direction.EAST);
		var second = chest(helper, 5, Storage.SORTING); tube(helper, 6, Direction.EAST);
		var bank = chest(helper, 7, Storage.BANK);
		first.setFilter(0, new ItemStack(Items.COBBLESTONE)); second.setFilter(0, new ItemStack(Items.COBBLESTONE));
		for (int i = 0; i < 27; i++) { first.setItem(i, new ItemStack(Items.COBBLESTONE, 64)); }
		first.setItem(20, new ItemStack(Items.COBBLESTONE, 60));
		helper.assertTrue(StorageRouting.transfer(dump, new ItemStack(Items.COBBLESTONE, 10), false) == 10, "Route accepts full offer");
		helper.assertTrue(first.getItem(20).getCount() == 64 && second.getItem(0).getCount() == 6, "First matching chest fills, remainder continues");
		helper.assertTrue(StorageRouting.transfer(dump, new ItemStack(Items.DIAMOND, 3), false) == 3 && bank.count(new ItemStack(Items.DIAMOND), false) == 3, "Unmatched items reach Bank");
		bank.setFilter(0, new ItemStack(Items.COBBLESTONE));
		StorageRouting.transfer(dump, new ItemStack(Items.COBBLESTONE, 2), false);
		helper.assertTrue(second.getItem(0).getCount() == 8 && bank.count(new ItemStack(Items.COBBLESTONE), true) == 0, "Sorting gets first chance before trash");
		second.setFilter(0, ItemStack.EMPTY);
		StorageRouting.transfer(dump, new ItemStack(Items.COBBLESTONE, 2), false);
		helper.assertTrue(bank.count(new ItemStack(Items.COBBLESTONE), true) == 2, "Filtered overflow is recoverable trash");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void dumpTickerPreservesSourceAcrossBrokenRoutesAndLoops(GameTestHelper helper) {
		var dump = chest(helper, 1, Storage.DUMP); tube(helper, 2, Direction.WEST);
		dump.setItem(0, new ItemStack(Items.IRON_INGOT, 17));
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(dump.getItem(0).getCount() == 17, "A cycle cannot consume or duplicate source items");
			tube(helper, 2, Direction.EAST);
			chest(helper, 3, Storage.BANK);
		});
		helper.runAfterDelay(24, () -> {
			var bank = (StorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 1)));
			helper.assertTrue(dump.getItem(0).isEmpty() && bank.count(new ItemStack(Items.IRON_INGOT), false) == 17, "Live ticker moves each item exactly once");
			helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 2, 1)), Blocks.AIR.defaultBlockState());
			dump.setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
		});
		helper.runAfterDelay(36, () -> {
			helper.assertTrue(dump.getItem(0).getCount() == 9, "Broken route retains source items"); helper.succeed();
		});
	}

	@GameTest
	public void bankCapacityAndComponentVariantsSurviveSaveLoad(GameTestHelper helper) {
		var bank = chest(helper, 1, Storage.BANK);
		ItemStack stone = new ItemStack(Items.STONE, BankContents.MAX_COUNT);
		helper.assertTrue(bank.insert(stone, false) == BankContents.MAX_COUNT, "Bank aggregates beyond stack size");
		helper.assertTrue(bank.insert(new ItemStack(Items.STONE), false) == 0, "Count cap does not overflow");
		for (int i = 0; i < 255; i++) {
			ItemStack named = new ItemStack(Items.DIAMOND_SWORD); named.set(DataComponents.CUSTOM_NAME, Component.literal("Sword " + i)); named.setDamageValue(i);
			helper.assertTrue(bank.insert(named, false) == 1, "Distinct components get separate entries");
		}
		helper.assertTrue(bank.insert(new ItemStack(Items.DIRT), false) == 0, "Full variant capacity rejects new types");
		bank.upgrade();
		helper.assertTrue(bank.insert(new ItemStack(Items.DIRT, 4), false) == 4, "Upgrade opens capacity");
		bank.setFilter(2, new ItemStack(Items.ROTTEN_FLESH)); bank.insert(new ItemStack(Items.ROTTEN_FLESH, 23), true);
		var restored = (StorageBlockEntity) BlockEntity.loadStatic(bank.getBlockPos(), bank.getBlockState(), bank.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
		helper.assertTrue(restored != null && restored.count(new ItemStack(Items.STONE), false) == BankContents.MAX_COUNT && restored.tier() == 1, "Save/load preserves aggregate counts and upgrades");
		helper.assertTrue(restored.count(new ItemStack(Items.ROTTEN_FLESH), true) == 23 && restored.filter(2).is(Items.ROTTEN_FLESH), "Save/load preserves trash and filters");
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD); sword.set(DataComponents.CUSTOM_NAME, Component.literal("Sword 81")); sword.setDamageValue(81);
		helper.assertTrue(restored.take(sword, 1, false).getDamageValue() == 81, "Withdrawal retains damage and custom name");
		helper.assertTrue(bank.insert(new ItemStack(Storage.BANK), false) == 0 && bank.insert(new ItemStack(Items.SHULKER_BOX), false) == 0, "Portable container nesting is rejected");
		helper.succeed();
	}

	@GameTest
	public void ghostFiltersNeverCreateItemsAndDepositPreservesEquipment(GameTestHelper helper) {
		var sorting = chest(helper, 1, Storage.SORTING);
		ServerPlayer player = player(helper);
		try {
			var menu = new StorageMenu(1, player.getInventory(), sorting);
			menu.setCarried(new ItemStack(Items.STONE, 12)); menu.clicked(27, 0, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().getCount() == 12 && sorting.filter(0).is(Items.STONE), "Ghost selection consumes nothing");
			menu.clicked(27, 0, ContainerInput.QUICK_MOVE, player); menu.clicked(27, 0, ContainerInput.SWAP, player);
			helper.assertTrue(menu.getCarried().getCount() == 12 && player.getInventory().getItem(0).isEmpty(), "Ghost slots cannot be extracted or swapped");
			menu.clicked(27, 1, ContainerInput.PICKUP, player);
			helper.assertTrue(sorting.filter(0).isEmpty() && menu.getCarried().getCount() == 12, "Clearing ghost consumes nothing");
			menu.setCarried(ItemStack.EMPTY);
			InventoryExpansion.setTier(player.getInventory(), 1);
			player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
			player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 40));
			player.getInventory().setItem(36, new ItemStack(Items.DIAMOND_BOOTS));
			player.getInventory().setItem(40, new ItemStack(Items.SHIELD));
			player.getInventory().setItem(InventoryExpansion.FIRST_EXTRA_SLOT, new ItemStack(Items.COBBLESTONE, 30));
			menu.action(player, StorageMenu.DEPOSIT, "");
			helper.assertTrue(sorting.countItem(Items.COBBLESTONE) == 70 && player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(43).isEmpty(), "Deposit includes extra unlocked rows");
			helper.assertTrue(player.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && player.getInventory().getItem(36).is(Items.DIAMOND_BOOTS) && player.getInventory().getItem(40).is(Items.SHIELD), "Hotbar, armor and offhand stay untouched");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest
	public void twoMenusWithdrawCurrentCountsWithoutRemappingRows(GameTestHelper helper) {
		var bank = chest(helper, 1, Storage.BANK);
		bank.insert(new ItemStack(Items.DIAMOND, 70), false); bank.insert(new ItemStack(Items.STONE, 5), false);
		ServerPlayer first = player(helper), second = player(helper);
		try {
			var a = new StorageMenu(1, first.getInventory(), bank); var b = new StorageMenu(2, second.getInventory(), bank);
			a.action(first, StorageMenu.SEARCH, "diamond"); b.action(second, StorageMenu.SEARCH, "diamond");
			helper.assertTrue(a.quantity(0) == 70, "Menu reports aggregate count");
			a.clicked(0, 0, ContainerInput.PICKUP, first); b.clicked(0, 0, ContainerInput.PICKUP, second);
			helper.assertTrue(a.getCarried().getCount() == 64 && b.getCarried().getCount() == 6 && bank.count(new ItemStack(Items.DIAMOND), false) == 0, "Concurrent views cannot duplicate withdrawals");
			b.setCarried(ItemStack.EMPTY); b.clicked(0, 0, ContainerInput.PICKUP, second);
			helper.assertTrue(b.getCarried().isEmpty() && bank.count(new ItemStack(Items.STONE), false) == 5, "Exhausted row does not silently target a different item");
			b.action(second, StorageMenu.SEARCH, "stone");
			for (int i = 0; i < 36; i++) { second.getInventory().setItem(i, new ItemStack(Items.DIRT, 64)); }
			b.clicked(0, 0, ContainerInput.QUICK_MOVE, second);
			helper.assertTrue(bank.count(new ItemStack(Items.STONE), false) == 5, "Full player inventory keeps Bank items");
			second.getInventory().setItem(9, ItemStack.EMPTY); b.clicked(0, 0, ContainerInput.QUICK_MOVE, second);
			helper.assertTrue(second.getInventory().getItem(9).getCount() == 5 && bank.count(new ItemStack(Items.STONE), false) == 0, "Shift withdrawal debits accepted amount only");
		} finally { remove(first); remove(second); }
		helper.succeed();
	}

	@GameTest
	public void trashConfirmationAndUpgradePaymentAreServerValidated(GameTestHelper helper) {
		var bank = chest(helper, 1, Storage.BANK);
		bank.setFilter(0, new ItemStack(Items.ROTTEN_FLESH)); bank.insert(new ItemStack(Items.ROTTEN_FLESH, 7), true);
		ServerPlayer player = player(helper);
		try {
			var menu = new StorageMenu(1, player.getInventory(), bank);
			menu.action(player, StorageMenu.TRASH, "");
			helper.assertTrue(bank.count(new ItemStack(Items.ROTTEN_FLESH), true) == 7, "Bank tab cannot delete trash");
			menu.action(player, StorageMenu.TAB, ""); menu.action(player, StorageMenu.TRASH, "");
			helper.assertTrue(menu.trashArmed() && bank.count(new ItemStack(Items.ROTTEN_FLESH), true) == 7, "First click only arms deletion");
			bank.insert(new ItemStack(Items.ROTTEN_FLESH), true); menu.action(player, StorageMenu.TRASH, "");
			helper.assertTrue(bank.count(new ItemStack(Items.ROTTEN_FLESH), true) == 8, "New arrivals invalidate prior confirmation");
			menu.action(player, StorageMenu.TRASH, ""); helper.assertTrue(bank.count(new ItemStack(Items.ROTTEN_FLESH), true) == 0, "Confirmed trash deletion clears only trash");
			menu.action(player, StorageMenu.UPGRADE, ""); helper.assertTrue(bank.tier() == 0, "Unpaid upgrade rejected");
			player.getInventory().setItem(9, new ItemStack(Items.DIAMOND_BLOCK, 3));
			menu.action(player, StorageMenu.UPGRADE, ""); menu.action(player, StorageMenu.UPGRADE, ""); menu.action(player, StorageMenu.UPGRADE, "");
			helper.assertTrue(bank.tier() == 2 && player.getInventory().getItem(9).isEmpty(), "Upgrade costs 1 then 2 blocks, never grants unpaid third tier");
			Vec3 far = Vec3.atCenterOf(bank.getBlockPos()).add(20, 0, 0); player.teleportTo(far.x, far.y, far.z);
			player.getInventory().setItem(9, new ItemStack(Items.DIAMOND_BLOCK, 4)); menu.action(player, StorageMenu.UPGRADE, "");
			helper.assertTrue(bank.tier() == 2 && player.getInventory().getItem(9).getCount() == 4, "Distant menu cannot mutate block or inventory");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest
	public void breakingBankPreservesContentsInOnePlaceableItem(GameTestHelper helper) {
		var bank = chest(helper, 1, Storage.BANK);
		bank.insert(new ItemStack(Items.STONE, 12000), false); bank.setFilter(0, new ItemStack(Items.DIRT)); bank.insert(new ItemStack(Items.DIRT, 200), true); bank.upgrade();
		BlockPos pos = bank.getBlockPos();
		helper.getLevel().destroyBlock(pos, true);
		var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), entity -> entity.getItem().is(Storage.BANK.asItem()));
		helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().has(Storage.BANK_CONTENTS), "Breaking drops exactly one packed Bank");
		ItemStack item = drops.getFirst().getItem().copy(); drops.getFirst().discard();
		var restored = chest(helper, 1, Storage.BANK); restored.applyComponentsFromItemStack(item);
		helper.assertTrue(restored.count(new ItemStack(Items.STONE), false) == 12000 && restored.count(new ItemStack(Items.DIRT), true) == 200 && restored.tier() == 1, "Placement restores stored items, trash and upgrade");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void bankResortUsesConnectedFiltersWithoutTouchingTrash(GameTestHelper helper) {
		chest(helper, 1, Storage.DUMP); tube(helper, 2, Direction.EAST);
		var sorting = chest(helper, 3, Storage.SORTING); tube(helper, 4, Direction.EAST);
		var bank = chest(helper, 5, Storage.BANK);
		bank.insert(new ItemStack(Items.STONE, 130), false); bank.insert(new ItemStack(Items.DIAMOND, 7), false);
		bank.setFilter(0, new ItemStack(Items.DIRT)); bank.insert(new ItemStack(Items.DIRT, 3), true);
		sorting.setFilter(0, new ItemStack(Items.STONE)); bank.startResort();
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(sorting.countItem(Items.STONE) == 130 && bank.count(new ItemStack(Items.STONE), false) == 0, "Re-sort drains matching entries over bounded ticks");
			helper.assertTrue(bank.count(new ItemStack(Items.DIAMOND), false) == 7 && bank.count(new ItemStack(Items.DIRT), true) == 3 && bank.resortRemaining() == 0, "Unmatched items and trash remain; pass terminates");
			helper.succeed();
		});
	}

	@GameTest
	public void chestSortPreservesComponentsAndBankSearchPagesAreAuthoritative(GameTestHelper helper) {
		var sorting = chest(helper, 1, Storage.SORTING);
		ServerPlayer player = player(helper);
		try {
			var menu = new StorageMenu(1, player.getInventory(), sorting);
			sorting.setItem(0, new ItemStack(Items.STONE, 40)); sorting.setItem(9, new ItemStack(Items.STONE, 40));
			ItemStack named = new ItemStack(Items.STONE, 4); named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this name")); sorting.setItem(20, named);
			sorting.setFilter(0, new ItemStack(Items.DIAMOND));
			menu.action(player, StorageMenu.ORDER, "");
			helper.assertTrue(sorting.getItem(0).getCount() == 64 && sorting.getItem(1).getCount() == 16 && sorting.getItem(2).getCount() == 4, "Chest sort merges compatible stacks only");
			helper.assertTrue(sorting.getItem(2).has(DataComponents.CUSTOM_NAME) && sorting.filter(0).is(Items.DIAMOND), "Sort preserves components and ghost filters");
			var bank = chest(helper, 1, Storage.BANK);
			for (int i = 0; i < 35; i++) {
				ItemStack stack = new ItemStack(Items.STONE, i == 34 ? 70000 : 2);
				stack.set(DataComponents.CUSTOM_NAME, Component.literal(String.format(java.util.Locale.ROOT, "Stone %02d", i)));
				bank.insert(stack, false);
			}
			menu = new StorageMenu(2, player.getInventory(), bank);
			helper.assertTrue(menu.pages() == 2, "Bank paginates distinct component variants");
			menu.action(player, StorageMenu.NEXT, ""); helper.assertTrue(menu.page() == 1 && menu.getSlot(0).getItem().getHoverName().getString().equals("Stone 27"), "Next page maps correct identities");
			menu.action(player, StorageMenu.SEARCH, "Stone 34");
			helper.assertTrue(menu.page() == 0 && menu.pages() == 1 && menu.quantity(0) == 70000 && menu.getSlot(1).getItem().isEmpty(), "Search refreshes slots and quantities above signed-short range");
			menu.action(player, StorageMenu.SEARCH, "absent item"); helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "No matches clears stale visible rows");
			menu.action(player, StorageMenu.SEARCH, ""); menu.action(player, StorageMenu.ORDER, "");
			helper.assertTrue(menu.getSlot(0).getItem().getHoverName().getString().equals("Stone 34"), "Quantity ordering puts largest entry first");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest
	public void packedBankNetworkRoundTripAndUnloadedRouteAreSafe(GameTestHelper helper) {
		var bank = chest(helper, 1, Storage.BANK);
		bank.insert(new ItemStack(Items.DIAMOND, 900000), false);
		ItemStack packed = new ItemStack(Storage.BANK); packed.set(Storage.BANK_CONTENTS, bank.snapshot());
		var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
		try {
			ItemStack.STREAM_CODEC.encode(buffer, packed);
			ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer);
			helper.assertTrue(decoded.get(Storage.BANK_CONTENTS).stored().getFirst().count() == 900000, "Packed Bank synchronizes aggregate counts without truncation");
		} finally { buffer.release(); }
		BlockPos remote = new BlockPos(-29000000, 100, 29000000);
		helper.assertTrue(!helper.getLevel().hasChunkAt(remote), "Fixture starts unloaded");
		helper.assertTrue(StorageRouting.route(helper.getLevel(), remote).isEmpty() && !helper.getLevel().hasChunkAt(remote), "Routing must not load absent chunks");
		helper.succeed();
	}
}
