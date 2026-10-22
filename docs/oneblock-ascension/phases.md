# M4 MVP phases, submissions and rewards

Historical M4 record. The [October 20 second chapter](chapter-two.md) extends chapter teams to six phases, preserves original rewards and adds the inventory screen. Its current migration and catalog rules supersede the three-phase-only limits below.

Implemented on the existing Fabric stack. The user selected coexistence: existing Ascension teams retain the six legacy phases, counters, generator state and milestone rewards; newly created teams use the three MVP phases. No existing team is reset or automatically converted. Shared legacy OneBlock remains unchanged.

## Default progression

| Phase | Confirmed survival breaks | Held-item submissions | Encounter gate | Completion rewards |
| --- | --- | --- | --- | --- |
| Genesis | 250 | `timber`: 16 oak logs; `stone`: 32 cobblestone | None | Water bucket, lava bucket, 2 oak saplings, 8 wheat seeds, 8 apples |
| Caverns | 500 | `stone`: 64 cobblestone; `iron`: 16 iron ingots | Cave Guardian | 4 carrots, 4 potatoes, 4 sugar cane, 2 diamonds |
| Verdant Wilds | 600 | `wheat`: 32 wheat; `carrots`: 16 carrots | None | 8 emeralds, 4 diamonds |

These are initial balance defaults chosen for the reproduced brief; the missing external specification did not supply objective amounts or reward contents. The thresholds are specified by the brief. The [M5 Cave Guardian event](features.md) is now available through `/ascension guardian start` after Caverns submissions and breaks are complete. Mining continues indefinitely while the encounter gate is unmet. Only an eligible teammate defeating the active Guardian marks the encounter complete.

Commands:

- `/ascension objectives`: shows phase breaks, each submission, encounter status and unclaimed reward IDs.
- `/ascension submit <objective>`: consumes only the needed amount from the held stack. Partial submissions accumulate for the team. Wrong items, completed objectives and surplus items are preserved.
- `/ascension next`: completes only when the break threshold, every submission and every required encounter are satisfied. Resets phase breaks when entering the next phase; total breaks and the generated block are preserved. Completing Verdant leaves its mining pool available indefinitely.
- `/ascension claim <reward>`: delivers one earned reward stack into one empty inventory slot. The reward is shared by the team and can be claimed only once by any member. Full inventories preserve the unclaimed entitlement. Example: `/ascension claim genesis_water`.

Submission, completion and claims require a living noncreative, nonspectator member in their own claim, with no other container open, no carried cursor stack and no travel countdown. Existing stationary/safe checks also apply to `/ascension next`. Visitors cannot contribute or claim while standing on another team's island. The existing command travel and inventory boundaries are retained.

## Resources and existing behavior

The generator still uses the M3 confirmed-break transaction. New teams select from the active JSON phase pool and deterministic guarantees using the persisted generation number; regeneration/recovery never rerolls. Every phase guarantees dirt at generation modulo 20 = 0 and oak log at modulo 20 = 1. Genesis also guarantees stone at offset 2; Caverns guarantees iron ore at 2 and coal ore at 3; Verdant guarantees stone at 2 and iron ore at 3. Tools and native drops still determine what is harvested.

MVP teams no longer receive legacy supply chests every 100 breaks, which would bypass the new completion rewards. Existing bounded creature attempts every 50 survival breaks remain; all three MVP phase indices use the existing passive animal set. Existing teams retain the complete legacy supply/creature behavior. Creative removals regenerate without submission credit, progression or rewards.

## Data pack configuration and reload

Override these ordinary server data resources:

- `data/darkspawn/ascension/phases/genesis.json`
- `data/darkspawn/ascension/phases/caverns.json`
- `data/darkspawn/ascension/phases/verdant_wilds.json`

Each schema-1 phase defines its fixed ID, display name, break threshold, cycle length, weighted pool, fixed-offset guarantees, objectives, encounters and rewards. IDs/order are fixed to the three MVP phases; a later 12-phase extension requires a migration. Item IDs must be canonical registered identifiers, such as `minecraft:oak_log`. Objective/reward IDs use lowercase letters, digits and underscores; reward IDs are unique across the complete catalog. Reward amounts must fit one native stack. Pool entries have positive bounded weights, unique valid resources, and guarantees cannot collide. Every phase must guarantee dirt and logs. Unknown encounter IDs are rejected.

