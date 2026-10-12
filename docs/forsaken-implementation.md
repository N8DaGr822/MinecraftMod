# The Forsaken: implementation record

The original user brief is preserved in [forsaken-design-v1.txt](forsaken-design-v1.txt). This record separates implemented behavior from the larger first-release target.

## Confirmed direction, 2026-10-12

- Original Darkspawn content, inspired by ruined cities and parasite horror. No Lost Cities or Parasites dependencies.
- Bunkers may be discovered before the dragon. Gate activation requires the server's first Ender Dragon defeat, matching existing ecosystem progression.
- Research unlocks fabrication permanently for each reader. Open physical gates admit everyone; travelers do not need individual research.
- Expedition inventories remain normal: carry supplies in, bring salvage out. OneBlock isolation remains specific to OneBlock.
- Preserve normal server death/keepInventory behavior. No Forsaken respawn override or permanent character penalties.

## Increment 1: research and travel foundation

Uses Minecraft Java 26.3, Fabric 0.19.5 / API 0.162.0, Java 25 and the existing GeckoLib dependency. Existing native structure starts, player save mixins, container screens and teleport APIs are the implementation references. The only extension to the existing landmark generator is admitting the research-bunker site identifier; old landmark behavior and terrain placement remain unchanged.

Three native bunker templates generate in eligible **new** plains, forest, birch-forest and taiga chunks. Structure-set spacing starts at 32 chunks, separation 12; the existing terrain suitability checks may reject candidates. There is no terrain retrofit into explored chunks. Administrators can locate `darkspawn:research_bunker` with the normal locate-structure command.

Each bunker contains a damaged obsidian gate, a reusable research archive and a supply chest with a guaranteed instruction book. The archive is unbreakable in Survival, protecting access for subsequent visitors. Using it permanently records the schematic and opens the Quarantine Fabricator. The blueprint is knowledge, not a consumable chest item.

The server checks research, distance, the terminal's continued existence, all ingredients and an empty cursor before fabrication. Costs come from the main inventory/hotbar. The result appears on the cursor; normal menu closing returns it to inventory or drops it if full. Native crafting-table recipes are deliberately not registered: recipe-book visibility alone cannot enforce a player knowledge requirement. A researched player can also sneak-use any controller to fabricate away from the bunker.

### Initial tuning defaults

| Output | Materials |
| --- | --- |
| Gate controller | 2 iron blocks, 2 unweathered copper blocks, 4 quartz, 1 redstone block |
| Reusable Containment Key | 1 ender pearl, 2 blaze powder, 4 quartz, 2 iron ingots |

Build a vertical **5×5 obsidian border** with the controller replacing its bottom-center block: 15 obsidian and one controller. The 3×3 interior stays empty. The controller's orientation is set when placed. The bunker frame has one missing upper corner to repair.

Use a key on the controller after the dragon victory to activate it. The key is not consumed, there is no fuel or expiration, and every trip validates the frame. A damaged frame deactivates when used and needs repair/reactivation. Breaking a controller drops an inactive controller. Empty-hand use enters; sneak-use opens fabrication. These costs and activation rules are playtest choices rather than immutable design requirements.

### Travel, persistence and safety

- Personal gates are built in the Overworld. Each traveler saves their most recent entrance as a dimension/block-position pair. Multiple gates share one Forsaken landing, while extraction uses each player's own saved entrance.
- This checkpoint introduced a flat stone foundation and a lit, fenced quarantine landing with a protected floor and visible Extraction Terminal. The city was added in increment 2 below; infected mobs, nests, pressure and the exclusive reward loop remain upcoming.
- Use the Extraction Terminal with an empty hand to return. No dimension-only materials are needed. A destroyed controller does not trap a traveler: extraction searches near the saved entrance. Missing or obstructed entrance destinations fall back to a safe position near the world's normal spawn.
- Arrival checks are bounded to a small destination neighborhood and account for blocks, border, hazardous floor blocks and occupants. If neither route has safe space, travel fails with feedback rather than destroying construction. Keep gate approaches clear.
- Stand on solid ground, finish dimension loading, dismount and detach shoulder/leashed pets. Travel rejects active damage, fire, water, item use, open containers and cursor-held items. A two-second cooldown prevents immediate repeat trips. Inventory contents and their components are untouched.
- Player save data records schema version 1, permanent research and entrance link. Native player saves persist these with inventory and dimension; successful travel immediately saves the traveling player. Native death copying retains knowledge and the extraction link.
- The landing has separately versioned saved initialization state. Repeated visits and restarts do not regenerate it over player construction.

### Assets and checks

