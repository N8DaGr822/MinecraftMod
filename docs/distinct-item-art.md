# Darkspawn item artwork pass

October 15, 2026. Mod-owned inventory items that directly used Minecraft item models or borrowed material textures now use Darkspawn artwork. This extends the existing code-authored cuboid style rather than introducing a new rendering dependency.

- Titanbreaker, Voidbow, Queen's Carapace, Soul Lantern Relic, Nexus key/core and Awakening Seal have dedicated geometry.
- Chain and piercing shot have recognizable ammunition shapes. Existing solid/incendiary shot, drinks, capsule, vet kit and containment key retain their authored shapes with original materials.
- Cookbook, eight recipe scrolls, discovery chart and both starter kits have custom inventory models.
- Landmark and Logbound compasses share 32 custom needle frames. Their existing lodestone selector, thresholds and route behavior remain unchanged.
- Voidbow retains its idle and three draw states. Queen's Carapace uses custom idle/blocking poses instead of Minecraft's special shield renderer. Damage, durability, ammunition, cooldowns, recipes and saves are unchanged.
- Inventory forms of storage blocks, naval machinery, bonfire and gate items use custom materials. Their placed world models remain as before. Heartwood is the exception: its log and seedling now match their custom inventory identity, with textures registered on the block atlas.

Ordinary Minecraft items remain Minecraft items. In particular, culinary potions still use the vanilla potion/splash/lingering bottles and effect tinting; this pass does not globally replace those shared item types. Equipment's native armor-trim overlays also remain intact.

## Authoring and verification

`tools/assets/distinct-item-models.py` is the editable geometry/material source, following the existing naval and cuisine asset authoring approach. Run it **after** older feature asset generators, which can otherwise restore their original model definitions. The inventory of 76 model states is in `art/items/distinct-models.json`.

Use `python tools/assets/verify-distinct-items.py` to validate geometry, materials, compass frame directions, bow selectors, shield poses and block-atlas coverage. It writes the offline contact sheet to `build/distinct-items-preview.png`. Then run `verify-resource-links.py`, `verify-pirate-assets.py` and `verify-endgame-expansion.py` for cross-feature references.

Offline geometry review does not verify actual lighting, GUI scaling or left/right-hand fit. Test those in Minecraft, especially compass bearing, bow draw, shield blocking, transparent bonfire flames and planted Heartwood. No gameplay code or JAR packaging is part of this pass.

Verification passed: the 76-state artwork validator; broad references across 1,467 asset JSON files and 4,571 model/texture/sound links; pirate/endgame validators; common/client compilation; and the two focused tests for actual-registry item/block resource coverage and equipment-atlas preservation (`build/distinct-item-final-tests.log`). `git diff --check` passed. This was targeted artwork verification, not a fullCheck or a live client test.
