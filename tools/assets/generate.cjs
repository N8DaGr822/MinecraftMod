const fs=require('node:fs');
const {Rig,materialUVs}=require('./rig.cjs');
const {buildShape}=require('./shapes.cjs');
const {clips}=require('./combat-clips.cjs');
const {details}=require('./species.cjs');
const catalog=require('./catalog.cjs');
for(const id of process.argv.slice(2)){
 const e=catalog.find(e=>e.id===id);if(!e)throw Error('Unknown entity '+id);if(e.preserve||id==='fossil_tyrant')throw Error('Preserve existing authored rig '+id);
 const r=new Rig(e);const shape=buildShape(r,e);details(r,e,shape);r.fitHeight();if(e.category!=='boss')materialUVs(r);clips(r,e);
 const family=`art/blockbench/${e.region}/${e.texture==='netherborn_warped'?'warped-atlas':'atlas'}.png`,atlas=fs.existsSync(family)?family:`src/main/resources/assets/darkspawn/textures/entity/${e.region}.png`;
 console.log(JSON.stringify(r.save(atlas)));
 if(e.category!=='boss')fs.writeFileSync(`art/blockbench/${e.id}/texture-prompt.txt`,`Material atlas: ${e.texture}. See ../${e.region}/${e.texture==='forest_creatures'?'mob-atlas-prompt':e.texture==='netherborn_warped'?'warped-texture-prompt':'texture-prompt'}.txt for the built-in image_gen prompt. Geometry and UVs are authored for ${e.id}.\n`);
}
