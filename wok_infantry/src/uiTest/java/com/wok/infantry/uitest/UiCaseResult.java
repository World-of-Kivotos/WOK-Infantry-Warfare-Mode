package com.wok.infantry.uitest;

import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutReport;

import java.util.List;

/**
 * Outcome of one capture: a case on one tier, or one of the 14 legacy live-flow screenshots.
 *
 * @param caseId     {@code <surface>.<state>}, or {@code legacy.<file stem>} for a live capture
 * @param tier       tier id ({@code 320x240} …)
 * @param fileName   screenshot written for this capture ("" when none was taken)
 * @param strict     whether layout violations fail the run (migrated surface, required tier,
 *                   {@code wok.ui.layoutStrict} on)
 * @param blocking   whether a failure fails the run (required tier)
 * @param frame      the probed frame, or {@code null} when the capture never happened
 * @param violations layout violations of {@code frame}
 * @param failure    why a step or check failed, or {@code null}
 */
public record UiCaseResult(String caseId, String surfaceId, String stateId, String group,
                           String tier, String previewTier, String fileName, boolean migrated,
                           boolean strict, boolean blocking, String screenClass, int layoutWidth,
                           int layoutHeight, int baseScale, double guiScale, UiLayoutFrame frame,
                           List<UiLayoutReport.Violation> violations, String failure) {
    public UiCaseResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    /** PASS, FAIL (fails the run) or REPORT (problems only reported). */
    public String status() {
        boolean problem = failure != null || !violations.isEmpty();
        if (!problem) {
            return "PASS";
        }
        boolean failsRun = blocking && (failure != null || strict);
        return failsRun ? "FAIL" : "REPORT";
    }

    public boolean failsRun() {
        return "FAIL".equals(status());
    }

    /** One line for the acceptance result file. */
    public String summary() {
        StringBuilder line = new StringBuilder("case[").append(caseId).append('@').append(tier)
                .append("]=").append(status())
                .append(" violations=").append(violations.size())
                .append(strict ? " strict" : " report-only");
        if (layoutWidth > 0) {
            line.append(" layout=").append(layoutWidth).append('x').append(layoutHeight)
                    .append(" x").append(baseScale);
        }
        if (!fileName.isEmpty()) {
            line.append(" file=").append(fileName);
        }
        if (failure != null) {
            line.append(" failure=").append(failure.replace('\n', ' '));
        }
        return line.toString();
    }
}
