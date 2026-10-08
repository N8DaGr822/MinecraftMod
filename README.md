# Darkspawn

Fabric mod for Minecraft **26.3**, with inventory upgrades, 16 post-dragon biome bosses, the Endborn, equipment empowerment, and permanent boss-heart progression.

## Cooking Station and meals

Craft a **Cooking Station** using four iron ingots, one campfire, and three planks:

```text
I . I
I C I
P P P

I = iron ingot, C = campfire, P = any planks, . = empty
```

The crafting-table recipe unlocks when you collect a campfire. The station is also available in Creative under Functional Blocks, and its ingredients and meals appear under Food & Drinks. For quick testing, use `/give @s darkspawn:cooking_station`.

Right-click the station and combine up to four ingredients **in any order**, one ingredient type per slot. The output previews the meal, hunger, saturation, effect, and duration before you take it. Taking the result consumes one of each ingredient; shift-click crafts as many as fit in the normal inventory. This first release prepares cooked ingredients immediately, without fuel or a cooking timer. Extra or incorrect ingredients produce no output.

| Output | Station ingredients | Hunger / saturation | Primary meal buff |
| --- | --- | --- | --- |
| Salt | Water bucket | — | Ingredient only |
| Cheese | Milk bucket + salt | 3 / 3.0 | None |
| Herb Steak | Cooked beef + dandelion | 8 / 12.8 | Night Vision, 3:00 |
| Honey Pork | Cooked porkchop + honey bottle | 8 / 12.8 | Regeneration I, 0:15 |
| Beef Wellington | Cooked beef + wheat + red or brown mushroom | 10 / 16.0 | +3 attack damage, 2:00 |
| Fish Chowder | Cooked cod or salmon + milk bucket + potato + bowl | 9 / 12.6 | Water Breathing, 3:00 |
| Chicken Cordon Bleu | Cooked chicken + wheat + cheese | 9 / 14.4 | Two golden absorption hearts, 2:00 |

Buckets and honey bottles return as empty containers. Eating chowder returns its bowl. Unused inputs return to your inventory when you close the station, walk away, disconnect, or the station is broken; overflow drops normally. Each player's open menu owns its ingredients, so the station does not store items between uses or expose another player's inputs. Extra inventory rows remain accessible through the regular player inventory, matching other container screens.

Meals stack to 16 and can be eaten at full hunger to change buffs. **Only one Well Fed meal buff is active at a time**: eating another meal replaces it, including refreshing the same meal. Cheese and vanilla foods do not replace a meal buff. Potions and boss powers retain their own effects and timers. Meal attack damage, regeneration, and absorption can coexist with potion bonuses; vision and breathing use whichever source remains active. Absorption follows Minecraft's shared golden-heart pool and capacity rules.

Meal timers survive saves, reconnects, and dimension changes, pause while offline, and clear on death or when drinking milk. Meal replacement removes only meal-owned effects and attribute modifiers. The five effect variants use vanilla saving and synchronization; vision and breathing checks recognize their dedicated meal effects without overwriting vanilla potion instances. Both client and server must use this updated mod.

Cooking recipes live in `data/darkspawn/recipe` with type `darkspawn:cooking`, support one to four ingredients, and can be overridden by data packs. Meal recipes are station-only; they do not appear in the vanilla crafting-table recipe book. This table is the starter recipe guide. Icons and the station model currently reuse vanilla artwork. Biome dishes, feasts, boss foods, crops, and food-diversity bonuses are future work.

Cooking tests cover recipe decoding and ingredient alternatives, network serialization, rejected recipes, shift-click item conservation, full inventories, container returns, menu closure, independent station sessions, meal replacement, potion preservation, milk, save/load, and effect expiration. The screen's appearance at different GUI scales and real two-client interactions still need live playtesting.

## Inventory sorting

- Press **R** during gameplay, or click **Sort inventory** in the upper-right corner of the Survival inventory screen.
- Rebind the shortcut under **Options > Controls > Key Binds > Darkspawn**. The shortcut does not run while a screen is open, so it will not interfere with chat or recipe searches.
- Sorts the 27 main inventory slots plus all unlocked extra slots by item ID (for example, `minecraft:apple` before `minecraft:stone`). Empty slots move to the end.
- Combines stacks only when their item type and all data components match, respecting their stack limits.
- Leaves the hotbar, armor, offhand, and crafting slots untouched. Put down any item on the cursor before sorting.
- Sorting runs on the server. Requests made while another container is open, while dead or spectating, or against an outdated inventory revision are rejected.

