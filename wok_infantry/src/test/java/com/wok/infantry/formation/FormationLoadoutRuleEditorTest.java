package com.wok.infantry.formation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FormationLoadoutRuleEditorTest {
    @Test
    void capturedEntryTurnsAllowAllIntoStrictFormationListAndThenAppends() {
        FormationConfigData original = FormationConfigData.defaultConfig();

        FormationConfigData first = FormationLoadoutRuleEditor.apply(original,
                "academy", "default", "assault", "primary", "m4a1",
                FormationLoadoutEditAction.INCLUDE_CAPTURED);
        FormationConfigData second = FormationLoadoutRuleEditor.apply(first,
                "academy", "default", "assault", "primary", "m4a1_acog",
                FormationLoadoutEditAction.INCLUDE_CAPTURED);

        assertTrue(rule(original).allowedEntriesFor("primary").isEmpty());
        assertEquals(List.of("m4a1"), rule(first).allowedEntriesFor("primary"));
        assertEquals(List.of("m4a1", "m4a1_acog"),
                rule(second).allowedEntriesFor("primary"));
    }

    @Test
    void strictListSupportsRemovalAndExplicitAllowAllReset() {
        FormationConfigData strict = FormationLoadoutRuleEditor.apply(
                FormationLoadoutRuleEditor.apply(FormationConfigData.defaultConfig(),
                        "academy", "default", "assault", "primary", "m4a1",
                        FormationLoadoutEditAction.INCLUDE_CAPTURED),
                "academy", "default", "assault", "primary", "m4a1_acog",
                FormationLoadoutEditAction.ADD);

        FormationConfigData removed = FormationLoadoutRuleEditor.apply(strict,
                "academy", "default", "assault", "primary", "m4a1",
                FormationLoadoutEditAction.REMOVE);
        assertEquals(List.of("m4a1_acog"), rule(removed).allowedEntriesFor("primary"));
        assertThrows(IllegalArgumentException.class, () -> FormationLoadoutRuleEditor.apply(
                removed, "academy", "default", "assault", "primary", "m4a1_acog",
                FormationLoadoutEditAction.REMOVE));

        FormationConfigData all = FormationLoadoutRuleEditor.apply(removed,
                "academy", "default", "assault", "primary", "",
                FormationLoadoutEditAction.ALLOW_ALL);
        assertTrue(rule(all).allowedEntriesFor("primary").isEmpty());
        assertTrue(rule(all).allowsEntry("primary", "any_global_entry"));
    }

    private static FormationClassRule rule(FormationConfigData config) {
        return config.findFormation("academy", "default").orElseThrow()
                .findClass("assault").orElseThrow();
    }
}
