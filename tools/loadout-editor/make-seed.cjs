// Read-only source snapshots; creates a visibly labelled demonstration catalog.
const fs=require('node:fs'),path=require('node:path');
const C=require('./catalog-core.js');
const src=path.resolve(__dirname,'../../dist/WOK步战测试包-配装-新编制-载具枪械平衡-20260905/config/wok_infantry');
const formations=JSON.parse(fs.readFileSync(path.join(src,'formations.json'),'utf8'));
const loadouts=JSON.parse(fs.readFileSync(path.join(src,'loadouts.json'),'utf8'));
const academy=formations.factions.find(x=>x.id==='academy');
const mobile=academy.formations.find(x=>x.id==='millennium_seminar_mobile');
const caesar=formations.factions.find(x=>x.id==='caesar');
const red=caesar.formations.find(x=>x.id==='caesar_234_mechanized');
for(const [f,fm] of [[academy,mobile],[caesar,red]]){
 const fallback=f.formations.find(x=>x.capabilities);
 fm.icon??='';fm.capabilities??=C.clone(fallback.capabilities);
 fm.classes.forEach(r=>{r.displayName??='';r.allowedEntries={}});
 f.formations=[fm];
}
// A sample deliberately allows all real options in its own pools. It is not a
// repair of the live configuration and must never overwrite that configuration.
const ids=new Set(formations.factions.flatMap(f=>f.formations.flatMap(fm=>fm.classes.map(r=>r.classId))));
loadouts.classes=loadouts.classes.filter(c=>ids.has(c.id));
const catalog={format:C.constants.FORMAT,version:1,exportedAt:'2026-09-06T00:00:00Z',formations,loadouts};
const r=C.validate(catalog);if(r.errors.length)throw Error(r.errors.join('\n'));
fs.writeFileSync(path.join(__dirname,'seed.js'),'window.WOK_EDITOR_SEED = '+JSON.stringify({source:'演示目录 · 2026-09-05 测试快照的两套编制，非服务器完整配置',catalog})+';\n');
console.log(JSON.stringify({factions:formations.factions.length,classes:loadouts.classes.length,errors:r.errors.length,warnings:r.warnings.length}));
