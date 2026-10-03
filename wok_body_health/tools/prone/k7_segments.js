// k7: faithful port of TAA 1.2.5 ProneTacticalAnimationClient (stationary / gait / transition / keepAboveGround)
// to compute hit segments in BODY-LOCAL coords: f = forward along anchor, s = right (+), y = up from feet.
// Frame: anchor = 0 -> forward = +Z, right = -X.  (MC: forward=(-sin a,0,cos a), right=(-cos a,0,-sin a))
'use strict';
const fs = require('fs');
const J = JSON.parse(fs.readFileSync(process.env.TAA_JSON, 'utf8'));
const D = Math.PI / 180, PI = Math.PI;

// ---------------- quaternion (JOML semantics) ----------------
const Q = {
  id: () => [0, 0, 0, 1],
  mul: (a, b) => [ // a*b
    a[3]*b[0] + a[0]*b[3] + a[1]*b[2] - a[2]*b[1],
    a[3]*b[1] - a[0]*b[2] + a[1]*b[3] + a[2]*b[0],
    a[3]*b[2] + a[0]*b[1] - a[1]*b[0] + a[2]*b[3],
    a[3]*b[3] - a[0]*b[0] - a[1]*b[1] - a[2]*b[2]],
  x: (t) => [Math.sin(t/2), 0, 0, Math.cos(t/2)],
  y: (t) => [0, Math.sin(t/2), 0, Math.cos(t/2)],
  z: (t) => [0, 0, Math.sin(t/2), Math.cos(t/2)],
  inv: (q) => { const n = q[0]*q[0]+q[1]*q[1]+q[2]*q[2]+q[3]*q[3]; return [-q[0]/n, -q[1]/n, -q[2]/n, q[3]/n]; },
  zyx: (z, y, x) => Q.mul(Q.mul(Q.z(z), Q.y(y)), Q.x(x)),
  rot: (q, v) => { const p = Q.mul(Q.mul(q, [v[0], v[1], v[2], 0]), Q.inv(q)); return [p[0], p[1], p[2]]; },
  rotationTo: (a, b) => { // unit a -> unit b
    const d = a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
    const c = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
    if (d < -0.999999) { let ax = [0, a[2], -a[1]]; if (Math.hypot(...ax) < 1e-6) ax = [-a[2], 0, a[0]]; const l = Math.hypot(...ax); return [ax[0]/l, ax[1]/l, ax[2]/l, 0]; }
    const q = [c[0], c[1], c[2], 1 + d]; const l = Math.hypot(...q); return q.map(v => v/l);
  },
  slerp: (a, b, t) => { let d = a[0]*b[0]+a[1]*b[1]+a[2]*b[2]+a[3]*b[3]; let bb = b; if (d < 0) { d = -d; bb = b.map(v => -v); }
    if (d > 0.9995) { const r = a.map((v, i) => v + (bb[i]-v)*t); const l = Math.hypot(...r); return r.map(v => v/l); }
    const th = Math.acos(d), s = Math.sin(th), w1 = Math.sin((1-t)*th)/s, w2 = Math.sin(t*th)/s; return a.map((v, i) => w1*v + w2*bb[i]); },
};
// PronePoseMath.eulerZYX
function eulerZYX(q) {
  const [x, y, z, w] = q; const sc = 2 / (x*x+y*y+z*z+w*w); const r20 = (x*z - w*y)*sc;
  const ry = Math.asin(Math.max(-1, Math.min(1, -r20))); let rx, rz;
  if (Math.abs(r20) > 0.9999999) { rx = Math.atan2(-(y*z - w*x)*sc, 1 - (x*x+z*z)*sc); rz = 0; }
  else { rx = Math.atan2((y*z + w*x)*sc, 1 - (x*x+y*y)*sc); rz = Math.atan2((x*y + w*z)*sc, 1 - (y*y+z*z)*sc); }
  return [rx, ry, rz];
}

// ---------------- 4x4 matrices (column-major) ----------------
function ident() { return [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]; }
function mul(a, b) { const r = new Array(16).fill(0); for (let c = 0; c < 4; c++) for (let rr = 0; rr < 4; rr++) { let s = 0; for (let k = 0; k < 4; k++) s += a[k*4+rr]*b[c*4+k]; r[c*4+rr] = s; } return r; }
function T(x, y, z) { const m = ident(); m[12] = x; m[13] = y; m[14] = z; return m; }
function S(x, y, z) { const m = ident(); m[0] = x; m[5] = y; m[10] = z; return m; }
function QM(q) { const [x, y, z, w] = q; return [1-2*(y*y+z*z), 2*(x*y+z*w), 2*(x*z-y*w), 0, 2*(x*y-z*w), 1-2*(x*x+z*z), 2*(y*z+x*w), 0, 2*(x*z+y*w), 2*(y*z-x*w), 1-2*(x*x+y*y), 0, 0, 0, 0, 1]; }
const RX = (a) => QM(Q.x(a)), RY = (a) => QM(Q.y(a));
function apply(m, p) { return [m[0]*p[0]+m[4]*p[1]+m[8]*p[2]+m[12], m[1]*p[0]+m[5]*p[1]+m[9]*p[2]+m[13], m[2]*p[0]+m[6]*p[1]+m[10]*p[2]+m[14]]; }
function inv3dir(m, v) { // solve M3 * x = v
  const a=m[0],b=m[4],c=m[8],d=m[1],e=m[5],f=m[9],g=m[2],h=m[6],i=m[10];
  const A=e*i-f*h, B=-(d*i-f*g), C=d*h-e*g, det=a*A+b*B+c*C;
  const r=[A, -(b*i-c*h), (b*f-c*e), B, (a*i-c*g), -(a*f-c*d), C, -(a*h-b*g), (a*e-b*d)].map(x=>x/det);
  return [r[0]*v[0]+r[1]*v[1]+r[2]*v[2], r[3]*v[0]+r[4]*v[1]+r[5]*v[2], r[6]*v[0]+r[7]*v[1]+r[8]*v[2]];
}

