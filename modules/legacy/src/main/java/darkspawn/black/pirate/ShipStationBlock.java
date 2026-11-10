package darkspawn.black.pirate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ShipStationBlock extends Block {
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
	public ShipStationBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)); }
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
	@Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
	@Override protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && !player.isSpectator()) Ships.open(server, pos);
		return InteractionResult.SUCCESS;
	}
}
