# World design and Logbound Compass

Status: future design, building on [existing Pirate Seas](../pirate-dimension.md). Working names do not change saved registry IDs.

## Arrival and Breaker's Rest

Current increment: [Briggs, survival compass acquisition, Cove Survey and new-harbor tavern/market buildings](harbor-and-contracts.md) are implemented. The following broader hub vision remains incremental.

Expand the current shared harbor into a dependable neutral starting hub: an ancient gateway overlooking the harbor, docks and ship services, the Rusty Anchor tavern, contract board, market and old lighthouse. Old Salt Briggs is an optional weathered, sarcastic guide who answers where the player is, who holds power, how to acquire a ship and where to find work. Later dialogue may recognize renown and infamy.

Use waves, gulls, bells, banners and visible harbor activity to establish atmosphere where assets support them. A proposed arrival title is The Shattered Seas, with “Where fortunes are made, legends are buried, and the sea claims its own.” Seven gateway symbols foreshadow the optional mystery. No forced combat, cutscene or tutorial chain. A missing lighthouse keeper and unusual lights offer another optional lead.

Keep the existing pre-dragon craftable gate and per-player Overworld return. Shared gateways must not share personal progression. The current 96-block naval protection radius does not implement complete faction neutrality or prevent all hostile mobs. Design those rules separately, including configurable arrival protection, building permissions and respawn safety. Preserve established harbor edits and finite loot when expanding existing saves.

## Islands, discoveries and events

Target tropical islands, neutral ports, Piglin settlements, Illager forts, coves, beaches, shipwrecks, ship graveyards, haunted/foggy waters, underwater ruins, cursed temples and legendary strongholds. Six island templates now provide coves, forts, wrecks, beacon towers, shrines and mangrove camps. Tune spacing against actual ship speed and player travel tests.

Future bounded events include Corsair/Fleet skirmishes (help either, attack, salvage or ignore), ghost ship sightings, treasure rumors, outpost raids, infamy-driven bounty hunters and rare captains/sea creatures. Start with controlled encounters at known sites; avoid an always-running naval economy or global entity scanning.

## Logbound Compass — user addendum

Working name: **Logbound Compass**. Requested loop:

1. Briggs gives the player a first compass at Breaker's Rest, calibrated to a nearby beginner island.
2. It points toward that destination even when the island is not visible.
3. Entering the island's designated boundary starts recalibration.
4. The player explores, finds treasure, fights or completes activities for approximately **8–10 minutes** while it recalibrates.
5. The compass updates to another island when recalibration completes.

The [compass prototype](logbound-compass.md) now provides the first cove, nine minutes of study inside its boundary, and continuing seeded destinations with saved arrival history. The user confirmed island-only timing, pausing offline and outside the dimension. Briggs now provides acquisition; shared-party navigation remains future work. Recommended interpretation to preserve the brief's sandbox rule: recalibration unlocks the next **guided destination**, while ordinary travel and independent discoveries remain available throughout. The current implementation leaves travel unrestricted; recalibration advances guidance only.

Current prototype rules: per-player progress, replacement items resume it, death preserves it, active server time only inside the discovered island, and a persisted sequence of unvisited seeded destinations. Briggs issues the first compass free; replacement costs one ordinary compass and requires inventory space. Open expansion rules: shared party progress, longer-route candidate selection and repeats, route exhaustion, and whether later timers vary. The countdown is nine minutes at normal server tick speed.

Stable destination identifiers and server-validated boundary entry are implemented. Destinations survive restart; route selection examines at most 25 nearby candidate starts on completion or an explicit retry. The guaranteed first cove and Briggs handoff are implemented. Longer voyages and local route exhaustion still need acceptance testing.

## Territory implementation and further proposals

Strategic forts, ports, shipyards, coves and resource islands may change ownership. **Skullwatch Fort** illustrates one reusable structure: Iron guards/prison/contracts, Corsair black market/smuggling, Drowned haunting/vault, or player crew/quartermaster/defense activities. Persist shared ownership and change controlled decorations/NPCs without overwriting player builds. Saltwatch Fort capture is now implemented: defeat its garrison and hold for sixty uncontested seconds. Offline owners can lose control; capture preserves all blocks and cargo. Ownership gates fort services rather than vanilla block editing. See [current rules](../pirate-expansion.md). Additional port types and faction-specific decoration changes remain proposals.
