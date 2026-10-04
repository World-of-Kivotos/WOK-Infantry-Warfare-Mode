package com.wok.commandersupport.artillery;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.airstrike.SuperbWarfareExplosionAdapter;
import com.wok.infantry.support.adapter.AbstractSoftSupportProvider;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Howitzer barrage shared by every {@link ArtilleryProfile}. No shell entity is spawned: each
 * round lands as a Superb Warfare {@code CustomExplosion} at its planned point, located by a
 * vanilla marker that never enters the world, with the commander as the attacker. Block damage
 * follows Superb Warfare's server setting, exactly as for the JDAM and F-16C strikes.
 *
 * <p>Acceptance queues the gun reports of every wave at a virtual battery
 * ({@link ArtilleryReportScheduler}); step 0 checks the explosion API and starts the first
 * whistle; each round whistles in 1.5 s before it lands. The fire plan is rebuilt from the call id
 * on every step ({@link ArtilleryFirePlan}), so the provider keeps no per-mission state besides
 * the queued reports.</p>
 *
 * <p>Failures follow {@link ArtilleryFailures}: anything failing on step 0 returns the cooldown;
 * a round outside the world border is skipped; an explosion fault after that trips the circuit
 * without a refund. Whenever the mission ends early, completes or is called off, its remaining
 * reports are dropped.</p>
 */
public final class ArtilleryBarrageProvider extends AbstractSoftSupportProvider {
    static final String SUPERB_WARFARE_MOD_ID = "superbwarfare";

    private final ArtilleryProfile profile;

    public ArtilleryBarrageProvider(ArtilleryProfile profile) {
        super(Objects.requireNonNull(profile, "profile").id());
        this.profile = profile;
    }

    public ArtilleryProfile profile() {
        return profile;
    }

    @Override
    protected ProviderAvailability probeAvailability() {
        if (!ModList.get().isLoaded(SUPERB_WARFARE_MOD_ID)) {
            return ProviderAvailability.unavailable("需要安装卓越前线");
        }
        if (!SuperbWarfareExplosionAdapter.available()) {
            return ProviderAvailability.unavailable("卓越前线 CustomExplosion 接口不可用");
        }
        return ProviderAvailability.present();
    }

    /**
     * Queues every gun report of the barrage and fires the first one now. Reads entity positions
     * and an already-loaded height only; a failure is logged and never touches the acceptance.
     */
    @Override
    public void accepted(SupportSpawnContext context) {
        if (context == null) {
            return;
        }
        try {
            announce(context);
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn("Howitzer barrage {} ({}) could not play or "
                    + "schedule its gun reports", context.callId(), profile.id(), failure);
        }
    }

    @Override
    protected void doExecuteStep(SupportSpawnContext context)
            throws ReflectiveOperationException, SupportSpawnException {
        boolean completed = false;
        try {
            try {
                fire(context);
            } catch (SupportSpawnException | RuntimeException | LinkageError failure) {
                throw ArtilleryFailures.classify(context.stepIndex(), failure);
            }
            completed = true;
        } finally {
            settleReports(context, completed);
        }
    }

    /**
     * The core called the mission off after step 0 (requester left the faction, footprint
     * unloaded, provider disabled): no shell exists, only the queued reports must stop.
     */
    @Override
    public void abandon(SupportSpawnContext context) {
        if (context == null) {
            return;
        }
        ArtilleryReportScheduler.cancel(context.callId());
        WokCommanderSupportMod.LOGGER.info(
                "Howitzer barrage {} ({}) was called off before its last round",
                context.callId(), profile.id());
    }

    private void announce(SupportSpawnContext context) {
        ServerLevel level = context.level();
        Entity commander = context.owner();
        long acceptedAt = ArtilleryReportScheduler.gameTime(level);
        double targetX = context.target().startX();
        double targetZ = context.target().startZ();
        double surfaceY = ArtilleryReportPlan.referenceSurfaceY(
                loadedSurfaceY(level, targetX, targetZ), commander.getY(), level.getSeaLevel());
        Vec3 battery = ArtilleryReportPlan.batteryPosition(targetX, targetZ,
                commander.getX(), commander.getZ(), surfaceY, profile.batteryOffset(),
                context.callId());
        ArtilleryReportScheduler.ReportCue cue = new ArtilleryReportScheduler.ReportCue(
                level.dimension(), battery.x, battery.y, battery.z, profile.caliber());

        List<ReportQueue.Timed<ArtilleryReportScheduler.ReportCue>> later = new ArrayList<>();
        int immediate = 0;
        for (ArtilleryReportPlan.Report report : ArtilleryReportPlan.reports(profile)) {
            if (report.delayTicks() <= 0L) {
                immediate++;
            } else {
                later.add(new ReportQueue.Timed<>(acceptedAt + report.delayTicks(), cue));
            }
        }
        ArtilleryReportScheduler.schedule(context.callId(), later,
                ArtilleryReportScheduler.acceptanceLease(acceptedAt, profile.inboundTicks()));
        for (int gun = 0; gun < immediate; gun++) {
            ArtillerySounds.play(level, battery.x, battery.y, battery.z,
                    ArtillerySounds.report(profile.caliber()), ArtilleryProfile.REPORT_VOLUME,
                    1.0F);
        }
        WokCommanderSupportMod.LOGGER.info(
                "Howitzer barrage {} ({}) accepted: {} gun reports from a virtual battery at "
                        + "[{}, {}, {}], first rounds land in {} ticks",
                context.callId(), profile.id(), later.size() + immediate,
                battery.x, battery.y, battery.z, profile.preparationTicks());
    }

