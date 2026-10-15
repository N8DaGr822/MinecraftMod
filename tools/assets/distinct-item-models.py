"""Author Darkspawn cuboid item models; run after older feature asset generators.

Geometry is the editable source, like naval-assets.cjs. Native display transforms
and conditional selectors are retained where they control use animations.
"""
from pathlib import Path
import copy
import json
import math
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / 'src/main/resources/assets/darkspawn'
PALETTE = {'iron':'#667d91', 'edge':'#b9d9de', 'dark':'#243344', 'gold':'#bd914b',
           'wood':'#68503e', 'paper':'#d5c19a', 'purple':'#694a94', 'glow':'#65dfce',
           'red':'#b24c55', 'green':'#416c59', 'bone':'#c4baa0', 'ember':'#e09144',
           'stone':'#485666', 'glass':'#65b9bd', 'chart':'#c9b78b'}
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8')

# Authored materials: restrained grain/rivets, not copied vanilla texture sheets.
for name, color in PALETTE.items():
    im = Image.new('RGB', (16,16), color); d = ImageDraw.Draw(im)
    rgb = im.getpixel((0,0))
    light = tuple(min(255,c+16) for c in rgb); shadow = tuple(max(0,c-16) for c in rgb)
    for y in range(16):
        for x in range(16):
            if (x*7+y*11)%19==0: d.point((x,y), fill=light)
            elif (x*3+y*5)%17==0: d.point((x,y), fill=shadow)
    if name == 'wood':
        for x in [2,7,12]: d.line((x,0,x,15),fill=shadow)
    if name == 'chart':
        d.line([(2,12),(5,9),(8,10),(10,5),(13,3)],fill='#68503e',width=1)
        for x,y in [(3,4),(11,11)]:d.rectangle((x,y,x+2,y+1),fill='#416c59')
    if name == 'glass':
        im=im.convert('RGBA');im.putalpha(130)
    path=A/f'textures/item/materials/{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)

# Cutout fire avoids turning the bonfire's crossed planes into opaque orange panels.
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(2,15),(1,10),(4,12),(4,6),(6,9),(8,1),(10,8),(12,5),(12,11),(15,9),(14,15)],fill='#d17a36')
d.polygon([(5,15),(4,11),(7,12),(8,6),(10,11),(12,10),(11,15)],fill='#f1c05c')
im.save(A/'textures/item/materials/flame.png')

DISPLAY = {'gui':{'rotation':[15,25,0],'scale':[.85,.85,.85]},
           'ground':{'translation':[0,2,0],'scale':[.45,.45,.45]},
           'fixed':{'rotation':[0,180,0],'scale':[.8,.8,.8]}}
for hand in ['righthand','lefthand']:
    DISPLAY['firstperson_'+hand]={'rotation':[0,-25 if hand=='righthand' else 25,0],'translation':[0,1,0],'scale':[.65,.65,.65]}
    DISPLAY['thirdperson_'+hand]={'rotation':[0,0,0],'translation':[0,2,0],'scale':[.6,.6,.6]}
def box(a,b,color):
    return {'from':a,'to':b,'faces':{f:{'texture':'#'+color,'uv':[0,0,16,16]} for f in ['north','south','east','west','up','down']}}
def b(x,y,z,X,Y,Z,c): return box([x,y,z],[X,Y,Z],c)
models={}
def model(name, elements, display=None):
    value={'parent':'minecraft:block/block','textures':{c:'darkspawn:item/materials/'+c for c in PALETTE},'elements':elements,'display':copy.deepcopy(display or DISPLAY)}
    value['textures']['particle']='darkspawn:item/materials/'+next(iter(elements[0]['faces'].values()))['texture'][1:]
    write(A/f'models/item/{name}.json',value);models[name]=value
def item(name): write(A/f'items/{name}.json',{'model':{'type':'minecraft:model','model':'darkspawn:item/'+name}})
def make(name, elements, display=None): model(name,elements,display);item(name)

