// ================= Kiver-M bulletproof helmet — EFT reference (tarkov.dev 5645bc214bdc2d363b8b4571) =================
// A round, deep titanium/aramid helmet in an olive green fabric cover with a slight leather sheen. The brim sits on the
// brow; behind the temples the sides drop into ear flaps that cover the ears down to the jaw, and the back covers the
// nape. Panel seams (double stitched welts) run over the crown from front to back; on each side an arched seam marks
// the side panel and a smaller arch the padded ear piece. A dark teal binding wraps the whole edge, with near-black
// padding just inside it. A black plastic visor clip sits on each side near the front, above the face opening.
// A black leather chin strap hangs from inside the front of each ear flap (the cup under the chin is left out).
ARMORS['kiver_m_heavy'] = function (mode) {
  const K = kit('M'), g = 0.5;
  const B = [];

  // ---- palette (albedo). Reference render: crown ~#68755f (lit), upper side ~#3b3c35, binding ~#2a3731 (lit ~#404b42),
  //      inner padding ~#141515, clips ~#2f3031 with highlights ~#4c4e51, chin strap ~#1d2221
  const KV = hex('#5b6951'), KVL = hex('#72815f'), KVD = hex('#46523e'), SEAM = hex('#343d2d'), WELT = hex('#7f8a68');
  const BIND = hex('#283a33'), BINDL = hex('#3a534a'), PAD = hex('#161718'), MESH = hex('#232526');
  const CLIP = hex('#222426'), CLIPL = hex('#4a4d50'), CLIPD = hex('#111213');
  const LEATHER = hex('#1d2121'), LEATHERL = hex('#2f3434');

  // ---- shell: round and deep. Brim on the brow, ear flaps from behind the temple down to the jaw, back to the nape.
  const rim = rimAlong([[-5.5, -4.5], [-4.25, -4.5], [-3.25, -2], [-2.75, -1.25], [-2, -1], [2.25, -1], [3.75, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]];
  // the last row of each ear flap steps in half a pixel, so the flaps round off towards the jaw
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim, cut: (x, y, z) => y > -1.5 && Math.abs(x) > 5 };

  // distance from a plan point to the outer outline (rounded rectangle, positive inside)
  const inPlan = (x, z) => {
    const qx = Math.abs(x) - 3.5, qz = Math.abs(z) - 3.5;
    return qx > 0 && qz > 0 ? 2 - Math.hypot(qx, qz) : Math.min(5.5 - Math.abs(x), 5.5 - Math.abs(z));
  };
  // thin elliptical seam on the side walls: true when the curve passes through this texel
  const arch = (z, y, zc, yc, rz, ry) => Math.abs((Math.hypot((z - zc) / rz, (y - yc) / ry) - 1) * Math.min(rz, ry)) < 0.26;
  const cover = c => {
    const [x, y, z] = c.p;
    const n = fbm(x * 0.45 + 3, y * 0.5 + 7, z * 0.45 + 1, 2);
    let col = mix(KVD, KVL, clamp01(0.4 + (n - 0.5) * 1.1 + 0.25 * smoothstep(-5, -9.5, y)));
    if (hash3(Math.floor(x * 2 + 900), Math.floor(y * 2 + 900), Math.floor(z * 2 + 900)) > 0.96) col = mul(col, 0.9);   // weave
    return col;
  };
  const paint = c => {
    const [x, y, z] = c.p, ax = Math.abs(x);
    if (c.face === 'bottom') return inPlan(x, z) < 0.5 ? mul(BIND, K.G(c, 0.05)) : mul(PAD, K.G(c, 0.06));
    // face opening: the flap fronts show the binding outside and the padding inside
    if (c.face === 'front' && y > -4.5) return ax > 5 ? mul(BINDL, 0.9 * K.G(c, 0.05)) : mul(((Math.floor(y * 2) + Math.floor(ax * 2)) & 1) ? MESH : PAD, 1);
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(BIND, K.G(c, 0.05));                          // edge binding
    let col = cover(c);
    // crown panel seams (welts running front to back), double stitched
    const onRun = c.face === 'top' || (c.face === 'front' && y < -5) || (c.face === 'back' && y < -2);
    if (onRun && ax >= 1.5 && ax < 2) return mul(mix(col, SEAM, 0.55), K.G(c, 0.04));
    if (onRun && ax >= 2 && ax < 2.5) return mix(col, WELT, 0.35);
    if (c.face === 'left' || c.face === 'right' || ((c.face === 'front' || c.face === 'back') && ax > 3.5)) {
      // side panel arch and the ear piece arch
      if (arch(z, y, 0.5, -2.5, 4.25, 4.75) && y < -4) return mix(col, SEAM, 0.5);
      if (arch(z, y, 0.25, -0.75, 2.75, 3.25)) return mix(col, SEAM, 0.55);
      if (Math.hypot((z - 0.25) / 2.75, (y + 0.75) / 3.25) < 1) col = mul(col, 0.94 + 0.1 * smoothstep(-1.5, -3.5, y));   // padded ear piece
    }
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- visor clips: black plastic, one on each side near the front, above the face opening
  for (const s of [-1, 1]) {
    const out = s < 0 ? 'right' : 'left', inn = s < 0 ? 'left' : 'right';
    const [pa, pb] = s < 0 ? [-5.75, -5.5] : [5.5, 5.75];
    const [la, lb] = s < 0 ? [-6, -5.75] : [5.75, 6];
    const plate = c => {
      if (c.face === inn) return null;
      if (c.face !== out) return mul(CLIP, c.face === 'top' ? 1.4 : 0.9);
      return c.ex < 0.5 ? mul(CLIPL, 0.85) : CLIP;
    };
    const lever = c => {
      if (c.face === inn) return null;
      if (c.face !== out) return mul(CLIP, c.face === 'top' ? 1.6 : 1);
      const y = c.p[1];
      if (y < -6.5) return CLIPD;                                            // strap loop at the top
      return (Math.floor(y * 2) & 1) ? CLIPL : mul(CLIP, 1.1);              // ridged grip
    };
    B.push(box('head', [pa, -7.25, -3.5], [pb, -5.25, -2.5], plate, { tag: 'visor clip' }));
    B.push(box('head', [la, -7, -3.25], [lb, -5.5, -2.75], lever, { tag: 'visor clip' }));
  }

  // ---- black leather chin strap from inside the front of each ear flap, with a snap tab at the end
  const leather = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(LEATHER, 0.85);
    return c.ex < 0.25 ? LEATHERL : mul(LEATHER, K.G(c, 0.08));
  };
  const tab = c => mul(c.face === 'top' ? LEATHERL : LEATHER, 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-2.5, -2.5], [-0.5, -3.25], 0.5, leather, 'chin strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -0.75, -3.75], [bx + 0.5, -0.25, -3], tab, { tag: 'strap tab' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
