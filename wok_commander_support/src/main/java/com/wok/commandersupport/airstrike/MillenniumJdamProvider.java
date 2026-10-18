package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.support.adapter.AbstractSoftSupportProvider;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * CBC projectile with optional Superb Warfare CustomExplosion detonation.
 *
 * <p>Failures follow {@link AirstrikeFailures}: nothing released returns the cooldown, a shell
 * lost after release only ends this mission, and a CBC reflection fault breaks the provider.
 * A released shell that can no longer be guided or armed, or whose mission the scheduler
 * cancelled, is discarded before the mission ends.</p>
 */
public final class MillenniumJdamProvider extends AbstractSoftSupportProvider {
    public static final String CBC_MOD_ID = "createbigcannons";
    public static final ResourceLocation CBC_HE_SHELL_ID =
            ResourceLocation.fromNamespaceAndPath(CBC_MOD_ID, "he_shell");
    /** Player-visible ordnance name used in mission-end notices. */
    static final String SHELL_NAME = "JDAM 炸弹";
    /** Persistent-data flag on every released JDAM shell. */
    static final String JDAM_MARKER = "WokCommanderSupportJdam";
    /**
     * Played once at the release point 200 blocks above the target. Audibility on the ground
     * comes from {@code attenuation_distance} in sounds.json (volume x distance).
     */
    static final float APPROACH_VOLUME = 3.0F;
    private static final String SBW_EXPLOSION_MARKER =
            "WokCommanderSupportUseSuperbWarfareExplosion";

    private volatile EntityType<?> heShellType;

    public MillenniumJdamProvider() {
        super(WokCommanderSupportMod.MILLENNIUM_JDAM_ID);
    }

    @Override
    protected ProviderAvailability probeAvailability() {
        if (!ModList.get().isLoaded(CBC_MOD_ID)) {
            return ProviderAvailability.unavailable(
                    "需要安装 Create Big Cannons 5.11.4 或兼容版本");
        }
        EntityType<?> resolved = ForgeRegistries.ENTITY_TYPES.getValue(CBC_HE_SHELL_ID);
        if (resolved == null) {
            return ProviderAvailability.unavailable(
                    "Create Big Cannons 未注册 HE 炮弹实体");
        }
        heShellType = resolved;
        return ProviderAvailability.present();
    }

    @Override
    protected void doExecuteStep(SupportSpawnContext context)
            throws ReflectiveOperationException, SupportSpawnException {
        switch (context.stepIndex()) {
            case 0 -> launch(context);
            case 1 -> impactAndArm(context);
            case 2 -> requireActiveShell(context);
            case 3 -> completeDelayedExplosion(context);
            default -> throw new IllegalArgumentException(
                    "Unexpected JDAM mission step " + context.stepIndex());
        }
    }

