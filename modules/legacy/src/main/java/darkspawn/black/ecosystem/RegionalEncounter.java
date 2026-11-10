package darkspawn.black.ecosystem;

import darkspawn.black.ServerTuning;
import net.minecraft.world.level.biome.Biomes;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Explicit solo/cooperative hunts; live encounters are transient, cooldowns and earned claims are saved. */
public final class RegionalEncounter {
	public final String TAG;
    private final String title, command, habitat;
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome;
    private final RegionalKind kind;
    private final net.minecraft.resources.ResourceKey<Level> dimension;
    private final boolean surface;
    private final net.minecraft.world.item.Item reward;
    private final net.minecraft.world.level.saveddata.SavedDataType<RegionalEncounterLedger> ledgerType;
    public RegionalEncounter(String command, String title, String habitat, net.minecraft.resources.ResourceKey<Level> dimension,
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome, RegionalKind kind, boolean surface,
        net.minecraft.world.item.Item reward) {
        this.dimension=dimension; this.command=command; this.title=title; this.habitat=habitat; this.biome=biome; this.kind=kind; this.surface=surface; this.reward=reward;
        TAG="darkspawn_event_"+command;
        ledgerType=new net.minecraft.world.level.saveddata.SavedDataType<>(darkspawn.black.Darkspawn.id(command),
            () -> new RegionalEncounterLedger(List.of()), RegionalEncounterLedger.CODEC, null);
    }
    public RegionalEncounterLedger ledger(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(ledgerType); }
	private final Map<ServerLevel, Map<UUID, Hunt>> ACTIVE = new WeakHashMap<>();
	private final class Hunt {
		final UUID leader;
		final Vec3 center;
		final boolean cooperative;
		final Set<UUID> members = new LinkedHashSet<>();
		final List<RegionalMob> combatants = new ArrayList<>();
		final Set<UUID> defeated = new HashSet<>();
		final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.literal(title), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		long expires;
		boolean started;
		Hunt(ServerPlayer p, boolean cooperative) {
			leader = p.getUUID(); center = p.position(); this.cooperative = cooperative;
			members.add(leader); expires = p.level().getGameTime() + 1200;
		}
	}