// ---------------- PlayerModel (wide arms) ----------------
const NAMES = ['head', 'body', 'leftArm', 'rightArm', 'leftLeg', 'rightLeg'];
const INIT = { head: [0,0,0], body: [0,0,0], rightArm: [-5,2,0], leftArm: [5,2,0], rightLeg: [-1.9,12,0], leftLeg: [1.9,12,0] };
const CUBE = { head: [-4,-8,-4,8,8,8], body: [-4,0,-2,8,12,4], rightArm: [-3,-2,-2,4,12,4], leftArm: [-1,-2,-2,4,12,4], rightLeg: [-2,0,-2,4,12,4], leftLeg: [-2,0,-2,4,12,4] };
const INFL = { head: 0.5, body: 0.25, rightArm: 0.25, leftArm: 0.25, rightLeg: 0.25, leftLeg: 0.25 }; // hat 0.5, jacket/sleeve/pants 0.25
function freshModel() { const m = {}; for (const n of NAMES) m[n] = { x: INIT[n][0], y: INIT[n][1], z: INIT[n][2], xr: 0, yr: 0, zr: 0 }; return m; }
const store = (p) => ({ ...p });
const load = (p, s) => { p.x = s.x; p.y = s.y; p.z = s.z; p.xr = s.xr; p.yr = s.yr; p.zr = s.zr; };
const reset = (m) => { for (const n of NAMES) load(m[n], { x: INIT[n][0], y: INIT[n][1], z: INIT[n][2], xr: 0, yr: 0, zr: 0 }); };
const partQ = (p) => Q.zyx(p.zr, p.yr, p.xr);
function rotate(p, q) { const e = eulerZYX(q); p.xr = e[0]; p.yr = e[1]; p.zr = e[2]; }
function applyT(p, t) { p.x += t.x; p.y += t.y; p.z += t.z; p.xr += t.rotX*D; p.yr += t.rotY*D; p.zr += t.rotZ*D; }
function blendPose(p, tgt, t) { p.x += (tgt.x-p.x)*t; p.y += (tgt.y-p.y)*t; p.z += (tgt.z-p.z)*t; rotate(p, Q.slerp(partQ(p), partQ(tgt), t)); }
function bendAtWaist(m, t) {
  const r = t.rotX*D, c = Math.cos(r), s = Math.sin(r);
  m.body.x = t.x; m.body.y = t.y + 12 - 12*c; m.body.z = t.z - 12*s; m.body.xr = r;
  for (const n of ['head', 'leftArm', 'rightArm']) { const p = m[n]; const y = p.y - 12, z = p.z; p.y = 12 + y*c - z*s + t.y; p.z = y*s + z*c + t.z; p.xr += r; }
}
const smooth = (x) => { x = Math.max(0, Math.min(1, x)); return x*x*(3-2*x); };
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const wrap = (a) => { a %= 360; if (a >= 180) a -= 360; if (a < -180) a += 360; return a; };

// ---------------- TAA config sampling ----------------
function mono(a, b) { return (Math.abs(a) < 1e-9 || Math.abs(b) < 1e-9 || a*b <= 0) ? 0 : 2*a*b/(a+b); }
function herm(p0, p1, p2, p3, t) { const d0 = p1-p0, d1 = p2-p1, d2 = p3-p2, m1 = mono(d0, d1), m2 = mono(d1, d2), t2 = t*t, t3 = t2*t; return (2*t3-3*t2+1)*p1 + (t3-2*t2+t)*m1 + (-2*t3+3*t2)*p2 + (t3-t2)*m2; }
function hermA(p0, p1, p2, p3, t) { const q0 = p1 + wrap(p0-p1), q2 = p1 + wrap(p2-p1), q3 = q2 + wrap(p3-q2); return wrap(herm(q0, p1, q2, q3, t)); }
const BONES = ['leftLeg', 'rightLeg', 'body', 'head', 'leftArm', 'rightArm', 'root'];
function hermFrame(f0, f1, f2, f3, t) { const o = {}; for (const b of BONES) { o[b] = {}; for (const k of ['x','y','z']) o[b][k] = herm(f0[b][k], f1[b][k], f2[b][k], f3[b][k], t); for (const k of ['rotX','rotY','rotZ']) o[b][k] = hermA(f0[b][k], f1[b][k], f2[b][k], f3[b][k], t); } return o; }
function lerpFrame(a, b, t) { const o = {}; for (const bn of BONES) { o[bn] = {}; for (const k of ['x','y','z','rotX','rotY','rotZ']) o[bn][k] = a[bn][k] + (b[bn][k]-a[bn][k])*t; } return o; }
function circLerp(a, b, t) { const o = {}; for (const bn of BONES) { o[bn] = {}; for (const k of ['x','y','z']) o[bn][k] = a[bn][k] + (b[bn][k]-a[bn][k])*t; for (const k of ['rotX','rotY','rotZ']) o[bn][k] = wrap(a[bn][k] + wrap(b[bn][k]-a[bn][k])*t); } return o; }
const ZERO = (() => { const o = {}; for (const b of BONES) o[b] = { x:0,y:0,z:0,rotX:0,rotY:0,rotZ:0 }; return o; })();
function sampleMotion(anim, progress) { const fr = anim.frames, c = clamp(progress, 0, 1), sc = c*6; const i1 = clamp(Math.floor(sc), 0, 6), i2 = clamp(i1+1, 0, 6), i0 = Math.max(0, i1-1), i3 = Math.min(6, i2+1); return hermFrame(fr[i0], fr[i1], fr[i2], fr[i3], clamp(sc-i1, 0, 1)); }
function sampleAngle(angle) {
  const ap = J.anglePoses; const c = clamp(angle, -180, 180); const seam = circLerp(ap[0].pose, ap[36].pose, 0.5);
  if (c <= -179.9999999 || c >= 179.9999999) return seam;
  const caf = (i) => (i > 0 && i < 36) ? ap[i].pose : seam;
  const sc = (c + 180)/10; const i1 = clamp(Math.floor(sc), 0, 35), i2 = i1+1, l = clamp(sc-i1, 0, 1);
  return hermFrame(i1 === 0 ? caf(35) : caf(i1-1), caf(i1), caf(i2), i2 === 36 ? caf(1) : caf(i2+1), l);
}

