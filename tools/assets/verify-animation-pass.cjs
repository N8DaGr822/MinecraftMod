// Read-only checks of editable/runtime animation parity and controller contracts.
const fs=require('node:fs'),assert=require('node:assert/strict');
const entries=require('./catalog.cjs').filter(e=>e.category!=='boss');
const axes=['x','y','z'];
const near=(a,b)=>a.length===b.length&&a.every((v,i)=>Math.abs(v-b[i])<1e-5);
let clips=0,frames=0;
for(const e of entries){
 const id=e.id,title=id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join('');
 const m=JSON.parse(fs.readFileSync(`art/blockbench/${id}/${title}.bbmodel`));
 const runtime=JSON.parse(fs.readFileSync(`src/main/resources/assets/darkspawn/geckolib/animations/entity/${id}.animation.json`)).animations;
 const bones=new Set(m.groups.map(g=>g.name));
 const contract=id==='endborn'?['idle','walk','attack','blink']:['idle','walk','attack','warning','active','hide','death'];
 assert.deepEqual(m.animations.map(a=>a.name.split('.').at(-1)).sort(),[...contract].sort(),id+' controller clip contract');
 assert.deepEqual(Object.keys(runtime).sort(),m.animations.map(a=>a.name).sort(),id+' runtime clip coverage');
 for(const a of m.animations){
  const exported=runtime[a.name],short=a.name.split('.').at(-1);
  assert.equal(a.length,exported.animation_length,a.name+' duration');
  assert.equal(exported.loop,a.loop==='loop'?true:a.loop==='hold'?'hold_on_last_frame':undefined,a.name+' loop');
  const cyclic=short==='idle'||short==='walk';
  for(const animator of Object.values(a.animators)){
   assert(bones.has(animator.name),a.name+' absent bone '+animator.name);
   for(const channel of ['rotation','position','scale']){
    const track=animator.keyframes.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);
    if(!track.length)continue;
    const values=k=>axes.map(axis=>Number(k.data_points[0][axis]));
    const times=new Set();
    for(const k of track){
     assert(!times.has(k.time),a.name+' duplicate keyframe');times.add(k.time);
     assert(k.time>=0&&k.time<=a.length,a.name+' keyframe outside clip');
     let v=values(k);assert(v.every(Number.isFinite),a.name+' invalid numeric keyframe');
     if(channel==='scale')assert(v.every(n=>n>0),a.name+' nonpositive scale');
     if(channel==='rotation')v=[-v[0],-v[1],v[2]];
     else if(channel==='position')v=[-v[0],v[1],v[2]];
     assert(near(v,exported.bones[animator.name][channel][String(k.time)]),a.name+' source/export mismatch');frames++;
    }
    if(cyclic)assert(near(values(track[0]),values(track.at(-1))),a.name+' loop seam on '+animator.name+'/'+channel);
    if(animator.name==='root'||animator.name==='crest'||animator.name.startsWith('shield_')){
     const neutral=channel==='scale'?[1,1,1]:[0,0,0];
     assert(track.every(k=>near(values(k),neutral)),a.name+' overwrites entity movement or shield alignment');
    }
   }
  }
  if(short==='death')assert.equal(a.length,1,a.name+' must fit synchronized 20-tick death');
  clips++;
 }
}
console.log(`${entries.length} creature projects, ${clips} clips, ${frames} keyframes: export parity, loop seams, controller names, death duration, and root/shield ownership passed.`);
