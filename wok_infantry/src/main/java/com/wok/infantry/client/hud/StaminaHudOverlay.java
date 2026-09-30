package com.wok.infantry.client.hud;

import com.wok.infantry.client.ClientStaminaState;
import com.wok.infantry.client.screen.BattleUiTheme;
import com.wok.infantry.stamina.StaminaSnapshot;
import com.wok.infantry.stamina.StaminaRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Compact split-stamina readout. With WOK步战附属-部位血量 installed it sits in the
 * strip reserved under the body-health figure; otherwise it keeps a bottom-left
 * position that still stays clear of the hotbar.
 */
public final class StaminaHudOverlay {
    private static final int PANEL_WIDTH = 52;
    private static final int PANEL_HEIGHT = 11;
    private static final int PANEL_LEFT = 43;

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private StaminaHudOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("stamina", INSTANCE);
    }

    private static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        StaminaSnapshot snapshot = ClientStaminaState.snapshot();
        if (minecraft.player == null || minecraft.options.hideGui || !snapshot.enabled()) {
            return;
        }

        int[] slot = BodyHealthHudBridge.companionSlot(screenWidth, screenHeight);
        int panelWidth;
        int left;
        int top;
        if (slot != null) {
            left = slot[0];
            top = slot[1];
            panelWidth = slot[2];
        } else {
            panelWidth = Math.min(PANEL_WIDTH, Math.max(32, screenWidth - 6));
            left = fallbackLeft(screenWidth, panelWidth);
            top = Math.max(2, screenHeight - PANEL_HEIGHT - 2);
        }
        int right = left + panelWidth;
        graphics.fill(left, top, right, top + PANEL_HEIGHT, 0xB5121A20);
        BattleUiTheme.outline(graphics, left, top, right, top + PANEL_HEIGHT,
                0xCC4C5E6B);
        drawBar(graphics, snapshot.arms(), left + 3, top + 2,
                panelWidth - 6, 0xFF5D9FC7);
        drawBar(graphics, snapshot.legs(), left + 3, top + 7,
                panelWidth - 6, 0xFFC88B42);
    }

    /** Bottom-left like before, but never past the hotbar's left edge (x = width / 2 - 91). */
    static int fallbackLeft(int screenWidth, int panelWidth) {
        int hotbarLeft = screenWidth / 2 - 91;
        return Math.max(3, Math.min(PANEL_LEFT, hotbarLeft - 4 - panelWidth));
    }

    private static void drawBar(GuiGraphics graphics, float value,
                                int x, int y, int width, int normalColor) {
        int barRight = x + width;
        graphics.fill(x, y, barRight, y + 3, 0xCC283137);
        float ratio = Math.max(0.0F, Math.min(1.0F, value / 100.0F));
        int fillRight = x + Math.round(width * ratio);
        int color = ratio <= 0.15F ? BattleUiTheme.DANGER
                : value < StaminaRules.SWAY_START_STAMINA ? BattleUiTheme.ACCENT : normalColor;
        graphics.fill(x, y, fillRight, y + 3, color);
    }
}
