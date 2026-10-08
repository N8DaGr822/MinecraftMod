const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'../..');
http.createServer((req,res)=>{
 let requested;
 try {requested=decodeURIComponent(new URL(req.url,'http://localhost').pathname);}catch{res.writeHead(400).end();return;}
 const file=path.resolve(root,'.'+(requested==='/'?'/tools/assets/preview.html':requested));
 if(!file.startsWith(root+path.sep)||requested.split('/').some(p=>p.startsWith('.'))||!(requested==='/'||requested.startsWith('/tools/assets/')||requested.startsWith('/art/blockbench/'))){res.writeHead(403).end();return;}
 fs.readFile(file,(error,data)=>{
  if(error){res.writeHead(404).end('Missing asset');return;}
  res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.json':'application/json','.bbmodel':'application/json','.png':'image/png'})[path.extname(file)]||'text/plain');
  res.setHeader('Cache-Control','no-store');res.end(data);
 });
}).listen(8766,'127.0.0.1',()=>console.log('Asset preview: http://127.0.0.1:8766/'));
