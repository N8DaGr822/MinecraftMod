// Orthographic geometry review; this does not reproduce Minecraft rendering.
const fs=require('node:fs'), zlib=require('node:zlib');
const ids=['woolly_rhino','flying_squirrel','poptart_cat','dragon_mount','winged_unicorn','riding_golem','sasquatch','cerberus'];
const W=1280,H=640,raw=Buffer.alloc(H*(W*3+1));
for(let y=0;y<H;y++)for(let x=0;x<W;x++)for(let c=0;c<3;c++)raw[y*(W*3+1)+1+x*3+c]=[25,34,42][c];
function pixel(x,y,color){if(x<0||x>=W||y<0||y>=H)return;for(let c=0;c<3;c++)raw[y*(W*3+1)+1+x*3+c]=Math.round(color[c]);}
function polygon(points,color){
  const min=Math.max(0,Math.ceil(Math.min(...points.map(p=>p[1])))),max=Math.min(H-1,Math.floor(Math.max(...points.map(p=>p[1]))));
  for(let y=min;y<=max;y++){
    const hits=[];for(let i=0;i<points.length;i++){const a=points[i],b=points[(i+1)%points.length];if((a[1]<=y&&b[1]>y)||(b[1]<=y&&a[1]>y))hits.push(a[0]+(y-a[1])*(b[0]-a[0])/(b[1]-a[1]));}
    hits.sort((a,b)=>a-b);for(let i=0;i+1<hits.length;i+=2)for(let x=Math.ceil(hits[i]);x<=hits[i+1];x++)pixel(x,y,color);
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
  for(const cube of model.elements){
    const [x,y,z]=cube.from,[X,Y,Z]=cube.to;
    const pts=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]].map(rotate);all.push(...pts);
    const u=Math.min(127,Math.floor(cube.uv_offset[0]+(Z-z)+(X-x)/2)),v=Math.min(127,Math.floor(cube.uv_offset[1]+(Z-z)+(Y-y)/2));
    const col=Array.from(tex.subarray(v*385+1+u*3,v*385+4+u*3));
    [[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]].forEach((f,i)=>{const p=f.map(j=>pts[j]);faces.push({p,depth:p.reduce((s,a)=>s+a[2],0)/4,color:col.map(c=>c*[.82,.75,.7,.92,1,.6][i])});});
  }
  const minX=Math.min(...all.map(p=>p[0])),maxX=Math.max(...all.map(p=>p[0])),minY=Math.min(...all.map(p=>p[1])),maxY=Math.max(...all.map(p=>p[1]));
  const scale=Math.min(280/(maxX-minX),260/(maxY-minY));
  const cx=index%4*320+160,cy=Math.floor(index/4)*320+160;
  for(const f of faces.sort((a,b)=>b.depth-a.depth))polygon(f.p.map(p=>[cx+(p[0]-(minX+maxX)/2)*scale,cy-(p[1]-(minY+maxY)/2)*scale]),f.color);
});
function crc(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(type,data){const n=Buffer.from(type),len=Buffer.alloc(4),sum=Buffer.alloc(4);len.writeUInt32BE(data.length);sum.writeUInt32BE(crc(Buffer.concat([n,data])));return Buffer.concat([len,n,data,sum]);}
const header=Buffer.alloc(13);header.writeUInt32BE(W);header.writeUInt32BE(H,4);header[8]=8;header[9]=2;
fs.writeFileSync('build/mount-contact-sheet.png',Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]));
console.log('Rendered build/mount-contact-sheet.png. Rows: rhino, squirrel, cat, dragon; unicorn, golem, Sasquatch, Cerberus.');
