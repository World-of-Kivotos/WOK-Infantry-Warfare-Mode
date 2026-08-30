package com.wok.infantry.client.hud;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.screen.BattleUiTheme;
import com.wok.infantry.client.screen.SquadScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Comparator;
import java.util.List;

/** Compact always-on roster for the viewer's own eight-player squad. */
public final class SquadHudOverlay {
    private static final int PANEL_WIDTH = 166;
    private static final int ROW_HEIGHT = 17;

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width);

    private SquadHudOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("squad_roster", INSTANCE);
    }

    private static void render(GuiGraphics graphics, int screenWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        SquadView squad = ClientBattleState.ownSquad();
        if (minecraft.player == null || minecraft.options.hideGui || snapshot == null
                || squad == null || squad.members().isEmpty()) {
            return;
        }

        List<MemberView> members = squad.members().stream()
                .sorted(Comparator.comparing(MemberView::leader).reversed())
                .limit(8).toList();
        Font font = minecraft.font;
        int left = 8;
        int top = 42;
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(120, screenWidth / 3));
        int bottom = top + 18 + members.size() * ROW_HEIGHT + 3;
        graphics.fill(left, top, left + panelWidth, bottom, 0xA8121A20);
        BattleUiTheme.outline(graphics, left, top, left + panelWidth, bottom,
                0xCC4C5E6B);
        graphics.fill(left, top, left + 3, bottom, BattleUiTheme.FRIENDLY);

        Component heading = SquadScreen.callsign(squad.callsign()).copy().append("  ")
                .append(Component.translatable("hud.wok_infantry.squad_count",
                        members.size(), Math.max(snapshot.squadCapacity(), squad.capacity())));
        graphics.drawString(font, heading, left + 7, top + 5, BattleUiTheme.ACCENT, false);

        for (int index = 0; index < members.size(); index++) {
            drawMember(graphics, font, snapshot, index + 1, members.get(index), left + 6,
                    top + 18 + index * ROW_HEIGHT, panelWidth - 11);
        }
    }

    private static void drawMember(GuiGraphics graphics, Font font, BattleSnapshot snapshot,
                                   int index, MemberView member, int x, int y, int width) {
        int statusColor;
        if (!member.online()) {
            statusColor = 0xFF65727A;
        } else if (!member.alive()) {
            statusColor = BattleUiTheme.DANGER;
        } else {
            statusColor = BattleUiTheme.SUCCESS;
        }
        graphics.fill(x, y + 4, x + 4, y + 8, statusColor);

        String prefix = index + " "
                + (member.commander() ? "[CO] " : member.leader() ? "[SL] " : "");
        String classDisplayName = SquadScreen.className(snapshot, member.classId()).getString();
        int classWidth = Math.min(46, Math.max(24, font.width(classDisplayName) + 2));
        int nameWidth = Math.max(24, width - classWidth - 12);
        String memberDisplayName = font.plainSubstrByWidth(prefix + member.name(), nameWidth);
        classDisplayName = font.plainSubstrByWidth(classDisplayName,
                Math.max(1, classWidth - 2));
        int nameColor = member.online() ? BattleUiTheme.TEXT : BattleUiTheme.MUTED_TEXT;
        graphics.drawString(font, memberDisplayName, x + 7, y + 1, nameColor, false);
        graphics.drawString(font, classDisplayName, x + width - classWidth + 1, y + 1,
                BattleUiTheme.FRIENDLY, false);

        int barLeft = x + 7;
        int barRight = x + width;
        int barTop = y + 12;
        graphics.fill(barLeft, barTop, barRight, barTop + 2, 0xAA323B41);
        if (member.online() && member.alive()) {
            float ratio = Math.max(0.0F, Math.min(1.0F, member.health() / member.maxHealth()));
            int healthRight = barLeft + Math.round((barRight - barLeft) * ratio);
            int healthColor = ratio > 0.55F ? BattleUiTheme.SUCCESS
                    : ratio > 0.25F ? BattleUiTheme.ACCENT : BattleUiTheme.DANGER;
            graphics.fill(barLeft, barTop, healthRight, barTop + 2, healthColor);
        }
    }
}
