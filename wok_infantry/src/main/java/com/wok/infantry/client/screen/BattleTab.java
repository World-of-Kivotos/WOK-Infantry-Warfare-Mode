package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The six pages of the battle terminal, in the preview's {@code BATTLE_TABS} order (the
 * formation page is appended so the older indexes stay valid).
 */
public enum BattleTab {
    SQUADS("squads", "screen.wok_infantry.tab.squads", "screen.wok_infantry.tab.squads_short"),
    CLASSES("classes", "screen.wok_infantry.tab.classes", "screen.wok_infantry.tab.classes_short"),
    DEPLOYMENT("deployment", "screen.wok_infantry.tab.deployment",
            "screen.wok_infantry.tab.deployment_short"),
    LOADOUT("loadout", "screen.wok_infantry.tab.loadout", "screen.wok_infantry.tab.loadout_short"),
    MAP("map", "screen.wok_infantry.tab.map", "screen.wok_infantry.tab.map_short"),
    FORMATION("formation", "screen.wok_infantry.tab.formation",
            "screen.wok_infantry.tab.formation_short");

    private final String id;
    private final String labelKey;
    private final String shortKey;

    BattleTab(String id, String labelKey, String shortKey) {
        this.id = id;
        this.labelKey = labelKey;
        this.shortKey = shortKey;
    }

    public String id() {
        return id;
    }

    public Component label() {
        return Component.translatable(labelKey);
    }

    public Component shortLabel() {
        return Component.translatable(shortKey);
    }

    /** Enabled tab entry for a {@link TacticalTabStrip}. */
    public TacticalTabStrip.Tab tab() {
        return TacticalTabStrip.Tab.of(id, label(), shortLabel());
    }

    /** Tab with the given id, or {@code null}. */
    public static BattleTab byId(String id) {
        for (BattleTab tab : values()) {
            if (tab.id.equals(id)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * All six tabs; {@code disabledReason} returns the reason a tab is greyed out (for example
     * "编制锁定后开放") or {@code null} when it is available.
     */
    public static List<TacticalTabStrip.Tab> tabs(Function<BattleTab, Component> disabledReason) {
        List<TacticalTabStrip.Tab> tabs = new ArrayList<>(values().length);
        for (BattleTab tab : values()) {
            Component reason = disabledReason == null ? null : disabledReason.apply(tab);
            tabs.add(reason == null ? tab.tab() : tab.tab().withDisabledReason(reason));
        }
        return tabs;
    }

    /**
     * Header tab strip of the battle terminal with {@code current} selected. {@code onSelect}
     * receives the requested tab; screens usually switch in place or call
     * {@link BattleTerminalNav#navigate}.
     */
    public static TacticalTabStrip strip(BattleTab current,
                                         Function<BattleTab, Component> disabledReason,
                                         Consumer<BattleTab> onSelect) {
        return new TacticalTabStrip(TacticalTabStrip.Skin.HEADER, tabs(disabledReason),
                current == null ? 0 : current.ordinal(),
                index -> {
                    if (onSelect != null && index >= 0 && index < values().length) {
                        onSelect.accept(values()[index]);
                    }
                });
    }
}
