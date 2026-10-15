# Weekend playtest audit — October 15, 2026

This is a source audit, not a release manifest. No JAR was packaged. Historical brainstorms preserve ideas; they do not promise that every example is implemented. Use this status sheet and the linked feature guides when deciding what to test.

Subsequent item-art update: the borrowed item artwork noted below has been replaced by [custom Darkspawn models and materials](distinct-item-art.md). The validation counts below record the earlier audit, before that asset pass.

Pre-push verification: unfiltered `fullCheck --rerun-tasks --max-workers=1` passed with 95 unit tests, all 292 server GameTests and 142 cuisine model references (`build/prepush-fullcheck-verified.log`). The storage artwork test now verifies the separate inventory models and custom textures. The wolf defeat fixture now waits for entity-ready spawn chunks instead of assuming a fixed three-tick delay is enough; gameplay is unchanged. No JAR was packaged.

## Selected features available in source

| Area | Current implementation / acceptance guide |
| --- | --- |
| Pirate progression | Formal promotions, shared-party compass navigation, skirmishes, nine island types, beer/rum buffs, captured-fort home ports, saving. [Stage rules](expansion-stages.md) |
| Ships and crew | Three sizes, separate Standard/Clipper/Ironclad/Corsair Raider/Fleet Battleship/Drowned Ghostship specializations; solid/fire/chain/piercing ammunition; docked roaming and shore support; forgiving wages/morale. Sailing remains seated. [Exact costs and limits](endgame-expansion-playtest.md) |
| Size limits | 4/8/12 gun positions; ordinary crew and officers give maximum NPC counts of 10/14/18, with four player seats separate. Specialization does not add a fourth size tier. |
| Exploration | Boss-landmark finder, discovery maps, regional caches and brushable archaeology, cosmetic awakened rematches, regional boons/audio and bounded server settings. [Server playtest](server-playtest.md) |
| Accessories and hub | All six accessories, three slots, glasses toggle; compact Inventory/Character/Companions/Storage/Journal tabs, search, favorites, comparisons and nearby accessible sorting storage. [Field utilities](field-utilities-playtest.md) |
| Fishing and livestock | Six swimming fish species, buckets, catches and recipes; pig truffles, chicken feed effects, goat dairy and mooshroom cream. [Field utilities](field-utilities-playtest.md) |
| Brewing and world progression | Six culinary potion families / sixteen variants, shared boss unlocks, planted Heartwood, dire-wolf companionship, regional harvests and reduced fossil/shadow natural spawning. Changes preserve existing builds. [Endgame rules](endgame-expansion-playtest.md) |
| Endgame | Group Nexus entry, five waves and an enhanced existing Void Eye Warden, saved run/recovery support, four legendary utility relics. [Nexus acceptance](endgame-expansion-playtest.md) |
| Testing Camp | Fresh camps contain supply groups through 25 and guide pages, including accessories/fish/animal supplies, brewing, ammunition, world harvests, Nexus and relics. Existing camps do not refill or rebuild themselves. |

## Brainstorm ideas still absent or only partly represented

These are remaining proposals or scope differences, not missing texture files. They are not implemented by this polish pass.

