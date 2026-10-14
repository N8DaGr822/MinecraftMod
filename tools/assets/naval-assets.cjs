// Native naval component models and crafting data; no external texture dependency.
const fs=require('node:fs'),path=require('node:path');
const res=path.resolve(__dirname,'../../src/main/resources');
function json(file,value){const p=path.join(res,file);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(value,null,2)+'\n');}
function box(from,to,texture){return {from,to,faces:Object.fromEntries(['up','down','north','south','east','west'].map(f=>[f,{texture:'#'+texture}]))};}
const textures={particle:'minecraft:block/spruce_planks',wood:'minecraft:block/spruce_planks',metal:'minecraft:block/iron_block',dark:'minecraft:block/deepslate_tiles',chart:'minecraft:block/cartography_table_top'};
const models={
 shipyard:[box([0,0,0],[16,12,16],'wood'),box([0,12,0],[16,16,16],'chart')],
 ship_helm:[box([6,0,7],[10,10,11],'wood'),box([0,9,6],[16,11,9],'wood'),box([7,2,6],[9,16,9],'wood'),box([2,6,6],[4,14,9],'wood'),box([12,6,6],[14,14,9],'wood'),box([4,14,6],[12,16,9],'wood'),box([4,4,6],[12,6,9],'wood'),box([6,8,5],[10,12,10],'metal')],
 naval_cannon:[box([1,0,2],[15,4,14],'wood'),box([0,1,1],[4,7,5],'dark'),box([12,1,1],[16,7,5],'dark'),box([0,1,11],[4,7,15],'dark'),box([12,1,11],[16,7,15],'dark'),box([0,6,5],[16,11,11],'metal'),box([0,7,6],[1,10,10],'dark')]
};
for(const [name,elements] of Object.entries(models)){
 json(`assets/darkspawn/models/block/${name}.json`,{parent:'minecraft:block/block',textures,elements});
 json(`assets/darkspawn/blockstates/${name}.json`,{variants:Object.fromEntries([['north',name==='naval_cannon'?90:0],['east',name==='naval_cannon'?180:90],['south',name==='naval_cannon'?270:180],['west',name==='naval_cannon'?0:270]].map(([facing,y])=>[`facing=${facing}`,{model:`darkspawn:block/${name}`,y}]))});
 json(`assets/darkspawn/items/${name}.json`,{model:{type:'minecraft:model',model:`darkspawn:block/${name}`}});
 json(`data/darkspawn/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`darkspawn:${name}`}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
}
for(const [name,texture] of [['solid_shot','iron_block'],['incendiary_shot','magma']]){
 json(`assets/darkspawn/models/item/${name}.json`,{parent:'minecraft:block/block',textures:{particle:`minecraft:block/${texture}`,ball:`minecraft:block/${texture}`},elements:[box([5,3,5],[11,13,11],'ball'),box([3,5,5],[13,11,11],'ball'),box([5,5,3],[11,11,13],'ball')],display:{gui:{rotation:[25,35,0],scale:[1.2,1.2,1.2]},ground:{scale:[.5,.5,.5]}}});
 json(`assets/darkspawn/items/${name}.json`,{model:{type:'minecraft:model',model:`darkspawn:item/${name}`}});
}
const recipes={
 shipyard:{pattern:['IPI','PCP','PPP'],key:{I:'minecraft:iron_ingot',P:'#minecraft:planks',C:'minecraft:crafting_table'}},
 ship_helm:{pattern:[' S ','SIS',' P '],key:{S:'minecraft:stick',I:'minecraft:iron_ingot',P:'#minecraft:planks'}},
 naval_cannon:{pattern:['III','  I','PPP'],key:{I:'minecraft:iron_ingot',P:'#minecraft:planks'}},
 solid_shot:{pattern:[' II',' II'],key:{I:'minecraft:iron_nugget'},count:4},
 incendiary_shot:{pattern:['SG'],key:{S:'darkspawn:solid_shot',G:'minecraft:gunpowder'},count:1}
};
for(const [name,recipe] of Object.entries(recipes)){
 json(`data/darkspawn/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'misc',pattern:recipe.pattern,key:recipe.key,result:{id:`darkspawn:${name}`,count:recipe.count||1}});
 json(`data/darkspawn/advancement/recipes/${name}.json`,{parent:'minecraft:recipes/root',criteria:{has_iron:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:'minecraft:iron_ingot'}]}}},requirements:[['has_iron']],rewards:{recipes:[`darkspawn:${name}`]}});
}
console.log('Generated five naval items, three component block models and recipes.');
