package com.wok.infantry.formation.selection;

import com.wok.infantry.formation.FormationSupportPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Structured, display-only composition of one formation (formation protocol 5). Every name is the
 * server's public display name, never an internal class or vehicle ID; support entries are IDs
 * whose names come from {@link FormationSelectionSnapshot#supportLabels()}.
 *
 * @param classQuotas         profession display names and their per-squad limit
 * @param vehicles            grouped vehicles: display name, count and replenishment cooldown
 *                            ({@link Vehicle#NEVER} = not replenished)
 * @param squads              squad display names and capacities
 * @param outpostMax          active outposts allowed, 0 = no outposts
 * @param rallyMax            rally packs per squad, 0 = none
 * @param respawnDelaySeconds formation respawn delay, {@link #INHERIT_RESPAWN} = global setting
 * @param mobileSpawnVehicles display names of the mobile-spawn vehicles
 * @param supportMode         which commander supports the formation may call
 * @param supportIds          support IDs (all registered ones for {@code ALL})
 */
public record FormationDetailView(List<ClassQuota> classQuotas,
                                  List<Vehicle> vehicles,
                                  List<Squad> squads,
                                  int outpostMax,
                                  int rallyMax,
                                  int respawnDelaySeconds,
                                  List<String> mobileSpawnVehicles,
                                  FormationSupportPolicy.Mode supportMode,
                                  List<String> supportIds) {
    public static final int INHERIT_RESPAWN = -1;
    public static final FormationDetailView EMPTY = new FormationDetailView(List.of(), List.of(),
            List.of(), 0, 0, INHERIT_RESPAWN, List.of(), FormationSupportPolicy.Mode.NONE,
            List.of());

    public FormationDetailView {
        classQuotas = List.copyOf(classQuotas == null ? List.of() : classQuotas);
        vehicles = List.copyOf(vehicles == null ? List.of() : vehicles);
        squads = List.copyOf(squads == null ? List.of() : squads);
        outpostMax = Math.max(0, outpostMax);
        rallyMax = Math.max(0, rallyMax);
        respawnDelaySeconds = Math.max(INHERIT_RESPAWN, respawnDelaySeconds);
        mobileSpawnVehicles = List.copyOf(mobileSpawnVehicles == null
                ? List.of() : mobileSpawnVehicles);
        supportMode = Objects.requireNonNullElse(supportMode, FormationSupportPolicy.Mode.NONE);
        supportIds = List.copyOf(supportIds == null ? List.of() : supportIds);
    }

    /** Total number of vehicles (sum of every group's count). */
    public int vehicleCount() {
        return vehicles.stream().mapToInt(Vehicle::count).sum();
    }

    /** One profession: public display name and how many of it one squad may field. */
    public record ClassQuota(String displayName, int squadLimit) {
        public ClassQuota {
            displayName = Objects.requireNonNullElse(displayName, "");
            squadLimit = Math.max(0, squadLimit);
        }
    }

    /** One vehicle group; {@code cooldownSeconds} is {@link #NEVER} when it is not replenished. */
    public record Vehicle(String displayName, int count, int cooldownSeconds) {
        public static final int NEVER = -1;

        public Vehicle {
            displayName = Objects.requireNonNullElse(displayName, "");
            count = Math.max(1, count);
            cooldownSeconds = Math.max(NEVER, cooldownSeconds);
        }

        public boolean replenishes() {
            return cooldownSeconds != NEVER;
        }
    }

    /** One squad: public display name (callsign) and member capacity. */
    public record Squad(String displayName, int capacity) {
        public Squad {
            displayName = Objects.requireNonNullElse(displayName, "");
            capacity = Math.max(1, capacity);
        }
    }
}
