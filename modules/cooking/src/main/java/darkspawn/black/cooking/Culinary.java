package darkspawn.black.cooking;

import darkspawn.black.core.DarkspawnCore;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Culinary {
	private Culinary() {}
	public static CulinaryProgress progress(Player player) { return ((CulinaryPlayer) player).darkspawn$culinary(); }
	public static void award(ServerPlayer player, String path, String criterion) {
		darkspawn.black.core.Progress.award(player, path, criterion);
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
	public static void initialize() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			if (damage > 0 && !blocked && entity.hasEffect(MealEffects.HUNTER)
					&& source.getEntity() instanceof net.minecraft.world.entity.monster.Monster attacker) {
				attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 60, 0));
			}
		});
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, damage) ->
			!source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)
				|| !entity.hasEffect(MealEffects.WARMING));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			var old = progress(oldPlayer);
			progress(newPlayer).restore(alive ? old.recent() : java.util.List.of(), old.meals(), old.recipes());
		});
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
