"""Check native hinterland resources, dry cave access and render terrain sections."""
from collections import Counter, deque
from pathlib import Path
import runpy
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
read = runpy.run_path(str(ROOT / 'tools/assets/verify-pirate-assets.py'))['template']
image = Image.new('RGB', (1130, 1170), '#132a36')
draw = ImageDraw.Draw(image)
diamonds = 0
for kind in range(9):
    terrain = read(ROOT / f'src/main/resources/data/darkspawn/structure/pirate/terrain_{kind}.nbt')
    assert terrain['size'] == [113, 56, 113]
    blocks = {}
    core = read(ROOT / f'src/main/resources/data/darkspawn/structure/pirate/island_{kind}.nbt')
    for source, offset in [(core, 32), (terrain, 0)]:
        seen = set()
        for block in source['blocks']:
            x, y, z = block['pos']
            assert 0 <= x < source['size'][0] and 0 <= y < source['size'][1] and 0 <= z < source['size'][2]
            assert (x,y,z) not in seen
            seen.add((x,y,z))
            blocks[x+offset,y,z+offset] = source['palette'][block['state']]['Name'].split(':')[-1]
    counts = Counter(blocks.values())
    assert counts['diamond_ore'] <= 4, (kind, 'rare resource budget')
    diamonds += counts['diamond_ore']
    for ore in ['coal','iron','copper','gold','lapis','redstone']:
        assert counts[ore+'_ore'] > 0, (kind, ore)
    def at(x,y,z):
        return blocks.get((x,y,z), 'water' if y < 24 else 'air')
    def walkable(x,y,z):
        return at(x,y,z) == at(x,y+1,z) == 'air' and at(x,y-1,z) not in {'air','water','jungle_leaves'}
    for block in terrain['blocks']:
        x,y,z = block['pos']
        if y < 24 and terrain['palette'][block['state']]['Name'] == 'minecraft:air':
            assert all(at(x+dx,y+dy,z+dz) != 'water' for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]), (kind, 'cave exposed to seawater', x,y,z)
    start = (76,25,56)
    assert walkable(*start), (kind, 'bridge')
    queue, visited = deque([start]), {start}
    while queue:
        x,y,z = queue.popleft()
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            for dy in [0,1,-1]:
                p = x+dx,y+dy,z+dz
                if 0 <= p[0] < 113 and 0 < p[1] < 55 and 0 <= p[2] < 113 and p not in visited and walkable(*p):
                    visited.add(p)
                    queue.append(p)
    assert any(y <= 13 and abs(z-56)>3 for x,y,z in visited), (kind, 'gallery unreachable from bridge')
    ox,oy = (kind%3)*376, (kind//3)*390
    draw.text((ox+8,oy+6), f'ISLAND {kind} | terrain and east mine section', fill='white')
    colors = {'water':'#277e9c','air':'#132a36','sand':'#ddca8e','grass_block':'#61994e','dirt':'#82613c','jungle_leaves':'#30633c','oak_planks':'#ba8d55'}
    for x in range(113):
        for z in range(113):
            name = next((at(x,y,z) for y in range(55,22,-1) if at(x,y,z)!='air'), 'water')
            draw.rectangle((ox+x*3,oy+25+z*2,ox+x*3+2,oy+26+z*2),fill=colors.get(name,'#88958b'))
        for y in range(56):
            name = at(x,y,56)
            draw.rectangle((ox+x*3,oy+264+(55-y)*2,ox+x*3+2,oy+265+(55-y)*2),fill='#d6a840' if name.endswith('_ore') else colors.get(name,'#88958b'))
    print(f'Island {kind}: six common ore types, {counts["diamond_ore"]} diamonds; bridge-to-gallery walking route; {len(visited)} reachable standing positions')
assert diamonds > 0, 'Rare diamond deposits must exist across the archipelago'
output = ROOT / 'build/pirate-terrain-preview.png'
image.save(output)
print(output)
