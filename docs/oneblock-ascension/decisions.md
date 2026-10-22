# OneBlock Ascension foundation decisions

Historical foundation decisions: M0/M1, October 19, 2026. [M2 bridge, archival and protection changes](bridges.md) now supersede the deferred-M2 limitations below.

Implementation scope: M0/M1, October 19, 2026. The user selected a separate destination and separate item inventory. This is the first playable foundation, not completion of the full Ascension design.

## Runtime and existing patterns

Retain Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, GeckoLib 5.5.7 and Gradle wrapper 9.7.1. The existing `1.18-SNAPSHOT` Loom declaration resolves to 1.18.4 in the current cache; it is not an immutable pin. No dependencies, release versions or loader architecture were changed. Architectury, NeoForge and optional automation adapters are not introduced.

The Loader and Fabric API artifacts were checked against [Fabric's Loader repository](https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/) and [Fabric's API repository](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.162.0%2B26.3/). Minecraft APIs were checked against the locally resolved 26.3 sources and compiled/runtime-tested artifacts. The version-specific Gradle compatibility page could not be retrieved; the local wrapper's successful Java 25 compilation/runtime checks are the compatibility evidence for this checkout.

Use the project's existing SavedData codecs, Fabric server events, Brigadier player commands, OneBlock harvesting, and synchronous inventory capture/apply. No service framework, loader adapter layer or database is needed. The existing Nexus command registration is the player-command precedent. New mixins enforce claim permissions before native block interaction and at resolved placement/fluid targets; callbacks protect direct entity interactions and player-sourced living-entity damage.

## Coexistence and storage

- `darkspawn:oneblock`, its shared island, its `oneblock` SavedData and existing player profile remain unchanged in meaning. No automatic migration or resource transfer occurs.
- `darkspawn:ascension` is a separate void dimension. Restart a server after installing a requested future play build to load the new dimension. The GameTest world explicitly lists it in its flat preset.
- Ascension has a third saved personal possession profile: expanded inventory, armor/offhand, Ender Chest, inactive armor loadouts and accessories use the existing capture/apply mechanism. Character unlocks, XP, hunger, journal knowledge and other non-item progression remain shared, as in legacy OneBlock.
- Return outside before switching modes. Native alternate dimension transfers for players, loose items, vehicles and other entities cannot bypass profile swapping. Visits within Ascension do not swap inventory or change the saved outside return point.
- Companion recovery/capsule records and Noah's stored companion/bond ledger are dimension-local in Ascension. Existing outside/legacy records remain in their original storage. This prevents recovering outside cargo into the new mode; Noah bond/cooldown state is consequently independent there as well.
- Player state adds `darkspawn_ascension_other` and `darkspawn_ascension_inside`; old saves default to an empty Ascension profile. Native death/keep-inventory rules apply only to active items; the existing death-copy hook retains inactive profiles.
- The dimension's `ascension` SavedData contains schema 1, saved geometry, next allocation index, team/owner/member UUIDs and each team's existing OneBlock progress. Saved geometry or schema mismatches disable access/mutation while retaining decoded records for migration. There is no editable live geometry configuration. [M6 hardening](hardening.md) now rejects unreadable/partial Ascension save files without replacing them and verifies SavedData backup fixtures. Full-world restore acceptance remains open; there is no automatic repair.

## Team and island rules

Teams have an owner and up to five additional members. Only the owner invites an online, unassigned player. Invitations expire after 60 seconds and require explicit acceptance; invitations are transient across restart. Membership and ownership are durable. Members can leave only outside both modes. Owners cannot leave in this stage; transfer, kick, dissolution, archival and cell recycling are deferred. Team names are labels (1-32 characters), not unique identifiers.

Allocate durable cells on a spiral grid, 250 blocks apart, at Y80. Allocation never reuses a previous index and is bounded to 1,000,000 cells. Validate the next claim against the world border before allocation. Each claim includes X/Z offsets -100 through +100 at all heights. Neutral columns are read-only until M2. Starter terrain is 5x5 at Y80, with the source at the center and bedrock beneath it. Validate the starter volume before writing any part; an obstruction cancels initialization without overwriting the obstruction.

Creation reserves a cell; explicit home/visit travel initializes terrain. Core recovery checks only initialized, loaded islands with an online member in the claim. Idle islands are not deliberately force-loaded for maintenance and have no offline automation.

