package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class BonfireBlock extends BaseEntityBlock {
	// 0: inactive, 1: clearing, 2: active. Radius tiers are 25, 50 and 75 blocks.
	public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 2);
	public static final IntegerProperty RANGE = IntegerProperty.create("range", 1, 3);
	public BonfireBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PHASE, 0).setValue(RANGE, 3));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PHASE, RANGE); }
	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BonfireBlockEntity(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, Bonfires.ENTITY, BonfireBlockEntity::tick);
	}
	@Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (level instanceof ServerLevel server) BonfireWards.register(server, pos);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer viewer)) return InteractionResult.SUCCESS;
		if (player.isSecondaryUseActive()) {
			int range = state.getValue(RANGE) % 3 + 1;
			server.setBlockAndUpdate(pos, state.setValue(RANGE, range).setValue(PHASE, state.getValue(PHASE) == 0 ? 0 : 1));
			viewer.sendOverlayMessage(Component.translatable("message.darkspawn.bonfire_range", range * 25));
		} else if (state.getValue(PHASE) == 0) {
			if (!BonfireWards.foundation(server, pos)) {
				viewer.sendOverlayMessage(Component.translatable("message.darkspawn.bonfire_foundation"));
				return InteractionResult.FAIL;
			}
			server.setBlockAndUpdate(pos, state.setValue(PHASE, 1));
			viewer.sendOverlayMessage(Component.translatable("message.darkspawn.bonfire_clearing"));
		} else {
			server.setBlockAndUpdate(pos, state.setValue(PHASE, 0));
			viewer.sendOverlayMessage(Component.translatable("message.darkspawn.bonfire_off"));
		}
		BonfireWards.register(server, pos);
		if (server.getBlockEntity(pos) instanceof BonfireBlockEntity bonfire) bonfire.resetLighting();
		BonfireWards.preview(server, viewer, new WardVolume(pos, server.getBlockState(pos).getValue(RANGE) * 25));
		return InteractionResult.SUCCESS;
	}
}
