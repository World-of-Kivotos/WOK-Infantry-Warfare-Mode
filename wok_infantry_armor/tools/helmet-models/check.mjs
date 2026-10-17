// Headless check of one helmet design (mode M):  node check.mjs <id>
// Paints every texel with the same face/UV math as viewer.js and reports:
//   bad colours (NaN / out of range), faces too close to or inside the skin hat layer, what covers the eyes, counts.
import fs from 'node:fs';
const here = new URL('./', import.meta.url);
const key = process.argv[2];
if (!key) { console.error('usage: node check.mjs <id>'); process.exit(2); }
const r = f => fs.readFileSync(new URL(f, here), 'utf8').replace(/^﻿/, '');
const tmp = new URL(`./.check_${key}.mjs`, here);
// load every design (a helmet may reuse another one's function), then look up this key
const all = fs.readdirSync(new URL('./helmets/', here)).filter(f => /^[^_].*\.js$/.test(f)).map(f => r('./helmets/' + f)).join('\n');
fs.writeFileSync(tmp, r('./designs.js') + '\n' + r('./helmets/_kit.js') + '\n' + all + '\n');
let mod;
try { mod = await import(tmp.href + '?t=' + process.hrtime.bigint()); } finally { fs.unlinkSync(tmp); }
const ARMORS = mod.ARMORS;
if (typeof ARMORS[key] !== 'function') { console.log(JSON.stringify({ ok: false, error: `ARMORS['${key}'] is not registered` })); process.exit(1); }
const d = ARMORS[key]('M');