// ---------------- TaCZ default third-person gun hold (ThirdPersonManager$1.animateGunHold) ----------------
// rightArm.yRot = -0.3 + head.yRot; leftArm.yRot = 0.8 + head.yRot; both xRot = -1.4 + head.xRot (head reset to 0 at call time)
function nativeHold() { return { right: { x: -5, y: 2, z: 0, xr: -1.4, yr: -0.3, zr: 0 }, left: { x: 5, y: 2, z: 0, xr: -1.4, yr: 0.8, zr: 0 } }; }

// ---------------- stationary() ----------------
function stationary(m, v, nat, pitchDeg) {
  const s = v.sample;
  m.leftLeg.zr = -0.09; m.rightLeg.zr = 0.09;
  bendAtWaist(m, s.body);
  applyT(m.leftLeg, s.leftLeg); applyT(m.rightLeg, s.rightLeg);
  const yaw = v.aim*D, pitch = clamp(pitchDeg, -70, 70)*D, roll = v.roll*D;
  const body = Q.mul(Q.mul(Q.x(-PI/2), Q.y(roll)), Q.z(PI));
  const head = Q.mul(Q.mul(Q.mul(Q.inv(body), Q.y(-yaw)), Q.z(PI)), Q.x(pitch));
  rotate(m.head, head);
  const arm = Q.mul(head, Q.x(-PI/2));
  rotate(m.leftArm, Q.mul(Q.mul(arm, Q.x(4*D)), Q.z(-0.12)));
  rotate(m.rightArm, Q.mul(Q.mul(arm, Q.x(4*D)), Q.z(0.12)));
  if (v.gun) {
    const nr = nat.right, nl = nat.left;
    const right = Q.inv(Q.zyx(nr.zr, nr.yr, nr.xr));
    const relLeft = Q.mul(right, Q.zyx(nl.zr, nl.yr, nl.xr));
    rotate(m.rightArm, arm);
    let leftRot = Q.mul(arm, relLeft);
    let wrist = Q.rot(Q.zyx(nl.zr, nl.yr, nl.xr), [0, 10, 0]); wrist = [wrist[0] + nl.x - nr.x, wrist[1] + nl.y - nr.y, wrist[2] + nl.z - nr.z];
    wrist = Q.rot(right, wrist); wrist = Q.rot(arm, wrist);
    wrist = [wrist[0] + m.rightArm.x - m.leftArm.x, wrist[1] + m.rightArm.y - m.leftArm.y, wrist[2] + m.rightArm.z - m.leftArm.z];
    const l2 = wrist[0]**2 + wrist[1]**2 + wrist[2]**2;
    if (l2 > 0.01) { const dir = Q.rot(leftRot, [0, 1, 0]); const wl = Math.sqrt(l2); leftRot = Q.mul(Q.rotationTo(dir, wrist.map(x => x/wl)), leftRot); }
    rotate(m.leftArm, leftRot);
  }
  applyT(m.head, s.head);
  if (!v.gun) { applyT(m.leftArm, s.leftArm); applyT(m.rightArm, s.rightArm); }
  gait(m, v);
  for (const n of ['leftLeg', 'rightLeg']) { const leg = m[n]; const local = partQ(leg); const brace = 65*Math.sin(v.roll*D); rotate(leg, Q.mul(Q.y(brace*D), local)); }
}
function gait(m, v) {
  if (!(v.movement > 0)) return;
  const f = v.forward, side = v.side, w = v.movement, cycle = v.stride*(f < -0.15 ? -1 : 1), a = Math.sin(cycle), b = Math.cos(cycle), left = (1+b)*0.5, right = (1-b)*0.5;
  m.leftLeg.xr += (0.04 + left*0.22)*w; m.rightLeg.xr += (0.04 + right*0.22)*w;
  m.leftLeg.yr += side*0.2*w; m.rightLeg.yr += side*0.2*w;
  m.leftLeg.zr -= (0.1 + left*0.2)*w; m.rightLeg.zr += (0.1 + right*0.2)*w;
  m.leftLeg.y -= left*2.8*w; m.rightLeg.y -= right*2.8*w;
  m.leftLeg.x += (a*0.65 + side*left*1.2)*w; m.rightLeg.x += (-a*0.65 + side*right*1.2)*w;
  m.leftLeg.z += Math.max(0, a)*1.25*w; m.rightLeg.z += Math.max(0, -a)*1.25*w;
  if (!v.gun) {
    blendPose(m.leftArm, { x: 5, y: 2 + a*0.9, z: 0, xr: -174*D, yr: 0, zr: -0.12 }, w);
    blendPose(m.rightArm, { x: -5, y: 2 - a*0.9, z: 0, xr: -174*D, yr: 0, zr: 0.12 }, w);
  } else {
    m.rightArm.y += a*1.5*w; m.rightArm.x += (b*0.35 + side*a*0.4)*w; m.rightArm.z += Math.max(0, a)*0.9*w;
    blendPose(m.leftArm, { x: 5.4 - a*0.65 + side*right, y: 2 - a*2.6, z: Math.max(0, -a)*1.35, xr: (-166 - 7*b)*D, yr: 0, zr: -0.12 }, w);
  }
}