	private Map<UUID, Hunt> hunts(ServerLevel level) { return ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>()); }
	private Hunt member(ServerPlayer p) {
		return hunts(p.level()).values().stream().filter(h -> h.members.contains(p.getUUID())).findFirst().orElse(null);
	}
	private Hunt battle(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level) || !combatant(entity)) return null;
		return hunts(level).values().stream().filter(h -> h.combatants.contains(entity)).findFirst().orElse(null);
	}
	public boolean combatant(Entity entity) { return entity.entityTags().contains(TAG); }
	private boolean survivor(ServerPlayer p) { return p.isAlive() && p.gameMode() == net.minecraft.world.level.GameType.SURVIVAL; }
	private boolean inside(ServerPlayer p, Hunt hunt) { return p.position().distanceToSqr(hunt.center) <= 48 * 48; }
	private boolean unlocked(ServerPlayer p) {
		var end = p.level().getServer().getLevel(Level.END);
		return end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon();
	}
	private boolean eligible(ServerPlayer p) {
		var state = darkspawn.black.oneblock.OneBlock.state(p);
		return ServerTuning.regionalEvents && survivor(p) && p.level().dimension() == dimension
			&& !state.inside && !state.ascensionInside && !state.travelling
			&& p.level().getDifficulty() != Difficulty.PEACEFUL && unlocked(p)
			&& (p.level().getBiome(p.blockPosition()).is(biome) || kind == RegionalKind.VOIDLING && (p.level().getBiome(p.blockPosition()).is(Biomes.END_MIDLANDS) || p.level().getBiome(p.blockPosition()).is(Biomes.END_BARRENS) || p.level().getBiome(p.blockPosition()).is(Biomes.THE_END)))
			&& (!surface || p.level().canSeeSky(p.blockPosition()));
	}
    private int message(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text.replace("{title}",title).replace("{command}",command)
            .replace("{habitat}",habitat).replace("{mobs}",kind.name().toLowerCase(java.util.Locale.ROOT).replace('_',' ') + "s")
            .replace("{reward}","4 " + new ItemStack(reward).getHoverName().getString())));
        return 0;
    }
	public void offer(ServerPlayer p) {
		if (eligible(p) && member(p) == null && ledger(p.level()).offer(p.getUUID(), p.level().getGameTime(), ServerTuning.eventTicks))
			message(p, "{title} available: /{command} solo (3 {mobs}) or /{command} form (2–4 players, 6 {mobs}). Optional; nothing spawns until you start. Reward: {reward} each.");
	}
	public int form(ServerPlayer p, boolean cooperative) {
		if (!eligible(p)) return message(p, "{title} needs Survival, {habitat}, non-Peaceful difficulty and the world's dragon victory.");
		var saved = ledger(p.level());
		if (!saved.ready(p.getUUID(), p.level().getGameTime())) return status(p);
		if (member(p) != null) return message(p, "You already belong to a hunt. Use /{command} status or /{command} leave.");
		if (hunts(p.level()).size() >= 4 || hunts(p.level()).values().stream().anyMatch(h -> h.center.distanceToSqr(p.position()) < 128 * 128))
			return message(p, "Hunt budget full or another hunt is within 128 blocks. Move away or wait.");
		var hunt = new Hunt(p, cooperative);
		hunts(p.level()).put(hunt.leader, hunt);
		if (!cooperative) {
			int result = start(p);
			if (!hunt.started) finish(p.level(), hunt, false, "No combatants spawned; no cooldown charged.");
			return result;
		}
		return message(p, "Cooperative lobby open for 60 seconds. Nearby players: /{command} join " + p.getGameProfile().name()
			+ ". Leader: /{command} start. 2–4 players, 6 {mobs}, 5 minutes. Everyone must stay alive within 48 blocks; any departure ends the hunt.");
	}
	public int join(ServerPlayer p, ServerPlayer leader) {
		var hunt = hunts(leader.level()).get(leader.getUUID());
		if (!eligible(p) || p.level() != leader.level() || member(p) != null || hunt == null || !hunt.cooperative || hunt.started
			|| hunt.members.size() >= 4 || p.position().distanceToSqr(hunt.center) > 32 * 32
			|| p.level().getGameTime() >= hunt.expires || !ledger(p.level()).ready(p.getUUID(), p.level().getGameTime()))
			return message(p, "Cannot join: use an open nearby lobby, stay in {habitat}, and finish any reward/cooldown first.");
		hunt.members.add(p.getUUID());
		message(leader, p.getGameProfile().name() + " joined. Party: " + hunt.members.size() + "/4.");
		return message(p, "Joined {title}. The leader starts it. Stay alive within 48 blocks; any departure ends the hunt. /{command} leave opts out.");
	}
    private boolean validGround(net.minecraft.world.level.block.state.BlockState ground) {
        return switch (kind) {
            case SPORELING -> ground.is(Blocks.MYCELIUM) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT);
            case VOIDLING -> ground.is(Blocks.END_STONE);
            case LOST_SOUL -> ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL);
            default -> ground.is(Blocks.SCULK) || ground.is(Blocks.DEEPSLATE) || ground.is(Blocks.STONE);
        };
    }
	public int start(ServerPlayer p) {
		var hunt = hunts(p.level()).get(p.getUUID());
		if (hunt == null || hunt.started) return message(p, "Only the leader of an open lobby can start.");
		var level = p.level();
		if (level.getGameTime() >= hunt.expires || hunt.cooperative && hunt.members.size() < 2) return message(p, "Lobby expired or fewer than two cooperative players.");
		var saved = ledger(level);
		for (UUID id : hunt.members) {
			var player = level.getServer().getPlayerList().getPlayer(id);
			if (player == null || player.level() != level || !eligible(player) || !inside(player, hunt) || !saved.ready(id, level.getGameTime()))
				return message(p, "Every member must be present, eligible and ready before starting.");
		}
		int count = hunt.cooperative ? 6 : 3;
		// Spawn Transaction: Validate the entire finite pack in loaded terrain before publishing any entity.
		for (int attempt = 0; attempt < 12 && hunt.combatants.size() < count; attempt++) {
			double angle = attempt * Math.PI / 6;
			var base = BlockPos.containing(hunt.center.add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12));
			if (!level.hasChunkAt(base)) continue;
			for (int dy = 3; dy >= -3; dy--) {
				var pos = base.offset(0, dy, 0);
				if (!level.hasChunkAt(pos.offset(-2, 0, -2)) || !level.hasChunkAt(pos.offset(-2, 0, 2))
					|| !level.hasChunkAt(pos.offset(2, 0, -2)) || !level.hasChunkAt(pos.offset(2, 0, 2))) continue;
				if (!level.isInWorldBounds(pos) || surface && !level.canSeeSky(pos)) continue;
				var ground = level.getBlockState(pos.below());
				// Encounter Ground: Require the configured habitat floor; never rebuild terrain.
				if (!validGround(ground) || !ground.isFaceSturdy(level, pos.below(), Direction.UP)) continue;
				var combatant = new RegionalMob(RegionalEcosystems.MOBS.get(kind), level, kind);
				combatant.setPos(Vec3.atBottomCenterOf(pos));
				var snow = level.getBlockState(pos);
				if (snow.is(Blocks.SNOW) && !snow.getCollisionShape(level, pos).isEmpty())
					combatant.setPos(combatant.position().add(0, snow.getCollisionShape(level, pos).max(Direction.Axis.Y), 0));
				if (!level.getWorldBorder().isWithinBounds(combatant.getBoundingBox()) || !level.noCollision(combatant)
					|| level.containsAnyLiquid(combatant.getBoundingBox()) || !level.getEntities(combatant, combatant.getBoundingBox()).isEmpty()) { combatant.discard(); continue; }
				combatant.markEncounter(TAG);
				combatant.setCustomName(Component.literal(title)); combatant.setCustomNameVisible(true);
				hunt.combatants.add(combatant); break;
			}
		}
		if (hunt.combatants.size() != count) {
			hunt.combatants.forEach(Entity::discard); hunt.combatants.clear();
			return message(p, "Not enough clear, dry, loaded natural ground within 12 blocks. Move the lobby to an {habitat}.");
		}
		hunt.started = true; hunt.expires = level.getGameTime() + 6000;
		for (var combatant : hunt.combatants) if (!level.addFreshEntity(combatant)) {
			finish(level, hunt, false, "Creature placement failed; no cooldown charged."); return 0;
		}
		for (UUID id : hunt.members) {
			saved.started(id, level.getGameTime(), ServerTuning.eventTicks);
			var player = level.getServer().getPlayerList().getPlayer(id);
			hunt.bar.addPlayer(player);
			message(player, "{title} started: defeat " + count + " marked {mobs} in 5 minutes. Stay alive within 48 blocks. Reward: {reward} each via /{command} claim.");
		}
		update(level, hunt);
		return 1;
	}
	public boolean canTarget(Entity combatant, LivingEntity target) {
		var hunt = battle(combatant);
		return hunt != null && hunt.started && ServerTuning.regionalEvents && combatant.level().getGameTime() < hunt.expires
			&& target instanceof ServerPlayer p && p.level() == combatant.level()
			&& hunt.members.contains(p.getUUID()) && survivor(p) && inside(p, hunt);
	}
	public void tickMob(RegionalMob combatant) {
		var hunt = battle(combatant);
		if (hunt == null || !hunt.started || combatant.position().distanceToSqr(hunt.center) > 48 * 48) {
			if (hunt != null) finish((ServerLevel) combatant.level(), hunt, false, "A hunt combatant left the arena.");
			else combatant.discard();
			return;
		}
		var target = ((ServerLevel) combatant.level()).players().stream().filter(p -> canTarget(combatant, p))
			.min(Comparator.comparingDouble(combatant::distanceToSqr)).orElse(null);
		combatant.setTarget(target);
	}
	private void finish(ServerLevel level, Hunt hunt, boolean won, String reason) {
		if (hunts(level).remove(hunt.leader) != hunt) return;
		if (won) for (UUID id : hunt.members) ledger(level).won(id);
		hunt.bar.removeAllPlayers();
		hunt.combatants.forEach(Entity::discard);
		for (UUID id : hunt.members) {
			var p = level.getServer().getPlayerList().getPlayer(id);
			if (p != null) message(p, reason);
		}
	}
	public int leave(ServerPlayer p) {
		var hunt = member(p);
		if (hunt == null) return message(p, "You are not in a hunt.");
		if (hunt.started || hunt.leader.equals(p.getUUID())) finish(p.level(), hunt, false, "{title} cancelled: a participant left. Started-hunt cooldowns remain.");
		else { hunt.members.remove(p.getUUID()); message(p, "Left the {title} lobby."); }
		return 1;
	}
	private void update(ServerLevel level, Hunt hunt) {
        // Rift Preview: Cosmetic particles only; no portal blocks or travel destination exists.
        if (kind == RegionalKind.VOIDLING) level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
            hunt.center.x, hunt.center.y + 1, hunt.center.z, 12, .8, 1, .8, .02);
		hunt.bar.setName(Component.literal(title + ": " + hunt.defeated.size() + "/" + hunt.combatants.size()
			+ " defeated · " + Math.max(0, (hunt.expires - level.getGameTime()) / 20) + "s · stay within 48 blocks"));
		hunt.bar.setProgress((float) hunt.defeated.size() / hunt.combatants.size());
		for (UUID id : hunt.members) {
			var p = level.getServer().getPlayerList().getPlayer(id);
			if (p != null) p.sendOverlayMessage(Component.literal("Hunt boundary: " + (int) Math.sqrt(p.position().distanceToSqr(hunt.center)) + "/48 blocks from start"));
		}
	}
	public void tick(ServerLevel level) {
		for (var hunt : List.copyOf(hunts(level).values())) {
			boolean valid = ServerTuning.regionalEvents && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameTime() < hunt.expires;
			for (UUID id : hunt.members) {
				var p = level.getServer().getPlayerList().getPlayer(id);
				if (p == null || p.level() != level || !survivor(p) || !inside(p, hunt)) valid = false;
			}
			if (!valid) { finish(level, hunt, false, "{title} ended: timer, departure, death, or event settings changed. No reward."); continue; }
			if (!hunt.started) continue;
			if (hunt.combatants.stream().anyMatch(w -> !hunt.defeated.contains(w.getUUID()) && (w.isRemoved() || !w.isAlive()
				|| !level.hasChunkAt(w.blockPosition())))) {
				finish(level, hunt, false, "{title} interrupted: a combatant disappeared. No reward."); continue;
			}
			update(level, hunt);
		}
	}
	public int status(ServerPlayer p) {
		var saved = ledger(p.level());
		var hunt = member(p);
		if (hunt != null) return message(p, hunt.started ? "Hunt active: " + hunt.defeated.size() + "/" + hunt.combatants.size()
			+ " defeated. /{command} leave cancels." : "Lobby: " + hunt.members.size() + "/4. Leader uses /{command} start; /{command} leave opts out.");
		if (saved.reward(p.getUUID())) return message(p, "Reward ready: {reward}. Leave one empty inventory slot and use /{command} claim in the Overworld.");
		long wait = saved.remaining(p.getUUID(), p.level().getGameTime());
		return message(p, wait > 0 ? "{title} cooldown: " + (wait + 19) / 20 + " seconds."
			: "{title}: /{command} solo or /{command} form, then friends /{command} join <leader>. Requires {habitat} and world dragon victory.");
	}
	public int claim(ServerPlayer p) {
		var state = darkspawn.black.oneblock.OneBlock.state(p);
		if (!survivor(p) || p.level().dimension() != Level.OVERWORLD || state.inside || state.ascensionInside || state.travelling
			|| p.containerMenu != p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())
			return message(p, "Claim in Overworld Survival after closing containers and finishing island travel.");
		var saved = ledger(p.level());
		if (!saved.reward(p.getUUID())) return message(p, "No {title} reward is pending.");
		int slot = p.getInventory().getFreeSlot();
		if (slot < 0) return message(p, "Leave one empty inventory slot; your reward remains saved.");
		if (!saved.claim(p.getUUID())) return 0;
		p.getInventory().setItem(slot, new ItemStack(reward, 4)); p.getInventory().setChanged(); p.inventoryMenu.broadcastChanges();
		message(p, "Claimed {reward}. This hunt's reward is now spent."); return 1;
	}
	public void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal(command)
			.requires(s -> s.getEntity() instanceof ServerPlayer).executes(c -> status(c.getSource().getPlayer()))
			.then(Commands.literal("status").executes(c -> status(c.getSource().getPlayer())))
			.then(Commands.literal("solo").executes(c -> form(c.getSource().getPlayer(), false)))
			.then(Commands.literal("form").executes(c -> form(c.getSource().getPlayer(), true)))
			.then(Commands.literal("join").then(Commands.argument("leader", EntityArgument.player()).executes(c -> join(c.getSource().getPlayer(), EntityArgument.getPlayer(c, "leader")))))
			.then(Commands.literal("start").executes(c -> start(c.getSource().getPlayer())))
			.then(Commands.literal("leave").executes(c -> leave(c.getSource().getPlayer())))
			.then(Commands.literal("claim").executes(c -> claim(c.getSource().getPlayer())))));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			var hunt = battle(entity);
			if (hunt == null) return;
			var level = (ServerLevel) entity.level();
			if (!(source.getEntity() instanceof ServerPlayer p) || !canTarget(entity, p)) {
				finish(level, hunt, false, "{title} interrupted by a non-participant kill. No reward."); return;
			}
			hunt.defeated.add(entity.getUUID());
			if (hunt.defeated.size() == hunt.combatants.size()) {
				// Final Blow: Revalidate the whole party before granting each once-only entitlement.
				tick(level);
				if (hunts(level).get(hunt.leader) == hunt) finish(level, hunt, true, "{title} complete! Each participant can /{command} claim for {reward}.");
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTickCount() % 20 == 0) tick(server.getLevel(dimension)); });
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (combatant(entity) && battle(entity) == null) entity.discard(); });
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}
}
