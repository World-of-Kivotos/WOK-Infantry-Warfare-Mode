// ===== Helmet kit: voxel shell generator shared by every helmet design =====
// MC head space: head x -4..4, y -8..0 (y down), z -4..4, front = -z. The skin hat layer is the box
// x/z +-4.5, y -8.5..0.5; every visible shell surface sits >= 0.25 outside it (walls at 5.0, cap above -8.75).
//
// shell(o) fills a grid (o.g: 0.5 for M / B, 1 for A) with the helmet shell and greedy-merges it into boxes:
//   o.hx          outer half-width (x)
//   o.zf, o.zb    outer front / back extent (front = -zf, back = +zb)
//   o.dome        [[yTop, inset, cornerR], ...] layers above the hat top, top-most first; each layer runs from
//                 its yTop down to the next layer's yTop (the last one down to -8.5)
//   o.corner      plan corner radius of the walls below -8.5
//   o.rim(x, z)   y of the lower edge at that plan position (a cell is kept when its bottom is above it)
//   o.hollow      true: keep the inside of the hat box empty (walls one cell thick)
//   o.cut(x,y,z)  optional: return true to drop a cell (ear cut-outs, visor openings ...)
function shellCells(o) {
  const g = o.g, cells = new Set(), key = (i, j, k) => i + ',' + j + ',' + k;
  const x0 = -o.hx, z0 = -o.zf, y0 = o.dome.length ? o.dome[0][0] : -8.5;
  const NI = Math.round(2 * o.hx / g), NK = Math.round((o.zf + o.zb) / g), NJ = Math.round((o.yMax - y0) / g);
  const layer = y => {
    if (y >= -8.5) return [0, o.corner];
    let L = o.dome[0];
    for (const d of o.dome) if (y >= d[0]) L = d;
    return [L[1], L[2]];
  };
  const hatX = g >= 1 ? 4 : 4.5, hatY = g >= 1 ? -8 : -8.5;
  for (let j = 0; j < NJ; j++) for (let i = 0; i < NI; i++) for (let k = 0; k < NK; k++) {
    const cx = x0 + (i + 0.5) * g, cy = y0 + (j + 0.5) * g, cz = z0 + (k + 0.5) * g;
    const [inset, r] = layer(cy - g / 2 + 1e-6);
    const hx = o.hx - inset, zf = o.zf - inset, zb = o.zb - inset;
    if (Math.abs(cx) > hx || cz < -zf || cz > zb) continue;
    if (r > 0) {
      const dx = Math.abs(cx) - (hx - r), dz = cz < 0 ? -cz - (zf - r) : cz - (zb - r);
      if (dx > 0 && dz > 0 && dx * dx + dz * dz > r * r + 1e-6) continue;
    }
    if (o.hollow !== false && Math.abs(cx) < hatX && Math.abs(cz) < hatX && cy > hatY) continue;
    if (cy + g / 2 > o.rim(cx, cz) + 1e-6) continue;
    if (o.cut && o.cut(cx, cy, cz)) continue;
    cells.add(key(i, j, k));
  }
  return { cells, g, x0, y0, z0, NI, NJ, NK, key };
}
// greedy merge: grow along x, then z, then y
function mergeCells(S, part, mat, tag) {
  const { cells, g, x0, y0, z0, NI, NJ, NK, key } = S, used = new Set(), out = [];
  const ok = (i, j, k) => cells.has(key(i, j, k)) && !used.has(key(i, j, k));
  for (let j = 0; j < NJ; j++) for (let k = 0; k < NK; k++) for (let i = 0; i < NI; i++) {
    if (!ok(i, j, k)) continue;
    let i1 = i; while (i1 + 1 < NI && ok(i1 + 1, j, k)) i1++;
    let k1 = k; grow: while (k1 + 1 < NK) { for (let a = i; a <= i1; a++) if (!ok(a, j, k1 + 1)) break grow; k1++; }
    let j1 = j; growY: while (j1 + 1 < NJ) { for (let a = i; a <= i1; a++) for (let c = k; c <= k1; c++) if (!ok(a, j1 + 1, c)) break growY; j1++; }
    for (let a = i; a <= i1; a++) for (let b = j; b <= j1; b++) for (let c = k; c <= k1; c++) used.add(key(a, b, c));
    out.push(box(part, [x0 + i * g, y0 + j * g, z0 + k * g], [x0 + (i1 + 1) * g, y0 + (j1 + 1) * g, z0 + (k1 + 1) * g], mat, { tag }));
  }
  return out;
}
function shell(o, mat, tag = 'shell') {
  return mergeCells(shellCells({ yMax: 1, ...o }), 'head', mat, tag);
}
// piecewise-linear rim height along z (front -> back) with a per-point list [[z, y], ...]; used as o.rim(x, z)
function rimAlong(pts) {
  return (x, z) => {
    if (z <= pts[0][0]) return pts[0][1];
    for (let i = 1; i < pts.length; i++) if (z <= pts[i][0]) { const [za, ya] = pts[i - 1], [zb, yb] = pts[i]; return ya + (yb - ya) * (z - za) / (zb - za); }
    return pts[pts.length - 1][1];
  };
}
// shell paint helpers
const smoothstep = (a, b, t) => sm(clamp01((t - a) / (b - a)));
// y of the actual (grid-stepped) lower edge under a painted texel of a shell box: the texel is nudged into its cell
const NUDGE = { left: [-1, 0, 0], right: [1, 0, 0], front: [0, 0, 1], back: [0, 0, -1], top: [0, 1, 0], bottom: [0, -1, 0] };
function colBottom(c, o) {
  const n = NUDGE[c.face], p = c.p.map((v, k) => v + n[k] * 1e-3);
  const cx = Math.floor((p[0] + o.hx) / o.g) * o.g - o.hx + o.g / 2, cz = Math.floor((p[2] + o.zf) / o.g) * o.g - o.zf + o.g / 2;
  const y0 = o.dome.length ? o.dome[0][0] : -8.5;
  return y0 + o.g * Math.floor((o.rim(cx, cz) - y0) / o.g + 1e-6);
}
// straight strap between two points on a head side plane (x = xs .. xs + t), as one box turned about x
function sideStrap(out, xs, t, a, b, wid, mat, tag) {
  const dz = b[1] - a[1], dy = b[0] - a[0], L = Math.hypot(dz, dy), th = Math.atan2(dz, dy) * 180 / Math.PI;
  out.push(box('head', [xs, a[0], a[1] - wid / 2], [xs + t, a[0] + L, a[1] + wid / 2], mat, { rot: [th, 0, 0], pivot: [xs + t / 2, a[0], a[1]], tag }));
}
