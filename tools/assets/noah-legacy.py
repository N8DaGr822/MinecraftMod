"""Author native cuboid armor/items and GeckoLib hippocampus assets; no downloaded artwork."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / 'src/main/resources/assets/darkspawn'
def write(path, value):
    path = A / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')

palette = [(18,43,69),(24,143,153),(231,184,71),(122,246,229)]
def atlas(size, cell, frames=1):
    im=Image.new('RGBA',(size,size*frames),(0,0,0,255)); d=ImageDraw.Draw(im)
    for frame in range(frames):
        for y in range(size):
            for x in range(size):
                material=(x//cell)%4
                shade=1 if material==2 else .88+((x*7+y*3)%11)*.018
                if material==3 and frame==1: shade*=.75
                c=tuple(min(255,int(v*shade)) for v in palette[material])
                d.point((x,y+frame*size),fill=c+(255,))
                if material==1 and y%8==0 and x%8<5:d.point((x,y+frame*size),fill=(43,182,180,255))
    return im

# Armor uses the existing 256x128 material atlas contract, with an animated sea-rune tile.
im=Image.new('RGBA',(256,256));tile=atlas(256,64)
im.paste(tile.crop((0,0,256,128)),(0,0));im.paste(tile.crop((0,0,256,128)),(0,128))
d=ImageDraw.Draw(im)
for frame in range(2):
    for y in range(0,128,12):
        for x in range(192,256,12):d.line([(x+2,y+frame*128+2),(x+6,y+frame*128+7),(x+10,y+frame*128+2)],fill=(235,255,250,255) if frame==0 else (75,195,192,255),width=2)
(A/'textures/equipment').mkdir(parents=True,exist_ok=True)
im.crop((0,0,256,128)).save(A/'textures/equipment/noah.png')
im.crop((0,128,256,256)).save(A/'textures/equipment/noah_pulse.png')
parts=[]
def part(name,origin):
    p={'name':name,'origin':origin,'cubes':[]};parts.append(p);return p
def box(p,name,slot,xyz,size,material):p['cubes'].append({'name':name,'slot':slot,'from':xyz,'size':size,'material':material})
head=part('head',[0,0,0])
for xyz,size in [([-4.6,-8.8,-4.6],[9.2,2,1]),([-4.6,-8.8,3.6],[9.2,2,1]),([-4.6,-8.8,-3.6],[1,2,7.2]),([3.6,-8.8,-3.6],[1,2,7.2])]:box(head,'sea_crown','helmet',xyz,size,2)
for x,h in [(-3,2),(0,3.5),(3,2)]:box(head,'crown_tine','helmet',[x-.6,-8.8-h,-4.6],[1.2,h,1.1],2)
box(head,'crown_gem','helmet',[-.8,-8.7,-4.9],[1.6,1.6,.6],3)
body=part('body',[0,0,0]);box(body,'royal_cuirass','chestplate',[-4.5,-.3,-2.6],[9,12.6,5.2],0)
box(body,'breastplate','chestplate',[-3.8,1,-3],[7.6,8,.7],1)
box(body,'heart_of_the_sea','chestplate',[-1,3,-3.5],[2,3,.6],3)
for x in [-4,3.3]:box(body,'gold_inlay','chestplate',[x,0,-3],[.7,11,.5],2)
box(body,'heir_belt','leggings',[-4.6,10,-2.7],[9.2,2.4,5.4],2)
for name,x in [('right_arm',-5),('left_arm',5)]:
    p=part(name,[x,2,0]);box(p,'wave_pauldron','chestplate',[-2.8,-2.8,-2.8],[5.6,4.2,5.6],1)
    box(p,'pauldron_trim','chestplate',[-3,-3.2,-3],[6,.7,6],2)
    box(p,'bracer','chestplate',[-2.4,5,-2.4],[4.8,4.7,4.8],0)
    box(p,'bracer_rune','chestplate',[-1,6,-2.7],[2,2.5,.5],3)
for name,x in [('right_leg',-1.9),('left_leg',1.9)]:
    p=part(name,[x,12,0]);box(p,'abyssal_greave','leggings',[-2.25,0,-2.3],[4.5,8.4,4.6],1)
    box(p,'greave_inlay','leggings',[-.4,1,-2.7],[.8,6,.5],2)
    box(p,'wavewalker','boots',[-2.4,8,-3],[4.8,4.3,5.4],0)
    box(p,'royal_boot_cuff','boots',[-2.6,7.8,-2.6],[5.2,1.1,5.2],2)
    box(p,'tide_step','boots',[-2.4,11.5,-3.1],[4.8,.8,5.5],3)
write('geometry/armor/noah.json',{'texture_width':256,'texture_height':128,'parts':parts})
write('equipment/noah.json',{'layers':{'humanoid':[{'texture':'minecraft:netherite'}],'humanoid_leggings':[{'texture':'minecraft:netherite'}]}})

# Thalassar: horse head/forelegs, plated chest, articulated fish tail and broad caudal fins.
(A/'textures/entity').mkdir(parents=True,exist_ok=True);atlas(128,32).save(A/'textures/entity/thalassar.png')
bones=[]
def bone(name,parent,pivot,cubes):
    b={'name':name,'pivot':pivot,'cubes':[]}
    if parent:b['parent']=parent
    for xyz,size,mat in cubes:
        b['cubes'].append({'origin':xyz,'size':size,'uv':{f:{'uv':[mat*32,0],'uv_size':[16,16]} for f in ['north','south','east','west','up','down']}})
    bones.append(b)
bone('root',None,[0,0,0],[])
bone('body','root',[0,18,0],[([-6,12,-10],[12,12,18],1),([-6.4,22,-7],[12.8,2,11],2),([-4.5,23,-5],[9,1.2,8],0)])
bone('neck','body',[0,23,-7],[([-4,21,-13],[8,12,7],1),([-1,23,-6.8],[2,12,1.2],3)])
bone('head','neck',[0,31,-13],[([-4,29,-19],[8,7,12],1),([-3,27.5,-23],[6,5,7],1),([-4.5,32,-17],[9,1.5,5],2),([-4.2,32,-19.3],[1,1.7,1],3),([3.2,32,-19.3],[1,1.7,1],3),([-3.3,35,-14],[1.8,5,2],2),([1.5,35,-14],[1.8,5,2],2)])
bone('left_leg','body',[4,14,-6],[([2.5,1,-7.5],[3,13,3],1),([2,0,-8],[4,3,5],2)])
bone('right_leg','body',[-4,14,-6],[([-5.5,1,-7.5],[3,13,3],1),([-6,0,-8],[4,3,5],2)])
bone('tail','body',[0,17,6],[([-5,13,6],[10,9,12],1),([-.5,21,8],[1,5,8],3)])
bone('tail_tip','tail',[0,17,17],[([-3,14,17],[6,6,10],1)])
bone('fin','tail_tip',[0,17,26],[([-10,16,26],[20,1.3,7],3),([-7,15.7,25],[14,1.8,3],2)])
write('geckolib/models/entity/thalassar.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.thalassar','texture_width':128,'texture_height':128,'visible_bounds_width':5,'visible_bounds_height':4,'visible_bounds_offset':[0,1.5,0]},'bones':bones}]})
def rot(values):return {'rotation':{str(t):v for t,v in values}}
animations={}
def anim(name,length,tracks,loop=True):animations['animation.thalassar.'+name]={'loop':loop,'animation_length':length,'bones':tracks}
anim('idle',3,{'head':rot([(0,[0,0,0]),(1.5,[3,0,0]),(3,[0,0,0])]),'tail':rot([(0,[0,-5,0]),(1.5,[0,5,0]),(3,[0,-5,0])])})
anim('walk',1,{'left_leg':rot([(0,[25,0,0]),(.5,[-25,0,0]),(1,[25,0,0])]),'right_leg':rot([(0,[-25,0,0]),(.5,[25,0,0]),(1,[-25,0,0])]),'tail':rot([(0,[0,-8,0]),(.5,[0,8,0]),(1,[0,-8,0])])})
anim('swim',1.4,{'left_leg':rot([(0,[-55,0,0]),(.7,[-40,0,0]),(1.4,[-55,0,0])]),'right_leg':rot([(0,[-40,0,0]),(.7,[-55,0,0]),(1.4,[-40,0,0])]),'tail':rot([(0,[0,-15,0]),(.7,[0,15,0]),(1.4,[0,-15,0])]),'tail_tip':rot([(0,[0,20,0]),(.7,[0,-20,0]),(1.4,[0,20,0])]),'fin':rot([(0,[10,0,0]),(.7,[-10,0,0]),(1.4,[10,0,0])])})
anim('dash',.75,{'neck':rot([(0,[0,0,0]),(.15,[15,0,0]),(.75,[0,0,0])]),'tail':rot([(0,[0,-25,0]),(.2,[0,25,0]),(.4,[0,-25,0]),(.6,[0,25,0]),(.75,[0,0,0])])},False)
anim('summon',1,{'root':{'scale':{'0':[.05,.05,.05],'.5':[1.08,1.08,1.08],'1':[1,1,1]}},'head':rot([(0,[15,0,0]),(.5,[-10,0,0]),(1,[0,0,0])])},False)
write('geckolib/animations/entity/thalassar.animation.json',{'format_version':'1.8.0','geckolib_format_version':2,'animations':animations})

# Item silhouettes are authored at pixel scale; the signature trident uses a native cuboid model.
(A/'textures/item').mkdir(parents=True,exist_ok=True)
ids=['noah_helmet','noah_chestplate','noah_leggings','noah_boots','tideborn_conch','legacy_of_noah']
for id in ids:
    icon=Image.new('RGBA',(32,32));d=ImageDraw.Draw(icon)
    if id.endswith('helmet'):
        d.rectangle((5,15,27,24),fill=palette[2]);d.polygon([(5,15),(5,6),(11,13),(16,3),(21,13),(27,6),(27,15)],fill=palette[2]);d.rectangle((14,16,18,20),fill=palette[3])
    elif id.endswith('chestplate'):
        d.polygon([(3,6),(10,4),(12,9),(20,9),(22,4),(29,6),(27,15),(23,14),(23,28),(9,28),(9,14),(5,15)],fill=palette[1]);d.rectangle((10,24,22,27),fill=palette[2]);d.rectangle((14,12,18,18),fill=palette[3])
    elif id.endswith('leggings'):
        d.polygon([(7,4),(25,4),(25,28),(18,28),(18,13),(14,13),(14,28),(7,28)],fill=palette[1]);d.rectangle((7,4,25,8),fill=palette[2])
    elif id.endswith('boots'):
        for x in [4,18]:d.rectangle((x,8,x+8,25),fill=palette[0]);d.rectangle((x-1,23,x+9,28),fill=palette[3]);d.rectangle((x,7,x+8,10),fill=palette[2])
    elif id=='tideborn_conch':
        d.polygon([(4,24),(8,10),(17,4),(26,10),(29,21),(23,27),(12,28)],fill=palette[2]);d.arc((9,8,26,25),0,310,fill=palette[1],width=3);d.arc((14,12,23,22),30,330,fill=palette[0],width=2);d.rectangle((4,22,9,27),fill=palette[3])
    else:
        d.rectangle((4,9,28,28),fill=palette[0]);d.rectangle((3,6,29,12),fill=palette[1]);d.rectangle((14,6,18,28),fill=palette[2]);d.polygon([(16,7),(7,2),(7,8),(25,8),(25,2)],fill=palette[2])
    icon.save(A/f'textures/item/{id}.png');write(f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'darkspawn:item/{id}'}})
    write(f'items/{id}.json',{'model':{'type':'minecraft:model','model':f'darkspawn:item/{id}'}})
elements=[]
def element(a,b,material):elements.append({'from':a,'to':b,'faces':{f:{'uv':[0,0,16,16],'texture':'#'+material} for f in ['north','south','east','west','up','down']}})
for name,color in [('tide',palette[1]),('royal',palette[2]),('rune',palette[3])]:Image.new('RGBA',(16,16),color+(255,)).save(A/f'textures/item/noah_{name}.png')
element([7.4,-12,7.4],[8.6,17,8.6],'tide');element([3.5,16,7.2],[12.5,18,8.8],'royal')
for x,top in [(3.5,27),(7.3,30),(11,27)]:element([x,17,7.3],[x+1.5,top,8.7],'royal');element([x,top-3,7.2],[x+1.5,top,8.8],'rune')
display={'gui':{'rotation':[0,0,-35],'translation':[0,0,0],'scale':[.34,.34,.34]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.4,.4,.4]}}
for side in ['righthand','lefthand']:
    display['thirdperson_'+side]={'rotation':[0,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]}
    display['firstperson_'+side]={'rotation':[0,0,-10],'translation':[1,-1,0],'scale':[.65,.65,.65]}
write('models/item/noahs_tempest.json',{'textures':{x:'darkspawn:item/noah_'+y for x,y in [('tide','tide'),('royal','royal'),('rune','rune')]},'elements':elements,'display':display})
write('items/noahs_tempest.json',{'model':{'type':'minecraft:model','model':'darkspawn:item/noahs_tempest'}})

lang=json.loads((A/'lang/en_us.json').read_text(encoding='utf-8'))
names={'noah_helmet':'Crown of the Tideborn Heir','noah_chestplate':'Cuirass of the Ocean Heir','noah_leggings':'Greaves of the Abyssal Prince','noah_boots':'Stride of the Endless Sea','noahs_tempest':"Noah’s Tempest",'tideborn_conch':'Conch of the Tideborn Heir','legacy_of_noah':'The Legacy of Noah'}
lores={'noah_helmet':'The sea placed its crown upon Noah’s brow. Water breathing; underwater sight.','noah_chestplate':'Forged for the abyss and fury of storms. Strength and resistance in water/rain.','noah_leggings':'No trench too deep. No wave too wild. Swift underwater movement.','noah_boots':'The tides carry the chosen heir. Fall protection; surface glide. Sneak to dive.','noahs_tempest':'The storm bends to Noah’s hand. Sneak-use: select power. Cast/ultimate keys: Controls → Legacy of Noah.','tideborn_conch':'Heard in the deepest trenches. Use: summon/recall. Sneak-use: rest. Defeat recovery: 30s.','legacy_of_noah':'A gift for Noah, Heir of Poseidon. Use to receive and bond all six relics.'}
for id,name in names.items():lang['item.darkspawn.'+id]=name;lang['tooltip.darkspawn.'+id]=lores[id]
lang.update({'entity.darkspawn.thalassar':'Thalassar, Steed of the Tideborn Heir','tooltip.darkspawn.noah_bond':'Divine powers bind to the first wearer or user.','key.category.darkspawn.noah':'The Legacy of Noah','key.darkspawn.noah_cast':'Cast selected Tempest power','key.darkspawn.noah_ultimate':'Wrath of the Seventh Sea','message.darkspawn.thalassar_controls':'Thalassar: WASD to ride, Space/X for depth, mount ability key for Abyssal Dash.'})
write('lang/en_us.json',lang)
progress=[('chosen','Chosen by the Sea','Receive a relic of Noah, Heir of Poseidon.','tideborn_conch'),('crowned','Crowned by the Ocean','Wear the complete Divine Regalia.','noah_helmet'),('storm','The Storm Answers','Unleash a power of Noah’s Tempest.','noahs_tempest'),('ride','Ride of the Tideborn','Ride Thalassar, steed of the chosen heir.','tideborn_conch'),('heir','Heir of Poseidon','Awaken Noah’s Dominion with armor, trident and steed.','noah_chestplate'),('legacy','Legacy of Noah','Command the Wrath of the Seventh Sea.','legacy_of_noah')]
for index,(id,title,description,icon) in enumerate(progress):
    adv={'display':{'title':{'text':title},'description':{'text':description},'icon':{'id':'darkspawn:'+icon},'frame':'challenge' if id=='legacy' else 'task','announce_to_chat':False},'criteria':{'earned':{'trigger':'minecraft:impossible'}},'requirements':[['earned']]}
    if index:adv['parent']='darkspawn:noah/'+progress[index-1][0]
    else:adv['display']['background']='minecraft:gui/advancements/backgrounds/stone'
    path=ROOT/f'src/main/resources/data/darkspawn/advancement/noah/{id}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(adv,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Authored Noah armor, six relic models, Thalassar geometry/animations and lore.')
