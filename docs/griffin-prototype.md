# Griffin flight prototype

First movement checkpoint from [vehicles, mounts, and base ideas](vehicles-mounts-and-base-ideas.md).

Griffins naturally spawn on exposed jagged, frozen and stony peaks at Y128 or higher; see [mount habitats](mount-prototypes.md#natural-habitats). Feed three raw rabbits, two seconds apart, to tame a griffin. Only its owner can ride, and no saddle is required. Cargo, capsule storage and vet recovery are not implemented. It is available before the dragon and remains present in Peaceful. Completing taming makes a wild griffin persistent.

Use the current playable JAR indexed in `build/libs/README.md`; older checkpoints are archived under `build/libs/archive`. Replace the existing Darkspawn JAR in a test instance; keep the required Fabric API and GeckoLib dependencies. Both clients and the server need the updated mod. Source edits do not update an already packaged JAR.

## Try it

Use a disposable Creative test world with commands enabled, find open ground, then run:

```mcfunction
/summon darkspawn:griffin ~ ~1 ~
```

Switch to Survival to check normal riding. Feed three raw rabbits with two-second pauses, then right-click your tamed griffin with an empty hand. One player can ride at a time. There must be enough overhead room for the rider.

| Control | Action |
| --- | --- |
| Normal movement keys (WASD by default) | Walk or fly relative to the direction you face |
| Mouse | Turn the griffin |
| Jump (Space by default) | Take off or rise |
| X | Descend; hold until touchdown to land |
| Release movement keys | Brake and hover while airborne |
| Sneak (Shift by default) | Dismount |

Change **Griffin: Descend / Land** under **Darkspawn Mounts** in Controls to rebind X. The normal movement and jump bindings are respected. Opening a screen stops movement input; in multiplayer the griffin hovers. If control packets stop arriving for more than ten server ticks, it starts descending. An unoccupied airborne griffin also descends.

Ground dismounting searches for a nearby safe floor with room for the player. If none exists, the prototype grants five minutes of Slow Falling as an emergency midair exit aid. This is a testing convenience, not a settled survival balance mechanic. It does not protect against lava, the void, or hostile attacks.

The griffin saves its name, health, position, and flight state. Reloading clears old movement input. Its body collides with terrain; the decorative wings are outside its collision box. Rider headroom is checked during walking and flight. Upward flight stops below the dimension's build ceiling.

## Verification

The full build passed with 64 unit tests and 92 server GameTests, including seven griffin tests. After concurrent model edits, the current model also passed a separate isolated model test and package build; the packaged classes and texture were checked against that output. Live-client and two-client playtesting remain pending.

Compile during development with JDK 25:

```powershell
.\gradlew.bat classes clientClasses --max-workers=1
```

Package only when explicitly requested with `.\gradlew.bat playBuild --max-workers=1`. See [the testing workflow](testing-workflow.md) for targeted checks and full verification without packaging.

Griffin server tests cover rider authorization, passenger limits, normal tick control handling and timeout, low-roof mounting, takeoff, hover, landing, wall and headroom collision, dismounts, and persistence. A unit test bakes and animates the model and decodes its texture. These checks do not establish camera comfort or multiplayer smoothness.

Live checks still needed:

1. Walk, strafe, fly from one mountain to another, hover, and land on a ledge. Inspect first- and third-person cameras, folded wings, flapping wings, and rider position.
2. Try slopes, narrow gaps, overhangs, water, and obstructed landing spots. Walk under a low roof and ascend toward one; neither should put the rider's head inside a block.
3. Dismount on flat ground and in midair. Check Slow Falling on a Survival player. Leave a griffin airborne and confirm it descends.
4. Rename it, damage it, save/reload while airborne and on the ground, and reconnect while mounted. Confirm its name and health persist and it does not continue an old input.
5. Use two clients with the same updated mod. Observe flight and rider position from the second client, join while flight is underway, and try to occupy the same griffin. Check behavior under latency.
6. Rebind movement, jump, and descend. Open chat and inventory in flight; typing must not steer the griffin.

Mountain discovery, rabbit-based taming and owner-only riding are implemented. Follow/stay commands, cargo and the rest of the companion lifecycle remain later milestones. Capsules and paid recovery remain required before a survival-ready mount release.

Model: `src/client/java/darkspawn/black/client/mount/GriffinModel.java`. Regenerate the prototype texture with `python tools/assets/griffin-texture.py`.
