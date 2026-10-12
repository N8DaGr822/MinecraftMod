package darkspawn.black.mixin;

import darkspawn.black.oneblock.OneBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class OneBlockEntityTravelMixin {
	@Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
	private void darkspawn$containEntities(TeleportTransition transition, CallbackInfoReturnable<Entity> ci) {
		// Transfer Boundary: Items, projectiles, vehicles, and pets cannot carry possessions across profiles.
		if (OneBlock.island(((Entity) (Object) this).level()) != OneBlock.island(transition.newLevel())) ci.setReturnValue(null);
	}
}
