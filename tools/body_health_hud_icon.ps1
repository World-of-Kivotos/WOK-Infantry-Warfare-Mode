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

        // Classify: -1 transparent, -2 gear (rifle), otherwise part index.
        var part = new int[n];
        for (int i = 0; i < n; i++) {
            part[i] = -1;
            if (coverage[i] < 0.5f) continue;
            int best = -1; float bestCover = 0;
            for (int p = 0; p < Parts.Length; p++) if (partCover[p, i] > bestCover) { bestCover = partCover[p, i]; best = p; }
            // Masks exclude the rifle, so a mostly dark pixel with little mask cover is gear.
            bool gear = best < 0 || bestCover < 0.3f || (darkFraction[i] > 0.7f && bestCover < 0.6f);
            part[i] = gear ? -2 : best;
        }

        // The source faces carry a noise texture; a 3x3 median within the same part keeps the
        // face tones and drops single-pixel speckles before quantising.
        var smoothLum = new float[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            smoothLum[i] = shadeLum[i];
            if (part[i] < 0) continue;
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

        // One-pixel separators: a part pixel whose right or lower neighbour is another part or gear.
        var separator = new bool[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            if (part[i] < 0) continue;
            int[] neighbours = { x + 1 < outW ? i + 1 : -1, y + 1 < outH ? i + outW : -1 };
            foreach (int q in neighbours) if (q >= 0 && part[q] != -1 && part[q] != part[i]) separator[i] = true;
        }

        const int Outline = unchecked((int) 0xFF15191B);
        int[] bodyRamp = { unchecked((int) 0xFF4F5857), unchecked((int) 0xFF6F7977), unchecked((int) 0xFF939D9B), unchecked((int) 0xFFB4BDBB) };
        int[] maskRamp = { 122, 172, 224, 255 };
        int[] gearRamp = { unchecked((int) 0xFF1F2426), unchecked((int) 0xFF3A4143) };

        var baseArgb = new int[n];
        var maskArgb = new int[Parts.Length][];
        for (int p = 0; p < Parts.Length; p++) maskArgb[p] = new int[n];
        for (int y = 0; y < outH; y++) for (int x = 0; x < outW; x++) {
            int i = y * outW + x;
            if (part[i] == -1) {
                bool touches = false;
                for (int dy = -1; dy <= 1 && !touches; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx >= 0 && ny >= 0 && nx < outW && ny < outH && part[ny * outW + nx] != -1) { touches = true; break; }
                }
                if (touches) baseArgb[i] = Outline;
                continue;
            }
            if (part[i] == -2) {
                baseArgb[i] = gearRamp[shadeLum[i] > 0.28f ? 1 : 0];
                continue;
            }
            if (separator[i]) {
                baseArgb[i] = Outline;
                continue;
            }
            float l = shadeLum[i];
            int step = l > 0.52f ? 3 : l > 0.44f ? 2 : l > 0.36f ? 1 : 0;
            baseArgb[i] = bodyRamp[step];
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
