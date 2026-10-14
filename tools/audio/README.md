# Darkspawn audio

Original synthesized audio follows the selected direction: dark atmosphere with increasing boss-phase intensity. Six theme groups (nature, mutated, arcane, hive, Nether, and void) provide 18 thirty-second stereo scores. All 16 bosses select a theme at phases 1–3. Regional mono voices provide two variations each for idle, hurt, death, warning, phase, and distant ambience; individual creature events vary pitch with registered size. The complete pass contains 210 Ogg files, about 7.9 MB, with 639 registered events and 591 English subtitles.

The hive uses breathy hisses, short chitin-like clicks, and a low resonant voice. Nature and stone use low creaks; frost, storm, and void use higher resonances; aquatic sounds pulse beneath the surface. Music uses low fifths, sparse minor motifs, filtered air, and restrained percussion. These are synthesized production assets and remain editable through the generator and normal resource-pack overrides.

## Runtime behavior

- Idle, hurt, and death sounds use Minecraft's entity hooks for every registered living creature. Idle intervals are 11–12 seconds before Minecraft's normal scheduling variation, with quieter ordinary mobs.
- Mob warning cues play once when a synchronized warning begins. Boss warning and phase sounds replace the earlier generic cues at their existing server transitions. Existing impact, footstep, and collapse effects can still use vanilla sounds.
- Regional atmosphere retains its existing post-dragon eligibility, per-player rate limits, and Ambient volume category.
- The client selects the nearest living boss within 64 blocks and retains it until it dies, unloads, or moves beyond 80 blocks. This includes creative auditioning; spectators and dead players do not select encounters. Proximity selects music, not combat participation.
- Phase changes crossfade between streamed scores. The mix gain rises from .36 to .46 to .56 across phases, accompanying the denser percussion. Fade-in is approximately three to four-and-a-half seconds; fade-out takes at most two-and-a-quarter seconds. At most two score instances coexist. Dimension change/disconnect stops the old world's scores immediately. Pause follows the game; muted/reloaded channels can recover when audible. Music volume controls these scores; entity sounds use Hostile Creatures.
- During the selected boss's synchronized warning, the encounter score ducks to 45% of its normal gain over four ticks and recovers over fourteen ticks after the warning. Crossfade envelopes continue independently; no other audio category or user volume setting is changed. The nine mounts use quiet native animal/material cues, separate from the 115-creature custom audio catalog.
- Vanilla background music is suspended while an encounter score or its fade is active. Normal music scheduling resumes afterward. Jukebox records are not controlled by this feature.

## Author and audition

`node tools/audio/serve.cjs` opens a local-only workshop at `http://127.0.0.1:8767/`. Set `DARKSPAWN_AUDIO_PORT` to use a different port. It has boss, intensity, creature, cue, and variation controls; playback is manual. The player applies the event's base pitch, while Minecraft adds its own pitch variation, attenuation, and category volume. The server supports byte ranges so Ogg durations and seeking work correctly.

`tools/audio/generate.py` requires NumPy and SoundFile. The local workflow installs SoundFile/CFFI/pycparser under the ignored `build/audio-python` directory and uses the bundled Python with NumPy. Run from the repository root. `roster.json` is the captured authoring catalog; refresh it from `require('../assets/catalog.cjs')` when the entity roster changes. The generator writes the Ogg files, `sounds.json`, subtitle additions in `en_us.json`, and an `analysis.json` decode/peak report. Regeneration replaces authored audio for this pass.

The generator checks every encoded file by decoding it, verifies frame counts and finite samples, and rejects peaks reaching full scale. Bounded encoder writes avoid Windows native conversion stack limits. Music has stereo channels and stream flags; positional creature sounds are mono.

## Verification and listening

`AudioTest` checks registry coverage, every referenced Ogg resource, captions, all 115 creature cue sets, all 16 three-phase boss scores, and warning countdown behavior. `tools/audio/verify.init.gradle` isolates Gradle outputs from other work in the shared repository; use `--project-cache-dir build/audio-gradle-cache --no-configuration-cache` with it.

Compilation, resource validation, and browser decoding do not establish final mix quality. An in-game listening pass should check warning audibility beneath phase-three music, simultaneous mob voices, boss transitions and defeat, the 64/80-block boundary, pause, music/master mute and unmute, resource reload, dimension travel, and two-client encounters. Fine-tune levels and timbre from that listening pass.

Verification on October 9, 2026: all 63 unit tests passed in the isolated build (zero failures/errors). The final phase-gain adjustment compiled and packaged successfully in build/audio-verification/libs/darkspawn-2.0.0-alpha.1.jar. All 210 Ogg files decoded with finite samples; maximum measured encoded peak was 0.8503. The browser decoded the music and creature samples, and byte-range playback exposed the full 30-second music duration. No in-game listening or multiplayer audio playtest is claimed.

## Imported shanties

The four user-supplied MP3 recordings are shipped as mono 48 kHz Ogg Vorbis under `src/main/resources/assets/darkspawn/sounds/shanties`. `shanties.json` records source/output SHA-256, lengths and decoded peaks. Conversion used soundfile/libsndfile: average stereo channels, reduce gain only when the mono peak exceeds 0.89, write Vorbis in 8,192-frame blocks, then verify frame count, channel count, format and decoded peak below 1. Original MP3s remain in the user's Downloads folder. The creature-audio generator preserves these manifest entries and does not regenerate the recordings. Nell's client playlist uses Records volume, follows the ship and never plays more than one nearby singer per listener.
