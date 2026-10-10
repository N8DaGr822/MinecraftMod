"""Author six native structure templates and their data resources. No external dependencies.

Coordinates are local Minecraft blocks. Y=3 is the finished ground; center is the
ritual anchor. Explicit air reserves the clearing only during new-chunk generation.
The generated JSON blueprints can also be reviewed outside Minecraft.
"""
from pathlib import Path
import gzip
import json
import math
import random
import struct

ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'src/main/resources/data/darkspawn'

def text(s):
    b=s.encode('utf-8'); return struct.pack('>H',len(b))+b

def tag(value):
    if isinstance(value,int): return 3,struct.pack('>i',value)
    if isinstance(value,str): return 8,text(value)
    if isinstance(value,list):
        entries=[tag(v) for v in value]; kind=entries[0][0] if entries else 10
        assert all(k==kind for k,_ in entries)
        return 9,bytes([kind])+struct.pack('>i',len(entries))+b''.join(v for _,v in entries)
    if isinstance(value,dict):
        return 10,b''.join(bytes([kind])+text(k)+payload for k,v in value.items() for kind,payload in [tag(v)])+b'\0'
    raise TypeError(value)

def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')

def template(site,variant):
    rng=random.Random(8732+variant+(100 if site=='hunter_camp' else 0))
    forest=site=='druid_shrine'; r=15 if forest else 12; size=r*2+1
    blocks={}
    def put(x,y,z,name,props=None,nbt=None):
        assert 0<=x<size and 0<=z<size and 0<=y<48,(site,x,y,z)
        state={'Name':'minecraft:'+name}
        if props: state['Properties']=props
        blocks[x,y,z]=(state,nbt)
    def fill(x1,y1,z1,x2,y2,z2,name,props=None):
        for x in range(x1,x2+1):
            for y in range(y1,y2+1):
                for z in range(z1,z2+1): put(x,y,z,name,props)
    # Irregular round clearing. Square central arena includes the full boss hitbox.
    for x in range(size):
        for z in range(size):
            dist=math.hypot(x-r,z-r)
            core=abs(x-r)<=10 and abs(z-r)<=10 if forest else abs(x-r)<=6 and abs(z-r)<=6
            if dist>r+.25 and not core: continue
            for y in range(3): put(x,y,z,'dirt')
            surface=rng.choice(['grass_block']*7+['moss_block','coarse_dirt'])
            if core: surface=rng.choice(['mossy_stone_bricks','cracked_stone_bricks','stone_bricks'] if forest else ['coarse_dirt','gravel','dirt_path'])
            if forest and 11<=dist<=13: surface=rng.choice(['mossy_cobblestone','cobblestone','andesite'])
            put(x,3,z,surface)
            for y in range(4,48): put(x,y,z,'air')
    if forest:
        # Eight ruined standing stones, all outside the 21x21 canopy clearance.
        for i,(x,z) in enumerate([(3,8),(3,22),(8,3),(22,3),(27,8),(27,22),(8,27),(22,27)]):
            h=3+(i+variant)%4
            fill(x,4,z,x+1,3+h,z+1,'mossy_stone_bricks')
            put(x,4+h,z,'chiseled_stone_bricks')
            if (i+variant)%2==0: put(x+1,4+h,z+1,'moss_block')
        # A broken northern arch and three old roots occupy only the perimeter.
        fill(12,4,1,13,9,2,'stone_bricks'); fill(18,4,1,19,8,2,'mossy_stone_bricks')
        fill(13,10,1,17 if variant!=1 else 15,10,2,'chiseled_stone_bricks')
        for x,z,axis in [(1,14,'z'),(26,2,'x'),(25,26,'x')]:
            for n in range(4): put(x+n if axis=='x' else x,4,z+n if axis=='z' else z,'dark_oak_log',{'axis':axis})
        # Lean-to offering shelter at the west edge; chest opens toward the arena.
        for z in (13,17): fill(1,4,z,1,7,z,'oak_log',{'axis':'y'})
        fill(0,8,12,3,8,18,'mossy_stone_brick_slab',{'type':'bottom','waterlogged':'false'})
        chest=(2,4,15); facing='east'
        # Ring motif is flush with the arena floor; no collision above the anchor.
        for dx,dz in [(0,-2),(2,0),(0,2),(-2,0)]: put(r+dx,3,r+dz,'chiseled_stone_bricks')
        put(r,3,r,'moss_block')
    else:
        # North shelter, deliberately outside the wolf's 10x10 spawn clearance.
        for x in (7,17):
            for z in (1,5): fill(x,4,z,x,7,z,'spruce_log',{'axis':'y'})
        fill(7,4,1,17,5,1,'spruce_planks')
        for x in range(6,19):
            for z in range(0,7):
                if variant==1 and x>14 and z>3: continue
                put(x,8-(abs(z-3)//2),z,'spruce_slab',{'type':'bottom','waterlogged':'false'})
        fill(8,4,2,10,4,3,'brown_wool'); fill(14,4,2,16,4,3,'gray_wool')
        chest=(12,4,2); facing='south'
        # Clawed posts, collapsed fence, supplies, unlit hearth on the eastern side.
        for x,z in [(2,8),(2,16),(22,8),(22,16)]:
            fill(x,4,z,x,6+variant%2,z,'stripped_spruce_log',{'axis':'y'})
            put(x,5,z,'spruce_trapdoor',{'facing':'west' if x==2 else 'east','half':'bottom','open':'true','powered':'false','waterlogged':'false'})
        for z in range(9,16): put(22,4,z,'spruce_fence',{'north':'true','south':'true','east':'false','west':'false','waterlogged':'false'})
        for dx,dz in [(0,0),(-1,0),(1,0),(0,-1),(0,1)]: put(20+dx,3,12+dz,'cobblestone')
        put(20,4,12,'campfire',{'facing':'north','lit':'false','signal_fire':'false','waterlogged':'false'})
        fill(1,4,20,4,4,21,'spruce_log',{'axis':'x'})
        put(3,5,20,'hay_block',{'axis':'y'})
        # Open southern approach and a flush bone ritual anchor.
        for z in range(r,size):
            for x in range(r-1,r+2): put(x,3,z,'gravel')
        put(r,3,r,'bone_block',{'axis':'y'})
    put(*chest,'chest',{'facing':facing,'type':'single','waterlogged':'false'}, {'id':'minecraft:chest','LootTable':'darkspawn:chests/'+site})
    palette=[]; lookup={}; entries=[]
    for pos,(state,nbt) in sorted(blocks.items(),key=lambda e:(e[0][1],e[0][0],e[0][2])):
        key=json.dumps(state,sort_keys=True)
        if key not in lookup: lookup[key]=len(palette); palette.append(state)
        entry={'pos':list(pos),'state':lookup[key]}
        if nbt: entry['nbt']=nbt
        entries.append(entry)
    # 26.3 uses lowercase id/properties, not the older Name/Properties schema.
    native_palette=[{'id':s['Name'],**({'properties':s['Properties']} if 'Properties' in s else {})} for s in palette]
    obj={'DataVersion':5023,'author':'Darkspawn','size':[size,48,size],'palette':native_palette,'blocks':entries,'entities':[]}
    dest=RES/f'structure/landmarks/{site}_{variant}.nbt'; dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(gzip.compress(b'\x0a\x00\x00'+tag(obj)[1],mtime=0))
    # Readable source of the visible build, useful for review and future template editing.
    write(ROOT/f'art/structures/{site}_{variant}.json',{'size':obj['size'],'anchor':[r,3,r],'chest':chest,'palette':palette,'blocks':[e for e in entries if palette[e['state']]['Name']!='minecraft:air' and e['pos'][1]>=3]})
    return len(entries)

def loot(site):
    forest=site=='druid_shrine'
    pages=(['The Overgrown Shrine\n\nThe forest sleeps until this world has defeated its first Ender Dragon. Nothing here summons a boss by itself.',
            'Ancient Heartwood\n\nCraft four logs in the corners, four saplings along the sides, and Dragon\'s Breath in the center. Any logs and saplings work.',
            'The Ritual\n\nUse Ancient Heartwood on the central moss block in a forest. Keep a 21-block square and 28 blocks overhead clear, open to the sky. Bring food and allies. One Tree Spirit at a time nearby.'] if forest else
           ['The Last Hunter\n\nWe tracked a giant wolf through these taigas. The hunt awakens only after this world has defeated its first Ender Dragon.',
            'Moonlit Fang\n\nCraft four bones in the corners, four rabbit hides along the sides, and Dragon\'s Breath in the center.',
            'The Night Hunt\n\nUse the Moonlit Fang on the central bone block at night (13000-22999). Keep a 10-block square and nine blocks overhead clear, open to the sky. One Mutant Wolf at a time nearby.'])
    book={'type':'minecraft:item','name':'minecraft:written_book','modifier':{'type':'minecraft:set_components','components':{'minecraft:written_book_content':{'title':'Druid Shrine Notes' if forest else 'The Last Hunter','author':'A wandering druid' if forest else 'An unknown hunter','pages':[{'text':p} for p in pages],'resolved':True}}}}
    supplies=['darkspawn:wild_herbs','darkspawn:onion_seeds','darkspawn:garlic_seeds','minecraft:bread','minecraft:oak_sapling','minecraft:oak_log'] if forest else ['minecraft:cooked_rabbit','minecraft:bread','darkspawn:pepper_seeds','darkspawn:garlic_seeds','minecraft:bone','minecraft:rabbit_hide']
    entries=[{'type':'minecraft:item','name':id,'modifier':{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}} for id in supplies]
    write(RES/f'loot_table/chests/{site}.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[book]},{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':entries}],'random_sequence':'darkspawn:chests/'+site})

def generate():
    for site,salt,tagname in [('druid_shrine',91827364,'tree_spirit_forests'),('hunter_camp',48291735,'mutant_wolf_taigas')]:
        write(RES/f'worldgen/structure/{site}.json',{'type':'darkspawn:boss_landmark','site':site,'biomes':'#darkspawn:has_structure/'+site,'spawn_overrides':{},'step':'top_layer_modification','terrain_adaptation':'none'})
        write(RES/f'worldgen/structure_set/{site}.json',{'structures':[{'structure':'darkspawn:'+site,'weight':1}],'placement':{'type':'minecraft:random_spread','spacing':40,'separation':16,'salt':salt}})
        write(RES/f'tags/worldgen/biome/has_structure/{site}.json',{'replace':False,'values':['#darkspawn:'+tagname]})
        loot(site)
        for variant in range(3): print(site,variant,template(site,variant),'blocks')
    write(RES/'tags/worldgen/structure/boss_landmarks.json',{'replace':False,'values':['darkspawn:druid_shrine','darkspawn:hunter_camp']})

if __name__=='__main__': generate()
