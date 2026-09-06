package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.infantry.client.map.TacticalMapAreaOverlay;
import com.wok.infantry.client.map.TacticalMapAreaOverlayRegistry;
import net.minecraft.network.chat.Component;

public final class ClientMapCompat {
    private static boolean registered;

    private ClientMapCompat() {
    }

    public static void register() {
        if (registered) return;
        TacticalMapAreaOverlayRegistry.register(() -> ClientCaptureState.points().stream()
                .filter(CapturePointView::enabled)
                .map(ClientMapCompat::overlay).toList());
        registered = true;
    }

    private static TacticalMapAreaOverlay overlay(CapturePointView point) {
        CaptureTeam visualTeam = point.activeTeam() != CaptureTeam.NEUTRAL
                ? point.activeTeam() : point.owner();
        int color = switch (visualTeam) {
            case BLUE -> 0xFF55A9E8;
            case RED -> 0xFFE56D59;
            case NEUTRAL -> 0xFFE0A04A;
        };
        double progress = visualTeam == CaptureTeam.BLUE ? (point.control() + 1.0D) / 2.0D
                : visualTeam == CaptureTeam.RED ? (1.0D - point.control()) / 2.0D
                : Math.abs(point.control());
        boolean locked = point.activeTeam() == CaptureTeam.BLUE && !point.blueAllowed()
                || point.activeTeam() == CaptureTeam.RED && !point.redAllowed();
        return new TacticalMapAreaOverlay("wok_capture_points:" + point.id(),
                point.dimension(), point.min().getX(), point.min().getZ(),
                point.max().getX() + 1.0D, point.max().getZ() + 1.0D,
                Component.literal(point.displayName()), Component.translatable(
                "hud.wok_capture_points.ratio", point.bluePlayers(), point.redPlayers()),
                color, progress, locked);
    }
}
