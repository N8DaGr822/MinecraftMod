package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AscensionBridgesTest {
    @Test void corridorsHaveExactWidthGapAndHeightInEveryCardinalDirection() {
        var origin=new BlockPos(0,80,0);
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var neighbor=origin.relative(direction,250);
            for(int distance=100;distance<=150;distance++)for(int width=-4;width<=4;width++)for(int y:new int[]{71,72,96,97}) {
                var pos=origin.relative(direction,distance).relative(direction.getClockWise(),width).atY(y);
                boolean expected=distance>=101&&distance<=149&&Math.abs(width)<=3&&y>=72&&y<=96;
                assertEquals(expected,AscensionBridges.between(origin,neighbor,pos));
                assertEquals(expected,AscensionBridges.between(neighbor,origin,pos));
            }
        }
        assertFalse(AscensionBridges.between(origin,new BlockPos(250,80,250),new BlockPos(125,80,0)));
        assertFalse(AscensionBridges.between(origin,new BlockPos(500,80,0),new BlockPos(125,80,0)));
    }
    @Test void corridorsRequireAllocatedActiveNeighborsAndRetainCanonicalIds() {
        var data=new AscensionLedger();var a=data.create(UUID.randomUUID(),"A");var pos=new BlockPos(125,80,0);
        assertNull(AscensionBridges.corridor(data,a,pos));var b=data.create(UUID.randomUUID(),"B");
        assertEquals("0:1",AscensionBridges.corridor(data,a,pos));assertEquals("0:1",AscensionBridges.corridor(data,b,pos));
        assertTrue(data.archive(a.owner()));assertNull(data.member(a.owner()));assertTrue(data.archived(a.id()));
        assertNull(AscensionBridges.corridor(data,b,pos));assertNull(AscensionBridges.corridor(data,a,pos));assertNotNull(data.at(a.center()));
        var next=data.create(a.owner(),"Replacement");assertEquals(2,next.cell());
        var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();
        assertTrue(restored.compatible());assertTrue(restored.archived(a.id()));assertEquals(next.id(),restored.member(a.owner()).id());
    }
    @Test void blockOwnershipRoundTripsAndCannotBeTakenOver() {
        var data=new AscensionBridges();var owner=UUID.randomUUID();var pos=new BlockPos(125,80,0);
        assertTrue(data.record(pos,owner,"0:1","minecraft:oak_planks"));
        assertFalse(data.record(pos,UUID.randomUUID(),"0:1","minecraft:stone"));
        var saved=AscensionBridges.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();
        var restored=AscensionBridges.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
        assertEquals(data.at(pos),restored.at(pos));restored.remove(pos);assertEquals(0,restored.size());
        saved.getAsJsonObject().addProperty("schema",999);
        var incompatible=AscensionBridges.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
        assertFalse(incompatible.compatible());assertFalse(incompatible.record(pos,owner,"0:1","minecraft:stone"));incompatible.remove(pos);
        assertEquals(saved,AscensionBridges.CODEC.encodeStart(JsonOps.INSTANCE,incompatible).getOrThrow());
    }
    @Test void schemaOneUpgradesWithoutResettingAnyTeamProgressOrCells() {
        var data=new AscensionLedger();var team=data.create(UUID.randomUUID(),"Existing");team.progress().mined();
        var old=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();old.addProperty("schema",1);
        old.getAsJsonArray("teams").get(0).getAsJsonObject().remove("archived");
        old.getAsJsonArray("teams").get(0).getAsJsonObject().remove("phases");
        var upgraded=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();
        assertTrue(upgraded.compatible());assertFalse(upgraded.member(team.owner()).archived());assertEquals(1,upgraded.member(team.owner()).progress().total);
        assertEquals(team.center(),upgraded.member(team.owner()).center());
        assertEquals(6,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,upgraded).getOrThrow().getAsJsonObject().get("schema").getAsInt());
    }
}
