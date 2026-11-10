package darkspawn.black.mixin;

import darkspawn.black.bonfire.BonfireWards;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class BonfireMovementMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void darkspawn$wardMovement(MoverType type, Vec3 movement, CallbackInfo callback) {
		if (BonfireWards.blocksMove((Entity) (Object) this, movement)) callback.cancel();
	}
}
