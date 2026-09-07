package com.wok.downed.client;

import com.wok.downed.registry.DownedEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class DownedOverlay {
    private static final int PANEL_HEIGHT = 46;
    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) ->
                    render(graphics, width, height);

    private static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        MobEffectInstance effect = minecraft.player.getEffect(DownedEffects.DOWNED.get());
        if (effect == null) {
            return;
        }

        int panelWidth = Math.min(260, Math.max(190, width - 20));
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, height - PANEL_HEIGHT - 30);
        int right = left + panelWidth;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left, top, right, bottom, 0xE5141A1D);
        graphics.fill(left, top, right, top + 3, 0xFFD0523C);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF59656A);
        graphics.fill(left, top, left + 1, bottom, 0xFF59656A);
        graphics.fill(right - 1, top, right, bottom, 0xFF59656A);

        int seconds = Math.max(0, (effect.getDuration() + 19) / 20);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("gui.wok_downed.title"),
                width / 2, top + 8, 0xFFFF7663);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("gui.wok_downed.bleedout", seconds),
                width / 2, top + 21, 0xFFF0D6C8);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("gui.wok_downed.waiting"),
                width / 2, top + 33, 0xFFB8C2C5);
    }

    private DownedOverlay() {
    }
}
