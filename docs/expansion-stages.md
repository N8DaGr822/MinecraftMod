# Expansion stages

October 15, 2026. The user selected staged implementation, beginning with factions, shared-party navigation and exploration tools. Seated sailing remains; roaming crew operate while docked. Implemented wages/morale provide bonuses or reduce efficiency, but must not permanently remove crew. Update the Testing Camp as each stage adds testable content.

## Stage 1: promotions, navigation, supplies and tavern drinks

Implemented in source. No release version change or JAR packaging. Validation results are recorded below.

### Faction promotions

Use **Promotion** at Briggs, the harbor board or your faction's contact. Requirements appear in chat and the Journal. Promotion is free and advances exactly one earned rank per request; current membership and the claimed faction introduction are required for every rank.

| Rank | Standing | Distinct visited islands | Claimed contracts | Additional requirement |
| --- | --- | --- | --- | --- |
| Swabbie / Recruit / Marked | 25 | 1 | Faction introduction | None |
| Corsair / Marine / Oathbound | 40 | 4 | 2 | None |
| First Mate / Lieutenant / Deathsworn | 60 | 8 | 4 | None |
| Captain / Captain / Harbinger | 80 | 12 | 6 | None |
| Dread Captain / Admiral / Wraith Captain | 90 | 16 | 8 | Completed personal Seven Seals ending |

Contracts use existing unique completion counts, including officer commissions; repeat voyages improve standing but do not inflate unique contract counts. Rank 3 discounts the matching faction crew offer by one additional emerald, rank 5 by two, with a minimum price of one. Discounts require active membership and at least Trusted standing. Ranks persist through death, reload and resignation; leaving suspends privileges rather than erasing achievements. Existing saves begin with rank zero and retain all previous progress. These are initial playtest thresholds.

### Shared-party navigation

The leader sneak-uses a Logbound Compass on a nearby player in Pirate Seas. That player accepts by sneak-using their compass on the leader within sixty seconds. Both must be alive, non-spectating, within six blocks and in line of sight. Parties allow one leader and five followers; chains and cycles are rejected. Membership is world-saved.

Followers see the leader's destination and study status. The leader owns the study timer; followers' personal route timers pause while following an available leader. Physical arrival at the shared destination adds a personal visited-island record. It does not copy contracts, rewards, seals, faction standing, inventory or ship permissions. Leader death, disconnect or dimension mismatch temporarily restores personal guidance. Personal routes are retained.

Sneak-use the compass in air **away from harbor/fort/shrine service locations** to leave or disband. Service locations retain their existing sneak-use menu. Leaders disband all members, including offline members; followers leave individually. Invitations expire and are not saved. Existing players remain independent by default.

### Landmark Compass

Craft with a compass, paper, two amethyst shards and an emerald; carrying an ordinary compass reveals the recipe. Also available in Creative and the Testing Camp. Sneak-use cycles sixteen boss landmark families; ordinary use searches in the selected site's dimension. Netherborn/Soulbound sites require the Nether; Void Eye requires the End; the others require the Overworld.

Search checks at most 25 nearby native structure candidates and is limited to once per player per ten game seconds, across compass copies. It can generate structure-start metadata, like the Logbound search, but does not place templates or consume shared exploration references. A failed search preserves the current chart; changing site intentionally clears it. The needle points to the saved structure's bounding-box center, including its vertical coordinate. It is a site finder, not a guaranteed safe arrival or boss summon. Journal discoveries and all normal rituals/progression gates remain unchanged. A bounded search can fail even when that structure exists farther away.

### Beer and rum

Buy through Briggs/board → Harbor services at the Rusty Anchor:

| Drink | Price | Buff | Container returned |
| --- | --- | --- | --- |
| Rusty Anchor Beer | 1 emerald | Haste I for 2 minutes | Bowl |
| Sailor's Rum | 2 emeralds | Resistance I for 1 minute | Glass bottle |

