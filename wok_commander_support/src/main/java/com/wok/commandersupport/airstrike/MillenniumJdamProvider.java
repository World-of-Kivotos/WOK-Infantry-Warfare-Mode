package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.support.adapter.AbstractSoftSupportProvider;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
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

/** CBC projectile with optional Superb Warfare CustomExplosion detonation. */
public final class MillenniumJdamProvider extends AbstractSoftSupportProvider {
    public static final String CBC_MOD_ID = "createbigcannons";
    public static final ResourceLocation CBC_HE_SHELL_ID =
            ResourceLocation.fromNamespaceAndPath(CBC_MOD_ID, "he_shell");
    private static final String JDAM_MARKER = "WokCommanderSupportJdam";
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
            throws ReflectiveOperationException {
        switch (context.stepIndex()) {
            case 0 -> launch(context);
            case 1 -> impactAndArm(context);
            case 2 -> requireActiveShell(context);
            case 3 -> completeDelayedExplosion(context);
            default -> throw new IllegalArgumentException(
                    "Unexpected JDAM mission step " + context.stepIndex());
        }
    }

    private void launch(SupportSpawnContext context)
            throws ReflectiveOperationException {
        ServerLevel level = context.level();
        Vec3 impact = impactPosition(context);
        JdamFlightPlan plan = JdamFlightPlan.fromImpact(impact.x, impact.y,
                impact.z, WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS);
        Entity shell = createShell(level);
        shell.setUUID(context.callId());
        shell.setPos(plan.spawn().x, plan.spawn().y, plan.spawn().z);
        shell.setDeltaMovement(plan.velocity());
        shell.setNoGravity(true);
        shell.noPhysics = true;
        invokeOrientation(shell, plan.velocity().normalize());
        shell.getPersistentData().putBoolean(JDAM_MARKER, true);
        if (!level.addFreshEntity(shell)) {
            throw new IllegalStateException("CBC HE shell could not be spawned");
        }
        level.playSound(null, plan.spawn().x, plan.spawn().y, plan.spawn().z,
                CommanderSupportSounds.JDAM_F15_APPROACH.get(),
                SoundSource.HOSTILE, 3.0F, 1.0F);
        WokCommanderSupportMod.LOGGER.info(
                "Spawned Millennium JDAM {} exactly 200 blocks above [{}, {}, {}] after the ten-second inbound delay",
                context.callId(), impact.x, impact.y, impact.z);
    }

    private void requireActiveShell(SupportSpawnContext context) {
        findActiveShell(context);
    }

    private void impactAndArm(SupportSpawnContext context)
            throws ReflectiveOperationException {
        ServerLevel level = context.level();
        Vec3 impact = impactPosition(context);
        Entity shell = findActiveShell(context);
        shell.setPos(impact.x, impact.y, impact.z);
        shell.setDeltaMovement(Vec3.ZERO);
        shell.setNoGravity(true);
        shell.noPhysics = true;
        invokeOrientation(shell, new Vec3(0.0D, -1.0D, 0.0D));
        boolean useSuperbWarfareExplosion = SuperbWarfareExplosionAdapter.available();
        shell.getPersistentData().putBoolean(SBW_EXPLOSION_MARKER,
                useSuperbWarfareExplosion);
        if (!useSuperbWarfareExplosion) {
            setCbcExplosionCountdown(shell,
                    WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS);
        }
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.NETHERITE_BLOCK_FALL, SoundSource.HOSTILE,
                2.0F, 0.65F);
        WokCommanderSupportMod.LOGGER.info(
                "Millennium JDAM {} struck the target and armed a two-second {} fuse",
                context.callId(), useSuperbWarfareExplosion
                        ? "Superb Warfare" : "CBC fallback");
    }

    private void completeDelayedExplosion(SupportSpawnContext context)
            throws ReflectiveOperationException {
        ServerLevel level = context.level();
        Vec3 impact = impactPosition(context);
        Entity shell = level.getEntity(context.callId());
        if (shell != null && !shell.isRemoved()
                && shell.getPersistentData().getBoolean(JDAM_MARKER)) {
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
                    setCbcExplosionCountdown(shell, 0);
                }
            } else {
                setCbcExplosionCountdown(shell, 0);
                WokCommanderSupportMod.LOGGER.info(
                        "Millennium JDAM {} used the CBC delayed-explosion fallback",
                        context.callId());
            }
        } else {
            WokCommanderSupportMod.LOGGER.info(
                    "Millennium JDAM {} completed its native CBC delayed explosion",
                    context.callId());
        }
        level.playSound(null, impact.x, impact.y, impact.z,
                CommanderSupportSounds.JDAM_BOMB_TAIL.get(),
                SoundSource.HOSTILE, 3.5F, 1.0F);
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

    private Entity findActiveShell(SupportSpawnContext context) {
        Entity shell = context.level().getEntity(context.callId());
        if (shell == null || shell.isRemoved()
                || !shell.getPersistentData().getBoolean(JDAM_MARKER)) {
            throw new IllegalStateException("JDAM shell disappeared before delayed impact");
        }
        return shell;
    }

    private static Vec3 impactPosition(SupportSpawnContext context) {
        int blockX = (int) Math.floor(context.target().startX());
        int blockZ = (int) Math.floor(context.target().startZ());
        int surfaceY = context.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        BlockPos impactBlock = BlockPos.containing(context.target().startX(),
                surfaceY, context.target().startZ());
        if (!context.level().getWorldBorder().isWithinBounds(impactBlock)) {
            throw new IllegalArgumentException("JDAM impact point is outside the world border");
        }
        double impactY = Math.min(context.level().getMaxBuildHeight() - 2.0D,
                Math.max(context.level().getMinBuildHeight() + 1.0D,
                        surfaceY + 0.25D));
        return new Vec3(context.target().startX(), impactY,
                context.target().startZ());
    }

    private static void invokeOrientation(Entity shell, Vec3 orientation)
            throws ReflectiveOperationException {
        Method method = shell.getClass().getMethod("setOrientation", Vec3.class);
        method.invoke(shell, orientation);
    }
}
