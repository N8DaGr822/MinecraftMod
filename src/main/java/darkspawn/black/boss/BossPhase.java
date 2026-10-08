package darkspawn.black.boss;

public final class BossPhase {
	private BossPhase() {
	}

	public static int advance(int current, float health, float maxHealth) {
		int next = health <= maxHealth * 0.35F ? 3 : health <= maxHealth * 0.70F ? 2 : 1;
		// Phase Progression: Healing must never reset phase transitions or respawn healing nodes.
		return Math.max(Math.clamp(current, 1, 3), next);
	}
}
