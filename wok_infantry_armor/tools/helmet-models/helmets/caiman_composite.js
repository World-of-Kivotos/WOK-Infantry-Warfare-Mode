// ================= Galvion Caiman Hybrid helmet (Grey) — EFT reference (tarkov.dev 5f60b34a41e30a4ab12a6947) =================
// Grey composite shell with a slightly bluish tint and a thin dark edge trim. High cut: the brim runs level from the
// brow well back along the temple, then the edge jumps up over the ear and drops again behind it. Long black side rails
// follow the cut: a horizontal run above the ear with rows of recesses, then down along the rear edge of the cut.
// Front: a black NVG mount (tall frame with a window, short arms with screws) and a black bungee cord from the mount
// along the brim to the rail front. Dark vent slots on the crown and the upper sides. Lighter grey loop Velcro patches
// (crown, upper sides, the front sides along the brim, the back). Black mesh pads show under the cut. Black harness: a
// front and a rear strap meet at a buckle by the jaw (no chin cup: it would cut into the body when the head tilts).
ARMORS.caiman_composite = function (mode) {
  const K = kit('M');
  const B = [];

  // ---- palette (albedo; MC entity light draws sides at ~0.74, tops at 1.0). Reference render: lit crown ~#6f7271,
  //      shaded side ~#525558, Velcro ~#6b6d69..#9b9a8f (speckled), rails ~#262627, mount ~#363431, vents ~#0e1113,
  //      mesh pads ~#15191b, webbing ~#211f1f
  const SHL = hex('#777c7b'), SHD = hex('#5b605f'), SCUFF = hex('#9ea3a0');
  const TRIM = hex('#2d3334');
  const VEL = hex('#9a9c95'), VELD = hex('#80827c'), VELL = hex('#b7b9b1'), VELE = hex('#8a8c86');
  const RAIL = hex('#303132'), RAILD = hex('#101112'), RAILL = hex('#505355');
  const SHR = hex('#2f3031'), SHRW = hex('#535858'), SHRL = hex('#56595b'), SCREW = hex('#6d7071');
  const VENT = hex('#111415'), VENTM = hex('#262b2d'), CORD = hex('#1b1c1d');
  const PAD = hex('#191c1e'), PADL = hex('#272c2f');
  const WEB = hex('#2b2929'), WEBL = hex('#3e3c3b'), BUCKLE = hex('#1e1d1d');

  // ---- shell: brim half a pixel above the eyes and level back past the temple, high cut over the ear, back down to
  //      the nape; a slightly narrower crown than the MTEK helmets
  const rim = rimAlong([[-5.5, -4.5], [-2.5, -4.5], [-1.5, -6], [1.5, -6], [2.5, -2.5], [3.5, -1.5], [5.5, -1.5]]);
  const dome = [[-10, 2.5, 1.5], [-9.5, 1, 2], [-9, 0.5, 2]];
  const S = { g: 0.5, hx: 5.5, zf: 5.5, zb: 5.5, dome, corner: 2, rim };

  const tex = (c, salt) => hash3(Math.floor(c.p[0] * 2 + 1000), Math.floor(c.p[1] * 2 + 1000), Math.floor(c.p[2] * 2 + 1000) + salt * 131);
  // patch test: conds [[axis, lo, hi]], axis x / y / z, ax = |x|, dy = height above the rim. Returns the distance to
  // the nearest in-plane patch edge, or -1 outside (corner texels dropped so the patches look rounded)
  const PLANE = { top: [0, 2], bottom: [0, 2], front: [0, 1], back: [0, 1], left: [1, 2], right: [1, 2] };
  const AX = { x: 0, ax: 0, y: 1, dy: 1, z: 2 };
  const patch = (c, dy, conds) => {
    let d1 = 9, d2 = 9;
    const pl = PLANE[c.face];
    for (const [a, lo, hi] of conds) {
      const v = a === 'ax' ? Math.abs(c.p[0]) : a === 'dy' ? dy : c.p[AX[a]];
      if (v < lo || v > hi) return -1;
      const d = a === 'ax' && lo === 0 ? hi - v : Math.min(v - lo, hi - v);  // |x| from 0: no edge on the centre line
      if (AX[a] === pl[0]) d1 = Math.min(d1, d); else if (AX[a] === pl[1]) d2 = Math.min(d2, d);
    }
    if (d1 < 0.5 && d2 < 0.5) return -1;
    return Math.min(d1, d2);
  };
  const PATCHES = [
    [['y', -11, -9.25], ['ax', 0, 2.5], ['z', -0.5, 4.5]],                  // crown, centre to rear
    [['ax', 4.25, 9], ['y', -9.5, -7.5], ['z', -1.5, 2.5]],                 // upper sides, above the rails, over the dome steps
    [['dy', 1, 2.5], ['ax', 3, 9], ['z', -9, -2]],                          // front sides along the brim
    [['z', 4.75, 9], ['ax', 0, 2.5], ['y', -7.5, -5.5]],                    // back
  ];
  const VENTS = [
    c => c.p[1] < -9.25 && Math.abs(c.p[0]) > 1 && Math.abs(c.p[0]) < 1.5 && c.p[2] > -3.5 && c.p[2] < -1,   // crown front pair
    c => Math.abs(c.p[0]) > 4.75 && c.p[1] > -8.5 && c.p[1] < -7.5 && c.p[2] > 2.5 && c.p[2] < 3.5,         // small rear side vent
    c => Math.abs(c.p[0]) > 4.75 && c.p[1] > -8.5 && c.p[1] < -7.5 && c.p[2] > -3 && c.p[2] < -1.5
      && !(c.p[1] < -8 && c.p[2] < -2.5),                                                                     // angled side vent
  ];
  const velcro = (c, d) => {
    const t = tex(c, 5);
    if (d < 0.5) return mul(t < 0.35 ? VELD : VELE, K.G(c, 0.05));           // edge: a little darker, no outline
    return mul(t < 0.35 ? VELD : t > 0.72 ? VELL : VEL, K.G(c, 0.04));       // fuzzy loop speckle
  };
  const paint = c => {
    const [x, y, z] = c.p;
    if (c.face === 'bottom') return mul(TRIM, 0.9 * K.G(c, 0.05));
    const dy = colBottom(c, S) - y;
    if (dy < 0.5) return mul(TRIM, K.G(c, 0.05));                            // thin dark edge trim
    if (dy < 1 && z < -2 && Math.abs(x) > 2.25) return mul(CORD, K.G(c, 0.08)); // bungee cord, mount to rail
    for (const V of VENTS) if (V(c)) return tex(c, 9) > 0.6 ? VENTM : VENT;
    for (const P of PATCHES) { const d = patch(c, dy, P); if (d >= 0) return velcro(c, d); }
    const n = fbm(x * 0.35 + 7, y * 0.35 + 3, z * 0.35 + 11, 2);
    let col = mix(SHD, SHL, clamp01(0.4 + (n - 0.5) * 0.8 + 0.25 * smoothstep(-5, -9.5, y)));
    if (tex(c, 3) > 0.985) col = mix(col, SCUFF, 0.45);                      // scuffs
    return mul(col, K.G(c, 0.05));
  };
  B.push(...shell(S, paint));

  // ---- black NVG mount: tall frame with a window (the grey shell shows through), short arms with screws
  const frame = c => {
    if (c.face === 'back') return null;
    if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.2 : 0.85);
    const i = Math.abs(Math.round(c.p[0] * 2)), y = c.p[1];
    if (i <= 1 && y > -8 && y < -5.5) return mul(SHRW, K.G(c, 0.05));       // window
    return c.ev < 0.5 || c.ex < 0.5 && i === 2 ? mul(SHRL, K.G(c, 0.05)) : mul(SHR, K.G(c, 0.06));
  };
  B.push(box('head', [-1.25, -8.5, -6.5], [1.25, -5, -5.5], frame, { tag: 'NVG mount' }));
  for (const s of [-1, 1]) {
    const arm = c => {
      if (c.face === 'back') return null;
      if (c.face !== 'front') return mul(SHR, c.face === 'top' ? 1.2 : 0.85);
      return Math.abs(c.p[0]) > 1.75 ? SCREW : mul(SHR, K.G(c, 0.06));
    };
    const [xa, xb] = s < 0 ? [-2.25, -1.25] : [1.25, 2.25];
    B.push(box('head', [xa, -6.5, -6], [xb, -6, -5.5], arm, { tag: 'NVG mount' }));
  }

  // ---- long side rails: a run above the ear (top slots, a row of long recesses, a slotted screw at the front), then
  //      down along the rear edge of the cut
  for (const s of [-1, 1]) {
    const [xa, xb] = s < 0 ? [-6.25, -5.5] : [5.5, 6.25];
    const inner = s < 0 ? 'left' : 'right', outer = s < 0 ? 'right' : 'left';
    const run = c => {
      if (c.face === inner) return null;                                       // lies on the shell
      const [, y, z] = c.p, col = Math.round((z + 1.75) * 2);                  // column 0 at the front end
      if (c.face === 'top') return mul(RAILL, 1.05);
      if (c.face !== outer) return mul(RAIL, 0.85);
      const r = Math.round((y + 7.25) * 2);                                     // row 0 = top lip
      if (r === 0) return mul(RAILL, K.G(c, 0.05));
      if (r === 1) return col % 3 === 0 ? mul(RAIL, K.G(c, 0.05)) : mul(RAILD, K.G(c, 0.05));
      return col === 0 ? SCREW : mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -7.5, -2], [xb, -6, 3], run, { tag: 'rail' }));
    const down = c => {
      if (c.face === inner) return null;
      if (c.face !== outer) return mul(RAIL, c.face === 'top' ? 1.2 : 0.85);
      const r = Math.round((c.p[1] + 5.75) * 2);                                // row 0 just under the run
      return r % 2 === 1 && c.ex >= 0.25 ? mul(RAILD, K.G(c, 0.05)) : mul(RAIL, K.G(c, 0.05));
    };
    B.push(box('head', [xa, -6, 2], [xb, -3.5, 3], down, { tag: 'rail' }));
    B.push(box('head', [xa, -3.5, 2.5], [xb, -2.5, 3], down, { tag: 'rail' }));
  }

  // ---- black mesh pads showing under the cut (set 0.5 px in from the shell surface)
  for (const s of [-1, 1]) {
    const x0 = s < 0 ? -5 : 4.75;
    const pad = c => {
      if (c.face === 'top' || c.face === (s < 0 ? 'left' : 'right')) return null;
      const dot = (Math.floor(c.p[1] * 2 + 100) + Math.floor(c.p[2] * 2 + 100)) % 2 === 0;
      return mul(dot ? PADL : PAD, K.G(c, 0.06));
    };
    B.push(box('head', [x0, -6, -2], [x0 + 0.25, -4.5, 1.5], pad, { tag: 'pad' }));
  }

  // ---- harness: front strap from the front of the cut, rear strap from behind the ear, buckle by the jaw
  const web = c => {
    if (c.face !== 'left' && c.face !== 'right') return mul(WEB, 0.8);
    return c.ex < 0.25 ? mul(WEBL, 0.95) : mul(WEB, K.G(c, 0.06));
  };
  const buckle = c => mul(BUCKLE, c.face === 'top' ? 1.4 : 1);
  for (const s of [-1, 1]) {
    const xs = s < 0 ? -4.75 : 4.5;
    sideStrap(B, xs, 0.25, [-5.5, -2.25], [-0.75, -0.75], 0.5, web, 'front strap');
    sideStrap(B, xs, 0.25, [-3, 2.75], [-0.75, -0.5], 0.5, web, 'rear strap');
    const bx = s < 0 ? -5 : 4.5;
    B.push(box('head', [bx, -1.25, -1], [bx + 0.5, -0.25, -0.25], buckle, { tag: 'buckle' }));
  }
  return { S: K.S, px: K.px, cell: K.cell, boxes: B };
};
