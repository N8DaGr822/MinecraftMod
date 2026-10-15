# Accessories, inventory hub, fishing and animal utility

Implemented in development source on October 15, 2026. No JAR or version change. Request a play build before installing these additions; an older JAR with the same version string does not contain them.

## Accessories

Three native inventory slots sit below the player portrait. Each holds one accessory; ordinary inventory clicks, swapping, dragging and shift-click removal retain server menu validation. Equip by moving the item into a slot. Accessories do not consume durability or charge, and duplicate copies never multiply their utility. Native death/keepInventory and vanishing rules apply. Equipped accessories belong to the current OneBlock possession profile, so they cannot cross that boundary. Names and item components persist.

| Accessory | Benefit | Crafting materials |
| --- | --- | --- |
| Night Vision Glasses | Full night vision while switched on; default N key is rebindable; hub toggle and HUD state | Two glass, gold ingot, glow ink sac |
| Diving Charm | Air drains half as often; shares the existing Deep Breath rule without stacking | Nautilus shell, prismarine shard, gold ingot |
| Fall Talisman | 25% less fall damage | Feather, leather, amethyst shard |
| Pickup Magnet | Native pickup of eligible drops within a three-block neighborhood and clear sight | Two iron ingots, redstone |
| Navigation Lens | Direction/coordinates plus bearing and distance to the first carried located compass target in this dimension | Spyglass, compass, gold ingot |
| Heat Trinket | 25 points of environmental heat protection; not fire/lava immunity | Magma cream, gold ingot, quartz |

Glasses consult equipment independently of active effects. Switching them off does not remove or shorten potion/meal night vision. Other equipment that provides light remains effective. The lens does not locate structures or grant discoveries. The magnet respects native pickup delay, item ownership and inventory capacity. Fall and heat benefits coexist with other equipment; live balance needs checking.

## Compact inventory hub

Use **Hub** at the left of the normal inventory. Native crafting, hotbar, equipment, loadouts, sorting and the staged trash slot stay in the inventory. **Move items / Back** returns there. Its new Search box marks nonmatching occupied slots; favorite slots have a gold marker.

- **Inventory:** searchable item list, count/capacity, durability, enchantments, accessory instructions and side-by-side raw attribute amounts for selected versus equipped gear (or main-hand equipment). Conditional powers remain separate from raw attributes. Select an eligible main/extra-storage slot and choose Favorite to protect it from Sort and hub deposits. Manual movement is still allowed; hotbar/equipment are already excluded from automatic sorting/deposit.
- **Character:** health, armor, absorption, movement speed, effect timers, glasses state and the three equipped accessories. More details pages long text without shrinking item slots.
- **Companions:** nearby owned deployed mounts, plus owned stored/recovery records for this inventory profile. Care opens the existing mount menu only at its normal range with clear sight. Use owned capsules/veterinary kits for release/recovery. Unloaded deployed mounts are not force-loaded or globally located.
- **Storage:** reachable Darkspawn sorting containers, positions and occupancy. Open uses the existing native storage interface for filters and refiltering. Deposit matching inserts only items already represented in a nearby Dump Chest and skips favorites, hotbar and equipment. The physical network then routes items normally. Access requires clear sight and at most five blocks; the server revalidates every request. This is not remote access to arbitrary vanilla chests or an inventory of unloaded networks.
- **Journal:** opens the existing Journal and its cooking/progression data; Back returns to the hub.

Search and selection are read-only. Server snapshots are bounded and throttled; opening a panel never authorizes item movement by itself. Normal inventory revision checks reject stale mutations.

## Six fish species

Pike, tuna, anglerfish, Arctic cod, piranha and cave fish now have actual swimming entities, raw/cooked items and species-preserving buckets. They reuse native schooling-fish movement and cod-derived geometry, with six authored skins. They are ordinary fish rather than added combat encounters. Natural population uses the vanilla water-ambient cap; no forced chunk loading or extra population manager.

| Habitat at the fish/bobber | Catch |
| --- | --- |
| Underground water below Y0 with no sky view | Cave fish |
| Deep-ocean biome below Y40 | Anglerfish |
| Very cold biome (temperature <= 0.15) | Arctic cod |
| Jungle or mangrove swamp | Piranha |
| Other ocean biome | Tuna |
| Other water, including rivers and ponds | Pike |

