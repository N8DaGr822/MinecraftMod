# Pirate and exploration server playtest

For the latest cross-feature status and remaining brainstorm ideas, see the [weekend source audit](weekend-playtest-audit.md). Later faction specializations, ammunition and Nexus rules are detailed in the [endgame increment](endgame-expansion-playtest.md); the original checks below record the earlier exploration increment.

## Status and tested baseline

All requested expansion categories are implemented in development source. Automated verification passed on October 15, 2026: 94 unit tests, 276 server GameTests, cuisine artwork validation, and the separate exploration/template validator. Final port-message polish is checked separately in `build/expansion-final-ui.log`. This is a playtest candidate: live client visuals, five-player synchronization, exploration frequency and balance still need gameplay acceptance. No new JAR has been packaged, version bumped or pushed.

The tested dependency pins are Minecraft **26.3**, Fabric Loader **0.19.5**, Fabric API **0.162.0+26.3**, GeckoLib **5.5.7** and Java **25**. Live verification of the newest Fabric release was unavailable; these exact pins are the verified baseline. Compatibility with other mods cannot be certified without the server's actual mod list. Start with the same requested play-build JAR on server and clients. Keep a world backup before installing a newly requested build.

## Included feature checklist

| Feature | Implemented behavior |
| --- | --- |
| Formal faction promotions | Earned requirements, deliberate promotion and saved ranks |
| Shared-party compass navigation | Consensual invitations; up to six players; shared guidance with personal discoveries |
| Faction skirmishes | Finite Redwater battles, participant-specific rewards, cleanup and saved encounter receipts |
| Nine island types | Three added islands in fresh terrain; compass/Journal discovery persistence |
| Boss finder | Bounded Landmark Compass search for sixteen boss landmark families |
| Beer and rum | Haste I / Resistance I, native drinking, finite durations and containers |
| Home ports | Owner-only captured Saltwatch Fort, drydock and tavern service upgrades |
| Advanced ship classes | Standard, Clipper and Ironclad refits on eligible larger hulls |
| Crew roaming and care | Docked roaming, seated sailing, saved morale, prepaid wages/provisions |
| Separate discovery maps | Native filled maps with a copied landmark target; shareable/copyable |
| Archaeology and regional loot | Fourteen landmark families, 42 variants, two brushable deposits per site and broader caches |
| Awakened rematches | Gated scaled bosses; glint trophies and colored leather laurels |
| Regional events/audio | Personal habitat boons, particles and regional cues using existing audio |
| Server tuning | Bounded patrol/skirmish budgets and atmosphere/event intervals |
| Testing Camp | New supplies, actual brushable samples and guide pages |
| Save support | New port, ship/care/crew identity and regional cooldown data; older saves default safely |

Earlier stage rules, drink prices and skirmish behavior are in [expansion stages](expansion-stages.md).

## Home ports and ships

Capture a natural Saltwatch Fort, then sneak-use the Logbound Compass there to open services. Select **Designate home port** and confirm. Each captain has one home port. Moving it resets purchased port upgrades; losing the fort revokes them. Recapturing does not resurrect old upgrades. Existing fort blocks and ship cargo are preserved. Services remain owner-only; shared storage/guest permissions are not added.

| Upgrade | Cost | Access |
| --- | --- | --- |
| Level 1 drydock | 32 iron ingots, 32 oak planks, 8 emeralds | Refit and recruit on your own docked ship within 48 blocks |
| Level 2 tavern | 16 iron ingots, 64 oak planks, 8 emeralds | Buy provisions, timber, shot, beer and rum through fort services |

At the original harbor or your drydock, use your ship's **Fit** tab after earning the Captain milestone, on a tier-1-or-larger ship. The three existing platform sizes remain unchanged. Class changes preserve cargo/build blocks and the current damage fraction.

| Class | Refit cost | Tradeoff |
| --- | --- | --- |
| Standard | 16 iron ingots | Baseline speed and hull |
| Clipper | 16 iron ingots, 8 emeralds | 25% faster, 15% less hull |
| Ironclad | 48 iron ingots, 8 emeralds | 25% more hull, 15% slower |

Use **Care** while docked: wages cost one emerald per assigned loaded sailor, cover 20 minutes and add 20 morale; provisions cost one bread per two sailors rounded up, cover ten minutes and add ten morale. Coverage must expire before repurchasing the same benefit. Morale starts at 70, caps at 100 and falls only while sailing unpaid, two points per minute (one with provisions), to a floor of 25. Paid crew do not decay. Morale at 80+ gives 115% efficiency; below 40 gives 75%; otherwise 100%. Efficiency affects crew cannon reload and worker repair. There is no wage debt or permanent crew loss from morale/wages.

