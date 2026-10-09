"""Package a crop's four-material atlas and create its editable growth-stage models."""
import base64
import json
import runpy
import shutil
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[2]
crop_id, source, prompt64 = sys.argv[1:]
assert crop_id in {entry["id"] for entry in json.loads(Path(__file__).with_name("crop-art.json").read_text())}
source = Path(source)
prepare = runpy.run_path(str(Path(__file__).with_name("prepare-texture.py")))["prepare"]
prepare(source, root / f"src/main/resources/assets/darkspawn/textures/block/{crop_id}_crop.png", 128)
subprocess.run(["node", "tools/assets/crop-models.cjs", crop_id], cwd=root, check=True)
folder = root / f"art/blockbench/{crop_id}_crop"
(folder / "texture-prompt.txt").write_text(
    "Built-in image_gen; 2026-10-09. 128x128 atlas; four 64x64 material tiles.\n"
    + f"Source: {source.name}\n" + base64.b64decode(prompt64).decode() + "\n")
shutil.copyfile(source, root / f"build/art-source/{crop_id}_crop.png")
