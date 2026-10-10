# Cuisine and project artwork

Created October 9, 2026 using the built-in image generation tool (not the CLI), plus repository-authored Minecraft Java model geometry.

- `art/source/cuisine_atlas.png` and `art/source/project-icon.png`: unchanged generated originals.
- `src/main/resources/assets/darkspawn/textures/block/cuisine_atlas.png`: opaque 4×4 material atlas, packaged at 512×512. Sixteen tiles: walnut, ceramic, bread, roast; tomato, cheese, herbs, rice; chorus, turquoise, bone, charcoal; ember, ice, gold, dark mushroom.
- `src/main/resources/assets/darkspawn/icon.png`: new tree-spirit emblem, packaged at 256×256 for the bundled mod icon. This is newly generated artwork, not the unavailable image previously supplied on another computer.
- `tools/assets/cuisine_art.py`: editable geometry source for 44 meals, 36 feast portion models, 16 distinct trophies, and three regional crops. Run `python tools/assets/cuisine.py` to regenerate the catalog and models together. No runtime generation or raster recoloring.
- `tools/assets/package-cuisine-art.ps1`: nearest-neighbor packaging from originals into power-of-two runtime sizes, preserving the tile layout and avoiding mipmap reduction from the original 1254-pixel dimensions.
- `tools/assets/preview-cuisine.cjs`: creates `build/art-review/cuisine.html`, an offline textured geometry gallery with depth-tested rendering. The gallery was rendered and visually inspected; it does not substitute for in-game lighting, GUI scale, and two-client review.

## Generation prompts

Atlas: “Create one production game texture atlas, square opaque image, for low-poly Minecraft fantasy cooking models. Exact 4 by 4 grid of 16 equal square material tiles, no gaps, no borders, no labels, no perspective, no objects, no text. Every tile is a flat evenly lit seamless-like pixel-art material with tiny square pixels, restrained noisy detail and no large highlights or directional shading. Row 1 left to right: dark walnut wood; pale cream glazed ceramic; golden toasted bread crust; dark roasted brown meat. Row 2: red tomato sauce; creamy golden cheese; leafy herb green; off-white cooked rice. Row 3: violet chorus fruit flesh; turquoise blue crystalline flesh; pale ivory bone; charcoal volcanic stone. Row 4: orange ember material; icy pale blue crystal; rich gold metal; dark purple mushroom flesh. Tile boundaries precisely at 25%, 50%, 75%. This is a single UV texture atlas, not a contact sheet of separate illustrations. Fill image edge to edge.”

Icon: “Square project icon for a Minecraft fantasy boss exploration mod named Darkspawn, NO TEXT and NO LETTERING. A bold crisp pixel-art emblem: ancient horned dark charcoal tree-spirit face with luminous amber eyes, a small violet crystal above forehead, angular branching antlers, emerald moss edges. Strong iconic silhouette, centered symmetrical composition, occupies 80 percent of canvas, dark midnight teal background with subtle square-shaped mist and thin antique gold inset border. Handcrafted retro game aesthetic using broad clean pixel clusters, limited palette, easily recognizable at 64 pixels. No Minecraft logo, no gradients, no tiny details, no watermark. Opaque background.”

The icon is bundled in the JAR. Do not use this generated icon for Modrinth promotional imagery under its [current project-page rules, section 6.2](https://modrinth.com/legal/rules), checked October 9, 2026; retain that distinction in release preparation. Nothing has been uploaded.
