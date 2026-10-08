package darkspawn.black.cooking;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MealEffects {
	public static final Holder<MobEffect> HERBAL = register("herbal_meal", new MealEffect(0x8EBE65));
	public static final Holder<MobEffect> SWEET = register("sweet_meal", new MealEffect(0xE5AB42) {
		@Override public boolean shouldApplyEffectTickThisTick(int ticks, int amplifier) { return ticks % 50 == 0; }
		@Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
			if (entity.getHealth() < entity.getMaxHealth()) {
				entity.heal(1.0F);
			}
			return true;
		}
	});
	public static final Holder<MobEffect> SAVORY = register("savory_meal", new MealEffect(0xB76B48)
			.addAttributeModifier(Attributes.ATTACK_DAMAGE, Darkspawn.id("meal_strength"), 3, AttributeModifier.Operation.ADD_VALUE));
	public static final Holder<MobEffect> SEAFOOD = register("seafood_meal", new MealEffect(0x58B4BC));
	public static final Holder<MobEffect> HEARTY = register("hearty_meal", new MealEffect(0xE7CF65) {
		@Override public void onEffectStarted(LivingEntity entity, int amplifier) {
			// Meal Absorption: Add two golden hearts, retaining absorption supplied by other systems.
			entity.setAbsorptionAmount(entity.getAbsorptionAmount() + 4);
		}
	}.addAttributeModifier(Attributes.MAX_ABSORPTION, Darkspawn.id("meal_absorption"), 4, AttributeModifier.Operation.ADD_VALUE));
	public static final List<Holder<MobEffect>> ALL = List.of(HERBAL, SWEET, SAVORY, SEAFOOD, HEARTY);

	private MealEffects() {
	}

	private static class MealEffect extends MobEffect {
		private MealEffect(int color) { super(MobEffectCategory.BENEFICIAL, color); }
	}

	private static Holder<MobEffect> register(String name, MobEffect effect) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Darkspawn.id(name), effect);
	}

	public static void apply(LivingEntity entity, Holder<MobEffect> effect, int ticks) {
		// Meal Ownership: Vanilla effect storage supplies saving, synchronization, expiration, and milk removal.
		// Replace only meal effects, leaving potions and boss powers untouched.
		for (Holder<MobEffect> meal : ALL) {
			entity.removeEffect(meal);
		}
		entity.addEffect(new MobEffectInstance(effect, ticks, 0));
	}

	public static MobEffectInstance utilityEffect(LivingEntity entity, Holder<MobEffect> requested, MobEffectInstance potion) {
		Holder<MobEffect> meal = requested.equals(MobEffects.NIGHT_VISION) ? HERBAL
				: requested.equals(MobEffects.WATER_BREATHING) ? SEAFOOD : null;
		if (meal == null) {
			return potion;
		}
		MobEffectInstance food = entity.getActiveEffectsMap().get(meal);
		if (food == null || potion != null && (potion.isInfiniteDuration() || !food.isInfiniteDuration() && potion.getDuration() >= food.getDuration())) {
			return potion;
		}
		return food;
	}
}
