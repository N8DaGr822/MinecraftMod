package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class ArmorLoadoutUpgradeItem extends Item {
	private final int sets;
	private ArmorLoadoutUpgradeItem(Properties properties, int sets) { super(properties); this.sets = sets; }
	public static void register() {
		register("iron_armor_loadout_upgrade", 2);
		register("diamond_armor_loadout_upgrade", 3);
	}
	private static void register(String name, int sets) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new ArmorLoadoutUpgradeItem(new Properties().setId(key).stacksTo(1), sets));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(item));
	}
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			var state = ArmorLoadouts.state(player);
			if (state.unlocked != sets - 1) {
				serverPlayer.sendOverlayMessage(Component.translatable(state.unlocked >= sets
					? "message.darkspawn.upgrade_already_applied" : "message.darkspawn.upgrade_previous_required"));
				return InteractionResult.FAIL;
			}
			ArmorLoadouts.cancel(serverPlayer);
			state.unlocked = sets;
			state.revision++;
			player.getItemInHand(hand).consume(1, player);
			player.getInventory().setChanged();
			player.inventoryMenu.broadcastFullState();
			ArmorLoadouts.sync(serverPlayer);
			serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.loadout_unlocked", sets));
		}
		return InteractionResult.SUCCESS;
	}
}