Every team currently uses the existing six resource phases and manual thresholds: 300/600/800/1000/1200, then endless End. Progress, supply attempts and creature attempts are independent per team. Native harvesting rules and the existing per-tick duplicate-break guard are reused. This is not the M3 state machine or the M4 three-phase objective system.

## Commands and travel

| Command | Behavior |
| --- | --- |
| `/ascension create <name>` | Reserve a new team island while outside both modes |
| `/ascension invite <player>` | Owner invites an online player |
| `/ascension accept` | Accept a current invitation while outside both modes |
| `/ascension home` | Travel to the player's own team island |
| `/ascension visit <player>` | Visit an online player's team island; visitor must belong to an Ascension team |
| `/ascension return` | Restore the outside location and possessions |
| `/ascension next` | Manually advance a ready phase while safely on the player's own claim |
| `/ascension leave` | Non-owner leaves a team while outside both modes |
| `/ascension info` | Show team role, member count, home coordinates, phase and command help |

Travel uses the existing three-second stationary countdown and safety checks. Damage or movement cancels it. The existing inventory Return button works inside Ascension; entry/team management and phase advancement use commands in this foundation. Respawn selects the player's own team island even when death occurs during a visit. A blocked visitor landing cancels travel without clearing blocks. Own-home emergency landing follows legacy behavior, clearing only the two blocks above the core and dropping them normally.

## Protection boundary and remaining work

M1 enforces player block breaking, block/container interaction, actual block placement (including redirected scaffolding and the second bed half), bucket filling via world interaction permissions, bucket pouring, direct entity interactions and player-sourced living-entity damage. Remote inventory-hub chest access observes the same world interaction check. Protection is server-authoritative; clients may briefly predict a denied action until corrected.

This is basic ownership protection, not comprehensive anti-grief support. M2 still needs explosion, piston, flowing-fluid, fire, machine and cross-boundary environmental handling, bridge ownership and archival cleanup. Native item pickup, walking, and non-item character progression are not team-isolated. Operator world-edit commands remain administrative authority. Third-party mods are not claimed compatible without adapters/tests.

Next M2 sequence: (1) pure geometry tests for allocated cardinal neighbors, 49-column gap, seven-column width and Y72-96 bounds; (2) durable ownership per placed bridge block and permission checks; (3) resumable ownership-only archival cleanup; (4) supported environmental mutation checks without chunk forcing. No automatic bridges, new progression phases, encounters, collectors or auto-miners were added in M1.

## Verification

Verified October 19, 2026: common/client compilation passed; all five selected OneBlock unit tests passed (four Ascension ledger tests and one legacy progress test); all 48 selected server tests passed: Ascension 8, legacy OneBlock 9, mount care 9, Noah 13, field utilities 9. `git diff --check` passed. Earlier test-fixture compilation and missing test-preset dimension failures were corrected before these successful runs. No play JAR, version bump, push or fullCheck is part of this pass.

The Ascension checks exercise three inventory profiles, companion ledger separation, independent core progress, duplicate break rejection, visitor/container permissions, edge placement/buckets, invitations and role persistence, countdown cancellation/completion, player-save/death copying, actual death/respawn/drop behavior, obstructed-site safety and alternate transfer rejection. Allocation tests cover 10,000 unique grid cells, inclusive claim edges and the 49-column gap, capacity, reload and incompatible schema/geometry preservation.

```powershell
.\gradlew.bat classes clientClasses --max-workers=1
.\gradlew.bat test --tests 'darkspawn.black.oneblock.*' --max-workers=1
.\gradlew.bat runGameTest '-PgameTestFilter=darkspawn-gametest:ascension_game_test*' --max-workers=1
```

Also run the existing `one_block_game_test*`, `mount_care_game_test*`, `noah_game_test*` and `field_utilities_game_test*` groups because profile boundaries touch shared systems. Local Windows checks use a process-local `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=Z:\darkspawn-unavailable-sockets` to allow the existing socket fallback; no project/global JVM setting is changed.

Manual acceptance still required: two human clients, reconnect/server restart with an existing world, inventory UI and live gameplay balance. Codec reconstruction and headless server checks do not substitute for those playtests.
