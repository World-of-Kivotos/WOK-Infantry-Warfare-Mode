package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.UiRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Always-on roster of the viewer's own squad (preview {@code HUDP.roster}, full form): squad
 * name and members over the squad's own capacity in the title, then one row per member with a
 * status dot, the map number, role tags, the name and the health-or-status column. The viewer's
 * row has a light blue wash. Drawn below the vanilla F3, chat and player list, and shrunk to its
 * title row while they need the room (see {@link WokHudLayout#rosterPresence}).
 */
public final class SquadHudOverlay {
    /** Overlay id {@code wok_infantry:squad_roster}; kept so packs can hide it by id. */
    public static final String ID = "squad_roster";

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private SquadHudOverlay() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), ID, INSTANCE);
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        HudFrame frame = HudFrame.current(width, height);
        if (frame == null || frame.hidden() || frame.roster() == null
                || frame.layout().roster() == null) {
            return;
        }
        HudPaint.begin(graphics, frame.factor());
        try {
            draw(graphics, Minecraft.getInstance().font, frame.layout().roster(), frame.roster(),
                    frame.layout().tight(),
                    frame.rosterPresence() == WokHudLayout.RosterPresence.COLLAPSED);
        } finally {
            HudPaint.end(graphics);
        }
    }

    static void draw(GuiGraphics graphics, Font font, UiRect rect, SquadRosterModel.Roster roster,
                     boolean tight, boolean collapsed) {
        int left = rect.left();
        int top = rect.top();
        TacticalHud.plate(graphics, left, top, rect.right(), rect.bottom(), TacticalHud.Edge.LEFT,
                TacticalBoardTheme.HUD_FRIENDLY, false);
        int titleY = top + 2 + (tight ? 0 : 1);
        graphics.drawString(font, roster.title(), left + SquadRosterModel.TITLE_X, titleY,
                TacticalBoardTheme.SECTION_B, false);
        TacticalHud.readout(graphics, font, roster.count(), left + roster.countX(), titleY,
                TacticalBoardTheme.LIGHT_MUTED);
        if (collapsed) {
            return;
        }
        int header = tight ? WokHudLayout.HEADER_HEIGHT_TIGHT : WokHudLayout.HEADER_HEIGHT;
        int rowHeight = tight ? WokHudLayout.ROW_HEIGHT_TIGHT : WokHudLayout.ROW_HEIGHT;
        for (SquadRosterModel.Row row : roster.rows()) {
            int y = top + header + (row.number() - 1) * rowHeight + 1;
            int textY = y + (tight ? 0 : 1);
            if (row.self()) {
                graphics.fill(left + 2, y, rect.right() - 1, y + rowHeight,
                        TacticalHud.SELF_ROW_TINT);
            }
            TacticalHud.statusDot(graphics, left + SquadRosterModel.DOT_X, textY + 2,
                    row.dotColor(), row.hollow());
            graphics.drawString(font, String.valueOf(row.number()),
                    left + SquadRosterModel.NUMBER_X, textY, TacticalBoardTheme.LIGHT_MUTED, false);
            if (!row.role().isEmpty()) {
                graphics.drawString(font, row.role(), left + SquadRosterModel.NAME_X, textY,
                        TacticalBoardTheme.SECTION_B, false);
            }
            graphics.drawString(font, row.name(), left + row.nameX(), textY, row.nameColor(),
                    false);
            if (row.hasTag()) {
                graphics.drawString(font, row.tag(), left + roster.barLeft(), textY,
                        row.tagColor(), false);
                continue;
            }
            if (row.healthBar()) {
                TacticalHud.meter(graphics, left + roster.barLeft(), textY + 2,
                        left + roster.barRight(), textY + 5, row.healthRatio(),
                        TacticalHud.healthColor(row.healthRatio()));
            }
            graphics.drawString(font, row.className(), left + roster.columnLeft(), textY,
                    TacticalBoardTheme.LIGHT_MUTED, false);
        }
    }
}