**Roam** permits the original crew entities to walk on safe deck positions while docked. Launch recalls those same sailors and resumes seated sailing. Missing, unloaded or distant assigned sailors block launch: load them and bring them within 48 blocks. No substitute crew are spawned. Check custom decks, stairs, narrow paths and named officers during live testing.

## Maps, archaeology and rematches

Craft a **Blank Discovery Chart** from two paper and an ink sac. Locate a target with the Landmark Compass. Hold the located compass in one hand and use the chart in the other, in the target dimension with a free inventory slot. This creates a native map with a red target marker and coordinate lore. Terrain starts unexplored and fills as you explore; creating the map performs no additional structure search or generation. Copy/share it through normal map mechanics. It grants no discovery or progression credit.

Newly generated regional landmarks have two supported suspicious-gravel deposits. Brush them repeatedly for a single regional find: pottery sherds, thematic provisions, amethyst, rare echo shards or a blank chart. Regional caches also have an extra thematic loot roll. Existing generated structures are not rebuilt. The Testing Camp offers two real single-use deposits for checking brushing without a long expedition.

Craft an **Awakening Seal** from an echo shard, diamond and amethyst shard. Keep it in the **offhand** during the boss's usual summon ritual, with the normal summon item/materials. You must already have that family's normal defeat advancement or have consumed its heart. Ordinary global progression, location, difficulty, clearance and encounter limits still apply. The seal is consumed only after successful spawn.

Awakened bosses gain 50% more maximum health, four armor and 10% movement speed where applicable. Existing attack patterns remain. Valid participants receive a named glint family trophy and colored/glint leather **Awakened Victor's Laurel** with ordinary leather stats. Awakened kills do not drop another heart, essence or boss cooking ingredient. Trophy blocks retain existing family visuals when placed; the item name/glint and worn laurel provide the distinction. These are cosmetic rematches rather than another equipment tier.

## Regional events and five-player configuration

After the normal global dragon gate, eligible Survival habitat visits start a saved personal cooldown. Default: a boon becomes available after 20 minutes, then at most once per 20 minutes. Offline elapsed time can make one boon due but never stockpiles rewards. Forest/jungle/mushroom habitats give Regeneration I; mountain/cave/deep-dark/fossil habitats give Haste I; the Kraken habitat gives Dolphin's Grace I; other eligible habitats give Speed I. Boons last 30 seconds with local particles, chat and an existing regional audio cue. No terrain replacement or extra event mobs occur.

The server creates `config/darkspawn-server.properties`; restart after edits. Defaults are a conservative starting point for five players, not a measured capacity guarantee.

| Key | Default | Allowed range |
| --- | --- | --- |
| `skirmishCap` | 4 | 0-8; zero disables new starts |
| `patrolCap` | 8 | 0-16 loaded patrol ships |
| `patrolAttemptsPerMinute` | 2 | 0-4 |
| `ambientSeconds` | 60 | 10-600, rounded up to ten-second steps |
| `regionalEventSeconds` | 1200 | 300-7200 |
| `regionalEvents` | true | true/false |

Budgets operate on loaded worlds/entities; gameplay does not force-load chunks for these systems. Keep defaults for the first session and record actual server tick performance before increasing encounter limits.

## Five-player acceptance route

1. Place a **fresh** Testing Camp in an open 21-by-21 area. Read the guide, find charts/seals/brushes and brush both samples. Existing camps do not restock. Supplied hearts change progression only when deliberately consumed.
2. Form a party, promote eligible faction members and visit all three new islands in ungenerated terrain. Try Redwater as a neutral observer, one-side helper and both-side aggressor on separate landings.
3. Capture/upgrade a fort. Verify another player cannot spend the owner's port upgrades. Move a home port once and recapture a lost fort to verify reset behavior.
4. Compare Clipper/Ironclad handling, hull and payments. Roam original named crew, recall and sail; unload/reload the dock area. Check officers, cargo and custom build blocks persist.
5. Pay wages/provisions, confirm duplicate prepayment is refused and compare morale/reload behavior. Restart the server and verify ship class, port level and remaining coverage.
6. Locate a landmark, make/copy/share its chart, explore the blank terrain and brush a newly generated deposit. Check map marker and compass point to the same target.
7. Try a locked awakened ritual, then an earned one. Confirm failed summons consume nothing, successful summons consume the seal, and all participating players receive cosmetics without another heart. Verify boss appearance and difficulty in actual combat.
8. Revisit habitats after the boon timer; reconnect and restart to test cooldown persistence. Listen for audio overlap and check effects against meals/drinks.

For feedback record: build label, dependency/mod list, player count, coordinates/dimension, steps, expected/actual behavior and relevant server log. Screenshots help for clipped labels, map markers, crew positions and cosmetics. Server tests do not establish live rendering, combat feel or third-party compatibility.
