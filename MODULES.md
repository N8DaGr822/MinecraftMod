# Darkspawn modules (end of Stage 2)

Branch: `split-modular`. Gradle multi-project; every module lives in `modules/<name>` and applies the `darkspawn.fabric-module` convention plugin from `buildSrc/`.

Common to every module:
- Content namespace stays `darkspawn:` (ids, lang, tags, recipes unchanged). Mod id is `darkspawn_<name>`; Java packages are `darkspawn.black.<name>..` (no split packages).
- Requires Minecraft 26.3, Java 25, Fabric Loader ≥ 0.19.5 and Fabric API. Only `legacy` needs GeckoLib.
- `darkspawn_core` is nested (Fabric jar-in-jar) in every feature jar, so a server installs one jar per feature. Fabric Loader keeps one copy of core however many modules are installed.
- Version: `2.0.0-alpha.1` (root `gradle.properties`, unchanged).
- Jar: `modules/<name>/build/libs/<mod id>-2.0.0-alpha.1.jar` (sources jar next to it).
- Build one jar: `bash ./gradlew :<name>:playBuild --max-workers=1`.
- Unit tests: `bash ./gradlew :<name>:test --max-workers=1`.
- GameTests (standalone: that module + core + Fabric API only): `bash ./gradlew :<name>:runGameTest -PgameTestRunDir=<isolated dir with server.properties port ≥ 25600> --max-workers=1`. Test ids are `darkspawn_<name>-gametest:<class>_<method>`.

## Modules

| Module (project) | Mod id | Purpose | Hard deps | Soft / integration | Tests |
|---|---|---|---|---|---|
| `core` | `darkspawn_core` | Library, no gameplay content. `DarkspawnCore` (`NAMESPACE`, `LOGGER`, `id()`), `ServerTuning` (shared `config/darkspawn-server.properties`; each mod passes its own defaults), `Sounds.register(modId)` (reads that mod's own `sounds.json` through its mod container), `PlayerSaves.save` (`PlayerList.save` invoker mixin), `Payments.pay`, `SafeArrival.position`, `DragonGate.defeated`, `Progress.award` (+ `progress/root.json`), client `UiTheme`. Hooks in `core.hook`: `TravelBoundary`, `EquipmentChanged`, `ClimateProtection`, `WardExemptions` (+ tag `#darkspawn:ward_exempt`), `JournalSections`. Also ships the shared item palette textures `assets/darkspawn/textures/item/materials/*` (18 PNGs used by ~70 models across modules). | Fabric API | — | 3 unit tests |
| `storage` | `darkspawn_storage` | Dump, Sorting and Bank chests, Sorting Tube, storage menu/screen, bank contents, routing, `ContainerSorter`. | core | none | 9 unit tests, 11 GameTests |
| `bonfire` | `darkspawn_bonfire` | Barracks bonfire ward, ward lights, upgrades, HUD, movement/spawn mixins. Asks `WardExemptions` which hostiles to leave alone (Wither and Ender Dragon are still exempt in its own code). | core | Bosses, Taiga wolves and pirate crew register exemptions from their side (today in `legacy`) | 9 GameTests |
| `quiver` | `darkspawn_quiver` | Quiver item, bow-feeding mixins, own shulker anti-nesting mixin and bundle-scroll client mixin. | core | none | 6 GameTests |
| `ingredient_pouch` | `darkspawn_ingredient_pouch` | Ingredient Pouch, tag `#darkspawn:pouch_ingredients` (Darkspawn crop/husbandry entries are `required:false`), own anti-nesting and scroll mixins. | core | farming/husbandry items via optional tag entries | 4 GameTests |
| `explorer_pack` | `darkspawn_explorer_pack` | Explorer Pack item, menu and screen, tag `#darkspawn:explorer_pack_contents` (`landmark_compass`, `logbound_compass` are `required:false`), own anti-nesting mixin. | core | landmarks, pirate compasses via optional tag entries | 5 GameTests |
| `enchantments` | `darkspawn_enchantments` | Hunter, Butcher, Forager, Soulward (`ThematicEnchantments`, the `thematic_food_bonus` loot condition, enchantment JSON, `hunter_targets` and `enchantable/forager` tags, its entries in the vanilla enchantment tags). | core | none (targets are vanilla mobs) | 5 GameTests |
| `legacy` | `darkspawn` | The rest of the original mod, being drained. Jar keeps the old name `darkspawn-2.0.0-alpha.1.jar` (`modules/legacy/build/libs/`) and nests core plus all six modules above, so it still deploys as one jar. | core, storage, bonfire, quiver, ingredient_pouch, explorer_pack, enchantments, GeckoLib | — | 114 unit tests, full GameTest suite (~455) |

## Still in `modules/legacy`

Everything not listed above: bosses and health/hearts, ecosystem (wildlife, hunts, regional events, world awakening/victory), cooking, farming, husbandry, fishing, culinary brewing, equipment (armor styles, legendary armor, expedition gear, temperature/climate including the Warmth/Cooling enchantments), mounts and vehicles, Legacy of Noah, endgame Nexus and relics, worldgen/landmarks, pirate seas, forsaken, OneBlock/Ascension, journal, testing camp and Dragon Starter Chest, audio/creature framework, data generation.

Legacy now calls core for: `Darkspawn.id/LOGGER/MOD_ID` (thin delegates), `ServerTuning` loading, sound registration, player save, payments (`Ships.pay` removed; callers use `Payments.pay`), safe arrival (`Pirate.safePosition`/`Forsaken.safePosition` delegate), dragon gate (Forsaken, WolfHunt, RegionalEncounter), `Culinary.award` → `Progress.award`. Hook registrations living in legacy: OneBlock registers the travel boundary (used by WolfHunt, RegionalEncounter, MountCompanions, NoahLegacy); OneBlock/Ascension fire `EquipmentChanged` and `Darkspawn` registers the armor-style/legendary refresh listener; Cooking registers the WARMING meal as a climate provider; Pirate registers ward exemptions and its journal section (sea legend, factions, Seven Seals, island notes); TaigaEcosystem and BossEntities register ward exemptions.

Cross-mod integration GameTests that need legacy content stay in legacy: `PirateBonfireWardGameTest`, `MountBonfireWardGameTest`, `BossHunterEnchantmentGameTest`, and `oneblock.{Quiver,IngredientPouch,ExplorerPack}ProfileGameTest` (OneBlock profile codec round-trips, mod-item tag membership, cross-container nesting).

## Working notes

- Module dependencies use the `modJar` outgoing configuration: `implementation module('core')` plus `include module('core')`. Consumers compile and run against the finished jar (main + client classes), the same shape as production.
- Root task names run in every module (`bash ./gradlew test`, `playBuild`, `fullCheck`). Pass `-PgameTestRunDir` only to a single module's `runGameTest`.
- Several jars now ship `assets/darkspawn/lang/en_us.json` and tag files. Unit tests that read those from the classpath must use `MergedResources.open` (legacy and storage have one).
- Repo-level `tools/` scripts still point at `src/...` paths that moved to `modules/legacy/src/...`. Only `verify-cuisine-art.py` (used by `fullCheck`) is updated.
