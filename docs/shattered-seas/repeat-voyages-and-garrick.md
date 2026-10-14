# Repeat voyages Garrick and faction patrols

Implemented in development source October 14, 2026. These optional activities extend existing contracts, captain-owned crews and naval encounters. No faction membership is required. Existing one-time rewards and captain milestones retain their original meanings.

## Repeatable faction voyages

Use Previous/Next contract at Briggs or the harbor board to choose one of the three repeat voyages. Each outpost contact cycles only between its own first contract and repeat voyage. Accept before performing the objective; return to the harbor or matching contact to claim.

| Voyage | Objective | Reward |
| --- | --- | --- |
| Corsair cargo recovery | Inspect The Pale Gull wreck's native cargo cache | +10 Corsair standing and 6 emeralds |
| Fleet outlaw patrol hunt | Sink one white-sailed Outlaw Raider using your owned ship | +10 Fleet standing and 6 emeralds |
| Drowned shrine expedition | Inspect Tideglass Shrine's native relic cache with four prismarine shards | +10 Drowned standing and 6 emeralds |

Each faction's repeat voyage has its own **ten-minute cooldown after claiming**, measured in saved server world time. It advances while the server runs, including while the captain is offline. Active objectives, completion and cooldown survive saves and death. Claiming consumes completion before paying; duplicate clicks cannot pay again. A free inventory slot is required; a full inventory leaves the reward ready to claim. Abandoning clears the objective without refunding an offering. Each new acceptance requires a new objective event.

Already emptied shared caches count. Cargo recovery records the recovery in personal quest state, following the existing inspection-contract pattern; it does not create an additional physical cargo item. Shrine expeditions consume four shards once. When both the first Drowned contract and the repeat expedition are active, separate inspections and separate offerings are required; one click never charges both. Outlaw hunts exclude faction patrols, player vessels, and cleanup/despawn events. The sinking captain receives progress, not every nearby player.

Repeated rewards do not increase the unique-contract count used by captain milestones. Reputation still clamps to -100..100. The ten-minute timer, six-emerald reward and ten-standing reward are first-pass balance values for playtesting.

## Garrick the Deserter

The user selected a **personal rescue for every captain**. Accept Garrick Rescue through Briggs or the harbor board, then visit Saltwatch Fort. Your first suitable fort binds the rescue location. A protected, unarmed Pillager captive appears in one of eight clear interior positions, labeled for the captain. Spawning checks loaded space and preserves player blocks; if blocked, it retries on observation.

Inspect that fort's dispatch chest to recover release papers, then right-click your captive within six blocks. Another player's Garrick cannot advance your rescue. Papers, the fort location and the captive's UUID persist; repeated observation and unloaded chunks do not create duplicates. The Journal records the bound fort coordinates.

After the rescue, select Garrick Rescue at the harbor and choose:

- **Recruit Garrick:** your nearest owned ship must be docked in the harbor, within 48 blocks, with a free cannoneer berth. He joins free as a named Pillager cannoneer using the existing ammunition, role and loyalty rules. Missing ships or full berths leave the choice pending.
- **Free Garrick:** finish the rescue without recruiting him. This is a final personal choice and grants no repeatable currency or reputation reward.

Both outcomes are saved and survive death. Recruitment commits only when boarding succeeds; it cannot create another Garrick through repeated clicks. The officer uses existing cannoneer abilities, not a new combat bonus. Native passenger saving preserves his name, role and captain UUID, including the existing Peaceful protection for hired crew. If his ship later sinks, the completed recruitment is not reset.

Abandoning an unfinished rescue pauses it; accepting again returns to the same recorded captive/fort. Administratively deleting the saved captive is not an automatic respawn path. This first rescue uses release papers and conversation in the existing fort; it does not add escort AI, prison terrain replacement or a bounty alternative.

## Faction naval patrols

| Vessel | Sail and identity | Default relationship |
| --- | --- | --- |
| Outlaw Raider | White sails | Hostile to eligible player vessels |
| Piglin Corsairs Patrol | Red sails and faction name | Hostile only at negative Corsair standing or after provocation |
| Illager Iron Fleet Patrol | Blue sails and faction name | Hostile only at negative Fleet standing or after provocation |
| The Drowned Patrol | Cyan sails and faction name | Hostile only at negative Drowned standing or after provocation |

Targeting uses the vessel owner's standing. AI selects occupied vessels with a living, non-Creative, non-Spectator captain. Friendly ships are also protected by the cannon-hit check, including projectiles fired before relationships changed. NPC vessels do not damage other NPC vessels in this pass. Player-ship PvP rules remain in place.

Attacking a faction patrol costs **10 standing**, at most once per captain/faction per second across guard and patrol attacks. It provokes that patrol against the most recent attacking captain for one minute, even if standing remains positive. After provocation expires, standing governs hostility again. Faction identity, expiry and provocation survive reloads. Target selection and lookout feedback show the named patrol rather than a generic pirate label.

Once per minute, spawning makes at most two eligible attempts across Pirate Seas, with a cap of eight loaded NPC vessels and no existing NPC vessel within 192 blocks of the triggering ship. Captains must be sailing their own vessel outside the harbor. Candidates remain 96 blocks east and 64 south, require loaded clear water, and stay at least 128 blocks from the harbor center. Faction selection is an equal draw among the three factions and outlaws. Peaceful difficulty and disabled mob spawning suppress new patrols. No chunks are generated or held loaded for encounters.

Unengaged patrols cruise briefly and leave after two minutes without a valid target. Every NPC vessel has a ten-minute world-time lifetime; expired vessels disappear on their next loaded tick without loot or victory rewards. Unloaded vessels do not count toward the loaded cap and are reconciled by their expiry when loaded again. Movement keeps NPC ships outside a 112-block approach boundary; existing 96-block harbor damage protection applies independently. Ammunition remains finite. These are patrol encounters, not territory warfare or boarding combat.

## Verification and live acceptance

Targeted checks cover exact and duplicate rewards, invalid hand-ins, inventory backpressure, cooldown save/death persistence, personal rescue ownership, final free/recruit choices, cannoneer capacity, native officer passenger persistence, faction colors, personal hostility, retaliation, harbor damage protection, patrol expiry, spawn population limits and unloaded-chunk refusal. Common/client compilation and three Journal unit tests passed. All 25 pirate tests passed. All 15 distinct naval tests passed across the naval run and the corrected population-cap test rerun: the cap fixture needed an observing player to register its distant NPCs as loaded. Logs: `build/pirate-activities-final.log`, `build/pirate-activities-ships-final.log` (14 passed; original cap fixture failed), and `build/pirate-patrol-budget.log` (corrected cap test passed). This is targeted verification, not unfiltered fullCheck.

Live acceptance remains: two captains at the same fort, two captains with different standings at sea, sail/name readability, harbor screen layout, patrol frequency, engagement range and contract reward pacing. No JAR packaging or version change is included.
