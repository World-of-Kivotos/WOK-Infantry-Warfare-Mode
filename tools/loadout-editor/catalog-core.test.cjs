'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const C = require('./catalog-core.js');

function fixture() {
  const policy = {enabled:false,maxActive:0,squadLeaderCanPlace:false,commanderCanPlace:false,maxHealth:0,placementCooldownSeconds:0,replacementCooldownSeconds:0,destructionCooldownSeconds:0};
  const formation = {id:'infantry',displayName:'步兵营',description:'',icon:'',category:'infantry',enabled:true,capacity:8,
    capabilities:{outpost:C.clone(policy),rally:C.clone(policy),respawn:{delaySeconds:-1,mobileSpawnVehicleIds:['truck_one']},support:{mode:'none',allowList:[]}},
    classes:[{classId:'blue_rifleman',displayName:'步枪兵',squadLimit:8,allowedEntries:{primary:['rifle']}}],
    squads:[{callsign:'alpha',displayName:'一班',capacity:8,classLimits:{blue_rifleman:8}}],
    vehicles:[{id:'truck_one',displayName:'运输车',entityId:'superbwarfare:truck',offsetX:0,offsetY:0,offsetZ:10,yaw:0,replenishmentCooldownSeconds:60}]};
  return {format:'wok_infantry_catalog',version:1,exportedAt:'2026-09-06T00:00:00Z',formations:{version:2,factions:[{id:'blue_force',displayName:'蓝方',description:'',battleSide:'blue',enabled:true,maxPlayers:40,formations:[formation]}]},
    loadouts:{version:3,classes:[{id:'blue_rifleman',displayName:'步枪兵',enabled:true,squadLimit:8,slotDefinitions:[{id:'primary',displayName:'主武器',target:'HOTBAR_1',required:true}],slots:{primary:[{id:'rifle',displayName:'M4A1',itemId:'tacz:modern_kinetic_gun',count:1,snbt:'{GunId:"tacz:m4a1",wok_infantry_ads_speed_scale:0.60f}',ammoReserveLimit:210}]}}]}};
}

test('actual archive validates, exports losslessly, and does not mutate input', () => {
  const source = fixture(), before = JSON.stringify(source);
  assert.deepEqual(C.validate(source).errors, []);
  const out = C.exportArchive(source);
  assert.equal(out.format, 'wok_infantry_catalog');
  assert.deepEqual(out.formations, source.formations);
  assert.deepEqual(out.loadouts, source.loadouts);
  assert.equal(JSON.stringify(source), before);
  assert.deepEqual(C.parse(JSON.stringify(out)), out);
});

test('formation copy remaps pools and squad quotas, with independent NBT and whitelists', () => {
  const c = fixture(), original = C.clone(c);
  const copy = C.cloneFormation(c, 'blue_force', 'infantry', 'mechanized', '机械化营');
  const newClass = copy.classes[0].classId;
  assert.notEqual(newClass, 'blue_rifleman');
  assert.deepEqual(copy.squads[0].classLimits, {[newClass]:8});
  assert.deepEqual(copy.capabilities.respawn.mobileSpawnVehicleIds, ['truck_one']);
  assert.deepEqual(C.validate(c).errors, []);
  const copiedPool = c.loadouts.classes.find(p => p.id === newClass);
  copiedPool.slots.primary[0].snbt = '{GunId:"tacz:ak47"}';
  copiedPool.slots.primary[0].ammoReserveLimit = 300;
  copy.classes[0].allowedEntries.primary.push('extra');
  assert.deepEqual(c.loadouts.classes[0], original.loadouts.classes[0]);
  assert.deepEqual(c.formations.factions[0].formations[0], original.formations.factions[0].formations[0]);
});

test('failed copy never partially modifies source catalog', () => {
  const c = fixture(); c.formations.factions[0].formations[0].squads[0].classLimits.unknown = 1;
  const before = JSON.stringify(c);
  assert.throws(() => C.cloneFormation(c, 'blue_force', 'infantry', 'new_form', '新编制'), /未知职业/);
  assert.equal(JSON.stringify(c), before);
});

