# M5 Cave Guardian and island automation

Historical M5 record. The [October 20 second chapter](chapter-two.md) adds Storm/Ember Guardians, the relay and drive upgrades, and live automation status. Original collector/miner rules below remain except where the purchased upgrades explicitly extend them.

MVP teams now have the Caverns encounter and two permanent team upgrades. Existing six-phase teams and the shared OneBlock island retain their behavior. Upgrade and encounter state is saved inside each MVP team's phase record with an additive feature schema; older MVP saves receive independent disabled defaults.

## Cave Guardian

After completing the Caverns break threshold and item submissions, use `/ascension guardian start` while safely within 16 blocks of your core. Any eligible survival teammate can start the encounter. Mob spawning must be enabled and difficulty must be above Peaceful. Clear standing space on the platform; spawning never removes blocks.

The Guardian uses the native iron golem appearance and movement, an 80-health boss bar, native melee with attack attribute 5, and a ground slam. The slam marks a fixed spot for one second, then deals 6 base damage within 2.5 blocks. Move away from the flame marker. Normal armor/damage mechanics still apply. This is initial balance, subject to playtesting.

Only living, non-creative, non-spectator teammates within 32 blocks of the core can fight it. Visitors and environmental damage cannot award victory. A qualifying player kill records the encounter once; `/ascension next` then completes Caverns when its other requirements remain satisfied. The Guardian uses native golem death drops; the phase reward ledger remains separate.

Each team has at most one active Guardian, with eight loaded Guardians allowed server-wide. Encounters expire after five minutes, or cancel when no eligible teammate remains nearby, the actor leaves its radius, mob spawning/difficulty becomes incompatible, or the actor unloads. Restart reconciliation cancels interrupted encounters, and orphan actors are discarded when loaded. Cancellation grants no victory or drops; retries are free. No encounter forces chunks to stay loaded.

## Dedicated chest and upgrades

Place a **single ordinary chest immediately east of the core, one block above its base**: core position plus `(1, 1, 0)`, on the starter platform. `/ascension automation` prints the exact coordinates and installed/enabled state. Double chests, trapped chests and other containers are not supported. Replacing the single chest at that position resumes operation with the replacement contents; purchases belong to the team.

Only the owner can purchase or toggle upgrades while safely on their own island. Hold the required iron ingots in the main hand:

| Command | Cost and prerequisite | Behavior |
| --- | --- | --- |
| `/ascension upgrade collector` | 16 iron ingots; Genesis complete | Collects dropped items within three blocks of the core's block box, respecting pickup delay |
| `/ascension upgrade miner` | 32 iron ingots; collector installed and Cave Guardian defeated | Mines through the guarded generator service, at most once per five seconds |
| `/ascension automation on` / `off` | Collector installed | Enables or pauses both upgrades |

The chest's **first slot is reserved for a plain, unenchanted iron pickaxe**. Slots 2-27 receive output. The collector never fills the tool slot. The miner consumes one durability per successful harvest and stops when the tool breaks or is missing/unsupported. Native block loot and tool-tier requirements apply; automated mining produces no XP. It counts toward mining progress and existing animal milestones, but does not automatically submit objectives, claim rewards or advance phases.

Both upgrades pause without a living survival teammate within 16 blocks, without the chest, or while the required nearby chunks are unloaded. Collector transfers require the entire item stack to fit. The miner conservatively requires at least one completely empty output slot before sampling loot, even if existing stacks have spare room. Failed output plans leave inventories and progress unchanged. The existing generator reservation prevents a manual break and an automated break from both committing in the same tick.

Maintenance visits at most 32 team records per second, round-robin. Each collector considers at most eight item entities per visit. More teams can reduce cadence; unloaded or unattended islands do no work. Saved cooldowns do not accumulate offline harvests. Native hoppers may move chest contents, including the tool; a missing tool pauses the miner.

## Verification and remaining work

Common/client compilation and all 23 OneBlock/Ascension unit tests passed. The combined Ascension server group passed all 36 tests, including nine M5 feature tests. Four immediate entity-count fixtures were corrected to wait for destination entity tracking; their exact-count assertions remain intact. All nine legacy OneBlock regressions also passed: 45 distinct server checks total. Logs: `build/ascension-m5-unit.log`, `build/ascension-m5-regression.log`, and `build/ascension-m5-legacy.log`. `git diff --check` and new-file whitespace checks passed. These are targeted checks, not fullCheck or live multiplayer validation. Commands:

```powershell
.\gradlew.bat classes clientClasses --max-workers=1
.\gradlew.bat test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:one_block_game_test*' --max-workers=1
```

The automated checks do not establish live boss presentation, two-client latency behavior, encounter/cost balance, or crash-atomic saving across team data and chest chunks. Normal save/reload retains upgrade purchases, enabled state and cooldowns; interrupted encounters cancel. [M6 hardening](hardening.md) adds strict save loading, request guards, disk restore fixtures and a 25-island maintenance microbenchmark. External-process crash/restore and live-client acceptance remain open. No JAR, version bump, push or full release verification is included in this pass.
