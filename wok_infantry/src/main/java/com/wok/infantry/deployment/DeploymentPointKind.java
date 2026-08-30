package com.wok.infantry.deployment;

import java.util.Arrays;
import java.util.Optional;

/** Wire-visible kind used to distinguish the permanent main base from field beacons. */
public enum DeploymentPointKind {
    MAIN_BASE("main_base"),
    FIELD_BEACON("field_beacon");

    private final String id;

    DeploymentPointKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<DeploymentPointKind> byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