test('unknown fields survive clone/edit but block unsupported game export', () => {
  const c = fixture(); c.formations.factions[0].formations[0].futureSetting = {preserve:true};
  const copy = C.cloneFormation(c, 'blue_force', 'infantry', 'copy', '副本');
  assert.deepEqual(copy.futureSetting, {preserve:true});
  assert(C.validate(c).errors.some(e => e.includes('futureSetting')));
  assert.throws(() => C.exportArchive(c), /未知字段/);
});

test('strict parse catches duplicate keys, truncation, invalid numeric tokens and nesting', () => {
  assert.throws(() => C.parse('{"version":1,"version":2}'), /字段重复/);
  assert.throws(() => C.parse('{"a":[1,]}'));
  assert.throws(() => C.parse('{"a":01}'));
  assert.throws(() => C.parse('['.repeat(50) + '1' + ']'.repeat(50)), /48/);
  assert.deepEqual(C.parse('{"text":"引号\\\"和\\\\路径","a":[1,true,null]}'), {text:'引号"和\\路径',a:[1,true,null]});
});

test('bad class references, duplicate inventory targets, ammo bounds and scales are rejected', () => {
  const c = fixture(), pool = c.loadouts.classes[0];
  c.formations.factions[0].formations[0].classes[0].allowedEntries.primary = ['missing'];
  pool.slotDefinitions.push({id:'secondary',displayName:'副武器',target:'HOTBAR_1',required:false}); pool.slots.secondary=[];
  pool.slots.primary[0].ammoReserveLimit=4097;
  pool.slots.primary[0].snbt='{GunId:"tacz:m4a1",wok_infantry_damage_scale:99.0f}';
  const errors=C.validate(c).errors.join('\n');
  assert.match(errors,/不存在的装备/); assert.match(errors,/重复 target/); assert.match(errors,/4096/); assert.match(errors,/0.25–15.00/);
});

test('one battle side cannot be assigned to multiple public factions', () => {
  const c = fixture(), other=C.clone(c.formations.factions[0]); other.id='another';
  c.formations.factions.push(other);
  assert(C.validate(c).errors.some(e => e.includes('重复 battleSide')));
});

test('empty allow-list retains allow-all semantics; missing fields are errors', () => {
  const c=fixture(); c.formations.factions[0].formations[0].classes[0].allowedEntries.primary=[];
  assert(C.validate(c).warnings.some(e=>e.includes('空白名单')));
  delete c.loadouts.classes[0].slots.primary[0].ammoReserveLimit;
  assert(C.validate(c).errors.some(e=>e.includes('ammoReserveLimit')&&e.includes('缺少字段')));
});

test('three-way merge preserves unrelated latest edits and merges entry fields by stable IDs', () => {
  const base=fixture(), edited=C.clone(base), latest=C.clone(base);
  edited.loadouts.classes[0].slots.primary[0].ammoReserveLimit=300;
  latest.loadouts.classes[0].slots.primary[0].displayName='服务端已改名';
  C.cloneFormation(latest,'blue_force','infantry','latest_only','服务端新增');
  const snapshot=JSON.stringify([base,edited,latest]);
  const result=C.mergeCatalog(base,edited,latest);
  assert.deepEqual(result.conflicts,[]);
  assert.equal(result.catalog.loadouts.classes[0].slots.primary[0].ammoReserveLimit,300);
  assert.equal(result.catalog.loadouts.classes[0].slots.primary[0].displayName,'服务端已改名');
  assert.equal(result.catalog.formations.factions[0].formations[1].id,'latest_only');
  assert.equal(JSON.stringify([base,edited,latest]),snapshot);
});

test('same-field changes and deletion versus modification yield explicit merge conflicts', () => {
  const base=fixture(), edited=C.clone(base), latest=C.clone(base);
  edited.loadouts.classes[0].slots.primary[0].ammoReserveLimit=300;
  latest.loadouts.classes[0].slots.primary[0].ammoReserveLimit=400;
  assert(C.mergeCatalog(base,edited,latest).conflicts.some(x=>x.includes('ammoReserveLimit')));
  edited.loadouts.classes[0].slots.primary=[];
  assert(C.mergeCatalog(base,edited,latest).conflicts.some(x=>x.includes('删除')));
});

