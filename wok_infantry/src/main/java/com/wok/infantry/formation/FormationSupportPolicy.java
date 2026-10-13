package com.wok.infantry.formation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Data-only support visibility policy; concrete support execution remains in SupportService. */
public final class FormationSupportPolicy {
    public enum Mode {
        ALL("all"),
        NONE("none"),
        ALLOW_LIST("allow_list");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        static Mode byId(String value) {
            if (value == null) {
                return NONE;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return null;
        }
    }

    public static final int MAX_SUPPORT_IDS = 64;

    private String mode = Mode.NONE.id();
    private List<String> allowList = new ArrayList<>();

    public FormationSupportPolicy() {
    }

    public FormationSupportPolicy(Mode mode, List<String> allowList) {
        this.mode = mode == null ? Mode.NONE.id() : mode.id();
        this.allowList = new ArrayList<>(allowList == null ? List.of() : allowList);
    }

    public Mode mode() {
        Mode resolved = Mode.byId(mode);
        return resolved == null ? Mode.NONE : resolved;
    }

    public List<String> allowList() {
        return List.copyOf(allowList == null ? List.of() : allowList);
    }

    public boolean allows(String supportId) {
        String normalized = FormationValidation.entityId(supportId);
        return normalized != null && switch (mode()) {
            case ALL -> true;
            case NONE -> false;
            case ALLOW_LIST -> allowList().contains(normalized);
        };
    }

    public FormationSupportPolicy copy() {
        return new FormationSupportPolicy(mode(), allowList);
    }

    void normalize(String path, List<FormationConfigDiagnostic> diagnostics) {
        Mode normalizedMode = Mode.byId(mode);
        if (normalizedMode == null) {
            normalizedMode = Mode.NONE;
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".mode", "unknown mode normalized to none");
        } else if (!normalizedMode.id().equals(mode)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".mode", "support mode normalized to " + normalizedMode.id());
        }
        mode = normalizedMode.id();

        List<String> normalized = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        if (allowList != null && normalizedMode == Mode.ALLOW_LIST) {
            for (int index = 0; index < allowList.size(); index++) {
                if (normalized.size() >= MAX_SUPPORT_IDS) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".allowList", "extra support ids removed");
                    break;
                }
                String childPath = path + ".allowList[" + index + "]";
                String id = FormationValidation.entityId(allowList.get(index));
                if (id == null) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID, childPath,
                            "invalid support id removed");
                    continue;
                }
                if (!unique.add(id)) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate support id removed: " + id);
                    continue;
                }
                normalized.add(id);
            }
        }
        allowList = normalized;
    }
}
