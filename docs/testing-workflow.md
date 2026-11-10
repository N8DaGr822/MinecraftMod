# Build for play, test by change, verify before pushing

Use Java 25. Run these commands from the repository root. Keep the default build directory so Gradle reuses compiled output; do not use `clean` or create a fresh checkpoint directory for each edit.

## Modules (split in progress)

The build is a Gradle multi-project. Each mod lives in `modules/<name>` (`core`, `storage`, `bonfire`, `quiver`, `ingredient_pouch`, `explorer_pack`, `enchantments`) and `modules/legacy` holds the rest of the original mod (mod id `darkspawn`). Task names without a project path run in every module (`test`, `playBuild`, `fullCheck`); prefer one module at a time, for example `:storage:test` or `:legacy:runGameTest '-PgameTestFilter=darkspawn-gametest:wolf_hunt*'`. Each module's GameTest ids are prefixed with its own test mod id (`darkspawn_storage-gametest:...`). Jars are written to `modules/<name>/build/libs`; the legacy jar keeps the `darkspawn-<version>.jar` name and nests every extracted module. Classpath tests that read files Minecraft merges (lang, `sounds.json`, tags) must use `MergedResources` because several jars now ship them.

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
| Ground walker | `runGameTest -PgameTestFilter=darkspawn-gametest:ground_walker_game_test*`; mount unit tests, resource coverage, `python tools/assets/preview-ground-walker.py`, shared mount-care, inventory-hub and camp regressions. [Scope and live acceptance](mech-prototype-design.md). |
| Aircraft assembly/flight | `runGameTest '-PgameTestFilter=darkspawn-gametest:aircraft_game_test*'`; shared `mount_care_game_test*`, inventory-hub and camp checks after integration changes. See [aircraft](aircraft-prototype.md). |
| Regional encounters | Focused `fungal_bloom_game_test*`, `shadow_incursion_game_test*`, `soul_storm_game_test*`, `end_rift_game_test*`; shared `regional_game_test*`, ledger unit tests and camp guidance. |
| Isolated recovery/timing drill | Explicit `gameTestRunDir` and `operationsDrill` properties; see [procedure](server-operations-validation.md). These stateful drill cases are inactive without the explicit seed/restore flag and are not established by ordinary fullCheck. |
| Companion lifecycle server checks | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:mount_care_game_test*' --max-workers=1` |
| All mount server regressions | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:*mount*' --max-workers=1` |
| Mount source/texture parity | `node tools/assets/verify-mounts.cjs` and `node tools/assets/griffin-model.cjs --check`, followed by visual review |
| Cooking logic | `.\gradlew.bat test --tests 'darkspawn.black.cooking.*' --max-workers=1` |
| Registered item/block visual definitions and names | `.\gradlew.bat test --tests 'darkspawn.black.ResourceCoverageTest' --max-workers=1` |
| Thematic enchantments | `:enchantments:runGameTest; :legacy:runGameTest '-PgameTestFilter=darkspawn-gametest:boss_hunter_enchantment_game_test*'`; `testing_camp_game_test*` for supplied rank II trials. Compile common/client and run `ResourceCoverageTest`. Live enchanting/trades and balance remain acceptance checks. |
| Fossil display and ocean clues | `runGameTest '-PgameTestFilter=darkspawn-gametest:exploration_finds_game_test*'`; also `boss_structure_game_test_regional_archaeology*`, `husbandry_fishing_game_test_fish*` and `testing_camp_game_test*` for shared loot/fishing/camp changes. Run `ResourceCoverageTest`, `verify-resource-links.py` and `preview-fossil-display.py`; live fishing and visual acceptance remain. |
| NPC patrol boarding | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ships_game_test_boarding*' --max-workers=1`; run `ships_game_test*` for shared ship damage/tick/menu changes and `testing_camp_game_test*` for guide changes |
| Docked-worker unloading | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ships_game_test_unloading*' --max-workers=1`; run `ships_game_test*` for shared crew/menu/launch changes and `testing_camp_game_test*` for guide changes |
| Explorer's Pack storage and ownership | `.\gradlew.bat :explorer_pack:runGameTest --max-workers=1` plus legacy `explorer_pack_profile_game_test*`; also run `testing_camp_game_test*` after supply changes |
| Quiver storage and automatic bow feeding | `.\gradlew.bat :quiver:runGameTest --max-workers=1` plus legacy `quiver_profile_game_test*`; also check `ingredient_pouch_game_test*` for shared bundle hooks, `testing_camp_game_test*` for supplies, and `endgame_game_test_legendary_bow_and_lantern_use_native_costs` for the existing custom bow |
| Custom item models, bow/shield poses and compass frames | `python tools/assets/verify-distinct-items.py`, then inspect `build/distinct-items-preview.png`; author with `distinct-item-models.py` after older feature generators |
| Broad asset references, PNG readability and sound subtitles | `python tools/assets/verify-resource-links.py` (requires Pillow and the compiled native asset cache); use the feature-specific validators for geometry/animation rules |
| OneBlock logic | `.\gradlew.bat test --tests 'darkspawn.black.oneblock.*' --max-workers=1` |
| Ascension teams, bridges, generator recovery, MVP phases, Guardian, automation and hardening | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension*' --max-workers=1`; also run `one_block_game_test*` when changing shared travel/harvesting, and storage/Forsaken checks for native protection hooks |
| Ascension chapter UI, new encounters, relay/drive and membership recovery | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension_chapter_game_test*' --max-workers=1`; run the complete Ascension group for catalog, save or team changes. Live GUI and multiplayer checks remain necessary. |
| One specific unit test | `.\gradlew.bat test --tests 'darkspawn.black.equipment.TemperatureTest' --max-workers=1` |
| Awakened boss patterns | Boss unit tests plus `runGameTest '-PgameTestFilter=darkspawn-gametest:awakened_pattern_game_test*'`; run `boss_encounter_game_test*` for shared AI regressions. Live multiplayer and dodge readability remain necessary. |
| Wolf Hunt | `test --tests 'darkspawn.black.ecosystem.WolfHuntLedgerTest'` and `runGameTest '-PgameTestFilter=darkspawn-gametest:wolf_hunt_game_test*'`; run `taiga_game_test*` for shared wolf behavior and starter-camp tests for guide changes. Live cooperative balance and reconnect acceptance remain necessary. |
| Fish and animal server behavior | `.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:husbandry_fishing_game_test*' --max-workers=1` |
| Ingredient Pouch | `.\gradlew.bat :ingredient_pouch:runGameTest --max-workers=1` plus legacy `ingredient_pouch_profile_game_test*`, inventory unit checks and `ResourceCoverageTest`; starter-camp checks when changing supplied items. Live bundle controls and reconnect/travel acceptance remain necessary. |
| Field utility icons, fish skins and recipe references | `python tools/assets/verify-field-utilities.py`; regenerate with `python tools/assets/field-utilities.py` and inspect `build/field-utilities-preview.png` |
| Pirate templates, loot references and shanty integrity | `python tools/assets/verify-pirate-assets.py`, then `python tools/assets/preview-pirate.py` and `python tools/assets/preview-naval.py` (previews require Pillow); inspect the offline previews |
| Pirate hinterland terrain, mine access and resources | `python tools/assets/verify-pirate-terrain.py` checks bounds, ore budgets, dry cave walls and walkable mine access; inspect `build/pirate-terrain-preview.png`. Regenerate only terrain with `node tools/assets/pirate-foundation.cjs --terrain-only`. |
| Pirate biome settlements | `python tools/assets/verify-pirate-biomes.py`; inspect `build/pirate-biomes-preview.png`. Regenerate only the six added types with `node tools/assets/pirate-foundation.cjs --biomes-only`. Server checks: `-PgameTestFilter=darkspawn-gametest:pirate_game_test_biome*`. |

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

### Endgame expansion checks

- World awakening, companions, legendary relics and cooperative Nexus: `./gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:endgame_game_test*' --max-workers=1`. Nexus coverage includes room-upgrade obstruction safety, preparation/reconnect persistence, timeout cleanup and once-only victory rewards. Run `testing_camp_game_test*` for its guide changes; live room navigation and two-client UI checks remain necessary.
- Native culinary brewing: `./gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:expansion_brewing_game_test*' --max-workers=1`.
- Ship classes and ammunition: existing `darkspawn-gametest:ships_game_test*` group.
- Noah's bonded gift, armor, Tempest and Thalassar: `./gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:noah_game_test*' --max-workers=1`; artwork references and source preview: `python tools/assets/verify-noah-legacy.py`. See `docs/legacy-of-noah.md` for live acceptance and gift instructions.
- Ship movement reliability: the same ship group includes sustained translation/turning, crouch edge safety, jump release, walker docking and swimmer rejection, standing-height boarding clearance, rotated ladders and large-deck menu access. Server tests do not replace pilot/observer multiplayer checks for interpolation, reconnects and latency.
- Data/native visual references: `python tools/assets/verify-endgame-expansion.py`. This is separate from the cuisine artwork validator; it checks reused native models rather than newly authored textures.
