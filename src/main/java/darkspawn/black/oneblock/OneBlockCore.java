package darkspawn.black.oneblock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class OneBlockCore extends Block {
	public static final IntegerProperty RESOURCE = IntegerProperty.create("resource", 0, 19);
	public OneBlockCore(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(RESOURCE, 0)); }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RESOURCE); }
	@Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return OneBlock.island(player.level()) && pos.equals(OneBlock.CORE_POS) ? resource(state).getDestroyProgress(player, level, pos) : 0;
	}
	@Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		var resource = resource(state); var tool = params.getOptionalParameter(LootContextParams.TOOL);
		var harvesting = tool == null ? null : tool.get(DataComponents.TOOL);
		// Native Harvesting: Preserve Silk Touch/Fortune and enforce the represented resource's tool tier.
		if (resource.requiresCorrectToolForDrops() && (harvesting == null || !harvesting.isCorrectForDrops(resource))) return List.of();
		return resource.getDrops(params);
	}
	@Override protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean experience) {
		var resource = resource(state);
		if (!resource.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(resource)) resource.spawnAfterBreak(level, pos, tool, experience);
	}
	public static BlockState resource(BlockState state) { return OneBlock.RESOURCES[state.getValue(RESOURCE)].defaultBlockState(); }
}
