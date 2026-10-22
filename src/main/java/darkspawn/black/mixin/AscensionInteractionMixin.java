package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import darkspawn.black.oneblock.AscensionGenerator;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ServerPlayerGameMode.class,priority=1100)
public abstract class AscensionInteractionMixin {
    @Shadow protected ServerPlayer player;
    @WrapMethod(method="destroyBlock")
    private boolean darkspawn$generatorBreak(BlockPos pos,Operation<Boolean> original) {
        return AscensionGenerator.destroyBlock(player,pos,()->original.call(pos));
    }
    @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
    private void darkspawn$claimBreak(BlockPos pos,CallbackInfoReturnable<Boolean> ci) {
        if(!AscensionProtection.canEdit(player,pos))ci.setReturnValue(false);
    }
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void darkspawn$claimUse(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci) {
        if(!AscensionProtection.canEdit(player,hit.getBlockPos())
            ||darkspawn.black.oneblock.Ascension.island(level)&&darkspawn.black.oneblock.AscensionLedger.get(player.level()).at(hit.getBlockPos())==null
                &&!stack.isEmpty()&&!(stack.getItem() instanceof net.minecraft.world.item.BlockItem))ci.setReturnValue(InteractionResult.FAIL);
    }
}
