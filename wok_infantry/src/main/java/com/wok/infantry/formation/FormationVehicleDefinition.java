package com.wok.infantry.formation;

import java.util.List;
import java.util.Objects;

/** Logical vehicle option resolved by a later optional vehicle-mod adapter. */
public final class FormationVehicleDefinition {
    private String id = "vehicle";
    private String displayName = "Vehicle";
    private String entityId = "minecraft:pig";
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private float yaw;
    /** -1 disables automatic replenishment; non-negative values are server seconds. */
    private int replenishmentCooldownSeconds = -1;

    public FormationVehicleDefinition() {
    }

    public FormationVehicleDefinition(String id, String displayName, String entityId,
                                      double offsetX, double offsetY, double offsetZ, float yaw) {
        this(id, displayName, entityId, offsetX, offsetY, offsetZ, yaw, -1);
    }

    public FormationVehicleDefinition(String id, String displayName, String entityId,
                                      double offsetX, double offsetY, double offsetZ, float yaw,
                                      int replenishmentCooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.entityId = entityId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.yaw = yaw;
        this.replenishmentCooldownSeconds = replenishmentCooldownSeconds;
    }

    public String id() {
        return Objects.requireNonNullElse(id, "");
    }

    public String displayName() {
        return Objects.requireNonNullElse(displayName, id());
    }

    public String entityId() {
        return Objects.requireNonNullElse(entityId, "");
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public double offsetZ() {
        return offsetZ;
    }

    public float yaw() {
        return yaw;
    }

    public int replenishmentCooldownSeconds() {
        return replenishmentCooldownSeconds;
    }

    public boolean automaticReplenishmentEnabled() {
        return replenishmentCooldownSeconds >= 0;
    }

    public FormationVehicleDefinition copy() {
        return new FormationVehicleDefinition(id(), displayName(), entityId(),
                offsetX, offsetY, offsetZ, yaw, replenishmentCooldownSeconds);
    }

    boolean normalize(String path, List<FormationConfigDiagnostic> diagnostics) {
        String normalizedId = FormationValidation.id(id, FormationValidation.MAX_ID_LENGTH);
        String normalizedEntityId = FormationValidation.entityId(entityId);
        double normalizedX = FormationValidation.offset(offsetX);
        double normalizedY = FormationValidation.offset(offsetY);
        double normalizedZ = FormationValidation.offset(offsetZ);
        float normalizedYaw = FormationValidation.yaw(yaw);
        if (normalizedId == null || normalizedEntityId == null
                || !Double.isFinite(normalizedX) || !Double.isFinite(normalizedY)
                || !Double.isFinite(normalizedZ) || !Float.isFinite(normalizedYaw)) {
            FormationValidation.add(diagnostics,
                    FormationConfigDiagnostic.Kind.REMOVED_INVALID, path,
                    "vehicle has an invalid id, entityId, offset, or yaw");
            return false;
        }
        if (!normalizedId.equals(id)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".id", "vehicle id normalized to " + normalizedId);
        }
        if (!normalizedEntityId.equals(entityId)) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".entityId", "entityId normalized to " + normalizedEntityId);
        }
        if (Float.compare(normalizedYaw, yaw) != 0) {
            FormationValidation.add(diagnostics, FormationConfigDiagnostic.Kind.NORMALIZED,
                    path + ".yaw", "yaw normalized to [0, 360)");
        }
        id = normalizedId;
        entityId = normalizedEntityId;
        displayName = FormationValidation.displayName(displayName, id,
                path + ".displayName", diagnostics);
        offsetX = normalizedX;
        offsetY = normalizedY;
        offsetZ = normalizedZ;
        yaw = normalizedYaw;
        replenishmentCooldownSeconds = FormationValidation.clamp(
                replenishmentCooldownSeconds, -1,
                FormationDeployablePolicy.MAX_COOLDOWN_SECONDS,
                path + ".replenishmentCooldownSeconds", diagnostics);
        return true;
    }
}
