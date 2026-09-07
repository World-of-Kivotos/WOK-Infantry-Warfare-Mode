package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;

import java.util.Objects;
import java.util.Optional;

/** Result of one entity-leave observation, including a genuine combat loss when present. */
public record VehicleRemovalObservation(ActionResult result,
                                        VehicleOwnership destroyedOwnership) {
    public VehicleRemovalObservation {
        Objects.requireNonNull(result, "result");
        if (!result.success() && destroyedOwnership != null) {
            throw new IllegalArgumentException("A failed observation cannot publish a loss");
        }
    }

    public Optional<VehicleOwnership> destroyed() {
        return Optional.ofNullable(destroyedOwnership);
    }

    public static VehicleRemovalObservation success(String message) {
        return new VehicleRemovalObservation(ActionResult.ok(message), null);
    }

    public static VehicleRemovalObservation destroyed(VehicleOwnership ownership) {
        return new VehicleRemovalObservation(ActionResult.ok(
                "已释放被摧毁载具的 allocation 台账"),
                Objects.requireNonNull(ownership, "ownership"));
    }

    public static VehicleRemovalObservation failure(ActionResult result) {
        if (result == null || result.success()) {
            throw new IllegalArgumentException("A failed removal result is required");
        }
        return new VehicleRemovalObservation(result, null);
    }
}
