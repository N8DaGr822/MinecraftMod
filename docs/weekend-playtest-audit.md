# Weekend playtest audit — October 15, 2026

## Current status — October 22, 2026

Use the [current expansion plan](future-expansion-plan.md) and its feature guides for implementation status. Aircraft, magic carpet, all three utility bags, four thematic enchantments, fossil/ocean clues and all five regional encounters are implemented in source. The approved mech is tracked in [its guide](mech-prototype-design.md). The dated checks below are historical evidence, not certification of the current working tree.

Live two-client acceptance remains **not run**. No Minecraft/Java processes were running during this review, and this agent session has no native application controls. See the [focused acceptance record](two-client-acceptance.md).

October 22 current-snapshot verification: the approved ground walker is implemented, and unfiltered `fullCheck --rerun-tasks --max-workers=1` passed **133 unit tests, all 467 server GameTests and cuisine artwork validation**. The final run used a fresh disposable runtime (`-PgameTestRunDir=build/oct22-fullcheck-runtime`) after an earlier native test-runner stall; compilation retained its normal build directory. Test fixture corrections activate Nexus/Ascension actor chunks and isolate Shadow Incursion terrain. Evidence: `build/oct22-snapshot-fullcheck.log`; source/build-configuration SHA-256 manifest: `build/oct22-verified-snapshot.json`, fingerprint `291434043ab6401197b8fd0b09a2553888785ba6799d4c60688bf4f47b8b6e2e`. These local ignored evidence files are not distributed artifacts. No JAR or version bump was produced; live presentation, reconnect/restart and two-client acceptance are still outstanding.

Final staging also passed the six backup-utility tests and the staged whitespace check. One tab-only blank line in `RegionalEncounter.java` was removed after the recorded fullCheck/source manifest; no executable code changed after verification.

## October 19 rework status

This table supersedes older missing-feature statements below. It describes source changes, not a newly packaged JAR.

| Item | Current status | Remaining acceptance |
| --- | --- | --- |
| 1. Ship clearance | Sailing checks captured geometry and occupied seats; docking keeps strict restoration clearance. | Cardinal/diagonal shoreline approaches and bridges in the client. |
| 2. Multiplayer reliability | New regression confirms standing-player support transfers from a removed ship to its native save/load replacement, followed by owner boarding and fresh controls. | Genuine reconnect/restart, pilot and observer agreement, latency and chunk tracking. A simulated replacement is not a network reconnect test. |
| 3. Pilot feedback | Server-fed HUD shows hull, speed, reload/readiness, selected ammunition with automatic fallback, volley cap, repair availability and automatic-fire order. Text wraps at available GUI width; stale/disconnected/wrong-ship state is hidden. | Normal/large GUI scales and readability during combat. |
| 4. Noah presentation | Tempest reuses its custom model in flight; a native synchronized flag distinguishes it for observers and after load. Ordinary tridents delegate to the native renderer. | Flight orientation, Loyalty return, foil, lighting and observer view; armor and rider alignment still need live review. |
| 5. Balance | Current server defaults were inspected and retained: four skirmishes, eight patrols, two patrol attempts/minute, 60-second ambience and 20-minute regional events. | Measure survival pacing before changing rates, loot or combat numbers. |
| 6. Documentation | README now identifies fifteen island types and links here; current source work is separated from historical results and future proposals. | Record tonight's observations against an exact build label. |

October 19 verification: common/client compilation, all **40 ship GameTests** (`build/ship-polish-oct19-tests.log`) and all **13 Noah GameTests** (`build/tempest-presentation-tests.log`) passed. Noah asset validation, pirate terrain/resource/mine-access checks, six-biome-island validation and `git diff --check` passed. These are targeted checks, not a new fullCheck or live client sign-off. No JAR or version bump was produced.

### Tonight's focused route

Use matching client/server builds containing these source edits. This pass does not package that build.

1. Sail a short ship under a bridge and beside a diagonal shoreline; confirm a real hull/mast obstruction still stops it. Try docking under the bridge: stricter docking rejection is expected.
2. Put one observer ashore and one player standing aboard. Sail, turn, crouch, jump and dock; reconnect each role and restart the server with cargo aboard. Check deck drift, crew/cargo counts and ship identity before and after.
3. Watch the HUD while firing the last fire shot in automatic mode, switching ammunition, entering harbor, opening menus and dismounting. Check both normal and large GUI scales.
4. Throw Tempest in several directions, let Loyalty return it, and watch from a second client. Compare an ordinary trident. Check custom shape, glint, held/charging poses, armor fit and Thalassar rider position.
5. Record exploration minutes per new island/patrol, ore and loot per island, boss/Nexus completion time and deaths, and temperature interruptions with biome/gear/wetness. Note player count and server tick performance. Use those observations for the next balance changes.

## Mod-wide polish follow-up — October 16, 2026

This follow-up combines unfiltered automated verification with a targeted source review of cross-system behavior. It is not an exhaustive line-by-line audit or a live client playtest. The historical October 15 inventory below is superseded by the newer pirate and [Noah](legacy-of-noah.md) implementation guides where they differ.

