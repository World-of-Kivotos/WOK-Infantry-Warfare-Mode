// ================= LShZ lightweight helmet (Olive Drab) — EFT reference (tarkov.dev 5b432d215acfc4771e1c6624) =================
// Green FAST-style high-cut shell with a slightly bumpy painted finish and khaki wear on the edges. The brim sits on
// the brow and runs back to the temple; there the edge rises into a high cut over the ear and drops straight down
// behind it, then covers the back of the head. On each side a grey ARC-style rail runs along the top of the cut and
// turns down along its rear edge (a row of slots, a round lock knob at the front end). Front: a Wilcox-style
// one-slot NVG bracket (dark olive plate, a long slot, two screws), like the 6B47 bracket. A large lighter khaki
// loop-Velcro patch with a lobed outline on the crown. Dark olive webbing harness with grey plastic sliders and a
// grey buckle by the jaw (the chin cup is left out: it would cut into the body when the head tilts).
ARMORS.lzsh_light = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: crown ~#91a774,
  //      lit front ~#6f805a, shaded side ~#60744d, worn brim ~#666245, loop Velcro ~#acac7e, bracket ~#485139 with
  //      edges ~#6a7953, rail ~#424545 / slots ~#2f3130, harness ~#282a20, buckles ~#465254
  const OD = hex('#76895a'), ODL = hex('#93ab70'), ODD = hex('#607245'), WEAR = hex('#a3a06b'), BUMP = hex('#55663c');
  const TRIM = hex('#3a4629');
  const LOOP = hex('#aeac7a'), LOOPD = hex('#8a895d');
  const BR = hex('#4c5638'), BRD = hex('#1f241a'), BRL = hex('#72805a'), SCREW = hex('#8d9384');
  const RAIL = hex('#4a4e4e'), RAILD = hex('#1d1f1f'), RAILL = hex('#6e7575'), KNOB = hex('#2c2d2b');
  const WEB = hex('#35382a'), WEBL = hex('#4b4e3c'), BUCKLE = hex('#56605f');

  // ---- shell: brim half a pixel above the eyes (the MC forehead is tall) and on to the temple, high cut over the
  //      ear with a straight rear edge, back down to the nape
  const rim = rimAlong([[-5.5, -4.5], [-4, -4.5], [-3, -4.5], [-2.5, -6], [1.25, -6], [1.5, -3.5], [2.5, -2.5],
    [4, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  // lobed loop-Velcro patch on the crown (plan shape, noise-edged)
  const onPatch = (x, y, z) => (y < -9.25 || (y < -8.75 && z > 1.5)) &&
    (x / 3.25) ** 2 + ((z - 1.25) / 3.5) ** 2 + (fbm(x * 0.7 + 2, 3, z * 0.7 + 5, 2) - 0.5) * 1.2 < 1;
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.05));
    if (dy < 1 && colBottom(c, S) > -5) return mul(mix(OD, WEAR, 0.45), K.G(c, 0.06));   // worn khaki brim edge
    if (onPatch(x, y, z)) return mix(LOOPD, LOOP, 0.35 + 0.65 * tex(c, 5));
    // painted shell: soft mottling, lighter towards the crown, small darker bumps and a few scuffs
    const n = fbm(x * 0.4 + 7, y * 0.4 + 3, z * 0.4 + 11, 2);
    let col = mix(ODD, ODL, clamp01(0.35 + (n - 0.5) * 0.9 + 0.3 * smoothstep(-5.5, -9.75, y)));
    const t = tex(c, 3);
    if (t > 0.95) col = mix(col, BUMP, 0.25);
    else if (t < 0.012) col = mix(col, WEAR, 0.4);
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- one-slot NVG bracket on the front
  const bracket = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(BR, c.face === 'top' ? 1.15 : 0.8);
    const [x, y] = c.p;
    if (Math.abs(x) < 0.5 && y > -7.5 && y < -5.75) return mul(BRD, K.G(c, 0.05));   // the long slot
    if (Math.abs(x) > 0.5 && Math.abs(x) < 1 && y > -6 && y < -5.5) return SCREW;       // screws at the foot
    if (y < -7.5 || c.ex < 0.5) return mul(BRL, K.G(c, 0.05));                          // top bar / light frame edge
    return mul(BR, K.G(c, 0.06));
  };
  B.push(box('head', [-1.25, -8, -6], [1.25, -5.5, -5.5], bracket, { tag: 'NVG bracket' }));

  // ---- ARC-style rails: along the top of the ear cut, then down its rear edge; lock knob at the front end
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const outer = s < 0 ? 'right' : 'left', inner = s < 0 ? 'left' : 'right';
    // texel index along the rail (k, from the front) and down it (j, from the top)
    const rail = vertical => c => {
      if (c.face === inner) return null;                                        // lies on the shell
      if (c.face !== outer) return mul(RAIL, c.face === 'top' ? 1.2 : 0.8);
      const k = Math.floor((c.p[2] - (vertical ? 1.25 : -2.75)) * 2), j = Math.floor((c.p[1] - (vertical ? -6 : -7.5)) * 2);
      if (!vertical) {
        if (j === 1 && (k === 3 || k === 5 || k === 7)) return mul(RAILD, K.G(c, 0.05));   // slots
        if (j === 0) return mul(RAILL, 0.95);                                   // top lip catches the light
      } else if (k === 0 && (j === 1 || j === 3)) return mul(RAILD, K.G(c, 0.05));
      return mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -7.5, -2.75], [xb, -6, 2.25], rail(false), { tag: 'ARC rail' }));
    B.push(box('head', [xa, -6, 1.25], [xb, -4, 2.25], rail(true), { tag: 'ARC rail (rear)' }));
    const [ka, kb] = s < 0 ? [-6.5, -6.25] : [6.25, 6.5];
    const knob = c => (c.face === inner ? null : mul(KNOB, c.face === outer ? 1.1 : 0.85));
    B.push(box('head', [ka, -7.25, -2.5], [kb, -6.25, -1.5], knob, { tag: 'rail knob' }));
  }

  // ---- harness: front strap from under the rail's front end, rear strap from behind the ear, grey sliders and a
  //      grey buckle by the jaw
  const strap = (xs, a, b, slider, tag) => {
    const web = c => {
      if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
      const d = c.p[1] - a[0];
      if (d > slider && d < slider + 0.5) return mul(BUCKLE, 1.05);
      return mul(WEB, K.G(c, 0.06));
    };
    sideStrap(B, xs, 0.25, a, b, 0.5, web, tag);
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.25 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    strap(xs, [-6.25, -1.75], [-0.75, -0.75], 2.5, 'front strap');
    strap(xs, [-3.25, 2.5], [-0.75, -0.5], 1, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
