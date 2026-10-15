# Pirate expansion

October 14, 2026. Source implementation; no release version change or packaged JAR.

## Menus and access

Ship & Crew uses Sail, Crew and Refit tabs in a 312x232 panel. Cargo retains full-size slots and two hold pages. Harbor dialogue and service descriptions paginate instead of overflowing. Contracts report missing acceptance, objectives, reward space, payments, cooldowns, officer berths and faction prerequisites separately.

Open Harbor services through Briggs or the harbor board. Sneak-use the Logbound Compass at harbor, inside a generated Saltwatch Fort or inside Tideglass Shrine for shore services. The server validates location, distance, player state and each transaction. Endings require a second confirmation click; selecting another service cancels that confirmation.

## Harbor and recovery

| Service | Payment | Result |
| --- | --- | --- |
| Tavern provisions | 1 emerald | 8 cooked cod |
| Repair timber | 2 emeralds; 1 at 50 renown | 32 oak planks |
| Cannon supplies | 2 emeralds; 1 at 50 renown | 16 solid shot |
| Vessel repairs | 10 oak planks + 2 emeralds | Up to 50 hull and extinguished fire; owned docked vessel within 48 blocks |
| Clear name | 8 gold ingots | Reduce infamy by 10 |
| Officer rescue | 8 emeralds per lost officer | Restore recruitment for Mara, Bram, Nell or Garrick |

Rescue receipts are world data, including for offline captains whose ships sink. Completed officer commissions remain earned. After payment, recruit Mara/Bram/Nell free through the ship's Crew tab; recruit Garrick through his harbor activity with a free cannoneer berth. Ordinary crew must be rehired. No ship reconstruction, cargo restoration or duplicate commission reward is granted.

Renown grows with exploration, commissions, NPC naval victories and first captures of distinct forts. It caps at 1,000. Infamy caps at 100 and grows through faction aggression, sinking other players' vessels, player fort takeovers and the Pirate King ending. At 30 infamy, bounty patrols may appear. Renown and infamy are separate from faction standing and survive death.

## Ghosts and territory

The free ghost voyage prioritizes a spectral ship on the next eligible patrol attempt. Ghosts may also appear naturally. Pale sails, a named ship and soul particles distinguish them. Bounty hunters have black sails, target infamous captains and retaliate against captains who attack them. Existing loaded-water validation, harbor protection, encounter caps, expiry and Peaceful rules apply. Ghosts and hunters do not count toward the Outlaw Hunt contract.

Capture a native Saltwatch Fort by defeating its responding garrison, then holding it for 60 uncontested seconds within 24 blocks. Rival players pause the hold. Leaving, dying, disconnecting, Peaceful difficulty or the ten-minute deadline aborts the attempt. Four simultaneous sieges are allowed. Interrupted sieges are discarded on restart; completed ownership persists.

Offline owners can lose forts. Capture replaces the owner and resets defenses and the supply timer; it does not alter blocks, refill loot caches, transfer ships or transfer cargo. Vanilla block editing remains available. Each of two reinforcement levels costs 8 iron and 24 oak and adds two defenders. Owners can repair docked ships and collect 4 emeralds every 20 minutes. Supplies do not accumulate into an offline stockpile. These are game-time intervals.

## Seven Seals

1. Lantern: complete and claim the existing First Binding commission.
2. Oath: inspect Saltwatch Fort's dispatch chest after the First Binding.
3. Memory: inspect the Pale Gull's cargo chest.
4. Hunger: inspect Rootbound Camp's manifest cache.
5. Mercy: offer 8 prismarine shards through Tideglass shore services, once.
6. Courage: sink a spectral ship with your owned vessel.
7. Name: revisit Lastlight Beacon's lantern with the first six seals recorded.

Journal entries and free tavern rumors show the missing leads. Old saves that already claimed the First Binding receive its seal when loaded. Subsequent discoveries require that first commission; they are personal knowledge, not shared consumable keys.

At Tideglass Shrine, choose one final personal ending:

| Ending | Requirement and reward |
| --- | --- |
| Pirate King | Seven seals; named golden crown, 24 emeralds, +40 infamy |
| Liberator | Seven seals and defeat your Bound Admiral; named trident, +30 renown, -20 infamy |
| Eternal Guardian | Seven seals; named Heart of the Sea, 16 emeralds, +30 renown, -20 infamy |

The Bound Admiral uses a native drowned with 120 health, trident attacks and three health-dependent tide-pulse intervals. Pulses warn one second before striking within six blocks. Helpers may assist; only the designated captain receives the victory receipt. At most eight encounters are active, each lasting ten minutes. Going offline or leaving suspends its AI while the deadline continues. Battle receipts survive reload; stale entities are removed. Selecting another completed ending cancels any remaining personal encounter.

Players can choose different endings in the same world. No ending resets terrain, closes the dimension or prevents continued sailing. Rewards cannot be claimed twice; full inventory preserves eligibility.

## Verification

Targeted verification uses Java 25, `classes clientClasses --max-workers=1` and the default incremental directory. The final pirate run passed all 30 tests (`build/pirate-expansion-final.log`), including harbor transactions, recovery receipt serialization, seven discoveries, independent endings, reward space, contested/offline captures, fort costs/cooldowns and the Admiral victory receipt. The 19 existing naval tests passed in `build/pirate-expansion-ships.log`; the additional ghost/sinking test was corrected to load its ocean fixture before spawning. Its targeted rerun passed (1/1) in `build/pirate-expansion-recovery.log` and also covers bounty targeting/retaliation, named recovery and compact slot bounds. Together these cover 50 distinct passing server tests across the targeted runs. Three Journal unit tests passed (`build/test-results/test/TEST-darkspawn.black.journal.JournalTest.xml`). Automated checks complement, but do not replace, a two-client playthrough, smallest-GUI rendering check, audio check and combat/economy balancing. No fullCheck or playable JAR is implied by source compilation.

Pre-push verification on October 14, 2026: `gradlew.bat fullCheck --rerun-tasks --max-workers=1` passed without filters or exclusions: 90 unit tests, 260 server GameTests and the cuisine asset validator. Log: `build/pirate-prepush-fullcheck.log`. The pirate validator also passed seven templates, loot references and four shanty hashes/subtitles. No JAR was packaged.
