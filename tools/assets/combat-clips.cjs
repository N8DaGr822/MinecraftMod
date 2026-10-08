const {rotation:rot,position:pos,wave}=require('./rig.cjs');
const attacks={thunder_bird:['lightning','volley','wind','storm'],titan_boa:['bite','venom','constrict','brood','charge'],baba_yaga:['hex','slam','miasma','brood','blink'],mountain_titan:['slam','boulders','eruption','sweep','faultline'],ice_wyrm:['frost','bite','burrow','blizzard'],kraken:['wave','ink','whirlpool','water_jet'],cave_crawler:['web','bite','brood','pounce','venom'],shadow_creeper_queen:['acid','sweep','shadow','brood','shadow_storm','charge'],mycelial_sovereign:['spores','roots','fungal_growth','eruption'],netherborn:['charge','flames','magma','brood','blink','constrict'],soulbound_colossus:['soul_drain','volley','soul_cages','beam','soul_storm'],void_eye:['beam','volley','portal','brood','void_rifts']};
function clips(r,e) {
 const names=[...r.bones.keys()],has=n=>r.bones.has(n),keep=t=>t.filter(x=>has(x[0]));
 const alive=(length,speed=1)=>keep([wave('head',0,1.5*speed,length),wave('jaw',0,2*speed,length,-2),...names.filter(n=>/^tail_\d+$|^segment_\d+$/.test(n)).map((n,i)=>wave(n,1,4*speed,length,0,i*.6)),...names.filter(n=>/^tentacle_\d+$/.test(n)).map((n,i)=>wave(n,0,5*speed,length,0,i*.7)),wave('left_wing',2,12*speed,length,6),wave('right_wing',2,-12*speed,length,-6),wave('left_wing_tip',2,8*speed,length),wave('right_wing_tip',2,-8*speed,length)]);
 const gait=(length,amount)=>keep([...alive(length),...names.filter(n=>/^leg_\d+$/.test(n)).map((n,i)=>wave(n,0,amount,length,0,(i%4===0||i%4===3)?0:Math.PI)),wave('left_arm',0,-amount*.55,length),wave('right_arm',0,amount*.55,length)]);
 r.clip('idle',3,'loop',alive(3));r.clip('walk',.9,'loop',gait(.9,24));
 if(e.category==='boss'){r.clip('wrath_idle',1.2,'loop',alive(1.2,2));r.clip('charge',.45,'loop',gait(.45,38));r.clip('recovery',1.5,'loop',keep([wave('head',0,2,1.5,-12),wave('jaw',0,3,1.5,-9)]));}
 function pose(name) {
  let p={};const put=(bone,v)=>{if(has(bone))p[bone]=v;};
  const arms=(left,right=left)=>{put('left_arm',left);put('right_arm',right);};
  const wings=angle=>{put('left_wing',[0,0,angle]);put('right_wing',[0,0,-angle]);put('left_wing_tip',[0,0,angle*.4]);put('right_wing_tip',[0,0,-angle*.4]);};
  const coils=amount=>names.filter(n=>/^segment_\d+$/.test(n)).forEach((n,i)=>put(n,[0,Math.sin(i*.7)*amount,0]));
  switch(name){
   case 'bite': put('head',[8,0,0]);put('neck',[-15,0,0]);put('jaw',[-38,0,0]);arms([0,-14,12],[0,14,-12]);break;
   case 'slam':case 'eruption':case 'faultline':arms([135,0,12],[135,0,-12]);put('head',[12,0,0]);put('body',[5,0,0]);break;
   case 'boulders':arms([35,0,15],[150,-12,-8]);put('head',[0,12,0]);break;
   case 'sweep':put('body',[0,-20,0]);arms([70,-30,18],[65,20,-18]);names.filter(n=>/^tail_\d+$/.test(n)).forEach(n=>put(n,[0,-24,0]));break;
   case 'charge':case 'pounce':put('head',[-15,0,0]);put('body',[-6,0,0]);arms([38,0,0]);break;
   case 'lightning':wings(65);put('head',[24,0,0]);break;
   case 'volley':wings(10);put('head',[-15,0,0]);arms([70,0,15],[70,0,-15]);break;
   case 'wind':wings(75);put('head',[-8,0,0]);break;
   case 'storm':wings(-25);put('head',[30,0,0]);break;
   case 'constrict':coils(30);put('head',[-9,0,0]);put('jaw',[-20,0,0]);arms([55,-25,20],[55,25,-20]);break;
   case 'burrow':put('neck',[-32,0,0]);put('head',[-25,0,0]);coils(18);break;
   case 'hex':arms([100,0,40],[45,0,-15]);put('head',[0,-12,0]);break;
   case 'blink':case 'shadow':arms([40,0,65],[40,0,-65]);put('head',[-20,0,0]);put('body',[0,18,0]);break;
   case 'soul_drain':arms([80,-25,12],[80,25,-12]);put('jaw',[-25,0,0]);put('head',[10,0,0]);break;
   case 'beam':put('head',[-7,0,0]);put('jaw',[-25,0,0]);arms([82,0,0]);break;
   case 'roots':arms([15,0,25],[15,0,-25]);put('head',[-10,0,0]);break;
   case 'fungal_growth':case 'brood':case 'soul_cages':arms([60,0,60],[60,0,-60]);put('head',[23,0,0]);put('jaw',[-30,0,0]);break;
   case 'wave':case 'whirlpool':case 'water_jet':case 'ink':names.filter(n=>/^tentacle_\d+$/.test(n)).forEach((n,i)=>put(n,[name==='whirlpool'?0:(i%2?30:-30),name==='whirlpool'?35:0,name==='wave'?(i%2?28:-28):0]));put('head',[name==='water_jet'?-12:12,0,0]);break;
   case 'portal':case 'void_rifts':put('head',[0,0,35]);put('crest',[0,45,0]);break;
   default:put('head',[16,0,0]);put('neck',[10,0,0]);put('jaw',[-32,0,0]);arms([40,0,30],[40,0,-30]);wings(35);coils(10);break;
  }
  return p;
 }
 const attackNames=e.category==='boss'?attacks[e.id]||[]:['attack'];
 for(const name of attackNames){const p=pose(name==='attack'?({SMASH:'slam',BOULDER:'boulders',POUNCE:'pounce',CHARGE:'charge',CONSTRICT:'constrict',LIGHTNING:'lightning',DIVE:'charge',SOUL_KEEPER:'soul_cages'}[e.behavior]||'bite'):name);
  if(e.category==='boss')r.clip('windup_'+name,2,'hold',Object.entries(p).map(([b,v])=>rot(b,[[0,[0,0,0]],[.7,v],[2,v]])));
  if(name==='charge'||name==='pounce')continue;
  const impact=Object.fromEntries(Object.entries(p).map(([b,v])=>[b,v.map(a=>-a*.38)]));
  if(name==='bite'&&has('jaw'))impact.jaw=[0,0,0];if(name==='wind'||name==='storm'){impact.left_wing=[0,0,-40];impact.right_wing=[0,0,40];}
  if(name==='slam'||name==='eruption'||name==='faultline'){if(has('body'))impact.body=[-18,0,0];if(has('left_arm'))impact.left_arm=[20,0,0];if(has('right_arm'))impact.right_arm=[20,0,0];}
  r.clip(name,.8,'once',Object.entries(impact).map(([b,v])=>rot(b,[[0,p[b]||[0,0,0]],[.18,v],[.8,[0,0,0]]])));
 }
 if(e.category==='boss')r.clip('phase_change',2,'once',Object.entries(pose('brood')).map(([b,v])=>rot(b,[[0,[0,0,0]],[.6,v],[1.4,v],[2,[0,0,0]]])));
 else {
  r.clip('warning',1.5,'loop',Object.entries(pose(e.behavior==='SMASH'?'slam':'bite')).map(([b,v])=>rot(b,[[0,v],[1.5,v]])));
  r.clip('active',.5,'loop',gait(.5,38));r.clip('hide',1,'hold',[['body','scale',[[0,[1,.55,1]]]]]);
 }
 r.clip('death',1,'hold',keep([rot('body',[[0,[0,0,0]],[.7,[0,0,55]],[1,[0,0,65]]]),pos('body',[[0,[0,0,0]],[1,[0,-e.height*.22,0]]]),rot('head',[[0,[0,0,0]],[1,[-22,0,0]]]) ]));
}
module.exports={clips,attacks};
