package com.wok.infantry.uitest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Writes the machine- and human-readable UI acceptance artifacts next to
 * {@code wok_ui_acceptance.txt}:
 * <ul>
 *   <li>{@code wok_ui_layout.json} — every capture with its tier, layout size, controls and
 *       layout violations (rule, subject, rectangles in GUI coordinates);</li>
 *   <li>{@code wok_ui_manifest.json} — the list of screenshots and their status;</li>
 *   <li>{@code index.html} — a page with each Java screenshot next to the preview's PNG of the
 *       same surface, state and tier, and its violations.</li>
 * </ul>
 */
public final class UiResultWriter {
    public static final String LAYOUT_FILE = "wok_ui_layout.json";
    public static final String MANIFEST_FILE = "wok_ui_manifest.json";
    public static final String INDEX_FILE = "index.html";

    private UiResultWriter() {
    }

    /**
     * @param resultDirectory {@code run/ui-test/ui-test-results}
     * @param previewShots    directory of the preview's PNGs ({@code ui-preview/shots}), or null
     */
    public static void write(Path resultDirectory, List<UiCaseResult> results, String status,
                             String language, Path previewShots) throws IOException {
        Files.createDirectories(resultDirectory);
        JsonObject layout = new JsonObject();
        layout.addProperty("status", status);
        layout.addProperty("language", language);
        layout.addProperty("generatedAt", Instant.now().toString());
        layout.addProperty("coordinates", "GUI-scaled screen pixels; layout = GUI / baseScale");
        JsonArray captures = new JsonArray();
        JsonArray manifest = new JsonArray();
        int violations = 0;
        int strictViolations = 0;
        for (UiCaseResult result : results) {
            captures.add(capture(result));
            JsonObject entry = new JsonObject();
            entry.addProperty("file", result.fileName());
            entry.addProperty("case", result.caseId());
            entry.addProperty("tier", result.tier());
            entry.addProperty("status", result.status());
            entry.addProperty("strict", result.strict());
            manifest.add(entry);
            violations += result.violations().size();
            if (result.strict()) {
                strictViolations += result.violations().size();
            }
        }
        layout.addProperty("violations", violations);
        layout.addProperty("strictViolations", strictViolations);
        layout.add("captures", captures);
        JsonObject manifestRoot = new JsonObject();
        manifestRoot.addProperty("status", status);
        manifestRoot.addProperty("screenshots", results.stream()
                .filter(result -> !result.fileName().isEmpty()).count());
        manifestRoot.add("captures", manifest);

        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Files.writeString(resultDirectory.resolve(LAYOUT_FILE), gson.toJson(layout),
                StandardCharsets.UTF_8);
        Files.writeString(resultDirectory.resolve(MANIFEST_FILE), gson.toJson(manifestRoot),
                StandardCharsets.UTF_8);
        Files.writeString(resultDirectory.resolve(INDEX_FILE),
                html(results, status, language, previewShots), StandardCharsets.UTF_8);
    }

