package darkspawn.black.boss;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import java.util.EnumMap;
import java.util.Locale;

/** Presentation only: the existing server timers continue to own every attack and damage window. */
public final class BiomeBossAnimations {
	public static final DataTicket<Float> DEATH_TIME = DataTickets.create("darkspawn_biome_death_time", Float.class);
	public static final DataTicket<Integer> AWAKENING = DataTickets.create("darkspawn_biome_awakening", Integer.class);
	public static final DataTicket<Float> AGE = DataTickets.create("darkspawn_biome_age", Float.class);
	private BiomeBossAnimations() { }

	public static void register(BossProfile profile, AnimatableManager.ControllerRegistrar controllers) {
		String prefix = "animation." + profile.id() + ".";
		var idle = RawAnimation.begin().thenLoop(prefix + "idle");
		var walk = RawAnimation.begin().thenLoop(prefix + "walk");
		var wrath = RawAnimation.begin().thenLoop(prefix + "wrath_idle");
		var charge = RawAnimation.begin().thenLoop(prefix + "charge");
		var recovery = RawAnimation.begin().thenLoop(prefix + "recovery");
		var death = RawAnimation.begin().thenPlayAndHold(prefix + "death");
		var awaken = RawAnimation.begin().thenPlayAndHold(prefix + "awaken");
		var warnings = new EnumMap<BossAttack, RawAnimation>(BossAttack.class);
		for (var attack : BossAttack.values()) { warnings.put(attack, RawAnimation.begin().thenPlayAndHold(prefix + "windup_" + attack.name().toLowerCase(Locale.ROOT))); }
		controllers.add(new AnimationController<BiomeBoss>("movement", 3, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)
					|| test.getDataOrDefault(AWAKENING, 0) > 0
					|| test.getDataOrDefault(BiomeBoss.ANIMATION_WINDUP, 0) > 0
					|| test.getDataOrDefault(BiomeBoss.ANIMATION_RECOVERY, 0) > 0
					|| test.getDataOrDefault(BiomeBoss.ANIMATION_CHARGING, false)) { return PlayState.STOP; }
			return test.setAndContinue(test.isMoving() ? walk : test.getDataOrDefault(BiomeBoss.ANIMATION_PHASE, 1) == 3 ? wrath : idle);
		}));
		var action = new AnimationController<BiomeBoss>("action", 0, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) { return PlayState.STOP; }
			if (test.getDataOrDefault(AWAKENING, 0) > 0) {
				test.setAnimation(awaken);
				test.controller().setAnimationTime((100 - test.getDataOrDefault(AWAKENING, 0)) / 20.0);
				return PlayState.CONTINUE;
			}
			if (test.getDataOrDefault(BiomeBoss.ANIMATION_CHARGING, false)) { return test.setAndContinue(charge); }
			int windup = test.getDataOrDefault(BiomeBoss.ANIMATION_WINDUP, 0);
			if (windup > 0) {
				int ordinal = Math.clamp(test.getDataOrDefault(BiomeBoss.ANIMATION_ATTACK, 0), 0, BossAttack.values().length - 1);
				test.setAnimation(warnings.get(BossAttack.values()[ordinal]));
				// Warning Clock: Phase-three Queen warnings last 35 ticks; all authored windups span two seconds.
				int duration = profile == BossProfile.SHADOW_CREEPER_QUEEN && test.getDataOrDefault(BiomeBoss.ANIMATION_PHASE, 1) == 3 ? 35 : 40;
				test.controller().setAnimationTime((duration - windup) * 2.0 / duration);
				return PlayState.CONTINUE;
			}
			return test.getDataOrDefault(BiomeBoss.ANIMATION_RECOVERY, 0) > 0 ? test.setAndContinue(recovery) : PlayState.STOP;
		});
		for (var attack : BossAttack.values()) {
			String name = attack.name().toLowerCase(Locale.ROOT);
			action.triggerableAnim(name, RawAnimation.begin().thenPlay(prefix + name));
		}
		action.triggerableAnim("phase_change", RawAnimation.begin().thenPlay(prefix + "phase_change"));
		controllers.add(action);
		controllers.add(new AnimationController<BiomeBoss>("death", 0, test -> {
			if (!test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) { return PlayState.STOP; }
			test.setAnimation(death);
			test.controller().setAnimationTime(Math.clamp(test.getDataOrDefault(DEATH_TIME, 0F), 0, 1));
			return PlayState.CONTINUE;
		}));
	}
}