Both stack to sixteen and can be consumed at full hunger. Effects work outside Pirate Seas, refresh without increasing their amplifier and follow vanilla effect replacement/milk/death rules. Stronger existing effects retain vanilla precedence. These are native effects, separate from the exclusive meal-effect group; beer and rum can coexist with meals and each other. They have native drinking animation/sounds and distinct cuboid mug/bottle models using vanilla block textures. Buff durations and prices are initial balance values. No brewing simulation, nausea or permanent bonuses are added.

### Testing Camp

New labeled supplies cover pirate travel, construction/ammunition/currency, both drinks, ready-to-use Forsaken equipment and fuel, the landmark compass, all sixteen boss summons and hearts, regional ritual materials, and mount food/care tools. Mount summon IDs and testing instructions are in the guide. Hearts only change progression when deliberately consumed. The camp preserves the 21×21 footprint and four-block clearance; it can use 72 perimeter barrel locations by filling gaps after the original 36 positions. Existing placed camps do not restock: place a fresh camp in clear terrain.

## Stage 2: faction skirmishes and three islands

Implemented in development source:

- **Redwater Landing:** opposing Corsair and Iron Fleet camps, shared finite salvage cache with beer, and a local four-actor shore skirmish.
- **Pearlwater Atoll:** shallow lagoon, pearl-diver shelter, bridge and finite marine salvage. No authored sentries; ordinary world spawning still applies.
- **Cinderjaw Anchorage:** basalt stacks, open smuggler drydock, hostile scavengers, rum and cannon ammunition.

All nine island types use the existing structure placement, compass and Journal. New types appear only in newly generated terrain; existing templates, builds and caches are not replaced. Saved discovery bits and selected island names now retain all nine types. Existing contracts and Seven Seals requirements remain tied to their original islands.

Redwater starts when a Survival visitor approaches within 40 blocks and all four safe spawn points are available. Peaceful difficulty and disabled mob spawning suppress starts. At most four battles run across Pirate Seas. Native fighters target opposing battle actors; neutral bystanders, same-side fighters, contacts and loyal crew are protected. Hostile-standing players or players who attack a side can be targeted. Existing faction aggression penalties still apply.

Only real actor deaths settle a side. A nearby, alive Survival participant who damaged only the losing side receives +10 winning-faction standing and +3 renown. Attacking both sides forfeits the reward; watching or environmental kills earn nothing. Each participant is evaluated independently. The shared loot cache is independent of the battle outcome.

Battles expire after five minutes, end when no Survival observer remains within 64 blocks, and abort if an actor disappears, unloads or leaves the 48-block combat boundary. Peaceful or disabled mob spawning also ends them. Actors are removed after completion/abort; orphaned actors are removed on load. A saved receipt is written when combat starts, so abandoned/restarted encounters do not respawn or become reward farms. Find another Redwater Landing for a new battle.

Fresh Testing Camp guides include island and skirmish pages. Existing supplied compass, ship equipment, weapons, ammunition and drinks cover this stage; no extra command-only summon items are required.

Manual acceptance: visit fresh terrain, check island silhouettes and terrain borders, watch native combat, help each side on separate landings, attack both sides, leave/rejoin, test two players with different standings, and check battle readability and balance. Automated tests do not establish exploration frequency or combat feel.

## Stages 3-5: home ports, crew, exploration and atmosphere

Implemented in development source. [Server playtest guide](server-playtest.md) contains controls, costs, configuration, save behavior and the live acceptance route.

- Captured Saltwatch Forts become owner-only home ports with paid drydock and tavern upgrades. Moving ports requires confirmation and resets upgrades; losing ownership revokes access.
- Standard, Clipper and Ironclad refits preserve the original ship build and cargo. Sailing remains seated. Assigned crew can roam safe deck positions while docked and must be recalled before launch.
- Saved morale, wages and provisions modify crew efficiency. Unpaid crew never permanently leave; there is no offline debt.
- Separate native discovery maps copy a located Landmark Compass target. Fourteen regional landmark families now supply brushable archaeology and broader cache loot in new terrain.
- Awakening Seals enable gated rematches with increased health, armor and speed. Victories award cosmetic trophies and laurels without stronger equipment or extra hearts.
- Saved regional boon cooldowns, regional sound cues using existing audio, and configurable bounded server budgets support small cooperative servers.
- Fresh Testing Camps include charts, seals, brushes, crafting/payment materials, two real archaeology samples and guide pages for these systems. Normal progression gates remain deliberate.

