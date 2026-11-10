package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class EcosystemEffects {
	public static final Holder<MobEffect> FADING_SOUL = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			Darkspawn.id("fading_soul"), new FadingSoul().addAttributeModifier(Attributes.MAX_HEALTH,
					Darkspawn.id("fading_soul"), -1, AttributeModifier.Operation.ADD_VALUE));
	private EcosystemEffects() { }
	private static final class FadingSoul extends MobEffect {
		private FadingSoul() { super(MobEffectCategory.HARMFUL, 0x7EDCDA); }
	}
	public static void initialize() { }
	static void fracture(LivingEntity target, boolean keeper) {
		// Soul Preview: Nonstacking, five-second loss of at most one heart; permanent progress and boss theft remain separate.
		if (!target.hasEffect(FADING_SOUL) && target.getMaxHealth() >= (keeper ? 4 : 3)) {
			target.addEffect(new MobEffectInstance(FADING_SOUL, 100, keeper ? 1 : 0));
			target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
		}
	}
}
