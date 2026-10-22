package darkspawn.black.mixin;

import darkspawn.black.oneblock.AscensionProtection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class AscensionPlacementMixin {
    @Inject(method="place",at=@At("HEAD"),cancellable=true)
    private void darkspawn$claimPlace(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci) {
        if(context.getPlayer()!=null&&(!AscensionProtection.canPlace(context.getPlayer(),context.getClickedPos())
            ||((BlockItem)(Object)this).getBlock() instanceof net.minecraft.world.level.block.BedBlock&&!AscensionProtection.canPlace(context.getPlayer(),context.getClickedPos().relative(context.getHorizontalDirection()))))ci.setReturnValue(InteractionResult.FAIL);
    }
    // Redirected Placement: Scaffolding may resolve a different target after the initial click.
    @Inject(method="getPlacementState",at=@At("RETURN"),cancellable=true)
    private void darkspawn$resolvedClaim(BlockPlaceContext context,CallbackInfoReturnable<net.minecraft.world.level.block.state.BlockState> ci) {
        if(context.getPlayer()!=null&&ci.getReturnValue()!=null&&!AscensionProtection.canPlace(context.getPlayer(),context.getClickedPos(),ci.getReturnValue()))ci.setReturnValue(null);
    }

    @Inject(method="placeBlock",at=@At("RETURN"))
    private void darkspawn$recordBridge(BlockPlaceContext context,net.minecraft.world.level.block.state.BlockState state,CallbackInfoReturnable<Boolean> ci) {
        if(ci.getReturnValue()&&context.getPlayer()!=null)AscensionProtection.placed(context.getPlayer(),context.getClickedPos(),state);
    }

}
