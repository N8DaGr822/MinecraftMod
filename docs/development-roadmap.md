# Darkspawn development roadmap

**V2 release preparation (2026-10-09):** `2.0.0-alpha.1` is packaged for upload. Full build passed with 63 unit tests and all 85 server GameTests; no publication performed. Current instructions: [Modrinth release](modrinth-release.md). Earlier alpha records below remain historical.

## Current increment Unified Darkspawn Journal (2026-10-13)

A free inventory Journal now combines personal creature and landmark discoveries with existing cooking knowledge, consumed boss hearts, credited kills and Forsaken research. Its seven tabs include all 82 regional creatures, 14 mounts, 16 bosses, four infected and 17 landmark families. Unseen species use authored silhouettes and habitat hints. Server observations require proximity and line of sight; landmark checks use loaded native starts. Knowledge survives death and OneBlock inventory changes without transferring items. Search, pagination and Refresh are included.

[Implementation and migration rules](journal-implementation.md) describe the first-pass limits. Map coordinates, structure compasses and archaeology remain later exploration work. Compilation, three Journal unit tests, four Journal server tests, nine OneBlock regression tests and the portrait validator passed. Live UI and narration acceptance is pending. No JAR was packaged or version changed.

## Previous increment Forsaken threats and rewards (2026-10-13)

The first Forsaken threat/reward loop now includes four infected roles, four permanent nests in fresh cities, recoverable contamination, saved shared city pressure with bounded surges, five useful exclusive resources, a complete Quarantine Armor set, and a loaded-area nest scanner. Existing cities and inventories remain intact; opened caches do not restock. Armor uses the existing smithing pattern and retains native item components. Pulse Blade, reusable core-fueled containment beacons and three scanner modes are implemented. The next Forsaken milestone is expedition pacing and multiplayer balance review.

