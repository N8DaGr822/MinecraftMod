"""Validate culinary brewing links and item visuals for the endgame increment."""
from pathlib import Path
import json
import zipfile

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
families = {"herbal_restoration", "pepper_haste", "dairy_guard", "truffle_vigor", "marsh_breath", "frostfire_tonic"}
potions = families | {"long_" + name for name in families} | {"strong_" + name for name in families - {"marsh_breath", "frostfire_tonic"}}
recipes = list((RES / "data/darkspawn/recipe/brewing").rglob("*.json"))
assert len(recipes) == 80
for path in recipes:
    recipe = json.loads(path.read_text(encoding="utf-8"))
    assert recipe["type"] == "minecraft:brewing"
    source = recipe["input"]["potion_contents"]["potions"]
    result = recipe["output"]["components"]["minecraft:potion_contents"]["potion"]
    assert source == "minecraft:awkward" or source.removeprefix("darkspawn:") in potions, path
    assert result.removeprefix("darkspawn:") in potions, path
    assert recipe["output"]["id"] in {"minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion"}, path

native = set()
for path in (ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*.jar"):
    if "sources" in path.name:
        continue
    with zipfile.ZipFile(path) as archive:
        native.update(archive.namelist())
assert "assets/minecraft/items/bow.json" in native, "Compile first to populate native resource cache"

def validate_model(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"model", "base"} and isinstance(child, str):
                namespace, name = child.split(":", 1)
                if namespace == "minecraft":
                    assert "assets/minecraft/models/" + name + ".json" in native, child
                elif namespace == "darkspawn":
                    assert (RES / ("assets/darkspawn/models/" + name + ".json")).is_file(), child
            validate_model(child)
    elif isinstance(value, list):
        for child in value:
            validate_model(child)

for name in ("chain_shot", "piercing_shot", "heartwood_log", "heartwood_seedling", "nexus_key", "nexus_core", "titanbreaker", "voidbow", "queens_carapace", "soul_lantern_relic"):
    validate_model(json.loads((RES / f"assets/darkspawn/items/{name}.json").read_text(encoding="utf-8")))
for name in ("chain_shot", "piercing_shot", "heartwood_seedling", "heartwood_tapping", "nexus_key", "titanbreaker", "voidbow", "queens_carapace", "soul_lantern_relic"):
    advancement = json.loads((RES / f"data/darkspawn/advancement/recipes/{name}.json").read_text(encoding="utf-8"))
    assert advancement["rewards"]["recipes"] == ["darkspawn:" + name]
print("Validated 80 brewing recipes, 16 potion variants, 10 item visual references and 9 recipe-book unlocks.")
