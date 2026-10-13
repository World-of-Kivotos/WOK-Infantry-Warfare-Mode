'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const S = require('./snbt.js');

test('lossless tuning edit preserves nested unknown tags, typed arrays and formatting', () => {
  const input = ' \n{ Unknown : {n:[I;1,-2,3],l:[L;9223372036854775807L], b:[B;1b,-2b], list:[{a:1d},{a:2d}]}, GunId : "tacz:m4a1", wok_infantry_ads_speed_scale:0.60f, X:\'quote"\\\'\\\\\' }\t';
  const edited = S.setTuning(input, 'ads_speed', 0.75);
  assert.equal(edited, input.replace('0.60f', '0.75f'));
  assert.equal(S.readTuning(edited, 'ads_speed'), 0.75);
  assert.equal(S.readGunId(edited), 'tacz:m4a1');
  assert.equal(S.parse(input).source, input);
});

test('SNBT quoted strings round trip without applying JSON escape semantics', () => {
  for (const value of ['line\nline', 'C:\\path\\"x"', '</script><img src=x onerror=alert(1)>', "单引号'中文"]) {
    assert.equal(S.readScalar(S.setScalar('{}', 'note', value), 'note'), value);
  }
  assert.throws(() => S.parse('{x:"bad\\njson"}'), /无效转义/);
  assert.throws(() => S.parse('{x:"bad\\u0041"}'), /无效转义/);
});

test('numbers preserve all legal tag types and lossless long precision', () => {
  const root = S.parse('{b:-128b,s:32767S,i:-2147483648,l:9223372036854775807L,f:1.25e-2f,d:2D,n:3.5,t:true,z:false}');
  assert.deepEqual(root.entries.map((e) => e.value.type), ['byte','short','int','long','float','double','double','byte','byte']);
  assert.equal(S.readScalar(root.source, 'l'), '9223372036854775807');
  assert.equal(S.readScalar(S.setScalar('{}', 'n', '9223372036854775807', 'long'), 'n'), '9223372036854775807');
  assert.equal(S.readScalar(S.setScalar('{}', 'n', 9n), 'n'), 9);
  assert.throws(() => S.setScalar('{}', 'n', 9223372036854775807, 'long'), /精度/);
  assert.throws(() => S.setScalar('{}', 'n', 128, 'byte'), /范围/);
  assert.throws(() => S.setScalar('{}', 'n', 1.2, 'int'), /小数/);
  assert.throws(() => S.setScalar('{}', 'n', Infinity), /有限/);
  assert.throws(() => S.setScalar('{}', 'n', 1e50, 'float'), /范围/);
  assert.equal(S.readScalar('{n:001}', 'n'), 1); // Vanilla treats decimal-like non-int text as double.
});

test('adds and removes fields in empty, spaced, trailing-comma and quoted-key compounds', () => {
  for (const source of ['{}', '{ }', '{a:1}', '{ a:1, }', '{ a : 1 , b : 2 }', '{"a b":3,}']) {
    const edited = S.setScalar(source, 'new key', '中文');
    assert.equal(S.readScalar(edited, 'new key'), '中文');
    assert.equal(S.readScalar(S.remove(edited, 'new key'), 'new key', 'missing'), 'missing');
  }
  for (const key of ['a', 'b', 'c']) {
    assert.equal(S.parse(S.remove('{a:1,b:2,c:3}', key)).entries.length, 2);
    assert.equal(S.parse(S.remove('{a:1,b:2,c:3,}', key)).entries.length, 2);
  }
  assert.equal(S.remove('{a:1}', 'a'), '{}');
  assert.equal(S.remove('{ a:1, }', 'a'), '{  }');
  assert.equal(S.setScalar('', 'GunId', 'tacz:m4a1'), '{GunId:"tacz:m4a1"}');
  assert.equal(S.readScalar('', 'anything', 7), 7);
  assert.equal(S.remove('{x:1}', 'missing'), '{x:1}');
});

test('attachments preserve unknown data including color, zoom and original numeric spelling', () => {
  const source = '{Other:[I;1,2],AttachmentSCOPE:{Count:1b,id:"tacz:attachment",tag:{AttachmentId:"tacz:old",ZoomNumber:5,Unknown:{v:2.00f}}},GunId:"tacz:m4a1"}';
  const output = S.setAttachment(source, 'SCOPE', 'suffuse:scope_compm4');
  assert.equal(output, source.replace('tacz:old', 'suffuse:scope_compm4'));
  assert.equal(S.readAttachment(output, 'AttachmentSCOPE'), 'suffuse:scope_compm4');
  assert.equal(S.readAttachment(S.setAttachment(output, 'SCOPE', ''), 'SCOPE'), '');
  assert.equal(S.readGunId(output), 'tacz:m4a1');
});

