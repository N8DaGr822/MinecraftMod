"""Render shipped textures on dark/light inventory backgrounds at 1x and 4x."""
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[2]
ids = sys.argv[1:]
sheet = Image.new("RGB", (640, math.ceil(len(ids) / 4) * 220), "#151c27")
draw = ImageDraw.Draw(sheet)
for i, item_id in enumerate(ids):
    x, y = (i % 4) * 160, (i // 4) * 220
    with Image.open(root / f"src/main/resources/assets/darkspawn/textures/item/{item_id}.png") as texture:
        icon = texture.convert("RGBA")
        draw.rectangle((x + 8, y + 8, x + 151, y + 151), fill="#293b48")
        sheet.paste(icon.resize((128, 128), Image.Resampling.NEAREST), (x + 16, y + 16), icon.resize((128, 128), Image.Resampling.NEAREST))
        for n, background in enumerate(("#202633", "#cac7bc")):
            draw.rectangle((x + 24 + n * 64, y + 157, x + 63 + n * 64, y + 196), fill=background)
            sheet.paste(icon, (x + 28 + n * 64, y + 161), icon)
    # Long identifiers wrap; the 32px samples above are actual shipped pixels.
    label = item_id.replace("_", " ")
    draw.text((x + 8, y + 200), label[:24], fill="white")
sheet.save(root / "build/art-source/item-review.png")
print(f"Reviewed sheet: {len(ids)} icons")
