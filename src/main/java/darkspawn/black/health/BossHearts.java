package darkspawn.black.health;

import darkspawn.black.Darkspawn;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class BossHearts {
	private BossHearts() {
	}

	public static BossHeartProgress progress(Player player) {
		return ((BossHeartPlayer) player).darkspawn$bossHearts();
	}

	public static void apply(Player player) {
		BossHeartProgress progress = progress(player);
		setBonus(player.getAttribute(Attributes.MAX_HEALTH), "boss_health", progress.bonusHealth());
		setBonus(player.getAttribute(Attributes.MAX_ABSORPTION), "boss_absorption", progress.goldenCapacity());
	}

	private static void setBonus(AttributeInstance attribute, String name, int amount) {
		var id = Darkspawn.id(name);
		var existing = attribute.getModifier(id);
		if (existing != null && existing.amount() == amount) {
			return;
		}
		// Health Ownership: Change only this mod's modifier, preserving potions and other mods.
		if (amount == 0) {
			attribute.removeModifier(id);
		} else {
			attribute.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	public static void initialize() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			apply(handler.player);
			progress(handler.player).damaged();
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			progress(newPlayer).restore(progress(oldPlayer).savedBosses());
			apply(newPlayer);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			apply(newPlayer);
			if (!alive) {
				newPlayer.setHealth(newPlayer.getMaxHealth());
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player && (damageTaken > 0 || !blocked && baseDamage > 0)) {
				progress(player).damaged();
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!player.isAlive() || player.isSpectator()) {
					continue;
				}
				BossHeartProgress progress = progress(player);
				// Absorption Refill: Restore only earned capacity; never erase stronger golden-apple absorption.
				if (progress.tickRefill() && player.getAbsorptionAmount() < progress.goldenCapacity()) {
					player.setAbsorptionAmount(progress.goldenCapacity());
				}
			}
		});
	}
}
