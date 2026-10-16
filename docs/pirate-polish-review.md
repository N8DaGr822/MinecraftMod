# Pirate Seas polish review

## October 16 reliability fixes following the comparative audit

Implemented the five reproduced failures and the related deck-menu/client-interpolation corrections. The historical audit below records the original failures; they are now permanent regression cases rather than temporary probes.

- **Deck carry:** restore grounded state after a horizontal carry only when geometry still supports the entity. Jumping releases carry. This fixes the every-other-step drift without forcing airborne players onto the ship.
- **Crouch safety:** include ship shapes in the native player's downward edge-support query. Ordinary terrain checks and real deck-edge protection remain active.
- **Docking walkers:** recognize supported living entities, validate their destination boxes, and rotate/translate them with the restored ship. Swimmers still block docking; cramped positions can reject docking rather than place people inside blocks.
- **Boarding clearance:** validate captured geometry before mounting, including earlier seat indices a passenger can inherit after another rider leaves. This conservatively rejects boarding when those positions are obstructed; it does not alter or destroy construction.
- **Ladders:** resolve climbable blocks using the entity's position transformed into ship coordinates, preserving native climbing movement rules. This adds ladder behavior, not general moving-block simulation.
- **Deck menus:** an owner supported on a large deck can keep the ship menu open beyond the previous eight-block radius. Ownership, removal and off-ship distance checks remain.
- **Client movement:** use the native three-step linear interpolation handler and interpolated render yaw. Collision continues to follow the entity transform; visual/rider agreement under network latency still needs live multiplayer acceptance.

These changes stay within existing upright snapshot ships. Navigational shoreline clearance, a richer sailing HUD, operational sails, persistent wreck salvage, engines/flight, working block entities and automated cargo remain separate items in the roadmap below. No dependency, save-format change, release version or JAR was added.

Validation: `classes clientClasses` passed; all 34 ship GameTests and all 45 pirate GameTests passed. Nine ship cases were added, including the six archived audit probes and three additional edge/docking/menu cases; sustained turning, rotated ladder lookup and swimming-height boarding strengthen those cases. Logs: `build/ship-polish-final.log` and `build/ship-polish-pirate.log`. `git diff --check` passed. This is targeted verification, not an unfiltered `fullCheck` or live two-client sign-off.

## October 16 comparative ship audit — not ready for gameplay sign-off

This follow-up supersedes the earlier implication that a passing single-step deck test established reliable walking aboard. Six additional server probes reproduced five failures. The existing implementation is a useful custom-built vehicle, but it does not yet provide dependable moving-platform behavior or Eureka-style functioning block worlds. This audit changes documentation only; earlier implementation work remains intact.

### Reference mods and the appropriate comparison

