package darkspawn.black.boss;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.world.entity.Mob;

public final class CreatureAnimations {
	public static final DataTicket<Integer> WARNING = DataTickets.create("darkspawn_creature_warning", Integer.class);
	public static final DataTicket<Boolean> ACTIVE = DataTickets.create("darkspawn_creature_active", Boolean.class);
	public static final DataTicket<Boolean> HIDING = DataTickets.create("darkspawn_creature_hiding", Boolean.class);
	public static final DataTicket<Integer> ROLE = DataTickets.create("darkspawn_creature_role", Integer.class);
	public static final DataTicket<Float> DEATH_TIME = DataTickets.create("darkspawn_creature_death", Float.class);
	public static final DataTicket<Float> SHIELD_ANGLE = DataTickets.create("darkspawn_creature_shield", Float.class);
	private CreatureAnimations() { }

	public static <T extends Mob & AnimatedCreature> void register(String id, AnimatableManager.ControllerRegistrar controllers) {
		String prefix = "animation." + id + ".";
		var idle = RawAnimation.begin().thenLoop(prefix + "idle");
		var walk = RawAnimation.begin().thenLoop(prefix + "walk");
		var warning = RawAnimation.begin().thenLoop(prefix + "warning");
		var active = RawAnimation.begin().thenLoop(prefix + "active");
		var hide = RawAnimation.begin().thenPlayAndHold(prefix + "hide");
		var death = RawAnimation.begin().thenPlayAndHold(prefix + "death");
		var attack = RawAnimation.begin().thenPlay(prefix + "attack");
		controllers.add(new AnimationController<T>("movement", 3, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false) || test.getDataOrDefault(WARNING, 0) > 0
					|| test.getDataOrDefault(ACTIVE, false) || test.getDataOrDefault(HIDING, false)) { return PlayState.STOP; }
			return test.setAndContinue(test.isMoving() ? walk : idle);
		}));
		controllers.add(DefaultAnimations.<T>genericAttackAnimation(attack).setTransitionTicks(0));
		controllers.add(new AnimationController<T>("action", 0, test -> PlayState.STOP).triggerableAnim("attack", attack));
		// State Priority: Full death/hide/warning poses override swings and triggered clips without changing AI timers.
		controllers.add(new AnimationController<T>("state", 0, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
				test.setAnimation(death); test.controller().setAnimationTime(Math.clamp(test.getDataOrDefault(DEATH_TIME, 0F), 0, 1));
				return PlayState.CONTINUE;
			}
			if (test.getDataOrDefault(HIDING, false)) { return test.setAndContinue(hide); }
			if (test.getDataOrDefault(WARNING, 0) > 0) { return test.setAndContinue(warning); }
			return test.getDataOrDefault(ACTIVE, false) ? test.setAndContinue(active) : PlayState.STOP;
		}));
	}
}
