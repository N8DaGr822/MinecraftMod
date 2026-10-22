package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({net.minecraft.world.level.block.DispenserBlock.class,net.minecraft.world.level.block.DropperBlock.class})
public abstract class AscensionDispenserMixin {
    @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true)
    private void darkspawn$boundOutput(ServerLevel level,BlockState state,BlockPos pos,CallbackInfo ci) {
        if(!AscensionProtection.canOperate(level,pos,pos.relative(state.getValue(net.minecraft.world.level.block.DispenserBlock.FACING))))ci.cancel();
    }
}
