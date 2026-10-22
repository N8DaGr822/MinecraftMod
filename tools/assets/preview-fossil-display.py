"""Validate and preview the authored fossil block's cuboids; native textures are audited separately."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
model = json.loads((ROOT / 'src/main/resources/assets/darkspawn/models/block/tyrant_fossil_display.json').read_text())
palette = {'bone': (221, 210, 176), 'stone': (132, 145, 151), 'eye': (34, 37, 39)}
faces = []

def project(p):
    x, y, z = p
    return (320 + (x + z - 16) * 16, 440 + (x - z) * 8 - y * 20)

for element in model['elements']:
    x, y, z = element['from']
    X, Y, Z = element['to']
    assert 2 <= x < X <= 14 and 0 <= y < Y <= 15 and 2 <= z < Z <= 14, element
    assert set(element['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'}
    material = element['faces']['up']['texture'].removeprefix('#')
    for points, shade in (
        ([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)], 1.08),
        ([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)], .82),
        ([(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)], .95),
    ):
        depth = sum(a - c + b * .8 for a, b, c in points) / 4
        color = tuple(min(255, int(c * shade)) for c in palette[material])
        # Thin eye inlays sit ahead of the skull face; draw them after its broad face in this flat preview.
        faces.append((depth + (100 if material == 'eye' else 0), points, color))

image = Image.new('RGB', (640, 600), '#19272f')
draw = ImageDraw.Draw(image)
draw.text((24, 24), 'RECONSTRUCTED TYRANT FOSSIL', fill='#ece2cc')
draw.text((24, 44), 'Authored block geometry / flat-color inspection', fill='#acbdc6')
for _, points, color in sorted(faces, key=lambda f: f[0]):
    draw.polygon([project(p) for p in points], fill=color, outline='#56554d')
draw.text((24, 570), 'Decorative display; native bone and smooth-stone textures in game.', fill='#acbdc6')
path = ROOT / 'build/fossil-display-preview.png'
image.save(path)
print(f'Validated {len(model["elements"])} bounded cuboids; preview: {path}')
