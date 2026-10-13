// ================= Death Shadow lightweight armored mask — EFT reference (tarkov.dev 6570aead4d84f81fd002a033) =================
// A matte black moulded skull face, no helmet shell. From the top: a forehead plate with angular panel lines, brow
// ridges over two big oval eye sockets (smoky lenses in the original), a nose cavity, cheekbones, a row of upper and
// lower teeth and hex-mesh vents on the lower cheeks. The mask wraps a little round the sides. Black webbing holds it:
// a strap from each temple runs up and over the crown, a second strap runs from the jaw round the back of the neck,
// with plastic buckles on the sides.
ARMORS.lightweight_ballistic_mask = function () {
  const K = kit('M'), B = [];

  // ---- palette (albedo). Reference render: brow ~#3d3b3d, cheeks ~#2d2c31, lenses ~#353a3c, sides ~#1e1e1e,
  //      teeth ~#1f2528, vents ~#10181b, straps ~#0d171a
  const M = hex('#313336'), ML = hex('#46484c'), MD = hex('#1d1e21'), SOCK = hex('#141517'), RIM = hex('#4b5256');
  const VENT = hex('#0f1113'), VENTL = hex('#2c3033'), TOOTH = hex('#3a3c40');
  const STRAP = hex('#1a1c1e'), STRAPL = hex('#2b2e31'), BUCK = hex('#2f3134'), LENS = hex('#2b3236');

  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);
  const inRect = (x, y, x0, x1, y0, y1) => x > x0 && x < x1 && y > y0 && y < y1;

  // ---- the moulded face plate (z -5.25..-4.75); eye holes exactly over the skin eyes (x 1..3, y -4..-2)
  const face = c => {
    const [x, y] = c.p, ax = Math.abs(x);
    // walls of the eye holes: dark socket
    if (c.face !== 'front' && c.face !== 'back' && ax >= 1 - 1e-3 && ax <= 3 + 1e-3 && y >= -4 - 1e-3 && y <= -2 + 1e-3) return SOCK;
    if (c.face === 'back') return MD;
    if (c.face === 'bottom') return mul(MD, 1.1);
    if (c.face === 'top') return mul(ML, K.G(c, 0.05));
    if (c.face !== 'front') return mul(M, 0.9 * K.G(c, 0.05));
    // socket rim round the hole: the lens frame
    if (inRect(ax, y, 0.5, 3.5, -4.5, -1.5)) return mul(inRect(ax, y, 0.5, 3.5, -4.5, -4) ? RIM : SOCK, K.G(c, 0.05));
    // nose cavity: narrow at the top, wide at the bottom, two nostrils
    if ((ax < 0.5 && y > -2 && y < -1.5) || (ax < 1 && y > -1.5 && y < -1)) return mul(SOCK, 0.9);
    // mouth recess behind the teeth
    if (ax < 2 && y > -1 && y < 0.5) return mul(VENT, 1.1);
    // hex-mesh vents on the lower cheeks
    if (inRect(ax, y, 2.5, 4.5, -1, 0.5)) return ((Math.floor(x * 2) + Math.floor(y * 2)) & 1) ? VENTL : VENT;
    // forehead panel lines
    if ((y > -5.5 && y < -5 && ax > 1.5 && ax < 4.5) || (y > -6 && y < -5 && ax > 1 && ax < 1.5)) return mul(MD, 0.9);
    let col = mix(M, ML, clamp01(0.25 + 0.4 * smoothstep(-2, -6, y) + (fbm(x * 0.5 + 3, y * 0.5, 7, 2) - 0.5) * 0.5));
    if (tex(c, 3) > 0.96) col = mul(col, 1.15);
    return mul(col, K.G(c, 0.05));
  };
  const Z = [-5.25, -4.75];
  const plate = (x0, y0, x1, y1, tag) => B.push(box('head', [x0, y0, Z[0]], [x1, y1, Z[1]], face, { tag }));
  plate(-3.75, -6.25, 3.75, -5.75, 'forehead');
  plate(-4.75, -5.75, 4.75, -4, 'forehead');
  plate(-1, -4, 1, -2, 'nose bridge');
  plate(-4.75, -4, -3, -2, 'temple');
  plate(3, -4, 4.75, -2, 'temple');
  plate(-4.75, -2, 4.75, -0.5, 'mid face');
  plate(-4.75, -0.5, 4.75, 0.25, 'jaw');
  plate(-3.75, 0.25, 3.75, 0.75, 'chin');

  // ---- smoky lenses set a quarter pixel into the sockets; tagged 'visor glass' so they are drawn see-through
  const lens = c => (c.face === 'front' ? mul(LENS, c.ex < K.px && c.ev < K.px ? 1.35 : 1) : null);
  for (const x0 of [-3, 1]) B.push(box('head', [x0, -4, -5], [x0 + 2, -2, -4.75], lens, { tag: 'visor glass' }));

  // ---- sides: the mask wraps back a little (behind the plate edge, so the corner steps round)
  const sideM = inner => c => {
    if (c.face === inner) return MD;
    if (c.face === 'top') return mul(ML, K.G(c, 0.05));
    if (c.face === 'bottom' || c.face === 'back') return MD;
    const [, y, z] = c.p;
    if (c.face !== 'front' && y > -1 && y < 0 && z < -3.75) return ((Math.floor(z * 2) + Math.floor(y * 2)) & 1) ? VENTL : VENT;
    return mul(M, 0.95 * K.G(c, 0.05));
  };
  for (const s of [-1, 1]) {
    const X = (a, b) => (s < 0 ? [-b, -a] : [a, b]), inner = s < 0 ? 'left' : 'right';
    let [x0, x1] = X(4.75, 5.25);
    B.push(box('head', [x0, -5.75, -4.75], [x1, 0.25, -3.25], sideM(inner), { tag: 'side wrap' }));
  }

  // ---- raised features (z -5.5..-5.25): brow ridges, glabella, cheekbones, teeth
  const ridge = c => {
    if (c.face === 'back') return null;
    if (c.face === 'top') return mul(ML, 1.1);
    if (c.face === 'bottom') return MD;
    if (c.face !== 'front') return mul(M, 0.85);
    return mul(c.ev < K.px ? ML : M, K.G(c, 0.05));
  };
  const tooth = c => {
    if (c.face === 'back') return null;
    if (c.face === 'front') return mul(TOOTH, c.ev < K.px ? 1.15 : K.G(c, 0.06));
    return mul(TOOTH, c.face === 'top' ? 1.2 : 0.75);
  };
  const R = [-5.5, -5.25];
  for (const s of [-1, 1]) {
    const X = (a, b) => (s < 0 ? [-b, -a] : [a, b]);
    let [x0, x1] = X(0.75, 3.75);
    B.push(box('head', [x0, -4.75, R[0]], [x1, -4.25, R[1]], ridge, { tag: 'brow ridge' }));
    [x0, x1] = X(2.75, 4.5);
    B.push(box('head', [x0, -2, R[0]], [x1, -1.25, R[1]], ridge, { tag: 'cheekbone' }));
  }
  B.push(box('head', [-0.75, -5, R[0]], [0.75, -3.75, R[1]], ridge, { tag: 'glabella' }));
  B.push(box('head', [-2.5, -6.25, R[0]], [2.5, -5.75, R[1]], ridge, { tag: 'forehead plate' }));
  for (const x0 of [-1.75, -1, -0.25, 0.5, 1.25]) B.push(box('head', [x0, -1, R[0]], [x0 + 0.5, -0.25, R[1]], tooth, { tag: 'upper tooth' }));
  for (const x0 of [-1.5, -0.75, 0.25, 1]) B.push(box('head', [x0, 0, R[0]], [x0 + 0.5, 0.5, R[1]], tooth, { tag: 'lower tooth' }));

  // ---- webbing: temple strap over the crown, jaw strap round the back of the neck, buckles
  const web = c => {
    const side = c.face === 'left' || c.face === 'right', flat = side || c.face === 'top' || c.face === 'back';
    if (!flat) return mul(STRAP, 0.85);
    if (c.ex < K.px) return mul(STRAPL, 0.95);                                // stitched edge
    return mul(STRAP, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCK, c.face === 'top' ? 1.3 : c.ex < K.px ? 1.15 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -5 : 4.75;
    sideStrap(B, xs, 0.25, [-5.25, -3.75], [-8.4, 1], 1, web, 'crown strap');
    B.push(box('head', [xs, -1.25, -3.5], [xs + 0.25, -0.25, 4.75], web, { tag: 'neck strap' }));
    const bx = s < 0 ? -5.25 : 5;
    B.push(box('head', [bx, -1.5, -1.75], [bx + 0.25, 0, -0.75], buckle, { tag: 'buckle' }));
    const ax0 = s < 0 ? -5.5 : 5.25;
    B.push(box('head', [ax0, -5.5, -4.25], [ax0 + 0.25, -4.5, -3.5], buckle, { tag: 'strap anchor' }));
  }
  B.push(box('head', [-5, -9, 0.5], [5, -8.75, 1.5], web, { tag: 'crown strap' }));
  B.push(box('head', [-5, -1.25, 4.75], [5, -0.25, 5], web, { tag: 'neck strap' }));

  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
