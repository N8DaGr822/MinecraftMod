"""Validate runtime atlas dimensions, native geometry, UVs, and model references."""
import json
import struct
from pathlib import Path
from cuisine import MEALS, BOSSES, FEASTS

root=Path(__file__).resolve().parents[2]
assets=root/'src/main/resources/assets/darkspawn'
for file,size in [('textures/block/cuisine_atlas.png',512),('icon.png',256)]:
    raw=(assets/file).read_bytes()
    assert raw[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',raw[16:24])==(size,size),file
models=[]
for row in MEALS.splitlines(): models.append('item/'+row.split('|')[0])
for row in BOSSES.splitlines():
    boss,_,meal,*_=row.split('|'); models += ['item/'+meal,'block/'+boss+'_trophy']
for row in FEASTS.splitlines():
    name=row.split('|')[0]
    states=json.loads((assets/f'blockstates/{name}.json').read_text())['variants']
    counts=[]
    for n in range(1,7):
        id=f'block/{name}_{n}'; models.append(id)
        assert states[f'servings={n}']['model']=='darkspawn:'+id
        elements=json.loads((assets/f'models/{id}.json').read_text())['elements']
        counts.append(sum(e['name']=='Portion' for e in elements))
    assert counts==list(range(1,7)),name
for name in ('frost_garlic','jungle_pepper','marsh_rice'):
    states=json.loads((assets/f'blockstates/{name}_crop.json').read_text())['multipart']
    assert {p['when']['age'] for p in states}==set(map(str,range(8)))
    models += [p['apply']['model'].split(':')[1] for p in states]+['item/'+name,'item/'+name+'_seeds']
for id in models:
    obj=json.loads((assets/f'models/{id}.json').read_text())
    assert obj['textures']['atlas']=='darkspawn:block/cuisine_atlas',id
    for e in obj['elements']:
        assert all(-16<=a<b<=32 for a,b in zip(e['from'],e['to'])),(id,e['name'])
        for face in e['faces'].values():
            assert face['texture']=='#atlas' and all(0<=v<=16 for v in face['uv']),id
    if id.startswith('item/'):
        definition=json.loads((assets/f'items/{id.split("/")[1]}.json').read_text())
        assert definition['model']['model']=='darkspawn:'+id,id
print(f'PASS: {len(models)} model references, bounded geometry/UVs, six serving counts per feast, crop stages, and power-of-two PNGs.')
