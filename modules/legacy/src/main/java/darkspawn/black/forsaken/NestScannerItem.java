package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class NestScannerItem extends Item {
	public NestScannerItem(Properties properties) { super(properties); }
	public static int mode(ItemStack stack) { return Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("ForsakenScanMode", 0), 3); }
	public static void cycle(ItemStack stack) {
		var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); tag.putInt("ForsakenScanMode", (mode(stack) + 1) % 3);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}
	public record Signal(BlockPos pos, String kind) { }
	private static boolean resource(ItemStack stack) {
		return stack.is(ForsakenContent.ALLOY) || stack.is(ForsakenContent.BIOFIBER) || stack.is(ForsakenContent.CORE)
			|| stack.is(ForsakenContent.DATA) || stack.is(ForsakenContent.MODULE);
	}
	public static Signal resources(ServerLevel level, BlockPos center) {
		Signal signal = null; double distance = 32 * 32 + 1;
		for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(center).inflate(32),
			entity -> entity.isAlive() && resource(entity.getItem()))) {
			double candidate = center.distSqr(item.blockPosition());
			if (candidate < distance) { distance = candidate; signal = new Signal(item.blockPosition(), "resource"); }
		}
		for (int x = (center.getX() - 32) >> 4; x <= (center.getX() + 32) >> 4; x++)
			for (int z = (center.getZ() - 32) >> 4; z <= (center.getZ() + 32) >> 4; z++) {
				var chunk = level.getChunkSource().getChunkNow(x, z); if (chunk == null) continue;
				for (var entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity cache) || entity.isRemoved()) continue;
					// Cache Signals: Inspect only the unresolved loot-table identity, never inventory slots or generated loot.
					var table = cache.getLootTable(); if (table == null) continue;
					var id = table.identifier();
					if (!id.getNamespace().equals("darkspawn") || !id.getPath().startsWith("chests/forsaken_")) continue;
					double candidate = center.distSqr(cache.getBlockPos());
					if (candidate < distance) { distance = candidate; signal = new Signal(cache.getBlockPos(), "cache"); }
				}
			}
		return signal;
	}
	public static BlockPos nearest(ServerLevel level, BlockPos center) {
		BlockPos nearest = null; double distance = 32 * 32 + 1;
		// Loaded Chunk Scan: At most 25 chunk block-entity maps; never load terrain to locate a nest.
		for (int x = (center.getX() - 32) >> 4; x <= (center.getX() + 32) >> 4; x++) {
			for (int z = (center.getZ() - 32) >> 4; z <= (center.getZ() + 32) >> 4; z++) {
				var chunk = level.getChunkSource().getChunkNow(x, z); if (chunk == null) continue;
				for (var entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof InfectedNestBlockEntity) || entity.isRemoved() || !level.getBlockState(entity.getBlockPos()).is(ForsakenContent.NEST)) continue;
					double candidate = center.distSqr(entity.getBlockPos());
					if (candidate < distance) { distance = candidate; nearest = entity.getBlockPos(); }
				}
			}
		}
		return nearest;
	}
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			if (!server.isAlive() || server.isSpectator()) return InteractionResult.FAIL;
			var stack = player.getItemInHand(hand);
			if (player.isShiftKeyDown()) {
				cycle(stack); server.sendOverlayMessage(Component.translatable("message.darkspawn.forsaken_scan_mode", Component.translatable("scanner.darkspawn.mode_" + mode(stack))));
				return InteractionResult.SUCCESS;
			}
			if (!level.dimension().equals(Forsaken.LEVEL)) { Forsaken.message(server, "scanner_dimension"); return InteractionResult.FAIL; }
			if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
			player.getCooldowns().addCooldown(stack, 100);
			if (mode(stack) == 1) {
				var signal = resources(server.level(), player.blockPosition());
				server.sendOverlayMessage(signal == null ? Component.translatable("message.darkspawn.forsaken_resources_empty")
					: Component.translatable("message.darkspawn.forsaken_resource_signal", Component.translatable("scanner.darkspawn." + signal.kind()),
						signal.pos().getX(), signal.pos().getY(), signal.pos().getZ(), Math.round(Math.sqrt(signal.pos().distSqr(player.blockPosition())))));
				return InteractionResult.SUCCESS;
			}
			if (mode(stack) == 2) {
				if (!ForsakenThreats.inCity(player.blockPosition())) {
					server.sendOverlayMessage(Component.translatable("message.darkspawn.forsaken_pressure_outside")); return InteractionResult.SUCCESS;
				}
				var zone = ForsakenPressure.get(server.level()).at(player.blockPosition(), level.getGameTime());
				server.sendOverlayMessage(Component.translatable("message.darkspawn.forsaken_pressure_scan", zone.pressure(),
						Component.translatable("scanner.darkspawn." + (player.getZ() - ForsakenPressure.origin(player.blockPosition()).getZ() >= 58 ? "infested" : "abandoned")),
						Math.max(0, (zone.surgeEnd() - level.getGameTime() + 19) / 20)));
				return InteractionResult.SUCCESS;
			}
			var pos = nearest(server.level(), player.blockPosition());
			server.sendOverlayMessage(pos == null ? Component.translatable("message.darkspawn.forsaken_scan_empty")
				: Component.translatable("message.darkspawn.forsaken_scan", pos.getX(), pos.getY(), pos.getZ(), Math.round(Math.sqrt(pos.distSqr(player.blockPosition())))));
		}
		return InteractionResult.SUCCESS;
	}
}
