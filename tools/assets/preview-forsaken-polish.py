"""Render the actual item cuboids and bunker floor plan; this is not a gameplay screenshot."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/darkspawn'
palette = [(94,109,81),(154,164,113),(72,82,63),(57,51,48),(190,145,48),(166,211,82),(31,42,43),(107,66,69)]
font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 18)
small = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 14)
names = ['contaminated_alloy','biofiber','parasitic_core','research_data','quarantine_module','bio_scanner','pulse_blade','quarantine_helmet','quarantine_chestplate','quarantine_leggings','quarantine_boots']
image = Image.new('RGB', (1200, 580), (20,40,46)); pen = ImageDraw.Draw(image)
pen.text((24,16),'FORSAKEN / 3D inventory and held models',font=font,fill='#e1e7de')
pen.text((24,44),'Actual model cuboids and atlas materials; offline illustration, not in-game lighting',font=small,fill='#a1b8b4')
for index,name in enumerate(names):
    cx=100+(index%6)*200;cy=182+(index//6)*250
    model=json.loads((ASSETS/f'models/item/{name}.json').read_text(encoding='utf-8'))
    def project(p):
        x,y,z=p;x-=8;y-=8;z-=8
        return cx+(x*.9+z*.42)*7,cy-y*7+(z*.9-x*.42)*2,z*.9-x*.42+y*.1
    faces=[]
    for box in model['elements']:
        x,y,z=box['from'];a,b,c=box['to']
        uv=box['faces']['north']['uv'];material=int(uv[0]/4+int(uv[1]/8)*4)
        for points,shade in [([(x,y,z),(a,y,z),(a,b,z),(x,b,z)],1),([(x,y,c),(a,y,c),(a,b,c),(x,b,c)],.7),
                             ([(x,y,z),(x,y,c),(x,b,c),(x,b,z)],.8),([(a,y,z),(a,y,c),(a,b,c),(a,b,z)],.9),
                             ([(x,b,z),(a,b,z),(a,b,c),(x,b,c)],1.15)]:
            points=[project(p) for p in points]
            color=tuple(min(255,int(v*shade)) for v in palette[material])
            faces.append((sum(p[2] for p in points),[(p[0],p[1]) for p in points],color))
    for _,points,color in sorted(faces,reverse=True):pen.polygon(points,fill=color,outline=tuple(int(v*.7) for v in color))
    pen.text((cx,cy+85),name.replace('_',' ').title(),font=small,anchor='mt',fill='#e1e7de')
target=ROOT/'art/forsaken/item-models-preview.png';image.save(target)

layout=json.loads((ROOT/'build/research-bunker-preview.json').read_text(encoding='utf-8'))
blocks={tuple(b['pos']):layout['palette'][b['state']]['Name'].split(':')[1] for b in layout['blocks']}
# Route Acceptance: Model a standing player's two-block clearance; doors are traversable when opened.
def clear(x,z):
    return all(blocks.get((x,y,z),'air') in ['air','iron_door','stone_pressure_plate','heavy_weighted_pressure_plate'] for y in [4,5])
seen={(12,20)};todo=list(seen)
while todo:
    x,z=todo.pop()
    for q in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
        if q not in seen and 5<=q[0]<=19 and 5<=q[1]<=20 and clear(*q):seen.add(q);todo.append(q)
assert (12,8) in seen, 'Portal approach must be reachable through the airlock'
assert any(p in seen for p in [(8,12),(8,15),(10,13)]), 'Archive must remain accessible'
image=Image.new('RGB',(1000,680),(20,40,46));pen=ImageDraw.Draw(image)
pen.text((24,16),'RESEARCH BUNKER / experimental containment facility',font=font,fill='#e1e7de')
pen.text((24,45),'Standing-height floor plan; door blocks treated as open for route validation',font=small,fill='#a1b8b4')
for x in range(4,21):
    for z in range(4,21):
        name=blocks.get((x,5,z),'air')
        color='#8d9992' if 'stone' in name else '#dce5d9' if 'quartz' in name else '#70c7d5' if 'glass' in name else '#402f52' if 'obsidian' in name else '#e1b845' if 'door' in name else '#b98c47' if 'archive' in name else '#243c42' if name=='air' else '#546966'
        if name=='air' and (x,z) in seen:color='#355b52'
        px=24+(x-4)*32;py=82+(z-4)*32
        pen.rectangle((px,py,px+30,py+30),fill=color)
for y,title,detail in [(100,'CONTAINMENT CHAMBER','Original damaged gate and repair clearance'),(190,'OBSERVATION WINDOW','Eight-block-wide glass partition'),(280,'TWO-DOOR AIRLOCK','Three-block separation, pressure plates'),(370,'CONTROL ROOM','Screens, consoles and seated stations'),(460,'PRESERVED PROGRESSION','Research archive and one finite supply chest')]:
    pen.text((600,y),title,font=font,fill='#65c8b5');pen.text((600,y+30),detail,font=small,fill='#e1e7de')
pen.text((24,640),'PASS: entrance -> airlock -> portal approach; archive remains reachable',font=font,fill='#65c8b5')
image.save(ROOT/'art/forsaken/bunker-layout-preview.png')
print('PASS: 11 model previews, reachable bunker portal and research archive.')
