# Pirate Seas: first playable dimension

Future expansion is documented in [The Shattered Seas plan](shattered-seas/project-overview.md). This page describes the current dimension implementation.

Scope approved October 14: dimension exploration and travel first; craftable pre-dragon access. This increment adds the ocean, harbor, island discoveries, finite loot and return travel. The subsequent [naval increment](naval-gameplay.md) adds ships, cannons, four crew roles and progression.

## Enter and return

Craft a **Sea Gate** at a crafting table:

```text
C P C
I K I
C P C

C = copper ingot (4 total)
P = prismarine shard (2 total)
I = iron ingot (2 total)
K = compass (1)
```

The recipe unlocks on acquiring a compass. Place the gate on land in the Overworld and use it with an empty hand. It is a single-block navigational terminal with a miniature illuminated arch; no surrounding frame, activation item or dragon victory is required. Creative: Functional Blocks, or `/give @s darkspawn:sea_gate`.

Travel retains normal survival inventory and records each player's own departure location. Stand on land, dismount, clear the cursor and staged trash, and finish any open menus. Leashed animals, shoulder pets, active item use, fire and recent damage prevent departure. Arrival acknowledgement and a two-second cooldown prevent immediate repeat transitions.

The shared harbor is centered at **0, 65, 0**. Use **Harbor Return** at **0, 65, -4** to return near your departure gate. If that area is obstructed, the terminal attempts the Overworld spawn. Both destinations require clear headroom, a safe solid floor, world-border clearance and no overlapping entities. A blocked arrival rejects travel without excavating blocks. The return terminal and its supporting block resist ordinary survival destruction; other harbor structures can be edited.

Normal survival death, beds and dropped inventory apply. Return links survive player saves and death copies. Each dimension has independent return state; OneBlock's inventory isolation and Forsaken's progression remain unchanged.

## Explore

- **Harbor:** lighthouse, lit arrival plaza, chandlery with crafting table/furnace, signed dock and shared starter supplies. Three boats, food, saplings and fishing equipment are finite; plant trees and craft replacement boats.
- **Castaway Cove:** a guaranteed island about 192 blocks east, with an open shelter, campfire, dock and salvage cache.
- **Saltwatch Fort:** ruined coastal walls and a treasure alcove with ordinary pre-dragon supplies.
- **The Pale Gull:** beached wreck, mast, torn sail and an accessible cargo cache.
- **Lastlight Beacon:** abandoned signal tower, ladder, keeper's cache and a working light.
- **Tideglass Shrine:** open prismarine ruins, broken columns and a finite relic cache.
- **Rootbound Camp:** mangrove roots/trees, shelter and a salvage cache with propagules.

Further islands use native seeded structure placement: 16-chunk regions, eight-chunk separation, and one of six 49-by-49 templates. A reserve prevents random islands overlapping the harbor and first cove. A level seabed at Y39 and water through Y63 provide open sailing lanes; templates grow solid foundations from Y40. This is an authored archipelago over a flat ocean, with repeated destination layouts, rather than varied noise-generated continents. It uses the Overworld day/night and weather rules with a dedicated warm ocean biome. Ordinary boats remain available; the naval increment adds larger craft and combat.

Loot uses native chest tables and never refills on revisits. The harbor and first cove initialize once per dimension through saved data. Random islands persist through native structure starts. Existing Overworld chunks are untouched. After installing a newly requested play build, reopen the world to load the dimension registry.

The Journal's Overview contains the recipe and travel instructions. Mark harbor coordinates when exploring; normal compasses follow their usual dimension behavior and are not a custom harbor locator.

## Source and verification

- Logic: `src/main/java/darkspawn/black/pirate/`.
- Rebuild authored data/models/templates: `node tools/assets/pirate-foundation.cjs`.
- Layout and walking-route check: `python tools/assets/preview-pirate.py` (requires Pillow); output [layout preview](../art/pirate/layout-preview.png). This is an offline plan view, not an in-game screenshot.
- Compile: `gradlew.bat classes clientClasses --max-workers=1`.
- Server checks: `gradlew.bat runGameTest -PgameTestFilter=darkspawn-gametest:pirate_game_test* --max-workers=1`.

Verified October 14: common/client compilation, **7/7 pirate server GameTests**, **3/3 Journal unit tests**, resource JSON parsing and all four authored-template walking-route checks pass. Server coverage includes pre-dragon multiplayer round trips, item/component retention, unsafe departure rejection, save/death return-link persistence, missing-entrance recovery, actual ocean depth and island placement, finite loot, and native structure serialization. This is targeted verification, not a full-check run. Log: `build/pirate-final-checks.log`.

Manual playtesting still needs to assess boat handling at docks, island spacing, lighting, underwater atmosphere, and visual quality at different graphics settings. No JAR packaging or version change is part of this implementation.