make('titanbreaker',[b(7,1,7,9,11,9,'wood'),b(6.5,1,6.5,9.5,3,9.5,'gold'),b(3,10,4,13,14,12,'dark'),b(2,10.5,4.5,4,13.5,11.5,'iron'),b(12,10.5,4.5,14,13.5,11.5,'iron'),b(6,10,3.5,10,14,4.5,'gold'),b(7,11,3,9,13,4,'glow')])
make('nexus_core',[b(4,4,4,12,12,12,'dark'),b(6,2,6,10,14,10,'purple'),b(2,6,6,14,10,10,'purple'),b(6,6,2,10,10,14,'purple'),b(6.5,6.5,1.5,9.5,9.5,2.5,'glow'),b(6.5,6.5,13.5,9.5,9.5,14.5,'glow')])
make('nexus_key',[b(7,2,7,9,10,9,'gold'),b(9,2,7,12,4,9,'gold'),b(9,5,7,11,6,9,'gold'),b(4,10,6,12,14,10,'dark'),b(6,11,5.5,10,13,10.5,'purple'),b(7,11.5,5,9,12.5,11,'glow')])
make('awakening_seal',[b(4,3,6,12,13,10,'dark'),b(3,5,6,13,11,10,'gold'),b(5,4,5.5,11,12,10.5,'purple'),b(7,6,5,9,10,11,'glow'),b(6,7,5,10,9,11,'glow')])
make('chain_shot',[b(1,5,5,6,10,10,'iron'),b(10,5,5,15,10,10,'iron'),b(5,6.5,6,11,7.5,7,'edge'),b(5,8,8,11,9,9,'dark'),b(6,6,6,7,9,9,'edge'),b(9,6,6,10,9,9,'edge')])
make('piercing_shot',[b(5,2,5,11,11,11,'iron'),b(5,3,5,11,5,11,'gold'),b(6,11,6,10,13,10,'edge'),b(7,13,7,9,15,9,'edge'),b(4.5,1,4.5,11.5,3,11.5,'dark')])
lantern=[b(4,3,4,12,4,12,'dark'),b(4,11,4,12,12,12,'dark'),b(5,4,5,11,5,11,'purple'),b(6,5,6,10,10,10,'glow')]
for x in [4,11]:
    for z in [4,11]:lantern.append(b(x,4,z,x+1,11,z+1,'gold'))
lantern += [b(6,12,7,7,15,9,'gold'),b(9,12,7,10,15,9,'gold'),b(6,14,7,10,15,9,'gold')]
make('soul_lantern_relic',lantern)

# Custom shell retains separate active/idle poses; it does not invoke the native shield renderer.
shell=[b(3,5,7,13,13,9,'dark'),b(4,3,7,12,5,9,'dark'),b(6,1,7,10,3,9,'dark'),b(4,6,6,12,12,7,'purple'),b(5,4,6,11,6,7,'purple'),b(7,3,5.5,9,13,6.5,'gold'),b(6,8,5,10,10,6,'glow')]
for x in [2,12]:
    for y in [5,8,11]:shell.append(b(x,y,6,x+2,y+1,9,'bone'))
for state in ['', '_blocking']:
    display=copy.deepcopy(DISPLAY)
    for hand in ['righthand','lefthand']:
        display['firstperson_'+hand]={'rotation':[0,180,0 if not state else -15],'translation':[(-3 if hand=='righthand' else 3) if not state else 0,1,-2],'scale':[1,1,1]}
        display['thirdperson_'+hand]={'rotation':[0,90 if not state else 145,0],'translation':[0,2,0],'scale':[1,1,1]}
    model('queens_carapace'+state,shell,display)
write(A/'items/queens_carapace.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_false':{'type':'minecraft:model','model':'darkspawn:item/queens_carapace'},'on_true':{'type':'minecraft:model','model':'darkspawn:item/queens_carapace_blocking'}}})

