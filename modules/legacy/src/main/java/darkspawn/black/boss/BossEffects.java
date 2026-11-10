package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class BossEffects {
	public static final Holder<MobEffect> SOUL_FRACTURE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			Darkspawn.id("soul_fracture"), new AttributeEffect(0x69DDDD)
					.addAttributeModifier(Attributes.MAX_HEALTH, Darkspawn.id("soul_fracture"), -2, AttributeModifier.Operation.ADD_VALUE));
	public static final Holder<MobEffect> FRACTURED_ARMOR = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			Darkspawn.id("fractured_armor"), new AttributeEffect(0xDDD1AA)
					.addAttributeModifier(Attributes.ARMOR, Darkspawn.id("fractured_armor"), -4, AttributeModifier.Operation.ADD_VALUE));

	private BossEffects() { }
	private static final class AttributeEffect extends MobEffect {
		private AttributeEffect(int color) { super(MobEffectCategory.HARMFUL, color); }
	}
	public static void initialize() { }

	static void apply(LivingEntity target, BossBolt.Kind kind) {
		switch (kind) {
			case POISON, SPORE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
			case HEX -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
			case FROST -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
			case INK -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
			case WEB -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 4));
			case ACID -> target.addEffect(new MobEffectInstance(FRACTURED_ARMOR, 100, 0));
			case FIRE -> target.igniteForSeconds(4);
			case SOUL -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			case VOID -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
			default -> { }
		}
	}

	public static int soulHeartsToSteal(float currentMaximum, int existingHearts) {
		// Soul Safety: A strike steals one heart, up to five, and always leaves at least one usable heart.
		return currentMaximum >= 4 ? Math.min(5, existingHearts + 1) : existingHearts;
	}

	static boolean stealHeart(LivingEntity target) {
		var old = target.getEffect(SOUL_FRACTURE);
		int oldHearts = old == null ? 0 : old.getAmplifier() + 1;
		int hearts = soulHeartsToSteal(target.getMaxHealth(), oldHearts);
		if (hearts == oldHearts) { return false; }
		target.addEffect(new MobEffectInstance(SOUL_FRACTURE, 600, hearts - 1));
		target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
		return true;
	}

	static void returnHeart(LivingEntity target) {
		var old = target.getEffect(SOUL_FRACTURE);
		if (old == null) { return; }
		target.removeEffect(SOUL_FRACTURE);
		if (old.getAmplifier() > 0) {
			target.addEffect(new MobEffectInstance(SOUL_FRACTURE, old.getDuration(), old.getAmplifier() - 1));
		}
	}
}
