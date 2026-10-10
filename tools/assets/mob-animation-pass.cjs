// Animation-only second pass. Dry-run by default; --write explicitly replaces clips.
// Optional creature IDs limit a batch. Existing forest work and all bosses are excluded.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {Rig} = require('./rig.cjs');
const catalog = require('./catalog.cjs');
const forest = new Set(['barkling','hollowed','rootcrawler','ancient_ent']);
const requested = process.argv.slice(2).filter(a => a !== '--write');
const roster = catalog.filter(e => e.category !== 'boss' && e.region !== 'shadow_creeper_queen' && !forest.has(e.id) && (!requested.length || requested.includes(e.id)));
for (const id of requested) assert(roster.some(e => e.id === id), 'Unsupported creature: ' + id);
const zero = [0,0,0], unit = [1,1,1];
const round = n => Math.abs(n) < 1e-9 ? 0 : Number(n.toFixed(6));
const frames = (length, fn, count = 16) => Array.from({length:count+1}, (_,i) => [round(length*i/count), fn(i/count).map(round)]);
const rot = (bone,length,fn) => [bone,'rotation',frames(length,fn)];
const pos = (bone,length,fn) => [bone,'position',frames(length,fn)];
const sine = (p,phase=0) => Math.sin(p*Math.PI*2+phase);
const staged = (bone,length,values,channel='rotation') => [bone,channel,values.map(([p,v]) => [round(p*length),v.map(round)])];
const write = process.argv.includes('--write');
const jobs = [];
let changed = 0;
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
  for(const k of clip.animators[body.uuid].keyframes)if(k.channel==='position'&&k.time>=.7)k.data_points[0].y=String(round(Number(k.data_points[0].y)-minY));
}
for (const entry of roster) {
  const {id,form,height} = entry;
  const file = `art/blockbench/${id}/${id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join('')}.bbmodel`;
  const model = JSON.parse(fs.readFileSync(file,'utf8'));
  const original = structuredClone(model);
  const rig = new Rig(entry);
  rig.model = model;
  rig.bones = new Map(model.groups.map(g=>[g.name,g]));
  const has = name => rig.bones.has(name);
  const names = [...rig.bones.keys()];
  const legs = names.filter(n=>/^leg_\d+$/.test(n)).sort((a,b)=>Number(a.slice(4))-Number(b.slice(4)));
  const tails = names.filter(n=>/^tail_\d+$/.test(n));
  const coils = names.filter(n=>/^segment_\d+$/.test(n));
  const tentacles = names.filter(n=>/^tentacle_\d+$/.test(n));
  const arthropod = ['ARTHROPOD','QUEEN'].includes(form);
  const quadruped = ['QUADRUPED','WOLF','CREEPER'].includes(form);
  const bird = form === 'BIRD', ray = form === 'RAY';
  const serpent = form === 'SERPENT', orb = ['ORB','SKULL','EYE'].includes(form);
  const still = entry.movement === 'STILL' || form === 'TENTACLE' || id === 'heartwood_sapling';
  const behavior = entry.behavior || ({baba_yaga_minion:'POTION',mountain_titan_minion:'BOULDER',mutant_zombie_minion:'SMASH',
    soulbound_colossus_minion:'SOUL_BOLT',void_eye_minion:'SENTINEL',mycelial_sovereign_minion:'SPORE',ice_wyrm_minion:'ICE_BREATH',
    thunder_bird_minion:'LIGHTNING',netherborn_minion:'CHARGE',shadow_creeper_queen_minion:'ACID'}[id]) || 'MELEE';
  const heavy = /brute|titan|colossus|guardian|praetorian|mooshroom|hoglin|crimson_spawn/.test(id);
  const nimble = /finch|imp|bogling|skitterer|larva|familiar|viper|leaping/.test(id);
  const croucher = /stalker|crawler|widow|ravaged/.test(id);
  const pace = id === 'frostfang' ? .76 : id === 'ravaged_wolf' ? .62 : id === 'alpha_dire_wolf' ? .9 : heavy ? 1.2 : nimble ? .64 : .86;
  const stride = heavy ? 16 : nimble ? 27 : 22;
  const idleLength = heavy ? 3.6 : nimble ? 2.4 : 3;
  function clip(name,length,loop,tracks) {
    const index = model.animations.findIndex(a=>a.name===`animation.${id}.${name}`);
    assert(index>=0, 'Unknown clip: '+id+'/'+name);
    // Reject duplicate tracks: silently replacing a yaw track with a roll loses motion.
    const seen = new Set();
    tracks = tracks.filter(t=>has(t[0]));
    for(const [bone,ch] of tracks) { assert(!seen.has(bone+'/'+ch),'Duplicate '+id+'/'+name+'/'+bone+'/'+ch); seen.add(bone+'/'+ch); }
    const previous = model.animations[index];
    rig.clip(name,length,loop,tracks);
    const replacement = model.animations.pop();
    replacement.uuid = previous.uuid;
    model.animations[index] = replacement;
    changed++;
  }
  function appendageMotion(length,amount=1) {
    return [
      ...tails.map((bone,i)=>rot(bone,length,p=>[0,sine(p,-i*.55)*3*amount,0])),
      ...coils.map((bone,i)=>rot(bone,length,p=>[0,sine(p,-i*.65)*5*amount,0])),
      ...tentacles.map((bone,i)=>rot(bone,length,p=>[sine(p,-i*.4)*2*amount,0,sine(p,-i*.4+.5)*amount])),
      ...[-1,1].filter(s=>has('branch_'+s)).map(s=>rot('branch_'+s,length,p=>[0,0,s*sine(p,.4)*3*amount])),
      ...(has('lure')?[rot('lure',length,p=>[sine(p)*4*amount,0,0])]:[])
    ];
  }
  // Side fins use low-amplitude waves; wing tips trail the main flight stroke.
  function wings(length,amount,flight=true) {
    return ['left','right'].flatMap((side,i)=>{
      const sign=i===0?1:-1;
      return [rot(side+'_wing',length,p=>[0,0,sign*(flight ? 8+amount*sine(p) : amount*sine(p))]),
        rot(side+'_wing_tip',length,p=>[0,0,sign*amount*.55*sine(p,-.55)])];
    });
  }
  function locomotion(length,fast=false) {
    const tracks = appendageMotion(length,fast?1.8:1);
    if(still) return [...tracks,rot('head',length,p=>[sine(p)*2,0,0])];
    if(orb) return [...tracks,pos('body',length,p=>[0,height*.55*(1+sine(p)),0]),rot('head',length,p=>[sine(p)*2,sine(p,.8)*4,0])];
    if(bird||ray) return [...tracks,...wings(length,ray?14:fast?48:34,!ray),
      rot('body',length,p=>[ray?-5:-9,0,sine(p)*2]),rot('head',length,p=>[5,0,0]),
      ...legs.map(n=>rot(n,length,()=>[-24,0,0]))];
    if(serpent) return [...tracks,rot('neck',length,p=>[sine(p)*3,sine(p,-.4)*6,0]),rot('head',length,p=>[0,-sine(p,-.4)*4,0])];
    if(form==='FISH'||id==='siren') return [...tracks,...wings(length,8,false),rot('body',length,p=>[id==='siren'?-12:0,sine(p)*4,0]),
      rot('head',length,p=>[0,-sine(p)*3,0]),...['left','right'].map((s,i)=>rot(s+'_arm',length,p=>[15+sine(p,i*Math.PI)*8,0,i?12:-12]))];
    if(arthropod) {
      for(const [i,bone] of legs.entries()) {
        const side=i%2?1:-1,phase=((Math.floor(i/2)+i%2)%2)*Math.PI;
        tracks.push(rot(bone,length,p=>[0,sine(p,phase)*(fast?23:15),side*Math.cos(p*Math.PI*2+phase)*7]),
          rot('shin_'+i,length,p=>[0,0,-side*Math.cos(p*Math.PI*2+phase)*10]));
      }
      return [...tracks,rot('body',length,p=>[0,0,sine(p)*1.5]),rot('head',length,p=>[0,-sine(p)*2,0])];
    }
    for(const [i,bone] of legs.entries()) {
      const phase=quadruped?(i===0||i===3?0:Math.PI):i*Math.PI;
      const amount=(fast?stride*1.25:stride)*(id==='ravaged_wolf'&&i===0?1.2:1);
      tracks.push(rot(bone,length,p=>[sine(p,phase)*amount,0,0]));
      // Rotation raises the toe during swing. Add a small lift without moving the entity root.
      tracks.push(pos(bone,length,p=>[0,Math.max(0,Math.cos(p*Math.PI*2+phase))*height*.45,0]));
    }
    tracks.push(rot('body',length,p=>[croucher?-3:0,0,sine(p)*(heavy?1:2)]),rot('head',length,p=>[croucher?-5:0,0,-sine(p)]));
    if(!quadruped) tracks.push(...['left','right'].map((s,i)=>rot(s+'_arm',length,p=>[sine(p,i*Math.PI)*stride*.55+(form==='HUMANOID'?12:0),0,i?3:-3])));
    return tracks;
  }
  function idle() {
    const tracks=appendageMotion(idleLength,.7);
    if(still) return [...tracks,rot('head',idleLength,p=>[sine(p)*1.5,0,0])];
    if(bird||ray) return [...tracks,...wings(idleLength,bird?16:8,!ray),rot('head',idleLength,p=>[sine(p)*2,0,0])];
    if(orb) return [...tracks,pos('body',idleLength,p=>[0,height*.5*(1+sine(p)),0]),rot('head',idleLength,p=>[sine(p)*2,sine(p)*3,0])];
    if(serpent) return [...tracks,rot('neck',idleLength,p=>[sine(p)*2,sine(p)*3,0]),rot('head',idleLength,p=>[0,-sine(p)*2,0])];
    return [...tracks,rot('body',idleLength,p=>[sine(p)*.6,0,0]),rot('head',idleLength,p=>[(croucher?-4:0)+sine(p)*1.5,0,id==='ravaged_wolf'?4:0]),
      ...(form==='FISH'?wings(idleLength,5,false):[]),rot('jaw',idleLength,p=>[-1-Math.max(0,sine(p))*(quadruped?3:1),0,0])];
  }
  // All poses use Blockbench source coordinates (forward = -Z). Positive X
  // raises a downward arm forward, while negative X lowers a forward muzzle.
  function poses() {
    const wind={},hit={};
    function put(b,w,h) { if(has(b)){wind[b]=w;hit[b]=h;} }
    put('head',[5,0,0],[-9,0,0]);
    put('jaw',[-22,0,0],[0,0,0]);
    if(quadruped||form==='BIPED') {
      put('body',[3,0,0],[-5,0,0]);
      for(const [i,n] of legs.entries())put(n,[i<2?12:-12,0,0],[i<2?-12:8,0,0]);
    } else if(arthropod) {
      put('left_arm',[8,-18,0],[18,12,0]);put('right_arm',[8,18,0],[18,-12,0]);
      for(const [i,n] of legs.entries())put(n,[0,0,(i%2?1:-1)*8],[0,0,-(i%2?1:-1)*4]);
    } else if(serpent) {
      put('neck',[12,0,0],[-16,0,0]);
      coils.forEach((n,i)=>put(n,[0,Math.sin(i*.7)*8,0],[0,-Math.sin(i*.7)*5,0]));
    } else if(bird||ray) {
      put('left_wing',[0,0,40],[0,0,-25]);put('right_wing',[0,0,-40],[0,0,25]);
      put('left_wing_tip',[0,0,20],[0,0,-12]);put('right_wing_tip',[0,0,-20],[0,0,12]);
    } else if(!orb) {
      put('right_arm',[95,-12,-8],[65,18,-4]);put('left_arm',[28,0,8],[35,0,4]);put('body',[4,-10,0],[-8,8,0]);
    }
    if(['SMASH','MAGMA','MYCELIUM','SHOCKWAVE'].includes(behavior)) {
      put('left_arm',[120,0,-12],[25,0,-8]);put('right_arm',[120,0,12],[25,0,8]);put('body',[6,0,0],[-15,0,0]);
      put('head',[12,0,0],[-8,0,0]);
    }
    if(['BOULDER','POTION'].includes(behavior)) {
      const side=behavior==='POTION'?'left':'right';
      put(side+'_arm',[140,-12,side==='left'?-8:8],[85,12,0]);put(side+'_hand',[20,0,0],[-12,0,0]);
      put('body',[5,-12,0],[-8,12,0]);
    }
    if(['CONSTRICT','SEA_GRAB','VINES'].includes(behavior)) {
      coils.forEach((n,i)=>put(n,[0,Math.sin(i*.7)*14,0],[0,Math.sin(i*.7)*20,0]));
      put('left_arm',[50,-25,-18],[70,15,-8]);put('right_arm',[50,25,18],[70,-15,8]);
    }
    if(['SIREN','SOUL_KEEPER','BUFF','BROOD','SPORE','LIGHTNING'].includes(behavior)) {
      put('left_arm',[60,0,-35],[85,-15,-15]);put('right_arm',[60,0,35],[85,15,15]);
      put('head',[18,0,0],[8,0,0]);put('jaw',[-18,0,0],[-8,0,0]);
    }
    if(['WEB','ACID','FROST','ICE_BREATH','SOUL_BOLT','FIRE'].includes(behavior)) {
      put('head',[10,0,0],[-8,0,0]);put('jaw',[-18,0,0],[-28,0,0]);
      put('body',[3,0,0],[-5,0,0]);
    }
    if(['POUNCE','DIVE','CHARGE','WIDOW'].includes(behavior)||id==='alpha_dire_wolf') {
      put('body',[-5,0,0],[-8,0,0]);put('head',[-12,0,0],[-5,0,0]);
      for(const [i,n]of legs.entries())put(n,[i<2?22:-25,0,0],[i<2?32:-30,0,0]);
    }
    if(behavior==='SHADOW_CLEAVE') {
      put('body',[0,-18,0],[0,18,0]);tails.forEach(n=>put(n,[0,-12,0],[0,15,0]));
    }
    if(form==='TENTACLE') tentacles.forEach((n,i)=>put(n,[i<3?4:7,0,0],[-(i<3?4:7),0,0]));
    if(id==='fossil_scorpion') tails.forEach((n,i)=>put(n,[i<2?5:10,0,0],[-(i<2?8:15),0,0]));
    // Shield transforms are controlled by the renderer, never by attack poses.
    if(orb){put('body',[0,0,-5],[0,0,5]);put('head',[0,0,3],[0,0,-3]);}
    return {wind,hit};
  }
  function release(length,wind,hit) {
    // Damage/projectiles are emitted at trigger time. Reach contact within one tick.
    return Object.entries(hit).map(([bone,v])=>staged(bone,length,[[0,wind[bone]],[.05/length,v],[.28,v.map(n=>n*.8)],[.65,v.map(n=>n*.25)],[1,zero]]));
  }
  if(id==='endborn') {
    // Retain the bespoke rig and its four-controller contract.
    clip('idle',4,'loop',[rot('body',4,p=>[sine(p)*.7,0,0]),rot('left_arm',4,p=>[2+sine(p)*2,0,-3]),
      rot('right_arm',4,p=>[2+sine(p,Math.PI)*2,0,3]),rot('left_forearm',4,p=>[4+sine(p)*2,0,0]),rot('right_forearm',4,p=>[4+sine(p,Math.PI)*2,0,0])]);
    clip('walk',1.4,'loop',['left','right'].flatMap((s,i)=>[
      rot(s+'_leg',1.4,p=>[sine(p,i*Math.PI)*18,0,0]),rot(s+'_shin',1.4,p=>[-Math.max(0,sine(p,i*Math.PI))*22,0,0]),
      rot(s+'_foot',1.4,p=>[Math.max(0,sine(p,i*Math.PI))*10,0,0]),rot(s+'_arm',1.4,p=>[-sine(p,i*Math.PI)*12,0,i?3:-3]),
      rot(s+'_forearm',1.4,p=>[5+Math.max(0,sine(p,i*Math.PI))*9,0,0])]));
    clip('attack',.3,'once',[staged('right_arm',.3,[[0,[90,-18,5]],[.16,[65,18,0]],[.55,[30,10,0]],[1,zero]]),
      staged('right_forearm',.3,[[0,[20,0,0]],[.16,[5,0,0]],[.6,[15,0,0]],[1,zero]]),staged('body',.3,[[0,[0,-8,0]],[.16,[-6,8,0]],[1,zero]])]);
    clip('blink',.4,'once',[staged('body',.4,[[0,[0,0,-8]],[.25,[0,0,5]],[.65,[0,0,-2]],[1,zero]]),
      staged('body',.4,[[0,[.82,1.12,.82]],[.25,[1.06,.96,1.06]],[.65,[.98,1.02,.98]],[1,unit]],'scale'),
      ...['left','right'].map((s,i)=>staged(s+'_arm',.4,[[0,[25,0,i?20:-20]],[.4,[8,0,i?-8:8]],[1,zero]]))]);
  } else {
    const idleTracks=idle();
    // Egg/cage are siblings of body, selected by minion role in the renderer.
    if(has('egg')) idleTracks.push(rot('egg',idleLength,p=>[0,0,sine(p)*2]));
    if(has('cage')) idleTracks.push(pos('cage',idleLength,p=>[0,height*.12*(1+sine(p)),0]));
    clip('idle',idleLength,'loop',idleTracks);
    clip('walk',pace,'loop',locomotion(pace));
    const {wind,hit}=poses();
    const warningLength = entry.category==='minion'?2:entry.herald?1.25:1;
    // Hold-ready tail avoids resetting the warning if server and render ticks differ.
    clip('warning',warningLength+.25,'loop',Object.entries(wind).map(([bone,v])=>staged(bone,warningLength+.25,[[0,zero],[.3,v.map(n=>n*.85)],[.55,v],[1,v]])));
    clip('attack',.75,'once',release(.75,wind,hit));
    let activeTracks,activeLength=.75;
    const travel=['POUNCE','DIVE','CHARGE','WIDOW'].includes(behavior)||id==='alpha_dire_wolf';
    if(travel && behavior==='CHARGE') {activeLength=.5;activeTracks=locomotion(activeLength,true);}
    else if(travel) {
      // The entity supplies the arc; a tucked limb pose must not add root translation.
      activeLength=1.5;activeTracks=Object.entries(hit).map(([bone,v])=>staged(bone,activeLength,[[0,v],[1,v]]));
    } else if(id==='ravaged_wolf') {activeLength=.48;activeTracks=locomotion(activeLength,true);}
    else if(behavior==='GRAZE') {activeLength=1.5;activeTracks=[rot('head',1.5,p=>[-24+3*sine(p*2),0,0]),rot('jaw',1.5,p=>[-3-3*sine(p*2),0,0])];}
    else if(['BLINK','BLINK_SPORE','STARE','RIFT','DODGE','SOUL_TOUCH'].includes(behavior)) {
      activeLength=.6;activeTracks=[rot('body',.6,p=>[0,0,4*sine(p)]),pos('body',.6,p=>[0,height*.25*(1-Math.cos(p*Math.PI*2)),0]),
        ['body','scale',frames(.6,p=>[1-.06*(1-Math.cos(p*Math.PI*2)),1+.05*(1-Math.cos(p*Math.PI*2)),1-.06*(1-Math.cos(p*Math.PI*2))])]];
    } else if(['STATIC','THAW','REBUILD'].includes(behavior)) {
      activeLength=1.5;activeTracks=[rot('body',1.5,p=>[sine(p*2)*2,0,sine(p*2)*2]),rot('head',1.5,p=>[sine(p*2)*3,0,0]),...appendageMotion(1.5)];
    } else if(behavior==='SEA_GRAB') {
      activeLength=3;activeTracks=Object.entries(hit).map(([bone,v])=>staged(bone,3,[[0,v],[1,v]]));
    } else activeTracks=release(.75,wind,hit);
    clip('active',activeLength,'loop',activeTracks);
    const hideTracks = [staged('body',.5,[[0,zero],[1,[0,-height*.8,0]]],'position'),
      staged('head',.5,[[0,zero],[1,[-12,0,0]]])];
    if(arthropod) for(const [i,n]of legs.entries())hideTracks.push(staged(n,.5,[[0,zero],[1,[0,0,(i%2?1:-1)*12]]]));
    else if(quadruped)for(const [i,n]of legs.entries())hideTracks.push(staged(n,.5,[[0,zero],[1,[i<2?18:-18,0,0]]]));
    else if(serpent)hideTracks.push(staged('neck',.5,[[0,zero],[1,[-20,0,0]]]));
    else if(has('left_arm'))hideTracks.push(staged('left_arm',.5,[[0,zero],[1,[10,0,12]]]),staged('right_arm',.5,[[0,zero],[1,[10,0,-12]]]));
    clip('hide',.5,'hold',hideTracks);
    const fallAngle=arthropod?10:serpent||orb?35:form==='TENTACLE'?12:80;
    const death=[staged('body',1,[[0,zero],[.25,[-4,0,5]],[.7,[-12,0,fallAngle]],[1,[-12,0,fallAngle]]]),
      staged('body',1,[[0,zero],[.7,[0,-height*(orb?.8:1.6),0]],[1,[0,-height*(orb?.8:1.6),0]]],'position'),
      staged('head',1,[[0,zero],[.35,[-10,0,0]],[.7,[-18,0,0]],[1,[-18,0,0]]])];
    for(const [i,n]of legs.entries())death.push(staged(n,1,[[0,zero],[.25,[10,0,0]],[.7,[arthropod?0:20,0,arthropod?(i%2?1:-1)*25:0]],[1,[arthropod?0:20,0,arthropod?(i%2?1:-1)*25:0]]]));
    for(const [i,n]of tentacles.entries())death.push(staged(n,1,[[0,zero],[.7,[-(i<2?6:10),0,0]],[1,[-(i<2?6:10),0,0]]]));
    if(bird||ray)death.push(staged('left_wing',1,[[0,zero],[.7,[0,0,-35]],[1,[0,0,-35]]]),staged('right_wing',1,[[0,zero],[.7,[0,0,35]],[1,[0,0,35]]]));
    if(has('egg'))death.push(staged('egg',1,[[0,unit],[.7,[1,.25,1]],[1,[1,.25,1]]],'scale'));
    if(has('cage'))death.push(staged('cage',1,[[0,unit],[.7,[.4,.4,.4]],[1,[.4,.4,.4]]],'scale'));
    clip('death',1,'hold',death);
    // Settle the lowest transformed body vertex on the floor, rather than
    // leaving a tilted corpse hovering or burying it under the terrain plane.
    settleDeath(model);
  }
  const beforeClips=original.animations;
  delete original.animations;
  const geometryOnly=structuredClone(model); delete geometryOnly.animations;
  assert.deepEqual(geometryOnly,original,'Animation pass changed geometry/texture metadata');
  assert.deepEqual(model.animations.map(a=>a.name),beforeClips.map(a=>a.name));
  const output={format_version:'1.8.0',geckolib_format_version:2,animations:{}};
  for(const a of model.animations) {
    const out={animation_length:a.length,bones:{}};
    if(a.loop==='loop')out.loop=true;else if(a.loop==='hold')out.loop='hold_on_last_frame';
    for(const animator of Object.values(a.animators)) {
      const bone=out.bones[animator.name]={};
      for(const k of animator.keyframes) {
        assert(k.time>=0 && k.time<=a.length, 'Keyframe outside duration');
        let v=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));assert(v.every(Number.isFinite));
        if(k.channel==='rotation')v=[-v[0],-v[1],v[2]];else if(k.channel==='position')v=[-v[0],v[1],v[2]];
        (bone[k.channel]??={})[String(k.time)]=v.map(round);
      }
    }
    output.animations[a.name]=out;
  }
  jobs.push([file,model],[`src/main/resources/assets/darkspawn/geckolib/animations/entity/${id}.animation.json`,output]);
  console.log(`${id}: ${form.toLowerCase()}, ${behavior.toLowerCase()}, ${model.animations.length} clips`);
}
if(write)for(const [file,data]of jobs)fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
console.log(`${write?'Saved':'Dry run:'} ${changed} clips across ${roster.length} creatures.`);