native={}
for jar in (ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*.jar'):
    if 'sources' in jar.name:continue
    with zipfile.ZipFile(jar) as archive:
        if 'assets/minecraft/models/item/bow.json' in archive.namelist():
            native=json.loads(archive.read('assets/minecraft/models/item/bow.json'));break
assert native, 'Compile first to populate native display transform cache'
for stage in range(4):
    name='voidbow'+('' if stage==0 else '_pulling_'+str(stage-1))
    elements=[b(3,6,7,5,10,9,'wood'),b(3,7,6.5,5,9,9.5,'gold')]
    for y,x in [(1,8),(2,6),(3,5),(4,4),(5,3),(10,3),(11,4),(12,5),(13,6),(14,8)]:
        elements.append(b(x,y,7,x+2,y+1,9,'purple'))
    # String bends progressively toward the drawing hand; visible on both sides.
    for y in range(2,15):
        x=9+(stage*1.25)*(1-abs(y-8)/7)
        elements.append(b(x,y,7.8,x+.3,y+1,8.2,'glow'))
    if stage:elements += [b(3,7.7,7.5,10+stage,8.3,8.5,'bone'),b(2,7,7,4,9,9,'glow')]
    disp=copy.deepcopy(DISPLAY);disp.update(native['display']);disp['gui']={'rotation':[0,0,0],'scale':[1,1,1]}
    model(name,elements,disp)
path=A/'items/voidbow.json';definition=json.loads(path.read_text())
def replace_refs(value, old, new):
    if isinstance(value,dict):return {k:replace_refs(v,old,new) for k,v in value.items()}
    if isinstance(value,list):return [replace_refs(v,old,new) for v in value]
    return value.replace(old,new) if isinstance(value,str) else value
write(path,replace_refs(definition,'minecraft:item/bow','darkspawn:item/voidbow'))

make('cookbook',[b(3,2,5,13,14,6,'green'),b(3,2,10,13,14,11,'green'),b(3,2,5,4,14,11,'gold'),b(4,3,6,12,13,10,'paper'),b(6,6,4.5,10,10,5,'gold'),b(7,7,4,9,9,4.5,'ember')])
scrolls=['bog_gumbo','captains_chowder','frost_garlic_soup','hero_feast','infernal_roast','jungle_pepper_skewer','marsh_rice_bowl','shadow_stew']
for i,name in enumerate(scrolls+['blank_discovery_chart']):
    name=name+'_scroll' if i<8 else name
    color=['green','iron','glow','gold','ember','red','wood','purple','iron'][i]
    elements=[b(3,3,7,13,13,8,'paper'),b(2,2,6,14,4,9,'bone'),b(2,12,6,14,14,9,'bone'),b(7,6,6.5,10,9,8.5,color)]
    for y in [5,10,11]:elements.append(b(4,y,6.8,7 if y==5 else 11,y+.3,8.2,'wood'))
    make(name,elements)
make('blank_discovery_chart',[b(2,3,7,14,13,8,'paper'),b(2,3,6.5,3,13,8.5,'gold'),b(13,3,6.5,14,13,8.5,'gold'),b(4,5,6.5,7,7,8.5,'green'),b(6,6,6.5,8,10,8.5,'green'),b(10,9,6,11,12,9,'red'),b(9,10,6,12,11,9,'red'),b(9,5,6.5,12,6,8.5,'iron')])
make('dragon_starter_chest',[b(2,3,3,14,10,13,'wood'),b(2,10,3,14,12,13,'green'),b(3,3,2.5,5,12,13.5,'gold'),b(11,3,2.5,13,12,13.5,'gold'),b(6,7,2,10,10,3,'dark'),b(7,8,1.5,9,9,2.5,'glow'),b(5,12,6,6,15,8,'bone'),b(10,12,6,11,15,8,'bone')])
make('testing_camp',[b(2,2,3,14,4,13,'wood'),b(3,4,4,13,6,12,'green'),b(4,6,4,12,8,12,'green'),b(5,8,4,11,10,12,'green'),b(6,10,4,10,12,12,'green'),b(7,12,4,9,14,12,'gold'),b(6,4,3.5,10,9,4,'dark'),b(7,5,3,9,7,4,'ember')])

# Keep the existing 32-step lodestone selector exactly; only its model resources change.
for frame in range(32):
    elements=[b(2,2,7,14,14,9,'dark'),b(3,3,6.5,13,13,9.5,'gold'),b(4,4,6,12,12,10,'green')]
    angle=frame*math.tau/32
    for step in range(5):
        x=8-math.sin(angle)*step;y=8-math.cos(angle)*step
        elements.append(b(x-.55,y-.55,5.3,x+.55,y+.55,10.7,'red'))
        x=8+math.sin(angle)*step;y=8+math.cos(angle)*step
        elements.append(b(x-.4,y-.4,5.3,x+.4,y+.4,10.7,'edge'))
    elements.append(b(7.3,7.3,5,8.7,8.7,11,'gold'))
    disp=copy.deepcopy(DISPLAY);disp['gui']={'rotation':[0,0,0],'scale':[1,1,1]}
    model(f'expedition_compass_{frame:02}',elements,disp)
for name in ['landmark_compass','logbound_compass']:
    path=A/f'items/{name}.json';write(path,replace_refs(json.loads(path.read_text()),'minecraft:item/compass_','darkspawn:item/expedition_compass_'))

# Existing authored geometry stays intact; replace only its borrowed material slots.
materials={'companion_capsule':{'shell':'iron','band':'purple','eye':'glow'},
 'containment_key':{'particle':'gold','metal':'gold','light':'glow'},
 'veterinary_kit':{'shell':'bone','band':'red','eye':'iron'},
 'solid_shot':{'particle':'iron','ball':'iron'},'incendiary_shot':{'particle':'ember','ball':'ember'},
 'pirate_beer':{'wood':'wood','band':'gold','foam':'bone','particle':'wood'},
 'pirate_rum':{'bottle':'purple','label':'paper','cork':'wood','particle':'purple'}}
for name,slots in materials.items():
    path=A/f'models/item/{name}.json';value=json.loads(path.read_text())
    for slot,color in slots.items():value['textures'][slot]='darkspawn:item/materials/'+color
    write(path,value);models[name]=value

# Heartwood is both an inventory item and a placeable block: keep those visuals consistent.
for name, end in [('heartwood_bark',False),('heartwood_rings',True)]:
    im=Image.new('RGB',(16,16),'#416253');d=ImageDraw.Draw(im)
    if end:
        for inset in range(1,8,2):d.rectangle((inset,inset,15-inset,15-inset),outline='#bd914b')
        d.rectangle((7,7,8,8),fill='#65dfce')
    else:
        for x in [1,5,10,14]:
            d.line((x,0,x,15),fill='#243e36');d.line((x+1,2,x+1,6),fill='#65a892')
    im.save(A/f'textures/item/materials/{name}.png')
value={'parent':'minecraft:block/cube_column','textures':{'end':'darkspawn:item/materials/heartwood_rings','side':'darkspawn:item/materials/heartwood_bark'}}
write(A/'models/block/heartwood_log.json',value)
write(A/'models/block/heartwood_log_horizontal.json',dict(value,parent='minecraft:block/cube_column_horizontal'))
path=A/'blockstates/heartwood_log.json';write(path,replace_refs(json.loads(path.read_text()),'minecraft:block/stripped_oak_log','darkspawn:block/heartwood_log'))
write(A/'items/heartwood_log.json',{'model':{'type':'minecraft:model','model':'darkspawn:block/heartwood_log'}})
seedling=[b(7,0,7,9,9,9,'wood'),b(4,6,6,8,8,10,'green'),b(8,8,6,12,10,10,'green'),b(6,10,6,10,13,10,'green'),b(7,7,6.5,9,11,7,'glow')]
make('heartwood_seedling',seedling)
write(A/'models/block/heartwood_seedling.json',models['heartwood_seedling'])
write(A/'blockstates/heartwood_seedling.json',{'variants':{'':{'model':'darkspawn:block/heartwood_seedling'}}})

# Block items: reuse their authored shapes with independent inventory materials.
# Placed machinery, portal frames and ward animations keep their existing world appearance.
def resolve_local(name):
    value=json.loads((A/f'models/{name}.json').read_text())
    parent=value.get('parent','')
    if parent.startswith('darkspawn:'):
        inherited=resolve_local(parent.split(':',1)[1]);inherited.update({k:v for k,v in value.items() if k not in {'parent','textures'}})
        inherited['textures'].update(value.get('textures',{}));return inherited
    return value
def material(texture):
    if texture.endswith(('campfire_fire','fire_0','fire_1')):return 'flame'
    if 'glass' in texture:return 'glass'
    if 'cartography' in texture:return 'chart'
    if 'planks' in texture or 'log' in texture:return 'wood'
    if 'copper' in texture or 'gold' in texture:return 'gold'
    if 'iron' in texture:return 'iron'
    if 'purple' in texture:return 'purple'
    if 'cyan' in texture or 'prismarine' in texture:return 'green'
    if 'orange' in texture or 'fire' in texture:return 'ember'
    if 'sea_lantern' in texture or 'glowstone' in texture:return 'glow'
    return 'dark' if 'black' in texture or 'deepslate' in texture else 'stone'
for path in sorted((A/'items').glob('*.json')):
    definition=json.loads(path.read_text());ref=definition.get('model',{}).get('model','')
    # Re-running also refreshes the generated item form from its block source.
    block=A/f'models/block/{path.stem}.json'
    if isinstance(ref,str) and ref.startswith('darkspawn:block/'):
        source=ref.split(':',1)[1]
    elif path.stem in {'barracks_bonfire','bank_chest','dump_chest','sorting_chest','sorting_tube','harbor_contract_board','harbor_return','sea_gate','naval_cannon','shipyard','ship_helm','quarantine_archive','quarantine_gate','quarantine_return'}:
        source='block/'+('barracks_bonfire_0' if path.stem=='barracks_bonfire' else path.stem)
    else:continue
    if not (A/f'models/{source}.json').exists():continue
    value=resolve_local(source)
    if not any(v.startswith('minecraft:') for v in value.get('textures',{}).values()):continue
    for key,texture in value['textures'].items():
        if texture.startswith('minecraft:'):value['textures'][key]='darkspawn:item/materials/'+material(texture)
    write(A/f'models/item/{path.stem}.json',value);models[path.stem]=value;item(path.stem)
write(ROOT/'art/items/distinct-models.json',{'authoring':'tools/assets/distinct-item-models.py','models':list(models),'materials':PALETTE})
print(f'Authored {len(models)} item model states and {len(PALETTE)} original materials; selectors retained.')
