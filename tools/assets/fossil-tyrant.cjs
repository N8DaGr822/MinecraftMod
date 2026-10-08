const {Rig,rotation:rot,position:pos,wave}=require('./rig.cjs');
const entry=require('./catalog.cjs').find(e=>e.id==='fossil_tyrant');
const r=new Rig(entry),b=r.bone.bind(r),box=r.box.bind(r);
b('root');b('hips',[0,6,1],'root');
box('pelvic_arch','hips',0,5,1,5,2,3);box('dark_pelvic_core','hips',0,5.5,1,3,1.5,2,2);
b('torso',[0,7,1],'hips');box('spine','torso',0,7,1,1,4,1);
for(let i=0;i<5;i++){const y=7+i*.65,w=5.8-i*.35;for(const s of [-1,1]){box('rib_'+i+'_'+s,'torso',s*w*.37,y,0.1,w*.22,.32,4.5-i*.18);box('rib_front_'+i+'_'+s,'torso',s*w*.2,y,-2,w*.4,.32,.45);}box('vertebra_'+i,'torso',0,y,2.2,1.6,.5,.8,1);}
b('neck',[0,9.5,-1.2],'torso');box('neck_bone','neck',0,9.5,-1.8,2,2.7,2);box('neck_ridge','neck',0,10.5,-.5,1.4,2,.6,1);
b('head_look',[0,12,-2.5],'neck');b('head',[0,12,-2.5],'head_look');
box('skull','head',0,11.9,-3.8,4.8,2.1,4.4);box('snout','head',0,11.8,-5.8,3.8,1.5,2.4);box('nasal_ridge','head',0,13.4,-5,1.4,.5,3.4,1);
for(const s of [-1,1]){box('eye_socket_'+s,'head',s*2.42,12.4,-3.2,.15,.85,1.3,2);box('amber_eye_'+s,'head',s*2.52,12.64,-3.25,.1,.32,.48,3);box('cheek_arch_'+s,'head',s*2.2,11.7,-3, .4,1.1,2.4);for(let i=0;i<6;i++)box('upper_tooth_'+s+'_'+i,'head',s*1.55,11.05,-6.5+i*.6,.3,.85,.33);}
b('jaw',[0,11.8,-2.2],'head');box('lower_jaw','jaw',0,10.6,-4.65,3.8,.55,5.4);box('jaw_interior','jaw',0,11.14,-4.5,2.6,.06,4.7,2);
for(const s of [-1,1])for(let i=0;i<5;i++)box('lower_tooth_'+s+'_'+i,'jaw',s*1.5,11.1,-6.4+i*.72,.27,.6,.3);
for(const [side,s]of [['left',1],['right',-1]]){
 b(side+'_leg',[s*2.15,6,1],'hips');box(side+'_femur',side+'_leg',s*2.15,2.9,1,1.1,3.2,1.3);box(side+'_hip_joint',side+'_leg',s*2.15,5.2,1,1.8,1.6,1.9,1);
 b(side+'_shin',[s*2.15,3,1],'left'===side?'left_leg':'right_leg');box(side+'_shin',side+'_shin',s*2.15,.65,1.2,.65,2.4,.7);box(side+'_heel',side+'_shin',s*2.15,.25,1.3,1.3,.6,1.4);
 b(side+'_foot',[s*2.15,.7,1],'left'===side?'left_shin':'right_shin');for(let i=0;i<3;i++){box(side+'_toe_'+i,side+'_foot',s*2.15+(i-1)*.53,0,-.05,.4,.55,3.4);box(side+'_claw_'+i,side+'_foot',s*2.15+(i-1)*.53,.04,-1.85,.3,.33,.55,2);}
 b(side+'_arm',[s*2.9,9,-1],'torso');box(side+'_forearm',side+'_arm',s*2.9,7,-1.2,.55,2,.65);box(side+'_wrist',side+'_arm',s*2.9,6.9,-1.6,.55,.5,1.1);for(let i=0;i<2;i++)box(side+'_finger_'+i,side+'_arm',s*2.9+(i-.5)*.35,6.4,-2,.19,.65,.35,2);
}
for(let i=0;i<5;i++){b('tail_'+i,[0,5.5-i*.45,2.3+i*1.3],i?'tail_'+(i-1):'hips');box('tail_bone_'+i,'tail_'+i,0,4.7-i*.42,3+i*1.3,2.2-i*.35,1.5-i*.2,1.65);box('tail_spine_'+i,'tail_'+i,0,6-i*.6,3+i*1.3,.4,.6,.5,1);}
const tail=(length,amount)=>[...Array(5)].map((_,i)=>wave('tail_'+i,1,amount,length,0,i*.7));
r.clip('idle',4,'loop',[wave('torso',0,1.5,4),wave('jaw',0,2,4,-3),...tail(4,2)]);
r.clip('wrath_idle',1.5,'loop',[wave('torso',0,3,1.5,-5),wave('jaw',0,6,1.5,-10),...tail(1.5,4)]);
function gait(length,amount){return [wave('left_leg',0,amount,length),wave('right_leg',0,-amount,length),wave('left_shin',0,-amount*.4,length,7),wave('right_shin',0,amount*.4,length,7),wave('left_foot',0,-amount*.6,length,-7),wave('right_foot',0,amount*.6,length,-7),wave('torso',2,2,length),...tail(length,5)];}
r.clip('walk',1.2,'loop',gait(1.2,20));r.clip('charge',.5,'loop',[...gait(.5,35),rot('neck',[[0,[-17,0,0]]]),rot('jaw',[[0,[-15,0,0]]])]);
const poses={bite:{neck:[-12,0,0],head:[8,0,0],jaw:[-40,0,0]},sweep:{torso:[0,-23,0],tail_0:[0,-35,0],tail_1:[0,-22,0],neck:[0,20,0]},boulders:{neck:[25,0,0],head:[15,0,0],jaw:[-30,0,0]},charge:{torso:[-10,0,0],neck:[-18,0,0],jaw:[-15,0,0]},eruption:{torso:[10,0,0],neck:[18,0,0],jaw:[-45,0,0],left_arm:[55,0,0],right_arm:[55,0,0]}};
const impacts={bite:{neck:[-30,0,0],head:[15,0,0],jaw:[0,0,0]},sweep:{torso:[0,32,0],tail_0:[0,65,0],tail_1:[0,40,0],neck:[0,-15,0]},boulders:{neck:[-15,0,0],head:[-10,0,0],jaw:[-48,0,0]},charge:poses.charge,eruption:{torso:[-15,0,0],neck:[-20,0,0],jaw:[-12,0,0]}};
for(const [name,pose]of Object.entries(poses)){
 r.clip('windup_'+name,2,'hold',Object.entries(pose).map(([bone,v])=>rot(bone,[[0,[0,0,0]],[.65,v],[1.8,v],[2,v]])));
 if(name!=='charge')r.clip(name,1,'once',Object.entries({...pose,...impacts[name]}).map(([bone,v])=>rot(bone,[[0,pose[bone]||[0,0,0]],[.2,v],[1,[0,0,0]]])));
}
r.clip('recovery',1.4,'loop',[wave('neck',0,2,1.4,-12),wave('jaw',0,3,1.4,-8),...tail(1.4,2)]);
r.clip('phase_change',2,'once',[rot('neck',[[0,[0,0,0]],[.6,[25,0,0]],[1.4,[25,0,0]],[2,[0,0,0]]]),rot('jaw',[[0,[0,0,0]],[.5,[-48,0,0]],[1.6,[-48,0,0]],[2,[0,0,0]]]),...tail(2,12)]);
// Death stays within the existing twenty-tick removal window; reward timing is unchanged.
r.clip('death',1,'hold',[rot('hips',[[0,[0,0,0]],[.8,[0,0,72]],[1,[0,0,80]]]),pos('hips',[[0,[0,0,0]],[1,[0,-3,0]]]),rot('jaw',[[0,[0,0,0]],[1,[-24,0,0]]])]);
console.log(JSON.stringify(r.save('art/blockbench/fossil_tyrant/atlas.png')));