// ---------------- root transforms (applyLockedStationaryProneRotations + LivingEntityRenderer tail) ----------------
function rootMatrix(v) {
  let m = RY((180 - 0)*D); // anchor = 0
  if (v.transition) m = mul(m, T(0, v.sample.root.y/16, v.sample.root.z/16));
  m = mul(m, RX(-90*v.prone*D));
  m = mul(m, T(0, -v.prone, 0.15*v.prone));
  m = mul(m, RY(v.roll*D));
  if (v.transition && v.recovering) { const r = v.recovering.sample.root; m = mul(m, T(r.x/16*v.recovery, r.y/16*v.recovery, r.z/16*v.recovery)); }
  if (!v.transition) m = mul(m, T(v.sample.root.x/16, v.sample.root.y/16, v.sample.root.z/16));
  m = mul(m, S(-1, -1, 1)); m = mul(m, S(0.9375, 0.9375, 0.9375)); m = mul(m, T(0, -1.501, 0));
  return m;
}
function partMat(p) { return mul(T(p.x/16, p.y/16, p.z/16), QM(partQ(p))); }
function corners(n, infl) { const [x, y, z, w, h, d] = CUBE[n]; const o = []; for (let i = 0; i < 8; i++) o.push([(i&1 ? x+w+infl : x-infl)/16, (i&2 ? y+h+infl : y-infl)/16, (i&4 ? z+d+infl : z-infl)/16]); return o; }
function worldPts(root, m, n, infl) { const M = mul(root, partMat(m[n])); return corners(n, infl).map(p => apply(M, p)); }
function lowest(root, m, list) { let lo = Infinity; for (const n of list) for (const p of worldPts(root, m, n, 0)) lo = Math.min(lo, p[1]); return lo; }
function keepAboveGround(root, m, v) {
  const contact = !v.transition ? ['body', 'head'] : NAMES;
  const lift = Math.max(0, 0.005 - lowest(root, m, contact));
  if (isFinite(lift) && lift < 2 && lift > 0) { const l = inv3dir(root, [0, lift, 0]); root = mul(root, T(l[0], l[1], l[2])); }
  if (!v.transition && v.gun && v.movement < 0.01) {
    const corr = Math.max(0, 0.003 - lowest(root, m, ['leftArm', 'rightArm']));
    if (corr > 0 && corr < 0.4) { const l = inv3dir(root, [0, corr, 0]).map(x => x*16); for (const n of ['leftArm', 'rightArm']) { m[n].x += l[0]; m[n].y += l[1]; m[n].z += l[2]; } }
  }
  if (!v.transition) for (const n of ['leftArm', 'rightArm', 'leftLeg', 'rightLeg']) {
    const corr = Math.max(0, 0.003 - lowest(root, m, [n]));
    if (corr > 0 && corr < 0.4) { const l = inv3dir(root, [0, corr, 0]).map(x => x*16); m[n].x += l[0]; m[n].y += l[1]; m[n].z += l[2]; }
  }
  return root;
}

