"""Render journal illustrations from existing cuboids; no new creature designs."""
import base64,io,json,math
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'src/main/resources/assets/darkspawn'
entries=json.loads((ROOT/'src/main/resources/data/darkspawn/journal/catalog.json').read_text(encoding='utf-8'))
out=ASSETS/'textures/gui/journal';out.mkdir(parents=True,exist_ok=True)
count=0
for entry in entries:
 id=entry['entity']
 if not id:continue
 geo=ASSETS/f'geckolib/models/entity/{id}.geo.json';boxes=[]
 if geo.exists():
  model=json.loads(geo.read_text(encoding='utf-8'))['minecraft:geometry'][0]
  texture_path=ASSETS/f'textures/entity/{id}.png'
  if texture_path.exists():texture=Image.open(texture_path).convert('RGB')
  else:
   source=json.loads(next((ROOT/f'art/blockbench/{id}').glob('*.bbmodel')).read_text(encoding='utf-8'))
   texture=Image.open(io.BytesIO(base64.b64decode(source['textures'][0]['source'].split(',')[1]))).convert('RGB')
  for bone in model['bones']:
   for c in bone.get('cubes',[]):boxes.append((c['origin'],c['size'],c.get('uv',[0,0])))
 else:
  sources=list((ROOT/f'art/blockbench/{id}').glob('*.bbmodel'));assert sources,id
  model=json.loads(sources[0].read_text(encoding='utf-8'))
  texture=Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1]))).convert('RGB')
  for c in model['elements']:
   if c.get('type','cube')=='cube':boxes.append((c['from'],[b-a for a,b in zip(c['from'],c['to'])],c.get('uv_offset',[0,0])))
 yaw=-.6;pitch=.18
 def project(p):
  x,y,z=p;xx=x*math.cos(yaw)+z*math.sin(yaw);zz=-x*math.sin(yaw)+z*math.cos(yaw)
  return xx,-y+zz*pitch,zz+y*pitch
 faces=[]
 for (x,y,z),(w,h,d),uv in boxes:
  X,Y,Z=x+w,y+h,z+d
  if not isinstance(uv,list):uv=[0,0]
  color=texture.getpixel((min(texture.width-1,max(0,int(uv[0]+d+w/2))),min(texture.height-1,max(0,int(uv[1]+d+h/2)))))
  for points,shade in [([(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],1),([(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],.7),([(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],.82),([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.9),([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.12)]:
   ps=[project(p) for p in points];faces.append((sum(p[2] for p in ps)/4,ps,tuple(min(255,int(c*shade)) for c in color)))
 points=[p for _,ps,_ in faces for p in ps];minx=min(p[0] for p in points);maxx=max(p[0] for p in points);miny=min(p[1] for p in points);maxy=max(p[1] for p in points);scale=min(108/max(1,maxx-minx),108/max(1,maxy-miny))
 known=Image.new('RGBA',(128,128));unknown=Image.new('RGBA',(128,128));dk=ImageDraw.Draw(known);du=ImageDraw.Draw(unknown)
 for _,ps,color in sorted(faces,reverse=True):
  poly=[(64+(p[0]-(minx+maxx)/2)*scale,64+(p[1]-(miny+maxy)/2)*scale) for p in ps]
  dk.polygon(poly,fill=(*color,255));du.polygon(poly,fill=(44,52,48,255))
 known.save(out/f'{id}.png');unknown.save(out/f'{id}_unknown.png');count+=1
assert count==116,count
print(f'Journal portraits: {count} known illustrations and matching silhouettes.')

# Shareable illustration: real authored geometry, with the same masks used by the Journal.
from PIL import ImageFont
preview=Image.new('RGB',(800,350),(225,216,188));pen=ImageDraw.Draw(preview)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
for i,(id,name) in enumerate([('griffin','Griffin'),('leopard','Jungle Leopard'),('poptart_cat','Poptart Cat'),('ancient_tree_spirit','Ancient Tree Spirit')]):
    for y,suffix in [(32,'_unknown'),(177,'')]:
        icon=Image.open(out/f'{id}{suffix}.png');preview.paste(icon,(36+i*200,y),icon)
    pen.text((100+i*200,314),name,font=font,anchor='mt',fill=(38,44,43))
pen.text((400,5),'Before discovery',font=font,anchor='mt',fill=(38,44,43))
pen.text((400,153),'After discovery',font=font,anchor='mt',fill=(38,44,43))
p=ROOT/'art/journal/discovery-preview.png';p.parent.mkdir(parents=True,exist_ok=True);preview.save(p)
for entry in entries:
    if not entry['entity']:continue
    a=Image.open(out/f"{entry['entity']}.png");b=Image.open(out/f"{entry['entity']}_unknown.png")
    assert a.size==b.size==(128,128) and a.getbbox() and a.getchannel('A').tobytes()==b.getchannel('A').tobytes()
print('PASS: all portrait masks match their illustrated models.')