| Original idea | Remaining work / current boundary |
| --- | --- |
| Buildable airplanes; power armor/mechs; DBZ-style cloud; magic carpet | Not implemented. Fourteen creature mounts exist, including griffin, dragon, Poptart cat, unicorn and golem; these do not provide the proposed aircraft/mech systems. |
| Material-specific naval warfare | No complete wood/copper/iron/diamond plating simulation, solvent/dousing/TNT ammunition roster or individual component destruction system. Current classes and ammunition use shared hull combat. |
| Full faction vessel roster and autonomous crews | Earned specializations reuse existing ship platforms. Walking on moving decks, autonomous route following/docking, combat boarding crews, crew loyalty stories and general base workers remain outside current scope. Shore companions provide support effects. |
| Thematic enchantments | Hunter, Butcher, Forager, Soulward, Venom Guard, Shadow Sight and Deep Diver are not added. Existing Warmth/Cooling and equipment/accessory powers are separate features. |
| Dedicated utility bags | Ingredient Pouch, Quiver and Explorer's Pack are not implemented as distinct items. Inventory upgrades and nearby storage do not constitute those proposed items. |
| Full archaeology/fishing proposal | Current regional loot and six fish species do not include a complete fossil reconstruction/extinct-seed system or rare Kraken fishing-clue progression. |
| Expanded animal interactions | The selected pig/chicken/goat/mooshroom behaviors exist; a further rabbit/bee behavioral expansion is not included. |
| Full regional transformation | Implemented unlocks are controlled harvests, cultivation, companionship, loot and spawn changes. No automatic biome rebuilding or new beneficial mushroom ecosystem. |
| Awakened mechanics and cosmetic roster | Current rematches scale encounters and award cosmetics/trophies. A complete new move set for each boss, all proposed capes/banners/trims and distinct awakened models remain future work. |
| Wildlife, elites and regional events | Existing regional creatures/heralds cover part of the original list. More species and fully staged Wolf Hunt, Fungal Bloom, Shadow Incursion, Soul Storm and End Rift encounters remain proposals; current regional events are personal timed boons. |
| Full Nexus dungeon | Functional cooperative arena uses existing enemies and an enhanced Void Eye. A large authored dungeon and unique final-boss art/mechanics remain future scope. |
| Comprehensive tuning/modpack support | Current server settings cover naval budgets and regional timing/toggle. The complete proposed boss-health, damage, heart, cooking and structure-rarity settings are not exposed. Other installed mods have not been specified or compatibility-tested. |
| Seasonal public-server design | Paid admission, seasons/resets, leaderboards, Hall of Fame and revised sanctuary/PvP rules remain the separate future server specification. |

Creature sound and boss music resources already exist; the old backlog wording must not be read as saying these are entirely absent.

## Asset coverage and polish

- Broad reference audit passed: 1,404 asset JSON files, 3,519 model/texture/sound references, 643 sound events/subtitles and 511 readable PNGs. References include locally cached native Minecraft resources.
- Focused validators passed for 92 catalog item sprites, 41 field-utility item resources, six fish skins, 142 cuisine model references, 48 crop stages, pirate structures/drinks/compass/four shanty recordings, 42 regional templates and 84 archaeology deposits, and 80 brewing recipes.
- Creature export/animation checks passed, including 99 creature animation projects / 690 clips; thirteen mount project exports and Forsaken content/city checks passed. This does not replace observing them in the client.
- New relics, ammunition, key/core and Heartwood blocks intentionally reuse native art. Fish use native cod geometry with six skins. Faction specializations reuse the editable hull and Nexus enemies reuse existing models. Having valid assets does not imply bespoke artwork for every addition.
- Corrected the displayed name to **Queen's Carapace**. Gameplay rules and saves are unchanged by this pass.
- Common/client compilation and all three focused unit tests passed: the new actual-registry item/block visual-definition/name check and both existing audio tests. Log: `build/polish-resource-tests.log`. These focused checks do not constitute a new fullCheck run.
- All three focused Testing Camp server tests passed, including placement, obstruction safety and supply coverage/capacity. Log: `build/polish-camp-tests.log`. `git diff --check` passed.
- Inspected the offline field-utility sprite sheet and beer/rum geometry preview. Icons are present and distinguishable; live GUI scaling, lighting and animation remain client acceptance work.

## Weekend acceptance

1. Request a current play build before installing: these checks did not update a playable JAR. Use matching client/server mod versions and the tested dependency pins in the server guide.
2. Create a fresh Testing Camp in clear space. Existing camp supplies and already-generated terrain do not automatically gain new content; explore fresh chunks for new island/structure generation.
3. With all five players, check GUI scales, item names/icons, fish/mount rendering, sound levels and ship boarding/menus. Four seats means five players cannot all ride one ship simultaneously.
4. Exercise save/restart, death/recovery, shared navigation, docked crew recall, accessory/loadout changes and nearby storage permissions.
5. Rehearse Nexus entry, death, retreat and recovery in a disposable world; then assess real progression, combat difficulty, cooking/brewing overlap and exploration frequency.

Prior unfiltered verification passed 94 unit and 291 server tests; the subsequently added fungal-harvest test passed in the focused five-test endgame run. Those historical results are detailed in the endgame guide. Live five-player gameplay, balance, visual acceptance and third-party mod compatibility remain unverified.
