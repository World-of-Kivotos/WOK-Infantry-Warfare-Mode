package com.wok.infantry.client.screen;

import com.wok.infantry.loadout.LoadoutClassDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdminLoadoutSelectionTest {
    @Test
    void classSettingsOpenOnlyWithFormationAndRule() {
        assertEquals(AdminLoadoutSelection.ClassSettingsBlock.NONE,
                AdminLoadoutSelection.classSettingsBlock(true, 3, true));
        assertNull(AdminLoadoutSelection.classSettingsReasonKey(
                AdminLoadoutSelection.ClassSettingsBlock.NONE, "突击兵"));
        // admin-01: a formation without a rule for the selected profession used to open an
        // empty page whose renderer dereferenced fields that were never created.
        AdminLoadoutSelection.ClassSettingsBlock missingRule =
                AdminLoadoutSelection.classSettingsBlock(true, 3, false);
        assertTrue(missingRule.blocked());
        assertEquals(AdminLoadoutSelection.CLASS_SETTINGS_PREFIX + "no_rule",
                AdminLoadoutSelection.classSettingsReasonKey(missingRule, "突击兵"),
                "the reason names the class");
        assertEquals(AdminLoadoutSelection.CLASS_SETTINGS_PREFIX + "no_rule_unnamed",
                AdminLoadoutSelection.classSettingsReasonKey(missingRule, " "));
        assertEquals(AdminLoadoutSelection.CLASS_SETTINGS_PREFIX + "no_rule_unnamed",
                AdminLoadoutSelection.classSettingsReasonKey(missingRule, null));
        assertEquals(AdminLoadoutSelection.ClassSettingsBlock.NO_RULES,
                AdminLoadoutSelection.classSettingsBlock(true, 0, false));
        assertEquals(AdminLoadoutSelection.ClassSettingsBlock.NO_FORMATION,
                AdminLoadoutSelection.classSettingsBlock(false, 0, false));
        // Review fix UI-06: every reason is a language key (en_us and zh_cn), never a literal.
        for (AdminLoadoutSelection.ClassSettingsBlock block
                : AdminLoadoutSelection.ClassSettingsBlock.values()) {
            String key = AdminLoadoutSelection.classSettingsReasonKey(block, "突击兵");
            assertEquals(block.blocked(), key != null, block::name);
            if (key != null) {
                assertTrue(key.startsWith("screen.wok_infantry."), key);
            }
        }
    }

    @Test
    void slotPageFollowsTheSelectedSlotWhenAsked() {
        // 9 slots, 4 tabs per page: slot 6 lives on page 1.
        assertEquals(1, AdminLoadoutSelection.slotPage(6, 4, 9, 0, true));
        assertEquals(2, AdminLoadoutSelection.slotPage(8, 4, 9, 0, true));
        assertEquals(0, AdminLoadoutSelection.slotPage(0, 4, 9, 2, true));
    }

    @Test
    void pagingKeepsThePageTheAdministratorFlippedTo() {
        // admin-07: flipping tabs must not jump back to (or change) the selected slot.
        assertEquals(2, AdminLoadoutSelection.slotPage(1, 4, 9, 2, false));
        assertEquals(2, AdminLoadoutSelection.slotPage(1, 4, 9, 7, false),
                "an out-of-range page is clamped to the last page");
        assertEquals(0, AdminLoadoutSelection.slotPage(1, 4, 9, -3, false));
    }

    @Test
    void unknownSelectionKeepsTheCurrentPageWithinBounds() {
        assertEquals(1, AdminLoadoutSelection.slotPage(-1, 4, 9, 1, true));
        assertEquals(0, AdminLoadoutSelection.slotPage(-1, 4, 0, 3, true));
        assertEquals(0, AdminLoadoutSelection.slotPage(5, 0, 3, 0, true),
                "a degenerate tab width still yields a valid page");
    }

    @Test
    void createdIdIsSelectedOnlyAfterTheRefreshContainsIt() {
        Set<String> refreshed = Set.of("primary", "custom_new");
        assertEquals("custom_new", AdminLoadoutSelection.confirmedSelection("custom_new",
                refreshed::contains, "primary"));
        // admin-04: a rejected create keeps the old selection instead of an id that does
        // not exist.
        assertEquals("primary", AdminLoadoutSelection.confirmedSelection("custom_missing",
                refreshed::contains, "primary"));
        assertEquals("primary", AdminLoadoutSelection.confirmedSelection(null,
                refreshed::contains, "primary"));
        assertEquals("primary", AdminLoadoutSelection.confirmedSelection(" ",
                refreshed::contains, "primary"));
    }

    @Test
    void slotIdFilterMatchesTheServerValidation() {
        assertTrue(AdminLoadoutSelection.slotIdDraft(""), "an empty draft is allowed");
        for (String accepted : new String[]{"primary", "custom_1a2b", "side-arm.2", "a_b-c.d"}) {
            assertTrue(AdminLoadoutSelection.slotIdDraft(accepted), accepted);
            assertTrue(LoadoutClassDefinition.validSlotId(accepted), accepted);
        }
        for (String rejected : new String[]{"Primary", "主武器", "has space", "a/b", "x:y"}) {
            assertFalse(AdminLoadoutSelection.slotIdDraft(rejected), rejected);
            assertFalse(LoadoutClassDefinition.validSlotId(rejected), rejected);
        }
        String longest = "a".repeat(AdminLoadoutSelection.MAX_SLOT_ID_LENGTH);
        assertTrue(AdminLoadoutSelection.slotIdDraft(longest));
        assertFalse(AdminLoadoutSelection.slotIdDraft(longest + "a"));
        assertFalse(AdminLoadoutSelection.slotIdDraft(null));
    }
}
