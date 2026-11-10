package darkspawn.black.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SortingTubeBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public SortingTubeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
	}
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		if (!state.is(Storage.TUBE)) { return net.minecraft.world.phys.shapes.Shapes.block(); }
		return switch (state.getValue(FACING).getAxis()) {
			case X -> Block.box(0, 3, 3, 16, 13, 13);
			case Y -> Block.box(3, 0, 3, 13, 16, 13);
			case Z -> Block.box(3, 3, 0, 13, 13, 16);
		};
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && !player.isSpectator()) {
			level.setBlockAndUpdate(pos, state.cycle(FACING));
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.darkspawn.storage_output",
				level.getBlockState(pos).getValue(FACING).getName()));
		}
		return InteractionResult.SUCCESS;
	}
}
