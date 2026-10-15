# Ships, culinary brewing, world awakening and Nexus playtest

Source implementation, October 15, 2026. No version bump or JAR packaging. Existing accessories, inventory hub, fishing, animal utility and earlier pirate expansion remain included. Validation results are recorded at the end after checks finish.

## Ship specializations and ammunition

The three existing platform sizes, 4/8/12 gunner berths, named officers and four player seats remain unchanged. Size and specialization are separate. Clipper and Ironclad retain their saved numeric IDs and behavior.

The **Care** tab adds these refits. Each requires an expanded platform, captain milestone, docked harbor or owned home drydock, matching faction membership, rank 3 and 60 standing. Cost: 64 iron ingots and 16 emeralds. Purchased vessels retain their class if their captain later leaves the faction. Refit preserves cargo, build and damage percentage.

| Class | Benefits | Tradeoff |
| --- | --- | --- |
| Corsair Raider | 15% faster sailing, 20% shorter base reload | Standard hull |
| Fleet Battleship | 40% more hull, 15% greater shot damage | 25% slower sailing |
| Drowned Ghostship | 10% faster sailing, incendiary fires last one-third as long | Standard hull; cannot phase through blocks |

**Ammunition** cycles Auto, Fire, Chain and Piercing. Auto retains the previous fire-first/solid-second behavior. Explicit selection requires that ammunition in the hold and never silently consumes another type. Chain shot costs a solid shot and iron chain: 6 hull damage and 40% slower sailing for ten seconds. Piercing shot costs a solid shot, iron ingot and flint: 28 hull damage. Neither damages terrain or bypasses naval PvP/harbor protection. Bram can resupply both.

**Shore expedition** reuses assigned crew and existing berths. Crew follow their owner within 40 blocks of a docked ship. Within twelve blocks and line of sight, the mate provides Resistance I, worker Haste I, and lookout night vision. These are support roles, not an autonomous boarding army. Sailors remain invulnerable, as with existing naval crew. Recall requires all assigned sailors loaded within 48 blocks. Sailing remains seated; roaming mode resumes on subsequent docking unless disabled.

## Culinary brewing

Use a normal brewing stand, blaze powder and awkward potions. Each ingredient brews up to three bottles per batch. Vanilla brewing and meal effect ownership remain intact.

| Ingredient | Potion | Base duration | Glowstone variant |
| --- | --- | --- | --- |
| Wild herbs | Regeneration I | 20 seconds | II for 10 seconds |
| Jungle pepper | Haste I | 60 seconds | II for 30 seconds |
| Goat milk | Resistance I | 30 seconds | II for 15 seconds |
| Woodland truffle | Strength I | 30 seconds | II for 15 seconds |
| Marsh rice | Water Breathing | 90 seconds | None |
| Frost garlic | Fire Resistance | 90 seconds | None |

Redstone doubles base duration. Strength and duration upgrades are alternatives. Gunpowder and dragon breath convert these potions to native splash and lingering bottles. Their normal delivery rules apply. New potion families use native potion visuals and colors.

## Shared world awakening

Boss victories are stored in the Overworld's saved data and apply to everyone. Existing earned boss advancements migrate on player join; merely receiving Testing Camp hearts does not unlock the world. Nexus opponents never grant normal boss hearts or world progression.

- Tree Spirit: Heartwood Seedlings can grow. Craft one with oak sapling and Living Heartwood, or obtain an occasional seedling from awakened log harvesting. Growth checks the complete volume first and refuses buildings, fluids, unloaded chunks and borders. Heartwood logs can be crafted into Living Heartwood. Trees grow from planted seedlings; established terrain is not automatically rewritten.
- Mutant Wolf: offer four cooked beef in the main hand to a normal Dire Wolf. The resulting peaceful companion follows locally; empty-hand interaction toggles rest. Ownership survives reload. It has no teleporting, cargo or combat role.
- Fossil Tyrant: half of otherwise applicable natural fossil spawn attempts are suppressed; fossil archaeology gains a weighted diamond entry.
- Thunder Bird: exposed, unweathered lightning rods can yield Stormforged Feathers during thunderstorms.
- Mycelial Sovereign: red/brown mushrooms become a renewable source of fungal cream.
- Shadow Queen: half of otherwise applicable natural shadow spawn attempts are suppressed. Existing mobs and command summons remain.

Sneak-use a brush on logs, mushrooms or the storm rod to harvest. Each player shares one saved five-minute cooldown across those three harvest routes. Fungal cream consumes one empty bowl; a rejected harvest never charges it. Blocks remain intact. These changes extend existing resources and ecology; they do not create new mushroom species or remodel existing biomes.