    private static JsonObject capture(UiCaseResult result) {
        JsonObject capture = new JsonObject();
        capture.addProperty("case", result.caseId());
        capture.addProperty("surface", result.surfaceId());
        capture.addProperty("state", result.stateId());
        capture.addProperty("group", result.group());
        capture.addProperty("tier", result.tier());
        capture.addProperty("previewTier", result.previewTier());
        capture.addProperty("file", result.fileName());
        capture.addProperty("status", result.status());
        capture.addProperty("migrated", result.migrated());
        capture.addProperty("strict", result.strict());
        capture.addProperty("blocking", result.blocking());
        capture.addProperty("screen", result.screenClass());
        capture.addProperty("layout", result.layoutWidth() + "x" + result.layoutHeight());
        capture.addProperty("baseScale", result.baseScale());
        capture.addProperty("guiScale", result.guiScale());
        if (result.failure() != null) {
            capture.addProperty("failure", result.failure());
        }
        JsonArray violations = new JsonArray();
        for (UiLayoutReport.Violation violation : result.violations()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("rule", violation.rule().id());
            entry.addProperty("subject", violation.subject());
            entry.addProperty("detail", violation.detail());
            entry.add("at", rect(violation.at()));
            if (violation.with() != null) {
                entry.add("with", rect(violation.with()));
            }
            violations.add(entry);
        }
        capture.add("violations", violations);
        UiLayoutFrame frame = result.frame();
        if (frame != null) {
            JsonObject counts = new JsonObject();
            counts.addProperty("boxes", frame.boxes().size());
            counts.addProperty("texts", frame.texts().size());
            counts.addProperty("controls", frame.controls().size());
            counts.addProperty("icons", frame.icons().size());
            capture.add("counts", counts);
            JsonArray controls = new JsonArray();
            for (UiLayoutFrame.Control control : frame.controls()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("uiId", control.uiId());
                entry.addProperty("kind", control.kind());
                entry.addProperty("state", control.state());
                entry.add("rect", rect(control.rect()));
                entry.addProperty("active", control.active());
                if (control.truncated()) {
                    entry.addProperty("truncated", true);
                }
                if (control.focusRing()) {
                    entry.addProperty("focusRing", true);
                }
                if (!control.disabledReason().isEmpty()) {
                    entry.addProperty("reason", control.disabledReason());
                }
                controls.add(entry);
            }
            capture.add("controls", controls);
            JsonArray boxes = new JsonArray();
            for (UiLayoutFrame.Box box : frame.boxes()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", box.id());
                entry.add("rect", rect(box.rect()));
                entry.addProperty("solid", box.solid());
                entry.addProperty("parent", box.parent());
                boxes.add(entry);
            }
            capture.add("boxes", boxes);
            if (!frame.notes().isEmpty()) {
                JsonArray notes = new JsonArray();
                frame.notes().forEach(notes::add);
                capture.add("notes", notes);
            }
        }
        return capture;
    }

    private static JsonArray rect(UiLayoutFrame.Rect rect) {
        JsonArray array = new JsonArray();
        array.add(round(rect.left()));
        array.add(round(rect.top()));
        array.add(round(rect.right()));
        array.add(round(rect.bottom()));
        return array;
    }

    private static double round(float value) {
        return Math.round(value * 10.0F) / 10.0D;
    }

    // ---- index.html -----------------------------------------------------------------------------

