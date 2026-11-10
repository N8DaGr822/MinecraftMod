package darkspawn.black.oneblock;

import darkspawn.black.ingredientpouch.*;

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

/** Container contents survive the OneBlock/Ascension profile codec (cross-mod integration). */
public final class IngredientPouchProfileGameTest {
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
	public void profileSerializationKeepsContents(GameTestHelper h) {
		var p = player(h);
		try {
			var pouch = new ItemStack(IngredientPouchItem.ITEM);
			var contents = BundleContents.EMPTY.asMutable();
			contents.tryInsert(new ItemStack(Items.NETHER_WART, 17));
			pouch.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
			p.getInventory().setItem(9, pouch);
			p.getInventory().setItem(10, new ItemStack(Items.APPLE));
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
		} finally { remove(p); }
		h.succeed();
	}
	@GameTest
	public void modBrewingHerbsAreEligible(GameTestHelper h) {
		// Optional tag entry: present only when the crop's mod is loaded.
		h.assertTrue(IngredientPouchItem.accepts(new ItemStack(darkspawn.black.cooking.Cooking.WILD_HERBS)), "Mod brewing herbs are eligible");
		h.succeed();
	}
}
