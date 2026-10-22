package darkspawn.black.inventory;

import com.mojang.authlib.GameProfile;
import darkspawn.black.oneblock.OneBlockItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;

public final class IngredientPouchGameTest {
	private static ServerPlayer player(GameTestHelper h) {
		var level = h.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pouch-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleportTo(h.absolutePos(net.minecraft.core.BlockPos.ZERO).getX(), h.absolutePos(net.minecraft.core.BlockPos.ZERO).getY() + 2,
			h.absolutePos(net.minecraft.core.BlockPos.ZERO).getZ());
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		return player;
	}

	private static void remove(ServerPlayer p) {
		p.level().getServer().getPlayerList().remove(p);
		p.discard();
	}

	private static int count(ItemStack pouch) {
		return pouch.get(DataComponents.BUNDLE_CONTENTS).itemCopies().mapToInt(ItemStack::getCount).sum();
	}

	@GameTest
	public void insertionBothDirectionsCapacityAndExtraction(GameTestHelper h) {
		var p = player(h);
		try {
			var pouch = new ItemStack(IngredientPouchItem.ITEM);
			var item = (IngredientPouchItem) pouch.getItem();
			var inventory = new SimpleContainer(1);
			var slot = new Slot(inventory, 0, 0, 0);
			inventory.setItem(0, new ItemStack(Items.NETHER_WART, 60));
			item.overrideStackedOnOther(pouch, slot, ClickAction.PRIMARY, p);
			h.assertTrue(inventory.isEmpty() && count(pouch) == 60, "Pouch on ingredient transfers exact count");
			inventory.setItem(0, pouch);
			var reagent = new ItemStack(Items.REDSTONE, 10);
			item.overrideOtherStackedOnMe(pouch, reagent, slot, ClickAction.PRIMARY, p, SlotAccess.of(() -> ItemStack.EMPTY, stack -> { }));
			h.assertTrue(count(pouch) == 64 && reagent.getCount() == 6, "Capacity allows only four more stackable reagents");
			item.overrideOtherStackedOnMe(pouch, reagent, slot, ClickAction.PRIMARY, p, SlotAccess.of(() -> ItemStack.EMPTY, stack -> { }));
			h.assertTrue(count(pouch) == 64 && reagent.getCount() == 6, "Full pouch never consumes overflow");
			var output = new SimpleContainer(1);
			item.overrideOtherStackedOnMe(pouch, ItemStack.EMPTY, slot, ClickAction.SECONDARY, p, SlotAccess.of(() -> output.getItem(0), stack -> output.setItem(0, stack)));
			h.assertTrue(count(pouch) == 60 && output.getItem(0).is(Items.REDSTONE) && output.getItem(0).getCount() == 4,
				"Extraction returns exact newest stack");
		} finally { remove(p); }
		h.succeed();
	}

	@GameTest
	public void eligibilityRejectsFoodPotionsAndNestedStorage(GameTestHelper h) {
		var p = player(h);
		try {
			var pouch = new ItemStack(IngredientPouchItem.ITEM);
			var item = (IngredientPouchItem) pouch.getItem();
			var inventory = new SimpleContainer(1);
			var slot = new Slot(inventory, 0, 0, 0);
			for (var invalid : new net.minecraft.world.item.Item[] { Items.APPLE, Items.POTION, Items.GLASS_BOTTLE, Items.BUNDLE, Items.SHULKER_BOX, IngredientPouchItem.ITEM }) {
				var other = new ItemStack(invalid);
				inventory.setItem(0, other);
				item.overrideStackedOnOther(pouch, slot, ClickAction.PRIMARY, p);
				h.assertTrue(count(pouch) == 0 && other.getCount() == 1, "Rejected slot remains untouched");
				inventory.setItem(0, pouch);
				item.overrideOtherStackedOnMe(pouch, other, slot, ClickAction.PRIMARY, p, SlotAccess.of(() -> ItemStack.EMPTY, stack -> { }));
				h.assertTrue(count(pouch) == 0 && other.getCount() == 1, "Rejected cursor remains untouched");
			}
			h.assertTrue(!BundleContents.canItemBeInBundle(pouch), "Pouch cannot be nested in a vanilla bundle");
			h.assertTrue(!new net.minecraft.world.inventory.ShulkerBoxSlot(inventory, 0, 0, 0).mayPlace(pouch),
				"Shulker menu rejects pouch nesting");
			var shulker = new net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity(net.minecraft.core.BlockPos.ZERO,
				net.minecraft.world.level.block.Blocks.SHULKER_BOX.defaultBlockState());
			h.assertTrue(!shulker.canPlaceItemThroughFace(0, pouch, net.minecraft.core.Direction.UP)
				&& shulker.canPlaceItemThroughFace(0, new ItemStack(Items.NETHER_WART), net.minecraft.core.Direction.UP),
				"Hopper insertion rejects pouches but preserves ordinary shulker storage");
			h.assertTrue(!pouch.is(net.minecraft.tags.ItemTags.BUNDLES), "Pouch cannot be converted into an ordinary bundle by dye recipes");
			var disguised = new ItemStack(Items.NETHER_WART);
			disguised.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
			h.assertTrue(!IngredientPouchItem.accepts(disguised), "Tagged ingredients with storage components cannot bypass nesting rules");
			h.assertTrue(IngredientPouchItem.accepts(new ItemStack(darkspawn.black.cooking.Cooking.WILD_HERBS)),
				"Mod brewing herbs are eligible");
			inventory.setItem(0, new ItemStack(Items.TURTLE_HELMET));
			item.overrideStackedOnOther(pouch, slot, ClickAction.PRIMARY, p);
			h.assertTrue(count(pouch) == 1 && item.getBarWidth(pouch) == 13, "Unstackable brewing reagent uses whole native bundle capacity");
		} finally { remove(p); }
		h.succeed();
	}