// ---------------- full pose pipelines ----------------
function steadyVisual(o) { // phase PRONE (2), converged smoothing
  const aim = o.aim || 0; const sample = sampleAngle(aim);
  return { sample, prone: 1, roll: sample.root.rotY, aim, movement: o.movement || 0, forward: 1, side: o.side || 0, stride: o.stride || 0, transition: false, gun: !!o.gun, recovering: null, recovery: 0, support: 0, turnProgress: -1 };
}
function poseSteady(o) {
  const v = steadyVisual(o); const m = freshModel(); const nat = nativeHold();
  stationary(m, v, nat, o.pitch || 0);
  let root = rootMatrix(v); root = keepAboveGround(root, m, v);
  return { root, m, v };
}
// standing-ish upright pose used by blendUpright (vanilla setupAnim result): head pitch/yaw, TaCZ hold arms if gun
function animatedPoses(o) {
  const hx = (o.pitch || 0)*D, hy = 0;
  const head = { x: 0, y: 0, z: 0, xr: hx, yr: hy, zr: 0 };
  if (o.gun) return { head, right: { x: -5, y: 2, z: 0, xr: -1.4 + hx, yr: -0.3 + hy, zr: 0 }, left: { x: 5, y: 2, z: 0, xr: -1.4 + hx, yr: 0.8 + hy, zr: 0 } };
  return { head, right: { x: -5, y: 2, z: 0, xr: 0, yr: 0, zr: 0 }, left: { x: 5, y: 2, z: 0, xr: 0, yr: 0, zr: 0 } };
}
function blendUpright(p, up, prone, w) { if (w <= 0) return; const q = Q.mul(Q.x(-90*prone*D), Q.zyx(up.zr, up.yr, up.xr)); const e = eulerZYX(q); blendPose(p, { x: up.x, y: up.y, z: up.z, xr: e[0], yr: e[1], zr: e[2] }, w); }
function poseTransition(enter, progress, o) {
  const anim = enter ? J.proneEnter : J.proneExit; const gun = !!o.gun;
  let from = null, recovery = 0, sampleTime = progress;
  if (!enter) { from = steadyVisual({ gun, aim: 0 }); recovery = 1 - smooth(progress/0.24); sampleTime = clamp((progress - 0.24)/0.76, 0, 1); }
  const sample = sampleMotion(anim, sampleTime);
  const blend = enter ? 1 + sample.root.rotX/90 : sample.root.rotX/90;
  let aim = 0, roll = from ? from.roll*recovery : 0;
  const v = { sample, prone: clamp(blend, 0, 1), roll, aim, movement: 0, forward: 1, side: 0, stride: 0, transition: true, gun, recovering: from, recovery, support: 0, turnProgress: enter ? progress : -1 };
  const m = freshModel(); const nat = nativeHold(); const an = animatedPoses(o);
  const s = sample;
  applyT(m.leftLeg, s.leftLeg); applyT(m.rightLeg, s.rightLeg); applyT(m.head, s.head); applyT(m.leftArm, s.leftArm); applyT(m.rightArm, s.rightArm);
  if (gun) {
    const grip = smooth(clamp((v.prone - 0.9)/0.1, 0, 1)); const nr = nat.right, nl = nat.left;
    m.rightArm.xr = (-90 - 90*v.prone - s.body.rotX)*D; m.rightArm.yr = 0; m.rightArm.zr = 0;
    const left = -180 + wrap((nl.xr - nr.xr)/D);
    m.leftArm.xr += (left*D - m.leftArm.xr)*grip; m.leftArm.yr += ((nl.yr - nr.yr) - m.leftArm.yr)*grip; m.leftArm.zr += ((nl.zr - nr.zr) - m.leftArm.zr)*grip;
    for (const k of ['x','y','z']) { m.rightArm[k] += (nr[k] - m.rightArm[k])*grip; m.leftArm[k] += (nl[k] - m.leftArm[k])*grip; }
  }
  bendAtWaist(m, s.body);
  if (v.recovering && v.recovery > 0) {
    const target = {}; for (const n of NAMES) target[n] = store(m[n]);
    reset(m); stationary(m, v.recovering, nat, o.pitch || 0);
    for (const n of NAMES) blendPose(m[n], target[n], 1 - v.recovery);
  }
  if (v.turnProgress >= 0) {
    const settle = smooth((v.turnProgress - 0.65)/0.35);
    if (settle > 0) { const prev = {}; for (const n of NAMES) prev[n] = store(m[n]); reset(m);
      stationary(m, { sample: sampleAngle(v.aim), prone: 1, roll: v.roll, aim: v.aim, movement: 0, forward: 1, side: 0, stride: 0, transition: false, gun, recovering: null, recovery: 0, support: 0 }, nat, o.pitch || 0);
      const tgt = {}; for (const n of NAMES) tgt[n] = store(m[n]); for (const n of NAMES) { load(m[n], prev[n]); blendPose(m[n], tgt[n], settle); } }
  }
  if (v.recovering) { const h = smooth((0.45 - v.prone)/0.45); blendUpright(m.head, an.head, v.prone, h); blendUpright(m.rightArm, an.right, v.prone, h); blendUpright(m.leftArm, an.left, v.prone, h); }
  let root = rootMatrix(v); root = keepAboveGround(root, m, v);
  return { root, m, v };
}

// ---------------- vanilla crawl (no TAA): PlayerRenderer.setupRotations swim branch + HumanoidModel swim pose ----------------
function quadraticArmUpdate(x) { return -65*x + x*x; }
function vanillaCrawl(ty, limbSwing) {
  const m = freshModel(); const sw = 1;
  m.head.xr = -PI/4; // rotlerpRad(swimAmount, head.xRot, -pi/4) for isVisuallySwimming
  const f5 = limbSwing % 26; // right-handed, not attacking, not using item
  const rl = (f, a, b) => { let d = (b - a) % (2*PI); if (d < -PI) d += 2*PI; if (d >= PI) d -= 2*PI; return a + f*d; };
  if (f5 < 14) {
    m.leftArm.xr = rl(sw, m.leftArm.xr, 0); m.rightArm.xr = m.rightArm.xr + (0 - m.rightArm.xr)*sw;
    m.leftArm.yr = rl(sw, m.leftArm.yr, PI); m.rightArm.yr = m.rightArm.yr + (PI - m.rightArm.yr)*sw;
    m.leftArm.zr = rl(sw, m.leftArm.zr, PI + 1.8707964*quadraticArmUpdate(f5)/quadraticArmUpdate(14));
    m.rightArm.zr = m.rightArm.zr + ((PI - 1.8707964*quadraticArmUpdate(f5)/quadraticArmUpdate(14)) - m.rightArm.zr)*sw;
  } else if (f5 < 22) {
    const f6 = (f5 - 14)/8;
    m.leftArm.xr = rl(sw, m.leftArm.xr, PI/2*f6); m.rightArm.xr = m.rightArm.xr + (PI/2*f6 - m.rightArm.xr)*sw;
    m.leftArm.yr = rl(sw, m.leftArm.yr, PI); m.rightArm.yr = m.rightArm.yr + (PI - m.rightArm.yr)*sw;
    m.leftArm.zr = rl(sw, m.leftArm.zr, 5.012389 - 1.8707964*f6); m.rightArm.zr = m.rightArm.zr + ((1.2707963 + 1.8707964*f6) - m.rightArm.zr)*sw;
  } else {
    const f7 = (f5 - 22)/4;
    m.leftArm.xr = rl(sw, m.leftArm.xr, PI/2 - PI/2*f7); m.rightArm.xr = m.rightArm.xr + ((PI/2 - PI/2*f7) - m.rightArm.xr)*sw;
    m.leftArm.yr = rl(sw, m.leftArm.yr, PI); m.rightArm.yr = m.rightArm.yr + (PI - m.rightArm.yr)*sw;
    m.leftArm.zr = rl(sw, m.leftArm.zr, PI); m.rightArm.zr = m.rightArm.zr + (PI - m.rightArm.zr)*sw;
  }
  m.leftLeg.xr = 0.3*Math.cos(limbSwing*0.33333334 + PI); m.rightLeg.xr = 0.3*Math.cos(limbSwing*0.33333334);
  let root = RY(180*D); root = mul(root, RX(-90*D)); root = mul(root, T(0, ty, 0.3));
  root = mul(root, S(-1, -1, 1)); root = mul(root, S(0.9375, 0.9375, 0.9375)); root = mul(root, T(0, -1.501, 0));
  return { root, m };
}

