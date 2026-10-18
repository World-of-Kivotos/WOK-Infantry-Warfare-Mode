package com.wok.infantry.formation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FormationClassEditorTest {
    @Test
    void displayNameIsOwnedByOneFormation() {
        FormationConfigData original = FormationConfigData.defaultConfig();

        FormationConfigData renamed = FormationClassEditor.apply(original,
                "academy", "default", "assault", "突破手", 8,
                FormationClassEditAction.UPDATE);

        assertEquals("突破手", rule(renamed, "academy", "default", "assault")
                .displayName());
        assertEquals("突击兵", rule(renamed, "caesar", "default", "assault")
                .displayName());
        assertEquals("突击兵", rule(original, "academy", "default", "assault")
                .displayName());
    }

    @Test
    void customProfessionCanBeCreatedUpdatedAndDeletedWithoutAffectingOthers() {
        FormationConfigData created = FormationClassEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "custom_breacher", "突破手", 2, FormationClassEditAction.CREATE);
        assertEquals("突破手", rule(created, "academy", "default", "custom_breacher")
                .displayName());
        assertTrue(created.findFormation("caesar", "default").orElseThrow()
                .findClass("custom_breacher").isEmpty());

        FormationConfigData restricted = FormationLoadoutRuleEditor.apply(created,
                "academy", "default", "custom_breacher", "primary", "short_rifle",
                FormationLoadoutEditAction.INCLUDE_CAPTURED);
        FormationConfigData updated = FormationClassEditor.apply(restricted,
                "academy", "default", "custom_breacher", "近战突破手", 1,
                FormationClassEditAction.UPDATE);
        assertEquals(List.of("short_rifle"),
                rule(updated, "academy", "default", "custom_breacher")
                        .allowedEntriesFor("primary"));

        FormationConfigData deleted = FormationClassEditor.apply(updated,
                "academy", "default", "custom_breacher", "", 1,
                FormationClassEditAction.DELETE);
        assertTrue(deleted.findFormation("academy", "default").orElseThrow()
                .findClass("custom_breacher").isEmpty());
    }

    @Test
    void everyBuiltInProfessionCanBeDeletedAfterAReplacementExists() {
        FormationConfigData edited = FormationClassEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "custom_breacher", "突破手", 1, FormationClassEditAction.CREATE);
        for (String classId : List.of("assault", "support", "engineer", "recon")) {
            edited = FormationClassEditor.apply(edited, "academy", "default",
                    classId, "", 1, FormationClassEditAction.DELETE);
        }

        FormationDefinition formation = edited.findFormation("academy", "default")
                .orElseThrow();
        assertEquals(List.of("custom_breacher"), formation.classes().stream()
                .map(FormationClassRule::classId).toList());
        assertEquals("custom_breacher", formation.defaultClass().orElseThrow().classId());
        formation.squads().forEach(squad -> assertEquals(squad.capacity(),
                squad.classLimit("custom_breacher", 0)));
        FormationConfigData finalEdited = edited;
        assertThrows(IllegalArgumentException.class, () -> FormationClassEditor.apply(finalEdited,
                "academy", "default", "custom_breacher", "", 1,
                FormationClassEditAction.DELETE));
    }

    @Test
    void currentDefaultProfessionCannotBeUnderProvisioned() {
        FormationConfigData original = FormationConfigData.defaultConfig();
        assertThrows(IllegalArgumentException.class, () -> FormationClassEditor.apply(original,
                "academy", "default", "assault", "突破手", 1,
                FormationClassEditAction.UPDATE));
    }

    @Test
    void limitedProfessionCanMoveAheadOfFallbackWithoutChangingItsQuota() {
        FormationConfigData reordered = FormationClassEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "support", "", 0, FormationClassEditAction.MOVE_UP);

        FormationDefinition formation = reordered.findFormation("academy", "default")
                .orElseThrow();
        assertEquals(List.of("support", "assault", "engineer", "recon"),
                formation.classes().stream().map(FormationClassRule::classId).toList());
        assertEquals(2, formation.findClass("support").orElseThrow().squadLimit());
        assertEquals("assault", formation.defaultClass().orElseThrow().classId());
        formation.squads().forEach(squad -> {
            assertEquals(2, squad.classLimit("support", 0));
            assertEquals(8, squad.classLimit("assault", 0));
        });
    }

    @Test
    void professionCanMoveDownAndCannotMovePastEitherBoundary() {
        FormationConfigData original = FormationConfigData.defaultConfig();
        FormationConfigData reordered = FormationClassEditor.apply(original,
                "academy", "default", "assault", "", 0,
                FormationClassEditAction.MOVE_DOWN);
        assertEquals(List.of("support", "assault", "engineer", "recon"),
                reordered.findFormation("academy", "default").orElseThrow().classes()
                        .stream().map(FormationClassRule::classId).toList());
        assertThrows(IllegalArgumentException.class, () -> FormationClassEditor.apply(original,
                "academy", "default", "assault", "", 0,
                FormationClassEditAction.MOVE_UP));
        assertThrows(IllegalArgumentException.class, () -> FormationClassEditor.apply(original,
                "academy", "default", "recon", "", 0,
                FormationClassEditAction.MOVE_DOWN));
    }

    private static FormationClassRule rule(FormationConfigData data, String faction,
                                           String formation, String classId) {
        return data.findFormation(faction, formation).orElseThrow()
                .findClass(classId).orElseThrow();
    }
}
