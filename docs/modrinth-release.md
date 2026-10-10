# Darkspawn alpha release preparation

**2026-10-09 update:** the workspace now targets `1.0.0-alpha.3`, adding naturally generated Druid Shrines and Hunter Camps to the alpha.2 cuisine, progression, art, crop, and equipment expansion. The alpha.1 verification and upload text below remain a historical record; they do not validate or describe the new artifact. Use the current README and roadmap for alpha.3 features and verification. No alpha.3 publication has been performed by this change.

The subsequent alpha.2 art/regional pass adds 44 bespoke meal models, six feast families, 16 sculpted trophies, three regional crops, additional equipment/discovery sources, and a new bundled icon. Current validation is in [the roadmap](development-roadmap.md). The new icon is AI-generated and is **not** a Modrinth project-page upload asset under [section 6.2](https://modrinth.com/legal/rules); use an independently created icon for that purpose. Artwork provenance and prompts are in [the art record](../art/cuisine-art-provenance.md).

Release candidate: **1.0.0-alpha.1**. This document is upload preparation, not evidence that a Modrinth project or version has been published.

## Verification record (2026-10-08)

- Final `gradlew.bat build --console=plain --max-workers=2` passed with **58 unit tests and 69 server GameTests** using JDK 25 and the documented Windows socket-directory workaround.
- The first run failed the zombie no-player attack-pose assertion and the Tree Spirit death-drop assertion. A rerun with additional assertion diagnostics passed without gameplay changes. Intermittent fixture isolation/entity-loading behavior remains to be investigated; the rerun is not evidence that a gameplay bug was fixed.
- Inspected the final JAR: mod ID `darkspawn`, version `1.0.0-alpha.1`, author `nathen.lentz`, and expected dependency metadata.
- SHA-256: `AE3B43D11D078E76B7AB61A5D8DB880042806A80504084618DDD85C757AA4CD7`.
- Build log: `build/alpha-build-recheck.log`. Test reports: `build/reports/tests/test/index.html`.
- Publication remains pending website sign-in and submission. Browser automation was unavailable in this session; no project, version, or moderation request was created.

## Project settings

| Field | Value |
| --- | --- |
| Project name | Darkspawn |
| Author / Modrinth account | nathen.lentz |
| Project type | Mod |
| Short description | Post-dragon biome bosses, regional creatures, cooking, farming, inventory upgrades, and permanent heart progression. Early playtest alpha. |
| Version name | Darkspawn 1.0.0-alpha.1 - Family Playtest |
| Version number | 1.0.0-alpha.1 |
| Release channel | Alpha |
| Game version | Minecraft Java 26.3 only |
| Loader | Fabric; tested target 0.19.5 |
| Client / server | Required on both |
| Required dependencies | Fabric API 0.162.0+26.3; GeckoLib 5.5.7 for Fabric / Minecraft 26.3 |
| Java | 25 or later; JDK 25 for building |
| License | Existing repository license: CC0-1.0 |
| Source URL | https://github.com/N8DaGr822/MinecraftMod |
| Suggested categories | Adventure, Mobs, Food, Equipment, Utility (select those available) |
| Requested visibility | Unlisted for family testing, subject to moderation |
| Main upload | build/libs/darkspawn-1.0.0-alpha.1.jar |

The old local `1.0.0` development build may remain in `build/libs`. Upload only the alpha JAR above, not the old file or the sources JAR. Remove the old Darkspawn JAR from a test instance before installing the alpha; do not install both.

## Project description (copy into Modrinth)

Darkspawn expands Minecraft's post-dragon game with 16 player-summoned biome bosses, regional creatures and discoveries, permanent boss-heart progression, and boss-powered melee weapons. Cooking, farming, and inventory upgrades support the journey before and after the dragon fight.

This is an early playtest alpha. Combat balance, animation polish, and multiplayer verification are ongoing.

### Included now

- 16 boss families with biome-specific rituals, phases, rewards, and animated models.
- 82 ecosystem creatures/discoveries across the boss regions, plus the Endborn.
- Permanent boss-heart progression: up to 20 normal hearts and five earned absorption hearts.
- 16 melee weapon empowerment powers applied at a smithing table.
- Inventory sorting and three permanent upgrades, reaching 54 main inventory slots plus the hotbar.
- A Cooking Station with six meals, ingredient processing, and one active meal buff at a time.
- Onion, garlic, tomato, rice, corn, and pepper crops, farmer seed trades, and butter.

### Installation

Use Minecraft Java 26.3 with Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, GeckoLib 5.5.7, and Java 25. Install Darkspawn, Fabric API, and GeckoLib on every client and on the server. GeckoLib is a separate required download.

Start with a disposable test world or a backup copy. The world's first Ender Dragon defeat unlocks natural regional creature spawning and boss rituals. Inventory, farming, and cooking work before that. Bosses are summoned by players; they do not spawn spontaneously.

### Known limitations and testing priorities

- The Ancient Tree Spirit was reported too easy to defeat by repeatedly attacking at close range. A balance pass is pending.
- Boss animations and warning readability need further live review.
- Heart reward receipt and persistence were not conclusively verified in the first live session, which involved Creative/Survival switching. Permanent absorbed hearts should survive game-mode changes.
- Remaining bosses, regional spawn density, and real multiplayer encounters still need broader testing.
- Some item, crop, and cooking visuals reuse vanilla artwork. More dishes, feasts, a cookbook, and ranged/armor empowerment are planned, not included.

Please report the mod version, biome, game mode, difficulty, equipment and active effects, reproduction steps, and expected/actual behavior. Screenshots or short videos are particularly helpful for animation and attack problems.

### AI disclosure

Generative AI has been used in development, including code and creature assets/textures, and in preparing this project description and release text. Human direction, in-game testing, and further animation/balance work are part of the development process. Enable Modrinth's applicable AI code, assets, and text disclosures as well as including this notice.

## Version changelog (copy into Modrinth)

First packaged playtest alpha of Darkspawn for Minecraft 26.3 / Fabric.

- Includes the current boss roster, ecosystems, boss hearts, melee empowerment, inventory upgrades/sorting, Cooking Station, six meals, and six crops.
- Initial live feedback reports working inventory, cooking/food, and Tree Spirit weapon empowerment.
- Marks the build as an alpha and replaces Fabric example-project metadata with the actual source repository.
- Known issues: Tree Spirit combat is too easy in the reported test; animations need refinement; hearts and multiplayer progression still need conclusive live tests.
- This release packaging does not change combat balance or claim the reported heart uncertainty is fixed.
- Verification: 58 unit tests and 69 server GameTests passed on the final run. Two server assertions failed on the first run and passed on rerun without gameplay changes; intermittent test behavior remains under investigation.

## Submission checklist

- [x] Account identified from the user's profile screenshot: `nathen.lentz`, with zero existing projects.
- [ ] Sign in to Modrinth.com as `nathen.lentz` and create the Darkspawn mod project.
- [x] User selected unlisted for family testing.
- [x] Author display name set to `nathen.lentz`. The login email is not included in release files.
- [ ] Paste the description and select categories, CC0-1.0, Fabric, and client/server requirements.
- [ ] Upload the alpha JAR; select Minecraft 26.3 and the Alpha channel.
- [ ] Add Fabric API and GeckoLib as **required** dependencies, using compatible versions.
- [ ] Complete AI code/assets/text disclosures accurately.
- [ ] Use genuine in-game screenshots for the gallery. Do not upload AI-generated promotional images or icons. An icon can be omitted; do not reuse the template icon as final branding.
- [ ] Review the current Modrinth rules, submit for moderation, and set the intended visibility when available. Unlisted means accessible by link, not private access limited to invited family.
- [ ] Once approved/available, install from the project link in a clean Modrinth instance and verify both dependencies are installed.
- [ ] Give brothers the project link and use matching versions on all clients/server. Publishing the mod does not host a multiplayer server.

Modrinth requires disclosure of generated code, assets, and publishing text. Its current guidance says projects made primarily through AI are generally restricted from public discovery, but can often be unlisted; moderation determines eligibility. Sources checked 2026-10-08: [AI usage](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai), [content disclosures](https://support.modrinth.com/en/articles/16567675-content-disclosures), [content rules](https://modrinth.com/legal/rules).

## Focused family playtest

- [ ] Tree Spirit: fight in Survival with recorded equipment and no command-granted Resistance/Strength; compare close-range and ranged pressure.
- [ ] Hearts: receive a participant reward, absorb it in Survival, switch Creative then Survival, reconnect, and die; record any loss. Creative hides the health HUD, so inspect again in Survival.
- [ ] Two players: both deal damage and stay nearby at victory; verify independent hearts and correct health scaling.
- [ ] Repeat kill: an absorbed heart cannot increase permanent health again.
- [ ] Weapon empowerment: preserve a named/enchanted/damaged weapon; replace its power and check only one remains.
- [ ] Cooking: concurrent station users, full inventories, meal replacement, potion coexistence, and returned containers.
- [ ] Inventory: sorting and extra-slot items across death, reconnect, and dimension changes.
- [ ] Remaining bosses/ecosystems: visuals, natural spawns, movement, warned attacks, saved phases, and abandonment cleanup.
