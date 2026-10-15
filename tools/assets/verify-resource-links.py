"""Read-only resource reference audit, including native Minecraft model dependencies."""
import json
import zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/darkspawn"
native = set()
for jar in (ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*.jar"):
    if "sources" not in jar.name:
        with zipfile.ZipFile(jar) as archive:
            native.update(archive.namelist())
assert "assets/minecraft/models/item/bow.json" in native, "Compile to populate native assets first"
errors = []
references = 0

def check(identifier, folder, suffix, source):
    global references
    if identifier.startswith("#"):
        return
    namespace, name = identifier.split(":", 1) if ":" in identifier else ("minecraft", identifier)
    path = f"assets/{namespace}/{folder}/{name}{suffix}"
    references += 1
    if not (RES / path).is_file() and path not in native:
        errors.append(f"{source}: missing {path}")

def models(value, source):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"model", "parent"} and isinstance(child, str):
                if child not in {"builtin/generated", "builtin/entity", "minecraft:builtin/generated", "minecraft:builtin/entity"}:
                    check(child, "models", ".json", source)
            elif key == "textures" and isinstance(child, dict):
                for texture in child.values():
                    if isinstance(texture, str):
                        check(texture, "textures", ".png", source)
            else:
                models(child, source)
    elif isinstance(value, list):
        for child in value:
            models(child, source)

files = list(ASSETS.rglob("*.json"))
for path in files:
    value = json.loads(path.read_text(encoding="utf-8"))
    if path.relative_to(ASSETS).parts[0] in {"items", "models", "blockstates"}:
        models(value, path.relative_to(ROOT))
lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
sounds = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
for event, definition in sounds.items():
    subtitle = definition.get("subtitle")
    if subtitle and subtitle not in lang:
        errors.append(f"{event}: missing subtitle {subtitle}")
    for sound in definition["sounds"]:
        if isinstance(sound, dict) and sound.get("type") == "event":
            name = sound["name"]
            if name.startswith("darkspawn:") and name.split(":", 1)[1] not in sounds:
                errors.append(f"{event}: missing sound event {name}")
        else:
            check(sound if isinstance(sound, str) else sound["name"], "sounds", ".ogg", event)
textures = list(ASSETS.rglob("*.png"))
for path in textures:
    with Image.open(path) as image:
        image.verify()
assert not errors, "\n".join(errors)
print(f"PASS: {len(files)} asset JSON files, {references} model/texture/sound links, "
      f"{len(sounds)} sound events/subtitles, {len(textures)} readable PNGs")
