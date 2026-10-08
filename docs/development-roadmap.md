# Darkspawn development roadmap

## Direction and current baseline

The design goal is a post-dragon world whose ecosystems connect exploration, farming, cooking, equipment, and boss progression. Preserve the existing 16 boss families, heart progression, inventory upgrades, smithing powers, and Cooking Station. The original expansion brief is in [progression-expansion-notes.txt](progression-expansion-notes.txt); creature concepts are in [boss-ecosystem-notes.txt](boss-ecosystem-notes.txt).

The first approved milestone is **the forest ecosystem, a new ingredient, and a meal**. Later milestones below are the accepted direction; detailed balance values and optional behaviors remain design targets until implemented and tested.

The follow-up ideas received on 2026-10-08 are saved in [future-expansion-notes.txt](future-expansion-notes.txt) and indexed under **Deferred expansion backlog** below. They are reserved for later and do not replace the active ecosystem/cooking/boss-reward/progression sequence. After forest and taiga, the user approved completing all remaining boss ecosystems. That roster is implemented below; live-client checks remain on the release checklist.

## Rules that apply to every milestone

- Bosses remain player-summoned. The world's first Ender Dragon defeat unlocks ecosystem spawning; a player's personal dragon achievement is not the gate.
- Support existing saves and already-generated biomes. Start with native spawn tables and biome tags; do not rewrite terrain or retrofit structures into player builds.
- Preserve existing recipes and progression. Add alternatives rather than replacing summon requirements or repurposing existing item IDs.
- One primary meal buff and one equipment power per item. Potions retain their own effects; a feast's combined benefits still occupy one meal slot.
- Reward dietary variety without reducing any food's normal hunger or saturation value.
- Use existing registration, recipe, rendering, networking, and test patterns. Introduce shared machinery when multiple implemented features require it.
- Keep crafting, harvesting, unlocks, combat, and rewards authoritative on the server. Test repeat requests, full inventories, save/load, death, and two-player ownership where relevant.

## Delivery sequence

| Milestone | Scope | Depends on | Completion criteria |
| --- | --- | --- | --- |
| 1. Forest ecosystem | Barkling, Hollowed, Rootcrawler, Ancient Ent; Wild Herbs; Woodland Stew | Existing bosses and cooking | Post-dragon forest spawns, neutral harvesting, readable attacks, renewable ingredient, working meal, tests, and playtest checklist |
| 2. Regional ecosystems and influence | Roll out taiga/plains/desert/savanna, then jungle/swamp/mountains/frozen/ocean, then caves/Deep Dark/mushroom/Nether/End | 1 | Each region has common and specialist roles, a rare herald or discovery equivalent, useful drops, and restrained spawn density; atmospheric cues do not automatically summon bosses or damage terrain |
| 3. Ingredient families and agriculture | Onion, garlic, tomato, rice, corn, pepper, butter; reuse salt, cheese, and herbs; farmer seed trades; later biome crop variants | 1; regional sources from 2 | Every ingredient has a renewable acquisition loop and planned uses in several dishes; test growth, harvesting, replanting, and biome variant probabilities |
| 4. Regional and everyday cuisine | Signature meals, utility foods, desserts, livestock niches, and meal-specific effects | 2–3 | Forest/taiga/plains/desert/jungle/swamp/mountain/frozen/ocean/mushroom/Nether/End dishes; noncombat choices for miners, builders, farmers, fishers, and explorers; one-meal replacement and potion coexistence tests |
| 5. Boss cuisine | Renewable culinary drops and premium temporary meals for repeat kills | 2–4 | Culinary rewards remain separate from hearts and smithing drops; no rewards from temporary encounter minions or abandoned fights; multiplayer ownership and repeat-kill tests |
| 6. Advancements and dietary variety | Darkspawn progression page; summon/kill/heart milestones; Varied Diet and meal collection | 4–5 | Save-compatible player tracking, correct killer/participant attribution, no duplicate heart rewards, no loss of baseline food value, and clear progress in the UI |
| 7. Trophy rooms | Decorative trophies for all 16 boss families | 2, 6 | Placeable, collectible boss-specific rewards with no combat advantage; correct block loot and multiplayer behavior |
| 8. Village kitchens | Chef profession using the Cooking Station; ingredient, meal, and scroll trades | 3–4; scrolls integrate with 10 | Correct job-site claiming, trade levels/restocking, no station ingredient sharing, balanced renewable trades |
| 9. Feasts | Six-serving Hunter's Feast first, then Woodland/Ocean/Nether/Ender/Hero variants | 4–5 | One serving consumed per accepted server interaction, one meal buff per diner, saved serving count, correct break behavior, and concurrent dining tests |
| 10. Cookbook and discoveries | Categorized recipes, ingredient hints, recipe scrolls, structure loot, completion rewards | 4–6; Chef integration from 8 | Server-enforced unlocks for new rare recipes; existing recipes stay available; persistent player discovery; useful locked previews; loot source coverage without retroactive terrain edits |
| 11. Equipment beyond melee | Shields, bows, then armor using existing boss materials | 2, 5 | One power per item; preserve name, enchantments, damage, and components; explicit rules for multiple armor pieces; projectile/block/equip/unequip/death tests |
| 12. Integrated release | Spawn rates, food economy, encounter balance, accessibility, artwork, and upgrade testing | Each completed milestone | Full build, unit and server tests, client visual checks, two-player playtests, existing-world upgrade checks, and release notes |

