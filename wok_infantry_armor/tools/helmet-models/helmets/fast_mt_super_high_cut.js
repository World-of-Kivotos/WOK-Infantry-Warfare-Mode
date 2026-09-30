// ================= Ops-Core FAST MT Super High Cut (Black) — EFT reference (tarkov.dev 5a154d5cfcdbcb001a3b00da) =================
// Black textured UHMWPE shell, rounded crown. Super high cut: the brim sits on the brow, then just behind the temple
// the edge jumps up and runs high over the ear (the sides of the head show), dropping again behind the ear to cover
// the back of the head. On each side an ARC accessory rail follows the cut above the ear (dark polymer, a row of
// slots). Front: the skeletonised NVG shroud (a raised frame with three openings) right above the brim, and a thin
// blue-grey shock cord running along the brim from the shroud to the rails. Loop Velcro on the crown (lighter,
// speckled). Olive-grey harness: a front strap from under the rail and a rear strap from behind the ear meet at a
// buckle by the jaw (the chin cup is left out: it would cut into the body when the head tilts).
ARMORS.fast_mt_super_high_cut = function (mode) {
  const K = kit(mode), { hd, mid } = K, fine = hd && !mid;
  const g = mode === 'A' ? 1 : fine ? 0.25 : 0.5;
  const B = [];

  // ---- palette (albedo). Reference render: crown ~#4f4b4a, sides ~#4d4640, rails ~#383737, shroud ~#3d3f40 with
  //      worn metal edges ~#837b72, shock cord ~#546a6e, harness ~#3c3b33
  const SH = hex('#3d3c3b'), SHL = hex('#4f4e4c'), SPECK = hex('#6a6b69');
  const TRIM = hex('#232424'), LOOP = hex('#57585a'), LOOPD = hex('#3f4042');
  const RAIL = hex('#333434'), RAILD = hex('#141515'), RAILL = hex('#5b5c5b');
  const SHR = hex('#3c3e3f'), SHRD = hex('#101314'), SHRL = hex('#7a756d');
  const CORD = hex('#5d767b'), WEB = hex('#48473d'), WEBL = hex('#5f5d50'), BUCKLE = hex('#2a2b2b');

  // ---- shell: brim half a pixel above the eyes (the MC forehead is tall), super high cut over the ears, back down
  //      to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -6.5], [1, -6.5], [2.5, -3], [3.5, -1.5], [5.5, -1.5]]);
  const dome = mode === 'A' ? [[-10, 2, 1], [-9, 0, 2]]
    : fine ? [[-10, 2.5, 1.75], [-9.75, 1.5, 2], [-9.5, 1, 2], [-9.25, 0.75, 2], [-9, 0.5, 2], [-8.75, 0.25, 2]]
    : [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);
  const trimW = mode === 'A' ? 1 : fine ? 0.25 : 0.5;
  const cordW = mode === 'A' ? 0 : fine ? 0.25 : 0.5;
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < trimW) return mul(TRIM, (fine && dy < 0.125 ? 1.25 : 1) * K.G(c, 0.05));
    // shock cord along the brim: front, round the front corners to the rail fronts
    const brimZone = z < -3.25 && colBottom(c, S) > -6;
    if (cordW && brimZone && dy < trimW + cordW && (c.face !== 'front' || Math.abs(x) > 1.5)) return mul(CORD, K.G(c, 0.05));
    // loop Velcro on the crown (top faces and the upper ring of the dome)
    if (y < -9.25 && Math.abs(x) < 3.5 && z > -2.5 && z < 3.5) {
      if (!hd) return mul(LOOP, 0.95 + 0.1 * tex(c, 5));
      return mix(LOOPD, LOOP, 0.35 + 0.65 * tex(c, 5));
    }
    // textured black paint: fine speckle, slightly lighter towards the crown
    let col = mix(SH, SHL, clamp01(0.3 + 0.5 * smoothstep(-6, -9.5, y) + (fbm(x * 0.4 + 5, y * 0.4, z * 0.4 + 9, 2) - 0.5) * 0.6));
    if (tex(c, 3) > (hd ? (mid ? 0.94 : 0.95) : 0.95)) col = mix(col, SPECK, hd ? 0.3 : 0.2);
    if (fine && tex(c, 7) > 0.995) col = mix(col, hex('#8d8f8c'), 0.5);        // edge wear chips
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- ARC rails on the sides, along the cut above the ear
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const outer = s < 0 ? 'right' : 'left';
    const rail = c => {
      if (c.face === (s < 0 ? 'left' : 'right')) return null;                 // lies on the shell
      if (c.face !== outer) return mul(RAIL, c.face === 'top' ? 1.2 : 0.85);
      const [, y, z] = c.p;
      const t = ((z + 3) % 1 + 1) % 1;                                          // slot pitch 1 px
      const slotRow = mode === 'A' ? y > -8 && y < -7 : y > -7.5 && y < -7;
      if (slotRow && z > -2.5 && z < 1 && t > 0.5) return mul(RAILD, K.G(c, 0.05));
      if (hd && c.ev < K.px) return mul(RAILL, 0.9);                            // top lip catches the light
      return mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -8, -3], [xb, -6.5, 1.5], rail, { tag: 'ARC rail' }));
  }

  // ---- skeletonised NVG shroud above the brim
  const shroud = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.25 : 0.8);
    const [x, y] = c.p, ax = Math.abs(x);
    if (mode === 'A') return ax < 1 && y > -7.5 && y < -6.5 ? SHRD : mul(SHR, K.G(c, 0.06));   // one 2 px opening
    // three openings: a centre slot and two side windows narrowing downwards, split by 0.5 px bars
    const centre = ax < 0.5 && y > -7.5 && y < -6;
    const side = ax > 1 && ax < (fine ? 1.75 : 1.5) - (fine ? Math.max(0, y + 6.75) * 0.5 : 0) && y > -7.5 && y < -6.25;
    if (centre || side) return mul(SHRD, K.G(c, 0.05));
    if (c.ex < K.px) return mul(SHRL, c.ev < K.px ? 1.05 : 0.85);             // worn metal edge
    return mul(SHR, K.G(c, 0.06));
  };
  B.push(box('head', [-2, -8, -6], [2, -5.5, -5.5], shroud, { tag: 'NVG shroud' }));

  // ---- harness (M / B): front strap from under the rail, rear strap from behind the ear, buckle by the jaw
  if (mode !== 'A') {
    const web = c => {
      if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
      let col = mul(WEB, K.G(c, 0.06));
      if (fine && c.ex < 0.125) col = mul(WEBL, 0.95);
      return col;
    };
    const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.3 : 1);
    for (const s of [-1, 1]) {
      const xs = s < 0 ? -4.75 : 4.5;
      sideStrap(B, xs, 0.25, [-6.5, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
      sideStrap(B, xs, 0.25, [-3, 2.75], [-0.75, -0.5], 0.5, web, 'rear strap');
      const bx = s < 0 ? -5 : 4.5;
      B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
    }
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
