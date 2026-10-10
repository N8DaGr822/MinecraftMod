package darkspawn.black.audio;

import darkspawn.black.boss.*;
import darkspawn.black.ecosystem.*;
import net.minecraft.world.entity.Mob;

/** One server-authored cue when a committed warning begins, not once per warning tick. */
public final class CreatureAudio {
	private CreatureAudio() {}
	public static int warning(Mob mob) {
		return switch (mob) {
			case BiomeBoss b -> b.windup();
			case AncientTreeSpirit b -> b.windup();
			case MutantWolf b -> b.windup();
			case RegionalMob m -> m.warning();
			case ForestMob m -> m.windup();
			case TaigaWolf m -> m.windup();
			case BossMinion m -> m.windup();
			default -> 0;
		};
	}
	public static boolean startsWarning(int previous, int current) { return current > previous && current > 0; }
	public static boolean boss(Mob mob) { return mob instanceof BiomeBoss || mob instanceof AncientTreeSpirit || mob instanceof MutantWolf; }
	public static int phase(Mob mob) {
		return switch(mob) {
			case BiomeBoss b -> b.phase();
			case AncientTreeSpirit b -> b.phase();
			case MutantWolf b -> b.phase();
			default -> 1;
		};
	}
}
