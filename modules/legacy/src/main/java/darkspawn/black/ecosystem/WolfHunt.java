package darkspawn.black.ecosystem;

import darkspawn.black.ServerTuning;
import darkspawn.black.boss.MutantWolfSummonItem;
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
public final class WolfHunt {
	public static final String TAG = "darkspawn_wolf_hunt";
	private static final Map<ServerLevel, Map<UUID, Hunt>> ACTIVE = new WeakHashMap<>();
	private static final class Hunt {
		final UUID leader;
		final Vec3 center;
		final boolean cooperative;
		final Set<UUID> members = new LinkedHashSet<>();
		final List<TaigaWolf> wolves = new ArrayList<>();
		final Set<UUID> defeated = new HashSet<>();
		final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("Wolf Hunt"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		long expires;
		boolean started;
		Hunt(ServerPlayer p, boolean cooperative) {
			leader = p.getUUID(); center = p.position(); this.cooperative = cooperative;
			members.add(leader); expires = p.level().getGameTime() + 1200;
		}
	}
	private WolfHunt() { }
	private static Map<UUID, Hunt> hunts(ServerLevel level) { return ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>()); }
	private static Hunt member(ServerPlayer p) {
		return hunts(p.level()).values().stream().filter(h -> h.members.contains(p.getUUID())).findFirst().orElse(null);
	}
	private static Hunt battle(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level) || !combatant(entity)) return null;
		return hunts(level).values().stream().filter(h -> h.wolves.contains(entity)).findFirst().orElse(null);
	}
	public static boolean combatant(Entity entity) { return entity.entityTags().contains(TAG); }
	private static boolean survivor(ServerPlayer p) { return p.isAlive() && p.gameMode() == net.minecraft.world.level.GameType.SURVIVAL; }
	private static boolean inside(ServerPlayer p, Hunt hunt) { return p.position().distanceToSqr(hunt.center) <= 48 * 48; }
	private static boolean unlocked(ServerPlayer p) {
		return darkspawn.black.core.DragonGate.defeated(p.level().getServer());
	}
	private static boolean eligible(ServerPlayer p) {
		return ServerTuning.regionalEvents && survivor(p) && p.level().dimension() == Level.OVERWORLD
			&& !darkspawn.black.core.hook.TravelBoundary.isolated(p)
			&& p.level().getDifficulty() != Difficulty.PEACEFUL && unlocked(p)
			&& p.level().getBiome(p.blockPosition()).is(MutantWolfSummonItem.TAIGAS)
			&& p.level().canSeeSky(p.blockPosition());
	}
	private static int message(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); return 0; }
	public static void offer(ServerPlayer p) {
		if (eligible(p) && member(p) == null && WolfHuntLedger.get(p.level()).offer(p.getUUID(), p.level().getGameTime(), ServerTuning.eventTicks))
			message(p, "Wolf Hunt available: /wolfhunt solo (3 wolves) or /wolfhunt form (2–4 players, 6 wolves). Optional; nothing spawns until you start. Reward: 4 emeralds each.");
	}
	public static int form(ServerPlayer p, boolean cooperative) {
		if (!eligible(p)) return message(p, "Wolf Hunt needs Survival, a surface Overworld taiga, non-Peaceful difficulty and the world's dragon victory.");
		var saved = WolfHuntLedger.get(p.level());
		if (!saved.ready(p.getUUID(), p.level().getGameTime())) return status(p);
		if (member(p) != null) return message(p, "You already belong to a hunt. Use /wolfhunt status or /wolfhunt leave.");
		if (hunts(p.level()).size() >= 4 || hunts(p.level()).values().stream().anyMatch(h -> h.center.distanceToSqr(p.position()) < 128 * 128))
			return message(p, "Hunt budget full or another hunt is within 128 blocks. Move away or wait.");
		var hunt = new Hunt(p, cooperative);
		hunts(p.level()).put(hunt.leader, hunt);
		if (!cooperative) {
			int result = start(p);
			if (!hunt.started) finish(p.level(), hunt, false, "No wolves spawned; no cooldown charged.");
			return result;
		}
		return message(p, "Cooperative lobby open for 60 seconds. Nearby players: /wolfhunt join " + p.getGameProfile().name()
			+ ". Leader: /wolfhunt start. 2–4 players, 6 wolves, 5 minutes. Everyone must stay alive within 48 blocks; any departure ends the hunt.");
	}
	public static int join(ServerPlayer p, ServerPlayer leader) {
		var hunt = hunts(leader.level()).get(leader.getUUID());
		if (!eligible(p) || p.level() != leader.level() || member(p) != null || hunt == null || !hunt.cooperative || hunt.started
			|| hunt.members.size() >= 4 || p.position().distanceToSqr(hunt.center) > 32 * 32
			|| p.level().getGameTime() >= hunt.expires || !WolfHuntLedger.get(p.level()).ready(p.getUUID(), p.level().getGameTime()))
			return message(p, "Cannot join: use an open nearby lobby, stay in Survival taiga, and finish any reward/cooldown first.");
		hunt.members.add(p.getUUID());
		message(leader, p.getGameProfile().name() + " joined. Party: " + hunt.members.size() + "/4.");
		return message(p, "Joined Wolf Hunt. The leader starts it. Stay alive within 48 blocks; any departure ends the hunt. /wolfhunt leave opts out.");
	}
	public static int start(ServerPlayer p) {
		var hunt = hunts(p.level()).get(p.getUUID());
		if (hunt == null || hunt.started) return message(p, "Only the leader of an open lobby can start.");
		var level = p.level();
		if (level.getGameTime() >= hunt.expires || hunt.cooperative && hunt.members.size() < 2) return message(p, "Lobby expired or fewer than two cooperative players.");
		var saved = WolfHuntLedger.get(level);
		for (UUID id : hunt.members) {
			var player = level.getServer().getPlayerList().getPlayer(id);
			if (player == null || player.level() != level || !eligible(player) || !inside(player, hunt) || !saved.ready(id, level.getGameTime()))
				return message(p, "Every member must be present, eligible and ready before starting.");
		}
		int count = hunt.cooperative ? 6 : 3;
		// Spawn Transaction: Validate the entire finite pack in loaded terrain before publishing any entity.
		for (int attempt = 0; attempt < 12 && hunt.wolves.size() < count; attempt++) {
			double angle = attempt * Math.PI / 6;
			var base = BlockPos.containing(hunt.center.add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12));
			if (!level.hasChunkAt(base)) continue;
			for (int dy = 3; dy >= -3; dy--) {
				var pos = base.offset(0, dy, 0);
				if (!level.hasChunkAt(pos.offset(-2, 0, -2)) || !level.hasChunkAt(pos.offset(-2, 0, 2))
					|| !level.hasChunkAt(pos.offset(2, 0, -2)) || !level.hasChunkAt(pos.offset(2, 0, 2))) continue;
				if (!level.isInWorldBounds(pos) || !level.canSeeSky(pos)) continue;
				var ground = level.getBlockState(pos.below());
				// Taiga Ground: Native dirt tags exclude grass and podzol in this Minecraft version.
				if (!(ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL)
					|| ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.SNOW_BLOCK))
					|| !ground.isFaceSturdy(level, pos.below(), Direction.UP)) continue;
				var wolf = new TaigaWolf(TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE), level, TaigaWolf.Kind.DIRE);
				wolf.setPos(Vec3.atBottomCenterOf(pos));
				var snow = level.getBlockState(pos);
				if (snow.is(Blocks.SNOW) && !snow.getCollisionShape(level, pos).isEmpty())
					wolf.setPos(wolf.position().add(0, snow.getCollisionShape(level, pos).max(Direction.Axis.Y), 0));
				if (!level.getWorldBorder().isWithinBounds(wolf.getBoundingBox()) || !level.noCollision(wolf)
					|| level.containsAnyLiquid(wolf.getBoundingBox()) || !level.getEntities(wolf, wolf.getBoundingBox()).isEmpty()) { wolf.discard(); continue; }
				wolf.markHunt();
				wolf.setCustomName(Component.literal("Wolf Hunt · Dire Wolf")); wolf.setCustomNameVisible(true);
				hunt.wolves.add(wolf); break;
			}
		}
		if (hunt.wolves.size() != count) {
			hunt.wolves.forEach(Entity::discard); hunt.wolves.clear();
			return message(p, "Not enough clear, dry, loaded natural ground within 12 blocks. Move the lobby to an open taiga clearing.");
		}
		hunt.started = true; hunt.expires = level.getGameTime() + 6000;
		for (var wolf : hunt.wolves) if (!level.addFreshEntity(wolf)) {
			finish(level, hunt, false, "Wolf placement failed; no cooldown charged."); return 0;
		}
		for (UUID id : hunt.members) {
			saved.started(id, level.getGameTime(), ServerTuning.eventTicks);
			var player = level.getServer().getPlayerList().getPlayer(id);
			hunt.bar.addPlayer(player);
			message(player, "Wolf Hunt started: defeat " + count + " marked wolves in 5 minutes. Stay alive within 48 blocks. Reward: 4 emeralds each via /wolfhunt claim.");
		}
		update(level, hunt);
		return 1;
	}
	public static boolean canTarget(Entity wolf, LivingEntity target) {
		var hunt = battle(wolf);
		return hunt != null && hunt.started && ServerTuning.regionalEvents && wolf.level().getGameTime() < hunt.expires
			&& target instanceof ServerPlayer p && p.level() == wolf.level()
			&& hunt.members.contains(p.getUUID()) && survivor(p) && inside(p, hunt);
	}
	public static void tickWolf(TaigaWolf wolf) {
		var hunt = battle(wolf);
		if (hunt == null || !hunt.started || wolf.position().distanceToSqr(hunt.center) > 48 * 48) {
			if (hunt != null) finish((ServerLevel) wolf.level(), hunt, false, "A hunt wolf left the arena.");
			else wolf.discard();
			return;
		}
		var target = ((ServerLevel) wolf.level()).players().stream().filter(p -> canTarget(wolf, p))
			.min(Comparator.comparingDouble(wolf::distanceToSqr)).orElse(null);
		wolf.setTarget(target);
	}
	private static void finish(ServerLevel level, Hunt hunt, boolean won, String reason) {
		if (hunts(level).remove(hunt.leader) != hunt) return;
		if (won) for (UUID id : hunt.members) WolfHuntLedger.get(level).won(id);
		hunt.bar.removeAllPlayers();
		hunt.wolves.forEach(Entity::discard);
		for (UUID id : hunt.members) {
			var p = level.getServer().getPlayerList().getPlayer(id);
			if (p != null) message(p, reason);
		}
	}
	public static int leave(ServerPlayer p) {
		var hunt = member(p);
		if (hunt == null) return message(p, "You are not in a hunt.");
		if (hunt.started || hunt.leader.equals(p.getUUID())) finish(p.level(), hunt, false, "Wolf Hunt cancelled: a participant left. Started-hunt cooldowns remain.");
		else { hunt.members.remove(p.getUUID()); message(p, "Left the Wolf Hunt lobby."); }
		return 1;
	}
	private static void update(ServerLevel level, Hunt hunt) {
		hunt.bar.setName(Component.literal("Wolf Hunt: " + hunt.defeated.size() + "/" + hunt.wolves.size()
			+ " defeated · " + Math.max(0, (hunt.expires - level.getGameTime()) / 20) + "s · stay within 48 blocks"));
		hunt.bar.setProgress((float) hunt.defeated.size() / hunt.wolves.size());
		for (UUID id : hunt.members) {
			var p = level.getServer().getPlayerList().getPlayer(id);
			if (p != null) p.sendOverlayMessage(Component.literal("Hunt boundary: " + (int) Math.sqrt(p.position().distanceToSqr(hunt.center)) + "/48 blocks from start"));
		}
	}
	public static void tick(ServerLevel level) {
		for (var hunt : List.copyOf(hunts(level).values())) {
			boolean valid = ServerTuning.regionalEvents && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameTime() < hunt.expires;
			for (UUID id : hunt.members) {
				var p = level.getServer().getPlayerList().getPlayer(id);
				if (p == null || p.level() != level || !survivor(p) || !inside(p, hunt)) valid = false;
			}
			if (!valid) { finish(level, hunt, false, "Wolf Hunt ended: timer, departure, death, or event settings changed. No reward."); continue; }
			if (!hunt.started) continue;
			if (hunt.wolves.stream().anyMatch(w -> !hunt.defeated.contains(w.getUUID()) && (w.isRemoved() || !w.isAlive()
				|| !level.hasChunkAt(w.blockPosition())))) {
				finish(level, hunt, false, "Wolf Hunt interrupted: a wolf disappeared. No reward."); continue;
			}
			update(level, hunt);
		}
	}
	public static int status(ServerPlayer p) {
		var saved = WolfHuntLedger.get(p.level());
		var hunt = member(p);
		if (hunt != null) return message(p, hunt.started ? "Hunt active: " + hunt.defeated.size() + "/" + hunt.wolves.size()
			+ " defeated. /wolfhunt leave cancels." : "Lobby: " + hunt.members.size() + "/4. Leader uses /wolfhunt start; /wolfhunt leave opts out.");
		if (saved.reward(p.getUUID())) return message(p, "Reward ready: 4 emeralds. Leave one empty inventory slot and use /wolfhunt claim in the Overworld.");
		long wait = saved.remaining(p.getUUID(), p.level().getGameTime());
		return message(p, wait > 0 ? "Wolf Hunt cooldown: " + (wait + 19) / 20 + " seconds."
			: "Wolf Hunt: /wolfhunt solo or /wolfhunt form, then friends /wolfhunt join <leader>. Requires a surface taiga and world dragon victory.");
	}
	public static int claim(ServerPlayer p) {
		if (!survivor(p) || p.level().dimension() != Level.OVERWORLD || darkspawn.black.core.hook.TravelBoundary.isolated(p)
			|| p.containerMenu != p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())
			return message(p, "Claim in Overworld Survival after closing containers and finishing island travel.");
		var saved = WolfHuntLedger.get(p.level());
		if (!saved.reward(p.getUUID())) return message(p, "No Wolf Hunt reward is pending.");
		int slot = p.getInventory().getFreeSlot();
		if (slot < 0) return message(p, "Leave one empty inventory slot; your reward remains saved.");
		if (!saved.claim(p.getUUID())) return 0;
		p.getInventory().setItem(slot, new ItemStack(Items.EMERALD, 4)); p.getInventory().setChanged(); p.inventoryMenu.broadcastChanges();
		message(p, "Claimed 4 emeralds. This hunt's reward is now spent."); return 1;
	}
	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("wolfhunt")
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
				finish(level, hunt, false, "Wolf Hunt interrupted by a non-participant kill. No reward."); return;
			}
			hunt.defeated.add(entity.getUUID());
			if (hunt.defeated.size() == hunt.wolves.size()) {
				// Final Blow: Revalidate the whole party before granting each once-only entitlement.
				tick(level);
				if (hunts(level).get(hunt.leader) == hunt) finish(level, hunt, true, "Wolf Hunt complete! Each participant can /wolfhunt claim for 4 emeralds.");
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTickCount() % 20 == 0) tick(server.overworld()); });
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (combatant(entity) && battle(entity) == null) entity.discard(); });
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}
}
