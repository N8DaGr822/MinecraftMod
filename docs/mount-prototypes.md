# Creature mount prototypes

Eight additional summon-only riding prototypes join the griffin. Use a Creative testing world with commands enabled, then switch to Survival to check riding. Clients and server must use the same updated Darkspawn JAR, alongside Fabric API and GeckoLib.

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
| Griffin | `/summon darkspawn:griffin ~ ~1 ~` | Existing flight and hover |

The riding dragon and golem have separate entity IDs from bosses and other creatures. These mounts do not replace or tame existing boss entities.

## Controls

Right-click with an empty hand to ride. Normal movement keys steer relative to where the rider faces. Jump rises on flyers and jumps on ground mounts. **Mount: Descend / Land**, bound to X by default under Darkspawn Mounts, lowers flyers and accelerates the squirrel's descent. Sneak dismounts. Only the current rider can send movement controls; each mount carries one player.

The squirrel needs height for a useful glide. It cannot gain altitude or hover once airborne, even while holding jump. Ground mounts retain gravity and normal fall damage. The airborne mounts descend when left unoccupied or when control messages stop arriving. Opening chat or inventory clears input; flyers hover, while a squirrel continues descending.

Names, health, position, and airborne state survive saving. Movement input is cleared on reload and dismount. Mounting and movement check rider headroom. The taller golem and Sasquatch need substantially more clearance. As with the griffin checkpoint, emergency dismounts without a safe nearby floor grant Slow Falling for testing.

## Prototype boundaries

These are movement and appearance checkpoints. They have no natural spawning, taming, ownership, saddles, cargo, breeding, capsule storage, vet recovery, or species combat/support abilities. Cerberus has no lava immunity; the dragon has no breath attack; the rhino has no damaging charge. Survival progression and balance remain future work. Cloud, carpet, airplanes, and mech are separate transport concepts.

Each new mount has its own Java model, texture, and editable source under `art/blockbench/<entity_id>/`. `node tools/assets/travel-mounts.cjs` regenerates all eight and overwrites their generated source files. Runtime models animate walking, head movement, tails, and wings where applicable; the Blockbench files contain an idle authoring pose.

## Verification and playtest

With JDK 25 selected, run `./gradlew.bat test --tests 'darkspawn.black.mount.*' --max-workers=1` for mount model checks. Before pushing, run `./gradlew.bat fullCheck --rerun-tasks --max-workers=1` without packaging. Create a JAR only when explicitly requested, using `playBuild`; see [the testing workflow](testing-workflow.md). Historical checkpoint JARs are archived under `build/libs/archive`, and the existing current JAR is indexed in `build/libs/README.md`.

Automated checks cover loading and animating each model, texture decoding, mounting, rider authorization, passenger limits, save/reload, blocked headroom, flight/hover/landing, gliding without altitude gain, and gravity on ground mounts. The original griffin tests exercise the shared controls, timeout, collision, and emergency dismount behavior.

Pre-push verification on October 12, 2026: `fullCheck --rerun-tasks --max-workers=1` passed all 81 unit tests, all 166 server GameTests (including all 11 mount tests), and all 142 cuisine asset checks in one run. Unit and server tests ran sequentially. No JAR was packaged.

The checkpoint JAR's 32 mount-related class files match the compiled output, and all eight new packaged textures match the source files. Editable Blockbench textures also match their runtime PNGs. Source silhouettes were visually reviewed in the generated contact sheet; run `node tools/assets/mount-contact-sheet.cjs` to reproduce it.

Live-client and multiplayer playtesting is still required. Check every silhouette, folded and spread wings, saddle-free rider placement, first- and third-person cameras, movement on slopes, water, low roofs, landing, and two-client observation. Automated model loading does not verify visual quality or camera comfort.
