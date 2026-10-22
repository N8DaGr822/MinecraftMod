# Ascension bridges and protection (M2)

Implemented October 19, 2026 on the existing Fabric runtime. This extends the [M0/M1 foundation](decisions.md); generator state-machine work, new objective phases and custom automation remain M3-M5.

## Building and ownership

Build bridges manually between allocated, active cardinal-neighbor islands. Centers stay 250 blocks apart and claims remain inclusive at offsets -100 through +100. The neutral build corridor is seven columns wide, at Y72 through Y96 inclusive. For centers (0,80,0) and (250,80,0), eligible neutral positions are X101..149, Z-3..3. North/south and negative-coordinate corridors follow the same rule. Diagonal, unallocated and archived destinations grant no build corridor. No bridge is automatically generated.

Both endpoint teams may build in the corridor, but each successfully placed neutral block belongs to the placing team. A neighbor cannot replace or break it. Walking across a bridge grants no edit permissions. The original claim rules still apply at either end: reaching a neighbor's edge does not permit building onto their island. Existing owned bridge blocks remain removable by their owner if the destination is later archived, but no new construction toward the archived destination is allowed.

Neutral bridge materials are planks, logs, stone, cobblestone, stone bricks, deepslate, cobbled deepslate, glass, and non-weathering stairs/slabs/fences/walls. They must have no block entity or fluid. Containers, machines, explosives, gravity blocks, dirt and weathering copper are excluded. This deliberately limits neutral construction to stable building materials whose ownership can be retained and cleaned up predictably. Full normal building remains available inside an active team's own claim. Non-block item use against neutral bridge blocks is denied, so tools cannot silently transform bridge materials or ignite them.

The separate `ascension_bridges` SavedData records schema 1 and each neutral position's team UUID, canonical corridor ID and block identifier. Record only successful native placement; canceled attempts create no ownership. Successful owner breaks remove their record. Invalid/unsupported bridge schemas disable neutral edits/cleanup without rewriting the decoded records. No player inventory or legacy OneBlock data is reset.

## Archival and cleanup

`/ascension archive` explains the operation. `/ascension archive confirm` performs it for the owner only. Every member must be online and outside both OneBlock modes; this avoids leaving an offline player inside an island whose membership has been released. No kick/owner-transfer/offline-admin archival workflow is added here.

Archival releases active memberships, retains the team as a historical record, and keeps its island cell reserved and protected against player edits. Members may create or join another active team afterward. Their personal Ascension item profiles are retained. Island terrain is not deleted, cells are never recycled, and there is no unarchive command in this milestone.

Only that team's recorded neutral bridge blocks are cleanup candidates. An archived marker in the team ledger is the durable cleanup request, so loading the saved ledgers resumes remaining work. Neighbor-owned bridge blocks are preserved. A current block with a different identifier than the recorded material is treated as an administrative replacement and retained; its stale ownership record is removed.

Cleanup examines at most 64 records per second, rotating entries so unloaded chunks do not starve loaded work. It never requests chunk tickets and only mutates already-loaded chunks. Removal suppresses neighbor and neighbor-shape propagation, preventing boundary cleanup from loading adjacent chunks. Blocks are removed without item drops. Nearby fence/wall connections may keep their old visual shape until a normal subsequent block update; this is the tradeoff for cleanup that never propagates into unloaded chunks. Clear visitors from a route before archiving; cleanup can remove the floor beneath someone still walking there.

The team ledger upgrades valid schema-1 saves to schema 2, preserving IDs, membership, allocation, geometry and progress. The new optional archived flag defaults to false for existing teams. Malformed or incompatible decoded records are preserved rather than treated as a new grid. Older M1-only code rejects schema 2 instead of treating archived teams as active.

## Native protection and behavior changes

These checks apply only to Ascension. Other dimensions retain their normal rules.

