"""Validate shipped references and render source geometry; preview is not a live Minecraft screenshot."""
import json, math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/darkspawn'
def read(path):return json.loads((A/path).read_text(encoding='utf-8'))
geo=read('geckolib/models/entity/thalassar.geo.json')['minecraft:geometry'][0]
animations=read('geckolib/animations/entity/thalassar.animation.json')['animations'];names={b['name'] for b in geo['bones']}
assert len(names)==len(geo['bones'])
for b in geo['bones']:
    assert not b.get('parent') or b['parent'] in names
    for c in b.get('cubes',[]):
        assert all(x>0 for x in c['size'])
        for face in c['uv'].values():assert all(0<=x<=128 for x in face['uv']) and all(a+b<=128 for a,b in zip(face['uv'],face['uv_size']))
for clip in ['idle','walk','swim','dash','summon']:
    a=animations['animation.thalassar.'+clip];assert a['animation_length']>0;assert set(a['bones'])<=names
armor=read('geometry/armor/noah.json');assert {p['name'] for p in armor['parts']}=={'head','body','right_arm','left_arm','right_leg','left_leg'}
assert {c['slot'] for p in armor['parts'] for c in p['cubes']}=={'helmet','chestplate','leggings','boots'}
assert Image.open(A/'textures/equipment/noah.png').size==(256,128)
assert Image.open(A/'textures/equipment/noah_pulse.png').size==(256,128)
assert Image.open(A/'textures/entity/thalassar.png').size==(128,128)
ids=['legacy_of_noah','noah_helmet','noah_chestplate','noah_leggings','noah_boots','noahs_tempest','tideborn_conch']
lang=read('lang/en_us.json')
for id in ids:
    assert 'item.darkspawn.'+id in lang and 'tooltip.darkspawn.'+id in lang
    ref=read(f'items/{id}.json')['model']['model'].split(':')[1]
    model=read('models/'+ref+'.json')
    for tex in model['textures'].values():assert (A/('textures/'+tex.split(':')[1]+'.png')).exists()
    for cube in model.get('elements',[]):assert all(-16<=v<=32 for v in cube['from']+cube['to'])
print('PASS: relic references, localized lore, armor atlases, GeckoLib bones and all five animation clips.')

canvas=Image.new('RGB',(1280,720),(17,29,40));draw=ImageDraw.Draw(canvas)
try:font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',22);small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',15)
except OSError:font=small=ImageFont.load_default()
draw.text((30,18),'THE LEGACY OF NOAH  /  Source geometry review',font=font,fill=(232,198,105))
palette=[(18,43,69),(24,143,153),(231,184,71),(122,246,229)]
def render(cubes,rect,yaw):
    angle=.22
    def rotate(v):
        x,y,z=v;xx=x*math.cos(yaw)+z*math.sin(yaw);zz=-x*math.sin(yaw)+z*math.cos(yaw)
        return (xx,y*math.cos(angle)-zz*math.sin(angle),zz*math.cos(angle)+y*math.sin(angle))
    faces=[];allpoints=[]
    for xyz,size,mat in cubes:
        x,y,z=xyz;X,Y,Z=[a+b for a,b in zip(xyz,size)]
        points=list(map(rotate,[(x,y,z),(X,y,z),(X,Y,z),(x,Y,z),(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]));allpoints+=points
        for face,shade in zip([[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]],[.82,.75,.7,.92,1,.6]):
            pts=[points[i] for i in face];faces.append((sum(p[2] for p in pts)/4,pts,tuple(int(c*shade) for c in palette[mat])))
    mn=[min(p[i] for p in allpoints) for i in (0,1)];mx=[max(p[i] for p in allpoints) for i in (0,1)]
    x,y,w,h=rect;scale=min(w/(mx[0]-mn[0]),h/(mx[1]-mn[1]))*.88
    for depth,points,color in sorted(faces,key=lambda f:f[0],reverse=True):
        poly=[(x+w/2+(p[0]-(mx[0]+mn[0])/2)*scale,y+h/2-(p[1]-(mx[1]+mn[1])/2)*scale) for p in points]
        draw.polygon(poly,fill=color,outline=tuple(max(0,c-18) for c in color))
mount=[(c['origin'],c['size'],int(c['uv']['north']['uv'][0]//32)) for b in geo['bones'] for c in b.get('cubes',[])]
wear=[]
for p in armor['parts']:
    for c in p['cubes']:
        pos=[a+b for a,b in zip(p['origin'],c['from'])];size=c['size'];wear.append(([pos[0],24-pos[1]-size[1],pos[2]],size,c['material']))
render(wear,(20,80,300,490),-.3);render(mount,(340,80,440,490),.8);render(mount,(800,80,450,490),1.7)
draw.text((48,580),"Noah's Divine Regalia",font=small,fill='white');draw.text((400,580),'Thalassar / three-quarter',font=small,fill='white');draw.text((885,580),'Thalassar / side',font=small,fill='white')
for i,id in enumerate(ids):
    path=A/f'textures/item/{id}.png'
    if path.exists():
        icon=Image.open(path).resize((48,48),Image.Resampling.NEAREST);canvas.paste(icon,(38+i*70,632),icon)
draw.text((555,650),'Native armor + animated runes | GeckoLib royal hippocampus',font=small,fill=(163,204,207))
(ROOT/'build').mkdir(exist_ok=True);canvas.save(ROOT/'build/noah-legacy-preview.png')
print('Rendered build/noah-legacy-preview.png (neutral poses; not in-game acceptance).')
