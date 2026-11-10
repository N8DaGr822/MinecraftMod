package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class InfectedNestBlock extends Block implements EntityBlock {
	public InfectedNestBlock(Properties properties) { super(properties); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InfectedNestBlockEntity(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != ForsakenContent.NEST_ENTITY ? null : (world, pos, block, entity) -> ((InfectedNestBlockEntity)entity).tick();
	}
	@Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		super.affectNeighborsAfterRemoval(state, level, pos, moved);
		if (level.dimension().equals(Forsaken.LEVEL) && !level.getBlockState(pos).is(this)) ForsakenPressure.get(level).suppress(pos, level.getGameTime());
	}
}
