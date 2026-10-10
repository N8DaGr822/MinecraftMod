# Creature asset tools

Each completed creature has an editable `art/blockbench/<id>/<PascalCase>.bbmodel` project with an embedded PNG, plus matching geometry, animation JSON, and texture under `src/main/resources/assets/darkspawn/`. Open the project using Blockbench's GeckoLib plugin. Preserve bone names and animation names used by the controllers.

- `node tools/assets/catalog.cjs` refreshes the full registered-creature inventory; catalog presence does not imply completed artwork.
- `node tools/assets/serve-preview.cjs` serves the local preview at `http://127.0.0.1:8766/`. Choose a creature and animation, scrub time, and compare against the two-block marker. Unfinished projects display an error.
- `node tools/assets/verify.cjs fossil_tyrant` checks project/resource coverage, embedded texture parity, bones, dimensions, UV bounds, and finite keyframes. Without IDs it requires every new roster asset to exist.
- `node tools/assets/rig.cjs art/blockbench/fossil_tyrant/FossilTyrant.bbmodel` exports numeric Box UV or per-face UV projects after edits. For Molang expressions, export directly from Blockbench instead.

The authoring scripts create initial rigs. Do not rerun one over a hand-edited project unless you intend to replace those edits. `CreatureAssets` enables finished rigs individually. The Java asset tests bake every enabled boss, mob, and minion with GeckoLib, check every boss phase/variant attack and every mob controller clip, and verify Void Sentinel shield alignment. Texture prompts record the built-in image-generation inputs; related mobs share their biome's material atlas.

The browser preview checks source geometry and numeric animation tracks. It does not reproduce Minecraft lighting, collision, controller transitions, or multiplayer timing. Those still require an in-game playtest.

Mob projects use shared runtime atlases; their embedded texture name identifies the shared PNG. Exporting an edited atlas changes every creature using that atlas. Keep embedded copies in related projects in sync, or assign a separate texture and renderer path for a species-specific repaint. Egg and soul-cage groups are hidden in the normal source preview and selected by synchronized minion role in Minecraft.

The complete first art pass covers 115 living entities: 16 bosses, 83 other mobs (including Endborn), and 16 summoned minions. Open `/tools/assets/gallery.html` on the preview server for a six-creature overview; select a name for animation controls. The Minion geometry selector previews normal, egg, and soul-cage shapes. Browser previews were visually reviewed; Minecraft client lighting, motion against terrain, collisions and two-client synchronization remain playtest checks.

Verification on 2026-10-08: full Gradle build passed with 58 unit tests and 69 headless server GameTests. All 115 projects and runtime model/animation resources are present; the roster contains 942 animation clips. Source/export checks passed for the 111 newly authored or extended projects; the four earlier bespoke rigs retain their dedicated Java asset tests. Each finished creature was committed and pushed separately.

## Mob animation refinement, 2026-10-09

The second animation pass covers all **83 non-boss mobs and 16 summoned minions**. The forest batch revises 19 clips; the remaining 95 creatures revise 662 clips, including Endborn's four bespoke clips. Existing names, rig geometry, UVs, textures, hitboxes, and gameplay rules are preserved. Boss animation refinement is a separate pass.

Locomotion now follows anatomy: diagonal quadruped steps, articulated crawler legs, wing strokes with delayed tips, swimming tails/fins, serpent coils, and floating spirits. Combat poses follow existing warning/release events. Active-state clips include the special-attack release because the state controller overrides triggered attacks. Hiding folds the existing rig; death poses fit the one-second controller window. The later batch settles the transformed body onto the preview's floor plane. Egg and soul-cage forms have their own idle/death tracks; the renderer still chooses which form is visible. No new AI states or howl events were added.

- `node tools/assets/mob-animation-pass.cjs` is a dry run; add creature IDs to restrict the batch, and `--write` only when intentionally replacing those clips. It reads the existing projects and exports animations only. It excludes the four earlier forest edits and all bosses.
- `node tools/assets/forest-animation-pass.cjs` is the separate forest pass, also a dry run unless `--write` is supplied. Do not rerun either authoring script over later hand-edited animations without reviewing the replacement.
- `node tools/assets/verify-animation-pass.cjs` checks all 99 projects and 690 existing clips for source/export parity, finite keyframes, idle/walk loop seams, controller names, one-second death duration, and neutral root/shield transforms. The normal `verify.cjs` remains the geometry/UV/texture checker.
- The gallery now has **Animation**, **Pose**, and **Play** controls for comparing six creatures. Missing clips fall back to idle. Bone/channel sampling is cached within each rendered frame.