    private static String html(List<UiCaseResult> results, String status, String language,
                               Path previewShots) {
        StringBuilder page = new StringBuilder(16_384);
        long failed = results.stream().filter(UiCaseResult::failsRun).count();
        long reported = results.stream().filter(result -> "REPORT".equals(result.status())).count();
        page.append("<!doctype html>\n<html lang=\"zh-CN\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>WOK UI acceptance</title><style>")
                .append(":root{--bg:#f2f4f3;--fg:#1b262a;--muted:#52626a;--card:#fff;")
                .append("--edge:#c9d1ce;--pass:#3b7a57;--fail:#b0443c;--report:#be7a1e}")
                .append("@media (prefers-color-scheme: dark){:root{--bg:#14191b;--fg:#eef3f0;")
                .append("--muted:#9daaa8;--card:#1d2527;--edge:#3a4547}}")
                .append("body{margin:0;padding:16px;background:var(--bg);color:var(--fg);")
                .append("font:14px/1.45 system-ui,sans-serif}")
                .append("h1{font-size:20px;margin:0 0 4px}.meta{color:var(--muted);margin-bottom:16px}")
                .append(".grid{display:grid;gap:16px;grid-template-columns:repeat(auto-fill,")
                .append("minmax(min(100%,640px),1fr))}")
                .append(".card{background:var(--card);border:1px solid var(--edge);")
                .append("border-radius:8px;padding:12px;min-width:0}")
                .append(".card h2{font-size:15px;margin:0 0 8px;display:flex;gap:8px;")
                .append("align-items:center;flex-wrap:wrap}")
                .append(".badge{font-size:12px;padding:1px 8px;border-radius:10px;color:#fff}")
                .append(".PASS{background:var(--pass)}.FAIL{background:var(--fail)}")
                .append(".REPORT{background:var(--report)}")
                .append(".shots{display:grid;grid-template-columns:1fr 1fr;gap:8px}")
                .append(".shots figure{margin:0;min-width:0}.shots img{width:100%;height:auto;")
                .append("image-rendering:pixelated;border:1px solid var(--edge)}")
                .append("figcaption{font-size:12px;color:var(--muted)}")
                .append("ul{margin:8px 0 0;padding-left:18px}li{word-break:break-word}")
                .append(".none{color:var(--muted);font-size:12px}</style></head><body>\n");
        page.append("<h1>WOK步战核心 UI 验收</h1><div class=\"meta\">status=")
                .append(escape(status)).append(" · lang=").append(escape(language))
                .append(" · captures=").append(results.size()).append(" · fail=")
                .append(failed).append(" · report=").append(reported).append(" · ")
                .append(escape(Instant.now().toString())).append("</div>\n<div class=\"grid\">\n");
        for (UiCaseResult result : results) {
            page.append("<section class=\"card\"><h2>").append(escape(result.caseId()))
                    .append(" @ ").append(escape(result.tier()))
                    .append(" <span class=\"badge ").append(result.status()).append("\">")
                    .append(result.status()).append("</span></h2>");
            page.append("<div class=\"meta\">").append(escape(result.screenClass()))
                    .append(" · layout ").append(result.layoutWidth()).append('x')
                    .append(result.layoutHeight()).append(" ×").append(result.baseScale())
                    .append(result.strict() ? " · strict" : " · report-only").append("</div>");
            page.append("<div class=\"shots\"><figure>");
            if (!result.fileName().isEmpty()) {
                page.append("<img loading=\"lazy\" alt=\"Java\" src=\"../screenshots/")
                        .append(escape(result.fileName())).append("\">");
            }
            page.append("<figcaption>Java · ").append(escape(result.fileName()))
                    .append("</figcaption></figure><figure>");
            Path preview = previewShot(previewShots, result);
            if (preview != null) {
                page.append("<img loading=\"lazy\" alt=\"preview\" src=\"")
                        .append(escape(preview.toUri().toString())).append("\">");
            }
            page.append("<figcaption>预览 ").append(escape(result.surfaceId())).append(" · ")
                    .append(escape(result.stateId())).append(" · ")
                    .append(escape(result.previewTier()))
                    .append(preview == null ? "（无对应预览图）" : "").append("</figcaption></figure></div>");
            if (result.failure() != null) {
                page.append("<p><b>failure:</b> ").append(escape(result.failure())).append("</p>");
            }
            if (result.violations().isEmpty()) {
                page.append("<p class=\"none\">0 layout violations</p>");
            } else {
                page.append("<ul>");
                for (UiLayoutReport.Violation violation : result.violations()) {
                    page.append("<li><b>").append(escape(violation.rule().id())).append("</b> ")
                            .append(escape(violation.subject())).append(" — ")
                            .append(escape(violation.detail())).append(" ")
                            .append(escape(String.valueOf(violation.at()))).append("</li>");
                }
                page.append("</ul>");
            }
            page.append("</section>\n");
        }
        page.append("</div></body></html>\n");
        return page.toString();
    }

    /** {@code <surface>__new__<tier>__<state>.png} of the preview, if it exists. */
    private static Path previewShot(Path previewShots, UiCaseResult result) {
        if (previewShots == null || result.surfaceId().isEmpty()
                || "legacy".equals(result.surfaceId())) {
            return null;
        }
        Path shot = previewShots.resolve(result.surfaceId() + "__new__" + result.previewTier()
                + "__" + result.stateId() + ".png");
        return Files.isRegularFile(shot) ? shot : null;
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
