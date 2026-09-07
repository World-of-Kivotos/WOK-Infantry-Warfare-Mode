package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class CaptureHudOverlay {
    private static final int BLUE = 0xFF55A9E8;
    private static final int RED = 0xFFE56D59;
    private static final int ORANGE = 0xFFE0A04A;
    private static final int GREEN = 0xFF75BE78;
    private static final int MUTED = 0xFF91A09D;

    public static final IGuiOverlay INSTANCE = (gui, graphics, partialTick, width, height) ->
            render(graphics, width);

    private CaptureHudOverlay() {
    }

    private static void render(GuiGraphics graphics, int width) {
        Minecraft minecraft = Minecraft.getInstance();
        CapturePointView point = ClientCaptureState.insidePoint();
        if (minecraft.player == null || minecraft.options.hideGui || point == null) return;

        if (width <= 360) {
            renderCompact(graphics, minecraft, point, width);
            return;
        }
        int panelWidth = Math.min(330, Math.max(190, width - 24));
        int left = (width - panelWidth) / 2;
        int top = 8;
        int right = left + panelWidth;
        int bottom = top + 52;
        graphics.fill(left, top, right, bottom, 0xE3141B1D);
        graphics.fill(left, top, right, top + 2, ORANGE);
        outline(graphics, left, top, right, bottom, 0xFF566461);

        Component title = Component.translatable("hud.wok_capture_points.title",
                point.displayName());
        drawCentered(graphics, minecraft, title, width / 2, top + 6,
                point.enabled() ? 0xFFF0F3ED : MUTED, panelWidth - 16);
        Component ratio = Component.translatable("hud.wok_capture_points.ratio",
                point.bluePlayers(), point.redPlayers());
        drawCentered(graphics, minecraft, ratio, width / 2, top + 18,
                0xFFE2E7E1, panelWidth - 16);

        int barLeft = left + 12;
        int barRight = right - 12;
        int barTop = top + 31;
        int center = (barLeft + barRight) / 2;
        graphics.fill(barLeft, barTop, barRight, barTop + 7, 0xFF283235);
        graphics.fill(center - 1, barTop - 1, center + 1, barTop + 8, 0xFFE2E7E1);
        int controlX = (int) Math.round(center + point.control() * (barRight - barLeft) / 2.0D);
        if (controlX >= center) graphics.fill(center, barTop, controlX, barTop + 7, BLUE);
        else graphics.fill(controlX, barTop, center, barTop + 7, RED);
        outline(graphics, barLeft, barTop, barRight, barTop + 7, 0xFF687673);

        Component state = stateText(point);
        drawCentered(graphics, minecraft, state, width / 2, top + 40,
                stateColor(point), panelWidth - 16);
    }

    private static void renderCompact(GuiGraphics graphics, Minecraft minecraft,
                                      CapturePointView point, int width) {
        int left = 8;
        int right = width - 8;
        int top = 4;
        int bottom = 39;
        graphics.fill(left, top, right, bottom, 0xE3141B1D);
        graphics.fill(left, top, right, top + 2, ORANGE);
        outline(graphics, left, top, right, bottom, 0xFF566461);

        Component heading = Component.translatable("hud.wok_capture_points.compact_heading",
                point.displayName(), point.bluePlayers(), point.redPlayers());
        drawCentered(graphics, minecraft, heading, width / 2, top + 5,
                point.enabled() ? 0xFFF0F3ED : MUTED, right - left - 12);

        int barLeft = left + 10;
        int barRight = right - 10;
        int barTop = top + 17;
        int center = (barLeft + barRight) / 2;
        graphics.fill(barLeft, barTop, barRight, barTop + 5, 0xFF283235);
        graphics.fill(center - 1, barTop - 1, center + 1, barTop + 6, 0xFFE2E7E1);
        int controlX = (int) Math.round(center + point.control() * (barRight - barLeft) / 2.0D);
        if (controlX >= center) graphics.fill(center, barTop, controlX, barTop + 5, BLUE);
        else graphics.fill(controlX, barTop, center, barTop + 5, RED);
        outline(graphics, barLeft, barTop, barRight, barTop + 5, 0xFF687673);

        Component state = stateText(point);
        drawCentered(graphics, minecraft, state, width / 2, top + 26,
                stateColor(point), right - left - 12);
    }

    private static Component stateText(CapturePointView point) {
        if (!point.enabled()) return Component.translatable("hud.wok_capture_points.disabled");
        if (point.bluePlayers() > 0 && point.redPlayers() > 0
                && point.activeTeam() == CaptureTeam.NEUTRAL) {
            return Component.translatable("hud.wok_capture_points.contested");
        }
        if (point.activeTeam() == CaptureTeam.BLUE && !point.blueAllowed()
                || point.activeTeam() == CaptureTeam.RED && !point.redAllowed()
                || point.activeTeam() == CaptureTeam.NEUTRAL
                && ((point.bluePlayers() > 0 && !point.blueAllowed())
                || (point.redPlayers() > 0 && !point.redAllowed()))) {
            return Component.translatable("hud.wok_capture_points.locked");
        }
        if (point.activeTeam() != CaptureTeam.NEUTRAL) {
            return Component.translatable("hud.wok_capture_points.capturing",
                    Component.translatable("team.wok_capture_points." + point.activeTeam().id()),
                    point.speedMultiplier());
        }
        if (point.owner() != CaptureTeam.NEUTRAL) {
            return Component.translatable("hud.wok_capture_points.secured",
                    Component.translatable("team.wok_capture_points." + point.owner().id()));
        }
        return Component.translatable("hud.wok_capture_points.neutral");
    }

    private static int stateColor(CapturePointView point) {
        if (point.activeTeam() == CaptureTeam.BLUE) return BLUE;
        if (point.activeTeam() == CaptureTeam.RED) return RED;
        if (point.owner() != CaptureTeam.NEUTRAL) return GREEN;
        return MUTED;
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right,
                                int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft minecraft,
                                     Component component, int centerX, int y, int color,
                                     int maxWidth) {
        String text = minecraft.font.plainSubstrByWidth(component.getString(), maxWidth);
        graphics.drawString(minecraft.font, text, centerX - minecraft.font.width(text) / 2,
                y, color, false);
    }
}
