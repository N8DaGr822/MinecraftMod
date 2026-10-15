# Pirate Seas polish review

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
