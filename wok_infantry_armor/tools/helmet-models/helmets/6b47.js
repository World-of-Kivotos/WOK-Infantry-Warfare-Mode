// ================= 6B47 Ratnik-BSh (Olive Drab) — EFT reference (tarkov.dev 5a7c4850e899ef00150be885) =================
// Smooth painted aramid shell in a greyish olive drab, a tall rounded crown, mid cut: the front edge sits on the brow,
// the sides come down over the temples and the back covers the nape. A thin dark rubber trim wraps the whole edge.
// Front: a dark olive NVG bracket (a raised frame with an inverted-U slot and a small screw plate under it).
// One flat screw head on each side, a little ahead of the middle. Dark grey webbing harness: a front and a rear
// strap on each side meet at a buckle by the jaw (the cup under the chin is left out: it would cut into the body
// when the head tilts).
ARMORS['6b47'] = function (mode) {
  const K = kit(mode), { hd, mid } = K, fine = hd && !mid;
  const g = mode === 'A' ? 1 : fine ? 0.25 : 0.5;
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#68704b,
  //      shaded sides ~#444831, edge trim ~#1b261f, bracket ~#45492e, harness ~#232223
  const OD = hex('#5f6842'), ODL = hex('#78805a'), ODD = hex('#4b5334');
  const TRIM = hex('#26302a'), BR = hex('#4a4f35'), BRD = hex('#20261d'), BRL = hex('#6f7453');
  const SCREW = hex('#8b8f84'), WEB = hex('#2e3132'), WEBL = hex('#45494b'), BUCKLE = hex('#7b7f7c');

  // ---- shell: 11 px wide with rounded corners, crown 2 px above the head, rim y along z (front -> back).
  //      The MC head is a big cube with a tall forehead, so the rim sits lower than on the real helmet: the front
  //      edge half a pixel above the eyes, the sides down to eye level, the back down to the nape.
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3], [-2, -2.5], [3, -2.5], [5.5, -1]]);
  const dome = mode === 'A' ? [[-10, 2, 1], [-9, 0, 2]]
    : fine ? [[-10, 2.75, 1.5], [-9.75, 1.75, 1.75], [-9.5, 1.25, 2], [-9.25, 0.75, 2], [-9, 0.5, 2], [-8.75, 0.25, 2]]
    : [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);
  const trimW = mode === 'A' ? 1 : fine ? 0.25 : 0.5;
  const paint = c => {
    const y = c.p[1];
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;                                          // height above the lower edge
    if (dy < trimW) return mul(TRIM, (fine && dy < 0.125 ? 0.85 : 1) * K.G(c, 0.05));
    // painted aramid: soft mottling, lighter towards the crown, a darker band just above the trim
    const n = fbm(c.p[0] * 0.35 + 7, c.p[1] * 0.35 + 3, c.p[2] * 0.35 + 11, 2);
    let col = mix(ODD, ODL, clamp01(0.35 + (n - 0.5) * 0.9 + 0.25 * smoothstep(-5, -9.5, y)));
    if (dy < trimW + (mode === 'A' ? 1 : 0.5)) col = mul(col, 0.9);
    if (hd && tex(c, 3) > (mid ? 0.985 : 0.993)) col = mix(col, hex('#9a9d80'), 0.35);   // scuffs
    return mul(col, K.G(c, mid ? 0.05 : 0.04));
  };
  B.push(...shell(S, paint));

  // ---- NVG bracket: raised frame on the upper front, inverted-U slot, screw plate below
  const bw = mode === 'A' ? 1.5 : 1.25, top = -8, bot = -5.5;
  const bracket = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(BR, 0.8);
    const x = c.p[0], y = c.p[1];
    const slot = Math.abs(x) < (mode === 'A' ? 0.5 : 0.5) && y > top + (mode === 'A' ? 1 : 0.5) && y < bot - (mode === 'A' ? 1 : 0.75);
    if (slot) return mul(BRD, K.G(c, 0.05));
    if (y > bot - (mode === 'A' ? 1 : 0.75)) {                               // screw plate
      if (hd && Math.abs(x) < K.px && y > bot - 0.5 && y < bot - 0.25 + (mid ? 0.25 : 0)) return SCREW;
      return mul(BR, 0.92 * K.G(c, 0.05));
    }
    if (hd && (c.ex < K.px)) return mul(BRL, c.ev < K.px ? 1.1 : 0.9);       // light frame edge
    return mul(BR, K.G(c, 0.06));
  };
  B.push(box('head', [-bw, top, -6], [bw, bot, -5.5], bracket, { tag: 'NVG bracket' }));

  // ---- side screws (a little ahead of the middle)
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5.75 : 5.5, sz = mode === 'A' ? 1 : 0.5;
    const screw = c => (c.face === (s < 0 ? 'left' : 'right') ? null : mul(SCREW, c.face === 'top' ? 1.1 : 0.85));
    B.push(box('head', [x0, -6.5, -0.75], [x0 + 0.25, -6.5 + sz, -0.75 + sz], screw, { tag: 'side screw' }));
  }

  // ---- harness (M / B): front and rear strap meet at a buckle by the jaw
  if (mode !== 'A') {
    const web = c => {
      if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
      const along = c.p[1];
      let col = mul(WEB, K.G(c, 0.06));
      if (fine && (c.ex < 0.125)) col = mul(WEBL, 0.9);                       // stitched edge
      return (Math.floor(along * f) & 1) && fine ? mul(col, 1.06) : col;       // webbing weave
    };
    const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.15 : (c.face === 'left' || c.face === 'right') && c.ex < K.px ? 0.8 : 1);
    for (const s of [-1, 1]) {
      const xs = s < 0 ? -4.75 : 4.5;
      sideStrap(B, xs, 0.25, [-3.25, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
      sideStrap(B, xs, 0.25, [-2.75, 2], [-0.75, -0.5], 0.5, web, 'rear strap');
      const bx = s < 0 ? -5 : 4.5;
      B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
    }
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