### Concrete issues fixed

| Priority | Trigger and previous behavior | Surgical correction / behavioral difference |
| --- | --- | --- |
| High | Enter a previously built Nexus after lava is placed on an arrival pad or its supporting floor is removed. Collision-only validation accepted the entry and charged a Nether Star. | Validate the entire arrival footprint for clear air, safe solid support and world-border containment before charging or starting the run. Unsafe pads must be repaired; player construction is preserved. |
| Medium | Bring recruited pirate crew or a tamed dire wolf into a bonfire ward. Their original monster category caused friendly entities to be blocked or expelled. | Exclude explicitly recruited crew, faction contacts and owned dire wolves from ward hostility. Wild equivalents remain warded; deliberate boss exceptions are unchanged. |
| Medium | Open Thalassar through the Expedition Hub instead of direct interaction. The generic care menu exposed follow/breeding/transfer controls outside the conch lifecycle. | Keep him visible in the roster with conch instructions, disable Care, reject the server-side open request and invalidate a generic care menu constructed for him. Ordinary mount care and veterinary healing retain their existing behavior. |
| Low | The mod's styleable-armor item tag lacked a translated label for recipe viewers. | Added the English `Styleable Armor` group label without changing tag membership or recipes. |

The three new regression cases reproduced all three defects before the fixes and passed afterward (`build/mod-polish-reproduce.log`, `build/mod-polish-fixes.log`). The existing dire-wolf taming test also now checks ward classification.

### Review coverage

| Area | Source paths inspected / automated coverage |
| --- | --- |
| Storage and container items | Item conservation, storage routing, chest sorting and save components; storage, quiver, pouch and pack tests. |
| Travel and progression | OneBlock profile isolation/travel, Forsaken entry/return/fabrication, Nexus group entry/rewards/recovery; full dimension and progression tests. |
| Mounts, wildlife and base protection | Companion ownership, care/cargo, capture/recovery, friendly classifications and ward movement; full mount/ecosystem/bonfire tests. |
| Combat and equipment | Boss-heart modifiers, empowered weapons, legendary relic effects and temperature sampling; full boss/equipment/temperature tests. |
| Cooking, farming and fishing | Cooking result/remainder handling, food progression, animal harvesting/cooldowns; full cuisine/agriculture/fishing/brewing tests. |
| Exploration and presentation | Journal discovery/request limits, bounded landmark searches, hub UI source, registry/resource references, model/animation contracts and authored city traversal. |
| Pirate and Noah integrations | Included in the complete regression run; their dedicated recent reviews retain the detailed gameplay and visual acceptance lists. |

Asset checks passed for 1,485 asset JSON files, 4,593 model/texture/sound links, 643 sound events/subtitles and 541 readable PNGs. Additional validators passed for 80 brewing recipes/16 potion variants, 42 regional templates/84 archaeology deposits, 41 field-utility resources/six fish skins, Forsaken content and city traversal, 76 custom item model states, 13 mount projects, and 99 creature animation projects/690 clips. These validate authored files and contracts, not their live appearance or combat balance.

Baseline unfiltered `fullCheck --rerun-tasks --max-workers=1` passed 95 unit tests and all 316 server GameTests plus cuisine artwork (`build/mod-polish-full-check.log`). After the gameplay fixes, the same unfiltered check passed **95 unit tests, all 319 server GameTests and 142 cuisine model references** (`build/mod-polish-verified.log`). The final English item-tag label was added afterward and separately checked as valid JSON; it changes no gameplay. `git diff --check` passed. No JAR, version bump, commit or push is part of this review.

### Remaining polish priorities

1. **Live multiplayer acceptance:** save/restart and reconnect, death/recovery across dimensions, five-player ship boarding/standing, client observers and latency. Automated serialization and simulated-player tests cannot establish the entire live experience.
2. **UI and artwork acceptance:** inspect normal and large GUI scales, inventory and storage layouts, book wrapping, held-item poses, armor fit and animated creature/rider alignment. Tempest now uses its custom in-flight model; orientation, return, lighting and observer view still need live review.
3. **Gameplay balance:** measure natural encounter frequency, resource/loot availability, temperature pacing, Nexus difficulty and travel costs with real survival progression. Passing behavior tests does not establish that these feel fair or enjoyable.
4. **Feature expansion:** keep new boss move sets, authored Nexus dungeon content, deeper crew AI and other brainstorm items separate from bug fixes. Their absence is not evidence of a regression in the implemented systems.

## Historical October 15 results

This is a source audit, not a release manifest. No JAR was packaged. Historical brainstorms preserve ideas; they do not promise that every example is implemented. Use this status sheet and the linked feature guides when deciding what to test.

Subsequent item-art update: the borrowed item artwork noted below has been replaced by [custom Darkspawn models and materials](distinct-item-art.md). The validation counts below record the earlier audit, before that asset pass.

Pre-push verification: unfiltered `fullCheck --rerun-tasks --max-workers=1` passed with 95 unit tests, all 292 server GameTests and 142 cuisine model references (`build/prepush-fullcheck-verified.log`). The storage artwork test now verifies the separate inventory models and custom textures. The wolf defeat fixture now waits for entity-ready spawn chunks instead of assuming a fixed three-tick delay is enough; gameplay is unchanged. No JAR was packaged.

