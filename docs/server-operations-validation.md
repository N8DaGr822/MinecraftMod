# Isolated server recovery and timing validation

October 21, 2026. User approved validation in a local isolated test world first. No production world was restored or replaced, and no JAR was packaged. This is a development-world recovery drill, not certification of a public server or its complete modpack.

## Completed evidence

- Fresh isolated native world: `build/operations-drill-source-20261021/world`.
- Stopped-world snapshot: `build/operations-drill-snapshot-20261021`, **53 files, 7,855,088 bytes**, individually SHA-256 verified. The native session lock is deliberately excluded.
- Restored copy: `build/operations-drill-restored-20261021/world`. Every restored file matched before startup.
- A separate server process loaded the restored terrain and aircraft entity. Original owner UUID, five exact airframe blocks and health 23 survived. Log: `build/operations-restored-final.log`.
- The first restore check loaded block data but did not activate the remote entity chunk. Explicit fixture chunk activation fixed the test; this was not repaired by recreating the saved aircraft. The successful check reads the original snapshot's entity.
- Backup utility checks cover round-trip hashes, existing-destination refusal, corruption, nested/incomplete snapshots, unexpected files and active-lock refusal.

The initial run-directory attempt was overridden by Loom and used the existing development GameTest world. The final configuration sets Loom's run directory explicitly; the source and restored worlds above are isolated. The earlier default-world timing log is not the reported benchmark.

## Local timing sample

Final log: `build/operations-measured.log`; separate fresh runtime `build/operations-drill-measured-20261021`.

Twenty idle owned aircraft were loaded and each was asserted to receive at least 100 ticks. After 40 warmup ticks, 120 server ticks measured **1.749 ms mean, 3.612 ms p95, 4.367 ms maximum** between Fabric's start/end tick callbacks. Java 25, Minecraft 26.3, the repository dependency pins, Windows, and `ActiveProcessorCount=2`; **zero real clients**. The isolated fixture also retained one remote recovery sentinel outside the measured idle fleet.

These are observed server-tick callback durations for this small idle workload. The GameTest runner can run faster than real time, so they do not establish sustained 20 TPS under player load, network latency, render performance, flight/chunk-generation performance, or a supported player count. There is no before/after optimization claim. World generation and startup are excluded from the sample. Real server hardware, view/simulation distance, modpack and expected player count are still required for a capacity test.

## Reproduce safely

Use fresh names for each drill. Runtime directories are separate; compilation remains in the ordinary incremental `build` directory. The two stateful operations tests are inactive in ordinary test runs, including fullCheck. Their results require these explicit seed/restore commands.

```powershell
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:vehicle_operations_game_test_seed*' '-PgameTestRunDir=build/my-drill-source' '-PoperationsDrill=seed' --max-workers=1
python tools/server/world_backup.py snapshot build/my-drill-source/world build/my-drill-snapshot
python tools/server/world_backup.py verify build/my-drill-snapshot
python tools/server/world_backup.py restore build/my-drill-snapshot build/my-drill-restored/world
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:vehicle_operations_game_test_restored*' '-PgameTestRunDir=build/my-drill-restored' '-PoperationsDrill=restore' --max-workers=1
python tools/server/test_world_backup.py
```

Wait for a successful server shutdown before snapshotting. The utility obtains the native `session.lock` lock for the full copy, compares source and copy hashes, and writes the completion manifest last. Restore verifies the snapshot before creating a **new** destination. Existing destinations, nested trees, linked files/directories, incomplete manifests and checksum mismatches are refused. Failed partial copies are left for diagnosis and are never treated as valid snapshots. The tool does not stop servers, overwrite or delete worlds, schedule backups, rotate archives, or copy a mod JAR.

## Actual server checklist still pending

Archive the world while stopped, plus the exact server/client modpack version and hashes, external configuration, server properties, whitelist/ops/ban lists, datapacks and any external service/database state. The utility above covers the world tree only. Store a verified backup off the server's disk; this local drill does not test disk loss or off-site retrieval.

On a separate staging server, restore the same complete environment and verify player inventory/profiles, island teams and phases, Nexus return links, boss claims, ships/crew/cargo, fossils, regional cooldowns/rewards and deployed/stored/recovery vehicles. Then reconnect actual clients and exercise death/recovery. Record durations and choose retention, recovery-point and recovery-time objectives before public launch.

Seasons, leaderboards, reset scope, admission/payment, PvP and Hall of Fame remain the separate [seasonal-server design](seasonal-server-design-v0.1.md). No automatic world reset, paid access or leaderboard rollout is enabled by this drill.
