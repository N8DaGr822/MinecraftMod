package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class AscensionBucketMixin {
    @Inject(method="emptyContents",at=@At("HEAD"),cancellable=true)
    private void darkspawn$claimFluid(LivingEntity user,Level level,BlockPos pos,BlockHitResult hit,CallbackInfoReturnable<Boolean> ci) {
        if(user instanceof Player player&&!AscensionProtection.canFluid(player,pos))ci.setReturnValue(false);
    }
}
