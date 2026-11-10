package darkspawn.black.creaturekit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Audio cues a creature reports about itself. One server-authored cue plays when a committed warning begins, not once
 * per warning tick; bosses also drive the client's local encounter score ({@code music.<theme>.<phase>}).
 */
public interface CreatureAudio {
	/** Ticks left on the current telegraph; 0 when idle. */
	default int audioWarning() { return 0; }
	/** Bosses play louder and start the encounter score. */
	default boolean audioBoss() { return false; }
	default int audioPhase() { return 1; }
	/** Encounter-score theme; the sound event is {@code darkspawn:music.<theme>.<phase>}. */
	default String musicTheme() { return BuiltInRegistries.ENTITY_TYPE.getKey(((Entity) this).getType()).getPath(); }

	static int warning(Mob mob) { return mob instanceof CreatureAudio audio ? audio.audioWarning() : 0; }
	static boolean startsWarning(int previous, int current) { return current > previous && current > 0; }
	static boolean boss(Mob mob) { return mob instanceof CreatureAudio audio && audio.audioBoss(); }
	static int phase(Mob mob) { return mob instanceof CreatureAudio audio ? audio.audioPhase() : 1; }
}
