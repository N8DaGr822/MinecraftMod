# The Legacy of Noah

Relics of **Noah, Heir of Poseidon — Prince of the Tides**. These are administrator gifts, with no crafting recipes, natural drops or automatic entitlement based on a player's name.

## Give Noah his gift

An administrator runs this with his actual Minecraft account name:

```mcfunction
/give <player> darkspawn:legacy_of_noah 1
```

The recipient uses the gift to receive four pieces of **Noah's Divine Regalia**, **Noah's Tempest**, the **Conch of the Tideborn Heir**, and a written lore/controls guide. The six relics bind immediately to the recipient's UUID. The guide is readable by anyone. Do not open the gift on the administrator's account first.

Individual gifts are also available through `/give`: `darkspawn:noah_helmet`, `darkspawn:noah_chestplate`, `darkspawn:noah_leggings`, `darkspawn:noah_boots`, `darkspawn:noahs_tempest`, and `darkspawn:tideborn_conch`. Individual armor binds when worn; the weapon binds when used or swung; the conch binds when used. An already bound item cannot be rebound by another player. Foreign armor is returned to inventory, or dropped if inventory is full, on the next server equipment check.

## Divine Regalia

| Relic | Divine benefit |
| --- | --- |
| Crown of the Tideborn Heir | Water breathing; underwater night vision |
| Cuirass of the Ocean Heir | Strength II and Resistance I in water or exposed to rain |
| Greaves of the Abyssal Prince | Dolphin's Grace while swimming |
| Stride of the Endless Sea | Fall-damage protection, speed in water/rain, and standing on exposed water surfaces; sneak to dive |

**Blessing of the Tideborn** requires all four bound pieces: conduit power and one health point healed per second in water, drowning protection, peace with drowned/guardians/elder guardians, and a six-damage lightning retaliation against a visible hostile attacker with a five-second cooldown. Bypass-invulnerability damage is not canceled. Short effect refreshes can linger briefly after removal; night vision lasts longer to avoid its native expiration flicker.

The first complete-set activation reveals **Noah, Heir of Poseidon / Prince of the Tides**. The raised crown, turquoise plating, gold trim and animated rune texture use the repository's native custom armor renderer. Thalassar uses GeckoLib. No new rendering library or dependency is required.

## Noah's Tempest

- Normal use retains native charged trident throwing. First binding adds Loyalty III and Impaling V. Its native melee attribute is stronger than an ordinary trident.
- Sneak-use cycles **Tidal Burst**, **Whirlpool Call**, and **Judgment of the Deep**.
- **K** casts the selected power; **B** invokes **Wrath of the Seventh Sea**. Both bindings can be changed under *Controls → The Legacy of Noah*.
- Impaling also affects land targets for bound direct melee and thrown hits. Targets already vulnerable to native Impaling do not receive the bonus twice. Powers use a separate magic damage source rather than accidentally inheriting weapon enchantment damage.

Tidal Burst damages and knocks back visible hostiles in a forward cone. Whirlpool Call pulls hostiles toward a point ahead and slows them. Judgment strikes visible nearby hostiles with lightning presentation and direct magic damage. Ordinary power cooldown is five seconds, reduced to 2.5 seconds in Dominion.

Powers and Guardian Surge exclude players, owned creatures, allied entities, friendly expedition NPCs and non-hostile regional wildlife. Lightning bolts are visual-only; intentional damage is applied separately to selected enemies. Powers do not create fire, replace terrain or change global weather. Normal trident attacks retain native combat behavior.

## Thalassar, Steed of the Tideborn Heir

Use the conch to summon or recall the Royal Hippocampus onto clear water or safe nearby ground. Sneak-use lets him rest. Dismount and remove any leash before recalling or resting him.

- Empty-hand use mounts him; only his bonded owner can ride.
- The owner can feed him kelp using the existing companion healing rules. Food cannot tame a separately command-spawned Thalassar; ownership comes from the conch. Horse calls on land and dolphin calls underwater replace the generic mount's fox sounds.
- **WASD** steers, **Space/X** changes depth, and the existing **G** mount ability invokes **Abyssal Dash / Guardian Surge** underwater. Controls are rebindable under Mounts.
- He swims quickly, moves more slowly on land, breathes underwater and takes no fall damage.
- Dash lasts approximately three quarters of a second and has a five-second owner cooldown; nearby visible hostile enemies take damage and knockback when it starts.
- Defeat returns him to a server-owned resting snapshot. After 30 seconds, the conch restores him at full health. Ordinary rest/recall preserves his health.
- The general companion capsule and transfer/breeding menu are deliberately unavailable: the conch is his single recovery route.

