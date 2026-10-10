// Animation-only polish; dry-run unless --write is supplied. Repeated runs are safe.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {Rig} = require('./rig.cjs');
const catalog = require('./catalog.cjs');
const version = 1;
const round = n => Math.abs(n) < 1e-8 ? 0 : Number(n.toFixed(6));
const sin = p => Math.sin(p * Math.PI * 2);
const smooth = p => p * p * (3 - 2 * p);
const value = k => ['x','y','z'].map(axis => Number(k.data_points[0][axis]));
function sample(keys,t) {
  if(t<=keys[0].time)return value(keys[0]);
  const next=keys.findIndex(k=>k.time>=t);
  if(next<0)return value(keys.at(-1));
  const a=keys[next-1],b=keys[next],p=smooth((t-a.time)/(b.time-a.time));
  return value(a).map((n,i)=>n+(value(b)[i]-n)*p);
}
const requested=process.argv.slice(2).filter(a=>a!=='--write');
const roster=catalog.filter(e=>e.category==='boss'&&(!requested.length||requested.includes(e.id)));
for(const id of requested)assert(roster.some(e=>e.id===id),'Unknown boss '+id);
let count=0;
const jobs=[];
for(const entry of roster){
  const {id,form}=entry,folder='art/blockbench/'+id;
  const file=folder+'/'+fs.readdirSync(folder).find(f=>f.endsWith('.bbmodel'));
  const model=JSON.parse(fs.readFileSync(file,'utf8')),before=structuredClone(model);
  const rig=new Rig(entry);rig.model=model;rig.bones=new Map(model.groups.map(g=>[g.name,g]));
  const bespoke=['ancient_tree_spirit','mutant_wolf','mutant_zombie','fossil_tyrant'].includes(id);
  let changed=0;
  for(let index=0;index<model.animations.length;index++){
    const old=model.animations[index],name=old.name.split('.').pop(),length=old.length;
    // Lifecycle silhouettes, planted death poses and timed reveals are already authored.
    if(['awaken','defeat','death'].includes(name)||old.darkspawn_polish_version===version)continue;
    const warning=/^(windup_|charge_)/.test(name),loop=old.loop==='loop',phase=name==='phase_change';
    const move=['walk','charge'].includes(name),wrath=name==='wrath_idle';
    const tracks=[];
    for(const g of model.groups)for(const channel of ['rotation','position','scale']){
      const keys=(old.animators[g.uuid]?.keyframes||[]).filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);
      if(!keys.length)continue;
      const neutral=channel==='scale'?[1,1,1]:[0,0,0];
      const active=keys.some(k=>value(k).some((n,i)=>Math.abs(n-neutral[i])>1e-7));
      const protectedBone=g.name==='root'||g.name==='heartwood'||g.name==='head_look'||(id==='void_eye'&&(g.name==='crest'||g.name.startsWith('shield_')));
      if(protectedBone){tracks.push([g.name,channel,id==='void_eye'?[[0,neutral]]:keys.map(k=>[k.time,value(k)])]);continue;}
      // Retain each authored key time; add samples to make easing identical in both formats.
      const times=new Set(keys.map(k=>round(k.time)));
      for(let i=0;i<=40;i++)times.add(round(length*i/40));
      const secondary=/tail|tentacle_\d+_\d+|wing_tip|twigs|bough|canopy|ear|shirt_flap|forearm|hand/.test(g.name);
      const frames=[...times].sort((a,b)=>a-b).map(t=>{
        const p=t/length;
        let v=sample(keys,Math.min(t,length));
        if(channel==='rotation'){
          const suffix=Number(g.name.match(/\d+$/)?.[0]||0),side=g.name.startsWith('right')?-1:1;
          // Small delayed motion reads as mass without displacing the collision body.
          if(secondary){
            const amplitude=/tentacle/.test(g.name)?3:/wing_tip/.test(g.name)?7:/tail/.test(g.name)?2:1.5;
            const envelope=loop?1:Math.sin(Math.PI*p)**2;
            const axis=/wing_tip|canopy|ear/.test(g.name)?2:/tail|bough|twigs/.test(g.name)?1:0;
            v[axis]+=side*amplitude*(wrath?1.5:1)*envelope*sin(p-(suffix+1)*.11);
          }
          if(warning&&active&&g.name==='head')v[0]+=1.2*Math.sin(Math.PI*p)**2*sin(p*2);
          // Generated spider legs sweep and lift in alternating tripods; they do not march like bipeds.
          if(!bespoke&&move&&['ARTHROPOD','QUEEN'].includes(form)&&/^leg_\d+$/.test(g.name)){
            const gait=p+(suffix%2)*.5,side=suffix%2?1:-1;
            v=[0,side*15*sin(gait),side*9*Math.max(0,sin(gait))];
          }
          if(!bespoke&&move&&form==='SERPENT'&&/^segment_/.test(g.name))v=[0,7*sin(p-suffix*.08),0];
          if(!bespoke&&move&&form==='BIRD'&&/^(left|right)_wing$/.test(g.name))v=[0,0,side*24*sin(p)];
          if(!bespoke&&move&&form==='BIRD'&&/^leg_/.test(g.name))v=[12,0,0];
          // Root-like mushroom limbs flex subtly instead of taking humanoid strides.
          if(!bespoke&&move&&form==='MUSHROOM'&&/^leg_/.test(g.name))v=[3*sin(p+suffix*.5),0,0];
          if(!bespoke&&phase&&g.name==='head')v[2]+=3*Math.sin(Math.PI*p)**2*sin(p*2);
        }
        return[t,v.map(round)];
      });
      // Keep constant channels compact and loop endpoints exact.
      if(loop)frames.at(-1)[1]=[...frames[0][1]];
      tracks.push([g.name,channel,frames.every(f=>f[1].every((n,i)=>n===frames[0][1][i]))?[frames[0]]:frames]);
    }
    rig.clip(name,length,old.loop,tracks);
    const replacement=model.animations.pop();
    model.animations[index]={...old,animators:replacement.animators,darkspawn_polish_version:version};
    changed++;
  }
  const rest=m=>{const c=structuredClone(m);delete c.animations;return c;};
  assert.deepEqual(rest(model),rest(before),'Geometry or textures changed');
  assert.deepEqual(model.animations.map(a=>[a.name,a.length,a.loop,a.uuid]),before.animations.map(a=>[a.name,a.length,a.loop,a.uuid]));
  const output={format_version:'1.8.0',geckolib_format_version:2,animations:{}};
  for(const a of model.animations){
    const out={animation_length:a.length,bones:{}};
    if(a.loop==='loop')out.loop=true;else if(a.loop==='hold')out.loop='hold_on_last_frame';
    for(const animator of Object.values(a.animators)){
      assert(rig.bones.has(animator.name));
      const bone=out.bones[animator.name]={};
      for(const k of animator.keyframes){
        assert(k.time>=0&&k.time<=a.length+1e-6);
        let v=value(k);assert(v.every(Number.isFinite));
        if(k.channel==='rotation')v=[-v[0],-v[1],v[2]];else if(k.channel==='position')v=[-v[0],v[1],v[2]];
        (bone[k.channel]??={})[String(k.time)]=v;
      }
    }
    output.animations[a.name]=out;
  }
  jobs.push([file,model],[`src/main/resources/assets/darkspawn/geckolib/animations/entity/${id}.animation.json`,output]);
  count+=changed;console.log(id+': '+changed+' polished clips');
}
if(process.argv.includes('--write'))for(const [file,data]of jobs)fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
console.log(`${process.argv.includes('--write')?'Saved':'Dry run:'} ${count} clips across ${roster.length} bosses.`);
