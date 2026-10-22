# Ascension: phases seven through twelve

October 20, 2026. Implemented in source, following the approved progression. This extends chapter teams; shared OneBlock and legacy Ascension teams retain their original six resource phases. No release JAR is included.

Open inventory > Ascension for requirements, submissions, rewards and Guardian instructions. Each phase requires its break threshold, all submissions and its Guardian before advancing. Mining remains available after Summit completion.

| Phase | Breaks | Submissions | Guardian warning |
| --- | ---: | --- | --- |
| Frostbound | 1,000 | 64 snow blocks, 32 glass, 8 lanterns | Three north-south frost lanes; stand between them |
| Tidal Reach | 1,100 | 48 prismarine, 64 glass, 4 buckets | Three east-west bands; stand between them |
| Crystal Depths | 1,200 | 24 amethyst blocks, 16 tinted glass, 8 lightning rods | Diagonal fracture; leave the marked X |
| Endreach | 1,300 | 64 end stone bricks, 48 purpur blocks, 16 obsidian | Center pulse and two rings; move into a gap |
| Astral Forge | 1,400 | 4 netherite ingots, 16 redstone blocks, 2 diamond blocks | Four impact circles; leave their marked areas |
| Ascension Summit | 1,600 | 8 netherite ingots, 32 amethyst blocks, 64 end stone bricks | Cross and outer ring; use a diagonal gap |

Rewards, in phase order:

- Frostbound: 64 packed ice, 64 snow blocks, 32 coal, 4 diamonds.
- Tidal Reach: 32 sea lanterns, 64 prismarine bricks, 4 sponges, 4 diamonds.
- Crystal Depths: 64 amethyst shards, 64 glass, 8 echo shards, 8 diamonds.
- Endreach: 8 shulker shells, 16 ender pearls, 32 end rods, 8 diamonds.
- Astral Forge: a netherite upgrade template, 4 netherite ingots, a smithing table and an enchanting table.
- Summit: elytra, a nether star, a dragon head and a netherite block.

New generator resources appear cumulatively: snow blocks, prismarine, amethyst clusters, then end stone/purpur/obsidian, then ancient debris. Each is guaranteed during its phase's 24-generation cycle. Basic wood, iron, fuel, sand, copper, redstone, gold and diamonds also remain renewable. Use a shovel for snowballs, an appropriate pickaxe for amethyst shards, and a diamond or netherite pickaxe for obsidian/debris. Smelt debris into scrap and combine with gold for netherite. The existing iron-pickaxe auto-miner does not replace manual harvesting of diamond-tier resources.

Guardians use the existing team-only encounter lifecycle, visible name/health bar, two-second warning and non-destructive strikes. New health totals rise from 140 to 240. The [presentation pass](guardian-presentation.md) adds custom construct silhouettes and existing-theme music integration; original Guardian compositions and live visual/audio acceptance remain open. These phase names do not change the island biome or global weather.

## Save compatibility and acceptance

Three- and six-phase catalogs append the new definitions while preserving earlier custom balances. Completed Emberlands teams explicitly continue to Frostbound without repeating rewards or resetting lifetime break totals. Save schema 6 supports twelve chapter phases and the six appended resource IDs. Older shared OneBlock save limits remain unchanged. Do not downgrade a world after saving the new chapter progression.

Live acceptance still needs Survival crafting, pacing, visible telegraphs, multiplayer encounters and a full-world backup/restore exercise. Automated checks cannot establish those experiences. See [chapter two](chapter-two.md) for the existing team controls and automation rules.

Automated validation: common/client compilation, 31 OneBlock/Ascension unit and resource-coverage checks, the 49-test Ascension regression group, and all nine focused chapter tests pass. The focused tests include new native material drops, blocked-miner guidance and expanded guide transport; the unit tests cover six-phase save/catalog migration and all new strike shapes. All six appended core model references resolve in the native asset cache.

All nine legacy OneBlock server regressions also pass after extending the shared core's resource variants.
