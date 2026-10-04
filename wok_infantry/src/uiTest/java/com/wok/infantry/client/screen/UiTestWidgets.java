package com.wok.infantry.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * uiTest-only bridge to the package-private WOK keys ({@link BattleUiButton}), so the component
 * gallery draws the very widgets the screens use. Lives in the uiTest source set and never ships.
 */
public final class UiTestWidgets {
    /** Kind of a battle key (mirrors {@code BattleUiButton.Kind}). */
    public enum Kind {
        NORMAL,
        CONTROL,
        DANGER,
        SUCCESS
    }

    private UiTestWidgets() {
    }

    /**
     * A battle key at {@code bounds}.
     *
     * @param icon     9×9 icon in front of the label, or {@code null}
     * @param iconOnly draw only the icon (the label stays the key's name and tooltip)
     */
    public static Button key(Component label, Button.OnPress onPress, UiRect bounds, Kind kind,
                             boolean selected, TacticalIcon icon, boolean iconOnly) {
        BattleUiButton.Builder builder = BattleUiButton.builder(label, onPress)
                .kind(switch (kind == null ? Kind.NORMAL : kind) {
                    case NORMAL -> BattleUiButton.Kind.NORMAL;
                    case CONTROL -> BattleUiButton.Kind.CONTROL;
                    case DANGER -> BattleUiButton.Kind.DANGER;
                    case SUCCESS -> BattleUiButton.Kind.SUCCESS;
                })
                .selected(selected);
        if (icon != null) {
            if (iconOnly) {
                builder.iconOnly(icon);
            } else {
                builder.icon(icon);
            }
        }
        builder.bounds(bounds.left(), bounds.top(), bounds.width(), bounds.height());
        return builder.build();
    }
}