// ---------------- reporting ----------------
const LABEL = { head: 'head', body: 'torso', rightArm: 'R.arm', leftArm: 'L.arm', rightLeg: 'R.leg', leftLeg: 'L.leg' };
function extentsLocal(r, infl) { // body-local: f = +Z, s = right = -X, y
  const out = {}; for (const n of NAMES) { const pts = worldPts(r.root, r.m, n, infl ? INFL[n] : 0);
    const f = pts.map(p => p[2]), s = pts.map(p => -p[0]), y = pts.map(p => p[1]);
    out[n] = { f: [Math.min(...f), Math.max(...f)], s: [Math.min(...s), Math.max(...s)], y: [Math.min(...y), Math.max(...y)], pts }; }
  return out;
}
const r3 = (x) => (x >= 0 ? ' ' : '') + x.toFixed(3);
function table(title, ex) { console.log(`\n${title}`); console.log('  part    f[min,max]          s[min,max]          y[min,max]');
  for (const n of ['head', 'body', 'rightArm', 'leftArm', 'rightLeg', 'leftLeg']) { const e = ex[n]; console.log(`  ${LABEL[n].padEnd(6)} [${r3(e.f[0])},${r3(e.f[1])}]  [${r3(e.s[0])},${r3(e.s[1])}]  [${r3(e.y[0])},${r3(e.y[1])}]`); } }
function union(list) { const o = {}; for (const n of NAMES) { o[n] = { f: [Infinity, -Infinity], s: [Infinity, -Infinity], y: [Infinity, -Infinity] };
  for (const e of list) for (const k of ['f','s','y']) { o[n][k][0] = Math.min(o[n][k][0], e[n][k][0]); o[n][k][1] = Math.max(o[n][k][1], e[n][k][1]); } } return o; }
function obb(e) { // principal axes from the 8 corners (cube edges): returns centre + 3 edge vectors (half) in body-local
  const p = e.pts.map(q => [q[2], -q[0], q[1]]); const c = [0,1,2].map(i => p.reduce((s, q) => s + q[i], 0)/8);
  const ax = [p[1].map((v, i) => (v - p[0][i])/2), p[2].map((v, i) => (v - p[0][i])/2), p[4].map((v, i) => (v - p[0][i])/2)];
  return { c, ax };
}

