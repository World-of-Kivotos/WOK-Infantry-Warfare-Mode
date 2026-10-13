// ================= Altyn bulletproof helmet (Olive Drab) + Altyn face shield — EFT reference (tarkov.dev 5aa7e276e5b5b000171d0647, shield 5aa7e373e5b5b000137b76f0) =================
// Titanium helmet in olive drab, a big round dome. Black rubber U-trim round the whole edge, thick black fur padding
// inside (it shows at the edge). The sides come down over the ears; low on each side a grey-olive visor pivot housing
// with a hex bolt above it; a small latch lug on the crown. The mod item ships with the face shield lowered: a thick
// olive armour plate that wraps the face from the brow to the chin and swings back to the pivots, with a narrow
// armoured-glass window at eye level in a black rubber frame. The in-game renderer draws the Altyn opaque, so the
// window is left open here (no glass pane) and the eyes show through it. Black webbing chin straps to the jaw buckles.
ARMORS.altyn_heavy = function (mode) {
  const K = kit('M');
  const g = 0.5, B = [];

  // ---- palette (albedo; entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#757b4e, shaded
  //      sides ~#504d3a, edge trim ~#141a19, fur ~#181611, pivot housing ~#494e44, shield plate ~#58553b,
  //      shield frame ~#34373a, glass ~#373c49
  const OD = hex('#6a7248'), ODL = hex('#858d5c'), ODD = hex('#535b3a'), SCUFF = hex('#a4a98a');
  const TRIM = hex('#1c2221'), FUR = hex('#1d1b17'), FURL = hex('#35322b');
  const VISL = hex('#7b7e55'), VISD = hex('#4e5034');
  const FR = hex('#212627'), FRL = hex('#3c4243'), GL = hex('#3b4250');
  const PIV = hex('#5d6354'), PIVD = hex('#3a3f35'), PIVL = hex('#7d8272');
  const BOLT = hex('#43472f'), LUG = hex('#4f563a'), WEB = hex('#1f2425'), BUCKLE = hex('#2f3434');

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

  // ---- shell: brim half a pixel above the eyes, the sides down over the ears, the back down to the nape
  // shield outline (used by the shell paint for the shadow line above the shield's top edge)
  const ytop = z => (z <= -4 ? -6.5 : z >= -1 ? -5.5 : -6.5 + (z + 4) / 3);
  const ybot = z => (z <= -4 ? -0.5 : z >= 0 ? -2.5 : -0.5 - (z + 4) * 0.5);
  const cellZ = z => (Math.floor((z + 6) / g + 1e-6) + 0.5) * g - 6;
  const shieldTop = z => -7 + g * (Math.floor((ytop(cellZ(z)) + 6.75) / g) + 1);

  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3], [-2, -2], [3, -2], [5.5, -1]]);
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome: [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]], corner: 2, rim };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return Math.max(Math.abs(x), Math.abs(z)) < 5 ? mul(FUR, K.G(c, 0.2)) : mul(TRIM, K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.06));
    const n = fbm(x * 0.35 + 7, y * 0.35 + 3, z * 0.35 + 11, 2);
    let col = mix(ODD, ODL, clamp01(0.38 + (n - 0.5) * 0.9 + 0.25 * smoothstep(-5, -9.5, y)));
    if (dy < 1) col = mul(col, 0.9);
    const st = shieldTop(z);                                                     // shadow line over the shield edge
    if (c.face !== 'top' && z < 0.5 && y < st && y > st - 0.5) col = mul(col, 0.62);
    if (tex(c, 3) > 0.99) col = mix(col, SCUFF, 0.28);                          // scuffs
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- fur padding: a roll of black fur peeking out under the trim on the sides and the back (0.25 grid, the upper
  //      half hides inside the shell wall)
  const shellBottom = (x, z) => {
    const cz = (Math.floor((z + 5.5) / g) + 0.5) * g - 5.5;
    return -10 + g * Math.floor((rim(0, cz) + 10) / g + 1e-6);
  };
  const furKeep = (x, y, z) => z > -3 && inRR(x, z, 5.25, 5.25, 5.25, 1.75) && !inRR(x, z, 4.75, 4.75, 4.75, 1.25) && Math.abs(y - shellBottom(x, z)) < 0.5;
  const fur = c => (c.face === 'top' ? null : mix(FUR, FURL, 0.15 + 0.6 * tex(c, 9) * tex(c, 4)));
  B.push(...vox([-5.25, -5, -5.25], [5.25, 0.5, 5.25], 0.25, furKeep, fur, 'fur padding'));

  // ---- face shield, lowered: a 0.5 px armour plate lying on the shell front and sides (1 px clear of the face),
  //      from the brow down to the chin, its wings narrowing back to the pivots; eye-level window cut through
  const win = (x, y) => Math.abs(x) < 3.5 && y > -4 && y < -2;
  const shieldKeep = (x, y, z) => z < 0.5 && inRR(x, z, 6, 6, 6, 2.5) && !inRR(x, z, 5.5, 5.5, 5.5, 2) && y > ytop(z) && y < ybot(z) && !win(x, y);
  const shieldPaint = c => {
    const [x, y, z] = c.p;
    // faces looking into the window: the edge of the armoured glass block
    if ((c.face === 'left' || c.face === 'right' || c.face === 'top' || c.face === 'bottom') && Math.abs(x) <= 3.5 + 1e-6 && y >= -4 - 1e-6 && y <= -2 + 1e-6 && z < -5.4) return mul(GL, K.G(c, 0.05));
    const n = fbm(x * 0.35 + 21, y * 0.35 + 5, z * 0.35 + 2, 2);
    let col = mix(VISD, VISL, clamp01(0.42 + (n - 0.5) * 0.9 - 0.12 * smoothstep(-3, 0, y)));
    if (c.face === 'top' || (c.face !== 'bottom' && c.face !== 'back' && y < shieldTop(z) + 0.5)) col = mul(col, 1.14);  // worn top edge
    else if (c.face === 'bottom' || c.face === 'back') col = mul(col, 0.8);     // plate edges / inside
    if (tex(c, 5) > 0.99) col = mix(col, SCUFF, 0.28);
    return mul(col, K.G(c, 0.05));
  };
  B.push(...vox([-6, -7, -6], [6, 0, 0.5], g, shieldKeep, shieldPaint, 'face shield'));

  // ---- rubber frame round the window, standing half a pixel proud of the plate
  const frame = c => {
    if (c.face === 'back') return null;
    const [x, y] = c.p;
    if (c.face !== 'front') return Math.abs(x) < 3.5 + 1e-6 && y > -4 - 1e-6 && y < -2 + 1e-6 ? mul(FR, 0.8) : mul(FR, c.face === 'top' ? 1.25 : 0.95);
    if (Math.abs(x) > 3.75 || y < -4.25 || y > -1.75) return mul(FR, K.G(c, 0.06));
    return mul(FRL, K.G(c, 0.06));                                              // inner lip catches the light
  };
  B.push(box('head', [-4, -4.5, -6.5], [4, -4, -6], frame, { tag: 'window frame' }));
  B.push(box('head', [-4, -2, -6.5], [4, -1.5, -6], frame, { tag: 'window frame' }));
  B.push(box('head', [-4, -4, -6.5], [-3.5, -2, -6], frame, { tag: 'window frame' }));
  B.push(box('head', [3.5, -4, -6.5], [4, -2, -6], frame, { tag: 'window frame' }));

  // ---- visor pivot housings low on the sides (the shield wings end under them), hex bolt above each
  for (const s of [-1, 1]) {
    const xa = s < 0 ? -6.75 : 6, outer = s < 0 ? 'right' : 'left', inner = s < 0 ? 'left' : 'right';
    const piv = c => {
      if (c.face === inner) return null;
      const [, y, z] = c.p;
      if (c.face === outer) {
        if (Math.abs(y + 3.5) < 0.5 && Math.abs(z) < 0.5) return mul(PIVD, K.G(c, 0.05));   // pivot bolt
        if (y < -4 || z < -0.5 || z > 0.5) return mul(PIVL, K.G(c, 0.05));                // worn edge
        return mul(PIV, K.G(c, 0.05));
      }
      return mul(PIV, c.face === 'top' ? 1.1 : c.face === 'bottom' ? 0.7 : 0.9);
    };
    B.push(box('head', [xa, -4.5, -1], [xa + 0.75, -2.5, 1], piv, { tag: 'visor pivot' }));
    const bx = s < 0 ? -5.75 : 5.5;
    const bolt = c => (c.face === inner ? null : mul(BOLT, c.face === outer ? K.G(c, 0.08) : 0.8));
    B.push(box('head', [bx, -7, -0.5], [bx + 0.25, -6.5, 0], bolt, { tag: 'hex bolt' }));
  }

  // ---- latch lug on the crown
  const lug = c => (c.face === 'bottom' ? null : mul(LUG, c.face === 'top' ? 1.15 : 0.85));
  B.push(box('head', [-0.75, -10.25, -1.75], [0.75, -10, -0.25], lug, { tag: 'top lug' }));
  B.push(box('head', [-0.5, -10.75, -1.5], [0.5, -10.25, -0.5], lug, { tag: 'top lug' }));

  // ---- black webbing chin straps: front and rear strap meet at a buckle by the jaw
  const web = c => (c.face !== 'left' && c.face !== 'right' ? mul(WEB, 0.8) : mul(WEB, K.G(c, 0.06)));
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.2 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-2.25, -2.5], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-2.25, 2], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
