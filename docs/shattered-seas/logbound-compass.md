# Logbound Compass: continuing island routes

Implemented in source October 14, 2026. The route now continues after each island, with nine minutes of island study and saved per-player visited coordinates. Briggs now provides [survival acquisition and replacement](harbor-and-contracts.md).

## Try it

Talk to **Old Salt Briggs** at the harbor for your first free, calibrated compass. For development testing, obtain **Logbound Compass** from Creative → Tools & Utilities, or `/give @s darkspawn:logbound_compass`. Enter Pirate Seas through the existing Sea Gate, then use the compass to calibrate it. Its native animated needle points to **Castaway Cove**, the guaranteed island centered at **192, 65, 0**, even beyond visible terrain. Use it again to read the route status.

The needle spins without calibration or outside Pirate Seas. It uses the native compass frames as prototype art. It cannot be retargeted by clicking a lodestone. After discovering the cove, it keeps pointing there while recalibrating. Use it to read the remaining minutes and seconds. When study finishes, it points to an unvisited seeded island. Arrival starts another nine-minute study period, allowing the voyage to continue. There is no fixed two-stop ending.

## Saved behavior

The route ID `darkspawn:first_cove` and arrival flag belong to each player. Calibration starts on item use in Pirate Seas; merely possessing the item does not start a route. Arrival is checked once per second for living non-spectator players with an active route. The boundary uses inclusive block coordinates **X 168–216, Z -24–24, Y 60–84**, matching the authored island footprint with a surface-height band. Flying above or swimming below this band does not count. This detects entry into the designated boundary, not a requirement to step on a particular block.

Arrival sends a one-time discovery message. Once calibrated, the player can arrive without holding or carrying the compass. Route and discovery survive normal save/load and death; ordinary inventory/drop rules still apply to the item. Replacement compasses resume the holder's state. Transferred or copied items update their display target to the new holder and never transfer discoveries. No rewards, renown or ship victories are awarded in this slice.

Old saves default to no route. Unknown future route IDs are preserved but display unavailable rather than being overwritten. Needle updates and arrival checks perform no chunk searches. Selecting the next destination uses one bounded native structure search; see below. No new network payload or permanent chunk tickets are added. Free exploration and existing ship/return travel are unchanged.

## Recalibration and continuing routes

Confirmed by the user: count time **only while on the discovered island**. The timer requires 540 one-second server updates while alive, non-spectating and inside the current destination's surface boundary. It pauses outside the boundary, outside Pirate Seas, while dead or offline. The item need not be carried. Server lag or altered tick speed changes wall-clock duration; this is nine minutes at the normal 20 ticks per second.

Using or duplicating compasses does not advance the timer. Save/load and death preserve remaining time. Existing first-slice saves with an already discovered cove start with the full 540 seconds, preserving their discovery. Every arrival is recorded in a saved coordinate history. Death copies keep independent history lists; sharing a compass never transfers this knowledge. Old completed two-stop saves retain both discoveries and resume with a full study period on their last island.

On completion, the server uses the dimension's native random-spread placement to examine at most 25 candidate structure-start chunks in a five-by-five placement-region neighborhood around the player. Candidates are sorted by distance; recorded discoveries and out-of-border centers are excluded before loading. A destination must have a valid native pirate island start containing the arrival point. This avoids re-selecting the nearest already visited island without consuming shared exploration references.

This search may load/generate structure-start information. It does not manually place templates, regenerate established islands, replenish caches or permanently force chunks. Different destinations may share the same authored layout; non-repetition applies to island coordinates, not template types. The six types are Castaway Cove, Saltwatch Fort, The Pale Gull, Lastlight Beacon, Tideglass Shrine and Rootbound Camp. New types appear in fresh terrain; density and spacing are unchanged.

The result is stored as the player's `darkspawn:charted_island` position, so restart and replacement items retain it. Players may receive the same island, but arrival and timer state remain independent. The destination has the same 49-block-wide footprint and Y 60–84 arrival band as the first island. Earlier independent exploration does not block this route; entering the boundary again after assignment counts.

If structures are disabled, none are found within the bounded search, or the result is outside the world border, calibration remains complete and the current target stays intact. Sail farther and use the compass to retry from your new location; a 30-second server-time cooldown prevents repeated searches. Searches are never repeated automatically every tick. Free exploration remains available throughout. The route has no fixed stop count, but each search remains bounded; a small world border or exhausted local area may prevent finding another island. Only guided arrivals enter this history. Independent visits before assignment remain eligible.

## Verification

Continuing-route increment: common/client compilation, all 15 pirate server tests and 3 Journal unit tests passed. Coverage now includes three-stop progression without repeats, independent death-copy history, partial timer persistence and migration from completed two-stop saves. Seven templates passed bounds, finite-cache and walking-route validation; the expanded layout preview was reviewed. Log: `build/pirate-routes-final.log`. Live pacing and visual checks remain; no JAR packaged.

October 14 recalibration increment: common/client compilation and all twelve pirate GameTests passed (five compass tests and seven travel regressions). Timer tests cover island-only progress, repeated-use rejection, pause conditions, persistence and death copying; route tests cover disabled structures, retry throttling, a real seeded island and both saved discoveries. The native GameTest world disables structures; the lookup fixture temporarily enables that flag for its synchronous test and restores it in finally. Native compass dispatch and its 33 model references were validated; documentation links and whitespace checks passed. Log: `build/logbound-recalibration-checks.log`. This is targeted source verification, not live client acceptance or a fullCheck run. No JAR was packaged.

Run `gradlew.bat classes clientClasses runGameTest -PgameTestFilter=darkspawn-gametest:pirate_game_test* --max-workers=1`. The pirate test class includes calibration/dimension checks, item transfer/replacement, boundary and spectator checks, independent player state, once-only arrival, native save/load, legacy defaults and death copying alongside existing travel regressions.

Live client needle appearance, item use while sailing and ordinary multiplayer transfer should still be playtested. Follow [world design](world-design.md) for remaining party-navigation and navigation-presentation decisions.

## Navigation feedback (October 14 expansion)

Use the compass in Pirate Seas to display the destination's island name, horizontal distance in blocks, number of guided island discoveries, and recalibration time as minutes:seconds. Leaving the discovered island displays **paused**; a completed search awaiting an available route displays **Ready to chart**. The readout appears on use, rather than continually overwriting naval action-bar messages. The counter records guided route arrivals, while Journal clues track the six island types independently, including unguided exploration.

The next destination name comes from its saved native structure template, with no extra terrain lookup for the readout. Older routes show **Charted island** until visiting that destination or charting a new one. The cached kind survives save/death alongside the existing route; island-only timing is unchanged.
