// Pirate Seas: Reproducible native structures, finite loot, gate models and world data.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const root = path.resolve(__dirname, '../..');
const res = path.join(root, 'src/main/resources');
const expansionOnly = process.argv.includes('--expansion-only');
const terrainOnly = process.argv.includes('--terrain-only');
function json(file, value) { if(expansionOnly && !/chests\/pirate_(redwater|pearlwater|cinderjaw)\.json$/.test(file))return; const target = path.join(res,file); fs.mkdirSync(path.dirname(target),{recursive:true}); fs.writeFileSync(target,JSON.stringify(value,null,2)+'\n'); }
function integer(n) { const b=Buffer.alloc(4);b.writeInt32BE(n);return b; }
function string(s) { const b=Buffer.from(s),n=Buffer.alloc(2);n.writeUInt16BE(b.length);return Buffer.concat([n,b]); }
function tag(v) {
  if(typeof v==='number')return [3,integer(v)];
  if(typeof v==='string')return [8,string(v)];
  if(Array.isArray(v)){const a=v.map(tag);return [9,Buffer.concat([Buffer.from([a[0]?.[0]||10]),integer(a.length),...a.map(e=>e[1])])];}
  return [10,Buffer.concat([...Object.entries(v).map(([k,v])=>{const [t,b]=tag(v);return Buffer.concat([Buffer.from([t]),string(k),b]);}),Buffer.from([0])])];
}
if (process.argv.includes('--biomes-only')) {
  require('./pirate-biome-islands.cjs')({fs,path,zlib,res,tag});
  process.exit(0);
}
if (terrainOnly) {
  // Island Hinterland: Keep the original 49-block quest core and its saved objective coordinates intact.
  for (let variant=0;variant<9;variant++) {
    const palette=[],indices=new Map(),blocks=[];
    function put(x,y,z,name) {
      const state={Name:'minecraft:'+name,...(name.endsWith('_leaves')?{Properties:{persistent:'true',distance:'1',waterlogged:'false'}}:{})};
      const key=JSON.stringify(state);if(!indices.has(key)){indices.set(key,palette.length);palette.push(state);}
      blocks.push({pos:[x,y,z],state:indices.get(key)});
    }
    for(let x=0;x<113;x++)for(let z=0;z<113;z++) {
      const dx=x-56,dz=z-56,r=Math.hypot(dx,dz),angle=Math.atan2(dz,dx);
      const edge=49+Math.sin(angle*3+variant)*4+Math.cos(angle*5-variant)*2;
      const mineBank=dx>=25&&dx<=45&&Math.abs(dz)<=5;
      if(r>edge&&!mineBank)continue;
      // Preserve quest buildings and a southern sailing approach; blend the old beach into the hinterland.
      if(r<=16 || dz>=17&&Math.abs(dx)<=6)continue;
      const core=Math.abs(dx)<=24&&Math.abs(dz)<=24;
      const shore=Math.max(0,Math.min(1,(edge-r)/9));
      const hill=Math.max(0,Math.sin(dx*.105+variant)*Math.cos(dz*.09-variant))*12;
      let top=Math.floor(18+shore*(9+hill));
      if(mineBank)top=Math.max(top,26);
      if(core)top=24;
      // Approach: A level east-west shelf connects the old shore to the new interior.
      if(Math.abs(dz)<=3&&Math.abs(dx)<edge-6)top=24;
      for(let y=0;y<=top;y++) {
        let material=y===top?(top<=24?'sand':variant===8?'gravel':'grass_block'):y>=top-3?(top<=24?'sandstone':'dirt'):'stone';
        // Native Deposits: Clustered veins, finite and mineable with normal tool/drop rules.
        const vein=Math.abs(Math.imul(Math.floor(x/3)+variant*71,7349)^Math.imul(Math.floor(y/3),193)^Math.imul(Math.floor(z/3),9151))%113;
        if(y<top-4) {
          if(vein<5)material='coal_ore';else if(vein<9)material='iron_ore';else if(vein<12)material='copper_ore';
          else if(y<12&&vein===12)material='gold_ore';else if(y<10&&vein===13)material='redstone_ore';
          else if(y<10&&vein===14)material='lapis_ore';
          else if(vein>100)material=variant===8?'tuff':'andesite';
        }
        // Dry cave galleries stay above the seabed and inside a sealed rock shell.
        const gallery=Math.abs(r-(34+Math.sin(angle*3+variant)*2))<2.5 && !(dz>15&&Math.abs(dx)<11);
        if(gallery&&y>=11&&y<=14&&y<top-3)material='air';
        if(dx>=32&&dx<=42&&Math.abs(dz)<=3&&y>=11&&y<=14&&y<top-3)material='air';
        // Walkable mine entrance: Three-wide staircase from the east shelf into the gallery.
        if(dx>=27&&dx<=41&&Math.abs(dz)<=1) {
          const floor=Math.max(8,24-(dx-27));
          if(y>floor&&y<=Math.max(floor+3,top))material='air';
          if(y===floor)material='stone';
        }
        put(x,y,z,material);
      }
      if(!core&&top>25&&Math.abs(dz)>5&&((x*17+z*31+variant*13)%97===0)) {
        for(let y=1;y<=4;y++)put(x,top+y,z,variant===8?'basalt':'jungle_log');
        if(variant!==8)for(let ox=-2;ox<=2;ox++)for(let oz=-2;oz<=2;oz++)if(Math.abs(ox)+Math.abs(oz)<=3)put(x+ox,top+5,z+oz,'jungle_leaves');
      }
    }
    for(let x=73;x<=83;x++)for(let z=54;z<=58;z++)put(x,24,z,'oak_planks');
    // Rare Resource Budget: One four-block deep deposit on selected variants, not diamond-rich foundations.
    if(variant!==1&&variant!==6)for(let x=0;x<2;x++)for(let z=0;z<2;z++)put(89+variant%3+x,3,52+variant%4+z,'diamond_ore');
    // Foliage can overlap adjacent columns; retain the last authored state per position.
    const unique=new Map(blocks.map(block=>[block.pos.join(','),block]));
    const template={DataVersion:5000,size:[113,56,113],palette,blocks:[...unique.values()],entities:[]};
    fs.writeFileSync(path.join(res,`data/darkspawn/structure/pirate/terrain_${variant}.nbt`),zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),tag(template)[1]])));
  }
  console.log('Generated nine island hinterlands; existing landmarks and world settings preserved.');
  process.exit(0);
}
json('data/darkspawn/dimension/pirate_seas.json',{type:'minecraft:overworld',generator:{type:'minecraft:flat',settings:{biome:'darkspawn:pirate_seas',features:false,lakes:false,structure_overrides:['darkspawn:pirate_islands'],layers:[{block:'minecraft:bedrock',height:1},{block:'minecraft:stone',height:99},{block:'minecraft:sand',height:4},{block:'minecraft:water',height:24}]}}});
json('data/darkspawn/worldgen/biome/pirate_seas.json',{attributes:{'minecraft:visual/sky_color':'#77b9eb'},carvers:[],downfall:0.7,effects:{water_color:'#278eaa'},features:Array.from({length:11},()=>[]),has_precipitation:true,temperature:0.9,spawners:{water_creature:[{type:'minecraft:squid',weight:4,minCount:1,maxCount:3},{type:'minecraft:dolphin',weight:1,minCount:1,maxCount:2}],water_ambient:[{type:'minecraft:tropical_fish',weight:10,minCount:3,maxCount:6}],monster:[{type:'minecraft:drowned',weight:5,minCount:1,maxCount:2}]},spawn_costs:{}});
json('data/darkspawn/worldgen/structure/pirate_island.json',{type:'darkspawn:pirate_island',biomes:['darkspawn:pirate_seas'],spawn_overrides:{},step:'top_layer_modification',terrain_adaptation:'none'});
json('data/darkspawn/worldgen/structure_set/pirate_islands.json',{structures:[{structure:'darkspawn:pirate_island',weight:1}],placement:{type:'minecraft:random_spread',spacing:16,separation:8,salt:14102671}});
json('data/darkspawn/recipe/sea_gate.json',{type:'minecraft:crafting_shaped',category:'misc',pattern:['CPC','IKI','CPC'],key:{C:'minecraft:copper_ingot',P:'minecraft:prismarine_shard',I:'minecraft:iron_ingot',K:'minecraft:compass'},result:{id:'darkspawn:sea_gate',count:1}});
json('data/darkspawn/advancement/recipes/sea_gate.json',{parent:'minecraft:recipes/root',criteria:{has_compass:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'minecraft:compass'}]}}},requirements:[['has_compass']],rewards:{recipes:['darkspawn:sea_gate']}});
function cube(from,to,texture){return {from,to,faces:Object.fromEntries(['up','down','north','south','east','west'].map(f=>[f,{texture:'#'+texture}]))};}
for(const name of ['sea_gate','harbor_return']) {
  json(`assets/darkspawn/models/block/${name}.json`,{parent:'minecraft:block/block',textures:{particle:'minecraft:block/cut_copper',copper:'minecraft:block/cut_copper',wood:'minecraft:block/dark_oak_planks',light:'minecraft:block/sea_lantern',rim:'minecraft:block/prismarine_bricks'},elements:[cube([0,0,0],[16,3,16],'wood'),cube([2,3,2],[14,5,14],'copper'),cube([3,5,3],[5,16,5],'rim'),cube([11,5,3],[13,16,5],'rim'),cube([3,14,3],[13,16,5],'copper'),cube([5,6,3.3],[11,14,4.7],'light'),cube([4,5,7],[12,7,13],'copper'),cube([6,7,8],[10,8,12],'light')]});
  json(`assets/darkspawn/blockstates/${name}.json`,{variants:{'':{model:`darkspawn:block/${name}`}}});
}
json('assets/darkspawn/items/sea_gate.json',{model:{type:'minecraft:model',model:'darkspawn:block/sea_gate'}});
json('data/darkspawn/loot_table/blocks/sea_gate.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'darkspawn:sea_gate'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
function entry(name,count=1){return {type:'minecraft:item',name:name.includes(':')?name:'minecraft:'+name,modifier:{type:'minecraft:set_count',count}};}
function loot(name,entries){json(`data/darkspawn/loot_table/chests/pirate_${name}.json`,{type:'minecraft:chest',pools:entries.map(e=>({rolls:1,entries:[e]}))});}
loot('harbor',[entry('oak_boat',3),entry('bread',12),entry('torch',16),entry('oak_sapling',4),entry('fishing_rod'),entry('compass')]);
loot('cove',[entry('iron_nugget',{type:'minecraft:uniform',min:8,max:18}),entry('bread',4),entry('leather',3),entry('prismarine_shard',2)]);
loot('fort',[entry('gold_ingot',{type:'minecraft:uniform',min:2,max:5}),entry('arrow',16),entry('darkspawn:solid_shot',12),entry('emerald',2),entry('iron_ingot',4)]);
loot('wreck',[entry('iron_ingot',{type:'minecraft:uniform',min:3,max:6}),entry('prismarine_shard',3),entry('gold_nugget',12),entry('paper',6),entry('darkspawn:solid_shot',8),entry('oak_planks',24)]);

loot('beacon',[entry('copper_ingot',8),entry('coal',8),entry('spyglass'),entry('paper',4)]);
loot('shrine',[entry('prismarine_shard',8),entry('prismarine_crystals',4),entry('gold_ingot',3)]);
loot('mangrove',[entry('mangrove_propagule',3),entry('slime_ball',6),entry('iron_ingot',3),entry('bread',6),entry('oak_planks',32)]);
loot('redwater',[entry('iron_ingot',4),entry('darkspawn:solid_shot',12),entry('darkspawn:pirate_beer',2),entry('bread',6)]);
loot('pearlwater',[entry('prismarine_crystals',5),entry('nautilus_shell',1),entry('sea_pickle',4),entry('emerald',2)]);
loot('cinderjaw',[entry('gold_ingot',3),entry('darkspawn:solid_shot',16),entry('darkspawn:pirate_rum',2),entry('coal',12)]);
const previews=[];
for(const variant of ['harbor',0,1,2,3,4,5,6,7,8]) {
  if(expansionOnly && (typeof variant !== 'number' || variant < 6))continue;
  const harbor=variant==='harbor',blocks=new Map();
  function put(x,y,z,name,props,nbt){blocks.set(`${x},${y},${z}`,{pos:[x,y,z],state:{Name:name.includes(':')?name:name.includes(':')?name:'minecraft:'+name,...(props?{Properties:props}:{})},...(nbt?{nbt}:{})});}
  function fill(x1,y1,z1,x2,y2,z2,name){for(let x=x1;x<=x2;x++)for(let y=y1;y<=y2;y++)for(let z=z1;z<=z2;z++)put(x,y,z,name);}
  function chest(x,y,z,table){put(x,y,z,'chest',{facing:'south',type:'single',waterlogged:'false'},{id:'minecraft:chest',LootTable:'darkspawn:chests/pirate_'+table});}
  function sign(x,y,z,lines){put(x,y,z,'oak_sign',{rotation:'0',waterlogged:'false'},{id:'minecraft:sign',front_text:{messages:lines.map(text=>({text})),color:'black'},back_text:{messages:lines.map(text=>({text})),color:'black'}});}
  function palm(x,z,ground){
    fill(x,ground+1,z,x,ground+6,z,'jungle_log');
    for(let d=-4;d<=4;d++)for(let w=-1;w<=1;w++)if(Math.abs(d)+Math.abs(w)<=4){put(x+d,ground+7-(Math.abs(d)>2?1:0),z+w,'jungle_leaves',{persistent:'true',distance:'1',waterlogged:'false'});put(x+w,ground+7-(Math.abs(d)>2?1:0),z+d,'jungle_leaves',{persistent:'true',distance:'1',waterlogged:'false'});}
  }
  // Solid seabed-to-shore foundations; open water between templates remains boat navigable.
  for(let x=0;x<49;x++)for(let z=0;z<49;z++){
    const dx=x-24,dz=z-24,r=Math.sqrt(dx*dx+dz*dz);
    const edge=23+Math.sin(x*.45)*Math.cos(z*.3)*1.5;
    if(r>edge)continue;
    const top=r<17?24:Math.max(0,Math.floor(24-(r-17)*3.4));
    fill(x,0,z,x,top,z,r<16?'stone':'sandstone');
    put(x,top,z,r<15&&!harbor?'grass_block':'sand');
    if(top>=23)fill(x,top+1,z,x,34,z,'air');
  }
  if(harbor){
    // Broad marked arrival plaza and walkable dock ending at water level.
    fill(18,24,17,30,24,31,'stone_bricks');fill(22,24,29,26,24,48,'spruce_planks');
    for(const x of [21,27])for(const z of [32,39,47]){fill(x,20,z,x,26,z,'stripped_spruce_log');put(x,27,z,'lantern',{hanging:'false',waterlogged:'false'});}
    put(24,25,20,'darkspawn:harbor_return');
    sign(26,25,20,['HARBOR RETURN','Empty-hand use','Returns you home','Keep path clear']);
    sign(27,25,29,['PIRATE SEAS','First cove: EAST','About 190 blocks','Harbor: 0, 0']);
    // Open-front chandlery: crafting, renewable saplings, food and finite starter boats.
    fill(9,24,17,17,24,26,'spruce_planks');
    for(const x of [9,17])for(const z of [17,26])fill(x,25,z,x,29,z,'stripped_spruce_log');
    fill(9,25,17,17,27,17,'spruce_planks');fill(8,30,16,18,30,27,'dark_oak_slab');
    put(11,25,18,'crafting_table');put(13,25,18,'furnace');chest(15,25,18,'harbor');
    sign(15,25,24,['CHANDLERY','Shared supplies','Plant trees for','more boats']);
    // Lighthouse with ladder access, beacon and readable long-distance silhouette.
    for(let y=24;y<=37;y++)for(let x=30;x<=36;x++)for(let z=12;z<=18;z++)if(x===30||x===36||z===12||z===18)put(x,y,z,y%5===0?'prismarine_bricks':'calcite');
    fill(31,25,13,35,36,17,'air');fill(32,25,18,34,27,18,'air');
    for(let y=25;y<=37;y++)put(31,y,14,'ladder',{facing:'east',waterlogged:'false'});
    fill(30,38,12,36,38,18,'dark_oak_slab');fill(32,37,14,34,37,16,'sea_lantern');
    for(const x of [19,29])for(const z of [18,30]){put(x,25,z,'spruce_fence');put(x,26,z,'lantern',{hanging:'false',waterlogged:'false'});}
    palm(12,32,24);palm(35,29,24);
    // Breaker's Rest: New harbors gain a tavern and market without migrating over player builds.
    fill(15,24,6,27,24,14,'spruce_planks');
    for(const x of [15,27])for(const z of [6,14])fill(x,25,z,x,29,z,'stripped_spruce_log');
    fill(15,25,6,27,28,6,'spruce_planks');
    fill(15,25,7,15,28,13,'spruce_planks');fill(27,25,7,27,28,13,'spruce_planks');
    fill(14,30,5,28,30,15,'dark_oak_slab');
    fill(17,25,8,24,25,8,'barrel');
    for(const x of [18,24]){put(x,25,11,'oak_fence');put(x,26,11,'oak_pressure_plate');put(x,25,12,'spruce_stairs',{facing:'north',half:'bottom',shape:'straight',waterlogged:'false'});}
    put(21,29,9,'lantern',{hanging:'true',waterlogged:'false'});
    sign(20,25,14,['THE RUSTY ANCHOR','Rest your boots','Chart your voyage','Mind the tide']);
    fill(36,24,22,42,24,28,'spruce_planks');
    for(const x of [36,42])for(const z of [22,28])fill(x,25,z,x,28,z,'oak_fence');
    fill(35,29,21,43,29,29,'white_wool');fill(37,25,23,41,25,23,'barrel');
    sign(39,25,27,['HARBOR MARKET','Trade winds ahead','Briggs has work','Near the plaza']);
    // A protected return pedestal prevents the terminal losing its supporting floor.
    put(24,24,20,'bedrock');
  } else {
    palm(14,24,24);palm(31,33,24);palm(31,15,24);
    fill(22,24,34,25,24,48,'spruce_planks');
    for(const z of [37,43,48]){fill(21,20,z,21,25,z,'oak_log');fill(26,20,z,26,25,z,'oak_log');}
    if(variant===0){
      fill(19,24,19,28,24,28,'oak_planks');
      for(const x of [19,28])for(const z of [19,28])fill(x,25,z,x,28,z,'oak_log');
      fill(19,25,19,28,27,19,'oak_planks');fill(18,29,18,29,29,29,'dark_oak_slab');
      chest(25,25,20,'cove');put(21,25,20,'crafting_table');
      sign(24,25,31,['CASTAWAY COVE','Salvage ahead','Harbor lies WEST','Bring a boat']);
      put(22,25,26,'campfire',{lit:'true',waterlogged:'false',signal_fire:'false',facing:'north'});
    } else if(variant===1){
      // Broken coastal fort: open doorway, battlements and an accessible treasure alcove.
      fill(17,24,17,30,24,30,'stone_bricks');
      for(let x=17;x<=30;x++)for(let z=17;z<=30;z++)if(x===17||x===30||z===17||z===30)fill(x,25,z,x,27+(x+z)%3,z,(x+z)%3?'mossy_stone_bricks':'cracked_stone_bricks');
      fill(22,25,30,25,27,30,'air');chest(23,25,19,'fort');
      for(const x of [18,29]){fill(x,25,18,x,32,18,'stone_bricks');put(x,33,18,'lantern',{hanging:'false',waterlogged:'false'});}
      sign(26,25,32,['SALTWATCH FORT','IRON FLEET POST','Renegades within','Recover dispatches']);
      fill(18,31,19,18,35,19,'iron_bars');fill(19,33,19,21,35,19,'blue_wool');put(20,34,19,'light_gray_wool');
    } else if(variant===2) {
      // Beached split hull with mast, torn sail and cargo below the open deck.
      for(let x=18;x<=29;x++)for(let z=15;z<=33;z++){
        if((x===18||x===29)&&z>17&&z<31)fill(x,25,z,x,28,z,'dark_oak_planks');
        if(x>18&&x<29)put(x,24,z,'dark_oak_planks');
        if(z===16||z===32)fill(x,25,z,x,26,z,'dark_oak_planks');
      }
      fill(24,25,23,24,36,23,'spruce_log');fill(20,33,23,28,33,23,'spruce_log');
      for(let x=21;x<=27;x++)for(let y=29;y<=35;y++)if((x+y)%7!==0)put(x,y,24,'white_wool');
      fill(20,25,32,22,26,32,'air');chest(27,25,20,'wreck');
      sign(24,25,35,['THE PALE GULL','Log: lights below','Cargo at the bow','Stern entry open']);
    } else if(variant===3) {
      // Abandoned beacon with open keeper's room and a ladder to its signal platform.
      fill(19,24,18,29,24,29,'stone_bricks');
      for(let y=25;y<=35;y++)for(let x=20;x<=28;x++)for(let z=19;z<=27;z++)if(x===20||x===28||z===19||z===27)put(x,y,z,y%3?'calcite':'cracked_stone_bricks');
      fill(23,25,27,25,27,27,'air');fill(20,36,19,28,36,27,'dark_oak_slab');
      for(let y=25;y<=36;y++)put(21,y,20,'ladder',{facing:'east',waterlogged:'false'});
      fill(23,37,22,25,37,24,'sea_lantern');chest(24,25,20,'beacon');
      sign(24,25,31,['LASTLIGHT BEACON','Reply from below','Keeper fled','Climb to lantern']);
    } else if(variant===4) {
      // Open tidal shrine: broken columns surround a finite relic cache.
      fill(16,24,16,32,24,30,'prismarine_bricks');
      for(const x of [17,31])for(const z of [17,29]){fill(x,25,z,x,29+(x+z)%3,z,'dark_prismarine');put(x,30+(x+z)%3,z,'sea_lantern');}
      fill(20,25,17,28,25,17,'mossy_stone_bricks');chest(24,25,18,'shrine');
      for(const x of [20,24,28])put(x,25,23,'prismarine_slab');
      sign(24,25,32,['TIDEGLASS SHRINE','DROWNED VIGIL','Four sea shards','Offer at cache']);
      fill(32,25,18,32,31,18,'dark_prismarine');fill(29,30,18,31,31,18,'black_wool');put(30,31,18,'cyan_wool');
      put(32,32,18,'soul_lantern',{hanging:'false',waterlogged:'false'});
    } else if(variant===5) {
      // Mangrove salvage camp with roots, a raised hut and an accessible supply cache.
      for(const x of [11,36]){fill(x,24,20,x+2,26,22,'mangrove_roots');fill(x+1,27,21,x+1,32,21,'mangrove_log');fill(x-2,32,18,x+4,33,24,'mangrove_leaves');}
      fill(18,24,19,29,24,29,'mangrove_planks');
      for(const x of [18,29])for(const z of [19,29])fill(x,25,z,x,29,z,'mangrove_log');
      fill(18,25,19,29,27,19,'mangrove_planks');fill(17,30,18,30,30,30,'mud_brick_slab');
      chest(25,25,20,'mangrove');put(21,25,20,'crafting_table');
      sign(24,25,32,['ROOTBOUND CAMP','CORSAIR OUTPOST','Scavengers within','Manifest in cache']);
      fill(30,25,20,30,34,20,'crimson_stem');fill(27,32,20,29,34,20,'red_wool');put(28,33,20,'yellow_wool');put(30,35,20,'gold_block');
    } else if(variant===6) {
      // Redwater: opposing camps leave the central battlefield and southern dock open.
      fill(15,24,19,33,24,29,'coarse_dirt');
      for(const x of [13,35]) {
        fill(x,25,18,x,27,29,'stone_bricks');
        fill(x,28,18,x,34,18,'spruce_log');
        fill(x-1,32,19,x+1,34,19,x===13?'red_wool':'blue_wool');
      }
      fill(20,24,14,28,24,17,'spruce_planks');chest(24,25,15,'redwater');
      sign(24,25,33,['REDWATER LANDING','Corsairs vs Fleet','Watch or help','Attacks cost trust']);
    } else if(variant===7) {
      // Pearlwater: shallow lagoon and a bridge to an open pearl-diver's shelter.
      fill(18,22,18,29,22,29,'sand');fill(18,23,18,29,24,29,'air');fill(18,23,18,29,23,29,'water');
      fill(23,24,18,25,24,32,'birch_planks');fill(19,24,13,29,24,18,'birch_planks');
      for(const x of [19,29])for(const z of [13,18])fill(x,25,z,x,28,z,'stripped_birch_log');
      fill(18,29,12,30,29,19,'white_wool');chest(24,25,14,'pearlwater');
      for(const x of [20,27])put(x,23,24,'sea_lantern');
      sign(24,25,34,['PEARLWATER ATOLL','Pearl divers rest','Cache across bridge','Mind the lagoon']);
    } else if(variant===8) {
      // Cinderjaw: basalt stacks frame an open drydock and a finite smuggler cache.
      fill(17,24,18,30,24,30,'blackstone');
      for(const x of [12,35])for(const z of [18,27]){fill(x,24,z,x+2,33+(z%3),z+2,'basalt');fill(x,34+(z%3),z,x+2,34+(z%3),z+2,'blackstone');}
      for(const x of [19,28])fill(x,25,19,x,27,29,'dark_oak_planks');
      fill(20,25,19,27,26,19,'dark_oak_planks');chest(24,25,20,'cinderjaw');
      sign(24,25,32,['CINDERJAW ANCHORAGE','Smuggler drydock','Scavengers ashore','Rum and shot within']);
    }
  }
  const palette=[],indices=new Map(),entries=[];
  for(const b of blocks.values()){const key=JSON.stringify(b.state);if(!indices.has(key)){indices.set(key,palette.length);palette.push(b.state);}entries.push({pos:b.pos,state:indices.get(key),...(b.nbt?{nbt:b.nbt}:{})});}
  const template={DataVersion:5000,size:[49,40,49],palette,blocks:entries,entities:[]};
  const name=harbor?'harbor':`island_${variant}`;
  const target=path.join(res,`data/darkspawn/structure/pirate/${name}.nbt`);fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),tag(template)[1]])));
  previews.push({name,...template});
}
fs.mkdirSync(path.join(root,'build'),{recursive:true});fs.writeFileSync(path.join(root,'build/pirate-preview.json'),JSON.stringify(previews));
console.log(expansionOnly ? 'Pirate Seas: generated three expansion islands and their loot only.' : 'Pirate Seas: generated harbor, nine island discoveries, sea gate and dimension data.');
