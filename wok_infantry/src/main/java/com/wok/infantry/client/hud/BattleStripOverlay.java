package com.wok.infantry.client.hud;

import com.mojang.blaze3d.platform.Window;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.List;

/**
 * Top battle strip (replaces the 0.3.0-beta.7 manpower banner): both sides' manpower with bars
 * running out from the centre, the viewer's side in the friendly blue, and the notices under it
 * (round result, base supply). The overlay keeps the id {@code wok_infantry:tickets}.
 *
 * <p>While any top-centre plate is shown, the vanilla boss bars move down below it as one
 * block (pose translation around {@link VanillaGuiOverlay#BOSS_EVENT_PROGRESS}), so the first
 * boss bar is never covered; on narrow screens they also move right, clear of the squad roster
 * ({@link WokHudLayout.Layout#bossShiftX()}).
 */
public final class BattleStripOverlay {
    /** Overlay id {@code wok_infantry:tickets}; kept so packs can hide it by id. */
    public static final String ID = "tickets";

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
            if (layout.strip() != null && frame.strip() != null) {
                drawStrip(graphics, font, layout.strip(), frame.strip());
            }
            List<HudFrame.Notice> notices = frame.notices();
            for (int i = 0; i < notices.size() && i < layout.toasts().size(); i++) {
                HudPaint.toast(graphics, font, layout.toasts().get(i), notices.get(i).text(),
                        notices.get(i).tone());
            }
        } finally {
            HudPaint.end(graphics);
        }
    }

    static void drawStrip(GuiGraphics graphics, Font font, UiRect rect,
                          BattleStripModel.Sides sides) {
        TacticalHud.plate(graphics, rect.left(), rect.top(), rect.right(), rect.bottom(),
                TacticalHud.Edge.TOP, TacticalBoardTheme.SECTION_B, false);
        BattleStripModel.Geometry geometry = BattleStripModel.geometry(rect,
                font.width(sides.blueText()), font.width(sides.redText()));
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
