package darkspawn.black.forsaken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InfectedNestBlockEntity extends BlockEntity {
	private int activeTicks;
	public InfectedNestBlockEntity(BlockPos pos, BlockState state) { super(ForsakenContent.NEST_ENTITY, pos, state); }
	public void tick() {
		if (!(level instanceof ServerLevel server) || !server.dimension().equals(Forsaken.LEVEL)) return;
		// No Backlog: Unloading or restarting resets this local timer; no missed waves are replayed.
		activeTicks++;
		if (activeTicks % 40 == 0) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER, worldPosition.getX() + .5, worldPosition.getY() + 1.1, worldPosition.getZ() + .5, 4, .4, .3, .4, .01);
			for (var player : server.players()) if (!player.isCreative() && !player.isSpectator() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) < 9) ForsakenContent.contaminate(player);
		}
		var zone = ForsakenPressure.get(server).at(worldPosition, server.getGameTime());
		int interval = zone.pressure() >= 50 ? 400 : 800;
		if (activeTicks % interval != 0) return;
		for (int i = 0; i < 2; i++) {
			var pos = worldPosition.offset(server.getRandom().nextInt(9) - 4, 0, server.getRandom().nextInt(9) - 4);
			if (ForsakenThreats.spawn(server, pos, true)) break;
		}
	}
}
