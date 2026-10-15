# Technical assessment

Assessment date: October 14, 2026, against the current working tree. This plan proposes incremental extensions; no framework installation or rewrite is authorized by documenting it.

## Verified repository baseline

[gradle.properties](../../gradle.properties) targets Minecraft **26.3**, Fabric Loader **0.19.5**, Fabric API **0.162.0+26.3**, GeckoLib **5.5.7** and Loom **1.18-SNAPSHOT**. [build.gradle](../../build.gradle) uses Java **25**, split common/client source sets, Fabric GameTests and JUnit. No external moving-ship or NPC quest framework is declared. Version compatibility must be checked against this project when implementing, not assumed from older Minecraft examples.

| Existing system | Evidence / reuse | Limits for this vision |
| --- | --- | --- |
| Dimension and return | [Pirate](../../src/main/java/darkspawn/black/pirate/Pirate.java), [player state](../../src/main/java/darkspawn/black/pirate/PiratePlayerState.java) | Travel exists; expanded arrival presentation and respawn design do not |
| Shared authored harbor | [PirateHarbor](../../src/main/java/darkspawn/black/pirate/PirateHarbor.java), native templates | One-time generation preserves edits and finite loot; not the complete Breaker's Rest |
| Island generation | [PirateIslandStructure](../../src/main/java/darkspawn/black/pirate/PirateIslandStructure.java), [asset generator](../../tools/assets/pirate-foundation.cjs) | Flat ocean with seeded templates; future biome/island diversity and navigation IDs need design |
| Custom ships | [PirateShip](../../src/main/java/darkspawn/black/pirate/PirateShip.java), [Ships](../../src/main/java/darkspawn/black/pirate/Ships.java) | Captured blocks while sailing, native blocks while docked, seated players; no free walking on moving decks |
| Naval combat and UI | [Cannonball](../../src/main/java/darkspawn/black/pirate/Cannonball.java), [ShipMenu](../../src/main/java/darkspawn/black/pirate/ShipMenu.java) | Shared hull damage and simple patrols; not boarding AI or faction warfare |
| Information discovery | [Journal](../../src/main/java/darkspawn/black/journal/Journal.java), [Journal implementation](../journal-implementation.md) | Existing catalog/snapshot pattern; no generic contracts or branching dialogue engine |
| Basic crew | PirateShip's native Villager/Piglin/Pillager/Husk passengers with saved captain tags | Four loyal station roles; no roaming companions, personal quests or morale simulation |

The brief's question about ship technology has an initial answer: this repository now has custom ship movement with seated sailing and docked editing. Future free movement/boarding or larger classes remain separate technical decisions. Reuse the working model before considering another dependency.

## State and authority

Current pirate player state saves entrance and NPC naval victories, preserves them through player copying, and resets travel cooldown on load. Harbor initialization is world SavedData. Vessels own their saved owner UUID, block layout, cargo and hull/combat state. Extend these conventions surgically with migration-safe defaults; do not overwrite existing ship progress when adding captaincy.

Implemented player state includes independent faction standings, membership, contract outcomes/cooldowns, personal discoveries, compass route/timing, Garrick and officer commissions. Renown, infamy, all seven seals and personal endings now persist per player. Island encounter slots are saved per world, and patrol lifetime/provocation is saved per vessel; fort ownership, sinking rescue claims and Admiral encounter receipts now persist in SeaLedger. Ship/crew identities and assignments belong to their persistent entities. Distinguish private knowledge from shared world changes; a shared gate or fort must not award every player the same personal progress.

Server authority applies to objectives, payments, rewards, reputation, boundary entry, timers, crew orders and territory capture. Client packets express intent; server checks ownership, range, current state and repeat submissions. Follow existing payload/menu and Journal snapshot patterns. Avoid assuming client-supplied destinations or reported kills are authoritative.

Use stable identifiers and small data-driven definitions for faction metadata, contracts, rewards, dialogue and loot where useful. These are conceptual domains, not a request to create an abstraction for each one. Implement the first real content case before generalizing.

## Risks and decisions

- **Existing saves:** harbor expansion must not regenerate over player edits or restock loot. Choose fresh-world-only templates or narrowly versioned additions with obstruction checks.
- **Neutrality:** native Piglin/Illager/undead behavior can conflict with faction and crew allegiance. Explicitly test target selection, neutral harbor behavior and species-specific transformations/environmental effects.
- **Naval scope:** collision envelopes, docking, passenger lifecycle, entity/chunk unload, restart and multiplayer rendering need playtesting. Station crew do not provide boarding/pathfinding support.
- **World pacing:** island spacing and an 8–10 minute compass cycle need player tests; distant navigation must not become unrestricted chunk generation/loading.
- **Reward duplication:** reconnect, death, repeated interaction, shared kills, duplicated compass items and contract hand-ins need server-owned once-only semantics.
- **Ownership:** parties, cargo access, stolen vessels, faction switching, territory disputes and global story endings require explicit rules.
- **Performance:** use loaded-area queries, bounded spawns, cooldowns and cleanup. Prefer controlled encounters to global scans or permanent forced chunks.
- **Presentation:** new dialogue/contract interactions should use existing UI styling and Journal conventions, with readable HUD limits and accessibility reviewed in game.

## Verification boundary

Existing implementation guides record source checks and remaining playtests. They do not certify the future features described here. Follow [testing workflow](../testing-workflow.md): compile common/client classes, run focused JUnit and GameTests, validate relevant assets, then perform live gameplay checks. A multiplayer naval playtest is still needed. Do not package a JAR or change the version without a build/release request; run unfiltered fullCheck before any push.
