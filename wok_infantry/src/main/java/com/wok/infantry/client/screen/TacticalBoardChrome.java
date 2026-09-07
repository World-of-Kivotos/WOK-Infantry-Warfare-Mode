package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/** Shared physical-tablet shell used by the tactical map, squad and loadout surfaces. */
final class TacticalBoardChrome {
    private TacticalBoardChrome() {
    }

    static void renderShell(GuiGraphics graphics, int width, int height,
                            TacticalMapLayout.Layout layout) {
        graphics.fill(0, 0, width, height, TacticalBoardTheme.WORLD_SHADE);
        graphics.fill(2, 3, width, height, TacticalBoardTheme.DEVICE_SHADOW);
        graphics.fill(0, 0, width, height, TacticalBoardTheme.DEVICE_FRAME);
        BattleUiTheme.outline(graphics, 1, 1, width - 1, height - 1,
                TacticalBoardTheme.DEVICE_EDGE);
        graphics.fill(4, 4, width - 4, height - 4, TacticalBoardTheme.DEVICE_MID);

        TacticalMapLayout.Rect header = layout.header();
        TacticalMapLayout.Rect footer = layout.footer();
        graphics.fill(header.left(), header.top(), footer.right(), footer.bottom(),
                TacticalBoardTheme.BOARD);
        TacticalBoardTheme.raisedPanel(graphics, header.left(), header.top(),
                header.right(), header.bottom(), TacticalBoardTheme.DEVICE_FRAME);
        TacticalBoardTheme.raisedPanel(graphics, footer.left(), footer.top(),
                footer.right(), footer.bottom(), TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.rivet(graphics, 6, 6);
        TacticalBoardTheme.rivet(graphics, width - 7, 6);
        TacticalBoardTheme.rivet(graphics, 6, height - 7);
        TacticalBoardTheme.rivet(graphics, width - 7, height - 7);
    }

    static void renderHeader(GuiGraphics graphics, Font font,
                             TacticalMapLayout.Layout layout,
                             Component title, Component identity,
                             boolean connected) {
        TacticalMapLayout.Rect header = layout.header();
        int identityWidth = Math.min(header.width() / 2, font.width(identity));
        int titleWidth = Math.max(32, header.width() - identityWidth - 32);
        int titleY = header.top() + 4;
        int statusColor = connected
                ? TacticalBoardTheme.SUCCESS : TacticalBoardTheme.ACCENT;
        graphics.fill(header.left() + 5, titleY + 1,
                header.left() + 8, titleY + 8, statusColor);
        graphics.drawString(font, fitted(font, title, titleWidth),
                header.left() + 12, titleY, TacticalBoardTheme.LIGHT_TEXT, false);
        graphics.drawString(font, fitted(font, identity, header.width() / 2),
                header.right() - identityWidth - 8, titleY,
                connected ? 0xFF9DD8F4 : TacticalBoardTheme.ACCENT, false);
    }

    static Component battleIdentity(BattleSnapshot snapshot) {
        if (snapshot == null) {
            return Component.translatable("screen.wok_infantry.map.link_connecting");
        }
        Component faction = snapshot.faction() == null
                ? Component.translatable("faction.wok_infantry.unassigned")
                : Component.translatable("faction.wok_infantry." + snapshot.faction().id());
        MutableComponent identity = faction.copy();
        if (snapshot.ownSquad() != null) {
            identity.append(" · ").append(SquadScreen.callsign(snapshot.ownSquad()));
        }
        if (snapshot.commander()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.commander"));
        } else if (snapshot.squadLeader()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.squad_leader"));
        } else {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.member"));
        }
        return identity;
    }

    private static FormattedCharSequence fitted(Font font, Component text, int maxWidth) {
        int safeWidth = Math.max(1, maxWidth);
        if (font.width(text) <= safeWidth) {
            return text.getVisualOrderText();
        }
        String ellipsis = "…";
        int bodyWidth = Math.max(1, safeWidth - font.width(ellipsis));
        return Component.literal(font.plainSubstrByWidth(text.getString(), bodyWidth)
                + ellipsis).getVisualOrderText();
    }
}
