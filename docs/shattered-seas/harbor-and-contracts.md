# Breaker's Rest: Briggs and optional contracts

Implemented in source October 14, 2026. This increment adds survival compass acquisition, an optional guide, a contract board, four personal exploration contracts, and new-harbor tavern/market buildings. Merchants, a general quest engine and broader harbor neutrality remain future work.

## Play flow

Enter Pirate Seas through your Sea Gate. **Old Salt Briggs** stands on the plaza west of the arrival point. Right-click him for guidance about the harbor, factions and sailing. **Compass / replace** provides one free calibrated Logbound Compass per player; replacements cost one ordinary compass. Keep one inventory slot empty. A player already carrying a Logbound Compass cannot request another. Compass issuance history survives death and save/load, and replacement preserves route and timer state.

Briggs and the **Harbor Contract Board** at **2, 65, 3** offer **Cove Survey**. Accept it, visit Castaway Cove's existing surface boundary, and return to Briggs or the board to claim **four emeralds**. These are initial reward/cost values for playtesting. The compass, its recalibration and faction membership are not requirements. The board handles contracts; only Briggs issues compasses.

The survey is personal: another player's visit does not complete it for you. Prior visits do not automatically complete a newly accepted survey. Abandoning clears its current objective; accepting again starts it afresh. Claiming is once-only and cannot be reset by abandoning, reconnecting or dying. A full inventory leaves the reward unclaimed; free an inventory slot and retry. Menu actions check proximity, a living non-spectator player and a valid guide or board inside the harbor.

## Buildings and migration

New shared harbors add **The Rusty Anchor** tavern north of the plaza and a market shelter to the east. These are authored buildings with tables, seating, lighting, signs and storage; they are not yet trading/rumor NPC systems. The existing lighthouse, chandlery, return gate and dock remain.

Existing harbors are never regenerated. On entry, a version-compatible SavedData extension adds the board only if its position is empty with a solid floor, and spawns Briggs only at a clear plaza location. Blocked additions retry on a later entry without breaking player blocks. Successful additions are saved once; later visits do not duplicate Briggs, restore a removed board or replenish supplies. The guide is a persistent, invulnerable, stationary native Villager placeholder. Admin/creative removal is not automatically repaired; later NPC lifecycle work should define that behavior. Briggs' compass and contract services work only within the existing 96-block harbor radius.

The harbor's existing naval protection is unchanged. Invulnerable Briggs does not imply complete faction or hostile-mob neutrality. New buildings appear in fresh harbors; established worlds receive the small service additions only.

## Implementation and verification

`HarborGuide` handles server-owned services, `HarborMenu` validates interactions, and `HarborScreen` follows the existing shared UI theme. Pirate player state adds compass issuance and contract progress; pirate harbor SavedData tracks one-time additions. Existing compass polling observes the optional survey without introducing a separate global scan. Contract selection uses the existing validated menu; server-owned objective flags and once-only reward states remain local to the pirate feature.

Regenerate authored templates with `node tools/assets/pirate-foundation.cjs`; validate routes with `tools/assets/preview-pirate.py`. The contract board is a hand-authored native model using existing Minecraft textures. Run common/client compilation, the pirate GameTests and Journal unit tests. Tests cover one-time guide migration, remote menu rejection, inventory/payment behavior, personal survey progress, once-only rewards and save/death persistence. Live dialogue sizing, NPC interaction and fresh/established-world visual checks still need playtesting. No JAR or release-version change is included.

October 14 verification: common/client compilation, all 15 pirate server tests and all 3 Journal unit tests passed. The template validator confirms four island bounds/caches and walking routes to harbor services; the layout preview was reviewed. Board model bounds, JSON, documentation links and whitespace checks passed. Log: `build/harbor-final-checks.log`. This is targeted verification, not unfiltered fullCheck or live UI acceptance.

## Island contracts and encounters (October 14 expansion)

Use **Previous contract / Next contract** at Briggs or the board to select an activity, then accept. All four can be active independently. The three additions are:

