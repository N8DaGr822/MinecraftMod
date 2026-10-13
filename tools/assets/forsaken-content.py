"""Forsaken authored assets: distinct cuboid rigs, quarantine gear, finite loot and recipes."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'
PALETTE = [(94, 109, 81), (154, 164, 113), (72, 82, 63), (57, 51, 48),
           (190, 145, 48), (166, 211, 82), (31, 42, 43), (107, 66, 69)]

def write(path, value):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

def asset(path, value):
    write('src/main/resources/assets/darkspawn/' + path, value)

def data(path, value):
    write('src/main/resources/data/darkspawn/' + path, value)

def atlas(path, palette):
    image = Image.new('RGBA', (256, 128))
    for y in range(128):
        for x in range(256):
            color = palette[x // 64 + (y // 64) * 4]
            shade = ((x * 7 + y * 13) % 17 - 8) // 2
            image.putpixel((x, y), tuple(max(0, min(255, c + shade)) for c in color) + (255,))
    target = RES / 'assets/darkspawn' / path
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)

def cube(origin, size, material):
    return dict(origin=origin, size=size, uv=[material % 4 * 64, material // 4 * 64])

ROSTER = [('infected_wanderer', 30, 10), ('infected_crawler', 10, 18), ('spore_spitter', 27, 12), ('assimilated_brute', 41, 20)]
for name, height, width in ROSTER:
    crawler = name == 'infected_crawler'
    brute = name == 'assimilated_brute'
    bones = [dict(name='root', pivot=[0, 0, 0]), dict(name='body', parent='root', pivot=[0, height * .5, 0],
             cubes=[cube([-width * .35, height * .35, -width * .25], [width * .7, height * .4, width * .5], 0)])]
    bones += [dict(name='head_look', parent='body', pivot=[0, height * .75, -3]),
              dict(name='head', parent='head_look', pivot=[0, height * .75, -3], cubes=[
                  cube([-4, height * .72, -7 if not crawler else -12], [8, height * .28, 8], 1),
                  cube([-3, height * .88, -7.3 if not crawler else -12.3], [6, 1.5, .5], 5)])]
    for side, sign in [('left', 1), ('right', -1)]:
        arm_x = sign * width * .48
        bones.append(dict(name=side + '_arm', parent='body', pivot=[arm_x, height * .67, 0], cubes=[
            cube([arm_x - 2, height * .2, -2 if not crawler else -9], [4, height * .49, 4 if not crawler else 11], 0)]))
        leg_x = sign * width * .2
        bones.append(dict(name=side + '_leg', parent='root', pivot=[leg_x, height * .36, 0], cubes=[
            cube([leg_x - 2, 0, -2 if not crawler else 2], [4, height * .38, 4 if not crawler else 9], 3)]))
    body = bones[1]['cubes']
    if crawler:
        body += [cube([-6, 5, 2], [12, 5, 12], 2), cube([-4, 9, 6], [8, 2, 6], 5)]
    elif name == 'spore_spitter':
        body += [cube([-6, 12, 3], [12, 11, 7], 2), cube([-4, 21, 4], [8, 5, 7], 5), cube([-2, 21, -9], [4, 4, 6], 7)]
    elif brute:
        body += [cube([-12, 25, -5], [8, 8, 10], 3), cube([4, 25, -5], [8, 8, 10], 3), cube([-7, 17, -6], [14, 8, 2], 4), cube([6, 32, 0], [5, 5, 5], 5)]
    else:
        body += [cube([-4, 11, -3], [8, 7, 6], 4), cube([2, 22, 1], [5, 5, 5], 5)]
    asset('geckolib/models/entity/' + name + '.geo.json', {'format_version': '1.12.0', 'minecraft:geometry': [dict(
        description=dict(identifier='geometry.' + name, texture_width=256, texture_height=128, visible_bounds_width=5, visible_bounds_height=5, visible_bounds_offset=[0, 1.5, 0]), bones=bones)]})
    def pose(bone, axis, frames): return {bone: {axis: {str(t): value for t, value in frames}}}
    animations = {}
    def animation(state, duration, parts, loop=False):
        animations['animation.' + name + '.' + state] = dict(animation_length=duration, loop=loop, bones=parts)
    animation('idle', 3, pose('body', 'rotation', [(0,[2,0,0]),(1.5,[-2,0,0]),(3,[2,0,0])]), True)
    walk = {}
    for bone, sign in [('left_arm',-1),('right_arm',1),('left_leg',1),('right_leg',-1)]:
        walk.update(pose(bone, 'rotation', [(0,[22*sign,0,0]),(.4,[-22*sign,0,0]),(.8,[22*sign,0,0])]))
    animation('walk', .8, walk, True)
    animation('warning', 1.2, {'body': {'rotation': {'0': [0,0,0], '.4': [-12,0,0], '1.2': [-12,0,0]}}, 'left_arm': {'rotation': {'0': [0,0,0], '.4': [-65,0,-10], '1.2': [-65,0,-10]}}}, True)
    animation('attack', .5, pose('body', 'rotation', [(0,[-12,0,0]),(.16,[25,0,0]),(.5,[0,0,0])]))
    animation('death', 1, pose('root', 'rotation', [(0,[0,0,0]),(1,[0,0,85])]))
    animation('active', 1, {}, True)
    animation('hide', 1, {})
    asset('geckolib/animations/entity/' + name + '.animation.json', dict(format_version='1.8.0', geckolib_format_version=2, animations=animations))
    atlas('textures/entity/' + name + '.png', PALETTE)

# Quarantine Geometry: Sealed hood, dark visor, twin respirator filters and a compact rear scrubber.
parts = []
def part(name, origin, boxes):
    parts.append(dict(name=name, origin=origin, cubes=[dict(slot=slot, **{'from': start}, size=size, material=material) for slot,start,size,material in boxes]))
part('head', [0,0,0], [('helmet',[-4.4,-8.4,-4.4],[8.8,8.8,8.8],4), ('helmet',[-3.6,-7.2,-4.8],[7.2,3.1,.5],6),
    ('helmet',[-3.7,-3.1,-5.5],[2.5,2.4,1.5],2),('helmet',[1.2,-3.1,-5.5],[2.5,2.4,1.5],2)])
part('body', [0,0,0], [('chestplate',[-4.5,-.4,-2.5],[9,12.8,5],4),('chestplate',[-3.5,1.5,2.5],[7,8,3],6),
    ('chestplate',[-2,2,-2.9],[4,3,.5],5),('leggings',[-4.5,10,-2.5],[9,2.5,5],6)])
for side, sign in [('right',-1),('left',1)]:
    part(side+'_arm',[sign*5,2,0],[('chestplate',[-2.5,-2.5,-2.5],[5,12,5],4),('chestplate',[-2.6,6,-2.6],[5.2,2,5.2],6)])
    part(side+'_leg',[sign*1.9,12,0],[('leggings',[-2.3,-.2,-2.3],[4.6,8.4,4.6],4),('boots',[-2.5,7.5,-2.8],[5,4.8,5.3],6)])
asset('geometry/armor/quarantine.json', dict(parts=parts))
atlas('textures/equipment/quarantine.png', PALETTE)
asset('equipment/quarantine.json', {'layers': {'humanoid': [{'texture':'minecraft:iron'}], 'humanoid_leggings':[{'texture':'minecraft:iron'}]}})

names = ['contaminated_alloy','biofiber','parasitic_core','research_data','quarantine_module','bio_scanner','pulse_blade'] + ['quarantine_'+slot for slot in ['helmet','chestplate','leggings','boots']]
for index,name in enumerate(names):
    image=Image.new('RGBA',(32,32)); draw=ImageDraw.Draw(image)
    if name=='pulse_blade':
        draw.polygon([(9,21),(22,3),(28,2),(27,8),(14,24)],fill=PALETTE[1],outline=PALETTE[6]);draw.line((12,21,25,5),fill=PALETTE[5],width=2)
        draw.line((7,19,17,27),fill=PALETTE[4],width=3);draw.line((11,24,5,30),fill=PALETTE[6],width=4)
    elif name=='biofiber':
        for x in [8,14,20]: draw.line([(x,27),(x-3,20),(x+3,10),(x,4)],fill=PALETTE[5],width=3)
    elif name=='parasitic_core':
        draw.ellipse((5,5,27,27),fill=PALETTE[7],outline=PALETTE[2],width=3);draw.ellipse((11,9,21,23),fill=PALETTE[5])
    elif name=='research_data':
        draw.rectangle((6,3,25,29),fill=(208,217,192),outline=PALETTE[6],width=2)
        for y in [9,14,19,24]:draw.line((10,y,21,y),fill=PALETTE[2],width=2)
    elif name in ['bio_scanner','quarantine_module']:
        draw.rounded_rectangle((5,3,26,29),radius=3,fill=PALETTE[4],outline=PALETTE[6],width=3)
        draw.rectangle((9,7,22,21),fill=PALETTE[6]);draw.ellipse((12,10,19,17),outline=PALETTE[5],width=2);draw.rectangle((11,24,15,26),fill=PALETTE[5])
    elif name.endswith('helmet'):
        draw.rounded_rectangle((5,3,27,28),radius=6,fill=PALETTE[4]);draw.rectangle((8,9,24,16),fill=PALETTE[6]);draw.rectangle((7,20,12,25),fill=PALETTE[2]);draw.rectangle((20,20,25,25),fill=PALETTE[2])
    elif name.endswith('chestplate'):
        draw.polygon([(5,4),(12,4),(13,8),(19,8),(21,4),(27,4),(30,14),(24,16),(24,29),(8,29),(8,16),(2,14)],fill=PALETTE[4]);draw.rectangle((12,13,20,19),fill=PALETTE[5])
    elif name.endswith('leggings'):
        draw.rectangle((6,3,26,9),fill=PALETTE[6]);draw.rectangle((6,9,14,28),fill=PALETTE[4]);draw.rectangle((18,9,26,28),fill=PALETTE[4])
    elif name.endswith('boots'):
        for x in [3,18]:draw.rectangle((x+2,7,x+10,23),fill=PALETTE[6]);draw.rectangle((x,22,x+10,28),fill=PALETTE[2]);draw.line((x+2,9,x+10,9),fill=PALETTE[4],width=3)
    else:
        draw.polygon([(3,19),(8,9),(25,6),(29,17),(23,25),(8,27)],fill=PALETTE[1],outline=PALETTE[6]);draw.line((8,10,12,20,25,17),fill=PALETTE[5],width=2)
    target=RES/'assets/darkspawn/textures/item'/f'{name}.png';target.parent.mkdir(parents=True,exist_ok=True);image.save(target)
    asset('items/'+name+'.json',dict(model=dict(type='minecraft:model',model='darkspawn:item/'+name)))
    asset('models/item/'+name+'.json',dict(parent='minecraft:item/generated',textures=dict(layer0='darkspawn:item/'+name)))

asset('models/item/pulse_blade.json',dict(parent='minecraft:item/handheld',textures=dict(layer0='darkspawn:item/pulse_blade')))
for active in [False,True]:
    texture=Image.new('RGB',(16,16),PALETTE[6]);d=ImageDraw.Draw(texture)
    d.rectangle((0,0,15,2),fill=PALETTE[4]);d.rectangle((0,13,15,15),fill=PALETTE[4]);d.ellipse((4,4,11,11),fill=PALETTE[5] if active else PALETTE[2])
    texture.save(RES/'assets/darkspawn/textures/block'/('containment_beacon'+('_active' if active else '')+'.png'))
    boxes=[([0,0,0],[16,3,16]),([3,3,3],[13,12,13]),([2,12,2],[14,14,14]),([5,14,5],[11,16,11])]
    model=dict(parent='minecraft:block/block',textures=dict(particle='darkspawn:block/containment_beacon',all='darkspawn:block/containment_beacon'+('_active' if active else '')),
               elements=[{'from':start,'to':end,'faces':{face:dict(texture='#all') for face in ['north','south','east','west','up','down']}} for start,end in boxes])
    asset('models/block/containment_beacon'+('_active' if active else '')+'.json',model)
asset('blockstates/containment_beacon.json',dict(variants={'active=false':dict(model='darkspawn:block/containment_beacon'),'active=true':dict(model='darkspawn:block/containment_beacon_active')}))
asset('items/containment_beacon.json',dict(model=dict(type='minecraft:model',model='darkspawn:block/containment_beacon')))
data('loot_table/blocks/containment_beacon.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='darkspawn:containment_beacon')])]))
data('recipe/containment_beacon.json',dict(type='minecraft:crafting_shaped',pattern=['AMA','RDR','AMA'],key={'A':'darkspawn:contaminated_alloy','M':'darkspawn:quarantine_module','R':'minecraft:redstone','D':'darkspawn:research_data'},result=dict(id='darkspawn:containment_beacon')))
data('recipe/pulse_blade.json',dict(type='minecraft:smithing_transform',template='darkspawn:quarantine_module',base='minecraft:iron_sword',addition='darkspawn:parasitic_core',result=dict(id='darkspawn:pulse_blade')))
for tag in ['swords','enchantable/durability']:
    path=RES/f'data/minecraft/tags/item/{tag}.json'
    content=json.loads(path.read_text(encoding='utf-8')) if path.exists() else dict(replace=False,values=[])
    if 'darkspawn:pulse_blade' not in content['values']:content['values'].append('darkspawn:pulse_blade')
    write('src/main/resources/data/minecraft/tags/item/'+tag+'.json',content)

effect=Image.new('RGBA',(18,18)); d=ImageDraw.Draw(effect); d.ellipse((3,3,15,15),outline=PALETTE[5],width=3); d.line((4,4,14,14),fill=PALETTE[5],width=2)
effect_path=RES/'assets/darkspawn/textures/mob_effect/contamination.png';effect_path.parent.mkdir(parents=True,exist_ok=True);effect.save(effect_path)
nest=Image.new('RGB',(16,16),PALETTE[7]);d=ImageDraw.Draw(nest)
for x,y,r in [(3,4,3),(11,5,4),(7,12,4)]:d.ellipse((x-r,y-r,x+r,y+r),fill=PALETTE[2],outline=PALETTE[1]);d.ellipse((x-1,y-1,x+1,y+1),fill=PALETTE[5])
nest.save(RES/'assets/darkspawn/textures/block/infected_nest.png')
asset('models/block/infected_nest.json',dict(parent='minecraft:block/cube_all',textures=dict(all='darkspawn:block/infected_nest')))
asset('blockstates/infected_nest.json',dict(variants={'':dict(model='darkspawn:block/infected_nest')}))
data('loot_table/blocks/infected_nest.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='darkspawn:parasitic_core')])]))
data('recipe/bio_scanner.json',dict(type='minecraft:crafting_shaped',pattern=[' A ','DCD',' R '],key={'A':'darkspawn:contaminated_alloy','D':'darkspawn:research_data','C':'darkspawn:parasitic_core','R':'minecraft:redstone'},result=dict(id='darkspawn:bio_scanner')))
data('recipe/quarantine_module.json',dict(type='minecraft:crafting_shaped',pattern=['BAB','DCD','BAB'],key={'A':'darkspawn:contaminated_alloy','B':'darkspawn:biofiber','D':'darkspawn:research_data','C':'darkspawn:parasitic_core'},result=dict(id='darkspawn:quarantine_module')))
for slot in ['helmet','chestplate','leggings','boots']:
    data('recipe/quarantine_'+slot+'.json',dict(type='minecraft:smithing_transform',template='darkspawn:quarantine_module',base='minecraft:iron_'+slot,addition='darkspawn:biofiber',result=dict(id='darkspawn:quarantine_'+slot)))

lang_path=RES/'assets/darkspawn/lang/en_us.json'
text=lang_path.read_text(encoding='utf-8')
translations={'item.darkspawn.'+name:name.replace('_',' ').title() for name in names}
translations.update({'entity.darkspawn.'+name:name.replace('_',' ').title() for name,_,_ in ROSTER})
translations.update({'block.darkspawn.infected_nest':'Infected Nest','effect.darkspawn.contamination':'Contamination',
 'block.darkspawn.containment_beacon':'Containment Beacon',
 'message.darkspawn.forsaken_beacon_dimension':'Containment beacons can only be powered in the Forsaken.',
 'message.darkspawn.forsaken_beacon_active':'Containment active: %s seconds remaining. Radius 16 blocks; overlaps do not stack.',
 'message.darkspawn.forsaken_beacon_empty':'Inactive. Use one Parasitic Core for five minutes of containment.',
 'message.darkspawn.forsaken_scanner_dimension':'The Bio-Scanner scans only in the Forsaken. Sneak-use changes modes.',
 'message.darkspawn.forsaken_scan_mode':'Scanner mode: %s',
 'message.darkspawn.forsaken_resources_empty':'No dropped Forsaken resources or unopened cache signals within 32 loaded blocks.',
 'message.darkspawn.forsaken_resource_signal':'%s: %s, %s, %s (%s blocks)',
 'message.darkspawn.forsaken_pressure_outside':'Outside the active city footprint. No local infestation reading.',
 'message.darkspawn.forsaken_pressure_scan':'Pressure %s/100 | %s district | surge remaining: %s seconds',
 'scanner.darkspawn.mode_0':'Nests', 'scanner.darkspawn.mode_1':'Resources and caches', 'scanner.darkspawn.mode_2':'Local infestation',
 'scanner.darkspawn.resource':'Dropped resource', 'scanner.darkspawn.cache':'Unopened Forsaken cache',
 'scanner.darkspawn.infested':'Infested', 'scanner.darkspawn.abandoned':'Abandoned',
 'tooltip.darkspawn.pulse_blade':'Hold Use 1.5s, release: 4-block visible-infected pulse, 10 damage, 8s cooldown, 8 durability. Melee: +25% against infected.',
 'tooltip.darkspawn.containment_beacon':'Forsaken only. One core powers a 16-block spawn-suppression field for 5 minutes. No stacking or top-ups; breaking loses fuel.',
 'message.darkspawn.forsaken_scan':'Nest detected: %s, %s, %s (%s blocks)',
 'message.darkspawn.forsaken_scan_empty':'No active nest in loaded terrain within 32 blocks.',
 'message.darkspawn.forsaken_watch':'The Forsaken stirs. Quiet movement lowers local pressure.',
 'message.darkspawn.forsaken_alert':'Local threat rising. Break nests or fall back.',
 'message.darkspawn.forsaken_surge':'Infected surge! Reinforcements are active for a short time.',
 'tooltip.darkspawn.quarantine':'Each piece reduces contamination by 15 seconds. Full sealed set prevents new exposure. Iron-tier armor.',
 'tooltip.darkspawn.bio_scanner':'Sneak-use cycles nests, resources/caches and local pressure. Use scans loaded terrain within 32 blocks. Shared 5-second cooldown.',
 'tooltip.darkspawn.contamination_cure':'Contamination: slowed movement for up to 60 seconds. Honey, milk or extraction cures it.'})
import re
for key,value in translations.items():
    pattern=r'('+re.escape(json.dumps(key))+r'\s*:\s*)"(?:[^"\\]|\\.)*"'
    if re.search(pattern,text):text=re.sub(pattern,lambda m:m.group(1)+json.dumps(value),text)
    else:
        end=text.rfind('}');text=text[:end].rstrip()+',\n\t'+json.dumps(key)+': '+json.dumps(value)+'\n'+text[end:]
lang_path.write_text(text,encoding='utf-8')
print('Forsaken: four rigs, quarantine armor, blade, beacon, scanner and eight recipes.')
