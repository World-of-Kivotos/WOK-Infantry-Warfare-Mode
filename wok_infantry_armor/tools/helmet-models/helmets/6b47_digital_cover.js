// ================= 6B47 Ratnik-BSh with the digital flora (EMR) cover — EFT reference (tarkov.dev 5aa7cfc0e5b5b00015693143) =================
// Same shell as the olive 6B47 (tall rounded crown, mid cut, back down to the nape), wrapped in a Russian digital flora
// fabric cover: small square blocks of light khaki green, mid green, dark green, red-brown and near-black. The cover
// is folded over the rim into a rolled edge with a dark teal elastic hem (no rubber trim). Front: a padded flap across
// the top, and under it a cut-out window where the NVG bracket shows, framed by two fabric panels. Light piping seams
// run over the crown from front to back; a horizontal loop-velcro strip sits on each side; a teal-grey bungee loop
// on the back. Dark grey webbing harness as on the 6B47 (the chin cup is left out: it would cut into the body when
// the head tilts).
ARMORS['6b47_digital_cover'] = function (mode) {
  const K = kit('M'), g = 0.5;
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#747a64,
  //      shaded sides ~#56594b, fold hem ~#23322c, bracket ~#50544d, bungee loop ~#38443e, harness ~#303431
  const LIGHT = hex('#8c9270'), MID = hex('#6d7855'), DARK = hex('#525e43'), BROWN = hex('#6f5e4a'), BLACK = hex('#3b4136');
  const PIPE = hex('#b3b492'), HEM = hex('#1f312b'), STITCH = hex('#3c4233');
  const BR = hex('#565b4b'), BRD = hex('#202425'), BRL = hex('#858a78'), SCREW = hex('#9a9e93');
  const CORD = hex('#465a54'), CORDD = hex('#2b3834');
  const WEB = hex('#2e3132'), WEBL = hex('#45494b'), BUCKLE = hex('#7b7f7c');

  // ---- EMR "Tsifra": blotches built from 0.5 px squares, a little wider than tall, ragged pixel edges
  const hsh = (q, s) => hash3(Math.floor(q[0] * 2 + 1000), Math.floor(q[1] * 2 + 1000), Math.floor(q[2] * 2 + 1000) + s * 131);
  const emr = p => {
    const q = snap(p, 0.5);
    const on = (fr, o, t, s) => fbm(q[0] * fr + o, q[1] * fr * 1.4 - o, q[2] * fr + o * 0.5, 2) + (hsh(q, s) - 0.5) * 0.18 > t;
    let col = LIGHT;
    if (on(0.55, 3, 0.48, 1)) col = MID;
    if (on(0.8, 17, 0.6, 2)) col = DARK;
    if (on(0.9, 41, 0.63, 3)) col = BROWN;
    if (on(1.3, 77, 0.69, 4)) col = BLACK;
    return col;
  };
  // fabric: soft wrinkles, a touch lighter towards the crown
  const cloth = (c, k = 1) => {
    const [x, y, z] = c.p;
    const w = 0.93 + 0.14 * fbm(x * 0.7 + 4, y * 0.9 + 2, z * 0.7 + 8, 2);
    return mul(emr(c.p), k * w * (0.96 + 0.08 * smoothstep(-5, -9.5, y)) * K.G(c, 0.04));
  };

  // ---- shell (6B47 geometry). Its bottom row is taken out and rebuilt as the rolled cover edge below.
  const rim0 = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3], [-2, -2.5], [3, -2.5], [5.5, -1]]);
  const dome = [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const colC = v => Math.max(-5.25, Math.min(5.25, Math.floor((v + 5.5) / g) * g - 5.5 + g / 2));
  const stepRim = (x, z) => -10 + g * Math.floor((rim0(colC(x), colC(z)) + 10) / g + 1e-6);   // grid-stepped lower edge
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim: (x, z) => stepRim(x, z) - 0.5 };

  const paint = c => {
    const [x, y, z] = c.p, ax = Math.abs(x);
    if (c.face === 'bottom') return mul(HEM, 0.8);                        // inside of the crown, seen from below
    // piping seams: crown panel edge, front (hidden under the flap) over the top and down the back
    const seamBand = ax >= 2 && ax < 2.5;
    if (seamBand && (c.face === 'top' || (c.face === 'front' && y < -7.5) || (c.face === 'back' && y < -4))) return mix(cloth(c), PIPE, 0.6);
    // stitching along the lower side, one texel above the fold
    const dy = colBottom(c, S) - y;
    if ((c.face === 'left' || c.face === 'right') && dy >= 0.5 && dy < 1 && z > -2 && z < 4.5 && (Math.floor(z * 2) & 1)) return mix(cloth(c), STITCH, 0.55);
    return cloth(c);
  };
  B.push(...shell(S, paint, 'cover shell'));

  // ---- rolled cover edge: 0.5 tall, a quarter pixel proud of the shell, dark teal elastic hem underneath
  const F = { g: 0.25, hx: 5.75, zf: 5.75, zb: 5.75, dome: [], corner: 2.25, rim: stepRim, cut: (x, y, z) => y - 0.125 < stepRim(x, z) - 0.5 - 1e-6 };
  const fold = c => {
    if (c.face === 'bottom') return mul(HEM, K.G(c, 0.06));
    if (c.face === 'top') return cloth(c, 1.08);                          // lit top of the roll
    return mix(cloth(c, 0.9), HEM, 0.18);                                  // shaded, slightly teal
  };
  B.push(...shell(F, fold, 'cover fold'));

  // ---- front: padded flap across the top, fabric panels either side of the bracket window, the bracket itself
  const flap = c => {
    if (c.face === 'back') return null;
    if (c.face === 'bottom') return mul(HEM, 0.9);
    const col = cloth(c, c.face === 'top' ? 1.05 : 1);
    if (c.face === 'front' && (c.ex < 0.5)) return mul(col, c.ev < 0.5 ? 1.08 : 0.8);   // puffy seamed edge
    return col;
  };
  B.push(box('head', [-2.5, -8.5, -6.25], [2.5, -7.5, -5.5], flap, { tag: 'cover flap' }));
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-2.25, -1.25] : [1.25, 2.25];
    const panel = c => {
      if (c.face === 'back') return null;
      const col = cloth(c, 0.97);
      if (c.face === 'front' && Math.abs(c.p[0]) > 1.75) return mix(col, PIPE, 0.35);   // seam on the outer edge
      return col;
    };
    B.push(box('head', [xa, -7.5, -5.75], [xb, -5.5, -5.5], panel, { tag: 'window panel' }));
  }
  const bracket = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(BR, c.face === 'top' ? 1.05 : 0.8);
    const [x, y] = c.p;
    if (Math.abs(x) < 0.5 && y > -7 && y < -6) return mul(BRD, K.G(c, 0.05));         // inverted-U slot
    if (y > -6) return Math.abs(x) < 0.5 ? SCREW : mul(BR, 0.9 * K.G(c, 0.05));       // screw plate
    if (c.ex < 0.5) return mul(BRL, c.ev < 0.5 ? 1.05 : 0.85);                          // worn frame edge
    return mul(BR, K.G(c, 0.06));
  };
  B.push(box('head', [-1.25, -7.5, -6], [1.25, -5.5, -5.5], bracket, { tag: 'NVG bracket' }));

  // ---- loop-velcro strip on each side (camo fabric, stitched into short loops)
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-5.75, -5.5] : [5.5, 5.75];
    const strip = c => {
      if (c.face === (s < 0 ? 'left' : 'right')) return null;             // lies on the shell
      const col = mix(cloth(c, 1.05), LIGHT, 0.4);                          // loop fabric: lighter, calmer camo
      if (c.face !== (s < 0 ? 'right' : 'left')) return mul(col, c.face === 'top' ? 1.2 : 0.6);
      const [, y, z] = c.p;
      if (z < -1.5 || z > 2 || y > -6.5) return mix(col, BLACK, 0.6);        // stitched ends, lower edge in shadow
      if (y < -7) return mix(col, PIPE, 0.5);                                // stitched top edge
      return (((z + 1.5) % 1) + 1) % 1 < 0.5 ? mix(col, BLACK, 0.45) : col;  // loop divisions
    };
    B.push(box('head', [xa, -7.5, -2], [xb, -6, 2.5], strip, { tag: 'velcro strip' }));
  }

  // ---- bungee loop on the back
  const cord = c => mul(c.face === 'back' || c.face === 'top' ? CORD : CORDD, K.G(c, 0.06));
  B.push(box('head', [-0.25, -7, 6], [0.25, -4.5, 6.25], cord, { tag: 'bungee loop' }));
  B.push(box('head', [-0.25, -7, 5.5], [0.25, -6.5, 6], cord, { tag: 'bungee loop' }));
  B.push(box('head', [-0.25, -5, 5.5], [0.25, -4.5, 6], cord, { tag: 'bungee loop' }));

  // ---- harness: front and rear strap meet at a buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.15 : (c.face === 'left' || c.face === 'right') && c.ex < K.px ? 0.8 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-3.25, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-2.75, 2], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
