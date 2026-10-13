# Creature mount prototypes

Thirteen additional riding prototypes join the griffin. All fourteen now have natural habitats and can also be summoned with commands. Clients and server must use the same updated Darkspawn JAR, alongside Fabric API and GeckoLib. Source changes require a future explicitly requested build before they reach an existing packaged JAR.

| Mount | Summon command | Movement |
| --- | --- | --- |
| Poptart Cat | `/summon darkspawn:poptart_cat ~ ~1 ~` | Flight and hover |
| Riding Dragon | `/summon darkspawn:dragon_mount ~ ~1 ~` | Flight and hover |
| Winged Unicorn | `/summon darkspawn:winged_unicorn ~ ~1 ~` | Flight and hover |
| Woolly Rhino | `/summon darkspawn:woolly_rhino ~ ~1 ~` | Ground travel and jumping |
| Flying Squirrel | `/summon darkspawn:flying_squirrel ~ ~1 ~` | Ground travel, jumping, and descending glides |
| Riding Golem | `/summon darkspawn:riding_golem ~ ~1 ~` | Slow ground travel and jumping |
| Sasquatch | `/summon darkspawn:sasquatch ~ ~1 ~` | Ground travel and jumping |
| Cerberus | `/summon darkspawn:cerberus ~ ~1 ~` | Fast ground travel and jumping |
| Jungle Leopard | `/summon darkspawn:leopard ~ ~1 ~` | Fast ground travel and strong jump |
| Giant Seahorse | `/summon darkspawn:seahorse ~ ~1 ~` | Swimming with depth control |
| Giant Scorpion | `/summon darkspawn:giant_scorpion ~ ~1 ~` | Ground travel and jumping |
| Swamp Crocodile | `/summon darkspawn:crocodile ~ ~1 ~` | Ground travel and swimming |
| Giant Mole | `/summon darkspawn:giant_mole ~ ~1 ~` | Ground travel and deliberate tunneling |
| Griffin | `/summon darkspawn:griffin ~ ~1 ~` | Existing flight and hover |

The riding dragon and golem have separate entity IDs from bosses and other creatures. These mounts do not replace or tame existing boss entities.

## Taming and ownership

Feed a mount its preferred item three times, at least two seconds apart. Progress messages and green particles show trust; hearts confirm the final feeding. The active feeder reserves the attempt for 60 seconds after each successful feeding. Another player cannot finish that attempt during the reservation; afterward, a new feeder starts from zero. The original feeder retains their saved progress until another player starts. Partial trust does not prevent a wild mount from despawning.

| Mount | Preferred item |
| --- | --- |
| Griffin | Raw rabbit |
| Woolly Rhino | Wheat |
| Flying Squirrel | Sweet berries |
| Poptart Cat | Cookie |
| Riding Dragon | Cooked beef |
| Winged Unicorn | Golden apple |
| Riding Golem | Iron ingot |
| Sasquatch | Apple |
| Cerberus | Bone |
| Jungle Leopard | Raw chicken |
| Giant Seahorse | Kelp |
| Giant Scorpion | Spider eye |
| Swamp Crocodile | Raw cod |
| Giant Mole | Beetroot |

Taming consumes one item per successful feeding in Survival; Creative does not consume items. Ownership uses the player's UUID and survives saves, restarts and reconnects. Wrong items, cooldown clicks and other players' attempts consume nothing. Once tamed, only the owner can feed or ride. Feeding heals four health points (two hearts), with the same two-second cooldown; full-health companions waste no food. Iron and bones work as bonding/repair items for the golem and Cerberus.

Existing mounts retain their identity, name, position and health. Ownerless mounts must be tamed normally; there is no free first-rider claim. Previously persistent mounts stay persistent. Movement controls and dismounting are unchanged, and no saddle or harness is required. Ownership does not make a companion invulnerable; defeated owned mounts enter veterinary recovery.

## Controls

Tame the mount first, then right-click with an empty hand to ride. Only its owner can ride; no saddle is required. Normal movement keys steer relative to where the rider faces. Jump rises on flyers and jumps on ground mounts. **Mount: Descend / Land**, bound to X by default under Darkspawn Mounts, lowers flyers and accelerates the squirrel's descent. Space and X also control swimming depth. Hold **Mount: Dig**, V by default, while moving on the mole to excavate. Sneak dismounts. Only the current rider can send movement controls; each mount carries one player.

