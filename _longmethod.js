const fs=require('fs'),path=require('path');
function walk(d){const o=[];for(const f of fs.readdirSync(d)){const p=path.join(d,f);const st=fs.statSync(p);if(st.isDirectory())o.push(...walk(p));else if(f.endsWith('.java'))o.push(p);}return o;}
function clean(l){let s=l.replace(/\/\/.*$/,'');s=s.replace(/"(?:\\.|[^"\\])*"/g,'""');s=s.replace(/'(?:\\.|[^'\\])*'/g,"''");return s;}
const out=[];
for(const f of walk('src/main/java')){
  const raw=fs.readFileSync(f,'utf8').split(/\r?\n/);const lines=raw.map(clean);
  for(let i=0;i<lines.length;i++){
    const ln=lines[i];
    if(!/^\s*(public|private|protected)\s+[\w<>\[\],.\s?]+\s+\w+\s*\(/.test(ln))continue;
    if(/\b(new|class|interface|enum)\b/.test(ln))continue;
    let d=0,started=false;
    for(let k=i;k<lines.length;k++){for(const ch of lines[k]){if(ch==='{'){started=true;d++;}else if(ch==='}')d--;}if(started&&d===0){const len=k-i+1;if(len>80){const nm=ln.match(/\s(\w+)\s*\(/);out.push({f,line:i+1,len,name:nm?nm[1]:'?'});}break;}}
  }
}
out.sort((a,b)=>b.len-a.len);
console.log('方法>80行: '+out.length);
for(const o of out)console.log(o.len+'行  '+o.f+'  '+o.name+'() @'+o.line);