| Native operation | Ascension rule |
| --- | --- |
| Explosion terrain destruction, block triggering and fire | Disabled throughout Ascension; explosions retain entity effects |
| Explosion effects on block-like entities | Disabled |
| Fire spread and lava random ignition spread | Disabled throughout Ascension; existing/placed fire is not itself erased by this rule |
| Water/lava propagation | Source and target must be inside the same active claim; no neutral flow |
| Player bucket placement | Own active claim only, including when targeting an owned bridge |
| Pistons, including sticky pushes/pulls and head removal | Every affected source/destination must remain in the active piston claim |
| Dispensers/droppers | Output target must remain inside the same active claim |
| Hoppers, including native hopper intake | No neutral or cross-boundary container operation |
| Supported piston/dispenser/dropper/hopper operation | At least one team member must be online and physically present in the claim |

Inactive supported machines do not queue missed redstone pulses or simulate offline production. Tick-driven machinery resumes when a member returns; a piston/dispenser/dropper may need a new redstone update or pulse. Ordinary within-claim machines remain functional while a member is present.

This is a defined set of native protection hooks, not universal mod compatibility or complete anti-grief support. Operator world edits remain administrative authority. Unsupported machines, third-party mutations, special entity-driven block changes, natural vegetation growth and item pickup are not covered by a blanket ownership transaction system. No neutral machine support, Create adapter, custom auto-miner or chunk-loading bridge is claimed. Players can still interact with native hazards and combat; preserved terrain does not make visitors invulnerable.

## Implementation changes

- `AscensionLedger`: additive archival state and versioned schema upgrade; keeps archived cells reserved while releasing active membership.
- `AscensionBridges`: corridor geometry, durable per-block ownership and bounded loaded-chunk cleanup.
- `AscensionProtection` and existing placement/interaction/bucket hooks: distinguish permission to create a bridge from permission to edit an existing owned block; restrict neutral materials and item use.
- `Ascension` commands/events: archive command, successful-break bookkeeping, and periodic cleanup alongside existing loaded-island maintenance.
- Native explosion/fluid/piston/dispenser/hopper and server-level fire hooks: enforce the supported environmental rules before the relevant mutation.
- Unit and server tests: geometry endpoints, schema upgrade, ownership, archival/reload, unloaded chunks, native mutation attempts and outside-world behavior.

## Verification

Verified October 19, 2026: common/client compilation and all nine OneBlock unit tests passed. All 53 selected server tests passed: Ascension foundation 8, bridges 7, legacy OneBlock 9, storage 10, Forsaken tools 4, Forsaken threats 6 and field utilities 9. The seven bridge tests were rerun successfully after the final cleanup boundary fix. `git diff --check` passed. No JAR, version change, push or fullCheck is part of this implementation pass.

The bridge checks verify successful placement/ownership, neighbor denial, rejected materials/fluids, archival safety, resumed cleanup after codec reconstruction, preservation of neighboring ownership and administrative replacements, bounded cleanup, unloaded records, and adjacent chunks remaining unloaded after boundary removal. Native-runtime checks exercise piston resolution, water/lava ticks, explosion/fire behavior, dispenser/dropper output, hopper rejection and permitted within-claim transfer, absence of team members, and outside-world explosion behavior. A Minecraft direction-API mismatch in the test fixture was corrected before the successful runs.

```powershell
.\gradlew.bat classes clientClasses test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1
```

The selected server group includes the existing foundation and new bridge tests. Regression groups are `one_block_game_test*`, `storage_game_test*`, `forsaken_tools_game_test*`, `forsaken_threat_game_test*` and `field_utilities_game_test*`. The repository's documented process-local Windows socket fallback is used for Gradle here; no permanent JVM setting is changed.

Manual acceptance remains: two players building from opposite islands, walking/visiting, restarting an existing world with partially unloaded archived routes, and inspecting cleanup updates in the client. Automated codec reconstruction and headless server tests are not a full external-process backup/restore or multiplayer playtest.

[M3 generator states, guarded harvesting and reconciliation](generator.md) now extends this implementation. The current six resource phases remain until M4 objective phases are implemented.
