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
for (const [id, name] of [['woolly_rhino','WoollyRhino'],['flying_squirrel','FlyingSquirrel'],['poptart_cat','PoptartCat'],['dragon_mount','DragonMount'],['winged_unicorn','WingedUnicorn'],['riding_golem','RidingGolem'],['sasquatch','Sasquatch'],['cerberus','Cerberus'],['leopard','Leopard'],['seahorse','Seahorse'],['giant_scorpion','GiantScorpion'],['crocodile','Crocodile'],['giant_mole','GiantMole']]) {
  const squirrel=id==='flying_squirrel', cat=id==='poptart_cat', dragon=id==='dragon_mount', unicorn=id==='winged_unicorn';
  const biped=id==='riding_golem'||id==='sasquatch', golem=id==='riding_golem', cerberus=id==='cerberus';
  const leopard=id==='leopard', seahorse=id==='seahorse', scorpion=id==='giant_scorpion', crocodile=id==='crocodile';
  const mole=id==='giant_mole';
  if (process.argv.includes('--attacks-only') && !['dragon_mount','woolly_rhino','giant_scorpion','crocodile'].includes(id)) continue;
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
  if (mole) {
    part('body','root',[0,14,1],[['fur',0,0,-9,-6,-12,18,12,25],['shoulders',0,0,-10,-5,-10,20,12,12],['belly',0,42,-7,4,-10,14,4,21]]);
    part('head','root',[0,14,-12],[['skull',0,0,-6,-5,-7,12,10,10],['snout',0,42,-4,-1,-13,8,5,8],['nose',0,70,-3,-1,-15,6,4,3]]);
    part('eyes','head',[0,0,0],[['left',96,100,5,-2,-6,1,1,2],['right',96,100,-6,-2,-6,1,1,2]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part('front_'+s,'root',[side*8,17,-7],[['arm',0,0,-3,-2,-3,6,7,7],['palm',0,42,-5,3,-7,10,3,9],...[-3,0,3].map((x,i)=>['claw_'+i,0,70,x-1,4,-12,2,2,7])]);
      part('back_'+s,'root',[side*6,19,9],[['leg',0,0,-3,-1,-3,6,5,6],['foot',0,42,-3,3,-5,6,2,8]]);
    }
    part('tail','root',[0,17,13],[['stem',0,42,-1.5,-1,0,3,3,9],['tip',0,70,-1,-.5,8,2,2,4]]);
  } else if (leopard) {
    part('body','root',[0,12,0],[['torso',0,0,-5,-5,-11,10,10,23],['chest',0,42,-4,1,-10,8,4,12],['haunch',0,0,-6,-3,5,12,9,8]]);
    part('head','root',[0,10,-12],[['skull',0,0,-4,-4,-5,8,8,8],['muzzle',0,42,-3,0,-8,6,4,4],['nose',96,100,-1,0,-8.5,2,1,1]]);
    part('ears','head',[0,0,0],[['left',0,0,2,-7,-1,3,4,2],['right',0,0,-5,-7,-1,3,4,2],['left_inner',0,42,3,-6,-1.5,1,2,1],['right_inner',0,42,-4,-6,-1.5,1,2,1]]);
    part('eyes','head',[0,0,0],[['left',0,70,2,-2,-5.5,2,1,1],['right',0,70,-4,-2,-5.5,2,1,1]]);
    for(const side of [-1,1]) for(const front of [true,false]) part((front?'front_':'back_')+(side===1?'left':'right'),'root',[side*4,15,front?-8:8],[['leg',0,0,-1.5,0,-2,3,8,4],['paw',0,42,-2,7,-3,4,2,6]]);
    part('tail','root',[0,10,11],[['base',0,0,-1,-1,0,2,2,12],['curve',0,0,-1,-4,10,2,4,3],['tip',96,100,-1,-6,9,2,3,3]]);
  } else if (seahorse) {
    part('body','root',[0,8,0],[['torso',0,0,-5,-6,-5,10,15,11],['belly',0,42,-4,-4,-6,8,12,2],['neck',0,0,-3,-12,-3,6,8,7]]);
    part('head','root',[0,-3,-2],[['skull',0,0,-4,-5,-4,8,9,9],['snout',0,42,-2,-1,-13,4,4,10],['mouth',96,100,-1,-.5,-13.5,2,3,1]]);
    part('eyes','head',[0,0,0],[['left',96,100,3,-3,-4,2,2,2],['right',96,100,-5,-3,-4,2,2,2]]);
    part('crest','head',[0,0,0],[['crown',0,70,-1,-8,0,2,4,5],['crown_tip',0,70,-.5,-10,2,1,3,2]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part('front_'+s,'root',[side*5,5,1],[['fin',0,70,side===1?0:-7,-3,0,7,9,1]]);
      part('back_'+s,'root',[side*3,12,5],[['lower_fin',0,70,side===1?0:-4,0,0,4,6,1]]);
    }
    for(let i=0;i<4;i++) part('ridge_'+i,'body',[0,0,0],[['spine',0,70,-1,-4+i*3,6,2,2,3]]);
    part('tail','root',[0,16,1],[['stem',0,0,-2,0,-1,4,6,5],['curl_bottom',0,0,-2,5,-7,4,3,8],['curl_front',0,0,-2,1,-9,4,5,3],['curl_tip',0,42,-1,0,-8,2,2,4]]);
  } else if (scorpion) {
    part('body','root',[0,16,1],[['carapace',0,0,-8,-5,-10,16,8,22],['plate',0,42,-7,-7,-7,14,3,17]]);
    part('head','root',[0,16,-10],[['face',0,0,-5,-3,-5,10,5,7],['left_eye',96,100,2,-4,-5,2,2,1],['right_eye',96,100,-4,-4,-5,2,2,1]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      for(let i=0;i<4;i++) {
        const n=(i===0?'front_':i===3?'back_':'middle_'+(i===1?'front_':'back_'))+s;
        part(n,'root',[side*7,18,-6+i*5],[['leg',0,0,side===1?0:-9,0,-1,9,2,3],['foot',0,42,side===1?7:-9,1,-2,2,5,4]]);
      }
      part('claw_'+s,'root',[side*6,16,-10],[['arm',0,0,-2,-1,-11,4,4,12],['palm',0,42,-4,-2,-17,8,5,7],['outer_finger',0,0,side===1?2:-4,-1,-23,2,3,7],['inner_finger',0,0,side===1?-3:1,-1,-21,2,3,5]]);
    }
    part('tail','root',[0,15,12],[['base',0,0,-3,-2,0,6,5,8],['rise',0,0,-2,-12,5,4,12,4],['arch',0,42,-2,-17,-1,4,6,8],['hook',0,0,-1.5,-16,-5,3,4,6],['stinger',96,100,-.5,-13,-5,1,6,1]]);
  } else if (crocodile) {
    part('body','root',[0,17,0],[['torso',0,0,-7,-4,-14,14,8,28],['belly',0,42,-6,2,-12,12,3,24]]);
    part('head','root',[0,17,-14],[['skull',0,0,-5,-3,-7,10,7,10],['snout',0,0,-4,-1,-20,8,4,15],['left_eye',96,100,3,-4,-7,2,2,2],['right_eye',96,100,-5,-4,-7,2,2,2]]);
    part('jaw','head',[0,3,-5],[['lower_jaw',0,42,-4,0,-15,8,2,16]]);
    for(const side of [-1,1]) for(const front of [true,false]) part((front?'front_':'back_')+(side===1?'left':'right'),'root',[side*6,19,front?-8:9],[['leg',0,0,side===1?0:-6,0,-2,6,3,5],['foot',0,42,side===1?3:-7,2,-4,4,3,7]]);
    for(const side of [-1,1]) for(let i=0;i<6;i++) part('scute_'+(side===1?'left':'right')+'_'+i,'body',[0,0,0],[['plate',0,70,side*3-1,-6,-11+i*4,2,3,3]]);
    part('tail','root',[0,17,13],[['base',0,0,-5,-3,0,10,6,10],['middle',0,0,-3,-2,8,6,4,12],['tip',0,42,-1,-1,18,2,2,10]]);
  } else if (biped) {
    part('body','root',[0,-1,0],[['torso',0,0,-9,-10,-5,18,20,10],['waist',0,42,-6,9,-4,12,4,8],
      ...(golem?[['lower_plate',0,42,-8,3,-6,16,6,2]]:[['hunched_back',0,0,-8,-13,1,16,9,7],['chest_fur',0,42,-6,-6,-6,12,10,2]])]);
    part('head','root',[0,golem?-12:-10,golem?0:-3],[['skull',0,0,-5,-9,-5,10,10,10],['muzzle',0,42,-4,-3,-7,8,4,3]]);
    part('eyes','head',[0,0,0],[['left',96,100,2,-6,-6,2,2,1],['right',96,100,-4,-6,-6,2,2,1]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part('front_'+s,'root',[side*11,-8,0],[['upper_arm',0,0,-3,-1,-3,6,11,6],['forearm',0,0,-3.5,11,-3.5,7,10,7],['hand',0,42,-4,20,-4,8,5,8],
        ...(golem?[['elbow_joint',96,100,-2,8,-2,4,5,4]]:[['knuckles',0,42,-4,22,-5,8,3,2],['fur',0,0,-4,4,0,8,7,5]])]);
      part('back_'+s,'root',[side*4,11,0],[['leg',0,0,-3,0,-3,6,11,6],['foot',0,42,-3,9,-5,6,4,8]]);
    }
    part('tail','root',[0,3,5],[['back_plate',0,42,-5,-4,0,10,8,1]]);
    if(golem) part('crest','body',[0,0,0],[['crystal',0,70,-2,-4,-6,4,5,2],['left_shoulder',0,42,7,-11,-6,6,7,12],['right_shoulder',0,42,-13,-11,-6,6,7,12]]);
    else part('brows','head',[0,0,0],[['brow',0,0,-5,-8,-6,10,3,2],['beard',0,0,-4,0,-6,8,4,4],['beard_tip',0,0,-2,3,-5,4,3,3]]);
  } else if (cat||dragon||unicorn||cerberus) {
    const bodyW=dragon?16:cat?12:cerberus?14:10, bodyH=cat?10:10, leg=cat?6:12;
    part('body','root',[0,24-leg-bodyH/2,0],[['torso',0,0,-bodyW/2,-bodyH/2,-11,bodyW,bodyH,22]]);
    if(cat) {
      // Nyan Cat: Pastry is the body, with pink frosting on both broad sides and the top.
      part('pastry','body',[0,0,0],[['crust',0,42,-8,-7,-10,16,13,20],['icing_top',0,70,-7,-7.5,-9,14,1,18],
        ['icing_left',0,70,8,-6,-9,1,10,18],['icing_right',0,70,-9,-6,-9,1,10,18]]);
      for(const side of [-1,1]) for(let i=0;i<9;i++) part('sprinkle_'+(side===1?'left':'right')+'_'+i,'pastry',[0,0,0],[['sugar',96,70,side===1?9:-9.5,-4+(i%3)*3,-7+Math.floor(i/3)*6,.5,1,2]]);
    }
    const heads=cerberus?[-9,0,9]:[0];
    for(const x of heads) {
      const h=x===0?'head':x<0?'left_head':'right_head';
      part(h,'root',[x,cat?12:unicorn?1:7,unicorn?-15:-10],[['skull',0,0,-4,-5,-5,8,8,8],
        ['muzzle',cat?0:0,cat?0:42,cat?-3:dragon?-3.5:-3,cat?0:-1,cat?-6:dragon?-13:-9,cat?6:dragon?7:6,cat?3:4,cat?2:dragon?9:5]]);
      if(!cat) part(h+'_eyes',h,[0,0,0],[['left',96,100,3,-3,-4,2,2,2],['right',96,100,-5,-3,-4,2,2,2]]);
      part(h+'_ears',h,[0,0,0],[['left',0,0,2,-9,0,2,5,3],['right',0,0,-4,-9,0,2,5,3]]);
      if(cat) {
        part('nose',h,[0,0,0],[['nose',96,100,-.5,0,-6.5,1,1,1],['smile',96,100,-1,2,-6.5,2,.5,1],['left_cheek',0,70,2,0,-6.1,1,1,.5],['right_cheek',0,70,-3,0,-6.1,1,1,.5]]);
        part('face_eyes',h,[0,0,0],[['left',96,100,2,-2,-5.5,1,2,1],['right',96,100,-3,-2,-5.5,1,2,1]]);
      }
      if(dragon) {
        part('jaw',h,[0,3,-4],[['lower_jaw',0,42,-3,0,-8,6,2,8]]);
        part('nostrils',h,[0,0,0],[['nostril_left',96,100,2,-1,-13.5,1,1,1],['nostril_right',96,100,-3,-1,-13.5,1,1,1]]);
      }
      if(cerberus) part(h+'_neck','root',[x*.65,7,-7],[['neck',0,0,-4,-3,-5,8,9,9]]);
      if(dragon||cerberus) part(h+'_horns',h,[0,0,0],[['left',0,70,3,-10,1,2,7,2],['right',0,70,-5,-10,1,2,7,2]]);
    }
    if(unicorn) {
      part('horn','head',[0,0,0],[['base',0,70,-1,-12,-4,2,8,2],['tip',0,70,-.5,-16,-3.5,1,5,1]]);
      part('mane','head',[0,0,0],[['hair',0,70,-2,-5,2,4,17,4]]);
      part('neck','root',[0,10,-11],[['neck',0,0,-3,-12,-3,6,15,7],['withers',0,0,-4,-6,1,8,8,6]]);
      part('mane_lower','neck',[0,0,0],[['hair',0,70,-1.5,-11,4,3,15,3]]);
    }
    for(const side of [-1,1]) for(const front of [true,false]) {
      const s=(front?'front_':'back_')+(side===1?'left':'right');
      part(s,'root',[side*(bodyW/2-1),24-leg,front?-7:8],[['leg',0,0,-2,0,-2,4,leg-1,4],['foot',96,100,-2,leg-2,-3,4,2,5]]);
    }
    part('tail','root',[0,cat?14:10,10],cat?[['base',0,0,-1.5,-1.5,0,3,3,8],['curl',0,0,-1.5,-4,6,3,4,3]]:
      unicorn?[['dock',0,0,-1.5,-2,0,3,3,5],['hair',0,70,-2,0,3,4,11,5],['tip',0,70,-1.5,10,4,3,3,3]]:
      [['base',0,0,-2.5,-2.5,0,5,5,8],['middle',0,0,-1.5,-1.5,7,3,3,9]]);
    if(dragon||cerberus) part('tail_tip','tail',[0,0,15],[['tip',0,70,-1,-1,0,2,2,dragon?12:6]]);
    if(dragon) for(let i=0;i<4;i++) part('spine_'+i,'body',[0,0,0],[['spike',0,70,-1,-9,-6+i*5,2,5,3]]);
    if(winged) for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part(s+'_wing','root',[side*(bodyW/2-1),8,-3],[['arm',0,0,side===1?0:-12,-1,-4,12,2,3],['inner_membrane',0,70,side===1?0:-12,0,-1,12,1,10]]);
      part(s+'_tip',s+'_wing',[side*11,0,-2],[['leading_edge',0,0,side===1?0:-15,-1,-2,15,2,2],['outer_membrane',0,70,side===1?0:-15,0,0,15,1,8]]);
      for(let i=0;i<5;i++) part(s+'_feather_'+i,s+'_tip',[0,0,0],[[unicorn?'feather':'finger',unicorn?0:0,unicorn?0:42,side===1?i*3:-3-i*3,0,unicorn?5:0,unicorn?3:1,1,(unicorn?13:11)-i]]);
    }
  } else if (squirrel) {
    part('body','root',[0,16,0],[['fur',0,0,-6,-4,-9,12,8,18],['belly',0,42,-5,3,-8,10,2,16]]);
    part('head','root',[0,13,-9],[['face',0,0,-4,-4,-6,8,7,7],['muzzle',0,42,-3,0,-8,6,3,3],['nose',96,100,-1,-1,-9,2,2,2]]);
    part('ears','head',[0,0,0],[['left',0,0,2,-8,-2,3,5,2],['right',0,0,-5,-8,-2,3,5,2]]);
    part('eyes','head',[0,0,0],[['left',96,100,3,-3,-5,2,3,2],['right',96,100,-5,-3,-5,2,3,2]]);
    for(const side of [-1,1]) {
      const s=side===1?'left':'right';
      part(s+'_wing','root',[side*5,16,0],[['membrane',0,70,side===1?0:-7,0,-7,7,1,14],['edge',0,42,side===1?6:-7,0,-6,1,1,12]]);
      part('front_'+s,'root',[side*5,18,-6],[['leg',0,0,-2,0,-2,4,5,4],['paw',96,100,-2,4,-3,4,1,5]]);
      part('back_'+s,'root',[side*5,18,6],[['leg',0,0,-2,0,-2,4,5,4],['paw',96,100,-2,4,-3,4,1,5]]);
    }
    part('tail','root',[0,15,8],[['base',0,0,-3,-3,0,6,6,6],['plume',0,0,-5,-6,4,10,8,8],['upper_plume',0,0,-4,-10,10,8,8,6],['tip',0,42,-2.5,-12,14,5,6,4]]);
  } else {
    part('body','root',[0,10,1],[['fur',0,0,-10,-8,-13,20,16,26],['skirt',0,0,-11,5,-11,22,5,23],['shoulder_hump',0,0,-8,-12,-10,16,7,12]]);
    part('head','root',[0,9,-13],[['skull',0,0,-6,-4,-10,12,11,12],['muzzle',0,42,-5,1,-15,10,7,7]]);
    part('horn','head',[0,0,0],[['base',0,70,-2,-3,-13,4,5,4],['middle',0,70,-1.5,-6,-14,3,4,3],['point',0,70,-.5,-9,-14.5,1,4,1],['small',0,70,-1,-5,-6,2,4,3]]);
    for(const side of [-1,1]) for(let i=0;i<4;i++) part('fur_'+(side===1?'left':'right')+'_'+i,'body',[0,0,0],[['tuft',0,0,side===1?9:-11,6,-9+i*6,2,4+(i%2),4]]);
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
    const palette=mole?[[67,57,54],[153,119,108],[215,195,157]]:leopard?[[203,153,67],[228,206,151],[127,180,75]]:seahorse?[[42,141,145],[221,177,82],[97,205,188]]:scorpion?[[126,83,40],[191,140,69],[85,49,31]]:crocodile?[[68,103,62],[157,157,96],[45,72,41]]:cat?[[145,149,155],[210,160,85],[240,123,177]]:dragon?[[49,91,79],[119,148,103],[180,93,53]]:unicorn?[[227,226,215],[182,176,207],[214,166,91]]:golem?[[119,130,130],[77,95,89],[75,217,186]]:cerberus?[[58,45,46],[111,61,44],[222,103,35]]:squirrel?[[118,66,36],[183,148,105],[173,116,78]]:[[88,69,53],[183,148,105],[224,207,164]];
    let base=x>=96&&y>=100?(cerberus?[245,125,27]:[28,23,20]):palette[y>=70?2:y>=42?1:0];
    // Material Detail: Scales, stone seams and directional fur use distinct pixel patterns.
    const hash=(x*13+y*29+(x*y)%17)%13;
    let noise=hash-6;
    if(y<42) {
      if(leopard) { const r=(x%9-4)**2+(y%8-4)**2; if(r>=5&&r<=11)base=[54,39,26]; }
      else if(scorpion||seahorse||crocodile) noise+=(y%5===0||((x+Math.floor(y/5)*3)%9===0))?-22:5;
      else if(dragon) noise+=(y%5===0||((x+(Math.floor(y/5)%2)*3)%6===0))?-20:7;
      else if(golem) noise+=((x+Math.floor(y/8)*5)%17===0||y%13===0)?-29:(hash<3?10:0);
      else if(!cat) noise+=((x+Math.floor(y/3))%5===0)?-17:(y%4===0?8:0);
    }
    if(cat&&y>=70&&y<100) { base=[248,145,187]; noise=hash<2?-8:2; if(x>=96||((x*7+y*11)%47<3))base=[188,47,108]; }
    if(cat&&y>=42&&y<70)noise+=(y%6===0?-16:7);
    if(dragon&&y>=70&&y<100)noise-=(x%9===0?24:0);
    for(let c=0;c<3;c++) raw[y*385+1+x*3+c]=Math.max(0,Math.min(255,base[c]+noise));
  }
  const header=Buffer.alloc(13); header.writeUInt32BE(128); header.writeUInt32BE(128,4); header[8]=8;header[9]=2;
  const png=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
  const folder=`art/blockbench/${id}`;fs.mkdirSync(folder,{recursive:true});
  fs.writeFileSync(`src/main/resources/assets/darkspawn/textures/entity/${id}.png`,png);
  rig.model.textures=[{name:id+'.png',id:'0',uuid:rig.uuid('texture'),width:128,height:128,uv_width:128,uv_height:128,mode:'bitmap',saved:true,source:'data:image/png;base64,'+png.toString('base64')}];
  // Attack Poses: One transform definition drives runtime offsets and editable Blockbench recovery clips.
  const attack = dragon ? {duration:12, tracks:[['head','rotation',[-.1,0,0]],['head','position',[0,0,-1.5]],['head/jaw','rotation',[.8,0,0]],['left_wing','rotation',[0,0,.12]],['right_wing','rotation',[0,0,-.12]]]} :
    crocodile ? {duration:8, tracks:[['head','rotation',[-.08,0,0]],['head','position',[0,0,-2.5]],['head/jaw','rotation',[.65,0,0]],['tail','rotation',[0,.18,0]]]} :
    scorpion ? {duration:8, tracks:[['tail','rotation',[1,.45,0]],['claw_left','rotation',[0,-.3,0]],['claw_right','rotation',[0,.3,0]],['head','rotation',[-.12,0,0]]]} :
    id==='woolly_rhino' ? {duration:20, tracks:[['head','rotation',[.32,0,0]],['head','position',[0,0,-2]],['body','rotation',[.05,0,0]],['tail','rotation',[-.3,0,0]]]} : null;
  const attackJava = attack ? '\n        // Attack Recovery: Add to locomotion after the base model resets every part for this entity.\n' + attack.tracks.flatMap(([bone,channel,values]) => values.flatMap((value,i) => value ? [
    `        root()${bone.split('/').map(n=>`.getChild("${n}")`).join('')}.${['x','y','z'][i]}${channel==='rotation'?'Rot':''} += ${num(value)} * state.attackPose;`
  ] : [])).join('\n') : '';
  rig.clip('idle',2,'loop',[]);
  if (attack) rig.clip('attack',attack.duration/20,'once',attack.tracks.map(([bone,channel,values])=>[
    bone.split('/').at(-1),channel,Array.from({length:attack.duration+1},(_,tick)=>{
      const t=Math.min(1,(attack.duration-tick)/(id==='woolly_rhino'?3:attack.duration)),amount=t*t*(3-2*t);
      // Java model Y points down; Blockbench Y points up, so X/Z rotations and Y offsets invert.
      return [tick/20,values.map((value,i)=>value*amount*(channel==='rotation'?180/Math.PI*(i===1?1:-1):(i===1?-1:1)))];
    })
  ]));
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
    ${dragon||unicorn?'private final ModelPart leftTip, rightTip;':''}
    ${cerberus?'private final ModelPart leftHead, rightHead;':''}
    ${dragon||cerberus?'private final ModelPart tailTip;':''}
    public ${name}Model(ModelPart root) {
        super(root); head=root.getChild("head"); tail=root.getChild("tail");
        legs=new ModelPart[]{root.getChild("front_left"),root.getChild("front_right"),root.getChild("back_left"),root.getChild("back_right")${scorpion?',root.getChild("middle_front_left"),root.getChild("middle_front_right"),root.getChild("middle_back_left"),root.getChild("middle_back_right")':''}};
        ${winged?'leftWing=root.getChild("left_wing"); rightWing=root.getChild("right_wing");':''}
        ${dragon||unicorn?'leftTip=leftWing.getChild("left_tip"); rightTip=rightWing.getChild("right_tip");':''}
        ${cerberus?'leftHead=root.getChild("left_head"); rightHead=root.getChild("right_head");':''}
        ${dragon||cerberus?'tailTip=tail.getChild("tail_tip");':''}
    }
    public static LayerDefinition createBodyLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
${lines.join('\n')}
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(GriffinRenderState state) {
        super.setupAnim(state);
        head.yRot=Math.clamp(state.yRot,-40F,40F)*(float)(Math.PI/180);
        head.xRot=Math.clamp(state.xRot,-30F,30F)*(float)(Math.PI/180);
        float flight=state.flightPose;
        for(int i=0;i<legs.length;i++) {
            float walk=(float)Math.cos(state.walkAnimationPos*${golem?'.4F':cerberus?'.8F':'.65F'}+(i==0||i==3?0:Math.PI))*state.walkAnimationSpeed*${golem?'.45F':cat?'.55F':'.8F'};
            legs[i].xRot=net.minecraft.util.Mth.lerp(flight,walk,${cat?'.3F':'-.4F'});
            ${seahorse?'legs[i].xRot=0; legs[i].zRot=(i%2==0?-1:1)*(.2F+(float)Math.sin(state.ageInTicks*.5F)*.3F);':''}
            ${crocodile?'if(state.swimming) legs[i].xRot=(float)Math.sin(state.ageInTicks*.3F+i)*.3F;':''}
            ${squirrel?'legs[i].zRot=(i%2==0?-1:1)*flight*.9F;':''}
        }
        tail.yRot=(float)Math.sin(state.ageInTicks*${cat?'.16F':'.07F'})*${cat?'.28F':'.12F'};
        ${biped?'tail.yRot=0;':''}
        ${crocodile?'if(state.swimming) tail.yRot=(float)Math.sin(state.ageInTicks*.22F)*.35F;':''}
        ${scorpion?'root().getChild("claw_left").yRot=(float)Math.sin(state.ageInTicks*.06F)*.08F; root().getChild("claw_right").yRot=-root().getChild("claw_left").yRot;':''}
        ${cerberus?'leftHead.yRot=head.yRot*.8F+(float)Math.sin(state.ageInTicks*.055F)*.12F; rightHead.yRot=head.yRot*.8F+(float)Math.sin(state.ageInTicks*.055F+2F)*.12F; leftHead.xRot=head.xRot; rightHead.xRot=head.xRot;':''}
        ${dragon||cerberus?'tailTip.yRot=(float)Math.sin(state.ageInTicks*.07F-.7F)*.2F;':''}
        ${winged?(squirrel?'leftWing.zRot=net.minecraft.util.Mth.lerp(flight,.85F,-.08F); rightWing.zRot=-leftWing.zRot; tail.xRot=net.minecraft.util.Mth.lerp(flight,-.4F,-.1F);':
          'leftWing.zRot=net.minecraft.util.Mth.lerp(flight,.65F,(float)Math.sin(state.ageInTicks*'+(dragon?'.22F':'.3F')+')*.45F); rightWing.zRot=-leftWing.zRot; leftWing.yRot=net.minecraft.util.Mth.lerp(flight,-1.1F,-.1F); rightWing.yRot=-leftWing.yRot; leftTip.yRot=net.minecraft.util.Mth.lerp(flight,-1.15F,-.08F); rightTip.yRot=-leftTip.yRot; leftTip.zRot=flight*(float)Math.sin(state.ageInTicks*'+(dragon?'.22F':'.3F')+'-.65F)*.25F; rightTip.zRot=-leftTip.zRot;'):''}
${attackJava}
    }
}
`.replace(/[ \t]+$/gm, ''));
  console.log(`${id}: ${rig.model.elements.length} cubes, Java model, texture and Blockbench source written.`);
}
