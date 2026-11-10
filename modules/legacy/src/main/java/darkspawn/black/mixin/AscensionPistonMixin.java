package darkspawn.black.mixin;

import darkspawn.black.oneblock.Ascension;
import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.level.block.piston.PistonStructureResolver.class)
public abstract class AscensionPistonMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private BlockPos pistonPos;
    @Shadow @Final private Direction pistonDirection;
    @Inject(method="resolve",at=@At("RETURN"),cancellable=true)
    private void darkspawn$boundMovement(CallbackInfoReturnable<Boolean> ci) {
        if(!ci.getReturnValue()||!Ascension.island(level))return;
        var resolver=(net.minecraft.world.level.block.piston.PistonStructureResolver)(Object)this;
        if(!AscensionProtection.canOperate(level,pistonPos,pistonPos.relative(pistonDirection))){ci.setReturnValue(false);return;}
        for(var pos:resolver.getToPush())if(!AscensionProtection.canOperate(level,pistonPos,pos)||!AscensionProtection.canOperate(level,pistonPos,pos.relative(resolver.getPushDirection()))){ci.setReturnValue(false);return;}
        for(var pos:resolver.getToDestroy())if(!AscensionProtection.canOperate(level,pistonPos,pos)){ci.setReturnValue(false);return;}
    }
}
