"""Reproduce authored utility icons, fish skins, recipes and a review sheet (Pillow)."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/darkspawn'
DATA = ROOT / 'src/main/resources/data/darkspawn'
FISH = ['pike', 'tuna', 'anglerfish', 'arctic_cod', 'piranha', 'cave_fish']
COLORS = ['#6c9465', '#557ca9', '#9e607d', '#b5d7df', '#bc7552', '#c3b4aa']
MEALS = ['pike_chowder', 'tuna_rice_bowl', 'anglerfish_stew', 'arctic_cod_soup', 'piranha_pepper_bowl', 'cave_fish_broth', 'truffle_risotto', 'farm_omelette', 'herbed_omelette', 'goat_cheese_plate', 'fungal_cream_soup']
EXTRAS = ['woodland_truffle', 'rich_egg', 'herbed_egg', 'goat_milk', 'goat_cheese', 'fungal_cream']

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def recipe(name, ingredients, result, count=1):
    write(DATA/'recipe'/f'{name}.json', {'type':'minecraft:crafting_shapeless', 'ingredients':ingredients, 'result':{'id':'darkspawn:'+result, 'count':count}})

gallery = []
def icon(name, im):
    path=ASSETS/'textures/item'/f'{name}.png'; path.parent.mkdir(parents=True,exist_ok=True); im.save(path)
    write(ASSETS/'items'/f'{name}.json', {'model':{'type':'minecraft:model','model':'darkspawn:item/'+name}})
    write(ASSETS/'models/item'/f'{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':'darkspawn:item/'+name}})
    gallery.append((name, im))

for i,name in enumerate(FISH):
    for cooked in [False,True]:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);color='#bc814f' if cooked else COLORS[i]
        d.polygon([(1,5),(5,7),(1,11)],fill=color);d.ellipse((4,4 if i==4 else 5,14,11),fill=color);d.polygon([(7,5),(10,2),(11,6)],fill=color);d.point((12,7),fill='#18262e');d.line((6,10,11,10),fill='#e6d8ba')
        if i==2:d.line((11,5,10,1,13,1,13,3),fill='#e5d782')
        icon(('cooked_' if cooked else '')+name,im)
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);d.polygon([(2,5),(13,5),(11,14),(4,14)],fill='#899ca6');d.ellipse((2,3,13,7),fill='#5b9dbd');d.ellipse((5,5,11,8),fill=COLORS[i]);icon(name+'_bucket',im)
    skin=Image.new('RGBA',(32,32),COLORS[i]);d=ImageDraw.Draw(skin)
    for y in range(0,32,4):
        for x in range((y//4)%2,32,4):d.line((x,y,x+2,y),fill='#d2cdb4')
    d.rectangle((0,0,3,3),fill='#283440');d.point((1,1),fill='#efeee2')
    path=ASSETS/'textures/entity/fish'/f'{name}.png';path.parent.mkdir(parents=True,exist_ok=True);skin.save(path)
    for cooking in ['smelting','smoking']:
        write(DATA/'recipe'/f'{name}_{cooking}.json',{'type':'minecraft:'+cooking,'category':'food','ingredient':'darkspawn:'+name,'result':{'id':'darkspawn:cooked_'+name},'experience':.35,'cookingtime':200 if cooking=='smelting' else 100})

for i,name in enumerate(EXTRAS+MEALS):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    if 'egg' in name:
        d.ellipse((4,2,11,14),fill='#d8c492');d.point((6,5),fill='#6c8f4b' if 'herbed' in name else '#b9824a');d.point((9,9),fill='#6c8f4b' if 'herbed' in name else '#b9824a')
    elif name=='woodland_truffle':d.ellipse((3,4,13,13),fill='#68503e');d.rectangle((5,5,6,7),fill='#9b7951');d.rectangle((9,9,11,10),fill='#382f2d')
    elif name=='goat_milk':d.rectangle((5,4,11,14),fill='#d8e4df');d.rectangle((6,1,10,4),fill='#adbdc1');d.rectangle((6,0,10,1),fill='#a58459')
    elif name=='goat_cheese':d.polygon([(2,11),(9,3),(14,7),(14,13),(2,13)],fill='#e6d9a1');d.ellipse((9,8,11,10),fill='#c6af72')
    else:
        d.polygon([(2,8),(14,8),(11,14),(5,14)],fill='#77523c');d.ellipse((2,5,14,10),fill='#cfb071');d.rectangle((5,6,7,8),fill=COLORS[i%6]);d.rectangle((9,7,11,9),fill='#6a9657')
    icon(name,im)

recipe('goat_cheese',['darkspawn:goat_milk','darkspawn:salt'],'goat_cheese',2)
ingredients=[['darkspawn:cooked_'+fish,'minecraft:potato','darkspawn:wild_herbs','minecraft:bowl'] for fish in FISH]
ingredients += [['darkspawn:woodland_truffle','darkspawn:rice','darkspawn:butter','minecraft:bowl'],['darkspawn:rich_egg','darkspawn:cheese','darkspawn:tomato','minecraft:bowl'],['darkspawn:herbed_egg','darkspawn:onion','darkspawn:butter','minecraft:bowl'],['darkspawn:goat_cheese','minecraft:bread','darkspawn:tomato','minecraft:bowl'],['darkspawn:fungal_cream','minecraft:brown_mushroom','darkspawn:onion','minecraft:bowl']]
for name,parts in zip(MEALS,ingredients):write(DATA/'recipe'/f'{name}.json',{'type':'darkspawn:cooking','ingredients':parts,'result':{'id':'darkspawn:'+name}})

# Native recipe-book discovery follows the first material acquired.
for name in ['goat_cheese']+[fish+'_'+method for fish in FISH for method in ['smelting','smoking']]:
    data=json.loads((DATA/'recipe'/f'{name}.json').read_text())
    ingredient=data.get('ingredient') or data['ingredients'][0]
    write(DATA/'advancement/recipes'/f'{name}.json',{'criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':ingredient}]}}},'requirements':[['has_material']],'rewards':{'recipes':['darkspawn:'+name]}})

langpath=ASSETS/'lang/en_us.json';lang=json.loads(langpath.read_text(encoding='utf-8'))
for name,_ in gallery:lang['item.darkspawn.'+name]=name.replace('_',' ').title()
for name in FISH:lang['entity.darkspawn.'+name]=name.replace('_',' ').title()
write(langpath,lang)
sheet=Image.new('RGB',(640,((len(gallery)+7)//8)*96),'#203138');d=ImageDraw.Draw(sheet)
for i,(name,im) in enumerate(gallery):
    x=(i%8)*80;y=(i//8)*96;sheet.paste(im.resize((48,48),Image.Resampling.NEAREST),(x+16,y+4),im.resize((48,48),Image.Resampling.NEAREST));d.text((x+3,y+55),name[:12],fill='#e4dbc1');d.text((x+3,y+67),name[12:24],fill='#e4dbc1')
out=ROOT/'build/field-utilities-preview.png';out.parent.mkdir(exist_ok=True);sheet.save(out)
print(f'Generated {len(gallery)} item icons, six fish skins, recipes and {out}')
