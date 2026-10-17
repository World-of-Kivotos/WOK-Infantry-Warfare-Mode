package com.wok.commandersupport.drone;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.recon.ReconSatelliteScanner;
import com.wok.commandersupport.registry.CommanderSupportEntities;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.adapter.SupportIntelPublisher;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Recon drone sortie: a drone circles the target for about two minutes and scans every ten
 * seconds, twelve scans in all, then flies away.
 *
 * <p>Step 0 launches the drone over the target and scans at once. It is only launched when the
 * whole orbit is in loaded, entity-ticking chunks; nothing is loaded for it. Every failure before
 * that first scan reached the map (unloaded airspace, a rejected spawn, a scan or publication
 * failure) removes the drone and returns the cooldown. From step 1 on the call is spent: a drone
 * that was shot down or lost its link only ends the mission. Any failure thrown here first
 * removes the drone, except a wreck that is already falling.</p>
 *
 * <p>Scans reuse the satellite scanner around the target point with the definition radius, so
 * only loaded entities are read. No third-party class is referenced.</p>
 */
public final class ReconDroneProvider implements SupportProvider {
    /** 5 minutes, from the faction table of 《步战模式公示表》. */
    public static final long COOLDOWN_TICKS = 5L * 60L * 20L;
    public static final int STEP_INTERVAL_TICKS = 40;
    /** 60 steps of 2 seconds: about two minutes on station. */
    public static final int STEP_COUNT = 60;
    public static final double SCAN_RADIUS = 96.0D;
    /** Longer than the 10-second scan interval, so contacts never blink between scans. */
    public static final int CONTACT_TTL_TICKS = 240;
    /** Missions are capped by the scheduler; this only bounds links left behind by a reset. */
    static final int MAX_TRACKED_SORTIES = 16;

    private final Map<UUID, ReconDroneSortie> sorties = new LinkedHashMap<>();

    public static SupportDefinition definition() {
        return new SupportDefinition(WokCommanderSupportMod.RECON_DRONE_ID,
                "support.wok_commander_support.recon_drone",
                "无人机侦察", "无人机侦察", SupportTargetMode.POINT,
                COOLDOWN_TICKS, 0L, STEP_COUNT, STEP_INTERVAL_TICKS, SCAN_RADIUS, false);
    }

    @Override
    public ResourceLocation supportId() {
        return WokCommanderSupportMod.RECON_DRONE_ID;
    }

    @Override
    public ProviderAvailability availability() {
        return CommanderSupportEntities.RECON_DRONE.isPresent()
                ? ProviderAvailability.present()
                : ProviderAvailability.unavailable("侦察无人机实体未注册");
    }

