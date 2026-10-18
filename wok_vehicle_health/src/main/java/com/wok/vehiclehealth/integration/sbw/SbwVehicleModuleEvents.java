package com.wok.vehiclehealth.integration.sbw;

import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraPos;
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import com.wok.vehiclehealth.WokVehicleHealthMod;
import com.wok.vehiclehealth.balance.InfantryAntiTankWeapon;
import com.wok.vehiclehealth.balance.TaczProjectileInspector;
import com.wok.vehiclehealth.balance.VehicleBalanceState;
import com.wok.vehiclehealth.balance.VehicleExplosionContexts;
import com.wok.vehiclehealth.config.VehicleModuleConfig;
import com.wok.vehiclehealth.module.VehicleModuleMath;
import com.wok.vehiclehealth.module.VehicleModulePart;
import com.wok.vehiclehealth.module.VehicleOpticsData;
import com.wok.vehiclehealth.network.VehicleModuleNetwork;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(
        modid = WokVehicleHealthMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SbwVehicleModuleEvents {
    private static final Map<Level, Map<Integer, VehicleTracker>> TRACKERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof VehicleEntity vehicle) {
            if (!event.getLevel().isClientSide()) {
                VehicleBalanceState.applyProfile(vehicle);
            }
            trackers(event.getLevel()).put(vehicle.getId(), new VehicleTracker(vehicle));
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof VehicleEntity vehicle) {
            Map<Integer, VehicleTracker> levelTrackers = TRACKERS.get(event.getLevel());
            if (levelTrackers != null) {
                levelTrackers.remove(vehicle.getId());
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (vehicle.level().isClientSide) {
            return;
        }

        Vec3 hitLocation = entityHit.getLocation();
        OBB.Part nativePart = currentHitPart(event, vehicle, hitLocation);
        VehicleModulePart modulePart = classify(vehicle, hitLocation, nativePart);
        if (modulePart == VehicleModulePart.NONE) {
            return;
        }

        VehicleTracker tracker = trackers(vehicle.level())
                .computeIfAbsent(vehicle.getId(), ignored -> new VehicleTracker(vehicle));
        tracker.pendingHits.addLast(new PendingHit(
                modulePart,
                nativePart,
                vehicle.level().getGameTime()));

    }

    /** TACZ explosive ammunition uses a custom Explosion rather than a projectile impact event. */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide
                || !isTaczExplosionClass(event.getExplosion().getClass().getName())) {
            return;
        }
        Vec3 origin = event.getExplosion().getPosition();
        InfantryAntiTankWeapon antiTankWeapon = TaczProjectileInspector
                .inspectExplosion(event.getExplosion())
                .orElse(null);
        for (Entity entity : List.copyOf(event.getAffectedEntities())) {
            if (!(entity instanceof VehicleEntity vehicle)) {
                continue;
            }
            if (antiTankWeapon != null) {
                VehicleExplosionContexts.register(vehicle, antiTankWeapon, origin);
            }
            VehicleModulePart modulePart = classifyExplosion(vehicle, origin);
            if (modulePart == VehicleModulePart.NONE) {
                continue;
            }
            VehicleTracker tracker = trackers(vehicle.level())
                    .computeIfAbsent(vehicle.getId(), ignored -> new VehicleTracker(vehicle));
            // TACZ area damage has no native OBB hit. EMPTY intentionally routes the observed
            // hull loss into our fallback module damage exactly once.
            tracker.pendingHits.addLast(new PendingHit(
                    modulePart, OBB.Part.EMPTY, vehicle.level().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        Map<Integer, VehicleTracker> levelTrackers = TRACKERS.get(event.level);
        if (levelTrackers == null || levelTrackers.isEmpty()) {
            return;
        }

        for (VehicleTracker tracker : List.copyOf(levelTrackers.values())) {
            VehicleEntity vehicle = tracker.vehicle;
            if (vehicle.isRemoved() || vehicle.level() != event.level) {
                levelTrackers.remove(vehicle.getId());
                continue;
            }
            if (event.phase == TickEvent.Phase.START) {
                tracker.beginTick();
            } else {
                tracker.endTick();
            }
        }
    }

    private static Map<Integer, VehicleTracker> trackers(Level level) {
        synchronized (TRACKERS) {
            return TRACKERS.computeIfAbsent(level, ignored -> new java.util.HashMap<>());
        }
    }

    static boolean isTaczExplosionClass(String className) {
        return className != null
                && (className.equals("com.tacz.guns.util.block.ProjectileExplosion")
                || className.startsWith("com.tacz.guns.")
                && className.toLowerCase(java.util.Locale.ROOT).contains("explosion"));
    }

    private static VehicleModulePart classifyExplosion(VehicleEntity vehicle, Vec3 origin) {
        Vec3 closest = closestPoint(vehicle, origin);
        OBB.Part closestPart = OBB.Part.EMPTY;
        double closestDistance = Double.POSITIVE_INFINITY;
        try {
            Vector3d blast = new Vector3d(origin.x, origin.y, origin.z);
            for (OBB obb : vehicle.getOBBs()) {
                Vector3d candidate = OBB.getClosestPointOBB(blast, obb);
                double distance = candidate.distanceSquared(blast);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = new Vec3(candidate.x, candidate.y, candidate.z);
                    closestPart = obb.part;
                }
            }
        } catch (RuntimeException exception) {
            WokVehicleHealthMod.LOGGER.debug(
                    "Could not resolve TACZ explosion against vehicle OBBs for {}",
                    vehicle.getType(), exception);
        }
        return classify(vehicle, closest, closestPart);
    }

    private static Vec3 closestPoint(VehicleEntity vehicle, Vec3 point) {
        return new Vec3(
                Math.max(vehicle.getBoundingBox().minX,
                        Math.min(vehicle.getBoundingBox().maxX, point.x)),
                Math.max(vehicle.getBoundingBox().minY,
                        Math.min(vehicle.getBoundingBox().maxY, point.y)),
                Math.max(vehicle.getBoundingBox().minZ,
                        Math.min(vehicle.getBoundingBox().maxZ, point.z)));
    }

    private static void sendOpticsFeedback(VehicleEntity vehicle) {
        float maximum = VehicleOpticsData.maximum(vehicle);
        float current = VehicleOpticsData.current(vehicle);
        float damageRatio = maximum > 0.0F ? 1.0F - current / maximum : 1.0F;
        float strength = VehicleModuleMath.clamp(0.55F + damageRatio * 0.45F, 0.0F, 1.0F);
        VehicleModuleNetwork.sendOpticsInterference(
                vehicle,
                VehicleModuleConfig.OPTICS_INTERFERENCE_TICKS.get(),
                strength);
    }

    private static OBB.Part currentHitPart(ProjectileImpactEvent event,
                                           VehicleEntity vehicle,
                                           Vec3 hitLocation) {
        try {
            OBB.Part part = OBBHitter.getInstance(event.getProjectile()).sbw$getCurrentHitPart();
            if (part != null && part != OBB.Part.EMPTY) {
                return part;
            }
        } catch (RuntimeException exception) {
            WokVehicleHealthMod.LOGGER.debug(
                    "Projectile {} did not expose a Superb Warfare OBB hit part",
                    event.getProjectile().getType(),
                    exception);
        }

        try {
            for (OBB obb : vehicle.getOBBs()) {
                if (obb.part != OBB.Part.EMPTY && obb.contains(hitLocation)) {
                    return obb.part;
                }
            }
        } catch (RuntimeException exception) {
            WokVehicleHealthMod.LOGGER.debug(
                    "Could not resolve fallback OBB hit part for {}",
                    vehicle.getType(),
                    exception);
        }
        return OBB.Part.EMPTY;
    }

    private static VehicleModulePart classify(VehicleEntity vehicle,
                                              Vec3 hitLocation,
                                              OBB.Part nativePart) {
        if ((nativePart == OBB.Part.TURRET
                || nativePart == OBB.Part.BODY
                || nativePart == OBB.Part.EMPTY)
                && isOpticsHit(vehicle, hitLocation)) {
            return VehicleModulePart.OPTICS;
        }

        return switch (nativePart) {
            case WHEEL_LEFT -> VehicleModulePart.TRACK_LEFT;
            case WHEEL_RIGHT -> VehicleModulePart.TRACK_RIGHT;
            case MAIN_ENGINE -> VehicleModulePart.ENGINE_MAIN;
            case SUB_ENGINE -> VehicleModulePart.ENGINE_SUB;
            case TURRET -> VehicleModulePart.TURRET_RING;
            default -> classifyModelFallback(vehicle, hitLocation);
        };
    }

    private static boolean isOpticsHit(VehicleEntity vehicle, Vec3 hitLocation) {
        double radiusSqr = Math.pow(VehicleModuleConfig.OPTICS_HIT_RADIUS.get(), 2.0D);
        for (Vec3 center : opticsCenters(vehicle)) {
            if (center.distanceToSqr(hitLocation) <= radiusSqr) {
                return true;
            }
        }
        return false;
    }

    private static List<Vec3> opticsCenters(VehicleEntity vehicle) {
        List<Vec3> centers = new ArrayList<>(3);
        try {
            int seatIndex = vehicle.getTurretControllerIndex();
            SeatInfo seat = vehicle.getSeat(seatIndex);
            CameraPos camera = seat == null ? null : seat.getCameraPos();
            if (camera != null) {
                Matrix4d transform = vehicle.getTransformFromString(camera.getTransform(), 1.0F);
                addTransformed(centers, vehicle, transform, camera.getPosition());
                addTransformed(centers, vehicle, transform, camera.getZoomPosition());
            }
        } catch (RuntimeException exception) {
            WokVehicleHealthMod.LOGGER.debug(
                    "Could not derive gunner optic center for {}",
                    vehicle.getType(),
                    exception);
        }

        if (centers.isEmpty()) {
            try {
                Matrix4d turretTransform = vehicle.getTransformFromString("Turret", 1.0F);
                addTransformed(centers, vehicle, turretTransform, Vec3.ZERO);
            } catch (RuntimeException ignored) {
                centers.add(vehicle.position().add(0.0D, vehicle.getBbHeight() * 0.75D, 0.0D));
            }
        }
        return centers;
    }

    private static void addTransformed(List<Vec3> centers,
                                       VehicleEntity vehicle,
                                       Matrix4d transform,
                                       Vec3 localPosition) {
        if (transform == null || localPosition == null) {
            return;
        }
        Vector4d transformed = vehicle.transformPosition(
                transform,
                localPosition.x,
                localPosition.y,
                localPosition.z);
        centers.add(new Vec3(transformed.x, transformed.y, transformed.z));
    }

    private static VehicleModulePart classifyModelFallback(VehicleEntity vehicle,
                                                           Vec3 hitLocation) {
        EngineType engineType = vehicle.computed().getEngineType();
        if (engineType != EngineType.TRACK && engineType != EngineType.WHEEL) {
            return VehicleModulePart.NONE;
        }

        try {
            Matrix4d inverse = new Matrix4d(vehicle.getVehicleTransform(1.0F)).invert();
            Vector4d local = inverse.transform(
                    new Vector4d(hitLocation.x, hitLocation.y, hitLocation.z, 1.0D));
            double normalizedY = (hitLocation.y - vehicle.getBoundingBox().minY)
                    / Math.max(0.5D, vehicle.getBbHeight());
            ModelBounds bounds = modelBounds(vehicle, inverse);
            return VehicleModuleMath.classifyGroundFallback(
                    engineType == EngineType.WHEEL,
                    vehicle.hasTurret(),
                    local.x,
                    local.z,
                    normalizedY,
                    bounds.halfWidth,
                    bounds.halfLength);
        } catch (RuntimeException exception) {
            WokVehicleHealthMod.LOGGER.debug(
                    "Could not classify model-space fallback hit for {}",
                    vehicle.getType(),
                    exception);
        }
        return VehicleModulePart.NONE;
    }

    private static ModelBounds modelBounds(VehicleEntity vehicle, Matrix4d inverse) {
        double halfWidth = Math.max(0.5D, vehicle.getBbWidth() * 0.5D);
        double halfLength = halfWidth;
        List<OBB> obbs = vehicle.getOBBs();
        boolean hasHullObb = obbs.stream().anyMatch(obb ->
                obb.part == OBB.Part.BODY || obb.part == OBB.Part.EMPTY);
        for (OBB obb : obbs) {
            if (hasHullObb && obb.part != OBB.Part.BODY && obb.part != OBB.Part.EMPTY) {
                continue;
            }
            for (Vector3d vertex : obb.getVertices()) {
                Vector4d local = inverse.transform(
                        new Vector4d(vertex.x, vertex.y, vertex.z, 1.0D),
                        new Vector4d());
                halfWidth = Math.max(halfWidth, Math.abs(local.x));
                halfLength = Math.max(halfLength, Math.abs(local.z));
            }
        }
        return new ModelBounds(halfWidth, halfLength);
    }

    private static boolean landVehicle(VehicleEntity vehicle) {
        EngineType engineType = vehicle.computed().getEngineType();
        return engineType == EngineType.TRACK || engineType == EngineType.WHEEL;
    }

    private static final class VehicleTracker {
        private final VehicleEntity vehicle;
        private final ArrayDeque<PendingHit> pendingHits = new ArrayDeque<>();

        private float hullHealth;
        private float turretHealth;
        private float leftTrackHealth;
        private float rightTrackHealth;
        private float mainEngineHealth;
        private float subEngineHealth;
        private float tickStartTurretYaw;
        private float tickStartTurretPitch;

        private VehicleTracker(VehicleEntity vehicle) {
            this.vehicle = vehicle;
            snapshotHealth();
        }

        private void beginTick() {
            tickStartTurretYaw = vehicle.getTurretYRot();
            tickStartTurretPitch = vehicle.getTurretXRot();
            enforceImmobilization();
        }

        private void endTick() {
            processPendingDamage();
            if (!vehicle.level().isClientSide) {
                limitTurretRotation();
            }
            enforceImmobilization();
            if (!vehicle.level().isClientSide) {
                VehicleOpticsData.repairTick(vehicle);
            }
            snapshotHealth();
        }

        private void processPendingDamage() {
            float hullLoss = VehicleModuleMath.positiveLoss(hullHealth, vehicle.getHealth());
            float turretLoss = VehicleModuleMath.positiveLoss(turretHealth, vehicle.getTurretHealth());
            float leftLoss = VehicleModuleMath.positiveLoss(leftTrackHealth, vehicle.getLeftWheelHealth());
            float rightLoss = VehicleModuleMath.positiveLoss(rightTrackHealth, vehicle.getRightWheelHealth());
            float mainLoss = VehicleModuleMath.positiveLoss(mainEngineHealth, vehicle.getMainEngineHealth());
            float subLoss = VehicleModuleMath.positiveLoss(subEngineHealth, vehicle.getSubEngineHealth());
            float observedLoss = Math.max(hullLoss,
                    turretLoss + leftLoss + rightLoss + mainLoss + subLoss);

            long now = vehicle.level().getGameTime();
            List<PendingHit> ready = pendingHits.stream()
                    .filter(hit -> hit.gameTick <= now)
                    .toList();
            if (ready.isEmpty()) {
                pendingHits.removeIf(hit -> now - hit.gameTick > 3L);
                return;
            }
            if (!(observedLoss > 0.0F)) {
                pendingHits.removeIf(hit -> now - hit.gameTick > 3L);
                return;
            }

            long nativeTurretHits = ready.stream()
                    .filter(hit -> hit.nativePart == OBB.Part.TURRET)
                    .count();
            long opticsOnNativeTurret = ready.stream()
                    .filter(hit -> hit.modulePart == VehicleModulePart.OPTICS
                            && hit.nativePart == OBB.Part.TURRET)
                    .count();
            if (turretLoss > 0.0F && nativeTurretHits > 0L && opticsOnNativeTurret > 0L) {
                float restore = turretLoss * opticsOnNativeTurret / nativeTurretHits;
                vehicle.setTurretHealth(Math.min(
                        vehicle.getTurretMaxHealth(),
                        vehicle.getTurretHealth() + restore));
                if (vehicle.getTurretHealth() > vehicle.getTurretMaxHealth() * 0.95F) {
                    vehicle.setTurretDamaged(false);
                }
            }

            float perHitDamage = observedLoss / Math.max(1, ready.size());
            for (PendingHit hit : ready) {
                if (hit.modulePart == VehicleModulePart.OPTICS) {
                    VehicleOpticsData.damage(vehicle, perHitDamage);
                    sendOpticsFeedback(vehicle);
                } else if (!nativeBacked(hit)) {
                    applyFallbackDamage(hit.modulePart, perHitDamage);
                }
                pendingHits.remove(hit);
            }
        }

        private boolean nativeBacked(PendingHit hit) {
            return switch (hit.modulePart) {
                case TRACK_LEFT -> hit.nativePart == OBB.Part.WHEEL_LEFT;
                case TRACK_RIGHT -> hit.nativePart == OBB.Part.WHEEL_RIGHT;
                case ENGINE_MAIN -> hit.nativePart == OBB.Part.MAIN_ENGINE;
                case ENGINE_SUB -> hit.nativePart == OBB.Part.SUB_ENGINE;
                case TURRET_RING -> hit.nativePart == OBB.Part.TURRET;
                default -> false;
            };
        }

        private void applyFallbackDamage(VehicleModulePart part, float amount) {
            switch (part) {
                case TRACK_LEFT -> {
                    vehicle.setLeftWheelHealth(Math.max(0.0F, vehicle.getLeftWheelHealth() - amount));
                    if (vehicle.getLeftWheelHealth() <= 0.0F) {
                        vehicle.setLeftWheelDamaged(true);
                    }
                }
                case TRACK_RIGHT -> {
                    vehicle.setRightWheelHealth(Math.max(0.0F, vehicle.getRightWheelHealth() - amount));
                    if (vehicle.getRightWheelHealth() <= 0.0F) {
                        vehicle.setRightWheelDamaged(true);
                    }
                }
                case ENGINE_MAIN -> {
                    vehicle.setMainEngineHealth(Math.max(0.0F, vehicle.getMainEngineHealth() - amount));
                    if (vehicle.getMainEngineHealth() <= 0.0F) {
                        vehicle.setMainEngineDamaged(true);
                    }
                }
                case ENGINE_SUB -> {
                    vehicle.setSubEngineHealth(Math.max(0.0F, vehicle.getSubEngineHealth() - amount));
                    if (vehicle.getSubEngineHealth() <= 0.0F) {
                        vehicle.setSubEngineDamaged(true);
                    }
                }
                case TURRET_RING -> {
                    vehicle.setTurretHealth(Math.max(0.0F, vehicle.getTurretHealth() - amount));
                    if (vehicle.getTurretHealth() <= 0.0F) {
                        vehicle.setTurretDamaged(true);
                    }
                }
                default -> {
                }
            }
        }

        private void limitTurretRotation() {
            float multiplier = VehicleModuleMath.rotationMultiplier(
                    vehicle.getTurretHealth(),
                    vehicle.getTurretMaxHealth(),
                    VehicleModuleConfig.TURRET_DAMAGED_SPEED_MULTIPLIER.get().floatValue(),
                    VehicleModuleConfig.TURRET_DESTROYED_SPEED_MULTIPLIER.get().floatValue());
            if (multiplier >= 0.999F) {
                return;
            }
            vehicle.setTurretYRot(VehicleModuleMath.limitWrappedDegrees(
                    tickStartTurretYaw,
                    vehicle.getTurretYRot(),
                    multiplier));
            vehicle.setTurretXRot(tickStartTurretPitch
                    + (vehicle.getTurretXRot() - tickStartTurretPitch) * multiplier);
            VehicleModuleNetwork.sendTurretRotation(vehicle);
        }

        private void enforceImmobilization() {
            if (!landVehicle(vehicle)) {
                return;
            }

            boolean leftDestroyed = vehicle.getWheelMaxHealth() > 0.0F
                    && vehicle.getLeftWheelDamaged();
            boolean rightDestroyed = vehicle.getWheelMaxHealth() > 0.0F
                    && vehicle.getRightWheelDamaged();
            boolean trackDestroyed = VehicleModuleConfig.STOP_ON_SINGLE_TRACK_DESTROYED.get()
                    ? leftDestroyed || rightDestroyed
                    : leftDestroyed && rightDestroyed;
            boolean engineDestroyed = (vehicle.getEngineMaxHealth() > 0.0F
                    && vehicle.getMainEngineDamaged())
                    || (vehicle.getEngineMaxHealth() > 0.0F
                    && vehicle.getSubEngineDamaged());
            if (!trackDestroyed && !engineDestroyed) {
                return;
            }

            vehicle.setForwardInputDown(false);
            vehicle.setBackInputDown(false);
            vehicle.setLeftInputDown(false);
            vehicle.setRightInputDown(false);
            vehicle.setSprintInputDown(false);
            vehicle.setPower(0.0F);
            vehicle.setTargetSpeed(0.0D);
            vehicle.setDeltaRot(0.0F);
            Vec3 movement = vehicle.getDeltaMovement();
            vehicle.setDeltaMovement(0.0D, movement.y, 0.0D);
        }

        private void snapshotHealth() {
            hullHealth = vehicle.getHealth();
            turretHealth = vehicle.getTurretHealth();
            leftTrackHealth = vehicle.getLeftWheelHealth();
            rightTrackHealth = vehicle.getRightWheelHealth();
            mainEngineHealth = vehicle.getMainEngineHealth();
            subEngineHealth = vehicle.getSubEngineHealth();
        }
    }

    private record PendingHit(VehicleModulePart modulePart,
                              OBB.Part nativePart,
                              long gameTick) {
    }

    private record ModelBounds(double halfWidth, double halfLength) {
    }

    private SbwVehicleModuleEvents() {
    }
}
