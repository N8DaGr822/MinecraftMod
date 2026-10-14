# Barracks Bonfire prototype

The first bonfire checkpoint implements a fuel-free base ward and a prototype for actual wide-area block lighting. It follows [the expansion brief](vehicles-mounts-and-base-ideas.md).

## Build and use

Use the current playable JAR indexed in `build/libs/README.md` in a test instance. Older checkpoints are archived under `build/libs/archive`. Replace the previous Darkspawn JAR; both clients and the server need the updated mod, Fabric API, and GeckoLib. Source edits do not update that JAR. Compile with `gradlew.bat classes clientClasses --max-workers=1`; package with `gradlew.bat playBuild --max-workers=1` only when explicitly requested. See [the testing workflow](testing-workflow.md).

Craft the Barracks Bonfire from two iron ingots, two copper ingots, one campfire, and three stone bricks:

```text
Iron    empty     Iron
Copper  Campfire  Copper
Brick   Brick     Brick
```

Or use `/give @s darkspawn:barracks_bonfire`.

Build this 3×3 foundation, then place the bonfire directly above the center copper block:

```text
Stone brick  Stone brick  Stone brick
Stone brick  Copper      Stone brick
Stone brick  Stone brick  Stone brick
```

All oxidation stages and waxed versions of the full copper block work. Cut copper does not.

With an empty hand:

- **Right-click:** turn the bonfire on or off.
- **Sneak + right-click:** upgrade through 12x12x12, 24x24x12, 32x32x20 and 50x50x30 blocks. New cores start at tier 1. Upgrades cost 8 iron ingots, then 4 gold ingots, then 2 diamonds from inventory; Creative skips payment.
- Each interaction briefly shows three square particle outlines: the lower boundary, ground level, and upper boundary.

An unlit bonfire is inactive. **Blue flames mean activation is still clearing hostiles or placing the first light sources. Orange flames mean active.** There is no ongoing fuel or resource consumption. Breaking the bonfire or any foundation block disables the ward. Repair the foundation and reactivate it.

Existing saved range IDs 1, 2 and 3 become tiers 1, 2 and 3. Their protected areas shrink on upgrade to this mod version; old outlying ward lights clean themselves up when ticking. Breaking and replacing a bonfire preserves its paid tier through the native block-state item component, but it must be activated again on a valid foundation. The fourth tier never wraps back to the first.

## Protection rules

Coverage is a block-aligned rectangular volume: width/depth/height are exactly the tier dimensions above. One third of the height (rounded down) extends below the core; the remainder extends above it. Even widths span whole blocks from core coordinate minus half-width to core coordinate plus half-width (exclusive). A hostile's entire collision box is checked, so it cannot put its head across a boundary while keeping its feet outside. Deeper caves are outside the ward.

Ordinary hostile spawns are rejected inside the ward, including spawn eggs, spawners, raids, and ordinary hostile summons. This can prevent raid waves from appearing inside a base. Players, villagers, animals, and griffins pass normally. The first version classifies ordinary hostiles by Minecraft's monster category, so neutral-but-monster-category creatures such as Endermen are also excluded.

Vanilla Withers and Ender Dragons, and Darkspawn's main bosses, are explicit exceptions. The ward does not cancel deliberate boss encounters or remove bosses. **An active ward is not protection from bosses, projectiles fired from outside, or explosions originating outside.** Those additional defenses remain a separate design decision.

Movement checks stop walking, knockback, piston movement, and falls across the ward, including movement that would cross the entire volume in one step. Ground navigation is directed away from a rejected crossing. Hostiles moved inside through teleportation or vehicle travel are checked before their entity ticks, including passengers.

On activation, existing ordinary hostiles are moved to safe, unoccupied ground outside all overlapping wards, with clearance beyond the boundary. This does not damage them or grant drops. The search is bounded, uses loaded chunks, and does not edit terrain. If it cannot find a safe location, the hostile is paused before its AI and fuse tick, and the bonfire remains blue. This can happen with flying mobs, enclosed terrain, or an unloaded perimeter. Clear a safe area outside the ward or move/remove the hostile to complete activation.

## Lighting and loaded chunks

The fire emits normal level-15 block light. For wider lighting, the prototype gradually places invisible, replaceable light sources on an eight-block grid in empty space at several heights. It never replaces water, buildings, vegetation, or other non-air blocks. Players can build through these lights. Standard block-light effects, including plant growth and melting, still apply.

This produces real environmental lighting, not a Night Vision effect. **Walls block the light normally; enclosed rooms and irregular terrain may still have dark pockets.** Light can also spill past the ward boundary through open space. The grid is a feasibility prototype, not a guarantee of uniform lighting throughout every building. Ward protection does not depend on brightness.

Bonfires do not force-load chunks. Protection is available while the core and its foundation are loaded; new chunks receive light on later passes. Light sources without a covering enabled bonfire remove themselves on their next scheduled check, normally within two seconds in ticking chunks. Cleanup waits while their chunk is unloaded. Overlapping bonfires retain shared lights, and upgrading tiers or breaking a core never removes player-placed replacement blocks.

## Verification

The checkpoint build passed all 67 unit tests and all 99 required server GameTests, including seven bonfire integration tests.

Unit tests exercise rectangular tier boundaries, body contact, fast crossings, vertical falls, and escape movement. Server GameTests exercise the actual spawn and movement hooks, friendly/boss exceptions, relocation, an ignited creeper with no safe destination, light placement and cleanup, and overlapping/unloaded wards.

Live playtest checklist:

1. Build the foundation in a disposable world. Test activation, range cycling, flame colors, boundary rings, and foundation removal.
2. At night, inspect a large flat area, a hill, a building interior, and shallow/deep caves. Confirm light sources are invisible and can be built over. Assess appearance and performance at 75 blocks.
3. Lead zombies and creepers toward each boundary. Test a flying hostile, knockback, a falling mob, and a hostile passenger in a vehicle. Try an ignited creeper before activation.
4. Let two bonfires overlap, disable one, and verify the other retains its coverage and lighting.
5. Save/reload, unload/reload the core's chunk, and inspect orphan-light cleanup after returning.
6. Use two clients to check state changes, lighting updates, hostile rejection, and performance. Ordinary mobs should never be reported cleared while trapped inside.

Live visual quality, full-scale lighting performance, and two-client behavior remain playtest checks.
