package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Board key of the map and formation surfaces, drawn from the shared
 * {@link TacticalButtonStyle} state table. Keeps the vanilla input, focus, tooltip and
 * narration behaviour.
 */
final class TacticalBoardButton extends Button {
    enum Kind {
        NAVIGATION,
        CONTROL,
        TOGGLE,
        TOOL,
        DANGER
    }

    private final Kind kind;
    private final boolean engaged;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();
    private boolean labelTruncated;

    /**
     * @param engaged     the key shows the current selection (selected tab, tool, layer...)
     * @param accentColor no longer used: selection is always blue with a light stripe; kept so
     *                    existing screens compile unchanged until they migrate
     */
    TacticalBoardButton(int x, int y, int width, int height, Component message,
                        OnPress onPress, Kind kind, boolean engaged, int accentColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind == null ? Kind.NAVIGATION : kind;
        this.engaged = engaged;
    }

    /**
     * Look of a board key. When an engaged key is not clickable only {@link Kind#NAVIGATION}
     * (the current page tab) keeps the selected look; every other kind is drawn as disabled,
     * so e.g. a highlighted formation row outside the voting phase no longer looks selectable.
     */
    static TacticalButtonStyle.Look look(Kind kind, boolean engaged, boolean active,
                                         boolean hovered) {
        boolean current = engaged && !active && kind == Kind.NAVIGATION;
        return TacticalButtonStyle.resolve(active, engaged, current, variant(kind), hovered,
                false);
    }

    static TacticalButtonStyle.Variant variant(Kind kind) {
        if (kind == null) {
            return TacticalButtonStyle.Variant.NORMAL;
        }
        return switch (kind) {
            case NAVIGATION, TOGGLE, TOOL -> TacticalButtonStyle.Variant.NORMAL;
            case CONTROL -> TacticalButtonStyle.Variant.CONTROL;
            case DANGER -> TacticalButtonStyle.Variant.DANGER;
        };
    }

    /** True when the last drawn label was shortened with an ellipsis. */
    boolean labelTruncated() {
        return labelTruncated;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        TacticalButtonStyle.Look look = look(kind, engaged, active,
                TacticalButtonStyle.hovered(this));
        TacticalButtonStyle.Options options = TacticalButtonStyle.Options.DEFAULT
                .withFocusRing(TacticalButtonStyle.keyboardFocused(this));
        TextFit.Fitted fitted = TacticalButtonStyle.render(graphics, Minecraft.getInstance().font,
                getX(), getY(), getX() + width, getY() + height, getMessage(), look, options);
        labelTruncated = fitted.truncated();
        truncationTooltip.sync(this, getMessage(), labelTruncated);
    }
}
