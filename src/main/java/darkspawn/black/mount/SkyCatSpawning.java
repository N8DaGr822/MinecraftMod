package darkspawn.black.mount;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded sky encounters: ordinary surface-height spawning cannot place an animal in open upper air. */
public final class SkyCatSpawning {
    private SkyCatSpawning() { }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var level = server.getLevel(Level.OVERWORLD);
            // One world-wide roll per minute, not one roll per player; about 20 eligible minutes per success.
            if (level == null || level.getGameTime() % 1200 != 0 || !level.getGameRules().get(GameRules.SPAWN_MOBS)) return;
            var players = level.players().stream().filter(p -> p.isAlive() && !p.isSpectator()
                && p.getY() >= level.getMaxY() - 80).toList();
            if (players.isEmpty() || level.getRandom().nextInt(20) != 0) return;
            var player = players.get(level.getRandom().nextInt(players.size()));
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double distance = 32 + level.getRandom().nextInt(33);
            var pos = BlockPos.containing(player.getX() + Math.cos(angle) * distance,
                level.getMaxY() - 48 + level.getRandom().nextInt(40), player.getZ() + Math.sin(angle) * distance);
            trySpawn(level, pos);
        });
    }
    static boolean trySpawn(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD || !level.getGameRules().get(GameRules.SPAWN_MOBS)
            || pos.getY() < level.getMaxY() - 48 || pos.getY() > level.getMaxY() - 8
            || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.offset(-2, 0, -2)) || !level.hasChunkAt(pos.offset(2, 0, 2))
            || !level.getWorldBorder().isWithinBounds(pos) || !level.canSeeSky(pos)) return false;
        // Local cap includes ridden/named cats. Never load chunks or search the whole world to create an encounter.
        if (!level.getEntitiesOfClass(PoptartCat.class, new AABB(pos).inflate(128), cat -> cat.isAlive()).isEmpty()) return false;
        var cat = Mounts.POPTART_CAT.create(level, EntitySpawnReason.NATURAL);
        if (cat == null) return false;
        cat.setPos(Vec3.atBottomCenterOf(pos));
        if (!level.noCollision(cat) || level.containsAnyLiquid(cat.getBoundingBox())) return false;
        cat.setWildSkyCat();
        cat.setOnGround(false);
        cat.travel(Vec3.ZERO);
        return level.addFreshEntity(cat);
    }
}
