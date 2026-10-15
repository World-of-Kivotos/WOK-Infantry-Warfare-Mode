package com.wok.infantry.battle;

import java.util.Objects;

public record ClassQuotaView(String classId, String displayName, int limit, int used) {
    public ClassQuotaView {
        classId = Objects.requireNonNullElse(classId, BattleRules.DEFAULT_CLASS_ID);
        displayName = sanitizeDisplayName(displayName);
        if (limit < 0 || used < 0) {
            throw new IllegalArgumentException("Class quota values cannot be negative");
        }
    }

    /** Compatibility constructor for callers that want client-side translation/ID fallback. */
    public ClassQuotaView(String classId, int limit, int used) {
        this(classId, "", limit, used);
    }

    public int remaining() {
        return Math.max(0, limit - used);
    }

    /**
     * Keeps administrator-provided Unicode names safe for chat components and the bounded wire
     * codec. The limit is measured in UTF-16 code units, matching FriendlyByteBuf#writeUtf.
     */
    public static String sanitizeDisplayName(String input) {
        String value = Objects.requireNonNullElse(input, "").strip();
        StringBuilder result = new StringBuilder(Math.min(value.length(),
                BattleRules.MAX_CLASS_DISPLAY_NAME_LENGTH));
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            int characterCount = Character.charCount(codePoint);
            offset += characterCount;
            if (Character.isISOControl(codePoint)
                    || result.length() + characterCount > BattleRules.MAX_CLASS_DISPLAY_NAME_LENGTH) {
                continue;
            }
            result.appendCodePoint(codePoint);
        }
        return result.toString().strip();
    }
}
