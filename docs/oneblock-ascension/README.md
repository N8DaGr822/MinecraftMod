# OneBlock Ascension future implementation plan

Saved October 14, 2026 for later development. This is a deferred proposal, not an implementation milestone or a change to the active pirate work. The [complete supplied brief](source-brief.txt) is preserved verbatim. The user selected Fabric within the existing Darkspawn project on October 14, 2026. The embedded instruction to implement M0 and M1 applies when implementation is requested; the current follow-up reviews and updates the saved plan.

## Scope and repository fit

Build a server-authoritative multiplayer OneBlock mode with one regenerating resource block and independent progression per team island. Teams support up to six players, team permissions and visitors who may travel but cannot edit another team's claim or use its generator. Survival, RPG encounters and bounded automation share persistent progression.

**Confirmed platform: Fabric, within the existing Darkspawn project.** Retain the repository's Minecraft 26.3 / Java 25 toolchain and current loader rather than creating a NeoForge-first project or downgrading to the original brief's preferred 1.21.1 / Java 21 baseline. Reuse and extend the existing OneBlock implementation under `src/main/java/darkspawn/black/oneblock`. The original brief remains verbatim design history; this confirmed platform decision supersedes its loader order and preferred baseline.

Preserve existing worlds, the shared island, inventory profiles and earned progression. Before introducing team islands or changing island geometry, define explicit versioned migration or coexistence behavior. The current shared-island save must not be silently reinterpreted as a newly spaced team grid. Keep domain logic independently testable using existing project conventions; additional loader adapters or a multi-module reorganization are not prerequisites.

The referenced `OneBlock_Ascension_Spec_v0.2.md` was not found in the repository. The supplied reproduced requirements are retained here; do not invent missing specification content. Before implementation, inspect the current repository again, verify compatible Minecraft/Java/loader/Gradle and any Architectury or optional-mod versions against official documentation and available artifacts, and record actual pinned choices in a scoped decisions document. Verify changes against the existing Fabric runtime; do not claim additional loader support.

## Existing Fabric baseline to preserve

The current mode has one shared 3x3 starter island, a protected regenerating source, free inventory-menu travel and separate saved possession profiles. Character upgrades remain shared. Its six phases currently require 300, 600, 800, 1,000 and 1,200 breaks before an endless End phase; advancement is manual after each threshold. These are verified in `OneBlockProgress.java` and the current README, and supersede older historical counts in `oneblock-dimension-notes.txt`.

Ascension proposes team-specific islands, claims and a new three-phase MVP with objective/encounter gates. That changes both world geometry and progression ownership. M0 must explicitly map the existing shared island and its progress into the new design, or keep it as a legacy mode alongside new team islands. Do not reset or silently convert existing saves. First implement foundation milestones M0/M1; bridges, generator replacement and new progression content follow in M2-M5.

## Required island and bridge geometry

These values supersede any older 1,024-block spacing assumption for the proposed Ascension design, not for existing saves without migration.

| Setting | Required default |
| --- | --- |
| Cardinal center spacing | 250 blocks |
| Center | `(gridX * 250, 80, gridZ * 250)` |
| Starter platform | 5 by 5 at Y80 |
| Square claim | Inclusive X/Z half-extent 100; 201 by 201 columns |
| Cardinal neighbors | Exactly one cell apart on one grid axis |
| Visitors | Entry allowed; building and generator use denied |
| Neutral bridge corridor | Seven columns wide |
| Bridge build heights | Y72 through Y96 inclusive |
| Bridge destination | Allocated cardinal-neighbor island only |
| Offline automation | Disabled |
| Bridge chunk loading | Disabled |

Claim membership is `abs(x-centerX) <= 100 && abs(z-centerZ) <= 100`. For centers at X0 and X250, claims cover -100..100 and 150..350. The neutral gap is **101..149 inclusive: 49 integer columns**, with a 50-block geometric edge-to-edge distance. The east/west corridor spans Z-3..3 across that gap; north/south is symmetric. Center spacing is not bridge length measured from the starter platform.

Bridges are player-built; never create automatic connections. Within claims, team permissions apply. Neutral-zone building is allowed only in a valid corridor and height band; general void building is denied. Persist each neutral bridge block's owning team and corridor ID. Protect supported breaking, explosion, piston, fluid and machine mutations. Archiving a team/island starts resumable cleanup of only its own bridge blocks. Visitors may cross without gaining edit rights. Neither bridges nor offline islands force chunks to remain loaded. Third-party protection requires tested adapters, not a universal compatibility claim.

