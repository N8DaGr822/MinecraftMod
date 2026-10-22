# M3 generator state and reconciliation

Implemented October 19, 2026 on the existing Fabric runtime. This extends [M1 teams](decisions.md) and [M2 bridges](bridges.md); [M4 objective phases](phases.md) now extend this work; M5 encounters and custom automation remain pending.

## Player behavior

The existing six phases, weighted resource pools, renewable dirt/wood schedule, native tool-tier requirements, and milestone supply chests/creatures remain. A confirmed survival core removal increments team progress once, even when the held tool cannot harvest its represented ore. Creative removal restores the same resource without progress, resource rolls or rewards. One confirmed removal per team per server tick remains the limit.

A mismatched core resource is denied until repair. `/ascension info` now shows generator state and generation number. A missing or incorrect core is repaired from saved state when entering an island or during loaded maintenance while a member is present. Repair never counts as mining and never emits loot, experience, milestone chests or creatures.

## Transaction and state

Each team's existing ledger entry now contains its generator. Its identity is the durable team/island UUID together with a monotonically increasing 64-bit generation number. A live break also gets a unique reservation token; stale tokens cannot finalize a later reservation, even when an earlier attempt was canceled without advancing the generation.

| State | Meaning and recovery |
| --- | --- |
| READY | Expected core can be reserved after ownership, resource and tick checks |
| PENDING | Native break is in progress; nested attempts are denied. A loaded PENDING snapshot has no live token and is repaired without credit |
| REGENERATING | Confirmed removal committed its generation, progress and next resource; write that resource without re-rolling it |
| LOCKED | Archived or exhausted generation counter; no mining or automatic repair |
| ERROR | World write failed; mining stays denied until loaded reconciliation succeeds |

The existing interaction mixin wraps the native `destroyBlock` call using bundled MixinExtras. The Fabric BEFORE callback retains protection checks; AFTER only marks confirmed removal. Its hook runs after successful native removal but before native loot emission. Finalization in `finally` releases a canceled/rejected reservation, or commits a confirmed one and regenerates after native harvesting finishes. Native early tool rejection, another Fabric listener canceling, nested calls, same-tick teammate attempts and stale finalization cannot add progress. No new dependency was introduced.

Live milestone effects run only on successful eligible finalization. Reconciliation never calls the harvesting or reward path. `OneBlock` exposes two package-local helpers for its existing resource selection and milestone effects so legacy and Ascension retain the same tables without duplicating them. Legacy keeps its existing harvesting flow.

## Persistence and work limits

The Ascension ledger is schema 3. Valid schemas 1 and 2 preserve team identities, memberships, cells, archived reservations and all progress. Missing old generators are initialized independently from the old total-break count; archived ones start locked. Joining/leaving retains the same generator object. Archival locks it. Current-schema entries missing generators, unknown generator states, negative generation numbers, and incompatible ledger schemas/geometry fail closed while retaining their decoded data.

Progress, the selected next resource, generator state, generation and last confirmed tick are encoded in the same team ledger. Live reservation tokens are deliberately not persisted. The generation advances for confirmed creative removal too, but its progression counters do not.

Periodic maintenance examines at most 32 team records every 20 ticks and rotates across records, including archived and idle entries. This bounds repair work and prevents idle records from permanently starving later active islands. With more than 32 total teams, automatic repair can take multiple passes. Normal successful mining still regenerates immediately. Periodic repair requires a member in the claim and an already loaded core chunk. World writes suppress neighbor-shape propagation so even a chunk-boundary core cannot load adjacent chunks. It creates no chunk tickets or offline catch-up work.

## Verification and limits

Common/client compilation and all 14 OneBlock unit tests passed. The first Ascension server pass ran 20 tests successfully (foundation 8, bridges 7, initial generator checks 5). The final six generator tests and nine legacy OneBlock tests also passed: 30 distinct server checks in total. `git diff --check` passed.

Tests cover migration, missing/invalid generator data, independent generator instances, stale tokens, repeated finalization, same-tick exclusion, cancellation, creative removal, counter exhaustion, rotation across 81 teams, real native drops/tool restrictions, serialized PENDING and REGENERATING reconstruction installed into server storage, exact-resource repair, ERROR recovery, locked/archive behavior, and unloaded core/neighbor chunks. The final generator pass verifies live milestone rewards and native early tool rejection.

```powershell
.\gradlew.bat classes clientClasses test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension_generator_game_test*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:one_block_game_test*' --max-workers=1
```

This establishes exactly-once progression for the supported live server break path and idempotent reconciliation of saved ledger snapshots. It does not make Minecraft chunk/entity saves and SavedData a single disk transaction. An abrupt process/power loss can lose unsaved progress or loot; recovery intentionally does not replay missed milestone rewards. External-process crash injection, backup/restore and multiplayer latency checks remain M6/manual acceptance. M4 adds objective/reward ledgers; see phases.md. No universal third-party mutation or event-order compatibility is claimed.

No JAR, version bump, push or full-suite release verification is part of this pass. M4 is now implemented for new teams; see phases.md for the next milestone and the preserved legacy path.
