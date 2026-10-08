const fs=require('node:fs'),assert=require('node:assert/strict');
const catalog=require('./catalog.cjs');
const ids=process.argv.slice(2);
let checked=0;
for(const e of catalog.filter(e=>ids.length?ids.includes(e.id):!e.preserve)) {
 const title=e.id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(''),file=`art/blockbench/${e.id}/${title}.bbmodel`;
 assert(fs.existsSync(file),'Missing Blockbench project '+e.id);
 const m=JSON.parse(fs.readFileSync(file,'utf8')),g=JSON.parse(fs.readFileSync(`src/main/resources/assets/darkspawn/geckolib/models/entity/${e.id}.geo.json`)),a=JSON.parse(fs.readFileSync(`src/main/resources/assets/darkspawn/geckolib/animations/entity/${e.id}.animation.json`));
 const png=fs.readFileSync(`src/main/resources/assets/darkspawn/textures/entity/${e.texture||e.id}.png`);
 assert.equal(m.model_identifier,e.id);assert.equal(m.textures[0].source,'data:image/png;base64,'+png.toString('base64'),'Embedded/runtime texture mismatch');
 const bones=g['minecraft:geometry'][0].bones,names=new Set(bones.map(b=>b.name)),groups=new Set(m.groups.map(b=>b.name));assert.equal(bones.length,m.groups.length);assert.equal(names.size,bones.length);
 let cubes=0,height=0;for(const bone of bones){assert(groups.has(bone.name));if(bone.parent)assert(names.has(bone.parent));for(const cube of bone.cubes||[]){cubes++;assert(cube.size.every(v=>Number.isFinite(v)&&v>0));height=Math.max(height,cube.origin[1]+cube.size[1]);if(Array.isArray(cube.uv)){const [w,h,d]=cube.size,[u,v]=cube.uv;assert(u>=0&&v>=0&&u+2*(w+d)<=m.resolution.width&&v+h+d<=m.resolution.height,'UV outside atlas');}else{for(const f of Object.values(cube.uv)){const [u,v]=f.uv,[w,h]=f.uv_size;assert(u>=0&&v>=0&&u+w<=m.resolution.width&&v+h<=m.resolution.height,'Face UV outside atlas');}}}}
 assert.equal(cubes,m.elements.length);assert(Math.abs(height/16-e.height)<.05,`${e.id} height ${height/16} != ${e.height}`);
 assert.equal(Object.keys(a.animations).length,m.animations.length);for(const [name,clip]of Object.entries(a.animations)){assert(name.startsWith(`animation.${e.id}.`));assert(clip.animation_length>0);for(const [bone,channels]of Object.entries(clip.bones)){assert(names.has(bone));for(const frames of Object.values(channels))for(const [time,values]of Object.entries(frames)){assert(Number(time)>=0&&Number(time)<=clip.animation_length);assert(values.every(Number.isFinite));}}}
 checked++;console.log(`${e.id}: ${cubes} cuboids, ${bones.length} bones, ${m.animations.length} clips, texture/export checks passed`);
}
assert(checked>0,'No known creature ids selected');
