# Docked worker: cargo unloading

October 20, 2026. Approved first job: unload ship cargo into a captain-designated chest. Implemented in source; live acceptance remains and no JAR is packaged.

## Captain controls

1. Dock, hire a Shipworker and recall any shore expedition/shore leave.
2. Place a normal **single chest within 16 blocks** of the ship. Keep its lid clear and build a dry, walkable connection to the deck. Double chests, trapped chests, unopened loot chests and other storage types are outside this first version.
3. Open the ship screen's **Job** tab and choose **Mark chest**. The screen closes. Within 30 seconds, use the desired chest while standing within six blocks of it. Selection identifies this particular ship; it does not choose another nearby vessel.
4. Return to Job and choose **Start unloading**. Keep the captain alive and within 48 blocks in the same dimension. The worker walks between a deck pickup point and the chest, delivering at most eight items per trip from unlocked hold slots.
5. Use **Pause unloading** to stop work or **Pause and recall** to seat the original crew. Launch pauses the job and uses the existing loaded-crew recall rules.

The visible status reports fetching, walking, delivered, full chest, blocked route, missing worker, absent captain, unavailable chest or empty hold. Full destinations and temporarily blocked routes retry while the order remains enabled. Starting shore leave or an expedition requires pausing the job first.

**All cargo is eligible, including repair planks and ammunition.** Remove anything you want to retain before starting. Existing automatic hull repairs and Bram's inventory-to-hold resupply remain unchanged.

## Ownership and recovery

Only the ship owner can mark, start, pause or recall. Chest access respects the native lock, lid obstruction and pending loot-table checks. Marking a new chest replaces the old destination. Replacing or breaking the selected block entity pauses effective delivery; explicitly mark or restart to bind its replacement. The destination is not a protected/private chest: ordinary shared-world chest access remains unchanged.

Cargo stays in the ship hold until the worker physically reaches an accessible position beside the chest. Each transaction inserts only the count the destination can accept, then removes that exact count from the hold, preserving components. There is no separate worker-carried inventory, generated cargo, escrow or delayed subtraction. Unlocked cargo capacity and native item stack limits apply.

The destination is saved with the ship. Jobs load paused and require an explicit resume; offline time performs no work. Paused workers remain stationary unless given a shore order or recalled. Missing/unloaded crew are never replaced with new entities. Navigation uses a bounded path search and refuses unavailable surrounding chunks rather than loading terrain for the job. A route over 24 path-search blocks or an unsuitable navigation attribute can remain blocked even if a longer detour exists.

Fresh Testing Camp guides explain the job; existing pirate supplies already include the shipyard, helm, emeralds, planks and ordinary chests needed to test it. Existing camps are unchanged.

## Verification and live acceptance

Targeted server checks cover exact partial transfers, full destinations, component preservation, owner-only commands, obstruction, absent captains, missing crew, saved destination with paused reload, original-crew recall and native navigation. This is not a full-server backup/restore guarantee: the ship entity and chest remain separate normal Minecraft save records.

Live playtesting should check the five-tab layout at minimum GUI size, chest-selection feedback, a dock/gangplank route, a blocked route, filling/removing the destination, reconnect, two captains near each other, and departure after recall. Confirm the first batch costs exactly the delivered items and that ammunition/planks unload only when intended.

Automated verification: common/client compilation passed. The five focused unloading tests passed, followed by all 45 ship server tests (including those five) for shared cargo, crew, launch, docking and combat behavior. The initial movement failure exposed a grounded-dismount issue and insufficient test chunk tickets; both were corrected before the passing native-walking run. Logs: `build/docked-worker-navigation.log` and `build/docked-worker-ship-regression.log`. Live GUI, docks and reconnect acceptance remain open.
All three Testing Camp placement/supply tests also passed with the new guide page (`build/docked-worker-camp.log`). No JAR was packaged.

The subsequent boarding regression found another route-retry edge case: a worker sampled mid-jump could be frozen by disabling AI when native navigation refused an airborne start. The job now leaves physics active until the worker lands, then retries without transferring cargo remotely. All 45 pre-existing ship checks, including actual worker movement, passed in `build/boarding-final-ships.log`; that run's separate boarding-arrow failure is tracked in the [boarding guide](patrol-boarding.md).
