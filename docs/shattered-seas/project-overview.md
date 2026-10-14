# The Shattered Seas: future development plan

Recorded October 14, 2026. This is a design and assessment milestone, not an implementation or release of the full vision. The complete user-supplied [design brief](design-brief.md), including the Logbound Compass addendum, is preserved verbatim. These topic pages organize it against the current repository; later user decisions take precedence.

## Vision and principles

A pirate sandbox RPG where players explore an ocean archipelago, recruit crews, seek treasure, join competing factions or become independent captains, and uncover an optional supernatural mystery. Bright tropical adventure sits alongside haunted seas and dangerous ruins. The world provides opportunities; players choose their legend.

Confirmed direction: craftable Overworld access before the dragon; safe return; three distinct factions based on Piglins, Illagers and mixed undead; optional contracts and lore; multiple progression paths; independent crews; useful native Minecraft building. Ordinary exploration must not require a faction, introductory ship quest or linear campaign. Players can use boats and ignore the story.

## Documentation map

- [Harbor and contracts](harbor-and-contracts.md): implemented Briggs, compass acquisition, four optional contracts, island encounters and Journal clues.
- [Factions](factions.md): identities, combat hierarchy, standing and proposed rewards.
- [Player progression](player-progression.md): renown, infamy, captaincy, ships and crew.
- [Logbound Compass prototype](logbound-compass.md): current behavior and testing instructions.
- [World design](world-design.md): hub, islands, Logbound Compass, events and territory.
- [Quests and lore](quests-and-lore.md): optional activities, Seven Seals and named characters.
- [Technical assessment](technical-architecture.md): existing systems, persistence, dependencies and risks.
- [Implementation roadmap](implementation-roadmap.md): ordered increments, acceptance and first step.

## Existing implementation versus future scope

The current source has Pirate Seas travel, ocean/islands, a shared harbor, three editable ship platform sizes, seated sailing, cannons, four basic crew roles and NPC/player ship combat. Current mechanics and limitations are documented in [dimension implementation](../pirate-dimension.md) and [naval gameplay](../naval-gameplay.md). They are a foundation, not completion of this brief; live visual and multiplayer acceptance remains outstanding.

The Shattered Seas, Breaker's Rest, Old Salt Briggs, Rusty Anchor, faction leaders/ranks, Seven Seals and Logbound Compass are working design names. This documentation does not rename `darkspawn:pirate_seas` or change existing worlds. A [first compass prototype](logbound-compass.md) now provides an island-study timer and continuing routes with saved discovery history. Briggs, a first survey contract and new-harbor tavern/market are implemented. Separate faction standings, three outpost activities and captain milestones are now implemented. Single-faction membership, local diplomacy and saved crew loyalty are implemented. Broader hub services, party navigation, morale, territory conquest and narrative endings remain future work.

## Decisions still needed

Exact standing gains and thresholds; joining/leaving penalties; party and crew permissions; deaths and respawns; cross-dimension crew behavior; island spacing and sailing time; NPC dialogue/quest presentation; compass timer and replacement rules; territory capture disputes; and the entity, mechanics and consequences behind the Seven Seals. Proposed class names and supernatural abilities are not final specifications. Resolve these at the relevant milestone rather than blocking the entire project.
