# Griffin

Open `Griffin.bbmodel` in Blockbench (Free Model format). It embeds the mod's original 128px texture and includes idle and flight reference clips.

The runtime uses `GriffinModel.java`, not GeckoLib. The project is an editable authoring snapshot; saving it alone does not update the game. Do not use the generic `rig.cjs` exporter for this mount. Transfer geometry changes into `tools/assets/griffin-model.cjs`, then run `node tools/assets/griffin-model.cjs --write`. This intentionally replaces the snapshot and Java geometry, preserving the Java animation controller. Preserve hand edits before regeneration. Animation changes must be applied to the Java controller and reference clips together.

Run `node tools/assets/griffin-model.cjs --check` for geometry/project/embedded-texture parity, unique cube IDs, and atlas bounds. Preview at `http://127.0.0.1:8766/?id=griffin` using the existing asset server. Flight reference clips hold the tail still to loop one wing cycle; the game retains independent tail motion and head tracking.

Polish adds jointed wings, a flank-folded ground pose, delayed wingtip strokes, staggered primaries, a hooked beak, brows, feathered cuffs, individual dark talons, and shaped lion hocks. Gameplay dimensions and the original atlas are retained. Source preview and model baking do not replace an in-game rider/terrain check.
