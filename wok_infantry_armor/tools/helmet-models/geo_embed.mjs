// Embed the shipped GeckoLib helmets (geo.json + layer PNG) as CURRENT_GEO for the "now" cards.
//   node geo_embed.mjs <assets/wok_infantry_armor dir>
// Each cube becomes a viewer box in MC model space (y down, front -z) with its bone chain as transforms:
//   mc = (fileX, 24 - fileY, fileZ); bone rotation [rx, ry, rz] (deg) about (px, 24 - py, pz), applied Z*Y*X
//   (GeckoLib bakes (-rx, -ry, rz) in its x-flipped space; GeoArmorRenderer renders that through scale(-1,-1,1)).
import fs from 'node:fs';
import path from 'node:path';
const dir = process.argv[2];
if (!dir) throw new Error('usage: node geo_embed.mjs <assets dir>');
const out = {};
for (const f of fs.readdirSync(path.join(dir, 'geo')).filter(n => /^helmet_.*\.geo\.json$/.test(n)).sort()) {
  const key = f.replace(/^helmet_/, '').replace(/\.geo\.json$/, '');
  const g = JSON.parse(fs.readFileSync(path.join(dir, 'geo', f), 'utf8'))['minecraft:geometry'][0];
  const byName = Object.fromEntries(g.bones.map(b => [b.name, b]));
  const chain = b => {
    const xf = [];
    for (let n = b; n; n = n.parent ? byName[n.parent] : null) {
      if (n.rotation && n.rotation.some(v => v)) xf.push({ rot: n.rotation, pivot: [n.pivot[0], 24 - n.pivot[1], n.pivot[2]] });
    }
    return xf;                                                  // innermost bone first
  };
  const boxes = [];
  const FACE = { north: 'front', south: 'back', west: 'left', east: 'right', up: 'top', down: 'bottom' };
  let perFace = false;
  for (const b of g.bones) for (const c of b.cubes || []) {
    const [x0, y0, z0] = c.origin, [w, h, d] = c.size, inf = c.inflate || 0;
    const box = { bone: b.name, tag: b.name === 'visor_glass' ? 'visor glass' : b.name, x: x0, y: 24 - y0 - h, z: z0, w, h, d, grow: inf, mirror: !!c.mirror,
      xf: [...(c.rotation && c.rotation.some(v => v) ? [{ rot: c.rotation, pivot: [c.pivot[0], 24 - c.pivot[1], c.pivot[2]] }] : []), ...chain(b)] };
    if (Array.isArray(c.uv)) { box.u = c.uv[0]; box.v = c.uv[1]; }
    else {                                                     // per-face UV: faces left out of the file are not drawn
      perFace = true;
      box.faceUV = {};
      for (const [k, f] of Object.entries(c.uv)) box.faceUV[FACE[k]] = [f.uv[0], f.uv[1], f.uv[0] + f.uv_size[0], f.uv[1] + f.uv_size[1]];
      box.noFace = Object.values(FACE).filter(f => !box.faceUV[f]);
    }
    boxes.push(box);
  }
  const png = fs.readFileSync(path.join(dir, 'textures', 'models', 'armor', `helmet_${key}_layer_1.png`));
  // perFace: files written by export.mjs, drawn at true size (the old box-UV models went through withScale(1.10, 1.06))
  out[key] = { tw: g.description.texture_width, th: g.description.texture_height, tex: 'data:image/png;base64,' + png.toString('base64'), boxes, perFace };
}
fs.writeFileSync(new URL('./current_geo.js', import.meta.url), 'export const CURRENT_GEO = ' + JSON.stringify(out) + ';\n');
console.log('ok', Object.keys(out).length, Object.keys(out).join(','));