	@GameTest
	public void sortingFavoritesAndProfileSerializationKeepContents(GameTestHelper h) {
		var p = player(h);
		try {
			var pouch = new ItemStack(IngredientPouchItem.ITEM);
			var contents = BundleContents.EMPTY.asMutable();
			contents.tryInsert(new ItemStack(Items.NETHER_WART, 17));
			pouch.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
			p.getInventory().setItem(9, pouch);
			p.getInventory().setItem(10, new ItemStack(Items.APPLE));
			Accessories.state(p).favorites = 1;
			InventorySorter.sort(p, p.inventoryMenu.getStateId());
			h.assertTrue(ItemStack.matches(pouch, p.getInventory().getItem(9)), "Favorite pouch remains in place with contents");
			var ops = p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
			var profile = OneBlockItems.capture(p);
			var saved = OneBlockItems.CODEC.encodeStart(ops, profile).getOrThrow();
			OneBlockItems.EMPTY.apply(p);
			h.assertTrue(p.getInventory().countItem(IngredientPouchItem.ITEM) == 0, "Empty island profile does not retain outside pouch");
			OneBlockItems.CODEC.parse(ops, saved).getOrThrow().apply(p);
			h.assertTrue(ItemStack.matches(pouch, p.getInventory().getItem(9)), "Shared OneBlock/Ascension profile codec restores exact pouch");
			var state = new darkspawn.black.oneblock.OneBlockPlayerState();
			state.other = profile;
			state.ascensionOther = profile;
			var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess());
			state.write(output);
			var restored = new darkspawn.black.oneblock.OneBlockPlayerState();
			restored.read(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess(), output.buildResult()));
			OneBlockItems.EMPTY.apply(p);
			restored.other.apply(p);
			h.assertTrue(ItemStack.matches(pouch, p.getInventory().getItem(9)), "Legacy OneBlock inactive profile preserves pouch");
			OneBlockItems.EMPTY.apply(p);
			restored.ascensionOther.apply(p);
			h.assertTrue(ItemStack.matches(pouch, p.getInventory().getItem(9)), "Ascension inactive profile preserves pouch independently");
			Accessories.state(p).favorites = 0;
			InventorySorter.sort(p, p.inventoryMenu.getStateId());
			h.assertTrue(java.util.Arrays.stream(InventoryExpansion.sortableSlots(p.getInventory()))
				.filter(i -> ItemStack.matches(pouch, p.getInventory().getItem(i))).count() == 1, "Sorting preserves one filled pouch");
		} finally { remove(p); }
		h.succeed();
	}

	@GameTest
	public void deathDropsOneFilledPouch(GameTestHelper h) {
		var p = player(h);
		try {
			var pouch = new ItemStack(IngredientPouchItem.ITEM);
			var contents = BundleContents.EMPTY.asMutable();
			contents.tryInsert(new ItemStack(Items.BLAZE_POWDER, 12));
			pouch.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
			p.getInventory().setItem(9, pouch);
			var area = p.getBoundingBox().inflate(8);
			p.die(p.damageSources().generic());
			var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> ItemStack.matches(e.getItem(), pouch));
			h.assertTrue(drops.size() == 1 && p.getInventory().countItem(IngredientPouchItem.ITEM) == 0,
				"Death drops one filled pouch and clears the inventory copy");
			drops.forEach(ItemEntity::discard);
		} finally { remove(p); }
		h.succeed();
	}
}
