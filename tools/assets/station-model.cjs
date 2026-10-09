const fs = require('node:fs');
const crypto = require('node:crypto');
const root = 'src/main/resources/assets/darkspawn';
const uv = {north:[0,0,8,8],south:[0,0,8,8],east:[0,8,8,16],west:[0,8,8,16],up:[8,0,16,8],down:[8,8,16,16]};
const model = {
  parent:'minecraft:block/block',
  textures:{atlas:'darkspawn:block/cooking_station',particle:'#atlas'},
  elements:[{from:[0,0,0],to:[16,16,16],faces:Object.fromEntries(Object.entries(uv).map(([face,coords])=>[face,{uv:coords,texture:'#atlas',cullface:face}]))}]
};
fs.writeFileSync(root+'/models/block/cooking_station.json',JSON.stringify(model,null,2)+'\n');
const texture=fs.readFileSync(root+'/textures/block/cooking_station.png');
const uuid=crypto.randomUUID();
const project={
  meta:{format_version:'5.0',model_format:'java_block',box_uv:false},
  name:'Cooking Station',model_identifier:'cooking_station',parent:model.parent,
  resolution:{width:128,height:128},
  elements:[{name:'Cooking cabinet',type:'cube',uuid,from:[0,0,0],to:[16,16,16],origin:[8,8,8],box_uv:false,
    faces:Object.fromEntries(Object.entries(uv).map(([face,coords])=>[face,{uv:coords.map(v=>v*8),texture:0,cullface:face}]))}],
  outliner:[uuid],textures:[{name:'cooking_station.png',namespace:'darkspawn',folder:'block',id:'0',uuid:crypto.randomUUID(),width:128,height:128,
    uv_width:128,uv_height:128,mode:'bitmap',saved:true,particle:true,source:'data:image/png;base64,'+texture.toString('base64')}]
};
fs.writeFileSync('art/blockbench/cooking_station/CookingStation.bbmodel',JSON.stringify(project,null,2)+'\n');
console.log('Cooking Station: six mapped faces and editable Java block project');
