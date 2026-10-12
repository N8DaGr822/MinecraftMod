"""Import one reviewed AI sprite without changing its item's behavior or definition."""
import base64
import json
import re
import runpy
import shutil
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/darkspawn"
prepare = runpy.run_path(str(Path(__file__).with_name("prepare-texture.py")))["prepare"]
new_only = sys.argv[-1] == "--new-only"
item_id, source, prompt64 = sys.argv[1:-1] if new_only else sys.argv[1:]
catalog = json.loads(Path(__file__).with_name("item-art.json").read_text())
assert item_id in {entry["id"] for entry in catalog}, item_id
source = Path(source)
model_path = ASSETS / f"models/item/{item_id}.json"
model_text = model_path.read_text()
model = json.loads(model_text)
assert model["parent"] == "minecraft:item/generated", item_id
with Image.open(source) as image:
    alpha = image.convert("RGBA").getchannel("A")
    assert alpha.getextrema()[0] == 0 and alpha.getextrema()[1] == 255, f"Missing transparency: {item_id}"
destination = ASSETS / f"textures/item/{item_id}.png"
if new_only and destination.exists():
    # Shared Artwork: Preserve an icon created while a long image-generation batch was running.
    print(f"Skipped existing {item_id}; review the current asset before replacing it")
    sys.exit(0)
prepare(source, destination, 32, cutout=True)
model_text = re.sub(r'("layer0"\s*:\s*)"[^"]+"',
                    lambda match: match[1] + json.dumps(f"darkspawn:item/{item_id}"), model_text, count=1)
model_path.write_text(model_text)
authoring = ROOT / "art/items"
authoring.mkdir(parents=True, exist_ok=True)
(authoring / f"{item_id}.json").write_text(json.dumps({
    "id": item_id, "generator": "built-in image_gen", "date": "2026-10-09",
    "source": source.name, "prompt": base64.b64decode(prompt64).decode(),
    "texture": f"src/main/resources/assets/darkspawn/textures/item/{item_id}.png",
    "packaging": "32x32 RGBA, Pillow BOX resize, alpha cutout at 128; transparent background retained"
}, indent=2) + "\n")
cache = ROOT / "build/art-source"
cache.mkdir(parents=True, exist_ok=True)
shutil.copyfile(source, cache / f"{item_id}.png")
print(f"Imported {item_id}; recipe, registration and item definition unchanged")
