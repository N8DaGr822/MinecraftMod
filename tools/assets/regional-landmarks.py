"""Author the fourteen regional discovery landmarks using the native landmark pipeline."""
from pathlib import Path
import gzip
import json
import math
import runpy

BASE = runpy.run_path(str(Path(__file__).with_name('boss-structures.py')))
ROOT, RES, write, tag = (BASE[k] for k in ('ROOT', 'RES', 'write', 'tag'))
# Stable IDs, boss biome tags, palette, altar, ordinary supplies, and accurate ritual clues.
SITES = {
 'farm_excavation': ('mutant_zombie', 'stone_bricks', 'coarse_dirt', 'hay_block', ['bread','rotten_flesh','iron_nugget'], 'Use the Mutant Zombie summon on hay in plains or meadows, under open sky. Reserve an 8-block-wide, 14-block-high space.'),
 'fossil_altar': ('fossil_tyrant', 'sandstone', 'cut_sandstone', 'bone_block', ['bone','bread','torch'], 'Use the Fossil Tyrant summon on bone in desert or badlands, under open sky. Reserve a 12-block-wide, 14-block-high space.'),
 'storm_altar': ('thunder_bird', 'cut_copper', 'stone_bricks', 'copper_block', ['feather','copper_ingot','bread'], 'Wait for a thunderstorm. Use the Thunder Bird summon on fresh copper in savanna under open sky. It appears eight blocks above the altar; keep the flight space clear.'),
 'serpent_sanctuary': ('titan_boa', 'mossy_stone_bricks', 'moss_block', 'moss_block', ['vine','melon_slice','bread'], 'Use the Titan Boa summon on moss in jungle under open sky. Reserve a 14-block-wide, seven-block-high space.'),
 'witch_hut': ('baba_yaga', 'dark_oak_planks', 'mud_bricks', 'cauldron', ['glass_bottle','sugar','red_mushroom'], 'At night (13000-22999), use the Baba Yaga summon on the outdoor cauldron in swamp. Keep a 10-block-wide, 15-block-high space open to the sky.'),
 'quarry_shrine': ('mountain_titan', 'stone_bricks', 'polished_andesite', 'chiseled_stone_bricks', ['iron_nugget','torch','bread'], 'Use the Mountain Titan summon on chiseled stone bricks in its mountain habitat under open sky. Reserve a 12-block-wide, 20-block-high space.'),
 'frozen_nest': ('ice_wyrm', 'packed_ice', 'snow_block', 'blue_ice', ['snowball','cooked_cod','coal'], 'Use the Ice Wyrm summon on blue ice in frozen lands under open sky. Reserve a 14-block-wide, 10-block-high space.'),
 'expedition_wreck': ('kraken', 'dark_oak_planks', 'prismarine', 'prismarine', ['cooked_cod','ink_sac','kelp'], 'Return to the surface. Cast the Kraken summon toward open ocean water, with seven blocks of depth across its sampled area and room ahead of your boat. The wreck is a clue site, not an underwater altar.'),
 'webbed_outpost': ('cave_crawler', 'deepslate_bricks', 'cobbled_deepslate', 'cobweb', ['string','torch','bread'], 'Use the Cave Crawler summon on cobweb underground in its cave habitat, or below Y=48 outside Deep Dark. Reserve a 10-block-wide, five-block-high space.'),
 'deep_dark_hive': ('shadow_creeper_queen', 'deepslate_tiles', 'sculk', 'sculk', ['echo_shard','coal','bone'], 'Use the Shadow Creeper Queen summon on sculk in covered Deep Dark. Reserve a 12-block-wide, 18-block-high space. The open southern route is your retreat.'),
 'fungal_court': ('mycelial_sovereign', 'mushroom_stem', 'mycelium', 'mycelium', ['red_mushroom','brown_mushroom','bowl'], 'Use the Mycelial Sovereign summon on mycelium in mushroom fields under open sky. Reserve a 14-block-wide, 20-block-high space.'),
 'ember_shrine': ('netherborn', 'polished_blackstone_bricks', 'basalt', 'magma_block', ['nether_brick','gold_nugget','magma_cream'], 'Use the Netherborn summon on magma in crimson or warped forest, Nether wastes, or basalt deltas. Reserve a 12-block-wide, 14-block-high space. Prepare for fire.'),
 'soul_reliquary': ('soulbound_colossus', 'polished_blackstone_bricks', 'soul_soil', 'soul_sand', ['bone','soul_torch','coal'], 'Use the Soulbound Colossus summon on soul sand in Soul Sand Valley. Reserve a 12-block-wide, 22-block-high space. Keep the central procession route open.'),
 'end_observatory': ('void_eye', 'purpur_block', 'end_stone_bricks', 'obsidian', ['chorus_fruit','ender_pearl','end_stone'], 'Use the Void Eye summon on obsidian on an outer End island. It appears eight blocks above the altar and needs a 10-block-wide, 10-block-high space. Stay on supported ground.'),
}

