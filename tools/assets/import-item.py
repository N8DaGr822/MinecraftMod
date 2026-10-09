"""Import one reviewed AI sprite without changing its item's behavior or definition."""
import base64
import json
import runpy
import shutil
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/darkspawn"
prepare = runpy.run_path(str(Path(__file__).with_name("prepare-texture.py")))["prepare"]
item_id, source, prompt64 = sys.argv[1:]
catalog = json.loads(Path(__file__).with_name("item-art.json").read_text())
assert item_id in {entry["id"] for entry in catalog}, item_id
source = Path(source)
model_path = ASSETS / f"models/item/{item_id}.json"
model = json.loads(model_path.read_text())
assert model["parent"] == "minecraft:item/generated", item_id
with Image.open(source) as image:
    alpha = image.convert("RGBA").getchannel("A")
    assert alpha.getextrema()[0] == 0 and alpha.getextrema()[1] == 255, f"Missing transparency: {item_id}"
destination = ASSETS / f"textures/item/{item_id}.png"
prepare(source, destination, 32)
model["textures"]["layer0"] = f"darkspawn:item/{item_id}"
model_path.write_text(json.dumps(model, indent=2) + "\n")
authoring = ROOT / "art/items"
authoring.mkdir(parents=True, exist_ok=True)
(authoring / f"{item_id}.json").write_text(json.dumps({
    "id": item_id, "generator": "built-in image_gen", "date": "2026-10-09",
    "source": source.name, "prompt": base64.b64decode(prompt64).decode(),
    "texture": f"src/main/resources/assets/darkspawn/textures/item/{item_id}.png",
    "packaging": "32x32 RGBA, Pillow BOX resize, original alpha preserved"
}, indent=2) + "\n")
cache = ROOT / "build/art-source"
cache.mkdir(parents=True, exist_ok=True)
shutil.copyfile(source, cache / f"{item_id}.png")
print(f"Imported {item_id}; recipe, registration and item definition unchanged")
