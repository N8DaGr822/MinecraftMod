package darkspawn.black.boss;

import darkspawn.black.creaturekit.BossBolt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** A bounded, warned interlude shared by the three existing awakened boss implementations. */
public final class AwakenedPattern {
	private int opportunities;
	private int age = -1;
	private Vec3 origin = Vec3.ZERO;
	private Vec3 forward = new Vec3(0, 0, 1);
	private final Map<UUID, Integer> warnedAt = new HashMap<>();

	record Zone(double x, double z, double inner, double outer) {
		boolean contains(double x, double z) {
			double distance = (x - this.x) * (x - this.x) + (z - this.z) * (z - this.z);
			return distance >= inner * inner && distance <= outer * outer;
		}
	}

	static List<Zone> zones(BossKind kind, int beat) {
		if (beat < 0 || beat > 2) throw new IllegalArgumentException("Awakened beat must be 0..2");
		return switch (kind) {
			case ANCIENT_TREE_SPIRIT -> List.of(new Zone(0, 0, 6 + beat * 4, 8 + beat * 4));
			case MUTANT_WOLF -> List.of(new Zone((beat - 1) * 6, 0, 0, 3.5));
			case MUTANT_ZOMBIE -> List.of(new Zone(-6, (beat - 1) * 5, 0, 3), new Zone(6, (beat - 1) * 5, 0, 3));
			case FOSSIL_TYRANT -> List.of(new Zone((beat - 1) * 6, -6, 0, 3), new Zone((beat - 1) * 6, 0, 0, 3), new Zone((beat - 1) * 6, 6, 0, 3));
			case THUNDER_BIRD -> List.of(new Zone(4 + beat * 3, 0, 0, 2.5), new Zone(-4 - beat * 3, 0, 0, 2.5),
				new Zone(0, 4 + beat * 3, 0, 2.5), new Zone(0, -4 - beat * 3, 0, 2.5));
			case TITAN_BOA -> List.of(new Zone(0, 0, 12 - beat * 4, 14 - beat * 4));
			case BABA_YAGA -> List.of(orbit(7, beat * Math.PI / 3, 3), orbit(7, beat * Math.PI / 3 + Math.PI * 2 / 3, 3),
				orbit(7, beat * Math.PI / 3 + Math.PI * 4 / 3, 3));
			case MOUNTAIN_TITAN -> List.of(new Zone(-8, (beat - 1) * 7, 0, 3.5), new Zone(0, (beat - 1) * 7, 0, 3.5),
				new Zone(8, (beat - 1) * 7, 0, 3.5));
			case ICE_WYRM -> List.of(new Zone((beat - 1) * 7, (beat - 1) * 7, 0, 4));
			case KRAKEN -> List.of(new Zone(0, 0, beat == 1 ? 0 : 7, beat == 1 ? 5 : 11));
			case CAVE_CRAWLER -> List.of(orbit(6, beat * Math.PI * 2 / 3, 4), orbit(6, beat * Math.PI * 2 / 3 + Math.PI, 4));
			case SHADOW_CREEPER_QUEEN -> List.of(new Zone(0, 0, 0, 3 + beat * 3), new Zone(0, 0, 12 + beat, 14 + beat));
			case MYCELIAL_SOVEREIGN -> List.of(orbit(4 + beat * 3, 0, 2.5), orbit(4 + beat * 3, Math.PI * 2 / 3, 2.5),
				orbit(4 + beat * 3, Math.PI * 4 / 3, 2.5));
			case NETHERBORN -> List.of(new Zone(-7 + beat * 7, -7, 0, 3), new Zone(7 - beat * 7, 7, 0, 3));
			case SOULBOUND_COLOSSUS -> List.of(new Zone(-8 + beat * 4, 0, 2, 4), new Zone(8 - beat * 4, 0, 2, 4));
			case VOID_EYE -> List.of(orbit(8, beat * Math.PI / 2, 4), orbit(8, beat * Math.PI / 2 + Math.PI, 4),
				new Zone(0, 0, beat == 2 ? 0 : 12, beat == 2 ? 3 : 14));
		};
	}

	private static Zone orbit(double distance, double angle, double radius) {
		return new Zone(Math.cos(angle) * distance, Math.sin(angle) * distance, 0, radius);
	}

	public static String name(BossKind kind) {
		return switch (kind) {
			case ANCIENT_TREE_SPIRIT -> "Heartwood Ripples";
			case MUTANT_WOLF -> "Frostfang Trail";
			case MUTANT_ZOMBIE -> "Gravehammer Steps";
			case FOSSIL_TYRANT -> "Fossil Stampede";
			case THUNDER_BIRD -> "Storm Compass";
			case TITAN_BOA -> "Closing Coils";
			case BABA_YAGA -> "Witch's Waltz";
			case MOUNTAIN_TITAN -> "Fault March";
			case ICE_WYRM -> "Glacial Traverse";
			case KRAKEN -> "Tidal Reversal";
			case CAVE_CRAWLER -> "Brood Scissors";
			case SHADOW_CREEPER_QUEEN -> "Shadow Pressure";
			case MYCELIAL_SOVEREIGN -> "Spore Blossom";
			case NETHERBORN -> "Cinder Pincers";
			case SOULBOUND_COLOSSUS -> "Soul Convergence";
			case VOID_EYE -> "Orbital Collapse";
		};
	}

