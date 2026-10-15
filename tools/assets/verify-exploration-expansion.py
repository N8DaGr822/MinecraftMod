"""Validate shipped regional archaeology templates, loot links and expansion item resources."""
from pathlib import Path
import json
import runpy
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'src/main/resources'
read=runpy.run_path(str(ROOT/'tools/assets/verify-pirate-assets.py'))['template']
sites=sorted(p.stem for p in (RES/'data/darkspawn/loot_table/archaeology').glob('*.json'))
assert len(sites)==14
image=Image.new('RGB',(980,7*275),'#172e35');draw=ImageDraw.Draw(image)
for index,site in enumerate(sites):
    loot=json.loads((RES/f'data/darkspawn/loot_table/archaeology/{site}.json').read_text(encoding='utf-8'))
    assert loot['type']=='minecraft:archaeology' and loot['pools'][0]['rolls']==1
    for variant in range(3):
        data=read(RES/f'data/darkspawn/structure/landmarks/{site}_{variant}.nbt')
        blocks={tuple(b['pos']):b for b in data['blocks']}
        deposits=[b for b in blocks.values() if data['palette'][b['state']]['id']=='minecraft:suspicious_gravel']
        assert len(deposits)==2,(site,variant)
        for block in deposits:
            x,y,z=block['pos'];assert block['nbt']['LootTable']==f'darkspawn:archaeology/{site}'
            assert data['palette'][blocks[x,y-1,z]['state']]['id'] not in ('minecraft:air','minecraft:water')
            assert (x,y,z)!=(12,3,12)
        if variant==0:
            ox,oy=(index%2)*490+12,(index//2)*275+8
            draw.text((ox,oy),site+' | cyan: brush sites',fill='#e9d9a5')
            for x in range(25):
                for z in range(25):
                    b=blocks.get((x,3,z));name=data['palette'][b['state']]['id'] if b else 'air'
                    color='#61e1dd' if name.endswith('suspicious_gravel') else '#b87b47' if (x,z)==(12,12) else '#7d8b7a' if b else '#254651'
                    draw.rectangle((ox+x*9,oy+18+z*9,ox+x*9+8,oy+18+z*9+8),fill=color)
for item in ('awakening_seal','blank_discovery_chart'):
    for file in (f'assets/darkspawn/items/{item}.json',f'data/darkspawn/recipe/{item}.json',f'data/darkspawn/advancement/recipes/{item}.json'):
        json.loads((RES/file).read_text(encoding='utf-8'))
image.save(ROOT/'build/archaeology-preview.png')
print('Validated 42 regional templates, 84 supported archaeology deposits, 14 regional loot tables and both new item/recipe resources.')
