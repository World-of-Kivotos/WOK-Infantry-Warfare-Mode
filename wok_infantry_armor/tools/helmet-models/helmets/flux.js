// ================= MTEK FLUX Ballistic helmet (Olive Drab) — EFT reference (tarkov.dev 6759655674aa5e0825040d62) =================
// Same MTEK high-cut shell as the STRIKE (level brim back to the temple, cut over the ear, back down to the nape,
// rolled lip round the edge), painted yellowish olive with light wear marks, but kitted out: large black loop Velcro
// panels (a band over the crown front, the crown rear, the upper sides, strips along the front brim, the back), a
// black NVG shroud (raised centre plate with a U-shaped window, wings with screws, a top cap), black accessory rails
// with slot rows, two windows and a slotted screw on each side above the cut, and a black bungee cord from the shroud
// along the brim to the rails. Dark mesh pads show under the cut. Black webbing harness: a front and a rear strap meet
// at a buckle by the jaw (no chin cup: it would cut into the body when the head tilts).
ARMORS.flux = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: lit crown ~#787650,
  //      shaded lip ~#474632, Velcro ~#2e2e2e..#404243, rails ~#292929 with slots ~#0f1112, shroud ~#343639 with
  //      worn edges, mesh pads ~#252526, webbing ~#232928
  const SHL = hex('#7f7c50'), SHD = hex('#5d5d39'), SCUFF = hex('#aeaa84');
  const LIP = hex('#666442'), GROOVE = hex('#3d3e28');
  const VEL = hex('#343537'), VELD = hex('#28292b'), VELL = hex('#47484a'), VELE = hex('#1f2021');
  const SHR = hex('#303234'), SHRD = hex('#141617'), SHRL = hex('#5a5d60'), SCREW = hex('#6b6e70');
  const RAIL = hex('#36383a'), RAILD = hex('#111213'), RAILL = hex('#595c5f');
  const CORD = hex('#1b1c1d'), PAD = hex('#2a2b2c'), PADD = hex('#18191a');
  const WEB = hex('#2d3131'), WEBL = hex('#414646'), BUCKLE = hex('#1e2020');

  // ---- shell: the MTEK high cut shared with the STRIKE
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -5.5], [1, -5.5], [2.5, -2.5], [3.5, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  // patch test: conds [[axis, lo, hi]], axis x / y / z, ax = |x|, dy = height above the rim. Returns the distance to
  // the nearest in-plane patch edge, or -1 outside (corner texels dropped so the panels look rounded)
  const PLANE = { top: [0, 2], bottom: [0, 2], front: [0, 1], back: [0, 1], left: [1, 2], right: [1, 2] };
  const AX = { x: 0, ax: 0, y: 1, dy: 1, z: 2 };
  const patch = (c, dy, conds) => {
    let d1 = 9, d2 = 9;
    const pl = PLANE[c.face];
    for (const [a, lo, hi] of conds) {
      const v = a === 'ax' ? Math.abs(c.p[0]) : a === 'dy' ? dy : c.p[AX[a]];
      if (v < lo || v > hi) return -1;
      const d = a === 'ax' && lo === 0 ? hi - v : Math.min(v - lo, hi - v);  // |x| from 0: no edge on the centre line
      if (AX[a] === pl[0]) d1 = Math.min(d1, d); else if (AX[a] === pl[1]) d2 = Math.min(d2, d);
    }
    if (d1 < 0.5 && d2 < 0.5) return -1;
    return Math.min(d1, d2);
  };
  const PATCHES = [
    [['y', -11, -9.25], ['ax', 0, 9], ['z', -4.5, -2]],                     // band over the crown front
    [['y', -11, -9.25], ['ax', 0, 9], ['z', 1.5, 4.5]],                     // crown rear
    [['ax', 4.75, 9], ['y', -9.25, -7.5], ['z', -1, 3]],                    // upper sides, above and behind the rails
    [['dy', 1, 2.5], ['ax', 3.25, 9], ['z', -9, -3]],                       // front brim strips, shroud to rail
    [['z', 4.75, 9], ['ax', 0, 3], ['y', -8, -5]],                          // back
  ];
  const velcro = (c, d) => {
    if (d < 0.5) return mul(VELE, K.G(c, 0.06));                              // recessed edge
    const t = tex(c, 5);
    return mul(t < 0.35 ? VELD : t > 0.75 ? VELL : VEL, K.G(c, 0.04));       // fuzzy loop speckle
  };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(LIP, 0.8 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(LIP, K.G(c, 0.05));                             // rolled lip
    if (dy < 1) return z < -3 && Math.abs(x) > 2.75 ? mul(CORD, K.G(c, 0.08)) // bungee cord, shroud to rail
      : mul(GROOVE, K.G(c, 0.05));
    for (const P of PATCHES) { const d = patch(c, dy, P); if (d >= 0) return velcro(c, d); }
    const n = fbm(x * 0.35 + 7, y * 0.35 + 3, z * 0.35 + 11, 2);
    let col = mix(SHD, SHL, clamp01(0.4 + (n - 0.5) * 0.9 + 0.25 * smoothstep(-5, -9.5, y)));
    if (tex(c, 3) > 0.975) col = mix(col, SCUFF, 0.5);                       // light wear marks
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- black NVG shroud: raised centre plate with a U-shaped window, wings with screws, top cap
  const edgeLit = (c, col) => (c.ev < 0.5 ? mul(SHRL, K.G(c, 0.05)) : mul(col, K.G(c, 0.06)));
  const plate = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.2 : 0.85);
    const i = Math.abs(Math.round(c.p[0] * 2)), y = c.p[1];
    if (i <= 1 && y > -7.5 && y < -6 && !(i === 0 && y < -7)) return mul(SHRD, K.G(c, 0.05));   // U window
    return edgeLit(c, SHR);
  };
  B.push(box('head', [-1.25, -8, -6.5], [1.25, -5, -5.5], plate, { tag: 'NVG shroud' }));
  for (const s of [-1, 1]) {
    const wing = c => {
      if (c.face === 'back') return null;
      if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.2 : 0.85);
      const ax = Math.abs(c.p[0]), y = c.p[1];
      if (y < -6) { if (ax > 1.75 && ax < 2.25) return SCREW; if (ax < 1.75) return mul(SHRD, K.G(c, 0.05)); }
      return edgeLit(c, SHR);
    };
    const [xa, xb] = s < 0 ? [-2.75, -1.25] : [1.25, 2.75];
    B.push(box('head', [xa, -6.5, -6], [xb, -5.5, -5.5], wing, { tag: 'NVG shroud' }));
  }
  const cap = c => (c.face === 'back' ? null : c.face === 'front' && Math.abs(c.p[0]) < 0.25 ? SCREW : mul(SHR, c.face === 'top' ? 1.2 : c.face === 'front' ? K.G(c, 0.05) : 0.85));
  B.push(box('head', [-0.75, -8.5, -6], [0.75, -8, -5.5], cap, { tag: 'NVG shroud' }));

  // ---- accessory rails above the cut: top lip, slot row, two windows with a slotted screw in front, slot row
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const outer = s < 0 ? 'right' : 'left';
    const rail = c => {
      if (c.face === (s < 0 ? 'left' : 'right')) return null;                 // lies on the shell
      const [, y, z] = c.p, col = Math.round((z + 2.75) * 2);                  // column 0 at the front end
      if (c.face === 'top') return col % 2 === 1 && col < 8 ? mul(RAILD, 1) : mul(RAILL, 1.05);
      if (c.face !== outer) return mul(RAIL, 0.85);
      const r = Math.round((y + 7.25) * 2);                                     // row 0 = top lip
      if (r === 0) return mul(RAILL, K.G(c, 0.05));
      if (r === 2 && col === 0) return SCREW;
      const slot = (r === 1 || r === 3) ? col % 2 === 1 && col < 8 : r === 2 ? (col >= 2 && col <= 3) || (col >= 5 && col <= 7) : false;
      return slot ? mul(RAILD, K.G(c, 0.05)) : mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -7.5, -3], [xb, -5.5, 1.5], rail, { tag: 'rail' }));
    // angled front end: a lower, thinner nose in front of the rail
    const nose = c => (c.face === (s < 0 ? 'left' : 'right') ? null : mul(c.face === 'top' ? RAILL : RAIL, c.face === outer ? K.G(c, 0.05) : 0.85));
    B.push(box('head', s < 0 ? [-6, -6.5, -3.5] : [5.5, -6.5, -3.5], s < 0 ? [-5.5, -5.5, -3] : [6, -5.5, -3], nose, { tag: 'rail' }));
  }

  // ---- mesh pads showing under the cut (set 0.5 px in from the shell surface)
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5 : 4.75;
    const pad = c => {
      if (c.face === 'top' || c.face === (s < 0 ? 'left' : 'right')) return null;
      const m = (Math.floor(c.p[1] * 2 + 100) + Math.floor(c.p[2] * 2 + 100)) & 1;
      return mul(m ? PADD : PAD, K.G(c, 0.06));
    };
    B.push(box('head', [x0, -5.5, -3], [x0 + 0.25, -4.5, 1], pad, { tag: 'pad' }));
  }

  // ---- harness: front strap from the front of the cut, rear strap from behind the ear, buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return c.ex < 0.25 ? mul(WEBL, 0.95) : mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.4 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-6, -3], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-3, 2.75], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
