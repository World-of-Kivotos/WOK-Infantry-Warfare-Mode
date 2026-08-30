package com.wok.infantry.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassQuotaViewTest {
    @Test
    void displayNameIsTrimmedAndControlCharactersAreRemoved() {
        ClassQuotaView quota = new ClassQuotaView("medic", "  医\n疗\u0000兵  ", 2, 1);

        assertEquals("医疗兵", quota.displayName());
        assertEquals(1, quota.remaining());
    }

    @Test
    void utf16LimitNeverSplitsSupplementaryCodePoint() {
        String twentyOneEmoji = "😀".repeat(21);
        String sanitized = ClassQuotaView.sanitizeDisplayName(twentyOneEmoji);

        assertEquals(BattleRules.MAX_CLASS_DISPLAY_NAME_LENGTH, sanitized.length());
        assertEquals(20, sanitized.codePointCount(0, sanitized.length()));
        assertEquals("😀".repeat(20), sanitized);
    }

    @Test
    void nullValuesUseSafeFallbacksAndRemainingNeverGoesNegative() {
        ClassQuotaView quota = new ClassQuotaView(null, null, 1, 3);

        assertEquals(BattleRules.DEFAULT_CLASS_ID, quota.classId());
        assertEquals("", quota.displayName());
        assertEquals(0, quota.remaining());
    }

    @Test
    void negativeQuotaValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassQuotaView("medic", "Medic", -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ClassQuotaView("medic", "Medic", 1, -1));
    }
}
