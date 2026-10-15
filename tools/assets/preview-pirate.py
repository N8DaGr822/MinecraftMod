"""Validate native pirate template routes and render an overhead layout sheet."""
import json
from collections import deque
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
templates = json.loads((ROOT / 'build/pirate-preview.json').read_text(encoding='utf-8'))
image = Image.new('RGB', (1080, 2870), '#132a36')
draw = ImageDraw.Draw(image)
draw.text((26, 16), 'PIRATE SEAS | authored structure layouts | north is up', fill='#f3dfb0')
passable = {'air', 'oak_sign', 'lantern', 'torch', 'ladder'}

def color(name):
    if name in {'red_wool','blue_wool','yellow_wool','black_wool','cyan_wool','gold_block'}: return {'red_wool':'#ac273b','blue_wool':'#3d53a5','yellow_wool':'#e7c443','black_wool':'#202c33','cyan_wool':'#269c9c','gold_block':'#e7bd4b'}[name]
    if name == 'water': return '#277e9c'
    if name == 'sand': return '#ddca8e'
    if name == 'grass_block': return '#61994e'
    if 'leaves' in name: return '#30633c'
    if name == 'chest': return '#eda93e'
    if name in ('sea_lantern', 'lantern', 'harbor_return'): return '#84f2db'
    if name == 'white_wool' or name == 'calcite': return '#ddd9c9'
    if 'stone' in name or 'brick' in name: return '#88958b'
    if 'sign' in name: return '#e3b682'
    return '#82613c'

for index, template in enumerate(templates):
    blocks = {tuple(b['pos']): template['palette'][b['state']]['Name'].split(':')[-1] for b in template['blocks']}
    assert all(0 <= x < 49 and 0 <= y < 40 and 0 <= z < 49 for x,y,z in blocks), template['name']
    assert sum(name == 'chest' for name in blocks.values()) == 1
    def at(x,y,z): return blocks.get((x,y,z), 'water' if y <= 23 else 'air')
    def walkable(x,y,z):
        return at(x,y,z) in passable and at(x,y+1,z) in passable and at(x,y-1,z) not in passable | {'water','campfire'}
    start = (24,25,24) if index == 0 else (24,25,47)
    target = [(15,25,19),(25,25,21),(23,25,20),(27,25,21),(24,25,21),(24,25,19),(25,25,21),(24,25,16),(24,25,15),(24,25,21)][index]
    queue, visited = deque([start]), {start}
    assert walkable(*start), f"Blocked arrival at {template['name']}"
    while queue:
        x,y,z = queue.popleft()
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            for dy in [0,1,-1]:
                pos = x+dx,y+dy,z+dz
                if 0 <= pos[0] < 49 and 0 <= pos[2] < 49 and pos not in visited and walkable(*pos):
                    visited.add(pos); queue.append(pos)
    assert target in visited, f"Treasure inaccessible in {template['name']}"
    if index == 0:
        assert (24,25,21) in visited and (24,25,48) in visited, 'Harbor return and dock must connect'
        assert all(pos in visited for pos in [(21,25,10),(39,25,26),(20,25,25),(26,25,26)]), 'Tavern, market, Briggs and board approaches must connect'
    ox,oz = 26+(index%2)*530, 52+(index//2)*560
    draw.text((ox,oz), ['HARBOR | lighthouse, supplies, return','CASTAWAY COVE | first discovery','SALTWATCH FORT | ruined watch','THE PALE GULL | beached wreck','LASTLIGHT BEACON | abandoned signal','TIDEGLASS SHRINE | tidal ruins','ROOTBOUND CAMP | mangrove salvage','REDWATER LANDING | opposing shore camps','PEARLWATER ATOLL | lagoon and pearl shelter','CINDERJAW ANCHORAGE | basalt drydock'][index], fill='#f3dfb0')
    for x in range(49):
        for z in range(49):
            surface = next((at(x,y,z) for y in range(39,22,-1) if at(x,y,z) != 'air'), 'water')
            draw.rectangle((ox+x*10,oz+23+z*10,ox+x*10+9,oz+23+z*10+9),fill=color(surface))
    draw.text((ox,oz+520), '49 x 49 blocks | chest access verified | cyan: light / return', fill='#b8cfcc')

output = ROOT / 'art/pirate/layout-preview.png'
output.parent.mkdir(parents=True,exist_ok=True)
image.save(output)
print('Validated 10 template bounds, finite caches, arrival/return, dock-to-cache and new harbor service walking routes.')
print(output)