The squirrel needs height for a useful glide. It cannot gain altitude or hover once airborne, even while holding jump. Ground mounts retain gravity and normal fall damage. Airborne mounts descend on Stay or when rider control messages stop arriving, except unclaimed sky cats, which hover. Follow steers unoccupied flyers toward their nearby owner. Opening chat or inventory clears input; flyers hover, while a squirrel continues descending.

Names, health, position, and airborne state survive saving. Movement input is cleared on reload and dismount. Mounting and movement check rider headroom. The taller golem and Sasquatch need substantially more clearance. As with the griffin checkpoint, emergency dismounts without a safe nearby floor grant Slow Falling for testing.

## Prototype boundaries

These are movement, appearance, and natural-encounter prototypes. Food-based taming and exclusive riding ownership are implemented; saddles are intentionally unnecessary. Cargo, breeding, capsule storage, veterinary recovery, follow/stay commands and recipient-approved transfers are implemented for all fourteen species. Further species support abilities remain future work. The four combat abilities and Cerberus protection below are implemented. Cloud, carpet, airplanes, and mech are separate transport concepts.

## Combat abilities

Hold **Mount: Ability**, G by default and rebindable under Darkspawn Mounts, to use an attack whenever its cooldown permits. Rhino charge also requires forward movement on dry ground. The server validates the owning rider and chooses targets; releasing forward, stale controls, dismounting, water, airborne movement or a wall ends a charge. Each activation displays its cooldown.

| Mount | Ability | Cooldown |
| --- | --- | --- |
| Riding Dragon | Eight-block aimed cone; 8 fire damage and three seconds of burning. Blocks stop hits and particles; terrain is never ignited or destroyed. | 4 seconds |
| Woolly Rhino | Up to one second of faster ground movement; 10 damage and knockback on contact, once per target per charge. Actual movement is required. | 5 seconds |
| Giant Scorpion | Nearest hostile in a forward cone within 3.5 blocks; 4 damage and Poison I for five seconds. Poison-immune mobs retain their immunity. | 3 seconds |
| Swamp Crocodile | Nearest hostile in a forward cone within 3.5 blocks; 8 damage and Slowness I for two seconds. Works on land and underwater. | 2 seconds |
| Cerberus | Passive fire/lava immunity for the mount and its mounted owner. Other damage still applies; rider protection ends immediately on dismount. | Always active while mounted |

Damage values are health points before armor and resistance; two health points equal one heart. Attacks exclude players, owned companions, allies and peaceful regional wildlife. Neutral vanilla enemies must already be targeting the rider or mount. All attacks require a clear line to the target. Misses still consume the cooldown. Cooldowns survive remounting and save/reload; active attacks do not resume after loading. Fire breath respects fire immunity. Cerberus does not add or remove potion effects, does not grant lava walking, and does not protect a dismounted player. Abilities use existing sounds and particles plus server-synchronized model poses: the dragon opens its jaw and braces its wings, the rhino lowers its head for the charge, the scorpion sweeps its tail and claws, and the crocodile opens and closes an articulated jaw. Strike poses begin on ability activation and ease back to locomotion over 12 ticks for breath or eight ticks for sting/bite. The charge pose holds during the burst and releases over its final three ticks; cancellation clears it immediately. Reloading and dismounting clear active poses without resetting cooldowns. Editable attack clips are included in the four Blockbench projects.

## Natural habitats

All mounts are available without a dragon-victory gate, including in Peaceful. The twelve ground-spawned mounts use native creature spawning, the mob-spawning gamerule and population caps, one animal per group, safe supported ground, full-body clearance, and no liquid overlap. One living mount of the same species within 96 blocks in each axis prevents another encounter nearby. These are relative spawn weights, not percentage probabilities or guaranteed timers. Existing loaded terrain can spawn them; new chunks are not required. Restart the updated client/server to register the new biome spawn entries.

| Mount | Habitat | Weight |
| --- | --- | ---: |
| Griffin | Exposed jagged, frozen or stony peaks at Y128 or higher | 2 |
| Cerberus | Nether biomes below Y120, on dry ground rather than the roof or lava | 2 |
| Flying Squirrel | Surface forests and taigas | 4 |
| Winged Unicorn | End island surface at Y30 or higher | 2 |
| Woolly Rhino | Snowy plains, ice spikes, snowy taiga, grove, snowy slopes, frozen or jagged peaks | 3 |
| Riding Dragon | Overworld caves below Y48, at least eight blocks of overhead terrain, lava within six blocks horizontally and three vertically | 1 |
| Riding Golem | Overworld caves below Y48, at least eight blocks of overhead terrain | 3 |
| Sasquatch | Snowy taiga and grove surfaces | 2 |
| Jungle Leopard | Surface jungle, sparse jungle and bamboo jungle | 3 |
| Giant Seahorse | Ocean water with full body clearance and water above/below | 2 (water creature) |
| Giant Scorpion | Desert surfaces | 2 |
| Swamp Crocodile | Swamp and mangrove swamp banks within six blocks horizontally and three vertically of water | 3 |
| Giant Mole | Soul Sand Valley below Y120, supported dry ground | 1 plus 1-in-100 placement roll |
| Poptart Cat | Very rare upper Overworld air; separate rules below | Separate timer |