Visual review covered every creature's warning pose (Endborn's blink instead) and representative locomotion, death, egg, and cage poses in the source preview. The existing `BossRosterTest.completedMobAssetsBakeAndSupportEveryControllerState` and `BossRosterTest.endbornAssetsBakeWithGeckoLibAndAllAnimatedBonesExist` are the targeted GeckoLib checks. In-game terrain contact, lighting, attack timing, and multiplayer transitions remain playtest checks; source previews and headless baking do not establish those results.

Final verification: both targeted GeckoLib tests passed after the death-pose correction (2 tests, no failures/errors); the full 111-project asset verifier passed; animation-contract checks passed for 99 projects, 690 clips, and 58,940 keyframes. This pass does not claim a full gameplay test run or an in-game playtest.

## Boss animation polish, 2026-10-09

Polished 234 existing clips across all 16 bosses. Source key poses and attack durations remain intact; sampled easing smooths the transitions in both Blockbench and GeckoLib. Delayed tail, wing-tip, tentacle-joint, branch, ear, and forearm motion adds follow-through. Generated crawler locomotion uses alternating leg groups, serpents undulate, Thunder Bird uses wing strokes, and Mycelial Sovereign's root limbs flex subtly. Void Eye's shield frame stays neutral so its renderer controls shield alignment.

`node tools/assets/boss-animation-pass.cjs` previews the batch; append `--write` to save, optionally followed by boss IDs. The script changes animations only and marks processed clips with `darkspawn_polish_version` so rerunning does not accumulate changes. It preserves later hand edits to marked clips. Authored awakening, defeat, and death sequences are retained, along with all geometry, textures, clip names, UUIDs, loop modes, and durations. The gallery adds wrath, phase-change, first-boss-warning, and matching attack previews; open an individual boss to select every attack.

Verification passed: all four existing boss GeckoLib asset tests (Tree Spirit, Mutant Wolf, Mutant Zombie, and regional roster); the 111-project asset verifier; source/export parity across all 252 boss clips and 98,918 keyframes; and matching endpoints for polished loops. A second dry run found zero clips requiring changes. Source preview review covered warning poses across all 16 bosses, phase poses and representative movement. Minecraft terrain contact, lighting, damage synchronization, and controller blending still require an in-game playtest.

## Shadow Creeper Queen redesign, 2026-10-09

The queen now follows the Alien Queen inspiration recorded in `docs/boss-ecosystem-notes.txt`: an upright ribbed torso, two hind legs, two large clawed arms and two smaller grasping arms, an elongated skull under a broad swept crown, dorsal tubes, an extending inner jaw, and an eight-joint bladed tail. The former six-legged spider anatomy is replaced. The existing sculk atlas and 18-block height are retained. All 18 controller clips are reauthored for the new 29-bone rig, including bipedal locomotion, acid-jaw extension, tail sweep, brood/phase displays, and a floor-settled collapse.

`node tools/assets/shadow-queen.cjs` is the dry-run authoring command; `--write` replaces only the queen project and its geometry/animation exports. The generic generator protects this bespoke rig. Other bosses and the queen ecosystem/minion models are outside this redesign. Review the individual queen preview for animation selection and side views.

Redesign verification: queen geometry/UV/embedded-texture checks passed, and the regional boss GeckoLib test passed with the new anatomy assertions after the floor correction. Source preview checks covered idle silhouette, acid, sweep, and final death pose. In-game collision and blending remain playtest checks.

## Shadow hive castes, 2026-10-09

The seven shadow ecosystem mobs and the summoned queen minion now share the redesigned queen’s Alien-inspired anatomy. Drone, stalker, spitter, guardian, praetorian, and queen minion use two hind legs, jointed claws, elongated skulls, ribbed torsos, and eight-joint tails. Royal castes retain the crown and secondary arms; guardians add armor and spitters add acid glands. Larvae are segmented hatchlings with vestigial claws; eggs have four opening lips. All 56 clips are adapted to these rigs. Summoned egg/cage role groups and their controller tracks are preserved. Death poses settle onto the source floor plane.

Use `node tools/assets/shadow-hive.cjs` for a dry run or append `--write` to rebuild these eight projects and their geometry/animation exports. The generic model generator and mob animation pass exclude these bespoke hive assets. The gallery includes two Shadow hive collections. Registered sizes, textures, AI, attacks, and loot are unchanged.

Hive verification: all eight geometry/texture checks passed; animation parity and controller checks passed across 99 non-boss projects; the existing GeckoLib mob/minion asset test passed. Source previews covered all eight castes, warnings, representative walking and final death poses. In-game terrain and combat blending remain playtest checks.
