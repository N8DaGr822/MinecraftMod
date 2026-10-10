package darkspawn.black.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Native block state makes serving counts atomic on the server and persistent in chunks. */
public final class FeastBlock extends Block {
	public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 1, 6);
	private final Holder<MobEffect> effect;
	public FeastBlock(Properties properties, Holder<MobEffect> effect) {
		super(properties); this.effect = effect;
		registerDefaultState(stateDefinition.any().setValue(SERVINGS, 6));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SERVINGS); }
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return Block.box(1, 0, 1, 15, 5, 15);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return serve(level, pos, player);
	}
	public InteractionResult serve(Level level, BlockPos pos, Player player) {
		if (!player.isAlive() || player.isSpectator() || !player.canEat(false)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		// Re-read live state: a second diner must never reuse a stale final serving.
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) return InteractionResult.PASS;
		int portions = current.getValue(SERVINGS);
		if (portions == 1) level.removeBlock(pos, false);
		else level.setBlock(pos, current.setValue(SERVINGS, portions - 1), 3);
		player.getFoodData().eat(8, 0.8F);
		MealEffects.apply(player, effect, 20 * 300);
		if (player instanceof ServerPlayer serverPlayer) Culinary.award(serverPlayer, "shared_table", "earned");
		return InteractionResult.SUCCESS;
	}
}
