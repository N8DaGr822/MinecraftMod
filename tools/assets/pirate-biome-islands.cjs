// Biome Islands: Native block templates, preserving the original nine quest landmarks.
module.exports = function({fs,path,zlib,res,tag}) {
  const variants=[
    ['Meadowhaven','grass_block','stone','oak_planks','oak_log','oak_leaves'],
    ['Verdant Sanctuary','grass_block','stone','mossy_stone_bricks','jungle_log','jungle_leaves'],
    ['Ashen Garrison','crimson_nylium','netherrack','blackstone','crimson_stem','nether_wart_block'],
    ['Starfall Sanctuary','end_stone','end_stone','purpur_block','obsidian','chorus_flower'],
    ['Sunscar Town','sand','sandstone','smooth_sandstone','cut_sandstone','cactus'],
    ['Frosthaven','snow_block','stone','spruce_planks','spruce_log','spruce_leaves']
  ];
  function save(file,size,palette,blocks) {
    const template={DataVersion:5000,size,palette,blocks:[...blocks.values()],entities:[]};
    fs.writeFileSync(path.join(res,`data/darkspawn/structure/pirate/${file}.nbt`),zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),tag(template)[1]])));
  }
  for(let n=0;n<variants.length;n++) {
    const kind=n+9,[name,surface,rock,wall,trunk,leaves]=variants[n],palette=[],indices=new Map(),blocks=new Map();
    function put(x,y,z,id,properties={},nbt) {
      if(x<0||x>128||z<0||z>128||y<0||y>=56)throw Error(`${name}: invalid block ${x},${y},${z}`);
      if(id.endsWith('_leaves'))properties={persistent:'true',distance:'1',waterlogged:'false',...properties};
      const state={Name:'minecraft:'+id,...(Object.keys(properties).length?{Properties:properties}:{})};
      const key=JSON.stringify(state);if(!indices.has(key)){indices.set(key,palette.length);palette.push(state);}
      blocks.set(`${x},${y},${z}`,{pos:[x,y,z],state:indices.get(key),...(nbt?{nbt}:{})});
    }
    function box(x0,y0,z0,x1,y1,z1,id,props={}) {for(let x=x0;x<=x1;x++)for(let y=y0;y<=y1;y++)for(let z=z0;z<=z1;z++)put(x,y,z,id,props);}
    const tops=new Map();
    for(let x=0;x<=128;x++)for(let z=0;z<=128;z++) {
      const dx=x-64,dz=z-64,r=Math.hypot(dx,dz),a=Math.atan2(dz,dx);
      const edge=60+Math.sin(a*3+n)*2;
      if(r>edge)continue;
      let top=r<=37?24:Math.floor(19+Math.min(1,(edge-r)/8)*(8+Math.max(0,Math.sin(dx*.13+n)*Math.cos(dz*.1))*10));
      if(Math.abs(dz)<=3&&dx>25&&dx<52)top=24;
      tops.set(`${x},${z}`,top);
      for(let y=0;y<=top;y++) {
        let material=y===top?(top<24?(n===2?'blackstone':n===3?'end_stone':'sand'):surface):y>top-4?(n===0||n===1||n===5?'dirt':rock):rock;
        const vein=Math.abs(Math.imul(Math.floor(x/3)+n*73,7349)^Math.imul(Math.floor(y/3),193)^Math.imul(Math.floor(z/3),9151))%127;
        if(y<top-4) {
          if(n===2){if(vein<7)material='nether_quartz_ore';else if(vein<10)material='nether_gold_ore';}
          else if(n!==3){if(vein<5)material='coal_ore';else if(vein<9)material='iron_ore';else if(vein<12)material='copper_ore';else if(y<12&&vein===12)material='gold_ore';else if(y<10&&vein===13)material='redstone_ore';else if(y<10&&vein===14)material='lapis_ore';}
        }
        if(Math.abs(r-44)<2.5&&y>=11&&y<=14&&y<top-3)material='air';
        if(dx>=35&&dx<=47&&Math.abs(dz)<=2&&y>=11&&y<=14)material='air';
        if(dx>=27&&dx<=41&&Math.abs(dz)<=1){const floor=24-(dx-27);if(y>floor)material='air';if(y===floor)material=rock;}
        put(x,y,z,material);
      }
    }
    // Surface Detail: Keep the town and mine clear; vegetation follows each native biome palette.
    for(const [key,top] of tops) {
      const [x,z]=key.split(',').map(Number),r=Math.hypot(x-64,z-64);
      if(r<39||top<25||Math.abs(z-64)<6)continue;
      const scatter=(Math.imul(x+17,73856093)^Math.imul(z+31,19349663)^Math.imul(n+1,83492791))>>>0;
      if(scatter%(n===1?37:n===0?149:79)===0) {
        if(n===3){put(x,top+1,z,'chorus_plant',{down:'true',up:'true'});put(x,top+2,z,'chorus_flower',{age:'0'});}
        else if(n===4){put(x,top,z,'sand');put(x,top+1,z,'cactus');put(x,top+2,z,'cactus');}
        else {
          for(let h=1;h<=5;h++)put(x,top+h,z,trunk);
          for(let ox=-2;ox<=2;ox++)for(let oz=-2;oz<=2;oz++)if(Math.abs(ox)+Math.abs(oz)<=3)put(x+ox,top+6,z+oz,leaves);
          if(n===1)put(x+1,top+2,z,'cocoa',{age:'2',facing:'west'});
        }
      } else if(scatter%29===0&&(n===0||n===1))put(x,top+1,z,n===0?'dandelion':'short_grass');
    }
    // Town Streets: All houses, guard positions and the warden share the same accessible plateau.
    box(40,24,62,88,24,66,n===3?'purpur_block':'stone_bricks');
    box(62,24,34,66,24,117,n===3?'purpur_block':'stone_bricks');
    box(61,23,115,67,24,126,n===2?'crimson_planks':'oak_planks');
    const homes=n===1||n===3?[[-18,-14],[18,-14]]:[[-18,-14],[18,-14],[-18,14],[18,14]];
    for(const [ox,oz] of homes) {
      const x=64+ox,z=64+oz;
      box(x-5,24,z-5,x+5,24,z+5,wall);box(x-4,25,z-4,x+4,29,z+4,wall);box(x-3,25,z-3,x+3,28,z+3,'air');
      box(x-1,25,z+4,x+1,27,z+4,'air');
      for(const wx of [-4,4])box(x+wx,26,z-1,x+wx,27,z+1,'glass_pane');
      for(let step=0;step<4;step++)box(x-5+step,30+step,z-5,x+5-step,30+step,z+5,n===2?'polished_blackstone_bricks':n===3?'purpur_block':n===4?'cut_sandstone':'dark_oak_planks');
      put(x-2,25,z-2,'crafting_table');put(x+2,25,z-2,'furnace',{facing:'south'});put(x,28,z,'lantern',{hanging:'true'});
      put(x-2,25,z+1,'red_bed',{part:'foot',facing:'north',occupied:'false'});put(x-2,25,z,'red_bed',{part:'head',facing:'north',occupied:'false'});
      box(x-1,24,z+5,x+1,24,z+8,'stone_bricks');
    }
    // Sanctuary Hall / Town Well: Distinct landmarks with finite, native loot tables.
    if(n===1||n===3) {
      box(56,24,75,72,24,87,wall);
      for(const x of [56,72])for(const z of [75,87])box(x,25,z,x,31,z,wall);
      box(55,32,74,73,32,88,wall);put(64,25,82,n===3?'enchanting_table':'moss_block');
      for(const x of [58,70])put(x,31,80,'lantern',{hanging:'true'});
      if(n===3) {
        box(62,33,78,66,35,84,'purpur_block');box(63,36,79,65,36,83,'end_stone_bricks');put(64,37,81,'end_rod');
        // Ruined Observatory: An open arch and fallen masonry distinguish it from the jungle hall.
        box(80,25,78,80,31,78,'purpur_pillar');box(86,25,78,86,28,78,'purpur_pillar');box(80,32,78,83,32,78,'purpur_block');
        put(87,25,80,'purpur_block');put(85,25,82,'purpur_pillar');
      }
    } else {
      box(60,25,60,68,25,68,wall);box(62,25,62,66,25,66,'water');
      for(const x of [60,68])for(const z of [60,68])box(x,26,z,x,28,z,wall);
      box(59,29,59,69,29,69,wall);
    }
    if(n===2) {
      // Occupied Garrison: Supported ramparts with four street gates, not a sealed decorative enclosure.
      for(let p=34;p<=94;p++)for(const [x,z] of [[p,34],[p,94],[34,p],[94,p]]) {
        if(x===94&&z>=62&&z<=66)continue; // Preserve the descending mine entrance through the east gate.
        box(x,0,z,x,24,z,'blackstone');box(x,25,z,x,28,z,'polished_blackstone_bricks');
        if(p%3===0)put(x,29,z,'polished_blackstone_brick_wall');
        if(p>=62&&p<=66)box(x,25,z,x,27,z,'air');
      }
    }
    put(64,25,52,'chest',{facing:'south',type:'single',waterlogged:'false'},{id:'minecraft:chest',LootTable:n===2?'minecraft:chests/ruined_portal':n===3?'darkspawn:chests/pirate_shrine':'minecraft:chests/village/village_plains_house'});
    // Food Supply: Mature crops and water are normal farm blocks and can be replanted.
    if(n===0||n===1||n===5)for(let x=52;x<=59;x++)for(let z=86;z<=93;z++){put(x,24,z,x===55?'water':'farmland',x===55?{}:{moisture:'7'});if(x!==55)put(x,25,z,'wheat',{age:'7'});else put(x,23,z,'sea_lantern');}
    if(n===2){box(52,24,86,59,24,93,'soul_sand');for(let x=52;x<=59;x++)for(let z=86;z<=93;z++)put(x,25,z,'nether_wart',{age:'3'});put(101,3,65,'ancient_debris');}
    if(n!==2&&n!==3)for(let x=0;x<2;x++)for(let z=0;z<2;z++)put(103+x,3,66+z,'diamond_ore');
    if(n===1)for(let h=1;h<=3;h++)put(100,tops.get('100,74')+h,74,'bamboo',{age:'1',leaves:h===3?'large':'none',stage:'0'});
    // Lantern Boundary: The warden protects players within 28 blocks, not the outer resource wilderness.
    for(const x of [42,64,86])for(const z of [42,64,86]) {
      if(x===64&&z===64)continue;
      box(x,25,z,x,27,z,n===2?'polished_blackstone_wall':'oak_fence');put(x,28,z,'lantern');
    }
    // Clear authored spawn pads after decoration and keep four finite hostile patrol positions accessible.
    for(const [dx,dz] of [[0,10],[-7,10],[7,10],[-7,-10],[7,-10]]){put(64+dx,24,64+dz,wall);box(64+dx,25,64+dz,64+dx,27,64+dz,'air');}
    save(`terrain_${kind}`,[129,56,129],palette,blocks);
    // Stable Discovery Marker: Existing native start identification keeps using island_<kind>.
    save(`island_${kind}`,[49,40,49],[{Name:'minecraft:'+rock}],new Map([['marker',{pos:[24,0,24],state:0}]]));
  }
  console.log('Generated six biome islands with native terrain, settlements, resources and finite caches.');
};
