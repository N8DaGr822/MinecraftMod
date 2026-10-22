# Focused two-client acceptance — October 22, 2026

Status: **not run**. No Minecraft/Java processes were present when checked. This agent session cannot operate native game windows. No live-client, network or visual pass is claimed. Implementation and headless regression work can continue, but this checkpoint remains open.

## Required setup

Use two real clients connected to one disposable test server with identical current source/build content. Record the exact build label or source snapshot, mod list, GUI scales, server settings and player count. Preserve existing worlds. An older JAR does not test current source; packaging requires an explicit build request. Development runs may use current compiled source without packaging.

## Focused circuit

| Check | Procedure and pass condition | Result |
| --- | --- | --- |
| Ships | Pilot plus standing passenger: sail, turn, crouch, jump, dock and reconnect in each role. Restart with cargo aboard. No drift, lost/duplicated cargo or stale controls; both clients agree on position. | Not run |
| Nexus | Opt in both players, navigate entry/courtyard, begin, retreat and reconnect. Verify objectives, return locations and recovery without duplicate rewards. | Not run |
| Storage/UI | Use pouch, quiver and pack; reconnect and cross supported profiles. Inspect inventory, ship HUD and menus at normal/large GUI scales. Contents remain intact and text/controls remain usable. | Not run |
| Vehicles | Place, pilot, dismount, store/release and recover carpet, aircraft and mech once implemented. Observer checks motion; pilot checks camera/headroom, walls, slopes and dimension rejection. No duplicate claims or suffocation. | Not run |
| Guardians | Observe warnings, animations and music transitions from both clients, including death, departure and reconnect. Cues remain readable and audio stops correctly. | Not run |

Record observations and failures against the tested snapshot. Headless GameTests establish selected server behavior only; they cannot close these rows.
