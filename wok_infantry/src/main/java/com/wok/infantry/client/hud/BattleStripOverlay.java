package com.wok.infantry.client.hud;

import com.mojang.blaze3d.platform.Window;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.client.screen.TextFit;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.List;
import java.util.Locale;

/**
 * Top battle strip (replaces the 0.3.0-beta.7 manpower banner): both sides' manpower with bars
 * running out from the centre, the viewer's side in the friendly blue, and the notices under it
 * (round result, base supply). The overlay keeps the id {@code wok_infantry:tickets}.
 *
 * <p>Since 0.5.0-beta.2 the strip also carries the WOK步战附属-占点 point the viewer stands in
 * (preview {@code 10-hud} new variant): the objective tile "B 62%" on the centre line and, on
 * non-tight screens, a second row with the point's name, status and time left
 * ({@link BattleStripModel#objective}). Inside a point the strip is shown even without manpower.
 *
 * <p>While any top-centre plate is shown, the vanilla boss bars move down below it as one
 * block (pose translation around {@link VanillaGuiOverlay#BOSS_EVENT_PROGRESS}), so the first
 * boss bar is never covered; on narrow screens they also move right, clear of the squad roster
 * ({@link WokHudLayout.Layout#bossShiftX()}).
 */
public final class BattleStripOverlay {
    /** Overlay id {@code wok_infantry:tickets}; kept so packs can hide it by id. */
    public static final String ID = "tickets";
    /** Layout-probe boxes of the strip and of each notice under it (uiTest only). */
    public static final String PROBE_BOX = "hud.strip";
    public static final String TOAST_PROBE_BOX = "hud.toast";
    /** Layout-probe box of the capture objective tile and prefix of its probe note (uiTest). */
    public static final String OBJECTIVE_PROBE_BOX = "hud.objective";
    public static final String OBJECTIVE_NOTE = "objective ";

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private static boolean bossShifted;

