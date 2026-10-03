# Generates the WOK步战附属-部位血量 HUD figure from the original 256x256 art in
# tools/body_health_hud_icon/source: the rifle-holding block soldier keeps its original
# faces and outlines, the source rifle is removed and replaced by an anti-aliased M4A1,
# and everything is written at 4 texels per GUI pixel (156x192 for the 39x48 HUD box).
# The overlay draws these textures with linear filtering.
#
# Each part mask carries the face luminance, so the 3D face shading stays visible when
# the renderer tints a damaged part. The figure is a front view: the player's right arm
# and leg are on screen left.
param(
    [string] $SourceDirectory = (Join-Path $PSScriptRoot 'body_health_hud_icon\source'),
    [string] $OutputDirectory = (Join-Path $PSScriptRoot '..\wok_body_health\src\main\resources\assets\wok_body_health\textures\gui')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class BodyHudIcon {
    static readonly string[] Parts = { "head", "chest", "abdomen", "left_arm", "right_arm", "left_leg", "right_leg" };

    const int S = 256;                 // source size
    const int Scale = 4;               // texels per GUI pixel
    const int OutW = 39 * Scale, OutH = 48 * Scale;
    const int Margin = Scale;          // one GUI pixel of margin, as in the HUD layout
    const float DarkLimit = 0.28f;     // source face edges and the rifle are darker than this
    // Generous box around the source rifle; only dark, mask-free pixels inside it can be removed.
    static readonly Rectangle RifleZone = new Rectangle(88, 36, 146, 88);

    // M4A1 placement in output texels: pistol grip reference point and bore axis.
    const float GripX = 64f, BoreY = 54f, CmToTexel = 1.5f;

    static int[] Read(string path) {
        using (var b = new Bitmap(path)) {
            var data = b.LockBits(new Rectangle(0, 0, S, S), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            var px = new int[S * S];
            Marshal.Copy(data.Scan0, px, 0, px.Length);
            b.UnlockBits(data);
            return px;
        }
    }

    static void Write(string path, int[] argb) {
        using (var b = new Bitmap(OutW, OutH, PixelFormat.Format32bppArgb)) {
            var data = b.LockBits(new Rectangle(0, 0, OutW, OutH), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
            Marshal.Copy(argb, 0, data.Scan0, argb.Length);
            b.UnlockBits(data);
            b.Save(path, ImageFormat.Png);
        }
    }

    static float A(int c) { return ((c >> 24) & 255) / 255f; }
    static float Lum(int c) { return (0.2126f * ((c >> 16) & 255) + 0.7152f * ((c >> 8) & 255) + 0.0722f * (c & 255)) / 255f; }

    public static string Build(string sourceDir, string outputDir) {
        int[] src = Read(Path.Combine(sourceDir, "body_hud.png"));
        var mask = new int[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) mask[p] = Read(Path.Combine(sourceDir, "body_hud_" + Parts[p] + ".png"));

        // Crop box of the original figure; the 39x48 HUD layout was built on this mapping.
        int x0 = S, y0 = S, x1 = -1, y1 = -1;
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) if (A(src[y * S + x]) > 0.5f) {
            x0 = Math.Min(x0, x); y0 = Math.Min(y0, y); x1 = Math.Max(x1, x); y1 = Math.Max(y1, y);
        }

        // 1. Remove the source rifle. Face edges are thin lines next to a face; rifle pixels sit
        //    at least three pixels from any face. Grow that core through adjacent dark pixels.
        Func<int, bool> inMask = i => { for (int p = 0; p < Parts.Length; p++) if (A(mask[p][i]) > 0.5f) return true; return false; };
        Func<int, bool> face = i => A(src[i]) > 0.5f && (Lum(src[i]) >= DarkLimit || inMask(i));
        Func<int, bool> darkFree = i => A(src[i]) > 0.5f && Lum(src[i]) < DarkLimit && !inMask(i);
        var rifle = new bool[S * S];
        for (int y = RifleZone.Top; y < RifleZone.Bottom; y++) for (int x = RifleZone.Left; x < RifleZone.Right; x++) {
            int i = y * S + x;
            if (!darkFree(i)) continue;
            bool nearFace = false;
            for (int dy = -2; dy <= 2 && !nearFace; dy++) for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < S && ny < S && face(ny * S + nx)) { nearFace = true; break; }
            }
            rifle[i] = !nearFace;
        }
        for (int pass = 0; pass < 3; pass++) {
            var grown = (bool[]) rifle.Clone();
            for (int y = RifleZone.Top; y < RifleZone.Bottom; y++) for (int x = RifleZone.Left; x < RifleZone.Right; x++) {
                int i = y * S + x;
                if (rifle[i] || !darkFree(i)) continue;
                if ((x > 0 && rifle[i - 1]) || (x < S - 1 && rifle[i + 1]) || (y > 0 && rifle[i - S]) || (y < S - 1 && rifle[i + S])) grown[i] = true;
            }
            rifle = grown;
        }

        // The rifle also has grey highlights and anti-aliased fringes. Inside the zone, any
        // remaining unmasked component that touches no part mask belonged to it; the body's
        // outlines and the torso side face always connect to a part.
        var visited = new bool[S * S];
        Func<int, bool> loose = i => A(src[i]) > 0.02f && !rifle[i] && !inMask(i);
        for (int y = RifleZone.Top; y < RifleZone.Bottom; y++) for (int x = RifleZone.Left; x < RifleZone.Right; x++) {
            int start = y * S + x;
            if (visited[start] || !loose(start)) continue;
            var members = new List<int>();
            var queue = new Queue<int>();
            queue.Enqueue(start);
            visited[start] = true;
            bool touchesPart = false;
            while (queue.Count > 0) {
                int i = queue.Dequeue();
                members.Add(i);
                int cx = i % S, cy = i / S;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx < 0 || ny < 0 || nx >= S || ny >= S) continue;
                    int q = ny * S + nx;
                    if (inMask(q)) { touchesPart = true; continue; }
                    if (visited[q] || !loose(q) || !RifleZone.Contains(nx, ny)) continue;
                    visited[q] = true;
                    queue.Enqueue(q);
                }
            }
            if (!touchesPart) foreach (int i in members) rifle[i] = true;
        }

        // 2. Fill the holes left inside the body from their neighbours (colour and part masks).
        var body = (int[]) src.Clone();
        var bodyMask = new int[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) bodyMask[p] = (int[]) mask[p].Clone();
        var hole = new bool[S * S];
        for (int i = 0; i < S * S; i++) if (rifle[i]) { hole[i] = true; body[i] = 0; for (int p = 0; p < Parts.Length; p++) bodyMask[p][i] = 0; }
        for (int pass = 0; pass < 48; pass++) {
            var filled = new List<int>();
            var colours = new List<int>();
            var masksOut = new List<int[]>();
            for (int y = 1; y < S - 1; y++) for (int x = 1; x < S - 1; x++) {
                int i = y * S + x;
                if (!hole[i]) continue;
                int[] around = { i - 1, i + 1, i - S, i + S };
                int n = 0; float r = 0, g = 0, b = 0;
                var votes = new int[Parts.Length];
                foreach (int q in around) {
                    if (hole[q] || A(body[q]) < 0.5f) continue;
                    n++;
                    r += (body[q] >> 16) & 255; g += (body[q] >> 8) & 255; b += body[q] & 255;
                    for (int p = 0; p < Parts.Length; p++) if (A(bodyMask[p][q]) > 0.5f) votes[p]++;
                }
                if (n < 2) continue;
                filled.Add(i);
                colours.Add(unchecked((int) 0xFF000000) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n));
                var m = new int[Parts.Length];
                for (int p = 0; p < Parts.Length; p++) m[p] = votes[p] * 2 > n ? unchecked((int) 0xFFFFFFFF) : 0;
                masksOut.Add(m);
            }
            if (filled.Count == 0) break;
            for (int k = 0; k < filled.Count; k++) {
                int i = filled[k];
                hole[i] = false;
                body[i] = colours[k];
                for (int p = 0; p < Parts.Length; p++) bodyMask[p][i] = masksOut[k][p];
            }
        }

        // 3. Area-average (premultiplied) downscale into the 4x canvas with the HUD's mapping.
        int innerH = OutH - 2 * Margin;
        double scale = innerH / (double) (y1 - y0 + 1);
        Func<int[], float[]> down = pixels => {
            var outPx = new float[OutW * OutH * 4];
            for (int ty = 0; ty < innerH; ty++) for (int tx = 0; tx < OutW - 2 * Margin; tx++) {
                double sx0 = x0 + tx / scale, sx1 = x0 + (tx + 1) / scale;
                double sy0 = y0 + ty / scale, sy1 = y0 + (ty + 1) / scale;
                double area = 0, a = 0, r = 0, g = 0, b = 0;
                for (int sy = (int) Math.Floor(sy0); sy < (int) Math.Ceiling(sy1); sy++) {
                    if (sy < 0 || sy >= S) continue;
                    double wy = Math.Min(sy + 1, sy1) - Math.Max(sy, sy0);
                    for (int sx = (int) Math.Floor(sx0); sx < (int) Math.Ceiling(sx1); sx++) {
                        if (sx < 0 || sx >= S) continue;
                        double w = (Math.Min(sx + 1, sx1) - Math.Max(sx, sx0)) * wy;
                        int c = pixels[sy * S + sx];
                        double ca = A(c) * w;
                        area += w; a += ca;
                        r += ca * ((c >> 16) & 255); g += ca * ((c >> 8) & 255); b += ca * (c & 255);
                    }
                }
                int o = ((ty + Margin) * OutW + tx + Margin) * 4;
                outPx[o] = (float) (a / area);
                if (a > 0) { outPx[o + 1] = (float) (r / a); outPx[o + 2] = (float) (g / a); outPx[o + 3] = (float) (b / a); }
            }
            return outPx;
        };
        float[] baseF = down(body);
        var maskF = new float[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) maskF[p] = down(bodyMask[p]);

        // 4. The M4A1 on its own layer, then composited over the body and cut out of the masks.
        var gun = new Bitmap(OutW, OutH, PixelFormat.Format32bppArgb);
        using (var gfx = Graphics.FromImage(gun)) {
            gfx.SmoothingMode = SmoothingMode.AntiAlias;
            gfx.PixelOffsetMode = PixelOffsetMode.HighQuality;
            DrawM4A1(gfx);
        }
        var gunData = gun.LockBits(new Rectangle(0, 0, OutW, OutH), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var gunPx = new int[OutW * OutH];
        Marshal.Copy(gunData.Scan0, gunPx, 0, gunPx.Length);
        gun.UnlockBits(gunData);
        gun.Dispose();

        var baseOut = new int[OutW * OutH];
        var maskOut = new int[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) maskOut[p] = new int[OutW * OutH];
        for (int i = 0; i < OutW * OutH; i++) {
            float ba = baseF[i * 4], br = baseF[i * 4 + 1], bg = baseF[i * 4 + 2], bb = baseF[i * 4 + 3];
            int gc = gunPx[i];
            float ga = A(gc);
            float oa = ga + ba * (1 - ga);
            float or = 0, og = 0, ob = 0;
            if (oa > 0) {
                or = (((gc >> 16) & 255) * ga + br * ba * (1 - ga)) / oa;
                og = (((gc >> 8) & 255) * ga + bg * ba * (1 - ga)) / oa;
                ob = ((gc & 255) * ga + bb * ba * (1 - ga)) / oa;
            }
            // Drop near-invisible residue of the source rifle's soft edges.
            baseOut[i] = oa < 0.03f ? 0 : Pack(oa, or, og, ob);
            float faceLum = (0.2126f * br + 0.7152f * bg + 0.0722f * bb) / 255f;
            int shade = (int) Math.Round(255 * Math.Max(0.45f, Math.Min(1f, faceLum / 0.56f)));
            for (int p = 0; p < Parts.Length; p++) {
                float ma = maskF[p][i * 4] * (1 - ga);
                maskOut[p][i] = ma <= 0.004f ? 0 : Pack(ma, shade, shade, shade);
            }
        }

        Directory.CreateDirectory(outputDir);
        Write(Path.Combine(outputDir, "body_hud.png"), baseOut);
        var report = new StringBuilder();
        report.AppendLine("size=" + OutW + "x" + OutH + " (" + Scale + "x of 39x48)");
        for (int p = 0; p < Parts.Length; p++) {
            Write(Path.Combine(outputDir, "body_hud_" + Parts[p] + ".png"), maskOut[p]);
            long count = 0;
            for (int i = 0; i < OutW * OutH; i++) if (((maskOut[p][i] >> 24) & 255) > 127) count++;
            report.AppendLine(Parts[p] + " texels=" + count);
        }
        return report.ToString();
    }

    static int Pack(float a, float r, float g, float b) {
        int ia = (int) Math.Round(Math.Max(0, Math.Min(1, a)) * 255);
        return (ia << 24) | ((int) Math.Round(Clamp(r)) << 16) | ((int) Math.Round(Clamp(g)) << 8) | (int) Math.Round(Clamp(b));
    }
    static float Clamp(float v) { return Math.Max(0, Math.Min(255, v)); }

    // Side view, muzzle to the right. Geometry in centimetres: x from the butt (collapsed stock),
    // y up from the bore axis. Proportions follow the M4A1 (75.6 cm collapsed, 36.8 cm barrel).
    static PointF P(float x, float y) { return new PointF(GripX + (x - 28.5f) * CmToTexel, BoreY - y * CmToTexel); }
    static PointF[] Poly(params float[] xy) {
        var pts = new PointF[xy.Length / 2];
        for (int k = 0; k < pts.Length; k++) pts[k] = P(xy[2 * k], xy[2 * k + 1]);
        return pts;
    }
    static RectangleF Rect(float xa, float ya, float xb, float yb) {
        PointF a = P(xa, ya), b = P(xb, yb);
        return new RectangleF(Math.Min(a.X, b.X), Math.Min(a.Y, b.Y), Math.Abs(b.X - a.X), Math.Abs(b.Y - a.Y));
    }

    static void DrawM4A1(Graphics g) {
        var outline = new Pen(Color.FromArgb(255, 10, 12, 14), 1.0f) { LineJoin = LineJoin.Round };
        var body = new SolidBrush(Color.FromArgb(255, 34, 39, 42));
        var face = new SolidBrush(Color.FromArgb(255, 46, 52, 56));
        var dark = new SolidBrush(Color.FromArgb(255, 18, 21, 23));
        var hole = new SolidBrush(Color.FromArgb(255, 8, 10, 11));
        var hi = new Pen(Color.FromArgb(255, 92, 104, 110), 1.0f);
        var hiSoft = new Pen(Color.FromArgb(200, 70, 80, 86), 1.0f);

        Action<Brush, PointF[]> shape = (brush, pts) => { g.FillPolygon(brush, pts); g.DrawPolygon(outline, pts); };

        // Collapsible stock on the buffer tube.
        shape(body, Poly(0f, 3.0f, 13f, 2.3f, 14.6f, 1.1f, 14.6f, -2.3f, 9.2f, -3.2f, 4.6f, -5.0f, 1.6f, -8.6f, 0f, -8.6f));
        shape(dark, Poly(0f, 3.0f, 1.4f, 3.0f, 1.4f, -8.6f, 0f, -8.6f));                       // butt pad
        g.FillPolygon(face, Poly(2.2f, 1.6f, 12.6f, 1.2f, 12.6f, -1.6f, 8.4f, -2.2f, 4.4f, -3.6f, 2.2f, -5.4f));
        g.DrawLine(hi, P(1.6f, 2.6f), P(13f, 2.0f));
        shape(body, Poly(13.5f, 1.6f, 22.2f, 1.6f, 22.2f, -1.6f, 13.5f, -1.6f));                // buffer tube
        g.DrawLine(hiSoft, P(14f, 1.1f), P(21.8f, 1.1f));
        shape(dark, Poly(21.2f, 2.2f, 22.6f, 2.2f, 22.6f, -2.0f, 21.2f, -2.0f));               // castle nut

        // Lower receiver with flared magazine well, pistol grip and trigger guard.
        shape(body, Poly(22.2f, -0.4f, 41.2f, -0.4f, 41.2f, -4.1f, 41.0f, -6.6f, 35.0f, -6.6f, 34.6f, -4.3f, 30.8f, -4.6f, 25.4f, -4.6f, 22.6f, -3.0f));
        shape(body, Poly(25.6f, -3.6f, 30.6f, -3.6f, 30.1f, -5.6f, 28.7f, -13.6f, 27.9f, -14.7f, 24.6f, -14.7f, 24.1f, -13.4f, 25.0f, -6.6f));
        g.DrawLine(hiSoft, P(25.9f, -5.0f), P(25.2f, -13.0f));
        var guard = new GraphicsPath();
        guard.AddLines(new[] { P(34.4f, -4.4f), P(34.4f, -7.0f), P(29.6f, -7.0f) });
        g.DrawPath(new Pen(Color.FromArgb(255, 10, 12, 14), 1.4f), guard);
        g.DrawLine(new Pen(Color.FromArgb(255, 60, 68, 72), 1.0f), P(32.2f, -4.6f), P(31.6f, -6.2f)); // trigger

        // Curved 30-round STANAG magazine.
        var mag = new GraphicsPath();
        mag.AddLine(P(35.3f, -6.4f), P(40.7f, -6.4f));
        mag.AddBezier(P(40.7f, -6.4f), P(41.4f, -12f), P(42.2f, -18f), P(43.4f, -23.4f));
        mag.AddLine(P(43.4f, -23.4f), P(43.6f, -24.8f));
        mag.AddLine(P(43.6f, -24.8f), P(36.6f, -25.0f));
        mag.AddLine(P(36.6f, -25.0f), P(36.9f, -23.8f));
        mag.AddBezier(P(36.9f, -23.8f), P(36.0f, -18f), P(35.5f, -12f), P(35.3f, -6.4f));
        mag.CloseFigure();
        g.FillPath(body, mag);
        g.DrawPath(outline, mag);
        g.DrawLine(hiSoft, P(36.4f, -8.5f), P(37.4f, -22.5f));
        g.DrawLine(new Pen(Color.FromArgb(255, 12, 14, 16), 1.0f), P(36.7f, -23.6f), P(43.3f, -23.3f)); // floor plate seam

        // Upper receiver: ejection port, forward assist, flat-top rail.
        shape(face, Poly(22.0f, 3.4f, 41.2f, 3.4f, 41.2f, -0.6f, 22.4f, -0.6f, 21.8f, 1.2f));
        g.FillRectangle(hole, Rect(31.0f, 1.9f, 37.0f, 0.1f));                                  // ejection port
        g.DrawLine(hi, P(31.0f, 2.1f), P(37.0f, 2.1f));
        g.FillEllipse(body, Rect(26.6f, 1.9f, 28.8f, -0.3f));                                    // forward assist
        g.DrawEllipse(outline, Rect(26.6f, 1.9f, 28.8f, -0.3f));
        shape(dark, Poly(21.6f, 3.4f, 41.2f, 3.4f, 41.2f, 4.5f, 21.6f, 4.5f));                  // rail
        for (float x = 22.4f; x < 41f; x += 1.15f) g.FillRectangle(hole, Rect(x, 4.55f, x + 0.5f, 3.95f));
        shape(dark, Poly(20.6f, 3.6f, 22.0f, 3.6f, 22.0f, 2.6f, 20.6f, 2.6f));                  // charging handle

        // Detachable carry handle with the rear sight.
        var handle = new GraphicsPath(FillMode.Alternate);
        handle.AddPolygon(Poly(23.0f, 4.5f, 23.0f, 9.6f, 24.2f, 10.8f, 36.4f, 10.8f, 37.6f, 9.2f, 37.6f, 4.5f));
        handle.AddPolygon(Poly(28.6f, 5.1f, 36.2f, 5.1f, 36.2f, 8.6f, 28.6f, 8.6f));
        g.FillPath(body, handle);
        g.DrawPath(outline, handle);
        g.DrawLine(hi, P(24.4f, 10.4f), P(36.2f, 10.4f));
        g.FillEllipse(face, Rect(24.2f, 8.4f, 27.2f, 5.6f));                                     // windage drum
        g.DrawEllipse(outline, Rect(24.2f, 8.4f, 27.2f, 5.6f));
        g.FillRectangle(hole, Rect(25.0f, 10.8f, 26.4f, 9.8f));                                  // aperture

        // Delta ring and ribbed oval handguard with vent holes.
        shape(dark, Poly(41.0f, 3.3f, 43.4f, 3.3f, 43.4f, -3.3f, 41.0f, -3.3f));
        var guardPath = new GraphicsPath();
        guardPath.AddArc(Rect(43.0f, 3.0f, 45.4f, -3.0f), 90, 180);
        guardPath.AddLine(P(44.2f, 3.0f), P(58.2f, 3.0f));
        guardPath.AddArc(Rect(57.0f, 3.0f, 59.4f, -3.0f), 270, 180);
        guardPath.AddLine(P(58.2f, -3.0f), P(44.2f, -3.0f));
        guardPath.CloseFigure();
        g.FillPath(face, guardPath);
        g.DrawPath(outline, guardPath);
        g.DrawLine(hi, P(44.4f, 2.4f), P(58.0f, 2.4f));
        g.DrawLine(new Pen(Color.FromArgb(255, 22, 26, 28), 1.0f), P(44.2f, 0f), P(58.2f, 0f));    // handguard seam
        for (float x = 45.2f; x < 57.6f; x += 2.15f) {
            g.FillEllipse(hole, Rect(x, 1.9f, x + 1.0f, 0.8f));
            g.FillEllipse(hole, Rect(x + 1.0f, -0.8f, x + 2.0f, -1.9f));
        }

        // Front sight base: triangular tower with the post, bayonet lug below.
        shape(body, Poly(59.0f, -1.6f, 62.6f, -1.6f, 62.6f, 2.2f, 59.0f, 2.2f));
        shape(body, Poly(59.2f, 2.2f, 60.3f, 8.0f, 61.5f, 8.0f, 62.5f, 2.2f));
        shape(dark, Poly(60.6f, 8.0f, 61.2f, 8.0f, 61.2f, 9.4f, 60.6f, 9.4f));                  // post
        shape(body, Poly(60.0f, -1.6f, 61.8f, -1.6f, 61.8f, -3.4f, 60.0f, -3.4f));              // bayonet lug
        g.DrawLine(hi, P(60.4f, 7.6f), P(59.5f, 2.6f));

        // Barrel with the M203 step and the A2 birdcage flash hider.
        shape(body, Poly(62.6f, 0.75f, 70.6f, 0.75f, 70.6f, -0.75f, 62.6f, -0.75f));
        g.DrawLine(new Pen(Color.FromArgb(255, 10, 12, 14), 1.0f), P(64.0f, 0.75f), P(64.0f, -0.75f));
        shape(body, Poly(70.4f, 1.15f, 75.6f, 1.15f, 75.6f, -1.15f, 70.4f, -1.15f));
        g.FillRectangle(hole, Rect(71.6f, 1.15f, 72.2f, 0.1f));
        g.FillRectangle(hole, Rect(73.2f, 1.15f, 73.8f, 0.1f));
        g.DrawLine(hiSoft, P(62.8f, 0.45f), P(70.4f, 0.45f));
    }
}
'@

$report = [BodyHudIcon]::Build((Resolve-Path $SourceDirectory).Path, [IO.Path]::GetFullPath($OutputDirectory))
Write-Output "Wrote body_hud.png and 7 part masks to $OutputDirectory"
Write-Output $report
