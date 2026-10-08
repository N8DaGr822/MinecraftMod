package darkspawn.black.health;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import darkspawn.black.boss.BossKind;

public final class BossHeartProgress {
	public static final int RED_UPGRADES = 10;
	public static final int GOLD_UPGRADES = 5;
	public static final int REFILL_DELAY_TICKS = 60 * 20;
	private final Set<String> consumed = new LinkedHashSet<>();
	private int quietTicks;

	public boolean hasConsumed(BossKind boss) {
		return consumed.contains(boss.id());
	}

	public boolean consume(BossKind boss) {
		if (consumed.size() >= RED_UPGRADES + GOLD_UPGRADES || !consumed.add(boss.id())) {
			return false;
		}
		quietTicks = 0;
		return true;
	}

	public int bonusHealth() {
		return Math.min(consumed.size(), RED_UPGRADES) * 2;
	}

	public int goldenCapacity() {
		return Math.max(0, consumed.size() - RED_UPGRADES) * 2;
	}

	public List<String> savedBosses() {
		return List.copyOf(consumed);
	}

	public void restore(List<String> ids) {
		consumed.clear();
		for (String id : ids) {
			// Ocean Heart Migration: The chosen Kraken replaces the earlier Leviathan placeholder.
			if ("leviathan".equals(id)) { id = BossKind.KRAKEN.id(); }
			for (BossKind boss : BossKind.values()) {
				if (boss.id().equals(id)) {
					consume(boss);
					break;
				}
			}
		}
		quietTicks = 0;
	}

	public void damaged() {
		quietTicks = 0;
	}

	public boolean tickRefill() {
		if (quietTicks < REFILL_DELAY_TICKS) {
			quietTicks++;
		}
		return goldenCapacity() > 0 && quietTicks >= REFILL_DELAY_TICKS;
	}
}
