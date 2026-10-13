// Expedition Armor: One geometry definition drives wearable meshes, inventory models, and editable Blockbench sources.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '../..');
const res = path.join(root, 'src/main/resources');
const write = (file, value) => { fs.mkdirSync(path.dirname(file), {recursive:true}); fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n'); };
const resource = (file, value) => write(path.join(res, file), value);
function uuid(key) { const h=crypto.createHash('sha256').update(key).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`; }
function crc(buffer) { let n=0xffffffff; for(const b of buffer){n^=b;for(let i=0;i<8;i++)n=(n>>>1)^((n&1)?0xedb88320:0);}return(n^0xffffffff)>>>0; }
function png(width,height,pixels) {
  const chunk=(name,data)=>{const type=Buffer.from(name),size=Buffer.alloc(4),sum=Buffer.alloc(4);size.writeUInt32BE(data.length);sum.writeUInt32BE(crc(Buffer.concat([type,data])));return Buffer.concat([size,type,data,sum]);};
  const header=Buffer.alloc(13);header.writeUInt32BE(width);header.writeUInt32BE(height,4);header[8]=8;header[9]=6;
  const rows=Buffer.alloc((width*4+1)*height);for(let y=0;y<height;y++)pixels.copy(rows,y*(width*4+1)+1,y*width*4,(y+1)*width*4);
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);
}
const origins={head:[0,0,0],body:[0,0,0],right_arm:[-5,2,0],left_arm:[5,2,0],right_leg:[-1.9,12,0],left_leg:[1.9,12,0]};
const families={
  mountaineering:{palette:[[47,65,78],[143,160,170],[222,209,174],[97,60,42],[173,121,57],[245,221,149],[29,36,44],[70,115,132]],accent:'minecraft:white_wool'},
  spelunking:{palette:[[71,70,64],[113,125,132],[153,119,65],[80,51,37],[191,130,48],[255,237,163],[28,34,40],[107,168,176]],accent:'minecraft:copper_ingot'},
  diver:{palette:[[39,68,65],[166,123,62],[211,171,95],[81,52,37],[231,189,98],[126,214,215],[26,37,40],[61,113,122]],addition:'minecraft:prismarine_shard'},
  desert_wanderer:{palette:[[214,199,160],[149,123,83],[239,224,180],[106,71,47],[184,139,63],[183,211,212],[62,54,45],[151,80,48]],addition:'minecraft:rabbit_hide'},
  ashwalker:{palette:[[54,47,49],[67,69,75],[106,103,99],[59,39,35],[177,90,44],[246,143,58],[26,28,34],[122,57,41]],addition:'minecraft:magma_cream'}
};
const armorStyles=['classic','viking','spartan','war_chief','knight','berserker','samurai','juggernaut','pathfinder','reaver','pirate','conquistador','ranger','paladin','shadowblade'];
for(const metal of ['iron','diamond'])for(const style of armorStyles)families[`${metal}_${style}`]={
  metal,style,palette:metal==='iron'?[[49,56,66],[153,163,174],[205,214,223],[81,49,37],[174,126,61],[138,42,37],[30,35,44],[219,211,187]]:[[28,68,78],[47,178,191],[135,229,232],[50,67,79],[163,198,205],[42,89,143],[19,43,58],[214,234,230]]
};
const legendaryBosses={dragonlord:['ice_wyrm','netherborn'],phoenix:['thunder_bird','netherborn'],voidwalker:['void_eye','shadow_creeper_queen'],titan:['mountain_titan','fossil_tyrant'],soul_reaper:['soulbound_colossus','baba_yaga']};
const legendaryPalettes={
 dragonlord:[[30,24,28],[43,30,36],[171,45,40],[48,25,25],[213,155,59],[240,78,40],[15,15,20],[121,40,43]],
 phoenix:[[63,34,24],[194,115,30],[255,204,71],[110,39,26],[255,228,139],[255,86,30],[60,22,24],[229,139,48]],
 voidwalker:[[22,18,33],[42,33,61],[99,57,141],[30,21,44],[165,104,220],[219,160,255],[12,11,20],[69,49,90]],
 titan:[[47,40,34],[102,83,61],[164,130,73],[61,46,31],[230,184,80],[255,225,150],[29,27,29],[129,105,70]],
 soul_reaper:[[25,32,38],[42,49,56],[115,136,143],[29,38,45],[59,192,208],[157,243,246],[15,22,28],[139,162,169]]
};
for(const [name,palette] of Object.entries(legendaryPalettes))families[name]={style:'legendary',legendary:true,palette};
const all=[];
for(const [family,config] of Object.entries(families)) {
  const mountain=family==='mountaineering';
  const parts=Object.entries(origins).map(([name,origin])=>({name,origin,cubes:[]}));
  function box(part,slot,name,from,size,material){parts.find(p=>p.name===part).cubes.push({name,slot,from,size,material});}
  if(config.style) {
    const shadow=config.style==='shadowblade';
    if(shadow){config.palette[3]=[30,25,42];config.palette[5]=[118,65,161];config.palette[7]=[53,50,67];}
    const ranger=config.style==='ranger',arbalist=config.style==='arbalist',brawler=config.style==='brawler';
    const reaver=config.style==='reaver',pirate=config.style==='pirate',conquistador=config.style==='conquistador';
    const viking=config.style==='viking',spartan=config.style==='spartan';
    const royal=config.style==='paladin',juggernaut=config.style==='juggernaut',pathfinder=config.style==='pathfinder';
    const commander=config.style==='war_chief',knight=config.style==='knight',berserker=config.style==='berserker',samurai=config.style==='samurai';
    function plate(part,slot,name,from,size,material,uv){box(part,slot,name,from,size,material);if(uv)parts.find(p=>p.name===part).cubes.at(-1).trim_uv=uv;}
    // Vanilla Armor Rework: Separate metal, lining, and trim-bearing plates; styles change silhouette only.
    plate('head','helmet','crown',[-4.5,-8.6,-4.4],[9,2.1,8.8],1,[0,0]);
    plate('head','helmet','back_guard',[-4.5,-6.5,3.45],[9,5.8,1],1,[0,2]);
    for(const x of [-4.5,3.45])plate('head','helmet','temple_'+x,[x,-6.5,-4.4],[1.05,4.9,7.85],1,[0,2]);
    plate('head','helmet','brow',[-4.65,-6.7,-4.7],[9.3,.8,.7],2);
    if(viking) {
      box('head','helmet','nasal',[-.55,-6.5,-4.85],[1.1,4.8,.65],2);
      box('head','helmet','ridge',[-.55,-9,-3.9],[1.1,.5,7.8],4);
      for(const x of [-3.9,1.2])box('head','helmet','spectacle_'+x,[x,-4.4,-4.75],[2.7,.65,.6],2);
      box('head','helmet','neck_mail',[-4.2,-1,3.55],[8.4,2.7,.75],0);
    } else if(spartan) {
      for(const x of [-4.45,2.35])plate('head','helmet','cheek_'+x,[x,-4.8,-4.75],[2.1,5.4,1],1,[0,3]);
      box('head','helmet','nose_guard',[-.55,-6.3,-4.95],[1.1,3.8,.6],2);
      box('head','helmet','crest_base',[-.85,-9.1,-4],[1.7,.7,8.8],4);
      for(const [i,y] of [-11.4,-12,-12.3,-12,-11.4].entries())box('head','helmet','crest_'+i,[-.65,y,-4+i*1.9],[1.3,-9.1-y,1.9],5);
    } else if(commander) {
      // War Chief: Armored commander silhouette, with a low metal crown rather than horns or tusks.
      box('head','helmet','crown_band',[-4.7,-8.2,-4.65],[9.4,.8,9.3],4);
      for(const [x,h] of [[-3.8,1.1],[-.65,2.1],[2.5,1.1]])box('head','helmet','crown_point_'+x,[x,-8.2-h,-4.65],[1.3,h,.7],4);
      plate('head','helmet','commander_brow',[-4.7,-6.65,-4.95],[9.4,1.2,.7],2,[0,2]);
      for(const x of [-4.45,2.95])plate('head','helmet','cheek_'+x,[x,-4.9,-4.7],[1.5,4.9,1.1],1,[0,3]);
      box('head','helmet','center_guard',[-.55,-5.6,-5],[1.1,2,.7],2);
    } else if(knight) {
      plate('head','helmet','visor_upper',[-4.5,-6.3,-4.85],[9,1.25,.8],1,[0,2]);
      plate('head','helmet','visor_lower',[-4.5,-4.25,-4.85],[9,3.95,.8],1,[0,3]);
      box('head','helmet','visor_slit',[-4.1,-5.1,-4.65],[8.2,.85,.3],6);
      for(const x of [-2.8,-1.4,1,2.4])box('head','helmet','breathing_slot_'+x,[x,-2.8,-5.3],[.4,1.5,.1],6);
      box('head','helmet','visor_ridge',[-.5,-6.4,-5.3],[1,6.2,.45],2);
      box('head','helmet','crown_ridge',[-.65,-9,-4],[1.3,.5,8],2);
    } else if(berserker) {
      box('head','helmet','brow_band',[-4.7,-6.65,-4.8],[9.4,1.1,.7],3);
      for(const x of [-3.6,-1.4,.8,3])box('head','helmet','brow_rivet_'+x,[x,-6.4,-5.05],[.6,.6,.3],4);
      for(const x of [-4.6,3.5])box('head','helmet','fur_flap_'+x,[x,-2.3,-2.8],[1.1,2.8,6.8],7);
      box('head','helmet','crest_ridge',[-.75,-9.15,-3.8],[1.5,.7,7.6],6);
    } else if(shadow) {
      box('head','helmet','shadow_hood',[-4.7,-9,-4.7],[9.4,1.5,9.4],3);
      for(const x of [-4.8,3.8])box('head','helmet','hood_side_'+x,[x,-7.5,-4.8],[1,7.7,9.4],3);
      box('head','helmet','face_wrap',[-4.45,-4.2,-4.95],[8.9,4.4,1],3);
      box('head','helmet','purple_brow',[-4.6,-6.6,-4.95],[9.2,.6,.55],5);
    } else if(ranger) {
      box('head','helmet','hood_top',[-4.65,-9,-4.65],[9.3,1.4,9.3],3);
      for(const x of [-4.8,3.8])box('head','helmet','hood_flap_'+x,[x,-7.6,-4.75],[1,7.8,9.4],3);
      box('head','helmet','hood_peak',[-4.5,-7.8,-5.5],[9,.7,1.4],7);
    } else if(arbalist) {
      plate('head','helmet','sight_visor',[-4.55,-6.3,-4.95],[9.1,1.3,.8],1,[0,2]);
      box('head','helmet','sight_slit',[-4.2,-5,-4.6],[8.4,.7,.3],6);
      plate('head','helmet','lower_visor',[-4.55,-4.3,-4.95],[9.1,3.8,.8],1,[0,3]);
      box('head','helmet','sight_ridge',[-.4,-9,-3.8],[.8,.6,7.6],4);
    } else if(brawler) {
      box('head','helmet','padded_brow',[-4.75,-7.5,-4.9],[9.5,1.5,1],3);
      for(const x of [-4.6,3.2])box('head','helmet','ear_guard_'+x,[x,-5.5,-2.6],[1.4,5,5.6],6);
      box('head','helmet','chin_guard',[-3.5,-1.1,-4.8],[7,1.6,.9],1);
    } else if(reaver) {
      plate('head','helmet','grim_mask',[-4.4,-4.8,-4.9],[8.8,4.8,.9],6,[0,3]);
      for(const x of [-3.3,-.4,2.5])box('head','helmet','mask_fang_'+x,[x,-3,-5.1],[.8,3,.5],2);
      for(const x of [-3.6,2.8])box('head','helmet','crown_spike_'+x,[x,-10.8,-.6],[.8,2.5,1.2],2);
    } else if(pirate) {
      box('head','helmet','broad_hat_brim',[-5.5,-8.5,-5.3],[11,.7,10.6],3);
      box('head','helmet','folded_front',[-4.8,-10.2,-5.3],[9.6,1.7,.7],3);
      for(const x of [-5.5,4.8])box('head','helmet','folded_side_'+x,[x,-9.5,-3.5],[.7,1.5,7],3);
      box('head','helmet','hat_badge',[-.6,-10,-5.6],[1.2,1.2,.4],4);
    } else if(conquistador) {
      box('head','helmet','morion_brim',[-5.2,-6.7,-5.2],[10.4,.65,10.4],2);
      box('head','helmet','morion_crest',[-.65,-11.6,-3.5],[1.3,3.1,7],1);
      box('head','helmet','crest_edge',[-.7,-11.9,-3.6],[1.4,.5,7.2],4);
      for(const x of [-4.5,3.25])plate('head','helmet','cheek_plate_'+x,[x,-4.8,-3.4],[1.25,4.6,4],1,[0,3]);
    } else if(royal) {
      box('head','helmet','royal_band',[-4.7,-8.7,-4.65],[9.4,1,9.3],4);
      for(const x of [-3.6,-.6,2.4])box('head','helmet','diadem_'+x,[x,-10.4,-4.65],[1.2,1.7,.65],4);
      box('head','helmet','royal_gem',[-.5,-8.6,-5],[1,1.2,.5],5);
      for(const x of [-4.5,2.7])plate('head','helmet','royal_cheek_'+x,[x,-4.5,-4.75],[1.8,4.5,1],1,[0,3]);
    } else if(juggernaut) {
      plate('head','helmet','heavy_face',[-4.7,-4.8,-5],[9.4,4.8,1.2],1,[0,3]);
      box('head','helmet','heavy_brow',[-4.8,-6.6,-5],[9.6,1.1,1],2);
      box('head','helmet','recessed_eyes',[-4.3,-5.5,-4.6],[8.6,.7,.3],6);
      for(const x of [-2.6,-.2,2.2])box('head','helmet','vent_'+x,[x,-3.6,-5.7],[.5,2.1,.15],6);
    } else if(pathfinder) {
      box('head','helmet','leather_cap',[-4.65,-8.95,-4.55],[9.3,1.2,9.1],3);
      box('head','helmet','sun_peak',[-4.6,-6.9,-6.2],[9.2,.5,2],3);
      box('head','helmet','trail_band',[-4.7,-7.8,-4.8],[9.4,.6,.4],7);
    } else if(samurai) {
      for(const y of [-3.8,-2.4,-1])plate('head','helmet','neck_lame_'+y,[-4.9,y,2.9],[9.8,1.1,2.1],1,[0,4]);
      for(const x of [-4.9,3.7])plate('head','helmet','flared_cheek_'+x,[x,-4.5,-4.3],[1.2,3.6,7.2],1,[0,3]);
      box('head','helmet','crest_center',[-.7,-8.2,-5.05],[1.4,1.5,.6],4);
      for(const sign of [-1,1]) {
        box('head','helmet','crescent_base_'+sign,[sign<0?-2.3:.6,-9,-5.05],[1.7,1.4,.6],4);
        box('head','helmet','crescent_tip_'+sign,[sign<0?-3.2:2.3,-11,-5.05],[.9,2.6,.6],4);
      }
      box('head','helmet','chin_mask',[-3.1,-2.2,-4.8],[6.2,2.2,.8],6);
    } else {
      for(const x of [-4.45,3.15])plate('head','helmet','cheek_'+x,[x,-3.4,-4.5],[1.3,3.6,3.2],1,[0,4]);
      box('head','helmet','center_ridge',[-.6,-8.95,-4],[1.2,.4,8],2);
    }
    box('body','chestplate','gambeson',[-4.4,.2,-2.45],[8.8,9.3,4.9],0);
    plate('body','chestplate','breastplate',[-4.2,1.1,-3.15],[8.4,5.7,1.05],1,[16,16]);
    plate('body','chestplate','back_plate',[-4.2,1.1,2.35],[8.4,7.9,.8],1,[16,16]);
    for(const y of [7,8.5])plate('body','chestplate','abdominal_'+y,[-3.95,y,-3.05],[7.9,1.25,.8],1,[16,20]);
    box('body','chestplate','collar',[-4.6,-.35,-2.8],[9.2,1.2,5.6],viking||berserker?7:2);
    box('body','chestplate','clasp',[-.8,1,-3.45],[1.6,1.3,.4],4);
    if(commander) {
      box('body','chestplate','command_sash',[1.7,.8,-3.5],[1.3,8.7,.3],5);
      box('body','chestplate','rank_badge',[1.5,2,-3.85],[1.7,1.5,.4],4);
      plate('body','chestplate','gorget',[-4.55,-1.2,-2.85],[9.1,1.2,5.7],1,[16,16]);
    }
    if(knight) {
      for(const x of [-4.7,3.65])plate('body','chestplate','neck_guard_'+x,[x,-1.3,-2.8],[1.05,2,5.6],1,[16,16]);
      plate('body','chestplate','sternum_ridge',[-.55,2.5,-3.45],[1.1,4,.45],2,[16,16]);
    }
    if(berserker) {
      box('body','chestplate','fur_mantle_back',[-4.8,-.8,2.5],[9.6,3.2,1.5],7);
      for(const x of [-4.5,2.6])box('body','chestplate','fur_mantle_front_'+x,[x,-.5,-3.45],[1.9,3.7,.7],7);
      box('body','chestplate','broad_strap',[-1.6,2.5,-3.35],[1.1,6.8,.4],3);
    }
    if(shadow) {
      box('body','chestplate','shadow_vest',[-4.3,1.3,-3.7],[8.6,7.2,.6],3);
      for(const x of [-3,2])box('body','chestplate','purple_binding_'+x,[x,1.5,-4],[1,7.1,.3],5);
    }
    if(ranger) {
      box('body','chestplate','quiver',[-3.8,1.5,3.1],[3.1,7.5,2.5],3);
      for(const x of [-3,-1.8])box('body','chestplate','arrow_fletching_'+x,[x,-.5,3.5],[.7,2.1,1.5],7);
      box('body','chestplate','bow_strap',[-2.8,1,-3.6],[1.1,8,.5],3);
    }
    if(arbalist) {
      for(const x of [-3.5,-1.5,.5,2.5])box('body','chestplate','bolt_pouch_'+x,[x,6,-3.7],[1.6,2.5,.8],3);
      box('body','chestplate','upper_harness',[-4,2.5,-3.5],[8,.8,.4],3);
    }
    if(brawler)box('body','chestplate','padded_chest',[-4.3,1.5,-3.55],[8.6,6.5,.6],3);
    if(reaver)box('body','chestplate','dark_cuirass',[-4.35,1.5,-3.6],[8.7,6.8,.65],6);
    if(pirate) {
      box('body','chestplate','sash',[-4.6,8.3,-3.5],[9.2,1.5,.5],5);
      box('body','chestplate','bandolier',[1.4,1.2,-3.5],[1.2,7.1,.5],3);
    }
    if(conquistador)box('body','chestplate','gilded_sternum',[-.55,2,-3.65],[1.1,5.5,.55],4);
    if(royal) {
      box('body','chestplate','royal_tabard',[-2.4,2,-3.5],[4.8,9.6,.4],5);
      box('body','chestplate','royal_emblem',[-.6,3,-4],[1.2,3,.5],4);
      box('body','chestplate','royal_emblem_cross',[-1.6,3.8,-4],[3.2,.7,.5],4);
    }
    if(juggernaut)plate('body','chestplate','heavy_cuirass',[-4.5,1.6,-3.9],[9,7,1.2],1,[16,16]);
    if(pathfinder) {
      box('body','chestplate','travel_strap',[-3,1,-3.6],[1.1,8,.5],3);
      box('body','chestplate','bedroll',[-4.5,2,3.2],[9,2.8,2.8],7);
      box('body','chestplate','trail_pouch',[1.1,6,-3.6],[2.8,2.5,1],3);
    }
    if(samurai)for(const y of [2,3.5,5,6.5])box('body','chestplate','lacing_'+y,[-3.7,y,-3.3],[7.4,.35,.35],5);
    for(const [part,x] of [['right_arm',-3],['left_arm',-1]]) {
      box(part,'chestplate','sleeve',[x-.3,-1.8,-2.3],[4.6,10.1,4.6],0);
      plate(part,'chestplate','pauldron',[x-.85,-2.8,-2.85],[5.7,2.7,5.7],1,[40,16]);
      plate(part,'chestplate','shoulder_lame',[x-.65,0,-2.65],[5.3,1.5,5.3],2,[40,16]);
      if(shadow) {
        box(part,'chestplate','shadow_mantle',[x-.9,-3.1,-2.9],[5.8,2,5.8],3);
        const outer=part==='right_arm'?x-1:x+4.7;
        for(const z of [-2.3,2.3]) {
          box(part,'chestplate','purple_edge_'+z,[outer,-4,z-.4],[.8,1.5,.8],5);
          box(part,'chestplate','edge_tip_'+z,[outer+.2,-4.5,z-.2],[.4,.6,.4],2);
        }
      }
      if(ranger || arbalist || brawler) {
        const outer=part==='right_arm'?x-1:x+4.7;
        if(ranger)box(part,'chestplate','light_mantle',[x-.9,-3,-2.9],[5.8,1.2,5.8],3);
        if(arbalist && part==='left_arm')plate(part,'chestplate','aiming_pauldron',[x-1.1,-3.4,-3.1],[6.2,3,6.2],1,[38,16]);
        if(brawler)box(part,'chestplate','padded_shoulder',[x-1,-3.1,-3],[6,2.7,6],6);
        for(const [i,z] of (ranger?[2.2]:arbalist?[-2.2,2.2]:[-2,0,2]).entries()) {
          box(part,'chestplate','shoulder_stud_'+i,[outer,-3.7,z-.45],[.9,1.2,.9],4);
          box(part,'chestplate','shoulder_stud_tip_'+i,[outer+.2,-4,z-.25],[.5,.4,.5],2);
        }
        if(brawler)for(const z of [-1.5,0,1.5])box(part,'chestplate','gauntlet_knuckle_'+z,[outer,6.4,z-.45],[.9,1.3,.9],2);
      }
      // Edge Spikes: Stepped tips retain the block-built art style and stay attached to each moving arm.
      if(reaver || pirate || conquistador) {
        const outer=part==='right_arm'?x-.9:x+4.9;
        const zs=reaver?[-2.6,0,2.6]:pirate?[-2.4,2.4]:[0];
        for(const [i,z] of zs.entries()) {
          const h=reaver?2.6:pirate?1.4:1.1;
          box(part,'chestplate','edge_spike_base_'+i,[outer-.55,-2.8-h,z-.55],[1.1,h,1.1],reaver?6:4);
          box(part,'chestplate','edge_spike_tip_'+i,[outer-.25,-3.4-h,z-.25],[.5,.7,.5],2);
        }
        if(reaver)plate(part,'chestplate','jagged_pauldron',[x-1.1,-3,-3.1],[6.2,1.4,6.2],6,[38,16]);
        if(pirate)box(part,'chestplate','leather_epaulet',[x-.95,-3,-2.95],[5.9,.5,5.9],3);
        if(conquistador)for(const y of [-1.4,-.3,.8])plate(part,'chestplate','stepped_pauldron_'+y,[x-.9,y,-2.9],[5.8,.65,5.8],1,[40,16]);
      }
      if(royal)box(part,'chestplate','gilded_epaulet',[x-.95,-3,-2.95],[5.9,.8,5.9],4);
      if(juggernaut) {
        plate(part,'chestplate','fortress_shoulder',[x-1.45,-3.6,-3.45],[6.9,4.6,6.9],1,[36,16]);
        box(part,'chestplate','fortress_rim',[x-1.5,.8,-3.5],[7,.7,7],2);
      }
      if(pathfinder)box(part,'chestplate','leather_shoulder',[x-.9,-3,-2.9],[5.8,.6,5.8],3);
      if(viking)plate(part,'chestplate','broad_shoulder',[x-1.2,-2.1,-3.2],[6.4,1.2,6.4],1,[38,16]);
      if(spartan)for(const side of [-2.5,2.15])box(part,'chestplate','leather_flap_'+side,[x-.25,1.4,side],[4.5,2.8,.45],3);
      if(commander) {
        plate(part,'chestplate','command_pauldron',[x-1.15,-3.3,-3.15],[6.3,2,6.3],1,[38,16]);
        box(part,'chestplate','command_edge',[x-1.2,-1.6,-3.2],[6.4,.6,6.4],4);
      }
      if(knight)for(const [i,w] of [5.8,5.4,5].entries())plate(part,'chestplate','shoulder_lame_'+i,[x-(w-4)/2,1+i*.9,-w/2],[w,.7,w],1,[40,16]);
      if(berserker&&part==='left_arm') {
        plate(part,'chestplate','heavy_pauldron',[x-1.3,-3.1,-3.3],[6.6,3.8,6.6],1,[37,16]);
        box(part,'chestplate','heavy_pauldron_rim',[x-1.35,.6,-3.35],[6.7,.7,6.7],6);
        for(const z of [-2,0,2])box(part,'chestplate','pauldron_rivet_'+z,[x+4.95,-2,z],[.45,.7,.7],4);
      }
      if(samurai)for(const y of [-1,1,3]) {
        plate(part,'chestplate','sode_'+y,[part==='right_arm'?x-1.05:x+3.6,y,-3],[1.45,1.6,6],1,[40,16]);
        box(part,'chestplate','sode_lace_'+y,[part==='right_arm'?x-1.15:x+3.5,y+.7,-3.15],[1.65,.3,6.3],5);
      }
      plate(part,'chestplate','vambrace',[x-.55,5.5,-2.55],[5.1,3.2,5.1],1,[40,20]);
      box(part,'chestplate','wrist_rim',[x-.6,8.4,-2.6],[5.2,.65,5.2],4);
    }
    box('body','leggings','belt',[-4.5,9.6,-2.5],[9,2.6,5],3);
    box('body','leggings','belt_clasp',[-.8,10,-2.9],[1.6,1.5,.5],4);
    for(const part of ['right_leg','left_leg']) {
      box(part,'leggings','lining',[-2.2,0,-2.2],[4.4,8.1,4.4],0);
      plate(part,'leggings','thigh_plate',[-2,.3,-2.7],[4,4.8,.7],1,[0,16]);
      plate(part,'leggings','knee_guard',[-2.1,5.5,-2.85],[4.2,2.3,.85],2,[0,21]);
      if(spartan)for(const x of [-1.85,.2])box(part,'leggings','skirt_strip_'+x,[x,.1,-2.9],[1.65,3.8,.4],3);
      if(commander||knight)plate(part,'leggings','hip_guard',[-1.85,.1,-2.95],[3.7,2.5,.45],1,[0,16]);
      if(berserker)box(part,'leggings','thigh_binding',[-2.3,3.9,-2.8],[4.6,.7,.4],3);
      if(samurai)for(const y of [.2,1.6,3]) {
        plate(part,'leggings','kusazuri_'+y,[-1.85,y,-3.05],[3.7,1.1,.5],1,[0,16]);
        box(part,'leggings','skirt_lace_'+y,[-1.65,y+.4,-3.2],[3.3,.25,.2],5);
      }
      if(royal)box(part,'leggings','royal_tasset',[-1.9,.4,-3],[3.8,3.2,.5],5);
      if(juggernaut)plate(part,'leggings','heavy_knee',[-2.4,5,-3.3],[4.8,3,1.1],1,[0,21]);
      if(pathfinder)box(part,'leggings','trail_pocket',[1.8,1,-1.8],[.9,3.6,3.6],3);
      if(royal) {
        box(part,'boots','gilded_cuff',[-2.6,7.4,-2.6],[5.2,1.3,5.2],4);
        box(part,'boots','ivory_shin',[-1.4,6.8,-3],[2.8,3.5,.5],7);
      }
      if(shadow) {
        box(part,'boots','shadow_cuff',[-2.55,7.3,-2.6],[5.1,1.4,5.2],3);
        for(const y of [8.6,10])box(part,'boots','purple_wrap_'+y,[-2.6,y,-3.3],[5.2,.45,.3],5);
        box(part,'boots','heel_blade',[-.45,7.5,2.45],[.9,3,1.1],2);
      }
      box(part,'boots','boot',[-2.35,8.2,-2.4],[4.7,3.8,4.8],3);
      plate(part,'boots','sabatons',[-2.5,9.5,-3.1],[5,2.4,5.65],1,[0,20]);
      if(ranger || arbalist || brawler) {
        box(part,'boots','specialist_cuff',[-2.6,7.4,-2.6],[5.2,1.4,5.2],ranger?3:1);
        if(ranger)for(const y of [8.8,10.2])box(part,'boots','boot_wrap_'+y,[-2.6,y,-3.2],[5.2,.5,.4],7);
        if(arbalist)box(part,'boots','reinforced_shin',[-1.8,6.9,-2.95],[3.6,3.7,.7],1);
        if(brawler)box(part,'boots','steel_toe',[-2.6,9.4,-3.5],[5.2,2.3,.8],2);
        const outer=part==='right_leg'?-3.1:2.5;
        for(const [i,z] of (brawler?[-1.6,0,1.6]:[1.4]).entries()) {
          box(part,'boots','ankle_stud_'+i,[outer,6.7,z-.4],[.7,1.4,.8],4);
          box(part,'boots','ankle_stud_tip_'+i,[outer+.15,6.3,z-.2],[.4,.5,.4],2);
        }
      }
      if(reaver || pirate || conquistador) {
        const outer=part==='right_leg'?-2.9:2.9;
        box(part,'boots','raised_cuff',[-2.6,7.5,-2.6],[5.2,1.3,5.2],pirate?3:1);
        for(const [i,z] of (reaver?[-1.7,.3,1.8]:[-1.6,1.6]).entries()) {
          box(part,'boots','cuff_spike_base_'+i,[outer-.45,6.3,z-.45],[.9,1.4,.9],reaver?6:4);
          box(part,'boots','cuff_spike_tip_'+i,[outer-.2,5.7,z-.2],[.4,.7,.4],2);
        }
        if(reaver)box(part,'boots','toe_blade',[-.65,10,-4.3],[1.3,1.3,1.6],2);
        if(pirate)box(part,'boots','boot_buckle',[-1,8.9,-3.25],[2,1.3,.55],4);
        if(conquistador)box(part,'boots','heel_spur',[-.5,10.3,2.4],[1,1.1,1.5],4);
      }
      box(part,'boots','sole',[-2.5,11.7,-3.1],[5,.65,5.7],6);
      plate(part,'boots','ankle_rim',[-2.5,8.15,-2.55],[5,1.1,5.1],2,[0,21]);
    }
    if(config.legendary) {
      const dragon=family==='dragonlord',phoenix=family==='phoenix',voidwalker=family==='voidwalker',titan=family==='titan',reaper=family==='soul_reaper';
      plate('body','chestplate','legendary_cuirass',[-4.55,1,-3.8],[9.1,7.5,1.2],1,[16,16]);
      box('body','chestplate','heart_gem',[-.8,2.3,-4.6],[1.6,2.2,.7],5);
      // Legendary Identity: Different profiles remain readable even without their palette or trim.
      if(dragon) {
        for(let i=0;i<3;i++)plate('body','chestplate','dragon_breast_scale_'+i,[-3.8+i*.55,3+i*1.8,-4.15],[7.6-i*1.1,1.6,.6],2,[16,16]);
        for(let i=0;i<4;i++)box('head','helmet','dragon_crest_'+i,[-.55,-10.6-i*.3,-3+i*2],[1.1,2.6,1.5],4);
      }
      if(phoenix)for(const sign of [-1,1])for(let i=0;i<4;i++) {
        box('body','chestplate','phoenix_back_feather_'+sign+'_'+i,[sign<0?-3.8-i*.6:2.8+i*.6,1+i*1.3,2.5],[1,5.5-i*.7,1],i%2?4:5);
        box('head','helmet','phoenix_crown_'+sign+'_'+i,[sign<0?-2.6-i*.55:1.6+i*.55,-9.8-i*.5,1+i*.8],[1,2.8,1.1],i%2?5:4);
      }
      if(voidwalker)for(let i=0;i<3;i++)box('left_arm','chestplate','void_shard_'+i,[3.5+i*.7,-4.4-i*.8,-2.5+i*1.8],[1,3.4,1.1],5);
      if(titan) {
        plate('body','chestplate','titan_belly',[-4.8,6.6,-4.4],[9.6,4.1,1.5],1,[16,16]);
        for(const part of ['left_leg','right_leg'])plate(part,'leggings','titan_thigh',[-2.8,0,-3.8],[5.6,5.5,1.5],1,[0,16]);
      }
      if(reaper) {
        for(let i=0;i<5;i++)box('body','chestplate','reaper_mantle_'+i,[-4.5+i*1.8,0,2.7],[1.8,11+(i%2)*2,.8],6);
        for(const part of ['left_leg','right_leg'])box(part,'leggings','reaper_robe',[-2.35,0,2],[4.7,7.5,.8],6);
      }
      if(dragon || phoenix)for(const sign of [-1,1])for(let i=0;i<3;i++) {
        const x=sign<0?-3.8-i*.55:2.8+i*.55;
        box('head','helmet','horn_'+sign+'_'+i,[x,-9.4-i*1.2,-1.7],[1,1.5,2],dragon?4:5);
      }
      if(voidwalker || reaper) {
        box('head','helmet','legendary_hood',[-4.8,-9,-4.8],[9.6,1.4,9.6],6);
        for(const x of [-4.8,3.8])box('head','helmet','hood_edge_'+x,[x,-7.6,-4.8],[1,7.8,9.6],6);
        box('head','helmet','dark_mask',[-4.3,-4.5,-5],[8.6,4.5,.8],6);
        for(const x of [-3,1.1])box('head','helmet','glowing_eye_'+x,[x,-5,-5.15],[1.9,.5,.3],5);
      }
      if(titan) {
        plate('head','helmet','titan_visor',[-4.7,-4.7,-5],[9.4,4.8,1.1],1,[0,3]);
        box('head','helmet','titan_ridge',[-.7,-10,-4.5],[1.4,1.5,9],4);
      }
      for(const [part,x] of [['right_arm',-3],['left_arm',-1]]) {
        const width=titan?7.2:6.4;
        plate(part,'chestplate','legendary_shoulder',[x-(width-4)/2,-3.6,-width/2],[width,3.3,width],1,[34,16]);
        for(let i=0;i<3;i++) {
          const outside=part==='right_arm'?x-1.3-i*.45:x+4.3+i*.45;
          if(phoenix)box(part,'chestplate','flame_fin_'+i,[outside,-6+i*.65,-2.6+i*1.7],[.75,3.3,1.1],i%2?5:4);
          else box(part,'chestplate','legendary_spike_'+i,[outside,-5.8+i*.35,-2.5+i*2],[.8,2.4,1],reaper?5:4);
          if(dragon || reaper)plate(part,'chestplate','layered_scale_'+i,[x-.85,i*1.15,-3.05],[5.7,.85,6.1],dragon?2:7,[38,16]);
        }
      }
      for(const part of ['right_leg','left_leg']) {
        plate(part,'leggings','legendary_tasset',[-2.3,.1,-3.3],[4.6,4.7,1],1,[0,16]);
        box(part,'boots','high_greave',[-2.65,6.6,-3.05],[5.3,3.9,6.1],1);
        box(part,'boots','legendary_toe',[-2.65,9.9,-3.7],[5.3,2.2,1.2],2);
        for(const x of [-2.6,1.9])box(part,'boots','cuff_spike_'+x,[x,5.3,-1.5],[.7,1.5,3],4);
        box(part,'boots','shin_gem',[-.5,7.2,-3.4],[1,1.7,.5],5);
      }
    }
  } else if(config.addition) {
    const diver=family==='diver',desert=family==='desert_wanderer';
    // Expedition Silhouettes: Diving porthole/tank, wrapped desert hood, and sealed ceramic ash mask.
    box('head','helmet','crown',[-4.6,-8.7,-4.5],[9.2,2.2,9],desert?0:1);
    box('head','helmet','back',[-4.6,-6.5,3.5],[9.2,6.8,1.1],desert?0:1);
    for(const x of [-4.6,3.5])box('head','helmet','side_'+x,[x,-6.5,-4.5],[1.1,6.8,8],desert?0:1);
    if(diver) {
      box('head','helmet','face',[-4.5,-6.5,-4.8],[9,6.5,1],1);
      box('head','helmet','port_rim',[-3.1,-6.2,-5.3],[6.2,4.9,.6],2);
      box('head','helmet','port_glass',[-2.5,-5.7,-5.65],[5,3.9,.4],7);
      for(const x of [-1.7,1.2])box('head','helmet','port_bar_'+x,[x,-5.7,-5.95],[.5,3.9,.3],2);
      box('head','helmet','lamp',[-1.1,-8,-5.1],[2.2,1.3,.7],5);
      box('body','chestplate','air_tank',[-3.2,1.2,2.6],[6.4,8.7,3.4],1);
      box('body','chestplate','tank_valve',[-.7,.2,3.4],[1.4,1,1.6],2);
      for(const y of [3,7])box('body','chestplate','tank_band_'+y,[-3.4,y,2.5],[6.8,.8,3.8],6);
    } else if(desert) {
      for(const y of [-8,-6.7])box('head','helmet','wrap_'+y,[-4.8,y,-4.7],[9.6,.7,9.5],2);
      box('head','helmet','face_wrap',[-4.5,-3.8,-4.9],[9,3.8,1],0);
      box('head','helmet','hood_tail',[-3.7,-.2,3.5],[7.4,4,.7],0);
      box('body','chestplate','scarf',[-4.7,-.5,-2.9],[9.4,2,5.8],2);
      box('body','chestplate','scarf_tail',[1.4,1.5,-3.3],[2.1,6,.6],2);
      box('body','chestplate','canteen',[-4.5,5,2.5],[3,4,1.8],3);
    } else {
      box('head','helmet','face_mask',[-4.5,-6.5,-4.8],[9,6.5,1],1);
      box('head','helmet','visor',[-3.8,-5.8,-5.1],[7.6,1.1,.4],6);
      for(const x of [-3.3,.8])box('head','helmet','eye_'+x,[x,-5.55,-5.35],[2.5,.5,.3],5);
      box('head','helmet','respirator',[-2,-3.9,-5.5],[4,2.6,1],6);
      for(const x of [-1.3,0,1.3])box('head','helmet','vent_'+x,[x,-3.4,-6],[.4,1.6,.2],4);
      box('body','chestplate','back_plate',[-4.2,1,2.5],[8.4,9,1.1],1);
    }
    box('body','chestplate','lining',[-4.4,.2,-2.45],[8.8,9.3,4.9],0);
    for(const y of [2,4.7,7.4])box('body','chestplate','plate_'+y,[-4.1,y,-3.1],[8.2,2.1,.8],1);
    for(const x of [-3,2.3])box('body','chestplate','harness_'+x,[x,0,-3.35],[.7,9.6,.3],desert?3:4);
    for(const [part,x] of [['right_arm',-3],['left_arm',-1]]) {
      box(part,'chestplate','sleeve',[x-.35,-1.8,-2.35],[4.7,10.2,4.7],0);
      box(part,'chestplate','shoulder',[x-.65,-2.4,-2.65],[5.3,2.5,5.3],desert?2:1);
      box(part,'chestplate','bracer',[x-.5,5.8,-2.5],[5,3.2,5],1);
      box(part,'chestplate','wrist_seal',[x-.55,8.6,-2.55],[5.1,.7,5.1],desert?2:6);
      if(!diver&&!desert)box(part,'chestplate','ember_inlay',[x-.6,-1.3,-2.8],[5.2,.4,.3],5);
    }
    box('body','leggings','belt',[-4.5,9.6,-2.5],[9,2.6,5],3);
    box('body','leggings','buckle',[-.8,10,-2.9],[1.6,1.5,.5],4);
    for(const part of ['right_leg','left_leg']) {
      box(part,'leggings','trouser',[-2.2,0,-2.2],[4.4,8.1,4.4],0);
      box(part,'leggings','thigh',[-1.9,.5,-2.65],[3.8,4.3,.6],desert?2:1);
      box(part,'leggings','knee',[-2.05,5.6,-2.8],[4.1,2,.8],1);
      box(part,'boots','boot',[-2.35,8.2,-2.4],[4.7,3.8,4.8],desert?0:3);
      box(part,'boots','toe',[-2.5,10.2,-3.1],[5,1.6,1.7],1);
      box(part,'boots','sole',[-2.5,11.6,-3.1],[5,.8,5.7],6);
      box(part,'boots','cuff',[-2.5,8.15,-2.6],[5,.9,5.2],desert?2:4);
      if(diver)box(part,'boots','ballast',[-2.6,10.9,-3.2],[5.2,.8,5.9],1);
    }
  } else {
  // Open-face Helmet: Shaped cap, cheek plates, rear guard, and a distinct brow/lamp silhouette.
  box('head','helmet','cap',[-4.6,-8.7,-4.5],[9.2,2.2,9],1);
  box('head','helmet','rear_guard',[-4.5,-6.5,3.5],[9,5.8,1.1],0);
  for(const x of [-4.6,3.6]) box('head','helmet','cheek_'+x,[x,-6.5,-3.9],[1,5.8,7.4],1);
  box('head','helmet','brow',[-4.7,-6.7,-4.7],[9.4,1.2,1],mountain?2:4);
  if(mountain) {
    box('head','helmet','fur_back',[-4.8,-1.8,2.9],[9.6,1.8,2],2);
    for(const x of [-4.8,3.6]) box('head','helmet','fur_flap_'+x,[x,-2.5,-3.9],[1.2,2.6,7.8],2);
    box('head','helmet','ridge',[-.7,-9,-3.7],[1.4,.4,7.5],4);
  } else {
    box('head','helmet','lamp_housing',[-2,-7.8,-5.4],[4,3.2,1.4],6);
    box('head','helmet','lamp_rim',[-1.6,-7.4,-5.8],[3.2,2.4,.5],4);
    box('head','helmet','lamp_lens',[-1.1,-7,-6],[2.2,1.6,.3],5);
    box('head','helmet','lamp_strap',[-4.7,-5.8,-4.2],[9.4,.7,8.4],3);
  }
  // Chest: Layered breastplate and leather back, with shoulders and gauntlets attached to animated arms.
  box('body','chestplate','jacket',[-4.4,.2,-2.45],[8.8,9.3,4.9],0);
  box('body','chestplate','breastplate',[-4.1,1.4,-3.2],[8.2,5.4,1.1],1);
  box('body','chestplate','lower_plate',[-3.8,7,-3.05],[7.6,2.2,.8],1);
  for(const x of [-3,2.3]) box('body','chestplate','strap_'+x,[x,0,-3.4],[.7,9.6,.3],3);
  box('body','chestplate','buckle',[-.8,7.5,-3.55],[1.6,1.2,.4],4);
  if(mountain) {
    box('body','chestplate','collar_back',[-4.9,-1.5,1.4],[9.8,2.8,2.4],2);
    for(const x of [-4.9,3.1]) box('body','chestplate','collar_side_'+x,[x,-1.2,-2.7],[1.8,2.5,4.1],2);
  } else {
    box('body','chestplate','supply_pouch',[-3.8,4,2.45],[7.6,5.5,2.4],3);
    box('body','chestplate','pouch_latch',[-.65,6,4.85],[1.3,1.6,.25],4);
  }
  for(const [part,x] of [['right_arm',-3],['left_arm',-1]]) {
    box(part,'chestplate','sleeve',[x-.35,-1.8,-2.35],[4.7,10.2,4.7],0);
    box(part,'chestplate','pauldron',[x-.8,-2.8,-2.85],[5.6,3.2,5.7],1);
    box(part,'chestplate','shoulder_trim',[x-.85,.1,-2.9],[5.7,.8,5.8],mountain?2:4);
    box(part,'chestplate','gauntlet',[x-.6,6,-2.6],[5.2,3.1,5.2],1);
    box(part,'chestplate','wrist',[x-.65,8.7,-2.65],[5.3,.7,5.3],mountain?2:3);
  }
  box('body','leggings','belt',[-4.5,9.6,-2.5],[9,2.6,5],3);
  box('body','leggings','belt_buckle',[-.8,10,-2.8],[1.6,1.5,.4],4);
  for(const part of ['right_leg','left_leg']) {
    box(part,'leggings','trouser',[-2.2,0,-2.2],[4.4,8.1,4.4],0);
    box(part,'leggings','thigh_plate',[-1.9,.4,-2.75],[3.8,4.5,.7],1);
    box(part,'leggings','knee_guard',[-2.05,5.5,-2.9],[4.1,2.2,.9],mountain?1:2);
    box(part,'boots','boot',[-2.35,8.2,-2.4],[4.7,3.8,4.8],3);
    box(part,'boots','toe_cap',[-2.5,10.2,-3.1],[5,1.6,1.7],1);
    box(part,'boots','cuff',[-2.55,8.15,-2.65],[5.1,1,5.3],mountain?2:4);
    box(part,'boots','sole',[-2.5,11.6,-3.1],[5,.8,5.7],6);
    if(mountain) for(const x of [-1.6,.9]) box(part,'boots','crampon_'+x,[x,12.4,-2.8],[.7,.5,5],1);
    else box(part,'boots','heel_pad',[-2.3,10.2,2.4],[4.6,2.2,.8],2);
  }
  }
  const geometry={texture_width:256,texture_height:128,parts};
  resource(`assets/darkspawn/geometry/armor/${family}.json`,geometry);
  const pixels=Buffer.alloc(256*128*4);
  for(let y=0;y<128;y++) for(let x=0;x<256;x++) {
    const material=Math.floor(x/64)+Math.floor(y/64)*4,base=config.palette[material];
    const noise=((x*17+y*31+(x*y)%23)%11)-5;
    const weave=material===0||material===3?((x+y)%4===0?-7:0):material===2?(y%4===0?9:-2):0;
    const highlight=material===1?(x%64<3?18:0):0;
    const i=(y*256+x)*4;for(let c=0;c<3;c++) pixels[i+c]=Math.max(0,Math.min(255,base[c]+noise+weave+highlight));pixels[i+3]=255;
  }
  const texture=png(256,128,pixels),textureFile=path.join(res,`assets/darkspawn/textures/equipment/${family}.png`);
  fs.mkdirSync(path.dirname(textureFile),{recursive:true});fs.writeFileSync(textureFile,texture);
  // Native Fallback: Equipment definitions require nonempty layers; Fabric's registered renderer replaces this fallback.
  if(!config.style || config.legendary)resource(`assets/darkspawn/equipment/${family}.json`,{layers:{humanoid:[{texture:config.legendary?'minecraft:netherite':'minecraft:iron'}],humanoid_leggings:[{texture:config.legendary?'minecraft:netherite':'minecraft:iron'}]}});
  const cubes=parts.flatMap(p=>p.cubes.map(c=>({...c,origin:p.origin,part:p.name})));
  const model={meta:{format_version:'5.0',model_format:'free',box_uv:true},name:family,model_identifier:family,resolution:{width:256,height:128},elements:[],groups:[],outliner:[],textures:[],animations:[]};
  for(const part of parts) {
    const id=uuid(family+'/'+part.name),origin=[part.origin[0],24-part.origin[1],part.origin[2]];
    model.groups.push({name:part.name,uuid:id,origin,rotation:[0,0,0],export:true,visibility:true});
    const group={uuid:id,children:[]};model.outliner.push(group);
    for(const cube of part.cubes) {
      const from=[origin[0]+cube.from[0],origin[1]-cube.from[1]-cube.size[1],origin[2]+cube.from[2]],to=from.map((v,i)=>v+cube.size[i]);
      const cubeId=uuid(family+'/'+part.name+'/'+cube.slot+'/'+cube.name);group.children.push(cubeId);
      model.elements.push({name:cube.slot+'_'+cube.name,uuid:cubeId,type:'cube',from,to,origin,rotation:[0,0,0],box_uv:true,uv_offset:[cube.material%4*64,Math.floor(cube.material/4)*64],faces:Object.fromEntries(['north','south','east','west','up','down'].map(f=>[f,{uv:[0,0,0,0],texture:0}])),visibility:true,autouv:0});
    }
  }
  model.textures.push({name:family+'.png',uuid:uuid(family+'/texture'),id:'0',width:256,height:128,uv_width:256,uv_height:128,mode:'bitmap',source:'data:image/png;base64,'+texture.toString('base64')});
  write(path.join(root,`art/blockbench/${family}_armor/${family}.bbmodel`),model);
  for(const slot of ['helmet','chestplate','leggings','boots']) {
    const id=family+'_'+slot,selected=cubes.filter(c=>c.slot===slot);
    const bounds=[0,1,2].map(axis=>[Math.min(...selected.map(c=>c.from[axis]+c.origin[axis])),Math.max(...selected.map(c=>c.from[axis]+c.origin[axis]+c.size[axis]))]);
    const scale=14/Math.max(...bounds.map(([a,b])=>b-a));
    const elements=selected.map(c=>{
      const from=c.from.map((v,i)=>(v+c.origin[i]-(bounds[i][0]+bounds[i][1])/2)*scale+8),to=from.map((v,i)=>v+c.size[i]*scale);
      // Java item models use Y up while wearable models use Y down.
      [from[1],to[1]]=[16-to[1],16-from[1]];
      const u=(c.material%4)*4,v=Math.floor(c.material/4)*8;
      return {from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(face=>[face,{texture:'#armor',uv:[u,v,u+2,v+4]}]))};
    });
    resource(`assets/darkspawn/models/item/${id}.json`,{textures:{armor:`darkspawn:equipment/${family}`,particle:`darkspawn:equipment/${family}`},elements,display:{gui:{rotation:[15,-25,0],translation:[0,0,0],scale:[.85,.85,.85]},ground:{rotation:[0,0,0],translation:[0,3,0],scale:[.4,.4,.4]},fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[.7,.7,.7]},thirdperson_righthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[.4,.4,.4]},firstperson_righthand:{rotation:[0,-35,0],translation:[0,0,0],scale:[.65,.65,.65]}}});
    if(config.style){
      // Inventory Trim Preview: Colored borders show material while wearable rendering retains the actual pattern.
      const overlay=elements.filter((e,i)=>selected[i].trim_uv).map(e=>({...e,from:e.from.map(n=>n-.015),to:e.to.map(n=>n+.015),faces:Object.fromEntries(Object.keys(e.faces).map(f=>[f,{texture:'#trim',uv:[0,0,16,16],tintindex:0}]))}));
      resource(`assets/darkspawn/models/item/${id}_trim.json`,{parent:`darkspawn:item/${id}`,textures:{trim:'darkspawn:equipment/style_trim_border'},elements:[...elements,...overlay]});
      if(!config.legendary)continue;
    }
    resource(`assets/darkspawn/items/${id}.json`,{model:{type:'minecraft:model',model:`darkspawn:item/${id}`}});
    if(config.legendary) {
      resource(`data/darkspawn/recipe/${id}.json`,{type:'minecraft:smithing_transform',template:'minecraft:netherite_upgrade_smithing_template',base:'minecraft:netherite_'+slot,addition:'darkspawn:'+family+'_core',result:{id:'darkspawn:'+id}});
      resource(`data/darkspawn/advancement/recipes/${id}.json`,{parent:'minecraft:recipes/root',criteria:{has_core:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'darkspawn:'+family+'_core'}]}}},requirements:[['has_core']],rewards:{recipes:['darkspawn:'+id]}});
      continue;
    }
    const material=config.addition || (mountain?'#minecraft:wool':slot==='helmet'?'minecraft:glowstone_dust':slot==='boots'?'minecraft:slime_ball':'minecraft:copper_ingot');
    // Smithing Conversion: Native transforms retain the base item's name, enchantments, damage, and boss-power component.
    resource(`data/darkspawn/recipe/${id}.json`,{type:'minecraft:smithing_transform',template:'minecraft:leather',base:'minecraft:iron_'+slot,addition:material,result:{id:'darkspawn:'+id}});
    resource(`data/darkspawn/advancement/recipes/${id}.json`,{parent:'minecraft:recipes/root',criteria:{has_ingredient:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'minecraft:iron_'+slot}]}},has_the_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipes:'darkspawn:'+id}}},requirements:[['has_ingredient','has_the_recipe']],rewards:{recipes:['darkspawn:'+id]}});
  }
  all.push({family,geometry,pixels,cubes,style:config.style});
}
// Style Selection: Vanilla items keep their identity and choose inventory geometry from a dedicated component.
const trimColors={quartz:0xe7e2d5,iron:0xb7bdc0,netherite:0x514955,redstone:0xb92721,copper:0xb96d4a,gold:0xeac449,emerald:0x2dcc7a,diamond:0x49cfce,lapis:0x345bc2,amethyst:0x9b72cc,resin:0xea8231};
for(const family of Object.keys(legendaryBosses))for(const slot of ['helmet','chestplate','leggings','boots'])resource(`assets/darkspawn/items/${family}_${slot}.json`,{model:{type:'minecraft:select',property:'minecraft:trim_material',cases:Object.entries(trimColors).map(([trim,color])=>({when:'minecraft:'+trim,model:{type:'minecraft:model',model:`darkspawn:item/${family}_${slot}_trim`,tints:[{type:'minecraft:constant',value:trim==='netherite'?0x38333b:color}]}})),fallback:{type:'minecraft:model',model:`darkspawn:item/${family}_${slot}`}}});
function itemStyle(metal,style,slot){return{type:'minecraft:select',property:'minecraft:trim_material',cases:Object.entries(trimColors).map(([trim,color])=>({when:'minecraft:'+trim,model:{type:'minecraft:model',model:`darkspawn:item/${metal}_${style}_${slot}_trim`,tints:[{type:'minecraft:constant',value:trim===metal?(metal==='iron'?0x737a80:0x21858d):color}]}})),fallback:{type:'minecraft:model',model:`darkspawn:item/${metal}_${style}_${slot}`}};}
for(const metal of ['iron','diamond'])for(const slot of ['helmet','chestplate','leggings','boots'])resource(`assets/minecraft/items/${metal}_${slot}.json`,{model:{type:'minecraft:select',property:'minecraft:component',component:'darkspawn:armor_style',cases:armorStyles.filter(s=>s!=='classic').map(style=>({when:style,model:itemStyle(metal,style,slot)})),fallback:itemStyle(metal,'classic',slot)}});
const trimBorder=Buffer.alloc(16*16*4);for(let y=0;y<16;y++)for(let x=0;x<16;x++)if(y<2||y>=14){const i=(y*16+x)*4;trimBorder.fill(255,i,i+4);}
fs.writeFileSync(path.join(res,'assets/darkspawn/textures/item/style_trim_border.png'),png(16,16,trimBorder));
// Item Baking: Equipment sprites use the blocks atlas; their trim must use that same atlas on Minecraft 26.3.
resource('assets/minecraft/atlases/blocks.json',{sources:[{type:'minecraft:directory',source:'equipment',prefix:'equipment/'}]});
fs.writeFileSync(path.join(res,'assets/darkspawn/textures/equipment/style_trim_border.png'),png(16,16,trimBorder));
for(const retired of ['royal_guard','arbalist','brawler'])for(const folder of ['recipe','advancement/recipes']) {
  const retiredFile=path.join(res,'data/darkspawn',folder,'armor_style_'+retired+'.json');
  if(fs.existsSync(retiredFile))fs.unlinkSync(retiredFile);
}
const styleable=['iron','diamond'].flatMap(m=>['helmet','chestplate','leggings','boots'].map(s=>`minecraft:${m}_${s}`));
resource('data/darkspawn/tags/item/styleable_armor.json',{replace:false,values:styleable});
for(const [style,addition] of Object.entries({classic:'iron_nugget',viking:'bone',spartan:'feather',war_chief:'gold_ingot',knight:'iron_ingot',berserker:'rabbit_hide',samurai:'bamboo',juggernaut:'obsidian',pathfinder:'string',reaver:'flint',pirate:'nautilus_shell',conquistador:'copper_ingot',ranger:'leather',paladin:'amethyst_shard',shadowblade:'ink_sac'})){
  resource(`data/darkspawn/recipe/armor_style_${style}.json`,{type:'darkspawn:armor_style',template:'minecraft:paper',base:'#darkspawn:styleable_armor',addition:'minecraft:'+addition,style});
  resource(`data/darkspawn/advancement/recipes/armor_style_${style}.json`,{parent:'minecraft:recipes/root',criteria:{has_armor:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'#darkspawn:styleable_armor'}]}},has_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipes:`darkspawn:armor_style_${style}`}}},requirements:[['has_armor','has_recipe']],rewards:{recipes:[`darkspawn:armor_style_${style}`]}});
}
for(const metal of ['iron','diamond','netherite'])resource(`assets/darkspawn/equipment/${metal}_style_overlay.json`,{layers:{humanoid:[{texture:'darkspawn:style_transparent'}],humanoid_leggings:[{texture:'darkspawn:style_transparent'}]},trim_overrides:[{palette:`minecraft:trim/${metal}_darker`,when:{material:`minecraft:${metal}`}}]});
for(const layer of ['humanoid','humanoid_leggings']){const file=path.join(res,`assets/darkspawn/textures/entity/equipment/${layer}/style_transparent.png`);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,png(64,32,Buffer.alloc(64*32*4)));}
// Enchanting: Add to functional enchantment tags without advertising unsupported cosmetic trim mapping.
function addTag(tag,values){const file=`data/minecraft/tags/item/enchantable/${tag}.json`,full=path.join(res,file),old=fs.existsSync(full)?JSON.parse(fs.readFileSync(full,'utf8')):{replace:false,values:[]};old.values=[...new Set([...old.values,...values])];resource(file,old);}
for(const [tag,slot] of Object.entries({'head_armor':'helmet','chest_armor':'chestplate','leg_armor':'leggings','foot_armor':'boots'})) addTag(tag,Object.keys(families).filter(f=>!families[f].style || families[f].legendary).map(f=>'darkspawn:'+f+'_'+slot));
for(const tag of ['durability','equippable']) addTag(tag,Object.keys(families).filter(f=>!families[f].style || families[f].legendary).flatMap(f=>['helmet','chestplate','leggings','boots'].map(s=>'darkspawn:'+f+'_'+s)));

for(const [name,bosses] of Object.entries(legendaryBosses)) {
  resource('data/darkspawn/recipe/'+name+'_core.json',{type:'minecraft:crafting_shapeless',category:'equipment',ingredients:bosses.map(b=>'darkspawn:'+b+'_essence').concat('minecraft:netherite_ingot'),result:{id:'darkspawn:'+name+'_core',count:1}});
  resource('data/darkspawn/advancement/recipes/'+name+'_core.json',{parent:'minecraft:recipes/root',criteria:{has_essence:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'darkspawn:'+bosses[0]+'_essence'}]}}},requirements:[['has_essence']],rewards:{recipes:['darkspawn:'+name+'_core']}});
  const gem=Buffer.alloc(32*32*4),palette=legendaryPalettes[name];
  for(let y=3;y<29;y++)for(let x=3;x<29;x++)if(Math.abs(x-15.5)+Math.abs(y-15.5)<14){const i=(y*32+x)*4,c=palette[Math.abs(x-15.5)+Math.abs(y-15.5)>11?4:x<15?5:2];gem[i]=c[0];gem[i+1]=c[1];gem[i+2]=c[2];gem[i+3]=255;}
  fs.writeFileSync(path.join(res,'assets/darkspawn/textures/item/'+name+'_core.png'),png(32,32,gem));
  resource('assets/darkspawn/models/item/'+name+'_core.json',{parent:'minecraft:item/generated',textures:{layer0:'darkspawn:item/'+name+'_core'}});
  resource('assets/darkspawn/items/'+name+'_core.json',{model:{type:'minecraft:model',model:'darkspawn:item/'+name+'_core'}});
}
resource('data/darkspawn/tags/entity_type/legendary_bosses.json',{replace:false,values:['minecraft:ender_dragon','minecraft:wither',...['ancient_tree_spirit','mutant_wolf','mutant_zombie','titan_boa','kraken','cave_crawler','mycelial_sovereign',...new Set(Object.values(legendaryBosses).flat())].map(n=>'darkspawn:'+n)]});
const translations={
  'tooltip.darkspawn.armor_style':'Style: %s',
  'tooltip.darkspawn.style_bonus.classic':'Full matching set: 20% less armor wear. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.viking':'Full matching set: +10% melee axe damage. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.spartan':'Full matching set: +10% melee spear damage. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.war_chief':'Full matching set: +1 melee attack damage. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.knight':'Full matching set: +2 armor toughness. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.berserker':'Full matching set: +10% attack speed. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.samurai':'Full matching set: +10% melee sword damage. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.royal_guard':'Full matching set: +1 heart maximum health. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.juggernaut':'Full matching set: +15 percentage points knockback resistance. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.pathfinder':'Full matching set: +5% movement speed. Iron and diamond may mix.',
  'armor_style.darkspawn.royal_guard':'Royal Guard',
  'armor_style.darkspawn.juggernaut':'Juggernaut',
  'armor_style.darkspawn.pathfinder':'Pathfinder',
  'tooltip.darkspawn.style_bonus.reaver':'Full matching set: +10% melee mace damage. Iron and diamond may mix.',
  'tooltip.darkspawn.style_bonus.pirate':'Full matching set: +10% melee and thrown trident damage. Must be worn when the hit lands.',
  'tooltip.darkspawn.style_bonus.conquistador':'Full matching set: +10% pickaxe mining speed. Iron and diamond may mix.',
  'armor_style.darkspawn.reaver':'Reaver',
  'armor_style.darkspawn.pirate':'Pirate',
  'armor_style.darkspawn.conquistador':'Conquistador',
  'tooltip.darkspawn.style_bonus.ranger':'Full matching set: +10% bow and crossbow damage, including crossbow rockets. Must be worn at impact.',
  'tooltip.darkspawn.style_bonus.arbalist':'Full matching set: +10% crossbow arrow and rocket damage. Must be worn when the hit lands.',
  'tooltip.darkspawn.style_bonus.brawler':'Full matching set: +1 unarmed attack damage with an empty main hand. Iron and diamond may mix.',
  'armor_style.darkspawn.ranger':'Ranger',
  'armor_style.darkspawn.arbalist':'Arbalist',
  'armor_style.darkspawn.brawler':'Brawler',
  'tooltip.darkspawn.style_bonus.paladin':'Full matching set: +10% healing received. Does not raise maximum health.',
  'tooltip.darkspawn.style_bonus.shadowblade':'Full matching set: 2 seconds of invisibility after damage. 15-second cooldown. Shadowblade armor also disappears while invisible.',
  'armor_style.darkspawn.paladin':'Paladin',
  'armor_style.darkspawn.shadowblade':'Shadowblade',
  'armor_style.darkspawn.classic':'Classic',
  'armor_style.darkspawn.viking':'Viking',
  'armor_style.darkspawn.spartan':'Spartan',
  'armor_style.darkspawn.war_chief':'War Chief',
  'armor_style.darkspawn.knight':'Knight',
  'armor_style.darkspawn.berserker':'Berserker',
  'armor_style.darkspawn.samurai':'Samurai',
  'tooltip.darkspawn.diver_helmet':'Diving helmet: +2 oxygen bonus, 2.5x underwater mining speed, and underwater night vision. Air remains limited.',
  'tooltip.darkspawn.desert_wanderer_armor':'Each worn piece provides 20 heat protection. Full suit: 80.',
  'tooltip.darkspawn.ashwalker_armor':'Each piece: 25 Nether heat protection (15 elsewhere), 10% less fire damage. No lava immunity.',
  'tooltip.darkspawn.mountaineering_armor':'Each piece: 20 cold protection, +5 heat in hot areas. Full set prevents powder-snow freezing; boots walk on snow.',
  'tooltip.darkspawn.spelunking_helmet':'Headlamp: personal night vision while worn. Does not light blocks or prevent mob spawning.',
  'tooltip.darkspawn.spelunking_boots':'Padded boots: 25% less remaining fall damage. Combines with meals and enchantments.'
};
for(const name of Object.keys(legendaryBosses)) {
 translations['item.darkspawn.'+name+'_core']=name.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ')+' Core';
}
Object.assign(translations,{
 'tooltip.darkspawn.legendary.dragonlord':'Full set: +25% damage against bosses and creatures with at least 40 maximum health.',
 'tooltip.darkspawn.legendary.phoenix':'Full set: survive a fatal hit at 1 heart with Regeneration II for 5 seconds. 10-minute cooldown.',
 'tooltip.darkspawn.legendary.voidwalker':'Full set: no ender pearl damage and 40% less fall damage.',
 'tooltip.darkspawn.legendary.titan':'Full set: +2 hearts maximum health and +40 percentage points knockback resistance.',
 'tooltip.darkspawn.legendary.soul_reaper':'Full set: restore 1 heart per hostile enemy kill. 5-second cooldown.'
});
for(const family of Object.keys(families).filter(f=>!families[f].style || families[f].legendary))for(const slot of ['helmet','chestplate','leggings','boots'])translations[`item.darkspawn.${family}_${slot}`]=`${family.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ')} ${slot[0].toUpperCase()+slot.slice(1)}`;
const lang=path.join(res,'assets/darkspawn/lang/en_us.json');let text=fs.readFileSync(lang).toString('latin1');
for(const [key,value] of Object.entries(translations)) {
  text=text.replace(new RegExp('('+JSON.stringify(key).replace(/\./g,'\\.')+'\\s*:\\s*)"(?:[^"\\\\]|\\\\.)*"'),(_,prefix)=>prefix+JSON.stringify(value));
}
fs.writeFileSync(lang,Buffer.from(text,'latin1'));
const missing=Object.entries(translations).filter(([key])=>!text.includes(JSON.stringify(key)));
if(missing.length){const end=text.lastIndexOf('}');text=text.slice(0,end).trimEnd()+',\n'+missing.map(([k,v])=>'\t'+JSON.stringify(k)+': '+JSON.stringify(v)).join(',\n')+'\n}'+text.slice(end+1);fs.writeFileSync(lang,Buffer.from(text,'latin1'));}

// Offline Preview: Rasterize the same cuboids and atlas for review; this is not a Minecraft screenshot.
function renderPreview(all, filename) {
const width=1000,height=440*Math.ceil(all.length/2),preview=Buffer.alloc(width*height*4),depth=new Float64Array(width*height).fill(-Infinity);
for(let i=0;i<preview.length;i+=4){preview[i]=23;preview[i+1]=30;preview[i+2]=37;preview[i+3]=255;}
function triangle(points,uv,shade,atlas) {
  const [a,b,c]=points,den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<.00001)return;
  for(let y=Math.max(0,Math.floor(Math.min(...points.map(p=>p[1]))));y<=Math.min(height-1,Math.ceil(Math.max(...points.map(p=>p[1]))));y++)for(let x=Math.max(0,Math.floor(Math.min(...points.map(p=>p[0]))));x<=Math.min(width-1,Math.ceil(Math.max(...points.map(p=>p[0]))));x++) {
    const w=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,t=1-w-v;if(w<0||v<0||t<0)continue;
    const d=w*a[2]+v*b[2]+t*c[2],i=y*width+x;if(d<depth[i])continue;depth[i]=d;
    const u=Math.max(0,Math.min(255,Math.floor(w*uv[0][0]+v*uv[1][0]+t*uv[2][0]))),q=Math.max(0,Math.min(127,Math.floor(w*uv[0][1]+v*uv[1][1]+t*uv[2][1]))),j=(q*256+u)*4;
    for(let k=0;k<3;k++)preview[i*4+k]=atlas[j+k]*shade;
  }
}
for(let family=0;family<all.length;family++)for(let view=0;view<2;view++) {
  const {cubes,pixels}=all[family],yaw=(view===0?-22:145)*Math.PI/180,pitch=.12,center=125+((family%2)*2+view)*250;
  const project=([x,y,z])=>{const X=x*Math.cos(yaw)+z*Math.sin(yaw),Z=-x*Math.sin(yaw)+z*Math.cos(yaw);return[center+X*10.5,140+Math.floor(family/2)*440+y*10.5+Z*pitch*10.5,-Z+y*pitch];};
  for(const c of cubes){const [a,b,d]=c.from.map((v,i)=>v+c.origin[i]),[e,f,g]=c.size.map((v,i)=>v+c.from[i]+c.origin[i]),u=c.material%4*64,v=Math.floor(c.material/4)*64;
    for(const [points,shade] of [ [[[a,b,d],[e,b,d],[e,f,d],[a,f,d]],.95], [[[e,b,g],[a,b,g],[a,f,g],[e,f,g]],.8], [[[a,b,g],[a,b,d],[a,f,d],[a,f,g]],.75], [[[e,b,d],[e,b,g],[e,f,g],[e,f,d]],.82], [[[a,b,g],[e,b,g],[e,b,d],[a,b,d]],1], [[[a,f,d],[e,f,d],[e,f,g],[a,f,g]],.65] ]) {
      const p=points.map(project),uv=[[u,v],[u+20,v],[u+20,v+20],[u,v+20]];
      for(const ids of [[0,1,2],[0,2,3]])triangle(ids.map(i=>p[i]),ids.map(i=>uv[i]),shade,pixels);
    }
  }
}
const previewPath=path.join(root,filename);fs.mkdirSync(path.dirname(previewPath),{recursive:true});fs.writeFileSync(previewPath,png(width,height,preview));
}
renderPreview(all.filter(f=>!f.style),'art/expedition-armor/preview.png');
renderPreview(['classic','viking','spartan'].flatMap(s=>all.filter(f=>f.style===s)),'art/armor-styles/preview.png');
renderPreview(['war_chief','knight','berserker','samurai'].flatMap(s=>all.filter(f=>f.style===s)),'art/armor-styles/commanders-preview.png');
console.log(`Generated expedition and cosmetic armor geometry, recipes, item models, and previews for ${all.length} families.`);

renderPreview(['paladin','juggernaut','pathfinder'].flatMap(s=>all.filter(f=>f.style===s)),'art/armor-styles/utility-preview.png');

renderPreview(['reaver','pirate','conquistador'].flatMap(s=>all.filter(f=>f.style===s)),'art/armor-styles/specialists-preview.png');

renderPreview(['paladin','shadowblade','ranger'].flatMap(s=>all.filter(f=>f.style===s)),'art/armor-styles/final-roster-preview.png');

renderPreview(all.filter(f=>families[f.family].legendary),'art/armor-styles/legendary-preview.png');
