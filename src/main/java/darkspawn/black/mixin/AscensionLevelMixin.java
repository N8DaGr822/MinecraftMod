package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class AscensionLevelMixin {
    @Inject(method="mayInteract",at=@At("HEAD"),cancellable=true)
    private void darkspawn$claimAccess(Entity entity,BlockPos pos,CallbackInfoReturnable<Boolean> ci) {
        if(entity instanceof Player player&&!AscensionProtection.canEdit(player,pos))ci.setReturnValue(false);
    }
    @Inject(method="canSpreadFireAround",at=@At("HEAD"),cancellable=true)
    private void darkspawn$noFireSpread(BlockPos pos,CallbackInfoReturnable<Boolean> ci) {
        if(darkspawn.black.oneblock.Ascension.island((ServerLevel)(Object)this))ci.setReturnValue(false);
    }

}
