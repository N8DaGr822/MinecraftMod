# Darkspawn v2 upload preparation

Release: **2.0.0-alpha.1** ? **Darkspawn v2 - Cuisine, Landmarks and Atmosphere**.
This package is prepared locally; no Modrinth version has been uploaded or published.

## Upload settings

| Field | Value |
| --- | --- |
| Version number | 2.0.0-alpha.1 |
| Version title | Darkspawn v2 - Cuisine, Landmarks and Atmosphere |
| Channel | Alpha |
| Minecraft | 26.3 |
| Loader | Fabric |
| Client / server | Required on both |
| Java | 25 |
| Required dependencies | Fabric API 0.162.0+26.3 and GeckoLib 5.5.7 for Fabric / Minecraft 26.3 |
| Tested Fabric Loader | 0.19.5 |
| Upload file | build/modrinth/2.0.0-alpha.1/darkspawn-2.0.0-alpha.1.jar |
| License | CC0-1.0 |
| Source | https://github.com/N8DaGr822/MinecraftMod |

Add this as a new version of the existing Darkspawn project if it is already created. Upload the named JAR only, not the sources JAR or the whole folder. Add both dependencies as required in the version settings. Copy the changelog below into the version description. Retain the intended unlisted family-playtest visibility where available.

## Version changelog ? copy into Modrinth

Darkspawn v2 expands cooking, exploration, boss rewards, and atmosphere for Minecraft 26.3 / Fabric.

- Adds 44 meals, six shareable feasts, 16 boss ingredients, and 16 decorative boss trophies.
- Adds Chef villagers and kitchen trades, a Cookbook, discoverable recipe scrolls, dietary-variety bonuses, and progression advancements.
- Adds Frost Garlic, Jungle Pepper, and Marsh Rice variants, with regional recipes and more discovery sources.
- Adds ten shield, bow, and armor empowerments alongside the existing melee powers.
- Adds naturally generated Overgrown Druid Shrines and Abandoned Hunter Camps, each with three variants, ritual guides, and modest supply chests. The other 14 boss landmarks remain planned.
- Adds dedicated cuisine and trophy artwork, updated creature animations, the redesigned Shadow Creeper Queen family, regional atmosphere, creature voices, and phase-based boss music.
- Retains the existing 16 player-summoned boss families, permanent boss hearts, inventory upgrades, and farming systems.

### Installing or updating

Install Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, GeckoLib 5.5.7 for Fabric / Minecraft 26.3, and Java 25. Install the same Darkspawn version and dependencies on every client and the server. Replace the old Darkspawn JAR; do not keep two versions installed.

Back up existing worlds before updating this alpha. New shrines and camps appear only in newly generated terrain. Bosses still require player-crafted summons and the world's first Ender Dragon victory. Structures can be explored before that victory. Existing portable summon rituals continue to work.

### Known limitations

This remains a playtest alpha. Combat balance, multiplayer progression, structure terrain seams, animation readability, and the final in-game audio mix need live testing. Automated encounter tests have previously shown intermittent isolation failures; see the verification record shipped with the package. This release does not claim those test failures or the previously reported Tree Spirit balance issues are fixed.

### AI disclosure

Generative AI assisted development of code, artwork, and release text. Apply the corresponding Modrinth AI disclosures. Human direction and playtesting remain part of development.

## Project-page artwork and eligibility

The bundled generated icon is not a project-page upload asset. Use the user's original independently created icon when available. Modrinth's current rules prohibit AI-generated project-page images and public publication of projects primarily or entirely made from AI output; disclosure alone does not establish eligibility. Review the project's provenance and intended visibility before submission: https://modrinth.com/legal/rules (checked October 9, 2026). This preparation does not claim moderation approval or upload an icon.

## Verification

The v2 full build passed on October 9, 2026: **63 unit tests and all 85 server GameTests**. Packaged size: **33,890,739 bytes**. SHA-256: `15b97fa15cfaef008f599dee0e733beb1d9339b9bd6254674d52aac17fa74029`. Archive integrity, all six landmark templates, both structure registrations/loot tables, 639 sound event references, and 126 cuisine model checks passed. Live-client and multiplayer checks remain open.

See `build/modrinth/2.0.0-alpha.1/VERIFICATION.txt` and `SHA256SUMS.txt` for the exact packaged artifact and this run's results. Historical alpha notes are in [modrinth-alpha-history.md](modrinth-alpha-history.md).
