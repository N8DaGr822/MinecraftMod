package darkspawn.black.pirate;

import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** One bounded, optional shore battle per Redwater Landing; no ongoing faction simulation. */
public final class FactionSkirmishes {
    public static final String TAG = "darkspawn_faction_skirmish";
    public static final int SPENT = 8;
    private static final Map<ServerLevel, Map<BlockPos, Battle>> ACTIVE = new WeakHashMap<>();
    private static final class Battle {
        final BlockPos center;
        final long expires;
        final List<Mob> units = new ArrayList<>();
        final Set<UUID> defeated = new HashSet<>();
        final Map<UUID, Integer> attacked = new HashMap<>();
        Battle(BlockPos center, long expires) { this.center = center; this.expires = expires; }
    }
    private FactionSkirmishes() { }
    public static boolean combatant(Entity entity) { return entity.entityTags().contains(TAG); }
    private static Battle battle(Entity entity) {
        if (!combatant(entity)) return null;
        return ACTIVE.getOrDefault(entity.level(), Map.of()).values().stream()
            .filter(b -> b.units.contains(entity)).findFirst().orElse(null);
    }
    private static int side(Entity entity) {
        return PirateDiplomacy.faction(entity) == PirateProgression.Faction.CORSAIRS ? 1 : 2;
    }
    private static boolean inside(Entity entity, Battle battle) {
        return entity.distanceToSqr(Vec3.atCenterOf(battle.center)) <= 48 * 48;
    }
    public static boolean canAttack(Mob attacker, LivingEntity target) {
        var battle = battle(attacker);
        if (battle == null || !attacker.isAlive() || !target.isAlive() || target.level() != attacker.level()
                || !inside(attacker, battle) || !inside(target, battle)) return false;
        if (target instanceof Mob mob) return battle.units.contains(mob) && side(attacker) != side(mob);
        if (target instanceof ServerPlayer player) return !player.isCreative() && !player.isSpectator()
            && ((battle.attacked.getOrDefault(player.getUUID(), 0) & side(attacker)) != 0
                || PirateProgression.standing(Pirate.state(player), PirateDiplomacy.faction(attacker)) < 0);
        return false;
    }
    public static boolean observe(ServerPlayer player, IslandDiscoveries.Island island) {
        var level = player.level();
        if (island.kind() != 6 || !level.dimension().equals(Pirate.LEVEL) || !player.isAlive() || player.isSpectator() || player.isCreative()
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
                || player.distanceToSqr(Vec3.atCenterOf(island.center())) > 40 * 40
                || island.center().getX() * (long)island.center().getX() + island.center().getZ() * (long)island.center().getZ() < 160 * 160) return false;
        var saved = level.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
        var active = ACTIVE.computeIfAbsent(level, key -> new HashMap<>());
        if ((saved.mask(island.center()) & SPENT) != 0 || active.containsKey(island.center()) || active.size() >= darkspawn.black.ServerTuning.skirmishCap) return false;
        var battle = new Battle(island.center(), level.getGameTime() + 6000);
        for (int side = 0; side < 2; side++) for (int slot = 0; slot < 2; slot++) {
            var pos = island.center().offset(side == 0 ? -8 : 8, 0, slot == 0 ? -3 : 3);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isSolidRender() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                    || level.players().stream().anyMatch(p -> p.distanceToSqr(Vec3.atCenterOf(pos)) < 16)) { discard(battle); return false; }
            Mob mob = side == 0 ? EntityTypes.PIGLIN_BRUTE.create(level, EntitySpawnReason.EVENT) : EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
            if (mob == null) { discard(battle); return false; }
            mob.setPos(Vec3.atBottomCenterOf(pos));
            if (!level.noCollision(mob) || !level.getEntities(mob, mob.getBoundingBox()).isEmpty()) { mob.discard(); discard(battle); return false; }
            mob.addTag(TAG); mob.addTag(PirateDiplomacy.FACTION_TAG + (side == 0 ? "corsairs" : "iron_fleet"));
            mob.setPersistenceRequired(); mob.setCustomName(Component.literal(side == 0 ? "Redwater Corsair" : "Redwater Fleet Marine")); mob.setCustomNameVisible(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(32); mob.setHealth(32);
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(side == 0 ? Items.GOLDEN_AXE : Items.CROSSBOW)); mob.setDropChance(EquipmentSlot.MAINHAND, 0);
            if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
            if (mob instanceof net.minecraft.world.entity.raid.Raider raider) raider.setCanJoinRaid(false);
            battle.units.add(mob);
        }
        active.put(island.center(), battle);
        for (var mob : battle.units) if (!level.addFreshEntity(mob)) { finish(level, battle, 0); return false; }
        // Durable Start: Abandoned or interrupted battles never become an infinite reinforcement/reward farm.
        saved.record(island.center(), saved.mask(island.center()) | SPENT);
        level.players().stream().filter(p -> p.distanceToSqr(Vec3.atCenterOf(island.center())) <= 64 * 64)
            .forEach(p -> p.sendSystemMessage(Component.literal("Redwater skirmish: Corsairs face the Iron Fleet. Watch, salvage or help one side. Neutral bystanders are spared; attacking either faction has consequences.")));
        for (var mob : battle.units) target(mob, battle);
        return true;
    }
    private static void target(Mob mob, Battle battle) {
        LivingEntity target = battle.units.stream().filter(other -> canAttack(mob, other) && mob.hasLineOfSight(other))
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (target == null && mob.level() instanceof ServerLevel level) target = level.players().stream()
            .filter(p -> canAttack(mob, p) && mob.hasLineOfSight(p)).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        mob.setTarget(target);
        if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin) {
            // Piglin Brain: Native target validation requires anger as well as an attack target.
            if (target == null) {
                mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            } else {
                mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 40);
                mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
        }
    }
    private static void discard(Battle battle) { battle.units.forEach(Entity::discard); }
    private static void finish(ServerLevel level, Battle battle, int winner) {
        ACTIVE.getOrDefault(level, Map.of()).remove(battle.center);
        if (winner != 0) for (var entry : battle.attacked.entrySet()) {
            if (entry.getValue() != (winner == 1 ? 2 : 1)) continue;
            var player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != level || !player.isAlive() || player.isSpectator() || player.isCreative() || !inside(player, battle)) continue;
            var faction = winner == 1 ? PirateProgression.Faction.CORSAIRS : PirateProgression.Faction.FLEET;
            PirateProgression.addStanding(Pirate.state(player), faction, 10); SeaLegends.renown(player, 3);
            player.sendSystemMessage(Component.literal(faction.title + " thanks you: +10 standing, +3 renown. The landing's skirmish is settled."));
        }
        discard(battle);
    }
    public static void tick(ServerLevel level) {
        for (var battle : List.copyOf(ACTIVE.getOrDefault(level, Map.of()).values())) {
            if (level.getGameTime() >= battle.expires || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
                    || level.players().stream().noneMatch(p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(Vec3.atCenterOf(battle.center)) <= 64 * 64)) {
                finish(level, battle, 0); continue;
            }
            // Missing/unloaded/deleted actors are not victories. Only actual death callbacks resolve a side.
            if (battle.units.stream().anyMatch(m -> !battle.defeated.contains(m.getUUID()) && (!m.isAlive() || m.level() != level || !inside(m, battle)))) {
                finish(level, battle, 0); continue;
            }
            boolean corsairs = battle.units.stream().anyMatch(m -> side(m) == 1 && !battle.defeated.contains(m.getUUID()));
            boolean fleet = battle.units.stream().anyMatch(m -> side(m) == 2 && !battle.defeated.contains(m.getUUID()));
            if (!corsairs || !fleet) { finish(level, battle, corsairs ? 1 : fleet ? 2 : 0); continue; }
            for (var mob : battle.units) if (mob.isAlive()) target(mob, battle);
        }
    }
    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            var battle = battle(entity);
            if (battle != null && taken > 0 && !blocked && source.getEntity() instanceof ServerPlayer player
                    && player.level() == entity.level() && player.isAlive() && !player.isCreative() && !player.isSpectator() && inside(player, battle))
                battle.attacked.merge(player.getUUID(), side(entity), (a, b) -> a | b);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            var battle = battle(entity);
            if (battle == null) return;
            battle.defeated.add(entity.getUUID());
            // Killing Blows: Fatal damage does not necessarily emit AFTER_DAMAGE.
            if (source.getEntity() instanceof ServerPlayer player && player.level() == entity.level()
                    && player.isAlive() && !player.isCreative() && !player.isSpectator() && inside(player, battle))
                battle.attacked.merge(player.getUUID(), side(entity), (a, b) -> a | b);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTickCount() % 20 == 0) { var level = server.getLevel(Pirate.LEVEL); if (level != null) tick(level); } });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (combatant(entity) && battle(entity) == null) entity.discard(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }
}