Habitat rules are evaluated in that order. Natural spawning is in Overworld water with another water block above the fish. Actual biome distribution and encounter frequency need a normal-world playtest.

Ordinary fishing keeps the original treasure/junk/luck roll. Half of plain vanilla cod/salmon results become the matching regional fish; named/component-modified catches remain intact. Catching with a water bucket retains the species, native bucket state and names. Furnace/smoker cooking provides cooked fish. Six Cooking Station dishes use one cooked regional fish, potato, wild herbs and a bowl. These new meals follow the existing one-meal-effect rule.

## Animal utility

Sneak-use the listed item on a living adult animal. Normal non-sneaking feeding, breeding, milking and stew behavior remains. Specialty harvests have a saved five-minute cooldown; reconnecting/reloading cannot reset it. Failed validation consumes nothing.

| Animal | Input | Outcome |
| --- | --- | --- |
| Pig in a forest | Carrot | Walks toward a nearby grass/podzol patch and can find one woodland truffle after five seconds; the hunt expires after 30 seconds if it cannot reach the patch |
| Chicken | Pumpkin seeds | Its next naturally laid egg becomes a rich egg |
| Chicken | Wild herbs | Its next naturally laid egg becomes an herbed egg |
| Goat | Empty glass bottle | Goat milk ingredient |
| Mooshroom | Bowl | Fungal cream ingredient |

Pigs consume the carrot when a valid hunt starts; an obstructed or unloaded hunt can fail without producing a truffle. Terrain is not dug up. Chicken feed does not accelerate laying or create a second egg, and a pending feed cannot be replaced. Pending egg choice and pig hunt/cooldown data survive native entity saves.

Goat milk plus salt crafts two goat cheese and returns the bottle. The Cooking Station adds truffle risotto, farm/herbed omelettes, goat cheese plate and fungal cream soup. Cream soup also requires an empty bowl, so its ingredient's returned bowl does not create free containers. Recipe ingredients appear in the cookbook and fresh Testing Camp supplies.

## Testing Camp and acceptance

Fresh camps add **Accessories and utility**, **Regional fishing** and **Animal husbandry** supplies, plus three guide pages. Supplies include all six accessories, six raw/cooked fish, six fish buckets, a fishing rod, animal spawn eggs, feed and specialty ingredients. Existing recipe-driven ingredient/meal supplies include all eleven new dishes. Place fish in water and test pig foraging in a forest; camp placement itself does not override habitat conditions or summon animals.

Test with two clients before the full family session: item movement at every GUI scale and with the recipe book; glasses with existing potions/meals; favorite sorting/deposit under latency; full-inventory magnet behavior; accessory death and keepInventory; both directions of OneBlock travel; named bucket release/reload; natural fishing in each habitat; pig pathfinding; next-egg replacement; harvest cooldowns across restart; and storage access from behind walls or outside reach.

Artwork source: `tools/assets/field-utilities.py`. Offline icon review: `build/field-utilities-preview.png`. Live rendering and five-player balance are not established by compilation or server tests.

## Verification record

October 15: common/client compilation, all 94 unit tests, all 284 server GameTests and cuisine artwork validation passed in the unfiltered fullCheck (`build/field-utilities-finalcheck.log`). The eight new server tests cover accessory serialization/OneBlock separation, potion-safe toggles, favorite sorting, death copying, fish bucket identity, native animal saves/egg timing, nearby storage deposits, magnet ownership/delay/capacity, talisman removal and finite foraging.

The final shift-click overflow correction passed all eight focused server tests and inventory regressions (`build/field-utilities-last-check.log`). The final unsupported-server slot guard and comparison-label changes compiled for common/client and passed inventory slot/sorting tests (`build/field-utilities-compatibility.log`). The dedicated asset validator passed all 41 item resources and six fish skins; the offline icon sheet was visually inspected. `git diff --check` passed. These checks do not constitute a live client or five-player playtest, and no JAR was packaged.