Use a versioned configuration schema. Reject hot changes to spacing or claim geometry on populated worlds with a migration-required explanation.

## Progression and state guarantees

The future content roadmap has 12 data-driven phases; the MVP implements Genesis at 250 breaks, Caverns at 500 and Verdant Wilds at 600. Completion requires the break threshold **and** required objectives **and** required encounters. Mining beyond the threshold must continue without deadlocking regeneration. Later MVP content includes a Cave Guardian event, a collection upgrade and a bounded auto-miner with output backpressure and no offline operation.

Persist teams, islands, generators, progression, bridge ownership, upgrades, encounters and reward ledgers with durable identifiers and schema versions. Exactly one confirmed eligible generator break produces exactly one progress increment; canceled, unauthorized, stale and duplicate attempts produce zero. Generator states are READY, PENDING, REGENERATING, LOCKED and ERROR; regeneration is idempotent by `(islandId, generationIndex)`. Restart reconciliation must repair world/state disagreement without extra progress or rewards.

Validated JSON/datapacks define phase content. Failed reloads preserve the previous valid configuration. Rewards use unique ledger IDs. Gameplay and world mutations run on the correct server thread, never under client authority. Use verified loader world persistence APIs; no external database, REST API or cloud service is needed for the MVP. Optional Create or other mod integrations belong behind adapters, outside common/domain imports.

Preserve the proposed domain models: TeamState, IslandState, GeneratorState, PhaseProgress, BridgeCorridorState, BridgeBlockOwnership, RewardLedger, UpgradeState and EncounterState. Proposed service boundaries are IslandService, TeamService, ProtectionService, BridgeService, GeneratorService, PhaseService, ObjectiveService, EncounterService, UpgradeService and PersistencePort. Reconcile these boundaries with existing patterns before introducing modules or abstractions.

## Milestones when activated

| Milestone | Deliverable |
| --- | --- |
| M0 | Confirm existing Fabric toolchain and entry points, testable domain logic, build/test/dev-server commands, and save migration/coexistence decision |
| M1 | Void dimension, durable grid allocation, starter islands, team creation/join/rejoin, owner/member identity, home/spawn flow and basic ownership protection |
| M2 | Bridge geometry, placement/removal rules, ownership ledger, visitor travel, resumable archival cleanup and supported grief protection |
| M3 | Guarded generator breaks, weighted resources, idempotent regeneration, bounded work and restart repair |
| M4 | Three MVP JSON phases, counters, submission objectives, guaranteed resources, ledger rewards and atomic validated reload |
| M5 | Cave Guardian lifecycle, collector and bounded auto-miner using the same generator service |
| M6 | Persistence/network hardening, compatibility tests, backup/restore checks and profiling with 25 active islands |
| M7 | Final Fabric integration, end-to-end acceptance and release readiness; other loaders are outside the confirmed scope |

The first authorized implementation pass should complete **M0 and M1 only**, report verified behavior and remaining limits, then stop with a concrete M2 plan. Do not label later milestones implemented. Follow the repository build/release policy when implementation begins; this saved brief does not authorize packaging, version changes or a loader migration now.

## Acceptance checklist

- Two teams receive distinct durable cells; cardinal centers are exactly 250 blocks apart and their inclusive claims never overlap.
- Test claim edges -100, 100, 150 and 350, neutral columns 101..149, both corridor orientations, seven-column width, height endpoints, and rejection of diagonal or unallocated destinations.
- Non-members cannot build, mine or operate another team's generator; visiting remains possible. Team identity, roles and allocation survive reload/rejoin.
- M2 preserves bridge ownership across restart, rejects edits to another claim or another team's bridge blocks, cleans up only archived ownership, and never loads chunks just to maintain bridges.
- M3 gives exactly-once progress and regeneration under canceled, duplicate, stale and unauthorized attempts, and restart reconciliation.
- Later phases cannot bypass objectives/encounters, duplicate rewards, deadlock after thresholds or replace valid content on failed reload.
- Run milestone-appropriate compilation and pure-Java tests, then available server/runtime checks. Record exact commands and actual results; explicitly identify unverified runtime behavior.

The platform decision is settled: Fabric in Darkspawn. The next implementation step is M0/M1, beginning with the existing code/save audit and explicit treatment of the current shared island. Existing gameplay remains unchanged during this plan review.
