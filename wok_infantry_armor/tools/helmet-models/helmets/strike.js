// ================= MTEK STRIKE Ballistic helmet (Coyote) — EFT reference (tarkov.dev 67597ceea35600b4c10cea86) =================
// Coyote-brown painted shell of the MTEK family (sibling of the FLUX), rounded crown, high cut: the brim runs level
// from the brow back over the ear, then drops behind the ear to cover the back of the head. A rolled lip runs round
// the whole edge. Front: a coyote skeletonised NVG shroud (diamond frame, H-shaped openings, three black screws).
// Loop Velcro patches in the shell colour but fuzzier (crown front and rear, a big patch on each upper side, strips
// along the front brim, one on the back). A black slotted bolt on each temple, no rails. Dark grey pads show under the
// cut. Coyote webbing harness: a front and a rear strap meet at a buckle by the jaw (no chin cup: it would cut into
// the body when the head tilts).
ARMORS.strike = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: lit crown ~#736043,
  //      shaded side ~#4e4434, lip ~#4e4a35, Velcro ~#5b503d..#927d59 (speckled), shroud openings ~#2d372c,
  //      pads ~#212525, webbing ~#3e3b2f
  const SHL = hex('#836f4c'), SHD = hex('#5d4c34'), SCUFF = hex('#a08f70');
  const LIP = hex('#685b40'), GROOVE = hex('#473e2c');
  const VEL = hex('#65573c'), VELD = hex('#54482f'), VELL = hex('#806f4f'), VELE = hex('#4a3f2c');
  const SHR = hex('#735f41'), SHRD = hex('#2e281e'), SHRL = hex('#907c59');
  const SCREW = hex('#242526'), SCREWL = hex('#4c4d4e');
  const PAD = hex('#2b2d2e'), PADD = hex('#1d1e1f');
  const WEB = hex('#5a523d'), WEBL = hex('#70674d'), BUCKLE = hex('#4b4433');

  // ---- shell: brim half a pixel above the eyes, level back to the temple, high cut over the ear (lower than the
  //      FAST super high cut), back down to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -5.5], [1, -5.5], [2.5, -2.5], [3.5, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  // patch test: conds [[axis, lo, hi]], axis x / y / z, ax = |x|, dy = height above the rim. Returns the distance to
  // the nearest in-plane patch edge, or -1 outside (corner texels dropped so the patches look rounded)
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
    [['y', -11, -9.25], ['ax', 0, 2.5], ['z', -4.5, -1]],                   // crown, front
    [['y', -11, -9.25], ['ax', 0, 3], ['z', 0, 4.5]],                       // crown, rear
    [['ax', 4.75, 9], ['y', -9, -6.5], ['z', -1.5, 1.5]],                   // upper sides
    [['dy', 1, 3], ['ax', 2.75, 9], ['z', -9, -2.5]],                       // front brim strips, round the corners
    [['z', 4.75, 9], ['ax', 0, 2.5], ['y', -7.5, -5]],                      // back
  ];
  // same coyote as the shell, just a little darker and fuzzier; the edge only a shade darker (the render reads calm)
  const velcro = (c, d) => {
    if (d < 0.5) return mul(mix(VELE, VEL, 0.45), K.G(c, 0.05));             // recessed edge
    const t = tex(c, 5);
    return mul(mix(VELD, VEL, 0.45 + 0.4 * t), K.G(c, 0.03));                 // fine loop fuzz
  };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(LIP, 0.8 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(LIP, K.G(c, 0.05));                             // rolled lip
    if (dy < 1) return mul(GROOVE, K.G(c, 0.05));                            // shadow line above the lip
    for (const P of PATCHES) { const d = patch(c, dy, P); if (d >= 0) return velcro(c, d); }
    const n = fbm(x * 0.35 + 7, y * 0.35 + 3, z * 0.35 + 11, 2);
    let col = mix(SHD, SHL, clamp01(0.5 + (n - 0.5) * 0.45 + 0.2 * smoothstep(-5, -9.5, y)));
    if (tex(c, 3) > 0.99) col = mix(col, SCUFF, 0.3);                        // paint scuffs
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- skeletonised NVG shroud (coyote): diamond frame, H-shaped centre openings, side windows, three screws
  const shroud = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.1 : 0.85);
    // texel column i (0 = centre) and row r (0 = top cap .. 5 = bottom bar)
    const i = Math.abs(Math.round(c.p[0] * 2)), r = Math.round((c.p[1] + 7.75) * 2);
    if ((r === 0 && i === 0) || (r === 3 && i === 4)) return SCREW;
    const open = (r === 2 || r === 3) ? i === 0 || i === 2 : r === 4 ? i === 0 || i === 2 || i === 3 : false;
    if (open) return mul(SHRD, K.G(c, 0.05));
    if (c.ev < 0.5) return mul(SHRL, K.G(c, 0.04));                          // upper edges catch the light
    return mul(SHR, K.G(c, 0.05));
  };
  B.push(box('head', [-1.75, -5.5, -6], [1.75, -5, -5.5], shroud, { tag: 'NVG shroud' }));
  B.push(box('head', [-2.25, -6.5, -6], [2.25, -5.5, -5.5], shroud, { tag: 'NVG shroud' }));
  B.push(box('head', [-1.75, -7.5, -6], [1.75, -6.5, -5.5], shroud, { tag: 'NVG shroud' }));
  B.push(box('head', [-0.75, -8, -6], [0.75, -7.5, -5.5], shroud, { tag: 'NVG shroud' }));

  // ---- slotted bolt on each temple, just in front of the cut
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5.75 : 5.5;
    const bolt = c => (c.face === (s < 0 ? 'left' : 'right') ? null : mul(c.face === 'top' ? SCREWL : SCREW, 1));
    B.push(box('head', [x0, -7, -2.5], [x0 + 0.25, -6.5, -2], bolt, { tag: 'side bolt' }));
  }

  // ---- inner pads showing under the cut (set 0.5 px in from the shell surface)
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5 : 4.75;
    const pad = c => {
      if (c.face === 'top' || c.face === (s < 0 ? 'left' : 'right')) return null;
      const seam = Math.floor(c.p[2] * 2 + 100) % 3 === 0;
      return mul(seam ? PADD : PAD, K.G(c, 0.06));
    };
    B.push(box('head', [x0, -5.5, -3], [x0 + 0.25, -4.5, 1], pad, { tag: 'pad' }));
  }

  // ---- harness: front strap from the front of the cut, rear strap from behind the ear, buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return c.ex < 0.25 ? mul(WEBL, 0.95) : mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.3 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-6, -3], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-3, 2.75], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
