// Orthographic textured source review; this does not reproduce Minecraft rendering or controller transitions.
const fs=require('node:fs'), zlib=require('node:zlib');
const attackReview=process.argv.includes('--attacks');
const ids=attackReview ? ['dragon_mount','woolly_rhino','giant_scorpion','crocodile'].flatMap(id=>[id,id,id,id]) : ['woolly_rhino','flying_squirrel','poptart_cat','dragon_mount','winged_unicorn','riding_golem','sasquatch','cerberus','leopard','seahorse','giant_scorpion','crocodile','giant_mole'];
const W=1280,H=1280,raw=Buffer.alloc(H*(W*3+1));
const depth=new Float64Array(W*H).fill(Infinity);
for(let y=0;y<H;y++)for(let x=0;x<W;x++)for(let c=0;c<3;c++)raw[y*(W*3+1)+1+x*3+c]=[25,34,42][c];
function pixel(x,y,color){if(x<0||x>=W||y<0||y>=H)return;for(let c=0;c<3;c++)raw[y*(W*3+1)+1+x*3+c]=Math.round(color[c]);}
function triangle(points,uv,tex,shade){
  const [a,b,c]=points,den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-8)return;
  for(let y=Math.max(0,Math.floor(Math.min(...points.map(p=>p[1]))));y<=Math.min(H-1,Math.ceil(Math.max(...points.map(p=>p[1]))));y++)
  for(let x=Math.max(0,Math.floor(Math.min(...points.map(p=>p[0]))));x<=Math.min(W-1,Math.ceil(Math.max(...points.map(p=>p[0]))));x++){
    const A=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,B=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,C=1-A-B;
    if(A<0||B<0||C<0)continue;
    const z=A*a[2]+B*b[2]+C*c[2],index=y*W+x;if(z>depth[index])continue;depth[index]=z;
    const u=Math.max(0,Math.min(127,Math.floor(A*uv[0][0]+B*uv[1][0]+C*uv[2][0]))),v=Math.max(0,Math.min(127,Math.floor(A*uv[0][1]+B*uv[1][1]+C*uv[2][1])));
    pixel(x,y,[0,1,2].map(k=>tex[v*385+1+u*3+k]*shade));
  }
}
ids.forEach((id,index)=>{
  const name=id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join('');
  const model=JSON.parse(fs.readFileSync(`art/blockbench/${id}/${name}.bbmodel`));
  const png=Buffer.from(model.textures[0].source.split(',')[1],'base64');let offset=8,idat=[];
  while(offset<png.length){const n=png.readUInt32BE(offset);if(png.toString('ascii',offset+4,offset+8)==='IDAT')idat.push(png.subarray(offset+8,offset+8+n));offset+=n+12;}
  const tex=zlib.inflateSync(Buffer.concat(idat));
  const yaw=-.6,pitch=.3;
  const rotate=([x,y,z])=>{const xx=x*Math.cos(yaw)+z*Math.sin(yaw),zz=-x*Math.sin(yaw)+z*Math.cos(yaw);return [xx,y*Math.cos(pitch)+zz*Math.sin(pitch),zz*Math.cos(pitch)-y*Math.sin(pitch)];};
  const faces=[],all=[];
  // Attack Review: Apply the same hierarchical bone offsets as the generated Java models.
  const groups=new Map(model.groups.map(g=>[g.uuid,g])), owners=new Map();
  function visit(nodes,parent){for(const node of nodes){if(typeof node==='string')owners.set(node,parent);else{owners.set(node.uuid,parent);visit(node.children,node.uuid);}}}
  visit(model.outliner,null);
  const clip=attackReview?model.animations.find(a=>a.name.endsWith('.attack')):null;
  const time=clip?clip.length*[0,.5,.9,1][index%4]:0;
  function channel(group,name){
    const frames=clip?.animators[group.uuid]?.keyframes.filter(k=>k.channel===name)??[];
    if(!frames.length)return [0,0,0];
    const a=frames.findLast(k=>k.time<=time)??frames[0],b=frames.find(k=>k.time>=time)??frames.at(-1);
    const mix=a.time===b.time?0:(time-a.time)/(b.time-a.time);
    return ['x','y','z'].map(axis=>Number(a.data_points[0][axis])*(1-mix)+Number(b.data_points[0][axis])*mix);
  }
  function pose(point,cube){
    if(!clip)return point;
    for(let parent=owners.get(cube);parent;parent=owners.get(parent)){
      const g=groups.get(parent),r=channel(g,'rotation').map(v=>v*Math.PI/180),shift=channel(g,'position');
      let [x,y,z]=point.map((v,i)=>v-g.origin[i]);
      [y,z]=[y*Math.cos(r[0])-z*Math.sin(r[0]),y*Math.sin(r[0])+z*Math.cos(r[0])];
      [x,z]=[x*Math.cos(r[1])+z*Math.sin(r[1]),-x*Math.sin(r[1])+z*Math.cos(r[1])];
      [x,y]=[x*Math.cos(r[2])-y*Math.sin(r[2]),x*Math.sin(r[2])+y*Math.cos(r[2])];
      point=[x,y,z].map((v,i)=>v+g.origin[i]+shift[i]);
    }
    return point;
  }
  for(const cube of model.elements){
    const [x,y,z]=cube.from,[X,Y,Z]=cube.to;
    const pts=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]].map(p=>rotate(pose(p,cube.uuid)));all.push(...pts);
    const [u,v]=cube.uv_offset,w=X-x,h=Y-y,d=Z-z;
    const rects=[[u+d,v+d,w,h],[u+2*d+w,v+d,w,h],[u,v+d,d,h],[u+d+w,v+d,d,h],[u+d,v,w,d],[u+d+w,v,w,d]];
    [[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]].forEach((f,i)=>{
      const [U,V,A,B]=rects[i];faces.push({p:f.map(j=>pts[j]),uv:[[U,V+B],[U+A,V+B],[U+A,V],[U,V]],shade:[.82,.75,.7,.92,1,.6][i]});
    });
  }
  const minX=Math.min(...all.map(p=>p[0])),maxX=Math.max(...all.map(p=>p[0])),minY=Math.min(...all.map(p=>p[1])),maxY=Math.max(...all.map(p=>p[1]));
  const scale=Math.min(280/(maxX-minX),260/(maxY-minY));
  const cx=index%4*320+160,cy=Math.floor(index/4)*320+160;
  for(const f of faces){const p=f.p.map(p=>[cx+(p[0]-(minX+maxX)/2)*scale,cy-(p[1]-(minY+maxY)/2)*scale,p[2]]);for(const t of [[0,1,2],[0,2,3]])triangle(t.map(i=>p[i]),t.map(i=>f.uv[i]),tex,f.shade);}
});
function crc(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(type,data){const n=Buffer.from(type),len=Buffer.alloc(4),sum=Buffer.alloc(4);len.writeUInt32BE(data.length);sum.writeUInt32BE(crc(Buffer.concat([n,data])));return Buffer.concat([len,n,data,sum]);}
const header=Buffer.alloc(13);header.writeUInt32BE(W);header.writeUInt32BE(H,4);header[8]=8;header[9]=2;
fs.writeFileSync(attackReview?'build/mount-attack-contact-sheet.png':'build/mount-contact-sheet.png',Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]));
console.log(attackReview?'Rendered build/mount-attack-contact-sheet.png. Rows: dragon, rhino, scorpion, crocodile; columns: strike, half recovery, late recovery, neutral.':'Rendered build/mount-contact-sheet.png. Rows: rhino, squirrel, cat, dragon; unicorn, golem, Sasquatch, Cerberus; leopard, seahorse, scorpion, crocodile; giant mole.');
