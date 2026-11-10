package darkspawn.black.pirate.compat;

import darkspawn.black.pirate.PirateDiplomacy;
import darkspawn.black.pirate.PirateShip;

/** Legacy of Noah's relic retaliation spares loyal crew and diplomatic contacts. Loaded only when Noah is installed. */
public final class NoahCompat {
	private NoahCompat() { }
	public static void initialize() {
		darkspawn.black.noah.NoahLegacy.registerSpared(entity -> PirateShip.loyalCrew(entity) || PirateDiplomacy.contact(entity));
	}
}
