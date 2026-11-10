package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AscensionLedgerTest {
    @Test void ownerTransferAndRemovalPreserveIslandIdentityAndRejectNonOwners(){
        var data=new AscensionLedger();var owner=UUID.randomUUID();var friend=UUID.randomUUID();var outsider=UUID.randomUUID();
        var team=data.create(owner,"Team");assertTrue(data.join(team.id(),friend));
        assertFalse(data.transfer(outsider,friend));
        assertFalse(data.remove(friend,owner));
        assertTrue(data.transfer(owner,friend));
        assertEquals(team.id(),data.member(friend).id());
        assertEquals(team.center(),data.member(friend).center());
        assertFalse(data.remove(owner,friend));
        assertFalse(data.remove(friend,friend));
        assertTrue(data.remove(friend,owner));
        assertNull(data.member(owner));
        assertEquals(friend,data.member(friend).owner());
    }
    @Test void spiralAllocationAndInclusiveClaimsLeaveFortyNineNeutralColumns() {
        var cells=new HashSet<BlockPos>();
        for(int i=0;i<10000;i++) {
            var center=AscensionLedger.cellCenter(i);
            assertTrue(cells.add(center));assertEquals(80,center.getY());
            assertEquals(0,center.getX()%250);assertEquals(0,center.getZ()%250);
        }
        var data=new AscensionLedger();var first=data.create(UUID.randomUUID(),"First");var second=data.create(UUID.randomUUID(),"Second");
        assertEquals(new BlockPos(0,80,0),first.center());assertEquals(new BlockPos(250,80,0),second.center());
        assertEquals(first,data.at(new BlockPos(-100,-64,100)));assertEquals(first,data.at(new BlockPos(100,319,0)));
        assertEquals(second,data.at(new BlockPos(150,80,0)));assertEquals(second,data.at(new BlockPos(350,80,0)));
        for(int x=101;x<=149;x++)assertNull(data.at(new BlockPos(x,80,0)));
        assertNull(data.at(new BlockPos(0,80,101)));
    }
    @Test void membershipRolesCapacityAndProgressSurviveReloadWithoutReusingCells() {
        var data=new AscensionLedger();var owner=UUID.randomUUID();var team=data.create(owner,"Saved");var member=UUID.randomUUID();
        assertNull(data.create(owner,"Duplicate"));assertTrue(data.join(team.id(),member));
        for(int i=0;i<4;i++)assertTrue(data.join(team.id(),UUID.randomUUID()));
        assertFalse(data.join(team.id(),UUID.randomUUID()));assertFalse(data.leave(owner));
        team.progress().mined();team.progress().initialized=true;
        var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();
        var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
        assertTrue(restored.compatible());assertEquals(team.id(),restored.member(member).id());assertEquals(owner,restored.member(member).owner());
        assertEquals(1,restored.member(owner).progress().total);assertTrue(restored.member(owner).progress().initialized);
        assertTrue(restored.leave(member));assertNull(restored.member(member));
        assertEquals(new BlockPos(250,80,0),restored.create(member,"Second").center());
        assertEquals(0,restored.member(member).progress().total);
    }
    @Test void unsupportedGeometryAndSchemasArePreservedButCannotBeMutated() {
        var data=new AscensionLedger();var owner=UUID.randomUUID();data.create(owner,"Saved");
        for(String field:new String[]{"schema","spacing","claim"}) {
            var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();saved.addProperty(field,999);
            var restored=AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
            assertFalse(restored.compatible());assertNull(restored.member(owner));assertNull(restored.create(UUID.randomUUID(),"New"));
            assertEquals(saved,AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow());
        }
    }
    @Test void duplicateMemberCorruptionFailsClosedAndInvalidNamesDoNotConsumeCells() {
        var data=new AscensionLedger();assertNull(data.create(UUID.randomUUID()," "));assertNull(data.create(UUID.randomUUID(),"bad\nname"));
        var owner=UUID.randomUUID();assertEquals(0,data.create(owner,"Valid").cell());
        var saved=AscensionLedger.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
        var members=saved.getAsJsonArray("teams").get(0).getAsJsonObject().getAsJsonArray("members");members.add(members.get(0).deepCopy());
        assertFalse(AscensionLedger.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow().compatible());
    }
}