[Implementation and balance rules](forsaken-implementation.md#increment-3-threats-contamination-and-rewards), [asset preview](../art/forsaken/content-preview.png), and the shareable implementation guide describe what is present versus deferred. Compilation, all 17 Forsaken server tests, the runtime model/animation bake test, and asset/route checks passed. No release version changed or JAR was packaged; live encounter feel and multiplayer capacity remain acceptance work.

## Previous increment: all regional boss landmarks (2026-10-13)

All sixteen boss families now have discovery structures. The regional increment adds fourteen families, each with three variants, guaranteed ritual books, finite supplies, and native locate support. The existing shrine/camp/bunker generation path and saved IDs are preserved. Surface, seabed, cavern, Nether and End placement use appropriate bounded surveys, with candidate exclusions around villages and Ancient Cities. The Kraken wreck directs players to the surface for its existing boat/open-water ritual.

Source and placement details are in [the updated landmark plan](boss-structures-plan.md) and player README. Verification and live acceptance are recorded in the landmark plan. No JAR was packaged and no release version changed. The unified Journal is now implemented; discovery maps, archaeology and regional loot remain next exploration increments.

## Previous increment: instant testing camp (2026-10-12)

Added an opt-in Creative/command Testing Camp item for rapid playtesting. One use on flat ground builds crafting and cooking stations, a fifteen-bookshelf enchanting setup, an irrigated nine-crop plot, an inactive bonfire with its foundation, and signed supply barrels. Supplies include the dragon combat kit, recipe-derived ingredients, prepared meals/feasts, cookbook/scrolls, all fifteen iron/diamond armor styles, expedition and legendary sets, farming, crafting, storage and travel materials. An in-game guide covers the test route. Existing progression and gamerules are preserved.

The complete placement area is checked before construction; occupied terrain, protected positions, fluids, unloaded chunks and intersecting entities cause refusal without a partial camp. Full build passed with **80 unit tests and all 162 server GameTests**, including three camp tests covering actual item placement/consumption, obstructions, supply capacity, loaded cooking recipe coverage and armor style samples. Fixed legacy non-UTF-8 dashes in English names during package validation. Checkpoint JAR: `build/testing-camp-verification/libs/darkspawn-2.0.0-alpha.1.jar`. [Usage and contents](testing-camp.md). Live appearance and multiplayer usability remain playtest checks.

## Previous increment: The Forsaken city slice (2026-10-12)

The first authored city adds two visually distinct districts, four reusable building designs, ten finite scavenging caches, upper floors, a rooftop circuit with a broken span, and a short maintenance tunnel. Dedicated native structures generate only in new Forsaken terrain, using a 64×80-block slice on a 512-block grid. Saved starts preserve looted containers and player edits; existing chunks are not retrofitted. Ordinary salvage tables are separate for housing/shops, medical rooms, research rooms and the roof cache. Exclusive resources and actual infestation mechanics remain for subsequent increments.

[Implementation and upgrade notes](forsaken-implementation.md#increment-2-ruined-city-slice) and [layout preview](../art/structures/forsaken-city-preview.html). Automated voxel traversal confirms all ten caches are reachable and the central retreat route remains open. Full build passed with 76 unit tests and all 143 server GameTests, including three city tests covering template contents, native structure starts/serialization, and placement/loot preservation. The production JAR contains the city template, biome, structure registration and loot tables and excludes the GameTest world preset. GameTestServer disables automatic structure generation, so live new-world generation, movement/collision, two-client exploration and restart checks remain acceptance work. The user requested committing and pushing after completion.

## Previous increment: The Forsaken travel foundation (2026-10-12)

The user supplied [The Forsaken design brief](forsaken-design-v1.txt) and approved standalone Darkspawn content, global post-dragon gate activation, and builder research with shared travel access. [The implementation record](forsaken-implementation.md) separates these confirmed decisions from initial tuning defaults and the later city/threat/reward milestones.

This first increment adds new-chunk research bunkers, recoverable permanent schematic knowledge, server-validated fabrication, repairable/buildable Quarantine Gates, reusable keys, and a shared landing with per-player return links. Normal expedition inventory and death rules apply. OneBlock remains isolated and proficiency remains deferred. The destination is currently a travel foundation, not a finished city expedition. Full build passed with **76 unit tests and all 140 server GameTests**, including eight Forsaken tests covering research/fabrication, dragon/frame validation, independent return links, persistence/death copying, blocked arrivals, landing preservation, missing-destination fallback and native rotated-bunker repair/loot. The packaged mod includes the dimension, client screen and all three bunker templates; the test-only world preset is excluded. Live client, two-player and server-restart acceptance remains pending. Next increment: the two-tier ruined-city slice.

## Previous increment: shared OneBlock dimension (2026-10-12)

The user deferred proficiency and selected OneBlock. Confirmed scope: one shared 3×3 starting footing, six resource phases, block-count unlocks with manual advancement, occasional supply chests/vanilla creatures, and free inventory-menu entry/return. Possessions are isolated—including Ender Chests and inactive armor—while character upgrades remain shared. Death stays on the island and follows server keepInventory rules; void losses are not recovered.

Implementation uses a void dimension, persistent shared progress, a regenerating resource with vanilla harvest rules, and synchronous saved item-profile switching. Alternate portal/entity routes cannot bypass the boundary. No pirate-portal implementation was found in this checkout; this destination is independent. No new bosses or distant islands are added. [Player guide](../README.md#shared-oneblock-dimension) records the exact rules and initial pools. Full build passed with 76 unit tests and all 132 server GameTests, including eight OneBlock integration tests covering item profiles, travel restrictions, persistence, death/respawn, progression, supply milestones, and native harvesting. Live multiplayer, GUI scales, server restarts, and a full progression run remain acceptance work. Proficiency remains deferred at the user's request.

## Previous increment: unified expedition temperature (2026-10-12)

Heat and cold now ship together with gradual exposure, clear HUD status/trend/wetness, biome/altitude/night/weather/cover checks, and bounded local campfire/lava heat. Worn expedition pieces provide distinct climate protection. Warmth and Cooling I–II supplement equipment through native enchanting, loot, and trades; existing Warming meals and Fire Resistance help too. Standard and legendary armor retain their combat roles. No thirst or skill gates are added.

Exposure persists across saves, reconnects, loadouts, and living transfers; death resets it. Server-owned penalties use separate movement/mining modifiers and dedicated climate damage types. Initial tuning, equipment values, counterplay, and limitations are in [the temperature guide](../README.md#temperature-and-expedition-preparation).

Verification: full build passed with **75 unit tests and all 124 server GameTests** (`build/temperature-build.log`). New tests cover progressive heat/cold, recovery, cave shelter, wetness, campfires, bounded enchantment protection, inactive loadouts, damage/penalty ownership, save/load, living transfers, and death. The prior forest spawning failure was resolved by establishing daylight and waiting for skylight in its test fixture; ecosystem behavior was unchanged. In-game HUD readability, real two-client play, and long-expedition balance remain acceptance work. Next planned discussion areas remain activity-based proficiency, shared OneBlock, and dangerous portal expeditions.

## Previous increment: five boss-forged legendary sets (2026-10-12)

The user selected legendary armor next and requested difficult boss-ingredient acquisition. Dragonlord, Phoenix, Voidwalker, Titan, and Soul Reaper are separate netherite-based item families, each with four pieces, custom wearable and inventory models, native trim support, and a matching-set ability. Standard styles remain at fifteen.

Each core consumes two distinct boss essences plus one netherite ingot; each armor piece consumes a core and netherite upgrade template when converting its corresponding netherite piece. A full set requires eight essences and four extra netherite ingots/templates. Existing shared boss essence drops are reused; permanent-health hearts and boss loot tables are unchanged. Exact pairings and abilities are in [the player guide](../README.md#legendary-armor).

Phoenix and Soul Reaper cooldowns persist through player saves and respawns; loadout changes never reset them. Titan and Voidwalker use feature-owned modifiers; Dragonlord applies only to direct player melee/projectiles against qualifying mobs. Twenty new armor items and five cores have generated assets and recipes. Verification: Java/client compilation, packaging, and all 72 unit tests passed. The last full server run passed 119 of 120 tests, including all legendary recipes, preservation, full-set bonuses, Phoenix save/respawn cooldown, and Soul Reaper eligibility checks. The existing forest natural-spawn fixture failed its unlocked Barkling surface assertion; forest code was not changed. Earlier runs also encountered the known agriculture lighting fixture. The full build remains red on that unrelated server test. In-game appearance and balance remain acceptance work.

## Previous increment: final 15-style standard roster (2026-10-12)

The user approved condensing the 40-style proposal into 15 standard styles plus five future legendary sets. All standard styles are now implemented: Classic, Knight, War Chief, Berserker, Samurai, Spartan, Viking, Ranger, Reaver, Pirate, Pathfinder, Conquistador, Juggernaut, Paladin, and Shadowblade. The [README](../README.md#full-set-style-bonuses) is authoritative for current recipes and reduced bonuses.

Ranger merges bow/crossbow damage. Saved Arbalist / Royal Guard / Brawler identities decode to Ranger / Paladin / Berserker without changing other item data; retired recipes are removed and artwork retained. Paladin adds 10% healing; Shadowblade grants two seconds of invisibility after damage with a server-session 15-second cooldown. At the user's request, Shadowblade armor, trims, and glint also hide whenever the wearer is invisible. Held items remain visible. New geometry preview: art/armor-styles/final-roster-preview.png. Verification: build passed with 71 unit tests and all 116 server game tests. Coverage includes retired-item migration, reduced bonuses, Paladin healing, and Shadowblade activation/cooldown. An unrelated agriculture lighting-fixture failure passed on retry without farming-code changes. Live client visibility and balance remain playtest tasks.

Earlier batch records below are historical, including their former roster sizes and values. Do not continue implementing the superseded 40-style list. Legendary designs and biome temperature remain separate future work.

## Previous increment: Ranger, Arbalist, and Brawler (2026-10-12)

Ranger and Arbalist start at +10% ranged damage, and Brawler at +1 unarmed attack damage. Bow/crossbow arrows use the weapon stored on the projectile; crossbow rockets use the native angled-launch marker. All ranged bonuses require the matching worn set at impact. Brawler uses a feature-owned native attribute, removed when the main hand contains any item, and preserves normal attack recharge. Three distinct iron/diamond designs add hood/quiver, asymmetric aiming armor/bolt pouches, and padded/studded gauntlet armor with corresponding shoulder and boot variants. Recipes use paper plus leather / tripwire hook / brick. Verification: full build passed with 70 unit tests and all 115 server game tests. A final hood-only geometry correction passed a subsequent package build and unit/asset checks; unchanged gameplay tests were not repeated. Live wearer fit and balance remain playtest tasks. Preview: art/armor-styles/ranged-brawler-preview.png.

Sixteen of forty standard styles are implemented. Continue through defensive, healing/status, conditional combat, mobility, companion, and legendary sections with conservative playtest values. Regional temperature remains a separate pass.

## Previous increment: three specialist armor styles (2026-10-12)

Reaver, Pirate, and Conquistador add three complete iron/diamond styles with distinct edge-spiked shoulders and boots. Each starts at a conservative 10% bonus: mace damage including smash attacks, melee/thrown trident damage, and pickaxe mining speed respectively. Pirate checks the worn set at impact and the actual thrown projectile; changing the held item does not change its identity. Conquistador applies after native mining modifiers on client and server without altering harvest requirements. Paper plus flint / nautilus shell / copper ingot selects the corresponding style. Verification: full build passed with 70 unit tests and 114 server game tests, including native mace-smash damage. Offline preview: art/armor-styles/specialists-preview.png; live wearer fit remains a playtest task.

Thirteen of forty standard variants are implemented. Remaining specialist weapons, defensive sets, status effects, conditional combat, companions, and legendary sets follow in manageable sections. Temperature remains a separate pass.

## Previous increment: three utility armor styles (2026-10-12)

Implementation proceeds in manageable sections. Royal Guard (+2 maximum health), Juggernaut (+15 percentage points knockback resistance), and Pathfinder (+5% movement speed) add three complete iron/diamond styles, smithing recipes, models, inventory previews, and matching-set bonuses. Juggernaut and Pathfinder start below their proposed values. Royal Guard does not heal on equip and clamps excess health on removal. The same worn-only rules and owned transient modifiers apply. Verification: full build passed with 70 unit tests and all 113 server game tests; asset checks include trim bounds and inventory models. Live wearer fit and balance still need playtesting. Offline preview: art/armor-styles/utility-preview.png.

Ten of forty standard variants are implemented. Remaining sections: weapon specialists; damage defenses; healing/status effects; conditional combat and companion effects; legendary sets. Balance each section before adding the next. Legendary acquisition still needs a design decision, and regional temperature work remains separate.

## Previous increment: full-set armor style bonuses (2026-10-12)

The user requested implementation of the variant buffs and subsequently revised Samurai to +15% sword damage. All seven styles now have a full-set bonus: Classic armor-wear reduction, Viking axe damage, Spartan spear damage, War Chief attack damage, Knight toughness, Berserker attack speed, and Samurai sword damage. See the [current values and activation rules](../README.md#full-set-style-bonuses).

Four worn matching-style iron/diamond pieces are required; materials may mix. Only feature-owned transient modifiers are managed. Weapon damage is restricted to direct native player/spear melee sources and the eligible weapon tag. Classic only reduces native wear calls on actual worn armor. No temperature mechanics are added. Verification: full build passed with 70 unit tests and 112 server game tests. Visual fit and balance still need in-game playtesting.

The user's expanded roster of 40 standard variants and five legendary sets is preserved in [the design notes](expedition-equipment-proficiency-notes.txt). The first seven were implemented in that increment; later progress is recorded above.

## Previous increment: additional cosmetic armor styles (2026-10-12)

The user selected War Chief's commander appearance, then asked to document potential full-variant combat bonuses while continuing visual styling. The [design notes](expedition-equipment-proficiency-notes.txt) distinguish user examples (Spartan spear damage, Knight defense, War Chief strength, Berserker attack recovery, Samurai counters) from proposed activation/stacking rules and assistant suggestions. None of these buffs is active or scheduled; all temperature systems remain a separate future pass.

The current visual batch adds War Chief, Knight, Berserker, and Samurai to iron and diamond using the existing cosmetic item component, native trim overlay, inventory model selectors, Blockbench review sources, and smithing recipe system. All armor stats and unrelated components remain unchanged. Paper plus gold ingot / iron ingot / rabbit hide / bamboo selects the four styles respectively. The offline preview is `art/armor-styles/commanders-preview.png`; live visual testing remains necessary.

Verification: full build passes with **70 unit tests and all 109 server GameTests** (`build/armor-style-expansion-build.log`). Existing preservation, serialization, recipe, selector and trim-coordinate checks cover the new variants. The first run hit the previously observed Kraken tentacle-count and agriculture lighting failures; both passed in the final build after the final leg-plate asset adjustment. No boss or agriculture code changed. Combat bonuses remain documentation only.

## Previous increment: iron and diamond cosmetic armor rework (2026-10-12)

The user selected automatic upgraded default models for vanilla iron and diamond armor, plus cosmetic Viking and Spartan variants applied by smithing. Original items, stats, enchantments, trims, and boss powers are preserved. Styles are independent per piece, reversible to Classic, and saved in a dedicated synchronized item component. Existing armor defaults to Classic without migration. Native netherite upgrades and expedition conversions retain their own equipment assets; unsupported items ignore the cosmetic component.

The existing armor mesh pipeline now includes six metal/style families, inventory selectors, Blockbench review files, and trim-bearing plates. Worn trims use native pattern/material rendering; item previews show material-colored borders. Paper plus bone selects Viking, paper plus feather selects Spartan, and paper plus iron nugget restores Classic. See the [player guide](../README.md#iron-and-diamond-armor-styles) and `art/armor-styles/preview.png`. All temperature work remains deferred to one dedicated pass.

Verification: full build passes with **70 unit tests and all 109 server GameTests** (`build/armor-styles-build.log`). Tests cover all eight vanilla armor items, style reset, saved-component round trips, preservation of stats/names/enchantments/damage/trims/boss powers, repeat-style rejection, loadout swaps, netherite and expedition conversions, and model/trim-coordinate resources. The asset test caught and corrected an oversized Viking shoulder UV region. All 80 packaged style resources match source. Offline visual review is complete; in-game movement, glint, trim placement, and wearer fit remain acceptance checks.

## Previous increment: three additional expedition armor sets (2026-10-12)

The user selected an armor-first pass for Diver, Desert Wanderer, and Ashwalker. All temperature systems will be addressed together in a later pass; no heat, thirst, or new cold mechanics are activated here. Marsh Warden and Blightguard remain future ideas.

The three four-piece sets add distinct wearable models, textured 3D inventory items, editable Blockbench review sources, and native smithing conversions from iron armor with leather plus prismarine shards, rabbit hide, or magma cream respectively. Existing item components survive conversion. All sets work with the existing three loadout slots, armor enchantments, repairs, and boss empowerments.

Diver's helmet uses native oxygen bonus and submerged mining attributes, plus equipment-owned underwater night vision. Air remains finite; tank refill and boot anchoring are not implemented. Desert Wanderer is wearable/craftable now, with climate benefits deferred. Ashwalker reduces incoming fire-tagged damage by 10% per worn piece, capped by four slots at 40%; lava remains damaging. Initial values and recipes require playtesting.

Verification: full build passes with **67 unit tests and all 107 server GameTests** (`build/expedition-expansion-build.log`). Existing armor resource/recipe checks now cover all 20 pieces. New server tests cover Diver's dry/submerged vision, potion preservation and attribute removal on loadout swap, plus full/partial/removed Ashwalker fire protection, ordinary damage, and lava damage. The fire fixture initially reset the wrong invulnerability field; resetting native damage cooldown corrected the test without changing gameplay. All 57 new packaged armor resources match source. Offline model review passed; live movement, fit, glint, and gameplay balance still require playtesting.

## Previous increment: biome expedition armor (2026-10-12)

The user selected biome armor next. The first armor-only increment adds four-piece Mountaineering and Spelunking sets, original raised armor geometry and textured item models, plus native smithing conversions that preserve existing item data. Mountaineering supplies full-set powder-snow freeze immunity and snow-walking boots. Spelunking supplies personal headlamp vision and padded boots with multiplicative fall protection. Both integrate with loadouts, armor enchantments, and existing boss empowerments. See the [player guide](../README.md#biome-expedition-armor).

Implementation is in the workspace. Common/client compilation, **67 unit tests**, and all **four new armor server tests** pass. An initial full run passed all 105 server tests; after correcting the client equipment definition's required nonempty fallback layers, the latest full run passed **104/105**, with the existing agriculture soil-lighting fixture failing (`build/expedition-armor-build.log`). The prior rerun failed an existing Kraken tentacle-count fixture instead; that fixture passed on the latest run. No agriculture or boss code was changed. The latest full build is therefore not completely green, despite successful packaging and all new armor checks passing.

Armor verification covers all eight native smithing conversions, saved item components, enchantment tags, boss smithing compatibility, full/partial snow suits, inactive loadouts, potion/meal preservation, fall-modifier stacking and removal, and geometry/texture/item references. All 38 packaged armor assets were checked against source. Initial defense, durability, recipes, and benefits are playtest choices; wider biome temperature remains separate future work. Offline geometry review is available in `art/expedition-armor/preview.png`. Custom armor-trim mapping and live checks for movement, slim/baby wearers, glint, and GUI scales are not complete.

## Current increment: craftable armor loadouts (2026-10-12)

The user selected armor loadouts as the first implementation from the new expedition ideas. One set is available initially; iron and diamond upgrades unlock sets two and three in order. Each recipe uses an armor stand, two leather, and two of its named material. Upgrades are shareable and permanent, without proficiency gates. The [armor loadout guide](../README.md#armor-loadouts) describes the controls and initial balance choices.

Inactive sets store actual armor separately from ordinary inventory, while the active set uses vanilla equipment slots. Server-validated requests use menu and loadout revisions, a two-second switch, damage interruption, grounded checks, and native Curse of Binding restrictions. Saves retain stored item components; normal death drops inactive armor with vanishing handling, while keepInventory retains it. Weapons and offhand remain independent.

Verification: the full build passed with **64 unit tests and all 101 server GameTests**, including six new loadout tests (`build/loadout-build.log`). Checks cover ordered upgrade consumption, full-inventory item/component conservation, inactive powers, independent players, replay/unsafe/Binding rejection, damage interruption, player save/load, respawn copying, normal death drops, vanishing, and keepInventory. The first run exposed a mock-client loading acknowledgement issue in the damage test; the fixture was corrected before the passing run. Live GUI-scale presentation and real two-client testing remain acceptance checks. Recipes and timings are initial playtest choices. This increment does not activate temperature, new armor families, proficiency, or dimension work.

## Future expedition design discussion (2026-10-12)

The [expedition, equipment, and proficiency notes](expedition-equipment-proficiency-notes.txt) preserve the agreed direction for temperature-based exploration, specialized armor with improved visuals, three armor loadouts unlocked through craftable upgrades, activity-based proficiency bonuses without usage gates, and a Lost Cities / Parasites-inspired portal destination. They also record the user's in-progress pirate portal, multiplayer catch-up goals, lower priority for broad weapon expansion, and unresolved proposals. **Saved for future planning only; no implementation milestone or release date is assigned.**

## Future shared OneBlock dimension (2026-10-12)

The [OneBlock dimension notes](oneblock-dimension-notes.txt) preserve the user's selected direction: a classic OneBlock experience on one shared family island within the existing server, with outside equipment prevented from bypassing the challenge. Separate inventory profiles and persistent shared progression are the working concept; phase activation, transfer details, and death rules remain undecided. Special events and distant islands are future possibilities only. **Saved for future planning; no implementation milestone or release date is assigned.**

## Pending home-checkout work and testing priorities (2026-10-12)

Status: **documented; implementation deferred until the home changes are available**. The user reports an existing **Griffon** mount and believes newer mount, barracks bonfire, and starting-chest work may be on the home machine. This checkout (`main` at `7944bcd`) and the accessible Minecraft chats do not contain those implementations. That does not establish that the work is missing from the home checkout. The user plans to commit and push the home changes on the evening of October 12; reconcile that code before extending or recreating these systems.

| Priority | Requested direction | Next step after the home changes arrive |
| --- | --- | --- |
| More flying mounts | Expand beyond the existing Griffon for Survival playtesting; originally requested for tonight's testing. | Inspect the Griffon's riding, flight controls, ownership, persistence, and assets; use its established implementation as the reference. Additional species are not selected yet. |
| Leveled barracks bonfire | Begin with a small **12×12 area**, called **Bonfire**. Spend items to upgrade it for a larger area and improved buffs, including movement, building, mining, and health regeneration. | Inspect the existing bonfire first, then define levels, upgrade items/costs, area dimensions and height, buff strengths, building behavior, multiplayer eligibility, and overlap rules. Only the initial 12×12 area and item-funded progression are established; later sizes and balance values remain undecided. |
| Testing starting chest | Supply gear and new feature items so Survival testing can start without several minutes of Creative inventory setup. | Check the existing chest's contents and acquisition/refill behavior, then add a practical equipment set and supplies for testing mounts, bonfire upgrades, and other new features. Exact contents and delivery behavior remain undecided. |

These requests are saved for follow-up while other work proceeds. The earlier suggested mount species, four-level bonfire table, increased building reach, and chest loadout were proposals, not approved implementation requirements. After pulling the home changes, reassess what is already complete, update this status, and make only the remaining necessary changes.

## Current increment: storage sorting network (2026-10-12)

The user selected this independent feature while the home-checkout work above waits for synchronization. First implementation: **Dump Chest, directional Sorting Tube, three-filter Sorting Chest, and searchable Bank Chest**. The [player guide](../README.md#sorting-tubes-dump-chests-sorting-chests-and-banks) records recipes, controls, routing, storage limits, and recovery behavior.

- Physical output direction determines chest priority. Bounded routing checks loaded chunks only; rejected remainders stay at their source. Matching chests merge compatible partial stacks before allocating empty slots.
- Three ghost filters designate exact item types without consuming examples. Sorting chests accept manual deposits regardless of filters. Their Sort button reuses the existing inventory sorter; player-inventory slot selection remains unchanged.
- Bulk Deposit preserves hotbar, equipment, and offhand while including unlocked extra rows. Bank withdrawal supports one item, one stack, or available normal inventory space.
- Bank storage aggregates component-identical items, supports search/paging/name-or-count ordering, and can re-sort old stock after new chests are added. Initial playtest capacity is 256 variants, upgraded to 512/1,024/2,048 with 1/2/4 diamond blocks; each variant holds at most one million items. Normal stock and Trash share capacity.
- Overflow trash filters feed a recoverable compartment. Deletion requires a second confirmation click, invalidated by storage changes. No automatic voiding. Breaking a Bank carries stock, Trash, filters, and upgrades in its dropped block item.
- New server tests cover route priority, partial acceptance, cycles, broken connections, live ticking, extra-row deposits, protected equipment, ghost-slot conservation, component preservation, capacity, save/load, packed item/network round trips, concurrent menu withdrawals, search/paging, confirmation, upgrade payment, re-sorting, and unloaded-route rejection.

Verification: the full build passed with **64 unit tests and all 95 server GameTests**, including ten new storage GameTests and one storage resource/recipe test (`build/storage-final.log`). The first combined run after storage fixes passed all storage checks but failed an existing agriculture lighting-fixture assertion; the final full run passed without farming changes. Earlier storage failures exposed a menu refresh issue (fixed) and timed tests exceeding the default timeout (corrected).

Remaining acceptance work: live client presentation at different GUI scales, output-direction usability, real two-client interaction, and throughput/capacity/cost balance. Branch-priority controls, category filters, automatic voiding, vanilla-chest sorting buttons, and direct Bank hopper integration remain later work. This increment does not implement the pending Griffon, bonfire, or starter-chest changes.

## Direction and current baseline

The design goal is a post-dragon world whose ecosystems connect exploration, farming, cooking, equipment, and boss progression. Preserve the existing 16 boss families, heart progression, inventory upgrades, smithing powers, and Cooking Station. The original expansion brief is in [progression-expansion-notes.txt](progression-expansion-notes.txt); creature concepts are in [boss-ecosystem-notes.txt](boss-ecosystem-notes.txt).

The first approved milestone is **the forest ecosystem, a new ingredient, and a meal**. Later milestones below are the accepted direction; detailed balance values and optional behaviors remain design targets until implemented and tested.

The follow-up ideas received on 2026-10-08 are saved in [future-expansion-notes.txt](future-expansion-notes.txt) and indexed under **Deferred expansion backlog** below. They are reserved for later and do not replace the active ecosystem/cooking/boss-reward/progression sequence. After forest and taiga, the user approved completing all remaining boss ecosystems. That roster is implemented below; live-client checks remain on the release checklist.

## First live playtest and alpha handoff (2026-10-08)

The user built and launched the mod in Modrinth and tested through the Ancient Tree Spirit. They reported that cooking/food and inventory worked, and successfully applied the Tree Spirit weapon empowerment. This is a user-reported smoke test, not completion of every client or multiplayer release check.

- **Tree Spirit balance:** the player could stand close and repeatedly chop with little incoming damage. Investigate close-range attack coverage, successful damage, armor/effects, and Survival targeting before tuning health/damage. The test included Creative/Survival switching; equipment, active effects, and exact combat conditions were not recorded. A buff is requested; it is not included in the alpha packaging change.
- **Animation polish:** the user captured phase screenshots and plans further animation work. Screenshots have not yet been reviewed here.
- **Hearts:** reward receipt/absorption was inconclusive while switching game modes. Creative hides the health HUD; absorbed permanent progress is not intended to reset on mode changes. Reproduce reward eligibility, absorption, mode switching, reconnect, and death in Survival before calling this a confirmed reset bug.
- **Family test release:** prepare `1.0.0-alpha.1` with clear known issues and required dependencies. Modrinth account/project selection and submission remain separate from creating the local artifact. See [release preparation](modrinth-release.md).
- **Next testing priorities:** two-player rewards and scaling, heart persistence, Tree Spirit damage without command effects, remaining bosses/regions, and animation synchronization. The remaining milestone release checks still apply.
- **Alpha verification:** final build passed with 58 unit tests and 69 server GameTests. The initial run failed zombie no-player attack-pose and Tree Spirit death-drop assertions; a diagnostic rerun passed without gameplay changes. Investigate intermittent test isolation/entity loading. The local alpha JAR is prepared under author `nathen.lentz`; the user selected unlisted distribution, but Modrinth submission is still pending.

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

## Milestone 3, first increment: ingredients and agriculture

Status: **implemented; full regression build passed; live-client review pending**. After the completed ecosystem roster, the user selected the ingredients/agriculture milestone. This increment supplies the six base crops and butter; biome crop variants remain a later increment. Regional and everyday cuisine is the next delivery milestone.

- Register onion, garlic, tomato, rice, corn, and pepper with separate seeds and the existing eight-age `CropBlock` lifecycle. Reuse native light, farmland hydration, density, bone meal, support removal, and saved block-state behavior. Rice uses ordinary farmland; this increment does not introduce paddies or two-block crop ownership.
- Each immature crop drops one seed. Ripe crops yield one or two produce, one guaranteed seed, and the normal Fortune-compatible extra-seed roll (two to five seeds total without Fortune). No produce before maturity. Harvesting followed by manual or native farmer replanting makes every crop renewable.
- Native seed/food integration: farmer planting and pickup tags, chicken food, and compostable components for seeds and produce. Five vegetables are ordinary raw foods with no meal effects; raw rice and butter are ingredients. Existing meals and Well Fed behavior retain their rules.
- Add six novice farmer offers through Minecraft 26.3's data-driven `villager_trade` registry and additive `farmer/level_1` tag: two emeralds for four seeds, twelve uses, one villager XP, normal price adjustments and restocking. Keep vanilla trades and the native two-offer selection count. Saved offers are not rerolled or injected into existing locked villagers.
- Add a Cooking Station recipe for one milk bucket to two butter, returning the bucket. Milk plus salt continues to make cheese. Salt, cheese, and Wild Herbs keep their existing renewable sources. The [player guide](../README.md#ingredients-and-agriculture) records several planned dish uses for each new ingredient; those recipes belong to milestone 4.
- Agriculture is not dragon-gated. Farmer trades work in existing villages, and crops require player planting rather than new world generation. Ordinary crop growth and harvesting remain authoritative on the server with no new tick manager, networking, mixins, or dependencies.
- Reuse vanilla foliage textures and inventory icons, with distinct ripe produce models and localized names. Custom textures, live-client growth/harvest visuals, trade balance, two-player farming, and existing-world upgrade playtests remain release checks.

### Verification record

- Common/client compilation and six focused agriculture server GameTests passed on 2026-10-08.
- Full `gradlew.bat build --console=plain`: passed on 2026-10-08 with **55 unit tests and 68 server GameTests**, including eight new agriculture/butter server tests and one crop asset test. This is the combined workspace result, including the concurrent boss animation work. The packaged JAR contains the crop registration, butter recipe, ripe models, and additive farmer trade resources. The Windows runner used the existing process-local socket-directory workaround.
- Server checks cover all six complete planting/bone-meal/harvest/replant loops, age-dependent loot, native random growth, saved age, seed identity, light/soil rejection without seed loss, chicken/farmer/compost integration, native farmer harvesting/replanting, trade-pool preservation, payment, depleted-stock save/load, and restocking.
- Two new cooking server tests exercise butter batches, both mouse buttons, repeat-click rejection, full inventories, partial-stack overflow, bucket return, and preserved cheese matching. The recipe decoding suite includes butter; an asset test resolves all growth stages, ripe models, textures, icons, and translations.

## Cuisine, rewards, and equipment expansion (2026-10-09)

The user authorized the next eight feature groups together: regional/everyday cuisine, boss cuisine, advancements/variety, trophies, Chef trades, feasts, cookbook/discoveries, and shield/bow/armor empowerment. Their first implementation is now in the workspace as **1.0.0-alpha.2**. These features extend the existing saves and do not activate the separate deferred expansion backlog.

- **Cuisine:** 25 regional, everyday, utility, and dessert dishes plus 16 boss dishes. All use the existing four-input station. Six distinct proposed meal identities are implemented, along with mining, fishing, warming, fire-resistance, slow-falling, and combined feast effects. Existing food and potion behavior stays compatible.
- **Boss cuisine and trophies:** all 16 families have independent renewable culinary ingredients and decorative pedestal trophies. Existing participant sets and death-loot paths own the rewards; living nearby contributors get reserved pickups regardless of their heart collection. Helpers and abandonment do not call this path.
- **Progression:** a dedicated advancement page tracks rituals, participant victories, hearts, collection thresholds, discoveries, and feasts. Variety uses the last eight eating events (not the last eight unique foods), gives positive saturation bonuses, and grants slow full-hunger healing at eight. Twenty lifetime distinct meals unlock Hero Feast. Saved discoveries and lifetime collection survive death; recent variety resets on death.
- **Chef:** Cooking Station POI, five native trade levels, normal price/stock/restock behavior. New profession presentation reuses the vanilla butcher apron. Player station ingredients remain menu-local and private.
- **Feasts:** six variants with six portions each, server-authoritative use and native saved block states. Empty-hand use requires hunger. Only untouched feasts drop when broken, preventing portion resets. Hunter's/Hero Feast combine strength and speed inside one meal effect.
- **Cookbook:** native book pages built from server recipes, categories, personal collection, locked previews, and discovery hints. Scrolls gate only five new recipes; three chest sources and Chef trades supply them. No retroactive modification of generated chests. Duplicate scroll use consumes nothing.
- **Equipment:** two shield, two bow, and three armor powers. One component per item preserves existing smithing data. Arrows use their captured weapon; armor duplicates do not stack, and transient attributes are removed when equipment is taken off. Voidmark uses reveal/slow instead of the brief's unresolved projectile-teleport proposal.

The initial placeholder presentation has now been replaced by bespoke textured models for 44 new meals, six portion-aware feast families, and 16 sculpted trophies. The bundled icon has new generated tree-spirit artwork. Three biome-mutated crops and their discovered dishes, three additional equipment powers, five additional loot sources, and bounded regional particles/ambient sounds are implemented. Effect icons still reuse vanilla art. Client readability, two-client use, village claiming in real terrain, economy, and combat balance still need live review. Original custom sounds and the remaining 14 structure families remain future work.

### Current priority: boss discovery structures

The user selected boss structures on October 9, 2026. **Alpha.3 implements the first two:** Druid Shrine and Hunter Camp, three native templates each with four rotations, biome-specific structure sets, guaranteed ritual books, and modest lazy chest loot. A bounded 49-point survey rejects water and steep terrain; gentle slopes meet a midpoint floor with at most three blocks of sampled cut/fill. Placement runs during new-terrain generation after vegetation and never on chunk load. Bosses remain player-summoned under existing progression rules. The [boss structures plan](boss-structures-plan.md) tracks the next 14 landmarks and live acceptance checks.

### Alpha.3 structure verification

- Common/client compilation, resource loading, packaging, and **61 unit tests** pass. The new unit test validates all six native NBT palettes, block properties, coordinate bounds/uniqueness, empty entity lists, and chest loot references. Minecraft 26.3 requires lowercase `id`/`properties` palette fields; the exporter was corrected after the initial integration test caught the old format.
- All **four new server tests** pass: all 24 template rotations retain loot and actual boss clearance; 60 loot rolls guarantee a readable three-page clue without victory rewards; both native pieces place usable lazy chests that cannot refill on a second opening; and 20 successful real-terrain starts across seeds `1`, `42`, `7319`, `-49017`, and `20261009` repeat and survive native save/load. Candidate sampling includes positive and negative coordinates. This exercises structure generation directly, not a live `/locate` travel session.
- The full suite passed all **85 server tests** in `build/structures-alpha3.log`. After the final palette test and lower-world-boundary adjustment, the latest full run, `build/structures-regression.log`, passed **84/85**: the existing zombie animation/no-player fixture still sees another test's Survival player inside its saved arena. Therefore the latest `build` task is **not fully green**, despite successful packaging and all structure checks. An attempted isolation change for existing encounter fixtures was reverted; no combat changes were made. Other intermediate runs also reproduced the previously recorded wolf-pack/Tree Spirit loot fixture failures.
- Offline voxel layouts were rendered and inspected at `build/structures-review/landmarks.png`. They are layout previews, not Minecraft screenshots. In-game materials, travel-distance balance, snowy ritual clearing, old/new chunk-border seams, and real two-client exploration remain manual checks.
- Packaged artifact: `build/libs/darkspawn-1.0.0-alpha.3.jar` (26,292,452 bytes), SHA-256 `3d2e890100c2eff0dffe0ea2147d55f49d85fbc09b1eab2ad5dbd5b96d447aad`. Verified embedded version, six NBT templates, both structure sets, and both loot tables. No publication was performed.

The reproducible cuisine definitions and resource generator are `tools/assets/cuisine.py` and `tools/assets/cuisine.java.template`. The player-facing recipes and behavior are documented in the README's **Expanded cuisine and progression** section.

### Expansion verification

**Art and regional expansion follow-up (October 9, 2026):** `build/art-expansion-final.log` records a successful full build with **60 unit tests and 81 server GameTests**, including regional biome/harvest checks and the three new equipment powers. The original farming regressions now exercise all nine crops for planting, growth, drops, replanting, villager behavior, and persistence; novice trades deliberately remain the six ordinary crops. The first run caught missing farming tags on the new variants; those resources were corrected before the passing run. `python tools/assets/verify-cuisine-art.py` passed all **126** model, UV, atlas-size, crop-stage, item-reference, and feast-portion checks. The offline textured gallery was rendered and visually inspected. Live in-game lighting, UI scale, ambient audio, and two-client checks remain open.

Historical art-expansion artifact: `build/libs/darkspawn-1.0.0-alpha.2.jar`; SHA-256 `846f77b138bdc51138ac4d40aa66786bc8fdce25bd151b9837284f659e0e3c79`. The following bullets preserve the earlier cuisine verification record.

- Common and client compilation passed. The full regression run passed **60 unit tests and 79 server GameTests**, including ten new server checks for locked cooking, scroll duplication, cookbook contents, two-diner serving counts, dietary tracking/save-load, Chef stock/restocking, equipment removal, actual arrow-damage events, smithing component preservation, participant-owned repeat rewards, and poison/potion ownership. Two new unit checks cover bounded dietary history and every new meal/feast recipe's decoding and distinct ingredients.
- All 63 new dish/feast/trophy item model references resolve, including their vanilla texture references. All generated recipes, trade sets, loot tables, and advancements loaded in the server run.
- The first server boot found an outdated dragon advancement predicate; the resource generator now uses the Minecraft 26.3 schema. A later run failed the existing wolf-defeat test's pack-spawn setup; the subsequent full run passed without a wolf gameplay or fixture change. This remains an intermittent test-fixture limitation, not evidence of a combat fix.
- Full regression log: `build/expansion-final.log`; alpha.2 packaging/final tooltip verification: `build/alpha2-package.log`. Live-client cookbook layout, Chef claiming, visual presentation, existing-world upgrades, and real two-client encounters remain manual release checks.

## Original design questions and later refinements

The implemented choices above supersede the original candidate values below. Three crop variants, regional influence cues, and ten shield/bow/armor powers now exist. Projectile teleporting remains deferred.

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

The October 13, 2026 [Seasonal Fabric Minecraft Server design specification v0.1](seasonal-server-design-v0.1.md) is saved for later planning. It covers paid admission, seasonal resets, validated leaderboards, cosmetic awards, a permanent Hall of Fame, PvP zones and death rules, and exact 12 × 12 × 12 bonfire sanctuaries. The supplied specification preserves agreed directions, proposals, example prices and remaining decisions separately. Its sanctuary design is a future server proposal; it does not change the existing bonfire ward or the active implementation order.

The first selected mount is a mountaintop griffin; raw rabbit and gradual trust are proposed taming mechanics. The bonfire must prevent wandering hostiles such as creepers from entering its protected volume as well as suppress hostile spawning. Mount storage and paid vet recovery remain part of the companion plan.

Confirmed direction for the ships, mounts, and base expansion: mounts and the fuel-free bonfire are the first user priorities; lost mounts have paid recovery through a vet villager. Bonfire protection targets about 75 blocks horizontally, 5–10 below, and 20 above. Pirate exploration blends fantasy and cannon-focused naval warfare, ships and airplanes use bounded editable platforms, and pirate access plus flight are available before the Ender Dragon. These new access rules do not alter the existing boss/ecosystem gates. See the brainstorm's confirmed decisions for details; implementation has not started under this planning request.

The same brainstorm also includes a proposed modern inventory hub: a clearer inventory layout, richer item and character information, and eventual accessory, companion, storage, and journal integration. Its first proposed increment builds on the existing inventory rows and Sort action; later panels depend on their respective systems.

The October 10, 2026 [ships, mounts, and base upgrades brainstorm](vehicles-mounts-and-base-ideas.md) records buildable battleships and airplanes, cannon ammunition, a pirate dimension, the full proposed mount roster and portable storage, Dump Chests with Sorting Tubes and filtered Sorting Chests ending in storage/refiltering/disposal options, a large-radius bonfire, and three accessory slots. Its design suggestions and prototype order remain open for discussion and do not change the active roadmap.

Status: **all sixteen boss landmark families implemented in development source; unified Journal implemented; discovery maps next in the exploration plan, other ideas saved for later**. Follow the [ordered expansion plan](boss-structures-plan.md). The source brief is preserved verbatim in [future-expansion-notes.txt](future-expansion-notes.txt); examples and numerical targets below are proposals, not current functionality.

| Later idea | Preserved direction and dependency |
| --- | --- |
| Boss discovery structures | All sixteen families now implemented, including the regional excavation, altar, hut, nest, wreck, hive, Nether, and End sites. Live terrain, visual and rarity acceptance remains; future variants can reuse the existing pipeline. |
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
