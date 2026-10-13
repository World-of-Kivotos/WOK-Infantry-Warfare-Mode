/* WOK步战离线目录编辑器：与 CatalogArchive v1 / formations v2 / loadouts v3 对齐。
 * 此库不访问网络/文件系统，也不修改 validate/exportArchive 的输入。
 * 未知字段保留在工作副本中，但当前游戏严格拒绝未知字段，因此导出前报告错误。
 */
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.WokCatalog = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const FORMAT = 'wok_infantry_catalog';
  const TARGETS = Object.freeze(['HOTBAR_1','HOTBAR_2','HOTBAR_3','HOTBAR_4','HOTBAR_5','HOTBAR_6','HOTBAR_7','HOTBAR_8','HOTBAR_9','OFFHAND','ARMOR_HEAD','ARMOR_CHEST','ARMOR_LEGS','ARMOR_FEET']);
  const CATEGORIES = Object.freeze(['infantry','armored','motorized','mechanized','special']);
  const CALLSIGNS = Object.freeze(['alpha','bravo','charlie','delta','echo']);
  const ID = /^[a-z0-9_.-]{1,64}$/;
  const ENTRY_ID = /^[a-z0-9_./-]{1,64}$/;
  const RESOURCE = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
  const LARGE_STATIONS = ['dragonrise_reforge:ammo_supply_station','wok_infantry:large_ammo_supply_station'];
  const SCALE_KEYS = ['ads_speed','vertical_recoil','horizontal_recoil','spread','damage','armor_ignore','rpm'].map(x => 'wok_infantry_' + x + '_scale');
  const own = (o, k) => Object.prototype.hasOwnProperty.call(o, k);
  const object = o => o !== null && typeof o === 'object' && !Array.isArray(o);
  const clone = value => JSON.parse(JSON.stringify(value));
  const byteLength = text => typeof TextEncoder !== 'undefined' ? new TextEncoder().encode(text).length : unescape(encodeURIComponent(text)).length;

  /** Strict JSON reader catches duplicate keys before JSON.parse would discard them. */
  function parse(text) {
    if (typeof text !== 'string') throw new Error('请输入 JSON 文本');
    if (byteLength(text) > 8 * 1024 * 1024) throw new Error('数据包超过 8 MiB 上限');
    let at = 0;
    const ws = () => { while (/\s/.test(text[at] || '') && at < text.length) at++; };
    function string() {
      const start = at++;
      while (at < text.length) {
        if (text[at] === '\\') { at += 2; continue; }
        if (text[at++] === '"') return JSON.parse(text.slice(start, at));
      }
      throw new Error('JSON 字符串未闭合');
    }
    function value(depth) {
      if (depth > 48) throw new Error('JSON 嵌套超过 48 层');
      ws();
      if (text[at] === '"') { string(); return; }
      if (text[at] === '{') {
        at++; ws(); const seen = new Set();
        if (text[at] === '}') { at++; return; }
        while (at < text.length) {
          ws(); if (text[at] !== '"') throw new Error('JSON 对象键必须用双引号');
          const key = string();
          if (seen.has(key)) throw new Error('JSON 字段重复：' + key);
          seen.add(key); ws();
          if (text[at++] !== ':') throw new Error('JSON 对象缺少冒号');
          value(depth + 1); ws();
          const next = text[at++];
          if (next === '}') return;
          if (next !== ',') throw new Error('JSON 对象未正确闭合');
        }
      } else if (text[at] === '[') {
        at++; ws(); if (text[at] === ']') { at++; return; }
        while (at < text.length) {
          value(depth + 1); ws(); const next = text[at++];
          if (next === ']') return;
          if (next !== ',') throw new Error('JSON 数组未正确闭合');
        }
      } else {
        const match = /^(?:true|false|null|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)/.exec(text.slice(at));
        if (!match) throw new Error('JSON 值无效，位置 ' + at);
        at += match[0].length; return;
      }
      throw new Error('JSON 内容不完整');
    }
    value(0); ws(); if (at !== text.length) throw new Error('JSON 末尾有多余内容');
    return JSON.parse(text);
  }

  /** Accepts an actual archive, or the pair { formations, loadouts } from two config files. */
  function archiveView(catalog) {
    if (!object(catalog)) return catalog;
    if (own(catalog, 'format')) return catalog;
    return { format: FORMAT, version: 1, exportedAt: new Date().toISOString(), ...catalog };
  }

  /** Shape gate before an editor renders a file. Semantic mistakes remain editable drafts.
   * Unknown properties are retained; this checks only required fields and their recursive types.
   */
  function assertEditable(catalog) {
    const entry = {id:'string',displayName:'string',itemId:'string',count:'number',snbt:'string',ammoReserveLimit:'number'};
    const slot = {id:'string',displayName:'string',target:'string',required:'boolean'};
    const role = {id:'string',displayName:'string',enabled:'boolean',squadLimit:'number',slots:{$map:[entry]},slotDefinitions:[slot]};
    const rule = {classId:'string',displayName:'string',squadLimit:'number',allowedEntries:{$map:['string']}};
    const squad = {callsign:'string',displayName:'string',capacity:'number',classLimits:{$map:'number'}};
    const vehicle = {id:'string',displayName:'string',entityId:'string',offsetX:'number',offsetY:'number',offsetZ:'number',yaw:'number',replenishmentCooldownSeconds:'number'};
    const deployable = {enabled:'boolean',maxActive:'number',squadLeaderCanPlace:'boolean',commanderCanPlace:'boolean',maxHealth:'number',placementCooldownSeconds:'number',replacementCooldownSeconds:'number',destructionCooldownSeconds:'number'};
    const capabilities = {outpost:deployable,rally:deployable,respawn:{delaySeconds:'number',mobileSpawnVehicleIds:['string']},support:{mode:'string',allowList:['string']}};
    const formation = {id:'string',displayName:'string',description:'string',icon:'string',category:'string',enabled:'boolean',capacity:'number',capabilities,classes:[rule],squads:[squad],vehicles:[vehicle]};
    const faction = {id:'string',displayName:'string',description:'string',battleSide:'string',enabled:'boolean',maxPlayers:'number',formations:[formation]};
    const schema = {format:'string',version:'number',exportedAt:'string',formations:{version:'number',factions:[faction]},loadouts:{version:'number',classes:[role]}};
    const errors=[];
    function visit(value, spec, path) {
      if (typeof spec === 'string') {
        if (typeof value !== spec) errors.push(path + '：必须是 ' + spec);
        return;
      }
      if (Array.isArray(spec)) {
        if (!Array.isArray(value)) { errors.push(path + '：必须是数组'); return; }
        value.forEach((item,i)=>visit(item,spec[0],path+'['+i+']'));
        return;
      }
      if (!object(value)) { errors.push(path+'：必须是非空对象'); return; }
      if (own(spec,'$map')) {
        Object.entries(value).forEach(([key,item])=>visit(item,spec.$map,path+'.'+key));
        return;
      }
      Object.keys(spec).forEach(key=>{
        if (!own(value,key)) errors.push(path+'.'+key+'：缺少必需字段');
        else visit(value[key],spec[key],path+'.'+key);
      });
    }
    visit(archiveView(catalog),schema,'目录');
    if (errors.length) {
      const failure=new Error('文件结构不适合安全编辑：\n'+errors.join('\n'));
      failure.errors=errors; throw failure;
    }
    return true;
  }

  function validate(input) {
    const errors = [], warnings = [];
    const c = archiveView(input);
    const err = (path, message) => errors.push(path + '：' + message);
    const warn = (path, message) => warnings.push(path + '：' + message);
    function fields(o, names, path) {
      if (!object(o)) { err(path, '必须是对象'); return false; }
      names.forEach(k => { if (!own(o, k)) err(path + '.' + k, '缺少字段（游戏导入不补默认值）'); });
      Object.keys(o).forEach(k => { if (!names.includes(k)) err(path + '.' + k, '未知字段已保留；当前游戏不接受此字段'); });
      return true;
    }
    function arr(a, path, max, min = 0) {
      if (!Array.isArray(a)) { err(path, '必须是数组'); return []; }
      if (a.length < min || a.length > max) err(path, '数量须为 ' + min + '–' + max);
      return a;
    }
    function map(o, path, max = 64) {
      if (!object(o)) { err(path, '必须是对象映射'); return {}; }
      if (Object.keys(o).length > max) err(path, '键数量超过 ' + max);
      return o;
    }
    function str(s, path, max, blank = false, trim = false) {
      if (typeof s !== 'string' || s.length > max || (!blank && !s.trim())) err(path, '须为' + (blank ? '可空' : '非空') + '字符串，最多 ' + max + ' 字符');
      else if (trim && s !== s.trim()) err(path, '首尾不能有空格（游戏会规范化）');
    }
    function id(s, path) { if (typeof s !== 'string' || !ID.test(s)) err(path, 'ID 须为 1–64 位小写字母、数字、_、-、.'); }
    function entryId(s, path) { if (typeof s !== 'string' || !ENTRY_ID.test(s)) err(path, '装备 ID 须为 1–64 位小写字母、数字、_、-、.、/'); }
    function resource(s, path, max = 128, blank = false) {
      if (blank && s === '') return;
      if (typeof s !== 'string' || s.length > max || !RESOURCE.test(s)) err(path, '须为有效的 namespace:path 资源 ID');
    }
    function integer(n, path, min, max) { if (!Number.isInteger(n) || n < min || n > max) err(path, '整数范围 ' + min + '–' + max); }
    function number(n, path, min, max, openMax = false) { if (!Number.isFinite(n) || n < min || (openMax ? n >= max : n > max)) err(path, '数值范围 ' + min + '–' + max + (openMax ? '（不含上限）' : '')); }
    function bool(b, path) { if (typeof b !== 'boolean') err(path, '必须是 true 或 false'); }
    function choice(v, values, path) { if (!values.includes(v)) err(path, '只允许 ' + values.join(' / ')); }
    function unique(list, key, path) {
      const set = new Set();
      list.forEach((item, i) => { const value = key ? item && item[key] : item; if (set.has(value)) err(path + '[' + i + ']', '重复 ' + (key || '值') + '：' + value); set.add(value); });
    }
    if (!fields(c, ['format','version','exportedAt','formations','loadouts'], '目录')) return {errors, warnings};
    if (c.format !== FORMAT || c.version !== 1) err('目录', '只支持 wok_infantry_catalog version=1');
    str(c.exportedAt, 'exportedAt', 128);
    if (!fields(c.formations, ['version','factions'], 'formations') || !fields(c.loadouts, ['version','classes'], 'loadouts')) return {errors, warnings};
    if (c.formations.version !== 2) err('formations.version', '当前必须为 2');
    if (c.loadouts.version !== 3) err('loadouts.version', '当前必须为 3');
    const classes = arr(c.loadouts.classes, 'loadouts.classes', 64, 1);
    const classById = new Map();
    unique(classes, 'id', 'loadouts.classes');
    classes.forEach((cl, i) => {
      const p = 'loadouts.classes[' + i + ']';
      if (!fields(cl, ['id','displayName','enabled','squadLimit','slots','slotDefinitions'], p)) return;
      id(cl.id, p + '.id'); str(cl.displayName, p + '.displayName', 80); bool(cl.enabled, p + '.enabled'); integer(cl.squadLimit, p + '.squadLimit', 1, 8);
      if (cl.id === 'assault' && (cl.enabled !== true || cl.squadLimit !== 8)) err(p, '旧 assault 安全职业必须 enabled=true、squadLimit=8');
      classById.set(cl.id, cl);
      const slots = arr(cl.slotDefinitions, p + '.slotDefinitions', 14);
      const entries = map(cl.slots, p + '.slots', 14);
      unique(slots, 'id', p + '.slotDefinitions'); unique(slots, 'target', p + '.slotDefinitions');
      Object.keys(entries).forEach(key => { if (!slots.some(s => s && s.id === key)) err(p + '.slots.' + key, '未在 slotDefinitions 定义；游戏会迁移此字段'); });
      slots.forEach((slot, si) => {
        const sp = p + '.slotDefinitions[' + si + ']';
        if (!fields(slot, ['id','displayName','target','required'], sp)) return;
        id(slot.id, sp + '.id'); str(slot.displayName, sp + '.displayName', 80); choice(slot.target, TARGETS, sp + '.target'); bool(slot.required, sp + '.required');
        const list = arr(entries[slot.id], p + '.slots.' + slot.id, 64);
        unique(list, 'id', p + '.slots.' + slot.id);
        if (slot.required && !list.some(e => e && e.itemId && e.itemId !== 'minecraft:air')) warn(sp, '必需槽位没有真实装备；部署会失败');
        if (!slot.required && list.length && !list.some(e => e && e.itemId === 'minecraft:air')) warn(sp, '非必需槽不会自动提供空选项；未保存选择时默认采用第一项');
        list.forEach((entry, ei) => {
          const ep = p + '.slots.' + slot.id + '[' + ei + ']';
          if (!fields(entry, ['id','displayName','itemId','count','snbt','ammoReserveLimit'], ep)) return;
          entryId(entry.id, ep + '.id'); str(entry.displayName, ep + '.displayName', 80); resource(entry.itemId, ep + '.itemId', 256);
          integer(entry.count, ep + '.count', 1, 64); integer(entry.ammoReserveLimit, ep + '.ammoReserveLimit', 1, 4096); str(entry.snbt, ep + '.snbt', 32767, true);
          if (LARGE_STATIONS.includes(entry.itemId)) err(ep, '大型弹药补给站只能通过载具运输，禁止步兵配装');
          if (typeof entry.snbt === 'string' && entry.snbt.trim()) {
            if (!entry.snbt.trim().startsWith('{') || !entry.snbt.trim().endsWith('}')) err(ep + '.snbt', 'SNBT 必须为 {...} 复合标签');
            SCALE_KEYS.forEach(key => {
              const match = new RegExp('(?:["\']?' + key + '["\']?)\\s*:\\s*([^,}]+)').exec(entry.snbt);
              if (match) {
                const scale = Number(match[1].trim().replace(/[fFdD]$/, ''));
                if (!Number.isFinite(scale) || scale < 0.25 || scale > 15) err(ep + '.snbt.' + key, '倍率范围 0.25–15.00');
              }
            });
          }
          if (entry.itemId === 'tacz:modern_kinetic_gun') {
            if (typeof entry.snbt !== 'string' || !/(?:["']?GunId["']?)\s*:/.test(entry.snbt)) err(ep, 'TaCZ 枪械缺少 GunId；通用物品 ID 不能指定枪型');
            if (entry.count !== 1) warn(ep, '枪械数量不是 1；请在游戏核对堆叠与发放');
          }
        });
      });
      if (cl.id === 'assault' && slots.some(s => s && s.id === 'gadget_two')) {
        const list = Array.isArray(entries.gadget_two) ? entries.gadget_two : [];
        if (list.length < 64 && !list.some(e => e && e.itemId === 'wok_infantry:ammo_supply_crate')) err(p, 'assault.gadget_two 缺少小型弹药箱；游戏规范化会添加它，严格目录导入将拒绝差异。请保留 wok_infantry:ammo_supply_crate，或改用独立自定义职业池');
      }
    });
    const factions = arr(c.formations.factions, 'formations.factions', 16, 1);
    unique(factions, 'id', 'formations.factions'); unique(factions, 'battleSide', 'formations.factions');
    const classOwners = new Map();
    factions.forEach((f, fi) => {
      const p = 'formations.factions[' + fi + ']';
      if (!fields(f, ['id','displayName','description','battleSide','enabled','maxPlayers','formations'], p)) return;
      id(f.id, p + '.id'); str(f.displayName, p + '.displayName', 40, false, true); str(f.description, p + '.description', 512, true, true);
      choice(f.battleSide, ['blue','red'], p + '.battleSide'); bool(f.enabled, p + '.enabled'); integer(f.maxPlayers, p + '.maxPlayers', 1, 40);
      const forms = arr(f.formations, p + '.formations', 32);
      unique(forms, 'id', p + '.formations');
      if (f.enabled && !forms.some(x => x && x.enabled)) err(p, '启用阵营至少需要一个已启用编制，否则游戏会禁用阵营');
      forms.forEach((form, index) => {
        const fp = p + '.formations[' + index + ']';
        if (!fields(form, ['id','displayName','description','icon','category','enabled','capacity','capabilities','classes','squads','vehicles'], fp)) return;
        id(form.id, fp + '.id'); str(form.displayName, fp + '.displayName', 40, false, true); str(form.description, fp + '.description', 512, true, true);
        resource(form.icon, fp + '.icon', 128, true); choice(form.category, CATEGORIES, fp + '.category'); bool(form.enabled, fp + '.enabled'); integer(form.capacity, fp + '.capacity', 1, Number.isInteger(f.maxPlayers) ? f.maxPlayers : 40);
        const rules = arr(form.classes, fp + '.classes', 64);
        const squads = arr(form.squads, fp + '.squads', 5);
        const vehicles = arr(form.vehicles, fp + '.vehicles', 64);
        unique(rules, 'classId', fp + '.classes'); unique(squads, 'callsign', fp + '.squads'); unique(vehicles, 'id', fp + '.vehicles');
        if (form.enabled && (!rules.length || !squads.length)) err(fp, '启用编制必须有职业和小队');
        const ruleIds = new Set(rules.filter(object).map(r => r.classId));
        rules.forEach((rule, ri) => {
          const rp = fp + '.classes[' + ri + ']';
          if (!fields(rule, ['classId','displayName','squadLimit','allowedEntries'], rp)) return;
          id(rule.classId, rp + '.classId'); str(rule.displayName, rp + '.displayName', 40, true, true); integer(rule.squadLimit, rp + '.squadLimit', 1, 8);
          const backing = classById.get(rule.classId);
          if (!backing) err(rp, '引用不存在的职业装备池：' + rule.classId);
          const owners = classOwners.get(rule.classId) || []; owners.push(f.id + '/' + form.id); classOwners.set(rule.classId, owners);
          const restrictions = map(rule.allowedEntries, rp + '.allowedEntries', 16);
          Object.entries(restrictions).forEach(([slotId, entryIds]) => {
            const ap = rp + '.allowedEntries.' + slotId; id(slotId, ap);
            const allowed = arr(entryIds, ap, 64); unique(allowed, null, ap);
            if (backing && (!Array.isArray(backing.slotDefinitions) || !backing.slotDefinitions.some(s => s && s.id === slotId))) err(ap, '白名单引用不存在的槽位');
            const pool = backing && object(backing.slots) && Array.isArray(backing.slots[slotId]) ? backing.slots[slotId] : [];
            allowed.forEach(value => { entryId(value, ap); if (backing && !pool.some(e => e && e.id === value)) err(ap, '白名单引用不存在的装备：' + value); });
            if (!allowed.length) warn(ap, '空白名单表示允许该槽全部条目，不能表示禁用');
          });
        });
        squads.forEach((squad, si) => {
          const sp = fp + '.squads[' + si + ']';
          if (!fields(squad, ['callsign','displayName','capacity','classLimits'], sp)) return;
          choice(squad.callsign, CALLSIGNS, sp + '.callsign'); str(squad.displayName, sp + '.displayName', 40, false, true); integer(squad.capacity, sp + '.capacity', 1, 8);
          Object.entries(map(squad.classLimits, sp + '.classLimits')).forEach(([classId, limit]) => {
            if (!ruleIds.has(classId)) err(sp + '.classLimits.' + classId, '配额引用不属于本编制的职业');
            integer(limit, sp + '.classLimits.' + classId, 0, Number.isInteger(squad.capacity) ? squad.capacity : 8);
          });
          if (!rules.some(r => r && ((object(squad.classLimits) && own(squad.classLimits, r.classId)) ? squad.classLimits[r.classId] : r.squadLimit) >= squad.capacity)) warn(sp, '没有能覆盖全队容量的兜底职业，请核对满队分配');
        });
        vehicles.forEach((v, vi) => {
          const vp = fp + '.vehicles[' + vi + ']';
          if (!fields(v, ['id','displayName','entityId','offsetX','offsetY','offsetZ','yaw','replenishmentCooldownSeconds'], vp)) return;
          id(v.id, vp + '.id'); str(v.displayName, vp + '.displayName', 40, false, true); resource(v.entityId, vp + '.entityId');
          ['offsetX','offsetY','offsetZ'].forEach(k => number(v[k], vp + '.' + k, -256, 256)); number(v.yaw, vp + '.yaw', 0, 360, true); integer(v.replenishmentCooldownSeconds, vp + '.replenishmentCooldownSeconds', -1, 86400);
        });
        const caps = form.capabilities;
        if (fields(caps, ['outpost','rally','respawn','support'], fp + '.capabilities')) {
          ['outpost','rally'].forEach(k => {
            const v = caps[k], cp = fp + '.capabilities.' + k;
            if (!fields(v, ['enabled','maxActive','squadLeaderCanPlace','commanderCanPlace','maxHealth','placementCooldownSeconds','replacementCooldownSeconds','destructionCooldownSeconds'], cp)) return;
            ['enabled','squadLeaderCanPlace','commanderCanPlace'].forEach(key => bool(v[key], cp + '.' + key));
            integer(v.maxActive, cp + '.maxActive', 0, 64); integer(v.maxHealth, cp + '.maxHealth', 0, 1000000);
            ['placementCooldownSeconds','replacementCooldownSeconds','destructionCooldownSeconds'].forEach(key => integer(v[key], cp + '.' + key, 0, 86400));
            if (v.enabled && (!v.maxActive || !v.maxHealth || (!v.squadLeaderCanPlace && !v.commanderCanPlace))) err(cp, '启用需数量、血量、至少一种放置权限');
          });
          const rp = fp + '.capabilities.respawn', respawn = caps.respawn;
          if (fields(respawn, ['delaySeconds','mobileSpawnVehicleIds'], rp)) {
            integer(respawn.delaySeconds, rp + '.delaySeconds', -1, 3600);
            const mobile = arr(respawn.mobileSpawnVehicleIds, rp + '.mobileSpawnVehicleIds', 64); unique(mobile, null, rp);
            mobile.forEach(vehicleId => { if (!vehicles.some(v => v && v.id === vehicleId)) err(rp, '移动重生引用不存在的本编制载具槽位：' + vehicleId); });
          }
          const sp = fp + '.capabilities.support', support = caps.support;
          if (fields(support, ['mode','allowList'], sp)) {
            choice(support.mode, ['none','all','allow_list'], sp + '.mode');
            const list = arr(support.allowList, sp + '.allowList', 64); unique(list, null, sp + '.allowList');
            list.forEach(v => resource(v, sp + '.allowList'));
            if (support.mode !== 'allow_list' && list.length) err(sp, '非白名单模式必须使用空 allowList，游戏会清除它');
          }
        }
      });
    });
    classOwners.forEach((owners, classId) => { if (owners.length > 1) warn('职业 ' + classId, '共享装备池被 ' + owners.join('、') + ' 引用；修改会共同生效，独立编辑前先复制'); });
    try {
      const compact = JSON.stringify(c.formations) + JSON.stringify(c.loadouts);
      if (compact.length > 900000) err('目录', 'formations 与 loadouts 总量超过客户端 900000 字符限制');
      if (byteLength(JSON.stringify(c, null, 2)) > 8 * 1024 * 1024) err('目录', '导出数据包超过 8 MiB');
    } catch (_) { err('目录', '含循环引用或不能保存为 JSON 的内容'); }
    warnings.push('离线校验不能确认已安装物品、GunId、配件、护甲适配或完整 SNBT 语法；最终以服务端预览与游戏发放为准。');
    warnings.push('游戏导入会整体替换阵营与配装目录；先载入目标实例最新完整导出，在其上修改并保留无关内容。');
    return {errors, warnings};
  }

  /** Adds an independent formation and pools atomically; leaves source and unrelated objects intact. */
  function cloneFormation(catalog, factionId, formationId, newId, newName) {
    if (!ID.test(newId || '')) throw new Error('新编制 ID 无效');
    if (typeof newName !== 'string' || !newName.trim() || newName !== newName.trim() || newName.length > 40) throw new Error('新编制名称须为 1–40 字符，首尾不含空格');
    const factions = catalog && catalog.formations && catalog.formations.factions;
    const pools = catalog && catalog.loadouts && catalog.loadouts.classes;
    if (!Array.isArray(factions) || !Array.isArray(pools)) throw new Error('目录缺少 factions 或 loadout classes');
    const faction = factions.find(f => f && f.id === factionId);
    if (!faction || !Array.isArray(faction.formations)) throw new Error('来源阵营不存在');
    const source = faction.formations.find(f => f && f.id === formationId);
    if (!source) throw new Error('来源编制不存在');
    if (faction.formations.some(f => f && f.id === newId)) throw new Error('目标编制 ID 已存在');
    if (faction.formations.length >= 32) throw new Error('阵营编制已达 32 个');
    if (!Array.isArray(source.classes) || !Array.isArray(source.squads)) throw new Error('来源编制缺少职业或小队');
    const ids = new Set(pools.map(p => p && p.id)), remap = new Map(), additions = [];
    source.classes.forEach(rule => {
      if (!rule || !ID.test(rule.classId || '')) throw new Error('来源职业 ID 无效');
      if (remap.has(rule.classId)) throw new Error('来源编制职业 ID 重复');
      const backing = pools.find(p => p && p.id === rule.classId);
      if (!backing) throw new Error('来源职业缺少装备池：' + rule.classId);
      const base = (factionId + '_' + newId + '_' + rule.classId).slice(0, 58);
      let next = base, suffix = 2;
      while (ids.has(next)) { const tail = '_' + suffix++; next = base.slice(0, 64 - tail.length) + tail; }
      if (!ID.test(next)) throw new Error('无法生成有效的独立职业 ID');
      ids.add(next); remap.set(rule.classId, next);
      const copied = clone(backing); copied.id = next; additions.push(copied);
    });
    if (pools.length + additions.length > 64) throw new Error('复制后全局职业池超过 64 个');
    const result = clone(source); result.id = newId; result.displayName = newName;
    result.classes.forEach(rule => { rule.classId = remap.get(rule.classId); });
    result.squads.forEach(squad => {
      if (!object(squad.classLimits)) throw new Error('来源小队职业配额无效');
      const entries = Object.entries(squad.classLimits).map(([key, value]) => {
        if (!remap.has(key)) throw new Error('来源小队引用未知职业：' + key);
        return [remap.get(key), value];
      });
      squad.classLimits = Object.fromEntries(entries);
    });
    // Entry IDs are local to each copied class/slot. Preserve them and allowedEntries verbatim.
    // Vehicle IDs are local to the new formation, so mobileSpawnVehicleIds remain valid too.
    pools.push(...additions); faction.formations.push(result);
    return result;
  }

  function exportArchive(catalog) {
    const archive = clone(archiveView(catalog));
    archive.exportedAt = new Date().toISOString();
    const result = validate(archive);
    if (result.errors.length) {
      const failure = new Error('目录校验失败：\n' + result.errors.join('\n'));
      failure.errors = result.errors; failure.warnings = result.warnings; throw failure;
    }
    return archive;
  }

  /** Three-way merge. Stable-ID arrays merge by identity, never by row number.
   * A conflict result is review-only: callers must not apply catalog while conflicts is nonempty.
   */
  function mergeCatalog(base, edited, latest) {
    if (!object(base) || !object(edited) || !object(latest)) throw new Error('三方合并需要基线、编辑稿和最新目录');
    const MISSING = Symbol('missing'), conflicts = [];
    const copy = v => v === MISSING ? MISSING : clone(v);
    const same = (a, b) => {
      if (a === b) return true;
      if (a === MISSING || b === MISSING) return false;
      if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((v,i) => same(v,b[i]));
      if (object(a) && object(b)) {
        const keys = Object.keys(a);
        return keys.length === Object.keys(b).length && keys.every(k => own(b,k) && same(a[k],b[k]));
      }
      return false;
    };
    function conflict(path, reason, fallback) {
      conflicts.push((path.join('.') || '目录') + '：' + reason);
      return copy(fallback);
    }
    function arrayKey(path, lists) {
      const tail = path[path.length - 1];
      if (tail === 'squads') return 'callsign';
      if (['factions','formations','slotDefinitions','vehicles'].includes(tail)) return 'id';
      if (path[path.length - 2] === 'slots') return 'id';
      if (tail === 'classes') return lists.flat().some(x => object(x) && own(x,'classId')) ? 'classId' : 'id';
      return null;
    }
    function mergeOrder(baseIds, editIds, latestIds, retained, path) {
      const common = baseIds.filter(id => editIds.includes(id) && latestIds.includes(id) && retained.has(id));
      const commonSet = new Set(common);
      const e = editIds.filter(id => commonSet.has(id)), l = latestIds.filter(id => commonSet.has(id));
      const editedOrder = !same(e,common), latestOrder = !same(l,common);
      if (editedOrder && latestOrder && !same(e,l)) conflicts.push(path.join('.') + '：双方对同一组对象的显示顺序作了不同修改');
      const primary = editedOrder && !latestOrder ? editIds : latestIds;
      const secondary = editedOrder && !latestOrder ? latestIds : editIds;
      const output = primary.filter(id => retained.has(id));
      secondary.forEach((id,index) => {
        if (!retained.has(id) || output.includes(id)) return;
        const next = secondary.slice(index+1).find(candidate => output.includes(candidate));
        if (next !== undefined) output.splice(output.indexOf(next),0,id);
        else output.push(id);
      });
      return output;
    }
    function visit(b, e, l, path) {
      if (same(e,b)) return copy(l);
      if (same(l,b) || same(e,l)) return copy(e);
      if (b === MISSING || e === MISSING || l === MISSING) return conflict(path, '新增或删除与另一方修改冲突', l);
      if (object(b) && object(e) && object(l)) {
        const output = {};
        new Set([...Object.keys(l), ...Object.keys(e), ...Object.keys(b)]).forEach(k => {
          const v = visit(own(b,k)?b[k]:MISSING, own(e,k)?e[k]:MISSING, own(l,k)?l[k]:MISSING, path.concat(k));
          if (v !== MISSING) Object.defineProperty(output,k,{value:v,writable:true,enumerable:true,configurable:true});
        });
        return output;
      }
      if (Array.isArray(b) && Array.isArray(e) && Array.isArray(l)) {
        const key = arrayKey(path,[b,e,l]);
        if (!key) return conflict(path, '双方修改了同一列表，请逐项确认',l);
        const lists = [b,e,l];
        if (lists.some(list => list.some(v => !object(v) || typeof v[key] !== 'string') || new Set(list.map(v=>v[key])).size !== list.length)) return conflict(path, '列表缺少唯一稳定 ID，不能安全合并',l);
        const maps = lists.map(list=>new Map(list.map(v=>[v[key],v])));
        const all = new Set([...maps[2].keys(),...maps[1].keys(),...maps[0].keys()]), merged = new Map();
        all.forEach(id => {
          const v = visit(...maps.map(m=>m.has(id)?m.get(id):MISSING),path.concat('['+key+'='+id+']'));
          if (v !== MISSING) merged.set(id,v);
        });
        return mergeOrder(...lists.map(list=>list.map(v=>v[key])),new Set(merged.keys()),path).map(id=>merged.get(id));
      }
      return conflict(path,'双方修改了同一字段，请选择保留值',l);
    }
    // exportedAt is export metadata, not author-authored catalog content.
    const b=clone(base), e=clone(edited), l=clone(latest);
    if (own(b,'exportedAt') || own(e,'exportedAt') || own(l,'exportedAt')) {
      b.exportedAt=''; e.exportedAt=''; l.exportedAt='';
    }
    const catalog=visit(b,e,l,[]);
    if (own(catalog,'exportedAt')) catalog.exportedAt=new Date().toISOString();
    return {catalog,conflicts};
  }

  return Object.freeze({parse, clone, assertEditable, validate, cloneFormation, exportArchive, mergeCatalog,
    constants: Object.freeze({FORMAT, VERSION: 1, FORMATIONS_VERSION: 2, LOADOUTS_VERSION: 3, TARGETS, CATEGORIES, CALLSIGNS})});
});
