// ================= HighCom Striker ACHHC IIIA (Black) — EFT reference (tarkov.dev 5b40e3f35acfc40016388218) =================
// Plain ACH / MICH-style shell in a smooth, slightly warm black paint, rounded crown, no rails and no NVG mount.
// Mid cut: the brim sits on the brow and the sides run back at about the same height; over each ear the edge steps up
// in a small arch (the "HC" cut), then behind the ear it drops to cover the back of the head down to the nape.
// A thin dark rubber trim wraps the whole edge (worn to a blue-grey sheen in places). One retention bolt on each
// side, just ahead of the ear arch. Grey-black webbing harness: a front strap from under the bolt and a rear strap
// from behind the ear meet at a black buckle by the jaw (the chin cup is left out: it would cut into the body when
// the head tilts).
ARMORS.achhc_light = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#50504f,
  //      shaded sides ~#3e3a39, edge trim ~#313537 with a blue-grey sheen ~#3b4547, bolt ~#3f4a43 / #6c6858,
  //      harness ~#363735 / #2b2e2c
  const SH = hex('#403e3c'), SHL = hex('#5c5a55'), SCUFF = hex('#7a776f');
  const TRIM = hex('#252829'), TRIML = hex('#374244');
  const BOLT = hex('#555a52'), BOLTL = hex('#807b6b');
  const WEB = hex('#3b3c3c'), WEBL = hex('#545657'), BUCKLE = hex('#1f2022'), RIVET = hex('#8a8c86');

  // ---- shell: brim half a pixel above the eyes (the MC forehead is tall), sides a little lower, a small arch over
  //      the ear (the model's own cut), back down to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -3.25], [-1.5, -3], [-1, -3.5], [-0.5, -4.5], [0, -5],
    [2, -5], [2.5, -4], [3, -3], [4.5, -1.5], [5.5, -1]]);
  const dome = [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    // rubber trim, worn to a blue-grey sheen in stretches
    if (dy < 0.5) return mul(fbm(x * 0.9 + 3, y * 0.9, z * 0.9 + 7, 2) > 0.6 ? TRIML : TRIM, K.G(c, 0.05));
    // smooth black paint: soft mottling, a little lighter towards the crown, a few light scuffs
    const n = fbm(x * 0.35 + 5, y * 0.35 + 2, z * 0.35 + 9, 2);
    let col = mix(SH, SHL, clamp01(0.25 + (n - 0.5) * 0.8 + 0.45 * smoothstep(-6, -9.75, y)));
    if (dy < 1) col = mul(col, 0.92);                                          // slight shade above the trim
    if (tex(c, 3) > 0.993) col = mix(col, SCUFF, 0.25);
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- retention bolt on each side, just ahead of the ear arch
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5.75 : 5.5;
    const bolt = c => (c.face === (s < 0 ? 'left' : 'right') ? null : mul(c.face === 'top' ? BOLTL : BOLT, c.face === (s < 0 ? 'right' : 'left') ? 1 : 0.8));
    B.push(box('head', [x0, -6, -1.5], [x0 + 0.25, -5.5, -1], bolt, { tag: 'side bolt' }));
  }

  // ---- harness: front strap from under the bolt, rear strap from behind the ear, buckle by the jaw.
  //      Webbing with a small black slider near the top of each strap
  const strap = (xs, a, b, slider, tag) => {
    const web = c => {
      if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
      const d = c.p[1] - a[0];                                                // distance along the strap
      if (d > slider && d < slider + 0.5) return mul(BUCKLE, 1.1);
      return mul(WEB, K.G(c, 0.06));
    };
    sideStrap(B, xs, 0.25, a, b, 0.5, web, tag);
  };
  const buckle = c => {
    if ((c.face === 'left' || c.face === 'right') && c.ev < 0.5 && c.eu < 0.5) return RIVET;
    return mul(BUCKLE, c.face === 'top' ? 1.3 : 1);
  };
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    strap(xs, [-3.75, -1.5], [-0.75, -0.75], 1.25, 'front strap');
    strap(xs, [-3.5, 3.5], [-0.75, -0.5], 1.5, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
