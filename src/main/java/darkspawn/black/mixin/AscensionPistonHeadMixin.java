package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.level.block.piston.PistonBaseBlock.class)
public abstract class AscensionPistonHeadMixin {
    // Retraction Safety: Vanilla removes the old head before asking the movement resolver.
    @Inject(method="triggerEvent",at=@At("HEAD"),cancellable=true)
    private void darkspawn$boundHead(BlockState state,Level level,BlockPos pos,int event,int direction,CallbackInfoReturnable<Boolean> ci) {
        if(!AscensionProtection.canOperate(level,pos,pos.relative(state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING))))ci.setReturnValue(false);
    }
}
