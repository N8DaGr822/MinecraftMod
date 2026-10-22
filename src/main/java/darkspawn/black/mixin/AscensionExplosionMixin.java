package darkspawn.black.mixin;

import darkspawn.black.oneblock.Ascension;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.level.ServerExplosion.class)
public abstract class AscensionExplosionMixin {
    // Terrain Safety: Explosions retain entity effects, but cannot destroy or trigger Ascension structures.
    @Inject(method={"interactsWithBlocks","canTriggerBlocks","shouldAffectBlocklikeEntities"},at=@At("HEAD"),cancellable=true)
    private void darkspawn$protectTerrain(CallbackInfoReturnable<Boolean> ci) {
        if(Ascension.island(((net.minecraft.world.level.ServerExplosion)(Object)this).level()))ci.setReturnValue(false);
    }
    @Inject(method="createFire",at=@At("HEAD"),cancellable=true)
    private void darkspawn$preventBlastFire(java.util.List<BlockPos> blocks,CallbackInfo ci) {
        if(Ascension.island(((net.minecraft.world.level.ServerExplosion)(Object)this).level()))ci.cancel();
    }
}