Resource numbers intentionally match the existing `OneBlockCore.RESOURCE` save/property indices:

| Index | Block | Index | Block |
| --- | --- | --- | --- |
| 0 | Dirt | 10 | Moss block |
| 1 | Oak log | 11 | Redstone ore |
| 2 | Grass block | 12 | Lapis ore |
| 3 | Stone | 13 | Gold ore |
| 4 | Coal ore | 14 | Diamond ore |
| 5 | Iron ore | 15 | Netherrack |
| 6 | Copper ore | 16 | Nether quartz ore |
| 7 | Sand | 17 | Soul sand |
| 8 | Gravel | 18 | Glowstone |
| 9 | Clay | 19 | End stone |

The existing lifecycle API loads the complete catalog at server start and after a successful `/reload`. All three candidates are read and validated before a single replacement. Invalid JSON, missing files or invalid fields retain the previous accepted catalog and log a rejection. `/ascension objectives` reports the rejected reload. The accepted catalog is also SavedData in the Ascension dimension, so restarting does not discard its last accepted balance.

Before any MVP team exists, a valid custom objective/reward layout may replace the defaults. Once an MVP team exists (including an archived one), objective IDs and item identities, encounter gates and reward IDs cannot change through hot reload. Such changes require an explicit migration. Thresholds, objective amounts, weighted resources/guarantees, display names and future reward contents may be rebalanced. Already earned rewards retain their saved item/count snapshot; changed rewards never reinterpret existing entitlements. Previously completed phases remain completed. Phase JSON does not expose island geometry; fixed saved geometry compatibility rules remain in force.

## Storage and implementation

- `AscensionPhases`: immutable validated definitions, bundled defaults, resource loading, accepted-catalog SavedData, atomic replacement and resource selection.
- `AscensionPhaseProgress`: phase ID, phase-qualified submission credit, encounter completion, completed phases and durable reward snapshots/claim flags. Completion and claims reject duplicates.
- `AscensionObjectives`: existing-style server commands, held-item consumption, own-claim authority, empty-slot delivery and progress text.
- `AscensionLedger`: schema 4 adds optional per-team objective state. Absence denotes a preserved legacy team; new allocation creates MVP state. Membership edits preserve that state; archival keeps the history. Unsupported schemas and invalid decoded phase states fail closed.
- `Ascension`, `AscensionGenerator`, and `OneBlock`: register commands/catalog lifecycle, route new-team phase transitions and resource selection, and retain the existing bounded creature method without legacy chest rewards for MVP teams.
- Unit/server checks and three JSON resources provide defaults and exercise migration, validation, inventory transactions and native mining.

Live reward completion records an entitlement before it can be claimed; claim records the ID before inventory delivery. As with M3, this does not turn player inventory files and world SavedData into one atomic disk transaction. Abrupt power-loss/crash injection and backup/restore checks remain M6. The catalog rejects malformed content but does not prove every custom objective is obtainable through arbitrary custom pools/crafting changes; administrators must playtest their balance.

## Verification

Verified October 19, 2026: common/client compilation, all 20 OneBlock unit tests, and 36 distinct server tests passed (foundation 8, bridges 7, generator 6, MVP phases 6, legacy OneBlock 9). The initial 26-test Ascension group passed; the final six phase tests also passed after adding fresh-world customization and exercising the registered submission command. A resource-manager type mismatch in the test fixture was corrected before successful server runs. `git diff --check` and new-file whitespace checks passed. These are targeted checks, not fullCheck or live multiplayer/crash testing. Commands:

```powershell
.\gradlew.bat classes clientClasses test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:one_block_game_test*' --max-workers=1
```

No JAR, version change, push or full-suite release verification is included. M5 is documented separately in [Guardian and automation](features.md), including its verification and remaining M6 work.
