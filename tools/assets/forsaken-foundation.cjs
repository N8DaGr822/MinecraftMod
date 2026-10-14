// Forsaken Foundation: Author only this increment's native models, data and bunker templates.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const root = path.resolve(__dirname, '../..');
const res = path.join(root, 'src/main/resources');
function json(file, value) { const target = path.join(res, file); fs.mkdirSync(path.dirname(target), {recursive: true}); fs.writeFileSync(target, JSON.stringify(value, null, 2) + '\n'); }
function integer(n) { const b = Buffer.alloc(4); b.writeInt32BE(n); return b; }
function string(s) { const b = Buffer.from(s); const len = Buffer.alloc(2); len.writeUInt16BE(b.length); return Buffer.concat([len, b]); }
function tag(v) {
  if (typeof v === 'number') return [3, integer(v)];
  if (typeof v === 'string') return [8, string(v)];
  if (Array.isArray(v)) { const entries = v.map(tag); return [9, Buffer.concat([Buffer.from([entries[0]?.[0] || 10]), integer(v.length), ...entries.map(e => e[1])])]; }
  return [10, Buffer.concat([...Object.entries(v).map(([k, val]) => { const [type, data] = tag(val); return Buffer.concat([Buffer.from([type]), string(k), data]); }), Buffer.from([0])])];
}
function cube(from, to, texture) { return {from, to, faces: Object.fromEntries(['north','south','east','west','up','down'].map(face => [face, {texture: '#' + texture}]))}; }
for (const name of ['quarantine_gate', 'quarantine_archive', 'quarantine_return']) {
  const elements = [cube([0,0,0],[16,12,16],'metal'), cube([1,12,1],[15,14,15],'rim'), cube([3,14,3],[13,15,13],'display')];
  const textures = {particle:'minecraft:block/iron_block', metal:'minecraft:block/iron_block', rim:'minecraft:block/black_concrete', display: name === 'quarantine_archive' ? 'minecraft:block/copper_block' : 'minecraft:block/sea_lantern'};
  json(`assets/darkspawn/models/block/${name}.json`, {parent:'minecraft:block/block', textures, elements});
  if (name === 'quarantine_gate') {
    json(`assets/darkspawn/models/block/${name}_inactive.json`, {parent:`darkspawn:block/${name}`, textures:{display:'minecraft:block/gray_concrete'}});
    const variants = {};
    for (const [facing,y] of [['north',0],['east',90],['south',180],['west',270]]) for (const active of [false,true]) variants[`active=${active},facing=${facing}`] = {model:`darkspawn:block/${name}${active?'':'_inactive'}`,y};
    json(`assets/darkspawn/blockstates/${name}.json`, {variants});
  } else json(`assets/darkspawn/blockstates/${name}.json`, {variants:{'':{model:`darkspawn:block/${name}`}}});
}
json('assets/darkspawn/items/quarantine_gate.json', {model:{type:'minecraft:model',model:'darkspawn:block/quarantine_gate'}});
json('assets/darkspawn/models/item/containment_key.json', {parent:'minecraft:block/block', textures:{particle:'minecraft:block/copper_block',metal:'minecraft:block/copper_block',light:'minecraft:block/sea_lantern'}, elements:[cube([6,2,7],[9,11,9],'metal'),cube([4,10,6],[11,15,10],'metal'),cube([5,11,5.8],[10,14,10.2],'light'),cube([9,3,7],[13,5,9],'metal'),cube([9,6,7],[11,8,9],'metal')],display:{gui:{rotation:[15,-35,0],translation:[0,0,0],scale:[1,1,1]},thirdperson_righthand:{rotation:[0,90,0],translation:[0,0,0],scale:[0.5,0.5,0.5]},firstperson_righthand:{rotation:[0,-90,20],translation:[0,2,0],scale:[0.6,0.6,0.6]},ground:{scale:[0.5,0.5,0.5]}}});
json('assets/darkspawn/items/containment_key.json', {model:{type:'minecraft:model',model:'darkspawn:item/containment_key'}});
json('data/darkspawn/loot_table/blocks/quarantine_gate.json', {type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'darkspawn:quarantine_gate'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
json('data/darkspawn/dimension/forsaken.json', {type:'minecraft:overworld',generator:{type:'minecraft:flat',settings:{biome:'darkspawn:forsaken',features:false,lakes:false,structure_overrides:['darkspawn:forsaken_city'],layers:[{block:'minecraft:bedrock',height:1},{block:'minecraft:deepslate',height:120},{block:'minecraft:stone',height:23}]}}});
json('data/darkspawn/worldgen/structure/research_bunker.json', {type:'darkspawn:boss_landmark',site:'research_bunker',biomes:'#darkspawn:has_structure/research_bunker',spawn_overrides:{},step:'top_layer_modification',terrain_adaptation:'none'});
json('data/darkspawn/worldgen/structure_set/research_bunker.json', {structures:[{structure:'darkspawn:research_bunker',weight:1}],placement:{type:'minecraft:random_spread',spacing:32,separation:12,salt:10412673}});
json('data/darkspawn/tags/worldgen/biome/has_structure/research_bunker.json', {replace:false,values:['minecraft:plains','minecraft:sunflower_plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga']});
json('data/darkspawn/loot_table/chests/research_bunker.json', {type:'minecraft:chest',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'minecraft:written_book',modifier:{type:'minecraft:set_components',components:{'minecraft:written_book_content':{title:'Quarantine Protocol',author:'Forsaken Research Division',resolved:true,pages:[{text:'The Forsaken\n\nThe city is quarantined. Read the copper-screen archive to recover the gate schematic. Each researcher may learn it independently.'},{text:'Gate Repair\n\nRestore a 5x5 obsidian border around the controller at bottom center. Keep the 3x3 opening clear. The damaged bunker gate is missing one upper corner.'},{text:'Fabrication\n\nUse the archive to craft a reusable Containment Key. Hover over its button for materials. Activation requires this world to have defeated the Ender Dragon.'},{text:'Extraction\n\nUse the key on the controller, then use it with an empty hand to travel. Everyone may use an open gate. The glowing extraction terminal returns each traveler to their own entrance.'},{text:'Personal Gates\n\nCraft a controller at an archive and build the same frame in the Overworld. Sneak-use a controller to fabricate more parts. Keep supplies and an escape route ready.'}]}}}}]},{rolls:4,entries:[['iron_ingot',3],['copper_ingot',3],['bread',4],['paper',3],['obsidian',1]].map(([name,weight])=>({type:'minecraft:item',name:'minecraft:'+name,weight,modifier:{type:'minecraft:set_count',count:{type:'minecraft:uniform',min:1,max:3}}}))}]});
for (let variant = 0; variant < 3; variant++) {
  const blocks = new Map();
  function put(x,y,z,name,props,nbt) { blocks.set(`${x},${y},${z}`, {pos:[x,y,z],state:{Name:name.includes(':')?name:'minecraft:'+name,...(props?{Properties:props}:{})},...(nbt?{nbt}:{})}); }
  function fill(x1,y1,z1,x2,y2,z2,name) { for(let x=x1;x<=x2;x++) for(let y=y1;y<=y2;y++) for(let z=z1;z<=z2;z++) put(x,y,z,name); }
  fill(2,0,2,22,2,22,'stone'); fill(2,3,2,22,3,22,'cracked_stone_bricks'); fill(2,4,2,22,12,22,'air');
  fill(4,4,4,20,10,20,'stone_bricks'); fill(5,4,5,19,9,19,'air');
  fill(11,4,19,13,6,20,'air'); fill(11,3,20,13,3,24,'stone_bricks');
  for(let x=5;x<=19;x++) put(x,3,18,x%2?'yellow_concrete':'black_concrete');
  for(const x of [4,20]) for(const z of [9,15]) fill(x,6,z,x,7,z+1,'iron_bars');
  for(const x of [6,18]) for(const z of [6,18]) put(x,9,z,'sea_lantern');
  fill(6,4,13,9,4,14,'smooth_stone'); put(8,5,13,'darkspawn:quarantine_archive');
  put(17,4,13,'chest',{facing:'west',type:'single',waterlogged:'false'},{id:'minecraft:chest',LootTable:'darkspawn:chests/research_bunker'});
  for(let x=-2;x<=2;x++) for(let y=0;y<=4;y++) if(Math.abs(x)===2||y===0||y===4) put(12+x,4+y,7,'obsidian');
  put(12,4,7,'darkspawn:quarantine_gate',{active:'false',facing:'south'}); put(10,8,7,'air'); // Repairable damaged frame.
  // Containment Lab: Observation glazing separates the portal from a two-door airlock.
  fill(5,3,5,19,3,17,'smooth_stone');
  fill(5,4,10,19,9,10,'smooth_quartz');
  fill(9,5,10,16,8,10,'cyan_stained_glass');
  fill(5,4,14,7,8,14,'smooth_quartz');
  fill(7,4,11,7,8,13,'smooth_quartz');
  fill(5,9,11,7,9,14,'smooth_quartz');
  for(const z of [10,14]) {
    put(6,4,z,'iron_door',{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'});
    put(6,5,z,'iron_door',{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'});
    for(const dz of [-1,1]) put(6,4,z+dz,'stone_pressure_plate',{powered:'false'});
  }
  // Keep the archive reachable from the observation room; the original chest stays finite.
  for(const x of [10,13,16]) {
    put(x,4,12,'smooth_quartz'); put(x,5,12,'black_concrete');
    put(x,5,13,'heavy_weighted_pressure_plate',{power:'0'});
    put(x,6,12,variant===1?'lime_stained_glass':'cyan_stained_glass');
    put(x,7,12,'sea_lantern');
    put(x,4,15,'polished_blackstone_brick_stairs',{facing:'north',half:'bottom',shape:'straight',waterlogged:'false'});
  }
  for(const x of [8,17]) {
    fill(x,4,5,x,8,5,'polished_deepslate');
    for(const y of [5,7]) put(x,y,6,'lightning_rod',{facing:'south',powered:'false',waterlogged:'false'});
  }
  for(let x=9;x<=16;x++) put(x,3,9,x%2?'yellow_concrete':'black_concrete');
  for(const z of [6,8,16]) { put(5,9,z,'sea_lantern'); put(19,9,z,'sea_lantern'); }
  for(let n=0;n<5;n++) put(5+n*3,10,5+variant*3,'mossy_stone_bricks');
  const palette=[]; const indices=new Map(); const entries=[];
  for(const b of blocks.values()) { const key=JSON.stringify(b.state); if(!indices.has(key)){indices.set(key,palette.length);palette.push(b.state);} entries.push({pos:b.pos,state:indices.get(key),...(b.nbt?{nbt:b.nbt}:{})}); }
  const template={DataVersion:5000,size:[25,13,25],palette,blocks:entries,entities:[]};
  if (variant === 0) { fs.mkdirSync(path.join(root,'build'),{recursive:true}); fs.writeFileSync(path.join(root,'build/research-bunker-preview.json'),JSON.stringify(template)); }
  const target=path.join(res,`data/darkspawn/structure/landmarks/research_bunker_${variant}.nbt`); fs.mkdirSync(path.dirname(target),{recursive:true}); fs.writeFileSync(target,zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),tag(template)[1]])));
}
console.log('Authored Forsaken gate models, foundation data, and three research bunkers.');
