package darkspawn.black.bonfire;

import darkspawn.black.boss.AncientTreeSpirit;
import darkspawn.black.boss.BiomeBoss;
import darkspawn.black.boss.MutantWolf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class BonfireWards {
	private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> COPPER_BASE = net.minecraft.tags.TagKey.create(
		net.minecraft.core.registries.Registries.BLOCK, darkspawn.black.Darkspawn.id("bonfire_copper_base"));
	private static final Map<ServerLevel, Set<BlockPos>> CORES = new WeakHashMap<>();
	private static final Map<Entity, Long> RETRY = new WeakHashMap<>();
	private BonfireWards() { }
	public static void clear() { CORES.clear(); RETRY.clear(); }
	public static void register(ServerLevel level, BlockPos pos) { CORES.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable()); }
	public static void unload(ServerLevel level, ChunkPos chunk) {
		var cores = CORES.get(level);
		if (cores != null) cores.removeIf(chunk::contains);
	}
	public static boolean foundation(ServerLevel level, BlockPos pos) {
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
			BlockPos base = pos.offset(x, -1, z);
			if (!level.hasChunkAt(base)) return false;
			var state = level.getBlockState(base);
			if (x == 0 && z == 0 ? !state.is(COPPER_BASE) : !state.is(Blocks.STONE_BRICKS)) return false;
		}
		return true;
	}
	private static List<WardVolume> volumes(ServerLevel level) {
		var result = new ArrayList<WardVolume>();
		var cores = CORES.get(level);
		if (cores == null) return result;
		var iterator = cores.iterator();
		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();
			if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Bonfires.CORE)) { iterator.remove(); continue; }
			var state = level.getBlockState(pos);
			if (state.getValue(BonfireBlock.PHASE) > 0 && foundation(level, pos)) result.add(new WardVolume(pos, state.getValue(BonfireBlock.RANGE) * 25));
		}
		return result;
	}
	public static boolean ordinaryHostile(Entity entity) {
		return entity instanceof Mob && entity.getType().getCategory() == MobCategory.MONSTER
			&& !(entity instanceof BiomeBoss || entity instanceof AncientTreeSpirit || entity instanceof MutantWolf
				|| entity instanceof WitherBoss || entity instanceof EnderDragon);
	}
	public static boolean covers(ServerLevel level, Vec3 point) { return volumes(level).stream().anyMatch(volume -> volume.contains(point)); }
	public static boolean blocksSpawn(ServerLevel level, Entity entity) {
		return ordinaryHostile(entity) && volumes(level).stream().anyMatch(volume -> volume.intersects(entity.getBoundingBox()));
	}
	/** True means quarantine this tick: no safe loaded destination exists yet. */
	public static boolean guard(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level) || !ordinaryHostile(entity)) return false;
		var volumes = volumes(level);
		var ward = volumes.stream().filter(volume -> volume.intersects(entity.getBoundingBox())).findFirst();
		if (ward.isEmpty()) return false;
		Mob mob = (Mob) entity;
		mob.setTarget(null);
		mob.getNavigation().stop();
		mob.setDeltaMovement(Vec3.ZERO);
		mob.resetFallDistance();
		markClearing(level, ward.get());
		long now = level.getGameTime();
		Long previous = RETRY.get(entity);
		if (previous != null && now - previous < 20) return true;
		RETRY.put(entity, now);
		Vec3 destination = escape(level, mob, ward.get(), volumes);
		if (destination == null) return true;
		// Avoid leaving a hostile passenger behind or carrying a friendly passenger out.
		mob.ejectPassengers();
		mob.stopRiding();
		mob.teleportTo(destination.x, destination.y, destination.z);
		mob.setDeltaMovement(Vec3.ZERO);
		RETRY.remove(entity);
		level.sendParticles(ParticleTypes.FLAME, destination.x, destination.y + .5, destination.z, 8, .3, .3, .3, .01);
		return false;
	}
	private static void markClearing(ServerLevel level, WardVolume ward) {
		var state = level.getBlockState(ward.center());
		if (state.is(Bonfires.CORE) && state.getValue(BonfireBlock.PHASE) == 2)
			level.setBlockAndUpdate(ward.center(), state.setValue(BonfireBlock.PHASE, 1));
	}
	private static Vec3 escape(ServerLevel level, Mob mob, WardVolume ward, List<WardVolume> all) {
		double angle = Math.atan2(mob.getZ() - ward.center().getZ() - .5, mob.getX() - ward.center().getX() - .5);
		for (int ring = 0; ring < 3; ring++) for (int direction = 0; direction < 16; direction++) {
			double theta = angle + direction * Math.PI / 8;
			double radius = ward.radius() + 12 + ring * 8 + mob.getBbWidth();
			int x = (int) Math.floor(ward.center().getX() + .5 + Math.cos(theta) * radius);
			int z = (int) Math.floor(ward.center().getZ() + .5 + Math.sin(theta) * radius);
			for (int step = 0; step <= 16; step++) {
				int dy = step == 0 ? 0 : (step + 1) / 2 * (step % 2 == 0 ? -1 : 1);
				BlockPos candidate = new BlockPos(x, mob.blockPosition().getY() + dy, z);
				if (!level.hasChunkAt(candidate) || level.isOutsideBuildHeight(candidate) || !level.getWorldBorder().isWithinBounds(candidate)) continue;
				Vec3 safe = DismountHelper.findSafeDismountLocation(mob.getType(), level, candidate, true);
				if (safe == null || !level.getFluidState(candidate).isEmpty()) continue;
				var box = mob.getDimensions(mob.getPose()).makeBoundingBox(safe);
				if (all.stream().anyMatch(volume -> volume.intersects(box)) || !level.noCollision(mob, box)) continue;
				return safe;
			}
		}
		return null;
	}
	/** Cancels boundary crossings before collision/movement is applied, including knockback and falls. */
	public static boolean blocksMove(Entity entity, Vec3 delta) {
		if (!(entity.level() instanceof ServerLevel level) || !ordinaryHostile(entity)) return false;
		for (WardVolume ward : volumes(level)) {
			if (!ward.crosses(entity.getBoundingBox(), delta)) continue;
			if (ward.intersects(entity.getBoundingBox())) guard(entity);
			Mob mob = (Mob) entity;
			mob.setDeltaMovement(Vec3.ZERO);
			mob.resetFallDistance();
			mob.setTarget(null);
			mob.getNavigation().stop();
			// Ask ground pathfinding to turn away; the swept boundary remains the backstop.
			Vec3 away = mob.position().subtract(Vec3.atCenterOf(ward.center())).multiply(1, 0, 1).normalize();
			if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(mob.getX() + away.x * 8, mob.getY(), mob.getZ() + away.z * 8, 1);
			if (mob.tickCount % 20 == 0) level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + .5, mob.getZ(), 3, .2, .2, .2, 0);
			return true;
		}
		return false;
	}
	public static void preview(ServerLevel level, ServerPlayer viewer, WardVolume ward) {
		for (int i = 0; i < 96; i++) {
			double angle = i * Math.PI / 48;
			double x = ward.center().getX() + .5 + Math.cos(angle) * ward.radius();
			double z = ward.center().getZ() + .5 + Math.sin(angle) * ward.radius();
			for (double y : new double[] {ward.bottom(), ward.center().getY() + .5, ward.top()})
				level.sendParticles(viewer, ParticleTypes.END_ROD, true, true, x, y, z, 1, 0, 0, 0, 0);
		}
	}
}
