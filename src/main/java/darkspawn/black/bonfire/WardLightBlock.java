package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible replaceable air-space light; never has an item or a block entity. */
public final class WardLightBlock extends Block {
	public WardLightBlock(Properties properties) { super(properties); }
	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
	@Override protected boolean propagatesSkylightDown(BlockState state) { return true; }
	@Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		if (!level.isClientSide()) level.scheduleTick(pos, this, 40);
	}
	@Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!level.getBlockState(pos).is(this)) return;
		if (!BonfireWards.covers(level, Vec3.atCenterOf(pos))) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		else level.scheduleTick(pos, this, 40);
	}
}