    private BattleStripOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), ID, INSTANCE);
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        HudFrame frame = HudFrame.current(width, height);
        if (frame == null || frame.hidden()) {
            return;
        }
        WokHudLayout.Layout layout = frame.layout();
        if (layout.strip() == null && layout.toasts().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        HudPaint.begin(graphics, frame.factor());
        try {
            if (layout.strip() != null && (frame.strip() != null || frame.objective() != null)) {
                drawStrip(graphics, font, layout.strip(), frame.strip(), frame.objective());
            }
            List<HudFrame.Notice> notices = frame.notices();
            for (int i = 0; i < notices.size() && i < layout.toasts().size(); i++) {
                UiRect toast = layout.toasts().get(i);
                HudPaint.probeBox(graphics, TOAST_PROBE_BOX, toast);
                HudPaint.toast(graphics, font, toast, notices.get(i).text(),
                        notices.get(i).tone());
                UiLayoutProbe.end(graphics);
            }
        } finally {
            HudPaint.end(graphics);
        }
    }

    /**
     * The strip in {@code rect}: manpower ({@code sides}, may be null) and the capture objective
     * tile ({@code objective}, may be null); a strip of the wide height also writes the
     * objective's second row.
     */
    static void drawStrip(GuiGraphics graphics, Font font, UiRect rect,
                          BattleStripModel.Sides sides, BattleStripModel.Objective objective) {
        HudPaint.probeBox(graphics, PROBE_BOX, rect);
        try {
            drawStripContent(graphics, font, rect, sides, objective);
        } finally {
            UiLayoutProbe.end(graphics);
        }
    }

    private static void drawStripContent(GuiGraphics graphics, Font font, UiRect rect,
                                         BattleStripModel.Sides sides,
                                         BattleStripModel.Objective objective) {
        TacticalHud.plate(graphics, rect.left(), rect.top(), rect.right(), rect.bottom(),
                TacticalHud.Edge.TOP, TacticalBoardTheme.SECTION_B, false);
        int blueWidth = sides == null ? 0 : font.width(sides.blueText());
        int redWidth = sides == null ? 0 : font.width(sides.redText());
        BattleStripModel.Tile tile = objective == null ? null
                : BattleStripModel.tile(rect, font.width(objective.name()),
                font.width(objective.value()), objective.lock(),
                BattleStripModel.tileMaxWidth(rect, blueWidth, redWidth, sides != null));
        if (sides != null) {
            BattleStripModel.Geometry geometry = BattleStripModel.geometry(rect, blueWidth,
                    redWidth, tile == null ? null : tile.plate());
            TacticalHud.readout(graphics, font, sides.blueText(), geometry.blueTextX(),
                    geometry.textY(), sides.blueColor());
            TacticalHud.readout(graphics, font, sides.redText(), geometry.redTextX(),
                    geometry.textY(), sides.redColor());
            UiRect blue = geometry.blueTrack();
            TacticalHud.meter(graphics, blue.left(), blue.top(), blue.right(), blue.bottom(),
                    sides.blueRatio(), sides.blueColor(), TacticalBoardTheme.HUD_TRACK, 0, true);
            UiRect red = geometry.redTrack();
            TacticalHud.meter(graphics, red.left(), red.top(), red.right(), red.bottom(),
                    sides.redRatio(), sides.redColor(), TacticalBoardTheme.HUD_TRACK, 0, false);
        }
        if (objective == null) {
            return;
        }
        drawTile(graphics, font, tile, objective);
        boolean line = rect.height() >= WokHudLayout.STRIP_HEIGHT_WIDE;
        if (line) {
            int room = rect.width() - 2 * BattleStripModel.TEXT_INSET;
            TextFit.draw(graphics, font, BattleStripModel.line(objective, room, font::width),
                    rect.left() + BattleStripModel.TEXT_INSET,
                    rect.top() + BattleStripModel.LINE_TOP, room,
                    TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.CENTER);
        }
        if (UiLayoutProbe.recording()) {
            UiRect plate = tile.plate();
            UiLayoutProbe.note(OBJECTIVE_NOTE + "look=" + objective.look()
                    + " edge=" + hex(objective.edge()) + " value=" + objective.value().getString()
                    + " valueColor=" + hex(objective.valueColor()) + " lock=" + objective.lock()
                    + " solid=" + objective.solid() + " line=" + line + " tile=" + plate.left()
                    + "," + plate.top() + "," + plate.right() + "," + plate.bottom());
        }
    }

    /**
     * The objective tile: a HUD plate with its left edge in the objective's colour, an optional
     * lock icon, the short name (shortened with "…" if need be) and the number. Its layout-probe
     * box is not solid: it lies on the strip's own plate by design.
     */
    private static void drawTile(GuiGraphics graphics, Font font, BattleStripModel.Tile tile,
                                 BattleStripModel.Objective objective) {
        UiRect plate = tile.plate();
        UiLayoutProbe.begin(graphics, OBJECTIVE_PROBE_BOX, plate.left(), plate.top(),
                plate.right(), plate.bottom(), false);
        try {
            TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                    TacticalHud.Edge.LEFT, objective.edge(), objective.solid());
            if (objective.lock()) {
                TacticalIcon.LOCK.draw(graphics, tile.iconX(), tile.iconY(),
                        TacticalBoardTheme.LIGHT_MUTED);
            }
            if (tile.nameRoom() > 0) {
                TextFit.draw(graphics, font, objective.name(), tile.nameX(), tile.textY(),
                        tile.nameRoom(), objective.nameColor(), TextFit.Align.LEFT);
            }
            TacticalHud.readout(graphics, font, objective.value(), tile.valueX(), tile.textY(),
                    objective.valueColor());
        } finally {
            UiLayoutProbe.end(graphics);
        }
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08X", argb);
    }

    /**
     * Forge bus, lowest priority: moves the vanilla boss bars below the core's top-centre plates
     * and, on narrow screens, right of the squad roster.
     * Listening last means a cancelled boss overlay is never shifted (no unmatched pose push).
     */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled() || bossShifted || !isBossOverlay(event)) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        HudFrame frame = HudFrame.current(window.getGuiScaledWidth(),
                window.getGuiScaledHeight());
        if (frame == null || frame.hidden() || frame.layout().bossShift() <= 0
                && frame.layout().bossShiftX() <= 0) {
            return;
        }
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(frame.layout().bossShiftX(),
                frame.layout().bossShift(), 0.0F);
        bossShifted = true;
    }

    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (bossShifted && isBossOverlay(event)) {
            event.getGuiGraphics().pose().popPose();
            bossShifted = false;
        }
    }

    /** Safety net: restores the pose if the boss overlay was cancelled after the shift began. */
    public static void onGuiPost(RenderGuiEvent.Post event) {
        if (bossShifted) {
            event.getGuiGraphics().pose().popPose();
            bossShifted = false;
        }
    }

    private static boolean isBossOverlay(RenderGuiOverlayEvent event) {
        return event.getOverlay() != null
                && VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id().equals(event.getOverlay().id());
    }
}
