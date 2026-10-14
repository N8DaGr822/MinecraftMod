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

## Increment 3 Threats contamination and rewards

Four standalone Forsaken enemies use authored rigs with idle, movement, warning, impact and death clips. Wanderers have 24 health and charge from slow patrols; Crawlers have 16 health, a low silhouette and flanking approach; Spore Spitters have 22 health, retreat at close range and fire physical spore projectiles; Brutes have 60 health, 10 armor, heavy knockback and slow swings. Spitters and Brutes belong to the infested district; Brutes require pressure of at least 50. Windups last 16 ticks, or 24 for Brutes. Impact checks range and sight again, so moving away or taking cover works.

### Permanent nests and migration

Four biological nest blocks are authored in the hospital and annex, on ground and upper floors. Mining or destroying one yields a single Parasitic Core through its native loot table. It has no obtainable placeable item, Silk Touch does not return the nest, and no regeneration hook recreates it. Native chunk saves preserve its removal. Creative breaking follows normal no-drop rules. Removal lowers city pressure by 25, ends an active surge and suppresses unrelated district spawns for 6,000 ticks (five minutes). Other intact nests continue spawning.

Only new city placement gains nests. Existing cities, player construction, emptied caches and broken blocks are not rebuilt. Existing unopened lazy-loot caches use the updated loot tables when first opened; already-opened caches are never refilled. Managed infected can appear in the known bounds of existing cities as well as new ones. Explore a wholly fresh site for the complete nest loop.

### Bounded local pressure

One saved pressure value belongs to each 512-block grid cell's 64 by 80 city footprint; nearby players share it. Accepted combat adds 6, an explosion adds 15, and block breaking adds 3, throttled to one contribution per city per second. Pressure caps at 100 and decays by one every 100 quiet ticks. At 25, players receive warning feedback; at 50 the district attempts two spawns per five seconds and nests shorten their interval. At 75 a surge can begin: three district attempts per five seconds for at most 600 ticks, with 2,400 ticks between surge starts.

Each city allows at most 12 nearby living infected, and Forsaken allows at most 64 loaded infected. Every district and nest shares a world budget of eight attempts per second, including failed attempts. District spawning checks players once per five seconds and deduplicates their cities. Normal nest intervals are 800 active ticks, falling to 400 at pressure 50, with at most two placement attempts per interval. Only loaded chunks are eligible. Natural candidates stay 12 blocks from players; nest candidates stay four blocks away. Creatures require clear space, solid support, the world border, a nearby living Survival player and enabled mob spawning. Peaceful difficulty stops spawning.

Pressure, suppression and surge timestamps survive saves/restarts. Unloaded areas decay lazily using elapsed world ticks; time while the server is stopped does not count. Nest timers reset on reload, so missed waves are never replayed. Saved pressure entries are pruned when quiet and expired, with a defensive 4,096-entry limit. Spawning counts loaded entities at most once per active spawn second; it never loads terrain to count or create infected. These are initial balance defaults, not a completed large-server performance claim.

### Contamination and protection

Infected hits, spore impacts and standing within three blocks of an active nest cause a non-stacking Contamination effect: 15% slower movement for up to 1,200 ticks. A later exposure can refresh the timer. Completed honey or milk consumption cures it; successful extraction clears it before the player's travel save. Native death clears effects. There is no permanent mutation or Overworld infection spread.

Each equipped Quarantine Armor piece shortens new exposure by 300 ticks. All four pieces prevent new contamination entirely; equipping them does not retroactively cure an existing effect. Armor uses iron-tier defense (2/6/5/2), durability factor 18, and iron repair materials. The authored yellow sealed suit has a dark visor, paired respirator filters and a rear scrubber.

### Resource and equipment loop

| Resource | Source | Current use |
| --- | --- | --- |
| Contaminated Alloy | Guaranteed 1–3 in salvage caches | Modules and scanner |
| Biofiber | Player-killed infected: 1, or 3 from Brutes; medical caches: 1–3 | Modules and armor smithing |
| Parasitic Core | One per destroyed nest | Modules and scanner |
| Research Data | Guaranteed 1–3 in annex and rooftop caches | Modules and scanner |
| Quarantine Module | 20% extra roll in medical/research/rooftop caches; also craftable | Armor smithing template |

The module recipe is `BAB / DCD / BAB`: B = Biofiber, A = Alloy, D = Data, C = Core. Smithing uses one module, an iron armor piece and one Biofiber to make the corresponding quarantine piece. The native smithing transform preserves damage, names, enchantments and other components. The scanner recipe has rows ` A `, `DCD`, and ` R `: A = Alloy, D = Data, C = Core, R = Redstone.

