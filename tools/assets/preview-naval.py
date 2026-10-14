"""Validate and preview actual naval model cuboids; not an in-game screenshot."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/darkspawn'
font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 20)
small = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 15)
image = Image.new('RGB', (1100, 460), '#142d38')
draw = ImageDraw.Draw(image)
draw.text((24, 18), 'NAVAL COMPONENTS | actual model geometry', font=font, fill='#e9dec2')
draw.text((24, 52), 'Offline material-color preview; lighting and Minecraft textures require a live check', font=small, fill='#a3bbb9')
colors = {'wood': (132,94,57), 'metal': (179,184,182), 'dark': (49,54,57), 'chart': (170,157,104), 'ball': (135,144,149)}
for index, name in enumerate(['shipyard','ship_helm','naval_cannon','solid_shot','incendiary_shot']):
    kind = 'block' if index < 3 else 'item'
    model = json.loads((ASSETS / f'models/{kind}/{name}.json').read_text(encoding='utf-8'))
    cx, cy = 112+index*218, 222
    def project(point):
        x,y,z = point; x-=8; y-=8; z-=8
        return cx+(x*.9+z*.42)*7, cy-y*7+(z*.9-x*.42)*2, z*.9-x*.42+y*.1
    faces = []
    for block in model['elements']:
        assert all(0 <= value <= 16 for value in block['from']+block['to'])
        assert all(a < b for a,b in zip(block['from'],block['to']))
        x,y,z = block['from']; a,b,c = block['to']
        texture = block['faces']['north']['texture'][1:]
        assert texture in model['textures']
        material = (186,89,36) if name == 'incendiary_shot' else colors[texture]
        for points, shade in [([(x,y,z),(a,y,z),(a,b,z),(x,b,z)],1), ([(x,y,c),(a,y,c),(a,b,c),(x,b,c)],.7), ([(x,y,z),(x,y,c),(x,b,c),(x,b,z)],.8), ([(a,y,z),(a,y,c),(a,b,c),(a,b,z)],.9), ([(x,b,z),(a,b,z),(a,b,c),(x,b,c)],1.15)]:
            points = [project(p) for p in points]
            color = tuple(min(255,int(v*shade)) for v in material)
            faces.append((sum(p[2] for p in points), [(p[0],p[1]) for p in points], color))
    for _, points, color in sorted(faces, reverse=True):
        draw.polygon(points, fill=color, outline=tuple(int(v*.65) for v in color))
    draw.text((cx,326),name.replace('_',' ').title(),font=small,anchor='mt',fill='#e9dec2')
draw.text((24,392),'Small: 7 x 15 / 4 guns     ->     5 NPC victories     ->     Large: 11 x 23 / 8 guns',font=font,fill='#7bd0c1')
output = ROOT / 'art/pirate/naval-components.png'
output.parent.mkdir(parents=True,exist_ok=True)
image.save(output)
print('PASS: five naval models have valid cuboids and texture references.')
print(output)
