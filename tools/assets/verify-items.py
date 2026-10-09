"""Check the item art's resource wiring, dimensions, alpha and authoring coverage."""
import json
import sys
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[2]
assets = root / "src/main/resources/assets/darkspawn"
catalog = json.loads(Path(__file__).with_name("item-art.json").read_text())
ids = sys.argv[1:] or [entry["id"] for entry in catalog]
assert len(catalog) == len({entry["id"] for entry in catalog})
assert {entry["id"] for entry in catalog} == {p.stem for p in (assets / "models/item").glob("*.json")}
for item_id in ids:
    definition = json.loads((assets / f"items/{item_id}.json").read_text())
    assert definition["model"]["model"] == f"darkspawn:item/{item_id}", item_id
    model = json.loads((assets / f"models/item/{item_id}.json").read_text())
    assert model["parent"] == "minecraft:item/generated", item_id
    assert model["textures"]["layer0"] == f"darkspawn:item/{item_id}", item_id
    with Image.open(assets / f"textures/item/{item_id}.png") as image:
        assert image.size == (32, 32) and image.mode == "RGBA", (item_id, image.size, image.mode)
        alpha = image.getchannel("A")
        assert alpha.getextrema() == (0, 255), item_id
        # Inventory Safety: Leave some true background and enough visible pixels to read.
        visible = sum(value >= 128 for value in alpha.getdata())
        assert 80 <= visible < 950, (item_id, visible)
    provenance = json.loads((root / f"art/items/{item_id}.json").read_text())
    assert provenance["id"] == item_id and provenance["prompt"], item_id
print(f"PASS: {len(ids)} item sprites; all catalog IDs match existing item models")
