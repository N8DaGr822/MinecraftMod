# Broader mod additions: prioritized plan

October 20, 2026. Requested alongside the Ascension expansion. Implementation has begun; only increments explicitly identified below are implemented in source. This does not authorize a release. Current implementation guides take precedence over older brainstorm wording.

## Current acceptance — October 22

Source implementation is distinct from live acceptance. The [focused two-client record](two-client-acceptance.md) remains not run; no clients were available and the agent cannot operate native game windows. Historical passing tests do not certify the entire current working tree. The approved [ground walker](mech-prototype-design.md) is now in source.

## Active work

The [Ingredient Pouch](ingredient-pouch.md) supports brewing reagents only, with one native bundle of capacity. The user-approved [Quiver](quiver.md) adds all arrow types at the same capacity, automatic bow feeding after loose ammunition, native Infinity rules and fresh Testing Camp samples. The approved [Explorer's Pack](explorer-pack.md) adds six exploration-only slots with manual access and no nested containers. Live inventory controls, reconnect and multiplayer acceptance remain outstanding.

The user expanded the awakened increment to all 16 seal-enabled families, starting with Ancient Tree Spirit. The [awakened attack pass](awakened-boss-patterns.md) adds one warned sequence per family while preserving normal fights and existing cosmetic rewards. Bespoke models and full new move sets remain future work.

Ascension guidance, team management and a second chapter are described in [chapter two](oneblock-ascension/chapter-two.md). The approved [twelve-phase extension](oneblock-ascension/chapter-three.md) adds Frostbound, Tidal Reach, Crystal Depths, Endreach, Astral Forge and Ascension Summit in source. The [Guardian presentation pass](oneblock-ascension/guardian-presentation.md) adds custom construct silhouettes and original Guardian music integration. Original Guardian music is now implemented as a shared theme with nine variations and three intensities each. Painted skins and live survival/visual/audio acceptance remain outstanding.

The [Wolf Hunt](wolf-hunt.md) now supports both solo and explicit cooperative participation, bounded packs, visible progress and saved personal rewards/cooldowns. The [Nexus room increment](endgame-expansion-playtest.md#darkspawn-nexus) adds a sheltered approach, connected encounter courtyard, explicit party start and visible objectives. Live room navigation and multiplayer acceptance remain before a unique final boss.

The [docked-worker unloading job](docked-worker.md) adds a captain-selected nearby single chest, bounded deliveries by the existing shipworker, visible job status and explicit pause/resume/recall. [NPC patrol boarding](patrol-boarding.md) adds optional solo or explicitly cooperative combat against disabled ordinary patrols, finite sentries and once-only shared existing cargo. Autonomous sailing remains planned.

## Proposed order

The approved [fossil reconstruction and ocean-clue increment](fossils-and-ocean-clues.md) adds a decorative Tyrant display and a separate optional crafting path to the existing Kraken Sigil. Finds have visible instructions, archaeology sources and renewable recovery paths. Original summon recipes and gates remain intact; live pacing/visual/reconnect acceptance remains.

| Order | Increment | Small first delivery | Dependencies and acceptance |
| --- | --- | --- | --- |
| 1 | Utility equipment | Ingredient Pouch, all-arrow Quiver and six-slot Explorer's Pack implemented in source | Pouch remains brewing-only; Quiver feeds bows after loose ammunition. Live controls/reconnect acceptance remains. Explorer's Pack provides manual exploration-only storage. |
| 2 | Awakened encounter variety | Implemented in source for all 16 seal-enabled families, Ancient Tree Spirit first: one new warned sequence each; existing cosmetics retained | Preserve summon identity, heart cap and existing victories. Automated scheduling, damage and lifecycle checks pass; live cues, dodge space and multiplayer acceptance remain. See the [attack guide](awakened-boss-patterns.md). |
| 3 | Regional events | Wolf Hunt implemented in source: three wolves solo or six for two-to-four opted-in players | Saved personal cooldowns/claims, finite packs, progress bar and interruption cleanup. Live multiplayer/balance acceptance remains; see [Wolf Hunt](wolf-hunt.md). [Fungal Bloom, Shadow Incursion, Soul Storm and End Rift](regional-encounters.md) are now implemented in source with solo/cooperative participation, finite enemies and saved claims; current timed boons remain distinct. |
| 4 | Nexus dungeon | Implemented in source: deepslate entry hall and encounter courtyard connected to the existing arena loop | Existing gates, team opt-in, recovery and once-only rewards retained; explicit begin and objective/timer added. Live route, UI and two-client acceptance remain. A unique final boss follows after the dungeon loop is proven. User confirmed October 21 that live room/start/recovery testing has not happened; this checkpoint remains pending. |
| 5 | Crew depth | Docked cargo unloading and optional NPC patrol boarding implemented in source | Captain-selected chest, worker routes and recall; boarding supports one to four opted-in players, finite sentries and shared original cargo. Live dock/boarding/reconnect acceptance remains. Autonomous sailing/docking remains planned. |
| 6 | Archaeology and fishing progression | Fossil reconstruction/display and optional Kraken clue chain implemented in source | Existing fossil identity, native crafting, finite archaeology and renewable ocean notes; original summon recipes/gates retained. Live pacing, rendering and reconnect acceptance remain. See [guide](fossils-and-ocean-clues.md). |
| 7 | Thematic enchantments | Hunter, Butcher, Forager and Soulward I-II implemented in source | Native acquisition, bounded food pools, boss exclusions and camp trials. Targeted server checks pass; live balance remains. See [guide](thematic-enchantments.md). |
| 8 | Advanced travel | [Magic carpet](magic-carpet.md) implemented in source | One seat, crafted placement, native-dimension restrictions, authoritative mount controls, capsule/recovery, native model and camp sample. Focused tests pass; live camera/visual/multiplayer acceptance remains. |
| 9 | Aircraft and mechs | [Editable aircraft](aircraft-prototype.md) implemented; [ground walker](mech-prototype-design.md) implemented in source | Aircraft: one pilot, five-to-nine ordinary blocks, manual flight/landed editing, saved build and ownership, capsule recovery, no containers or weapons. Mech is ground-only with step climbing, iron repair and recovery; see its guide for current checks. Live vehicle acceptance remains. |
| 10 | Public-server operations | [Isolated backup/recovery and idle-aircraft timing drill](server-operations-validation.md); seasons/leaderboards later | Stopped-world SHA-256 snapshot/restore tooling and native restart sentinel. Local zero-client timing is not multiplayer capacity. Actual server recovery, player load, admission/payment, seasonal reset policy, PvP and Hall of Fame remain separate acceptance/product decisions. |

## Cross-cutting improvements

Use visible level/state/next-step explanations for new systems, following the bonfire display and Ascension screen. Distinguish “not installed,” “not eligible,” “paused,” “blocked,” and “ready.” Avoid hiding essential instructions only in books.

Keep one current status section per feature guide. Historical roadmap entries should link to it rather than becoming competing feature lists. Fresh Testing Camps should receive relevant supplies and short instructions when each increment ships in source; existing camps are not silently modified.

Balance from recorded Survival sessions: time to first useful reward, materials earned/spent, travel downtime, deaths, temperature interruptions and player count. Automated correctness checks do not establish enjoyable pacing.

Separate spawn follow-up discovered during Wolf Hunt validation: Minecraft 26.3's `minecraft:dirt` block tag excludes grass blocks and podzol. The new hunt explicitly supports both. The ordinary taiga predicate now explicitly includes grass and podzol alongside the prior surfaces; nine taiga regressions pass.

Before widening vehicle, inventory or persistence changes, complete the relevant real-client reconnect, death/recovery and backup/restore checks. Use focused checks during development and the required unfiltered fullCheck before any push.

## Decisions to make when each increment starts

Pouch capacity and categories are settled: brewing-only, one bundle. Quiver scope is settled: all arrow types, one bundle, automatic bow feeding. Explorer's Pack scope is settled: six exploration-only slots, manual access, no nesting. The awakened roster is settled: all 16 existing families in BossKind order, Ancient Tree Spirit first. Wolf Hunt supports both solo and cooperative participation. The first Nexus rooms extend its deepslate theme with basalt and amethyst. The first worker job is settled: unload cargo to a designated chest. Boarding is settled: optional disabled NPC patrols, solo or up to four opted-in players, finite defenders, existing shared cargo, no player-ship capture. Magic carpet followed by editable aircraft is settled. The separate mech design is a one-pilot ground walker with step climbing and storage/recovery, without flight, weapons or mining. Server operations begin in an isolated local test world.
