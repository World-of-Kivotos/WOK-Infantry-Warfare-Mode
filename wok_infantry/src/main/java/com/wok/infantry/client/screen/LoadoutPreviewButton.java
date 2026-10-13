package com.wok.infantry.client.screen;

import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Tactical loadout card with the TaCZ HUD silhouette or a normal item fallback. The card
 * background comes from {@link TacticalButtonStyle#renderCard}: hover darkens the outline,
 * selection is blue with a light left stripe.
 */
final class LoadoutPreviewButton extends Button {
    private final LoadoutEntry entry;
    private final boolean selected;
    private final LoadoutEntryPreview.Preview preview;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();

    LoadoutPreviewButton(int x, int y, int width, int height,
                         LoadoutEntry entry, boolean selected, OnPress onPress) {
        super(x, y, width, height, Component.literal(entry.displayName()),
                onPress, DEFAULT_NARRATION);
        this.entry = entry;
        this.selected = selected;
        this.preview = LoadoutEntryPreview.resolve(entry);
    }

    static TacticalButtonStyle.CardState cardState(boolean selected, boolean active,
                                                   boolean hovered) {
        if (selected) {
            return TacticalButtonStyle.CardState.SELECTED;
        }
        if (!active) {
            return TacticalButtonStyle.CardState.DISABLED;
        }
        return hovered ? TacticalButtonStyle.CardState.HOVER
                : TacticalButtonStyle.CardState.NORMAL;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        TacticalButtonStyle.renderCard(graphics, left, top, right, bottom,
                cardState(selected, active, TacticalButtonStyle.hovered(this)));
        if (TacticalButtonStyle.keyboardFocused(this)) {
            TacticalButtonStyle.focusRing(graphics, left, top, right, bottom);
        }

        Font font = Minecraft.getInstance().font;
        int innerLeft = left + 4;
        int innerRight = right - 3;
        int innerWidth = Math.max(1, innerRight - innerLeft);
        int previewWidth = preview.available()
                ? Math.min(LoadoutEntryPreview.TACZ_HUD_WIDTH * 3,
                Math.max(LoadoutEntryPreview.TACZ_HUD_WIDTH, innerWidth / 3)) : 0;
        previewWidth = Math.min(previewWidth, Math.max(0, innerWidth - 24));
        if (previewWidth > 0) {
            graphics.fill(innerLeft, top + 2, innerLeft + previewWidth, bottom - 2,
                    selected ? TacticalBoardTheme.SELECT_WELL : TacticalBoardTheme.WELL);
            LoadoutEntryPreview.render(graphics, preview,
                    innerLeft + 2, top + 3, Math.max(1, previewWidth - 4),
                    Math.max(1, height - 6));
        }

        int textLeft = innerLeft + (previewWidth > 0 ? previewWidth + 4 : 0);
        int textWidth = Math.max(1, innerRight - textLeft);
        int textColor = selected ? TacticalBoardTheme.ON_SELECT
                : active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.DISABLED_TEXT;
        int textY = top + Math.max(0, (height - 8) / 2);
        boolean truncated = TextFit.draw(graphics, font, entry.displayName(), textLeft, textY,
                textWidth, textColor, TextFit.Align.LEFT).truncated();
        truncationTooltip.sync(this, getMessage(), truncated);
    }
}
