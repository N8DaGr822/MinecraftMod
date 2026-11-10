package darkspawn.black.mixin;

import darkspawn.black.pirate.ShipCollision;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ShipClimbingMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void darkspawn$shipLadder(CallbackInfoReturnable<Boolean> callback) {
        var entity = (LivingEntity)(Object)this;
        if (!callback.getReturnValue() && !entity.isSpectator() && ShipCollision.climbable(entity)) callback.setReturnValue(true);
    }
}
