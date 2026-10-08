const fs = require('node:fs');
const read = file => fs.readFileSync(file, 'utf8');
const entries = [];
const bosses = [...read('src/main/java/darkspawn/black/boss/BossProfile.java').matchAll(/^\t([A-Z_]+)\((\d+), (\d+),/gm)];
const forms = {mutant_zombie:'HUMANOID',fossil_tyrant:'BIPED',thunder_bird:'BIRD',titan_boa:'SERPENT',baba_yaga:'HOUSE',mountain_titan:'HUMANOID',ice_wyrm:'SERPENT',kraken:'KRAKEN',cave_crawler:'ARTHROPOD',shadow_creeper_queen:'QUEEN',mycelial_sovereign:'MUSHROOM',netherborn:'QUADRUPED',soulbound_colossus:'SKELETON',void_eye:'EYE'};
function add(id,category,region,form,width,height,extra={}) { entries.push({id,category,region,form,width:Number(width),height:Number(height),...extra}); }
add('ancient_tree_spirit','boss','ancient_tree_spirit','TREE',16,28,{preserve:true,modelScale:28/18});
add('mutant_wolf','boss','mutant_wolf','WOLF',10,9,{preserve:true});
for(const [,name,width,height] of bosses) {
 const id=name.toLowerCase();
 add(id,'boss',id,forms[id],width,height,{preserve:id==='mutant_zombie'});
 add(id+'_minion','minion',id,id==='kraken'?'TENTACLE':id==='baba_yaga'?'HUMANOID':forms[id],id==='kraken'?3:width*.18,id==='kraken'?12:height*.18);
}
add('endborn','mob','void_eye','HUMANOID',1.4,4.6,{preserve:true});
add('heartwood_sapling','minion','ancient_tree_spirit','TREE',1.6,2.88);
add('frost_wolf','minion','mutant_wolf','WOLF',1.5,1.35);
for(const file of ['ForestMob','TaigaWolf']) {
 for(const [,name,id,width,height] of read('src/main/java/darkspawn/black/ecosystem/'+file+'.java').matchAll(/^\t\t([A-Z_]+)\("([a-z_]+)", ([\d.]+)F, ([\d.]+)F,/gm)) {
  const forest=file==='ForestMob';
  add(id,'mob',forest?'ancient_tree_spirit':'mutant_wolf',forest?(id==='rootcrawler'?'ARTHROPOD':'TREE'):'WOLF',width,height,{kind:name});
 }
}
for(const line of read('src/main/java/darkspawn/black/ecosystem/RegionalKind.java').split('\n')) {
 const m=line.match(/^\t([A-Z_]+)\(BossProfile\.([A-Z_]+), Form\.([A-Z_]+), Move\.([A-Z_]+), Behavior\.([A-Z_]+), Temper\.([A-Z_]+), (.+)\)[,;]/);
 if(!m)continue;
 const values=m[7].split(',').map(v=>Number(v.trim().replace(/F$/,'')));
 add(m[1].toLowerCase(),'mob',m[2].toLowerCase(),m[3],values[4],values[5],{movement:m[4],behavior:m[5],temper:m[6],herald:values[6]===1});
}
if(new Set(entries.map(e=>e.id)).size!==entries.length)throw Error('Duplicate creature registry id');
// Runtime atlases are shared; editable projects still embed their own texture for portability.
for(const e of entries)e.texture=e.category==='boss'||e.preserve?e.id:e.region==='ancient_tree_spirit'?'forest_creatures':/^(warped_|riftling$)/.test(e.id)?'netherborn_warped':e.region;
module.exports=entries;
if(require.main===module) {
 fs.writeFileSync('art/blockbench/catalog.json',JSON.stringify({description:'Registered Darkspawn living entities; project/resource coverage is checked by tools/assets/verify.cjs.',creatures:entries},null,2)+'\n');
 console.log(JSON.stringify({total:entries.length,categories:Object.fromEntries(['boss','mob','minion'].map(c=>[c,entries.filter(e=>e.category===c).length]))}));
}
