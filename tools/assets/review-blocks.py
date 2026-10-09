"""Render native block JSONs with their shipped UVs for source-art review (not game lighting)."""
import json
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/darkspawn"
FACES = {"north":[0,1,2,3], "south":[5,4,7,6], "west":[4,0,3,7], "east":[1,5,6,2], "up":[3,2,6,7], "down":[4,5,1,0]}
SHADE = {"north":.88,"south":.78,"west":.8,"east":.92,"up":1,"down":.65}


def rotate(point, rotation):
    if not rotation:
        return point
    origin = rotation["origin"]
    x,y,z = [v-origin[i] for i,v in enumerate(point)]
    a = math.radians(rotation["angle"])
    c,s = math.cos(a),math.sin(a)
    if rotation["axis"] == "x": y,z = y*c-z*s,y*s+z*c
    elif rotation["axis"] == "y": x,z = x*c+z*s,-x*s+z*c
    else: x,y = x*c-y*s,x*s+y*c
    return [x+origin[0],y+origin[1],z+origin[2]]


def render(model_id, size=160, scale=5):
    model = json.loads((ASSETS / f"models/block/{model_id}.json").read_text())
    texture_id = model["textures"]["atlas"].split(":")[1]
    texture = Image.open(ASSETS / f"textures/{texture_id}.png").convert("RGBA")
    tiles = {}
    canvas = Image.new("RGBA", (size,size), "#24363e")
    def project(p):
        x,y,z = p[0]-8,p[1],p[2]-8
        x,z = x*.819+z*.574,-x*.574+z*.819
        return [size/2+x*scale,size*.83-(y*.94+z*.342)*scale,z*.94-y*.342]
    quads = []
    for element in model["elements"]:
        x,y,z = element["from"]
        X,Y,Z = element["to"]
        points = [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
        points = [project(rotate(p, element.get("rotation"))) for p in points]
        for face, definition in element["faces"].items():
            quad = [points[i] for i in FACES[face]]
            quads.append((sum(p[2] for p in quad)/4, face, quad, definition["uv"]))
    quads.sort(key=lambda q:q[0],reverse=True)
    for _, face, p, uv in quads:
        x0,y0 = p[0][:2]
        a,b,c,d = p[1][0]-x0,p[3][0]-x0,p[1][1]-y0,p[3][1]-y0
        det = a*d-b*c
        if det>=-.001: continue
        # Quad Mapping: Inverse affine transform samples the actual 0..16 Minecraft UVs.
        key = tuple(uv)
        if key not in tiles:
            bounds = tuple(round(q*texture.width/16) for q in uv)
            tile = texture.crop(bounds)
            w,h = tile.size
            padded = Image.new("RGBA",(w*3,h*3))
            # Preview Sampling: Clamp each material's edge pixels, as a rasterized face would.
            for row in range(3):
                for col in range(3):
                    x,X = ((0,1),(0,w),(w-1,w))[col]
                    y,Y = ((0,1),(0,h),(h-1,h))[row]
                    padded.paste(tile.crop((x,y,X,Y)).resize((w,h)),(col*w,row*h))
            tiles[key] = padded,(w,h,w*2,h*2)
        sampled,(u,v,U,V) = tiles[key]
        coeff = ((U-u)*d/det, -(U-u)*b/det, u+(U-u)*(-d*x0+b*y0)/det,
                 -(v-V)*c/det, (v-V)*a/det, V+(v-V)*(c*x0-a*y0)/det)
        mapped = sampled.transform((size,size),Image.Transform.AFFINE,coeff,Image.Resampling.NEAREST)
        mask = Image.new("L",(size,size))
        ImageDraw.Draw(mask).polygon([tuple(q[:2]) for q in p],fill=255)
        shade = SHADE[face]
        if shade != 1:
            r,g,b,alpha = mapped.split()
            mapped = Image.merge("RGBA",(r.point(lambda n:int(n*shade)),g.point(lambda n:int(n*shade)),b.point(lambda n:int(n*shade)),alpha))
        canvas.paste(mapped,(0,0),mask)
    return canvas


ids = sys.argv[1:]
sheet = Image.new("RGB",(1280,len(ids)*188),"#151c27")
draw = ImageDraw.Draw(sheet)
for row,crop_id in enumerate(ids):
    if crop_id == "cooking_station":
        sheet.paste(render(crop_id,160,5),(0,row*188))
        draw.text((8,row*188+166),"Cooking Station",fill="white")
        continue
    for age in range(8):
        name = crop_id+("_ripe" if age==7 else f"_crop_stage{age}")
        sheet.paste(render(name),(age*160,row*188))
        draw.text((age*160+8,row*188+166),f"{crop_id} / {age}"+(" ripe" if age==7 else ""),fill="white")
sheet.save(ROOT / "build/art-source/block-review.png")
print(f"Rendered {len(ids)} block families")