## Darkspawn Nexus

Craft a reusable Nexus Key from a Nether Star, echo shard and ender eye. Start in the Overworld. A survival captain needs all sixteen world boss victories, fifteen different consumed boss hearts, and one additional Nether Star per run. Up to five friends within twelve blocks opt in by holding a Nexus Key in either hand; they need not meet the progression gate. Creative captains can rehearse without granting world progression. Switch to Survival to test enemy targeting.

One shared arena run is active at a time. The arena is built once in its own dimension; later attempts preserve placed blocks and dropped equipment. Five waves combine fossil, shadow, fungal, Nether and void enemies. The final Nexus Warden is an enhanced Void Eye encounter using existing visuals and mechanics, with 50% more health after normal multiplayer scaling. This is a functional gauntlet arena, not a sprawling authored dungeon or a new boss model.

Run state, membership, target identities, reward claims and return locations are saved. Only actual enemy deaths advance waves; missing/unloaded entities never count as victories. The run ends after twenty minutes or when the group departs. All-offline runs wait until their deadline; they do not advance without players. Surviving entrants receive one Nexus Core each after the Warden. Retreat to finish the run before starting another.

Use the key or **`/nexusleave`** to return. Return placement uses the existing safe-position search and falls back to world spawn if the entrance is obstructed. **`/nexusrecover`** lets previous entrants recover equipment from the Overworld without paying for another run; it does not award a victory. A previous entrant cannot enter somebody else's active run through recovery. Keep arrival pads and return areas clear.

## Legendary relics

Each recipe consumes one Nexus Core and its listed materials. These now use [custom Darkspawn models](distinct-item-art.md), including bow draw and shield blocking states.

| Relic | Other ingredients | Behavior |
| --- | --- | --- |
| Titanbreaker | Mace, netherite ingot | Native mace attacks, 750 durability; direct melee hits slow hostile mobs for two seconds when the offhand is empty |
| Voidbow | Bow, ender eye | Gravity-free arrows for five seconds, then normal falling; normal ammunition use and durability cost, 768 durability |
| Queen's Carapace | Shield, echo shard | Native directional shield, 672 durability; successful blocks slow the attacking hostile mob for three seconds |
| Soul Lantern Relic | Soul lantern, echo shard | Reveals visible hostile mobs within sixteen blocks for ten seconds and grants brief night vision; ten-second reuse cooldown |

These four additions do not replace legendary armor or existing equipment empowerment.

## Testing Camp and manual acceptance

Fresh camps add groups 22–25: faction ammunition, sixteen culinary potion variants plus reagents, Heartwood/harvest supplies, and the key/core/four relics. The guide explains the controls and gates. Existing placed camps are not overwritten; place a new camp in a clear area.

For the five-player server, verify:

1. Faction refits reject insufficient rank/materials without charging; reload each new class with named cargo.
2. Select each ammunition type, test an empty hold, PvP off and harbor protection. Compare damage, speed and reload differences.
3. Lead and recall shore crew across real dock geometry. Reconnect with roaming crew and ensure no duplication or missing sailor.
4. Brew ordinary, strong, extended, splash and lingering variants. Compare potion/meal overlap and milk-bottle remainders.
5. Defeat relevant bosses in a disposable world, join with another player, test regional unlocks and reload. Obstruct a planted seedling and confirm no block loss.
6. Start a Nexus run with a qualified captain and unqualified friends holding keys. Test death, disconnect, restart, retreat, reward counts and equipment recovery.
7. Inspect custom relic visuals, recipe-book discovery and actual combat balance. Full five-player gameplay and compatibility with the server's unspecified other mods remain manual checks.

## Verification completed

- `classes clientClasses fullCheck --rerun-tasks --max-workers=1`: 94 unit tests, all 291 server tests and the existing 142-model cuisine artwork validation passed. Log: `build/next-expansion-finalcheck.log`.
- After the final fungal-cream bowl-conservation fix, `classes clientClasses` and all five focused endgame GameTests passed, including the new harvest payment/cooldown test. Log: `build/endgame-final-targeted.log`. The source now has 292 registered server tests; the final additional test was verified in that focused run.
- `verify-endgame-expansion.py`: 80 brewing recipes, sixteen potion variants, ten native item visual references and nine recipe-book unlocks passed.
- Existing `verify-field-utilities.py`: 41 utility item resources and six fish skins passed. `git diff --check` passed.
- No JAR packaging, version bump, commit, push or server deployment performed. Live five-player gameplay, new client visuals, navigation and unspecified third-party mod compatibility remain unverified.
