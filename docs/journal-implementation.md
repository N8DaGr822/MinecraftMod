# Darkspawn Journal

The free Journal button is available from the Survival inventory and the expanded Creative storage screen. It opens a read-only, non-pausing screen with Guide, Wildlife, Mounts, Bosses, Cooking, Forsaken and Sites tabs. Search matches the displayed title and text in the selected section. List and detail pages have separate navigation. Refresh requests current progress; it does not award discoveries or consume items.

## Discovery and existing saves

- Each player's newly observed species and visited landmark families are saved separately and survive death, reconnection and world saves. Old saves start with an empty observation record, without deleting or respawning anything.
- Once every two seconds, staggered per player, the server checks living catalog creatures within a three-dimensional 24-block radius and line of sight. Spectators and dead players do not discover entries. At most 128 nearby catalog candidates receive sight checks.
- Native landmark references in the player's current loaded chunk are checked against already-loaded structure starts. Entering a valid start's bounds discovers that family. Templates placed without a native structure start do not count as landmark visits. No locating, chunk loading, terrain edits or container reads are performed.
- Visiting a boss landmark unlocks that boss's ritual clues. Consumed boss hearts and existing credited-kill statistics also reveal the boss entry. Credited kills and consumed hearts remain separate; historical shared participation is not reconstructed or falsely awarded.
- The existing cookbook's recipe knowledge and Forsaken research remain the source of truth. Journal snapshots do not create duplicate recipe, heart or research state. Research reveals Forsaken guidance; infected portraits still require observing the species or an existing credited kill.
- Knowledge follows the player across OneBlock inventory profiles; it never moves possessions or becomes another player's discovery.

## Content and presentation

The catalog contains all 82 regional creatures, 14 mounts, 16 bosses, four Forsaken infected, 17 landmark families and six guidance entries. Cooking entries are generated from loaded Cooking Station recipes, currently 59. Locked dishes retain their discovery hint without exposing the ingredients. Creature entries provide habitat, behavior and rewards; mount entries include bonding items, travel controls, care and implemented combat abilities.

Undiscovered entity entries show habitat hints and dark silhouettes. Discovery reveals the name, body text and illustrated portrait. The 116 portrait pairs derive from existing cuboid models and textures; they are static field illustrations rather than live model renders. The first pass is English, consistent with the existing cookbook. Actual in-game GUI scaling, narration and visual feel remain live acceptance work.

The screen takes a server snapshot on opening and on Refresh. Replies within one second reuse the previous snapshot so rapid reopening does not get stuck loading. Replies are applied only while a Journal screen is open. Network entry counts and text lengths are bounded. Search and pagination are client-local; no packet can claim an observation or grant a reward.

## Authoring and checks

`tools/assets/journal-content.py` derives the catalog from existing documented rosters, behaviors and source registrations. `tools/assets/journal-portraits.py` renders portraits and verifies matching silhouette masks. Source data is bundled in `data/darkspawn/journal/catalog.json`; it is an authored catalog, not a runtime datapack extension API.

Compilation, all three Journal unit tests, four Journal server tests, nine OneBlock regression tests and all 116 portrait-mask validations passed. Focused checks cover registered roster and portrait coverage, deduplication and stale saved IDs, packet round trips and bounds, old recipe/heart/research migration, save/load and death copying, personal ownership, observation sight/range, loaded-only landmark discovery and OneBlock profile transfers. See `build/journal-tests.log` and `build/journal-oneblock-tests.log` for the current results.

Map markers, structure compasses, discovery coordinates and archaeology remain separate exploration increments. No JAR is packaged by this pass.
