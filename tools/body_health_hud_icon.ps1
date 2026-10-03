# Generates the WOK步战附属-部位血量 HUD figure: the rifle-holding block soldier from the
# original 256x256 art in tools/body_health_hud_icon/source, redrawn as native pixels
# (one texel per GUI pixel) with a dark silhouette outline, one-pixel separators between
# body parts and a grey-green shade ramp. Each part mask carries luminance, so gear shading
# stays visible when the renderer tints a damaged part.
#
# The figure is a front view: the player's right arm and leg are on screen left.
param(
    [string] $SourceDirectory = (Join-Path $PSScriptRoot 'body_health_hud_icon\source'),
    [string] $OutputDirectory = (Join-Path $PSScriptRoot '..\wok_body_health\src\main\resources\assets\wok_body_health\textures\gui'),
    [int] $Height = 48
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class BodyHudIcon {
    static readonly string[] Parts = { "head", "chest", "abdomen", "left_arm", "right_arm", "left_leg", "right_leg" };

    // Top-left of the rifle sprite in the 39x48 figure: the pistol grip sits in the rear hand
    // and the handguard rests on the front hand.
    const int GunX = 6, GunY = 9;

    // M4A1 side view facing right, as filled spans {row, firstColumn, lastColumn}:
    // collapsible stock on the buffer tube, flat-top receiver with a rear sight, pistol grip,
    // curved 30-round magazine, vented handguard, triangular front sight base, barrel, flash hider.
    static readonly int[][] M4A1Spans = {
        new[] { 0, 8, 9 }, new[] { 0, 22, 22 },
        new[] { 1, 8, 9 }, new[] { 1, 22, 22 },
        new[] { 2, 0, 4 }, new[] { 2, 7, 23 },
        new[] { 3, 0, 23 }, new[] { 3, 28, 30 },
        new[] { 4, 0, 30 },
        new[] { 5, 0, 20 }, new[] { 5, 28, 30 },
        new[] { 6, 0, 2 }, new[] { 6, 7, 14 },
        new[] { 7, 8, 14 },
        new[] { 8, 7, 9 }, new[] { 8, 12, 14 },
        new[] { 9, 7, 9 }, new[] { 9, 13, 15 },
        new[] { 10, 7, 8 }, new[] { 10, 13, 15 },
    };

    // 'k' outline, 'd' anodised body, 'm' lit face, 'h' highlight.
    static char[,] M4A1() {
        const int H = 11, W = 31;
        var shape = new bool[H, W];
        foreach (var s in M4A1Spans) for (int x = s[1]; x <= s[2]; x++) shape[s[0], x] = true;
        var g = new char[H, W];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (!shape[y, x]) { g[y, x] = '.'; continue; }
            bool edge = y == 0 || x == 0 || y == H - 1 || x == W - 1
                || !shape[y - 1, x] || !shape[y + 1, x] || !shape[y, x - 1] || !shape[y, x + 1];
            g[y, x] = edge ? 'k' : 'd';
        }
        for (int x = 1; x <= 4; x++) g[3, x] = 'm';               // stock upper face
        for (int x = 8; x <= 13; x++) g[3, x] = 'm';              // upper receiver
        g[4, 11] = 'h'; g[4, 12] = 'h';                           // ejection port
        for (int x = 8; x <= 14; x += 2) g[2, x] = 'm';           // Picatinny rail teeth
        for (int x = 16; x <= 19; x++) {                          // handguard vents
            g[3, x] = (x % 2 == 0) ? 'h' : 'd';
            g[4, x] = (x % 2 == 0) ? 'd' : 'h';
        }
        g[8, 13] = 'm';                                           // magazine rib
        return g;
    }

    static int GunColour(char c) {
        switch (c) {
            case 'k': return unchecked((int) 0xFF141819);
            case 'd': return unchecked((int) 0xFF262C2F);
            case 'm': return unchecked((int) 0xFF3A4246);
            default: return unchecked((int) 0xFF5F6B71);
        }
    }

    static float[] ReadAlpha(Bitmap bitmap, out float[] luminance) {
        int w = bitmap.Width, h = bitmap.Height;
        var data = bitmap.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var bytes = new byte[w * h * 4];
        Marshal.Copy(data.Scan0, bytes, 0, bytes.Length);
        bitmap.UnlockBits(data);
        var alpha = new float[w * h];
        luminance = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            float b = bytes[i * 4], g = bytes[i * 4 + 1], r = bytes[i * 4 + 2];
            alpha[i] = bytes[i * 4 + 3] / 255f;
            luminance[i] = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
        }
        return alpha;
    }

    static void Write(string path, int w, int h, int[] argb) {
        using (var bitmap = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
            var data = bitmap.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
            Marshal.Copy(argb, 0, data.Scan0, argb.Length);
            bitmap.UnlockBits(data);
            bitmap.Save(path, ImageFormat.Png);
        }
    }

    // Returns a report with the texture size and each part's anchor (centroid) in texture pixels.
    public static string Build(string sourceDir, string outputDir, int outH) {
        const int S = 256;
        float[] lum;
        float[] alpha;
        using (var b = new Bitmap(Path.Combine(sourceDir, "body_hud.png"))) alpha = ReadAlpha(b, out lum);
        var masks = new float[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) {
            float[] unused;
            using (var b = new Bitmap(Path.Combine(sourceDir, "body_hud_" + Parts[p] + ".png"))) masks[p] = ReadAlpha(b, out unused);
        }

        int x0 = S, y0 = S, x1 = -1, y1 = -1;
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) if (alpha[y * S + x] > 0.5f) {
            x0 = Math.Min(x0, x); y0 = Math.Min(y0, y); x1 = Math.Max(x1, x); y1 = Math.Max(y1, y);
        }
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        int innerH = outH - 2;
        double scale = innerH / (double) bh;
        int innerW = (int) Math.Round(bw * scale);
        int outW = innerW + 2;
        int n = outW * outH;

        // The source faces use three grey tones (about 0.35 / 0.45 / 0.55 luminance); its face
        // edges and the rifle are below DarkLimit. Shades come from face pixels only, so the
        // thick source edge lines do not muddy the downscaled faces.
        const float DarkLimit = 0.28f;
        var coverage = new float[n];
        var shadeLum = new float[n];
        var darkFraction = new float[n];
        var partCover = new float[Parts.Length, n];
        for (int ty = 0; ty < innerH; ty++) for (int tx = 0; tx < innerW; tx++) {
            double sx0 = x0 + tx / scale, sx1 = x0 + (tx + 1) / scale;
            double sy0 = y0 + ty / scale, sy1 = y0 + (ty + 1) / scale;
            double area = 0, a = 0, l = 0, lightA = 0, lightL = 0;
            var pc = new double[Parts.Length];
            for (int sy = (int) Math.Floor(sy0); sy < (int) Math.Ceiling(sy1); sy++) {
                if (sy < 0 || sy >= S) continue;
                double wy = Math.Min(sy + 1, sy1) - Math.Max(sy, sy0);
                for (int sx = (int) Math.Floor(sx0); sx < (int) Math.Ceiling(sx1); sx++) {
                    if (sx < 0 || sx >= S) continue;
                    double wx = Math.Min(sx + 1, sx1) - Math.Max(sx, sx0);
                    double wgt = wx * wy;
                    int i = sy * S + sx;
                    area += wgt;
                    a += wgt * alpha[i];
                    l += wgt * alpha[i] * lum[i];
                    if (lum[i] > DarkLimit) {
                        lightA += wgt * alpha[i];
                        lightL += wgt * alpha[i] * lum[i];
                    }
                    for (int p = 0; p < Parts.Length; p++) pc[p] += wgt * masks[p][i];
                }
            }
            int o = (ty + 1) * outW + (tx + 1);
            coverage[o] = (float) (a / area);
            shadeLum[o] = lightA > 0 ? (float) (lightL / lightA) : (a > 0 ? (float) (l / a) : 0f);
            darkFraction[o] = a > 0 ? (float) ((a - lightA) / a) : 0f;
            for (int p = 0; p < Parts.Length; p++) partCover[p, o] = (float) (pc[p] / area);
        }

        // Labels: -1 empty, -2 untinted body face (the torso's side face), -3 rifle, >= 0 part index.
        const int Empty = -1, Neutral = -2, Gun = -3;
        var part = new int[n];
        var wasRifle = new bool[n];
        for (int i = 0; i < n; i++) {
            part[i] = Empty;
            if (coverage[i] < 0.5f) continue;
            int best = -1; float bestCover = 0;
            for (int p = 0; p < Parts.Length; p++) if (partCover[p, i] > bestCover) { bestCover = partCover[p, i]; best = p; }
            bool unmasked = best < 0 || bestCover < 0.3f || (darkFraction[i] > 0.7f && bestCover < 0.6f);
            if (!unmasked) { part[i] = best; continue; }
            // Unmasked pixels are either the source rifle (near black) or body faces the source
            // masks leave untinted. The source rifle is dropped here and redrawn as an M4A1 below.
            if (darkFraction[i] > 0.55f) wasRifle[i] = true; else part[i] = Neutral;
        }

        // Fill the holes the dropped rifle leaves inside the body from their neighbours. A hole
        // needs two body neighbours, so rifle pixels beyond the silhouette stay empty.
        for (int pass = 0; pass < 6; pass++) {
            var nextPart = (int[]) part.Clone();
            var nextLum = (float[]) shadeLum.Clone();
            for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
                int i = y * outW + x;
                if (!wasRifle[i] || part[i] != Empty) continue;
                var counts = new Dictionary<int, int>();
                var sums = new Dictionary<int, float>();
                int[] around = { x > 0 ? i - 1 : -1, x + 1 < outW ? i + 1 : -1, y > 0 ? i - outW : -1, y + 1 < outH ? i + outW : -1 };
                foreach (int q in around) {
                    if (q < 0 || (part[q] < 0 && part[q] != Neutral)) continue;
                    counts[part[q]] = (counts.ContainsKey(part[q]) ? counts[part[q]] : 0) + 1;
                    sums[part[q]] = (sums.ContainsKey(part[q]) ? sums[part[q]] : 0f) + shadeLum[q];
                }
                int label = Empty, labelCount = 0;
                foreach (var entry in counts) if (entry.Value > labelCount) { label = entry.Key; labelCount = entry.Value; }
                if (labelCount >= 2) { nextPart[i] = label; nextLum[i] = sums[label] / labelCount; }
            }
            part = nextPart;
            shadeLum = nextLum;
        }

        // Hand-drawn M4A1 on top: grip at the rear hand, handguard resting on the front hand.
        char[,] rifle = M4A1();
        var gunArgb = new int[n];
        for (int sy = 0; sy < rifle.GetLength(0); sy++) for (int sx = 0; sx < rifle.GetLength(1); sx++) {
            char c = rifle[sy, sx];
            int x = GunX + sx, y = GunY + sy;
            if (c == '.' || x < 0 || y < 0 || x >= outW || y >= outH) continue;
            int i = y * outW + x;
            part[i] = Gun;
            gunArgb[i] = GunColour(c);
        }

        // The source faces carry a noise texture; a 3x3 median within the same label keeps the
        // face tones and drops single-pixel speckles before quantising.
        var smoothLum = new float[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            smoothLum[i] = shadeLum[i];
            if (part[i] < 0 && part[i] != Neutral) continue;
            var window = new List<float>(9);
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= outW || ny >= outH) continue;
                int q = ny * outW + nx;
                if (part[q] == part[i]) window.Add(shadeLum[q]);
            }
            window.Sort();
            smoothLum[i] = window[window.Count / 2];
        }
        shadeLum = smoothLum;

        // One-pixel separators: a body pixel whose right or lower neighbour is a different body
        // label. The rifle draws its own outline, so it never adds separators.
        var separator = new bool[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            if (part[i] < 0 && part[i] != Neutral) continue;
            int[] neighbours = { x + 1 < outW ? i + 1 : -1, y + 1 < outH ? i + outW : -1 };
            foreach (int q in neighbours) {
                if (q >= 0 && (part[q] >= 0 || part[q] == Neutral) && part[q] != part[i]) separator[i] = true;
            }
        }

        const int Outline = unchecked((int) 0xFF15191B);
        int[] bodyRamp = { unchecked((int) 0xFF4F5857), unchecked((int) 0xFF6F7977), unchecked((int) 0xFF939D9B), unchecked((int) 0xFFB4BDBB) };
        int[] maskRamp = { 122, 172, 224, 255 };

        var baseArgb = new int[n];
        var maskArgb = new int[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) maskArgb[p] = new int[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            if (part[i] == Empty) {
                bool touches = false;
                for (int dy = -1; dy <= 1 && !touches; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= outW || ny >= outH) continue;
                    int q = ny * outW + nx;
                    if (part[q] >= 0 || part[q] == Neutral) { touches = true; break; }
                }
                if (touches) baseArgb[i] = Outline;
                continue;
            }
            if (part[i] == Gun) {
                baseArgb[i] = gunArgb[i];
                continue;
            }
            if (separator[i]) {
                baseArgb[i] = Outline;
                continue;
            }
            float l = shadeLum[i];
            int step = l > 0.52f ? 3 : l > 0.44f ? 2 : l > 0.36f ? 1 : 0;
            baseArgb[i] = bodyRamp[step];
            if (part[i] == Neutral) continue;
            int m = maskRamp[step];
            maskArgb[part[i]][i] = unchecked((int) 0xFF000000) | (m << 16) | (m << 8) | m;
        }

        Directory.CreateDirectory(outputDir);
        Write(Path.Combine(outputDir, "body_hud.png"), outW, outH, baseArgb);
        var report = new StringBuilder();
        report.AppendLine("size=" + outW + "x" + outH);
        for (int p = 0; p < Parts.Length; p++) {
            Write(Path.Combine(outputDir, "body_hud_" + Parts[p] + ".png"), outW, outH, maskArgb[p]);
            long sx = 0, sy = 0, count = 0; int minX = outW, maxX = -1, minY = outH, maxY = -1;
            for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) if ((maskArgb[p][y * outW + x] >> 24) != 0) {
                sx += x; sy += y; count++;
                minX = Math.Min(minX, x); maxX = Math.Max(maxX, x); minY = Math.Min(minY, y); maxY = Math.Max(maxY, y);
            }
            report.AppendLine(Parts[p] + " pixels=" + count
                + (count > 0 ? " centroid=" + (sx / count) + "," + (sy / count) + " box=" + minX + "," + minY + ".." + maxX + "," + maxY : ""));
        }
        return report.ToString();
    }
}
'@

$report = [BodyHudIcon]::Build((Resolve-Path $SourceDirectory).Path, [IO.Path]::GetFullPath($OutputDirectory), $Height)
Write-Output "Wrote body_hud.png and 7 part masks to $OutputDirectory"
Write-Output $report
