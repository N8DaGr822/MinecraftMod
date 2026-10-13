"""Validate the authored Forsaken asset contract and render a geometry review sheet."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/darkspawn'
names = ['infected_wanderer','infected_crawler','spore_spitter','assimilated_brute']
palette = [(94,109,81),(154,164,113),(72,82,63),(57,51,48),(190,145,48),(166,211,82),(31,42,43),(107,66,69)]
panels=[]
for name in names:
    geometry=json.loads((ASSETS/f'geckolib/models/entity/{name}.geo.json').read_text())['minecraft:geometry'][0]
    bones=geometry['bones'];bone_names={bone['name'] for bone in bones}
    assert len(bone_names)==len(bones)
    cubes=[]
    for bone in bones:
        assert 'parent' not in bone or bone['parent'] in bone_names
        for cube in bone.get('cubes',[]):
            assert all(size>0 for size in cube['size'])
            x,y=cube['uv'];w,h,d=cube['size']
            assert x+2*(w+d)<=256 and y+h+d<=128, (name,cube)
            cubes.append((cube['origin'],cube['size'],int(x//64+y//64*4)))
    animations=json.loads((ASSETS/f'geckolib/animations/entity/{name}.animation.json').read_text())['animations']
    assert len(animations)==7
    for clip in animations.values(): assert set(clip['bones']).issubset(bone_names)
    assert Image.open(ASSETS/f'textures/entity/{name}.png').size==(256,128)
    panels.append((name.replace('_',' ').title(),cubes))
armor=json.loads((ASSETS/'geometry/armor/quarantine.json').read_text())
assert {p['name'] for p in armor['parts']}=={'head','body','left_arm','right_arm','left_leg','right_leg'}
cubes=[]
for part in armor['parts']:
    for box in part['cubes']:
        x,y,z=[a+b for a,b in zip(box['from'],part['origin'])];w,h,d=box['size']
        cubes.append(([x,24-y-h,z],[w,h,d],box['material']))
panels.append(('Quarantine Armor',cubes))
resources={'contaminated_alloy','biofiber','parasitic_core','research_data','quarantine_module'}
used=set()
for name in ['bio_scanner','quarantine_module','quarantine_helmet','quarantine_chestplate','quarantine_leggings','quarantine_boots','pulse_blade','containment_beacon']:
    recipe=json.loads((RES/f'data/darkspawn/recipe/{name}.json').read_text())
    ingredients=([recipe['template'],recipe['base'],recipe['addition']] if recipe['type']=='minecraft:smithing_transform' else recipe.get('ingredients',list(recipe.get('key',{}).values())))
    used.update(i.split(':')[1] for i in ingredients if i.startswith('darkspawn:'))
    assert (ASSETS/f'items/{name}.json').exists()
assert resources.issubset(used)
city=json.loads((ROOT/'art/structures/forsaken_city.json').read_text())
assert len(city['nests'])==4 and all(pos[2]>=42 for pos in city['nests'])

scale=2
image=Image.new('RGB',(1250*scale,520*scale),(20,29,32));draw=ImageDraw.Draw(image)
font_path=Path('C:/Windows/Fonts/segoeui.ttf')
font=ImageFont.truetype(str(font_path),20*scale)
small=ImageFont.truetype(str(font_path),14*scale)
draw.text((24*scale,15*scale),'THE FORSAKEN - authored geometry and item assets',font=font,fill=(233,228,209))
draw.text((24*scale,47*scale),'Offline geometry preview - not an in-game screenshot',font=small,fill=(150,170,163))
yaw=math.radians(-25);pitch=.18
for index,(name,cubes) in enumerate(panels):
    center=(125+index*250)*scale;baseline=363*scale;factor=5.8*scale
    def project(p):
        x,y,z=p;xx=x*math.cos(yaw)+z*math.sin(yaw);zz=-x*math.sin(yaw)+z*math.cos(yaw)
        return center+xx*factor,baseline-(y-zz*pitch)*factor,zz+y*pitch
    faces=[]
    for (x,y,z),(w,h,d),material in cubes:
        a,b,c=x+w,y+h,z+d
        for points,shade in [([(x,y,z),(a,y,z),(a,b,z),(x,b,z)],1),([(x,y,c),(a,y,c),(a,b,c),(x,b,c)],.7),
                             ([(x,y,z),(x,y,c),(x,b,c),(x,b,z)],.82),([(a,y,z),(a,y,c),(a,b,c),(a,b,z)],.9),
                             ([(x,b,z),(a,b,z),(a,b,c),(x,b,c)],1.12)]:
            projected=[project(p) for p in points]
            faces.append((sum(p[2] for p in projected)/4,[(p[0],p[1]) for p in projected],tuple(min(255,int(channel*shade)) for channel in palette[material])))
    for _,points,color in sorted(faces,reverse=True): draw.polygon(points,fill=color,outline=tuple(int(c*.65) for c in color),width=scale)
    draw.text((center,380*scale),name,anchor='mt',font=small,fill=(233,228,209))
    label=['Charges after detection','Fast, low flanking role','Ranged spores; use cover','Armored, slow heavy swing','Sealed hood and respirator'][index]
    draw.text((center,405*scale),label,anchor='mt',font=small,fill=(150,170,163))
for i,name in enumerate(['contaminated_alloy','biofiber','parasitic_core','research_data','quarantine_module','bio_scanner','quarantine_helmet','quarantine_chestplate','quarantine_leggings','quarantine_boots']):
    icon=Image.open(ASSETS/f'textures/item/{name}.png').resize((48*scale,48*scale),Image.Resampling.NEAREST)
    image.paste(icon,((250+i*75)*scale,450*scale),icon)
image=image.resize((1250,520),Image.Resampling.LANCZOS)
target=ROOT/'art/forsaken/content-preview.png';target.parent.mkdir(parents=True,exist_ok=True);image.save(target)
print('PASS: four rigs / 28 clips, UV bounds, armor parts, eight useful recipes, four authored nests. Preview:',target)

for name in ['containment_beacon', 'containment_beacon_active']:
    model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
    assert len(model['elements'])==4
    assert Image.open(ASSETS/f'textures/block/{name}.png').size==(16,16)
    for element in model['elements']:
        assert all(0<=a<b<=16 for a,b in zip(element['from'],element['to']))
assert set(json.loads((ASSETS/'blockstates/containment_beacon.json').read_text())['variants'])=={'active=false','active=true'}
assert json.loads((ASSETS/'models/item/pulse_blade.json').read_text())['parent']=='minecraft:item/handheld'
loot=json.loads((RES/'data/darkspawn/loot_table/blocks/containment_beacon.json').read_text())
assert 'copy_components' not in json.dumps(loot) and 'copy_nbt' not in json.dumps(loot)
preview=Image.new('RGB',(1200,350),(20,29,32)); pen=ImageDraw.Draw(preview)
title=ImageFont.truetype(str(font_path),26); label=ImageFont.truetype(str(font_path),19)
pen.text((28,16),'FORSAKEN EXPEDITION TOOLS',font=title,fill=(233,228,209))
for index,(name,caption,detail) in enumerate([
    ('pulse_blade','Pulse Blade','Charge 1.5 seconds; release pulse'),
    ('containment_beacon_active','Containment Beacon','One core; five minutes; 16 blocks'),
    ('bio_scanner','Bio-Scanner','Nests, resources and local pressure')]):
    x=200+index*400
    if index==1:
        texture=Image.open(ASSETS/f'textures/block/{name}.png').convert('RGBA')
        for left,top,right,bottom in [(x-64,192,x+64,216),(x-40,120,x+40,192),(x-48,104,x+48,120),(x-24,88,x+24,104)]:
            preview.paste(texture.resize((right-left,bottom-top),Image.Resampling.NEAREST),(left,top))
    else:
        icon=Image.open(ASSETS/f'textures/item/{name}.png').convert('RGBA').resize((144,144),Image.Resampling.NEAREST)
        preview.paste(icon,(x-72,80),icon)
    pen.text((x,244),caption,font=title,anchor='mt',fill=(233,228,209))
    pen.text((x,285),detail,font=label,anchor='mt',fill=(150,170,163))
pen.text((28,324),'Authored item textures and beacon front projection; not an in-game screenshot.',font=ImageFont.truetype(str(font_path),15),fill=(150,170,163))
preview.save(ROOT/'art/forsaken/expedition-tools-preview.png')
print('PASS: blade model, beacon geometry, active states and reusable drop contract.')
