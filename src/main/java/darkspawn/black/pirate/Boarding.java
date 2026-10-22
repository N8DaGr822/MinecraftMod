package darkspawn.black.pirate;

import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Optional patrol boarding. Attempts are persisted on the ship; live parties never survive reload. */
public final class Boarding {
    private static final String SHOT_TAG = "darkspawn_boarding_arrow";
    private static final Map<ServerLevel, Map<UUID, Battle>> ACTIVE = new WeakHashMap<>();
    private static final class Battle {
        final UUID leader;
        final PirateShip ship;
        final Set<UUID> members = new LinkedHashSet<>();
        final List<BoardingDefender> defenders = new ArrayList<>();
        final Set<UUID> defeated = new HashSet<>();
        final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("Patrol boarding"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        long expires;
        boolean started, won;
        Battle(ServerPlayer p, PirateShip ship) {
            leader = p.getUUID(); this.ship = ship; members.add(leader); expires = p.level().getGameTime() + 1200;
        }
    }
    private Boarding() { }
    private static Map<UUID, Battle> battles(ServerLevel level) { return ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>()); }
    private static Battle member(ServerPlayer p) {
        for (var map : ACTIVE.values()) for (var battle : map.values())
            if (!battle.won && battle.members.contains(p.getUUID())) return battle;
        return null;
    }
    private static Battle battle(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return null;
        return battles(level).values().stream().filter(b -> b.defenders.contains(entity)).findFirst().orElse(null);
    }
    public static boolean reserved(PirateShip ship) {
        if (!(ship.level() instanceof ServerLevel level)) return false;
        var b = battles(level).get(ship.getUUID());
        return b != null && b.ship == ship;
    }
    public static boolean canTarget(Entity defender, Entity target) {
        var b = battle(defender);
        return b != null && b.started && !b.won && target instanceof ServerPlayer p && b.members.contains(p.getUUID()) && present(p, b);
    }
    private static boolean present(ServerPlayer p, Battle b) {
        return p.isAlive() && p.gameMode() == GameType.SURVIVAL && p.level() == b.ship.level() && p.distanceToSqr(b.ship) <= 48 * 48;
    }
    private static int message(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); return 0; }
    public static int form(ServerPlayer p) {
        if (member(p) != null || p.gameMode() != GameType.SURVIVAL || !p.isAlive() || p.level().getDifficulty() == Difficulty.PEACEFUL
            || !p.level().dimension().equals(Pirate.LEVEL)) return message(p, "Boarding needs Pirate Seas Survival, non-Peaceful difficulty, and no existing boarding party.");
        var ship = p.level().getEntitiesOfClass(PirateShip.class, p.getBoundingBox().inflate(24), s -> s.boardableBy(p) && !reserved(s))
            .stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (ship == null || battles(p.level()).size() >= 4) return message(p, "No available disabled patrol within 24 blocks. Your ship must damage an ordinary NPC patrol to 25% hull or less; wait for fires to stop. Up to four boarding encounters at once.");
        var b = new Battle(p, ship); battles(p.level()).put(ship.getUUID(), b);
        message(p, "Boarding party open for 60 seconds. Solo: /boarding start. Friends: /boarding join " + p.getGameProfile().name()
            + ". Up to 4 players. Start moves everyone onto the patrol. Stay alive within 48 blocks for 5 minutes. A departure cancels the fight and loses the patrol cargo. /boarding leave opts out.");
        return 1;
    }
    public static int join(ServerPlayer p, ServerPlayer leader) {
        var b = member(leader);
        if (b == null || !b.leader.equals(leader.getUUID()) || b.started || b.members.size() >= 4 || member(p) != null
            || !present(p, b) || p.distanceToSqr(b.ship) > 24 * 24 || p.level().getGameTime() >= b.expires)
            return message(p, "Join a nearby open party in Survival: /boarding join <leader>. Maximum four players.");
        b.members.add(p.getUUID());
        message(p, "Joined boarding. Start moves you onto the patrol. Stay alive within 48 blocks; leaving ends a started fight for everyone. /boarding leave opts out.");
        message(leader, p.getGameProfile().name() + " joined boarding: " + b.members.size() + "/4."); return 1;
    }
    private static Vec3 entry(Battle b, int index) { return b.ship.local(index % 2 == 0 ? -1 : 1, 1.05, 4 + index / 2 * 2); }
    private static Vec3 sentry(Battle b, int index) { return b.ship.local(index % 2 == 0 ? -2 : 2, 1.05, -5 + index / 2 * 3); }
    public static int start(ServerPlayer leader) {
        var b = member(leader);
        if (b == null || !b.leader.equals(leader.getUUID()) || b.started) return message(leader, "Form a boarding party first; only its leader can start.");
        if (!valid(b) || !b.ship.boardableBy(leader)) return message(leader, "The patrol or party is no longer eligible. /boarding leave, then form again.");
        var level = leader.level(); int i = 0;
        b.defenders.clear();
        for (var id : b.members) {
            var p = level.getServer().getPlayerList().getPlayer(id); var pos = entry(b, i++);
            if (p.distanceToSqr(b.ship) > 24 * 24 || p.containerMenu != p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty()
                || !level.hasChunkAt(net.minecraft.core.BlockPos.containing(pos)) || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(pos))
                || !level.noCollision(p, p.getBoundingBox().move(pos.subtract(p.position()))))
                return message(leader, "Everyone must be within 24 blocks with menus closed; the boarding deck must be clear and loaded.");
        }
        int count = b.members.size() == 1 ? 3 : 4;
        for (i = 0; i < count; i++) {
            var defender = new BoardingDefender(Ships.BOARDING_DEFENDER, level); defender.setPos(sentry(b, i));
            defender.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            defender.setCustomName(Component.literal("Patrol sentry")); defender.setCustomNameVisible(true);
            if (!level.hasChunkAt(defender.blockPosition()) || !level.noCollision(defender)) {
                b.defenders.clear(); return message(leader, "Sentry positions are blocked; clear the patrol deck before starting.");
            }
            b.defenders.add(defender);
        }
        // Attempt Boundary: Persist before spawning enemies or moving players. Reload cancels without cargo.
        b.started = true; b.ship.boardingSpent = true; b.expires = level.getGameTime() + 6000;
        for (var defender : b.defenders) if (!level.addFreshEntity(defender)) { finish(b, false); return 0; }
        i = 0;
        for (var id : b.members) {
            var p = level.getServer().getPlayerList().getPlayer(id); var pos = entry(b, i++);
            p.stopRiding(); p.teleportTo(pos.x, pos.y, pos.z); p.setDeltaMovement(Vec3.ZERO); p.fallDistance = 0;
            b.bar.addPlayer(p);
        }
        update(b); return 1;
    }
    private static boolean valid(Battle b) {
        var level = (ServerLevel) b.ship.level();
        if (!b.ship.isAlive() || !level.hasChunkAt(b.ship.blockPosition()) || level.getDifficulty() == Difficulty.PEACEFUL || level.getGameTime() >= b.expires) return false;
        for (var id : b.members) {
            var p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || !present(p, b)) return false;
        }
        return true;
    }
    private static void update(Battle b) {
        b.bar.setName(Component.literal("Boarding: " + b.defeated.size() + "/" + b.defenders.size() + " sentries · "
            + Math.max(0, (b.expires - b.ship.level().getGameTime()) / 20) + "s · stay within 48 blocks"));
        b.bar.setProgress((float) b.defeated.size() / b.defenders.size());
    }
    private static void finish(Battle b, boolean won) {
        var level = (ServerLevel) b.ship.level();
        b.bar.removeAllPlayers(); b.defenders.forEach(Entity::discard);
        for (var entity : level.getAllEntities())
            if (entity.entityTags().contains(SHOT_TAG + "_" + b.ship.getUUID())) entity.discard();
        if (won) {
            // One-Way Reward: The original hold is emptied once, shared by the party; no extra drops or XP.
            b.won = true; b.expires = level.getGameTime() + 1200;
            b.ship.releaseBoardingCargo(level.getServer().getPlayerList().getPlayer(b.leader));
        } else {
            battles(level).remove(b.ship.getUUID());
            if (b.started) b.ship.discard();
        }
        for (var id : b.members) {
            var p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) message(p, won ? "Patrol captured! Collect its shared cargo on deck and return to your ship within 60 seconds. Naval credit goes to the party captain."
                : "Boarding ended: departure, death, timeout or interruption. No boarding reward; started patrols are lost.");
        }
    }
    public static int leave(ServerPlayer p) {
        var b = member(p); if (b == null) return message(p, "You are not in a boarding party.");
        if (b.started || b.leader.equals(p.getUUID())) finish(b, false);
        else { b.members.remove(p.getUUID()); message(p, "Left the boarding lobby."); }
        return 1;
    }
    public static int status(ServerPlayer p) {
        var b = member(p);
        return message(p, b == null ? "Boarding: disable an ordinary NPC patrol to 25% hull, approach within 24 blocks, then /boarding form. Solo or up to four explicit participants."
            : b.started ? "Boarding: " + b.defeated.size() + "/" + b.defenders.size() + " sentries. Stay alive within 48 blocks; /boarding leave cancels."
            : "Boarding lobby: " + b.members.size() + "/4. /boarding join <leader>, /boarding start, /boarding leave. Start moves everyone aboard.");
    }
    public static void tick(ServerLevel level) {
        for (var b : List.copyOf(battles(level).values())) {
            if (b.won) {
                if (!b.ship.isAlive() || level.getGameTime() >= b.expires) { battles(level).remove(b.ship.getUUID()); b.ship.discard(); }
                continue;
            }
            if (!valid(b)) { finish(b, false); continue; }
            if (!b.started) continue;
            if (b.defenders.stream().anyMatch(d -> !b.defeated.contains(d.getUUID()) && (!d.isAlive() || !level.hasChunkAt(d.blockPosition())))) { finish(b, false); continue; }
            for (int i = 0; i < b.defenders.size(); i++) {
                var d = b.defenders.get(i); if (!d.isAlive()) continue;
                d.setPos(sentry(b, i)); d.setDeltaMovement(Vec3.ZERO); d.clearFire();
                var target = b.members.stream().map(id -> level.getServer().getPlayerList().getPlayer(id)).min(Comparator.comparingDouble(d::distanceToSqr)).orElse(null);
                if (target != null) {
                    d.getLookControl().setLookAt(target, 30, 30);
                    d.getLookControl().tick(); d.setYRot(d.getYHeadRot());
                    if (level.getGameTime() % 40 == i * 10 && d.hasLineOfSight(target)) d.performRangedAttack(target, .8F);
                }
            }
            if (level.getGameTime() % 20 == 0) update(b);
        }
    }
    public static void initialize() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(Ships.BOARDING_DEFENDER, BoardingDefender.createAttributes());
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("boarding")
            .requires(s -> s.getEntity() instanceof ServerPlayer).executes(c -> status(c.getSource().getPlayer()))
            .then(Commands.literal("status").executes(c -> status(c.getSource().getPlayer())))
            .then(Commands.literal("form").executes(c -> form(c.getSource().getPlayer())))
            .then(Commands.literal("join").then(Commands.argument("leader", EntityArgument.player()).executes(c -> join(c.getSource().getPlayer(), EntityArgument.getPlayer(c, "leader")))))
            .then(Commands.literal("start").executes(c -> start(c.getSource().getPlayer())))
            .then(Commands.literal("leave").executes(c -> leave(c.getSource().getPlayer())))));
        ServerTickEvents.END_SERVER_TICK.register(server -> { var level = server.getLevel(Pirate.LEVEL); if (level != null) tick(level); });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getDirectEntity() != null && source.getDirectEntity().entityTags().contains(SHOT_TAG))
                return source.getEntity() instanceof BoardingDefender defender && canTarget(defender, entity);
            return !(source.getEntity() instanceof BoardingDefender defender) || canTarget(defender, entity);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            var b = battle(entity); if (b == null || b.won) return;
            if (!valid(b) || !canTarget(entity, source.getEntity())) { finish(b, false); return; }
            b.defeated.add(entity.getUUID());
            if (b.defeated.size() == b.defenders.size()) finish(b, true); else update(b);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof BoardingDefender && battle(entity) == null) entity.discard();
            if (entity instanceof net.minecraft.world.entity.projectile.Projectile shot) {
                if (shot.getOwner() instanceof BoardingDefender defender) {
                    entity.addTag(SHOT_TAG);
                    if (shot instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) arrow.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;
                    var b = battle(defender);
                    if (b == null) entity.discard(); else entity.addTag(SHOT_TAG + "_" + b.ship.getUUID());
                } else if (entity.entityTags().contains(SHOT_TAG)) entity.discard();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }
}