Unclaimed natural mounts count toward creature caps and may despawn with distance. Naming or completing taming preserves them. Wild/claimed state survives saves, and previously summoned mounts remain persistent. Mounts have no autonomous combat AI. Cerberus alone has fire and lava immunity, also protecting its mounted owner. The squirrel glides but cannot rise in flight.

## Swimming and digging

Seahorses and crocodiles breathe underwater; their riders still need air or suitable diving equipment. Swimming does not enable flight. The seahorse moves very slowly on land. Leopards have a stronger jump and six blocks of safe fall distance.

The giant mole spawns only in Soul Sand Valley below Y120. Its weight of 1 is further reduced by a 1-in-100 roll for each otherwise-valid placement; this is not a fixed encounter timer. Hold V while moving to clear body and rider headroom, preserving the floor. Excavation is limited to 12 blocks per five ticks, drops normal diamond-tier mining loot without Fortune or Silk Touch, and respects build restrictions, world borders, spawn protection and Fabric block-break vetoes. Fluids, unbreakable blocks and block entities such as containers are skipped. Creative digging follows the normal no-drops rule. The mole has no lava immunity and does not dig vertically.

## Nyan-inspired Poptart Cat

The gray cat has a pastry body, pink frosting, sprinkles, and a six-color rainbow emitted from its rear whenever a player rides it, on the ground or in flight. Every tracking client produces the cosmetic particles locally; particle settings still apply. It does not play the Nyan Cat song or modify terrain.

Natural encounters are available at any time in the Overworld, without dragon progression. Once per minute, the world makes a 1-in-20 roll while at least one living, non-spectator player is within 80 blocks of the height ceiling. A successful roll tries one loaded position 32–64 blocks horizontally from an eligible player, in the top 48 blocks with eight blocks of upper clearance (normally Y=272–311). This is an average of 20 eligible minutes per successful roll, not a guaranteed spawn interval; blocked positions, unloaded chunks and a living cat within 128 blocks cancel the attempt. The mob-spawning gamerule is respected.

Unclaimed natural cats hover and use normal distance despawning. Naming preserves them; completing taming immediately makes them persistent, including before the first ride and after reload. Previously summoned cats retain their persistent behavior. Only the tamer can ride. A naturally spawned cat keeps hovering until its first ride.

## Appearance pass

All thirteen newer models now have species-specific anatomy and material patterns. Dragon/unicorn wings have articulated tips; Cerberus turns all three heads; squirrel legs spread with its gliding membranes; golem joints and Sasquatch fur/hunch differ. Flight poses, including the griffin, blend over eight entity ticks with render interpolation. Mounts use quiet native animal/material sounds and their existing resource-pack subtitles. Legendary armor, cooking labels, temperature HUD layout, and boss-warning music ducking were polished alongside this pass.

Run `node tools/assets/verify-mounts.cjs` for the thirteen Java-model source/texture checks and `node tools/assets/griffin-model.cjs --check` for the griffin. The contact sheet now samples textures rather than using one flat color per cube. Neither the sheet nor the automated checks establishes live rider fit, camera comfort or audio mix quality.

Each new mount has its own Java model, texture, and editable source under `art/blockbench/<entity_id>/`. `node tools/assets/travel-mounts.cjs` regenerates all thirteen and overwrites their generated source files. Runtime models animate walking, head movement, tails, and wings where applicable; the Blockbench files contain an idle authoring pose.

## Verification and playtest

With JDK 25 selected, run `./gradlew.bat test --tests 'darkspawn.black.mount.*' --max-workers=1` for mount model checks. Before pushing, run `./gradlew.bat fullCheck --rerun-tasks --max-workers=1` without packaging. Create a JAR only when explicitly requested, using `playBuild`; see [the testing workflow](testing-workflow.md). Historical checkpoint JARs are archived under `build/libs/archive`, and the existing current JAR is indexed in `build/libs/README.md`.

