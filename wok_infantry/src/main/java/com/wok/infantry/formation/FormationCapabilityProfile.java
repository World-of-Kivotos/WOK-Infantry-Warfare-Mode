package com.wok.infantry.formation;

import java.util.List;
import java.util.Set;

/**
 * Extensible formation-level capability policy. This stage stores and validates rules only;
 * concrete deployables, vehicle respawns and support skills are implemented separately.
 */
public final class FormationCapabilityProfile {
    private FormationDeployablePolicy outpost = new FormationDeployablePolicy();
    private FormationDeployablePolicy rally = new FormationDeployablePolicy();
    private FormationRespawnPolicy respawn = new FormationRespawnPolicy();
    private FormationSupportPolicy support = new FormationSupportPolicy();

    public FormationCapabilityProfile() {
    }

    public FormationCapabilityProfile(FormationDeployablePolicy outpost,
                                      FormationDeployablePolicy rally,
                                      FormationRespawnPolicy respawn,
                                      FormationSupportPolicy support) {
        this.outpost = outpost == null ? new FormationDeployablePolicy() : outpost.copy();
        this.rally = rally == null ? new FormationDeployablePolicy() : rally.copy();
        this.respawn = respawn == null ? new FormationRespawnPolicy() : respawn.copy();
        this.support = support == null ? new FormationSupportPolicy() : support.copy();
    }

    public FormationDeployablePolicy outpost() {
        return outpost == null ? new FormationDeployablePolicy() : outpost.copy();
    }

    public FormationDeployablePolicy rally() {
        return rally == null ? new FormationDeployablePolicy() : rally.copy();
    }

    public FormationRespawnPolicy respawn() {
        return respawn == null ? new FormationRespawnPolicy() : respawn.copy();
    }

    public FormationSupportPolicy support() {
        return support == null ? new FormationSupportPolicy() : support.copy();
    }

    public FormationCapabilityProfile copy() {
        return new FormationCapabilityProfile(outpost, rally, respawn, support);
    }

    void normalize(String path, Set<String> vehicleIds,
                   List<FormationConfigDiagnostic> diagnostics) {
        if (outpost == null) {
            outpost = new FormationDeployablePolicy();
        }
        if (rally == null) {
            rally = new FormationDeployablePolicy();
        }
        if (respawn == null) {
            respawn = new FormationRespawnPolicy();
        }
        if (support == null) {
            support = new FormationSupportPolicy();
        }
        outpost.normalize(path + ".outpost", diagnostics);
        rally.normalize(path + ".rally", diagnostics);
        respawn.normalize(path + ".respawn", vehicleIds, diagnostics);
        support.normalize(path + ".support", diagnostics);
    }
}
