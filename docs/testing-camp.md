# Darkspawn Testing Camp

Get **Darkspawn Testing Camp** from Creative's Functional Blocks tab, or run:

```mcfunction
/give @s darkspawn:testing_camp
```

Right-click the **top of the center ground block** in a flat, open area. The camp needs a **21 x 21 footprint and four blocks of headroom**. It preserves solid ground and fills gaps with stone bricks. Obstructions, fluids, protected positions, unloaded chunks, world boundaries, and entities occupying new solid blocks cause the entire placement to be refused before building. The message identifies blocked terrain. Grass and other replaceable vegetation can be replaced at construction positions.

There is no survival recipe or automatic placement when joining. One kit is consumed on successful Survival placement; Creative retains it. Use a disposable testing world. The camp is made of ordinary persistent blocks: dismantle it normally. Place another kit in another clear area to restock; placing over an existing camp is rejected.

## Ready-to-use stations

The north workbench row has a crafting table, Darkspawn Cooking Station, furnace, smoker, blast furnace, stonecutter, smithing table, anvil, grindstone, and brewing stand. The east corner has an enchanting table with fifteen bookshelves. Fuel, lapis, XP bottles, and brewing supplies are in the crafting barrel. Supply your own brewing water using the included buckets and bottles.

The northwest bonfire has its copper/stone-brick foundation already built. It starts **off** so ordinary hostile testing works. Right-click with an empty hand to activate; sneak-right-click to change radius. The southeast irrigated, illuminated plot has all nine Darkspawn crops planted at their first growth stage. Seeds, produce, bone meal, and a hoe are provided. Beds are in the utility barrel; place one in the Overworld to set your respawn point.

## Organized supplies

Barrels around the perimeter have visible signs and named inventories. Large categories span numbered barrels.

- **Start here:** a short in-game testing guide.
- **Dragon combat kit:** the full enchanted diamond kit, food, potions, and travel supplies from Dragon Starter Chest.
- **Cooking ingredients:** up to a full stack of every ingredient alternative referenced by the server's loaded Darkspawn cooking recipes, including boss ingredients. Data-pack changes are included automatically.
- **Meals and feasts:** eight of each registered dish, capped by its normal stack limit.
- **Cookbook and scrolls:** recipe instructions and scrolls for discovering locked meals. Use the scrolls before attempting those recipes; the camp does not silently unlock them.
- **Farming:** all nine seed/produce pairs, vanilla seeds, bone meal, and a diamond hoe.
- **Expedition armor:** complete mountaineering, spelunking, diver, desert wanderer, and ashwalker sets.
- **Legendary armor:** all five sets and their cores.
- **Armor styles:** each of the fifteen styles in complete iron and diamond sets, one labeled barrel per style.
- **Crafting and fuel:** metals, wood, basic materials, books, lapis, XP bottles, coal, and brewing supplies.
- **Base, storage and travel:** bonfire supplies, beds, storage chests and tubes, inventory upgrades, Quarantine Gates and keys, and obsidian.

All stacks respect normal stack limits. If data packs add more ingredients than the camp's 36 barrel positions can hold, placement refuses rather than losing supplies.

## Quick playtest

1. Take the combat kit, testing guide, cookbook, and recipe scrolls. Switch to Survival to exercise normal mechanics.
2. Cook a basic dish, a regional dish, and a boss dish using the cookbook. Compare those with the prepared meals and feast servings.
3. Grow and harvest the crop plot. Replant from the supplied seeds.
4. Compare armor styles and loadouts, then take expedition gear into hot/cold biomes. Use XP bottles and the enchanting setup for equipment tests.
5. Build a dump-chest/tube/sorting-chest line. Test the bonfire separately, keeping it off during ordinary hostile tests.
6. Run `/summon darkspawn:griffin` in open space. Empty-hand ride; Space rises, X descends, Shift dismounts.
7. Enter OneBlock through the inventory menu. Forsaken travel still requires dragon completion and the normal valid gate frame. The camp does not alter dragon state, recipes, gamerules, weather, time, or player progression.

Use the same updated mod on clients and server. The checkpoint JAR is `build/testing-camp-verification/libs/darkspawn-2.0.0-alpha.1.jar`; replace the old Darkspawn JAR and retain Fabric API and GeckoLib.

Verification on October 12, 2026: full build passed with 80 unit tests and all 162 server GameTests. Three new tests cover actual camp-item placement and Survival consumption, obstruction rejection, supply capacity, recipe coverage, and armor style samples. Live layout, sign readability, and multiplayer usability still need a playtest.