const mode = process.argv[2] || 'all';
if (mode === 'debug') {
  for (const a of [0, 45, 90, 180]) { const r = poseSteady({ gun: true, aim: a, pitch: 0 });
    const piv = (n) => { const p = apply(r.root, [r.m[n].x/16, r.m[n].y/16, r.m[n].z/16]); return `(${p[2].toFixed(3)},${(-p[0]).toFixed(3)},${p[1].toFixed(3)})`; };
    const e = extentsLocal(r, false); const ctr = (n) => { const q = e[n].pts; const c = [0,1,2].map(i => q.reduce((s, v) => s + v[i], 0)/8); return `(${c[2].toFixed(3)},${(-c[0]).toFixed(3)},${c[1].toFixed(3)})`; };
    console.log(`aim ${a} roll ${r.v.roll.toFixed(1)} | pivots f,s,y: head ${piv('head')} R.arm ${piv('rightArm')} L.arm ${piv('leftArm')} body ${piv('body')} | centres: R.arm ${ctr('rightArm')} torso ${ctr('body')} head ${ctr('head')} | R.arm euler ${[r.m.rightArm.xr, r.m.rightArm.yr, r.m.rightArm.zr].map(x => (x/D).toFixed(1)).join(',')} pos ${[r.m.rightArm.x, r.m.rightArm.y, r.m.rightArm.z].map(x => x.toFixed(2)).join(',')}`); }
}
if (mode === 'all' || mode === 'steady') {
  for (const gun of [true, false]) {
    const r = poseSteady({ gun, aim: 0, pitch: 0 });
    table(`== TAA steady prone, aim=0 pitch=0 ${gun ? 'TaCZ gun held' : 'empty hand'} (base cubes)`, extentsLocal(r, false));
    table(`   same, with outer layers (hat +0.5px, jacket/sleeve/pants +0.25px)`, extentsLocal(r, true));
    if (gun) { const e = extentsLocal(r, false); for (const n of ['head','rightArm','leftArm']) { const o = obb(e[n]); console.log(`   OBB ${LABEL[n]} centre(f,s,y)=(${o.c.map(x=>x.toFixed(3)).join(',')}) halfX=(${o.ax[0].map(x=>x.toFixed(3)).join(',')}) halfY=(${o.ax[1].map(x=>x.toFixed(3)).join(',')}) halfZ=(${o.ax[2].map(x=>x.toFixed(3)).join(',')})`); } }
  }
}
if (mode === 'all' || mode === 'pitch') {
  console.log('\n== pitch sweep (gun, aim=0): head / R.arm / L.arm  (f,y ranges); TAA clamps pitch to [-70,70]; TaCZ+tacz-tweaks mouse clamp with gun: [-25,+10]');
  for (const p of [-70, -45, -25, -10, 0, 10, 25, 45, 70]) { const e = extentsLocal(poseSteady({ gun: true, aim: 0, pitch: p }), false);
    console.log(`  pitch ${String(p).padStart(4)}: head f[${r3(e.head.f[0])},${r3(e.head.f[1])}] y[${r3(e.head.y[0])},${r3(e.head.y[1])}] | R.arm f[${r3(e.rightArm.f[0])},${r3(e.rightArm.f[1])}] y[${r3(e.rightArm.y[0])},${r3(e.rightArm.y[1])}] | L.arm f[${r3(e.leftArm.f[0])},${r3(e.leftArm.f[1])}] s[${r3(e.leftArm.s[0])},${r3(e.leftArm.s[1])}] y[${r3(e.leftArm.y[0])},${r3(e.leftArm.y[1])}] | torso y[${r3(e.body.y[0])},${r3(e.body.y[1])}]`); }
}
if (mode === 'all' || mode === 'aim') {
  console.log('\n== aim sweep (aim = wrapDegrees(yRot - anchor)), gun, pitch 0. roll = sampleAngle(aim).root.rotY');
  for (const a of [0, 20, 30, 45, 60, 90, 100, 120, 135, 150, 180, -45, -90, -135]) { const r = poseSteady({ gun: true, aim: a, pitch: 0 }); const e = extentsLocal(r, false);
    const all = NAMES.map(n => e[n]); const top = Math.max(...all.map(x => x.y[1]));
    console.log(`  aim ${String(a).padStart(4)} roll ${r.v.roll.toFixed(1).padStart(7)} top ${top.toFixed(3)} | head f[${r3(e.head.f[0])},${r3(e.head.f[1])}] s[${r3(e.head.s[0])},${r3(e.head.s[1])}] y[${r3(e.head.y[0])},${r3(e.head.y[1])}] | torso s[${r3(e.body.s[0])},${r3(e.body.s[1])}] y[${r3(e.body.y[0])},${r3(e.body.y[1])}] | R.arm f[${r3(e.rightArm.f[0])},${r3(e.rightArm.f[1])}] s[${r3(e.rightArm.s[0])},${r3(e.rightArm.s[1])}] y[${r3(e.rightArm.y[0])},${r3(e.rightArm.y[1])}] | legs y[${r3(Math.min(e.leftLeg.y[0], e.rightLeg.y[0]))},${r3(Math.max(e.leftLeg.y[1], e.rightLeg.y[1]))}] s[${r3(Math.min(e.leftLeg.s[0], e.rightLeg.s[0]))},${r3(Math.max(e.leftLeg.s[1], e.rightLeg.s[1]))}] f[${r3(Math.min(e.leftLeg.f[0], e.rightLeg.f[0]))},${r3(Math.max(e.leftLeg.f[1], e.rightLeg.f[1]))}]`); }
}
if (mode === 'all' || mode === 'crawl') {
  for (const gun of [true, false]) { const list = []; for (let k = 0; k < 36; k++) list.push(extentsLocal(poseSteady({ gun, aim: 0, pitch: 0, movement: 1, stride: k*10*D }), false));
    table(`== TAA prone CRAWLING envelope (movement weight 1, forward, stride 0..2pi) ${gun ? 'gun' : 'empty hand'}`, union(list)); }
}
if (mode === 'all' || mode === 'vanilla') {
  for (const [ty, lab] of [[-1, 'vanilla crawl (translate 0,-1,0.3)'], [-1.4, 'vanilla crawl + tacz-tweaks crawl.visualTweak=true (translate 0,-1.4,0.3)']]) {
    const still = extentsLocal(vanillaCrawl(ty, 0), false); table(`== ${lab}, limbSwing=0 (still)`, still);
    const list = []; for (let k = 0; k < 52; k++) list.push(extentsLocal(vanillaCrawl(ty, k*0.5), false)); table(`   envelope over swim-stroke cycle (limbSwing 0..26)`, union(list)); }
}
if (mode === 'all' || mode === 'trans') {
  for (const gun of [true, false]) for (const enter of [true, false]) {
    const N = enter ? 17 : 15; console.log(`\n== ${enter ? 'PRONE_ENTER (17 ticks)' : 'PRONE_EXIT (15 ticks)'} ${gun ? 'gun' : 'empty hand'}  [server pose SWIMMING throughout; EXIT clears at elapsed>=15]`);
    console.log('  tick prog  prone | top   | head f/y                        | torso f / y                    | legs f / y top             | arms f max');
    for (let t = 0; t <= N; t++) { const p = t/N; const r = poseTransition(enter, p, { gun, pitch: 0 }); const e = extentsLocal(r, false);
      const top = Math.max(...NAMES.map(n => e[n].y[1])); const legF = [Math.min(e.leftLeg.f[0], e.rightLeg.f[0]), Math.max(e.leftLeg.f[1], e.rightLeg.f[1])]; const legY = Math.max(e.leftLeg.y[1], e.rightLeg.y[1]);
      console.log(`  ${String(t).padStart(3)} ${p.toFixed(2)} ${r.v.prone.toFixed(2)} | ${top.toFixed(2)} | f[${r3(e.head.f[0])},${r3(e.head.f[1])}] y[${r3(e.head.y[0])},${r3(e.head.y[1])}] | f[${r3(e.body.f[0])},${r3(e.body.f[1])}] y[${r3(e.body.y[0])},${r3(e.body.y[1])}] | f[${r3(legF[0])},${r3(legF[1])}] ytop ${legY.toFixed(2)} | ${Math.max(e.rightArm.f[1], e.leftArm.f[1]).toFixed(2)}`); }
  }
}
if (mode === 'standing') {
  // reference standing (vanilla, gun hold) for lerp endpoint
  const m = freshModel(); const an = animatedPoses({ gun: true, pitch: 0 }); load(m.rightArm, an.right); load(m.leftArm, an.left);
  let root = RY(180*D); root = mul(root, S(-1, -1, 1)); root = mul(root, S(0.9375, 0.9375, 0.9375)); root = mul(root, T(0, -1.501, 0));
  table('== standing reference (vanilla, TaCZ rifle hold)', extentsLocal({ root, m }, false));
}

