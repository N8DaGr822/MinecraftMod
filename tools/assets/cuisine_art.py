"""Editable Java block/item geometry using the generated culinary material atlas.

Called by cuisine.py. No raster processing: the source texture remains unchanged.
Each named dish has authored geometry; all trophies have family-specific silhouettes.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/darkspawn'
RES = ROOT / 'src/main/resources'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')

def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n', encoding='utf-8')

def box(lo, hi, tile, name=''):
    x, y = tile % 4 * 4, tile // 4 * 4
    return {'name': name, 'from': lo, 'to': hi,
            'faces': {f: {'texture': '#atlas', 'uv': [x+.1,y+.1,x+3.9,y+3.9]} for f in FACES}}

def model(path, elements):
    save(ASSETS / ('models/' + path + '.json'), {
        'parent': 'minecraft:block/block', 'ambientocclusion': False,
        'textures': {'atlas': 'darkspawn:block/cuisine_atlas', 'particle': '#atlas'},
        'display': {'gui': {'rotation': [30,225,0], 'translation':[0,0,0], 'scale':[.85,.85,.85]},
                    'ground': {'rotation':[0,0,0], 'translation':[0,3,0], 'scale':[.4,.4,.4]},
                    'fixed': {'rotation':[0,180,0], 'translation':[0,0,0], 'scale':[.7,.7,.7]}},
        'elements': elements})

def ingredient(name):
    """Boss drops have recognizable silhouettes, independent of the cooked meals."""
    e = []
    def b(lo, hi, tile, label): e.append(box(lo, hi, tile, label))
    if name == 'ancient_sap':
        b([4,2,4],[12,10,12],14,'Amber sap'); b([5,3,3.8],[7,9,4],5,'Amber highlight')
        b([6,10,6],[10,12,10],14,'Bottle neck'); b([5.5,12,5.5],[10.5,14,10.5],0,'Bark stopper')
        b([10,10,7],[14,11,9],6,'Leaf'); b([12,11,7],[14,12,9],6,'Leaf tip')
    elif name == 'prime_beast_meat':
        b([2,5,6],[14,7,10],10,'Bone'); b([1,4,5],[4,8,11],10,'Knuckle')
        b([5,3,3],[12,10,13],3,'Prime cut'); b([6,4,2.8],[11,9,3],4,'Raw meat')
        b([8,6,2.5],[9,8,3],5,'Fat seam'); b([12,4,5],[15,8,11],10,'Knuckle')
    elif name == 'restored_marrow':
        b([6,3,6],[10,12,10],10,'Restored bone'); b([4,2,4],[12,5,12],10,'Lower joint')
        b([4,11,4],[12,14,12],10,'Cut joint'); b([6,14,6],[10,14.3,10],4,'Healthy marrow')
        b([5.8,6,5.8],[10.2,8,10.2],6,'Restorative herb wrap')
    elif name == 'marrow_extract':
        b([4,2,4],[12,9,12],3,'Extract jar'); b([5,3,3.8],[11,8,4],10,'Bone label')
        b([7,4,3.5],[9,7,3.8],3,'Label center'); b([5,9,5],[11,11,11],10,'Ivory neck')
        b([5,11,5],[11,13,11],0,'Cork'); b([4,2,4],[12,3,12],10,'Jar foot')
    elif name == 'charged_yolk':
        b([4,2,4],[12,5,12],10,'Broken eggshell'); b([3,4,5],[5,7,11],10,'Shell shard')
        b([11,4,5],[13,8,11],10,'Shell shard'); b([5,5,5],[11,9,11],14,'Charged yolk')
        for lo,hi in [([8,10,7],[10,13,9]),([7,12,7],[9,14,9]),([8,14,7],[10,16,9])]: b(lo,hi,9,'Electric spark')
    elif name == 'titan_meat':
        b([3,2,3],[13,5,13],6,'Serpent hide'); b([4,5,4],[12,7,12],4,'Steak flesh')
        b([6,7,6],[10,7.4,10],10,'Vertebra'); b([7,7.4,7],[9,7.6,9],3,'Marrow center')
        for x in (3,7,11): b([x,3,2.7],[x+2,5,3],9,'Scales')
    elif name == 'witch_spice':
        b([4,2,4],[12,9,12],15,'Spice pouch'); b([5,9,5],[11,11,11],8,'Gathered cloth')
        b([6,11,6],[10,12,10],14,'Drawstring'); b([5,12,5],[11,14,11],15,'Open rim')
        b([6,14,6],[10,14.2,10],12,'Spice powder'); b([6,5,3.7],[10,7,4],14,'Gold seal')
    elif name in ('mineral_salt','soul_salt'):
        soul = name == 'soul_salt'
        b([3,2,3],[13,3,13],11 if soul else 10,'Salt bed')
        for i,(x,z,h) in enumerate([(4,4,7),(8,5,13),(5,9,10),(10,10,6)]):
            b([x,3,z],[x+2,h,z+2],9 if soul else 10,'Salt crystal')
            b([x+.4,h,z+.4],[x+1.6,h+1,z+1.6],13 if soul else 7,'Crystal tip')
        if soul: b([2,8,7],[3,10,8],9,'Floating soul mote'); b([12,12,4],[13,14,5],9,'Floating soul mote')
    elif name == 'wyrm_fat':
        b([3,2,4],[13,5,12],13,'Icy hide'); b([3,5,4],[13,8,12],5,'Wyrm fat')
        b([4,8,5],[12,10,11],1,'Fat cap')
        for x in (4,8,11): b([x,3,3.5],[x+1,7,4],9,'Frost crystal')
    elif name == 'kraken_meat':
        b([3,3,4],[7,7,12],8,'Tentacle'); b([6,3,9],[12,7,13],8,'Tentacle bend')
        b([10,6,9],[13,11,12],8,'Curled tip'); b([8,10,9],[12,12,12],9,'Tip')
        for z in (5,8,11): b([2.5,4,z],[3,6,z+1],1,'Sucker')
        for x in (7,10): b([x,7,10],[x+1.5,7.5,12],1,'Sucker')
        b([3,3,3.7],[7,7,4],4,'Cut surface')
    elif name == 'cave_truffle':
        b([4,3,4],[12,10,12],3,'Truffle heart')
        for x,y,z in [(2,5,5),(10,6,5),(5,9,5),(5,2,6),(5,5,2),(6,5,10)]: b([x,y,z],[x+4,y+3,z+4],11,'Knobby rind')
        b([4,10,6],[6,10.5,8],10,'Spore fleck'); b([10,7,4],[12,8,4.3],10,'Spore fleck')
    elif name == 'royal_jelly':
        b([3,2,3],[13,4,13],15,'Dark jelly base'); b([4,4,4],[12,9,12],9,'Royal jelly')
        b([5,9,5],[11,11,11],9,'Jelly dome'); b([4,10,4],[12,11,12],14,'Crown band')
        for x,z in [(4,4),(10,4),(4,10),(10,10)]: b([x,11,z],[x+2,14,z+2],14,'Crown point')
    elif name == 'sovereign_cap':
        b([6,2,6],[10,7,10],10,'Stem'); b([3,6,3],[13,8,13],15,'Gills')
        b([2,8,2],[14,10,14],8,'Cap brim'); b([4,10,4],[12,13,12],8,'Cap crown')
        for x,z in [(4,4),(9,6),(6,10)]: b([x,13,z],[x+2,13.3,z+2],1,'Spore spot')
    elif name == 'infernal_marrow':
        b([5,3,5],[11,12,11],11,'Charred bone'); b([3,2,3],[13,5,13],11,'Joint')
        b([4,11,4],[12,14,12],11,'Split bone rim'); b([6,14,6],[10,14.4,10],12,'Exposed molten marrow')
        b([7,4,4.7],[9,12,5],12,'Molten fissure'); b([10.8,7,7],[11.2,12,9],12,'Side fissure')
    elif name == 'void_essence':
        b([5,2,5],[11,3,11],11,'Obsidian base'); b([4,3,4],[12,4,12],8,'Lower containment ring')
        b([4,12,4],[12,13,12],8,'Upper containment ring')
        for x,z in [(4,4),(11,11)]: b([x,4,z],[x+1,12,z+1],15,'Containment strut')
        b([6,6,6],[10,10,10],8,'Suspended essence'); b([7,7,5.7],[9,9,6],9,'Essence core')
        b([3,8,9],[4,9,10],9,'Void mote'); b([11,10,5],[12,11,6],8,'Void mote')
    else:
        raise ValueError('Missing boss ingredient artwork: ' + name)
    model('item/' + name, e)

def generate_ingredients(bosses):
    for _, name, *_ in bosses:
        ingredient(name)

def meal(name, category, index):
    from cuisine_meals import elements_for
    model('item/'+name, elements_for(name, box))

def trophy(boss):
    e=[box([2,0,2],[14,2,14],11,'Plinth'),box([3,2,3],[13,2.5,13],14,'Gold inlay'),box([6,2.5,6],[10,5,10],11,'Stand')]
    def b(lo,hi,t,n='Sculpture'): e.append(box(lo,hi,t,n))
    if boss=='ancient_tree_spirit':
        b([5,5,5],[11,12,11],0)
        for x in (3,11): b([x,10,7],[x+2,15,9],0,'Antler'); b([x-1,13,7],[x+3,14,9],6,'Leaves')
        for x in (6,9): b([x,9,4.8],[x+1,10,5],14,'Eye')
    elif boss in ('mutant_wolf','fossil_tyrant','ice_wyrm'):
        t={'mutant_wolf':11,'fossil_tyrant':10,'ice_wyrm':13}[boss]
        b([4,6,5],[12,12,12],t,'Skull'); b([5,6,2],[11,9,5],t,'Muzzle')
        for x in (4,10): b([x,12,8],[x+2,15,10],t,'Ear or horn'); b([x+1,5,2],[x+1.5,6.5,3],10,'Fang')
        for x in (4.5,10): b([x,10,4.8],[x+1.5,11,5],9 if boss=='ice_wyrm' else 14,'Eye')
    elif boss=='thunder_bird':
        b([7,5,7],[9,13,9],14,'Body'); b([6,11,6],[10,14,10],14,'Head'); b([7,11,4],[9,12,6],11,'Beak')
        for side in (0,1):
            for n in range(3):
                x=2+n*2 if side==0 else 9+n*2
                b([x,8+abs(1-n),7],[x+2,12+abs(1-n),9],14 if n%2 else 9,'Wing')
    elif boss=='titan_boa':
        for lo,hi in [([3,5,3],[13,7,6]),([3,5,6],[6,7,13]),([6,5,10],[13,7,13]),([10,7,8],[13,12,11]),([8,11,5],[13,14,10])]: b(lo,hi,6,'Coiled serpent')
        b([8,12,4.8],[9,13,5],14,'Eye')
    elif boss=='baba_yaga':
        b([5,5,5],[11,9,11],10,'Mask'); b([2,9,2],[14,10,14],15,'Hat brim'); b([5,10,5],[11,12,11],15,'Hat'); b([7,12,7],[10,15,10],15,'Hat point')
        b([7,6,3],[9,8,5],10,'Nose')
    elif boss=='kraken':
        b([5,8,5],[11,14,11],9,'Mantle')
        for x,z in [(3,4),(10,3),(3,10),(10,10)]: b([x,4,z],[x+2,9,z+2],9,'Tentacle'); b([x,4,z],[x+3,5,z+3],8,'Curl')
    elif boss=='cave_crawler':
        b([5,7,5],[11,11,11],11,'Carapace')
        for z in (4,7,10):
            b([2,6,z],[5,9,z+1],15,'Left leg'); b([11,6,z],[14,9,z+1],15,'Right leg')
        for x in (6,9): b([x,8,4.8],[x+1,9,5],4,'Eye')
    elif boss=='mycelial_sovereign':
        b([6,5,6],[10,11,10],10,'Stem'); b([2,10,2],[14,12,14],15,'Cap'); b([4,12,4],[12,14,12],8,'Crown')
        for x,z in [(4,4),(9,8),(5,10)]: b([x,14,z],[x+2,14.3,z+2],1,'Spore spot')
    elif boss=='void_eye':
        b([3,6,5],[13,13,11],8,'Eye shell'); b([4,7,4],[12,12,5],1,'Eye'); b([6,7,3.7],[10,12,4],9,'Iris'); b([7,8,3.5],[9,11,3.7],11,'Pupil')
    else:
        t={'mutant_zombie':6,'mountain_titan':11,'shadow_creeper_queen':15,'netherborn':12,'soulbound_colossus':11}[boss]
        b([4,5,5],[12,12,12],t,'Head'); b([6,5,4],[10,7,5],11,'Mouth')
        for x in (5,9): b([x,9,4.8],[x+2,10,5],9 if boss=='soulbound_colossus' else 14,'Eye')
        if boss=='shadow_creeper_queen':
            for x in (3,7,11): b([x,12,6],[x+2,15,8],9,'Crown')
        elif boss=='mountain_titan': b([5,12,6],[8,15,10],13,'Crystal')
        elif boss=='netherborn':
            for x in (3,11): b([x,11,7],[x+2,15,9],11,'Horn')
        elif boss=='soulbound_colossus': b([6,12,6],[10,15,10],9,'Soul flame')
        else: b([4,12,6],[9,13,12],6,'Broken crown')
    model('block/'+boss+'_trophy',e)

def crops():
    langpath=ASSETS/'lang/en_us.json'; lang=json.loads(langpath.read_text(encoding='utf-8'))
    for name,base,fruit in [('frost_garlic','garlic',13),('jungle_pepper','pepper',8),('marsh_rice','rice',14)]:
        states=[]
        for age in range(8):
            original=base+('_ripe' if age==7 else '_crop_stage'+str(age))
            obj=json.loads((ASSETS/f'models/block/{original}.json').read_text())
            obj['textures']={'atlas':'darkspawn:block/cuisine_atlas','particle':'#atlas'}
            for element in obj['elements']:
                for face in element['faces'].values():
                    u,v=face['uv'][:2]; tile=6 if v<8 and u<8 else 0 if v<8 else fruit
                    x,y=tile%4*4,tile//4*4; face['uv']=[x+.1,y+.1,x+3.9,y+3.9]; face['texture']='#atlas'
            modelid=name+'_crop_stage'+str(age)
            save(ASSETS/f'models/block/{modelid}.json',obj)
            states.append({'when':{'age':str(age)},'apply':{'model':'darkspawn:block/'+modelid}})
        save(ASSETS/f'blockstates/{name}_crop.json',{'multipart':states})
        loot=json.loads((RES/f'data/darkspawn/loot_table/blocks/{base}_crop.json').read_text())
        save(RES/f'data/darkspawn/loot_table/blocks/{name}_crop.json',json.loads(json.dumps(loot).replace('darkspawn:'+base,'darkspawn:'+name)))
        save(RES/f'data/darkspawn/recipe/{name}_conversion.json',{'type':'minecraft:crafting_shapeless','ingredients':['darkspawn:'+name],'result':{'id':'darkspawn:'+base}})
        for suffix in ('','_seeds'):
            id=name+suffix
            save(ASSETS/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'darkspawn:item/'+id}})
            model('item/'+id,[box([4,2,4],[12,7 if suffix else 11,12],2 if suffix else fruit,'Produce'),box([7,7 if suffix else 11,7],[9,13,9],6,'Stem')])
            lang['item.darkspawn.'+id]=id.replace('_',' ').title()
        lang['block.darkspawn.'+name+'_crop']=name.replace('_',' ').title()+' Crop'
    save(langpath,lang)
    for folder,tag,suffix in [('block','crops','_crop'),('item','villager_plantable_seeds','_seeds'),('item','chicken_food','_seeds')]:
        path=RES/f'data/minecraft/tags/{folder}/{tag}.json'
        obj=json.loads(path.read_text(encoding='utf-8'))
        for name in ('frost_garlic','jungle_pepper','marsh_rice'):
            entry='darkspawn:'+name+suffix
            if entry not in obj['values']: obj['values'].append(entry)
        save(path,obj)

def generate_art(rows,bosses):
    generate_ingredients(bosses)
    for i,(name,_,category,*_) in enumerate(rows): meal(name,category,i)
    for boss,*_ in bosses: trophy(boss)
    for name,tile in [('hunters_feast',3),('woodland_feast',6),('ocean_feast',9),('nether_feast',12),('ender_banquet',8),('hero_feast',14)]:
        for n in range(1,7):
            e=[box([1,0,1],[15,1,15],0,'Serving board'),box([1,1,1],[15,1.3,2],14,'Inlay')]
            for portion in range(n):
                x=2+(portion%3)*4; z=3+(portion//3)*6
                e += [box([x,1,z],[x+3,2,z+4],1,'Plate'),box([x,2,z],[x+3,4,z+3],tile,'Portion'),box([x+.5,4,z+.5],[x+2.5,5,z+2.5],2 if portion%2 else 7,'Garnish')]
            model(f'block/{name}_{n}',e)
    crops()