    private void launch(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        Vec3 impact = impactPosition(context).orElseThrow(AirstrikeFailures::borderMoved);
        JdamFlightPlan plan = JdamFlightPlan.fromImpact(impact.x, impact.y,
                impact.z, WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS);
        Entity shell;
        try {
            shell = createShell(level);
            shell.setUUID(context.callId());
            shell.setPos(plan.spawn().x, plan.spawn().y, plan.spawn().z);
            shell.setDeltaMovement(plan.velocity());
            shell.setNoGravity(true);
            shell.noPhysics = true;
            invokeOrientation(shell, plan.velocity().normalize());
            shell.getPersistentData().putBoolean(JDAM_MARKER, true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            // The shell never entered the world, so the call is returned with the circuit trip.
            throw AirstrikeFailures.brokenBeforeRelease(failure);
        }
        if (!level.addFreshEntity(shell)) {
            throw AirstrikeFailures.spawnRejected();
        }
        Vec3 spawn = plan.spawn();
        cue(context, "release", () -> {
            WokCommanderSupportMod.LOGGER.info(
                    "Spawned Millennium JDAM {} exactly 200 blocks above [{}, {}, {}] after the ten-second inbound delay",
                    context.callId(), impact.x, impact.y, impact.z);
            level.playSound(null, spawn.x, spawn.y, spawn.z,
                    CommanderSupportSounds.JDAM_F15_APPROACH.get(),
                    SoundSource.HOSTILE, APPROACH_VOLUME, 1.0F);
        });
    }

    /**
     * The scheduler cancelled the mission after release (requester left the faction, footprint
     * unloaded, provider disabled): a landed Superb Warfare-mode shell has no CBC fuse, so it
     * would otherwise stay at the target as an inert prop.
     */
    @Override
    public void abandon(SupportSpawnContext context) {
        Entity shell = findActiveShell(context);
        if (shell != null) {
            shell.discard();
            WokCommanderSupportMod.LOGGER.info(
                    "Millennium JDAM {} was withdrawn because its mission was cancelled",
                    context.callId());
        }
    }

    private Entity requireActiveShell(SupportSpawnContext context)
            throws SupportSpawnException {
        Entity shell = findActiveShell(context);
        if (shell == null) {
            throw AirstrikeFailures.shellLost(SHELL_NAME);
        }
        return shell;
    }

    private void impactAndArm(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        Entity shell = requireActiveShell(context);
        Optional<Vec3> landing = impactPosition(context);
        if (landing.isEmpty()) {
            throw discarding(shell, AirstrikeFailures.borderMovedAfterRelease());
        }
        Vec3 impact = landing.get();
        boolean useSuperbWarfareExplosion;
        try {
            shell.setPos(impact.x, impact.y, impact.z);
            shell.setDeltaMovement(Vec3.ZERO);
            shell.setNoGravity(true);
            shell.noPhysics = true;
            invokeOrientation(shell, new Vec3(0.0D, -1.0D, 0.0D));
            useSuperbWarfareExplosion = SuperbWarfareExplosionAdapter.available();
            shell.getPersistentData().putBoolean(SBW_EXPLOSION_MARKER,
                    useSuperbWarfareExplosion);
            if (!useSuperbWarfareExplosion) {
                setCbcExplosionCountdown(shell,
                        WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            // An unarmed shell must not linger at the target as an inert prop.
            throw discarding(shell, AirstrikeFailures.brokenAfterRelease(failure));
        }
        String fuse = useSuperbWarfareExplosion ? "Superb Warfare" : "CBC fallback";
        cue(context, "touchdown", () -> {
            WokCommanderSupportMod.LOGGER.info(
                    "Millennium JDAM {} struck the target and armed a two-second {} fuse",
                    context.callId(), fuse);
            level.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.NETHERITE_BLOCK_FALL, SoundSource.HOSTILE,
                    2.0F, 0.65F);
        });
    }

    private void completeDelayedExplosion(SupportSpawnContext context)
            throws SupportSpawnException {
        ServerLevel level = context.level();
        Optional<Vec3> landing = impactPosition(context);
        Entity shell = findActiveShell(context);
        if (shell == null) {
            if (SuperbWarfareExplosionAdapter.available()) {
                // Superb Warfare mode never arms the CBC countdown: a missing shell never exploded.
                throw AirstrikeFailures.shellLost(SHELL_NAME);
            }
            // The CBC countdown armed at touchdown expires on this tick and may fire first.
            cue(context, "native detonation", () -> {
                WokCommanderSupportMod.LOGGER.info(
                        "Millennium JDAM {} completed its native CBC delayed explosion",
                        context.callId());
                landing.ifPresent(at -> playTail(level, at));
            });
            return;
        }
        if (landing.isEmpty()) {
            throw discarding(shell, AirstrikeFailures.borderMovedAfterRelease());
        }
        Vec3 impact = landing.get();
        if (shell.getPersistentData().getBoolean(SBW_EXPLOSION_MARKER)) {
            try {
                SuperbWarfareExplosionAdapter.explode(shell, context.owner(), impact);
                shell.discard();
                WokCommanderSupportMod.LOGGER.info(
                        "Millennium JDAM {} detonated through Superb Warfare CustomExplosion",
                        context.callId());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                WokCommanderSupportMod.LOGGER.error(
                        "Superb Warfare JDAM explosion failed; using CBC fallback for {}",
                        context.callId(), failure);
                detonateWithCbc(shell);
            }
        } else {
            detonateWithCbc(shell);
            cue(context, "fallback detonation", () -> WokCommanderSupportMod.LOGGER.info(
                    "Millennium JDAM {} used the CBC delayed-explosion fallback",
                    context.callId()));
        }
        cue(context, "tail", () -> playTail(level, impact));
    }

    /** Fires the CBC fuse now; a refused call removes the shell and breaks the provider. */
    private static void detonateWithCbc(Entity shell) throws SupportSpawnException {
        try {
            setCbcExplosionCountdown(shell, 0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw discarding(shell, AirstrikeFailures.brokenAfterRelease(failure));
        }
    }

    private static void playTail(ServerLevel level, Vec3 impact) {
        level.playSound(null, impact.x, impact.y, impact.z,
                CommanderSupportSounds.JDAM_BOMB_TAIL.get(),
                SoundSource.HOSTILE, 3.5F, 1.0F);
    }

    /** Sound cues and progress logs are cosmetic: their failure never ends the mission. */
    private static void cue(SupportSpawnContext context, String stage, Runnable effect) {
        try {
            effect.run();
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn("Millennium JDAM {} skipped its {} cue",
                    context.callId(), stage, failure);
        }
    }

    /** Removes a released shell before its mission reports why it ended. */
    private static SupportSpawnException discarding(Entity shell,
                                                    SupportSpawnException failure) {
        try {
            shell.discard();
        } catch (RuntimeException | LinkageError discardFailure) {
            failure.addSuppressed(discardFailure);
        }
        return failure;
    }

    private static void setCbcExplosionCountdown(Entity shell, int ticks)
            throws ReflectiveOperationException {
        Method countdown = shell.getClass().getMethod(
                "setExplosionCountdown", int.class);
        countdown.invoke(shell, ticks);
    }

    private Entity createShell(ServerLevel level) {
        EntityType<?> type = heShellType;
        if (type == null) {
            type = ForgeRegistries.ENTITY_TYPES.getValue(CBC_HE_SHELL_ID);
        }
        Entity shell = type == null ? null : type.create(level);
        if (shell == null) {
            throw new IllegalStateException("CBC HE shell entity is unavailable");
        }
        return shell;
    }

    /** The released shell for this call, or null once it has left the world. */
    private static Entity findActiveShell(SupportSpawnContext context) {
        Entity shell = context.level().getEntity(context.callId());
        return shell != null && !shell.isRemoved()
                && shell.getPersistentData().getBoolean(JDAM_MARKER) ? shell : null;
    }

    /** Surface impact point under the target, or empty once it lies outside the world border. */
    private static Optional<Vec3> impactPosition(SupportSpawnContext context) {
        int blockX = (int) Math.floor(context.target().startX());
        int blockZ = (int) Math.floor(context.target().startZ());
        int surfaceY = context.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        BlockPos impactBlock = BlockPos.containing(context.target().startX(),
                surfaceY, context.target().startZ());
        if (!context.level().getWorldBorder().isWithinBounds(impactBlock)) {
            return Optional.empty();
        }
        double impactY = Math.min(context.level().getMaxBuildHeight() - 2.0D,
                Math.max(context.level().getMinBuildHeight() + 1.0D,
                        surfaceY + 0.25D));
        return Optional.of(new Vec3(context.target().startX(), impactY,
                context.target().startZ()));
    }

    private static void invokeOrientation(Entity shell, Vec3 orientation)
            throws ReflectiveOperationException {
        Method method = shell.getClass().getMethod("setOrientation", Vec3.class);
        method.invoke(shell, orientation);
    }
}
