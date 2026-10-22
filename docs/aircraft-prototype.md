# Editable aircraft prototype

October 21, 2026. Implemented in source: one pilot, five to nine captured ordinary blocks, manual flight, saved ownership and capsule/recovery support. No JAR packaged. No passengers, weapons, onboard containers or automatic navigation.

## Assemble and edit

Craft the Aircraft Core with `IPI / RAR / IPI`: four iron ingots, two phantom membranes, two redstone and one amethyst shard. A membrane unlocks the native recipe. The temporary inventory icon uses the native nautilus-shell texture; the flying body renders the actual captured blocks.

On solid ground, build a flat plus sign: a center block and its four cardinal neighbors. The four corners are optional, making a maximum 3x3 single layer. Allowed materials are planks, logs, wool, glass and iron blocks. Containers, fluids and other functional blocks are rejected. Stand beside the build and use the core on the center's upper face. Clear a 5x5 area around the airframe. Assembly checks protection, loaded chunks, world bounds, entities, footing and clearance before capturing exact block states. A successful Survival assembly consumes the core and binds the aircraft to its placer; failures conserve the build and item.

Empty-hand use pilots. WASD steers, Jump ascends, X descends, Sneak exits. Flight reuses the existing server mount controls: bounded target speed, stale-input descent, rider clearance and slow-falling emergency dismount. Manually descend onto clear solid ground before editing or storing. No fuel or aerodynamic simulation is included.

Sneak-use a stopped, landed, empty aircraft to restore its blocks at the current grid location and return one core. The original block orientation is restored, regardless of flight heading. All destination cells must be empty, permitted and clear of other entities. Edit the restored blocks, then assemble again. Original world coordinates are not reserved while flying; protect your landing area normally.

## Bounds, persistence and recovery

The fixed 4.25-block-square collision box encloses the 3x3 layer at every yaw. Its one-block height and 1.1-block pilot seat use the existing rider-clearance checks. This deliberately needs more clearance than a thin wing silhouette. Aircraft do not have the ordinary ground mount's one-block step assist.

Entity save data stores original block states and ownership. Existing Companion Capsules and defeat recovery preserve that snapshot and identity; copied spent tokens cannot duplicate the craft. Capsules require safe ground for release. Placement, capsule release and dimension travel allow only the Overworld, Nether and End. No animal breeding, care menu, inventory-hub cargo access or Follow command is available. Iron repairs through the existing repair/feed interaction.

Fresh Testing Camps include one core and nine oak planks in barrel 22 and a guide page. Existing camps are unchanged.

## Verification

Common/client compilation, resource coverage and three focused server checks passed: actual assembly/item consumption, exact block restoration after changing heading, saved capsule ownership/build, copied-token rejection, forbidden containers, obstructed assembly/editing, outsider/airborne editing rejection, one-seat controls, takeoff, hover, rotated collision and manual landing. Nine shared mount-care checks, the existing unique-companion hub guard, and three starter-camp checks also pass. The resource-reference audit passes. The isolated native world backup/restore/restart check preserved the aircraft owner, block states and health; see [operations validation](server-operations-validation.md). Real client appearance, camera fit, two-client observation and reconnect remain acceptance work.
