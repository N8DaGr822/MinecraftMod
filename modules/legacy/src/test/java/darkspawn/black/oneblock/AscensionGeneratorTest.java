package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AscensionGeneratorTest {
    @Test void onlyConfirmedMatchingAttemptCommitsOnceAndDuplicateTickIsDenied(){
        var generator=new AscensionGenerator(0);var progress=new OneBlockProgress();var player=UUID.randomUUID();
        var first=generator.begin(player,10,true);assertNotNull(first);assertNull(generator.begin(player,10,true));
        generator.removed(UUID.randomUUID());assertFalse(generator.finish(first,progress,()->1));assertEquals(0,progress.total);
        var next=generator.begin(player,10,true);generator.removed(player);
        assertFalse(generator.finish(first,progress,()->{throw new AssertionError("Stale attempt sampled RNG");}));
        assertTrue(generator.finish(next,progress,()->7));assertEquals(1,progress.total);assertEquals(7,progress.resource);assertEquals(1,generator.generation());
        assertFalse(generator.finish(next,progress,()->1));assertEquals(AscensionGenerator.State.REGENERATING,generator.state());
        generator.repaired(true);assertNull(generator.begin(player,10,true));assertNotNull(generator.begin(player,11,true));
    }
    @Test void pendingAndCommittedSnapshotsRecoverWithoutReplayingProgressOrRandomness(){
        var data=new AscensionLedger();var team=data.create(UUID.randomUUID(),"Recovery");var generator=team.generator();
        var attempt=generator.begin(team.owner(),5,true);
        var pending=reload(data).team(team.id());assertEquals(AscensionGenerator.State.PENDING,pending.generator().state());
        assertTrue(pending.generator().recovering());pending.generator().repaired(true);
        assertEquals(0,pending.progress().total);assertEquals(0,pending.generator().generation());
        generator.removed(team.owner());assertTrue(generator.finish(attempt,team.progress(),()->14));
        var committed=reload(data).team(team.id());assertEquals(AscensionGenerator.State.REGENERATING,committed.generator().state());
        assertTrue(committed.generator().recovering());committed.generator().repaired(true);
        assertEquals(1,committed.progress().total);assertEquals(14,committed.progress().resource);assertEquals(1,committed.generator().generation());
        assertFalse(committed.generator().finish(attempt,committed.progress(),()->0));
    }
    @Test void creativeRemovalNeverCreditsAndLockedOrExhaustedGeneratorsRejectWork(){
        var generator=new AscensionGenerator(0);var progress=new OneBlockProgress();var player=UUID.randomUUID();
        var attempt=generator.begin(player,1,false);generator.removed(player);
        assertFalse(generator.finish(attempt,progress,()->{throw new AssertionError("Creative sampled RNG");}));
        assertEquals(0,progress.total);assertEquals(1,generator.generation());generator.repaired(false);
        assertEquals(AscensionGenerator.State.ERROR,generator.state());assertNull(generator.begin(player,2,true));
        assertTrue(generator.recovering());generator.repaired(true);generator.lock();assertFalse(generator.recovering());
        assertNull(generator.begin(player,2,true));
        var exhausted=new AscensionGenerator(Long.MAX_VALUE);assertNull(exhausted.begin(player,2,true));assertEquals(AscensionGenerator.State.LOCKED,exhausted.state());
    }
    @Test void oldSchemasSeedIndependentGeneratorsAndCurrentCorruptionFailsClosed(){
        for(int schema:new int[]{1,2}){
            var data=new AscensionLedger();var a=data.create(UUID.randomUUID(),"A");var b=data.create(UUID.randomUUID(),"B");a.progress().total=123;
            var json=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();json.addProperty("schema",schema);
            json.getAsJsonArray("teams").forEach(t->{t.getAsJsonObject().remove("generator");t.getAsJsonObject().remove("phases");});
            var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();assertTrue(restored.compatible());
            assertEquals(123,restored.team(a.id()).generator().generation());assertEquals(0,restored.team(b.id()).generator().generation());
            assertNotSame(restored.team(a.id()).generator(),restored.team(b.id()).generator());
        }
        var data=new AscensionLedger();var team=data.create(UUID.randomUUID(),"Invalid");
        for(String field:new String[]{"missing","state","generation"}){
            var json=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();var saved=json.getAsJsonArray("teams").get(0).getAsJsonObject();
            if(field.equals("missing"))saved.remove("generator");
            else if(field.equals("state"))saved.getAsJsonObject("generator").addProperty("state","UNKNOWN");
            else saved.getAsJsonObject("generator").addProperty("generation",-1);
            var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();assertFalse(restored.compatible());assertNull(restored.member(team.owner()));
            assertEquals(json,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow());
        }
    }
    @Test void maintenanceIsBoundedAndRotatesPastArchivedAndIdleTeams(){
        var data=new AscensionLedger();var seen=new HashSet<UUID>();
        for(int i=0;i<81;i++){var team=data.create(UUID.randomUUID(),"Island "+i);if(i%2==0)data.archive(team.owner());}
        for(int i=0;i<3;i++){var batch=data.maintenanceBatch();assertEquals(32,batch.size());batch.forEach(t->seen.add(t.id()));}
        assertEquals(81,seen.size());
    }
    private static AscensionLedger reload(AscensionLedger data){return AscensionLedger.CODEC.parse(JsonOps.INSTANCE,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();}
}
