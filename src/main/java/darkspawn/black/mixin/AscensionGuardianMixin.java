package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionGuardian;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class AscensionGuardianMixin {
    @Inject(method="canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",at=@At("HEAD"),cancellable=true)
    private void darkspawn$guardianTarget(LivingEntity target,CallbackInfoReturnable<Boolean> ci){if(AscensionGuardian.disallows((Mob)(Object)this,target))ci.setReturnValue(false);}
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity darkspawn$guardianTarget(LivingEntity target){return target!=null&&AscensionGuardian.disallows((Mob)(Object)this,target)?null:target;}
}
