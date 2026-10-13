package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ContainmentBeaconBlock extends Block implements EntityBlock {
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
		Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 3, 13, 12, 13), Block.box(2, 12, 2, 14, 14, 14), Block.box(5, 14, 5, 11, 16, 11));
	public ContainmentBeaconBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(ACTIVE, false)); }
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return SHAPE; }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ContainmentBeaconBlockEntity(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != ForsakenContent.BEACON_ENTITY ? null : (world, pos, block, entity) -> ((ContainmentBeaconBlockEntity)entity).tick();
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ForsakenContent.CORE)) return InteractionResult.PASS;
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ContainmentBeaconBlockEntity beacon) beacon.fuel(server, stack);
		return InteractionResult.SUCCESS;
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ContainmentBeaconBlockEntity beacon) beacon.status(server);
		return InteractionResult.SUCCESS;
	}
}
