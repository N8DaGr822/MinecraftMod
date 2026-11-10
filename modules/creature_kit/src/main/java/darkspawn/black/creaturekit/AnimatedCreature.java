package darkspawn.black.creaturekit;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/**
 * Shared asset contract for GeckoLib creatures; each entity owns its animation cache. Rigs animate only after their
 * owner registers the asset id with {@link CreatureAssets}. The render hooks feed {@code CreatureGeoRenderer}.
 */
public interface AnimatedCreature extends GeoEntity {
	int SHELL_NONE = 0, SHELL_EGG = 1, SHELL_CAGE = 2;
	default String assetId() { return BuiltInRegistries.ENTITY_TYPE.getKey(((Entity)this).getType()).getPath(); }
	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		String id = assetId();
		if (CreatureAssets.animated(id)) { CreatureAnimations.register(id, controllers); }
	}
	default void animateAttack() {
		if (CreatureAssets.animated(assetId())) { triggerAnim("action", "attack"); }
	}
	/** Ticks left on a committed telegraph (warning pose and warm tint); 0 when idle. */
	default int renderWarning() { return 0; }
	default boolean renderActive() { return false; }
	default boolean renderHiding() { return false; }
	/** Alternate geometry shown instead of the body: {@link #SHELL_NONE}, {@link #SHELL_EGG} or {@link #SHELL_CAGE}. */
	default int renderShell() { return SHELL_NONE; }
	/** Rotation in radians for an optional {@code crest} bone. */
	default float renderCrest(float partialTicks) { return 0; }
	default int renderTint() { return 0xFFFFFFFF; }
}
