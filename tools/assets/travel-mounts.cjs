// Deterministic Java geometry and editable Blockbench sources for movement prototypes.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const { Rig } = require('./rig.cjs');
process.chdir(path.resolve(__dirname, '../..'));
function crc(bytes) {
  let c = 0xffffffff;
  for (const b of bytes) { c ^= b; for (let i=0;i<8;i++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0); }
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const name = Buffer.from(type), len = Buffer.alloc(4), sum = Buffer.alloc(4);
  len.writeUInt32BE(data.length); sum.writeUInt32BE(crc(Buffer.concat([name,data])));
  return Buffer.concat([len,name,data,sum]);
}
for (const [id, name] of [['woolly_rhino','WoollyRhino'],['flying_squirrel','FlyingSquirrel'],['poptart_cat','PoptartCat'],['dragon_mount','DragonMount'],['winged_unicorn','WingedUnicorn'],['riding_golem','RidingGolem'],['sasquatch','Sasquatch'],['cerberus','Cerberus']]) {
  const squirrel=id==='flying_squirrel', cat=id==='poptart_cat', dragon=id==='dragon_mount', unicorn=id==='winged_unicorn';
  const biped=id==='riding_golem'||id==='sasquatch', golem=id==='riding_golem', cerberus=id==='cerberus';
  const winged=squirrel||dragon||unicorn;
  const rig = new Rig({id,height:squirrel?1:1.8,width:squirrel?1.2:1.7});
  rig.model.meta.model_format = 'free'; rig.model.resolution = {width:128,height:128};
  const lines = [], parts = new Map();
  const num = n => Number.isInteger(n) ? String(n) : `${n}F`;
  function part(name,parent,pivot,cubes) {
    const absolute = pivot.map((v,i)=>v+(parts.get(parent)?.[i]||0)); parts.set(name,absolute);
    rig.bone(name,[absolute[0]/16,(24-absolute[1])/16,absolute[2]/16],parent==='root'?undefined:parent);
    let builder='CubeListBuilder.create()';
    for (const [label,u,v,x,y,z,w,h,d] of cubes) {
      if(u+2*(w+d)>128 || v+d+h>128) throw Error('UV overflow '+label);
      builder+=`\n            .texOffs(${u}, ${v}).addBox(${[x,y,z,w,h,d].map(num).join(', ')})`;
      const c=rig.box(name+'_'+label,name,(absolute[0]+x+w/2)/16,(24-absolute[1]-y-h)/16,(absolute[2]+z+d/2)/16,w/16,h/16,d/16); c.uv_offset=[u,v];
    }
    lines.push(`        var ${name} = ${parent}.addOrReplaceChild("${name}", ${builder}, PartPose.offset(${pivot.map(num).join(', ')}));`);
  }
  if (biped) {
    part('body','root',[0,-1,0],[['torso',0,0,-9,-10,-5,18,20,10],['waist',0,42,-6,9,-4,12,4,8]]);
    part('head','root',[0,-12,0],[['skull',0,0,-5,-9,-5,10,10,10],['muzzle',0,42,-4,-3,-7,8,4,3]]);
    part('eyes','head',[0,0,0],[['left',96,100,2,-6,-6,2,2,1],['right',96,100,-4,-6,-6,2,2,1]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part('front_'+s,'root',[side*11,-8,0],[['arm',0,0,-3,-1,-3,6,23,6],['hand',0,42,-4,20,-4,8,5,8]]);
      part('back_'+s,'root',[side*4,11,0],[['leg',0,0,-3,0,-3,6,11,6],['foot',0,42,-3,9,-5,6,4,8]]);
    }
    part('tail','root',[0,3,5],[['back_plate',0,42,-5,-4,0,10,8,1]]);
    if(golem) part('crest','body',[0,0,0],[['crystal',0,70,-2,-4,-6,4,5,2],['left_shoulder',0,42,7,-11,-6,6,7,12],['right_shoulder',0,42,-13,-11,-6,6,7,12]]);
    else part('brows','head',[0,0,0],[['brow',0,0,-5,-8,-6,10,3,2],['beard',0,0,-4,0,-6,8,4,4]]);
  } else if (cat||dragon||unicorn||cerberus) {
    const bodyW=dragon?16:cat?12:10, bodyH=cat?8:10, leg=cat?6:12;
    part('body','root',[0,24-leg-bodyH/2,0],[['torso',0,0,-bodyW/2,-bodyH/2,-11,bodyW,bodyH,22]]);
    if(cat) {
      part('pastry','body',[0,0,0],[['crust',0,42,-8,-6,-10,16,3,20],['icing',0,70,-7,-7,-9,14,1,18]]);
      for(let i=0;i<7;i++) part('sprinkle_'+i,'pastry',[0,0,0],[['sugar',96,70,(i%3)*4-5,-8,Math.floor(i/3)*6-6,2,1,3]]);
    }
    const heads=cerberus?[-9,0,9]:[0];
    for(const x of heads) {
      const h=x===0?'head':x<0?'left_head':'right_head';
      part(h,'root',[x,cat?13:7,-10],[['skull',0,0,-4,-5,-5,8,8,8],['muzzle',0,42,-3,-1,-9,6,4,5]]);
      part(h+'_eyes',h,[0,0,0],[['left',96,100,3,-3,-4,2,2,2],['right',96,100,-5,-3,-4,2,2,2]]);
      part(h+'_ears',h,[0,0,0],[['left',0,0,2,-9,0,2,5,3],['right',0,0,-4,-9,0,2,5,3]]);
      if(cat) part('nose',h,[0,0,0],[['nose',96,100,-1,0,-10,2,2,2]]);
      if(dragon||cerberus) part(h+'_horns',h,[0,0,0],[['left',0,70,3,-10,1,2,7,2],['right',0,70,-5,-10,1,2,7,2]]);
    }
    if(unicorn) {
      part('horn','head',[0,0,0],[['base',0,70,-1,-12,-4,2,8,2],['tip',0,70,-.5,-16,-3.5,1,5,1]]);
      part('mane','head',[0,0,0],[['hair',0,70,-2,-5,2,4,17,4]]);
      part('neck','root',[0,10,-9],[['neck',0,0,-3,-7,-3,6,14,6]]);
    }
    for(const side of [-1,1]) for(const front of [true,false]) {
      const s=(front?'front_':'back_')+(side===1?'left':'right');
      part(s,'root',[side*(bodyW/2-1),24-leg,front?-7:8],[['leg',0,0,-2,0,-2,4,leg-1,4],['foot',96,100,-2,leg-2,-3,4,2,5]]);
    }
    part('tail','root',[0,cat?14:10,10],[['base',0,0,-2,-2,0,4,4,12],['tip',0,70,-1,-1,10,2,2,10]]);
    if(dragon) for(let i=0;i<4;i++) part('spine_'+i,'body',[0,0,0],[['spike',0,70,-1,-9,-6+i*5,2,5,3]]);
    if(winged) for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part(s+'_wing','root',[side*(bodyW/2-1),8,-3],[['span',0,70,side===1?0:-18,-1,-5,18,2,12],['tip',0,70,side===1?16:-29,0,-2,13,1,10]]);
      if(unicorn) for(let i=0;i<6;i++) part(s+'_feather_'+i,s+'_wing',[0,0,0],[['feather',0,0,side===1?8+i*3:-11-i*3,0,5,3,1,12-i]]);
    }
  } else if (squirrel) {
    part('body','root',[0,16,0],[['fur',0,0,-6,-4,-9,12,8,18],['belly',0,42,-5,3,-8,10,2,16]]);
    part('head','root',[0,13,-9],[['face',0,0,-4,-4,-6,8,7,7],['muzzle',0,42,-3,0,-8,6,3,3],['nose',96,100,-1,-1,-9,2,2,2]]);
    part('ears','head',[0,0,0],[['left',0,0,2,-8,-2,3,5,2],['right',0,0,-5,-8,-2,3,5,2]]);
    part('eyes','head',[0,0,0],[['left',96,100,3,-3,-5,2,3,2],['right',96,100,-5,-3,-5,2,3,2]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part(s+'_wing','root',[side*5,16,0],[['membrane',0,70,side===1?0:-10,0,-8,10,1,17]]);
      part('front_'+s,'root',[side*5,18,-6],[['leg',0,0,-2,0,-2,4,5,4],['paw',96,100,-2,4,-3,4,1,5]]);
      part('back_'+s,'root',[side*5,18,6],[['leg',0,0,-2,0,-2,4,5,4],['paw',96,100,-2,4,-3,4,1,5]]);
    }
    part('tail','root',[0,15,8],[['base',0,0,-4,-3,0,8,6,7],['plume',0,0,-6,-6,5,12,9,12],['tip',0,42,-5,-8,14,10,8,5]]);
  } else {
    part('body','root',[0,10,1],[['fur',0,0,-10,-8,-13,20,16,26],['skirt',0,0,-11,5,-11,22,5,23]]);
    part('head','root',[0,9,-13],[['skull',0,0,-6,-4,-10,12,11,12],['muzzle',0,42,-5,1,-15,10,7,7]]);
    part('horn','head',[0,0,0],[['base',0,70,-2,-3,-13,4,6,4],['point',0,70,-1,-8,-12,2,6,2],['small',0,70,-1,-5,-6,2,4,3]]);
    part('eyes','head',[0,0,0],[['left',96,100,5,-2,-7,2,2,2],['right',96,100,-7,-2,-7,2,2,2]]);
    part('ears','head',[0,0,0],[['left',0,42,5,-7,0,4,5,3],['right',0,42,-9,-7,0,4,5,3]]);
    for(const side of [-1,1]) for(const front of [true,false]) {
      const s=(front?'front_':'back_')+(side===1?'left':'right');
      part(s,'root',[side*7,16,front?-8:10],[['leg',0,0,-3,-1,-3,6,8,6],['hoof',96,100,-3,5,-4,6,3,7]]);
    }
    part('tail','root',[0,9,14],[['stem',0,0,-1,-1,0,2,2,8],['tuft',0,42,-2,-1,6,4,4,4]]);
  }
  const raw = Buffer.alloc(128*(128*3+1));
  for(let y=0;y<128;y++) for(let x=0;x<128;x++) {
    const palette=cat?[[145,149,155],[210,160,85],[240,123,177]]:dragon?[[49,91,79],[119,148,103],[180,93,53]]:unicorn?[[227,226,215],[182,176,207],[214,166,91]]:golem?[[119,130,130],[77,95,89],[75,217,186]]:cerberus?[[58,45,46],[111,61,44],[222,103,35]]:squirrel?[[118,66,36],[183,148,105],[173,116,78]]:[[88,69,53],[183,148,105],[224,207,164]];
    const base=x>=96&&y>=100?(cerberus?[245,125,27]:[28,23,20]):palette[y>=70?2:y>=42?1:0];
    const noise=((x*17+y*31)%9-4)*2 - (x%7===0?9:0);
    for(let c=0;c<3;c++) raw[y*385+1+x*3+c]=Math.max(0,Math.min(255,base[c]+noise));
  }
  const header=Buffer.alloc(13); header.writeUInt32BE(128); header.writeUInt32BE(128,4); header[8]=8;header[9]=2;
  const png=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
  const folder=`art/blockbench/${id}`;fs.mkdirSync(folder,{recursive:true});
  fs.writeFileSync(`src/main/resources/assets/darkspawn/textures/entity/${id}.png`,png);
  rig.model.textures=[{name:id+'.png',id:'0',uuid:rig.uuid('texture'),width:128,height:128,uv_width:128,uv_height:128,mode:'bitmap',saved:true,source:'data:image/png;base64,'+png.toString('base64')}];
  rig.clip('idle',2,'loop',[]);
  fs.writeFileSync(`${folder}/${name}.bbmodel`,JSON.stringify(rig.model,null,2)+'\n');
  fs.writeFileSync(`src/client/java/darkspawn/black/client/mount/${name}Model.java`,`package darkspawn.black.client.mount;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Generated by tools/assets/travel-mounts.cjs. */
public final class ${name}Model extends EntityModel<GriffinRenderState> {
    private final ModelPart head, tail;
    private final ModelPart[] legs;
    ${winged?'private final ModelPart leftWing, rightWing;':''}
    public ${name}Model(ModelPart root) {
        super(root); head=root.getChild("head"); tail=root.getChild("tail");
        legs=new ModelPart[]{root.getChild("front_left"),root.getChild("front_right"),root.getChild("back_left"),root.getChild("back_right")};
        ${winged?'leftWing=root.getChild("left_wing"); rightWing=root.getChild("right_wing");':''}
    }
    public static LayerDefinition createBodyLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
${lines.join('\n')}
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(GriffinRenderState state) {
        super.setupAnim(state);
        head.yRot=Math.clamp(state.yRot,-40F,40F)*(float)(Math.PI/180);
        head.xRot=state.xRot*(float)(Math.PI/180);
        for(int i=0;i<legs.length;i++) legs[i].xRot=state.flying?-.4F:(float)Math.cos(state.walkAnimationPos*.65F+(i==0||i==3?0:Math.PI))*state.walkAnimationSpeed*.8F;
        tail.yRot=(float)Math.sin(state.ageInTicks*.07F)*.12F;
        ${winged?(squirrel?'leftWing.zRot=state.flying?-.08F:.85F; rightWing.zRot=-leftWing.zRot; tail.xRot=state.flying?-.1F:-.4F;':'leftWing.zRot=state.flying?(float)Math.sin(state.ageInTicks*.3F)*.45F:.25F; rightWing.zRot=-leftWing.zRot; leftWing.yRot=state.flying?0:-1.25F; rightWing.yRot=-leftWing.yRot;'):''}
    }
}
`.replace(/[ \t]+$/gm, ''));
  console.log(`${id}: ${rig.model.elements.length} cubes, Java model, texture and Blockbench source written.`);
}
