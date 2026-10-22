# M6 persistence and request hardening

This pass adds strict Ascension save loading, duplicate/unknown request rejection, real SavedData disk/backup fixtures, and a 25-attended-island maintenance measurement. It does not claim whole-world crash atomicity, a completed external-process restore drill, or real-client latency acceptance.

## Save loading

Minecraft's default SavedData loader accepts partial codec results and treats read/parse failures as missing data. `computeIfAbsent` may then create and later write a new default record. Losing even part of an Ascension team or bridge ownership list is unsafe: it can lose earned progression or reuse/protect the wrong allocation.

`AscensionSavedDataMixin` intercepts loading only for `darkspawn:ascension`, `darkspawn:ascension_bridges`, and `darkspawn:ascension_phases`. It accepts compressed and uncompressed native NBT, requires a complete codec result, and leaves custom schema migration to the existing codecs. These custom types do not use a vanilla data-fixer. A genuinely absent file still initializes normally. Other SavedData types keep their existing behavior.

Unreadable, truncated or partially decoded files now throw an explicit error with the affected path. The load fails before a default object can be cached or saved. The original bytes remain untouched. This intentionally prevents server startup or the affected operation until the save is repaired/restored. Decodable but unsupported schemas/geometry retain the existing compatibility lock and are not silently converted.

This prevents silent replacement; it does not make native asynchronous writes atomic or reconstruct deleted files. Disk write failures still need operational monitoring. Do not delete a damaged ledger to get past the guard: that can reset allocation while existing island chunks remain.

## Travel requests

The existing Fabric receiver continues to handle the two legacy inventory-button actions on the server. Unknown action integers and requests received during an active travel countdown now return before safety/entity scans. They cannot change the destination, restart the countdown, or advance the legacy phase during travel. Repeated `/ascension home`/return requests also leave the active countdown alone without emitting repeated unsafe messages. No payload format or client protocol changed; MVP advancement remains subject to server-side objectives and encounter gates.

This is bounded handling of invalid/duplicate requests, not a new general packet rate limiter. The normal server networking and command permission rules still apply.

## Disk and workload checks

The hardening GameTests use separate temporary fixture directories under the test working directory. They do not corrupt or restore a playable world. They close and reopen real `SavedDataStorage`, verify teams/progress, claimed rewards, upgrades/cooldowns, bridge ownership and phase definitions, mutate the saved state, then restore byte-for-byte copies of all three fixture files. Separate cases verify truncated and partially decodable files are rejected without byte changes, and that all three stores still accept uncompressed NBT.

The workload fixture creates 25 distinct attended islands with collector/miner upgrades, one eligible player each, loaded output chests, and eight dropped items per island. It executes 20 complete round-robin maintenance rotations and logs median/p95/maximum elapsed time. The first rotation collects 200 items and harvests 25 cores; same-tick repeats must not mine again. Historic inactive team records remain in the measured rotation. This is a maintenance microbenchmark, including an initial active burst and subsequent cooldown work, not a sustained TPS or networking benchmark. It does not load chunks inside maintenance; fixture setup loads the attended islands first.

Verified October 19, 2026: common/client compilation, all 23 OneBlock unit tests, and all 41 combined Ascension server tests passed. The five new hardening tests also passed as an isolated group. Combined-run maintenance results: 25 active islands, 336 total team records, 11 batches per full rotation, 20 rotations; median 0.5953 ms, p95 0.7518 ms, maximum 15.9855 ms. The isolated run measured 0.5104 / 1.1646 / 30.6603 ms respectively with 268 records; these are local diagnostic measurements, not performance guarantees. Logs: `build/ascension-m6-hardening.log` and `build/ascension-m6-regression.log`. All nine legacy OneBlock server regressions also passed (`build/ascension-m6-legacy.log`): 50 distinct server checks total. `git diff --check` and new-file whitespace checks passed. These are targeted checks, not fullCheck or release certification.

```powershell
.\gradlew.bat classes clientClasses --max-workers=1
.\gradlew.bat test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:one_block_game_test*' --max-workers=1
```

## Full-world recovery procedure and remaining acceptance

1. Stop the server normally and verify shutdown/save completion. Keep an untouched copy of the entire world directory, including `level.dat`, all dimension folders, player data and all mod SavedData. Record the mod/datapack versions alongside it.
2. Restore into a separate test location with the same mod/datapack versions. Never mix a newer player/chest save with an older ledger: those files are not one disk transaction.
3. Start the restored copy and verify team membership/cells, both inventory boundaries and Ender Chests, generator resource/counters, claimed rewards, upgrades/chest contents, bridge permissions and archived cleanup. Interrupted Guardians should cancel without victory.
4. Test two clients reconnecting and mining near a save/restart boundary. Check duplicate travel clicks, loss of the last nearby teammate, full automation output, interrupted encounters and restored reward claims.
5. Run a sustained 25-client/island workload and capture server tick timing and network behavior before calling M6 acceptance complete. An external-process abrupt-stop/restore drill remains outstanding; the fixture checks above are not a substitute.

No JAR, version bump, push or fullCheck/release certification is included in this pass.
