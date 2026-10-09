// Crop Artwork: Generate eight native Java block stages from a four-material pixel atlas.
// Coordinates are model pixels. Keep roots at Y=-1 to meet farmland's 15/16-height surface.
const fs=require('node:fs');
const crypto=require('node:crypto');
const id=process.argv[2];
if(!['onion','garlic','tomato','rice','corn','pepper'].includes(id))throw Error('Unknown crop '+id);
const root='src/main/resources/assets/darkspawn';
const atlas=fs.readFileSync(root+'/textures/block/'+id+'_crop.png');
const folder='art/blockbench/'+id+'_crop';
fs.mkdirSync(folder,{recursive:true});
const uuid=key=>{const h=crypto.createHash('sha256').update(id+'/'+key).digest('hex');return h.slice(0,8)+'-'+h.slice(8,12)+'-4'+h.slice(13,16)+'-a'+h.slice(17,20)+'-'+h.slice(20,32);};
// Atlas Seams: Inset one texture pixel so filtering cannot sample the neighboring material.
const materials=[[.125,.125,7.875,7.875],[8.125,.125,15.875,7.875],[.125,8.125,7.875,15.875],[8.125,8.125,15.875,15.875]];
const faceNames=['north','east','south','west','up','down'];
const states=[];
for(let age=0;age<8;age++){
 const elements=[],growth=(age+1)/8,fruit=age===7?3:2;
 const box=(name,from,to,material=0,rotation)=>{
  const element={name,from:from.map(v=>+v.toFixed(3)),to:to.map(v=>+v.toFixed(3)),faces:Object.fromEntries(faceNames.map(face=>[face,{uv:materials[material],texture:'#atlas'}]))};
  if(rotation)element.rotation=rotation;
  elements.push(element);
 };
 const blade=(name,x,y,z,length,width,direction=1,axis='z')=>{
  // Tapered leaf silhouette: three stepped segments with a consistent tilt.
  for(let n=0;n<3;n++){
   const w=width*(1-n*.3),a=n*length/3,b=(n+1)*length/3;
   if(axis==='z')box(name+n,[x+(direction>0?a:-b),y,z-w/2],[x+(direction>0?b:-a),y+.3,z+w/2],0,{origin:[x,y,z],axis:'z',angle:direction*22.5});
   else box(name+n,[x-w/2,y,z+(direction>0?a:-b)],[x+w/2,y+.3,z+(direction>0?b:-a)],0,{origin:[x,y,z],axis:'x',angle:-direction*22.5});
  }
 };
 const bulb=(name,x,z,r)=>{
  box(name+' middle',[x-r,-1,z-r],[x+r,r*.9,z+r],fruit);
  box(name+' neck',[x-r*.55,r*.9,z-r*.55],[x+r*.55,r*1.6,z+r*.55],fruit);
 };
 if(id==='onion'||id==='garlic'){
  const centers=[[4.5,4.5],[11.5,10.5]];
  for(const [i,[x,z]]of centers.entries()){
   const r=.65+growth*1.65,h=2+growth*(id==='onion'?10:8);
   if(age>=3)bulb('Bulb '+i,x,z,r);
   for(let n=0;n<3;n++){
    const dx=(n-1)*.65;
    box('Upright leaf '+i+' '+n,[x+dx-.35,-1,z-.35],[x+dx+.35,h-(n===1?0:1.4),z+.35],0,
      {origin:[x,-1,z],axis:n===1?'x':'z',angle:n===0?-22.5:22.5});
   }
  }
 }else if(id==='corn'){
  const h=2+growth*20;
  box('Corn stalk',[7.55,-1,7.55],[8.45,h,8.45],1);
  for(let n=0;n<2+Math.floor(growth*4);n++)blade('Corn leaf '+n,8,Math.max(0,h*(.17+n*.11)),8,2+growth*4,1+growth,n%2?1:-1,n%3?'z':'x');
  if(age>=5){
   for(const [i,[x,z,y]]of [[5,7,h*.52],[10,8,h*.64]].entries()){
    box('Cob '+i,[x-1.1,y,z-1],[x+1.1,y+4.5,z+1],fruit);
    box('Cob tip '+i,[x-.65,y+4.5,z-.65],[x+.65,y+5.5,z+.65],fruit);
    box('Husk '+i,[x-1.4,y-.5,z-.9],[x+1.4,y+2,z+.9],0);
   }
   box('Tassel stem',[7.75,h,7.75],[8.25,h+2,8.25],fruit);
   box('Tassel spread',[6.4,h+.7,7.75],[9.6,h+1.2,8.25],fruit);
  }
 }else if(id==='rice'){
  for(let n=0;n<5;n++){
   const x=4+(n%3)*3.4,z=5+Math.floor(n/3)*5,h=2+growth*(10+n%2*2);
   box('Rice reed '+n,[x-.25,-1,z-.25],[x+.25,h,z+.25],1);
   blade('Rice leaf '+n,x,h*.45,z,1.5+growth*2,.6,n%2?1:-1);
   if(age>=5)for(let g=0;g<4;g++){
    const direction=n%2?1:-1,px=x+direction*g*.65,py=h-g*.55;
    box('Grain '+n+' '+g,[px-.35,py-.9,z-.35],[px+.35,py+.3,z+.35],fruit);
   }
  }
 }else{
  const h=2+growth*12;
  box('Main stem',[7.6,-1,7.6],[8.4,h,8.4],1);
  for(let n=0;n<2+Math.floor(growth*4);n++)blade('Canopy '+n,8,h*(.3+n*.1),8,1.6+growth*4.2,1+growth*1.8,n%2?1:-1,n%3?'z':'x');
  if(age>=4)for(const [i,[x,z,y]]of [[4,7,h*.4],[11,8,h*.57],[8,11,h*.73]].entries()){
   const r=(age===4?.55:1.2),fh=id==='pepper'?3:2.1;
   box('Fruit '+i,[x-r,y,z-r],[x+r,y+fh,z+r],fruit);
   box('Fruit crown '+i,[x-r*.65,y+fh,z-r*.65],[x+r*.65,y+fh+.45,z+r*.65],0);
   box('Fruit stalk '+i,[Math.min(x,8),y+fh+.45,Math.min(z,8)],[Math.max(x,8)+.35,y+fh+.8,Math.max(z,8)+.35],1);
  }
 }
 const name=id+(age===7?'_ripe':'_crop_stage'+age),title=id[0].toUpperCase()+id.slice(1)+(age===7?'Ripe':'Stage'+age);
 const model={ambientocclusion:false,textures:{atlas:'darkspawn:block/'+id+'_crop',particle:'#atlas'},elements};
 fs.writeFileSync(root+'/models/block/'+name+'.json',JSON.stringify(model,null,2)+'\n');
 states.push({when:{age:String(age)},apply:{model:'darkspawn:block/'+name}});
 const cubes=elements.map((e,i)=>({...e,type:'cube',uuid:uuid(age+'/'+i),box_uv:false,origin:e.rotation?.origin||[8,0,8],
  rotation:e.rotation?['x','y','z'].map(a=>a===e.rotation.axis?e.rotation.angle:0):[0,0,0],
  faces:Object.fromEntries(Object.entries(e.faces).map(([f,v])=>[f,{uv:v.uv.map(n=>n*8),texture:0}]))}));
 const project={meta:{format_version:'5.0',model_format:'java_block',box_uv:false},name:title,model_identifier:name,ambientocclusion:false,
  resolution:{width:128,height:128},elements:cubes,outliner:cubes.map(c=>c.uuid),textures:[{name:id+'_crop.png',namespace:'darkspawn',folder:'block',id:'0',uuid:uuid('texture'),width:128,height:128,uv_width:128,uv_height:128,mode:'bitmap',saved:true,source:'data:image/png;base64,'+atlas.toString('base64')}]};
 fs.writeFileSync(folder+'/'+title+'.bbmodel',JSON.stringify(project,null,2)+'\n');
}
fs.writeFileSync(root+'/blockstates/'+id+'_crop.json',JSON.stringify({multipart:states},null,2)+'\n');
console.log(id+': eight growth stages, including existing ripe model path');
