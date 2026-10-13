// Render the existing gallery's canvas code without a browser. No raster edits.
const fs=require('node:fs'),vm=require('node:vm'),zlib=require('node:zlib');
const png=fs.readFileSync('src/main/resources/assets/darkspawn/textures/block/cuisine_atlas.png');
const width=png.readUInt32BE(16),height=png.readUInt32BE(20),channels=png[25]===6?4:3;
if(png[24]!==8||![2,6].includes(png[25])||png[28]!==0)throw Error('Expected non-interlaced RGB/RGBA atlas');
let chunks=[];
for(let o=8;o<png.length;){const n=png.readUInt32BE(o);if(png.toString('ascii',o+4,o+8)==='IDAT')chunks.push(png.subarray(o+8,o+8+n));o+=n+12;}
const packed=zlib.inflateSync(Buffer.concat(chunks)),stride=width*channels,decoded=Buffer.alloc(height*stride),rgba=Buffer.alloc(width*height*4);
const paeth=(a,b,c)=>{const p=a+b-c,da=Math.abs(p-a),db=Math.abs(p-b),dc=Math.abs(p-c);return da<=db&&da<=dc?a:db<=dc?b:c;};
for(let y=0;y<height;y++)for(let x=0;x<stride;x++){
 const filter=packed[y*(stride+1)],a=x>=channels?decoded[y*stride+x-channels]:0,b=y?decoded[(y-1)*stride+x]:0,c=y&&x>=channels?decoded[(y-1)*stride+x-channels]:0;
 const predictor=[0,a,b,Math.floor((a+b)/2),paeth(a,b,c)][filter];if(predictor===undefined)throw Error('PNG filter');
 decoded[y*stride+x]=(packed[y*(stride+1)+1+x]+predictor)&255;
}
for(let i=0;i<width*height;i++){for(let c=0;c<3;c++)rgba[i*4+c]=decoded[i*channels+c];rgba[i*4+3]=channels===4?decoded[i*4+3]:255;}
const renders=[],labels=[];
const document={title:'',querySelector:()=>({append(){}}),createElement(tag){
 if(tag==='canvas')return {getContext:()=>({drawImage(){},getImageData:()=>({data:rgba}),createImageData:(w,h)=>({data:new Uint8ClampedArray(w*h*4)}),putImageData(p){renders.push(p.data);}})};
 if(tag==='figcaption')return {set textContent(v){labels.push(v);}};
 return {append(){}};
}};
class Image{constructor(){this.width=width;this.height=height;}set onload(fn){fn();}}
const html=fs.readFileSync('build/art-review/cuisine.html','utf8');
vm.runInNewContext(html.match(/<script>([\s\S]*?)<\/script>/)[1],{document,Image,Float32Array,Math,Infinity});
const W=960,H=Math.ceil(renders.length/4)*210,raw=Buffer.alloc(H*(W*3+1));
for(let y=0;y<H;y++)for(let x=0;x<W;x++){
 const index=Math.floor(y/210)*4+Math.floor(x/240),p=renders[index],offset=((y%210)*240+x%240)*4;
 for(let c=0;c<3;c++)raw[y*(W*3+1)+1+x*3+c]=p&&p[offset+3]?p[offset+c]:[25,34,42][c];
}
function crc(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(type,data){const n=Buffer.from(type),len=Buffer.alloc(4),sum=Buffer.alloc(4);len.writeUInt32BE(data.length);sum.writeUInt32BE(crc(Buffer.concat([n,data])));return Buffer.concat([len,n,data,sum]);}
const header=Buffer.alloc(13);header.writeUInt32BE(W);header.writeUInt32BE(H,4);header[8]=8;header[9]=2;
fs.writeFileSync('build/art-review/cuisine-contact-sheet.png',Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]));
console.log('Rows, left to right:\n'+labels.reduce((rows,label,i)=>{if(i%4===0)rows.push([]);rows.at(-1).push(label);return rows;},[]).map(row=>row.join(', ')).join('\n'));
