package com.wok.infantry.formation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Formation-scoped class quota and allowed loadout-entry ids. */
public final class FormationClassRule {
    public static final int MAX_ALLOWED_SLOTS = 16;
    public static final int MAX_ALLOWED_ENTRIES_PER_SLOT = 64;
    private String classId = "assault";
    private String displayName = "";
    private int squadLimit = 8;
    private Map<String, List<String>> allowedEntries = new LinkedHashMap<>();

    public FormationClassRule() {
    }

    public FormationClassRule(String classId, int squadLimit,
                              Map<String, List<String>> allowedEntries) {
        this(classId, "", squadLimit, allowedEntries);
    }

    public FormationClassRule(String classId, String displayName, int squadLimit,
                              Map<String, List<String>> allowedEntries) {
        this.classId = classId;
        this.displayName = displayName;
        this.squadLimit = squadLimit;
        this.allowedEntries = copyAllowedEntries(allowedEntries, false);
    }

    public String classId() {
        return Objects.requireNonNullElse(classId, "");
    }

    public int squadLimit() {
        return squadLimit;
    }

    /** Blank only for legacy catalogs; callers then fall back to the global backing label. */
    public String displayName() {
        return Objects.requireNonNullElse(displayName, "");
    }

    /** Empty map/list means every entry in that slot is allowed. */
    public Map<String, List<String>> allowedEntries() {
        return copyAllowedEntries(allowedEntries, true);
    }

    public List<String> allowedEntriesFor(String slotId) {
        String normalized = FormationValidation.id(slotId, FormationValidation.MAX_ID_LENGTH);
        if (normalized == null || allowedEntries == null) {
            return List.of();
        }
        List<String> entries = allowedEntries.get(normalized);
        return entries == null ? List.of() : List.copyOf(entries);
    }

    public boolean allowsEntry(String slotId, String entryId) {
        String normalizedSlot = FormationValidation.id(slotId, FormationValidation.MAX_ID_LENGTH);
        String normalizedEntry = FormationValidation.entryId(entryId);
        if (normalizedSlot == null || normalizedEntry == null) {
            return false;
        }
        List<String> entries = allowedEntries == null ? null : allowedEntries.get(normalizedSlot);
        return entries == null || entries.isEmpty() || entries.contains(normalizedEntry);
    }

    public FormationClassRule copy() {
        return new FormationClassRule(classId(), displayName(), squadLimit, allowedEntries);
    }

    boolean normalize(String path, List<FormationConfigDiagnostic> diagnostics) {
        String normalizedClassId = FormationValidation.id(classId,
                FormationValidation.MAX_ID_LENGTH);
        if (normalizedClassId == null) {
            FormationValidation.add(diagnostics,
                    FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                    "class rule has an invalid classId");
            return false;
        }
        if (!normalizedClassId.equals(classId)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".classId", "classId normalized to " + normalizedClassId);
        }
        classId = normalizedClassId;
        displayName = FormationValidation.optionalDisplayName(displayName,
                path + ".displayName", diagnostics);
        squadLimit = FormationValidation.clamp(squadLimit, 1,
                FormationValidation.MAX_SQUAD_CAPACITY, path + ".squadLimit", diagnostics);
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (allowedEntries != null) {
            for (Map.Entry<String, List<String>> slot : allowedEntries.entrySet()) {
                if (normalized.size() >= MAX_ALLOWED_SLOTS) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".allowedEntries", "extra slot restrictions removed");
                    break;
                }
                String slotId = FormationValidation.id(slot.getKey(),
                        FormationValidation.MAX_ID_LENGTH);
                if (slotId == null) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".allowedEntries", "invalid slot id removed");
                    continue;
                }
                if (normalized.containsKey(slotId)) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE,
                            path + ".allowedEntries." + slotId,
                            "duplicate normalized slot restriction removed");
                    continue;
                }
                List<String> source = slot.getValue();
                if (source == null || source.isEmpty()) {
                    normalized.put(slotId, new ArrayList<>());
                    continue;
                }
                Set<String> accepted = new LinkedHashSet<>();
                for (String entry : source) {
                    if (accepted.size() >= MAX_ALLOWED_ENTRIES_PER_SLOT) {
                        FormationValidation.add(diagnostics,
                                FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                                path + ".allowedEntries." + slotId,
                                "extra allowed entry ids removed");
                        break;
                    }
                    String entryId = FormationValidation.entryId(entry);
                    if (entryId == null) {
                        FormationValidation.add(diagnostics,
                                FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                                path + ".allowedEntries." + slotId,
                                "invalid allowed entry id removed");
                    } else if (!accepted.add(entryId)) {
                        FormationValidation.add(diagnostics,
                                FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE,
                                path + ".allowedEntries." + slotId,
                                "duplicate allowed entry id removed");
                    }
                }
                if (accepted.isEmpty()) {
                    // A malformed non-empty allow-list must not silently become an allow-all rule.
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                            "class rule removed because a non-empty allow-list had no valid ids");
                    return false;
                }
                normalized.put(slotId, new ArrayList<>(accepted));
            }
        }
        allowedEntries = normalized;
        return true;
    }

    private static Map<String, List<String>> copyAllowedEntries(
            Map<String, List<String>> source, boolean immutable) {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((slot, entries) -> {
                if (slot != null) {
                    List<String> entryCopy = entries == null
                            ? new ArrayList<>() : new ArrayList<>(entries);
                    copy.put(slot, immutable
                            ? Collections.unmodifiableList(entryCopy) : entryCopy);
                }
            });
        }
        return immutable ? Collections.unmodifiableMap(copy) : copy;
    }
}
