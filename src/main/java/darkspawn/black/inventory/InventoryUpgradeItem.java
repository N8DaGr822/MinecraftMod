package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class InventoryUpgradeItem extends Item {
	private final int tier;

	private InventoryUpgradeItem(Properties properties, int tier) {
		super(properties);
		this.tier = tier;
	}

	public static void register() {
		register("leather_inventory_upgrade", 1);
		register("iron_inventory_upgrade", 2);
		register("diamond_inventory_upgrade", 3);
	}

	private static void register(String name, int tier) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		Registry.register(BuiltInRegistries.ITEM, key,
				new InventoryUpgradeItem(new Properties().setId(key).stacksTo(1), tier));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			int currentTier = InventoryExpansion.tier(player.getInventory());
			if (currentTier != tier - 1) {
				serverPlayer.sendOverlayMessage(Component.translatable(currentTier >= tier
						? "message.darkspawn.upgrade_already_applied" : "message.darkspawn.upgrade_previous_required"));
				return InteractionResult.FAIL;
			}
			InventoryExpansion.setTier(player.getInventory(), tier);
			player.getItemInHand(hand).consume(1, player);
			player.getInventory().setChanged();
			InventoryExpansion.sync(serverPlayer);
			player.inventoryMenu.broadcastFullState();
			serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.inventory_upgraded", 27 + tier * 9));
		}
		return InteractionResult.SUCCESS;
	}
}
