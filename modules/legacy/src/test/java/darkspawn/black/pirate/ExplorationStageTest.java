package darkspawn.black.pirate;

import com.mojang.serialization.JsonOps;
import darkspawn.black.TestBootstrap;
import darkspawn.black.worldgen.LandmarkCompass;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExplorationStageTest {
    @BeforeAll static void bootstrap() { TestBootstrap.initialize(); }
    @Test void ranksRequireAchievementsAndPrivilegesRequireCurrentMembership() {
        for (var faction : PirateProgression.Faction.values()) {
            var state = new PiratePlayerState(); state.factionMembership = faction.id;
            state.factionStanding[faction.ordinal()] = 100;
            assertFalse(FactionPromotions.eligible(state, faction));
            state.factionContracts[faction.ordinal()] = 2; state.logboundVisited.add(BlockPos.ZERO);
            assertTrue(FactionPromotions.eligible(state, faction));
            state.factionRanks[faction.ordinal()] = 1;
            assertFalse(FactionPromotions.eligible(state, faction));
            state.harborContract = 2;
            for (int i = 1; i < 16; i++) state.logboundVisited.add(new BlockPos(i, 65, i));
            assertTrue(FactionPromotions.eligible(state, faction));
            state.factionRanks[faction.ordinal()] = 4;
            state.islandContracts = new int[]{2,2,2}; state.officerContracts = new int[]{3,3,3};
            assertFalse(FactionPromotions.eligible(state, faction));
            state.ending = 1; assertTrue(FactionPromotions.eligible(state, faction));
            state.factionRanks[faction.ordinal()] = 5;
            assertFalse(FactionPromotions.eligible(state, faction));
            assertEquals(2, FactionPromotions.discount(state, faction));
            assertTrue(PirateProgression.crewCost(state, faction.role) >= 1);
            state.factionMembership = ""; assertEquals(0, FactionPromotions.discount(state, faction));
            state.factionMembership = faction.id; state.factionStanding[faction.ordinal()] = -1;
            assertEquals(0, FactionPromotions.discount(state, faction));
        }
    }
    @Test void partyPersistenceCapsMembershipAndDisbandsOfflineMembers() {
        var leader = UUID.randomUUID(); var links = new com.google.gson.JsonArray();
        var members = new ArrayList<UUID>();
        for (int i = 0; i < 7; i++) {
            var member = UUID.randomUUID(); members.add(member);
            links.add(PartyNavigation.Link.CODEC.encodeStart(JsonOps.INSTANCE, new PartyNavigation.Link(member, leader)).getOrThrow());
        }
        var party = PartyNavigation.CODEC.parse(JsonOps.INSTANCE, links).getOrThrow();
        assertEquals(5, members.stream().filter(m -> leader.equals(party.leader(m))).count());
        var restored = PartyNavigation.CODEC.parse(JsonOps.INSTANCE, PartyNavigation.CODEC.encodeStart(JsonOps.INSTANCE, party).getOrThrow()).getOrThrow();
        assertTrue(restored.leave(leader));
        for (var member : members) assertNull(restored.leader(member));
        assertFalse(restored.leave(leader));
    }
    @Test void landmarkSelectionCoversAllBossSitesAndClearsPreviousTarget() {
        var compass = new ItemStack(LandmarkCompass.ITEM); var sites = new HashSet<String>();
        for (int i = 0; i < 16; i++) {
            var site = LandmarkCompass.selected(compass); assertTrue(sites.add(site.site()));
            assertEquals(switch(site.boss()) { case "netherborn", "soulbound_colossus" -> Level.NETHER; case "void_eye" -> Level.END; default -> Level.OVERWORLD; }, LandmarkCompass.dimension(site));
            compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO)), false));
            LandmarkCompass.cycle(compass);
            assertTrue(compass.get(DataComponents.LODESTONE_TRACKER).target().isEmpty());
        }
        assertEquals(16, sites.size()); assertEquals(LandmarkCompass.SITES.getFirst(), LandmarkCompass.selected(compass));
    }
}
