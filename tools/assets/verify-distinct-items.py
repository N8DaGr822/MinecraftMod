"""Validate authored item models and create an offline geometry contact sheet."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[2]
A=ROOT/'src/main/resources/assets/darkspawn'
manifest=json.loads((ROOT/'art/items/distinct-models.json').read_text())
names=manifest['models']
colors={name:tuple(bytes.fromhex(value[1:])) for name,value in manifest['materials'].items()}
def read(path):return json.loads(path.read_text(encoding='utf-8'))
assert any(s.get('source')=='item/materials' and s.get('prefix')=='item/materials/' for s in read(A.parent/'minecraft/atlases/blocks.json')['sources']), 'Placed Heartwood must resolve materials on the blocks atlas'
with Image.open(A/'textures/item/materials/flame.png') as fire:
    assert fire.mode=='RGBA' and fire.getchannel('A').getextrema()==(0,255), 'Bonfire fire must remain cutout'
def check_links(value):
    if isinstance(value,dict):
        for key,child in value.items():
            if key in {'model','base'} and isinstance(child,str):
                assert not child.startswith(('minecraft:item/','minecraft:block/')),child
            check_links(child)
    elif isinstance(value,list):
        for child in value:check_links(child)
for path in (A/'items').glob('*.json'):check_links(read(path))
for name in names:
    value=read(A/f'models/item/{name}.json')
    assert value['elements'],name
    for texture in value['textures'].values():
        assert texture.startswith('darkspawn:'),(name,texture)
        with Image.open(A/f'textures/{texture.split(":",1)[1]}.png') as image:image.verify()
    for cube in value['elements']:
        assert all(-16<=v<=32 and math.isfinite(v) for v in cube['from']+cube['to']),name
        assert all(a<=b for a,b in zip(cube['from'],cube['to'])) and sum(a<b for a,b in zip(cube['from'],cube['to']))>=2,name
        for face in cube['faces'].values():
            assert face['texture'][1:] in value['textures'],name
            assert all(0<=v<=16 for v in face.get('uv',[])),name
bow=read(A/'items/voidbow.json')['model']
assert bow['property']=='minecraft:using_item'
assert bow['on_true']['property']=='minecraft:use_duration' and bow['on_true']['scale']==.05
assert [e['threshold'] for e in bow['on_true']['entries']]==[.65,.9]
assert len({json.dumps(read(A/f'models/item/{name}.json')['elements']) for name in ['voidbow','voidbow_pulling_0','voidbow_pulling_1','voidbow_pulling_2']})==4
shield=read(A/'items/queens_carapace.json')['model']
assert shield['property']=='minecraft:using_item'
assert read(A/'models/item/queens_carapace.json')['display']!=read(A/'models/item/queens_carapace_blocking.json')['display']
compass=read(A/'items/logbound_compass.json')
assert compass==read(A/'items/landmark_compass.json')
assert compass['model']['target']=='lodestone' and compass['model']['scale']==32
assert len({json.dumps(read(A/f'models/item/expedition_compass_{i:02}.json')['elements']) for i in range(32)})==32
# Cardinal needle direction matches native frame numbering: down, left, up, right.
for frame,expected in [(0,(8,4)),(8,(4,8)),(16,(8,12)),(24,(12,8))]:
    red=[c for c in read(A/f'models/item/expedition_compass_{frame:02}.json')['elements'] if c['faces']['north']['texture']=='#red'][-1]
    center=[(a+b)/2 for a,b in zip(red['from'],red['to'])]
    assert all(abs(a-b)<.001 for a,b in zip(center,expected)),frame

visible=[n for n in names if not n.startswith('expedition_compass_') or n.endswith(('00','08','16','24'))]
columns=6;width=228;height=218
image=Image.new('RGB',(columns*width,90+math.ceil(len(visible)/columns)*height),'#142631')
draw=ImageDraw.Draw(image);font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',14)
draw.text((22,16),'DARKSPAWN | distinct item artwork',font=font,fill='#e8d7b1')
draw.text((22,43),'Offline authored-geometry preview; Minecraft lighting and hand poses still need live review.',font=font,fill='#a6bfbe')
for i,name in enumerate(visible):
    cx=(i%columns)*width+width/2;cy=90+(i//columns)*height+92
    value=read(A/f'models/item/{name}.json');faces=[]
    def project(p):
        x,y,z=p;x-=8;y-=8;z-=8
        return cx+(x*.92+z*.38)*7,cy-y*7+(z*.92-x*.38)*2,z*.92-x*.38+y*.1
    for cube in value['elements']:
        x,y,z=cube['from'];X,Y,Z=cube['to']
        tex=value['textures'][cube['faces']['north']['texture'][1:]]
        material=colors.get(tex.rsplit('/',1)[-1],(105,131,120))
        for points,shade in [([(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],1),([(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],.7),([(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],.8),([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.9),([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.1)]:
            points=[project(p) for p in points];color=tuple(min(255,int(v*shade)) for v in material)
            faces.append((sum(p[2] for p in points),[(p[0],p[1]) for p in points],color))
    for _,points,color in sorted(faces,reverse=True):draw.polygon(points,fill=color,outline=tuple(int(v*.65) for v in color))
    draw.text((cx,cy+80),name.replace('_',' ').title(),font=font,anchor='mt',fill='#e8d7b1')
path=ROOT/'build/distinct-items-preview.png';image.save(path)
print(f'PASS: {len(names)} authored item model states, materials, bow/shield poses and 32 compass directions. Preview: {path}')