if (mode === 'export') {
  // OBBs in body-local (f, s, y): { c:[f,s,y], u:[..], v:[..], w:[..] } where u,v,w are HALF-extent edge vectors (cube local x,y,z).
  const PN = { head: 'head', body: 'torso', rightArm: 'rightArm', leftArm: 'leftArm', rightLeg: 'rightLeg', leftLeg: 'leftLeg' };
  const round = (a) => a.map(x => Math.round(x*1000)/1000);
  const pack = (r) => { const e = extentsLocal(r, false); const o = {}; for (const n of NAMES) { const b = obb(e[n]); o[PN[n]] = { c: round(b.c), u: round(b.ax[0]), v: round(b.ax[1]), w: round(b.ax[2]) }; } return o; };
  const out = { note: 'TAA 1.2.5 prone model as hit OBBs, body-local f=forward(anchor), s=right, y=up from feet; derived by k7_segments.js (unverified in-game)', steady: {}, crawlEnvelopeAABB: {}, enter: {}, exit: {} };
  for (const gun of [true, false]) {
    const g = gun ? 'gun' : 'empty'; out.steady[g] = {};
    for (let a = -180; a <= 180; a += 10) for (const p of [-70, -45, -25, -10, 0, 10, 25, 45, 70]) out.steady[g][`aim${a}_pitch${p}`] = pack(poseSteady({ gun, aim: a, pitch: p }));
    const list = []; for (let k = 0; k < 36; k++) list.push(extentsLocal(poseSteady({ gun, aim: 0, pitch: 0, movement: 1, stride: k*10*D }), false));
    const u = union(list); out.crawlEnvelopeAABB[g] = {}; for (const n of NAMES) out.crawlEnvelopeAABB[g][PN[n]] = { f: round(u[n].f), s: round(u[n].s), y: round(u[n].y) };
    out.enter[g] = []; out.exit[g] = [];
    for (let k = 0; k <= 16; k++) { const p = k/16; out.enter[g].push({ progress: p, prone: Math.round(poseTransition(true, p, { gun }).v.prone*1000)/1000, obb: pack(poseTransition(true, p, { gun })) }); out.exit[g].push({ progress: p, prone: Math.round(poseTransition(false, p, { gun }).v.prone*1000)/1000, obb: pack(poseTransition(false, p, { gun })) }); }
  }
  fs.writeFileSync(process.env.OUT_JSON, JSON.stringify(out));
  console.log('written', process.env.OUT_JSON, Object.keys(out.steady.gun).length, 'steady poses per variant');
}
if (mode === 'stages') {
  const st = (enter, N, a, b, gun) => { const list = []; for (let t = a; t <= b; t++) list.push(extentsLocal(poseTransition(enter, t/N, { gun }), false)); return union(list); };
  table('== ENTER kneel/dive stage union, ticks 3..11 (progress 0.18..0.65), gun', st(true, 17, 3, 11, true));
  table('== EXIT kneel stage union, ticks 6..11 (progress 0.40..0.73), gun', st(false, 15, 6, 11, true));
  // error of naive lerp(standing, prone, prone-blend) for head centre
  const hc = (r) => { const e = extentsLocal(r, false).head; return [(e.f[0]+e.f[1])/2, (e.y[0]+e.y[1])/2]; };
  const S0 = hc(poseTransition(true, 0, { gun: true })), P0 = hc(poseSteady({ gun: true }));
  let worst = 0, wt = -1; for (let t = 0; t <= 17; t++) { const r = poseTransition(true, t/17, { gun: true }); const h = hc(r); const w = r.v.prone; const l = [S0[0] + (P0[0]-S0[0])*w, S0[1] + (P0[1]-S0[1])*w]; const err = Math.hypot(h[0]-l[0], h[1]-l[1]); if (err > worst) { worst = err; wt = t; } }
  console.log(`naive lerp(standing,prone,w=prone) head-centre worst error during ENTER = ${worst.toFixed(2)} blocks at tick ${wt}`);
  worst = 0; for (let t = 0; t <= 15; t++) { const r = poseTransition(false, t/15, { gun: true }); const h = hc(r); const w = r.v.prone; const l = [S0[0] + (P0[0]-S0[0])*w, S0[1] + (P0[1]-S0[1])*w]; const err = Math.hypot(h[0]-l[0], h[1]-l[1]); if (err > worst) { worst = err; wt = t; } }
  console.log(`naive lerp head-centre worst error during EXIT = ${worst.toFixed(2)} blocks at tick ${wt}`);
  // error of keyframe table at 1/16 progress with linear interpolation, measured at per-tick samples (enter 17 / exit 15) on OBB centres
  for (const [enter, N] of [[true, 17], [false, 15]]) { let w2 = 0; for (let t = 0; t <= N; t++) { const p = t/N; const k = Math.min(15, Math.floor(p*16)), a = p*16 - k;
      const A = extentsLocal(poseTransition(enter, k/16, { gun: true }), false), B = extentsLocal(poseTransition(enter, (k+1)/16, { gun: true }), false), X = extentsLocal(poseTransition(enter, p, { gun: true }), false);
      for (const n of NAMES) { const c = (e) => [0,1,2].map(i => e[n].pts.reduce((s, q) => s + q[i], 0)/8); const ca = c(A), cb = c(B), cx = c(X); const ci = ca.map((v, i) => v + (cb[i]-v)*a); w2 = Math.max(w2, Math.hypot(...ci.map((v, i) => v - cx[i]))); } }
    console.log(`keyframe(1/16)+lerp worst OBB-centre error ${enter ? 'ENTER' : 'EXIT'} = ${w2.toFixed(3)} blocks`); }
}
