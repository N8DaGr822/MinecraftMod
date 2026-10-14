# Ship identity, first binding and named officers

Implemented in source October 14, 2026. This extends the existing seated-sailing system. No release or JAR is included.

## Ship management

Open the ship menu and select **Refit/Crew**. Nearby owners can name a docked ship (32 characters maximum; blank clears the name). Flags cycle between independent black and the captain's current faction: red Corsairs, blue Iron Fleet, cyan Drowned. Flags are cosmetic and never override reputation or targeting. Leaving a faction retains an existing flag until changed. The stern flag renders while docked and sailing without placing world blocks.

Cargo starts at 27 slots. At the harbor, Captain rank unlocks a 36-slot hold for 16 iron ingots and four emeralds. Veteran Captain unlocks 54 slots for another 32 iron and eight emeralds. Cargo rank is independent of platform size; upgrades preserve contents and item components. The Hold button switches pages of up to 27 slots. Locked and hidden slots cannot be used through stale clicks or shift-click transfers.

## Optional commissions

Accept these personal commissions through Briggs or the harbor board before completing their objectives. Claim at the harbor, then hire through Refit/Crew while your ship is docked at the harbor. Each officer can be recruited once per captain. There is no additional hiring payment after claiming the commission. Existing ordinary and faction crew remain available.

| Officer | Commission | Job aboard ship |
| --- | --- | --- |
| Mara Tidewise, navigator | Beacon bearings: climb Lastlight Beacon to its lantern after accepting | Reports the captain's existing Logbound Compass route every six seconds while sailing; does not reveal new islands or skip recalibration |
| Bram Ledger, quartermaster | Supply ledger: inspect Rootbound Camp's cache, then deliver eight iron ingots and sixteen oak planks at claim | Resupply button moves the captain's oak planks and solid/incendiary ammunition into available unlocked hold slots while docked; preserves components and partial stacks |
| Nell Saltvoice, shanty singer | Lost songbook: inspect Pale Gull's wreck cache after accepting | Play/Stop shanty controls a rotating playlist of four user-supplied recordings |

Nell now sings **Chase the Sun** (1:57), **The Blackened Sail** (2:32), **Brothers on the Water** (2:38), and **Haul Away, Hearties** (2:36), in that order with a twenty-second rest between tracks. User-supplied MP3s are converted to streamed mono Ogg Vorbis so sound follows the ship and attenuates with distance. Original files in Downloads remain unchanged. Playback requires Nell aboard in Pirate Seas and a living listener within 32 blocks. Each client plays at most one nearby singer, retaining that ship until it becomes ineligible. Leaving range, disconnecting, changing dimensions, losing Nell or stopping playback releases the stream. Records and Master volume control audibility; muting pauses playlist advancement. The ship's play/stop preference survives saves. Listeners start tracks locally; entering range or unmuting restarts a track rather than seeking to a shared timestamp. The placeholder melody is replaced, and no lyrics were transcribed. Source/output hashes, duration, sample rate and measured peaks are recorded in `tools/audio/shanties.json`. All three officers are native Villager crew with persistent captain loyalty and separate roles; they do not replace the lookout, repair worker or first mate.

## Seven Seals: The First Binding

Inspect Tideglass Shrine's cache to discover a personal Journal clue: seven rings surround a closed eye, with a lantern marking the first binding. No faction membership or offering is required for this discovery. Accept **The First Binding**, visit Lastlight Beacon's lantern, and return to claim eight emeralds once. The signal hints at a prison and warns against waking the remaining lights. This establishes the mystery; it does not break a seal or complete the Seven Seals storyline.

The binding contract and three claimed officer commissions each count once toward independent captain milestones. Repeatable voyages still do not inflate that count. Progress survives death and saves. Legacy saves default to no new story knowledge, no officer commissions, an independent flag and a 27-slot hold; existing cargo and crew remain intact.

## Acceptance checks

Common/client compilation passed, along with 26 pirate server tests, 17 ship server tests, four Journal server tests and three Journal unit tests. These targeted checks cover personal objectives, exact costs, once-only claims/recruitment, native saves, ownership, remote menu rejection, cargo paging and partial transfers, officer loyalty and playback state. Live acceptance still needs ship-menu layout, stern-flag placement on custom builds, audible melody timing/volume, and multiplayer boarding/reload checks. Richer officer conversations and synchronized seeking remain future work.
