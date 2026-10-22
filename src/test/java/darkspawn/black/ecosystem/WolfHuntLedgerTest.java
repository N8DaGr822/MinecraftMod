package darkspawn.black.ecosystem;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WolfHuntLedgerTest {
	@Test void cooldownAndOfferPersistWithoutOfflineStockpiles() {
		var id = UUID.randomUUID();
		var ledger = new WolfHuntLedger(List.of());
		assertTrue(ledger.offer(id, 100, 24000));
		assertFalse(ledger.offer(id, 101, 24000));
		ledger.started(id, 100, 24000);
		var copy = WolfHuntLedger.CODEC.parse(JsonOps.INSTANCE, WolfHuntLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow()).getOrThrow();
		assertFalse(copy.ready(id, 24099));
		assertTrue(copy.ready(id, 24100));
		assertTrue(copy.offer(id, 1000000, 24000));
		assertFalse(copy.offer(id, 1000001, 24000));
		assertFalse(copy.reward(id));
	}

	@Test void eachMemberHasOneDurableClaimAndMustClaimBeforeStartingAgain() {
		var first = UUID.randomUUID(); var second = UUID.randomUUID();
		var ledger = new WolfHuntLedger(List.of());
		for (var id : List.of(first, second)) { ledger.started(id, 0, 24000); ledger.won(id); }
		var copy = WolfHuntLedger.CODEC.parse(JsonOps.INSTANCE, WolfHuntLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow()).getOrThrow();
		assertFalse(copy.ready(first, 50000));
		assertTrue(copy.claim(first)); assertFalse(copy.claim(first));
		assertTrue(copy.reward(second));
		assertTrue(copy.ready(first, 50000));
		assertFalse(copy.ready(second, 50000));
	}
}
