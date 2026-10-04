package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Pre-battle formation ballot at the top centre (waiting to open, open and not voted, voted,
 * and the 3-second lock notice whose bottom hairline runs out). It only reads the client caches
 * and never sends a packet; the terminal key opens the formation page.
 */
public final class FormationVoteHudOverlay {
    /** Overlay id {@code wok_infantry:formation_vote}. */
    public static final String ID = "formation_vote";
    /** Layout-probe box of the ballot plate (uiTest only). */
    public static final String PROBE_BOX = "hud.vote";
    /** Icon and first text row 5px under the top edge; second row 10px further down. */
    static final int TEXT_TOP = 5;
    static final int SECOND_LINE = 15;
    static final int SECOND_LINE_WITH_METER = 20;

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private FormationVoteHudOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), ID, INSTANCE);
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        HudFrame frame = HudFrame.current(width, height);
        if (frame == null || frame.hidden() || frame.vote() == null
                || frame.layout().vote() == null) {
            return;
        }
        HudPaint.begin(graphics, frame.factor());
        try {
            draw(graphics, Minecraft.getInstance().font, frame.layout().vote(), frame.vote());
        } finally {
            HudPaint.end(graphics);
        }
    }

    static void draw(GuiGraphics graphics, Font font, UiRect rect,
                     FormationVoteHudModel.Plate plate) {
        HudPaint.probeBox(graphics, PROBE_BOX, rect);
        try {
            drawPlate(graphics, font, rect, plate);
        } finally {
            UiLayoutProbe.end(graphics);
        }
    }

    private static void drawPlate(GuiGraphics graphics, Font font, UiRect rect,
                                  FormationVoteHudModel.Plate plate) {
        TacticalHud.plate(graphics, rect.left(), rect.top(), rect.right(), rect.bottom(),
                TacticalHud.Edge.TOP, plate.accent(), plate.solid());
        if (plate.hasEmblem()) {
            int emblemY = rect.top() + (rect.height() - HudPaint.EMBLEM_SIZE) / 2;
            HudPaint.emblem(graphics, HudPaint.emblemTexture(plate.emblemId()), rect.left() + 5,
                    emblemY);
        } else if (plate.icon() != null) {
            plate.icon().draw(graphics, rect.left() + 5, rect.top() + TEXT_TOP, plate.iconColor());
        }
        int textX = rect.left() + 6 + plate.leadWidth();
        int right = rect.right() - 5;
        TacticalHud.drawSegments(graphics, font, plate.firstLine(), textX, rect.top() + TEXT_TOP,
                right);
        int secondY = rect.top() + SECOND_LINE;
        if (plate.meter()) {
            TacticalHud.meter(graphics, textX, rect.top() + SECOND_LINE, right,
                    rect.top() + SECOND_LINE + 2, plate.meterRatio(), TacticalBoardTheme.NEUTRAL_B);
            secondY = rect.top() + SECOND_LINE_WITH_METER;
        }
        TacticalHud.drawSegments(graphics, font, plate.secondLine(), textX, secondY, right);
        if (plate.state() == FormationVoteHudModel.State.LOCKED) {
            TacticalHud.meter(graphics, rect.left() + 1, rect.bottom() - 2, rect.right() - 1,
                    rect.bottom() - 1, plate.remain(), TacticalBoardTheme.SUCCESS_B);
        }
    }
}
