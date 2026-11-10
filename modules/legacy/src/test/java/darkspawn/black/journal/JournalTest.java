package darkspawn.black.journal;

import darkspawn.black.TestBootstrap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import darkspawn.black.Darkspawn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JournalTest {
    @BeforeAll static void bootstrap() { TestBootstrap.initialize(); }
    @Test void catalogCoversRegisteredRosterAndEveryPortrait() {
        var entries=JournalCatalog.entries().values();
        assertEquals(82,entries.stream().filter(e -> e.section().equals("creatures")).count());
        assertEquals(14,entries.stream().filter(e -> e.section().equals("mounts")).count());
        assertEquals(16,entries.stream().filter(e -> e.section().equals("bosses")).count());
        assertEquals(17,entries.stream().filter(e -> e.section().equals("landmarks")).count());
        for(var entry:entries) if(!entry.entity().isEmpty()) {
            assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Darkspawn.id(entry.entity())),entry.entity());
            for(String suffix:List.of("","_unknown")) assertNotNull(getClass().getResource("/assets/darkspawn/textures/gui/journal/"+entry.entity()+suffix+".png"),entry.entity());
        }
    }
    @Test void discoveriesDeduplicateAndIgnoreUnknownSavedIds() {
        var progress=new JournalProgress();
        progress.restore(List.of("entity/leopard","entity/leopard","obsolete","recipe/not_owned_here"));
        assertEquals(List.of("entity/leopard"),progress.saved());
        assertFalse(progress.discover("entity/leopard")); assertFalse(progress.discover("entity/fake"));
        assertTrue(progress.discover("landmark/druid_shrine"));
        var restored=new JournalProgress(); restored.restore(progress.saved()); assertEquals(progress.saved(),restored.saved());
    }
    @Test void snapshotCodecRoundTripsAndRejectsUnboundedCounts() {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {
            var snapshot=new JournalSnapshotPayload(List.of(new JournalSnapshotPayload.Entry("entity/leopard","mounts","Leopard","Habitat\nDetails",true)));
            JournalSnapshotPayload.CODEC.encode(buffer,snapshot); assertEquals(snapshot,JournalSnapshotPayload.CODEC.decode(buffer));
            buffer.clear();buffer.writeVarInt(513);assertThrows(IllegalArgumentException.class,()->JournalSnapshotPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
