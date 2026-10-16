"""Validate the new biome islands, accessible mines, settlement pads and finite resources."""
from collections import Counter, deque
from pathlib import Path
import runpy
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
read = runpy.run_path(str(ROOT / 'tools/assets/verify-pirate-assets.py'))['template']
native = zipfile.ZipFile(next((ROOT / '.gradle/loom-cache/minecraftMaven').rglob('minecraft-common-*-26.3.jar')))
image = Image.new('RGB', (1161, 814), '#173747')
draw = ImageDraw.Draw(image)
names = ['Meadowhaven', 'Verdant Sanctuary', 'Ashen Garrison', 'Starfall Sanctuary', 'Sunscar Town', 'Frosthaven']
colors = {'grass_block':'#619849','jungle_leaves':'#216c3d','oak_leaves':'#44823c','spruce_leaves':'#305449',
          'snow_block':'#e2eeee','sand':'#e1cc91','sandstone':'#c3aa73','smooth_sandstone':'#d9c793',
          'crimson_nylium':'#9b3d43','nether_wart_block':'#711f2b','netherrack':'#794447','blackstone':'#393641',
          'end_stone':'#d5d7a0','purpur_block':'#a384b0','obsidian':'#332744','dark_oak_planks':'#5e412e',
          'oak_planks':'#b78c55','spruce_planks':'#8a6948','mossy_stone_bricks':'#68795c','stone_bricks':'#878889',
          'wheat':'#bdab43','water':'#297e99','lantern':'#ffb654','air':'#173747'}
for n in range(6):
    data = read(ROOT / f'src/main/resources/data/darkspawn/structure/pirate/terrain_{n+9}.nbt')
    assert data['size'] == [129,56,129]
    blocks = {}
    for block in data['blocks']:
        x,y,z = block['pos']
        assert 0 <= x <= 128 and 0 <= y < 56 and 0 <= z <= 128
        assert (x,y,z) not in blocks
        blocks[x,y,z] = data['palette'][block['state']]['Name'].split(':')[-1]
        loot = block.get('nbt', {}).get('LootTable')
        if loot:
            namespace,path = loot.split(':')
            file = f'data/{namespace}/loot_table/{path}.json'
            assert file in native.namelist() if namespace == 'minecraft' else (ROOT / 'src/main/resources' / file).exists(), loot
    def at(x,y,z): return blocks.get((x,y,z), 'water' if y<24 else 'air')
    counts = Counter(blocks.values())
    assert counts['chest'] == 1 and counts['diamond_ore'] <= 4 and counts['ancient_debris'] <= 1
    assert counts['red_bed'] >= 4 and counts['furnace'] >= 2 and counts['lantern'] >= 10
    if n == 2: assert counts['nether_quartz_ore'] and counts['nether_gold_ore'] and counts['nether_wart']
    elif n == 3: assert counts['end_stone'] and counts['chorus_flower'] and counts['purpur_block']
    else: assert all(counts[ore+'_ore'] for ore in ['coal','iron','copper','gold','lapis','redstone'])
    for dx,dz in [(0,10),(-7,10),(7,10),(-7,-10),(7,-10)]:
        assert at(64+dx,25,64+dz) == at(64+dx,26,64+dz) == 'air'
        assert at(64+dx,24,64+dz) not in {'air','water'}
    for (x,y,z),block in blocks.items():
        if block == 'air' and y < 24:
            assert all(at(x+dx,y+dy,z+dz) != 'water' for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]), (n,'wet cave',x,y,z)
    def walkable(x,y,z): return at(x,y,z) == at(x,y+1,z) == 'air' and at(x,y-1,z) not in {'air','water'}
    queue,visited = deque([(88,25,64)]), {(88,25,64)}
    while queue:
        x,y,z=queue.popleft()
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            for dy in [0,1,-1]:
                p=x+dx,y+dy,z+dz
                if 0<=p[0]<=128 and 0<p[1]<55 and 0<=p[2]<=128 and p not in visited and walkable(*p):
                    visited.add(p);queue.append(p)
    assert any(y<=13 and abs(z-64)>3 for x,y,z in visited), (n,'mine not accessible')
    assert (64,25,74) in visited, (n,'warden disconnected from mine')
    ox,oy=(n%3)*387,(n//3)*407
    draw.text((ox+8,oy+7),names[n],fill='white')
    for x in range(129):
        for z in range(129):
            block=next((at(x,y,z) for y in range(55,22,-1) if at(x,y,z)!='air'),'water')
            draw.rectangle((ox+x*3,oy+20+z*3,ox+x*3+2,oy+22+z*3),fill=colors.get(block,'#8b8179'))
image.save(ROOT / 'build/pirate-biomes-preview.png')
print('Verified six biome islands: native loot, bounded resources, safe pads, dry accessible mines; preview rendered.')
