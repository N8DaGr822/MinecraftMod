const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

// Authoring coordinates: blocks, Y up, facing negative Z. Exports use GeckoLib's mirrored X convention.
class Rig {
  constructor(entry) {
    this.entry = entry;
    this.bones = new Map(); this.nodes = new Map();
    this.model = {meta:{format_version:'5.0',model_format:'geckolib_model',box_uv:true},name:entry.id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(' '),model_identifier:entry.id,
      geckolib_modid:'darkspawn',geckolib_model_type:'Entity',resolution:{width:2048,height:2048},elements:[],groups:[],outliner:[],animations:[],textures:[]};
  }
  uuid(key) { const h=crypto.createHash('sha256').update(this.entry.id+'/'+key).digest('hex'); return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`; }
  bone(name, origin=[0,0,0], parent) {
    if(this.bones.has(name))throw Error('Duplicate bone '+name);
    const g={name,origin:origin.map(v=>v*16),rotation:[0,0,0],uuid:this.uuid('bone/'+name),export:true,mirror_uv:false,isOpen:true,visibility:true,autouv:0};
    this.model.groups.push(g);this.bones.set(name,g);
    const node={uuid:g.uuid,isOpen:true,children:[]};this.nodes.set(name,node);
    (parent?this.nodes.get(parent).children:this.model.outliner).push(node);return name;
  }
  box(name,parent,x,y,z,w,h,d,material=0) {
    const from=[x-w/2,y,z-d/2].map(v=>v*16),to=[x+w/2,y+h,z+d/2].map(v=>v*16);
    const size=to.map((v,i)=>v-from[i]),i=this.model.elements.length;
    const tile=[(material%2)*1024,Math.floor(material/2)*1024],uv=[tile[0]+12+i%7*3,tile[1]+12+i%5*7];
    if(size.some(v=>v<=0)||uv[0]+2*(size[0]+size[2])>=tile[0]+1012||uv[1]+size[1]+size[2]>=tile[1]+1012)throw Error('Cube exceeds material tile: '+name);
    const c={name,box_uv:true,rescale:false,locked:false,light_emission:0,render_order:'default',from,to,autouv:0,color:material,origin:[...this.bones.get(parent).origin],rotation:[0,0,0],uv_offset:uv,
      faces:Object.fromEntries(['north','east','south','west','up','down'].map(f=>[f,{uv:[0,0,0,0],texture:0}])),type:'cube',uuid:this.uuid('cube/'+name)};
    this.model.elements.push(c);this.nodes.get(parent).children.push(c.uuid);return c;
  }
  clip(name,length,loop,tracks=[]) {
    const channels=new Map();
    // Full poses prevent stale limbs when an attack, death, or warning interrupts another clip.
    for(const g of this.model.groups)for(const ch of ['rotation','position','scale'])channels.set(g.name+'/'+ch,[[0,ch==='scale'?[1,1,1]:[0,0,0]]]);
    for(const [bone,ch,frames] of tracks){if(!this.bones.has(bone))throw Error('Unknown animation bone '+bone);channels.set(bone+'/'+ch,frames);}
    const animators={};
    for(const [key,frames]of channels){const [bone,channel]=key.split('/'),g=this.bones.get(bone),a=animators[g.uuid]??={name:bone,type:'bone',keyframes:[]};
      for(const [time,values]of frames)a.keyframes.push({channel,uuid:this.uuid(name+'/'+key+'/'+time),time,color:-1,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((axis,i)=>[axis,String(values[i])]))]});}
    this.model.animations.push({uuid:this.uuid('clip/'+name),name:`animation.${this.entry.id}.${name}`,loop,override:false,length,snapping:20,animators});
  }
  fitHeight() {
    const min=Math.min(...this.model.elements.map(c=>c.from[1])),max=Math.max(...this.model.elements.map(c=>c.to[1]));
    const scale=this.entry.height*16/(max-min);
    for(const c of this.model.elements){c.from[1]=(c.from[1]-min)*scale;c.to[1]=(c.to[1]-min)*scale;c.origin[1]=(c.origin[1]-min)*scale;}
    for(const g of this.model.groups)g.origin[1]=(g.origin[1]-min)*scale;
  }
  save(atlas) {
    const e=this.entry,png=fs.readFileSync(atlas),folder=`art/blockbench/${e.id}`,base=e.id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join('');
    fs.mkdirSync(folder,{recursive:true});
    this.model.textures=[{path:'',name:e.id+'.png',folder:'entity',namespace:'darkspawn',id:'0',uuid:this.uuid('texture'),width:png.readUInt32BE(16),height:png.readUInt32BE(20),uv_width:2048,uv_height:2048,particle:false,use_as_default:true,mode:'bitmap',saved:true,source:'data:image/png;base64,'+png.toString('base64')}];
    const extent=Math.max(e.width,e.height)*2.5;
    this.model.visible_box=[extent,extent,e.height/2];
    this.model.editor_state={save_path:'',export_path:'',saved:true,mode:'edit',tool:'move_tool',previews:{main:{position:[e.height*22,e.height*17,-e.height*26],target:[0,e.height*8,0],projection:'perspective'}}};
    fs.writeFileSync(`${folder}/${base}.bbmodel`,JSON.stringify(this.model,null,2)+'\n');
    fs.copyFileSync(atlas,`src/main/resources/assets/darkspawn/textures/entity/${e.id}.png`);
    exportModel(this.model);
    return {id:e.id,cubes:this.model.elements.length,bones:this.model.groups.length,clips:this.model.animations.length};
  }
}

function exportModel(model) {
  const id=model.model_identifier,byCube=new Map(model.elements.map(c=>[c.uuid,c])),byGroup=new Map(model.groups.map(g=>[g.uuid,g])),bones=[];
  const flip=v=>[-v[0],v[1],v[2]],rot=v=>[-v[0],-v[1],v[2]];
  function visit(node,parent){const g=byGroup.get(node.uuid),b={name:g.name,pivot:flip(g.origin)};if(parent)b.parent=parent;
    if(g.rotation?.some(v=>v))b.rotation=rot(g.rotation);
    const cubes=node.children.filter(c=>typeof c==='string').map(c=>byCube.get(c));
    if(cubes.length)b.cubes=cubes.map(c=>{if(!c.box_uv)throw Error('Use Box UV for this exporter: '+c.name);const out={origin:[-c.to[0],c.from[1],c.from[2]],size:c.to.map((v,i)=>v-c.from[i]),uv:c.uv_offset};if(c.rotation?.some(v=>v)){out.pivot=flip(c.origin);out.rotation=rot(c.rotation);}return out;});
    bones.push(b);for(const child of node.children)if(typeof child!=='string')visit(child,g.name);
  }
  model.outliner.forEach(n=>visit(n));
  const [width,height,offset]=model.visible_box;
  const geometry={format_version:'1.12.0','minecraft:geometry':[{description:{identifier:'geometry.'+id,texture_width:model.resolution.width,texture_height:model.resolution.height,visible_bounds_width:width,visible_bounds_height:height,visible_bounds_offset:[0,offset,0]},bones}]};
  const animations={format_version:'1.8.0',geckolib_format_version:2,animations:{}};
  for(const a of model.animations){const out={animation_length:a.length,bones:{}};if(a.loop==='loop')out.loop=true;else if(a.loop==='hold')out.loop='hold_on_last_frame';
    for(const animator of Object.values(a.animators)){const b=out.bones[animator.name]={};for(const k of animator.keyframes){let values=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));if(values.some(v=>!Number.isFinite(v)))throw Error('Non-numeric keyframe in '+a.name);if(k.channel==='rotation')values=rot(values);else if(k.channel==='position')values=flip(values);(b[k.channel]??={})[String(k.time)]=values;}}
    animations.animations[a.name]=out;
  }
  for(const [file,data]of [[`models/entity/${id}.geo.json`,geometry],[`animations/entity/${id}.animation.json`,animations]])fs.writeFileSync(path.join('src/main/resources/assets/darkspawn/geckolib',file),JSON.stringify(data,null,2)+'\n');
}
const rotation=(bone,frames)=>[bone,'rotation',frames];
const position=(bone,frames)=>[bone,'position',frames.map(([t,v])=>[t,v.map(n=>n*16)])];
const wave=(bone,axis,amount,length,base=0,phase=0)=>rotation(bone,[0,.25,.5,.75,1].map(t=>[t*length,[0,0,0].map((v,i)=>i===axis?base+Math.sin(t*Math.PI*2+phase)*amount:0)]));
module.exports={Rig,exportModel,rotation,position,wave};
if(require.main===module){const file=process.argv[2];if(!file)throw Error('Usage: node tools/assets/rig.cjs path/to/Creature.bbmodel');const model=JSON.parse(fs.readFileSync(file,'utf8'));exportModel(model);const match=model.textures[0].source.match(/^data:image\/png;base64,(.+)$/);if(!match)throw Error('Project must embed a PNG texture');fs.writeFileSync(`src/main/resources/assets/darkspawn/textures/entity/${model.model_identifier}.png`,Buffer.from(match[1],'base64'));}