def template(site, variant):
    boss, wall, floor, altar, supplies, clue = SITES[site]
    water = site == 'expedition_wreck'
    blocks = {}
    def put(x,y,z,name,props=None,nbt=None):
        assert 0 <= x < 25 and 0 <= z < 25 and 0 <= y < 28, (site,x,y,z)
        blocks[x,y,z] = ({'Name':'minecraft:'+name, **({'Properties':props} if props else {})}, nbt)
    def fill(a,b,name,props=None):
        for x in range(a[0],b[0]+1):
            for y in range(a[1],b[1]+1):
                for z in range(a[2],b[2]+1): put(x,y,z,name,props)
    # Ritual Clearance: A 15x15 central square stays unobstructed above the flush altar.
    for x in range(25):
        for z in range(25):
            if math.hypot(x-12,z-12)>12.5 and not (abs(x-12)<=7 and abs(z-12)<=7): continue
            fill((x,0,z),(x,2,z), 'prismarine' if water else wall)
            put(x,3,z,floor)
            fill((x,4,z),(x,12 if water else 27,z),'water' if water else 'air')
    # A three-wide, level southern approach never contains webs, fire, cages, or mandatory jumps.
    for x in range(5,20):
        for z in range(5,20):
            distance=math.hypot(x-12,z-12)
            if 5.5 <= distance <= 6.5 and (x+z+variant)%7 != 0:
                put(x,3,z,wall)
    fill((11,3,12),(13,3,24), 'prismarine' if water else wall)
    for x,z in [(3,7),(3,17),(21,7),(21,17)]:
        fill((x,4,z),(x,6+(variant+x+z)%3,z),wall)
    if site == 'farm_excavation':
        # Ruined north farmhouse, open cellar steps, and perimeter graves.
        fill((6,4,1),(18,6,2),'oak_planks')
        fill((6,4,1),(6,8,4),'oak_log',{'axis':'y'})
        fill((17,7,1),(19,7,4),'oak_slab',{'type':'bottom','waterlogged':'false'})
        for x in (2,21):
            for z in (10,13,16):
                fill((x,3,z),(x+1,3,z+1),'coarse_dirt'); put(x,4,z,'cobblestone_wall')
        for n in range(3):
            fill((8+n,1+n,3),(8+n,3,4),'air'); put(8+n,n,3,'stone_bricks')
    elif site == 'fossil_altar':
        for z in (5,10,15,20):
            for x,sign in ((2,1),(22,-1)):
                fill((x,4,z),(x,7+variant%2,z),'bone_block',{'axis':'y'})
                put(x+sign,8,z,'bone_block',{'axis':'x'}); put(x+2*sign,9,z,'bone_block',{'axis':'x'})
        fill((8,7,1),(16,7,3),'white_wool')
        for x in (8,16): fill((x,4,2),(x,6,2),'oak_fence')
    elif site == 'storm_altar':
        for x in (3,21):
            fill((x,4,3),(x,10+variant,3),'cut_copper')
            put(x,11+variant,3,'lightning_rod',{'facing':'up','waterlogged':'false','powered':'false'})
        for x in range(6,19): put(x,3,3,'waxed_cut_copper')
        for z in (4,20): fill((8,3,z),(16,3,z),'waxed_cut_copper')
    elif site == 'serpent_sanctuary':
        for x in (3,21):
            fill((x,4,2),(x,8,4),'mossy_stone_bricks')
            fill((x-1,9,2),(x+1,9,4),'chiseled_stone_bricks')
        for x in range(6,19):
            z=2+(x+variant)%3
            put(x,4,z,'green_terracotta'); put(x,5,z,'moss_block')
        for z in range(6,20): put(2,4,z,'jungle_leaves',{'persistent':'true','distance':'1','waterlogged':'false'})
    elif site == 'witch_hut':
        fill((7,4,1),(17,7,1),'dark_oak_planks')
        fill((7,4,1),(7,7,4),'dark_oak_planks'); fill((17,4,1),(17,7,4),'dark_oak_planks')
        fill((6,8,0),(18,8,4),'dark_oak_planks')
        fill((8,9,0),(16,9,3),'dark_oak_planks'); fill((10,10,1),(14,10,2),'dark_oak_planks')
        put(8,4,3,'brewing_stand'); put(16,4,3,'flower_pot')
    elif site == 'quarry_shrine':
        for x in (2,20):
            fill((x,4,1),(x+2,10+variant,3),'stone_bricks')
            fill((x+1,7,2),(x+2,8,3),'air')
        for z in (8,12,16): fill((1,4,z),(3,5,z+1),'chiseled_stone_bricks')
        fill((8,4,1),(16,4,2),'polished_andesite')
    elif site == 'frozen_nest':
        for x,z in ((3,3),(21,3),(3,20),(21,20)):
            fill((x,4,z),(x+1,6,z+1),'packed_ice')
            put(x,7,z,'calcite'); put(x+1,7,z+1,'snow_block')
        for x in range(6,19):
            put(x,4,2,'bone_block',{'axis':'x'})
            if (x+variant)%3==0: put(x,5,2,'blue_ice')
    elif site == 'expedition_wreck':
        # Open hull sides, broken deck, and no ceiling over the central ascent column.
        for x in (2,21):
            fill((x,4,4),(x+1,6,20),'dark_oak_planks')
            fill((x,4,10),(x+1,5,13),'water')
        fill((4,4,2),(20,5,3),'dark_oak_planks')
        fill((4,6,3),(20,6,4),'dark_oak_planks')
        fill((3,4,5),(3,11,5),'stripped_dark_oak_log',{'axis':'y'})
        fill((3,10,5),(7+variant,10,5),'stripped_dark_oak_log',{'axis':'x'})
        fill((6,3,8),(18,3,19),'dark_oak_planks')
    elif site == 'webbed_outpost':
        for x in (3,21):
            for z in (3,10,18):
                fill((x,4,z),(x,7,z),'oak_log',{'axis':'y'})
                put(x,8,z,'oak_planks')
                if z != 10: put(x+(-1 if x==3 else 1),7,z,'cobweb')
        fill((6,4,2),(18,4,3),'oak_planks')
        for x in (7,17): put(x,5,2,'lantern',{'hanging':'false','waterlogged':'false'})
    elif site == 'deep_dark_hive':
        for x,z in ((2,4),(21,4),(2,19),(21,19)):
            fill((x,4,z),(x+1,8+variant,z+1),'sculk')
            put(x,9+variant,z,'sculk_catalyst')
        for x in range(7,18): put(x,4,2,'reinforced_deepslate')
    elif site == 'fungal_court':
        for x,z in ((3,3),(21,3),(3,20),(21,20)):
            fill((x,4,z),(x,7+variant%2,z),'mushroom_stem')
            fill((x-1,8+variant%2,z-1),(x+1,8+variant%2,z+1),'red_mushroom_block' if x==3 else 'brown_mushroom_block')
        for x in range(7,18): put(x,4,2,'red_mushroom' if x%2 else 'brown_mushroom')
    elif site == 'ember_shrine':
        for x in (2,21):
            fill((x,4,2),(x+1,9+variant,4),'polished_basalt')
            put(x,10+variant,3,'shroomlight')
        fill((6,4,1),(18,6,2),'chiseled_polished_blackstone')
        for x in (8,12,16): put(x,7,1,'gilded_blackstone')
    elif site == 'soul_reliquary':
        for x in (3,21):
            for z in (4,10,17,21):
                fill((x,4,z),(x,7+(z+variant)%2,z),'polished_blackstone_bricks')
                put(x,8+(z+variant)%2,z,'soul_lantern',{'hanging':'false','waterlogged':'false'})
        fill((7,4,1),(17,5,2),'chiseled_polished_blackstone')
        for x in (8,12,16): put(x,6,1,'bone_block')
    elif site == 'end_observatory':
        for x in (3,21):
            fill((x,4,2),(x,11,2),'purpur_pillar',{'axis':'y'})
            fill((x-1,12,2),(x+1,12,2),'purpur_block')
        for x in range(7,18):
            if (x+variant)%4: put(x,7+int(abs(x-12)/2),2,'purpur_block')
        fill((11,10,2),(13,12,2),'purple_stained_glass')
        for x in (4,20): put(x,4,19,'end_rod',{'facing':'up'})
    # Each variant has a different surviving perimeter monument and provision niche.
    chest=(2 if variant != 1 else 22,4,12)
    put(*chest,'chest',{'facing':'east' if chest[0]==2 else 'west','type':'single','waterlogged':str(water).lower()}, {'id':'minecraft:chest','LootTable':'darkspawn:chests/'+site})
    put(chest[0],5,chest[2], 'water' if water else 'air')
    put(12,3,12,altar)
    # Monument Foundations: Perimeter ruins outside the round court still receive bounded support.
    for (x,y,z),(state,_) in list(blocks.items()):
        if y==4 and state['Name'] not in ('minecraft:air','minecraft:water') and (x,3,z) not in blocks:
            fill((x,0,z),(x,3,z),wall)
    palette=[]; lookup={}; entries=[]
    for pos,(state,nbt) in sorted(blocks.items(),key=lambda e:(e[0][1],e[0][0],e[0][2])):
        key=json.dumps(state,sort_keys=True)
        if key not in lookup: lookup[key]=len(palette); palette.append(state)
        entry={'pos':list(pos),'state':lookup[key]}
        if nbt: entry['nbt']=nbt
        entries.append(entry)
    native=[{'id':s['Name'],**({'properties':s['Properties']} if 'Properties' in s else {})} for s in palette]
    obj={'DataVersion':5023,'author':'Darkspawn','size':[25,28,25],'palette':native,'blocks':entries,'entities':[]}
    dest=RES/f'structure/landmarks/{site}_{variant}.nbt'; dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(gzip.compress(b'\x0a\x00\x00'+tag(obj)[1],mtime=0))
    write(ROOT/f'art/structures/{site}_{variant}.json',{'size':obj['size'],'anchor':[12,3,12],'chest':chest,'palette':palette,'blocks':[e for e in entries if palette[e['state']]['Name'] not in ('minecraft:air','minecraft:water') and e['pos'][1]>=3]})