test('merge applies explicit deletions, preserves latest insertions and handles author reordering', () => {
  const base=fixture();
  const one=base.loadouts.classes[0].slots.primary[0];
  base.loadouts.classes[0].slots.primary.push({...C.clone(one),id:'second'},{...C.clone(one),id:'third'});
  const edited=C.clone(base),latest=C.clone(base);
  edited.loadouts.classes[0].slots.primary=[edited.loadouts.classes[0].slots.primary[2],edited.loadouts.classes[0].slots.primary[0]];
  latest.loadouts.classes[0].slots.primary.push({...C.clone(one),id:'latest_added'});
  const result=C.mergeCatalog(base,edited,latest);
  assert.deepEqual(result.conflicts,[]);
  assert.deepEqual(result.catalog.loadouts.classes[0].slots.primary.map(x=>x.id),['third','rifle','latest_added']);
});

test('shape gate rejects malformed nested content before UI state is replaced', () => {
  for (const damage of [
    c=>{c.formations.factions[0].formations[0].capabilities=null},
    c=>{delete c.formations.factions[0].formations[0].classes[0].allowedEntries},
    c=>{c.loadouts.classes[0].slots.primary[0]=null},
    c=>{c.loadouts.classes[0].slotDefinitions.push(null)},
    c=>{c.loadouts.classes[0].slots={primary:{}}},
    c=>{c.formations.factions[0].formations[0].squads[0].classLimits=[]},
    c=>{c.loadouts.classes[0].slots.primary[0].snbt=null}
  ]) {
    const c=fixture(); damage(c); const before=JSON.stringify(c);
    assert.throws(()=>C.assertEditable(c),/文件结构/);
    assert.equal(JSON.stringify(c),before);
  }
});

test('shape gate allows semantic draft errors and keeps unknown fields unchanged', () => {
  const c=fixture(); c.loadouts.classes[0].squadLimit=-5;
  c.loadouts.classes[0].slotDefinitions.push({id:'duplicate_target',displayName:'新槽位',target:'HOTBAR_1',required:false});
  c.loadouts.classes[0].slots.duplicate_target=[];
  c.formations.factions[0].futureField={anything:[null,1]};
  const before=JSON.stringify(c);
  assert.equal(C.assertEditable(c),true); assert.equal(JSON.stringify(c),before);
  assert(C.validate(c).errors.length>0);
});

test('current Java model permits slash in entry IDs but not in class or slot IDs', () => {
  const c=fixture(); c.loadouts.classes[0].slots.primary[0].id='weapons/test';
  c.formations.factions[0].formations[0].classes[0].allowedEntries.primary=['weapons/test'];
  assert.deepEqual(C.validate(c).errors,[]);
  c.loadouts.classes[0].id='roles/test';
  assert(C.validate(c).errors.some(e=>e.includes('loadouts.classes[0].id')));
});

test('assault crate auto-insertion is caught before strict native decoding', () => {
  const c=fixture(),role=c.loadouts.classes[0],formation=c.formations.factions[0].formations[0];
  role.id='assault'; formation.classes[0].classId='assault'; formation.squads[0].classLimits={assault:8};
  role.slotDefinitions.push({id:'gadget_two',displayName:'道具二',target:'HOTBAR_2',required:false});
  role.slots.gadget_two=[];
  assert(C.validate(c).errors.some(e=>e.includes('assault.gadget_two')&&e.includes('独立自定义职业池')));
  role.slots.gadget_two.push({id:'crate',displayName:'小型弹药箱',itemId:'wok_infantry:ammo_supply_crate',count:1,snbt:'',ammoReserveLimit:180});
  assert.deepEqual(C.validate(c).errors,[]);
  role.slots.gadget_two=Array.from({length:64},(_,i)=>({id:'item_'+i,displayName:'石头',itemId:'minecraft:stone',count:1,snbt:'',ammoReserveLimit:180}));
  assert.deepEqual(C.validate(c).errors,[]);
});
