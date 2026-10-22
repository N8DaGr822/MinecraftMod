# Ascension guidance and second chapter

The [twelve-phase extension](chapter-three.md) supersedes this chapter's terminal phase and save-schema limit. The UI, team controls and upgrade rules below still apply.

October 20, 2026. This increment extends Ascension within the existing Fabric mod. Shared legacy OneBlock and existing legacy Ascension teams retain their six-phase behavior. No release packaging is implied.

## Player entry and guidance

Open the inventory and select **Ascension** in the left column. The expedition-style screen has Overview, Objectives, Rewards, Automation and Team tabs. Details and lists are paginated, including at a 320×240 scaled viewport. The server refreshes the displayed state while the screen is open; closing it prevents late replies reopening it.

Overview explains the current phase and next task. Objectives submit only the needed items from the main hand. Rewards remain once per team, require an empty inventory slot, and show claimed status. Guardian starts, upgrades, phase completion and membership changes have a second confirmation click. Team creation and invitations use the text field.

The UI uses the same authoritative services as commands. Single-use, bounded-lifetime tokens bind offered actions to the current team, owner, membership, phase and dimension. Item transactions retain their Survival, own-claim, cursor/container and travel checks. Commands remain available.

## Second chapter

| Phase | Breaks | Held-item submissions | Encounter | Completion rewards |
| --- | --- | --- | --- | --- |
| Forgeworks | 700 | 4 hoppers, 4 pistons, 32 rails | None | 32 redstone, 4 diamonds, 32 copper ingots |
| Stormbound | 800 | 4 lightning rods, 32 glass, 64 stone bricks | Storm Guardian | 32 redstone, 6 diamonds, 8 obsidian |
| Emberlands | 900 | 32 nether bricks, 16 quartz blocks, 3 empty buckets | Ember Guardian | 4 ancient debris, 8 nether wart, 8 blaze rods, 8 ender pearls |

These initial balance values are playtest defaults. Every pool retains guaranteed dirt/logs and the materials needed for its submissions. Forgeworks introduces renewable metal/redstone/diamond resources; Stormbound mixes copper, sand and workshop resources; Emberlands adds netherrack, quartz, soul sand and glowstone while retaining fuel and iron. Correct tools still govern native drops.

Objectives ask players to craft and submit preparation supplies; they do not inspect placed machinery, roofs or structures. The island remains a plains-biome void world: phase names do not change the biome, global weather or temperature rules. The existing passive-animal milestone continues in all six chapter phases; new encounters are explicitly started rather than unexpected hostile spawns.

All three Guardians use native iron-golem models and movement, with distinct names and attack patterns. Cave retains 80 health and its one-second marked slam. Storm has 100 health and a two-second marked cross extending six blocks along each horizontal axis. Ember has 120 health and a two-second marked ring between radii two and five, leaving the center safe. Specials deal six base damage; normal armor applies. Attacks are local particles/damage, not real lightning, terrain destruction or fire placement.

Existing participant rules remain: Survival teammates within 32 blocks, start within 16, five-minute timeout, at most eight loaded encounters, cancellation without victory on unload/restart/abandonment. Clear enough standing space before summoning. The new Guardians have no bespoke meshes, music or phase-changing AI yet.

## Automation

The Automation tab and `/ascension automation` identify current attendance, chest, tool, output-capacity, generator and cooldown blockers. A collector can still merge suitable stacks when the miner requires an empty output slot.

| Upgrade | Prerequisite | Held payment | Result |
| --- | --- | --- | --- |
| Relay | Collector installed; Forgeworks completed | 32 copper ingots | A living Survival teammate anywhere inside the claim counts as attending automation |
| Drive | Miner installed; Stormbound completed | 32 redstone dust | Mining interval reduces from five to three seconds |

Use the UI or `/ascension upgrade relay` / `drive`. Owner only; duplicate purchases do not charge. The output remains the single ordinary chest east of the core, with its first slot reserved for a plain iron pickaxe. Relay does not load chunks or allow offline mining. An attended island whose core/output area is unloaded still pauses.

## Team management and recovery

Owners can transfer ownership or remove another member through Team, or `/ascension transfer <member-uuid> confirm` and `/ascension remove <member-uuid> confirm`. Online names are displayed; offline members are identified by their durable UUIDs. Owners and online targets must be outside both island modes with no travel countdown. The owner cannot remove themselves.

Archival no longer requires offline members to reconnect first. Online members must be outside both modes. Archival preserves the frozen island and cleans up only the team's neutral bridge blocks as chunks load.

A removed or archived member who reconnects inside Ascension is returned through the existing inventory-profile boundary once their client and inventory are ready. `/ascension recover` retries this return for unassigned players. Ground/combat restrictions are relaxed only for this recovery, so falling does not prevent escape. Open containers, carried cursor stacks, crafting/trash items, vehicles, shoulder entities, leashes and pending loadouts still block recovery; clear them first. Outside possessions are restored and Ascension possessions retained separately. Recovery never grants new island membership.

## Save compatibility

The team ledger writes schema 5, preserving IDs, cells, ownership, counters, generator state and reward snapshots. Schemas 1–4 remain accepted with existing migrations. Earlier binaries cannot interpret schema 5 as a supported team ledger; do not downgrade a live save.

An accepted original three-phase catalog is extended with the bundled new phases, retaining the original accepted definitions. Reload expects all six phase files and still forbids changing established objective/reward identities or encounter gates on populated worlds.

Teams already finished with Verdant stay there until explicitly selecting Continue or `/ascension next`. That advances to Forgeworks with zero phase breaks, preserves total breaks and does not grant Verdant rewards again. In-progress teams advance normally. Legacy teams without chapter progress remain legacy.

## Live acceptance

Check minimum/large GUI scales, keyboard navigation, confirmation flows, reconnects during team changes, warning-particle visibility and dodge timing with two clients. Play through all objectives in Survival, time progression and compare relay attendance near claim boundaries. Full-world crash/restore and sustained multiplayer profiling remain the existing hardening acceptance work.

## Automated verification

Common/client compilation, all 27 OneBlock unit tests and all 49 Ascension server GameTests passed (`build/ascension-chapter-checks.log`). The eight new server checks cover the new Guardians, relay/drive, removed-member profile recovery, blocked cursor recovery, membership authority/archival, repeated and stale UI submissions, visible action feedback and bounded snapshot serialization. Unit checks include three-phase catalog migration, completed-Verdant continuation without duplicate rewards, new phase gates/guarantees and attack geometry.

The first combined run passed 46/47; its remaining assertion still expected Verdant to be terminal. That fixture now verifies Forgeworks entry and preserved once-only rewards. The final 49-test run includes the additional membership and transport cases.

All nine legacy OneBlock server tests and all three Testing Camp server tests also passed (`build/ascension-chapter-legacy.log` and `build/ascension-chapter-camp.log`), bringing targeted server verification to 61 passing tests. These checks are not live GUI/multiplayer acceptance or unfiltered fullCheck. No JAR, version change or push was performed.