function polys(b) {
  const g = b.grow || 0;
  const x1 = b.x - g, y1 = b.y - g, z1 = b.z - g, x2 = b.x + b.w + g, y2 = b.y + b.h + g, z2 = b.z + b.d + g;
  const V = [[x1, y1, z1], [x2, y1, z1], [x2, y2, z1], [x1, y2, z1], [x1, y1, z2], [x2, y1, z2], [x2, y2, z2], [x1, y2, z2]];
  const P = (ids, face) => { const [u1, v1, u2, v2] = b.faceUV[face]; return { face, verts: ids.map(i => V[i]), uv: [[u2, v1], [u1, v1], [u1, v2], [u2, v2]] }; };
  return [P([5, 4, 0, 1], 'top'), P([2, 3, 7, 6], 'bottom'), P([0, 4, 7, 3], 'right'), P([1, 0, 3, 2], 'front'), P([5, 1, 2, 6], 'left'), P([4, 5, 6, 7], 'back')];
}
function packFaces(boxes, S) {
  const faces = [];
  for (const b of boxes) {
    const dims = { top: [b.w, b.d], bottom: [b.w, b.d], right: [b.d, b.h], left: [b.d, b.h], front: [b.w, b.h], back: [b.w, b.h] };
    b.faceUV = {};
    for (const [f, [fw, fh]] of Object.entries(dims)) faces.push({ b, f, fw, fh, pw: Math.max(1, Math.ceil(fw * S - 1e-6)), ph: Math.max(1, Math.ceil(fh * S - 1e-6)) });
  }
  faces.sort((p, q) => q.ph - p.ph || q.pw - p.pw);
  let W = 64 * S;
  for (;;) {
    let x = 0, y = 0, row = 0, ok = true;
    for (const F of faces) {
      if (F.pw > W) { ok = false; break; }
      if (x + F.pw > W) { x = 0; y += row; row = 0; }
      F.x = x; F.y = y; x += F.pw; row = Math.max(row, F.ph);
    }
    const used = y + row;
    if (ok && used <= W) { let H = 16 * S; while (H < used) H *= 2; for (const F of faces) F.b.faceUV[F.f] = [F.x / S, F.y / S, F.x / S + F.fw, F.y / S + F.fh]; return { tw: W / S, th: H / S }; }
    W *= 2;
  }
}
const problems = [], boxes = d.boxes.map(b => ({ ...b }));
boxes.forEach((b, i) => {
  if (![b.x, b.y, b.z, b.w, b.h, b.d].every(Number.isFinite) || b.w <= 0 || b.h <= 0 || b.d <= 0) problems.push(`box ${i} (${b.tag}) has bad size/position`);
  if (b.part !== 'head') problems.push(`box ${i} (${b.tag}) part is ${b.part}, expected head`);
});
const { tw, th } = packFaces(boxes, d.S);
// paint: per face, which texels are painted, and bad colours
const painted = new Map();
let badCol = 0, texels = 0;
for (const b of boxes) for (const q of polys(b)) {
  const [u2, v1] = q.uv[0], [u1] = q.uv[1], [, v2] = q.uv[2];
  const V0 = q.verts[0], V1 = q.verts[1], V2 = q.verts[2];
  const du = V0.map((c, i) => c - V1[i]), dv = V2.map((c, i) => c - V1[i]);
  const fw = Math.hypot(...du), fh = Math.hypot(...dv), S = d.S, cell = d.cell;
  let any = false;
  for (let j = Math.floor(Math.min(v1, v2) * S + 1e-6); j < Math.ceil(Math.max(v1, v2) * S - 1e-6); j++) for (let i = Math.floor(Math.min(u1, u2) * S + 1e-6); i < Math.ceil(Math.max(u1, u2) * S - 1e-6); i++) {
    const a = ((i + 0.5) / S - u1) / (u2 - u1), bb = ((j + 0.5) / S - v1) / (v2 - v1);
    if (!(a >= 0 && a <= 1 && bb >= 0 && bb <= 1)) continue;
    let eu = a * fw, ev = bb * fh;
    if (cell) { eu = Math.min(fw, (Math.floor(eu / cell) + 0.5) * cell); ev = Math.min(fh, (Math.floor(ev / cell) + 0.5) * cell); }
    const ea = fw ? eu / fw : 0, eb = fh ? ev / fh : 0;
    const p = [0, 1, 2].map(k => V1[k] + ea * du[k] + eb * dv[k]);
    let col;
    try { col = b.mat({ p, face: q.face, eu, ev, fw, fh, ex: Math.min(eu, fw - eu, ev, fh - ev), px: cell || 1 / S, box: b }); }
    catch (e) { problems.push(`${b.tag} ${q.face}: mat threw ${e.message}`); col = null; }
    texels++;
    if (col == null) continue;
    if (!Array.isArray(col) || col.length < 3 || !col.slice(0, 3).every(Number.isFinite) || col.some(v => v < -1 || v > 400)) { badCol++; if (badCol < 4) problems.push(`${b.tag} ${q.face}: bad colour ${JSON.stringify(col)}`); continue; }
    any = true;
  }
  painted.set(b.tag + '|' + q.face + '|' + boxes.indexOf(b), any);
}
// clearance vs the hat layer (x/z +-4.5, y -8.5..0.5): outward, painted faces of unrotated boxes
const E = 1e-6, near = [], inside = [];
const ov = (a0, a1, b0, b1) => a1 > b0 + E && a0 < b1 - E;
boxes.forEach((b, i) => {
  if (b.rot || b.xf) return;
  const x1 = b.x, x2 = b.x + b.w, y1 = b.y, y2 = b.y + b.h, z1 = b.z, z2 = b.z + b.d;
  const check = (face, dist, spanOk) => {
    if (!painted.get(b.tag + '|' + face + '|' + i) || !spanOk) return;
    if (dist < 4.5 - E) inside.push(`${b.tag} ${face} at ${dist}`);
    else if (dist < 4.75 - E) near.push(`${b.tag} ${face} at ${dist}`);
  };
  const inYZ = ov(y1, y2, -8.5, 0.5) && ov(z1, z2, -4.5, 4.5), inXY = ov(x1, x2, -4.5, 4.5) && ov(y1, y2, -8.5, 0.5), inXZ = ov(x1, x2, -4.5, 4.5) && ov(z1, z2, -4.5, 4.5);
  if (x2 > 0) check('left', x2, inYZ);
  if (x1 < 0) check('right', -x1, inYZ);
  if (z1 < 0) check('front', -z1, inXY);
  if (z2 > 0) check('back', z2, inXY);
  check('top', -4 - y1, inXZ && ov(y1, y1, -99, 0.5));                     // top face over the hat top (-8.5) must be at or above -8.75
});
// what sits in front of the eyes (skin eyes: x 1..3 and -3..-1, y -4..-2, face at z -4)
const eyes = boxes.filter(b => b.z < -4 && (ov(b.x, b.x + b.w, -3, -1) || ov(b.x, b.x + b.w, 1, 3)) && ov(b.y, b.y + b.h, -4, -2)).map(b => b.tag);
const xs = boxes.flatMap(b => [b.x, b.x + b.w]), ys = boxes.flatMap(b => [b.y, b.y + b.h]), zs = boxes.flatMap(b => [b.z, b.z + b.d]);
const out = {
  ok: problems.length === 0 && badCol === 0 && inside.length === 0,
  boxes: boxes.length, rotated: boxes.filter(b => b.rot).length, texture: `${tw * d.S}x${th * d.S}`, texels,
  thin: boxes.filter(b => Math.min(b.w, b.h, b.d) < 0.5 - E).length,
  offGrid: boxes.filter(b => !b.rot && [b.w, b.h, b.d].some(s => Math.abs(s * 4 - Math.round(s * 4)) > 1e-3)).map(b => b.tag),
  bbox: `x ${Math.min(...xs)}..${Math.max(...xs)}  y ${Math.min(...ys)}..${Math.max(...ys)}  z ${Math.min(...zs)}..${Math.max(...zs)}`,
  badColours: badCol, problems: [...new Set(problems)].slice(0, 20),
  facesInsideHatLayer: [...new Set(inside)].slice(0, 20), facesCloserThanQuarterPx: [...new Set(near)].slice(0, 20),
  coversEyes: [...new Set(eyes)],
};
console.log(JSON.stringify(out, null, 1));
process.exit(out.ok ? 0 : 1);