Milestone 12 is also a release gate for each playable increment. Expand one regional group at a time rather than registering unfinished mobs across the whole world.

## Milestone 1: forest implementation contract

Status: **implemented; live-client review pending**. The forest checkpoint passed the full Gradle build with 48 unit tests and 22 server GameTests, including seven forest server tests and the forest recipe/model checks. Later combined verification is recorded under the regional increments below.

### Creatures

| Creature | Role | First implementation |
| --- | --- | --- |
| Barkling | Common neutral sapling | Wanders, freezes near a player while unprovoked, retaliates with thrown sticks, and allows peaceful herb harvesting with shears |
| Hollowed | Armored specialist | Slow wooden humanoid; vulnerable to axes and fire; successful melee briefly slows its target |
| Rootcrawler | Ground-attack preview | Low root silhouette; marks a fixed nearby ground point before roots erupt, allowing the player to move out of danger |
| Ancient Ent | Rare herald | Larger forest creature with a wider telegraphed root strike; player kills grant the existing Ancient Heartwood summon item as an alternate acquisition route |

All four use the existing `darkspawn:tree_spirit_forests` biome tag and the global dragon gate. Barklings use the creature cap and bright conditions; the other three use the hostile cap, dark spawning, and non-Peaceful difficulty. Restrict natural spawns to the forest surface on soil/moss. Command summons remain available for testing. Neither burrowing visuals nor root attacks replace blocks.

Do not reuse the Tree Spirit's encounter-only healing saplings as persistent ecosystem mobs: their owner and expiration rules have a different purpose. Shared rendering and registration should follow the existing biome-boss approach, with separate entity IDs and distinct silhouettes for these creatures.

### Forest-to-kitchen loop

1. After defeating the dragon, explore a supported forest.
2. Use shears on an unprovoked Barkling to obtain **Wild Herbs** without killing it. Harvesting costs one shear durability and has a saved five-minute world-time regrowth cooldown. Hostile forest creatures can also drop herbs when killed by a player.
3. Combine **red or brown mushroom + carrot + Wild Herbs + bowl** at the Cooking Station to make **Woodland Stew**.
4. The stew restores 8 hunger and 12.8 saturation, grants the existing Regeneration I meal effect for 30 seconds, replaces another meal normally, and returns its bowl when eaten.
5. Defeat an Ancient Ent for an alternative Tree Spirit summon. Existing summon crafting, boss rewards, heart identities, and smithing remain unchanged.

Onion is deferred to agriculture. This first stew remains a valid recipe after onions and more advanced regional dishes arrive. Wild Herbs should gain additional uses in those later milestones.