    private void fire(SupportSpawnContext context) throws SupportSpawnException {
        if (context.stepIndex() == 0) {
            precheck(context);
        }
        List<ArtilleryRound> rounds = ArtilleryFirePlan.rounds(profile, context.callId(),
                context.target().startX(), context.target().startZ());
        ArtilleryStep step = ArtilleryFirePlan.step(rounds, context.stepIndex());
        for (ArtilleryRound round : step.impacts()) {
            land(context, round);
        }
        // After the impacts, so a whistle above a fresh crater starts from the new ground.
        for (ArtilleryRound round : step.whistles()) {
            whistle(context, round);
        }
        if (context.stepIndex() == 0) {
            WokCommanderSupportMod.LOGGER.info(
                    "Howitzer barrage {} ({}) is inbound: {} waves x {} rounds",
                    context.callId(), profile.id(), profile.waves(), profile.roundsPerWave());
        }
    }

    /** Step 0: nothing has landed yet, so every failure here returns the cooldown. */
    private void precheck(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(
                context.target().startX(), level.getMinBuildHeight(),
                context.target().startZ()))) {
            throw ArtilleryFailures.targetOutsideBorder();
        }
        try {
            SuperbWarfareExplosionAdapter.verify(profile.explosionParticle());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw ArtilleryFailures.explosionUnavailable(failure);
        }
        if (EntityType.MARKER.create(level) == null) {
            throw ArtilleryFailures.markerUnavailable();
        }
    }

    private void land(SupportSpawnContext context, ArtilleryRound round)
            throws SupportSpawnException {
        ServerLevel level = context.level();
        OptionalInt surface = loadedSurfaceY(level, round.x(), round.z());
        if (surface.isEmpty()) {
            // The core keeps the whole danger area loaded; never load a column for a shell.
            WokCommanderSupportMod.LOGGER.info(
                    "Howitzer barrage {} skipped round {}/{}: column [{}, {}] is not loaded",
                    context.callId(), round.wave(), round.round(), round.x(), round.z());
            return;
        }
        if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(round.x(),
                surface.getAsInt(), round.z()))) {
            WokCommanderSupportMod.LOGGER.info(
                    "Howitzer barrage {} skipped round {}/{}: [{}, {}] is outside the world border",
                    context.callId(), round.wave(), round.round(), round.x(), round.z());
            return;
        }
        Vec3 impact = new Vec3(round.x(), ArtilleryFirePlan.impactY(surface.getAsInt(),
                level.getMinBuildHeight(), level.getMaxBuildHeight()), round.z());
        Entity marker = EntityType.MARKER.create(level);
        if (marker == null) {
            throw ArtilleryFailures.explosionFailed(
                    new IllegalStateException("Vanilla marker entity could not be created"));
        }
        // Locates the explosion only; it never enters the world, so nothing has to remove it.
        marker.moveTo(impact.x, impact.y, impact.z);
        try {
            SuperbWarfareExplosionAdapter.explode(marker, context.owner(), impact,
                    profile.damage(), profile.explosionRadius(), profile.explosionParticle());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw ArtilleryFailures.explosionFailed(failure);
        }
    }

    /** Cosmetic: a missing height or a sound failure never changes the mission. */
    private void whistle(SupportSpawnContext context, ArtilleryRound round) {
        try {
            OptionalInt surface = loadedSurfaceY(context.level(), round.x(), round.z());
            if (surface.isEmpty()) {
                return;
            }
            ArtillerySounds.play(context.level(), round.x(),
                    surface.getAsInt() + ArtilleryProfile.WHISTLE_HEIGHT, round.z(),
                    ArtillerySounds.incoming(), ArtilleryProfile.INCOMING_VOLUME,
                    profile.incomingPitch());
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn(
                    "Howitzer barrage {} could not play the whistle of round {}/{}",
                    context.callId(), round.wave(), round.round(), failure);
        }
    }

    /** A step that failed, or the last one, ends the reports; any other step renews them. */
    private void settleReports(SupportSpawnContext context, boolean completed) {
        try {
            if (!completed || context.stepIndex() >= profile.stepCount() - 1) {
                ArtilleryReportScheduler.cancel(context.callId());
            } else {
                ArtilleryReportScheduler.renew(context.callId(),
                        ArtilleryReportScheduler.stepLease(
                                ArtilleryReportScheduler.gameTime(context.level())));
            }
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn(
                    "Howitzer barrage {} could not update its gun reports",
                    context.callId(), failure);
        }
    }

    /**
     * Motion-blocking surface (first free block, as {@code Level.getHeight} reports it) of a
     * column whose chunk has finished loading; empty otherwise. Never loads or waits for a chunk.
     */
    private static OptionalInt loadedSurfaceY(ServerLevel level, double x, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        if (chunk == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                blockX & 15, blockZ & 15) + 1);
    }
}
