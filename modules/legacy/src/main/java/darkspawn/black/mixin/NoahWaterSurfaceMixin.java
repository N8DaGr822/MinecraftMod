package darkspawn.black.mixin;

import darkspawn.black.legacy.NoahLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class NoahWaterSurfaceMixin {
    @Inject(method="getCollisionShape",at=@At("RETURN"),cancellable=true)
    private void darkspawn$waterSurface(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> callback) {
        if(!(context instanceof EntityCollisionContext collision)||!(collision.getEntity() instanceof Player player)
            ||player.isSpectator()||player.isShiftKeyDown()||player.isUnderWater()||!NoahLegacy.wearing(player,ArmorType.BOOTS)
            ||!state.getFluidState().is(FluidTags.WATER)||level.getFluidState(pos.above()).is(FluidTags.WATER))return;
        // Wavewalking: Only the exposed surface supports feet approaching from above; swimmers remain free below it.
        var shape=Shapes.box(0,0,0,1,state.getFluidState().getHeight(level,pos),1);
        if(context.isAbove(shape,pos,true))callback.setReturnValue(shape);
    }
}
