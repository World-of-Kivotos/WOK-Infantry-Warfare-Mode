// Export every helmet design (mode M) as a GeckoLib model + texture into the mod resources.
//   node export.mjs [assetsDir] [--only <id>]
// assetsDir defaults to ../../src/main/resources/assets/wok_infantry_armor
//
// Writes geo/helmet_<id>.geo.json and textures/models/armor/helmet_<id>_layer_1.png.
// Coordinates: designs are in MC head space (y down, front -z); GeckoLib files use
//   file = (x, 24 - y, z), cube rotation [rx, ry, rz] unchanged, pivot (px, 24 - py, pz)
// (GeckoLib bakes x-flipped with rotation (-rx, -ry, rz) and GeoArmorRenderer draws through translate(0, 1.5, 0) +
// scale(-1, -1, 1), which lands back on exactly these values). Faces use per-face UV; each face keeps the same vertex
// order and UV corners as vanilla ModelPart (checked against GeckoLib 4.8.4 VertexSet / GeoQuad), so the texels are
// painted with the same face math the preview uses. Faces with no opaque texel are left out of the file.
// Boxes tagged 'visor glass' go into their own bone, which HelmetGeoRenderer draws on a translucent pass.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const here = new URL('./', import.meta.url);
const argv = process.argv.slice(2);
const onlyAt = argv.indexOf('--only'), only = onlyAt >= 0 ? argv[onlyAt + 1] : null;
const assets = path.resolve(argv.find((a, i) => !a.startsWith('--') && argv[i - 1] !== '--only') ||
  fileURLToPath(new URL('../../src/main/resources/assets/wok_infantry_armor', here)));

// ---- load designs.js + _kit.js + every design into one module (a design may build on another one)
const r = f => fs.readFileSync(new URL(f, here), 'utf8').replace(/^﻿/, '');
const designFiles = fs.readdirSync(new URL('./helmets/', here)).filter(f => /^[^_].*\.js$/.test(f)).sort();
const tmp = new URL('./.export_tmp.mjs', here);
fs.writeFileSync(tmp, [r('./designs.js'), r('./helmets/_kit.js'), ...designFiles.map(f => r('./helmets/' + f))].join('\n'));
let ARMORS;
try { ({ ARMORS } = await import(tmp.href + '?t=' + process.hrtime.bigint())); } finally { fs.unlinkSync(tmp); }
const ids = designFiles.map(f => f.replace(/\.js$/, '')).filter(id => !only || id === only);
if (!ids.length) throw new Error('no design ' + (only || ''));

// ---- face geometry: vanilla ModelPart.Cube vertex order (same as viewer.js polys)
const FACES = ['top', 'bottom', 'right', 'front', 'left', 'back'];
const GECKO_FACE = { front: 'north', back: 'south', left: 'west', right: 'east', top: 'up', bottom: 'down' };
function faceVerts(b, face) {
  const g = b.grow || 0;
  const x1 = b.x - g, y1 = b.y - g, z1 = b.z - g, x2 = b.x + b.w + g, y2 = b.y + b.h + g, z2 = b.z + b.d + g;
  const V = [[x1, y1, z1], [x2, y1, z1], [x2, y2, z1], [x1, y2, z1], [x1, y1, z2], [x2, y1, z2], [x2, y2, z2], [x1, y2, z2]];
  const ids = { top: [5, 4, 0, 1], bottom: [2, 3, 7, 6], right: [0, 4, 7, 3], front: [1, 0, 3, 2], left: [5, 1, 2, 6], back: [4, 5, 6, 7] }[face];
  return ids.map(i => V[i]);              // [0] carries (u2, v1), [1] (u1, v1), [2] (u1, v2), [3] (u2, v2)
}
const faceDims = (b, face) => ({ top: [b.w, b.d], bottom: [b.w, b.d], right: [b.d, b.h], left: [b.d, b.h], front: [b.w, b.h], back: [b.w, b.h] }[face]);

