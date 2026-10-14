# Factions

Status: faction foundations implemented October 14, 2026. The three separate standings, optional outpost contracts and earned crew offers below are in source. Membership, local diplomacy, friendly contacts and distinct-species crew are also implemented. Rank ladders, named captains, faction warfare and special abilities from the [original brief](design-brief.md) remain proposals.

| Faction | Identity and territory | Proposed leader | Combat variants |
| --- | --- | --- | --- |
| Crimson Corsairs | Piglins/Brutes; crimson and gold, greed and crew loyalty; coves, treasure ports, smuggling routes | Captain Gildtusk, the Golden Tyrant | Melee crewman, crossbow gunner, heavy Brute, elite first mate, captain |
| Iron Fleet | Pillagers/Illagers; gray/blue/black, discipline and conquest; forts, watchtowers, shipyards and naval routes | Grand Admiral Blackthorn | Crossbow sailor, long-range sharpshooter, Vindicator enforcer, coordinating first mate, captain |
| The Drowned | Zombies, Skeletons, Drowned and Wither Skeletons; spectral light, rust, barnacles and dark sails; haunted islands, wrecks, underwater ruins and cursed temples | Captain Hollow-Eye, the Undying | Zombie deckhand, skeleton marksman, trident harpooner, Wither Skeleton first mate, supernatural captain |

Corsairs value gold, raids, cargo theft and rivalry. Rewards may include golden cutlasses, maps, trade discounts, hidden ports and recruits; Golden Tribute could spend gold for temporary assistance. Iron Fleet contracts emphasize bounties, escorts, defense and suppression; rewards could include armor, crossbows, shipyards and support through Call to Arms. Individual officers need not share their leader's cruelty.

The Drowned begin hostile. A ritual, artifact or captain's trust could allow cooperation. Intelligent ancient captains retain memories while ordinary undead may remain mindless. Rewards could include cursed weapons, tridents, underwater benefits, spectral equipment, ghost ships and undead recruits. Dead Man's Call is a proposed temporary summon.

## Standing and rank

Standing is separate for each faction and separate from membership, renown and crew allegiance. Suggested range is -100 to +100: Hunted (-100..-50), Hostile (-49..-1), Neutral (0..24), Trusted (25..59), Honored (60..89), Legendary (90..100). These bands now label the saved standings. Starting values are Corsairs 0, Fleet 0, Drowned -25. Tagged Drowned outpost guards begin hostile; tagged Corsair and Fleet guards begin neutral. Ordinary untagged mobs retain vanilla behavior.

Proposed ladders:

- Corsairs: Outsider → Swabbie → Corsair → First Mate → Captain → Dread Captain.
- Iron Fleet: Civilian → Recruit → Marine → Lieutenant → Captain → Admiral.
- Drowned: Living Intruder → Marked → Oathbound → Deathsworn → Harbinger → Wraith Captain.

Promotions should require meaningful achievements as well as standing. Standing can affect hostility, dialogue, contracts, trades, restricted areas and recruitment. Joining is optional; leaving may cost privileges or standing and open reconciliation work, while generally preserving personally owned vessels, legitimate inventory and independent crew.

## Encounter rules

Build coordinated groups: crew → specialist → first mate → captain. First mates may buff or coordinate a bounded group, with their defeat disrupting it. Captains need distinct equipment, tactics, locations and rewards rather than health inflation alone. Some named captains are allies or contacts, not obligatory bosses.

Neutral hub rules must override native mob targeting. Hiring a Piglin or undead sailor does not make every member of its species friendly. Faction hostility, species behavior and a recruited individual's loyalty require separate decisions. Corsair/Fleet rivalry and Drowned mystery should yield different opportunities, including viable independent play.

## First activities: all three factions

Briggs and the harbor board broker the seven initial contracts plus [repeat voyages and Garrick Rescue](repeat-voyages-and-garrick.md). Use Previous/Next contract to select a faction activity. No faction membership, ship ownership or compass is required. The objective must be performed after accepting; return to the harbor or the matching outpost contact to claim a one-time commendation.