def generate():
    for index,(site,(boss,wall,floor,altar,supplies,clue)) in enumerate(SITES.items()):
        write(RES/f'worldgen/structure/{site}.json',{'type':'darkspawn:boss_landmark','site':site,'biomes':'#darkspawn:has_structure/'+site,'spawn_overrides':{},'step':'top_layer_modification','terrain_adaptation':'none'})
        placement={'type':'minecraft:random_spread','spacing':40,'separation':16,'salt':73194001+index*8191}
        if site in ('farm_excavation','deep_dark_hive'):
            placement['exclusion_zone']={'other_set':'minecraft:villages' if site=='farm_excavation' else 'minecraft:ancient_cities','chunk_count':12}
        write(RES/f'worldgen/structure_set/{site}.json',{'structures':[{'structure':'darkspawn:'+site,'weight':1}],'placement':placement})
        write(RES/f'tags/worldgen/biome/has_structure/{site}.json',{'replace':False,'values':['#darkspawn:'+boss+'_biomes']})
        preparation="Combine this region's discovery material with dragon's breath for an alternative summon recipe."
        if boss=='netherborn': preparation="Combine all three regional organs (Crimson, Warped, Infernal) with dragon's breath for the alternative summon recipe."
        if boss=='thunder_bird': preparation="Use a Charged Feather on exposed fresh copper during thunder to make a Stormforged Feather. Combine that with dragon's breath for the alternative summon recipe."
        pages=[f'{site.replace("_"," ").title()}\n\nThis site can be explored before the first Ender Dragon victory. The boss ritual requires that world victory and never starts by itself.',
               'Preparation\n\n'+preparation+' Existing summon recipes still work. These supplies are provisions, not victory rewards.',clue]
        book={'type':'minecraft:item','name':'minecraft:written_book','modifier':{'type':'minecraft:set_components','components':{'minecraft:written_book_content':{'title':site.replace('_',' ').title(),'author':'An expedition survivor','pages':[{'text':p} for p in pages],'resolved':True}}}}
        items=[{'type':'minecraft:item','name':'minecraft:'+item,'modifier':{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}} for item in supplies]
        write(RES/f'loot_table/chests/{site}.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[book]},{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':items}],'random_sequence':'darkspawn:chests/'+site})
        for variant in range(3): template(site,variant)
        print(site)
    write(RES/'tags/worldgen/structure/boss_landmarks.json',{'replace':False,'values':['darkspawn:druid_shrine','darkspawn:hunter_camp']+['darkspawn:'+s for s in SITES]})

if __name__=='__main__': generate()
