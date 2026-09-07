package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.network.CaptureSnapshotPacket;

import java.util.List;

public final class ClientCaptureState {
    private static volatile List<CapturePointView> points = List.of();
    private static volatile String insidePointId;
    private static volatile boolean sequential;
    private static volatile int defaultCaptureSeconds = 90;

    private ClientCaptureState() {
    }

    public static void update(CaptureSnapshotPacket packet) {
        points = packet.points();
        insidePointId = packet.insidePointId();
        sequential = packet.sequential();
        defaultCaptureSeconds = packet.defaultCaptureSeconds();
    }

    public static List<CapturePointView> points() { return points; }
    public static String insidePointId() { return insidePointId; }
    public static boolean sequential() { return sequential; }
    public static int defaultCaptureSeconds() { return defaultCaptureSeconds; }

    public static CapturePointView insidePoint() {
        String id = insidePointId;
        if (id == null) return null;
        return points.stream().filter(point -> point.id().equals(id)).findFirst().orElse(null);
    }

    public static void clear() {
        points = List.of();
        insidePointId = null;
    }
}
