# Ground-walker mech: implementation and acceptance

October 22, 2026. The approved ground-walker design is implemented in source; current verification is recorded below. One pilot, ground movement, one-block stepping, iron repair, saved ownership and capsule/defeat recovery reuse the existing mount systems. Live two-client acceptance has not run; see [the acceptance record](two-client-acceptance.md).

## Existing implementation reference

Use `PrototypeMount` and the non-flying `RidingGolem` movement as the reference. This is a separate crafted machine, not a replacement for the living golem or an aircraft variant. Reuse server-authoritative pilot input, rider headroom checks, one-seat ownership, capsule claims and defeat recovery. A separate mechanical model and native crafting recipe are included. This is not wearable power armor.

## Bounded first implementation

- One owner and one pilot. Empty-hand use mounts; WASD moves, Jump uses the existing ground jump, Sneak dismounts. No hover or vertical flight input.
- Start with the golem's 0.18 movement-speed attribute and a one-block step height. Test slopes, ceilings, fences and ledges before changing these values. No block breaking to make room.
- Fixed collision bounds matching the mechanical model, including pilot headroom. Arms and feet must not reach outside collision in a way that implies usable clearance.
- Native Overworld, Nether and End only, matching the flight prototypes' restrictions on expedition and island profiles.
- Crafted placement binds ownership after safe insertion. Iron repairs bounded health; no animal trust/breeding, autonomous follow, combat ability or onboard cargo in the first version.
- Landed capsule storage and the existing recovery token/veterinary-kit flow preserve identity and owner. “Storage” here means storing the walker, not adding a chest to it.
- Show controls and state through placement/use messages and the starter-camp guide. Fresh camps supply one sample in barrel 22 and a short controls/recovery guide.

## Acceptance before implementation is called complete

Test duplicate capsule claims, blocked placement conservation, outsider control, second-seat rejection, save/restart, recovery after defeat, stale input and dimension restrictions. Exercise real one-block stepping, walls, low ceilings, stairs, water, lava edges, safe dismount, death and reconnect. Validate model bounds and camera fit in a real client. Measure a small loaded group separately from real multiplayer server capacity.

No new weapons, mining tool, flight, passenger seats, power/fuel economy or wearable armor API is implied. These require later explicit scope decisions. Live acceptance remains required after the focused automated checks.

## Crafting and controls

Craft `IPI / RBR / I I`: four iron ingots (I), one piston (P), two redstone (R) and one iron block (B). An iron block unlocks the recipe. Use the item on the top of clear solid ground; successful Survival placement consumes one and binds ownership. The walker has 80 health, a 1.8 x 2.5-block body and a 2.4-block pilot seat. Rider headroom is checked separately.

Empty-hand use pilots; sneak-use explains repair/storage. Each iron repairs up to four health with a two-second cooldown, consuming nothing at full health or during cooldown. Animal care/cargo and the hub Care action are unavailable. Capsule capture requires landing. Defeat uses the existing single-claim recovery token and veterinary kit. The mechanical model uses native iron material with alternating leg motion; live appearance and camera fit are unverified.

For a disposable test world, `/give @s darkspawn:ground_walker` supplies the placement item. Fresh Testing Camps include one in barrel 22.

## Current verification

Common/client compilation, six focused unit checks (mount models and resource coverage), and all six mech server GameTests passed. Server coverage includes real one-block stepping, walls/headroom, owner/seat/input restrictions, native crafting and placement conservation, bounded repair and saved cooldown, disabled animal/hub care, capsule duplicate rejection, dimension restrictions and defeat recovery. Logs: `build/mech-targeted.log` (initial fixture failure) and `build/mech-server-verified.log` (all six passed).

`tools/assets/preview-ground-walker.py` validates eleven Java cuboids and their horizontal footprint at idle/walk extremes; the [flat-color preview](../build/ground-walker-preview.png) was inspected. The resource audit passed 1,505 asset JSON files and 4,666 model/texture/sound links (`build/mech-resource-links.log`). These do not establish live rendering or multiplayer acceptance.

The October 22 unfiltered current-snapshot `fullCheck --rerun-tasks --max-workers=1` passed: 133 unit tests, all 467 server GameTests and the cuisine artwork validator. Nexus and Ascension fixtures now activate their actor chunks; the Shadow Incursion fixture isolates its terrain from concurrent encounter tests. A stalled native runner was restarted using the disposable `-PgameTestRunDir=build/oct22-fullcheck-runtime`, retaining normal incremental compilation output. The final log is `build/oct22-snapshot-fullcheck.log`. No JAR or version change was produced. Live two-client acceptance remains open.
