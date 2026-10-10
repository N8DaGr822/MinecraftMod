// Refine existing forest projects without regenerating geometry, textures, or UUIDs.
// Run explicitly after reviewing: node tools/assets/forest-animation-pass.cjs --write
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {Rig, rotation, position, wave} = require('./rig.cjs');
const catalog = require('./catalog.cjs');
const ids = ['barkling', 'hollowed', 'rootcrawler', 'ancient_ent'];
const write = process.argv.includes('--write');
const zero = [0, 0, 0];
const turn = (bone, frames) => rotation(bone, frames);
const staged = (bone, length, poses) => turn(bone, poses.map(([fraction, value]) => [fraction * length, value]));
const jobs = [];
for (const id of ids) {
  const entry = catalog.find(e => e.id === id);
  const title = id.split('_').map(s => s[0].toUpperCase() + s.slice(1)).join('');
  const file = `art/blockbench/${id}/${title}.bbmodel`;
  const model = JSON.parse(fs.readFileSync(file, 'utf8'));
  const rig = new Rig(entry);
  rig.model = model;
  rig.bones = new Map(model.groups.map(g => [g.name, g]));
  const modified = [];
  function replace(name, length, loop, tracks) {
    const index = model.animations.findIndex(a => a.name === `animation.${id}.${name}`);
    assert(index >= 0, `Missing existing clip ${id}/${name}`);
    const old = model.animations[index];
    // Source rigs face -Z: positive X raises a hanging arm toward the target.
    tracks = tracks.map(([bone, channel, frames]) => [bone, channel, channel === 'rotation' ? frames.map(([time, v]) => [time, [-v[0], v[1], v[2]]]) : frames]);
    rig.clip(name, length, loop, tracks);
    const next = model.animations.pop();
    next.uuid = old.uuid;
    model.animations[index] = next;
    modified.push(name);
  }
  const crawler = id === 'rootcrawler', bark = id === 'barkling', ent = id === 'ancient_ent';
  if (!crawler) {
    const idleLength = bark ? 3 : ent ? 4 : 3.6;
    const idle = [wave('body', 2, bark ? 2 : 1, idleLength), wave('head', 0, bark ? 3 : 1.5, idleLength, ent ? -3 : 0),
      wave('branch_-1', 2, bark ? 5 : 2, idleLength, 0, Math.PI / 2),
      wave('branch_1', 2, bark ? 4 : 2, idleLength, 0, -Math.PI / 2),
      wave('right_arm', 0, 2, idleLength, ent ? -6 : -2), wave('left_arm', 0, 2, idleLength, ent ? -6 : -2, Math.PI)];
    if (id === 'hollowed') idle.push(wave('head', 2, 2, idleLength, 6), wave('jaw', 0, 2, idleLength, 5));
    replace('idle', idleLength, 'loop', idle);
    const length = bark ? .8 : ent ? 1.4 : 1.2;
    const amplitude = bark ? 23 : ent ? 14 : 12;
    const walk = [wave('body', 2, bark ? 3 : 2, length), wave('head', 2, bark ? -2 : -1, length),
      wave('right_arm', 0, bark ? 18 : 9, length, -5), wave('left_arm', 0, bark ? 18 : 7, length, -5, Math.PI),
      wave('branch_-1', 2, bark ? 7 : 3, length, 0, Math.PI / 2), wave('branch_1', 2, bark ? 7 : 3, length, 0, -Math.PI / 2)];
    for (let i = 0; i < 2; i++) {
      const phase = i * Math.PI;
      walk.push(wave(`leg_${i}`, 0, amplitude, length, 0, phase));
      // Lift during the forward swing; keep the stance half on the ground.
      walk.push(['leg_' + i, 'position', Array.from({length: 9}, (_, k) => {
        const p = k / 8, angle = p * Math.PI * 2 + phase;
        return [p * length, [0, Math.max(0, Math.cos(angle)) * entry.height * (bark ? .9 : .55), 0]];
      })]);
    }
    replace('walk', length, 'loop', walk);
    if (bark) {
      // The projectile already exists when this event arrives: begin at release.
      replace('attack', .65, 'once', [
        staged('right_arm', .65, [[0,[-105,0,-12]],[.16,[-72,0,-6]],[.45,[-28,0,3]],[1,zero]]),
        staged('right_hand', .65, [[0,[-18,0,0]],[.3,[12,0,0]],[1,zero]]),
        staged('left_arm', .65, [[0,[-22,0,15]],[.5,[-8,0,5]],[1,zero]]),
        staged('body', .65, [[0,[8,-12,0]],[.2,[3,5,0]],[1,zero]]),
        staged('branch_1', .65, [[0,[0,0,-8]],[.25,[0,0,9]],[.65,[0,0,-3]],[1,zero]])]);
      replace('hide', .55, 'hold', [
        position('body', [[0,zero],[.25,[0,-.1,0]],[.55,[0,-.15,0]]]),
        staged('head', .55, [[0,zero],[.55,[16,0,0]],[1,[22,0,0]]]),
        staged('right_arm', .55, [[0,zero],[1,[0,0,-14]]]),
        staged('left_arm', .55, [[0,zero],[1,[0,0,14]]]),
        staged('branch_-1', .55, [[0,zero],[1,[0,0,-20]]]),
        staged('branch_1', .55, [[0,zero],[1,[0,0,20]]])]);
    } else if (ent) {
      const ready = [-108,0,0];
      replace('warning', 1.25, 'loop', [
        staged('body', 1.25, [[0,zero],[.4,[-6,0,0]],[.8,[-10,0,0]],[1,[-10,0,0]]]),
        staged('right_arm', 1.25, [[0,zero],[.55,[-85,0,-8]],[.8,ready],[1,ready]]),
        staged('left_arm', 1.25, [[0,zero],[.55,[-85,0,8]],[.8,ready],[1,ready]]),
        staged('head', 1.25, [[0,zero],[.6,[-12,0,0]],[1,[-12,0,0]]]),
        staged('branch_-1', 1.25, [[0,zero],[.6,[0,0,-9]],[1,[0,0,-9]]]),
        staged('branch_1', 1.25, [[0,zero],[.6,[0,0,9]],[1,[0,0,9]]])]);
      replace('attack', .85, 'once', [
        staged('body', .85, [[0,[-10,0,0]],[.08,[13,0,0]],[.25,[16,0,0]],[.6,[6,0,0]],[1,zero]]),
        ...['right_arm','left_arm'].map(b => staged(b,.85,[[0,ready],[.08,[-35,0,0]],[.25,[-20,0,0]],[.6,[-12,0,0]],[1,zero]])),
        staged('head', .85, [[0,[-12,0,0]],[.12,[14,0,0]],[.5,[4,0,0]],[1,zero]]),
        ...[-1,1].map(s => staged('branch_'+s,.85,[[0,[0,0,s*9]],[.18,[0,0,-s*12]],[.45,[0,0,s*5]],[1,zero]]))]);
    } else {
      replace('attack', .7, 'once', [
        staged('right_arm',.7,[[0,[-88,-12,-12]],[.12,[-58,22,-5]],[.45,[-22,8,0]],[1,zero]]),
        staged('left_arm',.7,[[0,[-32,0,8]],[.3,[-45,0,4]],[1,zero]]),
        staged('body',.7,[[0,[7,-10,0]],[.2,[12,8,0]],[.55,[5,4,0]],[1,zero]]),
        staged('head',.7,[[0,[-6,0,6]],[.2,[8,0,4]],[1,zero]]),
        staged('jaw',.7,[[0,[12,0,0]],[.35,[7,0,0]],[1,zero]])]);
    }
    replace('death', 1, 'hold', [
      staged('body',1,[[0,zero],[.2,[8,0,3]],[.6,[25,0,20]],[.85,[35,0,55]],[1,[35,0,55]]]),
      position('body',[[0,zero],[.6,[0,-entry.height*.12,0]],[.85,[0,-entry.height*.2,0]],[1,[0,-entry.height*.2,0]]]),
      staged('head',1,[[0,zero],[.4,[15,0,-5]],[.85,[24,0,-8]],[1,[24,0,-8]]]),
      staged('right_arm',1,[[0,zero],[.4,[-20,0,-15]],[.85,[5,0,-8]],[1,[5,0,-8]]]),
      staged('left_arm',1,[[0,zero],[.4,[-20,0,15]],[.85,[5,0,8]],[1,[5,0,8]]]),
      ...[-1,1].map(s=>staged('branch_'+s,1,[[0,zero],[.6,[0,0,s*12]],[.85,[0,0,s*5]],[1,[0,0,s*5]]]))]);
  } else {
    replace('idle', 2.4, 'loop', [wave('body',0,1.5,2.4),wave('head',1,4,2.4),wave('right_arm',1,5,2.4),wave('left_arm',1,5,2.4,0,Math.PI)]);
    const walk = [wave('body',2,1.5,.65),wave('head',1,2,.65)];
    for(let i=0;i<8;i++) {
      const side=i%2 ? 1 : -1, phase=((Math.floor(i/2)+i%2)%2)*Math.PI;
      walk.push(turn('leg_'+i,Array.from({length:9},(_,k)=>{const p=k/8,a=p*Math.PI*2+phase;return[p*.65,[0,14*Math.sin(a),side*7*Math.cos(a)]];})));

      walk.push(wave('shin_'+i,2,-side*10,.65,0,phase+Math.PI/2));
    }
    replace('walk', .65, 'loop', walk);
    replace('warning',1.25,'loop',[
      position('body',[[0,zero],[.5,[0,-.08,0]],[1.25,[0,-.08,0]]]),
      staged('head',1.25,[[0,zero],[.4,[-12,0,0]],[1,[-12,0,0]]]),
      ...Array.from({length:8},(_,i)=>staged('leg_'+i,1.25,[[0,zero],[.4,[0,0,(i%2?1:-1)*12]],[1,[0,0,(i%2?1:-1)*12]]])),
      staged('right_arm',1.25,[[0,zero],[.6,[-20,-20,0]],[1,[-20,-20,0]]]),
      staged('left_arm',1.25,[[0,zero],[.6,[-20,20,0]],[1,[-20,20,0]]])]);
    replace('attack',.65,'once',[
      position('body',[[0,[0,-.08,0]],[.05,[0,.07,0]],[.2,[0,.02,0]],[.65,zero]]),
      staged('head',.65,[[0,[-12,0,0]],[.08,[16,0,0]],[.4,[5,0,0]],[1,zero]]),
      ...Array.from({length:8},(_,i)=>staged('leg_'+i,.65,[[0,[0,0,(i%2?1:-1)*12]],[.1,[0,0,-(i%2?1:-1)*6]],[.5,[0,0,(i%2?1:-1)*3]],[1,zero]])),
      staged('right_arm',.65,[[0,[-20,-20,0]],[.1,[12,12,0]],[.5,[4,4,0]],[1,zero]]),
      staged('left_arm',.65,[[0,[-20,20,0]],[.1,[12,-12,0]],[.5,[4,-4,0]],[1,zero]])]);
    replace('death',1,'hold',[
      position('body',[[0,zero],[.25,[0,.03,0]],[.65,[0,-.12,0]],[1,[0,-.12,0]]]),
      staged('head',1,[[0,zero],[.25,[-12,0,0]],[.65,[18,0,0]],[1,[18,0,0]]]),
      ...Array.from({length:8},(_,i)=>staged('leg_'+i,1,[[0,zero],[.25,[0,0,-(i%2?1:-1)*12]],[.65,[0,0,(i%2?1:-1)*25]],[1,[0,0,(i%2?1:-1)*25]]]))]);
  }
  // Export animations only. The geometry and shared atlas are intentionally untouched.
  const output = {format_version:'1.8.0',geckolib_format_version:2,animations:{}};
  for(const a of model.animations) {
    const out = {animation_length:a.length,bones:{}};
    if(a.loop==='loop')out.loop=true;else if(a.loop==='hold')out.loop='hold_on_last_frame';
    for(const animator of Object.values(a.animators)) {
      const bone=out.bones[animator.name]={};
      for(const k of animator.keyframes) {
        let v=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));
        assert(v.every(Number.isFinite));
        if(k.channel==='rotation')v=[-v[0],-v[1],v[2]];
        else if(k.channel==='position')v=[-v[0],v[1],v[2]];
        (bone[k.channel]??={})[String(k.time)]=v;
      }
    }
    output.animations[a.name]=out;
  }
  jobs.push([file,model],[`src/main/resources/assets/darkspawn/geckolib/animations/entity/${id}.animation.json`,output]);
  console.log(`${id}: ${modified.join(', ')}`);
}
if(write)for(const [file,data]of jobs)fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
else console.log('Dry run. Pass --write to update the four projects and their animation exports.');
