const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const assets=path.resolve(__dirname,'../../src/main/resources/assets/darkspawn');
http.createServer((req,res)=>{
 let url;try{url=decodeURIComponent(new URL(req.url,'http://localhost').pathname);}catch{res.writeHead(400).end();return;}
 let file;
 if(url==='/')file=path.join(__dirname,'preview.html');
 else if(url==='/roster.json')file=path.join(__dirname,'roster.json');
 else if(url==='/sounds.json'||/^\/sounds\/[a-z0-9_/]+\.ogg$/.test(url))file=path.resolve(assets,'.'+url);
 else{res.writeHead(404).end();return;}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404).end();return;}res.setHeader('Content-Type',file.endsWith('.ogg')?'audio/ogg':file.endsWith('.json')?'application/json':'text/html');res.setHeader('Cache-Control','no-store');res.end(data);});
}).listen(8767,'127.0.0.1',()=>console.log('Darkspawn audio workshop: http://127.0.0.1:8767/'));
