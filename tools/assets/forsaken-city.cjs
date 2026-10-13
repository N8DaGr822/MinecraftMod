// Authored city slice: native saved structure, finite scavenging loot, no chunk-load rebuilding.
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = path.resolve(__dirname, '../..'), res = path.join(root, 'src/main/resources');
function write(file, value) { const p = path.join(root, file); fs.mkdirSync(path.dirname(p), {recursive:true}); fs.writeFileSync(p, JSON.stringify(value,null,2)+'\n'); }
function data(file,value) { write('src/main/resources/data/darkspawn/'+file,value); }
function int(n) { const b=Buffer.alloc(4);b.writeInt32BE(n);return b; }
function str(s) { const b=Buffer.from(s),n=Buffer.alloc(2);n.writeUInt16BE(b.length);return Buffer.concat([n,b]); }
function tag(v) {
  if(typeof v==='number')return [3,int(v)];if(typeof v==='string')return [8,str(v)];
  if(Array.isArray(v)){const entries=v.map(tag);return [9,Buffer.concat([Buffer.from([entries[0]?.[0]||10]),int(v.length),...entries.map(e=>e[1])])];}
  return [10,Buffer.concat([...Object.entries(v).map(([k,v])=>{const [t,b]=tag(v);return Buffer.concat([Buffer.from([t]),str(k),b]);}),Buffer.from([0])])];
}
const blocks=new Map(), caches=[], buildings=[];
function put(x,y,z,name,props,nbt) {
  if(x<0||x>=64||z<0||z>=80||y<0||y>=24)throw Error('City bounds: '+[x,y,z]);
  blocks.set([x,y,z].join(','),{pos:[x,y,z],state:{Name:name.includes(':')?name:'minecraft:'+name,...(props?{Properties:props}:{})},...(nbt?{nbt}:{})});
}
function fill(x1,y1,z1,x2,y2,z2,name,props) { for(let x=x1;x<=x2;x++)for(let y=y1;y<=y2;y++)for(let z=z1;z<=z2;z++)put(x,y,z,name,props); }
function chest(x,y,z,table,facing='south') { put(x,y,z,'chest',{facing,type:'single',waterlogged:'false'},{id:'minecraft:chest',LootTable:'darkspawn:chests/forsaken_'+table});caches.push({pos:[x,y,z],table}); }
function sign(x,y,z,lines) { put(x,y,z,'oak_sign',{rotation:'8',waterlogged:'false'},{id:'minecraft:sign',front_text:{messages:lines.map(text=>({text})),color:'black'},back_text:{messages:lines.map(text=>({text})),color:'black'}}); }
// Street datum is y=3. Explicit air is limited to native generation, never applied on chunk reload.
fill(0,0,0,63,2,79,'stone');fill(0,3,0,63,3,79,'gray_concrete');fill(0,4,0,63,23,79,'air');
fill(29,3,0,34,3,79,'black_concrete');fill(0,3,34,63,3,41,'black_concrete');
for(let z=1;z<79;z+=5)fill(31,3,z,32,3,z+1,'yellow_concrete');
for(let x=2;x<62;x+=6)fill(x,3,37,x+2,3,38,'white_concrete');
// Readable quarantine seam with open street passages; infection is visual until the threat increment.
for(let x=0;x<64;x++)put(x,3,42,x%2?'yellow_concrete':'black_concrete');
for(let x=0;x<64;x++)if(x<29||x>34)put(x,4,42,'iron_bars');
sign(28,4,40,['QUARANTINE','INFESTED SECTOR','Proceed prepared','Extraction: north']);
sign(35,4,4,['ABANDONED','CIVIC DISTRICT','Supplies ahead','Extraction: north']);
function building(name,x,z,wall,infested) {
  buildings.push({name,origin:[x,3,z],size:[24,13,24],district:infested?'infested':'abandoned'});
  fill(x,4,z,x+23,14,z+23,wall);fill(x+1,4,z+1,x+22,14,z+22,'air');
  fill(x,9,z,x+23,9,z+23,'smooth_stone');fill(x,15,z,x+23,15,z+23,'stone_bricks');
  for(const y of [6,12])for(let n=3;n<22;n+=5){fill(x+n,y,z,x+n+2,y+1,z,'gray_stained_glass');fill(x+n,y,z+23,x+n+2,y+1,z+23,'gray_stained_glass');fill(x,y,z+n,x,y+1,z+n+2,'gray_stained_glass');fill(x+23,y,z+n,x+23,y+1,z+n+2,'gray_stained_glass');}
  // Two open ground exits, internal ladder shaft and a roof hatch make every cache reachable without mining.
  fill(x+10,4,z,x+12,6,z,'air');fill(x+10,4,z+23,x+12,6,z+23,'air');
  for(let y=4;y<=16;y++){put(x+1,y,z+2,'stone_bricks');put(x+2,y,z+2,'ladder',{facing:'east',waterlogged:'false'});}
  for(let dx=0;dx<24;dx++){put(x+dx,16,z,'stone_brick_wall');put(x+dx,16,z+23,'stone_brick_wall');}
  for(let dz=1;dz<23;dz++){put(x,16,z+dz,'stone_brick_wall');put(x+23,16,z+dz,'stone_brick_wall');}
  // Broken windows and roof scars; keep stairs, paths and cache lids unobstructed.
  fill(x+8,6,z,x+9,7,z,'air');fill(x+17,15,z+18,x+19,15,z+20,'air');
  if(infested){for(let n=0;n<7;n++){const dx=4+(n*7)%16,dz=4+(n*5)%16;put(x+dx,3,z+dz,'sculk');put(x+dx,4,z+dz,'nether_wart_block');if(n%2===0)put(x+dx,5,z+dz,'shroomlight');}fill(x+19,4,z+5,x+21,7,z+7,'nether_wart_block');}
  else {put(x+5,4,z+5,'bookshelf');put(x+6,4,z+5,'bookshelf');put(x+7,4,z+5,'crafting_table');}
  put(x+4,8,z+4,infested?'redstone_lamp':'sea_lantern');put(x+18,14,z+4,'sea_lantern');
}
building('Apartments',3,7,'bricks',false);building('Supply exchange',37,7,'stone_bricks',false);
building('St. Marrow Hospital',3,46,'white_concrete',true);building('Research annex',37,46,'deepslate_bricks',true);
// Apartment rooms and household salvage.
fill(14,4,10,14,7,19,'white_terracotta');fill(14,4,13,14,5,14,'air');
for(const [x,z]of [[7,12],[18,24]]){fill(x,4,z,x+2,4,z+1,'gray_wool');put(x+3,4,z,'barrel',{facing:'up',open:'false'});}
chest(7,4,24,'salvage');chest(21,10,12,'salvage');sign(12,4,6,['CIVIC HOUSING','STAIR ACCESS','Roof crossings','Keep moving']);
// Shop counters and empty shelving.
for(const z of [12,18,25]){fill(41,4,z,45,4,z,'smooth_stone');fill(55,4,z,58,5,z,'bookshelf');}
chest(56,4,10,'salvage');chest(42,10,25,'salvage');sign(47,4,6,['SUPPLY EXCHANGE','Rations / repairs','No deliveries','since quarantine']);
// Hospital wards and unmistakable cross, no free effect beacons.
fill(13,11,45,16,11,45,'red_concrete');fill(14,10,45,15,13,45,'red_concrete');
for(const z of [51,57,63]){fill(7,4,z,9,4,z+1,'white_wool');fill(19,4,z,21,4,z+1,'white_wool');put(10,4,z,'iron_bars');}
chest(22,4,66,'medical');chest(8,10,50,'medical');sign(13,4,45,['ST. MARROW','ISOLATION WARD','Do not disturb','biological growth']);
// Research benches and a high-value vault, accessible through the upper floor.
fill(41,4,51,47,4,52,'smooth_stone');for(const x of [42,44,46])put(x,5,51,'glass');
fill(54,10,59,59,13,67,'iron_bars');fill(55,10,60,58,13,66,'air');fill(54,10,61,54,12,62,'air');
chest(57,10,65,'research');chest(42,4,66,'research');sign(47,4,45,['RESEARCH ANNEX','RESTRICTED','Records upstairs','Roof route east']);
// Rooftop circuit: housing -> exchange -> annex. One readable one-block gap on the south crossing.
fill(25,15,18,39,15,20,'stone_bricks');fill(25,16,17,39,16,17,'iron_bars');fill(25,16,21,39,16,21,'iron_bars');
fill(26,16,18,26,17,20,'air');fill(37,16,18,37,17,20,'air');
fill(48,15,29,50,15,47,'stone_bricks');fill(47,16,29,47,16,47,'iron_bars');fill(51,16,29,51,16,47,'iron_bars');
fill(48,16,30,50,17,30,'air');fill(48,16,46,50,17,46,'air');fill(48,15,38,50,15,38,'air');
sign(52,16,29,['ROOF CROSSING','Broken span ahead','One-block gap','Watch your step']);chest(56,16,62,'rooftop');
// Short maintenance connection between housing and hospital; ladders connect both floor openings.
fill(8,0,26,10,0,52,'stone_bricks');fill(8,1,26,10,2,52,'air');
for(const z of [27,50]){fill(7,1,z,7,4,z,'stone_bricks');for(let y=1;y<=4;y++)put(8,y,z,'ladder',{facing:'east',waterlogged:'false'});put(9,3,z,'air');}
put(10,1,36,'sea_lantern');chest(10,1,41,'salvage','north');
// Barricades and growth stay off the central escape corridor.
for(const [x,z]of [[27,26],[36,28],[27,60],[36,58]]){put(x,4,z,'cobblestone');put(x,5,z,'iron_bars');}
for(let z=47;z<78;z+=6){put(27,3,z,'sculk');put(36,3,z,'sculk');put(28,4,z,'nether_wart_block');put(35,4,z+1,'nether_wart_block');}
for(const z of [3,32,44,73]){fill(28,4,z,28,7,z,'iron_bars');put(28,8,z,'sea_lantern');}
sign(33,4,76,['END OF CITY SLICE','More ruins repeat','every 512 blocks','Extraction: north']);
// Permanent Nests: Only fresh native city placement creates these; there is no regeneration hook.
const nests=[[12,4,58],[20,10,64],[46,4,56],[55,10,52]];
for(const [x,y,z] of nests)put(x,y,z,'darkspawn:infected_nest',null,{id:'darkspawn:infected_nest'});
const palette=[],indices=new Map(),entries=[];
for(const b of blocks.values()){const k=JSON.stringify(b.state);if(!indices.has(k)){indices.set(k,palette.length);palette.push(b.state);}entries.push({pos:b.pos,state:indices.get(k),...(b.nbt?{nbt:b.nbt}:{})});}
// Minecraft 26.3 stores lowercase block-state keys; keep the readable blueprint schema separate.
const nativePalette=palette.map(s=>({id:s.Name,...(s.Properties?{properties:s.Properties}:{})}));
const template={DataVersion:5023,size:[64,24,80],palette:nativePalette,blocks:entries,entities:[]};
const target=path.join(res,'data/darkspawn/structure/forsaken/city_slice.nbt');fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),tag(template)[1]])));
write('art/structures/forsaken_city.json',{size:template.size,anchor:[32,3,0],buildings,caches,nests,districts:[{name:'abandoned',min:[0,0,0],max:[63,23,41]},{name:'infested',min:[0,0,42],max:[63,23,79]}],palette,blocks:entries.filter(b=>palette[b.state].Name!=='minecraft:air')});
data('worldgen/biome/forsaken.json',{attributes:{'minecraft:visual/sky_color':'#89998f'},carvers:[],downfall:0.2,effects:{water_color:'#526959'},features:Array.from({length:11},()=>[]),has_precipitation:true,temperature:0.65});
data('worldgen/structure/forsaken_city.json',{type:'darkspawn:forsaken_city',biomes:['darkspawn:forsaken'],spawn_overrides:{},step:'top_layer_modification',terrain_adaptation:'none'});
data('worldgen/structure_set/forsaken_city.json',{structures:[{structure:'darkspawn:forsaken_city',weight:1}],placement:{type:'minecraft:random_spread',spacing:32,separation:31,salt:12610317}});
const loot={salvage:[['bread',5,1,3],['iron_nugget',5,3,9],['copper_ingot',4,1,3],['string',3,1,3],['torch',4,2,6]],medical:[['honey_bottle',5,1,2],['golden_carrot',3,1,2],['paper',4,2,5],['golden_apple',1,1,1]],research:[['quartz',4,2,5],['redstone',4,3,8],['iron_ingot',3,1,3],['ender_pearl',1,1,1]],rooftop:[['diamond',1,1,1],['gold_ingot',3,1,3],['iron_ingot',5,2,4],['cooked_beef',5,2,4]]};
for(const [name,items]of Object.entries(loot)) {
  const pools=[{rolls:{type:'minecraft:uniform',min:2,max:4},entries:items.map(([item,weight,min,max])=>({type:'minecraft:item',name:'minecraft:'+item,weight,modifier:{type:'minecraft:set_count',count:{type:'minecraft:uniform',min,max}}}))}];
  const resource=name==='salvage'?'contaminated_alloy':name==='medical'?'biofiber':'research_data';
  pools.push({rolls:1,entries:[{type:'minecraft:item',name:'darkspawn:'+resource,modifier:{type:'minecraft:set_count',count:{type:'minecraft:uniform',min:1,max:3}}}]});
  if(name==='medical'||name==='research'||name==='rooftop')pools.push({rolls:1,entries:[{type:'minecraft:item',name:'darkspawn:quarantine_module',weight:1},{type:'minecraft:empty',weight:4}]});
  data(`loot_table/chests/forsaken_${name}.json`,{type:'minecraft:chest',pools,random_sequence:'darkspawn:chests/forsaken_'+name});
}
console.log(`Forsaken city: ${entries.length} blocks, ${buildings.length} buildings, ${caches.length} finite caches.`);