Use the Bio-Scanner in Forsaken to report the closest active nest's coordinates and distance. It searches only block-entity maps in at most 25 already-loaded chunks, within a three-dimensional 32-block radius. It has a five-second cooldown and does not reveal unloaded structures or regenerate destroyed nests.

### Authoring and verification

`python tools/assets/forsaken-content.py` generates four creature rigs, their textures and animation clips, quarantine armor geometry, inventory icons, recipes, and nest/effect assets. `python tools/assets/verify-forsaken-content.py` validates the asset contract and renders [the content preview](../art/forsaken/content-preview.png). City generation and route validation continue through the existing city scripts. The preview is authored geometry, not a gameplay screenshot.

Compilation, all 17 targeted Forsaken GameTests, the GeckoLib runtime model/animation bake test, and both asset/route validators passed. Targeted checks cover pressure caps, shared throttling, lazy decay, serialized cooldowns, honey/milk cures, all four armor protection levels, outside-dimension isolation, scanner radius and loaded-chunk behavior, single core drops, permanent template nests, spawn caps/gamerules, smithing components, and the existing travel/city regression tests. See `build/forsaken-tests.log`. No JAR is packaged by this increment. Visual feel, long expeditions and actual multiplayer load remain live acceptance work.

## Increment 4: expedition tools

The Pulse Blade is a native iron-tier sword with 500 durability and a 25% melee bonus against infected. Smith one using a Quarantine Module template, iron sword base and Parasitic Core addition; existing item components survive the upgrade. Main-hand Use charges for 30 ticks, with sparks and a ready chime. Release after charging pulses for 10 damage against visible, non-allied infected within four blocks, capped at 16 targets. A valid pulse costs eight durability and starts a 160-tick cooldown even when no target qualifies. Early release costs nothing. Players, pets and blocks are excluded from the pulse.

Containment Beacons use the recipe `AMA / RDR / AMA`: A = Alloy, M = Module, R = Redstone, D = Data. Place the reusable block in Forsaken, then use one Core for 6,000 ticks of suppression in a three-dimensional 16-block radius. Both nest and district spawn paths consult the same boolean protection query. Existing infected remain, overlaps do not amplify protection, and active top-ups consume nothing. Empty-hand use reports remaining time. Breaking returns one device without its fuel data. Absolute world-time deadlines persist across saves and unloading; server downtime does not advance world time. Queries inspect at most nine already-loaded chunk maps. These devices do not disable unrelated mod or command spawns.

Sneak-use the Bio-Scanner to cycle three persisted modes. Nests retains its original search. Resources and caches finds the nearest dropped exclusive material or unresolved Forsaken cache within 32 blocks; it reads only the loot-table identity, never inventory contents or loot generation. Opened containers stop producing cache signals. Local infestation reports the player's current city pressure, district and remaining surge time. All modes share one 100-tick native cooldown; switching modes cannot reset it. Scanning never loads chunks. The pressure mode reports the current zone, not distant cities.

New textures, handheld blade model, four-part beacon geometry, lit/unlit states, recipes and tooltips accompany these mechanics. See [the tools preview](../art/forsaken/expedition-tools-preview.png). Compilation, all 21 targeted Forsaken GameTests, the GeckoLib asset bake test and the asset validator passed (`build/forsaken-tools-tests.log`). New checks cover pulse charge, targeting, cover, durability, cooldown, melee bonus, beacon persistence/expiry/overlap and drops, both suppressed spawn paths, native recipes, scanner mode data, shared cooldown, loaded-area bounds and untouched cache loot. Live visual feel and multiplayer pacing remain acceptance work. No JAR is packaged by this increment.

## Remaining increments

1. Full expedition review: tune pacing, rewards and multiplayer performance against an agreed player-count/server budget.

Procedural metropolis generation, spreading infection, mutation trees, bosses and Overworld outbreaks remain deferred. Activity-based proficiency is still paused.

## October 14 presentation pass

Research bunkers now have glass observation windows, a two-door pressure-plate airlock, console/screen stations, seating and gate equipment. Saved gate coordinates and the damaged frame, archive and finite chest remain. New generation receives these templates; existing sites are not rebuilt. Eleven resource/tool/armor items now use native cuboid models with depth, and worn quarantine armor has raised respirator, harness, seal and cartridge details. Asset and route previews are under `art/forsaken`; in-game visual acceptance remains pending.
