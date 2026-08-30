package com.wok.infantry.formation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One data-driven squad slot inside a formation. */
public final class FormationSquadDefinition {
    public static final int MAX_CLASS_LIMITS = 64;

    private String callsign = "alpha";
    private String displayName = "Alpha";
    private int capacity = 8;
    private Map<String, Integer> classLimits = new LinkedHashMap<>();

    public FormationSquadDefinition() {
    }

    public FormationSquadDefinition(String callsign, String displayName, int capacity,
                                    Map<String, Integer> classLimits) {
        this.callsign = callsign;
        this.displayName = displayName;
        this.capacity = capacity;
        this.classLimits = classLimits == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(classLimits);
    }

    public String callsign() {
        return Objects.requireNonNullElse(callsign, "");
    }

    public String displayName() {
        return Objects.requireNonNullElse(displayName, callsign());
    }

    public int capacity() {
        return capacity;
    }

    public Map<String, Integer> classLimits() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
                classLimits == null ? Map.of() : classLimits));
    }

    public int classLimit(String classId, int fallback) {
        String normalized = FormationValidation.id(classId, FormationValidation.MAX_ID_LENGTH);
        if (normalized == null || classLimits == null) {
            return fallback;
        }
        return classLimits.getOrDefault(normalized, fallback);
    }

    public FormationSquadDefinition copy() {
        return new FormationSquadDefinition(callsign(), displayName(), capacity, classLimits);
    }

    boolean normalize(String path, Set<String> knownClasses,
                      List<FormationConfigDiagnostic> diagnostics) {
        String normalizedCallsign = FormationValidation.callsign(callsign);
        if (normalizedCallsign == null) {
            FormationValidation.add(diagnostics,
                    FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                    "squad has an unsupported callsign (expected alpha through echo)");
            return false;
        }
        if (!normalizedCallsign.equals(callsign)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".callsign", "callsign normalized to " + normalizedCallsign);
        }
        callsign = normalizedCallsign;
        displayName = FormationValidation.displayName(displayName, callsign,
                path + ".displayName", diagnostics);
        capacity = FormationValidation.clamp(capacity, 1,
                FormationValidation.MAX_SQUAD_CAPACITY, path + ".capacity", diagnostics);

        LinkedHashMap<String, Integer> normalizedLimits = new LinkedHashMap<>();
        if (classLimits != null) {
            for (Map.Entry<String, Integer> limit : classLimits.entrySet()) {
                if (normalizedLimits.size() >= MAX_CLASS_LIMITS) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".classLimits", "extra class limits removed");
                    break;
                }
                String classId = FormationValidation.id(limit.getKey(),
                        FormationValidation.MAX_ID_LENGTH);
                if (classId == null || !knownClasses.contains(classId)
                        || limit.getValue() == null) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".classLimits", "unknown or invalid class limit removed");
                    continue;
                }
                if (normalizedLimits.containsKey(classId)) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE,
                            path + ".classLimits." + classId,
                            "duplicate normalized class limit removed");
                    continue;
                }
                normalizedLimits.put(classId, FormationValidation.clamp(limit.getValue(), 0,
                        capacity, path + ".classLimits." + classId, diagnostics));
            }
        }
        classLimits = normalizedLimits;
        return true;
    }
}
