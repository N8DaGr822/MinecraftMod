package darkspawn.black.forsaken;

import java.util.HashSet;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

public final class ForsakenThreats {
	public static final int CITY_CAP = 12, WORLD_CAP = 64, ATTEMPT_CAP = 8;
	private static final WeakHashMap<ServerLevel, Budget> BUDGETS = new WeakHashMap<>();
	private static final class Budget { long second = -1; int attempts, count; }
	private ForsakenThreats() { }
	public static boolean inCity(BlockPos pos) {
		var origin = ForsakenPressure.origin(pos); int x = pos.getX() - origin.getX(), z = pos.getZ() - origin.getZ();
		return x >= -32 && x <= 31 && z >= 16 && z <= 95 && pos.getY() >= 75 && pos.getY() <= 104;
	}
	public static void noise(ServerLevel level, BlockPos pos, int amount) {
		if (level.dimension().equals(Forsaken.LEVEL) && inCity(pos)) ForsakenPressure.get(level).disturb(pos, level.getGameTime(), amount);
	}
	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
			if (taken > 0 && target.level() instanceof ServerLevel level) noise(level, target.blockPosition(), source.is(DamageTypeTags.IS_EXPLOSION) ? 15 : 6);
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> { if (level instanceof ServerLevel server) noise(server, pos, 3); });
		ServerTickEvents.END_SERVER_TICK.register(server -> { var level = server.getLevel(Forsaken.LEVEL); if (level != null) tick(level); });
	}
	private static void tick(ServerLevel level) {
		if (!level.dimension().equals(Forsaken.LEVEL) || level.getGameTime() % 100 != 0 || level.getDifficulty() == Difficulty.PEACEFUL) return;
		var visited = new HashSet<BlockPos>();
		var players = level.players();
		// Fair Scheduling: Rotate the first city when many players share the world attempt budget.
		for (int index = 0; index < players.size(); index++) {
			var player = players.get((index + (int)(level.getGameTime() / 100 % players.size())) % players.size());
			if (!player.isAlive() || player.isSpectator() || player.isCreative() || !inCity(player.blockPosition())) continue;
			var origin = ForsakenPressure.origin(player.blockPosition()); var zone = ForsakenPressure.get(level).at(origin.offset(0, 0, 16), level.getGameTime());
			if (level.getGameTime() % 200 == 0 && zone.pressure() >= 25) player.sendOverlayMessage(Component.translatable(
				zone.surgeEnd() > level.getGameTime() ? "message.darkspawn.forsaken_surge" : zone.pressure() >= 50 ? "message.darkspawn.forsaken_alert" : "message.darkspawn.forsaken_watch"));
			if (!visited.add(origin) || zone.suppressed() > level.getGameTime()) continue;
			int attempts = zone.surgeEnd() > level.getGameTime() ? 3 : zone.pressure() >= 50 ? 2 : 1;
			for (int i = 0; i < attempts; i++) {
				var pos = origin.offset(level.getRandom().nextInt(64) - 32, 0, 16 + level.getRandom().nextInt(80));
				spawn(level, pos, false);
			}
		}
	}
	public static boolean spawn(ServerLevel level, BlockPos pos, boolean nest) {
		if (!level.dimension().equals(Forsaken.LEVEL) || level.getDifficulty() == Difficulty.PEACEFUL || !inCity(pos)
			|| !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)
			|| !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos.offset(-2, 0, -2)) || !level.hasChunkAt(pos.offset(2, 0, 2))) return false;
		var budget = BUDGETS.computeIfAbsent(level, ignored -> new Budget());
		if (budget.second != level.getGameTime() / 20) {
			budget.second = level.getGameTime() / 20; budget.attempts = 0; budget.count = 0;
			// Loaded Entities Only: This never requests chunks and runs at most once in an active spawn second.
			for (var entity : level.getAllEntities()) if (entity instanceof Infected && entity.isAlive()) budget.count++;
		}
		if (budget.attempts++ >= ATTEMPT_CAP || budget.count >= WORLD_CAP || !level.hasChunkAt(pos)) return false;
		if (ContainmentBeaconBlockEntity.suppresses(level, pos)) return false;
		var origin = ForsakenPressure.origin(pos);
		if (level.getEntitiesOfClass(Infected.class, new AABB(origin.getX() - 48, 64, origin.getZ(), origin.getX() + 48, 120, origin.getZ() + 112)).size() >= CITY_CAP) return false;
		if (level.players().stream().noneMatch(player -> player.isAlive() && !player.isSpectator() && !player.isCreative() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 48 * 48)
			|| level.players().stream().anyMatch(player -> player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < (nest ? 4 * 4 : 12 * 12))) return false;
		var zone = ForsakenPressure.get(level).at(pos, level.getGameTime());
		if (!nest && zone.suppressed() > level.getGameTime()) return false;
		boolean infested = pos.getZ() - origin.getZ() >= 58;
		int roll = level.getRandom().nextInt(10);
		var kind = infested && zone.pressure() >= 50 && roll == 0 ? Infected.Kind.BRUTE
			: infested && roll < 3 ? Infected.Kind.SPITTER : roll < 6 ? Infected.Kind.CRAWLER : Infected.Kind.WANDERER;
		var mob = ForsakenContent.MOBS.get(kind).create(level, EntitySpawnReason.EVENT);
		if (mob == null) return false;
		mob.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		if (!level.getBlockState(pos.below()).isSolidRender() || !level.getFluidState(pos).isEmpty()
			|| !level.noCollision(mob) || !level.getEntities(mob, mob.getBoundingBox()).isEmpty()) return false;
		if (!level.addFreshEntity(mob)) return false;
		budget.count++; return true;
	}
}
