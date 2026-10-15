# Implementation roadmap

Current acceptance and remaining-work inventory: [pirate polish review](../pirate-polish-review.md). Phases below describe the original sequence; several are now implemented in part and are not a list of wholly missing features.

Earlier increment: [ship names, flags, earned cargo upgrades, the first Seven Seals clue, and Mara/Bram/Nell recruitment](ship-identity-seals-and-officers.md). Nell now sings four user-supplied recordings through streamed ship-following audio.

Status: implementation started October 14, 2026. The [Logbound Compass navigation slice](logbound-compass.md), island-only recalibration, continuing non-repeating routes and six island types are now in source; [Briggs, survival acquisition, new-harbor tavern/market and the first survey contract](harbor-and-contracts.md) are also in source. The exploration expansion adds native island encounters, persistent Journal clues, useful naval salvage, named compass feedback and three more optional contracts. Separate faction standings, one optional activity for each faction, earned crew offers and independent captain milestones with a third platform tier are now implemented. Single-faction membership with resignation consequences, local reputation-aware guards, friendly outpost contacts and loyal distinct-species crew are now implemented. [Repeat voyages, Garrick's personal rescue and bounded faction naval patrols](repeat-voyages-and-garrick.md) are also implemented. The [current expansion](../pirate-expansion.md) implements compact menus, paid officer recovery, harbor services, renown/infamy, ghosts and hunters, fort capture with offline takeovers, and all seven seals with personal endings. Optional larger ship/crew systems remain planned. Keep the existing [dimension](../pirate-dimension.md) and [naval](../naval-gameplay.md) foundation intact.

## First implementation: navigation slice

The first increment implements one **Logbound Compass navigation slice** against the existing guaranteed first cove: stable destination identity/boundary, a server-owned per-player route record, direction display, and once-only arrival detection. Briggs now provides survival acquisition, with Creative and `/give` available for development testing. Test save/reload and two players independently before adding timed recalibration or procedural route selection.

This produces a useful, independently testable exploration feature without first building three factions, a quest framework or a new ship engine. The user has confirmed nine-minute island-only timing. The current implementation preserves per-player progress through death and replacement, and keeps exploration unrestricted. Remaining expansion choices are listed in [world design](world-design.md).

## Ordered increments

| Phase | Small tasks and dependencies | Acceptance criteria |
| --- | --- | --- |
| 0. Foundation acceptance | Playtest existing ships with two players; verify docking, reload, crew, PvP, return travel and existing saves | No lost/duplicated cargo or captured builds; ownership enforced; protected harbor works; visual issues recorded and fixed before expansion |
| 1. Guided exploration | First compass slice above; then agreed timer/replacement rules and a second destination | Direction works beyond visibility; boundary starts one cycle; approximately 8–10 minute target; restart/death behavior matches chosen rules; independent travel remains possible |
| 2. Breaker's Rest | Authored hub expansion, neutral arrival area, Briggs, tavern/market/board/lighthouse; migration policy first | Existing builds/loot preserved; new players can arrive, obtain compass and return safely; guide optional; no accidental faction fighting |
| 3. One contract end to end | Minimal interaction and persisted objective/reward using an independent recovery or exploration contract | Accept, complete, abandon/retry as designed; no duplicate rewards on repeat packets or reconnect; multiplayer completion semantics explicit |
| 4. Factions and progression | Stable IDs, independent standing/renown/infamy/captaincy; one outpost and distinct controlled encounter per faction; neutral targeting rules | Standings persist independently; hostile/neutral rules enforced; non-members retain playable activities; no rank solely from unvalidated client claims |
| 5. Cohesive vertical slice | Add faction contracts, initial membership/promotion path, two example recruits, treasure/wreck content and a Seven Seals clue | Complete the checklist below with faction-aligned and independent playthroughs; optional lore never blocks return or exploration |
| 6. Captain and crew expansion | Varied recruitment, named officers, navigator/quartermaster roles, ship acquisition routes and milestones; build on current sizes | Preserve existing ship ownership/earned progress; crews obey owner and resource rules across reload; alternatives provide equivalent starter access |
| 7. Living seas | Bounded faction skirmishes, ghost encounters, rumors and infamy hunters | Spawn caps/cooldowns and cleanup hold; encounters can be ignored; no world-wide scans or unbounded chunk loads |
| 8. Territory and late legends | Resolve capture/shared-world rules, then one fort; expand ship classes, legendary captains and Seven Seals only after earlier acceptance | Ownership persists without overwriting player builds; disputes/permissions tested; any story ending allows continued sandbox play |

The first technical compass prototype can precede the full hub. Player-facing compass acquisition depends on Briggs/hub work. Later phases may be split further; do not combine them into a single rewrite.

## First cohesive playable vertical slice

The brief's target slice expands the existing foundation with:

- Breaker's Rest gateway, neutral harbor, Rusty Anchor, contract board and optional Briggs guidance.
- One outpost and one functional, distinct encounter for each faction, with a small representative mob set.
- Persisted basic standing, renown, initial infamy and a first captaincy milestone.
- At least two example recruitable characters; preserve the four existing naval station roles.
- A treasure island, hostile location and dungeon or wreck; useful rewards and safe return.
- One contract for each faction plus one independent captain activity.
- One discoverable journal, ancient symbol clue and connection to the Seven Seals.
- The requested compass loop, once timing/ownership rules are settled, without mandatory story or faction gates.

The first 30 minutes should offer arrival/exploration, optional guidance, a chosen small activity and a first voyage. Timings are pacing targets, not forced tutorials. Completion means these systems work together in a tested slice; it does not mean all ranks, islands, ships or endings are delivered.

## Acceptance and change discipline

For each increment: choose the smallest code/data surface, add relevant persistence/authorization tests, compile common/client sources, validate changed assets, and playtest the actual interaction. Include death/reconnect, two players with different progress, duplicate hand-ins and old-save compatibility when affected. Record balancing assumptions separately from approved rules. Keep the original brief and this roadmap linked as decisions evolve.
