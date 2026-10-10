// Bespoke bipedal royal predator. Run explicitly with --write to replace this boss only.
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {Rig,materialUVs,exportModel}=require('./rig.cjs');
const entry=require('./catalog.cjs').find(e=>e.id==='shadow_creeper_queen');
const file='art/blockbench/shadow_creeper_queen/ShadowCreeperQueen.bbmodel';
const previous=JSON.parse(fs.readFileSync(file,'utf8'));
const r=new Rig(entry),w=entry.width,h=entry.height;
const bone=(n,x,y,z,p='body')=>r.bone(n,[x*w,y*h,z*w],p);
const box=(n,p,x,y,z,dx,dy,dz,m=0)=>r.box(n,p,x*w,y*h,z*w,dx*w,dy*h,dz*w,m);
r.bone('root');bone('body',0,.45,.06,'root');
box('pelvic_armor','body',0,.4,.08,.29,.14,.27);
box('narrow_waist','body',0,.52,.055,.19,.13,.22,2);
box('armored_thorax','body',0,.63,.035,.4,.15,.29);
box('sternum_keel','body',0,.59,-.125,.09,.2,.045,1);
for(let i=0;i<6;i++)for(const s of [-1,1]){
  box('rib_'+s+'_'+i,'body',s*(.115+i*.009),.55+i*.033,-.07,.065,.018,.22,1);
  box('vertebra_'+s+'_'+i,'body',s*.045,.48+i*.048,.205,.065,.025,.08,1);
}
bone('neck',0,.76,-.035);box('neck_column','neck',0,.75,-.04,.15,.085,.18);
bone('head_look',0,.815,-.08,'neck');bone('head',0,.825,-.12,'head_look');
// The broad swept crown is the defining silhouette, with a projecting eyeless skull.
box('elongated_cranium','head',0,.83,-.18,.32,.105,.64);
box('forward_dome','head',0,.825,-.43,.27,.1,.2);
box('crown_center','head',0,.925,.08,.19,.075,.68,1);
for(const s of [-1,1]){
  box('crown_inner_'+s,'head',s*.145,.915,.10,.16,.07,.65,1);
  box('crown_fan_'+s,'head',s*.275,.89,.13,.15,.07,.55);
  box('crown_swept_tip_'+s,'head',s*.36,.875,.24,.085,.065,.36,1);
  box('cheek_blade_'+s,'head',s*.175,.79,-.31,.065,.08,.3,1);
  box('brow_armor_'+s,'head',s*.09,.85,-.523,.14,.035,.03,2);
  // Small sculk sense slits, rather than the former six spider eyes.
  box('sense_slit_'+s,'head',s*.112,.838,-.537,.047,.008,.008,3);
}
box('mouth_cavity','head',0,.8,-.465,.22,.045,.16,2);
bone('jaw',0,.803,-.3,'head');box('lower_jaw','jaw',0,.77,-.415,.205,.035,.25,1);
bone('inner_jaw',0,.805,-.43,'head');box('inner_jaw_piston','inner_jaw',0,.802,-.48,.065,.025,.12,1);
for(let i=0;i<7;i++){
  box('upper_tooth_'+i,'head',(i-3)*.027,.791,-.535,.014,.022,.022,1);
  box('lower_tooth_'+i,'jaw',(i-3)*.027,.8,-.527,.012,.018,.018,1);
}
for(const s of [-1,1]){
  const side=s===1?'left':'right',i=s===1?0:1;
  bone('leg_'+i,s*.15,.46,.07);
  box('haunch_'+i,'leg_'+i,s*.18,.29,.015,.17,.18,.25);
  box('thigh_ridge_'+i,'leg_'+i,s*.235,.3,.01,.045,.17,.18,1);
  bone('shin_'+i,s*.18,.29,-.015,'leg_'+i);
  box('sloping_shin_'+i,'shin_'+i,s*.18,.13,.075,.09,.17,.14);
  box('hock_spur_'+i,'shin_'+i,s*.18,.17,.2,.055,.05,.17,1);
  bone('foot_'+i,s*.18,.12,.105,'shin_'+i);
  box('ankle_'+i,'foot_'+i,s*.18,.045,.08,.07,.095,.085);
  box('foot_pad_'+i,'foot_'+i,s*.18,.015,-.035,.14,.045,.28);
  for(let j=0;j<3;j++)box('toe_claw_'+i+'_'+j,'foot_'+i,s*.18+(j-1)*.045,0,-.19,.03,.035,.15,1);
  bone(side+'_arm',s*.245,.745,-.005);
  box(side+'_shoulder',side+'_arm',s*.26,.69,0,.145,.1,.19);
  box(side+'_upper_arm',side+'_arm',s*.305,.55,-.005,.085,.18,.115);
  bone(side+'_forearm',s*.305,.565,-.02,side+'_arm');
  box(side+'_forearm_shell',side+'_forearm',s*.315,.445,-.1,.075,.13,.18,1);
  bone(side+'_hand',s*.315,.455,-.17,side+'_forearm');
  box(side+'_palm',side+'_hand',s*.315,.41,-.18,.09,.07,.12);
  for(let j=0;j<3;j++){
    box(side+'_finger_'+j,side+'_hand',s*.315+(j-1)*.035,.335,-.22,.022,.085,.035);
    box(side+'_hook_'+j,side+'_hand',s*.315+(j-1)*.035,.33,-.25,.018,.025,.065,1);
  }
  bone(side+'_small_arm',s*.12,.605,-.13);
  box(side+'_small_upper',side+'_small_arm',s*.17,.545,-.17,.045,.075,.08);
  box(side+'_small_forearm',side+'_small_arm',s*.17,.525,-.255,.035,.04,.14,1);
  for(let j=0;j<2;j++)box(side+'_small_claw_'+j,side+'_small_arm',s*.17+(j-.5)*.035,.5,-.32,.02,.045,.035,1);
  for(let i=0;i<3;i++){
    box(side+'_dorsal_tube_'+i,'body',s*(.12+i*.065),.71-i*.035,.24+i*.035,.055,.17,.065);
    box(side+'_dorsal_hook_'+i,'body',s*(.12+i*.065),.85-i*.035,.29+i*.035,.06,.035,.16,1);
  }
}
for(let i=0;i<8;i++){
  const y=.43-i*.035,z=.2+i*.155,size=.14-i*.012;
  bone('tail_'+i,0,y,z,i?'tail_'+(i-1):'body');
  box('tail_segment_'+i,'tail_'+i,0,y-.035,z+.08,size,.075,.2);
  box('tail_spine_'+i,'tail_'+i,0,y+.04,z+.1,.035,.045,.08,1);
}
box('tail_lance','tail_7',0,.16,1.49,.035,.095,.32,1);
box('tail_blade_wide','tail_7',0,.14,1.39,.035,.16,.14);
r.fitHeight();materialUVs(r);
const zero=[0,0,0],tau=Math.PI*2,round=n=>Number(n.toFixed(6));
const track=(b,ch,L,fn)=>[b,ch,Array.from({length:33},(_,i)=>[round(L*i/32),fn(i/32).map(round)])];
const rot=(b,L,fn)=>track(b,'rotation',L,fn);
function pose(attack){
  const p={head:[-5,0,0],jaw:[-15,0,0],left_arm:[25,-8,8],right_arm:[25,8,-8],left_forearm:[18,0,0],right_forearm:[18,0,0],left_small_arm:[12,-12,0],right_small_arm:[12,12,0]};
  if(attack==='acid'){p.head=[-14,0,0];p.jaw=[-32,0,0];}
  if(attack==='sweep'){p.body=[0,-18,0];for(let i=0;i<8;i++)p['tail_'+i]=[0,-8,0];p.left_arm=[55,-25,15];}
  if(attack==='brood'){p.head=[15,0,0];p.left_arm=[55,-20,30];p.right_arm=[55,20,-30];p.jaw=[-28,0,0];}
  if(attack==='shadow'||attack==='shadow_storm'){p.head=[12,0,0];p.left_arm=[60,-15,35];p.right_arm=[60,15,-35];p.jaw=[-35,0,0];}
  if(attack==='charge'){p.body=[-8,0,0];p.head=[8,0,0];p.left_arm=[-15,0,10];p.right_arm=[-15,0,-10];}
  return p;
}
for(const old of previous.animations){
  const name=old.name.split('.').pop(),L=old.length,warning=name.startsWith('windup_'),move=['walk','charge'].includes(name),loop=old.loop==='loop';
  let tracks=[];
  if(loop){
    const speed=name==='wrath_idle'?1.6:1;
    tracks.push(rot('head',L,p=>[-5+1.5*Math.sin(tau*p),0,0]),rot('jaw',L,p=>[-8-2*Math.sin(tau*p),0,0]));
    for(const side of ['left','right']){
      const s=side==='left'?1:-1;
      tracks.push(rot(side+'_arm',L,p=>[18+(move?14:2)*Math.sin(tau*p)*s,0,s*8]),rot(side+'_forearm',L,p=>[20+3*Math.sin(tau*p-.4),0,0]),rot(side+'_small_arm',L,p=>[12+3*Math.sin(tau*p+.8),s*-10,0]));
    }
    for(let i=0;i<2;i++){
      const s=i? -1:1;
      tracks.push(rot('leg_'+i,L,p=>[move?s*22*Math.sin(tau*p):0,0,0]),rot('shin_'+i,L,p=>[move?-16*Math.max(0,s*Math.sin(tau*p)):0,0,0]),rot('foot_'+i,L,p=>[move?-s*22*Math.sin(tau*p)+16*Math.max(0,s*Math.sin(tau*p)):0,0,0]));
    }
    for(let i=0;i<8;i++)tracks.push(rot('tail_'+i,L,p=>[0,speed*2.5*Math.sin(tau*p-i*.45),0]));
  }else if(name==='death'){
    tracks.push(track('body','position',L,p=>[0,-h*16*.28*Math.min(1,p/.8),0]),rot('body',L,p=>[-18*Math.min(1,p/.8),0,0]),rot('head',L,p=>[-20*p,0,0]));
    for(let i=0;i<2;i++)tracks.push(rot('leg_'+i,L,p=>[55*p,0,0]),rot('shin_'+i,L,p=>[-95*p,0,0]),rot('foot_'+i,L,p=>[40*p,0,0]));
    for(const side of ['left','right'])tracks.push(rot(side+'_arm',L,p=>[30*p,0,0]));
  }else{
    const attack=warning?name.slice(7):name==='phase_change'?'shadow_storm':name,ready=pose(attack);
    for(const [b,v]of Object.entries(ready)){
      tracks.push(rot(b,L,p=>{
        let strength;
        if(warning){const x=Math.min(1,p/.38);strength=x*x*(3-2*x);}
        else if(name==='phase_change')strength=Math.sin(Math.PI*p)**2;
        else strength=p<.15?1-1.3*(p/.15):-.3*(1-(p-.15)/.85);
        if(!warning&&attack==='sweep'&&b.startsWith('tail'))strength=p<.3?1-3*p/.3:-2*(1-(p-.3)/.7);
        return v.map(n=>n*strength);
      }));
    }
    if(attack==='acid'&&!warning)tracks.push(track('inner_jaw','position',L,p=>[0,0,-w*16*.08*Math.sin(Math.PI*p)**2]));
    for(let i=0;i<8;i++)if(!ready['tail_'+i])tracks.push(rot('tail_'+i,L,p=>[0,3*Math.sin(tau*p-i*.4)*Math.sin(Math.PI*p)**2,0]));
  }
  r.clip(name,L,old.loop,tracks);r.model.animations.at(-1).uuid=old.uuid;
  // Exclude this bespoke rig from the generic polish pass after regeneration.
  r.model.animations.at(-1).darkspawn_polish_version=1;
}
function settleDeath(model) {
  const clip=model.animations.find(a=>a.name.endsWith('.death'));
  if(!clip)return;
  const groups=new Map(model.groups.map(g=>[g.uuid,g])),parents=new Map(),cubeParents=new Map();
  function visit(n,parent){if(typeof n==='string'){cubeParents.set(n,parent);return;}parents.set(n.uuid,parent);n.children.forEach(c=>visit(c,n.uuid));}
  model.outliner.forEach(n=>visit(n));
  const values=(uuid,channel)=>{const k=clip.animators[uuid]?.keyframes.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time).at(-1);return k?['x','y','z'].map(a=>Number(k.data_points[0][a])):channel==='scale'?[1,1,1]:zero;};
  function rotate(v,r){let [x,y,z]=v;for(let axis=0;axis<3;axis++){const a=r[axis]*Math.PI/180,c=Math.cos(a),s=Math.sin(a);if(axis===0)[y,z]=[y*c-z*s,y*s+z*c];if(axis===1)[x,z]=[x*c+z*s,-x*s+z*c];if(axis===2)[x,y]=[x*c-y*s,x*s+y*c];}return[x,y,z];}
  function transform(v,uuid){if(!uuid)return v;const g=groups.get(uuid),s=values(uuid,'scale'),r=values(uuid,'rotation').map((n,i)=>n+(g.rotation?.[i]||0)),p=values(uuid,'position');return transform(rotate(v.map((n,i)=>(n-g.origin[i])*s[i]),r).map((n,i)=>n+g.origin[i]+p[i]),parents.get(uuid));}
  function underBody(uuid){if(!uuid)return false;return groups.get(uuid).name==='body'||underBody(parents.get(uuid));}
  let minY=Infinity;
  for(const cube of model.elements){const parent=cubeParents.get(cube.uuid);if(!underBody(parent))continue;for(const x of [cube.from[0],cube.to[0]])for(const y of [cube.from[1],cube.to[1]])for(const z of [cube.from[2],cube.to[2]]){
    let p=[x,y,z];if(cube.rotation?.some(n=>n)){const o=cube.origin||zero;p=rotate(p.map((n,i)=>n-o[i]),cube.rotation).map((n,i)=>n+o[i]);}minY=Math.min(minY,transform(p,parent)[1]);
  }}
  assert(Number.isFinite(minY));
  const body=model.groups.find(g=>g.name==='body');
  for(const k of clip.animators[body.uuid].keyframes)if(k.channel==='position')k.data_points[0].y=String(round(Number(k.data_points[0].y)-minY*(k.time/clip.length)));
}
settleDeath(r.model);
r.model.textures=previous.textures;r.model.visible_box=[48,48,9];r.model.editor_state=previous.editor_state;
assert.equal(r.model.animations.length,previous.animations.length);
assert.equal(r.model.groups.filter(g=>/^leg_/.test(g.name)).length,2);
if(process.argv.includes('--write')){fs.writeFileSync(file,JSON.stringify(r.model,null,2)+'\n');exportModel(r.model);}
console.log(`${process.argv.includes('--write')?'Saved':'Dry run:'} queen: ${r.model.elements.length} cubes, ${r.model.groups.length} bones, ${r.model.animations.length} clips; two hind legs, four arms, eight tail joints.`);
