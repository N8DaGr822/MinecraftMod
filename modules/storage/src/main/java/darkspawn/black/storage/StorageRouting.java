package darkspawn.black.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class StorageRouting {
	public static final int MAX_NODES = 128;
	private StorageRouting() { }

	public static List<StorageBlockEntity> route(Level level, BlockPos start) {
		var result = new ArrayList<StorageBlockEntity>();
		var visited = new HashSet<BlockPos>();
		BlockPos pos = start;
		// Chunk Safety: Inspect loaded nodes only, with a hard visit limit and cycle detection.
		for (int step = 0; step < MAX_NODES; step++) {
			if (!level.hasChunkAt(pos) || !visited.add(pos)) { break; }
			var state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof SortingTubeBlock)) { break; }
			if (!pos.equals(start) && level.getBlockEntity(pos) instanceof StorageBlockEntity chest) {
				if (chest.isDump()) { break; }
				result.add(chest);
				if (chest.isBank()) { break; }
			}
			pos = pos.relative(state.getValue(SortingTubeBlock.FACING));
		}
		return result;
	}

	private static BlockPos findDump(StorageBlockEntity bank) {
		Level level = bank.getLevel();
		var queue = new ArrayDeque<BlockPos>();
		var visited = new HashSet<BlockPos>();
		queue.add(bank.getBlockPos()); visited.add(bank.getBlockPos());
		while (!queue.isEmpty() && visited.size() <= MAX_NODES) {
			BlockPos current = queue.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos candidate = current.relative(direction);
				if (visited.contains(candidate) || !level.hasChunkAt(candidate)) { continue; }
				var state = level.getBlockState(candidate);
				if (!(state.getBlock() instanceof SortingTubeBlock)
						|| !candidate.relative(state.getValue(SortingTubeBlock.FACING)).equals(current)) { continue; }
				if (!visited.add(candidate) || visited.size() > MAX_NODES) { return null; }
				if (state.is(Storage.DUMP)) { return candidate; }
				if (!state.is(Storage.BANK)) { queue.addLast(candidate); }
			}
		}
		return null;
	}

	public static int transfer(StorageBlockEntity source, ItemStack offered, boolean resort) {
		if (source.getLevel() == null || source.getLevel().isClientSide()) { return 0; }
		BlockPos start = resort ? findDump(source) : source.getBlockPos();
		if (start == null) { return 0; }
		List<StorageBlockEntity> route = route(source.getLevel(), start);
		if (resort && !route.contains(source)) { return 0; }
		int remaining = offered.getCount();
		for (StorageBlockEntity destination : route) {
			if (destination == source || resort && destination.isBank()) { break; }
			remaining -= destination.insert(offered.copyWithCount(remaining), true);
			if (remaining == 0) { break; }
		}
		return offered.getCount() - remaining;
	}
}
