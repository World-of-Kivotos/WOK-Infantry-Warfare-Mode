// ================= Maska-1SCh bulletproof helmet (Olive Drab) + Maska-1SCh face shield (Olive Drab) — EFT reference (tarkov.dev 5c091a4e0db834001d5addc8, shield 5c0919b50db834001b7ce3b9) =================
// Titanium helmet in a yellowish olive drab, a tall round dome with a visor lug (a small bracket with a knob) on the
// crown. Dark grey rubber trim round the edge and a blue-grey padded lining that shows under it. On each side a
// domed bolt towards the front and a hex bolt with a small latch further back, where the shield hangs. The mod item
// ships with the shield lowered: a curved olive steel plate from the brow to the chin with one narrow eye slit, cut
// through a raised reinforcing plate held by four bolts. Olive webbing chin straps to the jaw buckles.
ARMORS.maska_1sch_heavy = function (mode) {
  const K = kit('M');
  const g = 0.5, B = [];

  // ---- palette (albedo; entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#b9b77c (glossy),
  //      shaded sides ~#66654a, front ~#717052, trim ~#515550, lining ~#2e3454..#435771, shield plate ~#5b5f40,
  //      reinforcing plate ~#5f6549, eye slot ~#1e1d12, bolts ~#847e6c
  const ODL = hex('#9b9a62'), ODD = hex('#636440'), SCUFF = hex('#b8b89a');
  const TRIM = hex('#33383a'), TRIML = hex('#50554f'), LIN = hex('#34405c'), LINL = hex('#4a5a78');
  const VISL = hex('#7b8255'), VISD = hex('#555b3a'), PAN = hex('#6c7350'), PANL = hex('#848a61');
  const SLOT = hex('#1b1a12'), BOLT = hex('#8c8b7e'), BOLTD = hex('#4b4c44'), LUG = hex('#5b5b43'), LUGL = hex('#9a9884');
  const WEB = hex('#5c6a3a'), WEBL = hex('#6f7d48'), BUCKLE = hex('#3a3d38');

  // ---- helpers (plan rounded rectangle test, voxel fill + greedy merge on a grid)
  const inRR = (x, z, hx, zf, zb, r) => {
    if (Math.abs(x) > hx || z < -zf || z > zb) return false;
    const dx = Math.abs(x) - (hx - r), dz = z < 0 ? -z - (zf - r) : z - (zb - r);
    return !(dx > 0 && dz > 0 && dx * dx + dz * dz > r * r + 1e-6);
  };
  const vox = (a, b, gg, keep, mat, tag) => {
    const [x0, y0, z0] = a, NI = Math.round((b[0] - x0) / gg), NJ = Math.round((b[1] - y0) / gg), NK = Math.round((b[2] - z0) / gg);
    const key = (i, j, k) => i + ',' + j + ',' + k, cells = new Set();
    for (let j = 0; j < NJ; j++) for (let i = 0; i < NI; i++) for (let k = 0; k < NK; k++)
      if (keep(x0 + (i + 0.5) * gg, y0 + (j + 0.5) * gg, z0 + (k + 0.5) * gg)) cells.add(key(i, j, k));
    return mergeCells({ cells, g: gg, x0, y0, z0, NI, NJ, NK, key }, 'head', mat, tag);
  };
  const f = K.f;
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * f + 1000), Math.floor(c.p[1] * f + 1000), Math.floor(c.p[2] * f + 1000) + salt * 131);

  // ---- shield outline: plate from the brow (-6) to the chin (0), wings rising back to a mounting tab at z -1.5..-0.5
  const tab = z => z > -1.5;
  const ytop = z => (tab(z) ? -7 : -6);
  const ybot = z => (tab(z) ? -4.5 : z <= -4 ? 0 : -2 * (z + 4) / 2.5);
  const slot = (x, y) => Math.abs(x) < 3.5 && y > -3.5 && y < -3;                  // narrow slit, 0.5 px, across both eyes
  const cellZ = z => (Math.floor((z + 6) / g + 1e-6) + 0.5) * g - 6;
  const shieldTop = z => -7 + g * (Math.floor((ytop(cellZ(z)) + 6.75) / g) + 1);

  // ---- shell: brim half a pixel above the eyes, the sides down to eye level, the back down to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3], [-2, -2.5], [3, -2.5], [5.5, -1.5]]);
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome: [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]], corner: 2, rim };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return Math.max(Math.abs(x), Math.abs(z)) < 5 ? mul(LIN, K.G(c, 0.08)) : mul(TRIM, K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(tex(c, 2) > 0.8 ? TRIML : TRIM, K.G(c, 0.06));
    const n = fbm(x * 0.35 + 4, y * 0.35 + 9, z * 0.35 + 1, 2);
    let col = mix(ODD, ODL, clamp01(0.36 + (n - 0.5) * 0.9 + 0.3 * smoothstep(-5, -9.5, y)));
    if (dy < 1) col = mul(col, 0.9);
    const st = shieldTop(z);                                                     // shadow line over the shield edge
    if (c.face !== 'top' && z < -0.5 && y < st && y > st - 0.5) col = mul(col, 0.62);
    if (tex(c, 3) > 0.992) col = mix(col, SCUFF, 0.25);                         // scratches
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- blue-grey lining: a padded roll peeking out under the trim on the sides and the back (0.25 grid, the upper
  //      half hides inside the shell wall)
  const shellBottom = z => {
    const cz = (Math.floor((z + 5.5) / g) + 0.5) * g - 5.5;
    return -10 + g * Math.floor((rim(0, cz) + 10) / g + 1e-6);
  };
  const linKeep = (x, y, z) => z > -3 && inRR(x, z, 5.25, 5.25, 5.25, 1.75) && !inRR(x, z, 4.75, 4.75, 4.75, 1.25) && Math.abs(y - shellBottom(z)) < 0.5;
  const lining = c => (c.face === 'top' ? null : mul(mix(LIN, LINL, 0.25 * tex(c, 6)), K.G(c, 0.06)));
  B.push(...vox([-5.25, -5, -5.25], [5.25, 0.5, 5.25], 0.25, linKeep, lining, 'lining'));

  // ---- face shield, lowered: 0.5 px plate lying on the shell front and sides (1 px clear of the face)
  const shieldKeep = (x, y, z) => z < -0.5 && inRR(x, z, 6, 6, 6, 2.5) && !inRR(x, z, 5.5, 5.5, 5.5, 2) && y > ytop(z) && y < ybot(z) && !slot(x, y);
  const shieldPaint = c => {
    const [x, y, z] = c.p;
    if (c.face !== 'front' && c.face !== 'back' && Math.abs(x) <= 3.5 + 1e-6 && y >= -3.5 - 1e-6 && y <= -3 + 1e-6 && z < -5.4) return mul(SLOT, K.G(c, 0.05));
    const n = fbm(x * 0.35 + 13, y * 0.35 + 2, z * 0.35 + 8, 2);
    let col = mix(VISD, VISL, clamp01(0.45 + (n - 0.5) * 0.9 - 0.1 * smoothstep(-3, 0, y)));
    if (c.face === 'top' || (c.face !== 'bottom' && c.face !== 'back' && y < shieldTop(z) + 0.5)) col = mul(col, 1.12);  // rolled top edge
    else if (c.face === 'bottom' || c.face === 'back') col = mul(col, 0.8);
    if (tex(c, 5) > 0.992) col = mix(col, SCUFF, 0.25);
    return mul(col, K.G(c, 0.05));
  };
  B.push(...vox([-6, -7, -6], [6, 0, -0.5], g, shieldKeep, shieldPaint, 'face shield'));

  // ---- raised reinforcing plate round the slot (lower corners cut off), bolted at the corners
  const panKeep = (x, y) => Math.abs(x) < 4 && y > -4.5 && y < -1 && !slot(x, y) && !(y > -1.5 && Math.abs(x) > 3.5);
  const panel = c => {
    if (c.face === 'back') return null;
    const [x, y] = c.p;
    if (c.face !== 'front') return slot(x, y + (c.face === 'top' ? 0.01 : c.face === 'bottom' ? -0.01 : 0)) || (Math.abs(x) <= 3.5 + 1e-6 && y > -3.5 && y < -3) ? mul(SLOT, 1.1) : mul(PAN, c.face === 'top' ? 1.2 : 0.8);
    let col = mix(PAN, PANL, 0.35 + 0.4 * fbm(x * 0.5 + 3, y * 0.5 + 1, 5, 2));
    if (y < -4) col = mul(col, 1.08);                                          // upper edge catches the light
    return mul(col, K.G(c, 0.05));
  };
  B.push(...vox([-4, -5, -6.5], [4, -1, -6], g, (x, y) => panKeep(x, y), panel, 'reinforcing plate'));
  const bolt = c => (c.face === 'back' ? null : mul(c.face === 'front' ? BOLT : BOLTD, c.face === 'top' ? 1.3 : 1));
  for (const [bx, by] of [[-3.75, -4.25], [3.25, -4.25], [-3.25, -1.75], [2.75, -1.75]]) B.push(box('head', [bx, by, -6.75], [bx + 0.5, by + 0.5, -6.5], bolt, { tag: 'shield bolt' }));

  // ---- helmet side hardware: domed bolt towards the front, hex bolt on the shield tab, small latch under it
  for (const s of [-1, 1]) {
    const inner = s < 0 ? 'left' : 'right', outer = s < 0 ? 'right' : 'left';
    const hw = col => c => (c.face === inner ? null : mul(col, c.face === outer ? K.G(c, 0.06) : c.face === 'top' ? 1.2 : 0.75));
    const x0 = s < 0 ? -5.75 : 5.5;                                                // on the shell side
    B.push(box('head', [x0, -7.5, -3.25], [x0 + 0.25, -7, -2.75], hw(BOLT), { tag: 'domed bolt' }));
    const x1 = s < 0 ? -6.25 : 6;                                                  // on the shield tab
    B.push(box('head', [x1, -6.75, -1.25], [x1 + 0.25, -6.25, -0.75], hw(BOLTD), { tag: 'hex bolt' }));
    B.push(box('head', [x0, -3.75, -0.25], [x0 + 0.25, -3, 0.25], hw(BOLTD), { tag: 'latch' }));
  }

  // ---- visor lug on the crown: a small bracket with a knob
  const lug = c => (c.face === 'bottom' ? null : mul(LUG, c.face === 'top' ? 1.15 : 0.85));
  const knob = c => (c.face === 'bottom' ? null : mul(LUGL, c.face === 'top' ? 1.1 : 0.8));
  B.push(box('head', [-1, -10.25, -2], [1, -10, -0.5], lug, { tag: 'top lug' }));
  B.push(box('head', [-0.5, -11, -1.75], [0.5, -10.25, -0.75], knob, { tag: 'top lug' }));

  // ---- olive webbing chin straps: front and rear strap meet at a buckle by the jaw
  const web = c => (c.face !== 'left' && c.face !== 'right' ? mul(WEB, 0.8) : mul(tex(c, 8) > 0.85 ? WEBL : WEB, K.G(c, 0.06)));
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.2 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-2.75, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-2.75, 2], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
