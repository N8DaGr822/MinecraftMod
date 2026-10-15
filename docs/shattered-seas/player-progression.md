# Player progression, ships and crew



Status: captain milestones, three platform tiers, earned crew offers, renown/infamy, paid officer recovery and fort services are implemented. See [current expansion rules](../pirate-expansion.md); other examples below remain future design. See the [brief](design-brief.md) for complete examples.



| Progression | Meaning | Example sources and uses |
| --- | --- | --- |
| Renown | Overall fame, not morality | Islands, adventures, contracts, captains and treasure; NPC recognition and opportunities |
| Faction standing | Relationship with each faction | Choices and contracts; hostility, trade, recruitment and promotion eligibility |
| Infamy | Criminal notoriety | Raids, cargo theft, patrol attacks and bounties; hunters and intimidating opportunities |
| Captaincy | Ability and accomplishments as a commander | Vessel ownership, officers, equipment, crew and hideout development |



Castaway → Sailor → Adventurer → Captain → Pirate Legend are proposed milestone stages. They should recognize varied achievements, not collapse all play into one mandatory experience bar. Formal faction rank is another layer, not a replacement for independent captaincy.



## Ships and ownership



The current source offers 7x15, 11x23 and 15x31 platforms. Five NPC victories still unlock the first expansion; the independent exploration paths below are alternatives. Seated sailing and docked editing are the approved initial movement model. The existing five-victory requirement is provisional tuning, not the future complete captaincy system. Preserve earned progress when introducing richer milestones.



Sloop, Brig, Frigate and Legendary Galleon are proposed future classes with increasing crew, cargo, equipment and prestige. The three current platform sizes are not four completed ship classes. Names, independent/faction flags and earned cargo upgrades are implemented; Clipper, Ironclad and three earned faction specializations now extend these platforms. See [current ship rules](../endgame-expansion-playtest.md); distinct faction hull designs remain future work. Both faction members and independent players can own ships.



The optional **A Ship of Your Own** quest offers equivalent starter access by salvaging wreck parts, buying/repairing a vessel through trade, or claiming an outlaw crew's ship. The abandoned sloop's former captain disappeared into fog. None of these routes should block ordinary boat travel or existing ship construction.



## Crew



Preserve the user's initial first mate, shipworker (repair), cannoneer and crow's-nest lookout roles. Those roles exist as basic stationed crew in the current source. Their current targeting, supplies and lookout behavior are described in [naval gameplay](../naval-gameplay.md); they are not yet named adventuring companions.



[Mara the navigator, Bram the quartermaster and Nell the shanty singer](ship-identity-seals-and-officers.md) now have personal commissions and separate ship roles. Mara reports the existing compass route; Bram transfers carried supplies on request; Nell sings an optional playlist of four recordings. Combat-leading mates, boarding gunners, treasure knowledge and standard sailors remain future work.



Paid ordinary crew, standing-based Piglin/Pillager/Husk offers, Garrick's rescue and officer commissions are implemented. Hired crew have persistent captain ownership; resignation does not transfer or dismiss them. Docked roaming, shore support and forgiving wages/provisions morale are implemented. Cursed-artifact recruitment and a deeper loyalty system influenced by quests, victories or betrayal remain proposals.



**Garrick the Deserter** now has a [personal rescue](repeat-voyages-and-garrick.md#garrick-the-deserter): obtain his release papers at Saltwatch, speak to your captive, then recruit him as a named cannoneer or free him without hiring. Bounty choices and additional ranged abilities remain future proposals.



## Independent organizations and bases



Named crews, custom flags, home ports, docks, quarters, storage, treasure vaults, repairs, trade and defenses can grow an independent captain into a fourth power. Keep native building relevant. Diplomacy and territory can follow later; no full kingdom-management simulation is required. Shared ownership, guest access, cargo permissions and allegiance changes need explicit multiplayer rules before implementation.



## Implemented captain milestones



| Earned tier | Either achievement path | Platform / build height / block cap / guns | Expansion cost |
| --- | --- | --- | --- |
| Sailor | Available immediately | 7×15 / 6 / 512 / 4 | Build: 105 oak planks + helm |
| Captain | 5 NPC naval victories **or** 4 distinct guided discoveries and 2 claimed contracts | 11×23 / 8 / 1,024 / 8 | 148 oak planks |
| Veteran Captain | 12 NPC naval victories **or** 8 distinct guided discoveries and 4 claimed contracts | 15×31 / 10 / 1,536 / 12 | 212 additional oak planks |



These are initial tuning values. Each of the seven original claimed contracts counts once, as do the new First Binding contract and three officer commissions. No faction work is required for either expansion. Accepting, abandoning or finishing an objective without claiming does not award a milestone. Duplicate discovery coordinates do not count. Existing naval victories, ship tiers, cargo and crew survive migration. Hull caps are 200/400/600; larger vessels turn and sail more slowly. Cargo starts at 27 slots and can be upgraded to 36/54 through captain milestones and materials. Sailing remains seated, with up to four players.



Captain tier or higher reduces every ordinary crew hire by two emeralds. A claimed faction commendation plus Trusted standing instead grants that faction role's four-emerald discount; discounts do not stack. Existing hired crew keep their roles and behavior. The ship menu shows actual hire prices; the Journal's **Factions and Captaincy** entry shows standings, discoveries, claimed contracts, victories and unlock requirements. Briggs' faction dialogue also shows current standings.



Expansion validates loaded water, world border, new perimeter/height clearance and other vessels before consuming planks or placing blocks. The third tier retains the existing native build snapshot with a 1,536-block cap to stay within its synced-string budget. Native saves preserve that tier and maximum-size builds.


## Verification of faction and captain increment

October 14 targeted checks passed: common/client compilation; 20 pirate server GameTests; 11 naval server GameTests; 4 existing Journal server GameTests; and 3 Journal unit tests. The maximum-size third-tier test also passed after adding an actual native entity-sync packet encode/decode round trip. Coverage includes all three native outpost objectives, exact Drowned offering cost, independent standings, once-only rewards, legacy defaults, save/death copying, independent captain paths, existing five-win progression, earned crew prices/limits, blocked expansion, exact 148/212-plank costs, full 1,536-block capture/docking/save and Piglin zombification immunity.

All seven authored templates pass bounds, cache and walking-route validation; the overhead layout preview was inspected. Live combat balance, faction-sign appearance, ship handling/performance and menu layout still need in-game acceptance. This is not unfiltered fullCheck; no JAR was packaged and no version changed. Logs: `build/pirate-factions-final.log`, `build/pirate-captain-tests.log`, `build/pirate-max-ship-packet.log`, and `build/pirate-factions-journal.log`.
