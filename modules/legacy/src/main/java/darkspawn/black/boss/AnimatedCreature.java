package darkspawn.black.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Shared asset contract for ecosystem mobs and summoned creatures; each entity owns its animation cache. */
public interface AnimatedCreature extends GeoEntity {
	default String assetId() { return BuiltInRegistries.ENTITY_TYPE.getKey(((Entity)this).getType()).getPath(); }
	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		String id = assetId();
		if (CreatureAssets.MOBS.contains(id) || CreatureAssets.MINIONS.contains(id)) { CreatureAnimations.register(id, controllers); }
	}
	default void animateAttack() {
		if (CreatureAssets.MOBS.contains(assetId()) || CreatureAssets.MINIONS.contains(assetId())) { triggerAnim("action", "attack"); }
	}
}
