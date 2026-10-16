package darkspawn.black.mixin;

import darkspawn.black.legacy.NoahLegacy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class NoahSeaPeaceMixin {
    @Inject(method="canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",at=@At("HEAD"),cancellable=true)
    private void darkspawn$seaPeace(LivingEntity target,CallbackInfoReturnable<Boolean> callback) {
        if(NoahLegacy.seaProtected((Mob)(Object)this,target))callback.setReturnValue(false);
    }
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity darkspawn$seaTarget(LivingEntity target) {
        return target!=null&&NoahLegacy.seaProtected((Mob)(Object)this,target)?null:target;
    }
}
