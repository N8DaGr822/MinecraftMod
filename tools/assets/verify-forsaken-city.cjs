// Layout QA: Traverse the authored voxel grid with walking, ladders and a one-block-gap jump.
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),city=JSON.parse(fs.readFileSync(path.join(root,'art/structures/forsaken_city.json')));
const blocks=new Map(city.blocks.map(b=>[b.pos.join(','),city.palette[b.state].Name.split(':')[1]]));
const block=(x,y,z)=>blocks.get([x,y,z].join(','))||'air';
const pass=(x,y,z)=>['air','ladder','oak_sign'].includes(block(x,y,z));
const clear=(x,y,z)=>x>=0&&x<64&&z>=0&&z<80&&y>=1&&y<23&&pass(x,y,z)&&pass(x,y+1,z);
const ladder=(x,y,z)=>block(x,y,z)==='ladder'||block(x,y-1,z)==='ladder';
const stand=(x,y,z)=>clear(x,y,z)&&(!pass(x,y-1,z)||ladder(x,y,z));
const queue=[[31,4,0]],seen=new Set([queue[0].join(',')]);
function offer(x,y,z){const key=[x,y,z].join(',');if(stand(x,y,z)&&!seen.has(key)){seen.add(key);queue.push([x,y,z]);}}
for(let n=0;n<queue.length;n++){
  const [x,y,z]=queue[n];
  for(const [dx,dz]of [[1,0],[-1,0],[0,1],[0,-1]]){
    for(const dy of [0,1,-1,-2,-3])if((dy!==1||pass(x,y+2,z))&&clear(x+dx,y+Math.max(0,dy),z+dz))offer(x+dx,y+dy,z+dz);
    if(clear(x+dx,y,z+dz)&&pass(x,y+2,z))offer(x+2*dx,y,z+2*dz);
  }
  if(ladder(x,y,z)){offer(x,y+1,z);offer(x,y-1,z);}
}
for(const cache of city.caches){const [x,y,z]=cache.pos;if(![[1,0],[-1,0],[0,1],[0,-1]].some(([dx,dz])=>seen.has([x+dx,y,z+dz].join(','))))throw Error('Unreachable cache: '+JSON.stringify(cache));}
for(let z=0;z<80;z++)if(!seen.has([31,4,z].join(',')))throw Error('Blocked main escape street at '+z);
if(!seen.has('49,16,39')||!seen.has('49,16,37'))throw Error('Rooftop gap does not connect both districts');
console.log(`PASS: ${city.caches.length} caches reachable, continuous escape street, rooftop crossing and tunnel; ${seen.size} reachable positions.`);
const plan=city.buildings.map(b=>`<rect x="${b.origin[0]*7}" y="${b.origin[2]*7}" width="168" height="168" fill="${b.district==='infested'?'#65483f':'#3d555a'}" stroke="#b7beb0"/><text x="${(b.origin[0]+12)*7}" y="${(b.origin[2]+11)*7}" text-anchor="middle" fill="#f4eddb" font-size="12">${b.name}</text>`).join('');
const pins=city.caches.map((c,i)=>`<g class="cache" data-y="${c.pos[1]}"><circle cx="${c.pos[0]*7+3}" cy="${c.pos[2]*7+3}" r="8" fill="#edba5a"/><text x="${c.pos[0]*7+3}" y="${c.pos[2]*7+7}" text-anchor="middle" font-size="10" fill="#102126">${i+1}</text><title>${c.table} cache · local ${c.pos.join(', ')}</title></g>`).join('');
const html=`<!doctype html><meta charset="utf-8"><title>The Forsaken · city layout</title><style>body{background:#101c20;color:#eee9d9;font:16px system-ui;margin:32px}main{display:flex;gap:32px;flex-wrap:wrap}svg{background:#758078;max-width:100%;height:auto}aside{max-width:420px}p,li{line-height:1.6;color:#bfcac6}button{background:#314b52;color:white;border:1px solid #758e91;padding:8px 16px;margin:4px}h1{font-size:32px}strong{color:#edba5a}</style><h1>The Forsaken</h1><p>Authored city slice · 64 × 80 blocks · layout preview, not an in-game screenshot</p><main><svg width="448" height="560" viewBox="0 0 448 560"><rect x="203" width="42" height="560" fill="#263035"/><rect y="238" width="448" height="56" fill="#263035"/>${plan}<path d="M224 0V560" stroke="#d7d1a1" stroke-width="2" stroke-dasharray="12 12"/><path d="M0 297H448" stroke="#e3b247" stroke-width="5" stroke-dasharray="7 7"/><g id="roof"><path d="M91 140H343V448" stroke="#81dbe0" stroke-width="9" fill="none" opacity=".85"/><path d="M336 269H357" stroke="#1c282e" stroke-width="10"/></g><path id="tunnel" d="M63 189V350" stroke="#b6a4d9" stroke-width="5" stroke-dasharray="6 5"/>${pins}</svg><aside><h2>North → south expedition</h2><p>The landing sits just north of the city. The central street remains an open return route.</p><p><strong>Abandoned:</strong> housing and the supply exchange provide ordinary provisions and repair materials.</p><p><strong>Infested:</strong> the marked quarantine line leads into hospital wards and a research annex. Four permanent nests now threaten the infested district. Destroy them to claim cores and suppress district reinforcements for five minutes.</p><p><span style="color:#81dbe0">Cyan:</span> rooftop alternative with one broken span. <span style="color:#b6a4d9">Violet:</span> maintenance tunnel. Gold markers are finite loot caches.</p><button onclick="filter('all')">All caches</button><button onclick="filter('ground')">Ground / tunnel</button><button onclick="filter('upper')">Upper / roof</button><ol>${city.caches.map(c=>`<li>${c.table} · ${c.pos[1]===16?'roof':c.pos[1]===10?'upper floor':c.pos[1]===1?'tunnel':'ground floor'}</li>`).join('')}</ol><p>Automated grid check: all ten caches reachable from the north approach. Native collision and atmosphere still require live playtesting.</p></aside></main><script>function filter(mode){document.querySelectorAll('.cache').forEach(g=>g.style.opacity=mode==='all'||(mode==='upper')===(+g.dataset.y>=10)?1:.12)}</script>`;
const out=path.join(root,'art/structures/forsaken-city-preview.html');fs.writeFileSync(out,html);console.log(out);
