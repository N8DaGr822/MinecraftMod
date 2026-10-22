# Awakened boss attack pass

October 20, 2026. One additional attack sequence for each of the 16 existing seal-enabled boss families, in `BossKind` order. Ancient Tree Spirit is first. Endborn and Ascension Guardians have separate encounter/progression systems and are outside this awakened-rematch roster.

## Shared rules

Use the existing normal summon ritual with an Awakening Seal in the offhand after earning that family's victory or consuming its heart. Failed eligibility still consumes nothing. Existing health/armor/speed scaling, achievements, summon materials, trophy/laurel rewards and heart limits remain unchanged.

Every fourth attack opportunity in an awakened fight becomes a three-strike special. Normal fights never enter this controller. The target's position and the boss-to-target direction are locked at the start; moving afterward does not drag danger zones onto teammates. Pattern coordinates below are relative to that locked position and direction.

The first strike has a three-second warning. Subsequent strikes follow two seconds apart, each with its own warning. The action bar names the sequence and displays 1/3, 2/3 or 3/3. Circles mark dangerous discs; paired circles mark dangerous ring bands. Native themed particles draw the same boundaries used by damage checks, with forced visibility for participants. Each impact deals eight base damage, subject to normal armor, difficulty and invulnerability rules. Overlapping zones in a beat cannot damage a player twice.

Players must remain eligible and in the same dimension, within 40 blocks of the locked center, between two blocks below and four blocks above its height, and visible to the boss. New arrivals receive at least two seconds of warning before they can be hit. Moving out of the participant area and returning restarts that warning protection. Vertical escape and solid cover remain valid.

The boss stops navigation during the sequence and gets a one-second pause after its last impact. Phase transitions, abandonment, loss of the awakened tag and reload cancel pending strikes. Specials create no persistent entities, blocks, explosions, fire, lightning or terrain changes. Reloads restart the attack-opportunity count; there is no saved delayed damage to surprise returning players.

## Roster and dodge patterns

| Order | Boss | New sequence | Pattern |
| --- | --- | --- | --- |
| 1 | Ancient Tree Spirit | Heartwood Ripples | Expanding root bands at radii 6–8, 10–12 and 14–16; the center stays safe. |
| 2 | Mutant Wolf | Frostfang Trail | Three frost discs cross the locked point from left to right. |
| 3 | Mutant Zombie | Gravehammer Steps | Paired impact discs march from back to front, leaving a middle lane. |
| 4 | Fossil Tyrant | Fossil Stampede | Three-disc columns move left to right; move between the columns. |
| 5 | Thunder Bird | Storm Compass | Four lightning discs spread outward along the cardinal axes. |
| 6 | Titan Boa | Closing Coils | Ring bands contract from radius 14 to 10 to 6; escape outward or remain in the safe center. |
| 7 | Baba Yaga | Witch's Waltz | Three hex discs rotate around the locked point. |
| 8 | Mountain Titan | Fault March | Three-disc rows advance from back to front. |
| 9 | Ice Wyrm | Glacial Traverse | A large frost disc crosses the point diagonally. |
| 10 | Kraken | Tidal Reversal | Outer ring, center disc, outer ring; switch between inside and outside. |
| 11 | Cave Crawler | Brood Scissors | Opposed web discs rotate around the point. |
| 12 | Shadow Creeper Queen | Shadow Pressure | Expanding center discs plus outer rings leave a shrinking safe band between them. |
| 13 | Mycelial Sovereign | Spore Blossom | Three spore discs spread outward in a triangle. |
| 14 | Netherborn | Cinder Pincers | Paired fire discs cross in opposite directions on two parallel lanes. |
| 15 | Soulbound Colossus | Soul Convergence | Two hollow soul rings approach and finally overlap at the center; their holes stay safe. |
| 16 | Void Eye | Orbital Collapse | Opposed orbiting discs and an outer ring finish with a center strike. |

## Implementation and acceptance

The existing Tree Spirit, Mutant Wolf and shared BiomeBoss AI each own one small transient controller. Existing attack enums, saved IDs and animation arrays are unchanged. This pass uses native themed particles and the existing boss models, not bespoke awakened models or new skeletal animations. Existing glint trophies and laurels supply the cosmetic reward; no new gear tier is added.

Fresh Testing Camp guides and the Awakening Seal tooltip explain the three-strike mechanic. Existing camp books are not rewritten.

Automated checks cover distinct bounded patterns, safe positions, exact damage timing across all 16 families, late arrivals, abandonment, normal-fight exclusion, saved identity, reload cancellation, actual AI scheduling and phase cancellation. Live survival balance, terrain visibility, vertical arenas, accessibility at low particle settings, latency and two-client dodge readability remain playtest work. These initial timings and damage values are playtest defaults.

Verification passed: common/client compilation, all 33 boss unit tests, six awakened-related server checks (including existing seal/reward compatibility), all 18 existing boss encounter tests, and all three Testing Camp tests. The final server total is 27 passing targeted checks. Logs: `build/awakened-final-checks.log`, `build/awakened-boss-regressions.log`, `build/awakened-camp-checks.log`. This is not unfiltered fullCheck or live gameplay acceptance. No JAR, release version change or push was performed.