Automated checks cover loading and animating each model, texture decoding, mounting, rider authorization, passenger limits, save/reload, blocked headroom, flight/hover/landing, gliding without altitude gain, gravity on ground mounts, aquatic depth control, habitat restrictions, and deliberate mole excavation with protected-block and control-timeout checks. The original griffin tests exercise the shared controls, timeout, collision, and emergency dismount behavior.

Mount movement is server-owned on both sides of the connection. `PrototypeMount` overrides both authority checks so a local rider accepts server position updates and does not send conflicting vanilla vehicle positions. Regression checks cover this client ownership flag and the cat's normal-tick flight followed by a vanilla Shift-input dismount.

October 13, 2026: the user confirmed the repaired cat controls work in the development client (WASD, Space, X and Shift). This verifies the reported frozen-rider fix; the broader camera, terrain and multiplayer checks below remain separate.

Pre-push verification on October 12, 2026: `fullCheck --rerun-tasks --max-workers=1` passed all 81 unit tests, all 166 server GameTests (including all 11 mount tests), and all 142 cuisine asset checks in one run. Unit and server tests ran sequentially. No JAR was packaged.

The checkpoint JAR's 32 mount-related class files match the compiled output, and all eight new packaged textures match the source files. Editable Blockbench textures also match their runtime PNGs. Source silhouettes were visually reviewed in the generated contact sheet; run `node tools/assets/mount-contact-sheet.cjs` to reproduce it.

Live-client and multiplayer playtesting is still required. Check every silhouette, folded and spread wings, saddle-free rider placement, first- and third-person cameras, movement on slopes, water, low roofs, landing, and two-client observation. Automated model loading does not verify visual quality or camera comfort.

## Companion care

### Companion commands and cargo

Sneak-right-click your companion empty-handed to open its panel within eight blocks. Follow approaches the owner within 32 blocks in the same dimension. Ground mounts navigate, flyers steer through open air and aquatic mounts swim. Seahorses stop chasing an owner on land. Following does not teleport, load chunks or dig, and terrain can block it. Stay stops following and cancels breeding readiness; flyers settle.

All fourteen species have nine cargo slots. Cargo survives saves, capsules, recovery and ownership transfer. Capsules, bundles, shulker boxes and items carrying container contents cannot go inside mount cargo.

### Capsules and veterinary recovery

Use an empty Companion Capsule on your owned, unoccupied, unleashed mount to store its identity, name, health and cargo. Use the filled capsule on safe ground to release in Stay mode. Seahorses require water around their full body. Ridden mounts, breeding requests and pending transfers block capture. Ordinary lost capsules must be recovered; veterinary reissue is limited to defeated companions.

Lethal damage to an owned mount creates a recovery token. Carry a Veterinary Kit and use the token on a safe surface to restore full health, consuming one kit on success. Use a kit directly on an injured companion to heal it fully. Use a kit in air to reclaim one missing recovery token; joining also returns one. Offline recoveries remain saved. Copied tokens cannot redeem a companion twice. OneBlock companions and cargo cannot cross into normal worlds through capsules, or vice versa.

### Breeding and ownership transfer

Set two healthy adults of the same species and owner to Stay near each other. Choose Breed for each, spending one preferred item from inventory per parent. Readiness lasts thirty seconds. A successful pairing creates one owned baby and starts a five-minute cooldown for both parents. Babies grow after twenty minutes of loaded entity ticks and cannot be ridden before then. Growth pauses while stored. Saves retain growth and cooldowns but cancel incomplete breeding requests.

Select a living nearby player and choose Offer transfer. That recipient must sneak-right-click the mount empty-handed and accept within sixty seconds while both players remain within eight blocks. The offer includes cargo. Pending offers block riding, storage and breeding. Cancel or reload clears the offer. Acceptance revokes the old owner immediately and selects Stay.

### Companion item recipes

Each shaped crafting-table recipe creates one item; both items also appear in Tools and Utilities. Creative use retains consumables.

| Item | Top row | Middle row | Bottom row |
| --- | --- | --- | --- |
| Companion Capsule | empty / iron ingot / empty | amethyst shard / ender pearl / amethyst shard | empty / iron ingot / empty |
| Veterinary Kit | empty / white wool / empty | apple / gold ingot / apple | empty / iron ingot / empty |

Capsules and recovery tokens reference one authoritative server record. Records preserve identity and cargo and can be redeemed only once. Online owners receive recovery tokens immediately when in the matching OneBlock or normal inventory domain. Recovery for offline owners remains saved until they return. Transfer recipients cannot take cargo before accepting.
