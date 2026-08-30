package com.wok.infantry.formation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Formation-specific respawn delay and optional mobile-spawn vehicle allocation ids. */
public final class FormationRespawnPolicy {
    public static final int INHERIT_GLOBAL_DELAY = -1;
    public static final int MAX_DELAY_SECONDS = 3_600;
    public static final int MAX_MOBILE_SPAWN_VEHICLES = 64;

    private int delaySeconds = INHERIT_GLOBAL_DELAY;
    private List<String> mobileSpawnVehicleIds = new ArrayList<>();

    public FormationRespawnPolicy() {
    }

    public FormationRespawnPolicy(int delaySeconds, List<String> mobileSpawnVehicleIds) {
        this.delaySeconds = delaySeconds;
        this.mobileSpawnVehicleIds = new ArrayList<>(mobileSpawnVehicleIds == null
                ? List.of() : mobileSpawnVehicleIds);
    }

    public int delaySeconds() {
        return delaySeconds;
    }

    public boolean inheritsGlobalDelay() {
        return delaySeconds == INHERIT_GLOBAL_DELAY;
    }

    public List<String> mobileSpawnVehicleIds() {
        return List.copyOf(mobileSpawnVehicleIds == null ? List.of() : mobileSpawnVehicleIds);
    }

    public FormationRespawnPolicy copy() {
        return new FormationRespawnPolicy(delaySeconds, mobileSpawnVehicleIds);
    }

    void normalize(String path, Set<String> vehicleIds,
                   List<FormationConfigDiagnostic> diagnostics) {
        delaySeconds = FormationValidation.clamp(delaySeconds, INHERIT_GLOBAL_DELAY,
                MAX_DELAY_SECONDS, path + ".delaySeconds", diagnostics);
        List<String> normalized = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        if (mobileSpawnVehicleIds != null) {
            for (int index = 0; index < mobileSpawnVehicleIds.size(); index++) {
                if (normalized.size() >= MAX_MOBILE_SPAWN_VEHICLES) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID,
                            path + ".mobileSpawnVehicleIds", "extra vehicle ids removed");
                    break;
                }
                String childPath = path + ".mobileSpawnVehicleIds[" + index + "]";
                String id = FormationValidation.id(mobileSpawnVehicleIds.get(index),
                        FormationValidation.MAX_ID_LENGTH);
                if (id == null || !vehicleIds.contains(id)) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_INVALID, childPath,
                            "mobile spawn id must reference a vehicle in the same formation");
                    continue;
                }
                if (!unique.add(id)) {
                    FormationValidation.add(diagnostics,
                            FormationConfigDiagnostic.Kind.REMOVED_DUPLICATE, childPath,
                            "duplicate mobile spawn vehicle id removed: " + id);
                    continue;
                }
                normalized.add(id);
            }
        }
        mobileSpawnVehicleIds = normalized;
    }
}
