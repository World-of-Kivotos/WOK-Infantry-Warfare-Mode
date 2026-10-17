package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Tactical-tablet key drawn from the shared {@link TacticalButtonStyle} state table. Labels that
 * do not fit end in "…" and, unless the screen set its own tooltip, show the full text as a
 * tooltip. Input, keyboard focus, tooltip and narration behaviour remain the vanilla button's.
 */
final class BattleUiButton extends Button {
    enum Kind {
        NORMAL,
        CONTROL,
        DANGER,
        SUCCESS
    }

    private final boolean selected;
    private final boolean current;
    private final boolean armed;
    private final Kind kind;
    private final Component badge;
    private final int badgeColor;
    private final TextFit.Align align;
    private final TacticalIcon icon;
    private final boolean iconOnly;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();
    private boolean labelTruncated;

    private BattleUiButton(Button.Builder builder, Builder options) {
        super(builder);
        this.selected = options.selected;
        this.current = options.current;
        this.armed = options.armed;
        this.kind = options.kind;
        this.badge = options.badge;
        this.badgeColor = options.badgeColor;
        this.align = options.align;
        this.icon = options.icon;
        this.iconOnly = options.iconOnly;
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    /**
     * Look of a battle key. A selected key that is not clickable (current tab, current class,
     * current deployment point) is drawn as "current" instead of disabled.
     */
    static TacticalButtonStyle.Look look(Kind kind, boolean selected, boolean current,
                                         boolean active, boolean hovered, boolean armed) {
        return TacticalButtonStyle.resolve(active, selected, current || selected && !active,
                variant(kind), hovered, armed);
    }

    static TacticalButtonStyle.Variant variant(Kind kind) {
        if (kind == null) {
            return TacticalButtonStyle.Variant.NORMAL;
        }
        return switch (kind) {
            case NORMAL -> TacticalButtonStyle.Variant.NORMAL;
            case CONTROL -> TacticalButtonStyle.Variant.CONTROL;
            case DANGER -> TacticalButtonStyle.Variant.DANGER;
            case SUCCESS -> TacticalButtonStyle.Variant.SUCCESS;
        };
    }

    /** True when the last drawn label was shortened with an ellipsis. */
    boolean labelTruncated() {
        return labelTruncated;
    }

    /** Badge colour as built; {@link TacticalButtonStyle#NONE} = muted, resolved when drawn. */
    int badgeColor() {
        return badgeColor;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        TacticalButtonStyle.Look look = look(kind, selected, current, active,
                TacticalButtonStyle.hovered(this), armed);
        TacticalButtonStyle.Options options = new TacticalButtonStyle.Options(align, badge,
                badgeColor, TacticalButtonStyle.keyboardFocused(this), icon, iconOnly);
        TextFit.Fitted fitted = TacticalButtonStyle.render(graphics, Minecraft.getInstance().font,
                getX(), getY(), getX() + width, getY() + height, getMessage(), look, options);
        labelTruncated = fitted.truncated();
        truncationTooltip.sync(this, getMessage(), labelTruncated);
        UiLayoutProbe.widget(graphics, this, "button", look.state().name(), labelTruncated,
                options.focusRing());
    }

    /** Draws the label centred, shadowless and ellipsized; no scrolling marquee. */
    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int left = getX() + 3;
        int room = Math.max(0, getWidth() - 6);
        int textY = getY() + Math.max(0, (getHeight() - 8) / 2);
        labelTruncated = TextFit.draw(graphics, font, getMessage(), left, textY, room, color,
                TextFit.Align.CENTER).truncated();
    }

    static final class Builder extends Button.Builder {
        private boolean selected;
        private boolean current;
        private boolean armed;
        private Kind kind = Kind.NORMAL;
        private Component badge;
        /** {@link TacticalButtonStyle#NONE}: MUTED of the palette active while the key is drawn. */
        private int badgeColor = TacticalButtonStyle.NONE;
        private TextFit.Align align = TextFit.Align.CENTER;
        private TacticalIcon icon;
        private boolean iconOnly;

        private Builder(Component message, OnPress onPress) {
            super(message, onPress);
        }

        /** 9×9 icon in front of the label, drawn in the label colour; null for none. */
        Builder icon(TacticalIcon icon) {
            this.icon = icon;
            this.iconOnly = false;
            return this;
        }

        /**
         * Icon-only key (pager arrows, steppers, close): only the icon is drawn, centred. The
         * message stays the key's name for narration and is shown as its tooltip unless the
         * screen sets its own.
         */
        Builder iconOnly(TacticalIcon icon) {
            this.icon = icon;
            this.iconOnly = icon != null;
            return this;
        }

        /**
         * Shows the current selection (faction colour with the light bar). Selected but inactive
         * keys draw as "current".
         */
        Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        /** Selected entry that is deliberately not clickable; never reacts to hover. */
        Builder current(boolean current) {
            this.current = current;
            return this;
        }

        Builder kind(Kind kind) {
            this.kind = kind == null ? Kind.NORMAL : kind;
            return this;
        }

        /** Danger key in its confirmation step: whole key red, dark hazard stripes, light text. */
        Builder armed(boolean armed) {
            this.armed = armed;
            return this;
        }

        /**
         * Small right-aligned tag such as a count; drawn on a faint wash. A {@code color} of
         * {@link TacticalButtonStyle#NONE} is the muted text colour of the palette active while
         * the key is drawn; a selected, success, armed or disabled key overrides the colour so
         * the count stays readable on its fill.
         */
        Builder badge(Component badge, int color) {
            this.badge = badge;
            this.badgeColor = color;
            return this;
        }

        Builder align(TextFit.Align align) {
            this.align = align == null ? TextFit.Align.CENTER : align;
            return this;
        }

        /**
         * @deprecated the selected stripe is always {@link TacticalBoardTheme#SELECT_BAR} now;
         * the colour passed here is ignored.
         */
        @Deprecated
        Builder accentColor(int accentColor) {
            return this;
        }

        @Override
        public BattleUiButton build() {
            return (BattleUiButton) super.build(builder -> new BattleUiButton(builder, this));
        }
    }
}
