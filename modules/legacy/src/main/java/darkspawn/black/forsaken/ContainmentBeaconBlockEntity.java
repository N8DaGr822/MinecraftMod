package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ContainmentBeaconBlockEntity extends BlockEntity {
	public static final int DURATION = 6000, RADIUS = 16;
	private long poweredUntil;
	public ContainmentBeaconBlockEntity(BlockPos pos, BlockState state) { super(ForsakenContent.BEACON_ENTITY, pos, state); }
	public boolean active() { return level != null && level.dimension().equals(Forsaken.LEVEL) && poweredUntil > level.getGameTime(); }
	public boolean fuel(ServerPlayer player, ItemStack stack) {
		if (level != player.level() || !player.isAlive() || player.isSpectator() || !stack.is(ForsakenContent.CORE)
			|| player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) > 36) return false;
		if (!level.dimension().equals(Forsaken.LEVEL)) { Forsaken.message(player, "beacon_dimension"); return false; }
		if (active()) { status(player); return false; }
		// Fuel Transaction: Refuse top-ups; absolute world time survives saves and cannot accumulate stacked charges.
		poweredUntil = level.getGameTime() + DURATION; stack.consume(1, player); setChanged();
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ContainmentBeaconBlock.ACTIVE, true));
		status(player); return true;
	}
	public void status(ServerPlayer player) {
		player.sendOverlayMessage(active() ? Component.translatable("message.darkspawn.forsaken_beacon_active", (poweredUntil - level.getGameTime() + 19) / 20)
			: Component.translatable("message.darkspawn.forsaken_beacon_empty"));
	}
	public void tick() {
		if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) return;
		if (getBlockState().getValue(ContainmentBeaconBlock.ACTIVE) != active())
			server.setBlockAndUpdate(worldPosition, getBlockState().setValue(ContainmentBeaconBlock.ACTIVE, active()));
		if (active()) server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, worldPosition.getX() + .5,
			worldPosition.getY() + 1.2, worldPosition.getZ() + .5, 3, .2, .3, .2, .01);
	}
	public static boolean suppresses(ServerLevel level, BlockPos pos) {
		if (!level.dimension().equals(Forsaken.LEVEL)) return false;
		// Overlap Union: Any one powered beacon suffices; a query visits at most nine loaded chunk maps.
		for (int x = (pos.getX() - RADIUS) >> 4; x <= (pos.getX() + RADIUS) >> 4; x++)
			for (int z = (pos.getZ() - RADIUS) >> 4; z <= (pos.getZ() + RADIUS) >> 4; z++) {
				var chunk = level.getChunkSource().getChunkNow(x, z); if (chunk == null) continue;
				for (var entity : chunk.getBlockEntities().values())
					if (entity instanceof ContainmentBeaconBlockEntity beacon && !beacon.isRemoved() && beacon.active()
						&& pos.distSqr(beacon.getBlockPos()) <= RADIUS * RADIUS) return true;
			}
		return false;
	}
	@Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); output.putLong("PoweredUntil", poweredUntil); }
	@Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input); poweredUntil = input.getLongOr("PoweredUntil", 0); }
}
