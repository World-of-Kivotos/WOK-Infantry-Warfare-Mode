package com.wok.commandersupport.recon;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.formation.vehicle.VehicleOwnership;
import com.wok.infantry.formation.vehicle.VehiclePersistentData;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.adapter.SupportIntelPublisher;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-only contact acquisition with no third-party vehicle classes in its API surface. */
public final class ReconSatelliteScanner {
    private ReconSatelliteScanner() {
    }

    public static List<SupportIntelContact> scan(SupportSpawnContext context)
            throws SupportSpawnException {
        if (context == null || !(context.owner() instanceof ServerPlayer owner)) {
            throw new SupportSpawnException("侦察卫星缺少在线指挥官");
        }
        BattleService battle = BattleService.get(owner).orElse(null);
        Faction friendly = context.faction();
        if (battle == null || friendly == null) {
            throw new SupportSpawnException("侦察卫星无法确定调用方阵营");
        }

        ServerLevel level = context.level();
        double centerX = context.target().startX();
        double centerZ = context.target().startZ();
        double radius = context.definition().radius();
        Map<UUID, Entity> found = new LinkedHashMap<>();
        DeploymentService deployments = DeploymentService.get(owner.server).orElse(null);

        for (ServerPlayer candidate : level.players()) {
            boolean deployed = deployments == null || deployments.isActive(candidate.getUUID());
            Entity contact = candidate.getRootVehicle();
            if (contact == null) {
                contact = candidate;
            }
            Faction candidateFaction = battle.factionOf(candidate.getUUID()).orElse(null);
            if (shouldScanPlayer(friendly, candidateFaction, deployed,
                    candidate.isAlive(), candidate.isSpectator(),
                    centerX, centerZ, contact.getX(), contact.getZ(), radius)
                    && validContact(level, contact, centerX, centerZ, radius)) {
                found.putIfAbsent(resolvedPlayerContactId(candidate.getUUID(),
                        contact.getUUID()), contact);
            }
        }

        FormationService formations = FormationService.get(owner.server).orElse(null);
        AABB bounds = new AABB(centerX - radius, level.getMinBuildHeight(), centerZ - radius,
                centerX + radius, level.getMaxBuildHeight(), centerZ + radius);
        for (Entity entity : level.getEntities((Entity) null, bounds,
                candidate -> candidate != null && !candidate.isRemoved())) {
            VehicleOwnership ownership = VehiclePersistentData.read(entity).orElse(null);
            Faction vehicleSide = ownership == null || formations == null ? null
                    : formations.battleSideForPublicFaction(ownership.factionId()).orElse(null);
            if (shouldScanVehicle(friendly, vehicleSide,
                    centerX, centerZ, entity.getX(), entity.getZ(), radius)
                    && validContact(level, entity, centerX, centerZ, radius)) {
                found.putIfAbsent(entity.getUUID(), entity);
            }
        }

        return found.values().stream()
                .sorted(Comparator
                        .comparingDouble((Entity entity) -> distanceSquared(
                                centerX, centerZ, entity.getX(), entity.getZ()))
                        .thenComparing(Entity::getUUID))
                .limit(SupportIntelPublisher.MAX_CONTACTS)
                .map(entity -> new SupportIntelContact(entity.getUUID(),
                        entity.getX(), entity.getY(), entity.getZ()))
                .toList();
    }

    static boolean insideRadius(double centerX, double centerZ,
                                double x, double z, double radius) {
        return Double.isFinite(centerX) && Double.isFinite(centerZ)
                && Double.isFinite(x) && Double.isFinite(z)
                && Double.isFinite(radius) && radius >= 0.0D
                && distanceSquared(centerX, centerZ, x, z) <= radius * radius;
    }

    static boolean shouldScanPlayer(Faction friendly, Faction candidateFaction,
                                    boolean deployed, boolean alive, boolean spectator,
                                    double centerX, double centerZ,
                                    double contactX, double contactZ, double radius) {
        return friendly != null && candidateFaction == friendly.opposite()
                && deployed && alive && !spectator
                && insideRadius(centerX, centerZ, contactX, contactZ, radius);
    }

    static boolean shouldScanVehicle(Faction friendly, Faction vehicleFaction,
                                     double centerX, double centerZ,
                                     double vehicleX, double vehicleZ, double radius) {
        return friendly != null && vehicleFaction == friendly.opposite()
                && insideRadius(centerX, centerZ, vehicleX, vehicleZ, radius);
    }

    static UUID resolvedPlayerContactId(UUID playerId, UUID rootVehicleId) {
        return rootVehicleId == null ? playerId : rootVehicleId;
    }

    private static boolean validContact(ServerLevel level, Entity entity,
                                        double centerX, double centerZ, double radius) {
        return entity.getY() >= level.getMinBuildHeight()
                && entity.getY() < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(entity.blockPosition())
                && insideRadius(centerX, centerZ, entity.getX(), entity.getZ(), radius);
    }

    private static double distanceSquared(double centerX, double centerZ,
                                          double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz;
    }
}
