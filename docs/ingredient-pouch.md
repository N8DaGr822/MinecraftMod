# Ingredient Pouch

October 20, 2026. First utility-equipment increment, implemented in source.

## Obtaining and using

Craft with three leather, one string and one nether wart:

```text
 S
LWL
 L
```

S = string, L = leather, W = nether wart. Fresh Testing Camps include one pouch in barrel 09 and instructions in the guide. Existing camps remain unchanged. It also appears in the Tools & Utilities creative tab.

The pouch uses native bundle controls, contents preview and capacity bar. Left-click a reagent onto the pouch (or the pouch onto a reagent) to insert. Right-click with an empty cursor to remove a stack, or right-click the carried pouch onto an empty slot. Using it in hand drops contents into the world, as a bundle does.

Capacity is one native bundle: 64 ordinary stackable ingredients total, not 64 per ingredient. Items with smaller stack limits consume proportionally more space; a turtle helmet fills it by itself. Full insertion leaves overflow where it was.

## Eligibility and ownership

Only the reagents listed in `data/darkspawn/tags/item/pouch_ingredients.json` are accepted. The initial list matches all current vanilla brewing reagents and the mod's wild herbs, jungle pepper, goat milk, woodland truffle, marsh rice and frost garlic. Some ordinary materials, including stone, cobwebs and slime blocks, qualify because vanilla brewing uses them.

Empty bottles, finished potions and unrelated cooking ingredients are excluded. Containers cannot go inside the pouch, even if a data pack adds them to the ingredient tag. The pouch itself cannot go inside bundles or shulker boxes. Ordinary chests and barrels can hold it.

Contents live in the item's native bundle component. There is no separate player-owned pouch inventory, automatic pickup, remote access, or direct brewing from the pouch: remove the ingredients first. Sorting and profile saves carry the filled item intact. Favoriting its inventory slot protects it from Sort and matching deposits. Normal death and keep-inventory rules still apply; the pouch does not protect ingredients from loss.

## Presentation and acceptance

This first version reuses the native bundle artwork and open/selected-item presentation, with an Ingredient Pouch name and explanatory lore. A small client hook enables native scrolling without adding the pouch to the vanilla dye-recipe tag; dyeing cannot silently turn it into a general-purpose bundle. Bespoke artwork remains future polish.

Targeted checks cover insertion in both directions, full capacity, extraction, eligible custom herbs, unstackable reagent weight, nesting rejection, favorite sorting, serialized island profiles and death drops. Live-client controls, preview selection, reconnects and travel through all dimensions still need gameplay acceptance before widening portable storage.

Common/client compilation, 18 inventory unit tests, ResourceCoverageTest, all four pouch server tests and all three Testing Camp server tests passed. The final pouch run includes hopper/menu nesting restrictions and separate saved OneBlock and Ascension profiles. All 27 tagged ingredients were checked against current vanilla/mod brewing recipes, and all three referenced native bundle models exist. Logs: `build/pouch-checks.log`, `build/pouch-final-checks.log`, `build/pouch-camp-checks.log`. This was targeted verification, not unfiltered fullCheck or a packaged play build.
