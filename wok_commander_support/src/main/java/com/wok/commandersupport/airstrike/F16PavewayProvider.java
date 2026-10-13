package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.support.adapter.AbstractSoftSupportProvider;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * F-16C 500 lb Paveway strike continuously corrected by one allied designator.
 *
 * <p>Nothing released (no designation, none of the four diagonal release points loaded, rejected
 * spawn, CBC fault before the shell entered the world) returns the cooldown. Once the shell is in
 * the world the call is spent: a lost shell only ends the mission, and a CBC fault or a mission
 * cancelled by the scheduler removes the shell first.</p>
 */
public final class F16PavewayProvider extends AbstractSoftSupportProvider {
    public static final float EXPLOSION_DAMAGE = 500.0F;
    public static final float EXPLOSION_RADIUS = 16.0F;
    public static final String EXPLOSION_PARTICLE = "LARGE";
    static final String SHELL_NAME = "宝石路航弹";
    static final String NO_DESIGNATION_REASON = "许可区内没有友军持续照射";
    /**
     * Played once at the release point, about 181 blocks from the spot. Audibility on the
     * ground comes from {@code attenuation_distance} in sounds.json (volume x distance).
     */
    static final float APPROACH_VOLUME = 3.0F;
    /** Persistent-data flag on every released Paveway shell. */
    static final String MARKER = "WokCommanderSupportF16Paveway";

    private static final String DESIGNATOR = "WokPavewayDesignator";
    private static final String DESIGNATOR_NAME = "WokPavewayDesignatorName";
    private static final String LAST_X = "WokPavewayLastX";
    private static final String LAST_Y = "WokPavewayLastY";
    private static final String LAST_Z = "WokPavewayLastZ";
    private static final String GUIDANCE_LOST = "WokPavewayGuidanceLost";

    private volatile EntityType<?> heShellType;

    public F16PavewayProvider() {
        super(WokCommanderSupportMod.F16C_PAVEWAY_ID);
    }

    @Override
    protected ProviderAvailability probeAvailability() {
        if (!ModList.get().isLoaded(MillenniumJdamProvider.CBC_MOD_ID)) {
            return ProviderAvailability.unavailable(
                    "需要安装 Create Big Cannons 5.11.4 或兼容版本");
        }
        if (!ArtilleryIndicatorDesignation.available()) {
            return ProviderAvailability.unavailable(
                    "需要安装卓越前线并提供火炮指示器");
        }
        if (!SuperbWarfareExplosionAdapter.available()) {
            return ProviderAvailability.unavailable(
                    "卓越前线 CustomExplosion 接口不可用");
        }
        EntityType<?> resolved = ForgeRegistries.ENTITY_TYPES.getValue(
                MillenniumJdamProvider.CBC_HE_SHELL_ID);
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
        if (context.stepIndex() == 0) {
            launch(context);
        } else if (context.stepIndex()
                < WokCommanderSupportMod.F16C_PAVEWAY_FLIGHT_TICKS) {
            correctFlight(context);
        } else if (context.stepIndex()
                == WokCommanderSupportMod.F16C_PAVEWAY_FLIGHT_TICKS) {
            detonate(context);
        } else {
            throw new IllegalArgumentException("Unexpected Paveway mission step "
                    + context.stepIndex());
        }
    }

    /** No allied designator in the permit at release: end the mission and return the cooldown. */
    static SupportSpawnException noDesignation() {
        return SupportSpawnException.notDelivered(NO_DESIGNATION_REASON);
    }

