"""Validate all crop stages, native model limits, UVs and editable project texture parity."""
import base64
import json
import math
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[2]
assets = root / "src/main/resources/assets/darkspawn"
count = 0
for crop in json.loads(Path(__file__).with_name("crop-art.json").read_text()):
    crop_id = crop["id"]
    texture = assets / f"textures/block/{crop_id}_crop.png"
    with Image.open(texture) as image:
        assert image.size == (128,128), crop_id
    parts = json.loads((assets / f"blockstates/{crop_id}_crop.json").read_text())["multipart"]
    assert len(parts) == 8 and {int(p["when"]["age"]) for p in parts} == set(range(8)), crop_id
    stages = set()
    for part in parts:
        age = int(part["when"]["age"])
        model_id = part["apply"]["model"]
        assert model_id.startswith("darkspawn:block/"), model_id
        model = json.loads((assets / f"models/{model_id.split(':')[1]}.json").read_text())
        assert model["textures"]["atlas"] == f"darkspawn:block/{crop_id}_crop"
        assert len(model["elements"]) <= 40, model_id
        stages.add(json.dumps(model["elements"],sort_keys=True))
        title = crop_id.capitalize()+("Ripe" if age==7 else f"Stage{age}")
        project = json.loads((root / f"art/blockbench/{crop_id}_crop/{title}.bbmodel").read_text())
        assert base64.b64decode(project["textures"][0]["source"].split(",")[1]) == texture.read_bytes(), title
        assert len(model["elements"]) == len(project["elements"]), title
        for element, editable in zip(model["elements"],project["elements"]):
            assert element["from"] == editable["from"] and element["to"] == editable["to"], title
            for lo,hi in zip(element["from"],element["to"]):
                assert math.isfinite(lo) and math.isfinite(hi) and -16 <= lo < hi <= 32, (model_id,element["name"])
            if "rotation" in element:
                assert element["rotation"]["angle"] in (-45,-22.5,0,22.5,45), model_id
            for face, definition in element["faces"].items():
                assert definition["texture"] == "#atlas", model_id
                assert all(0 <= v <= 16 for v in definition["uv"]), model_id
                assert editable["faces"][face]["uv"] == [v*8 for v in definition["uv"]], title
        count += 1
    assert len(stages) == 8, crop_id
station = json.loads((assets / "models/block/cooking_station.json").read_text())
assert station["elements"][0]["from"] == [0,0,0] and station["elements"][0]["to"] == [16,16,16]
assert len(station["elements"][0]["faces"]) == 6
print(f"PASS: {count} distinct crop stages; bounded geometry, UVs, project parity; station remains a full cube")
