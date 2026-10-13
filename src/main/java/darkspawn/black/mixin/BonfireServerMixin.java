package darkspawn.black.mixin;

import darkspawn.black.bonfire.BonfireWards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class BonfireServerMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void darkspawn$wardSpawn(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (BonfireWards.blocksSpawn((ServerLevel) (Object) this, entity)) callback.setReturnValue(false);
	}
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void darkspawn$clearBeforeHostileTick(Entity entity, CallbackInfo callback) {
		if (BonfireWards.guard(entity)) callback.cancel();
	}
	@Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
	private void darkspawn$clearHostilePassenger(Entity vehicle, Entity passenger, CallbackInfo callback) {
		if (BonfireWards.guard(passenger)) callback.cancel();
	}
}
