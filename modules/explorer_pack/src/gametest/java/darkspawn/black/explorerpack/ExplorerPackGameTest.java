package darkspawn.black.explorerpack;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;

public final class ExplorerPackGameTest {
	private static ServerPlayer player(GameTestHelper h) {
		var level = h.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pack-test"), false);
		var p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(p.getAbilities());
		var pos = h.absolutePos(net.minecraft.core.BlockPos.ZERO);
		p.teleportTo(pos.getX(), pos.getY() + 2, pos.getZ());
		p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		return p;
	}
	private static void remove(ServerPlayer p) { p.closeContainer(); p.level().getServer().getPlayerList().remove(p); p.discard(); }
	private static ExplorerPackMenu open(ServerPlayer p, boolean offhand) {
		var hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		p.getItemInHand(hand).getItem().use(p.level(), p, hand);
		return (ExplorerPackMenu)p.containerMenu;
	}
	private static int count(ItemStack pack) { return pack.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum(); }
	@GameTest public void sixSlotsAcceptExplorationOnlyAndPersistImmediately(GameTestHelper h) {
		var p = player(h);
		try {
			var pack = new ItemStack(ExplorerPackItem.ITEM); p.getInventory().setItem(0, pack);
			var menu = open(p, false);
			for (var item : new net.minecraft.world.item.Item[]{Items.MAP, Items.FILLED_MAP, Items.COMPASS, Items.RECOVERY_COMPASS,
				Items.CLOCK, Items.SPYGLASS, Items.BRUSH})
				h.assertTrue(ExplorerPackItem.accepts(new ItemStack(item)), "Exploration item accepted: " + item);
			for (var item : new net.minecraft.world.item.Item[]{Items.APPLE, Items.ARROW, Items.BUNDLE, Items.SHULKER_BOX,
				ExplorerPackItem.ITEM}) {
				menu.setCarried(new ItemStack(item)); menu.clicked(0, 0, ContainerInput.PICKUP, p);
				h.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getCarried().getCount() == 1, "Rejected items stay on cursor");
			}
			var nested = new ItemStack(Items.COMPASS); nested.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
			h.assertTrue(!ExplorerPackItem.accepts(nested), "Disguised nested container rejected");
			menu.setCarried(ItemStack.EMPTY);
			for (int i = 0; i < 6; i++) { menu.setCarried(new ItemStack(Items.MAP, 64)); menu.clicked(i, 0, ContainerInput.PICKUP, p); }
			h.assertTrue(count(pack) == 384, "Six normal stack slots save immediately");
			p.getInventory().setItem(9, new ItemStack(Items.CLOCK)); menu.quickMoveStack(p, 6);
			h.assertTrue(p.getInventory().getItem(9).is(Items.CLOCK), "Full pack leaves extra item untouched");
			p.closeContainer(); menu = open(p, false);
			h.assertTrue(menu.getSlot(5).getItem().getCount() == 64, "Last slot survives reopen");
			menu.quickMoveStack(p, 5); h.assertTrue(count(pack) == 320, "Shift withdrawal saves without duplication");
		} finally { remove(p); } h.succeed();
	}
	@GameTest public void sourceLockCoversHotbarOffhandAndStaleMenus(GameTestHelper h) {
		var p = player(h);
		try {
			var pack = new ItemStack(ExplorerPackItem.ITEM); p.getInventory().setItem(0, pack);
			var menu = open(p, false);
			for (var input : new ContainerInput[]{ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.THROW}) menu.clicked(33, 0, input, p);
			menu.clicked(0, 0, ContainerInput.SWAP, p);
			h.assertTrue(p.getInventory().getItem(0) == pack && menu.getCarried().isEmpty(), "Held pack cannot move, drop or swap");
			menu.setCarried(new ItemStack(Items.BRUSH)); menu.clicked(0, 0, ContainerInput.PICKUP, p);
			p.closeContainer(); p.getInventory().setItem(0, ItemStack.EMPTY); p.getInventory().setItem(40, pack);
			menu = open(p, true); menu.clicked(6, 40, ContainerInput.SWAP, p);
			h.assertTrue(p.getOffhandItem() == pack && count(pack) == 1, "Offhand swap cannot move the open pack");
			p.getInventory().setItem(40, ItemStack.EMPTY);
			h.assertTrue(!menu.stillValid(p), "External removal invalidates menu");
			menu.clicked(0, 0, ContainerInput.PICKUP, p);
			h.assertTrue(menu.getCarried().isEmpty() && count(pack) == 1, "Stale menu cannot withdraw duplicate contents");
			h.assertTrue(!pack.getItem().canFitInsideContainerItems(), "Pack cannot nest in native containers");
			var shulker = new net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.SHULKER_BOX.defaultBlockState());
			h.assertTrue(!shulker.canPlaceItemThroughFace(0, pack, net.minecraft.core.Direction.UP), "Hoppers cannot nest the pack");
		} finally { remove(p); } h.succeed();
	}
	@GameTest public void overflowRefusesOpenWithoutLosingData(GameTestHelper h) {
		var p = player(h);
		try {
			var pack = new ItemStack(ExplorerPackItem.ITEM);
			pack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.Collections.nCopies(7, new ItemStack(Items.MAP))));
			p.getInventory().setItem(0, pack); pack.getItem().use(p.level(), p, InteractionHand.MAIN_HAND);
			h.assertTrue(p.containerMenu == p.inventoryMenu && count(pack) == 7, "Malformed overflow is preserved, never truncated");
		} finally { remove(p); } h.succeed();
	}
	@GameTest public void draggingFullInventoryAndCompassComponentsAreSafe(GameTestHelper h) {
		var p = player(h);
		try {
			var pack = new ItemStack(ExplorerPackItem.ITEM); p.getInventory().setItem(0, pack); var menu = open(p, false);
			menu.setCarried(new ItemStack(Items.MAP, 9));
			menu.clicked(-999, 0, ContainerInput.QUICK_CRAFT, p);
			for (int i = 0; i < 3; i++) menu.clicked(i, 1, ContainerInput.QUICK_CRAFT, p);
			menu.clicked(-999, 2, ContainerInput.QUICK_CRAFT, p);
			h.assertTrue(count(pack) == 9 && menu.getCarried().isEmpty() && menu.getSlot(2).getItem().getCount() == 3,
				"Drag distributes and persists exactly nine maps");
			var compass = new ItemStack(Items.COMPASS);
			compass.set(DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
				java.util.Optional.of(net.minecraft.core.GlobalPos.of(p.level().dimension(), p.blockPosition())), false));
			menu.setCarried(compass.copy()); menu.clicked(3, 0, ContainerInput.PICKUP, p);
			for (int i = 1; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
			menu.quickMoveStack(p, 3);
			h.assertTrue(count(pack) == 10 && ItemStack.isSameItemSameComponents(compass, menu.getSlot(3).getItem()),
				"Full inventory cannot spill or erase the bound compass");
			menu.quickMoveStack(p, 6);
			h.assertTrue(count(pack) == 10 && p.getInventory().getItem(9).getCount() == 64, "Invalid shift insertion consumes nothing");
			p.closeContainer(); menu = open(p, false);
			menu.clicked(3, 0, ContainerInput.PICKUP, p);
			h.assertTrue(ItemStack.isSameItemSameComponents(compass, menu.getCarried()) && count(pack) == 9,
				"Manual withdrawal preserves compass target and saves immediately");
			menu.setCarried(ItemStack.EMPTY);
		} finally { remove(p); } h.succeed();
	}
}
