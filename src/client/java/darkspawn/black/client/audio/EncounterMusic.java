package darkspawn.black.client.audio;

import darkspawn.black.audio.CreatureAudio;
import darkspawn.black.audio.DarkspawnSounds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;

/** Local encounter score: sticky nearest boss, phase crossfades, and no server music packets. */
public final class EncounterMusic {
	private static final List<Score> SCORES = new ArrayList<>();
	private static Mob boss;
	private static Object world;
	private static String key;
	private static int ticks;
	private EncounterMusic() {}
	public static void initialize() { ClientTickEvents.END_CLIENT_TICK.register(EncounterMusic::tick); }
	public static boolean active() { return key != null || !SCORES.isEmpty(); }
	private static void tick(Minecraft client) {
		if (client.level != world) {
			for (var score : SCORES) client.getSoundManager().stop(score);
			SCORES.clear(); boss = null; key = null; world = client.level;
		}
		if (client.isPaused()) return;
		for (var score : SCORES) {
			score.age++;
			if (score.fading && ++score.fadeTicks > 45) {
				client.getSoundManager().stop(score);
				score.finish();
			}
		}
		SCORES.removeIf(Score::isStopped);
		if (++ticks % 10 != 0) return;
		boolean eligible = client.level != null && client.player != null && client.player.isAlive() && !client.player.isSpectator();
		if (!eligible) boss = null;
		else {
			if (boss != null && (!boss.isAlive() || boss.isRemoved() || boss.distanceToSqr(client.player) > 80 * 80)) boss = null;
			if (boss == null) {
				double closest = 64 * 64;
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof Mob mob && mob.isAlive() && CreatureAudio.boss(mob)) {
						double distance = mob.distanceToSqr(client.player);
						if (distance < closest) { boss = mob; closest = distance; }
					}
				}
			}
		}
		String next = boss == null ? null : "music." + BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath() + "." + Math.clamp(CreatureAudio.phase(boss), 1, 3);
		boolean changed = !java.util.Objects.equals(next, key);
		// A resource reload or muted start can discard a channel. Retry only when audible.
		boolean missing = key != null && client.options.getSoundSourceVolume(SoundSource.MUSIC) > 0
				&& client.options.getSoundSourceVolume(SoundSource.MASTER) > 0
				&& SCORES.stream().noneMatch(s -> !s.fading && (s.age < 40 || client.getSoundManager().isActive(s)));
		if (changed || missing) {
			for (var score : SCORES) score.fading = true;
			key = next;
			if (key != null) {
				client.getMusicManager().stopPlaying();
				// Bound overlap even if phases change or resources reload repeatedly.
				while (SCORES.size() >= 2) client.getSoundManager().stop(SCORES.removeFirst());
				var score = new Score(key); SCORES.add(score); client.getSoundManager().play(score);
			}
		}
	}
	private static final class Score extends AbstractTickableSoundInstance {
		private boolean fading;
		private int fadeTicks;
		private int age;
		void finish() { stop(); }
		Score(String key) {
			super(DarkspawnSounds.get(key), SoundSource.MUSIC, RandomSource.create());
			looping = true; delay = 0; relative = true; attenuation = SoundInstance.Attenuation.NONE;
			volume = .001F; pitch = 1;
		}
		@Override public void tick() {
			volume = fading ? Math.max(0, volume - .0125F) : Math.min(.5F, volume + .00625F);
			if (fading && volume <= 0) stop();
		}
	}
}
