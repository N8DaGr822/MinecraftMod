// Shared anatomy, separate species: every project receives named joints and species-specific details.
// Coordinates below are fractions of the registered width/height; models face negative Z.
function buildShape(r, e) {
 const w=e.width,h=e.height,id=e.id;
 const bone=(name,p,parent='body')=>r.bone(name,[p[0]*w,p[1]*h,p[2]*w],parent);
 const box=(name,parent,x,y,z,dx,dy,dz,m=0)=>r.box(name,parent,x*w,y*h,z*w,dx*w,dy*h,dz*w,m);
 r.bone('root');bone('body',[0,.45,0],'root');
 const eye=(parent,x,y,z,size=.04)=>{box('eye_socket_'+x,parent,x,y,z,size*1.6,.07,.025,2);box('eye_'+x,parent,x,y+.018,z-.016,size,.031,.014,3);};
 const head=(x=0,y=.77,z=-.12)=>{bone('head_look',[x,y,z]);bone('head',[x,y,z],'head_look');};
 const limb=(name,x,y,z,dx,dy,dz,m=0)=>{bone(name,[x,y,z]);box(name+'_upper',name,x,y-dy,z,dx,dy,dz,m);};
 const horns=(parent,y=.88)=>{for(const s of [-1,1]){box('horn_base_'+s,parent,s*.2,y,-.17,.06,.1,.07,3);box('horn_tip_'+s,parent,s*.22,y+.07,-.15,.035,.07,.045,3);}};
 const cap=(parent,x,y,z,size)=>{box('cap_gills_'+x+'_'+z,parent,x,y,z,size,.055,size,1);box('cap_rim_'+x+'_'+z,parent,x,y+.04,z,size*1.1,.09,size*1.1,0);box('cap_crown_'+x+'_'+z,parent,x,y+.115,z,size*.8,.09,size*.8,0);};
 switch(e.form) {
 case 'HUMANOID': case 'SKELETON': case 'TREE': {
  const skeletal=e.form==='SKELETON'||/bonewalker|soul_keeper|tormented/.test(id),tree=e.form==='TREE';
  box('pelvis','body',0,.3,0,.45,.14,.3,tree?0:2);
  if(skeletal){box('spine','body',0,.42,.05,.08,.38,.08);for(let i=0;i<5;i++)for(const s of [-1,1]){box('rib_'+s+'_'+i,'body',s*.23,.47+i*.053,0,.08,.025,.37);box('rib_front_'+s+'_'+i,'body',s*.12,.47+i*.053,-.16,.24,.025,.055);}box('soul_heart','body',0,.55,0,.2,.16,.19,3);}
  else if(id==='hollowed'){for(const s of [-1,1])box('hollow_side_'+s,'body',s*.23,.36,0,.16,.4,.4);box('hollow_back','body',0,.37,.16,.35,.37,.08);box('hollow_glow','body',0,.49,.1,.16,.15,.04,3);}
  else {box('abdomen','body',0,.4,0,.46,.2,.32);box('chest','body',0,.56,0,.62,.2,.38,tree?0:1);box('sternum','body',0,.55,-.2,.18,.16,.04,3);}
  head();box('skull','head',0,.77,-.1,.44,.21,.4,skeletal?0:0);box('brow','head',0,.9,-.32,.46,.045,.065,1);
  eye('head',-.115,.858,-.312);eye('head',.115,.858,-.312);
  box('mouth','head',0,.79,-.312,.23,.042,.025,2);
  bone('jaw',[0,.79,-.1],'head');box('chin','jaw',0,.748,-.2,.3,.065,.22);for(const s of [-1,1])box('tooth_'+s,'head',s*.07,.803,-.335,.04,.025,.03,3);
  for(const s of [-1,1]){const side=s===1?'left':'right';limb(side+'_arm',s*.4,.72,0,.18,.28,.23,tree?0:1);bone(side+'_hand',[s*.4,.47,0],side+'_arm');box(side+'_fist',side+'_hand',s*.4,.37,-.02,.23,.12,.26);limb('leg_'+(s===1?0:1),s*.16,.36,0,.18,.3,.24,tree?0:2);box(side+'_foot','leg_'+(s===1?0:1),s*.16,0,-.035,.22,.08,.32);}
  if(tree){for(const s of [-1,1]){bone('branch_'+s,[s*.4,.62,0],s===1?'left_arm':'right_arm');box('branch_'+s,'branch_'+s,s*.48,.62,0,.18,.05,.09);box('branch_tip_'+s,'branch_'+s,s*.54,.63,0,.045,.17,.055);box('leaves_'+s,'head',s*.22,.94,0,.4,.06,.5,1);}box('root_toe','body',0,.06,.12,.6,.06,.14);}
  if(id==='mountain_titan'||/stoneborn|titan_spawn|magma_brute|infernal_spawn/.test(id)){for(const s of [-1,1]){box('shoulder_crag_'+s,s===1?'left_arm':'right_arm',s*.4,.7,0,.24,.16,.3,2);for(let i=0;i<3;i++)box('crystal_'+s+'_'+i,s===1?'left_arm':'right_arm',s*(.33+i*.06),.83,-.01,.04,.09-i*.015,.05,3);}box('forehead_crystal','head',0,.91,-.33,.08,.07,.035,3);}
  if(e.form==='SKELETON'){for(const s of [-1,1]){box('crown_horn_'+s,'head',s*.16,.97,-.1,.06,.1,.07,2);for(let i=0;i<3;i++)box('finger_'+s+'_'+i,s===1?'left_hand':'right_hand',s*.4+(i-1)*.055,.31,-.04,.035,.08,.065);}}
  break;
 }
 case 'QUADRUPED': case 'WOLF': case 'BIPED': {
  const wolf=e.form==='WOLF'||/icefang|stalker|familiar/.test(id),nether=e.region==='netherborn',biped=e.form==='BIPED';
  box('ribcage','body',0,.34,0,.5,.4,.65);box('shoulder_mane','body',0,.48,-.19,.58,.29,.35,1);
  head(0,.68,-.32);box('skull','head',0,.65,-.37,.35,.23,.32);box('snout','head',0,.64,-.55,.27,.14,.25,1);box('nose','head',0,.68,-.69,.22,.05,.025,2);
  eye('head',-.13,.79,-.535);eye('head',.13,.79,-.535);
  bone('jaw',[0,.68,-.32],'head');box('lower_jaw','jaw',0,.6,-.53,.25,.065,.32);
  for(const s of [-1,1]){box('ear_'+s,'head',s*.16,.87,-.3,.1,.13,.12,1);box('fang_'+s,'head',s*.09,.62,-.64,.025,.07,.025,3);}
  for(let i=0;i<(biped?2:4);i++){const s=i%2?1:-1,z=i<2?(biped?.18:-.23):.25;limb('leg_'+i,s*.22,.4,z,.13,.33,.16);box('paw_'+i,'leg_'+i,s*.22,0,z-.035,.17,.1,.23,2);for(let j=0;j<3;j++)box('claw_'+i+'_'+j,'leg_'+i,s*.22+(j-1)*.045,.02,z-.16,.027,.035,.06,3);}
  if(biped)for(const s of [-1,1]){limb(s===1?'left_arm':'right_arm',s*.3,.7,-.18,.06,.17,.08);box('raptor_claw_'+s,s===1?'left_arm':'right_arm',s*.3,.5,-.22,.035,.08,.09,3);}
  for(let i=0;i<4;i++){bone('tail_'+i,[0,.6-i*.05,.29+i*.13],i?'tail_'+(i-1):'body');box('tail_'+i,'tail_'+i,0,.51-i*.05,.38+i*.13,.16-i*.03,.14-i*.025,.19,1);}
  if(nether){for(const s of [-1,1]){box('tusk_'+s,'head',s*.22,.55,-.55,.07,.44,.065,3);box('tusk_tip_'+s,'head',s*.22,.9,-.58,.043,.1,.05,3);}for(let i=0;i<5;i++)box('dorsal_coal_'+i,'body',0,.74,.2-i*.11,.15,.16,.07,2);}
  if(wolf){for(let i=0;i<4;i++)box('mane_tuft_'+i,'body',0,.74,.17-i*.09,.18,.15,.08,1);}
  if(/goat|stormstrider|end_grazer/.test(id))horns('head');
  if(id==='hexed_frog'){box('frog_throat','head',0,.47,-.5,.4,.2,.25,3);for(const s of [-1,1])box('raised_eye_'+s,'head',s*.19,.85,-.43,.12,.15,.12,3);}
  break;
 }
 case 'BIRD': case 'RAY': {
  const ray=e.form==='RAY';
  box('breast','body',0,.25,0,.28,.4,.44,0);box('back_plumage','body',0,.55,.02,.32,.15,.5,1);
  head(0,.68,-.18);box('head','head',0,.67,-.25,.23,.27,.25,0);box('crown','head',0,.91,-.23,.18,.09,.2,1);
  if(!ray){box('beak','head',0,.71,-.4,.12,.12,.18,2);box('beak_hook','head',0,.64,-.47,.075,.12,.055,2);eye('head',-.08,.825,-.38);eye('head',.08,.825,-.38);for(let i=0;i<3;i++)box('crest_'+i,'head',0,.9,-.15+i*.05,.04,.08-i*.01,.08,3);}
  for(const s of [-1,1]){const side=s===1?'left':'right';bone(side+'_wing',[s*.12,.6,0]);box(side+'_wing_root',side+'_wing',s*.24,.52,0,.26,.12,.36,1);bone(side+'_wing_tip',[s*.32,.56,0],side+'_wing');box(side+'_outer_wing',side+'_wing_tip',s*.4,.49,.025,.18,.1,.32,0);
   for(let i=0;i<7;i++){box(side+'_flight_feather_'+i,side+'_wing_tip',s*(.2+i*.045),.47,.15+i*.02,.035,.045,.2+i*.025,i%3===0?3:0);}
   if(!ray){limb('leg_'+(s===1?0:1),s*.09,.3,.03,.035,.24,.045,2);for(let i=0;i<3;i++)box(side+'_talon_'+i,'leg_'+(s===1?0:1),s*.09+(i-1)*.029,0,-.015,.02,.065,.18,2);}
  }
  bone('tail_0',[0,.38,.19]);for(let i=0;i<5;i++)box('tail_feather_'+i,'tail_0',(i-2)*.038,.29+Math.abs(i-2)*.02,.42,.033,.045,.42-Math.abs(i-2)*.05,i%2?1:0);
  break;
 }
 case 'SERPENT': {
  const giant=e.category==='boss',count=giant?14:9,radius=giant?.33:.24,ice=e.region==='ice_wyrm';
  for(let i=0;i<count;i++){const a=i*Math.PI*1.75/(count-1),x=Math.cos(a)*radius,z=Math.sin(a)*radius;
   bone('segment_'+i,[x,.14,z]);box('coil_'+i,'segment_'+i,x,.015+i*.007,z,.19,.27,.19,0);box('belly_'+i,'segment_'+i,x,.02+i*.007,z-.015,.195,.055,.195,1);
   if(ice)box('spine_'+i,'segment_'+i,x,.27+i*.007,z,.04,.13,.04,3);
  }
  bone('neck',[0,.22,-.26]);box('rising_neck','neck',0,.2,-.26,.18,.51,.18,0);box('neck_belly','neck',0,.26,-.355,.12,.42,.02,1);
  head(0,.72,-.27);box('serpent_skull','head',0,.7,-.31,.29,.24,.3);box('brow','head',0,.9,-.35,.31,.1,.16,ice?1:0);
  eye('head',-.095,.82,-.468);eye('head',.095,.82,-.468);
  bone('jaw',[0,.72,-.23],'head');box('lower_jaw','jaw',0,.62,-.35,.26,.085,.3,1);for(const s of [-1,1])box('fang_'+s,'head',s*.09,.61,-.455,.033,.14,.034,3);
  box('forked_tongue_base','jaw',0,.708,-.51,.03,.015,.13,2);for(const s of [-1,1])box('tongue_tip_'+s,'jaw',s*.025,.708,-.59,.02,.015,.055,2);
  if(ice)for(const s of [-1,1]){box('ice_horn_'+s,'head',s*.14,.9,-.26,.035,.1,.06,3);box('cheek_frill_'+s,'head',s*.18,.73,-.22,.07,.18,.22,1);}
  break;
 }
 case 'ARTHROPOD': case 'QUEEN': {
  const queen=e.form==='QUEEN',crab=/crab/.test(id),scorpion=/scorpion/.test(id);
  box('abdomen','body',0,.3,.2,.47,queen?.43:.62,.48,0);box('thorax','body',0,.32,-.06,.31,.38,.3,1);
  head(0,queen?.73:.45,-.28);box('head','head',0,queen?.7:.35,-.3,.33,queen?.2:.33,.26,0);
  for(const s of [-1,1])for(let j=0;j<3;j++)eye('head',s*(.055+j*.049),queen?.8:.55,-.443+j*.005,.022);
  for(let i=0;i<(queen?6:8);i++){const s=i%2?1:-1,z=(Math.floor(i/2)-1.5)*.14;bone('leg_'+i,[s*.18,.5,z]);box('leg_upper_'+i,'leg_'+i,s*.32,.47,z,.32,.055,.065,0);bone('shin_'+i,[s*.45,.5,z],'leg_'+i);box('leg_lower_'+i,'shin_'+i,s*.455,.04,z,.05,.45,.05,0);box('leg_tip_'+i,'shin_'+i,s*.455,0,z-.015,.065,.06,.08,3);}
  for(const s of [-1,1]){bone(s===1?'left_arm':'right_arm',[s*.12,queen?.72:.45,-.35]);box('mandible_'+s,s===1?'left_arm':'right_arm',s*.13,queen?.45:.22,-.45,queen?.05:.06,queen?.28:.19,.08,3);box('fang_hook_'+s,s===1?'left_arm':'right_arm',s*.1,queen?.44:.2,-.49,.1,.055,.05,3);if(crab){box('claw_palm_'+s,s===1?'left_arm':'right_arm',s*.22,.4,-.5,.19,.22,.2,0);box('claw_pincer_'+s,s===1?'left_arm':'right_arm',s*.16,.55,-.57,.05,.18,.08,3);}}
  if(queen){for(let i=0;i<5;i++){box('crown_plate_'+i,'head',(i-2)*.085,.87,-.19,.075,.13,.3-Math.abs(i-2)*.035,1);}box('creeper_mouth','head',0,.735,-.44,.13,.07,.02,2);for(let i=0;i<5;i++){bone('tail_'+i,[0,.3,.38+i*.12],i?'tail_'+(i-1):'body');box('tail_'+i,'tail_'+i,0,.22,.44+i*.12,.14-i*.02,.12,.17,0);}box('tail_blade','tail_4',0,.23,.98,.05,.16,.16,3);}
  if(scorpion){for(let i=0;i<4;i++){bone('tail_'+i,[0,.6+i*.1,.35-i*.04],i?'tail_'+(i-1):'body');box('tail_arch_'+i,'tail_'+i,0,.55+i*.1,.35-i*.04,.1,.16,.13,1);}box('stinger','tail_3',0,.75,.12,.04,.25,.1,3);}
  if(!queen&&!scorpion){box('abdomen_plate','body',0,.88,.22,.3,.12,.3,1);}
  break;
 }
 case 'MUSHROOM': case 'PLANT': {
  box('stalk','body',0,.14,0,.25,.66,.26,1);box('chest_gills','body',0,.48,-.14,.2,.18,.045,3);
  head(0,.7,0);cap('head',0,.74,0,.88);
  for(const s of [-1,1]){limb('leg_'+(s===1?0:1),s*.19,.28,0,.11,.24,.13,1);box('root_foot_'+s,'leg_'+(s===1?0:1),s*.19,0,-.045,.25,.07,.3,2);bone(s===1?'left_arm':'right_arm',[s*.14,.52,0]);box('root_arm_'+s,s===1?'left_arm':'right_arm',s*.29,.48,0,.36,.075,.09,1);box('root_finger_'+s,s===1?'left_arm':'right_arm',s*.45,.36,0,.05,.17,.07,2);}
  for(let i=0;i<6;i++)box('cap_spot_'+i,'head',(i%3-1)*.2,.958,(i<3?-.16:.16),.1,.02,.11,1);
  eye('body',-.075,.62,-.147,.034);eye('body',.075,.62,-.147,.034);
  if(e.form==='PLANT'){for(const s of [-1,1]){box('leaf_'+s,s===1?'left_arm':'right_arm',s*.34,.5,0,.2,.04,.25,2);box('flower_petal_'+s,'head',s*.32,.83,0,.18,.12,.4,3);}}
  break;
 }
 case 'HOUSE': {
  box('cabin_floor','body',0,.36,0,.73,.065,.73,0);box('cabin','body',0,.425,0,.68,.31,.65,0);
  for(let i=0;i<5;i++)box('roof_tier_'+i,'body',0,.72+i*.045,0,.9-i*.14,.05,.9-i*.14,1);
  box('chimney','body',.23,.72,.17,.12,.28,.12,0);box('chimney_rim','body',.23,.95,.17,.16,.04,.16,2);
  for(const s of [-1,1]){limb('leg_'+(s===1?0:1),s*.24,.38,0,.085,.29,.09,3);box('knobby_knee_'+s,'leg_'+(s===1?0:1),s*.24,.19,0,.12,.08,.14,3);for(let i=0;i<3;i++)box('chicken_toe_'+s+'_'+i,'leg_'+(s===1?0:1),s*.24+(i-1)*.07,0,-.1,.045,.06,.35,3);box('window_frame_'+s,'body',s*.23,.52,-.334,.18,.16,.025,1);box('window_glow_'+s,'body',s*.23,.54,-.352,.12,.12,.015,3);box('window_cross_'+s,'body',s*.23,.54,-.365,.025,.12,.01,0);}
  box('door_frame','body',0,.43,-.34,.19,.27,.04,1);box('door_shadow','body',0,.44,-.365,.145,.24,.01,2);
  head(0,.6,-.35);box('witch_head','head',0,.6,-.4,.095,.11,.11,3);box('witch_nose','head',0,.63,-.47,.023,.035,.055,3);box('hat_brim','head',0,.707,-.4,.14,.015,.14,2);box('hat_crown','head',0,.722,-.4,.075,.08,.075,2);
  for(const s of [-1,1]){bone(s===1?'left_arm':'right_arm',[s*.045,.61,-.4],'head');box('witch_arm_'+s,s===1?'left_arm':'right_arm',s*.075,.54,-.42,.04,.1,.06,2);}
  break;
 }
 case 'KRAKEN': {
  box('mantle_lower','body',0,.2,0,.57,.42,.57,0);box('mantle_upper','body',0,.58,.03,.46,.27,.47,0);box('mantle_crown','body',0,.8,.04,.31,.2,.33,1);
  head(0,.4,-.15);for(const s of [-1,1]){box('eye_white_'+s,'head',s*.22,.48,-.285,.16,.12,.055,3);box('eye_pupil_'+s,'head',s*.22,.49,-.32,.035,.09,.014,1);}
  box('beak','body',0,.2,-.29,.15,.22,.13,1);
  for(let i=0;i<8;i++){const a=i*Math.PI/4,x=Math.cos(a),z=Math.sin(a);bone('tentacle_'+i,[x*.23,.28,z*.23]);for(let j=0;j<3;j++){const name='tentacle_'+i+'_'+j;bone(name,[x*(.25+j*.09),.24-j*.07,z*(.25+j*.09)],j?'tentacle_'+i+'_'+(j-1):'tentacle_'+i);box('tentacle_segment_'+i+'_'+j,name,x*(.29+j*.09),.01+j*.015,z*(.29+j*.09),.14-j*.025,.22-j*.04,.14-j*.025,0);for(let k=0;k<2;k++)box('sucker_'+i+'_'+j+'_'+k,name,x*(.29+j*.09),.065+k*.06,z*(.29+j*.09)-.07,.05,.035,.025,2);}}
  break;
 }
 case 'FISH': {
  box('fish_body','body',0,.15,0,.28,.65,.64);head(0,.5,-.25);box('fish_head','head',0,.3,-.33,.34,.5,.25,0);eye('head',-.11,.62,-.462);eye('head',.11,.62,-.462);
  bone('tail_0',[0,.45,.27]);box('tail_fin','tail_0',0,.1,.4,.035,.75,.28,2);box('dorsal_fin','body',0,.68,0,.03,.32,.35,1);
  for(const s of [-1,1]){bone(s===1?'left_wing':'right_wing',[s*.12,.4,0]);box('side_fin_'+s,s===1?'left_wing':'right_wing',s*.21,.35,0,.18,.06,.25,2);}break;
 }
 case 'CREEPER': {
  box('carapace','body',0,.2,0,.5,.52,.42);head(0,.72,-.08);box('head','head',0,.72,-.07,.66,.28,.6,0);eye('head',-.17,.88,-.379,.09);eye('head',.17,.88,-.379,.09);box('mouth_bridge','head',0,.78,-.377,.14,.07,.025,2);for(const s of [-1,1]){box('mouth_prong_'+s,'head',s*.105,.737,-.377,.07,.09,.025,2);}
  for(let i=0;i<4;i++){const s=i%2?1:-1,z=i<2?-.22:.22;limb('leg_'+i,s*.18,.25,z,.22,.2,.23);box('foot_'+i,'leg_'+i,s*.18,0,z-.025,.23,.08,.29,1);}
  box('back_spine','body',0,.35,.235,.08,.38,.07,1);break;
 }
 case 'ORB': case 'SKULL': case 'EYE': {
  const skull=e.form==='SKULL',eyeForm=e.form==='EYE';
  head(0,.5,0);box('core','head',0,.15,0,.65,.7,.65,eyeForm?1:0);box('upper_bevel','head',0,.83,0,.45,.17,.47,0);box('lower_bevel','head',0,0,0,.45,.17,.47,0);
  if(eyeForm){box('iris','head',0,.32,-.342,.31,.34,.05,3);box('pupil','head',0,.39,-.38,.105,.22,.025,2);box('glint','head',-.055,.52,-.396,.04,.065,.014,1);}
  else{eye('head',-.17,.6,-.34,.1);eye('head',.17,.6,-.34,.1);box('mouth','head',0,.28,-.34,.22,.12,.04,2);}
  if(skull){bone('jaw',[0,.3,0],'head');box('jaw','jaw',0,.14,-.08,.63,.13,.55);for(let i=0;i<5;i++)box('tooth_'+i,'jaw',(i-2)*.085,.25,-.36,.05,.09,.045);}
  else if(!eyeForm){for(const s of [-1,1])box('wisp_tail_'+s,'body',s*.16,0,.25,.07,.34,.09,3);}
  bone('crest',[0,.5,0],'root');
  if(eyeForm||id==='shardling'){for(let i=0;i<4;i++){const a=i*Math.PI/2;bone('shield_'+i,[0,.5,0],'crest');box('obsidian_shard_'+i,'shield_'+i,Math.cos(a)*.5,.25,Math.sin(a)*.5,.14,.5,.14,0);box('shard_rune_'+i,'shield_'+i,Math.cos(a)*.5,.37,Math.sin(a)*.5-.076,.035,.18,.018,3);}}
  break;
 }
 default: throw Error('Missing anatomy '+e.form);
 }
 return {bone,box,cap,horns};
}
module.exports={buildShape};
