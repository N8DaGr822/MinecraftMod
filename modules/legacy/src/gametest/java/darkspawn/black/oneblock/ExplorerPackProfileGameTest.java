package darkspawn.black.oneblock;

import darkspawn.black.explorerpack.*;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;

/** Container contents survive the OneBlock/Ascension profile codec (cross-mod integration). */
public final class ExplorerPackProfileGameTest {
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
	@GameTest public void componentsProfilesAndDeathPreserveContents(GameTestHelper h) {
		var p = player(h);
		try {
			var pack = new ItemStack(ExplorerPackItem.ITEM); p.getInventory().setItem(0, pack); var menu = open(p, false);
			var map = new ItemStack(Items.FILLED_MAP); map.set(DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(123));
			map.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Expedition route"));
			menu.setCarried(map.copy()); menu.clicked(4, 0, ContainerInput.PICKUP, p); p.closeContainer();
			var ops = p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
			var saved = OneBlockItems.CODEC.encodeStart(ops, OneBlockItems.capture(p)).getOrThrow(); OneBlockItems.EMPTY.apply(p);
			OneBlockItems.CODEC.parse(ops, saved).getOrThrow().apply(p); menu = open(p, false);
			h.assertTrue(ItemStack.isSameItemSameComponents(map, menu.getSlot(4).getItem()), "Map identity/name and empty slots survive profile codec");
			var area = p.getBoundingBox().inflate(8); p.die(p.damageSources().generic());
			var drops = p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area, e -> e.getItem().is(ExplorerPackItem.ITEM));
			h.assertTrue(drops.size() == 1 && count(drops.getFirst().getItem()) == 1, "Death drops one filled pack even while open");
			drops.forEach(net.minecraft.world.entity.Entity::discard);
		} finally { remove(p); } h.succeed();
	}	@GameTest public void landmarkAndLogboundCompassesFitThePack(GameTestHelper h) {
		// Optional tag entries: present only when landmarks and pirate seas are loaded.
		h.assertTrue(ExplorerPackItem.accepts(new ItemStack(darkspawn.black.worldgen.LandmarkCompass.ITEM))
			&& ExplorerPackItem.accepts(new ItemStack(darkspawn.black.pirate.LogboundCompass.ITEM)), "Mod compasses are exploration items");
		h.succeed();
	}
	@GameTest public void containerModsRejectEachOther(GameTestHelper h) {
		// Each container mod ships its own nesting checks; together they still refuse one another.
		for (var other : new net.minecraft.world.item.Item[]{darkspawn.black.quiver.QuiverItem.ITEM, darkspawn.black.ingredientpouch.IngredientPouchItem.ITEM})
			h.assertTrue(!ExplorerPackItem.accepts(new ItemStack(other)), "Explorer pack rejects " + other);
		h.assertTrue(!darkspawn.black.quiver.QuiverItem.accepts(new ItemStack(darkspawn.black.ingredientpouch.IngredientPouchItem.ITEM))
			&& !darkspawn.black.quiver.QuiverItem.accepts(new ItemStack(ExplorerPackItem.ITEM))
			&& !darkspawn.black.ingredientpouch.IngredientPouchItem.accepts(new ItemStack(darkspawn.black.quiver.QuiverItem.ITEM))
			&& !darkspawn.black.ingredientpouch.IngredientPouchItem.accepts(new ItemStack(ExplorerPackItem.ITEM)), "Quiver and pouch reject the other containers");
		h.succeed();
	}
}
