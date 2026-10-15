package darkspawn.black.pirate;

import net.minecraft.server.level.ServerPlayer;

/** Earned ranks survive resignation; privileges require current membership and standing. */
public final class FactionPromotions {
    private static final String[][] TITLES = {
        {"Outsider", "Swabbie", "Corsair", "First Mate", "Captain", "Dread Captain"},
        {"Civilian", "Recruit", "Marine", "Lieutenant", "Captain", "Admiral"},
        {"Living Intruder", "Marked", "Oathbound", "Deathsworn", "Harbinger", "Wraith Captain"}
    };
    private static final int[] STANDING = {0, 25, 40, 60, 80, 90};
    private FactionPromotions() { }
    public static String title(PirateProgression.Faction faction, int rank) {
        return TITLES[faction.ordinal()][Math.clamp(rank, 0, 5)];
    }
    public static String requirement(PiratePlayerState state, PirateProgression.Faction faction) {
        int rank = state.factionRanks[faction.ordinal()];
        if (rank >= 5) return "Highest faction rank earned.";
        return "Next: " + title(faction, rank + 1) + "; " + STANDING[rank + 1] + " standing, claimed faction introduction, "
            + switch (rank + 1) {
                case 1 -> "one discovered island.";
                case 2 -> "four discovered islands and two claimed contracts.";
                case 3 -> "eight discovered islands and four claimed contracts.";
                case 4 -> "twelve discovered islands and six claimed contracts.";
                default -> "sixteen discovered islands, eight claimed contracts and a completed personal Seven Seals ending.";
            };
    }
    public static boolean eligible(PiratePlayerState state, PirateProgression.Faction faction) {
        int next = state.factionRanks[faction.ordinal()] + 1;
        if (next > 5 || !state.factionMembership.equals(faction.id) || state.factionContracts[faction.ordinal()] != 2
                || PirateProgression.standing(state, faction) < STANDING[next]) return false;
        long discoveries = state.logboundVisited.stream().distinct().count();
        int contracts = PirateProgression.completedContracts(state);
        return discoveries >= (next == 1 ? 1 : (next - 1) * 4) && contracts >= (next - 1) * 2
            && (next < 5 || state.ending != 0);
    }
    public static boolean promote(ServerPlayer player) {
        var state = Pirate.state(player);
        var faction = PirateDiplomacy.faction(state.factionMembership);
        if (faction == null) { Ships.message(player, "Join a faction before requesting promotion."); return false; }
        if (!PirateDiplomacy.canParley(player, faction)) return false;
        if (!eligible(state, faction)) { Ships.message(player, requirement(state, faction)); return false; }
        state.factionRanks[faction.ordinal()]++;
        Ships.message(player, "Promoted to " + title(faction, state.factionRanks[faction.ordinal()]) + ". " + requirement(state, faction));
        return true;
    }
    public static int discount(PiratePlayerState state, PirateProgression.Faction faction) {
        if (!state.factionMembership.equals(faction.id) || PirateProgression.standing(state, faction) < 25) return 0;
        int rank = state.factionRanks[faction.ordinal()];
        return rank >= 5 ? 2 : rank >= 3 ? 1 : 0;
    }
}
