// ================= Atomic Defense CQCM ballistic mask (Black) — EFT reference (tarkov.dev 657089638db3adca1009f4ca) =================
// One smooth matte black oval shell over the whole face, no helmet: a rounded forehead, a gentle nose, a narrowing
// chin, two oval eye slots. Six black plastic strap clips on the rim (two at the upper corners, two at the sides,
// two at the lower corners). Black webbing: the upper straps run round the back of the head, the side straps run
// back and up into them, the lower straps run round the nape.
ARMORS.cqcm_ballistic_mask = function () {
  const K = kit('M'), B = [];

  // ---- palette (albedo). Reference render: forehead ~#515153 (lit), cheeks ~#2b3439, lower face ~#152426 (shadow),
  //      clips ~#4d5355, straps ~#141617..#21282a
  const M = hex('#303437'), ML = hex('#464a4d'), MD = hex('#1e2224'), CLIP = hex('#383c3e'), CLIPL = hex('#5b6062');
  const STRAP = hex('#1b1d1f'), STRAPL = hex('#2c3033');

  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);

  // ---- shell: three layers stepping forward (z -5.25..-4.75 base, -5.5..-5.25 bulge, -5.75..-5.5 nose) so the
  //      oval reads round from the side; eye slots exactly over the skin eyes (x 1..3, y -4..-2) through all layers
  const shellM = c => {
    const [x, y] = c.p, ax = Math.abs(x);
    if (c.face !== 'front' && c.face !== 'back' && ax >= 1 - 1e-3 && ax <= 3 + 1e-3 && y >= -4 - 1e-3 && y <= -2 + 1e-3) return MD;   // slot walls
    if (c.face === 'back') return MD;
    if (c.face === 'bottom') return mul(M, 0.75);
    const lit = clamp01(0.3 + 0.45 * smoothstep(0, -6, y) + (fbm(x * 0.45 + 2, y * 0.45, 5, 2) - 0.5) * 0.5);
    let col = mix(M, ML, lit);
    if (c.face === 'top') col = mix(col, ML, 0.5);
    else if (c.face !== 'front') col = mul(col, 0.9);
    if (tex(c, 3) > 0.965) col = mul(col, 1.12);                               // scuffs
    return mul(col, K.G(c, 0.04));
  };
  const rows = (z0, z1, list, tag) => {
    for (const [y0, y1, hw] of list) B.push(box('head', [-hw, y0, z0], [hw, y1, z1], shellM, { tag }));
  };
  const eyeRow = (z0, z1, hw, tag) => {
    B.push(box('head', [-1, -4, z0], [1, -2, z1], shellM, { tag }));
    B.push(box('head', [-hw, -4, z0], [-3, -2, z1], shellM, { tag }));
    B.push(box('head', [3, -4, z0], [hw, -2, z1], shellM, { tag }));
  };
  rows(-5.25, -4.75, [[-7.5, -7, 1.75], [-7, -6.5, 2.75], [-6.5, -6, 3.5], [-6, -5.5, 4.25], [-5.5, -4, 4.75], [-2, -1, 4.75], [-1, -0.25, 4.25], [-0.25, 0.5, 3.5], [0.5, 1, 2.75], [1, 1.5, 1.75]], 'mask');
  eyeRow(-5.25, -4.75, 4.75, 'mask');
  rows(-5.5, -5.25, [[-7, -6.5, 1.75], [-6.5, -6, 2.75], [-6, -5.5, 3.5], [-5.5, -4, 3.75], [-2, -1, 3.75], [-1, -0.25, 3.25], [-0.25, 0.5, 2.75], [0.5, 1, 2], [1, 1.25, 1]], 'mask bulge');
  eyeRow(-5.5, -5.25, 3.75, 'mask bulge');
  B.push(box('head', [-0.75, -2.25, -5.75], [0.75, -0.75, -5.5], shellM, { tag: 'nose' }));

  // ---- the shell wraps back round the cheeks
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5.25 : 4.75, inner = s < 0 ? 'left' : 'right';
    B.push(box('head', [x0, -5.5, -4.75], [x0 + 0.5, -1, -3], c => (c.face === inner ? MD : shellM(c)), { tag: 'cheek wrap' }));
  }

  // ---- strap clips and webbing
  const clip = c => {
    if (c.face === 'top') return mul(CLIPL, 0.95);
    if (c.ex < K.px) return mul(CLIPL, 0.85);
    return mul(CLIP, K.G(c, 0.05));
  };
  const web = c => {
    const flat = c.face === 'left' || c.face === 'right' || c.face === 'back' || c.face === 'top';
    if (!flat) return mul(STRAP, 0.85);
    if (c.ex < K.px) return mul(STRAPL, 0.95);
    return mul(STRAP, K.G(c, 0.06));
  };
  for (const s of [-1, 1]) {
    const X = (a, b) => (s < 0 ? [-b, -a] : [a, b]);
    const xs = s < 0 ? -5 : 4.75;
    // upper clip at the forehead corner, strap round the back of the head
    let [x0, x1] = X(4.25, 5.25);
    B.push(box('head', [x0, -6.25, -5], [x1, -5.5, -4.5], clip, { tag: 'upper clip' }));
    B.push(box('head', [xs, -6.25, -4.5], [xs + 0.25, -5.5, 4.75], web, { tag: 'upper strap' }));
    // side clip on the cheek wrap, strap back and up into the upper strap
    [x0, x1] = X(5.25, 5.5);
    B.push(box('head', [x0, -3.5, -4.25], [x1, -2.5, -3.25], clip, { tag: 'side clip' }));
    sideStrap(B, xs, 0.25, [-3, -3.5], [-5.9, 2.5], 0.75, web, 'side strap');
    // lower clip at the jaw corner, strap round the nape
    [x0, x1] = X(3.75, 4.75);
    B.push(box('head', [x0, -1, -5.5], [x1, -0.25, -5.25], clip, { tag: 'lower clip' }));
    B.push(box('head', [xs, -1, -5.5], [xs + 0.25, -0.25, 4.75], web, { tag: 'lower strap' }));
  }
  B.push(box('head', [-5, -6.25, 4.75], [5, -5.5, 5], web, { tag: 'upper strap' }));
  B.push(box('head', [-5, -1, 4.75], [5, -0.25, 5], web, { tag: 'lower strap' }));

  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