`node tools/assets/forsaken-foundation.cjs` rebuilds only Forsaken foundation models, data and the three bunker templates. Controller/archive/return terminals and the Containment Key use original code-authored geometry with vanilla material textures. The instruction book explains the full access loop. Further visual polish belongs with the city slice.

Full build passed with 76 unit tests and all 140 server GameTests (`build/forsaken-build.log`). Eight Forsaken tests cover the research/crafting transaction, global activation gate, damaged frames, two independent return links with preserved item components, save/death copying, blocked arrivals, landing initialization persistence, missing destination fallback, all template variants/rotations and actual native bunker placement/repair/book loot. Packaged dimension, screen and templates were verified; the test-only dimension preset is excluded from the distributable.

Manual acceptance still includes discovering a bunker in new Survival terrain, reading the instruction book, inspecting model/menu appearance, two actual clients using different entrances, and a real server restart while inside Forsaken.

## Increment 2: ruined-city slice

Native saved structures now generate a compact **64×80-block city** on Forsaken's flat foundation. The first site runs from X=-32 through 31 and Z=16 through 95, south of the landing; sites repeat every 512 blocks along both axes. The fixed layout is an intentional first-release limit, not a procedural metropolis. The new custom biome gives fresh terrain a muted sky and water palette without forcing camera effects or overriding visibility settings.

Only newly generated terrain receives cities. Already-explored Forsaken chunks and player construction are not retrofitted. In an existing save, explore a fresh grid site, such as the site 512 blocks south of the first. Partially explored sites can have incomplete edges; choose a completely fresh site for acceptance testing. Native structure starts and placed blocks persist through ordinary world saves. There is no city placement or whole-world scan on player/chunk tick.

The abandoned district contains apartments and a supply exchange. A black/yellow quarantine seam, signs, barricades and living-looking growth mark the transition to the infested hospital and research annex. **District infestation is visual in this increment.** Custom enemies, pressure, contamination and mechanically different district spawn budgets belong to the next increment.

Every building has open ground entrances, upper-floor access and a rooftop ladder. A rooftop circuit connects housing, the exchange and the research annex, including a marked one-block broken span. A short maintenance tunnel connects housing to the hospital. The central street stays open for retreat toward extraction.

Ten native loot chests use four separate data tables:

| Cache | Initial contents | Placement |
| --- | --- | --- |
| Salvage | Food, iron nuggets, copper, string and torches | Housing, shops and tunnel |
| Medical | Honey, paper, golden carrots; occasional golden apple | Infested hospital |
| Research | Quartz, redstone, iron; occasional ender pearl | Infested annex |
| Rooftop | Food, iron, gold; occasional diamond | Annex roof, beyond the rooftop crossing |

Loot is ordinary expedition salvage for now. The five exclusive resource types are still reserved for the reward increment, when each will have a useful purpose. Chests generate loot once and never automatically restock. Repeat trips can explore another untouched site rather than resetting looted buildings.

### Authoring and verification

- `node tools/assets/forsaken-foundation.cjs` maintains the dimension/gate/bunker assets, including the city structure-set reference.
- `node tools/assets/forsaken-city.cjs` generates the city template, biome, structure configuration, loot and readable voxel blueprint.
- `node tools/assets/verify-forsaken-city.cjs` checks cache reachability, the continuous return street and rooftop connection, then produces [the layout preview](../art/structures/forsaken-city-preview.html). It caught and helped correct an inaccessible research vault. This simplified voxel traversal is not a substitute for Minecraft collision playtesting.
- The authored slice is 122,880 template blocks, four buildings and ten caches. Native generation clips placement to chunks; no per-player simulation was added.
- GameTestServer disables automatic structure generation. Tests exercise native structure-start generation, serialization and piece placement explicitly; actual new-world generation remains a live acceptance check. The full build passed with 76 unit tests and all 143 server GameTests; the build log is `build/forsaken-city-build.log`.

Manual acceptance: discover a fresh site in a world with structures enabled, verify every route using normal movement, inspect sign/lighting/sky readability, loot with two players, then restart and revisit edited buildings and emptied chests. No client playtest is claimed by the layout preview.

## Remaining increments

1. Threat loop: four enemy roles, permanent nest destruction, temporary contamination and bounded local pressure. Define caps, decay/restart behavior and contamination cure before implementation.
2. Reward loop: five exclusive resources with useful purposes, quarantine protection and a nest scanner. Pulse Blade, beacon and expanded scanning follow afterward.
3. Full expedition review: tune pacing, rewards and multiplayer performance against an agreed player-count/server budget.

Procedural metropolis generation, spreading infection, mutation trees, bosses and Overworld outbreaks remain deferred. Activity-based proficiency is still paused.
