package darkspawn.black.storage;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class StorageBlock extends SortingTubeBlock implements EntityBlock {
	public StorageBlock(Properties properties) { super(properties); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StorageBlockEntity(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != Storage.ENTITY ? null : (world, pos, block, entity) -> ((StorageBlockEntity) entity).tick();
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown()) { return super.useWithoutItem(state, level, pos, player, hit); }
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageBlockEntity chest) { player.openMenu(chest); }
		return InteractionResult.SUCCESS;
	}
	@Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		if (this == Storage.BANK && params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StorageBlockEntity chest) {
			for (ItemStack drop : drops) { if (drop.is(asItem())) { drop.set(Storage.BANK_CONTENTS, chest.snapshot()); } }
		}
		return drops;
	}
	@Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Bank Recovery: Creative breaking also preserves the stored items, just like a stocked shulker box.
		if (!level.isClientSide() && this == Storage.BANK && player.preventsBlockDrops()
				&& level.getBlockEntity(pos) instanceof StorageBlockEntity chest && chest.hasBankData()) {
			ItemStack drop = new ItemStack(this);
			drop.set(Storage.BANK_CONTENTS, chest.snapshot());
			popResource(level, pos, drop);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}
}
