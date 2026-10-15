"""Validate shipped pirate templates and shanties without regenerating game assets."""
import gzip
import hashlib
import io
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'


def template(path):
    stream = io.BytesIO(gzip.decompress(path.read_bytes()))

    def number(fmt):
        return struct.unpack('>' + fmt, stream.read(struct.calcsize(fmt)))[0]

    def string():
        return stream.read(number('H')).decode('utf-8')

    def payload(kind):
        if kind in range(1, 7):
            return number({1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[kind])
        if kind == 8:
            return string()
        if kind == 9:
            child, count = number('B'), number('i')
            return [payload(child) for _ in range(count)]
        if kind == 10:
            result = {}
            while (child := number('B')) != 0:
                name = string()
                result[name] = payload(child)
            return result
        if kind in (7, 11, 12):
            return [number({7: 'b', 11: 'i', 12: 'q'}[kind]) for _ in range(number('i'))]
        raise ValueError(f'Unknown NBT tag {kind}')

    assert number('B') == 10, path
    string()
    result = payload(10)
    assert stream.read() == b'', path
    return result


previews = []
for name in ['harbor', *[f'island_{i}' for i in range(9)]]:
    data = template(RES / f'data/darkspawn/structure/pirate/{name}.nbt')
    assert data['size'] == [49, 40, 49], name
    positions = [tuple(block['pos']) for block in data['blocks']]
    assert len(positions) == len(set(positions)), f'Duplicate blocks: {name}'
    for block in data['blocks']:
        assert 0 <= block['state'] < len(data['palette']), name
        loot = block.get('nbt', {}).get('LootTable')
        if loot:
            namespace, path = loot.split(':')
            json.loads((RES / f'data/{namespace}/loot_table/{path}.json').read_text())
    previews.append({'name': name, **data})

assets = RES / 'assets/darkspawn'
sounds = json.loads((assets / 'sounds.json').read_text())
language = json.loads((assets / 'lang/en_us.json').read_text())
for name in ['pirate_beer', 'pirate_rum']:
    assert f'item.darkspawn.{name}' in language, name
    item = json.loads((assets / f'items/{name}.json').read_text())
    assert item['model'] == {'type': 'minecraft:model', 'model': f'darkspawn:item/{name}'}, name
    model = json.loads((assets / f'models/item/{name}.json').read_text())
    assert model['elements'], name
    for element in model['elements']:
        assert all(0 <= low < high <= 16 for low, high in zip(element['from'], element['to'])), name
        for face in element['faces'].values():
            assert face['texture'][1:] in model['textures'], name
            assert all(0 <= value <= 16 for value in face['uv']), name
assert json.loads((assets / 'items/landmark_compass.json').read_text()) == json.loads((assets / 'items/logbound_compass.json').read_text())
for recording in json.loads((ROOT / 'tools/audio/shanties.json').read_text()):
    event = sounds[recording['id']]
    assert event['subtitle'] in language, recording['id']
    sound, = event['sounds']
    assert sound['stream'] and sound['attenuation_distance'] == 32, recording['id']
    namespace, path = sound['name'].split(':')
    audio = (RES / f'assets/{namespace}/sounds/{path}.ogg').read_bytes()
    assert audio.startswith(b'OggS'), path
    assert hashlib.sha256(audio).hexdigest() == recording['ogg_sha256'], path

(ROOT / 'build').mkdir(exist_ok=True)
(ROOT / 'build/pirate-preview.json').write_text(json.dumps(previews), encoding='utf-8')
print('Validated 10 shipped NBT templates, loot references, 4 streamed shanty hashes/subtitles, 2 drink models and landmark compass needle parity.')
