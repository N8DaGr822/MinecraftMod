# Darkspawn Testing Camp

Get **Darkspawn Testing Camp** from Creative's Functional Blocks tab, or run:

```mcfunction
/give @s darkspawn:testing_camp
```

Right-click the **top of the center ground block** in a flat, open area. The item tooltip lists the **21 x 21 footprint and four blocks of headroom**; clicking a side reminds you to use the top. The camp preserves solid ground and fills gaps with stone bricks. Obstructions, fluids (including waterlogged ground), protected positions, unloaded chunks, world boundaries, and entities occupying new solid blocks cause the entire placement to be refused before building. The message identifies blocked terrain. Grass and other replaceable vegetation can be replaced at construction positions and is cleared from the enchanting setup's bookshelf gaps.

There is no survival recipe or automatic placement when joining. One kit is consumed on successful Survival placement; Creative retains it. Use a disposable testing world. The camp is made of ordinary persistent blocks: dismantle it normally. Place another kit in another clear area to restock; placing over an existing camp is rejected.

## Ready-to-use stations

The north workbench row has a crafting table, Darkspawn Cooking Station, furnace, smoker, blast furnace, stonecutter, smithing table, anvil, grindstone, and brewing stand. Directional workstations face south into the central aisle. The furnace, smoker, and blast furnace each start with 32 coal; the brewing stand has 32 blaze powder. A full water cauldron beside the row supplies bottles. Refill it with the supplied water buckets. The east corner has an enchanting table with fifteen unobstructed bookshelves. Extra fuel, lapis, XP bottles, and brewing supplies are in the crafting barrel.

The northwest bonfire has its copper/stone-brick foundation already built. It starts **off** so ordinary hostile testing works. Right-click with an empty hand to activate; sneak-use empty-handed to upgrade with 8 iron, then 4 gold, then 2 diamonds. The southeast irrigated, illuminated plot has all nine Darkspawn crops planted at their first growth stage. Seeds, produce, bone meal, and a hoe are provided. Beds are in the utility barrel; place one in the Overworld to set your respawn point.

## Organized supplies

October 15 expansion: new camps also supply pirate gates/compasses, shipyards/helms/cannons, both shot types, timber/currency, beer and rum; Quarantine armor, Pulse Blade, Bio Scanner, beacon/fuel/resources; the Landmark Compass, all boss summons/hearts and regional ritual materials; mount food, companion capsules and veterinary kits. The in-game guide explains party navigation, promotions, drinks and mount summon IDs. See [stage rules](expansion-stages.md). Consuming supplied hearts changes personal progression; simply placing the camp does not.

The **Charts archaeology rematches** supplies include blank discovery charts, Awakening Seals, brushes, echo shards, amethyst, diamonds and bread. Two suspicious-gravel samples southwest of the center use real regional archaeology loot. Brush each until it releases its find; deposits are single-use. New guide pages explain home ports, refits, care/roaming, chart creation, archaeology, awakened rituals and regional events. Existing currency/wood/iron supplies cover port and ship payments. See the [server playtest route](server-playtest.md). Place a fresh camp for these additions; existing camps are not automatically altered.

Accessories/hub/fishing/animal expansion: three new supply groups contain all six accessories, regional raw/cooked fish and six fish buckets, a fishing rod, animal spawn eggs, specialty feed and ingredients. Eleven new dishes and their ingredients flow through the existing recipe-driven supply lists. Three guide pages explain controls and habitat conditions. See [field utility rules and acceptance](field-utilities-playtest.md).

Barrels around the perimeter have visible signs and named inventories. Follow them clockwise from the northwest corner: guide, combat kit, pantry, prepared food, farming/equipment, utilities, then armor styles. Large categories span numbered barrels.

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
- **Combat tools:** diamond axe and spear, trident, mace, crossbow, shovel, and fishing rod.
- **Target practice:** targets, armor stands, arrows, and zombie/skeleton/creeper spawn eggs. Keep the bonfire off when testing ordinary hostiles.

All stacks respect normal stack limits. The camp has 72 possible perimeter barrel positions, using the original 36 first and then filling their gaps. If data packs add more ingredients than these can hold, placement refuses rather than losing supplies.

## Quick playtest

1. Take the combat kit, testing guide, cookbook, and recipe scrolls. Switch to Survival to exercise normal mechanics.
2. Cook a basic dish, a regional dish, and a boss dish using the cookbook. Compare those with the prepared meals and feast servings.
3. Grow and harvest the crop plot. Replant from the supplied seeds.
4. Compare armor styles and loadouts, then take expedition gear into hot/cold biomes. Use XP bottles and the enchanting setup for equipment tests.
5. Build a dump-chest/tube/sorting-chest line. Test the bonfire separately, keeping it off during ordinary hostile tests.
6. Run `/summon darkspawn:griffin` in open space. Empty-hand ride; Space rises, X descends, Shift dismounts.
7. Enter OneBlock through the inventory menu. Forsaken travel still requires dragon completion and the normal valid gate frame. The camp does not alter dragon state, recipes, gamerules, weather, time, or player progression.

Use the same updated mod on clients and server, retaining Fabric API and GeckoLib. The October 12 checkpoint predates the current expansion; use a newly requested play build to test these changes. No new JAR is implied by source edits.

Verification on October 12, 2026: full build passed with 80 unit tests and all 162 server GameTests. Three new tests cover actual camp-item placement and Survival consumption, obstruction rejection, supply capacity, recipe coverage, and armor style samples. Live layout, sign readability, and multiplayer usability still need a playtest.

October 15, 2026: all three camp tests passed again with the expanded supplies and 72 available barrel positions. The wider stage passed 35 pirate and 20 naval server tests plus six focused unit tests; see [verification and limitations](expansion-stages.md#acceptance). No new JAR was packaged.

Stage 2 adds two guide pages for the three new island types and Redwater skirmishes. Take the existing Pirate supplies and Logbound Compass into fresh sea terrain. Test a neutral observer, aid each faction on separate landings, attack both sides, leave during combat, and reload afterward. Started battles are one-time encounters even when abandoned. Existing placed camp books are unchanged; place a fresh camp for the updated guide.

Stage 2 verification: common/client compilation, six focused unit tests, 40 pirate, 20 naval and three camp server tests passed. All ten pirate templates passed asset and walking-route checks; overhead layouts were inspected. Live gameplay/multiplayer acceptance remains open. No JAR was packaged.

## Endgame increment

Fresh camps include groups 22–25 for faction ammunition, culinary potion samples and reagents, Heartwood/harvest supplies, and Nexus/legendary relics. The guide explains creative rehearsals and shared-world survival gates. See [endgame playtest](endgame-expansion-playtest.md). Existing placed camps are not modified.
