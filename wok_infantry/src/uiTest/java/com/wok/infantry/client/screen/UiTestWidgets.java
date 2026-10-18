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

    /**
     * Geometry the ammunition board drew this frame (player-01 regression): the remaining-points
     * text, its meter, the section header and the panel, as {@code [left, top, right, bottom]}
     * in the screen's GUI coordinates, keyed {@code points}, {@code meter}, {@code section},
     * {@code panel} and {@code list}.
     */
    public static java.util.Map<String, int[]> ammoSupplyGeometry(AmmoSupplyScreen screen)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = AmmoSupplyScreen.class.getDeclaredField("supplyLayout");
        field.setAccessible(true);
        AmmoSupplyLayout layout = (AmmoSupplyLayout) field.get(screen);
        java.util.Map<String, int[]> geometry = new java.util.LinkedHashMap<>();
        geometry.put("points", rect(layout.pointsText()));
        geometry.put("meter", rect(layout.meterBar()));
        geometry.put("section", rect(layout.sectionHeader()));
        geometry.put("panel", rect(layout.panel()));
        geometry.put("list", rect(layout.list()));
        return geometry;
    }

    /** Whether the viewer of the formation page has joined a faction (the page then has tabs). */
    public static boolean formationJoined(FormationSelectionScreen screen) {
        return screen.snapshot() != null && screen.model().joined();
    }

    /**
     * The header identity the formation page hands to its shell this frame, e.g.
     * "学院军 · 阿尔法小队 · 指挥官" (the shell may still hide it when the header is full).
     */
    public static String formationIdentity(FormationSelectionScreen screen) {
        return FormationText.identity(screen.model(),
                com.wok.infantry.client.ClientBattleState.snapshot()).getString();
    }

    /**
     * Pagination of the squad screen's point list as {@code [page, pageCount, start, end]}, or
     * {@code null} when the deployment page is not shown (squad terminal, 0.5.0-beta.1).
     */
    public static int[] squadPointPage(SquadScreen screen) {
        SquadBoardModel.Page page = screen.shownPointPage();
        return page == null ? null : new int[]{page.page(), page.pageCount(), page.start(),
                page.end()};
    }

    /** Pagination of the squad screen's class list, like {@link #squadPointPage}. */
    public static int[] squadClassPage(SquadScreen screen) {
        SquadBoardModel.Page page = screen.shownClassPage();
        return page == null ? null : new int[]{page.page(), page.pageCount(), page.start(),
                page.end()};
    }

    /**
     * Swappable theme tokens whose {@link TacticalBoardTheme} field does not hold its A value
     * right now. Outside every tablet frame the list must be empty: a faction palette that
     * outlived its scope would recolour the HUD (plan 10, "调色板漏进 HUD").
     */
    public static java.util.List<String> paletteLeaks() {
        java.util.List<String> leaks = new java.util.ArrayList<>();
        for (PaletteToken token : PaletteToken.values()) {
            int value = TacticalPalette.themeValue(token);
            if (value != TacticalPalette.A.get(token)) {
                leaks.add(token.name() + "=" + String.format(java.util.Locale.ROOT, "%08X", value));
            }
        }
        return leaks;
    }

    /** The livery {@code screen} resolved for its last drawn frame. */
    public static TacticalLivery.Livery frameLivery(TacticalScreen screen) {
        return screen.frameLivery();
    }

    private static int[] rect(TacticalMapLayout.Rect rect) {
        return new int[]{rect.left(), rect.top(), rect.right(), rect.bottom()};
    }
}