| Contract | Objective after acceptance | Reward |
| --- | --- | --- |
| Wreck salvage | Interact with the cargo chest aboard The Pale Gull | 6 emeralds |
| Shrine exploration | Interact with the relic chest at Tideglass Shrine | 8 emeralds |
| Beacon investigation | Reach Lastlight Beacon's lantern platform (Y 76 or higher, within its upper tower) | 6 emeralds |

Return to Briggs or the board to claim. Cache inspections work even after shared loot has been taken; they do not consume items or require exclusive loot ownership. Removed caches must be restored at their original location. Beacon ground-floor visits do not complete its contract. Prior visits do not count until the action is performed after accepting. Each reward pays once per player, with state retained through saving and death. Abandoning an unclaimed contract clears its objective. Values are initial balancing choices for playtesting.

The existing native island templates now have small encounters: one husk at random coves, two crossbow pillagers at forts, two drowned at wrecks, two skeleton archers at beacons, two drowned (one with a trident) at shrines, and two Piglin scavengers at mangrove camps (updated in the faction increment). These use native AI and daylight behavior, not faction standing. The guaranteed starter cove has no authored hostiles. Groups spawn on a living survival player's approach, respecting Peaceful and the mob-spawning rule. Creative and spectator players do not trigger groups.

Each successful spawn slot is saved per island for the world, preventing repeated visits or different players from respawning defeated guards. Blocked slots retry without replacing player blocks. Spawn attempts are limited to eight per second per dimension and 48 loaded encounter mobs; a loaded-island query never generates terrain. Spawn points require a clear space, solid floor and no player within four blocks. This applies to existing islands too, so established player builds can block their slots.

First visits record a short clue per island type in chat and the Journal's Landmarks section. Fort and wreck caches now include solid shot; wreck and camp caches include oak repair planks. Beacon tools and shrine sea-gate materials retain their uses. New warning signs tell more of the keeper, wreck and Seven Seals story. Existing island blocks are not regenerated; updated signs need fresh terrain. Already opened caches are never restocked; unopened native loot tables use the updated rewards.

Manual acceptance still includes dialogue layout at GUI scales, native enemy behavior and encounter balance, the beacon climb, and shared-cache interaction with two players. This is a source increment, with no packaged JAR.

### Expansion verification

Common/client compilation, all **18 pirate server tests**, all **4 existing Journal server tests**, and **3 Journal unit tests** pass. New checks exercise real native wreck/shrine/beacon objective positions, spectator/distance rejection, independent player state, exact rewards and full-inventory behavior, save/death persistence, native fort sentries, world-saved encounter masks, navigation text, Journal clues and board selection. Encounter fixtures use isolated test regions because dimension SavedData intentionally survives test-server restarts. Seven-template bounds/cache/walking-route validation passes; the layout preview was inspected, eight resource JSON files parsed, and changed tracked files pass whitespace checks. Logs: `build/pirate-discoveries-verified.log`, `build/pirate-discoveries-final.log` (unit results), and `build/pirate-journal-regression.log`. This is targeted verification, not fullCheck or live gameplay acceptance.

## Faction expansion

Briggs and the board now cycle through seven contracts. [Faction activities](factions.md#first-activities-all-three-factions) add Corsair cargo recovery, Fleet dispatch recovery and a Drowned offering, with independent saved standing and earned crew offers. See [captain milestones](player-progression.md#implemented-captain-milestones) for faction-free progression and the third platform tier.

## Faction contacts and diplomacy

Briggs and the board now broker joining, leaving and paid amends. Select a faction contract first; one membership is allowed, joining requires Trusted (25+), leaving costs 25 standing with the former faction, and amends cost 8 gold for +5 standing up to 25. Friendly outpost contacts offer these services and their own contract within six blocks. Ships and hired crew stay with their captain. See [full diplomacy rules](factions.md#membership-and-local-diplomacy).

## Repeat voyages and officer rescue

The harbor menu now cycles eleven activities: the seven existing one-time contracts, three repeat voyages and Garrick Rescue. Outpost contacts cycle their own first and repeat faction activities. Repeat voyages pay +10 standing and six emeralds with a ten-minute cooldown. Garrick has personal release papers and an explicit recruit-or-free choice at harbor. [Complete rules](repeat-voyages-and-garrick.md).
