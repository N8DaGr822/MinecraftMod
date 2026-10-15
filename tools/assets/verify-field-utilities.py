"""Validate field utility resource links without regenerating or rewriting assets."""
from pathlib import Path
import ast
import json
from PIL import Image

root = Path(__file__).resolve().parents[2]
assets = root / 'src/main/resources/assets/darkspawn'
data = root / 'src/main/resources/data/darkspawn'
catalog = {}
for statement in ast.parse((Path(__file__).with_name('field-utilities.py')).read_text()).body:
    if isinstance(statement, ast.Assign):
        for target in statement.targets:
            if isinstance(target, ast.Name) and target.id in {'FISH', 'ACCESSORIES', 'MEALS', 'EXTRAS'}:
                catalog[target.id] = ast.literal_eval(statement.value)
fish = catalog['FISH']
items = catalog['ACCESSORIES'] + catalog['MEALS'] + catalog['EXTRAS'] + fish + ['cooked_' + f for f in fish] + [f + '_bucket' for f in fish]
language = json.loads((assets/'lang/en_us.json').read_text(encoding='utf-8'))
for item in items:
    declaration = json.loads((assets/'items'/f'{item}.json').read_text())
    assert declaration['model']['model'] == 'darkspawn:item/' + item, item
    model = json.loads((assets/'models/item'/f'{item}.json').read_text())
    assert model['textures']['layer0'] == 'darkspawn:item/' + item, item
    with Image.open(assets/'textures/item'/f'{item}.png') as image:
        assert image.size == (16, 16) and image.getbbox(), item
    assert language['item.darkspawn.' + item], item
for species in fish:
    with Image.open(assets/'textures/entity/fish'/f'{species}.png') as image:
        assert image.size == (32, 32), species
    assert language['entity.darkspawn.' + species], species
    for method in ['smelting', 'smoking']:
        recipe = json.loads((data/'recipe'/f'{species}_{method}.json').read_text())
        assert recipe['ingredient'] == 'darkspawn:' + species
        assert recipe['result']['id'] == 'darkspawn:cooked_' + species
for meal in catalog['MEALS']:
    recipe = json.loads((data/'recipe'/f'{meal}.json').read_text())
    assert recipe['type'] == 'darkspawn:cooking' and 'minecraft:bowl' in recipe['ingredients'], meal
for recipe_id in catalog['ACCESSORIES'] + ['goat_cheese']:
    recipe = json.loads((data/'recipe'/f'{recipe_id}.json').read_text())
    assert recipe['type'] == 'minecraft:crafting_shapeless' and recipe['ingredients'], recipe_id
    advancement = json.loads((data/'advancement/recipes'/f'{recipe_id}.json').read_text())
    assert advancement['rewards']['recipes'] == ['darkspawn:' + recipe_id], recipe_id
print(f'PASS: {len(items)} item icons/models/translations, six native fish skins, regional cooking and recipe-book resources.')