### Combined expansion verification (October 15, 2026)

Unfiltered `fullCheck --rerun-tasks --max-workers=1` passed: 94 unit tests, 276 native server GameTests and cuisine artwork validation (`build/expansion-fullcheck.log`). The installed Python executable was supplied through `-PpythonExecutable` because this machine's `python` Store alias is unavailable. The process-local Java socket override described below was also used. The exploration validator separately checks all 42 regional templates, 84 deposits, 14 archaeology tables and the new recipe/item resources. Offline archaeology layouts were inspected; live client rendering, five-player gameplay and third-party mod compatibility still need playtesting. No JAR, version change or push.

The following acceptance entries preserve the earlier stage-specific checks.

## Acceptance

Automated verification on October 15, 2026 passed:

- Common/client compilation, including final compass tooltips (`build/stage-one-ships.log`).
- Six unit tests: three new progression/party/compass checks and three Journal regressions (`build/stage-one-unit.log`).
- All 35 pirate server GameTests, including five new promotion, party, drink and compass checks (`build/stage-one-pirate.log`). The native landmark fixture temporarily installs the selected placement/start because GameTest worlds disable ordinary structures, then restores it; this does not establish real-world landmark frequency.
- All 20 naval server GameTests (`build/stage-one-ships.log`).
- All three Testing Camp server GameTests, including actual placement, obstruction refusal, supply coverage and capacity (`build/stage-one-camp.log`).
- Pirate asset validator: seven templates, loot references, four shanties, two drink models and compass needle parity. Offline drink geometry was inspected; it is not an in-game rendering check. `git diff --check` passed.

The Windows Java installation failed to create its default Unix-domain loopback socket. The checks used a process-local `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/nathen.lentz/.vscode/Projects/Tests/MinecraftMod/build` override. No project Java configuration was changed. These were targeted checks, not an unfiltered `fullCheck`; nothing was pushed or packaged.

Live checks still needed: promotion readability and pacing; two-client invitation, departure and reconnect; follower needle and physical visit behavior; landmark search success in ordinary generated terrain; mug/bottle rendering and drinking sounds; tavern prices and combat balance; adjacent camp-sign readability and full multiplayer camp use.

### Stage 2 verification (October 15, 2026)

- Common/client compilation and six focused progression/navigation/Journal unit tests passed (`build/stage-two-unit.log`).
- All 40 pirate server GameTests passed (`build/stage-two-pirate.log`), including actual native AI damage, neutral-observer protection, victory/repeat rewards, both-side aggression, global battle cap, missing/reloaded actor cleanup, departure cleanup, spent-receipt serialization and all nine discovery save bits.
- All 20 naval and all three Testing Camp server tests passed (`build/stage-two-ships.log`, `build/stage-two-camp.log`), including guide coverage and camp supply placement.
- All ten shipped templates passed bounds, loot-reference and dock-to-cache route checks. Updated overhead layouts were inspected (`art/pirate/layout-preview.png`). Generation used `node tools/assets/pirate-foundation.cjs --expansion-only` so existing shipped templates and other generated resources were not rewritten.
- The native combat fixture waits for forced entity chunks to load before adding actors, then releases those tickets. Gameplay skirmishes never force chunks. The test exposed and corrected the piglin Brain anger-memory requirement.

No JAR, version change or push. These are focused checks, not a fullCheck pass. Live Minecraft visuals, fresh-world exploration distribution and multiplayer acceptance remain open.

## Next expansion: faction vessels, brewing, awakening and Nexus

Implemented in source using the approved separate size/specialization progression and shared world unlocks. [Full rules, boundaries and testing checklist](endgame-expansion-playtest.md). Seated sailing and existing ships are preserved. No JAR or version change.
