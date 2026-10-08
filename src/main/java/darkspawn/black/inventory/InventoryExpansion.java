package darkspawn.black.inventory;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public final class InventoryExpansion {
	public static final int MAX_TIER = 3;
	public static final int SLOTS_PER_TIER = 9;
	public static final int EXTRA_SLOTS = MAX_TIER * SLOTS_PER_TIER;
	// Slot Compatibility: Vanilla equipment keeps indices 36-42; extra storage starts after it.
	public static final int FIRST_EXTRA_SLOT = 43;
	public static final int STORAGE_SIZE = FIRST_EXTRA_SLOT + EXTRA_SLOTS;
	public static final int FIRST_EXTRA_MENU_SLOT = 46;

	private InventoryExpansion() {
	}

	public static int tier(Inventory inventory) {
		return ((ExpandedInventory) inventory).darkspawn$getUpgradeTier();
	}

	public static void setTier(Inventory inventory, int tier) {
		((ExpandedInventory) inventory).darkspawn$setUpgradeTier(tier);
	}

	public static int unlockedSlots(Inventory inventory) {
		return tier(inventory) * SLOTS_PER_TIER;
	}

	public static int[] sortableSlots(Container inventory) {
		int extra = inventory instanceof Inventory playerInventory ? unlockedSlots(playerInventory) : 0;
		int[] slots = new int[Inventory.INVENTORY_SIZE - Inventory.SELECTION_SIZE + extra];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = i < 27 ? i + Inventory.SELECTION_SIZE : FIRST_EXTRA_SLOT + i - 27;
		}
		return slots;
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(InventoryTierPayload.TYPE, InventoryTierPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		// Respawn: Capacity is permanent; vanilla inventory copying/dropping still controls the items.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				setTier(newPlayer.getInventory(), tier(oldPlayer.getInventory())));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> sync(player));
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, InventoryTierPayload.TYPE)) {
			ServerPlayNetworking.send(player, new InventoryTierPayload(tier(player.getInventory())));
		}
	}
}
