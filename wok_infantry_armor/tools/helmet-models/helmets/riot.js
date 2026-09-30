// ================= Kolpak-1S riot helmet — EFT reference (tarkov.dev 59e7711e86f7746cae05fbe1) =================
// An open-face helmet shaped like a motorcycle helmet: a smooth, rounded grey-olive painted shell whose sides come
// all the way down over the ears to the jaw. The face is open from the brow down. A thick dark blue-grey rubber trim
// runs round the face opening and the lower edge; just inside it a mint green lining shows along the opening, with a
// dark olive padding further in. On each side: a black comma-shaped clip plate with two screws at temple height, a
// small round vent/speaker grill lower down over the ear, and two small rivets. Olive webbing chin straps come out of
// the side pieces to a black buckle by the jaw (a small red tag on the buckle). No visor on this item.
ARMORS['riot'] = function (mode) {
  const K = kit('M'), g = 0.5;
  const B = [];

  // ---- palette (albedo). Reference render: crown ~#7f7a71, upper side ~#625e5c, trim ~#495256 (lower edge ~#232c2d),
  //      mint lining ~#468060, padding ~#2c3326 / lit ~#4e543e, clip ~#34353a, grill ~#2e3739, webbing ~#546347,
  //      buckle ~#394246, red tag ~#7b3d30
  const SHL = hex('#8a897b'), SHD = hex('#5d5e55'), SCUFF = hex('#a3a296');
  const TRIM = hex('#444d51'), TRIML = hex('#5a6468');
  const MINT = hex('#4f9575'), MINTD = hex('#3b765c'), PAD = hex('#2f3628'), PADL = hex('#4b513c');
  const CLIP = hex('#2a2d31'), CLIPL = hex('#464b50'), SCREW = hex('#6b7176');
  const GRILL = hex('#4a4c47'), HOLE = hex('#15191a'), RIVET = hex('#4f514b');
  const WEB = hex('#56663d'), WEBD = hex('#394424'), BUCKLE = hex('#30363a'), BUCKLEL = hex('#4c5559'), RED = hex('#a63c2d');

  // ---- shell: round crown, open face from the brow down, the sides down to the jaw, the back to the nape.
  //      The last row of each side piece steps in half a pixel so it rounds off towards the jaw.
  const rim = rimAlong([[-5.5, -4.5], [-4.25, -4.5], [-3.25, -2.5], [-2.75, -0.5], [2, -0.5], [4, -1], [5.5, -1]]);
  const dome = [[-10, 2, 2], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim, cut: (x, y, z) => y > -1 && Math.abs(x) > 5 };

  const inPlan = (x, z) => {
    const qx = Math.abs(x) - 3.5, qz = Math.abs(z) - 3.5;
    return qx > 0 && qz > 0 ? 2 - Math.hypot(qx, qz) : Math.min(5.5 - Math.abs(x), 5.5 - Math.abs(z));
  };
  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  const paint = c => {
    const [x, y, z] = c.p, ax = Math.abs(x);
    if (c.face === 'bottom') {
      if (inPlan(x, z) < 0.5) return mul(TRIM, K.G(c, 0.05));                // rubber trim wraps under the edge
      if (z < -4.5 && y > -5) return mul(MINT, K.G(c, 0.05));                 // lining under the brow
      return mul(y > -1.5 ? PADL : PAD, K.G(c, 0.06));
    }
    // face opening: the side-piece fronts show the rubber trim outside and the mint lining inside
    if (c.face === 'front' && y > -4.5) return ax > 5 ? mul(TRIML, K.G(c, 0.05)) : mul(y < -2.5 ? MINT : MINTD, K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, (dy < 0.25 ? 0.9 : 1.05) * K.G(c, 0.05));
    // smooth painted shell: soft mottling, lighter on the crown, a few scuffs and scratch marks
    const n = fbm(x * 0.35 + 5, y * 0.35 + 1, z * 0.35 + 9, 2);
    let col = mix(SHD, SHL, clamp01(0.35 + (n - 0.5) * 0.8 + 0.3 * smoothstep(-5, -9.5, y)));
    if (dy < 1) col = mul(col, 0.92);                                            // shadow above the trim
    if (tex(c, 3) > 0.985) col = mix(col, SCUFF, 0.45);
    if ((c.face === 'top' || c.face === 'left') && Math.abs(z - 0.8 * x - 1) < 0.3 && y < -8.5 && x > 0) col = mix(col, SCUFF, 0.3);   // long scratch
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- side hardware: comma-shaped clip plate with two screws, round grill, two rivets
  // clip plate texel map (0.5 px cells), front -> back along z, top -> bottom along y ('#' plate, 'o' screw)
  const CLIPMAP = ['.####..', '#o##o#.', '.######', '.....##'];
  for (const s of [-1, 1]) {
    const out = s < 0 ? 'right' : 'left', inn = s < 0 ? 'left' : 'right';
    const [xa, xb] = s < 0 ? [-5.75, -5.5] : [5.5, 5.75];
    const cell = (c, z0, y0) => [Math.floor((c.p[2] - z0) * 2 + 1e-6), Math.floor((c.p[1] - y0) * 2 + 1e-6)];
    const clip = c => {
      if (c.face === inn) return null;
      const [u, v] = cell(c, -2.25, -6.25);
      const ch = (CLIPMAP[Math.min(3, Math.max(0, v))] || '')[Math.min(6, Math.max(0, u))];
      if (ch !== '#' && ch !== 'o') return null;
      if (c.face !== out) return mul(CLIP, c.face === 'top' ? 1.5 : 0.85);
      if (ch === 'o') return SCREW;
      return v === 0 && u < 5 ? CLIPL : mul(CLIP, K.G(c, 0.05));               // glossy top edge
    };
    B.push(box('head', [xa, -6.25, -2.25], [xb, -4.25, 1.25], clip, { tag: 'clip plate' }));
    // round grill: 2 px disc (corner texels left out) a little darker than the shell, dark holes in the middle
    const grill = c => {
      if (c.face === inn) return null;
      const [u, v] = cell(c, -2, -3.25);
      if ((u === 0 || u === 3) && (v === 0 || v === 3)) return null;
      if (c.face !== out) return mul(GRILL, c.face === 'top' ? 1.6 : 1.1);
      if ((u === 1 || u === 2) && (v === 1 || v === 2)) return (u + v) & 1 ? HOLE : mul(GRILL, 1.3);
      return mul(GRILL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -3.25, -2], [xb, -1.25, 0], grill, { tag: 'grill' }));
    const rivet = c => (c.face === inn ? null : mul(RIVET, c.face === 'top' ? 1.3 : c.face === out ? 1.1 : 0.85));
    B.push(box('head', [xa, -1.75, -2.75], [xb, -1.25, -2.25], rivet, { tag: 'rivet' }));
    B.push(box('head', [xa, -2, 2.25], [xb, -1.5, 2.75], rivet, { tag: 'rivet' }));
  }

  // ---- olive chin straps out of the side pieces to a black buckle by the jaw (red tag on the buckle)
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEBD, 0.9);
    return c.ex < 0.25 ? WEBD : mul(WEB, K.G(c, 0.08));
  };
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5, out = s < 0 ? 'right' : 'left';
    sideStrap(B, xs, 0.25, [-2.25, -2.5], [-0.5, -3.5], 0.5, web, 'chin strap');
    const bx = s < 0 ? -5 : 4.5;
    const buckle = c => {
      if (c.face === 'top') return BUCKLEL;
      if (c.face !== out) return mul(BUCKLE, 0.9);
      const [, y, z] = c.p;
      return z > -3.75 && y < -0.5 ? RED : mul(BUCKLE, K.G(c, 0.05));
    };
    B.push(box('head', [bx, -1, -4.25], [bx + 0.5, -0.25, -3.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
