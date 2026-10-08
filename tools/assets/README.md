# Creature asset tools

Each completed creature has an editable `art/blockbench/<id>/<PascalCase>.bbmodel` project with an embedded PNG, plus matching geometry, animation JSON, and texture under `src/main/resources/assets/darkspawn/`. Open the project using Blockbench's GeckoLib plugin. Preserve bone names and animation names used by the controllers.

- `node tools/assets/catalog.cjs` refreshes the full registered-creature inventory; catalog presence does not imply completed artwork.
- `node tools/assets/serve-preview.cjs` serves the local preview at `http://127.0.0.1:8766/`. Choose a creature and animation, scrub time, and compare against the two-block marker. Unfinished projects display an error.
- `node tools/assets/verify.cjs fossil_tyrant` checks project/resource coverage, embedded texture parity, bones, dimensions, UV bounds, and finite keyframes. Without IDs it requires every new roster asset to exist.
- `node tools/assets/rig.cjs art/blockbench/fossil_tyrant/FossilTyrant.bbmodel` exports numeric Box UV projects after edits. For Molang expressions or per-face UVs, export directly from Blockbench instead.

The authoring scripts create initial rigs. Do not rerun one over a hand-edited project unless you intend to replace those edits. `CreatureAssets` enables finished rigs individually. The Java asset test bakes enabled boss resources with GeckoLib and checks every phase/variant attack. Texture prompts record the built-in image-generation inputs; related mobs share their biome's material atlas.

The browser preview checks source geometry and numeric animation tracks. It does not reproduce Minecraft lighting, collision, controller transitions, or multiplayer timing. Those still require an in-game playtest.
