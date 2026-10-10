# Darkspawn expansion plan: discover the bosses

Status: **first milestone implemented for alpha.3**, October 9, 2026. Druid Shrines and Hunter Camps have native generation, three templates each, lazy loot chests, and ritual clue books. The remaining 14 boss landmarks are still planned. The acceptance checklist below distinguishes intended live review from automated validation recorded in the development roadmap.

## First playable milestone: forest and taiga

The first two landmarks let players explore, learn the ritual, and return prepared. The shipped shrine footprint is 31×31 to preserve the Tree Spirit's 21×21 horizontal and 28-block vertical clearance. The camp footprint is 25×25 around the wolf's 10×10×9 clearance. The smaller dimensions below were the initial design targets.

| Structure | Presentation and footprint target | Discoveries | Encounter relationship |
| --- | --- | --- | --- |
| Overgrown Druid Shrine | Roughly 25×25 blocks; broken stone circle, giant roots, moss, fallen arch, small offering shelter. Three rotation-safe variants. | A written ritual clue, herbs, ordinary crop seeds, a small chance of Ancient Heartwood summon materials. One provision chest. | Recognizable center for the existing Ancient Tree Spirit ritual. Clear approach and arena space; never automatically spawns the boss. |
| Abandoned Hunter Camp | Roughly 23×19 blocks; collapsed spruce shelter, extinguished fire, equipment rack, claw-marked posts, trail toward a clearing. Three variants. | A hunter's note explaining night-time summoning, food, garlic/pepper seeds, and limited wolf summon materials. One supply chest. | The existing Mutant Wolf ritual remains valid anywhere its current conditions allow. Camp is a preparation stop, not a required progression key. |

Keep the first structures compact and recognizable. No randomly placed trophy or boss-only culinary ingredient: those remain victory rewards. Loot supports a ritual without guaranteeing every component. Clues explicitly explain the world's dragon requirement. Structures may exist before the dragon; rituals and regional ecosystems retain their existing gate.

### Delivery sequence

1. Author six editable templates and export versioned structure NBT. Implemented with `tools/assets/boss-structures.py`, readable voxel blueprints in `art/structures/`, and an offline layout gallery from `tools/assets/preview-structures.cjs`. This reproducible source replaces manual development-world authoring for the first milestone. Template origins, ritual anchors, and loot positions are explicit; native chest NBT replaces marker callbacks.
2. Implement native structure and structure-set resources for the pinned Minecraft version. Shipped as one-piece saved `TemplateStructurePiece` starts with a custom bounded terrain suitability check; no jigsaw pool is needed yet. Dedicated biome tags and loot tables support datapack changes. Generation runs at the top-layer stage to clear natural trees from the ritual volume after vegetation, not on later chunk loads.
3. Candidate placement uses spacing 40 chunks, separation 16, independent stable salts. These are candidate-grid settings, not guaranteed distances. A fixed 49-point survey checks water and elevation. Sites with a sampled height range over six blocks are rejected; accepted sites meet the midpoint with at most three blocks of sampled cut/fill. This was widened from an initial three-block total range after the five-seed survey found that restriction too sparse. Small unsampled terrain variations and border seams remain visual playtest checks.
4. Add written clue books and themed provision loot. Guarantee one useful clue per successful structure; make food/material rolls modest. Use chest loot tables and seeds so unopened chests remain lazy-generated; do not rewrite existing inventories.
5. Add `/locate structure darkspawn:druid_shrine` and `darkspawn:hunter_camp` through native registration. Verify placement with `/place structure` in a test world, then sample natural generation across at least five seeds and positive/negative coordinates.
6. Run multiplayer, reload, terrain, and upgrade checks. Capture screenshots and approve rarity after actual travel between structures. Package as the next feature alpha only after these checks pass.

### Acceptance criteria

- All template rotations load, all markers resolve, loot has no missing entries, and players can navigate every intended path.
- A fixed seed produces stable placement; supported biomes produce examples and excluded biomes/dimensions do not.
- No retroactive placement into generated chunks. Existing worlds find structures only in newly generated terrain; no automatic edits around player builds.
- Generation performs no unbounded search or synchronous chunk loading. Terrain adaptation stays within the declared footprint and does not leave hanging terrain or sealed entrances.
- Two players can explore/open the same chest normally. Boss contribution, hearts, abandonment, reset, and death rules remain unchanged.
- Structures do not spawn bosses. Existing crafted summons and portable rituals continue working independently of finding a structure.
- A pre-dragon world permits exploration but rejects the boss ritual with the existing explanation; a post-dragon world accepts a valid ritual.
- New chunks across an upgraded world's border load correctly. Removing or changing a template never renames shipped IDs or breaks saved structure references.

## Second milestone: regional landmarks

Ship in small groups after the first two prove the placement/loot pipeline. The names and sizes below are design proposals.

| Boss | Landmark | Design hook |
| --- | --- | --- |
| Mutant Zombie | Abandoned farm excavation | Collapsed cellar, disturbed graves, mutation notes; avoid generating inside villages. |
| Fossil Tyrant | Exposed fossil altar | Rib-shaped stone formation, excavation tents, archaeology clues. |
| Thunder Bird | Storm altar | Copper spire and wind-worn platform with a safe route to the summit. |
| Titan Boa | Overgrown serpent sanctuary | Broken jungle court and enormous shed-scale motif. |
| Baba Yaga | Wandering witch's hut | Purpose-built loot source, brewing clues, unsettling roof silhouette. |
| Mountain Titan | Shattered quarry shrine | Chisel marks, fractured monolith, visible approach through rock. |
| Ice Wyrm | Frozen nest | Ice-buried remains and cracked eggs around a sheltered hollow. |
| Kraken | Sunken expedition wreck | Diveable supply compartment and clear surface escape route. |
| Cave Crawler | Webbed expedition outpost | Abandoned mining equipment; capped webs to avoid trapping every path. |
| Shadow Creeper Queen | Deep Dark hive | Sculk-lined chambers and readable escape paths; preserve Ancient Cities. |
| Mycelial Sovereign | Fungal court | Giant mushroom ring, spore garden, hidden pantry. |
| Netherborn | Ember shrine | Basalt shelter, scorched offerings, fire-safe approach. |
| Soulbound Colossus | Soul reliquary | Bound pillars, soul-lantern procession, ruined memorial. |
| Void Eye | End observatory | Broken viewing rings and floating-lens motif; no unsafe mandatory jumps. |

Underground, underwater, Nether, and End placement each need their own terrain tests. Do not reuse a surface-height rule for every structure. Regional particles currently signal suitable habitat, not the presence or direction of one of these future structures.

## Following expansions, in priority order

1. **Exploration journal and maps:** combine cookbook, boss clues, discovered landmarks, and existing advancement progress. Add a structure compass only after structure tags and saved discovery ownership are stable.
2. **Archaeology and regional loot:** fossil fragments, extinct seeds, fishing clues, and alternate scroll sources. Preserve existing recipes and avoid extra permanent hearts.
3. **Rematches and cosmetics:** awakened versions of existing bosses, improved trophies, banners, and trims. Reward ingredients/materials/cosmetics; retain the current heart cap.
4. **Regional events and sound:** bounded optional encounters, distinct warning sounds, and carefully scoped post-victory changes. No unsolicited damage to terrain or builds.
5. **Server tuning and compatibility:** validated rarity/spawn settings, ingredient tags, and modpack overrides; then consider the optional endgame Nexus.

Before widening each milestone: finish live visual review, two-client testing, performance sampling, and balance. These priorities are a work order, not release dates.
