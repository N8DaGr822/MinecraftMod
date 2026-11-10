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
	public static final Holder<MobEffect> SUSTAINED = register("sustained_meal", new MealEffect(0xC99645));
	public static final Holder<MobEffect> HUNTER = register("hunter_meal", new MealEffect(0xAE7547)
		.addAttributeModifier(Attributes.MOVEMENT_SPEED, Darkspawn.id("meal_hunter"), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final Holder<MobEffect> SUREFOOTED = register("surefooted_meal", new MealEffect(0xA6A69C)
		.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Darkspawn.id("meal_footing"), 0.3, AttributeModifier.Operation.ADD_VALUE)
		.addAttributeModifier(Attributes.FALL_DAMAGE_MULTIPLIER, Darkspawn.id("meal_fall"), -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final Holder<MobEffect> IRON_STOMACH = register("iron_stomach_meal", new MealEffect(0x97AD56));
	public static final Holder<MobEffect> DEEP_BREATH = register("deep_breath_meal", new MealEffect(0x4DA8B7));
	public static final Holder<MobEffect> ENERGIZED = register("energized_meal", new MealEffect(0xEED65F)
		.addAttributeModifier(Attributes.MOVEMENT_SPEED, Darkspawn.id("meal_energy"), 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final Holder<MobEffect> MINER = register("miner_meal", new MealEffect(0xD9BD71)
		.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Darkspawn.id("meal_mining"), 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final Holder<MobEffect> FISHER = register("fisher_meal", new MealEffect(0x62C6AF)
		.addAttributeModifier(Attributes.LUCK, Darkspawn.id("meal_luck"), 1, AttributeModifier.Operation.ADD_VALUE));
	public static final Holder<MobEffect> WARMING = register("warming_meal", new MealEffect(0xEA9162));
	public static final Holder<MobEffect> FIREPROOF = register("fireproof_meal", new MealEffect(0xDB653D));
	public static final Holder<MobEffect> FLOATING = register("floating_meal", new MealEffect(0xB699DE));
	public static final Holder<MobEffect> FEAST = register("feast_meal", new MealEffect(0xDCA963)
		.addAttributeModifier(Attributes.ATTACK_DAMAGE, Darkspawn.id("feast_strength"), 3, AttributeModifier.Operation.ADD_VALUE)
		.addAttributeModifier(Attributes.MOVEMENT_SPEED, Darkspawn.id("feast_speed"), 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	public static final List<Holder<MobEffect>> ALL = List.of(HERBAL, SWEET, SAVORY, SEAFOOD, HEARTY,
		SUSTAINED, HUNTER, SUREFOOTED, IRON_STOMACH, DEEP_BREATH, ENERGIZED, MINER, FISHER, WARMING, FIREPROOF, FLOATING, FEAST);

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
				: requested.equals(MobEffects.WATER_BREATHING) ? SEAFOOD
				: requested.equals(MobEffects.FIRE_RESISTANCE) ? FIREPROOF
				: requested.equals(MobEffects.SLOW_FALLING) ? FLOATING : null;
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