## Selected features available in source

| Area | Current implementation / acceptance guide |
| --- | --- |
| Pirate progression | Formal promotions, shared-party compass navigation, skirmishes, nine island types, beer/rum buffs, captured-fort home ports, saving. [Stage rules](expansion-stages.md) |
| Ships and crew | Three sizes, separate Standard/Clipper/Ironclad/Corsair Raider/Fleet Battleship/Drowned Ghostship specializations; solid/fire/chain/piercing ammunition; docked roaming and shore support; forgiving wages/morale. Standing decks are implemented; pilot/observer agreement needs live acceptance. [Exact costs and limits](endgame-expansion-playtest.md) |
| Size limits | 4/8/12 gun positions; ordinary crew and officers give maximum NPC counts of 10/14/18, with four player seats separate. Specialization does not add a fourth size tier. |
| Exploration | Boss-landmark finder, discovery maps, regional caches and brushable archaeology, cosmetic awakened rematches, regional boons/audio and bounded server settings. [Server playtest](server-playtest.md) |
| Fishing and livestock | Six swimming fish species, buckets, catches and recipes; pig truffles, chicken feed effects, goat dairy and mooshroom cream. [Field utilities](field-utilities-playtest.md) |
| Brewing and world progression | Six culinary potion families / sixteen variants, shared boss unlocks, planted Heartwood, dire-wolf companionship, regional harvests and reduced fossil/shadow natural spawning. Changes preserve existing builds. [Endgame rules](endgame-expansion-playtest.md) |
| Endgame | Group Nexus entry, five waves and an enhanced existing Void Eye Warden, saved run/recovery support, four legendary utility relics. [Nexus acceptance](endgame-expansion-playtest.md) |
| Testing Camp | Fresh camps contain supply groups through 25 and guide pages, including fish/animal supplies, brewing, ammunition, world harvests, Nexus and relics. Existing camps do not refill or rebuild themselves. |

## Brainstorm ideas still absent or only partly represented

This inventory distinguishes implemented portions from remaining proposals. Current linked feature guides take precedence over dated audit results.

| Original idea | Remaining work / current boundary |
| --- | --- |
| Buildable airplanes; power armor/mechs; DBZ-style cloud; magic carpet | Aircraft and magic carpet are implemented. See [mech status](mech-prototype-design.md) for the approved ground walker. Wearable power armor and a DBZ-style cloud remain proposals. |
| Material-specific naval warfare | No complete wood/copper/iron/diamond plating simulation, solvent/dousing/TNT ammunition roster or individual component destruction system. Current classes and ammunition use shared hull combat. |
| Full faction vessel roster and autonomous crews | Specializations reuse editable platforms. Moving decks, docked unloading and optional NPC patrol boarding are implemented. Autonomous sailing/docking, richer crew stories and general base workers remain future scope. |
| Thematic enchantments | Hunter, Butcher, Forager and Soulward are implemented. Venom Guard, Shadow Sight and Deep Diver remain proposals; live balance acceptance remains. |
| Dedicated utility bags | Ingredient Pouch, all-arrow Quiver and six-slot Explorer’s Pack are implemented. Live controls, reconnect and multiplayer acceptance remain. |
| Full archaeology/fishing proposal | Fossil reconstruction/display and the optional Kraken ocean-clue chain are implemented. Extinct-seed progression remains future scope; live pacing and visuals remain open. |
| Expanded animal interactions | The selected pig/chicken/goat/mooshroom behaviors exist; a further rabbit/bee behavioral expansion is not included. |
| Full regional transformation | Implemented unlocks are controlled harvests, cultivation, companionship, loot and spawn changes. No automatic biome rebuilding or new beneficial mushroom ecosystem. |
| Awakened mechanics and cosmetic roster | [All 16 awakened families now have one new warned sequence](awakened-boss-patterns.md), alongside existing scaling and cosmetics/trophies. Complete new move sets, proposed capes/banners/trims and distinct awakened models remain future work. |
| Wildlife, elites and regional events | Wolf Hunt, Fungal Bloom, Shadow Incursion, Soul Storm and End Rift are implemented as bounded optional encounters. Additional species and larger ecosystem transformations remain proposals. |
| Full Nexus dungeon | Entry hall, encounter courtyard and cooperative arena loop are implemented. Live room/start/recovery acceptance is pending; unique final-boss art/mechanics and a larger dungeon remain future scope. |
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
4. Exercise save/restart, death/recovery, shared navigation, docked crew recall, armor changes and nearby storage permissions.
5. Rehearse Nexus entry, death, retreat and recovery in a disposable world; then assess real progression, combat difficulty, cooking/brewing overlap and exploration frequency.

Prior unfiltered verification passed 94 unit and 291 server tests; the subsequently added fungal-harvest test passed in the focused five-test endgame run. Those historical results are detailed in the endgame guide. Live five-player gameplay, balance, visual acceptance and third-party mod compatibility remain unverified.
