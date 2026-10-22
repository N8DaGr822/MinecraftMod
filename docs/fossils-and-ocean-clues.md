# Fossil reconstruction and Kraken clues

Implemented in source on October 21, 2026. This is the approved decorative fossil display plus a separate optional route to the existing Kraken summon. No new boss, heart identity, release version or JAR is added.

## Fossil reconstruction

Brush suspicious gravel at Fossil Altars for **Tyrant Fossil Fragments**. Each deposit remains single-use and yields one weighted find; fragments are not guaranteed. The existing archaeology entries remain, including the conditional post-victory diamond entry.

1. Craft **four fragments + one bone block** (shapeless) into the existing **Ancient Tyrant Fossil**.
2. Craft the display in two rows: `BFB / SSS`, where `B` is a bone block, `F` is the fossil and `S` is a smooth stone slab.
3. Place the **Reconstructed Tyrant Fossil** as a compact skull-and-rib display on its stone plinth. Breaking it returns one display; it does not refund the crafting ingredients.

The display follows the existing trophy-block placement/collision pattern, but is not an earned boss trophy. It grants no victory, heart, buff or boss unlock. Its model uses native bone, stone and dark eye textures. It has a fixed orientation, like the existing trophy blocks.

Recovery does not require finding the same finite deposit again: naturally spawning Tyrant Skulls still yield the original fossil when used with a pickaxe. Their existing desert habitat, spawning settings and dragon gate remain. The original fossil-to-Tyrant-sigil recipe remains available, so decide whether to display or spend a fossil.

## Ocean clues

**Saltworn Ocean Notes** can be found by brushing Expedition Wreck deposits. They are also renewable: each successful catch containing a plain native fish has a **1-in-16 chance** to append one note when the hook reports open water in an **Overworld ocean biome**. This means eligible fish catches, not every cast. Closed-water farms, inland/custom-dimension fishing, empty catches, treasure-only/junk-only catches and customized fish do not trigger the addition. Native catch items, quantities and components are retained; existing regional fish substitutions still run afterward.

1. Craft **note + compass + ink sac + prismarine shard** into a **Decoded Tide Chart** (shapeless). All four ingredients are consumed.
2. Craft **chart + nautilus shell + dragon breath** into the existing **Kraken Sigil** (shapeless).
3. Use the normal sigil ritual. The world's dragon victory, non-Peaceful difficulty, Overworld deep-ocean habitat, open sky, sampled water depth, encounter spacing and spawn clearance are unchanged. The normal recipe and ecosystem recipe remain alternatives.

The chart is a readable ritual clue, not a map with coordinates and not a new summon implementation. Notes, fragments and charts have next-step tooltips; use them to read full instructions without consuming the item. Lost notes/charts can be replaced through further ocean fishing and crafting. There is no hidden personal quest state or once-only claim to lose on death/reconnect; finds are ordinary saved, shareable items.

## Testing Camp and acceptance

Fresh camps supply fragments, bone blocks and smooth stone slabs in **18 Charts archaeology rematches**. **20 Regional fishing** supplies notes and all materials for the chart and sigil chain. Guide pages explain both paths. Existing camps are not modified.

Targeted server checks: `exploration_finds_game_test*`; recipe results, original summons, real archaeology rolls, catch conservation/eligibility, renewable notes and display recovery. Regressions: `boss_structure_game_test_regional_archaeology*`, `field_utilities_game_test_fish*`, and `testing_camp_game_test*`. Run `ResourceCoverageTest` for names/item definitions, `tools/assets/verify-resource-links.py` for native asset references and `tools/assets/preview-fossil-display.py` for model bounds and an offline geometry preview.

Live acceptance remains: examine the display and tooltips at normal/minimum GUI scale, craft each chain in Survival, fish in open ocean versus an enclosed pool, brush both landmark families, and check shared items after reconnect. The offline preview uses flat colors to inspect cuboids; it is not an in-game render. Test fishing pacing before changing rarity. No full-check or play-build result is implied by focused checks.

October 21 verification: common/client compilation and `ResourceCoverageTest` pass. All four new exploration GameTests pass (`build/exploration-finds-verified.log`); the archaeology fixture supplies the native player/brush context and uses spread deterministic seeds to avoid adjacent-seed first-roll bias. The reference validator passes 1,499 asset JSON files and 4,630 model/texture/sound links. The 18-cuboid display passes bounds checks and its offline geometry preview was inspected (`build/fossil-display-preview.png`). No JAR was packaged.

Existing native archaeology brushing (one test), regional fishing (one test) and Testing Camp placement/supplies (three tests) also pass: `build/exploration-finds-archaeology.log`, `build/exploration-finds-fishing.log`, and `build/exploration-finds-camp.log`. Total targeted server coverage for this increment: nine passing tests. This is not an unfiltered fullCheck.
