package darkspawn.black.ecosystem;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionalEventsTest {
    @Test void timersWaitPersistAndNeverAccumulateOfflineRewards() {
        var timers=RegionalEvents.Timers.CODEC.parse(JsonOps.INSTANCE,new com.google.gson.JsonArray()).getOrThrow();var id=UUID.randomUUID();
        assertFalse(timers.ready(id,0));assertFalse(timers.ready(id,23999));assertTrue(timers.ready(id,24000));assertFalse(timers.ready(id,24000));
        var restored=RegionalEvents.Timers.CODEC.parse(JsonOps.INSTANCE,RegionalEvents.Timers.CODEC.encodeStart(JsonOps.INSTANCE,timers).getOrThrow()).getOrThrow();
        assertFalse(restored.ready(id,24001));assertTrue(restored.ready(id,100000));assertFalse(restored.ready(id,100001));
    }
}
