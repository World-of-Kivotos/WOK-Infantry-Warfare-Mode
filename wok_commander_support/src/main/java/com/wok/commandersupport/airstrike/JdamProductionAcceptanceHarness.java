package com.wok.commandersupport.airstrike;

import com.mojang.authlib.GameProfile;
import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTarget;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** One-shot production-runtime acceptance hook; inert unless explicitly enabled by JVM property. */
@Mod.EventBusSubscriber(modid = WokCommanderSupportMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JdamProductionAcceptanceHarness {
    private static final String ENABLE_PROPERTY =
            "wok.commanderSupport.runJdamAcceptance";
    private static final String RESULT_PROPERTY =
            "wok.commanderSupport.jdamAcceptanceResult";
    private static AcceptanceState active;

    private JdamProductionAcceptanceHarness() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || active != null) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            ServerLevel level = server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            double targetX = spawn.getX() + 0.5D;
            double targetZ = spawn.getZ() + 0.5D;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    spawn.getX(), spawn.getZ());
            validatePavewayDesignation(server, level, targetX, targetZ, surfaceY);
            ArmorStand owner = new ArmorStand(level, targetX + 40.0D,
                    surfaceY + 0.25D, targetZ);
            if (!level.addFreshEntity(owner)) {
                throw new IllegalStateException("acceptance owner could not be spawned");
            }
            ArmorStand witness = new ArmorStand(level, targetX,
                    surfaceY + 0.25D, targetZ);
            if (!level.addFreshEntity(witness)) {
                throw new IllegalStateException("acceptance witness could not be spawned");
            }

            MillenniumJdamProvider provider = new MillenniumJdamProvider();
            ProviderAvailability availability = provider.availability();
            if (!availability.available()) {
                throw new IllegalStateException("provider unavailable: "
                        + availability.reason());
            }
            UUID callId = UUID.randomUUID();
            SupportDefinition definition = WokCommanderSupportMod.millenniumJdamDefinition();
            if (definition.inboundTicks()
                    != WokCommanderSupportMod.MILLENNIUM_JDAM_INBOUND_TICKS) {
                throw new IllegalStateException("JDAM inbound delay is not ten seconds");
            }
            SupportTarget target = SupportTarget.point(level.dimension().location(),
                    targetX, targetZ);
            provider.executeStep(new SupportSpawnContext(level, owner, callId,
                    definition, target, 0, Faction.BLUE));
            Entity shell = level.getEntity(callId);
            if (shell == null) {
                throw new IllegalStateException("CBC HE shell was not spawned");
            }
            double initialSpeed = shell.getDeltaMovement().length();
            if (!MillenniumJdamProvider.CBC_HE_SHELL_ID.equals(
                    shell.getType().builtInRegistryHolder().key().location())) {
                throw new IllegalStateException("unexpected projectile type");
            }
            if (initialSpeed <= 10.0D
                    || Math.abs(shell.getDeltaMovement().x) > 1.0E-9D
                    || shell.getDeltaMovement().y >= 0.0D
                    || Math.abs(shell.getDeltaMovement().z) > 1.0E-9D) {
                throw new IllegalStateException("projectile is not a fast vertical drop");
            }
            active = new AcceptanceState(server, level, owner, witness, provider, callId,
                    definition, target, initialSpeed, witness.getHealth(), 0);
        } catch (Throwable failure) {
            finish(server, false, "startup failure: " + concise(failure));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        AcceptanceState state = active;
        if (state == null || event.phase != TickEvent.Phase.END) {
            return;
        }
        int elapsed = state.elapsedTicks() + 1;
        active = state.withElapsedTicks(elapsed);
        try {
            if (elapsed > 0
                    && elapsed < WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                    + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS
                    && elapsed % WokCommanderSupportMod.MILLENNIUM_JDAM_STEP_INTERVAL_TICKS == 0) {
                int stepIndex = elapsed
                        / WokCommanderSupportMod.MILLENNIUM_JDAM_STEP_INTERVAL_TICKS;
                state.provider().executeStep(new SupportSpawnContext(state.level(),
                        state.owner(), state.callId(), state.definition(), state.target(),
                        stepIndex, Faction.BLUE));
            }
            if (elapsed == WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS) {
                Entity grounded = state.level().getEntity(state.callId());
                if (grounded == null || grounded.isRemoved()
                        || grounded.getDeltaMovement().lengthSqr() > 1.0E-9D
                        || Math.abs(grounded.getX() - state.target().startX()) > 0.05D
                        || Math.abs(grounded.getZ() - state.target().startZ()) > 0.05D) {
                    throw new IllegalStateException(
                            "projectile did not enter a stationary grounded-fuse state");
                }
                if (!state.witness().isRemoved()
                        && state.witness().getHealth() < state.witnessHealth()) {
                    throw new IllegalStateException("explosion occurred at impact without delay");
                }
            }
            if (elapsed == WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                    + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS / 2) {
                Entity delayed = state.level().getEntity(state.callId());
                if (delayed == null || delayed.isRemoved()) {
                    throw new IllegalStateException(
                            "projectile did not remain grounded during the delay fuse");
                }
                if (!state.witness().isRemoved()
                        && state.witness().getHealth() < state.witnessHealth()) {
                    throw new IllegalStateException("delay fuse exploded before one second");
                }
            }
            int explosionTick = WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                    + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS;
            if (elapsed == explosionTick) {
                state.provider().executeStep(new SupportSpawnContext(state.level(),
                        state.owner(), state.callId(), state.definition(), state.target(),
                        3, Faction.BLUE));
            }
            if (elapsed >= explosionTick + 4) {
                Entity remaining = state.level().getEntity(state.callId());
                if (remaining != null && !remaining.isRemoved()) {
                    throw new IllegalStateException("CBC HE shell remained after detonation");
                }
                if (!state.witness().isRemoved()
                        && state.witness().getHealth() >= state.witnessHealth()) {
                    throw new IllegalStateException(
                            "target witness was not affected by a CBC HE explosion");
                }
                finish(state.server(), true,
                        "entityType=" + MillenniumJdamProvider.CBC_HE_SHELL_ID + "\n"
                                + "inboundTicks="
                                + WokCommanderSupportMod.MILLENNIUM_JDAM_INBOUND_TICKS
                                + "\nspawnHeight=" + JdamFlightPlan.VERTICAL_OFFSET
                                + "\n"
                                + "initialSpeed=" + state.initialSpeed() + "\n"
                                + "flightTicks="
                                + WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                                + "\ndelayFuseTicks="
                                + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS
                                + "\nfastVerticalDrop=true"
                                + "\ngroundedDelayObserved=true"
                                + "\nexplodedAndRemoved=true"
                                + "\nexplosionAffectedTargetWitness=true"
                                + "\npavewayAlliedIndicator=true"
                                + "\npavewayMovingEntityTracked=true"
                                + "\npavewayReleaseBreaksGuidance=true");
            }
        } catch (Throwable failure) {
            finish(state.server(), false, "tick failure: " + concise(failure));
        }
    }

    private static void finish(MinecraftServer server, boolean passed, String details) {
        active = null;
        String text = "status=" + (passed ? "PASS" : "FAIL") + "\n" + details + "\n";
        try {
            Path result = Path.of(System.getProperty(RESULT_PROPERTY,
                    "jdam_production_acceptance.txt")).toAbsolutePath().normalize();
            Files.createDirectories(result.getParent());
            Files.writeString(result, text, StandardCharsets.UTF_8);
        } catch (Exception writeFailure) {
            writeFailure.printStackTrace();
        }
        server.halt(false);
    }

    private static String concise(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static void validatePavewayDesignation(MinecraftServer server,
                                                   ServerLevel level,
                                                   double targetX,
                                                   double targetZ,
                                                   int surfaceY) {
        ServerPlayer designator = new ServerPlayer(server, level, new GameProfile(
                UUID.nameUUIDFromBytes("wok-paveway-production-designator"
                        .getBytes(StandardCharsets.UTF_8)), "wok-paveway-designator"));
        ArmorStand movingTarget = new ArmorStand(level, targetX + 9.0D,
                surfaceY + 3.0D, targetZ);
        try {
            designator.setPos(targetX, surfaceY + 3.0D, targetZ);
            level.addNewPlayer(designator);
            BattleService battle = BattleService.get(server).orElseThrow();
            if (!battle.ensurePlayer(designator).success()
                    || !battle.selectFormation(designator, Faction.BLUE, "default",
                            40, 40).success()) {
                throw new IllegalStateException(
                        "production designator could not join the caller faction");
            }
            Item indicator = ForgeRegistries.ITEMS.getValue(
                    ArtilleryIndicatorDesignation.ITEM_ID);
            if (indicator == null) {
                throw new IllegalStateException("artillery indicator is not registered");
            }
            designator.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(indicator));
            designator.startUsingItem(InteractionHand.MAIN_HAND);
            if (!level.addFreshEntity(movingTarget)) {
                throw new IllegalStateException("moving designation target could not spawn");
            }
            lookAt(designator, movingTarget.getBoundingBox().getCenter());
            SupportTarget area = SupportTarget.point(level.dimension().location(),
                    movingTarget.getX(), movingTarget.getZ());
            ArtilleryIndicatorDesignation.Designation first =
                    ArtilleryIndicatorDesignation.acquire(level, Faction.BLUE, area,
                                    WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS)
                            .orElseThrow(() -> new IllegalStateException(
                                    "allied indicator did not acquire the moving entity"));
            movingTarget.setPos(movingTarget.getX() + 4.0D, movingTarget.getY(),
                    movingTarget.getZ());
            lookAt(designator, movingTarget.getBoundingBox().getCenter());
            ArtilleryIndicatorDesignation.Designation moved =
                    ArtilleryIndicatorDesignation.update(level, designator.getUUID(),
                                    Faction.BLUE, area,
                                    WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS)
                            .orElseThrow(() -> new IllegalStateException(
                                    "continuous indicator lost the moved entity"));
            if (moved.position().x <= first.position().x + 2.0D) {
                throw new IllegalStateException(
                        "designation did not follow the moving entity");
            }
            designator.stopUsingItem();
            if (ArtilleryIndicatorDesignation.update(level, designator.getUUID(),
                    Faction.BLUE, area,
                    WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS).isPresent()) {
                throw new IllegalStateException(
                        "designation survived after the indicator was released");
            }
        } finally {
            movingTarget.discard();
            level.removePlayerImmediately(designator,
                    Entity.RemovalReason.DISCARDED);
        }
    }

    private static void lookAt(ServerPlayer player, Vec3 target) {
        Vec3 direction = target.subtract(player.getEyePosition()).normalize();
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x,
                direction.z)));
        player.setXRot((float) Math.toDegrees(-Math.asin(direction.y)));
        player.setYHeadRot(player.getYRot());
    }

    private record AcceptanceState(MinecraftServer server, ServerLevel level,
                                   ArmorStand owner, ArmorStand witness,
                                   MillenniumJdamProvider provider,
                                   UUID callId, SupportDefinition definition,
                                   SupportTarget target, double initialSpeed,
                                   float witnessHealth, int elapsedTicks) {
        AcceptanceState withElapsedTicks(int updated) {
            return new AcceptanceState(server, level, owner, witness, provider, callId,
                    definition, target, initialSpeed, witnessHealth, updated);
        }
    }
}
