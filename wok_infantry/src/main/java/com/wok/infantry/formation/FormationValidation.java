package com.wok.infantry.formation;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.SquadCallsign;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Shared bounded validation without a Minecraft or Forge runtime dependency. */
final class FormationValidation {
    static final int MAX_ID_LENGTH = 64;
    static final int MAX_CALLSIGN_LENGTH = 32;
    static final int MAX_ENTITY_ID_LENGTH = 128;
    static final int MAX_DISPLAY_NAME_LENGTH = 40;
    static final int MAX_DESCRIPTION_LENGTH = 512;
    static final int MAX_PLAYERS = BattleRules.FACTION_CAPACITY;
    static final int MAX_SQUAD_CAPACITY = BattleRules.SQUAD_CAPACITY;
    static final int MAX_SQUADS = SquadCallsign.values().length;
    static final double MAX_VEHICLE_OFFSET = 256.0D;

    private FormationValidation() {
    }

    static String id(String value, int maximumLength) {
        return identifier(value, maximumLength, false);
    }

    static String entryId(String value) {
        return identifier(value, MAX_ID_LENGTH, true);
    }

    static String callsign(String value) {
        return SquadCallsign.byId(value).map(SquadCallsign::id).orElse(null);
    }

    private static String identifier(String value, int maximumLength, boolean allowSlash) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            return null;
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            boolean valid = current >= 'a' && current <= 'z'
                    || current >= '0' && current <= '9'
                    || current == '_' || current == '-' || current == '.'
                    || allowSlash && current == '/';
            if (!valid) {
                return null;
            }
        }
        return normalized;
    }

    static String entityId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > MAX_ENTITY_ID_LENGTH) {
            return null;
        }
        int separator = normalized.indexOf(':');
        if (separator <= 0 || separator != normalized.lastIndexOf(':')
                || separator == normalized.length() - 1) {
            return null;
        }
        String namespace = normalized.substring(0, separator);
        String path = normalized.substring(separator + 1);
        if (id(namespace, MAX_ID_LENGTH) == null) {
            return null;
        }
        for (int index = 0; index < path.length(); index++) {
            char current = path.charAt(index);
            if (!(current >= 'a' && current <= 'z')
                    && !(current >= '0' && current <= '9')
                    && current != '_' && current != '-' && current != '.' && current != '/') {
                return null;
            }
        }
        return path.isEmpty() ? null : normalized;
    }

    static String optionalResourceId(String value, String path,
                                     List<FormationConfigDiagnostic> diagnostics) {
        String candidate = Objects.requireNonNullElse(value, "").trim();
        if (candidate.isEmpty()) {
            return "";
        }
        String normalized = entityId(candidate);
        if (normalized == null) {
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "invalid resource id removed");
            return "";
        }
        if (!normalized.equals(candidate)) {
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "resource id normalized to " + normalized);
        }
        return normalized;
    }

    static String displayName(String value, String fallback, String path,
                              List<FormationConfigDiagnostic> diagnostics) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            normalized = Objects.requireNonNullElse(fallback, "");
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "blank display name replaced with its stable id");
        }
        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_DISPLAY_NAME_LENGTH);
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "display name truncated to " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    static String optionalDisplayName(String value, String path,
                                      List<FormationConfigDiagnostic> diagnostics) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_DISPLAY_NAME_LENGTH);
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "display name truncated to " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    static String description(String value, String path,
                              List<FormationConfigDiagnostic> diagnostics) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            normalized = normalized.substring(0, MAX_DESCRIPTION_LENGTH);
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "description truncated to " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return normalized;
    }

    static int clamp(int value, int minimum, int maximum, String path,
                     List<FormationConfigDiagnostic> diagnostics) {
        int normalized = Math.max(minimum, Math.min(maximum, value));
        if (normalized != value) {
            add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED, path,
                    "value clamped to " + normalized);
        }
        return normalized;
    }

    static double offset(double value) {
        return Double.isFinite(value) && Math.abs(value) <= MAX_VEHICLE_OFFSET
                ? value : Double.NaN;
    }

    static float yaw(float value) {
        if (!Float.isFinite(value)) {
            return Float.NaN;
        }
        float normalized = value % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    static void add(List<FormationConfigDiagnostic> diagnostics,
                    FormationConfigDiagnostic.Kind kind, String path, String message) {
        diagnostics.add(new FormationConfigDiagnostic(kind, path, message));
    }
}