### Verification

- Registrations, attributes, model baking/animation, localized names, item assets, and recipe decoding.
- Spawn predicates reject pre-dragon worlds, wrong dimensions/biomes, underground sites, and unsuitable light/difficulty; biome modifications use the intended mob caps and group sizes.
- Barklings do not acquire unprovoked targets; retaliation and harvesting have separate rules; repeated harvesting cannot bypass the cooldown or lose items when an inventory is full.
- Root strikes have a warning, a fixed destination, finite range/height, and obstruction checks; reload cancels unfinished strikes and gives a fresh warning.
- Player-only drops, herb cooldown persistence, and the Ancient Ent's alternate summon reward.
- Woodland Stew recipe, bowl return, and one-meal replacement.
- Full existing regression suite. Manually check silhouettes, warning visibility, natural encounter frequency, shearing feedback, and two-client interactions in a disposable post-dragon world.

### Verification record

- Common and client sources compile. All four forest models bake and animate without a game window; referenced vanilla textures exist.
- Seven server tests cover neutral/retaliation AI, wooden weaknesses, shared herb harvesting, durability, overflow, regrowth save/load, root dodging/cover/elevation/reload, player-only Ent rewards, cooking/eating, and spawn gates.
- Both mushroom alternatives decode and craft Woodland Stew. Server tests verify ingredient consumption, hunger/saturation, bowl return, and replacement of the prior meal buff.
- Spawn tests use the real End fight state and forest biome, then check wrong dimensions, biome, ground, surface height, daytime hostile rejection, bright Barkling spawning, and Peaceful mode. Trial-spawner context isolates progression/terrain checks from random hostile light checks; live night spawn density remains a playtest item.
- A two-line test bootstrap adjustment aligns Java service loading with Fabric's mod classloader, allowing mod service providers to load correctly in the unit suite.
- Full `gradlew.bat build --console=plain`: passed on 2026-10-08 with 48 unit tests and 22 server GameTests. The Windows runner used a process-local `jdk.net.unixdomain.tmpdir` pointing to `build/sockets`.
- Remaining release checks: live-client visuals, natural encounter balance, and real two-client feedback. Forest models and cooking continue to use the established rendering and meal patterns.

## Milestone 2, first increment: taiga ecosystem

Status: **implemented; automated verification passed; live-client review pending**. This continues the active regional ecosystem roadmap; the deferred brief below does not expand this increment.

- Dire Wolf: hostile pack predator hunting players, rabbits, and sheep; 28 HP. Nearby visible ecosystem wolves grant a single 10% movement-speed bonus within eight blocks, rechecked once per second.
- Frostfang: 30 HP; successful bites apply Slowness I for three seconds. Uses vanilla powder-snow walking support and is weighted more heavily in snowy taiga.
- Ravaged Wolf: 44 HP; at or below 35% health while targeting prey, enters a five-second frenzy with +35% movement speed and +2 attack damage, with a fifteen-second trigger cooldown.
- Alpha Dire Wolf: rare 90 HP herald. A one-second warning marks a fixed pounce destination three to eight blocks away; a low collision-aware leap previews the boss mechanic. Howls buff visible nearby ecosystem wolves with Speed I for five seconds, on a ten-second cooldown.
- Reinforcements: at most two Dire Wolves once per Alpha lifetime, with a local six-wolf cap and safe placement. Called wolves last at most thirty seconds, disappear after their owner is lost, grant no drops/XP, and cannot call more wolves. Ownership, expiry, and the Alpha's one-call limit survive reloads; a pending pounce is canceled on reload.
- Natural spawning: the existing taiga biome tag, global dragon defeat, Overworld surface, suitable soil/snow, hostile lighting, non-Peaceful difficulty, and native monster cap. No automatic boss summons or terrain changes.
- Rewards: player kills yield bones, or rabbit hide from Ravaged Wolves. The Alpha additionally drops the existing **Moonlit Fang summon**. The original concept's **Alpha Fang** name already identifies a boss smithing reward; preserve that exclusive boss reward and its existing recipe. No herald hearts.