| Faction | Recognizable outpost and objective | Standing reward | Earned crew offer at Trusted (25+) |
| --- | --- | --- | --- |
| Piglin Corsairs | Rootbound Camp: crimson/gold standard; inspect the cargo chest to recover a lost manifest from rival scavengers | +25 Corsair standing | Corsair First Mate: 8 emeralds instead of 12 |
| Illager Iron Fleet | Saltwatch Fort: blue/gray standard; inspect the cache for dispatches held by renegades | +25 Fleet standing | Fleet Cannoneer: 4 emeralds instead of 8 |
| The Drowned | Tideglass Shrine: black/cyan standard and soul light; inspect the relic cache while carrying four prismarine shards to make an offering | +50 Drowned standing | Tidebound Shipworker: 8 emeralds instead of 12 |

Standings are clamped to -100..100 and change independently. Each faction contract grants its standing only once. Commendations need no inventory space. An already emptied shared chest still counts; removing the chest requires restoring it at its native position. Drowned offerings consume four shards once when the objective completes, not when claiming. Abandoning before claiming clears the objective and does not refund the offering. No physical manifest/dispatch item is added; inspection records recovered information in player state. Combat is optional and these contracts do not transfer territory ownership.

The crew offers apply automatically through the existing ship menu while docked at the harbor. A claimed commendation and Trusted standing recruit an actual Piglin first mate, Pillager cannoneer or sun-safe Husk shipworker. Ordinary hires remain Villagers. Supplies, station abilities and role limits are unchanged. Saved captain UUID tags preserve individual loyalty through reloads and resignation; old Villager crews gain ownership tags without being replaced. Piglins resist zombification, Pillagers cannot join raids, and friendly contacts/crew can spawn, reload and remain in Peaceful. Crew remain seated station workers rather than roaming companions or a morale simulation.

## Membership and local diplomacy

- Join one faction at a time, free at Trusted (25+), through Briggs, the harbor board or that faction's contact. Select its contract on the harbor screen first.
- Leaving costs **25 standing with the former faction**. Ships, cargo and hired crew remain yours. Joining a different faction requires leaving first; standing with other factions stays separate.
- Make amends costs **8 gold ingots for +5 standing**, capped at 25. This provides recovery after one-time contracts are exhausted. No bounty system is added in this pass.
- Membership reduces the matching earned crew offer by two more emeralds: member prices are 6 for a first mate, 2 for a cannoneer and 6 for a shipworker. Membership alone does not replace the commendation/Trusted requirement. Independent captains retain all ordinary recruitment and progression routes.
- Rootbound Camp gains a Piglin **Corsair Quartermaster**, Saltwatch Fort a Pillager **Fleet Liaison**, and Tideglass Shrine a Husk **Tidebound Emissary**. Contacts are stationary, protected and friendly at any standing. They offer their own contract and diplomacy services within six blocks; only Briggs issues compasses.
- Explicitly tagged guards spare players with zero or positive standing and target hostile players within 24 blocks and line of sight. Target selection is personal, including mixed-standing multiplayer groups. Native unrelated mobs retain their behavior.
- Damaging a tagged guard costs 10 standing, at most once per faction per second. Guards and their projectiles cannot harm neutral players, contacts or hired crew. The 96-block harbor protection overrides faction aggression. [Naval patrols](repeat-voyages-and-garrick.md#faction-naval-patrols) now share personal reputation rules and the per-faction attack penalty budget. Inter-faction warfare remains future work; ship PvP stays available.

Contacts are added once at a clear, loaded position on existing or new islands. Blocked positions retry without replacing player blocks; successful additions persist in the encounter mask. Removed contacts are not automatically replaced. Existing matching Piglin/Pillager/Drowned guards gain faction tags on nearby observation; old camp spiders remain unchanged.

Existing structures are not rebuilt. New standards/signs appear in fresh terrain; all contracts also work with the same native cache locations in existing islands. Already spawned encounters keep their saved entities and masks, including older camp spiders. The guaranteed first cove remains safe.

Verification: see [faction and captain checks](player-progression.md#verification-of-faction-and-captain-increment), including all three activities and saved standing.

Diplomacy verification: common/client compilation, 23 pirate server tests, 11 naval server tests and 3 Journal unit tests. Includes membership/save/death-copy, resignation and reconciliation, server-side contact distance checks, personal hostility, retained crew, legacy crew migration, distinct-species recursive passenger persistence and Peaceful NPC creation/reload. Live two-player AI behavior and screen readability still need playtesting. No JAR was packaged.
