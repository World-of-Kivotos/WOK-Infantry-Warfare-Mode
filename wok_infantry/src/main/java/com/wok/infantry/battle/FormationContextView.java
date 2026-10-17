package com.wok.infantry.battle;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Public names of the viewer's battle context (battle protocol 20): the formation the viewer's
 * faction plays this round, its default class, and both public factions with their player caps.
 * Display only; the server never trusts any of these values coming back.
 *
 * <p>{@code formationId} is empty while the viewer has no formation, which during a round means
 * the faction's formation vote is not locked yet. Names are clipped and stripped of control
 * characters by the constructor, so an over-long administrator name can never make a snapshot
 * unencodable (the 0.3.0-beta.7 login kick).
 *
 * @param factionCapacity      the own public faction's player cap ({@code maxPlayers})
 * @param enemyFactionCapacity the opposing public faction's player cap
 */
public record FormationContextView(
        String formationId,
        String formationName,
        String defaultClassId,
        String factionId,
        String factionName,
        int factionCapacity,
        String enemyFactionId,
        String enemyFactionName,
        int enemyFactionCapacity
) {
    /** Same bound as formation, faction and class ids in the catalog. */
    public static final int MAX_ID_LENGTH = 64;
    /** Same bound as formation and faction display names in the catalog. */
    public static final int MAX_NAME_LENGTH = 40;
    /** Wire bound for both capacities. */
    public static final int MAX_CAPACITY = 128;
    // Declared before EMPTY: the constructor below needs it during static initialization.
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]{1,64}");
    public static final FormationContextView EMPTY = new FormationContextView(
            "", "", "", "", "", 0, "", "", 0);

    public FormationContextView {
        formationId = sanitizeId(formationId);
        formationName = sanitizeName(formationName);
        defaultClassId = sanitizeId(defaultClassId);
        factionId = sanitizeId(factionId);
        factionName = sanitizeName(factionName);
        factionCapacity = clampCapacity(factionCapacity);
        enemyFactionId = sanitizeId(enemyFactionId);
        enemyFactionName = sanitizeName(enemyFactionName);
        enemyFactionCapacity = clampCapacity(enemyFactionCapacity);
    }

    /** The faction's shared formation is known (locked, or assigned by an administrator). */
    public boolean hasFormation() {
        return !formationId.isEmpty();
    }

    /** The viewer's public faction is known. */
    public boolean hasFaction() {
        return !factionId.isEmpty();
    }

    /** Lower-case catalog id, or "" when absent or malformed. */
    public static String sanitizeId(String input) {
        if (input == null) {
            return "";
        }
        String value = input.strip().toLowerCase(Locale.ROOT);
        return ID_PATTERN.matcher(value).matches() ? value : "";
    }

    /** Drops control characters and clips to {@link #MAX_NAME_LENGTH} UTF-16 code units. */
    public static String sanitizeName(String input) {
        String value = Objects.requireNonNullElse(input, "").strip();
        StringBuilder result = new StringBuilder(Math.min(value.length(), MAX_NAME_LENGTH));
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            int characterCount = Character.charCount(codePoint);
            offset += characterCount;
            if (Character.isISOControl(codePoint)) {
                continue;
            }
            if (result.length() + characterCount > MAX_NAME_LENGTH) {
                break;
            }
            result.appendCodePoint(codePoint);
        }
        return result.toString().strip();
    }

    private static int clampCapacity(int capacity) {
        return Math.max(0, Math.min(MAX_CAPACITY, capacity));
    }
}
