package com.wok.infantry.client.screen;

import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Tactical loadout card with the TaCZ HUD silhouette or a normal item fallback. */
final class LoadoutPreviewButton extends Button {
    private final LoadoutEntry entry;
    private final boolean selected;
    private final LoadoutEntryPreview.Preview preview;

    LoadoutPreviewButton(int x, int y, int width, int height,
                         LoadoutEntry entry, boolean selected, OnPress onPress) {
        super(x, y, width, height, Component.literal(entry.displayName()),
                onPress, DEFAULT_NARRATION);
        this.entry = entry;
        this.selected = selected;
        this.preview = LoadoutEntryPreview.resolve(entry);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        int fillColor;
        if (selected) {
            fillColor = isHoveredOrFocused()
                    ? TacticalBoardTheme.SELECTED_HOVER : TacticalBoardTheme.SELECTED;
        } else if (!active) {
            fillColor = TacticalBoardTheme.CARD_DISABLED;
        } else if (isHoveredOrFocused()) {
            fillColor = TacticalBoardTheme.CARD_HOVER;
        } else {
            fillColor = TacticalBoardTheme.CARD;
        }
        graphics.fill(left, top, right, bottom, fillColor);
        BattleUiTheme.outline(graphics, left, top, right, bottom,
                selected ? TacticalBoardTheme.SELECTED
                        : isHoveredOrFocused() ? TacticalBoardTheme.ACCENT
                        : TacticalBoardTheme.BORDER);
        if (selected) {
            graphics.fill(left + 1, top + 1, left + 4, bottom - 1,
                    TacticalBoardTheme.ACCENT);
        }

        Font font = Minecraft.getInstance().font;
        int innerLeft = left + (selected ? 6 : 3);
        int innerRight = right - 3;
        int innerWidth = Math.max(1, innerRight - innerLeft);
        int previewWidth = preview.available()
                ? Math.min(LoadoutEntryPreview.TACZ_HUD_WIDTH * 3,
                Math.max(LoadoutEntryPreview.TACZ_HUD_WIDTH, innerWidth / 3)) : 0;
        previewWidth = Math.min(previewWidth, Math.max(0, innerWidth - 24));
        if (previewWidth > 0) {
            graphics.fill(innerLeft, top + 2, innerLeft + previewWidth, bottom - 2,
                    TacticalBoardTheme.DEVICE_FRAME);
            BattleUiTheme.outline(graphics, innerLeft, top + 2,
                    innerLeft + previewWidth, bottom - 2, TacticalBoardTheme.BORDER);
            LoadoutEntryPreview.render(graphics, preview,
                    innerLeft + 2, top + 3, Math.max(1, previewWidth - 4),
                    Math.max(1, height - 6));
        }

        int textLeft = innerLeft + (previewWidth > 0 ? previewWidth + 3 : 0);
        int textWidth = Math.max(1, innerRight - textLeft);
        String label = font.plainSubstrByWidth(entry.displayName(), textWidth);
        int textColor = selected ? TacticalBoardTheme.LIGHT_TEXT
                : active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED_TEXT;
        int textY = top + Math.max(0, (height - font.lineHeight) / 2) + 1;
        graphics.drawString(font, label, textLeft, textY, textColor, false);
    }
}
