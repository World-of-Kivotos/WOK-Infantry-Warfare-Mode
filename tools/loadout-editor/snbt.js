/* WOK步战离线编辑器 — dependency-free, lossless SNBT field editing.
 * Browser: window.WokSnbt; Node: require('./snbt.js').
 * parse(text) requires one compound and returns a source-positioned AST.
 * readScalar / setScalar / remove operate on top-level keys; empty editor
 * documents mean {}. Unedited tokens, spelling, suffixes and whitespace remain
 * unchanged. Duplicate keys are rejected because they make edits ambiguous.
 * String values use SNBT quoting (only quote/backslash escaping, not JSON).
 */
(function (root, factory) {
  'use strict';
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.WokSnbt = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  const LIMITS = Object.freeze({ maxLength: 1048576, maxDepth: 64, maxNodes: 100000 });
  const TYPES = Object.freeze({ byte: 'b', short: 's', int: '', long: 'L', float: 'f', double: 'd' });
  const TUNING_KEYS = Object.freeze([
    'wok_infantry_ads_speed_scale', 'wok_infantry_vertical_recoil_scale',
    'wok_infantry_horizontal_recoil_scale', 'wok_infantry_spread_scale',
    'wok_infantry_damage_scale', 'wok_infantry_armor_ignore_scale',
    'wok_infantry_rpm_scale'
  ]);
  const ATTACHMENT_SLOTS = Object.freeze(['SCOPE', 'MUZZLE', 'STOCK', 'GRIP', 'LASER', 'EXTENDED_MAG']);
  const integerPattern = /^[+-]?(?:0|[1-9][0-9]*)$/;
  const decimalPattern = /^[+-]?(?:[0-9]+\.?|[0-9]*\.[0-9]+)(?:[eE][+-]?[0-9]+)?$/;
  const tokenChar = /[A-Za-z0-9_.+\-]/;
  const resourceId = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;

  class SnbtError extends SyntaxError {
    constructor(message, position) {
      super('SNBT: ' + message + (position === undefined ? '' : ' (位置 ' + position + ')'));
      this.name = 'SnbtError';
      if (position !== undefined) this.position = position;
    }
  }

  function numericToken(token) {
    if (/^(true|false)$/i.test(token)) return { type: 'byte', value: /^true$/i.test(token) ? 1 : 0 };
    const suffix = token.slice(-1).toLowerCase();
    const explicit = ({ b: 'byte', s: 'short', l: 'long', f: 'float', d: 'double' })[suffix];
    const digits = explicit ? token.slice(0, -1) : token;
    const type = explicit || (integerPattern.test(token) ? 'int' : 'double');
    const isInteger = ['byte', 'short', 'int', 'long'].includes(type);
    if (!(isInteger ? integerPattern : decimalPattern).test(digits)) return null;
    if (isInteger) {
      const n = BigInt(digits);
      const ranges = { byte: [-128n, 127n], short: [-32768n, 32767n], int: [-2147483648n, 2147483647n], long: [-9223372036854775808n, 9223372036854775807n] };
      if (n < ranges[type][0] || n > ranges[type][1]) return null;
      // A decimal string preserves long precision without putting BigInt in JSON-facing ASTs.
      return { type, value: type === 'long' && (n > BigInt(Number.MAX_SAFE_INTEGER) || n < BigInt(Number.MIN_SAFE_INTEGER)) ? n.toString() : Number(n) };
    }
    const value = Number(digits);
    if (!Number.isFinite(value) || (type === 'float' && !Number.isFinite(Math.fround(value)))) return null;
    return { type, value };
  }

  function parse(text) {
    if (typeof text !== 'string') throw new TypeError('SNBT 必须是字符串。');
    if (text.length > LIMITS.maxLength) throw new SnbtError('内容超过 1 MiB 限制');
    let p = 0;
    let nodes = 0;
    const fail = (message) => { throw new SnbtError(message, p); };
    const skip = () => { while (p < text.length && /\s/.test(text[p])) p++; };
    function quoted() {
      const quote = text[p++];
      let result = '';
      while (p < text.length) {
        const ch = text[p++];
        if (ch === quote) return result;
        if (ch === '\\') {
          if (p >= text.length) fail('引号内转义未结束');
          const escaped = text[p++];
          if (escaped !== quote && escaped !== '\\') fail('无效转义；SNBT 只转义当前引号和反斜杠');
          result += escaped;
        } else result += ch;
      }
      fail('字符串缺少结束引号');
    }
    function word() {
      const start = p;
      while (p < text.length && tokenChar.test(text[p])) p++;
      if (start === p) fail('缺少键或值');
      return text.slice(start, p);
    }
    function value(depth) {
      if (depth > LIMITS.maxDepth) fail('嵌套超过 64 层');
      if (++nodes > LIMITS.maxNodes) fail('节点数超过限制');
      skip();
      const start = p;
      if (text[p] === '{') {
        p++;
        skip();
        const entries = [];
        const keys = new Set();
        while (text[p] !== '}') {
          if (p >= text.length) fail('复合标签缺少 }');
          const keyStart = p;
          const key = text[p] === '"' || text[p] === "'" ? quoted() : word();
          const keyEnd = p;
          if (!key) fail('键不能为空');
          if (keys.has(key)) fail('重复键：' + key);
          keys.add(key);
          skip();
          if (text[p++] !== ':') fail('键后缺少冒号');
          const child = value(depth + 1);
          skip();
          const entry = { key, keyStart, keyEnd, value: child, commaAfter: null };
          entries.push(entry);
          if (text[p] === ',') { entry.commaAfter = p++; skip(); }
          else if (text[p] !== '}') fail('字段间缺少逗号');
        }
        const closeStart = p++;
        return { type: 'compound', start, end: p, closeStart, entries };
      }
      if (text[p] === '[') {
        p++;
        skip();
        let arrayType = null;
        if (/^[BIL]$/.test(text[p] || '') && text[p + 1] === ';') {
          arrayType = ({ B: 'byte', I: 'int', L: 'long' })[text[p]];
          p += 2;
          skip();
        }
        const items = [];
        while (text[p] !== ']') {
          if (p >= text.length) fail('列表缺少 ]');
          const child = value(depth + 1);
          if (arrayType && child.type !== arrayType) fail('类型数组元素必须为 ' + arrayType);
          if (!arrayType && items.length && child.type !== items[0].type) fail('NBT 列表元素类型必须一致');
          items.push(child);
          skip();
          if (text[p] === ',') { p++; skip(); }
          else if (text[p] !== ']') fail('列表元素间缺少逗号');
        }
        p++;
        return { type: arrayType ? arrayType + 'Array' : 'list', start, end: p, items };
      }
      if (text[p] === '"' || text[p] === "'") return { type: 'string', start, value: quoted(), end: p };
      const token = word();
      const numeric = numericToken(token);
      return Object.assign({ start, end: p }, numeric || { type: 'string', value: token });
    }
    skip();
    if (text[p] !== '{') fail('根节点必须为复合标签 {…}');
    const root = value(0);
    skip();
    if (p !== text.length) fail('根标签后有多余内容');
    root.source = text;
    return root;
  }

  function editable(text) {
    if (typeof text !== 'string') throw new TypeError('SNBT 必须是字符串。');
    return text.trim() ? text : '{}';
  }
  function entryOf(node, key) {
    return node.type === 'compound' ? node.entries.find((entry) => entry.key === key) : undefined;
  }
  function scalar(node, fallback) {
    return node && Object.prototype.hasOwnProperty.call(node, 'value') ? node.value : fallback;
  }
  function readScalar(text, key, fallback) {
    const entry = entryOf(parse(editable(text)), key);
    return scalar(entry && entry.value, fallback);
  }
  function quote(value) {
    return '"' + value.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
  }
  function encodeScalar(value, type) {
    type = type || (typeof value === 'string' ? 'string' : typeof value === 'boolean' ? 'byte' : typeof value === 'bigint' ? 'long' : Number.isInteger(value) && value >= -2147483648 && value <= 2147483647 ? 'int' : 'double');
    if (type === 'string') {
      if (typeof value !== 'string') throw new TypeError('字符串标签必须使用字符串值。');
      return quote(value);
    }
    if (!Object.prototype.hasOwnProperty.call(TYPES, type)) throw new TypeError('未知 SNBT 数值类型：' + type);
    if (typeof value === 'boolean' && type === 'byte') value = value ? 1 : 0;
    if (type === 'long') {
      if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new TypeError('长整数请使用 bigint 或十进制字符串，避免精度丢失。');
      if (!['string', 'number', 'bigint'].includes(typeof value) || !integerPattern.test(String(value))) throw new TypeError('无效长整数。');
    } else if (typeof value !== 'number' || !Number.isFinite(value)) throw new TypeError('数值必须是有限数字。');
    if (['byte', 'short', 'int'].includes(type) && !Number.isInteger(value)) throw new TypeError('整数标签不能使用小数。');
    const raw = String(value) + TYPES[type];
    const parsed = numericToken(raw);
    if (!parsed || parsed.type !== type) throw new RangeError('数值超出 ' + type + ' 的范围。');
    return raw;
  }
  function validKey(key) {
    if (typeof key !== 'string' || !key.length) throw new TypeError('键必须是非空字符串。');
    return /^[A-Za-z0-9_.+\-]+$/.test(key) ? key : quote(key);
  }
  function replaceRange(text, start, end, replacement) {
    const updated = text.slice(0, start) + replacement + text.slice(end);
    // Validate every returned edit; never return partially valid output.
    parse(updated);
    return updated;
  }
  function upsertRaw(text, compound, key, raw) {
    const encodedKey = validKey(key);
    const existing = entryOf(compound, key);
    if (existing) return replaceRange(text, existing.value.start, existing.value.end, raw);
    const last = compound.entries[compound.entries.length - 1];
    const at = !last ? compound.start + 1 : last.commaAfter === null ? last.value.end : last.commaAfter + 1;
    const prefix = last && last.commaAfter === null ? ',' : '';
    return replaceRange(text, at, at, prefix + encodedKey + ':' + raw);
  }
  function setScalar(text, key, value, type) {
    text = editable(text);
    return upsertRaw(text, parse(text), key, encodeScalar(value, type));
  }
  function remove(text, key) {
    text = editable(text);
    const root = parse(text);
    const index = root.entries.findIndex((entry) => entry.key === key);
    if (index < 0) return text;
    const entry = root.entries[index];
    let start = entry.keyStart;
    let end = entry.value.end;
    if (entry.commaAfter !== null) end = entry.commaAfter + 1;
    else if (index > 0) start = root.entries[index - 1].commaAfter;
    return replaceRange(text, start, end, '');
  }
  function setPathScalar(text, path, value, type) {
    if (!Array.isArray(path) || !path.length || path.length > LIMITS.maxDepth) throw new TypeError('标签路径必须包含 1–64 个键。');
    path.forEach(validKey);
    text = editable(text);
    let parent = parse(text);
    for (let i = 0; i < path.length - 1; i++) {
      const entry = entryOf(parent, path[i]);
      if (!entry) {
        let raw = encodeScalar(value, type);
        for (let j = path.length - 1; j > i; j--) raw = '{' + validKey(path[j]) + ':' + raw + '}';
        return upsertRaw(text, parent, path[i], raw);
      }
      if (entry.value.type !== 'compound') throw new SnbtError('路径标签不是复合标签：' + path.slice(0, i + 1).join('.'), entry.value.start);
      parent = entry.value;
    }
    return upsertRaw(text, parent, path[path.length - 1], encodeScalar(value, type));
  }
  function attachmentKey(slot) {
    if (typeof slot !== 'string') throw new TypeError('配件槽必须是字符串。');
    const short = slot.startsWith('Attachment') ? slot.slice(10) : slot;
    if (!/^[A-Z][A-Z0-9_]{0,63}$/.test(short)) throw new TypeError('无效配件槽：' + slot);
    return 'Attachment' + short;
  }
  function checkedResource(value, label) {
    if (typeof value !== 'string' || !resourceId.test(value)) throw new TypeError(label + ' 必须为 namespace:path 资源 ID。');
    return value;
  }
  function readAttachment(text, slot) {
    const entry = entryOf(parse(editable(text)), attachmentKey(slot));
    if (!entry || entry.value.type !== 'compound') return '';
    const itemId = scalar(entryOf(entry.value, 'id')?.value, '');
    const count = scalar(entryOf(entry.value, 'Count')?.value, 1);
    if (itemId === 'minecraft:air' || count === 0) return '';
    const tag = entryOf(entry.value, 'tag');
    const attachment = tag && entryOf(tag.value, 'AttachmentId');
    return attachment && attachment.value.type === 'string' ? attachment.value.value : '';
  }
  function setAttachment(text, slot, id) {
    const key = attachmentKey(slot);
    if (id === '') return remove(text, key);
    checkedResource(id, '配件');
    text = editable(text);
    const entry = entryOf(parse(text), key);
    if (entry && entry.value.type !== 'compound') throw new SnbtError('配件标签不是复合标签：' + key, entry.value.start);
    const currentItem = entry && scalar(entryOf(entry.value, 'id')?.value, '');
    const count = entry && scalar(entryOf(entry.value, 'Count')?.value, 0);
    if (!entry || !currentItem || currentItem === 'minecraft:air') text = setPathScalar(text, [key, 'id'], 'tacz:attachment');
    if (!entry || !count || currentItem === 'minecraft:air') text = setPathScalar(text, [key, 'Count'], 1, 'byte');
    return setPathScalar(text, [key, 'tag', 'AttachmentId'], id, 'string');
  }
  function tuningKey(key) {
    if (typeof key !== 'string') throw new TypeError('倍率名称必须是字符串。');
    if (!key.startsWith('wok_infantry_')) key = 'wok_infantry_' + key + (key.endsWith('_scale') ? '' : '_scale');
    if (!TUNING_KEYS.includes(key)) throw new TypeError('未知枪械倍率：' + key);
    return key;
  }
  function readTuning(text, key, fallback = 1) {
    const value = readScalar(text, tuningKey(key), fallback);
    return typeof value === 'number' ? value : fallback;
  }
  function setTuning(text, key, value) {
    if (typeof value !== 'number' || !Number.isFinite(value) || value < 0.25 || value > 15) throw new RangeError('枪械倍率必须为 0.25–15。');
    return setScalar(text, tuningKey(key), value, 'float');
  }
  function readGunId(text) { return readScalar(text, 'GunId', ''); }
  function setGunId(text, id) { return setScalar(text, 'GunId', checkedResource(id, '枪械'), 'string'); }

  return Object.freeze({ SnbtError, LIMITS, TUNING_KEYS, ATTACHMENT_SLOTS, parse, readScalar,
    setScalar, remove, setPathScalar, readAttachment, setAttachment,
    readGunId, setGunId, readTuning, setTuning });
});
