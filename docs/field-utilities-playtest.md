# Fishing and animal utility

Implemented in development source on October 15, 2026. The accessories and inventory hub that originally shipped alongside these features were removed with the other inventory changes in November 2026.

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

Fresh camps add **Regional fishing** and **Animal husbandry** supplies, plus guide pages. Supplies include six raw/cooked fish, six fish buckets, a fishing rod, animal spawn eggs, feed and specialty ingredients. Existing recipe-driven ingredient/meal supplies include all eleven new dishes. Place fish in water and test pig foraging in a forest; camp placement itself does not override habitat conditions or summon animals.

Live checks: named bucket release/reload; natural fishing in each habitat; pig pathfinding; next-egg replacement; harvest cooldowns across restart.

Artwork source: `tools/assets/field-utilities.py` (validator: `tools/assets/verify-field-utilities.py`). Server coverage: `darkspawn.black.cooking.HusbandryFishingGameTest`.
