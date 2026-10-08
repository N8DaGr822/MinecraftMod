// Used after export checks and visual review; each invocation enables only the named completed asset.
const fs=require('node:fs');const catalog=require('./catalog.cjs');
const file='src/main/java/darkspawn/black/boss/CreatureAssets.java';
let source=fs.readFileSync(file,'utf8');
for(const id of process.argv.slice(2)){
 const e=catalog.find(e=>e.id===id);if(!e)throw Error('Unknown creature '+id);
 const category=e.category==='boss'?'BOSSES':e.category==='minion'?'MINIONS':'MOBS';
 for(const relative of [`geckolib/models/entity/${id}.geo.json`,`geckolib/animations/entity/${id}.animation.json`,`textures/entity/${id}.png`])if(!fs.existsSync('src/main/resources/assets/darkspawn/'+relative))throw Error('Missing '+relative);
 const re=new RegExp(`(${category} = Set\\.of\\()([^;]*)(\\);)`);const match=source.match(re);if(!match)throw Error('Missing asset registry '+category);
 const ids=[...match[2].matchAll(/"([a-z_]+)"/g)].map(m=>m[1]);if(!ids.includes(id))ids.push(id);
 source=source.replace(re,(_,start,body,end)=>start+ids.map(v=>'"'+v+'"').join(', ')+end);
}
fs.writeFileSync(file,source);