The server stores one active/resting mount per bonded player, shared by all copies of that player's conch. Recall preserves his UUID. If his recorded entity is unloaded, the conch requests its last recorded chunk and asks the owner to try again; it does not create a replacement while the original is unaccounted for. There is no permanent force-loaded chunk. Rest before crossing the OneBlock boundary; ordinary dimension recall can move a loaded, unoccupied companion after finding a safe destination.

GeckoLib clips cover idle, walking, swimming, dash/strike and summoning. Model and animation sources are native JSON under `assets/darkspawn/geckolib`; their generation source is `tools/assets/noah-legacy.py`.

## Noah's Dominion

Wear the full bound regalia, hold the bound Tempest in the main hand, and ride the owner's living Thalassar. This grants faster swimming, faster power recharge, a sea-particle aura and nearby hostile slowing.

**Wrath of the Seventh Sea** requires Dominion, lasts five seconds and has a 60-second cooldown. Its repeated storm strikes stop when the complete combination is no longer active. Power, retaliation, dash and recovery times are saved on the server against the owner, so changing items or reconnecting cannot reset them.

Six named advancements record the journey: **Chosen by the Sea**, **Crowned by the Ocean**, **The Storm Answers**, **Ride of the Tideborn**, **Heir of Poseidon**, and **Legacy of Noah**. The feature does not add a world-generated shrine or a survival unlock quest: receipt remains administrator-controlled.

## Verification and gameplay acceptance

Compile with `gradlew.bat classes clientClasses --max-workers=1`. Run `gradlew.bat runGameTest -PgameTestFilter=darkspawn-gametest:noah_game_test* --max-workers=1` for binding, powers, recovery, water surfaces, swimming, persistent cooldowns, land Impaling and advancement loading. `tools/assets/verify-noah-legacy.py` checks asset references, geometry, animation bones and texture bounds and renders `build/noah-legacy-preview.png`.

The preview is an orthographic review of authored source geometry, not an in-game screenshot. Live acceptance must cover animation transitions, first/third-person trident poses, armor fit across player skins, book page wrapping, key rebinding, multiplayer observers, genuine disconnect/restart, cross-dimension recall and calling from an unloaded chunk. No JAR or version bump is implied by this implementation.

Implementation validation (October 16, 2026): common/client compilation passed, as did all 9 Noah checks, 36 checks selected by the mount-related filter, 23 expedition-armor checks and 8 armor-loadout checks. The mount wildcard can also select mount-related cases from other groups. Asset validation and `git diff --check` passed. Logs: `build/noah-tests.log`, `build/noah-regression-mount.log`, `build/noah-regression-expedition_armor.log`, and `build/noah-regression-armor_loadout.log`. These are targeted checks, not an unfiltered `fullCheck` or a release build.

## Polish review — October 16, 2026

The code and targeted server checks cover binding, armor protections, hostile target selection, cooldowns, mount controls/recovery, item delivery and native trident behavior. The source-geometry preview and asset references were also checked; this does not constitute live visual acceptance.

| Finding | Resolution |
| --- | --- |
| Thalassar inherited food taming, allowing a bond outside the conch ledger | Blocked interaction while unbound; existing owner feeding now uses kelp. The new test reproduced the original food-consumption defect. |
| A bonded entity missing from the ledger could remain stuck in its death path | Recovery now reports whether it saved the companion. An untracked duplicate is discarded without overwriting the real companion or using generic mount recovery. |
| Thalassar inherited fox calls | Added horse calls on land and dolphin calls in water, using native resource-pack-compatible sounds. |
| Recall and thrown-weapon integration lacked direct regression coverage | Added checks for injured rest/recall, blocked destinations, actual charged throwing and Loyalty returning exactly one bonded Tempest. |

Polish verification: common/client compilation and **all 12 Noah GameTests passed** in `build/noah-polish-tests.log`. Asset validation and `git diff --check` passed. No shared mount implementation was changed, no unfiltered `fullCheck` was run, and no JAR was packaged.

Remaining presentation work, in priority order:

1. **Thrown Tempest appearance:** the held item is custom, but the projectile still uses the vanilla trident renderer. A dedicated projectile presentation would preserve the royal appearance in flight.
2. **Live visual acceptance:** check armor fit on both player body types, held/charging poses, mount rider placement and animation transitions, underwater readability, and book wrapping at normal GUI scales. The source preview cannot establish these.
3. **Power feedback:** selection and cooldowns currently use action-bar messages. A compact selected-power/cooldown display is an optional UX addition; it would require server state synchronization.
4. **Persistence acceptance:** exercise a genuine server restart, distant unloaded-chunk recall, cross-dimension recall and a second observing client. Saved-data serialization and duplicate prevention are tested, but these complete sessions are not yet verified.