    private void launch(SupportSpawnContext context) throws SupportSpawnException {
        ServerLevel level = context.level();
        Optional<ArtilleryIndicatorDesignation.Designation> acquired =
                ArtilleryIndicatorDesignation.acquire(level, context.faction(),
                        context.target(), WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
        if (acquired.isEmpty()) {
            notifyOwner(context, "message.wok_commander_support.paveway_no_designation");
            WokCommanderSupportMod.LOGGER.info(
                    "F-16C Paveway {} not released: no allied continuous designation",
                    context.callId());
            throw noDesignation();
        }

        ArtilleryIndicatorDesignation.Designation designation = acquired.get();
        PavewayGuidancePlan plan = loadedReleasePlan(level, designation).orElse(null);
        if (plan == null) {
            // A shell added to an unloaded chunk is invisible to getEntity: it would be a dud
            // that silently spends the call, so treat it as never released.
            WokCommanderSupportMod.LOGGER.info(
                    "F-16C Paveway {} not released: none of the four release points around "
                            + "[{}, {}, {}] is loaded",
                    context.callId(), designation.position().x, designation.position().y,
                    designation.position().z);
            throw AirstrikeFailures.releaseUnloaded();
        }
        Vec3 spawn = plan.spawn();
        Vec3 velocity = PavewayGuidancePlan.velocityTo(spawn,
                PavewayGuidancePlan.guidancePoint(designation.position()),
                plan.flightTicks());
        Entity shell;
        try {
            shell = createShell(level);
            shell.setUUID(context.callId());
            shell.setPos(spawn.x, spawn.y, spawn.z);
            shell.setDeltaMovement(velocity);
            shell.setNoGravity(true);
            shell.noPhysics = true;
            invokeOrientation(shell, velocity.normalize());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw AirstrikeFailures.brokenBeforeRelease(failure);
        }
        CompoundTag data = shell.getPersistentData();
        data.putBoolean(MARKER, true);
        data.putUUID(DESIGNATOR, designation.playerId());
        data.putString(DESIGNATOR_NAME, designation.playerName());
        saveDesignation(data, designation.position());
        if (!level.addFreshEntity(shell)) {
            throw AirstrikeFailures.spawnRejected();
        }
        playSound(level, spawn, CommanderSupportSounds.PAVEWAY_F16_APPROACH, APPROACH_VOLUME,
                context.callId());
        notifyOwner(context, "message.wok_commander_support.paveway_designation_acquired",
                designation.playerName());
        notifyPlayer(level, designation.playerId(),
                "message.wok_commander_support.paveway_designator_linked");
        WokCommanderSupportMod.LOGGER.info(
                "Released F-16C Paveway {} under continuous guidance from {}",
                context.callId(), designation.playerName());
    }

    /**
     * First of the four diagonal release points whose chunk has finished loading, trying the one
     * nearest the designator first. Checking a candidate never loads or waits for its chunk.
     */
    private static Optional<PavewayGuidancePlan> loadedReleasePlan(ServerLevel level,
            ArtilleryIndicatorDesignation.Designation designation) {
        ServerPlayer designator = level.getServer().getPlayerList()
                .getPlayer(designation.playerId());
        Vec3 preferNear = designator == null ? null : designator.position();
        for (PavewayGuidancePlan candidate : PavewayGuidancePlan.releaseCandidates(
                designation.position(), preferNear,
                WokCommanderSupportMod.F16C_PAVEWAY_FLIGHT_TICKS)) {
            if (ArtilleryIndicatorDesignation.chunkLoaded(level,
                    BlockPos.containing(candidate.spawn()))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * The scheduler cancelled the mission after release (requester left the faction, footprint
     * unloaded, provider disabled by another mission): remove the bomb instead of leaving it to
     * drift on its last vector as a dud.
     */
    @Override
    public void abandon(SupportSpawnContext context) {
        Entity shell = activeShell(context);
        if (shell != null) {
            shell.discard();
            WokCommanderSupportMod.LOGGER.info(
                    "F-16C Paveway {} was withdrawn because its mission was cancelled",
                    context.callId());
        }
    }

    private void correctFlight(SupportSpawnContext context) throws SupportSpawnException {
        Entity shell = activeShell(context);
        if (shell == null) {
            throw AirstrikeFailures.shellLost(SHELL_NAME);
        }
        try {
            steer(context, shell);
        } catch (ReflectiveOperationException failure) {
            shell.discard();
            throw AirstrikeFailures.brokenAfterRelease(failure);
        } catch (RuntimeException | LinkageError failure) {
            // Never leave a released bomb drifting on a stale vector after a guidance fault.
            shell.discard();
            throw failure;
        }
    }

    private void steer(SupportSpawnContext context, Entity shell)
            throws ReflectiveOperationException {
        CompoundTag data = shell.getPersistentData();
        if (!data.getBoolean(GUIDANCE_LOST)) {
            UUID designatorId = data.getUUID(DESIGNATOR);
            Optional<ArtilleryIndicatorDesignation.Designation> update =
                    ArtilleryIndicatorDesignation.update(context.level(), designatorId,
                            context.faction(), context.target(),
                            WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
            if (update.isPresent()) {
                saveDesignation(data, update.get().position());
            } else {
                data.putBoolean(GUIDANCE_LOST, true);
                notifyOwner(context,
                        "message.wok_commander_support.paveway_guidance_lost");
                notifyPlayer(context.level(), designatorId,
                        "message.wok_commander_support.paveway_designator_lost");
                WokCommanderSupportMod.LOGGER.info(
                        "F-16C Paveway {} lost guidance and retained its last correction",
                        context.callId());
            }
        }
        Vec3 destination = PavewayGuidancePlan.guidancePoint(loadDesignation(data));
        int remainingTicks = WokCommanderSupportMod.F16C_PAVEWAY_FLIGHT_TICKS
                - context.stepIndex();
        Vec3 velocity = PavewayGuidancePlan.velocityTo(shell.position(), destination,
                remainingTicks);
        shell.setDeltaMovement(velocity);
        invokeOrientation(shell, velocity.normalize());
    }

    private void detonate(SupportSpawnContext context) throws SupportSpawnException {
        Entity shell = activeShell(context);
        if (shell == null) {
            WokCommanderSupportMod.LOGGER.info(
                    "F-16C Paveway {} was already gone at its detonation step",
                    context.callId());
            return;
        }
        Vec3 impact = loadDesignation(shell.getPersistentData());
        shell.setPos(impact.x, impact.y, impact.z);
        shell.setDeltaMovement(Vec3.ZERO);
        try {
            SuperbWarfareExplosionAdapter.explode(shell, context.owner(), impact,
                    EXPLOSION_DAMAGE, EXPLOSION_RADIUS, EXPLOSION_PARTICLE);
            shell.discard();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.error(
                    "Superb Warfare Paveway explosion failed; using CBC fallback for {}",
                    context.callId(), failure);
            try {
                setCbcExplosionCountdown(shell, 0);
            } catch (ReflectiveOperationException | RuntimeException
                     | LinkageError fallbackFailure) {
                fallbackFailure.addSuppressed(failure);
                shell.discard();
                throw AirstrikeFailures.brokenAfterRelease(fallbackFailure);
            }
        }
        playSound(context.level(), impact, CommanderSupportSounds.PAVEWAY_BOMB_TAIL, 3.0F,
                context.callId());
        WokCommanderSupportMod.LOGGER.info(
                "F-16C Paveway {} detonated at its last valid designation [{}, {}, {}]",
                context.callId(), impact.x, impact.y, impact.z);
    }

    private Entity createShell(ServerLevel level) {
        EntityType<?> type = heShellType;
        if (type == null) {
            type = ForgeRegistries.ENTITY_TYPES.getValue(
                    MillenniumJdamProvider.CBC_HE_SHELL_ID);
        }
        Entity shell = type == null ? null : type.create(level);
        if (shell == null) {
            throw new IllegalStateException("CBC HE shell entity is unavailable");
        }
        return shell;
    }

    private static Entity activeShell(SupportSpawnContext context) {
        Entity shell = context.level().getEntity(context.callId());
        return shell != null && !shell.isRemoved()
                && shell.getPersistentData().getBoolean(MARKER) ? shell : null;
    }

    private static void saveDesignation(CompoundTag data, Vec3 position) {
        data.putDouble(LAST_X, position.x);
        data.putDouble(LAST_Y, position.y);
        data.putDouble(LAST_Z, position.z);
    }

    private static Vec3 loadDesignation(CompoundTag data) {
        return new Vec3(data.getDouble(LAST_X), data.getDouble(LAST_Y),
                data.getDouble(LAST_Z));
    }

    private static void notifyOwner(SupportSpawnContext context, String key,
                                    Object... arguments) {
        if (context.owner() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(key, arguments), true);
        }
    }

    private static void notifyPlayer(ServerLevel level, UUID playerId, String key) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    /** Sound is cosmetic: a failure is logged and never changes the mission outcome. */
    private static void playSound(ServerLevel level, Vec3 position,
                                  RegistryObject<SoundEvent> sound, float volume,
                                  UUID callId) {
        try {
            level.playSound(null, position.x, position.y, position.z, sound.get(),
                    SoundSource.HOSTILE, volume, 1.0F);
        } catch (RuntimeException failure) {
            WokCommanderSupportMod.LOGGER.warn("F-16C Paveway {} could not play sound {}",
                    callId, sound.getId(), failure);
        }
    }

    private static void setCbcExplosionCountdown(Entity shell, int ticks)
            throws ReflectiveOperationException {
        Method countdown = shell.getClass().getMethod(
                "setExplosionCountdown", int.class);
        countdown.invoke(shell, ticks);
    }

    private static void invokeOrientation(Entity shell, Vec3 orientation)
            throws ReflectiveOperationException {
        Method method = shell.getClass().getMethod("setOrientation", Vec3.class);
        method.invoke(shell, orientation);
    }
}
