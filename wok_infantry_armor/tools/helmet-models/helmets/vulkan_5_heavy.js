// ================= Vulkan-5 LShZ-5 bulletproof helmet (Black) + Vulkan-5 face shield — EFT reference (tarkov.dev 5ca20ee186f774799474abc2, shield 5ca2113f86f7740b2547e1d2) =================
// Very round, thick black shell with a matte finish. A heavy rubber edge trim stands proud round the whole rim; a
// small screw low on each side towards the back. The mod item ships with the face shield lowered: a big curved
// armoured-glass visor from the brow to the chin that wraps round to the cheeks, held by a black brow frame whose
// arms run back along the sides to a hinge above each ear. The glass sits in its own 'visor glass' boxes so the game
// can draw that part translucent through a named bone; the preview paints it opaque smoky blue-grey. Black webbing
// chin straps to the jaw buckles.
ARMORS.vulkan_5_heavy = function (mode) {
  const K = kit('M');
  const g = 0.5, B = [];

  // ---- palette (albedo; entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#333336, shaded
  //      sides ~#232224, trim ~#181a18..#212627, shield frame / arms ~#2f2f30, hinge ~#2c2c2c, screw ~#5d5958
  const SH = hex('#303134'), SHL = hex('#404245'), SHD = hex('#28292b'), SPECK = hex('#56585b');
  const TRIM = hex('#1b1d1e'), TRIML = hex('#2e3132');
  const FR = hex('#2b2c2e'), FRL = hex('#46484b'), FRD = hex('#1a1b1c');
  const GL = hex('#3a4552'), GLL = hex('#5f6d7b'), GLD = hex('#29303a');
  const HINGE = hex('#37383a'), SCREW = hex('#6e6a68'), WEB = hex('#222527'), BUCKLE = hex('#34383a');

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

  // ---- shell: very round (corner 2.5, round dome), brim half a pixel above the eyes, sides to eye level, back to
  //      the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3], [-2, -2.5], [3, -2.5], [5.5, -1.5]]);
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome: [[-10, 2.5, 2], [-9.5, 1, 2.5], [-9, 0.5, 2.5]], corner: 2.5, rim };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.05));
    let col = mix(SHD, SHL, clamp01(0.3 + 0.45 * smoothstep(-6, -9.5, y) + (fbm(x * 0.4 + 5, y * 0.4, z * 0.4 + 9, 2) - 0.5) * 0.6));
    if (tex(c, 3) > 0.985) col = mix(col, SPECK, 0.3);                          // scuffs
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- heavy rubber trim standing half a pixel proud of the shell along the lowest pixel of the rim (behind the
  //      glass the painted trim is enough)
  const stepRim = z => -10 + g * Math.floor((rim(0, z) + 10) / g + 1e-6);
  const lipKeep = (x, y, z) => z > -2 && inRR(x, z, 6, 6, 6, 3) && !inRR(x, z, 5.5, 5.5, 5.5, 2.5) && y < stepRim(z) && y > stepRim(z) - 1;
  const lip = c => {
    if (c.face === 'top') return mul(TRIML, K.G(c, 0.05));
    return mul(c.face === 'bottom' ? mul(TRIM, 0.85) : c.p[1] < stepRim(c.p[2]) - 0.5 + 1e-6 && c.face !== 'bottom' ? mul(TRIML, 0.9) : TRIM, K.G(c, 0.05));
  };
  B.push(...vox([-6, -6, -6], [6, 0, 6], g, lipKeep, lip, 'edge trim'));

  // ---- face shield, lowered. Brow frame: 1 px thick on the shell front, arms back to the hinges above the ears
  const ftop = z => (z <= -3 ? -6.5 : -6.5 + (z + 3) * 0.4);
  const frameKeep = (x, y, z) => z < 1 && inRR(x, z, 6.5, 6.5, 6.5, 3.5) && !inRR(x, z, 5.5, 5.5, 5.5, 2.5) && y > ftop(z) && y < ftop(z) + 1;
  const frame = c => {
    const [x, y, z] = c.p;
    if (c.face === 'top') return mul(FRL, K.G(c, 0.05));
    if (c.face === 'bottom' || c.face === 'back') return mul(FRD, K.G(c, 0.05));
    let col = y < ftop(z) + 0.5 ? FR : mul(FR, 0.88);
    if (tex(c, 4) > 0.97) col = mix(col, FRL, 0.5);                            // worn edges
    return mul(col, K.G(c, 0.05));
  };
  B.push(...vox([-6.5, -7, -6.5], [6.5, -3, 1], g, frameKeep, frame, 'visor frame'));

  // ---- the glass: 0.5 px lying on the shell front above the brim, 1 px clear of the hat layer below it, from under
  //      the brow frame to the chin, wrapping round to the cheeks; the frame stands half a pixel proud of it
  const gbot = z => (z <= -4 ? 0 : -3 * (z + 4) / 2);
  const glassKeep = (x, y, z) => z < -2 && inRR(x, z, 6, 6, 6, 3) && !inRR(x, z, 5.5, 5.5, 5.5, 2.5) && y > ftop(z) + 1 && y < gbot(z);
  const glass = c => {
    const [x, y, z] = c.p;
    if (c.face !== 'front' && c.face !== 'left' && c.face !== 'right') return mul(GLD, K.G(c, 0.04));
    let col = mix(GLD, GL, clamp01((y + 5.5) / 2.5));                          // shadow under the brow frame
    if (y > -5 && y < -4.5) col = mix(col, GLL, 0.3);                          // sky reflection band
    if (c.face !== 'front') col = mix(col, GLL, 0.12);                          // the wrap catches more light
    if (y > gbot(z) - 0.5) col = mul(col, 0.82);                               // bottom edge
    return mul(col, K.G(c, 0.03));
  };
  B.push(...vox([-6, -6, -6], [6, 0, -2], g, glassKeep, glass, 'visor glass'));

  // ---- hinges above the ears where the arms end, a screw low on each side towards the back
  for (const s of [-1, 1]) {
    const inner = s < 0 ? 'left' : 'right', outer = s < 0 ? 'right' : 'left';
    const xh = s < 0 ? -7 : 6.5;
    const hinge = c => {
      if (c.face === inner) return null;
      if (c.face === outer) {
        const [, y, z] = c.p;
        if (Math.abs(y + 4.75) < 0.26 && Math.abs(z - 0.25) < 0.26) return mul(SCREW, K.G(c, 0.05));   // pivot screw
        return mul(HINGE, K.G(c, 0.05));
      }
      return mul(HINGE, c.face === 'top' ? 1.2 : 0.8);
    };
    B.push(box('head', [xh, -5.5, -0.5], [xh + 0.5, -4, 1], hinge, { tag: 'visor hinge' }));
    const xs = s < 0 ? -6.25 : 6;
    const screw = c => (c.face === inner ? null : mul(SCREW, c.face === outer ? K.G(c, 0.06) : 0.75));
    B.push(box('head', [xs, -2.75, 2.25], [xs + 0.25, -2.25, 2.75], screw, { tag: 'side screw' }));
  }

  // ---- black webbing chin straps: front and rear strap meet at a buckle by the jaw
  const web = c => (c.face !== 'left' && c.face !== 'right' ? mul(WEB, 0.8) : mul(WEB, K.G(c, 0.06)));
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.25 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-2.75, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-2.75, 2], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
