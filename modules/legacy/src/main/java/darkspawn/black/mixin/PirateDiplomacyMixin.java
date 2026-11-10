package darkspawn.black.mixin;

import darkspawn.black.pirate.PirateDiplomacy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PirateDiplomacyMixin {
    @Inject(method="checkDespawn",at=@At("HEAD"),cancellable=true)
    private void darkspawn$keepStationedCrew(CallbackInfo callback) {
        var mob=(Mob)(Object)this;
        if(PirateDiplomacy.contact(mob)||darkspawn.black.pirate.PirateShip.loyalCrew(mob))callback.cancel();
    }
    // Faction Targets: Veto native goals and Brain sensors only for tagged expedition NPCs.
    @Inject(method="canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",at=@At("HEAD"),cancellable=true)
    private void darkspawn$canAttack(LivingEntity target,CallbackInfoReturnable<Boolean> callback) {
        if(PirateDiplomacy.disallows((Mob)(Object)this,target))callback.setReturnValue(false);
    }
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity darkspawn$target(LivingEntity target) {
        return target!=null&&PirateDiplomacy.disallows((Mob)(Object)this,target)?null:target;
    }
}
