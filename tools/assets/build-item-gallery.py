"""Build a portable review of the exact shipped item pixels and crop model UVs."""
import base64
import html
import io
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/darkspawn"
catalog = json.loads(Path(__file__).with_name("item-art.json").read_text())
names = json.loads((ASSETS / "lang/en_us.json").read_text())
render = runpy.run_path(str(Path(__file__).with_name("review-blocks.py")))["render"]
groups = {"kitchen": "Food & farming", "utility": "Inventory upgrades", "heart": "Boss hearts",
          "summon": "Summoning", "essence": "Essences", "trophy": "Regional materials"}


def png_url(data):
    return "data:image/png;base64," + base64.b64encode(data).decode()


cards = []
for entry in catalog:
    item_id = entry["id"]
    name = names.get("item.darkspawn." + item_id, item_id.replace("_", " ").title())
    url = png_url((ASSETS / f"textures/item/{item_id}.png").read_bytes())
    cards.append(f'''<article class="item" data-group="{entry['group']}" data-name="{html.escape(name.lower())} {item_id}">
<div class="pixels"><img width="96" height="96" src="{url}" alt="{html.escape(name)}"></div>
<h3>{html.escape(name)}</h3><p>{groups[entry['group']]}</p>
<div class="native"><span><img width="32" height="32" src="{url}" alt=""></span><span><img width="32" height="32" src="{url}" alt=""></span><small>1×</small></div>
</article>''')

blocks = []
for crop in ("onion", "garlic", "tomato", "rice", "corn", "pepper", "cooking_station"):
    stages = []
    for age in range(1 if crop == "cooking_station" else 8):
        model_id = crop if crop == "cooking_station" else crop + ("_ripe" if age == 7 else f"_crop_stage{age}")
        buffer = io.BytesIO()
        render(model_id).save(buffer, format="PNG")
        label = "Full block" if crop == "cooking_station" else ("7 · Ripe" if age == 7 else str(age))
        stages.append(f'<figure><img width="160" height="160" src="{png_url(buffer.getvalue())}" alt="{model_id}"><figcaption>{label}</figcaption></figure>')
    blocks.append('<section class="crop"><h3>' + crop.replace("_", " ").title() + '</h3><div class="stages">' + "".join(stages) + '</div></section>')

page = '''<!doctype html>
<html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Darkspawn · Item & Crop Artwork</title>
<style>
:root{color-scheme:dark;font:16px/1.5 system-ui,sans-serif;background:#111018;color:#eeeaf5}
*{box-sizing:border-box}body{margin:0}main{max-width:1480px;margin:auto;padding:32px}
header{border-bottom:1px solid #393047;padding-bottom:24px}h1{font-size:36px;line-height:1.15;margin:8px 0 16px}h2{margin:32px 0 8px}
header small{color:#bca1f5;letter-spacing:3px}p{color:#c5becf;margin:6px 0}.controls{display:flex;flex-wrap:wrap;gap:16px;margin:24px 0 12px;align-items:end}
label{display:grid;gap:5px;font-size:14px}input,select,button{font:inherit;background:#24202e;color:#eeeaf5;border:1px solid #63536f;border-radius:5px;padding:9px 12px}
input{width:260px}button{cursor:pointer}button[aria-pressed=true]{background:#53347a}button:focus-visible,input:focus-visible,select:focus-visible{outline:2px solid #c7a4ff;outline-offset:3px}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:14px;margin-top:16px}.item{background:#1b1822;border:1px solid #3b3248;border-radius:8px;padding:12px}
.pixels{display:grid;place-items:center;min-height:112px;background:#283843;border-radius:4px}.light .pixels{background:#ddd8cb}img{image-rendering:pixelated}
.item h3{font-size:15px;line-height:1.3;margin:12px 0 4px;min-height:39px}.item p{font-size:12px;color:#b69fca}.native{display:flex;align-items:center;gap:6px;margin-top:12px}
.native span{display:flex;padding:4px;background:#283843}.native span+span{background:#ddd8cb}.native small{color:#c5becf;margin-left:auto}.item[hidden]{display:none}
.stages{display:flex;overflow-x:auto;gap:4px;background:#1b1822;padding:12px;border-radius:8px}.stages figure{margin:0;flex:0 0 160px}.stages img{display:block}.stages figcaption{text-align:center;color:#c5becf;font-size:13px}
.crop h3{font-size:18px;margin:24px 0 10px}.count{font-size:14px;color:#c5becf}footer{border-top:1px solid #393047;padding:24px 0;margin-top:32px;color:#a69bae;font-size:13px}
@media(max-width:600px){main{padding:18px}h1{font-size:28px}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}input{width:100%}.controls label{flex:1 1 160px}.item h3{min-height:58px}}
</style><main><header><small>DARKSPAWN / ART LIBRARY</small><h1>Items, crops & the kitchen</h1>
<p>92 inventory icons · 48 crop stages · 1 Cooking Station</p><p>Rustic farming materials and food. Dark ritual artifacts and biome-colored rewards.</p></header>
<div class="controls"><label>Find an item<input id="search" type="search" placeholder="Name or item ID"></label>
<label>Category<select id="group"><option value="all">All items</option>GROUP_OPTIONS</select></label>
<button id="background" type="button" aria-pressed="false">Light preview background</button></div>
<p class="count" id="count" role="status"></p><div class="grid" id="items">ITEM_CARDS</div>
<h2>Growth & station models</h2><p>All eight ages, from sprout to harvest. Rendered from the shipped model geometry and texture coordinates.</p>BLOCK_CARDS
<footer>Icons are shown at 3× and at their native 32×32 size on dark and light backgrounds. Block previews use approximate source lighting; a Minecraft client playtest is still required. Artwork created with built-in image generation and locally packaged. Prompts and provenance: art/items/*.json and each crop/station Blockbench folder.</footer></main>
<script>
const search=document.getElementById('search'),group=document.getElementById('group'),cards=[...document.querySelectorAll('.item')];
function filter(){let shown=0;const query=search.value.toLowerCase().trim();for(const card of cards){card.hidden=!(card.dataset.name.includes(query)&&(group.value==='all'||card.dataset.group===group.value));if(!card.hidden)shown++}document.getElementById('count').textContent=shown+' of '+cards.length+' items · enlarged 3×';}
search.addEventListener('input',filter);group.addEventListener('change',filter);
document.getElementById('background').addEventListener('click',function(){const light=document.getElementById('items').classList.toggle('light');this.setAttribute('aria-pressed',String(light));});filter();
</script></html>'''
options = ''.join(f'<option value="{key}">{value}</option>' for key,value in groups.items())
page = page.replace("GROUP_OPTIONS", options).replace("ITEM_CARDS", "".join(cards)).replace("BLOCK_CARDS", "".join(blocks))
destination = Path(__file__).with_name("item-gallery.html")
destination.write_text(page, encoding="utf-8")
print(f"Gallery: {len(cards)} icons, 48 crop stages, Cooking Station -> {destination}")