// paint one face into its own texel grid (texel (k, l) = column k from the u1 edge, row l from the v1 edge)
function paintFace(b, face, S, cell) {
  const [fwBox, fhBox] = faceDims(b, face);
  const pw = Math.max(1, Math.ceil(fwBox * S - 1e-6)), ph = Math.max(1, Math.ceil(fhBox * S - 1e-6));
  const [V0, V1, V2] = faceVerts(b, face);
  const du = V0.map((c, i) => c - V1[i]), dv = V2.map((c, i) => c - V1[i]);
  const fw = Math.hypot(...du), fh = Math.hypot(...dv);
  const px = new Array(pw * ph).fill(null);
  let any = false;
  for (let l = 0; l < ph; l++) for (let k = 0; k < pw; k++) {
    const a = ((k + 0.5) / S) / fwBox, bb = ((l + 0.5) / S) / fhBox;
    if (a > 1 || bb > 1) continue;
    let eu = a * fw, ev = bb * fh;
    if (cell) { eu = Math.min(fw, (Math.floor(eu / cell) + 0.5) * cell); ev = Math.min(fh, (Math.floor(ev / cell) + 0.5) * cell); }
    const ea = fw ? eu / fw : 0, eb = fh ? ev / fh : 0;
    const p = [0, 1, 2].map(i => V1[i] + ea * du[i] + eb * dv[i]);
    const col = b.mat({ p, face, eu, ev, fw, fh, ex: Math.min(eu, fw - eu, ev, fh - ev), px: cell || 1 / S, box: b });
    if (col == null) continue;
    if (!col.slice(0, 3).every(Number.isFinite)) throw new Error(`${b.tag} ${face}: bad colour ${col}`);
    px[l * pw + k] = col.slice(0, 3).map(v => Math.max(0, Math.min(255, Math.round(v))));
    any = true;
  }
  return { pw, ph, fw: fwBox, fh: fhBox, px, any };
}

// shelf packing, tallest first; atlas width 64 texture units (x S texels), height a power of two
function pack(faces, S) {
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
    if (ok && used <= W) { let H = 16 * S; while (H < used) H *= 2; return { W, H }; }
    W *= 2;
  }
}

// ---- minimal RGBA PNG writer
const CRC = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
const crc32 = buf => { let c = -1; for (const byte of buf) c = CRC[(c ^ byte) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
function png(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

const q = v => Math.round(v * 10000) / 10000;
const summary = [];
for (const id of ids) {
  const d = ARMORS[id]('M');
  const S = d.S, cell = d.cell;
  const faces = [];
  for (const b of d.boxes) for (const face of FACES) {
    const F = paintFace(b, face, S, cell);
    if (F.any) faces.push({ ...F, b, face });
  }
  const { W, H } = pack(faces, S);
  const rgba = Buffer.alloc(W * H * 4);
  for (const F of faces) for (let l = 0; l < F.ph; l++) for (let k = 0; k < F.pw; k++) {
    const c = F.px[l * F.pw + k];
    if (!c) continue;
    const o = ((F.y + l) * W + F.x + k) * 4;
    rgba[o] = c[0]; rgba[o + 1] = c[1]; rgba[o + 2] = c[2]; rgba[o + 3] = 255;
  }
  // cubes, grouped into the opaque shell bone and the translucent glass bone
  const uvOf = new Map();
  for (const F of faces) {
    if (!uvOf.has(F.b)) uvOf.set(F.b, {});
    uvOf.get(F.b)[GECKO_FACE[F.face]] = { uv: [q(F.x / S), q(F.y / S)], uv_size: [q(F.fw), q(F.fh)] };
  }
  const cube = b => {
    const c = { origin: [q(b.x), q(24 - b.y - b.h), q(b.z)], size: [q(b.w), q(b.h), q(b.d)] };
    if (b.grow) c.inflate = q(b.grow);
    if (b.rot && b.rot.some(v => v)) {
      const pv = b.pivot || [b.x + b.w / 2, b.y + b.h / 2, b.z + b.d / 2];
      c.pivot = [q(pv[0]), q(24 - pv[1]), q(pv[2])];
      c.rotation = b.rot.map(q);
    }
    c.uv = uvOf.get(b);
    return c;
  };
  const drawn = d.boxes.filter(b => uvOf.has(b));
  const glass = drawn.filter(b => b.tag === 'visor glass'), solid = drawn.filter(b => b.tag !== 'visor glass');
  const bones = [
    { name: 'armorHead', pivot: [0, 24, 0] },
    { name: 'helmet_shell', parent: 'armorHead', pivot: [0, 24, 0], cubes: solid.map(cube) },
  ];
  if (glass.length) bones.push({ name: 'visor_glass', parent: 'armorHead', pivot: [0, 24, 0], cubes: glass.map(cube) });
  const geo = {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: `geometry.wok_infantry_armor.helmet_${id}`, texture_width: W / S, texture_height: H / S,
        visible_bounds_width: 3, visible_bounds_height: 3, visible_bounds_offset: [0, 1.5, 0] },
      bones,
    }],
  };
  fs.mkdirSync(path.join(assets, 'geo'), { recursive: true });
  fs.mkdirSync(path.join(assets, 'textures', 'models', 'armor'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'geo', `helmet_${id}.geo.json`), JSON.stringify(geo, null, 1) + '\n');
  fs.writeFileSync(path.join(assets, 'textures', 'models', 'armor', `helmet_${id}_layer_1.png`), png(W, H, rgba));
  summary.push(`${id}: ${solid.length} cubes${glass.length ? ` + ${glass.length} glass` : ''}, ${faces.length} faces, texture ${W}x${H} (uv ${W / S}x${H / S})`);
}
console.log(summary.join('\n'));
