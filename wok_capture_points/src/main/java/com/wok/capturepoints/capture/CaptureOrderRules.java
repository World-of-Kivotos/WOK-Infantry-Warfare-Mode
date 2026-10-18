package com.wok.capturepoints.capture;

import java.util.List;
import java.util.Map;

public final class CaptureOrderRules {
    private CaptureOrderRules() {
    }

    public static boolean allowed(List<CapturePoint> points, Map<String, CaptureTeam> owners,
                                  CapturePoint target, CaptureTeam team) {
        if (team != CaptureTeam.BLUE && team != CaptureTeam.RED) return false;
        for (CapturePoint point : points) {
            if (!point.enabled() || point.id().equals(target.id())) continue;
            boolean prerequisite = team == CaptureTeam.BLUE
                    ? point.order() < target.order() : point.order() > target.order();
            if (prerequisite && owners.get(point.id()) != team) return false;
        }
        return true;
    }
}
