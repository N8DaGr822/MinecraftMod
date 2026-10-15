# Build for play, test by change, verify before pushing

Use Java 25. Run these commands from the repository root. Keep the default build directory so Gradle reuses compiled output; do not use `clean` or create a fresh checkpoint directory for each edit.

## Develop without creating a JAR

Do not package a JAR or change the version until the user explicitly requests a build/release. Compile with `.\gradlew.bat classes clientClasses --max-workers=1` and run the relevant checks below. Full verification also runs without packaging.

## Package only when requested

```powershell
.\gradlew.bat playBuild --max-workers=1
```

This explicitly packages the mod without unit tests or a headless test server. Keep one current playable JAR directly in `build/libs`, move source JARs into `build/libs/sources`, and preserve older builds under labeled `build/libs/archive` subfolders. Record the build label, contents and SHA-256 in `build/libs/README.md`; the shared release version alone does not identify a development build. Change release versions only with user approval. A successful play build is not a full test pass.

## Check only what changed

| Change | Check |
| --- | --- |
| Cuisine models or textures | `.\gradlew.bat checkCuisineArt` and visual review |
| Mount code/models | `.\gradlew.bat test --tests 'darkspawn.black.mount.*' --max-workers=1` |
| Companion lifecycle server checks | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:mount_care_game_test*' --max-workers=1` |
| All mount server regressions | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:*mount*' --max-workers=1` |
| Mount source/texture parity | `node tools/assets/verify-mounts.cjs` and `node tools/assets/griffin-model.cjs --check`, followed by visual review |
| Cooking logic | `.\gradlew.bat test --tests 'darkspawn.black.cooking.*' --max-workers=1` |
| OneBlock logic | `.\gradlew.bat test --tests 'darkspawn.black.oneblock.*' --max-workers=1` |
| One specific unit test | `.\gradlew.bat test --tests 'darkspawn.black.equipment.TemperatureTest' --max-workers=1` |
| Pirate templates, loot references and shanty integrity | `python tools/assets/verify-pirate-assets.py`, then `python tools/assets/preview-pirate.py` and `python tools/assets/preview-naval.py` (previews require Pillow); inspect the offline previews |

For server behavior, select a registered GameTest ID with the Fabric filter:

```powershell
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:expedition_armor_game_test_temperature_samples_biome_shelter_wetness_and_campfires' --max-workers=1
```

Use IDs from the GameTest output, not Java class names. Check the output's selected-test count and results; a mistyped filter is not validation. Omit the property to run every server test. Broaden checks when changes affect shared systems such as inventory, saving or networking. Playtest controls and appearance in Minecraft after the JAR is ready.

## Before pushing

```powershell
.\gradlew.bat fullCheck --rerun-tasks --max-workers=1
```

This runs all unit tests, all server GameTests and the cuisine asset validator without packaging a JAR. `--rerun-tasks` ensures a previous targeted test run cannot stand in for full verification. Do not pass test filters or exclusions. The task rejects `-PgameTestFilter`; unit and server tests run sequentially. Address failures before pushing, and report any remaining manual playtesting gaps. CI runs `fullCheck` on every push and pull request without producing release artifacts.

`build` and `playBuild` both package JARs: do not run either unless packaging was requested. There is no installed Git hook: run the pre-push command explicitly. Other art-specific validators remain available under `tools/assets` for their corresponding changes.