    @Override
    public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        if (context == null) {
            throw SupportSpawnException.endMission(ReconDroneMissionPolicy.MISSING_CONTEXT_MESSAGE);
        }
        try {
            if (context.stepIndex() == 0) {
                launch(context);
            } else {
                patrol(context);
            }
        } catch (Exception | LinkageError failure) {
            // Exception also covers SupportSpawnException; VirtualMachineError is never caught.
            withdraw(context, "its mission step failed");
            throw ReconDroneMissionPolicy.classify(context.stepIndex(), failure);
        }
    }

    /**
     * The scheduler cancelled the mission after launch (requester left the faction, footprint
     * outside the border, provider disabled): recall the drone. A wreck that is already falling
     * finishes its crash and removes itself.
     */
    @Override
    public void abandon(SupportSpawnContext context) {
        if (context != null) {
            withdraw(context, "its mission was cancelled");
        }
    }

    private void launch(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        requireScanner(context);
        double centerX = context.target().startX();
        double centerZ = context.target().startZ();
        if (!airspaceReady(level, centerX, centerZ)) {
            WokCommanderSupportMod.LOGGER.info(
                    "Recon drone {} not launched: the orbit around [{}, {}] is not loaded",
                    context.callId(), centerX, centerZ);
            throw ReconDroneMissionPolicy.airspaceUnloaded();
        }
        Integer targetSurface = loadedSurface(level, centerX, centerZ,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        List<Integer> orbitSurfaces = new ArrayList<>(ReconDroneFlight.HEIGHT_SAMPLES);
        for (double[] point : ReconDroneFlight.orbitSamplePoints(centerX, centerZ,
                ReconDroneFlight.ORBIT_RADIUS, ReconDroneFlight.HEIGHT_SAMPLES)) {
            orbitSurfaces.add(loadedSurface(level, point[0], point[1],
                    Heightmap.Types.MOTION_BLOCKING));
        }
        OptionalDouble altitude = ReconDroneFlight.cruiseAltitude(targetSurface, orbitSurfaces,
                level.getMaxBuildHeight());
        if (altitude.isEmpty()) {
            throw ReconDroneMissionPolicy.airspaceUnloaded();
        }

        long now = level.getGameTime();
        ReconDroneFlight.Orbit orbit = new ReconDroneFlight.Orbit(centerX, centerZ,
                ReconDroneFlight.ORBIT_RADIUS, altitude.getAsDouble(),
                ReconDroneFlight.launchPhase(context.callId()),
                ReconDroneFlight.launchClockwise(context.callId()));
        ReconDroneEntity drone = CommanderSupportEntities.RECON_DRONE.get().create(level);
        if (drone == null) {
            throw ReconDroneMissionPolicy.launchRejected();
        }
        ReconDroneSortie sortie = new ReconDroneSortie(drone);
        drone.setUUID(context.callId());
        drone.launch(orbit, altitude.getAsDouble() - targetSurface, now,
                ReconDroneFlight.expireGameTime(now, context.definition().stepCount(),
                        context.definition().stepIntervalTicks()),
                context.faction(), sortie);
        if (!level.addFreshEntity(drone)) {
            throw ReconDroneMissionPolicy.launchRejected();
        }
        remember(context.callId(), sortie);
        WokCommanderSupportMod.LOGGER.info(
                "Recon drone {} launched over [{}, {}] at Y {} for faction {}",
                context.callId(), centerX, centerZ, altitude.getAsDouble(),
                context.faction().id());
        scanAndPublish(context);
    }

    private void patrol(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        ReconDroneSortie sortie = sortie(context.callId());
        ReconDroneEntity drone = locate(level, context.callId(), sortie);
        boolean present = drone != null && !drone.isRemoved();
        ReconDroneMissionPolicy.Status status = ReconDroneMissionPolicy.status(
                sortie != null && sortie.shotDown(), present,
                present && drone.isCrashing(),
                present && level.isPositionEntityTicking(drone.blockPosition()));
        if (status != ReconDroneMissionPolicy.Status.ACTIVE) {
            WokCommanderSupportMod.LOGGER.info("Recon drone {} ended its sortie at step {}: {}",
                    context.callId(), context.stepIndex(), status);
            throw ReconDroneMissionPolicy.outcome(status);
        }
        if (ReconDroneMissionPolicy.isScanStep(context.stepIndex())) {
            scanAndPublish(context);
        }
        if (ReconDroneMissionPolicy.isFinalStep(context.stepIndex(),
                context.definition().stepCount())) {
            forget(context.callId());
            drone.depart();
            WokCommanderSupportMod.LOGGER.info(
                    "Recon drone {} completed its sortie and is leaving the area",
                    context.callId());
        }
    }

    /** Only the commander's current session may publish, for the faction that called the drone. */
    private static void scanAndPublish(SupportSpawnContext context)
            throws SupportSpawnException {
        ServerPlayer owner = requireScanner(context);
        BattleService battle = BattleService.get(owner).orElseThrow(
                ReconDroneMissionPolicy::noBattle);
        if (ReconDroneMissionPolicy.requesterLeftMissionFaction(context.faction(),
                battle.factionOf(owner.getUUID()).orElse(null))) {
            // The scheduler cancels the mission before its next step.
            WokCommanderSupportMod.LOGGER.info(
                    "Recon drone {} skipped scan {}: requester left faction {}",
                    context.callId(), context.stepIndex(), context.faction().id());
            return;
        }
        List<SupportIntelContact> contacts = ReconSatelliteScanner.scan(context);
        SupportIntelPublisher.publish(context, contacts, CONTACT_TTL_TICKS);
    }

    /** Checked up front so a missing commander or battle is reported with the drone's name. */
    private static ServerPlayer requireScanner(SupportSpawnContext context)
            throws SupportSpawnException {
        if (!(context.owner() instanceof ServerPlayer owner)) {
            throw ReconDroneMissionPolicy.noCommander();
        }
        if (BattleService.get(owner).isEmpty()) {
            throw ReconDroneMissionPolicy.noBattle();
        }
        return owner;
    }

    /**
     * Every chunk under the orbit has finished loading and ticks entities. Only already-loaded
     * state is read: {@code getChunkNow} never waits for or generates a chunk.
     */
    private static boolean airspaceReady(ServerLevel level, double centerX, double centerZ) {
        for (long packed : ReconDroneFlight.orbitChunks(centerX, centerZ,
                ReconDroneFlight.ORBIT_RADIUS, ReconDroneFlight.AIRFRAME_MARGIN)) {
            int chunkX = ReconDroneFlight.chunkX(packed);
            int chunkZ = ReconDroneFlight.chunkZ(packed);
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null
                    || !level.isPositionEntityTicking(new BlockPos(
                    SectionPos.sectionToBlockCoord(chunkX), level.getMinBuildHeight(),
                    SectionPos.sectionToBlockCoord(chunkZ)))) {
                return false;
            }
        }
        return true;
    }

    /** First free Y above the column, or null when its chunk is not loaded (never loads it). */
    private static Integer loadedSurface(ServerLevel level, double x, double z,
                                         Heightmap.Types type) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        return chunk == null ? null : chunk.getHeight(type, blockX & 15, blockZ & 15) + 1;
    }

    /**
     * Removes this call's drone, if it is still in the world and not already falling. Cleanup
     * never fails the caller: problems are only logged.
     */
    private void withdraw(SupportSpawnContext context, String reason) {
        ReconDroneSortie sortie = forget(context.callId());
        try {
            ReconDroneEntity drone = locate(context.level(), context.callId(), sortie);
            if (drone != null && !drone.isRemoved() && !drone.isCrashing()) {
                drone.discard();
                WokCommanderSupportMod.LOGGER.info("Recon drone {} was recalled because {}",
                        context.callId(), reason);
            }
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn("Recon drone {} could not be recalled",
                    context.callId(), failure);
        }
    }

    /** The drone by its call id, or through the sortie when it is not indexed by the level. */
    private static ReconDroneEntity locate(ServerLevel level, UUID callId,
                                           ReconDroneSortie sortie) {
        Entity found = level.getEntity(callId);
        if (found instanceof ReconDroneEntity drone) {
            return drone;
        }
        return sortie == null ? null : sortie.drone();
    }

    private synchronized void remember(UUID callId, ReconDroneSortie sortie) {
        sorties.put(callId, sortie);
        Iterator<UUID> eldest = sorties.keySet().iterator();
        while (sorties.size() > MAX_TRACKED_SORTIES && eldest.hasNext()) {
            eldest.next();
            eldest.remove();
        }
    }

    private synchronized ReconDroneSortie sortie(UUID callId) {
        return sorties.get(callId);
    }

    private synchronized ReconDroneSortie forget(UUID callId) {
        return sorties.remove(callId);
    }
}