Use the existing forest registration, state synchronization, server tests, and native wolf-model conventions. Keep encounter-only Frost Wolves separate from persistent ecosystem wolves. Custom art/audio, taming, additional minibosses, and new taiga cuisine are separate later increments.

### Verification record

- Full `gradlew.bat build --console=plain`: passed on 2026-10-08 with 50 unit tests and 32 server GameTests. The Windows runner used the same process-local socket-directory workaround as the forest checkpoint.
- Nine new taiga server tests cover player/rabbit/sheep targeting, pack-bonus removal and reload, successful-hit-only Frostfang slowing and native powder-snow walking, frenzy duration and saved cooldowns, Alpha howl/reinforcement limits and cleanup, helper reward suppression, pounce warning/dodge/collision/reload, player-only herald rewards, and natural spawn gates.
- One new model test bakes and animates all four native wolf models. Common and client sources compile; the existing forest, cooking, inventory, and boss regression suites also pass.
- Biome registration gives Frostfang a spawn weight of 12 in snowy taiga and 3 in other supported taigas. Encounter frequency remains a playtest item rather than an automated probability assertion.
- Remaining release checks: live-client movement and silhouettes, audio/particle readability, natural pack frequency, existing-world behavior, and two-player encounters. Headless pounce checks exercise the warning, collision, and damage rules; they do not replace a client movement playtest.

## Milestone 2, remaining regions: full ecosystem roster

Status: **implemented; full regression build passed; live-client review pending**. The approved scope is the remaining ecosystems, not a replacement of the existing 16 boss encounters or activation of the deferred expansion backlog.

