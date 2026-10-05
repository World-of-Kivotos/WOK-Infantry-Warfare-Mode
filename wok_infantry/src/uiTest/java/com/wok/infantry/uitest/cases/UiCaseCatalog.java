package com.wok.infantry.uitest.cases;

import com.wok.infantry.uitest.UiCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Every UI acceptance case, in run order. Screen batches add their own {@code cases/*Cases.java}
 * file and one line here; the harness itself does not change.
 *
 * <p>The legacy cases come first because they finish the 14 baseline screenshots of the live flow
 * ({@code wok_ui_10}–{@code 14}) in their original order; the gallery follows.
 */
public final class UiCaseCatalog {
    private UiCaseCatalog() {
    }

    /** Cases that finish the 14 legacy screenshots (group {@code legacy}). */
    public static List<UiCase> legacy() {
        List<UiCase> cases = new ArrayList<>();
        cases.addAll(FormationCases.cases());
        cases.addAll(AdminCases.cases());
        return cases.stream().filter(uiCase -> UiCase.LEGACY_GROUP.equals(uiCase.group()))
                .toList();
    }

    /**
     * Every case after the legacy ones. The HUD cases come last: they replace client caches
     * (battle snapshot, tickets, stamina, ballot) until each case's cleanup restores them.
     */
    public static List<UiCase> surfaces() {
        List<UiCase> cases = new ArrayList<>();
        cases.addAll(KitCases.cases());
        cases.addAll(MapIconCases.cases());
        cases.addAll(FormationCases.cases());
        cases.addAll(AdminCases.cases());
        cases.addAll(AmmoCases.cases());
        cases.addAll(SquadCases.cases());
        cases.addAll(HudCases.cases());
        return cases.stream().filter(uiCase -> !UiCase.LEGACY_GROUP.equals(uiCase.group()))
                .toList();
    }
}
