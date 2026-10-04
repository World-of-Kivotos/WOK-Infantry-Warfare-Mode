package com.wok.infantry.client.hud;

import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.screen.BattleUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "wok_infantry", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TicketHudOverlay {
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("tickets", (gui, graphics, partial, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            var state = TicketNetwork.snapshot();
            if (minecraft.player == null || minecraft.options.hideGui || !state.visible()) return;
            String label = state.blue() == 0 ? "本局结束 · 红方获胜" : state.red() == 0 ? "本局结束 · 蓝方获胜"
                    : "兵力值 · 蓝 " + state.blue() + "   红 " + state.red();
            int panelWidth = Math.min(width - 8, Math.max(160, minecraft.font.width(label) + 16));
            int left = (width - panelWidth) / 2;
            graphics.fill(left, 3, left + panelWidth, 22, 0xE3141B1D);
            BattleUiTheme.outline(graphics, left, 3, left + panelWidth, 22, 0xFF566461);
            String fitted = minecraft.font.plainSubstrByWidth(label, panelWidth - 12);
            BattleUiTheme.drawCenteredText(graphics, minecraft.font, fitted, width / 2, 8, 0xFFE2E7E1);
            String supply = TicketNetwork.supplyHint();
            if (!supply.isEmpty()) {
                // Narrow screens reserve the left column for the full squad roster.
                boolean compact = width <= 360;
                int supplyWidth = Math.min(width - (compact ? 148 : 16), minecraft.font.width(supply) + 16);
                int supplyLeft = compact ? 140 : (width - supplyWidth) / 2;
                int supplyTop = compact ? 66 : 86;
                graphics.fill(supplyLeft, supplyTop, supplyLeft + supplyWidth, supplyTop + 19, 0xED141B1D);
                BattleUiTheme.outline(graphics, supplyLeft, supplyTop, supplyLeft + supplyWidth, supplyTop + 19, 0xFFE0A04A);
                BattleUiTheme.drawCenteredText(graphics, minecraft.font,
                        minecraft.font.plainSubstrByWidth(supply, supplyWidth - 12),
                        supplyLeft + supplyWidth / 2, supplyTop + 5, 0xFFF0F3ED);
            }
        });
    }
    @Mod.EventBusSubscriber(modid = "wok_infantry", value = Dist.CLIENT)
    public static final class Logout {
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { TicketNetwork.clearClient(); }
    }
}
