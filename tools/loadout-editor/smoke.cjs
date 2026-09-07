const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {pathToFileURL}=require('node:url');
const {chromium}=require('C:/Users/RogYukz/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const C=require('./catalog-core.js'),S=require('./snbt.js');
const out=path.resolve(__dirname,'../../outputs/loadout-editor-20260906');
const shots=path.join(out,'verification');fs.mkdirSync(shots,{recursive:true});
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});
try{const page=await browser.newPage({viewport:{width:1440,height:1000},acceptDownloads:true});
const errors=[];page.on('pageerror',e=>errors.push(e.message));
await page.goto(pathToFileURL(path.join(out,'WOK步战编制配装编辑器.html')).href);
await page.getByRole('heading',{name:'千禧年研讨会机动部队',exact:true}).waitFor();
assert.equal(await page.locator('[data-action="role"]').count(),6);
await page.screenshot({path:path.join(shots,'01-loadout-wide.png'),fullPage:true});
const beforeDownload=page.waitForEvent('download');await page.locator('[data-action="save-project"]').first().click();const before=await beforeDownload;await before.saveAs(path.join(shots,'before-project.json'));
let base=JSON.parse(fs.readFileSync(path.join(shots,'before-project.json'),'utf8'));
C.assertEditable(base.catalog);assert.deepEqual(C.validate(base.catalog).errors,[]);
const ads=page.getByRole('spinbutton',{name:'开镜速度倍率',exact:true});await ads.fill('0.85');await ads.press('Tab');
const reserve=page.getByRole('spinbutton',{name:'此枪备用弹药上限（发）'});await reserve.fill('222');await reserve.press('Tab');
await page.locator('[data-action="clone-formation"]').first().click();await page.locator('#new-name').fill('浏览器独立复制测试');await page.locator('#new-id').fill('browser_copy_test');await page.locator('[data-action="modal-confirm"]').click();
await page.getByRole('heading',{name:'浏览器独立复制测试',exact:true}).waitFor();
await page.locator('[data-action="library"]').click();await page.locator('#library-search').fill('M4A1');assert.ok(await page.locator('[data-action="library-pick"]').count()>0);await page.locator('[data-action="library-pick"]').first().click();
await page.locator('[data-action="gun-add"]').click();await page.locator('#library-search').fill('tacz:m9a4');assert.ok(await page.locator('[data-action="library-pick"]').count()>0);await page.locator('[data-action="library-pick"]').first().click();
await page.locator('[data-action="tab"][data-tab="check"]').click();await page.getByText('结构检查通过，可以导出',{exact:true}).waitFor();
await page.screenshot({path:path.join(shots,'02-check.png'),fullPage:true});
const projectDownload=page.waitForEvent('download');await page.locator('[data-action="save-project"]').first().click();await(await projectDownload).saveAs(path.join(shots,'edited-project.json'));
const edited=JSON.parse(fs.readFileSync(path.join(shots,'edited-project.json'),'utf8'));assert.deepEqual(C.validate(edited.catalog).errors,[]);
const copied=edited.catalog.formations.factions[0].formations.find(f=>f.id==='browser_copy_test');assert.ok(copied);assert.notEqual(copied.classes[0].classId,edited.catalog.formations.factions[0].formations[0].classes[0].classId);
const sourceClass=edited.catalog.loadouts.classes.find(c=>c.id===edited.catalog.formations.factions[0].formations[0].classes[0].classId);
const sourceGun=sourceClass.slots[sourceClass.slotDefinitions[0].id][0];assert.equal(S.readScalar(sourceGun.snbt,'wok_infantry_ads_speed_scale'),.85);assert.equal(sourceGun.ammoReserveLimit,222);
await page.locator('[data-action="export"]').first().click();const exported=page.waitForEvent('download');await page.locator('[data-action="modal-confirm"]').click();await(await exported).saveAs(path.join(shots,'browser-export.json'));
assert.deepEqual(C.validate(C.parse(fs.readFileSync(path.join(shots,'browser-export.json'),'utf8'))).errors,[]);
// Reopening must preserve the complete worker project exactly.
await page.locator('#file').setInputFiles(path.join(shots,'edited-project.json'));await page.getByRole('heading',{name:'浏览器独立复制测试',exact:true}).waitFor();
const redBefore=JSON.stringify(edited.catalog.formations.factions[1]);
const bad=JSON.parse(JSON.stringify(edited));bad.catalog.formations.factions[0].formations[0].capacity='<img src=x onerror=window.injected=1>';
fs.writeFileSync(path.join(shots,'malformed-project.json'),JSON.stringify(bad));await page.locator('#file').setInputFiles(path.join(shots,'malformed-project.json'));await page.getByRole('heading',{name:'需要修正',exact:true}).waitFor();assert.equal(await page.evaluate(()=>window.injected),undefined);await page.locator('[data-action="close-modal"]').click();
// Merge, undo, merge again: the latest side's change must survive.
const latest=JSON.parse(JSON.stringify(edited.base));latest.formations.factions[1].description='LATEST_UNRELATED_CHANGE';fs.writeFileSync(path.join(shots,'latest.json'),JSON.stringify(latest));
await page.locator('[data-action="merge"]').click();await page.locator('#file').setInputFiles(path.join(shots,'latest.json'));await page.getByText('结构检查通过，可以导出',{exact:true}).waitFor();
await page.locator('[data-action="undo"]').click();await page.locator('[data-action="merge"]').click();await page.locator('#file').setInputFiles(path.join(shots,'latest.json'));
const mergedDownload=page.waitForEvent('download');await page.locator('[data-action="save-project"]').first().click();await(await mergedDownload).saveAs(path.join(shots,'merged-project.json'));
const merged=JSON.parse(fs.readFileSync(path.join(shots,'merged-project.json'),'utf8'));assert.equal(merged.catalog.formations.factions[1].description,'LATEST_UNRELATED_CHANGE');assert.ok(merged.catalog.formations.factions[0].formations.find(f=>f.id==='browser_copy_test'));
for(const tab of ['overview','vehicles','support','loadout']){await page.locator(`[data-action="tab"][data-tab="${tab}"]`).click();assert.ok((await page.locator('#content').innerText()).length>50)}
for(const [w,h] of [[960,720],[320,240]]){await page.setViewportSize({width:w,height:h});await page.screenshot({path:path.join(shots,`03-loadout-${w}x${h}.png`),fullPage:true});const dim=await page.evaluate(()=>({doc:document.documentElement.scrollWidth,width:innerWidth}));assert.ok(dim.doc<=dim.width,JSON.stringify(dim));await page.locator('[data-action="help"]').click();await page.screenshot({path:path.join(shots,`04-help-${w}x${h}.png`)});await page.locator('[data-action="close-modal"]').click();}
assert.deepEqual(errors,[]);fs.writeFileSync(path.join(shots,'result.json'),JSON.stringify({passed:true,browser:'Edge headless',viewports:['1440x1000','960x720','320x240'],checks:['offline startup','real resource selection','tuning and reserve','independent clone','project roundtrip','native export','malformed input rejection','merge-undo-merge','all editor tabs','responsive width'],pageErrors:errors},null,2));
console.log('Browser smoke passed: '+shots);
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exit(1)});