Install Darkspawn and Fabric API on **both the client and the Fabric server**. The sorting controls are unavailable on servers that do not support the feature. Chest sorting, quick dump, and custom slot locks are not implemented yet.

## Inventory upgrades

Craft each upgrade at a crafting table, hold it, and right-click in the air to apply it. Each permanently unlocks one 9-slot row directly below the main inventory. Apply them in order: leather, iron, then diamond. The upgrade item is consumed in Survival; duplicate or out-of-order upgrades are rejected without consuming them.

| Upgrade | Crafting ingredients | Total main slots |
| --- | --- | --- |
| Leather | 4 leather, 4 string, 1 chest | 36 |
| Iron | 8 iron ingots, 1 leather | 45 |
| Diamond | 4 diamonds, 1 iron ingot | 54 |

The hotbar remains 9 slots. The unlocked capacity survives death and is saved per player. Stored items follow the world's normal death and `keepInventory` rules. Picking up items can fill unlocked extra slots when the original inventory is full. Sorting includes them, and recipe-book crafting can find ingredients in them.

Crafting patterns (the recipe book also unlocks these when you collect the tier's material):

```text
Leather       Iron          Diamond
S L S         I I I         . D .
L C L         I L I         D I D
S L S         I I I         . D .

S = string, L = leather, C = chest, I = iron ingot, D = diamond, . = empty
```

Locked rows show the required upgrade. In Creative, click **Extra inventory** to access these rows; closing it and reopening the inventory returns to the Creative catalog. Upgrade icons currently reuse vanilla material textures. Chest and other container screens keep their existing layouts; access the extra rows through the normal inventory screen.

## First boss: Ancient Tree Spirit

The forest encounter is one of 16 implemented boss families. The full roster follows the two original encounter guides below.

- The Ender Dragon must have been defeated at least once **in this world**.
- Craft **Ancient Heartwood** with four logs in the corners, four saplings on the edges, and one dragon's breath in the center.
- Use it on a moss block in an Overworld forest clearing. The ritual requires open sky, a clear 10-by-10 footprint, and 18 blocks of height. It rejects Peaceful mode and a second Tree Spirit within 128 blocks; failed summons do not consume the item.
- The Tree Spirit is rooted in place, with an animated 18-block tree silhouette. It has 800 health for one nearby Survival player, plus 400 per additional nearby player, capped at four players when summoned.
- Root attacks mark the ground for two seconds before erupting. Seed volleys aim at a fixed predicted position and travel as dodgeable projectiles. Ground attacks do not damage players more than six blocks above their impact area.

| Phase | Health threshold | Mechanics |
| --- | --- | --- |
| Rooted Guardian | Above 70% | Marked root eruptions, close-range root sweeps, three-seed volleys |
| Broken Canopy | 70% or lower | Up to four destructible healing saplings, five-seed volleys, exposed heartwood during recovery |
| Ancient Wrath | 35% or lower | Shorter attack intervals, seven-seed volleys, ground attacks followed by aerial volleys |

After phase two begins, healing cannot reverse a phase. Following attacks, the trunk's heartwood band at 8-14 blocks high glows for two seconds and takes 50% extra damage. Destroy the green saplings to stop their healing. Flower forests and cherry groves strengthen sapling healing; dark forests and pale gardens add two seeds per volley. Sub-biomes share one boss-heart identity.

Attacks use particles and projectiles without replacing terrain. With no living Survival players within 128 blocks for 60 seconds of server game time, the encounter ends without rewards and removes its healing saplings. If the area unloads, expiration is checked when it next loads. Boss health, phase, variant, and participant IDs persist when saved. The current model, sounds, and icons use vanilla textures/sounds; visual polish and combat balance still need in-game iteration.

The boss drops **Living Heartwood**. Nearby players who attacked it also receive an individually reserved **Ancient Tree Spirit Heart** unless they have already absorbed one. A repeat kill can replace a lost unused heart; it cannot grant a second permanent upgrade from this boss.

## Second boss: Giant Mutated Wolf

- Craft a **Moonlit Fang** with four bones in the corners, four rabbit hides on the edges, and one dragon's breath in the center.
- After the world's first dragon kill, use it on a **bone block in a taiga clearing at night** (Overworld clock ticks 13000-22999). Regular, snowy, and both old-growth taigas are supported. The fight can continue into daylight.
- The ritual needs open sky and a clear 10-by-10 footprint with 9 blocks of height. Peaceful mode, daytime, obstructed space, and another wolf boss within 128 blocks reject the summon without consuming the item.
- The wolf has 900 health for one nearby Survival player, plus 450 per additional nearby player, capped at four when summoned. It moves around the arena and uses an enlarged wolf model with a raised mane and dorsal crest.

| Phase | Health threshold | Mechanics |
| --- | --- | --- |
| Stalking Predator | Above 70% | Marked pounces, three-shard frost volleys, close-range frost bursts |
| Call of the Pack | 70% or lower | Two summoned pack wolves (four in old-growth taigas), five-shard volleys |
| Winter Frenzy | 35% or lower | Two additional pack wolves, seven-shard volleys, faster attacks, frost volleys after landing a pounce |

Each attack has a two-second particle warning. A pounce commits to a ground point up to 24 blocks away and follows an arc using normal block collision. Ground bursts reach only five blocks above their impact; frost projectiles threaten flying players and aim at a fixed predicted position. Frost hits slow players. The wolf pauses after attacks and takes 25% more damage during that recovery window, shown by a pale-yellow tint. Recovery lasts two seconds in phases one/two and one second in phase three.

Snowy taigas add two frost shards per volley and double the ground burst's slowing duration. Old-growth taigas increase the initial pack wave. Pack members are encounter-only hostile wolves, cannot be tamed, and do not spawn naturally. The encounter summons at most four pack wolves total, or six in old-growth taigas; killing them permanently reduces the pressure. Surviving pack members expire after two minutes or when their boss disappears.

The boss drops an **Alpha Fang** for smithing and reserves a **Mutant Wolf Heart** for each nearby qualifying participant who has not absorbed one. All taiga variants share that heart identity. The same 60-second abandonment timeout and saved phase/participant behavior as the Tree Spirit apply. Reloading cancels an unfinished pounce and gives a fresh warning before the next attack. Attacks do not replace terrain. The model, collision feel, pounce timing, and difficulty still need live playtesting.

## Remaining biome bosses

All 16 boss families are implemented. The 14 encounters below join the Tree Spirit and Mutated Wolf. Each has a distinct animated model, three phases, a craftable summon, a smithing reward, and its own permanent-heart identity. The Endborn is also implemented; broader ecosystems remain deferred.

Every ritual requires the world's first dragon defeat, the correct dimension/biome, non-Peaceful difficulty, and clear space for the boss. Overworld surface rituals need open sky; the Crawler and Queen require underground space. Another living boss of the same family within 128 blocks blocks the ritual. Failed attempts preserve the summon item.

Most sigils use **four outer materials in the corners, four inner materials on the edges, and dragon's breath in the center**. The Watcher Lens has a separate recipe below. The recipe book and summon tooltip describe the requirements.

| Boss / entity ID | Solo HP | Ritual | Outer material / inner material |
| --- | ---: | --- | --- |
| Giant Mutant Zombie / `mutant_zombie` | 1,000 | Hay bale in plains or meadows | Rotten flesh / iron ingot |
| Fossil Tyrant / `fossil_tyrant` | 1,200 | Bone block in desert or badlands | Bone / diamond |
| Thunder Bird / `thunder_bird` | 1,100 | Unoxidized copper block in savanna during a thunderstorm | Feather / lightning rod |
| Titan Boa / `titan_boa` | 1,100 | Moss block in jungle | Vine / emerald |
| Baba Yaga / `baba_yaga` | 1,200 | Empty cauldron in swamp at night | Fermented spider eye / ghast tear |
| Mountain Titan / `mountain_titan` | 1,500 | Chiseled stone bricks in mountains | Iron ingot / amethyst shard |
| Ice Wyrm / `ice_wyrm` | 1,300 | Blue ice in frozen lands | Snowball / prismarine crystals |
| Kraken / `kraken` | 1,500 | Cast into open ocean water at least seven blocks deep | Ink sac / nautilus shell |
| Giant Cave Crawler / `cave_crawler` | 1,100 | Cobweb in cave biomes, or ordinary caves below Y48; excludes Deep Dark | String / spider eye |
| Shadow Creeper Queen / `shadow_creeper_queen` | 2,400 | Sculk in a large Deep Dark cavern | Echo shard / nether star |
| Mycelial Sovereign / `mycelial_sovereign` | 1,400 | Mycelium in mushroom fields | Red mushroom / brown mushroom |
| Netherborn / `netherborn` | 1,500 | Magma in crimson/warped forests, Nether wastes, or basalt deltas | Blaze rod / crying obsidian |
| Soulbound Colossus / `soulbound_colossus` | 1,700 | Soul sand in a Soul Sand Valley | Soul lantern / wither skeleton skull |
| Void Eye / `void_eye` | 1,800 | Obsidian on an outer End island | Special Watcher Lens recipe below |

The summon ID is `<boss_id>_sigil`, the smithing drop is `<boss_id>_essence`, and the heart is `<boss_id>_heart`. Display names are unique: `kraken_sigil` is **Abyssal Bait** and `kraken_essence` is **Kraken Pearl**. Forest and taiga items retain their existing IDs above.

Health increases by 50% of solo HP per additional nearby Survival player, capped at four players and fixed at summon time. Bosses use the synced `darkspawn:boss_max_health` attribute so larger pools exceed vanilla's 1,024 HP limit without changing players or other mobs. Earlier Tree Spirit/Wolf saves migrate their stored pool and current health. Phases advance at **70% and 35% HP** and cannot reverse through healing. Attacks normally warn for two seconds, commit to fixed aim points, and leave a recovery window taking 30% extra damage. Phase three shortens recovery and attack intervals; the Queen's final warning is 1.75 seconds. Ground effects have bounded height while projectiles, beams, lightning, and aerial attacks keep Elytra combat active.

| Boss | Phase one | Phase two | Phase three |
| --- | --- | --- | --- |
| Zombie | Slams and thrown debris | Summoned mutant horde | Charges and aerial follow-up volleys |
| Fossil Tyrant | Bites, sweeps, bone volleys | Charges and destructible bone guardians | Eruptions and aggressive charge/bite rotation |
| Thunder Bird | Marked lightning and volleys | Wind blasts and living storm turrets | Multiple lightning columns and more turrets |
| Titan Boa | Bites and venom | Constriction zones and offspring | Charges between constrictions |
| Baba Yaga | Hex volleys from a walking chicken-legged hut | Poison miasma, healing nodes, hunters | Teleports between hex/miasma attacks |
| Mountain Titan | Slams and rocks | Eruptions and broad sweeps | Lines of delayed ground fractures |
| Ice Wyrm | Frost volleys and bites | Burrowing strikes and blizzards | Burrows leave lingering frost zones |
| Kraken | Four destructible tentacles, waves, ink | Two more tentacles and whirlpools | Two final tentacles and water-jet beams |
| Cave Crawler | Web volleys and bites | Hatchable eggs, web zones, pounces | Poison volleys mixed with pounces |
| Shadow Creeper Queen | Armor-breaking acid, sweeps, darkness | Royal guardians, acid pools, eggs | Shadow storms, charges, turret support |
| Mycelial Sovereign | Mushroom stands up, then spores and root marks | Healing mushrooms and fungal turrets | More turrets, fungal patches, eruptions |
| Netherborn | Regional charge or teleport plus fire/hex volleys | Magma cracks or warped constriction, regional minions | Stronger regional rotation; warped teleports leave tendrils at both ends |
| Soulbound Colossus | Soul volleys and heart-stealing strikes | Soul turrets and beams | Beams overlap persistent soul storms |
| Void Eye | Beams, volleys, four rotating shields | Endermites/Endborn and platform displacement | Void rifts, platforms, and only two shield shards |

Destroy **bone guardians, royal guardians, or Kraken tentacles** to remove their owner's 65% damage reduction. Summon waves have a finite budget of 24 creatures per encounter and 12 living at once. Phase progress saves so reloads cannot repeat waves. Healing nodes pulse every two seconds; turrets and tentacles have their own warnings. Adds expire or are removed with their encounter. Vanilla Endermites retain their normal finite lifetime.

**Kraken and ship combat:** cast Abyssal Bait at water from a boat or shore. The Kraken appears 18 blocks ahead of your horizontal facing direction, with a broad seven-block-deep water check. Its mantle rises above the water. Tentacle strikes and waves damage boats and players; watch the marked zones and steer clear. Tentacles have 120 HP each, with eight total across phases when space permits. Deep oceans widen whirlpools and increase projectile pressure.

**Mycelial Sovereign:** its five-second awakening begins as a crouched mushroom, then limbs emerge and the organism rises. Spore clouds, roots, healing mushrooms, and turrets build pressure. Fungal patches cover the ground with temporary mycelium visuals using encounter entities, preserving player terrain.

**Soulbound Colossus:** successful soul-drain strikes temporarily remove one maximum-health heart, up to five, always leaving at least one usable heart. Destroy a soul cage to recover one. Remaining stolen hearts return when the effect expires after 30 seconds or the encounter ends; permanent boss-heart progress is never deleted. Soul Siphon weapons can also release one stolen heart per cooldown.

**Void Eye:** rotating shards reduce projectile damage within their arcs; recovery opens a damage window and phase three removes two shards. Platform displacement requires a broad, solid island floor. Platforms wait until unoccupied before expiring, and displaced players receive Slow Falling. The boss never deliberately teleports players over empty void.

Variants share the family's heart reward. Meadows, badlands, windswept savannas, bamboo jungles, mangrove swamps, jagged peaks, ice spikes/frozen peaks, deep oceans, lush caves, and small End islands increase projectile pressure. Crimson Netherborn charges hit harder; warped Netherborn uses teleportation, hexes, and tendrils; wastes/delta Netherborn uses larger fire volleys and ranged minions. Biome membership is editable through `<boss_id>_biomes` data tags.

Encounters end after 60 seconds with no living Survival player within 160 blocks of their arena. State and participants save across reloads; unfinished attacks restart with a warning. Nearby participating players receive individually reserved hearts on victory, and the boss drops one smithing material. Attacks do not permanently change blocks.

## Endborn

The Endborn is a tall hostile humanoid with an Enderman-like body and crown. It has 60 HP, teleports toward distant targets, and can dodge a projectile when its five-second blink cooldown is ready. After the world's dragon defeat, it can spawn naturally in End highlands, midlands, and barrens in small groups.

Natural Endborn killed by a player drop an **Endborn Shard** and an ender pearl. The Void Eye's summoned Endborn expire with the encounter and do not drop these materials. Craft the **Watcher Lens** from four obsidian, one eye of ender, two Endborn Shards, one dragon's breath, and one End crystal:

```text
O E O
S D S
O C O

O = obsidian, E = eye of ender, S = Endborn Shard,
D = dragon's breath, C = End crystal
```

## Boss hearts

Hold a boss heart and right-click to absorb it. Progress belongs to the player and survives death, reconnects, and server restarts.

| Distinct boss hearts absorbed | Permanent reward |
| --- | --- |
| 1-10 | One normal heart each, increasing the vanilla 10 hearts to 20 |
| 11-15 | One golden absorption heart of capacity each, up to five |
| Duplicate boss or beyond the cap | Rejected without consuming the item |

Golden hearts absorb damage first. Earned golden capacity refills after **60 seconds without taking damage**; taking damage, joining, or respawning restarts that delay. Golden-apple absorption continues to work, and refill never reduces a stronger active absorption amount. Normal extra hearts use vanilla health regeneration.

The refill delay is defined by `BossHeartProgress.REFILL_DELAY_TICKS`. Bonuses use named attribute modifiers, preserving other mods' and potions' attribute changes. The cap applies to this progression system's contribution.

All 16 boss hearts are obtainable through their encounters. Fifteen distinct hearts finish health progression, leaving a choice among the 16 families. Legacy Leviathan hearts and saved progress migrate to the Kraken identity and cannot grant a duplicate upgrade.

## Equipment empowerment

At a smithing table, combine **amethyst shard** (template slot), a **melee weapon** (base slot), and one boss drop (addition slot).

| Boss drop | Weapon power |
| --- | --- |
| Living Heartwood | **Rootbound:** direct melee hits inflict Slowness III for two seconds |
| Alpha Fang | **Predator's Rush:** direct melee hits grant the attacker Speed II for four seconds |
| Mutant Marrow | **Undying Fury:** Melee hits grant Strength I for 3 seconds. |
| Tyrant Tooth | **Armor Rend:** Melee hits reduce enemy armor by 4 for 5 seconds. |
| Thunder Quill | **Stormchain:** Melee hits arc 4 magic damage to up to 3 nearby monsters. 2-second cooldown. |
| Venom Heart | **Venomfang:** Melee hits inflict Poison II for 4 seconds. |
| Hexbound Eye | **Hexweaver:** Melee hits inflict Weakness II for 5 seconds. |
| Titan Core | **Stoneguard:** Melee hits grant Resistance I for 4 seconds. |
| Glacial Fang | **Winterbite:** Melee hits inflict Slowness IV for 3 seconds. |
| Kraken Pearl | **Tidecaller:** Melee hits grant Water Breathing and Dolphin's Grace for 8 seconds. |
| Silken Fang | **Broodmark:** Melee hits mark enemies with Glowing for 10 seconds and Poison I for 4 seconds. |
| Royal Shadow Gland | **Shadowbane:** Melee hits inflict Wither II for 4 seconds and grant Night Vision for 30 seconds. |
| Sovereign Spore | **Mycelial Mending:** Melee hits grant Regeneration I for 4 seconds. |
| Netherborn Organ | **Netherflame:** Melee hits ignite enemies for 4 seconds and grant Fire Resistance for 5 seconds. |
| Freed Soul | **Soul Siphon:** Melee hits restore 1 heart and release 1 stolen soul-heart. 4-second cooldown. |
| Void Iris | **Voidstep:** Melee hits grant Speed III for 2 seconds and Slow Falling for 5 seconds. |

The weapon keeps its item type, enchantments, name, durability damage, and unrelated components. Each item has one boss-power slot: applying a different power replaces the current one, and reapplying the same power is rejected. Only successful direct melee damage triggers these effects. Repeated timed effects refresh their duration without stacking strength. Stormchain and Soul Siphon have explicit cooldowns. Recipe data validates power names and synchronizes them to clients.

## Ecosystems afterward

The requested scope of **all 16 bosses plus Endborn** is implemented. Ambient mobs, specialist enemies, heralds, environmental clues, and discovery/ritual structures remain future work. The supplied ecosystem concept notes are preserved in `docs/boss-ecosystem-notes.txt`; summon items remain the agreed way to begin boss fights.

Models are custom animated cuboid silhouettes using vanilla textures, sounds, and item icons. They still need visual and balance iteration in a live client; headless checks do not establish combat feel or art polish.

## Setup

Use **JDK 25** and the included Gradle wrapper. From this folder in PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

`build` compiles common/client code, runs unit tests, and starts a headless Minecraft server for encounter GameTests. It does not open a game window. The distributable mod is `build/libs/darkspawn-1.0.0.jar`; the `-sources.jar` file is for development.

The tests cover stack limits, item counts and component data, protected slots, empty/full inventories, repeat sorting, and 200 reproducible randomized inventories. They also check upgrade capacity, locked menu slots, extra-slot pickups, inventory copying, save/load, crafting recipes, and client mixin injection targets without opening a game window. Boss tests cover unique-heart limits, refill timing, saved progression, phase thresholds, smithing preservation and power replacement, recipe network round-trips, ritual night boundaries, recipe decoding, and model loading. Roster tests also decode every new summon/empowerment recipe and advancement, resolve biome IDs, bake/animate models, and verify ocean-heart migration. Run unit tests with `.\gradlew.bat test`. Run server checks with `.\gradlew.bat runGameTest`; these exercise phases, save/load, projectiles, minion cleanup, soul cages, and melee events. AI smoke tests call server callbacks directly and do not replace movement or multiplayer playtesting.

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## In-game verification

The visual layout, upgrade use, death/respawn, and multiplayer behavior still need a live game check:

1. Use a new test world and place mixed items, partial stacks, named items, enchanted gear, and damaged tools in the main inventory.
2. Sort using R with the inventory closed, then using the button with it open. Check order, total counts, item data, and unchanged hotbar/equipment/crafting slots.
3. Check the button with the recipe book open and closed, at different GUI scales, and with an item on the cursor. Confirm typing in chat or recipe search does not sort.
4. Test on a Fabric server with Darkspawn installed on both sides. Try repeated sorting and sorting immediately after moving or picking up items; an outdated request should ask you to sort again.
5. Join a server without Darkspawn and confirm the button is disabled and the shortcut causes no disconnect.
6. Craft and apply leather, iron, and diamond upgrades in order. Check that each unlocks exactly one row. Try reusing a tier and applying diamond before iron; neither should consume the item. For quick Creative testing, use `/give @s darkspawn:leather_inventory_upgrade` (and the corresponding iron/diamond IDs).
7. Fill all original slots, then pick up items and damaged tools. Move items between the extra rows, main inventory, hotbar, and equipment using clicks, shift-clicks, number keys, and dragging. Sort again and check counts and item data. Check the Creative catalog and its **Extra inventory** button too.
8. Put named items in the last extra slot, reconnect, restart the server, and travel between dimensions. Verify the items and capacity persist. Test death with `keepInventory` both off and on: capacity should persist in both cases, while items follow that rule.

## Boss in-game verification

Use a disposable test world for the first encounter checks:

1. Before defeating the dragon, attempt the moss ritual and confirm it fails without consuming the summon. After defeating the dragon, test valid/invalid biomes, obstructed clearings, Peaceful mode, and duplicate summons.
2. Fight on foot and with Elytra. Check the two-second telegraphs, projectile collisions, ground-attack height, healing saplings, phase transitions, and glowing recovery windows. Verify attacks do not alter blocks.
3. Rejoin/restart mid-fight and verify health/phase persist. Leave the area and return after the abandonment timeout; check that no healing saplings or boss bars remain.
4. Fight with two players. Both should receive their own usable heart after contributing damage and remaining nearby. Use one heart, repeat the fight, and confirm the same boss cannot increase that player's health twice.
5. Smith a named, enchanted, damaged weapon. Confirm all original data survives, Rootbound appears in the tooltip and slows melee targets, and a second application is rejected.
6. Check health upgrades after death with `keepInventory` both off and on, reconnecting, restarting, and changing dimensions. Use `/give @s darkspawn:<boss_id>_heart` to accelerate testing of the full 20+5 cap.
7. Damage golden hearts and verify they refill only after 60 seconds without damage. Take damage just before the timeout, then test golden apples alongside the earned absorption.

For direct encounter testing, `/summon darkspawn:ancient_tree_spirit` bypasses the player ritual like vanilla summon commands. It uses the default solo health and forest variant. `/give @s darkspawn:ancient_heartwood` tests the normal ritual instead.

### Wolf in-game verification

1. Craft a Moonlit Fang and test the bone-block ritual in all four taigas. Check day/night boundaries, wrong biomes, blocked space, pre-dragon worlds, and duplicate summons. Confirm rejected attempts preserve the item.
2. Fight on foot and while flying. Dodge marked pounces, interrupt their path with obstacles, and verify the boss collides with terrain instead of teleporting through it. Check frost projectile collisions, ground-burst height, and the recovery damage window.
3. Cross 70% and 35% health, including a single hit that skips a phase. Count the finite pack waves; repeat in snowy and old-growth taigas to check their variations. Verify surviving pack members disappear after victory or abandonment.
4. Save/reload during a pounce and during each phase. Confirm no surprise landing damage after reload, no repeated pack wave, and no lingering boss bar after the encounter ends.
5. Fight alongside another player and absorb the forest and taiga boss hearts. Confirm each boss grants exactly one permanent heart per player, including across sub-biome variants.
6. Smith Rootbound onto a named, enchanted, damaged weapon, replace it with Predator's Rush, then replace it back. Check data preservation, no same-power reapplication, four-second Speed II on successful melee hits, and no speed reward from arrows or blocked hits.

For direct testing, `/summon darkspawn:mutant_wolf` uses solo health and the regular taiga variant. `/give @s darkspawn:moonlit_fang` tests the full ritual, scaling, and biome variant selection.

### Full-roster in-game verification

1. Use `/give @s darkspawn:<boss_id>_sigil` and follow the ritual table. Check invalid biomes, dragon progression, obstruction, weather/night requirements, failed consumption, and two-player scaling. `/summon darkspawn:<boss_id>` bypasses ritual checks for direct testing.
2. Check silhouette scale, textures, warning placement, phases, recovery tint, and movement on foot and with Elytra. The Queen needs a large cavern.
3. Fight the Kraken from boats. Verify clear summoning placement, targetable tentacles, finite waves, visible whirlpools, and boat damage.
4. Watch the Sovereign stand up, destroy healing mushrooms/turrets, and verify temporary mycelium disappears without changing island blocks.
5. Let the Colossus steal hearts, break cages, wait out the effect, then leave/reload/end the encounter. Permanent health progress should remain.
6. Check Void Eye shield arcs, End reinforcements, safe platform landing/expiry, and rift warnings. Natural Endborn should drop shards; summoned ones should not.
7. Save/reload every phase, abandon encounters, and repeat kills with two players. Check cleanup, one heart per family, no repeated phase waves, and all powers on named/enchanted/damaged weapons.

Both client and server must use the updated jar for the new entities, effects, items, and smithing recipe synchronization.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
