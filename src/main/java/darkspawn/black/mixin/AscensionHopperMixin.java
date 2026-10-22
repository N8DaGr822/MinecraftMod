package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.level.block.entity.HopperBlockEntity.class)
public abstract class AscensionHopperMixin {
    @Inject(method="tryMoveItems",at=@At("HEAD"),cancellable=true)
    private static void darkspawn$boundTransfer(Level level,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.HopperBlockEntity hopper,java.util.function.BooleanSupplier action,CallbackInfoReturnable<Boolean> ci) {
        if(!AscensionProtection.canOperate(level,pos,pos.relative(state.getValue(net.minecraft.world.level.block.HopperBlock.FACING))))ci.setReturnValue(false);
    }
    @Inject(method="suckInItems",at=@At("HEAD"),cancellable=true)
    private static void darkspawn$boundIntake(Level level,net.minecraft.world.level.block.entity.Hopper hopper,CallbackInfoReturnable<Boolean> ci) {
        var pos=BlockPos.containing(hopper.getLevelX(),hopper.getLevelY(),hopper.getLevelZ());
        if(!AscensionProtection.canOperate(level,pos,pos.above()))ci.setReturnValue(false);
    }
}
