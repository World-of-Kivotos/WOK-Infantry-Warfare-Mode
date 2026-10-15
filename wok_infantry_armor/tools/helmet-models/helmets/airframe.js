// ================= Crye Precision AirFrame (Tan) — EFT reference (tarkov.dev 5c17a7ed2e2216152142459c) =================
// Tan high-cut shell with a rounded crown and a thick rolled brim (light top, dark green-grey rubber trim under it).
// The brim runs round the front to the temples; there the edge jumps up into the AirFrame ear cut (the "chop"): high
// over the ear, with a stepped rear edge that leaves a narrow tab coming down behind the ear, then the shell covers
// the back of the head. Dark liner pads show inside the cut. On each side a chunky tan Crye rail sits above the cut
// (ribbed top and bottom edges, a row of vertical slots, a dark round lock knob at the front). Front: a skeletonised
// NVG shroud, hexagonal, with a tall centre window, two small side windows and three screws. The front-view render
// shows no vents on the crown, so none are modelled. Tan webbing harness: a front strap from under the rail and a
// rear strap from the tab meet at a buckle by the jaw (the chin cup is left out: it would cut into the body when the
// head tilts).
ARMORS.airframe = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: shaded front
  //      ~#8d7a5e, lit side ~#a28764, crown blown out to ~#ffeeab, shroud ~#ad9b76, brim trim ~#2f4437,
  //      rail ~#524b3c .. #9d8f75, knob ~#3d3a38, pads ~#171819, harness ~#5d5946
  const SH = hex('#ac9169'), SHL = hex('#c6ab7f'), SHD = hex('#937a54'), DIRT = hex('#81704f');
  const TRIM = hex('#34413a'), LIP = hex('#ccb58a');
  const SHR = hex('#b39a70'), SHRD = hex('#6e5f45'), SHRL = hex('#cdb68c'), SCREW = hex('#6f6758');
  const RAIL = hex('#8a7b5d'), RAILD = hex('#3a3428'), RAILL = hex('#a89773'), KNOB = hex('#3a3835');
  const PAD = hex('#1d1e1d'), PADL = hex('#2c2d2a');
  const WEB = hex('#6b6650'), WEBL = hex('#827b61'), BUCKLE = hex('#4d4a3f');

  // ---- shell: brim half a pixel above the eyes (the MC forehead is tall), the AirFrame ear cut over the ear with
  //      a stepped rear edge (the tab behind the ear), back down to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3.25, -4.5], [-2.75, -6], [1, -6], [1.25, -4.75], [1.75, -4.75],
    [2, -2.5], [3.5, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const bot = colBottom(c, S), dy = bot - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.05));
    // rolled brim round the front: a light lip over the trim, a soft shadow above it
    if (bot > -5 && z < -2.75) {
      if (dy < 1) return mul(LIP, K.G(c, 0.05));
      if (dy < 1.5) return mul(SHD, K.G(c, 0.05));
    }
    // tan paint: soft mottling, lighter towards the crown, a little grime low down and a few scuffs
    const n = fbm(x * 0.35 + 3, y * 0.35 + 8, z * 0.35 + 1, 2);
    let col = mix(SHD, SHL, clamp01(0.4 + (n - 0.5) * 0.9 + 0.35 * smoothstep(-5.5, -9.75, y)));
    if (fbm(x * 0.8 + 11, y * 0.8, z * 0.8 + 4, 2) > 0.68) col = mix(col, DIRT, 0.25);
    if (tex(c, 3) > 0.992) col = mix(col, SHL, 0.6);
    return mul(col, K.G(c, 0.04));
  };
  B.push(...shell(S, paint));

  // ---- skeletonised NVG shroud: hexagonal outline from three stacked boxes; centre window, two side windows,
  //      three screws; the windows show the shell behind in shadow
  const shroud = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(c.face === 'top' ? SHRL : SHRD, K.G(c, 0.05));
    const [x, y] = c.p, ax = Math.abs(x);
    if (ax < 0.75 && y > -8 && y < -6) return mul(SH, y < -7.5 ? 0.42 : 0.66);          // centre window (shell behind)
    if (ax > 1.25 && ax < 1.75 && y > -7.5 && y < -6.5) return mul(SH, y < -7 ? 0.42 : 0.66);  // side windows
    if ((ax < 0.25 && y < -8) || (ax > 1.75 && y > -7.5 && y < -7)) return SCREW;            // screws
    return mul(SHRL, K.G(c, 0.05));                                                          // frame catches the light
  };
  B.push(box('head', [-1.25, -8.5, -6], [1.25, -7.5, -5.5], shroud, { tag: 'NVG shroud (top)' }));
  B.push(box('head', [-2.25, -7.5, -6], [2.25, -6.5, -5.5], shroud, { tag: 'NVG shroud' }));
  B.push(box('head', [-1.75, -6.5, -6], [1.75, -5.5, -5.5], shroud, { tag: 'NVG shroud (foot)' }));

  // ---- Crye rails above the ear cut: ribbed top and bottom rows, vertical slots between, lock knob at the front
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const outer = s < 0 ? 'right' : 'left', inner = s < 0 ? 'left' : 'right';
    // texel index along the rail (k, from the front) and down it (j, from the top)
    const rail = c => {
      if (c.face === inner) return null;                                        // lies on the shell
      const k = Math.floor((c.p[2] + 3) * 2), j = Math.floor((c.p[1] + 8) * 2);
      if (c.face === 'top') return mul(RAILL, k % 2 ? 0.85 : 1.05);             // ribbed top edge
      if (c.face !== outer) return mul(RAIL, 0.8);
      if (j === 0) return k >= 2 && k % 2 ? mul(RAIL, 0.8) : mul(RAILL, K.G(c, 0.05));   // ribbed top row
      if ((j === 1 || j === 2) && (k === 3 || k === 5 || k === 7)) return mul(RAILD, K.G(c, 0.05));   // slots
      if (j === 3) return mul(RAIL, 0.82 * K.G(c, 0.05));                       // lower ledge in shadow
      return mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -8, -3], [xb, -6, 1], rail, { tag: 'Crye rail' }));
    const [ka, kb] = s < 0 ? [-6.5, -6.25] : [6.25, 6.5];
    const knob = c => (c.face === inner ? null : mul(KNOB, c.face === outer ? 1.1 : 0.85));
    B.push(box('head', [ka, -7.5, -2.75], [kb, -6.5, -1.75], knob, { tag: 'rail knob' }));
    // dark liner pad showing along the top of the ear cut
    const [pa, pb] = s < 0 ? [-5, -4.75] : [4.75, 5];
    const pad = c => (c.face === outer ? mul(tex(c, 11) > 0.6 ? PADL : PAD, 1) : mul(PAD, 0.8));
    B.push(box('head', [pa, -6, -3], [pb, -5, 1], pad, { tag: 'liner pad' }));
  }

  // ---- harness: front strap from under the rail, rear strap from the tab behind the ear, buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.3 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-6.25, -2], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-3.25, 2.5], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
