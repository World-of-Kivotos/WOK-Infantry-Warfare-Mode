package com.wok.capturepoints.client;

import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The thin capture strip (0.1.0-alpha.4, replaces the 330×52 panel of alpha.3): one row "name │
 * blue ▬▬|▬▬ red" at most 200 wide at the top centre, the status text in a second row on wide
 * screens ({@link CaptureStripLayout}). Shown only while the viewer stands in a point and
 * WOK步战核心 does not show the point in its own battle strip ({@link InfantryHudLink}).
 *
 * <p>Plate colours follow the core HUD's field plates (translucent fill, 1px edge, accent line on
 * top), copied here so the add-on keeps no hard dependency. Text is never shadowed.
 */
public final class CaptureHudOverlay {
    private static final int PLATE = 0xB3121A1D;
    private static final int EDGE = 0xCC56625F;
    private static final int TRACK = 0xCC424E52;
    private static final String ELLIPSIS = "…";

    public static final IGuiOverlay INSTANCE = (gui, graphics, partialTick, width, height) ->
            render(graphics, width, height);

    /** Lowest vanilla boss bar bottom (GUI y) drawn in this frame and in the previous one. */
    private static int bossBottomThisFrame;
    private static int bossBottomLastFrame;

    private CaptureHudOverlay() {
    }

    /** Forge bus, start of a GUI frame: rolls the boss bar measurement over. */
    static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        bossBottomLastFrame = bossBottomThisFrame;
        bossBottomThisFrame = 0;
    }

    /**
     * Forge bus, lowest priority, drawn bars only: records where a vanilla boss bar ends,
     * including any translation another MOD (WOK步战核心) applied to the boss overlay.
     */
    static void onBossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        int shift = Math.round(event.getGuiGraphics().pose().last().pose().m31());
        bossBottomThisFrame = Math.max(bossBottomThisFrame, event.getY() + shift + 5);
    }

    /**
     * The strip this add-on draws this frame as {@code {left, top, width, height}} GUI pixels,
     * or null while it draws nothing (also while WOK步战核心 shows the point itself). Called by
     * the core while it lays out its HUD, before the boss bars of this frame are drawn, so it
     * uses the previous frame's boss bars.
     */
    public static int[] panelRect(int guiWidth, int guiHeight) {
        CapturePointView point = shownPoint();
        return point == null ? null : plate(guiWidth, guiHeight, bossBottomLastFrame).guiRect();
    }

    private static CapturePointView shownPoint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return null;
        }
        CapturePointView point = ClientCaptureState.insidePoint();
        if (point == null || InfantryHudLink.coreRendersCapturePoints()) {
            return null;
        }
        return point;
    }

    private static CaptureStripLayout.Plate plate(int guiWidth, int guiHeight, int bossBottom) {
        Minecraft minecraft = Minecraft.getInstance();
        int factor = CaptureStripLayout.factor(minecraft.getWindow().getGuiScale(), guiWidth,
                guiHeight);
        boolean core = InfantryHudLink.coreLoaded();
        int[] slot = core ? InfantryHudLink.topCenterNext(guiWidth, guiHeight) : null;
        return CaptureStripLayout.place(guiWidth, guiHeight, factor, slot, core, bossBottom);
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        CapturePointView point = shownPoint();
        if (point == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        CaptureStripLayout.Plate plate = plate(width, height, bossBottomThisFrame);
        if (plate.width() <= 0) {
            return;
        }
        graphics.pose().pushPose();
        try {
            if (plate.factor() > 1) {
                graphics.pose().scale(plate.factor(), plate.factor(), 1.0F);
            }
            draw(graphics, minecraft.font, point, plate);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void draw(GuiGraphics graphics, Font font, CapturePointView point,
                             CaptureStripLayout.Plate plate) {
        int left = plate.left();
        int top = plate.top();
        int right = plate.right();
        int bottom = plate.bottom();
        graphics.fill(left, top, right, bottom, PLATE);
        outline(graphics, left, top, right, bottom, EDGE);
        graphics.fill(left, top, right, top + 1, CaptureHudModel.accent(point));

        String blue = String.valueOf(point.bluePlayers());
        String red = String.valueOf(point.redPlayers());
        String name = point.displayName() == null ? point.id() : point.displayName();
        CaptureStripLayout.Row row = CaptureStripLayout.row(plate, font.width(name),
                font.width(blue), font.width(red));
        boolean enabled = point.enabled();
        int y = row.textY();
        if (row.nameRoom() > 0) {
            graphics.drawString(font, fit(font, name, row.nameRoom()), row.nameX(), y,
                    enabled ? CaptureHudModel.LIGHT : CaptureHudModel.MUTED, false);
            graphics.fill(row.dividerX(), y - 1, row.dividerX() + 1, y + 8, EDGE);
        }
        graphics.drawString(font, blue, row.blueX(), y,
                enabled ? CaptureHudModel.BLUE : CaptureHudModel.MUTED, false);
        graphics.drawString(font, red, row.redX(), y,
                enabled ? CaptureHudModel.RED : CaptureHudModel.MUTED, false);

        graphics.fill(row.barLeft(), row.barTop(), row.barRight(), row.barBottom(), TRACK);
        int[] fill = CaptureStripLayout.fill(row, point.control());
        if (fill[1] > fill[0]) {
            CaptureTeam leading = CaptureHudModel.leading(point.control());
            int color = !enabled ? CaptureHudModel.MUTED : CaptureHudModel.teamColor(leading);
            graphics.fill(fill[0], row.barTop(), fill[1], row.barBottom(), color);
        }
        graphics.fill(row.centre(), y + 1, row.centre() + 1, y + 6, CaptureHudModel.LIGHT);

        if (plate.wide()) {
            Component status = CaptureHudModel.statusWithTime(point);
            graphics.drawString(font, fit(font, status.getString(), row.statusRoom()),
                    left + CaptureStripLayout.PAD, row.statusY(),
                    CaptureHudModel.statusColor(point), false);
        }
    }

    /** {@code text} cut to {@code maxWidth} with a trailing "…" when it does not fit. */
    static String fit(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int room = maxWidth - font.width(ELLIPSIS);
        return room <= 0 ? "" : font.plainSubstrByWidth(text, room) + ELLIPSIS;
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right,
                                int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }
}
