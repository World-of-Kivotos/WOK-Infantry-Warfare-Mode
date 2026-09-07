package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.adapter.AbstractSoftSupportProvider;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** F-16C 500 lb Paveway strike continuously corrected by one allied designator. */
public final class F16PavewayProvider extends AbstractSoftSupportProvider {
    public static final float EXPLOSION_DAMAGE = 500.0F;
    public static final float EXPLOSION_RADIUS = 16.0F;
    public static final String EXPLOSION_PARTICLE = "LARGE";

    private static final String MARKER = "WokCommanderSupportF16Paveway";
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
            throws ReflectiveOperationException {
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

    private void launch(SupportSpawnContext context)
            throws ReflectiveOperationException {
        Optional<ArtilleryIndicatorDesignation.Designation> acquired =
                ArtilleryIndicatorDesignation.acquire(context.level(), context.faction(),
                        context.target(), WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS);
        if (acquired.isEmpty()) {
            notifyOwner(context, "message.wok_commander_support.paveway_no_designation");
            WokCommanderSupportMod.LOGGER.info(
                    "F-16C Paveway {} aborted at release: no allied continuous designation",
                    context.callId());
            return;
        }

        ArtilleryIndicatorDesignation.Designation designation = acquired.get();
        PavewayGuidancePlan plan = PavewayGuidancePlan.fromDesignation(
                designation.position(), WokCommanderSupportMod.F16C_PAVEWAY_FLIGHT_TICKS);
        Entity shell = createShell(context.level());
        shell.setUUID(context.callId());
        shell.setPos(plan.spawn().x, plan.spawn().y, plan.spawn().z);
        Vec3 velocity = PavewayGuidancePlan.velocityTo(plan.spawn(),
                PavewayGuidancePlan.guidancePoint(designation.position()),
                plan.flightTicks());
        shell.setDeltaMovement(velocity);
        shell.setNoGravity(true);
        shell.noPhysics = true;
        invokeOrientation(shell, velocity.normalize());
        CompoundTag data = shell.getPersistentData();
        data.putBoolean(MARKER, true);
        data.putUUID(DESIGNATOR, designation.playerId());
        data.putString(DESIGNATOR_NAME, designation.playerName());
        saveDesignation(data, designation.position());
        if (!context.level().addFreshEntity(shell)) {
            throw new IllegalStateException("CBC HE shell could not be spawned");
        }
        notifyOwner(context, "message.wok_commander_support.paveway_designation_acquired",
                designation.playerName());
        notifyPlayer(context.level(), designation.playerId(),
                "message.wok_commander_support.paveway_designator_linked");
        WokCommanderSupportMod.LOGGER.info(
                "Released F-16C Paveway {} under continuous guidance from {}",
                context.callId(), designation.playerName());
    }

    private void correctFlight(SupportSpawnContext context)
            throws ReflectiveOperationException {
        Entity shell = activeShell(context);
        if (shell == null) {
            return;
        }
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

    private void detonate(SupportSpawnContext context)
            throws ReflectiveOperationException {
        Entity shell = activeShell(context);
        if (shell == null) {
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
            setCbcExplosionCountdown(shell, 0);
        }
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
