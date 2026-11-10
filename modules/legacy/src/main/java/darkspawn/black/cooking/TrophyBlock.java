package darkspawn.black.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TrophyBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);
	public TrophyBlock(Properties properties) { super(properties); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
