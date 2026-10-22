# Thematic enchantments

Implemented in source October 21, 2026. All four are optional, ranks I–II, available through native enchanting, random enchanted loot, enchanted equipment trades and book trades. Existing worlds need no migration. No boss unlock requires them.

| Enchantment | Equipment | Effect at I / II |
| --- | --- | --- |
| Hunter | Bow | +1 / +2 projectile damage against the ordinary hostile whitelist |
| Butcher | Native melee-enchantable weapons | 12.5% / 25% chance of one extra raw meat on a direct player kill of adult vanilla livestock |
| Forager | Native hoes, including copper | 12.5% / 25% chance of one extra produce when a player harvests mature wheat, carrots, potatoes or beetroot |
| Soulward | Worn chest armor | +1 / +2 native protection points against undead attackers (normally 4% / 8% mitigation before the shared protection cap) |

Hunter's whitelist contains zombies, husks, drowned, skeletons, strays, wither skeletons, bogged, creepers, spiders, cave spiders, witches, phantoms, blazes, ghasts, silverfish, endermites, slimes, magma cubes, pillagers and vindicators. Animals, players, Warden, Wither, Dragon and custom boss types are excluded. Bow melee attacks receive no bonus. Naming a vanilla mob does not change its type eligibility.

Butcher supports cows, mooshrooms, pigs, sheep, chickens and rabbits. Babies, projectiles, indirect/environmental kills, Creative and Spectator players are excluded. The extra meat is raw even on burning kills; ordinary drops retain their native cooking behavior. Forager excludes immature crops, Silk Touch, automated breaks, Creative/Spectator harvesting and other crop species, including Darkspawn crops.

## Compatibility and limits

Hunter stacks additively with Power. Butcher and Forager append a separate, single-item pool after native loot, so Looting/Fortune still affect ordinary drops but do not multiply the bonus. Only built-in livestock/crop tables are extended; replacement datapack tables are left alone. Native loot execution retains mob/block-drop game rules. No boss loot table or reward event is modified. Food enchantment ranks are clamped to II even if forced higher by commands.

Soulward stacks with Protection under Minecraft's shared 20-point protection cap. It uses the undead attacker tag, including undead bosses; it grants no protection against unrelated environmental damage. Holding chest armor or storing it in an inactive loadout gives no benefit. Native enchantment damage/protection runs alongside existing armor, Resistance, meals and accessories; these retain their own rules. There is no extra potion effect or permanent player state.

## Testing

Fresh Testing Camps supply all four rank II samples in **22 Faction ammunition**, plus guide pages. Existing camp inventories/books are unchanged. Use Survival for food trials; compare enough kills/harvests to account for the random chance. Grow native crops for Forager instead of using the camp's Darkspawn crop plot.

Four targeted server tests exercise native arrow damage and target exclusions, worn Soulward and Protection stacking, actual livestock loot with bounded bonuses and baby/projectile exclusions, and mature/immature crop loot with Silk Touch rejection. Camp checks verify all samples and instructions. Common/client compilation and resource coverage complement those checks. Live enchanting/trade acquisition, real combat balance and guide readability remain acceptance work. No JAR or release version is produced by this increment.

Verification: all four enchantment GameTests and all three camp GameTests passed, along with common/client compilation and ResourceCoverageTest. Logs: `build/thematic-enchantments-tests.log` and `build/thematic-enchantments-validation.log`. These are focused checks, not an unfiltered fullCheck or a play build.