The 14 remaining regions add 74 independently registered creatures/discoveries, 17 discovery/ritual ingredients, 14 alternative summon recipes and their recipe-book unlocks. Alongside forest and taiga, this completes the 82-entry ecosystem roster described by the active creature brief. The existing Endborn remain separate. The [player guide](../README.md#remaining-regional-ecosystems) lists each roster, acquisition route, and testing commands.

| Region | Roster / implementation focus | Alternate discovery |
| --- | --- | --- |
| Plains | Five undead variants; knockback resistance, wooden-door opening, warned slam/pounce, debris projectiles | Failed Mutant: Mutated Flesh |
| Desert / badlands | Raptor, Scorpion, Vulture, Husk, excavatable Skull; poison, scavenging, physical pounce | Pickaxe interaction: Ancient Tyrant Fossil |
| Savanna | Finch, Shocktalon, Stormstrider, Roc; storm particles, diving, electrical projectiles | Charged Feather; exposed-copper thunder ritual makes Stormforged Feather |
| Jungle | Viper, Constrictor, Stalker, Brood Serpent; climbing, venom, ambush, fixed constriction zones | Titan Scale |
| Swamp | Bogling, Frog, Wisp, Hexbound, Familiar; saved loose-item carrying, curses, follower-owned discoveries | Follow Familiar: Witch Token |
| Mountains | Goat, Crawler, Stoneborn, Titan Spawn; projectile resistance, ambush, boulders/slams | Titan Stone |
| Frozen | Frostling, Husk, Icefang, Young Wyrm; snow walking, ice attacks, fire weakness, low-health retreat | Wyrm Scale |
| Oceans | Abyssal Fish, Crab, Siren, Leviathan Spawn; swimming/breathing, seabed restriction, rear weakness, pulls/boat attacks | Abyssal Scale, mapped to Kraken |
| Caves | Skitterer, Spitter, Carrier, Widow; climbing, finite hatchlings, temporary webs, physical pounce | Brood Fang |
| Deep Dark | Drone, Stalker, Spitter, Guardian, Larva, Egg, Praetorian; observation, darkness, territory, bounded lifecycle | Shadow Membrane; Royal Shadow Gland stays boss-exclusive |
| Mushroom | Sporeling, Crawler, Infected Mooshroom, Sporewalker, Guardian; hive retaliation, reactive spores, mycelium healing | Mycelial Heart Fragment |
| Nether | Three regional groups plus one herald per group; charges, buffing, snares, stare reaction, blinks, magma | Crimson + Warped + Infernal Organ |
| Soul Sand Valley | Soul, Bonewalker, Skull, Tormented, Keeper; saved one-time revival, temporary health loss, finite helpers | Captured Soul |
| End | Voidling, Grazer, Shardling, Ray, Watcher, Sentinel; passive wildlife, safe teleports, projectile dodge, rotating shield | Watcher Fragment, mapped to existing Watcher Lens |

### Implementation decisions

- Follow the existing `BossProfile`/`BiomeBoss` pattern for the remaining roster: `RegionalKind` holds identities and attributes, `RegionalEcosystems` registers biome spawns and materials, and `RegionalMob` implements the shared lifecycle and variant behaviors. Existing forest/taiga entities and boss encounter classes retain their behavior. The shared bolt accepts regional owners, ignores their regional allies, and notifies a regional caster after a successful hit for limited secondary effects.
- Use the world's saved dragon state, dimension, biome, habitat, light/difficulty, and native mob caps. Nether variants use disjoint sub-biomes. Ocean creatures use native water navigation; flying creatures use flight navigation/control; cave spiders use wall-climbing navigation. Mushroom hostiles explicitly support post-dragon daylight spawning on mycelium.
- Preserve every existing summon recipe, ritual, heart identity, and smithing reward. Herald discoveries plus dragon's breath provide alternatives; Nether exploration requires three different organs. Material IDs avoid the existing Alpha Fang, Royal Shadow Gland, and Watcher Lens identities. No herald grants permanent hearts.
- Reuse native textures and animated cuboid rendering. Each creature has its own entity ID and dimensions; body forms cover humanoids, quadrupeds, birds, serpents, arthropods, aquatic creatures, colony castes, fungi, spirits, and eyes. Custom artwork/audio remains a later quality pass.
- Keep terrain stable. The Skull is a rare surface discovery entity; Familiar/Wisp destinations use reachable particle markers. Grazer feeding, burrowing, fungal growth, webs, magma, shadows, and small rifts are poses/effects. Large skull structures, witch altars, nests, tunnels, shed skins, colony architecture, and alternative structure-based boss rituals remain in the already-deferred world-generation work. Brutes open wooden doors instead of deleting them; familiars reward following rather than killing.
- Reinforcement limits, ownership/expiry, one-time rewards, reconstruction, and carried items persist. Helpers cannot multiply or grant rewards. Attack markers stay fixed, movement collides normally, and reload cancels pending attacks/patches. Soul previews use a separate nonstacking five-second effect that leaves at least one usable heart and does not edit saved heart progression.

### Verification record

- Full `gradlew.bat build --console=plain`: passed on 2026-10-08 with **53 unit tests and 57 server GameTests**, including three new regional unit tests and 21 regional server tests. This is the combined workspace regression result, including the existing boss, forest, taiga, cooking, and inventory suites. The Windows runner used the process-local `jdk.net.unixdomain.tmpdir` socket-directory workaround.
- All 74 creature models bake and animate; referenced vanilla textures resolve. Every regional discovery maps to the correct existing summon recipe, and original recipes remain decodable.
- Server coverage exercises real entity initialization/AI, save/load, regional rewards, helper suppression/limits/expiry, hatching and maturation, reconstruction, temporary health restoration, fossil interaction, follower ownership, loose-item conservation with mob drops disabled, warned strikes, physical collision, temporary patches, habitat predicates across all three dimensions, storm forging, neutral behavior, and synchronized Sentinel shield facets/gaps.
- Habitat checks use immediately updated height maps for cover and actual overhead water depth for the rare ocean serpent. They do not depend on queued sky-light updates or a particular world generator's sea-level constant.
- Manual release checks still include native spawn density, complete traversal/flight/swimming behavior, silhouettes and cues, generated/old-world exploration, and real two-client combat/discovery feedback. Headless tests establish logic and asset validity, not live combat feel or final artwork quality.

## Decisions to settle at their milestone

- **Custom meal identities:** prototype Hearty (20% slower hunger drain), Hunter's Instinct, Surefooted, Iron Stomach, Deep Breath, and Energized. Define each precisely and test interactions before promising exact percentages.
- **Variety:** use a bounded window of recent eating events and count distinct foods within it. Merely remembering the last eight distinct foods would become a permanent bonus after eight discoveries. Candidate tiers are 3/5/8, with +5%/+10% bonus saturation and modest full-hunger regeneration; never subtract normal food value. Track lifetime distinct meals separately for the 20-meal advancement.
- **Boss ingredients:** begin with Ancient Sap and Prime Beast Meat, then the brief's Marrow Extract, Charged Egg, Titan Meat, Wyrm Fat, Kraken Meat, Sovereign Cap, Infernal Marrow, Soul Salt, and Void Essence. Specify culinary drops for remaining boss families before completing that catalog.
- **Boss influence:** begin with sound/particle cues and supporting mobs, then opt-in new-generation environmental features. No spontaneous bosses, build destruction, or forced modification of old chunks.
- **Recipe discoveries:** choose which new advanced dishes are gated; select actual loot-bearing structures. Witch huts need an explicit reward-source design because there is no normal vanilla hut chest to extend.
- **Feasts:** six servings is the first target. Decide material cost, whether breaking preserves remaining portions, and placement visuals before implementing additional feast families.
- **Empowerments:** define combined armor bonuses and arrow ownership/accuracy limits before adding homing or teleport behavior. Existing melee powers stay compatible.
- **Chorus desserts:** any teleport chance must use validated safe landings and avoid involuntary entry into damage or another player's build; the playful behavior remains optional until designed.

## Working cadence

For each milestone: inspect the closest existing implementation, agree material gameplay choices, implement a playable increment, run focused tests followed by the required build, update the roadmap and player guide, and review the result. Keep subsequent commits focused on one milestone or a coherent part of it. Publish only when requested.

## Deferred expansion backlog

Status: **saved for later, not scheduled for implementation**. Finish the active ecosystems, cooking, boss rewards, and progression work first, then prioritize cohesion and polish before starting more major systems. The source brief is preserved verbatim in [future-expansion-notes.txt](future-expansion-notes.txt); examples and numerical targets below are proposals, not current functionality.

| Later idea | Preserved direction and dependency |
| --- | --- |
| Boss discovery structures | Druid shrines, hunter camps, excavation sites, storm altars, witch-hut variants, frozen nests, wrecks, Deep Dark hives, Nether shrines, fossil altars, End observatories; clues, lore, ingredients, summon components. Design world generation and rarity after the regional loops work. |
| Additional rare minibosses | Alpha Dire Wolf, Fossil Raptor Matriarch, Storm Roc, Jungle Broodmother, Cave Widow, Shadow Praetorian, Netherborn Spawn, Void Sentinel; ingredients, summon components, cosmetics, or enchantment materials, **no permanent hearts**. Already-planned regional heralds such as the taiga Alpha remain in the active ecosystem scope; this proposal does not add a second Alpha encounter. |
| Small thematic enchantment set | Hunter, Butcher, Forager, Soulward, Venom Guard, Shadow Sight, Deep Diver. Optional advantages; never mandatory keys to a boss. Define compatibility and stacking with meal/equipment powers first. |
| Utility equipment | Hunter's Compass for boss structures, Ingredient Pouch, Quiver, Explorer's Pack. Reuse inventory upgrades and preserve item ownership/save rules instead of creating overlapping storage systems. |
| Unified Darkspawn Journal | Creatures, bosses, cooking, progress; silhouettes before discovery, narrative hints, summon clues, kill/heart status, discovered recipes. Consolidate the proposed Field Journal and existing cookbook/progression data instead of tracking discoveries twice. Sample 11/16, 10/15, and 37/65 counters are illustrative; use actual registries and the established heart cap. |
| Expanded structure loot | Mineshaft cave ingredients/Crawler hints; desert archaeology; jungle serpent clues; Baba Yaga herbs/lore; Ancient City Queen artifacts; bastion Netherborn materials; End City Void Eye clues. Extend compatible loot sources; witch huts need a designed reward source because ordinary huts have no loot chest. |
| Archaeology | Fossil teeth, bones, fragments, extinct seeds in appropriate suspicious blocks; reconstruct a Tyrant summon component. Preserve existing summon recipes and define new-generation placement before implementation. |
| Fishing | Pike, tuna, anglerfish, Arctic cod, piranha, cave fish; regional recipes and rare ocean summon clues. Resolve the brief's legacy Leviathan-fragment name against the existing Kraken family without restoring a duplicate ocean boss/heart identity. |
| Expanded animal utility | Pig truffle hunting, feed-influenced eggs, rabbit ingredients, goat specialty cheese, bee honey recipes, mooshroom ingredients. These advanced animal behaviors are deferred; basic livestock ingredient roles already in the active cuisine roadmap remain in scope. |
| Brewing crossover | Culinary ingredients usable in brewing; food offers broader sustained utility, potions stronger short-lived choices. Preserve vanilla brewing and the existing independent potion/meal ownership rules; example effect durations are balance proposals. |
| Post-boss regional changes | Heartwood trees, conditional dire-wolf taming, changed fossil spawn/archaeology rewards, storm resources, beneficial mushrooms, reduced shadow activity. Decide world versus player unlock ownership and save migration; avoid automatic terrain edits in established builds. |
| Awakened rematches | After all 16 boss families, candidate summon upgrade uses the original summon, a Nether Star, and dragon's breath. Add mechanics, culinary/cosmetic rewards, possibly equipment materials; **no extra permanent hearts**. |
| Cosmetic boss gear | Helmets, cape-like cosmetics, banners, armor trims, trophies, particles; Queen carapace, Kraken tentacles, Thunderbird lightning motifs. Extend active trophies later without implying vanilla cape support or adding combat power. |
| More ambient wildlife | Forest songbirds/deer; taiga elk; savanna gazelle; jungle butterflies/frogs/lizards; ocean jellyfish/crabs; End Grazers/Void Rays. Budget spawn caps and performance alongside existing regional mobs. |
| Rare spawn events | Full-moon Wolf Hunt, Fungal Bloom, Shadow Incursion, Soul Storm, End Rift. Infrequent, bounded encounters with ownership, cleanup, cooldown, and terrain-preservation rules. |
| A few legendary items | Approximately 4–6 exceptions: Titanbreaker, Voidbow, Soul Lantern, Queen's Carapace. Define roles after the existing equipment empowerment expansion; preserve its relevance. |
| Server tuning | Boss health/damage, multiplayer scaling, heart progression, mob rates, cooking duration, dragon requirement, structure rarity. Current progression defaults remain unchanged; validate ranges, synchronization, and save compatibility when this milestone is scheduled. |
| Mod interoperability | Ingredient/material/tool/weapon tags and modpack compatibility. Treat example `c:` tag names as candidates to verify against the target ecosystem before adopting them. Preserve baseline recipes and explicit item restrictions where gameplay requires them. |
| Sound and atmosphere | Distinct idle, phase-change, attack-warning, death, and ambient clue sounds. Readable encounter cues take priority over adding more mobs. |
| Music | Theme groups for Overworld nature, mutations, ancient magic, Nether, Deep Dark, End. Define encounter start/stop, competing bosses, volume controls, and resource rights before production. |
| Darkspawn Nexus / endgame gauntlet | Last, optional completionist dungeon after all 16 bosses and a defined heart threshold; mixes regional mechanics and ends in an optional superboss. Not required for normal progression. |

Before activating this backlog, review animation, texture, sound, particle, balance, world-generation, UI, multiplayer, compatibility, and performance quality. Promote one concrete increment into the active roadmap when it is selected; saving an idea does not mark it implemented or commit to a release date.
