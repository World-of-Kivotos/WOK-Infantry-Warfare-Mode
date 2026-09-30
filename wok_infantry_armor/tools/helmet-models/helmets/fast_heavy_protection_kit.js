// ================= FAST MT Heavy protection kit — EFT references: FAST MT Super High Cut Urban Tan (5ac8d6885acfc400180ae7b0),
// SLAAP armor helmet plate (5c0e66e2d174af02a96252f4), Gunsight Mandible (5a16ba61fcdbcb098008728a) =================
// Same helmet as the black FAST MT (super high cut, ARC rails, skeletonised NVG shroud, shock cord, loop Velcro) in
// Urban Tan. On top: the SLAAP plate, a thick tan-brown armour plate over the front half of the crown; at the front a
// big arched cut-out leaves the NVG shroud free, the two legs beside it run down to the brim, the sides stop on the
// rails with a small step in front of them, and the rear edge curves over the crown. The Gunsight Mandible (MultiCam
// fabric armour with a black moulded front plate and a dark liner) hangs from the ARC rails and wraps round the jaw
// in front of the mouth. No FAST side armour: the shipped kit model has none either.
ARMORS.fast_heavy_protection_kit = function () {
  const K = kit('M'), g = 0.5, B = [];

  // ---- palette (albedo). Reference renders: tan crown ~#ccbd98 (lit), sides ~#78715c, rails ~#7c6e59, shock cord
  //      ~#7e7763, harness ~#4b4e39; SLAAP top ~#7d715b / edges ~#5d5340 (darker, browner than the shell);
  //      mandible MultiCam ~#4b5043..#628772, front plate ~#172526, liner ~#2c2728
  const SH = hex('#a0977e'), SHL = hex('#b8ae90'), SPECK = hex('#cfc6a8');
  const TRIM = hex('#5f5846'), LOOP = hex('#aca386'), LOOPD = hex('#8d846b');
  const RAIL = hex('#8c7f66'), RAILD = hex('#3a3429'), RAILL = hex('#b3a78a');
  const SHR = hex('#9b9075'), SHRD = hex('#2d2a24'), SHRL = hex('#c7bb9c');
  const CORD = hex('#6e6e58'), WEB = hex('#50543f'), WEBL = hex('#686c53'), BUCKLE = hex('#3d3f33');
  const PL = hex('#86795b'), PLL = hex('#9b8e6d'), PLD = hex('#574e3c');
  const BLK = hex('#1e2325'), BLKL = hex('#394044'), LINER = hex('#2c2728'), BIND = hex('#3a3a30');

  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);

  // ---- helmet shell: the FAST MT super high cut geometry (brim above the eyes, high over the ears, down to the nape)
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -6.5], [1, -6.5], [2.5, -3], [3.5, -1.5], [5.5, -1.5]]);
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome: [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]], corner: 2, rim };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.05));
    // shock cord along the brim, round the front corners to the rail fronts
    if (z < -3.25 && colBottom(c, S) > -6 && dy < 1 && (c.face !== 'front' || Math.abs(x) > 1.5)) return mul(CORD, K.G(c, 0.05));
    // loop Velcro on the rear crown (the front half is under the SLAAP plate)
    if (y < -9.25 && Math.abs(x) < 3.5 && z > -2.5 && z < 3.5) return mul(LOOP, 0.95 + 0.1 * tex(c, 5));
    let col = mix(SH, SHL, clamp01(0.3 + 0.5 * smoothstep(-6, -9.5, y) + (fbm(x * 0.4 + 5, y * 0.4, z * 0.4 + 9, 2) - 0.5) * 0.6));
    if (tex(c, 3) > 0.95) col = mix(col, SPECK, 0.25);
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- SLAAP plate: the shell envelope grown by half a pixel, minus the helmet envelope, front half only
  const inEnv = (o, x, yTop, z) => {
    if (yTop < o.dome[0][0]) return false;
    let inset = 0, r = o.corner;
    if (yTop < -8.5) { let L = o.dome[0]; for (const d of o.dome) if (yTop >= d[0]) L = d; inset = L[1]; r = L[2]; }
    const hx = o.hx - inset, zf = o.zf - inset, zb = o.zb - inset;
    if (Math.abs(x) > hx || z < -zf || z > zb) return false;
    const dx = Math.abs(x) - (hx - r), dz = z < 0 ? -z - (zf - r) : z - (zb - r);
    return !(dx > 0 && dz > 0 && dx * dx + dz * dz > r * r + 1e-6);
  };
  const zRear = (x, y) => 1.25 - 0.08 * x * x - 0.75 * clamp01((y + 9.75) / 1.5);
  const arch = (x, y, z) => z < -4 && Math.abs(x) < 2.5 && y > (Math.abs(x) > 2 ? -8 : -8.5);
  const P = {
    g, hx: 6, zf: 6, zb: 6, dome: [[-10.5, 2.5, 2], [-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]], corner: 2,
    rim: (x, z) => (z < -3.5 ? -5.5 : -8),                                   // legs down to the brim, sides stop on the rails
    cut: (x, y, z) => inEnv(S, x, y - g / 2 + 1e-6, z) || z > zRear(x, y) || arch(x, y, z),
  };
  const plate = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom' && Math.abs(x) > 5.5 && z > -3) return null;    // rests on the ARC rail
    const cutFace = c.face === 'bottom' || c.face === 'back' || ((c.face === 'left' || c.face === 'right') && Math.abs(x) < 3 && z < -5);
    if (cutFace) return mul(PLD, K.G(c, 0.05));
    let col = mix(PL, PLL, clamp01(0.35 + 0.35 * smoothstep(-8, -10.5, y) + (fbm(x * 0.5 + 21, y * 0.5, z * 0.5 + 4, 2) - 0.5) * 0.7));
    if (tex(c, 11) > 0.94) col = mix(col, PLD, 0.35);                        // pitted texture
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(P, plate, 'SLAAP plate'));

  // ---- ARC rails on the sides, along the cut above the ear
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const outer = s < 0 ? 'right' : 'left';
    const rail = c => {
      if (c.face === (s < 0 ? 'left' : 'right')) return null;
      if (c.face !== outer) return mul(RAIL, c.face === 'top' ? 1.15 : 0.85);
      const [, y, z] = c.p, t = ((z + 3) % 1 + 1) % 1;
      if (y > -7.5 && y < -7 && z > -2.5 && z < 1 && t > 0.5) return mul(RAILD, K.G(c, 0.05));
      if (c.ev < K.px) return mul(RAILL, 0.9);
      return mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -8, -3], [xb, -6.5, 1.5], rail, { tag: 'ARC rail' }));
  }

  // ---- skeletonised NVG shroud inside the SLAAP arch
  const shroud = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.2 : 0.8);
    const [x, y] = c.p, ax = Math.abs(x);
    if ((ax < 0.5 && y > -7.5 && y < -6) || (ax > 1 && ax < 1.5 && y > -7.5 && y < -6.25)) return mul(SHRD, K.G(c, 0.05));
    if (c.ex < K.px) return mul(SHRL, c.ev < K.px ? 1.05 : 0.9);
    return mul(SHR, K.G(c, 0.06));
  };
  B.push(box('head', [-2, -8, -6], [2, -5.5, -5.5], shroud, { tag: 'NVG shroud' }));

  // ---- harness: front strap from under the rail, rear strap from behind the ear, buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.3 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-6.5, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-3, 2.75], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }

  // ---- Gunsight Mandible: MultiCam jaw guard hung from the rails, black moulded plate in the middle
  const camo = c => mul(mix(multicam(c.p, 0.5), hex('#66785d'), 0.3), 0.92);
  const mand = (inner, tag) => c => {
    if (c.face === inner) return mul(LINER, K.G(c, 0.05));
    if (c.face === 'top') return mul(BIND, K.G(c, 0.05));                   // bound upper edge, liner behind it
    if (c.face === 'bottom') return mul(camo(c), 0.7);
    let col = camo(c);
    if (c.ev < K.px && c.face !== 'top') col = mix(col, BIND, 0.6);          // binding along the top edge
    return mul(col, K.G(c, 0.05));
  };
  B.push(box('head', [-3, -1.75, -5.25], [3, 0.75, -4.75], mand('back'), { tag: 'mandible front' }));
  for (const s of [-1, 1]) {
    const X = (a, b) => (s < 0 ? [-b, -a] : [a, b]);
    const inner = s < 0 ? 'left' : 'right';
    let [x0, x1] = X(3, 4.75);
    B.push(box('head', [x0, -2.75, -5.25], [x1, 0.5, -4.75], mand('back'), { tag: 'mandible cheek' }));
    [x0, x1] = X(4.75, 5.25);
    // side panel with a slanted front edge rising to the rail adapter (stepped)
    B.push(box('head', [x0, -3.25, -4.75], [x1, 0.5, -2.5], mand(inner), { tag: 'mandible side' }));
    B.push(box('head', [x0, -3.25, -2.5], [x1, 0, -1.25], mand(inner), { tag: 'mandible side' }));
    B.push(box('head', [x0, -4.5, -3.75], [x1, -3.25, -1.25], mand(inner), { tag: 'mandible wing' }));
    B.push(box('head', [x0, -6, -2.75], [x1, -4.5, -1.25], mand(inner), { tag: 'mandible wing' }));
    // rail adapter under the ARC rail
    [x0, x1] = X(4.75, 6.25);
    const clamp = c => (c.face === 'top' ? null : mul(BLK, c.face === (s < 0 ? 'right' : 'left') ? 1.1 : 0.9));
    B.push(box('head', [x0, -6.5, -2.75], [x1, -6, -1], clamp, { tag: 'mandible rail adapter' }));
  }
  const blk = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(BLK, c.face === 'top' ? 1.25 : 0.85);
    if (c.ex < K.px) return mul(BLKL, 0.85);                                  // moulded facet edges
    return mul(BLK, K.G(c, 0.06));
  };
  B.push(box('head', [-1, -2, -5.75], [1, 1, -5.25], blk, { tag: 'mandible front plate' }));
  B.push(box('head', [-0.5, -1.75, -6], [0.5, 0.75, -5.75], c => (c.face === 'back' ? null : mul(BLKL, c.face === 'front' ? 1 : 0.8)), { tag: 'front plate ridge' }));

  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
