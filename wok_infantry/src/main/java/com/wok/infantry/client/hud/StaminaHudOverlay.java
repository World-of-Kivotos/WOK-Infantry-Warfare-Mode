package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Split stamina (arms 手, legs 腿) in the vitals group.
 *
 * <p>With WOK步战附属-部位血量 on screen it stays in the 52×11 companion strip that add-on
 * reserves under its 39×48 figure and draws one row, "手 ▬ 腿 ▬" (the figure and its layout are
 * the add-on's and are not touched). The strip is in the add-on's GUI pixels, so this mode is
 * drawn without the core HUD scale; at GUI scale 1 with the 2× HUD the labels would be 1×
 * unreadable glyphs, so the row falls back to two unlabelled bars, as it does when the labels do
 * not fit. Without the add-on it draws its own plate, "手" over "腿", in the vitals slot, whose
 * right edge stays clear of the hotbar, the off-hand slot and the attack indicator; where the
 * slot under the vanilla chat has room for one row only (the 2× HUD), both share one row.
 *
 * <p>Hidden in creative and spectator mode, with F1, and whenever the server has stamina off
 * (the visibility the body-health HUD test checks through
 * {@code ClientStaminaState.snapshot().enabled()}). Colours: at most 15 red, below the sway
 * threshold (50) orange, otherwise neutral.
 */
public final class StaminaHudOverlay {
    /** Overlay id {@code wok_infantry:stamina}; kept so packs can hide it by id. */
    public static final String ID = "stamina";
    public static final String ARMS_SHORT_KEY = "hud.wok_infantry.stamina.arms_short";
    public static final String LEGS_SHORT_KEY = "hud.wok_infantry.stamina.legs_short";
    /** Smallest labelled bar in the companion row. */
    public static final int MIN_LABELLED_BAR = 10;
    /** Companion row: 2px accent + 2px before the first label, 3px after the last bar. */
    static final int COMPANION_LEFT_INSET = 4;
    static final int COMPANION_RIGHT_INSET = 3;
    static final int COMPANION_PAIR_GAP = 3;
    static final int LABEL_GAP = 2;
    /** Stacked plate: text 5px in from the left, bars end 4px before the right edge. */
    static final int STACK_LEFT_INSET = 5;
    static final int STACK_RIGHT_INSET = 4;
    static final int STACK_FIRST_ROW = 3;
    static final int STACK_SECOND_ROW = 13;
    static final int ROW_PAIR_GAP = 4;

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private StaminaHudOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), ID, INSTANCE);
    }

    /**
     * One-row companion layout: labelled ("手" bar, 3px, "腿" bar) when both bars keep at least
     * {@link #MIN_LABELLED_BAR} pixels, otherwise two unlabelled 3px bars stacked in the strip.
     */
    public record CompanionRow(boolean labelled, int textY, int armsLabelX, UiRect armsBar,
                               int legsLabelX, UiRect legsBar) {
    }

    public static CompanionRow companionRow(int left, int top, int width, int height,
                                            int armsLabelWidth, int legsLabelWidth,
                                            boolean labelsAllowed) {
        int innerLeft = left + COMPANION_LEFT_INSET;
        int innerRight = left + width - COMPANION_RIGHT_INSET;
        int textY = top + Math.max(0, (height - 8) / 2);
        if (labelsAllowed) {
            int half = (innerRight - innerLeft - COMPANION_PAIR_GAP) / 2;
            int armsBarLeft = innerLeft + armsLabelWidth + LABEL_GAP;
            int armsBarRight = innerLeft + half;
            int legsLabelX = armsBarRight + COMPANION_PAIR_GAP;
            int legsBarLeft = legsLabelX + legsLabelWidth + LABEL_GAP;
            if (armsBarRight - armsBarLeft >= MIN_LABELLED_BAR
                    && innerRight - legsBarLeft >= MIN_LABELLED_BAR) {
                return new CompanionRow(true, textY, innerLeft,
                        UiRect.of(armsBarLeft, textY + 2, armsBarRight, textY + 5), legsLabelX,
                        UiRect.of(legsBarLeft, textY + 2, innerRight, textY + 5));
            }
        }
        return new CompanionRow(false, textY, innerLeft,
                UiRect.of(innerLeft, top + 2, innerRight, top + 5), innerLeft,
                UiRect.of(innerLeft, top + 7, innerRight, top + 10));
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        HudFrame frame = HudFrame.current(width, height);
        if (frame == null || frame.hidden()) {
            return;
        }
        StaminaSnapshot stamina = frame.stamina();
        Font font = Minecraft.getInstance().font;
        int[] slot = frame.companionSlot();
        if (slot != null) {
            drawCompanion(graphics, font, slot, stamina, frame.factor() == 1);
            return;
        }
        UiRect plate = frame.staminaShown() ? frame.layout().staminaPlate() : null;
        if (plate == null) {
            return;
        }
        HudPaint.begin(graphics, frame.factor());
        try {
            if (frame.layout().staminaRow()) {
                drawRow(graphics, font, plate, stamina);
            } else {
                drawStack(graphics, font, plate, stamina);
            }
        } finally {
            HudPaint.end(graphics);
        }
    }

    /** Pair widths of the one-row plate: arms bar from x, 4px gap, legs bar to {@code right}. */
    static int rowPairWidth(int x, int right) {
        return Math.max(0, (right - x - ROW_PAIR_GAP) / 2);
    }

    /** One-row plate under the chat at the 2× HUD: "手 ▬  腿 ▬". */
    static void drawRow(GuiGraphics graphics, Font font, UiRect plate, StaminaSnapshot stamina) {
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.LEFT, TacticalBoardTheme.NEUTRAL_B, false);
        int x = plate.left() + STACK_LEFT_INSET;
        int right = plate.right() - STACK_RIGHT_INSET;
        int pair = rowPairWidth(x, right);
        int y = plate.top() + STACK_FIRST_ROW;
        TacticalHud.labelledBar(graphics, font, Component.translatable(ARMS_SHORT_KEY),
                stamina.armRatio(), TacticalHud.staminaColor(stamina.arms()), x, y, x + pair);
        TacticalHud.labelledBar(graphics, font, Component.translatable(LEGS_SHORT_KEY),
                stamina.legRatio(), TacticalHud.staminaColor(stamina.legs()),
                x + pair + ROW_PAIR_GAP, y, right);
    }

    static void drawCompanion(GuiGraphics graphics, Font font, int[] slot,
                              StaminaSnapshot stamina, boolean labelsAllowed) {
        int left = slot[0];
        int top = slot[1];
        int right = left + slot[2];
        int bottom = top + slot[3];
        TacticalHud.plate(graphics, left, top, right, bottom, TacticalHud.Edge.LEFT,
                TacticalBoardTheme.NEUTRAL_B, false);
        Component arms = Component.translatable(ARMS_SHORT_KEY);
        Component legs = Component.translatable(LEGS_SHORT_KEY);
        CompanionRow row = companionRow(left, top, slot[2], slot[3], font.width(arms),
                font.width(legs), labelsAllowed);
        if (row.labelled()) {
            graphics.drawString(font, arms, row.armsLabelX(), row.textY(),
                    TacticalBoardTheme.LIGHT_MUTED, false);
            graphics.drawString(font, legs, row.legsLabelX(), row.textY(),
                    TacticalBoardTheme.LIGHT_MUTED, false);
        }
        bar(graphics, row.armsBar(), stamina.arms());
        bar(graphics, row.legsBar(), stamina.legs());
    }

    static void drawStack(GuiGraphics graphics, Font font, UiRect plate,
                          StaminaSnapshot stamina) {
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.LEFT, TacticalBoardTheme.NEUTRAL_B, false);
        int x = plate.left() + STACK_LEFT_INSET;
        int right = plate.right() - STACK_RIGHT_INSET;
        TacticalHud.labelledBar(graphics, font, Component.translatable(ARMS_SHORT_KEY),
                stamina.armRatio(), TacticalHud.staminaColor(stamina.arms()), x,
                plate.top() + STACK_FIRST_ROW, right);
        TacticalHud.labelledBar(graphics, font, Component.translatable(LEGS_SHORT_KEY),
                stamina.legRatio(), TacticalHud.staminaColor(stamina.legs()), x,
                plate.top() + STACK_SECOND_ROW, right);
    }

    private static void bar(GuiGraphics graphics, UiRect bar, float value) {
        TacticalHud.meter(graphics, bar.left(), bar.top(), bar.right(), bar.bottom(),
                value / 100.0F, TacticalHud.staminaColor(value));
    }
}
