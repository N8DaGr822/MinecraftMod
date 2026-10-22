package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AscensionPhasesTest {
    @BeforeAll static void bootstrap(){darkspawn.black.TestBootstrap.initialize();}
    @Test void bundledPhasesHaveMvpThresholdsAndDeterministicEssentialGuarantees(){
        var phases=AscensionPhases.defaults();AscensionPhases.validate(phases);
        assertEquals(List.of(250,500,600,700,800,900,1000,1100,1200,1300,1400,1600),phases.stream().map(AscensionPhases.Phase::breaks).toList());
        for(var phase:phases){
            var random=net.minecraft.util.RandomSource.create(42);var seen=new HashSet<Integer>();
            for(int generation=0;generation<2000;generation++){
                int resource=phase.resource(generation,random);seen.add(resource);
                for(var guarantee:phase.guaranteed())if(generation%phase.cycle()==guarantee.offset())assertEquals(guarantee.resource(),resource);
            }
            for(var entry:phase.pool())assertTrue(seen.contains(entry.resource()));
        }
    }
    @Test void invalidCatalogReplacementIsAtomicAndValidBalanceChangesPersist(){
        var catalog=catalog();var original=AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow();
        for(String field:List.of("breaks","cycle","schema")){
            var invalid=original.deepCopy().getAsJsonArray();invalid.get(1).getAsJsonObject().addProperty(field,-1);
            assertFalse(catalog.replace(parse(invalid)));assertEquals(original,AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow());assertFalse(catalog.lastError().isEmpty());
        }
        var valid=original.deepCopy().getAsJsonArray();valid.get(0).getAsJsonObject().addProperty("breaks",275);
        assertTrue(catalog.replace(parse(valid)));assertEquals(275,catalog.phase("genesis").breaks());assertTrue(catalog.lastError().isEmpty());
        var restored=AscensionPhases.CODEC.parse(JsonOps.INSTANCE,AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow()).getOrThrow();assertEquals(275,restored.phase("genesis").breaks());
    }
    @Test void invalidResourcesItemsDuplicateIdsAndIdentityChangesAreRejected(){
        var catalog=catalog();var original=AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow();
        for(int kind=0;kind<8;kind++){
            var invalid=original.deepCopy().getAsJsonArray();var p=invalid.get(0).getAsJsonObject();
            switch(kind){
                case 0->p.getAsJsonArray("pool").get(0).getAsJsonObject().addProperty("resource",26);
                case 1->p.getAsJsonArray("pool").get(0).getAsJsonObject().addProperty("weight",0);
                case 2->p.getAsJsonArray("guaranteed").add(p.getAsJsonArray("guaranteed").get(0).deepCopy());
                case 3->p.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("item","minecraft:not_an_item");
                case 4->p.getAsJsonArray("rewards").add(p.getAsJsonArray("rewards").get(0).deepCopy());
                case 5->p.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("item","minecraft:birch_log");
                case 6->invalid.get(1).getAsJsonObject().getAsJsonArray("encounters").remove(0);
                case 7->p.getAsJsonArray("rewards").get(0).getAsJsonObject().addProperty("count",2);
            }
            assertFalse(catalog.replace(parse(invalid)),"Invalid candidate "+kind);assertEquals(original,AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow());
        }
    }
    @Test void thresholdsSubmissionsAndEncountersAreAllRequiredAndTerminalMiningStaysOpen(){
        var definitions=AscensionPhases.defaults();var state=new AscensionPhaseProgress();var progress=new OneBlockProgress();
        for(var phase:definitions){
            progress.breaks=phase.breaks()-1;
            for(var goal:phase.objectives()){
                assertEquals(0,state.submit(phase,goal.id(),"minecraft:diamond",999));
                assertEquals(goal.count(),state.submit(phase,goal.id(),goal.item(),1000000));assertEquals(0,state.submit(phase,goal.id(),goal.item(),1));
            }
            assertFalse(state.advance(phase,progress));progress.mined();
            if(!phase.encounters().isEmpty()){assertFalse(state.advance(phase,progress));assertFalse(state.recordEncounter("unknown"));assertTrue(state.recordEncounter(phase.encounters().getFirst()));assertFalse(state.recordEncounter("cave_guardian"));}
            assertTrue(state.advance(phase,progress));assertFalse(state.advance(phase,progress));assertTrue(state.compatible());
        }
        assertTrue(state.completed());int previous=progress.breaks;progress.mined();assertEquals(previous+1,progress.breaks);
        assertEquals(45,state.rewards().size());
    }
    @Test void earnedRewardSnapshotsAndClaimsSurviveReloadAndCannotBeReissued(){
        var catalog=catalog();var phase=catalog.phase("genesis");var progress=new OneBlockProgress();progress.breaks=250;var state=new AscensionPhaseProgress();
        for(var goal:phase.objectives())state.submit(phase,goal.id(),goal.item(),goal.count());assertTrue(state.advance(phase,progress));
        assertTrue(state.claim("genesis_food"));assertFalse(state.claim("genesis_food"));
        var restored=AscensionPhaseProgress.CODEC.parse(JsonOps.INSTANCE,AscensionPhaseProgress.CODEC.encodeStart(JsonOps.INSTANCE,state).getOrThrow()).getOrThrow();
        assertFalse(restored.claim("genesis_food"));assertNotNull(restored.unclaimed("genesis_water"));
        var updated=AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,catalog).getOrThrow().getAsJsonArray();
        updated.get(0).getAsJsonObject().getAsJsonArray("rewards").get(4).getAsJsonObject().addProperty("count",16);assertTrue(catalog.replace(parse(updated)));
        assertEquals(8,restored.rewards().stream().filter(g->g.reward().id().equals("genesis_food")).findFirst().orElseThrow().reward().count());
    }
    @Test void existingSchemaThreeTeamsStayLegacyAndNewTeamsStartMvp(){
        var data=new AscensionLedger();var owner=UUID.randomUUID();var team=data.create(owner,"Existing");team.progress().phase=4;team.progress().breaks=99;
        var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();saved.addProperty("schema",3);
        saved.getAsJsonArray("teams").get(0).getAsJsonObject().remove("phases");
        var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();assertTrue(restored.compatible());assertNull(restored.member(owner).phases());
        assertEquals(4,restored.member(owner).progress().phase);assertEquals(99,restored.member(owner).progress().breaks);assertEquals(team.id(),restored.member(owner).id());
        var fresh=restored.create(UUID.randomUUID(),"MVP");assertEquals("genesis",fresh.phases().phase());assertEquals(0,fresh.progress().total);
    }
    private static AscensionPhases catalog(){return AscensionPhases.CODEC.parse(JsonOps.INSTANCE,AscensionPhases.Phase.CODEC.listOf().encodeStart(JsonOps.INSTANCE,AscensionPhases.defaults()).getOrThrow()).getOrThrow();}
    @Test void originalCompletedChapterContinuesWithoutRepeatingRewardsOrResettingTotals(){
        var data=new AscensionLedger();var owner=UUID.randomUUID();var team=data.create(owner,"Old chapter");
        for(var phase:AscensionPhases.defaults().subList(0,3)){
            team.progress().breaks=phase.breaks();
            for(var goal:phase.objectives())team.phases().submit(phase,goal.id(),goal.item(),goal.count());
            for(var encounter:phase.encounters())team.phases().recordEncounter(encounter);
            assertTrue(team.phases().advance(phase,team.progress()));
        }
        team.phases().claim("genesis_water");
        var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
        saved.addProperty("schema",4);
        var old=saved.getAsJsonArray("teams").get(0).getAsJsonObject();
        old.getAsJsonObject("phases").addProperty("phase","verdant_wilds");
        old.getAsJsonObject("progress").addProperty("phase",2);
        old.getAsJsonObject("progress").addProperty("breaks",987);
        old.getAsJsonObject("progress").addProperty("total",2345);
        var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();var migrated=restored.member(owner);
        assertTrue(restored.compatible());assertEquals("verdant_wilds",migrated.phases().phase());
        int rewards=migrated.phases().rewards().size();
        assertTrue(migrated.phases().advance(AscensionPhases.defaults().get(2),migrated.progress()));
        assertEquals("forgeworks",migrated.phases().phase());assertEquals(0,migrated.progress().breaks);assertEquals(2345,migrated.progress().total);
        assertEquals(rewards,migrated.phases().rewards().size());assertFalse(migrated.phases().claim("genesis_water"));
        assertTrue(AscensionLedger.CODEC.parse(JsonOps.INSTANCE,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow()).getOrThrow().compatible());
    }
    @Test void originalCatalogKeepsCustomBalanceWhenNewPhasesAreAppended(){
        var json=AscensionPhases.Phase.CODEC.listOf().encodeStart(JsonOps.INSTANCE,AscensionPhases.defaults().subList(0,3)).getOrThrow().getAsJsonArray();
        json.get(0).getAsJsonObject().addProperty("breaks",275);
        var restored=AscensionPhases.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        assertEquals(275,restored.phase("genesis").breaks());assertEquals(900,restored.phase("emberlands").breaks());
        assertEquals(12,AscensionPhases.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow().getAsJsonArray().size());
    }
    @Test void encounterTelegraphsHaveDistinctSafeAndDangerousPositions(){
        var zero=net.minecraft.world.phys.Vec3.ZERO;
        assertTrue(AscensionGuardian.inStrike("stormbound",zero,new net.minecraft.world.phys.Vec3(5,0,.5)));
        assertFalse(AscensionGuardian.inStrike("stormbound",zero,new net.minecraft.world.phys.Vec3(3,0,3)));
        assertFalse(AscensionGuardian.inStrike("emberlands",zero,zero));
        assertTrue(AscensionGuardian.inStrike("emberlands",zero,new net.minecraft.world.phys.Vec3(3,0,0)));
        assertFalse(AscensionGuardian.inStrike("emberlands",zero,new net.minecraft.world.phys.Vec3(6,0,0)));
        assertFalse(AscensionGuardian.inStrike("stormbound",zero,new net.minecraft.world.phys.Vec3(0,8,0)));
    }
    @Test void completedEmberlandsSaveContinuesAndSummitRoundTrips(){
        var data=new AscensionLedger();var owner=UUID.randomUUID();var team=data.create(owner,"Chapter two");
        for(var phase:AscensionPhases.defaults().subList(0,6)){
            team.progress().breaks=phase.breaks();
            for(var goal:phase.objectives())team.phases().submit(phase,goal.id(),goal.item(),goal.count());
            for(var encounter:phase.encounters())team.phases().recordEncounter(encounter);
            assertTrue(team.phases().advance(phase,team.progress()));
        }
        team.phases().claim("genesis_water");
        var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();saved.addProperty("schema",5);
        var old=saved.getAsJsonArray("teams").get(0).getAsJsonObject();
        old.getAsJsonObject("phases").addProperty("phase","emberlands");
        old.getAsJsonObject("progress").addProperty("phase",5);
        old.getAsJsonObject("progress").addProperty("total",4321);
        var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();var migrated=restored.member(owner);
        assertTrue(restored.compatible());int rewards=migrated.phases().rewards().size();
        assertTrue(migrated.phases().advance(AscensionPhases.defaults().get(5),migrated.progress()));
        assertEquals("frostbound",migrated.phases().phase());assertEquals(4321,migrated.progress().total);
        assertEquals(rewards,migrated.phases().rewards().size());assertFalse(migrated.phases().claim("genesis_water"));
        for(var phase:AscensionPhases.defaults().subList(6,12)){
            migrated.progress().breaks=phase.breaks();
            for(var goal:phase.objectives())migrated.phases().submit(phase,goal.id(),goal.item(),goal.count());
            for(var encounter:phase.encounters())migrated.phases().recordEncounter(encounter);
            assertTrue(migrated.phases().advance(phase,migrated.progress()));
        }
        migrated.progress().resource=25;
        var finalSave=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow()).getOrThrow();
        assertTrue(finalSave.compatible());assertEquals(11,finalSave.member(owner).progress().phase);
        assertEquals(25,finalSave.member(owner).progress().resource);assertTrue(finalSave.member(owner).phases().completed());
    }
    @Test void sixPhaseCatalogKeepsCustomBalance(){
        var json=AscensionPhases.Phase.CODEC.listOf().encodeStart(JsonOps.INSTANCE,AscensionPhases.defaults().subList(0,6)).getOrThrow().getAsJsonArray();
        json.get(5).getAsJsonObject().addProperty("breaks",975);
        var restored=AscensionPhases.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        assertEquals(975,restored.phase("emberlands").breaks());assertEquals(1600,restored.phase("ascension_summit").breaks());
    }
    @Test void newGuardianWarningsLeaveReachableGaps(){
        var zero=net.minecraft.world.phys.Vec3.ZERO;
        double[][] dangerous={{4,2},{2,4},{3,3},{3,0},{3,3},{0,3}};
        double[][] safe={{2,2},{2,2},{3,0},{1.5,0},{0,0},{2,2}};
        for(int i=0;i<6;i++){
            String phase=AscensionPhases.IDS.get(i+6);
            assertTrue(AscensionGuardian.inStrike(phase,zero,new net.minecraft.world.phys.Vec3(dangerous[i][0],0,dangerous[i][1])),phase);
            assertFalse(AscensionGuardian.inStrike(phase,zero,new net.minecraft.world.phys.Vec3(safe[i][0],0,safe[i][1])),phase);
            assertFalse(AscensionGuardian.inStrike(phase,zero,new net.minecraft.world.phys.Vec3(dangerous[i][0],4,dangerous[i][1])),phase);
        }
    }
    private static List<AscensionPhases.Phase> parse(com.google.gson.JsonElement value){return AscensionPhases.Phase.CODEC.listOf().parse(JsonOps.INSTANCE,value).getOrThrow();}
}
