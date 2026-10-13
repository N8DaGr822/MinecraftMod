const fs=require('node:fs'),assert=require('node:assert/strict');
const {animationJson}=require('./rig.cjs');
const folder='src/main/resources/assets/darkspawn/geckolib/animations/entity';
let changed=0,checked=0;
for(const name of fs.readdirSync(folder).filter(name=>name.endsWith('.animation.json'))) {
  const file=folder+'/'+name,original=fs.readFileSync(file,'utf8').replaceAll('\r\n','\n');
  const ordered=animationJson(JSON.parse(original));checked++;
  if(original===ordered)continue;
  changed++;
  if(process.argv.includes('--write'))fs.writeFileSync(file,ordered);
  else assert.fail(name+': keyframes are not serialized in chronological order; run with --write');
}
console.log(`${checked} animation resources checked; ${changed} reordered without changing keyframe values.`);
