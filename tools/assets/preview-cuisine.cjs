// Offline orthographic review of the actual Java model geometry and UV atlas.
const fs=require('node:fs'), path=require('node:path');
const root=path.resolve(__dirname,'../..'), assets=path.join(root,'src/main/resources/assets/darkspawn');
const ids=[...fs.readdirSync(path.join(assets,'models/block')).filter(n=>n.endsWith('_trophy.json')).map(n=>'block/'+n),
 ...['hunters_stew','garlic_bread','vegetable_pizza','berry_pie','chocolate_cake','tropical_skewer','frost_garlic_soup','marsh_rice_bowl'].map(n=>'item/'+n+'.json'),
 ...['hunters_feast_6','ocean_feast_6','nether_feast_6','ender_banquet_6','hero_feast_6','hero_feast_2'].map(n=>'block/'+n+'.json')];
const models=ids.map(id=>({id,model:JSON.parse(fs.readFileSync(path.join(assets,'models',id),'utf8'))}));
const texture='data:image/png;base64,'+fs.readFileSync(path.join(assets,'textures/block/cuisine_atlas.png')).toString('base64');
const html=`<!doctype html><meta charset="utf-8"><title>Darkspawn cuisine art review</title>
<style>body{background:#142126;color:#eee;font:14px system-ui;margin:24px}h1{font-size:26px}main{display:grid;grid-template-columns:repeat(6,1fr);gap:10px}figure{margin:0;background:#203139;border:1px solid #40545d;padding:8px;text-align:center}canvas{width:100%;image-rendering:pixelated}figcaption{font-size:12px;min-height:30px}</style>
<h1>Darkspawn · cuisine & trophy artwork</h1><p>Actual models and texture atlas · orthographic preview · in-game lighting review still required</p><main></main>
<script>const models=${JSON.stringify(models)},image=new Image();image.src=${JSON.stringify(texture)};
image.onload=()=>{const texCanvas=document.createElement('canvas');texCanvas.width=image.width;texCanvas.height=image.height;const texCtx=texCanvas.getContext('2d');texCtx.drawImage(image,0,0);const tex=texCtx.getImageData(0,0,image.width,image.height).data;
for(const {id,model} of models){const card=document.createElement('figure'),canvas=document.createElement('canvas');canvas.width=240;canvas.height=210;card.append(canvas);const caption=document.createElement('figcaption');caption.textContent=id.split('/')[1].replace('.json','').replaceAll('_',' ');card.append(caption);document.querySelector('main').append(card);const ctx=canvas.getContext('2d'),pixels=ctx.createImageData(240,210),depth=new Float32Array(240*210).fill(-Infinity);
const project=([x,y,z])=>[120+(x+z-16)*6,160+(x-z)*3-y*7,x-z+y*6/7];
function triangle(points,uv,shade){const [a,b,c]=points,den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<.001)return;
for(let y=Math.max(0,Math.floor(Math.min(...points.map(p=>p[1]))));y<=Math.min(209,Math.ceil(Math.max(...points.map(p=>p[1]))));y++)for(let x=Math.max(0,Math.floor(Math.min(...points.map(p=>p[0]))));x<=Math.min(239,Math.ceil(Math.max(...points.map(p=>p[0]))));x++){
const w=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,t=1-w-v;if(w<0||v<0||t<0)continue;const d=w*a[2]+v*b[2]+t*c[2],i=y*240+x;if(d<depth[i])continue;depth[i]=d;
const u=Math.max(0,Math.min(image.width-1,Math.floor((w*uv[0][0]+v*uv[1][0]+t*uv[2][0])/16*image.width))),q=Math.max(0,Math.min(image.height-1,Math.floor((w*uv[0][1]+v*uv[1][1]+t*uv[2][1])/16*image.height))),j=(q*image.width+u)*4;for(let k=0;k<3;k++)pixels.data[i*4+k]=tex[j+k]*shade;pixels.data[i*4+3]=255;}}
for(const e of model.elements){const [a,b,c]=e.from,[d,f,g]=e.to;
for(const [side,points,shade] of [['up',[[a,f,c],[d,f,c],[d,f,g],[a,f,g]],1],['north',[[d,f,c],[a,f,c],[a,b,c],[d,b,c]],.88],['east',[[d,f,g],[d,f,c],[d,b,c],[d,b,g]],.72]]){const def=e.faces[side];if(!def)continue;const p=points.map(project),[u,v,r,s]=def.uv,uv=[[u,v],[r,v],[r,s],[u,s]];for(const indices of [[0,1,2],[0,2,3]])triangle(indices.map(i=>p[i]),indices.map(i=>uv[i]),shade);}}
ctx.putImageData(pixels,0,0);}
document.title='READY · Darkspawn cuisine art review';};</script>`;
fs.mkdirSync(path.join(root,'build/art-review'),{recursive:true});
fs.writeFileSync(path.join(root,'build/art-review/cuisine.html'),html);
console.log('build/art-review/cuisine.html: '+models.length+' textured models');