test('new and empty-air attachments create valid item tags', () => {
  for (const input of ['', '{}', '{AttachmentGRIP:{Count:0b,id:"minecraft:air",tag:{keep:7}}}']) {
    const output = S.setAttachment(input, 'GRIP', 'tacz:grip_vertical_military');
    assert.equal(S.readAttachment(output, 'GRIP'), 'tacz:grip_vertical_military');
    assert.match(output, /Count:1b/);
    assert.match(output, /id:"tacz:attachment"/);
    if (input.includes('keep')) assert.match(output, /keep:7/);
  }
  assert.equal(S.readAttachment('{AttachmentGRIP:{Count:0b,id:"minecraft:air"}}', 'GRIP'), '');
  assert.throws(() => S.setAttachment('{AttachmentSCOPE:4}', 'SCOPE', 'tacz:scope'), /不是复合/);
  assert.throws(() => S.setAttachment('{AttachmentSCOPE:{tag:4}}', 'SCOPE', 'tacz:scope'), /不是复合/);
  assert.throws(() => S.setAttachment('{}', 'SCOPE', 'missing namespace'), /资源 ID/);
  assert.throws(() => S.setAttachment('{}', '__proto__', 'a:b'), /配件槽/);
});

test('generic nested edits preserve all siblings and create only missing compounds', () => {
  assert.equal(S.setPathScalar('{a:{ keep : [I;7] },b:2}', ['a','nested','v'], 3, 'short'), '{a:{ keep : [I;7],nested:{v:3s} },b:2}');
  assert.throws(() => S.setPathScalar('{a:3}', ['a','b'], 2), /不是复合/);
  assert.throws(() => S.setPathScalar('{}', [], 2), /标签路径/);
});

test('malformed, ambiguous, mixed-type, partial and malicious documents fail closed', () => {
  for (const input of ['', '[]', '{a:}', '{a:1 b:2}', '{a:1,,}', '{a:1,a:2}', '{a:{x:1,x:2}}', '{a:[1,"x"]}', '{a:[B;1]}', '{a:[L;1]}', '{a:[I;1L]}', '{a:"unfinished}', '{a:1} trailing', '{a:foo:bar}', '{a:1};process.exit()']) {
    assert.throws(() => S.parse(input), S.SnbtError, input);
    if (input.trim()) assert.throws(() => S.setScalar(input, 'n', 2), S.SnbtError, input);
  }
  assert.throws(() => S.parse('{a:' + '{a:'.repeat(65) + '1' + '}'.repeat(66)), /嵌套/);
  assert.throws(() => S.parse('{a:"' + 'x'.repeat(S.LIMITS.maxLength) + '"}'), /1 MiB/);
  assert.throws(() => S.parse('{a:[' + Array(100001).fill('0').join(',') + ']}'), /节点/);
  const dangerous = S.setPathScalar('{}', ['__proto__', 'polluted'], 'yes');
  assert.equal({}.polluted, undefined);
  assert.equal(S.parse(dangerous).entries[0].key, '__proto__');
});

test('all seven tuning keys share current game limits and invalid values are rejected', () => {
  for (const key of S.TUNING_KEYS) {
    assert.equal(S.readTuning('', key), 1);
    assert.equal(S.readTuning(S.setTuning('{}', key, 0.25), key), 0.25);
    assert.equal(S.readTuning(S.setTuning('{}', key, 15), key), 15);
    assert.throws(() => S.setTuning('{}', key, 0.24), /0.25/);
    assert.throws(() => S.setTuning('{}', key, 15.01), /0.25/);
  }
  assert.throws(() => S.setTuning('{}', 'other', 1), /未知/);
  assert.throws(() => S.setTuning('{}', 'damage', NaN), /0.25/);
  assert.throws(() => S.setGunId('{}', 'M4A1'), /资源 ID/);
  assert.equal(S.readGunId(S.setGunId('', 'tacz:m4a1')), 'tacz:m4a1');
});

test('UMD works offline in a browser global without require, DOM access or code evaluation', () => {
  const context = vm.createContext({});
  vm.runInContext(fs.readFileSync(require.resolve('./snbt.js'), 'utf8'), context);
  assert.equal(context.WokSnbt.readGunId('{GunId:"tacz:m4a1"}'), 'tacz:m4a1');
  assert.equal(context.WokSnbt.readScalar('{x:"<script>alert(1)</script>"}', 'x'), '<script>alert(1)</script>');
});

// Optional read-only compatibility check: node --test snbt.test.cjs <not needed>.
// The integration runner can point this environment variable at a known config.
if (process.env.WOK_SNBT_FIXTURE) {
  test('all real loadout SNBT can be read and losslessly edited', () => {
    const loadouts = JSON.parse(fs.readFileSync(process.env.WOK_SNBT_FIXTURE, 'utf8'));
    let checked = 0;
    for (const cls of loadouts.classes) for (const entries of Object.values(cls.slots)) for (const entry of entries) {
      if (!entry.snbt?.trim()) continue;
      S.parse(entry.snbt);
      const changed = S.setScalar(entry.snbt, 'editor_test_only', 7);
      assert.equal(S.remove(changed, 'editor_test_only'), entry.snbt);
      checked++;
    }
    assert.ok(checked > 0);
  });
}