| Reference | Relevant behavior | Our current implementation / gap |
| --- | --- | --- |
| [Small Ships](https://modrinth.com/mod/small-ships) | Purpose-built medieval vessels with cargo, passenger/gunner seating, sail controls, repair and upgrades. Its current description includes wind/weather handling; wood choice is cosmetic. | Three build footprints, separate cargo, passengers, crew, refits and broadside combat already exist. Dedicated player gunner stations, sail operation, wind response and comparable handling feedback do not. |
| [Eureka](https://github.com/ValkyrienSkies/Eureka), [block mechanics](https://github.com/ValkyrienSkies/Eureka/wiki/Blocks) | Helm assembly/disassembly uses Valkyrien Skies; floaters, balloons, engines, ballast and anchors add physical controls. | We capture allowed block states, remove world blocks and render a moving snapshot. Building works while docked. Moving editing, block entities and general native block interactions are not implemented. Engine power and flight are separate features, not small collision fixes. |
| [Shippy Ships](https://modrinth.com/mod/shippy-ships) | Designed sail vessels with crew-dependent handling, wood-dependent characteristics, sail/steering animation, damage feedback and recoverable wreck cargo; naval weapons integrate with Old Cannons. | Crew affects reload/repair and has wages/provisions; it does not operate sails or change propulsion. Hull classes affect speed, but construction wood does not. Sinking drops cargo rather than leaving a persistent salvage wreck. |
| [Faster Boats](https://modrinth.com/mod/faster-boats), [Seaworthy Boats](https://www.curseforge.com/minecraft/mc-mods/seaworthy-boats) | Focused speed changes; Seaworthy adds material tiers, shipyard repair and configurable boat statistics. | Tier/class speed and repair already exist. Better controls and readable speed/reload/damage feedback matter more than copying another mod's exact balance numbers. |
| [Cargo Boats](https://www.curseforge.com/minecraft/mc-mods/cargo-boats), [repository](https://github.com/Landmaster/CargoBoats) | Automated item/fluid routes using docks, buoys, scheduling and powered boats. | Our hold is manual inventory. Automation would require route recovery, unloading permissions, fuel policy and deliberate chunk-loading limits. Treat it as an optional expansion. |

The strongest implementation reference for walkable custom ships is [Valkyrien Skies' entity collision integration](https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/1.20.1/main/common/src/main/java/org/valkyrienskies/mod/mixin/feature/entity_collision/MixinEntity.java): it transforms movement, tracks ship support and handles ship-relative ground queries. Our narrower collision injection does not cover all those paths. That historical 1.20.1 source illustrates the mechanism; it is not proof of a compatible dependency for Minecraft 26.3. Likewise, [Small Ships' historical vehicle source](https://github.com/talhanation/smallships/blob/main_old/src/main/java/com/talhanation/smallships/entities/AbstractWaterVehicle.java) is a seating/collision reference, not an arbitrary moving-block implementation. No external code was copied.

### Reproduced correctness failures

| Priority | Finding and reproduction | Smallest repair direction and constraint |
| --- | --- | --- |
| P1 | **Standing riders lose sustained deck transport.** Across twenty 0.2-block translations with ordinary downward movement between steps, a rider lost approximately 2 blocks relative to a ship traveling 4 blocks. `ShipCollisionMixin.darkspawn$carryDeck` calls native horizontal `move`, which can clear grounded state; the immediately following `ShipCollision.supporting` then drops support. | Preserve/recompute valid deck support across the carry operation. Do not blindly force grounded state: jumping, walking off, hitting obstacles and transferring to land must release support correctly. Keep a sustained regression, not only a one-step check. |
| P1 | **Crouching freezes deck walking.** A grounded player at the center of a launched deck moved 0.0 blocks for a requested 0.2-block horizontal movement while sneaking. Native edge safety does not see the snapshot through the collision paths it queries. | Extend ship support into the relevant sneak/edge-support checks. Preserve actual edge protection; disabling sneak protection globally is not a valid fix. |
| P2 | **Walking aboard prevents docking.** A stationary ship with its captain standing on deck returns “Move swimmers and creatures out of the docking area.” `PirateShip.dock` excludes mounted riders, but counts supported deck walkers as obstructions. | Distinguish supported walkers from external actors and reposition them safely after docking snaps heading. Continue rejecting swimmers and true obstructions. |
| P2 | **Late boarding can place passengers inside built blocks.** Launch with the future second passenger seat occupied by two captured planks; a later passenger still mounts successfully. Launch validates current seats, while `canAddPassenger` checks capacity rather than clearance. | Validate the actual selected seat on every boarding path, including recall and passenger changes. Reject or select a clear permitted seat; never delete the player's build to make room. |
| P2 | **Allowed ladders become nonfunctional on launch.** A supported captured ladder renders, but a player occupying its local position reports `onClimbable() == false`. | Add ship-local climbable lookup and appropriate movement behavior, or explicitly limit supported construction until implemented. A visual ladder with no climbing behavior is not functional block parity. |

These probes use the actual launched ship and server entity APIs. The sustained-transport probe explicitly sequences ship translation, entity base ticks and downward movement; it demonstrates a lifecycle defect, not a measured live-client drift rate. Reproduction source is archived locally in `build/ship-polish-audit/ShipsGameTest.probes.java`; results are in `build/ship-polish-audit/probes.log`. The temporary methods were removed from the registered suite after the audit, preserving its pre-audit source exactly. Promote them into permanent regression tests alongside the fixes.

### Additional source findings and polish gaps

| Area | Evidence / consequence | Acceptance before calling it polished |
| --- | --- | --- |
| Client movement | `PirateShip` has no interpolation-handler override; the native base handler is `NO_OP`. `ShipRenderer` reads current yaw rather than interpolated yaw. Live jitter has not been measured. | Add/test the appropriate client interpolation lifecycle, with rendered deck, collision and rider positions agreeing. Test pilot and observer clients under latency, reconnect and chunk tracking transitions. |
| Deck menus | `ShipMenu.stillValid` accepts an unmounted player only within 8 blocks of the entity center. Larger decks extend farther, so being aboard alone does not keep a ship menu valid. | Recognize supported presence on the owned ship while retaining distance, ownership and removal checks. Test bow/stern access and closing after leaving. |
| Shore handling | Sailing reuses full-height, enclosing-rectangle `clearWater` clearance. Empty rotated corners or space above a short build can block travel before visible geometry touches land. | Separate navigational collision from safe block-restoration clearance. Verify diagonal shore approaches and bridges without allowing restoration through terrain. |
| Construction contract | A complete rectangular plank floor is mandatory; only whitelisted blocks without block entities/fluids are captured. The capture volume also accepts disconnected allowed blocks. | Explain size/height limits and unsupported blocks before launch. Keep full-platform building available while docked. Moving construction and working chests/furnaces require a separately scoped design. |
| Sailing feel | WASD drives fixed class/tier propulsion, with reverse and stationary turning. Wool sails, wood choice, wind and crew availability do not drive speed. | First tune acceleration, braking, turning and collision feedback through survival play. Then decide whether sail deployment and crew handling should change the rules; do not silently invalidate existing builds. |
| Crew and passengers | Player capacity is four; NPC crew use roles, wages, provisions and seated travel, with roaming/expeditions while docked. | Clear seat/role/capacity feedback, safe dismounts, recall failure messages and reconnect behavior. Crew should visibly explain its existing reload/repair contribution before adding new obligations. |
| Naval combat | Side firing, ammunition, reload, crew assistance, targeting and protected areas exist. Both broadsides share a reload timer. Cannonballs target ships rather than providing general terrain/player artillery. | Show ready/reloading state and ammo choice. Test both headings/sides, allied/PvP boundaries, diagonal impacts and sinking recovery with two clients. Shared reload is a balance choice, not automatically a defect. |
| Presentation | No integrated speed/sail/reload HUD; action-bar producers can compete. No comparable rudder/sail/wreck animation. | Prioritize readable pilot feedback and damage states. Cosmetic roll/pitch must not desynchronize a currently upright collision platform. |
| Performance | Collision/support queries inspect captured blocks and allocate transformed shapes; maximum builds contain up to 1,536 blocks. No performance regression is measured. | Profile several loaded maximum-size ships with players/crew before choosing caching or spatial indexing. Avoid speculative rewrites. |
| Save and recovery | Existing snapshot, cargo, ownership and crew persistence checks remain valuable but do not establish client reconnect safety on a moving deck. | Reload/rejoin while seated and standing; unload/reload nearby chunks; lose an owner connection; verify exactly-once cargo recovery on sinking. |

The sixth probe, firing through empty air above a bare deck, **passed**: it did not damage the hull. A suspected broad hitbox problem was therefore not reproduced and is not listed as a confirmed defect. Rotated/corner impact precision remains an acceptance case.

### Dimension-wide polish acceptance

The expanded terrain and real-biome settlement work described below remains in place. Automated validation establishes generated resources, finite occupation slots, biome storage and protection boundaries; it does not establish visual quality or survival pacing. Play a complete route across plains, jungle, Nether, End, desert and snowy islands. Check dock approaches, mine entrance readability, ore accessibility, weather/color transitions, native spawns, safe settlement boundaries and defeated-town persistence. Existing chunks are not regenerated. A stored Nether/End biome does not turn the Pirate dimension into the Nether/End dimension or import every dimension-level rule.

Also exercise return gates with obstructed arrivals, discovery/compass clues at enlarged boundaries, contracts at preserved quest coordinates, faction and territory services, inventory menus at supported GUI scales and shanty playback. Balance the compass wait and loot economy using actual survival voyages rather than changing timers during a collision fix.

### Recommended implementation order

1. **Reliable platform:** sustained carry, sneak movement, jump/landing/shore transfer, all heading angles, clear seats and walker-safe docking. Make the reproduced failures permanent passing regressions.
2. **Reliable client:** interpolation, reconnect/chunk transitions, ship-relative menu access and ladders. Run two-client acceptance before declaring the ship walkable.
3. **Sailing and combat feedback:** better shoreline clearance, clear construction restrictions, speed/reload/ammo/crew feedback, then handling balance and damage presentation.
4. **Optional feature expansion:** operational sails and crew-dependent handling first if desired; persistent wreck salvage, working moving block entities, engines/flight and automated cargo routes are separate substantial capabilities.

Target the custom-build freedom of Eureka with the understandable controls and naval roles of Small Ships/Shippy Ships. Matching that player experience does not require cloning every feature or adding all of their dependencies. After restoring the registered tests, common/client compilation and all 25 existing ship GameTests passed again (`build/ship-polish-audit/regression.log`). Those passing regressions do not negate the five additional diagnostic failures. No JAR, version bump, push or live gameplay sign-off is part of this examination.

## October 16 biome and settlement expansion

Six additional island types extend the nine existing quest destinations. New islands use 129x56x129 terrain templates versus the earlier 113x56x113 hinterlands. The native random-spread spacing remains unchanged: its 144-block minimum separation leaves at least 15 blocks between the larger template bounds. Harbor reserves, old island IDs, quest coordinates and saved player progress remain compatible.

| Island | Actual stored biome | Site and exploration |
| --- | --- | --- |
| Meadowhaven | Plains | Protected faction warden, four houses, beds, workstations, irrigated farm, mineral gallery |
| Verdant Sanctuary | Jungle | Protected Tidebound warden, mossy sanctuary, two lodges, cocoa, bamboo, crops and mine |
| Ashen Garrison | Nether wastes | Four finite piglin occupiers, blackstone town, nether wart, quartz, gold and one buried ancient debris block |
| Starfall Sanctuary | End highlands | Protected Tidebound warden, purpur sanctuary, lodges, chorus, end stone and a finite relic cache |
| Sunscar Town | Desert | Four finite pillager occupiers, sandstone houses and well, cactus and mineral gallery |
| Frosthaven | Snowy taiga | Protected faction warden, spruce houses, beds, illuminated irrigated farm and mine |

Each site has a dock, reachable mine staircase and exactly one finite loot chest. Overworld resource variants have at most four diamond ore blocks; End islands deliberately do not add Overworld ore. New discovery clues and the Logbound Compass recognize all fifteen types, including the wider settlement bounds.

Biomes are written by the native decoration lifecycle to the chunk being generated, using its actual saved island structure references. This avoids changing the saved flat generator or predicting different random island positions. The island columns contain native biome holders, so weather, tint, environmental attributes and biome mob tables apply. Terrain and vegetation remain authored templates. **Limitation:** the flat generator's biome source still describes the ocean, so `/locate biome` cannot discover these patches; use the Logbound Compass. Nether/End islands retain the pirate dimension's Overworld rules, including water and beds. Native Nether creatures retain normal dimension-dependent behavior; authored piglin occupiers explicitly resist zombification.

Safe settlements reuse existing faction contacts and menus. A persistent, invulnerable warden prevents mob targeting/damage to players within 28 blocks; the outer resource wilderness remains dangerous. This is not PvP, explosion or player-build protection. Occupation spawns use saved finite slots and the existing global encounter population cap, never refilling after defeat. Old explored chunks and player builds are not regenerated.

Validation: common/client compilation; 45/45 pirate and 25/25 ship server regression tests; two focused biome tests cover the actual generation hook, native templates, wardens, protection boundaries, finite garrisons and save/load. Native asset checks cover bounds, loot references, resource budgets, clear spawn pads, sealed caves and reachable galleries. The generated overhead preview was inspected. Logs: `build/biome-pirate-regression.log`, `build/biome-ships-regression.log`, and the final focused recheck in `build/biome-final-check.log`. Live weather transitions, settlement appearance, exploration-generation performance, combat balance and multiplayer remain gameplay acceptance items. No JAR or version bump was requested.

## October 16, 2026 foundation review

Pulled `main` fast-forward from `42a6726` to `2f82f0f` before this review. The findings below supersede the earlier seated-sailing acceptance and compact-island assumptions. This is source review plus targeted verification, not live multiplayer acceptance.

### Findings and implementation priorities

| Priority | Evidence and player impact | Resolution / next acceptance |
| --- | --- | --- |
| P1: launched decks have no collision | `PirateShip.launch` removes world blocks; `ShipRenderer` draws the snapshot. Rendering supplies no movement collision. Leaving a seat therefore exposes water beneath the deck. | Add snapshot block shapes to native movement collision on both sides, carry grounded entities with ship translation/yaw, and dismount onto the cleared seat. Verify walking, falling, turning, jumping, shore transfer and reconnect with two clients. |
| P1: ship hitbox intercepts construction | `isPickable` remains true while docked and `interact` returns success even for held blocks. The invisible ship volume competes with ordinary block targeting. | Make docked ships unpickable, leave station-block menus available, pass held-item interactions through, and preserve cannon targeting independently. Validate every platform edge at all three tiers. |
| P1: bounds shrink during movement | `tick` sets a footprint box, but native `setPos` rebuilds it from the entity dimensions. Long vessels can lose their complete query footprint after moving. | Override position-based bounding-box creation to preserve the rotated hull footprint. |
| P1: islands lack a mining loop | Flat dimension layers contain stone/sand/water; features are disabled, biome carvers/features are empty, and original templates mostly fill stone/sandstone. | Add native finite ore deposits, dry cave galleries, descending entrances, vegetation, hills and broader shores around the original quest cores. No world reset or automatic regeneration of explored islands. |
| P2: discovery assumes tiny surface islands | `IslandDiscoveries.at` and compass study used a 24-block radius and Y60–84. Larger shores and mining fall outside this contract. | Extend the loaded-chunk survey and study bounds to encompass the 113x56x113 terrain envelope; retain objective offsets and discovery center identities. |
| P2: shoreline navigation is conservative | `clearWater` tests the entire rotated footprint's enclosing rectangle and full height, including empty corners. Turning beside land can fail before visible geometry touches shore. | Retained for safe docking and world preservation. A later change should distinguish navigation collision from docking clearance and measure multiplayer cost. |
| P2: exploration pacing needs play evidence | Compass study is nine minutes; original islands are repeated finite-cache stops. Added resources provide activity during study, but do not establish good timing. | Time several survival voyages before changing the study duration, loot economy or faction requirements. |

### Other boat mods examined

- [Valkyrien Skies collision implementation](https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/1.20.1/main/common/src/main/java/org/valkyrienskies/mod/mixin/feature/entity_collision/MixinEntity.java) intercepts movement, adjusts it against ship collisions, tracks the last ship stood on, and transforms world positions into ship space. This supports the architectural conclusion that drawing blocks and mounting passengers cannot provide a walkable deck by themselves. The local implementation is independent and considerably narrower: upright ships and conservative rotated shape bounds.
- [Eureka](https://github.com/ValkyrienSkies/Eureka) assembles ordinary block builds through a helm and depends on Valkyrien Skies for the physics backend. It is a useful design reference, not a verified drop-in dependency for this project's Minecraft 26.3/Fabric target.
- [Small Ships' AbstractWaterVehicle](https://github.com/talhanation/smallships/blob/main_old/src/main/java/com/talhanation/smallships/entities/AbstractWaterVehicle.java) explicitly enables entity collision, excludes passengers of the same vehicle from collision, and handles passenger orientation and dismounting. This is a useful vehicle reference, not evidence of arbitrary editable moving blocks. The inspected branch is `main_old`; no code was copied.

### Scope across the dimension

| System | Review conclusion / next work |
| --- | --- |
| Travel, return and saves | Keep personal return links, safe-arrival checks and one-time harbor placement. Test obstructed gates and simultaneous arrivals; do not rebuild old harbor terrain. |
| Construction, cargo and crew | Preserve costs, cargo components, ownership, block caps and seated crew. Building remains a docked operation; moving block editing and block-entity simulation are separate work. Existing reserved seat headroom still applies. |
| Combat and patrols | Preserve ammunition, PvP checks, harbor protection and finite patrol lifetime. Regression-test docked targets after targeting changes; playtest broadside feedback and diagonal shoreline movement. |
| Contracts, factions and officers | Keep exact landmark positions, personal objectives and finite shared caches. Larger terrain must not move the shrine, wreck cache, beacon or faction contacts. |
| Territory and home ports | Their 24-block contest and service areas describe the fort, not the entire island; do not automatically enlarge capture zones with terrain. Offline capture remains existing behavior. |
| Menus, Journal and audio | Existing features remain. Verify normal/minimum GUI scale, overlapping action-bar reports and actual shanty playback in a live client. No visual or audio sign-off is implied by source checks. |
| World generation and upgrades | New structures receive the hinterland; generated chunks remain untouched. The guaranteed cove gains terrain only when a world's harbor is first initialized. Ordinary deep ocean below the authored terrain still uses the existing flat layers. |

### Tradeoffs and verification boundaries

The moving shape boxes are conservative at non-cardinal headings: corners can collide slightly outside the rendered block, and narrow diagonal passages need live acceptance. This is not a full rigid-body physics engine. Ship construction still occurs while docked; chests/furnaces and other unsupported block entities remain excluded from launch snapshots.

Hinterlands retain nine authored variants instead of introducing a new chunk generator. Coal, copper, iron, gold, redstone, lapis and scarce diamond deposits use native blocks and normal mining drops. The quest cores and old template files remain stable. Resource quantities and terrain repetition require survival balance testing.

Verification for this pass:

- `classes clientClasses --max-workers=1` passed, including the final resource synchronization.
- `runGameTest -PgameTestFilter=darkspawn-gametest:ships_game_test* --max-workers=1`: 25/25 passed. Covers deck landing at cardinal and diagonal headings, deck translation, open-water fall-through, construction targeting eligibility, existing saves/cargo/crew and real cannonball hits. Log: `build/ships-polish-check.log`.
- Pirate dimension group: 42/43 initially passed. The remaining test's old outside-boundary coordinate was now inside the enlarged island; updated that fixture, then its focused rerun passed 1/1. New terrain loading, structure serialization and cave-study checks passed in the group run. Logs: `build/pirate-expansion-check.log`, `build/pirate-study-check.log`. This is not a claim of a later 43/43 group rerun.
- `verify-pirate-terrain.py` passed all nine hinterlands: bounds/unique positions, six common ore types, a maximum of four diamonds per variant, no cave air directly exposed to seawater, and bridge-to-gallery walking paths. It also runs the existing validator: ten landmark templates, loot references, four shanty hashes/subtitles, drink models and compass parity passed.
- Inspected `build/pirate-terrain-preview.png`; revised the initial disconnected square shoreline and sealed the mine approach before final validation. `git diff --check` passed.

The local JDK's Unix-domain socket connection failed during Gradle startup; a process-scoped `jdk.net.unixdomain.tmpdir` pointing to an absent build subdirectory allowed the JDK's TCP fallback. No persistent system or repository Gradle setting was changed.

No JAR was packaged, no release version changed, and nothing was pushed. Unfiltered fullCheck and live multiplayer/GUI/audio acceptance were not performed.

October 14, 2026. Source review, targeted automated checks and offline asset previews; not a live gameplay sign-off, fullCheck, release or new JAR. This inventory supersedes older planning text that still calls the compass, factions or officers wholly unimplemented.

## Implemented and reviewed

| Area | Current behavior | Remaining acceptance |
| --- | --- | --- |
| Travel and harbor | Craftable pre-dragon Sea Gate, personal return links, safe-arrival checks, one-time harbor/cove, obstruction-aware service additions | Two players arriving together; obstructed return; old-world harbor edits and safe respawn |
| Exploration | Six island types, finite shared loot, loaded-area encounters and personal Journal clues | Island repetition, terrain/lighting, ladder access in game, loot value and first-voyage pacing |
| Logbound Compass | Free first compass from Briggs, paid replacement, personal saved route, nine-minute island-only study, unvisited destination selection | Needle readability, travel time versus study time, long voyages and exhaustion of the local candidate search |
| Harbor contracts | Fifteen selectable activities: four independent contracts, three faction introductions, three repeat voyages, Garrick, First Binding and three officer commissions | Dialogue readability, finding the right activity, clear failure reasons, empty-inventory reward requirements |
| Factions | Three separate standings, membership/resignation/amends, local contacts and relationship-aware guards | Two players with different standings near the same guards; reputation balance and neutral-harbor behavior |
| Ship construction | Editable 7x15, 11x23 and 15x31 platforms; exact costs, build caps, launch capture and cardinal docking | Large custom builds, shoreline clearance, turn/dock convenience and reload with passengers |
| Cargo and identity | Owner-only menus, names/flags, 27/36/54 slots and paged transfers | All cargo pages at normal GUI scales; name/flag readability on custom builds |
| Combat | Ammunition/reload, first-mate orders, hull/fire/repairs, PvP checks and harbor naval protection | Broadside aim, cannon feedback, resource balance, collision and sinking/recovery with two clients |
| Patrols | Outlaws plus three faction colors, standing/provocation, capped spawning and timed cleanup | Spawn visibility, encounter frequency and performance with several large player ships |
| Crew and officers | Four standard stations, earned mixed-species hires, Garrick rescue, Mara navigation, Bram resupply, Nell shanties | Boarding/seating at maximum crew, restart/death behavior, readability and loss expectations |
| Audio and lore | Four streamed shanties, volume/range/stop logic, all seven personal seals and three independent endings | Actual mix, attenuation, stop/reconnect behavior and Admiral combat pacing |

## Fixes in this pass

- Cargo shift-click now uses synchronized hold capacity. Client menus have no server ship reference; dereferencing that reference could crash prediction when depositing cargo.
- Dismounting clears pending movement/fire inputs immediately, instead of allowing the previous command to remain active for up to ten ticks.
- Ship-menu actions reflect whether a ship exists and is docked. The fire-order button names the next action, and rejected renaming explains the dock/name requirement.
- Journal repeat voyages and officer commissions use named progress descriptions instead of numeric state arrays.
- Added `tools/assets/verify-pirate-assets.py`: validates the actual shipped NBT, loot references and recording hashes/subtitles, and supplies preview data without regenerating game assets.
- Updated stale planning references and repaired naval/progression Markdown table spacing.

## Expansion follow-through

The requested harbor services, compact menus, paid officer recovery, renown/infamy, spectral voyages, fort takeovers and personal Seven Seals endings are now implemented. See [Pirate expansion](pirate-expansion.md) for costs, rules and validation. The verification below records the earlier polish baseline.

## Remaining acceptance and optional future work

1. **Complete foundation playtesting before further expansion.** Run the two-player circuit below, especially ship conversion, cargo, saving and crew. Automated tests do not establish a comfortable sailing or multiplayer experience.
2. **Presentation and pacing.** Ship, harbor and shore menus now fit a 312x232 panel with tabs or paginated text. Check actual rendering at the minimum 320x240 scaled viewport, HUD overlap, island repetition, nine-minute study times and encounter frequency.
3. **Exceptional entity removal.** Sinking has paid named-officer rescue and ordinary rehiring. Garrick's pre-recruitment captive identity still has no automatic recovery if an administrator or another mod deletes the captive.
4. **Optional expansion beyond this request.** Formal faction promotions, shared-party navigation, faction skirmishes, additional island templates, home-port management, advanced ship classes, roaming/boarding crew and morale/wages remain future scope. Territory control provides ownership and services; it does not implement block-protection permissions. Moving decks retain seated sailing.

## Two-player acceptance circuit

1. Enter through different Overworld gates; obtain personal compasses. Confirm different return locations and preserved inventory after death/reconnect.
2. Accept different contracts. Visit the same cache, test empty-cache credit and repeated hand-ins, then abandon/reaccept an unfinished activity. Check repeat cooldown and personal Garrick ownership.
3. Build and name a ship, fill cargo with named/component-bearing items, hire crew, then launch with a passenger. Verify the passenger cannot steer, rename or withdraw cargo.
4. Sail/turn around shore and another ship. Dismount while firing, reboard, stop, attempt blocked docking, then dock safely. Restart docked and sailing; compare cargo, blocks, hull and crew.
5. Outside harbor, fight an outlaw and a faction patrol. Test two owners with PvP enabled and disabled; return to harbor with a burning ship. Check sinking drops once and player kills do not award captaincy.
6. Upgrade platform and cargo through both combat and exploration paths. Test all hold pages, full holds, partial Bram transfers and maximum crew seating.
7. Complete Mara/Bram/Nell and First Binding. Test shanty start/stop, Records/Master mute, range exit, portal travel and reconnect. Check menu/HUD/flags at multiple GUI scales.

## Verification

Passed using Java 25 and the default incremental build directory:

- `gradlew.bat classes clientClasses --max-workers=1` — common and client compilation, including the final UI/Journal edits.
- `gradlew.bat test --tests 'darkspawn.black.journal.*' --max-workers=1` — 3/3 unit tests.
- `gradlew.bat runGameTest -PgameTestFilter=darkspawn-gametest:ships_game_test* --max-workers=1` — 19/19 server tests, including the two new regressions. Log: `build/pirate-polish-ships.log`.
- `gradlew.bat runGameTest -PgameTestFilter=darkspawn-gametest:pirate_game_test* --max-workers=1` — 26/26 server tests. Log: `build/pirate-polish-travel.log`.
- Shipped-asset validator — seven NBT templates, loot references and four shanty hashes/subtitles passed. Existing preview checks passed seven template routes and five naval models; both offline previews were inspected. The naval preview now shows all three platform sizes and both progression paths.
- Parsed 70 pirate-related JSON resources; `git diff --check` passed.

These are targeted checks, not unfiltered fullCheck. No JAR was packaged, no release version changed and nothing was pushed. Live Minecraft rendering, sound mix, GUI-scale acceptance and a real two-client playthrough remain unverified.
