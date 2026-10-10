package darkspawn.black.ecosystem;

import darkspawn.black.boss.BossKind;
import darkspawn.black.boss.BossProfile;
import darkspawn.black.boss.MutantWolfSummonItem;
import darkspawn.black.boss.TreeSpiritSummonItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Sparse biome clues. These identify habitat, never claim a boss or structure has spawned. */
public final class RegionalAtmosphere {
	private RegionalAtmosphere() {}
	public static BossKind region(ServerPlayer player) {
		var level = player.level();
		var pos = player.blockPosition();
		var biome = level.getBiome(pos);
		boolean surface = level.canSeeSky(pos);
		if (level.dimension() == Level.OVERWORLD && surface) {
			if (biome.is(TreeSpiritSummonItem.FORESTS)) return BossKind.ANCIENT_TREE_SPIRIT;
			if (biome.is(MutantWolfSummonItem.TAIGAS)) return BossKind.MUTANT_WOLF;
		}
		for (var profile : BossProfile.values()) {
			if (level.dimension() != profile.dimension() || !biome.is(profile.biomes())) continue;
			if (level.dimension() == Level.OVERWORLD && (profile.underground() ? surface || pos.getY() >= 40 : !surface && !player.isInWater())) continue;
			return profile.kind();
		}
		return null;
	}
	public static SimpleParticleType particle(BossKind kind) {
		return switch (kind) {
			case ANCIENT_TREE_SPIRIT, TITAN_BOA -> ParticleTypes.SPORE_BLOSSOM_AIR;
			case MUTANT_WOLF, FOSSIL_TYRANT, MOUNTAIN_TITAN -> ParticleTypes.WHITE_ASH;
			case MUTANT_ZOMBIE -> ParticleTypes.ASH;
			case THUNDER_BIRD -> ParticleTypes.ELECTRIC_SPARK;
			case BABA_YAGA, MYCELIAL_SOVEREIGN -> ParticleTypes.MYCELIUM;
			case ICE_WYRM -> ParticleTypes.SNOWFLAKE;
			case KRAKEN -> ParticleTypes.BUBBLE_POP;
			case CAVE_CRAWLER -> ParticleTypes.SMOKE;
			case SHADOW_CREEPER_QUEEN -> ParticleTypes.SCULK_SOUL;
			case NETHERBORN -> ParticleTypes.CRIMSON_SPORE;
			case SOULBOUND_COLOSSUS -> ParticleTypes.SOUL;
			case VOID_EYE -> ParticleTypes.REVERSE_PORTAL;
		};
	}
	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			var end = server.getLevel(Level.END);
			if (end == null || end.getDragonFight() == null || !end.getDragonFight().hasPreviouslyKilledDragon()) return;
			for (var player : server.getPlayerList().getPlayers()) {
				// Stagger work, no chunk scans, persistent markers, or world edits.
				if (!player.isAlive() || player.isSpectator() || Math.floorMod(player.tickCount + player.getId(), 200) != 0) continue;
				var kind = region(player);
				if (kind == null) continue;
				var level = player.level();
				level.sendParticles(player, particle(kind), false, false, player.getX(), player.getY()+1.4, player.getZ(), 7, 3, 1, 3, .015);
				if (Math.floorMod(player.tickCount + player.getId(), 1200) != 0) continue;
				var sound = darkspawn.black.audio.DarkspawnSounds.get("ambient." + kind.id());
				player.connection.send(new ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT,
					player.getX(), player.getY(), player.getZ(), .18F, .75F, player.getRandom().nextLong()));
			}
		});
	}
}
