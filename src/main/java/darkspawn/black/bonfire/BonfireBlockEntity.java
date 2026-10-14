package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BonfireBlockEntity extends BlockEntity {
	private int lightCursor;
	private int lastRange;
	private boolean firstPassDone;
	public BonfireBlockEntity(BlockPos pos, BlockState state) { super(Bonfires.ENTITY, pos, state); }
	public void resetLighting() { lightCursor = 0; firstPassDone = false; }
	public static void tick(Level raw, BlockPos pos, BlockState state, BonfireBlockEntity entity) {
		if (!(raw instanceof ServerLevel level)) return;
		BonfireWards.register(level, pos);
		if (state.getValue(BonfireBlock.PHASE) == 0) return;
		if (!BonfireWards.foundation(level, pos)) {
			level.setBlockAndUpdate(pos, state.setValue(BonfireBlock.PHASE, 0));
			entity.resetLighting();
			return;
		}
		WardVolume volume = WardVolume.forTier(pos, state.getValue(BonfireBlock.RANGE));
		int radius = volume.radius();
		if (radius != entity.lastRange) { entity.lastRange = radius; entity.resetLighting(); }
		// Spread light updates across ticks. Only already-loaded chunks and empty space are touched.
		int side = (radius * 2 + 7) / 8, total = side * side * ((volume.height() + 7) / 8);
		for (int i = 0; i < 16; i++) {
			int index = entity.lightCursor++;
			int x = index % side, z = index / side % side, y = index / (side * side);
			BlockPos light = new BlockPos(pos.getX() - radius + x * 8, (int) volume.bottom() + y * 8, pos.getZ() - radius + z * 8);
			if (volume.contains(Vec3.atCenterOf(light)) && level.hasChunkAt(light) && !level.isOutsideBuildHeight(light)) {
				BlockState existing = level.getBlockState(light);
				if (existing.isAir() && !existing.is(Bonfires.LIGHT)) level.setBlock(light, Bonfires.LIGHT.defaultBlockState(), 3);
			}
			if (entity.lightCursor >= total) { entity.lightCursor = 0; entity.firstPassDone = true; }
		}
		if (level.getGameTime() % 20 != 0) return;
		boolean clear = true;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, volume.bounds(), BonfireWards::ordinaryHostile)) {
			if (volume.intersects(mob.getBoundingBox()) && BonfireWards.guard(mob)) clear = false;
		}
		int phase = clear && entity.firstPassDone ? 2 : 1;
		if (phase != state.getValue(BonfireBlock.PHASE)) level.setBlockAndUpdate(pos, state.setValue(BonfireBlock.PHASE, phase));
		level.sendParticles(phase == 2 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
			pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, 6, .25, .2, .25, .02);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + .5, pos.getY() + 1.3, pos.getZ() + .5, 2, .1, .1, .1, .01);
	}
}
