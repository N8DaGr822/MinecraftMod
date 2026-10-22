package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({net.minecraft.world.level.material.FlowingFluid.class,net.minecraft.world.level.material.LavaFluid.class})
public abstract class AscensionFlowMixin {
    @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
    private void darkspawn$containFluid(net.minecraft.world.level.LevelAccessor access,BlockPos pos,BlockState state,Direction direction,net.minecraft.world.level.material.FluidState fluid,CallbackInfo ci) {
        if(access instanceof Level level&&!AscensionProtection.sameClaim(level,pos.relative(direction.getOpposite()),pos))ci.cancel();
    }
}
