# Ascension Guardian presentation

October 20, 2026. First presentation pass in source; no release JAR. New encounters use a dedicated `darkspawn:ascension_guardian` entity with synchronized chapter identity and warning countdown. Combat still inherits the native golem behavior and retains the existing team restrictions, health, strike geometry, five-minute timeout, restart cleanup and native golem loot.

## Models and warnings

All nine Guardians share an articulated construct body with separate chapter silhouettes and native block materials:

| Guardian | Silhouette | Material |
| --- | --- | --- |
| Cave | Broad stone shoulder blocks | Stone |
| Storm | Conductor arch | Oxidized copper |
| Ember | Paired furnace stacks | Netherrack |
| Frost | Three tall spires | Packed ice |
| Tidal | Side fins and dorsal ridge | Prismarine bricks |
| Crystal | Five staggered crystal spires | Amethyst |
| End | Rectangular portal frame | Purpur |
| Astral | Broad forge shoulders and backplate | Polished deepslate |
| Summit | Three-point crown | Quartz |

The model raises both arms and warms its tint during the synchronized ground warning. Ground particles, action-bar instructions and the boss health bar remain the primary combat cues. Decorations do not enlarge collision or attack reach. Native iron golems outside these encounters retain their normal renderer.

These are custom cuboid silhouettes using existing Minecraft materials, not newly painted skins. A live client still needs to establish texture mapping, lighting, animation readability and whether the silhouettes need stronger differences.

## Music and lifecycle

October 21 original audio pass: all nine Guardians now use one original Ascension battle motif with distinct arrangements and three intensities each (**27 stereo Ogg tracks**). The 24-second, eight-bar phrase runs at 80 BPM in D minor. Melody and harmonic progression connect the encounters; register, synthesis color, pulse and stereo width distinguish them.

| Guardian | Arrangement |
| --- | --- |
| Cave | Low, rounded resonance and sparse pulse |
| Storm | Higher metallic modulation and rapid color movement |
| Ember | Warm midrange attack with a three-part pulse |
| Frost | High, restrained bell-like tone and slower movement |
| Tidal | Longer decays, broad stereo color and a rolling pulse |
| Crystal | Highest register with bright modulated attacks |
| End | Lowest register with darker modulation |
| Astral | Strong metallic modulation and a measured pulse |
| Summit | Upper register with an additional sustained fifth |

Phase two adds quiet offbeat notes; phase three doubles the low percussion rate. Health thresholds remain two-thirds and one-third. These original synthesized tracks replace only Guardian score selection; normal bosses retain their existing music.

The existing encounter controller retains crossfades, two-score overlap cap, Music-volume controls, warning ducking, distance limits and world-change cleanup. Proximity determines listening, not combat participation. No gameplay or warning timing changed.

Author with `tools/audio/generate-ascension.py`; it regenerates only the 27 Ascension tracks and their `music.ascension_*` events, preserving other manifest entries. The general creature generator also preserves these separately authored events. The existing audio workshop now lists all nine Guardians and all three intensities. Original painted skins remain separate future art work.

Chapter identity survives native entity serialization. Warning countdowns deliberately reset on load because interrupted Guardian encounters remain free to retry, rather than resuming an old strike. Previously saved tagged iron-golem actors retain the existing recovery behavior; newly started encounters use the new type.

## Acceptance

Targeted checks cover model baking, chapter visibility and warning-pose reset, score-event coverage, server chapter identity, serialization, intensity transitions and the existing Ascension encounter/protection suite. Native texture references are checked against the local Minecraft asset cache.

Common/client compilation, 35 unit/resource checks and all 50 Ascension server tests passed. An initial recovery-test summon failure did not reproduce in the isolated nine-test group or the subsequent full Ascension run; the test now reports the direct refusal message if it recurs. Logs: `build/ascension-presentation-checks.log`, `build/ascension-presentation-recovery.log`, `build/ascension-presentation-server-final.log`.

Live checks remain: all nine appearances in normal and low light, warning pose versus actual strike timing, sound ducking and crossfades, death/retry, travel/disconnect, pause/mute/resource reload and two-client observation.

October 21 audio verification: generation decoded every track and checked sample rate, stereo channels, exact duration, finite samples, non-clipping peaks, quiet loop endpoints and 27 distinct file hashes. Common/client compilation and all four focused audio/Guardian tests passed (`build/ascension-music-checks.log`). A workshop selection check resolved all 27 Guardian/intensity combinations to their local audio files. Live listening, perceived loop seams, mix balance and transitions remain acceptance checks; numerical audio checks do not establish listening quality.