	private static ParticleOptions particle(BossKind kind) {
		return BossBolt.particle(switch (kind) {
			case ANCIENT_TREE_SPIRIT, MYCELIAL_SOVEREIGN -> BossBolt.Kind.SPORE;
			case MUTANT_WOLF, ICE_WYRM -> BossBolt.Kind.FROST;
			case MUTANT_ZOMBIE, FOSSIL_TYRANT, MOUNTAIN_TITAN -> BossBolt.Kind.PHYSICAL;
			case THUNDER_BIRD -> BossBolt.Kind.LIGHTNING;
			case TITAN_BOA -> BossBolt.Kind.POISON;
			case BABA_YAGA -> BossBolt.Kind.HEX;
			case KRAKEN -> BossBolt.Kind.INK;
			case CAVE_CRAWLER -> BossBolt.Kind.WEB;
			case SHADOW_CREEPER_QUEEN, VOID_EYE -> BossBolt.Kind.VOID;
			case NETHERBORN -> BossBolt.Kind.FIRE;
			case SOULBOUND_COLOSSUS -> BossBolt.Kind.SOUL;
		});
	}

	public boolean begin(Mob boss, BossKind kind, ServerPlayer target) {
		if (!boss.entityTags().contains(AwakenedRematches.TAG)) return false;
		if (++opportunities < 4) return false;
		opportunities = 0;
		// Target Commitment: Lock both position and orientation; none of the three strikes tracks a dodging player.
		origin = target.position();
		Vec3 delta = origin.subtract(boss.position()).multiply(1, 0, 1);
		forward = delta.horizontalDistanceSqr() < 0.01 ? new Vec3(0, 0, 1) : delta.normalize();
		age = 0;
		warnedAt.clear();
		return true;
	}

	public void reset() {
		age = -1;
		opportunities = 0;
		warnedAt.clear();
	}

	boolean active() { return age >= 0; }

	public boolean tick(ServerLevel level, Mob boss, BossKind kind, List<ServerPlayer> participants) {
		if (!active()) return false;
		if (!boss.isAlive() || !boss.entityTags().contains(AwakenedRematches.TAG) || participants.isEmpty()) {
			reset();
			return false;
		}
		boss.getNavigation().stop();
		boss.setDeltaMovement(0, Math.min(0, boss.getDeltaMovement().y), 0);
		age++;
		var players = participants.stream().filter(p -> p.level() == level && BiomeBoss.eligible(p)
			&& p.position().distanceToSqr(origin) <= 40 * 40).toList();
		warnedAt.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));
		for (var player : players) {
			boolean arriving = !warnedAt.containsKey(player.getUUID());
			if (arriving) {
				warnedAt.put(player.getUUID(), age);
			}
			if (arriving || age == 61 || age == 101)
				player.sendOverlayMessage(Component.literal(name(kind) + " " + (age <= 60 ? 1 : age <= 100 ? 2 : 3)
					+ "/3: avoid the marked circles and ring bands."));
		}
		int beat = Math.min(2, Math.max(0, (age - 21) / 40));
		var zones = zones(kind, beat);
		if (age <= 140 && (age == 1 || age % 4 == 0)) {
			for (Zone zone : zones) {
				boundary(level, players, particle(kind), zone, zone.outer);
				if (zone.inner > 0) boundary(level, players, particle(kind), zone, zone.inner);
			}
		}
		// Fair Impact: Every beat uses its rendered geometry. Late arrivals get at least two seconds of warning.
		if (age == 60 || age == 100 || age == 140) {
			level.playSound(null, net.minecraft.core.BlockPos.containing(origin), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.5F, 0.8F + beat * 0.2F);
			for (var player : players) {
				Vec3 delta = player.position().subtract(origin);
				double x = delta.x * forward.z - delta.z * forward.x;
				double z = delta.x * forward.x + delta.z * forward.z;
				if (age - warnedAt.get(player.getUUID()) >= 40 && delta.y >= -2 && delta.y <= 4
					&& zones.stream().anyMatch(zone -> zone.contains(x, z)) && boss.hasLineOfSight(player)) {
					player.hurtServer(level, boss.damageSources().mobAttack(boss), 8);
				}
			}
		}
		if (age >= 160) reset();
		return true;
	}

	private void boundary(ServerLevel level, List<ServerPlayer> players, ParticleOptions particle, Zone zone, double radius) {
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI / 16;
			double x = zone.x + Math.cos(angle) * radius, z = zone.z + Math.sin(angle) * radius;
			Vec3 point = origin.add(x * forward.z + z * forward.x, 0.25, -x * forward.x + z * forward.z);
			for (var player : players) level.sendParticles(player, particle, true, true, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
	}
}
