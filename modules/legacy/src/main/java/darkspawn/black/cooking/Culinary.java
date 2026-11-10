package darkspawn.black.cooking;

import darkspawn.black.Darkspawn;
import darkspawn.black.health.BossHearts;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Culinary {
	private Culinary() {}
	public static CulinaryProgress progress(Player player) { return ((CulinaryPlayer) player).darkspawn$culinary(); }
	public static void award(ServerPlayer player, String path, String criterion) {
		if (!path.equals("root")) {
			var end = player.level().getServer().getLevel(net.minecraft.world.level.Level.END);
			if (end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon()) award(player, "root", "dragon");
		}
		var advancement = player.level().getServer().getAdvancements().get(Darkspawn.id("progress/" + path));
		if (advancement != null) player.getAdvancements().award(advancement, criterion);
	}
	public static void eaten(ServerPlayer player, ItemStack stack) {
		var food = stack.get(DataComponents.FOOD);
		if (food == null) return;
		var progress = progress(player);
		progress.eat(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getItem() instanceof MealItem);
		player.getFoodData().setSaturation(Math.min(player.getFoodData().getFoodLevel(),
			player.getFoodData().getSaturationLevel() + food.saturation() * progress.saturationBonus()));
		if (progress.variety() >= 3) award(player, "varied_diet", "earned");
		if (progress.meals().size() >= 20) {
			award(player, "well_rounded", "earned");
			progress.unlock("hero_feast");
		}
	}
	public static void hearts(ServerPlayer player) {
		var hearts = BossHearts.progress(player).savedBosses();
		for (String boss : hearts) award(player, "heart_" + boss, "earned");
		if (hearts.size() >= 8) award(player, "heart_collector", "earned");
		if (hearts.size() >= 15) award(player, "beyond_mortal", "earned");
	}
	public static void initialize() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			if (damage > 0 && !blocked && entity.hasEffect(MealEffects.HUNTER)
					&& source.getEntity() instanceof net.minecraft.world.entity.monster.Monster attacker) {
				attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 60, 0));
			}
		});
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, damage) ->
			!source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)
				|| !(entity.hasEffect(MealEffects.WARMING) || darkspawn.black.boss.EquipmentPowers.wears(entity, "armor_winter")));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			var old = progress(oldPlayer);
			progress(newPlayer).restore(alive ? old.recent() : java.util.List.of(), old.meals(), old.recipes());
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> hearts(handler.player));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.hasEffect(MealEffects.WARMING)) player.setTicksFrozen(0);
			}
			if (server.getTickCount() % 100 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive() && !player.isSpectator() && progress(player).variety() >= 8
						&& player.getFoodData().getFoodLevel() == 20 && player.getHealth() < player.getMaxHealth()) {
					player.heal(1);
				}
			}
		});
	}
}
