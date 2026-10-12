package darkspawn.black.inventory;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;

public final class ArmorLoadouts {
	public static final int SWITCH_TICKS = 40;
	private ArmorLoadouts() { }
	public static ArmorLoadoutState state(Player player) { return ((ArmorLoadoutPlayer) player).darkspawn$armorLoadouts(); }

	public static void initialize() {
		ArmorLoadoutUpgradeItem.register();
		PayloadTypeRegistry.serverboundPlay().register(ArmorLoadoutPayload.TYPE, ArmorLoadoutPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ArmorLoadoutSyncPayload.TYPE, ArmorLoadoutSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ArmorLoadoutPayload.TYPE, (payload, context) -> request(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> state(newPlayer).copyFrom(state(oldPlayer),
			alive || newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> { cancel(player); sync(player); });
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && (taken > 0 || !blocked && base > 0)) {
				state(player).hurtCooldown = 60;
				cancel(player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ArmorLoadouts::tick));
	}

	private static boolean safe(Player player) {
		return player.isAlive() && !player.isSpectator() && player.onGround() && !player.isPassenger()
			&& !player.isFallFlying() && !player.isUsingItem() && player.hurtTime == 0
			&& state(player).hurtCooldown == 0 && player.containerMenu == player.inventoryMenu
			&& player.inventoryMenu.getCarried().isEmpty();
	}

	public static void request(ServerPlayer player, ArmorLoadoutPayload payload) {
		var state = state(player);
		if (payload.revision() != state.revision || payload.stateId() != player.inventoryMenu.getStateId()) {
			player.inventoryMenu.broadcastFullState();
			sync(player);
			return;
		}
		if (payload.target() < 0 || payload.target() >= state.unlocked || payload.target() == state.active || state.pending >= 0) return;
		if (!safe(player) || !canSwap(player, payload.target())) {
			player.sendOverlayMessage(Component.translatable("message.darkspawn.loadout_unsafe"));
			return;
		}
		state.pending = payload.target();
		state.remaining = SWITCH_TICKS;
		state.menuRevision = payload.stateId();
		state.revision++;
		sync(player);
		player.sendOverlayMessage(Component.translatable("message.darkspawn.loadout_changing", payload.target() + 1));
	}

	private static boolean canSwap(ServerPlayer player, int target) {
		for (int i = 0; i < 4; i++) {
			var slot = player.inventoryMenu.getSlot(5 + i);
			ItemStack incoming = state(player).stored(target, i);
			if (!slot.isActive() || !slot.mayPickup(player) || !incoming.isEmpty() && (!slot.mayPlace(incoming) || incoming.getCount() != 1)) return false;
		}
		return true;
	}

	public static void tick(ServerPlayer player) {
		var state = state(player);
		if (state.hurtCooldown > 0) state.hurtCooldown--;
		if (state.pending < 0) return;
		if (!safe(player) || state.menuRevision != player.inventoryMenu.getStateId() || !canSwap(player, state.pending)) {
			cancel(player);
			return;
		}
		if (--state.remaining > 0) return;
		int target = state.pending;
		// Atomic Swap: All four slots are validated before moving any stack; inactive sets never equip effects.
		for (int i = 0; i < 4; i++) {
			var slot = player.inventoryMenu.getSlot(5 + i);
			ItemStack incoming = state.stored.set(target * 4 + i, ItemStack.EMPTY);
			state.stored.set(state.active * 4 + i, slot.getItem());
			slot.setByPlayer(incoming);
		}
		state.active = target;
		darkspawn.black.equipment.ArmorStyleBonuses.update(player);
		darkspawn.black.equipment.LegendaryArmor.update(player);
		state.pending = -1;
		state.revision++;
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastFullState();
		sync(player);
		player.sendOverlayMessage(Component.translatable("message.darkspawn.loadout_equipped", target + 1));
	}

	public static void cancel(ServerPlayer player) {
		var state = state(player);
		if (state.pending < 0) return;
		state.pending = -1;
		state.remaining = 0;
		state.revision++;
		sync(player);
		player.sendOverlayMessage(Component.translatable("message.darkspawn.loadout_cancelled"));
	}

	public static void dropStored(Player player, ServerLevel level) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
		var state = state(player);
		// Death Ownership: Match vanilla vanishing rules and clear before dropping to prevent repeat rewards.
		for (int i = 0; i < state.stored.size(); i++) {
			ItemStack stack = state.stored.set(i, ItemStack.EMPTY);
			if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				var drop = player.createItemStackToDrop(stack, true, false);
				if (drop != null) level.addFreshEntity(drop);
			}
		}
		state.pending = -1;
		state.revision++;
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ArmorLoadoutSyncPayload.TYPE)) {
			var state = state(player);
			ServerPlayNetworking.send(player, new ArmorLoadoutSyncPayload(state.unlocked, state.active, state.revision, state.pending));
		}
	}
}
