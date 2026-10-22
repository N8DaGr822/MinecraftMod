# Magic carpet prototype

The approved one-seat carpet uses existing server-authoritative mount flight, collision, ownership, capsule storage and recovery. No natural spawning or new combat power is added. Source implementation requires an explicitly requested future play build before it reaches a packaged JAR.

## Craft and place

Craft `PPP / AMA / PPP`: six purple carpets, two amethyst shards and one phantom membrane. Obtaining a membrane unlocks the recipe in the native recipe book. Place the item on the top of clear, dry, solid ground; successful Survival placement consumes one item and binds the new carpet to its placer. Blocked or protected placement consumes nothing. Creative placement retains the item.

Initial placement, capsule release and dimension travel are restricted to the Overworld, Nether and End. The prototype cannot enter Ascension, OneBlock, Nexus, Pirate Seas or Forsaken. It does not teleport to an unloaded owner or force-load chunks.

## Controls and care

Empty-hand use mounts; WASD steers, Jump rises, **X descends** by default, and Sneak dismounts. The descent binding is configurable in the existing mount controls. Released controls settle into hover while fresh packets continue; missing input expires after ten ticks and requests descent. Horizontal target speed is bounded by the existing mount flight calculation (0.6 blocks/tick before smoothing), with 0.32 blocks/tick vertical input. The server handles movement and rider headroom. Walls and the world's upper height limit constrain flight.

Land before packing. Dismount searches for nearby safe ground; emergency midair exits receive the existing slow-falling protection. This does not make lava, the void or hostile landing sites safe. An unoccupied carpet descends rather than remaining an abandoned flyer.

Sneak-use opens the existing care menu: Follow/Stay, nine cargo slots and explicit ownership transfer. The breeding button is hidden, and the server rejects breeding. Amethyst repairs through the existing feeding interaction; a command-summoned ownerless carpet can be bound by three amethyst interactions using normal trust cooldowns. Crafted placement skips that binding step.

Companion Capsules store and release the same saved carpet identity, owner and cargo. Defeat uses the existing recovery-token/veterinary-kit workflow. Copies of a spent capsule cannot duplicate a vehicle. The prototype deliberately reuses that existing recovery UI and cost rather than adding a second storage system.

## Presentation and testing

The native model has a thin purple woven deck, gently curling ends and individual fringe, using Minecraft's purple wool texture. The inventory item uses the native purple-carpet model. It has a fixed 1.7-block-wide collision box; decorative fringe extends beyond it. Original painted carpet patterns remain a possible later presentation pass.

Fresh Testing Camps supply one carpet item in barrel 22 alongside ammunition/enchantment trials, plus a guide page. Existing camps are unchanged.

Focused tests cover native crafting/placement, blocked placement conservation, saved ownership and capsule claims, one-seat authority, timed input expiry, ascent/hover/descent, wall/headroom collision, safe dismount and save/load descent. All nine carpet behaviors passed across the initial run and the corrected save-fixture rerun; nine shared mount-care checks, three camp checks, resource coverage and the resource-reference audit also passed. Live rider fit, camera, fringe motion, normal terrain and real two-client/reconnect acceptance remain pending.
