// Alien-inspired hive castes. Explicit --write replaces these eight projects only.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {Rig,materialUVs,exportModel}=require('./rig.cjs');
const catalog=require('./catalog.cjs');
const queen=JSON.parse(fs.readFileSync('art/blockbench/shadow_creeper_queen/ShadowCreeperQueen.bbmodel'));
const entries=catalog.filter(e=>e.region==='shadow_creeper_queen'&&e.category!=='boss');
const zero=[0,0,0],round=n=>Number(n.toFixed(6));
const frames=(L,fn)=>Array.from({length:25},(_,i)=>[round(L*i/24),fn(i/24).map(round)]);
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
for(const e of entries){
 const id=e.id,title=id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(''),file=`art/blockbench/${id}/${title}.bbmodel`;
 const old=JSON.parse(fs.readFileSync(file)),r=new Rig(e),w=e.width,h=e.height;
 const larva=id==='shadow_larva',egg=id==='shadow_egg',royal=/praetorian|minion/.test(id),heavy=id==='shadow_guardian',stalker=id==='shadow_stalker',spitter=id==='shadow_spitter';
 const bone=(n,x,y,z,p='body')=>r.bone(n,[x*w,y*h,z*w],p);
 const box=(n,p,x,y,z,dx,dy,dz,m=0)=>r.box(n,p,x*w,y*h,z*w,dx*w,dy*h,dz*w,m);
 if(!larva&&!egg){
  // Reuse the queen's jointed anatomy, with caste-specific crown and armor profiles.
  const scale=[w/12,h/18,w/12],keep=queen.elements.filter(c=>{
   if(!royal&&/small_|crown_(inner|fan|swept)/.test(c.name))return false;
   if(!royal&&!heavy&&/dorsal_(tube|hook)_[12]/.test(c.name))return false;
   return true;
  });
  const removed=new Set(queen.elements.filter(c=>!keep.includes(c)).map(c=>c.uuid));
  r.model.groups=structuredClone(queen.groups);r.model.outliner=structuredClone(queen.outliner);r.model.elements=structuredClone(keep);
  const uuids=new Map();for(const g of r.model.groups)uuids.set(g.uuid,r.uuid('bone/'+g.name));for(const c of r.model.elements)uuids.set(c.uuid,r.uuid('cube/'+c.name));
  for(const g of r.model.groups){g.uuid=uuids.get(g.uuid);g.origin=g.origin.map((v,i)=>v*scale[i]);r.bones.set(g.name,g);}
  const groupById=new Map(r.model.groups.map(g=>[g.uuid,g]));
  function remap(n){n.uuid=uuids.get(n.uuid);r.nodes.set(groupById.get(n.uuid).name,n);n.children=n.children.filter(c=>typeof c!=='string'||!removed.has(c)).map(c=>typeof c==='string'?uuids.get(c):(remap(c),c));}
  r.model.outliner.forEach(remap);
  for(const c of r.model.elements){c.uuid=uuids.get(c.uuid);for(const field of ['from','to','origin'])c[field]=c[field].map((v,i)=>v*scale[i]);
   if(!royal&&c.name==='crown_center'){c.from[0]*=.65;c.to[0]*=.65;c.to[1]=c.from[1]+h*16*.035;}
   if(stalker&&/cranium|dome/.test(c.name)){c.from[2]-=w*16*.16;}
  }
  if(heavy){box('guardian_chest_plate','body',0,.63,-.16,.47,.15,.09,1);for(const s of [-1,1])box('guardian_shoulder_'+s,s>0?'left_arm':'right_arm',s*.265,.685,0,.22,.115,.24,1);}
  if(spitter){for(const s of [-1,1]){box('acid_gland_'+s,'neck',s*.11,.755,-.095,.11,.07,.17,1);box('acid_duct_'+s,'neck',s*.11,.785,-.17,.035,.025,.025,3);}}
  if(id==='shadow_drone')box('darkness_gland','body',0,.56,-.13,.1,.085,.04,1);
  if(royal)for(const s of [-1,1])box('royal_forearm_blade_'+s,s>0?'left_forearm':'right_forearm',s*.35,.46,-.09,.035,.14,.22,1);
 }else{
  r.bone('root');bone('body',0,.35,0,'root');
  if(larva){
   bone('head_look',0,.6,-.25);bone('head',0,.6,-.25,'head_look');
   box('larval_dome','head',0,.35,-.27,.39,.55,.4);
   box('larval_brow','head',0,.75,-.28,.3,.25,.32,1);
   bone('jaw',0,.4,-.35,'head');box('larval_mouth','jaw',0,.28,-.46,.18,.1,.04,2);
   for(let j=0;j<4;j++)box('larval_tooth_'+j,'jaw',(j-1.5)*.037,.29,-.486,.023,.09,.025,1);
   for(let i=0;i<7;i++){bone('tail_'+i,0,.4-i*.025,-.04+i*.15,i?'tail_'+(i-1):'body');box('larval_segment_'+i,'tail_'+i,0,.05,-.015+i*.15,.32-i*.037,.53-i*.05,.2);box('larval_ridge_'+i,'tail_'+i,0,.5-i*.045,i*.15,.2-i*.023,.075,.09,1);}
   for(const s of [-1,1]){bone(s>0?'left_arm':'right_arm',s*.15,.42,-.2);box('vestigial_claw_'+s,s>0?'left_arm':'right_arm',s*.2,.08,-.25,.035,.3,.09,1);}
  }else{
   box('egg_base','body',0,0,0,.65,.12,.65,2);box('egg_belly','body',0,.1,0,.75,.47,.7);box('egg_neck','body',0,.54,0,.58,.23,.55,1);
   box('egg_dark_opening','body',0,.75,0,.42,.03,.4,2);
   for(let i=0;i<4;i++){const x=i<2?(i?1:-1)*.24:0,z=i>=2?(i===2?1:-1)*.24:0;bone('petal_'+i,x,.72,z);box('egg_lip_'+i,'petal_'+i,x,.72,z,i<2?.16:.4,.28,i<2?.4:.16,0);box('egg_seam_'+i,'petal_'+i,x*.95,.75,z*.95,i<2?.018:.25,.2,i<2?.25:.018,1);}
   for(const s of [-1,1])for(const z of [-1,1])box('nest_tendril_'+s+'_'+z,'body',s*.34,0,z*.31,.15,.055,.2,1);
  }
 }
 // Preserve the summoned egg/cage variants and their renderer-owned visibility groups.
 const roleNames=new Set(['egg','cage']);
 if(e.category==='minion'){
  const oldGroups=new Map(old.groups.map(g=>[g.uuid,g])),oldCubes=new Map(old.elements.map(c=>[c.uuid,c]));
  function visit(n){if(typeof n==='string')return;const g=oldGroups.get(n.uuid);if(roleNames.has(g.name)){r.model.groups.push(structuredClone(g));r.bones.set(g.name,r.model.groups.at(-1));const node=structuredClone(n);r.nodes.get('root').children.push(node);r.nodes.set(g.name,node);for(const c of n.children)if(typeof c==='string')r.model.elements.push(structuredClone(oldCubes.get(c)));}else n.children.forEach(visit);}
  old.outliner.forEach(visit);
 }
 r.fitHeight();materialUVs(r);
 const queenMap=new Map(queen.animations.map(a=>[a.name.split('.').pop(),a]));
 for(const previous of old.animations){
  const name=previous.name.split('.').pop(),L=previous.length,tracks=[];
  if(!larva&&!egg){
   const attack=spitter||e.category==='minion'?'acid':royal?'sweep':'shadow';
   const sourceName={idle:'idle',walk:'walk',warning:'windup_'+attack,attack,active:stalker?'charge':attack,hide:'windup_charge',death:'death'}[name];
   const source=queenMap.get(sourceName);
   for(const a of Object.values(source.animators))for(const channel of ['rotation','position','scale']){
    const keys=a.keyframes.filter(k=>k.channel===channel);if(!keys.length)continue;
    tracks.push([a.name,channel,keys.map(k=>[round(k.time/source.length*L),['x','y','z'].map((axis,i)=>{let v=Number(k.data_points[0][axis]);if(channel==='position')v*=i===1?h/18:w/12;if(channel==='rotation'&&stalker&&a.name==='head')v-=6;return round(v);})])]);
   }
  }else{
   const active=['warning','attack','active'].includes(name),dying=name==='death';
   if(larva){
    for(let i=0;i<7;i++)tracks.push(['tail_'+i,'rotation',frames(L,p=>[0,(dying?1-p:1)*(name==='walk'?12:active?16:5)*Math.sin(p*Math.PI*2-i*.55),0])]);
    tracks.push(['head','rotation',frames(L,p=>[active?-12*Math.sin(p*Math.PI):dying?-20*p:2*Math.sin(p*Math.PI*2),0,0])]);
    tracks.push(['jaw','rotation',frames(L,p=>[active?-25*Math.sin(p*Math.PI):0,0,0])]);
    if(dying)tracks.push(['body','scale',frames(L,p=>[1,1-.55*p,1])]);
   }else{
    for(let i=0;i<4;i++)tracks.push(['petal_'+i,'rotation',frames(L,p=>{const angle=dying?70*p:active?55*Math.sin(p*Math.PI/2):3*(1-Math.cos(p*Math.PI*2));return i<2?[0,0,angle*(i? -1:1)]:[angle*(i===2?-1:1),0,0];})]);
    if(dying)tracks.push(['body','scale',frames(L,p=>[1,1-.7*p,1])]);
   }
  }
  // Keep role-specific hatch/cage tracks from the already-tested minion controller contract.
  if(e.category==='minion')for(const a of Object.values(previous.animators))if(roleNames.has(a.name))for(const channel of ['rotation','position','scale']){
   const keys=a.keyframes.filter(k=>k.channel===channel);if(keys.length)tracks.push([a.name,channel,keys.map(k=>[k.time,['x','y','z'].map(axis=>Number(k.data_points[0][axis]))])]);
  }
  r.clip(name,L,previous.loop,tracks);r.model.animations.at(-1).uuid=previous.uuid;
 }
 const death=r.model.animations.find(a=>a.name.endsWith('.death')),body=r.bones.get('body');
 const positions=death.animators[body.uuid].keyframes.filter(k=>k.channel==='position');
 if(positions.length===1){const last=structuredClone(positions[0]);last.time=death.length;last.uuid=r.uuid('death/floor/end');death.animators[body.uuid].keyframes.push(last);}
 settleDeath(r.model);
 r.model.textures=old.textures;r.model.editor_state=old.editor_state;r.model.visible_box=[Math.max(w,h)*3,Math.max(w,h)*3,h/2];
 assert.equal(r.model.animations.length,7);
 if(!larva&&!egg)assert.equal(r.model.groups.filter(g=>/^leg_/.test(g.name)).length,2);
 if(process.argv.includes('--write')){fs.writeFileSync(file,JSON.stringify(r.model,null,2)+'\n');exportModel(r.model);}
 console.log(`${id}: ${r.model.elements.length} cubes, ${r.model.groups.length} bones, 7 clips`);
}
