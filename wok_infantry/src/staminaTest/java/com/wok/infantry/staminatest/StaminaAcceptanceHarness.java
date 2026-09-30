package com.wok.infantry.staminatest;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.ClientStaminaState;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.integration.tacz.TaczStaminaAdapter;
import com.wok.infantry.stamina.StaminaEvents;
import com.wok.infantry.stamina.StaminaMath;
import com.wok.infantry.stamina.StaminaState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Real local input, vanilla sprint setter/packets, server persistence and rendered fatigue. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT)
public final class StaminaAcceptanceHarness {
    private static int phase, phaseTicks, ticks, visual;
    private static boolean opened, finished;
    private static CompletableFuture<Void> serverWork;
    private static CompletableFuture<List<String>> recoveryChecks;
    private static int recoveryDelayTicks;
    private static float releasedAimStamina;
    private static net.minecraft.client.KeyMapping aimKey;
    private static Vec3 movementStart;
    private static double sprintSpeed;
    private static String capture;
    private static float cameraYawBefore, cameraPitchBefore, maximumCameraSway;
    private static final float[] RESERVES = {50, 40, 25, 10, 0};
    private static final List<String> checks = new ArrayList<>();
    private static final List<String> screenshots = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void input(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (aimKey != null) aimKey.setDown(phase == 20);
        boolean moving = phase == 2 || phase == 3 || phase == 4;
        // During phase 4 also alternate forward input to exercise double-tap activation.
        mc.options.keyUp.setDown(moving && !(phase == 4 && phaseTicks >= 12 && phaseTicks < 15));
        mc.options.keySprint.setDown(moving && !(phase == 4 && phaseTicks >= 10));
        if (phase == 4) mc.player.setSprinting(true); // Same vanilla entry point used by mods.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (++ticks > 2200) throw new IllegalStateException("stamina acceptance timeout, phase " + phase);
            phaseTicks++;
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (!opened && mc.screen instanceof TitleScreen && phaseTicks > 40) {
                    opened = true;
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "wok_stamina_test");
                } else if (mc.screen instanceof ConfirmScreen screen && phaseTicks > 60) {
                    screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(button -> button.active).min(java.util.Comparator.comparingInt(Button::getX))
                            .ifPresent(Button::onPress);
                }
                return;
            }
            if (serverWork != null) {
                if (!serverWork.isDone()) return;
                serverWork.join();
                serverWork = null;
            }
            if (phase == 0) {
                if (phaseTicks < 100) return;
                require(ModList.get().isLoaded("tacz") == Boolean.getBoolean("wok.staminaTestTacz"),
                        "expected TaCZ fixture presence");
                mc.options.pauseOnLostFocus = false;
                mc.getTutorial().setStep(TutorialSteps.NONE);
                mc.getToasts().clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                onServer(p -> {
                    recoveryDelayTicks = InfantryServerConfig.staminaRecoveryDelayTicks();
                    require(recoveryDelayTicks == 60, "default recovery cooldown is three seconds");
                    p.server.getPlayerList().op(p.getGameProfile());
                    require(DeploymentService.get(p).orElseThrow()
                            .setVehicleTestMode(p.createCommandSourceStack(), p, true).success(),
                            "administrator test mode isolates deployment restrictions");
                    var level = p.server.overworld();
                    for (int x = -4; x <= 4; x++) for (int z = -4; z <= 90; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y <= 104; y++) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                    }
                    p.teleportTo(level, 0.5, 101, 0.5, 0, 0);
                    p.connection.resetPosition();
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    p.setHealth(20);
                    p.getFoodData().setFoodLevel(20);
                    StaminaEvents.overwrite(p, 100, 100);
                    p.setSprinting(true);
                    require(p.isSprinting(), "healthy server player may sprint");
                });
                next();
            } else if (phase == 1) {
                if (!ClientStaminaState.snapshot().enabled() || phaseTicks < 20) return;
                ClientFormationState.clear();
                mc.setScreen(null);
                mc.player.setYRot(0);
                mc.player.setXRot(0);
                movementStart = mc.player.position();
                next();
            } else if (phase == 2) {
                mc.setScreen(null);
                if (phaseTicks < 30) return;
                require(mc.player.isSprinting(), "held vanilla sprint key activates healthy client sprint");
                sprintSpeed = (mc.player.getZ() - movementStart.z) / phaseTicks;
                require(sprintSpeed > 0.23, "healthy client moves at sprint speed: " + sprintSpeed);
                onServer(p -> {
                    StaminaEvents.overwrite(p, 100, 0);
                    require(!p.isSprinting(), "exhaustion immediately ends active server sprint");
                    for (int attempt = 0; attempt < 20; attempt++) {
                        p.setSprinting(true);
                        if (p.isSprinting()) throw new IllegalStateException("server sprint setter bypass");
                    }
                    require(p.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.101,
                            "server sprint speed modifier removed before movement");
                    StaminaEvents.overwrite(p, 100, 5);
                    p.setSprinting(true);
                    StaminaEvents.onPlayerJump(new net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(p));
                    require(StaminaState.load(p).legs() == 0 && !p.isSprinting(),
                            "jump depletion immediately removes active sprint");
                    // Restore a small positive reserve; real held movement must drain it naturally.
                    StaminaEvents.overwrite(p, 100, 1);
                    p.setSprinting(true);
                    require(p.isSprinting(), "positive non-exhausted reserve permits sprint");
                });
                next();
            } else if (phase == 3) {
                if (phaseTicks < 8) return;
                if (!ClientStaminaState.snapshot().sprintBlocked()) return;
                require(ClientStaminaState.snapshot().legs() == 0 && !mc.player.isSprinting(),
                        "natural sprint drain reaches zero and exhaustion sync stops active client sprint");
                movementStart = mc.player.position();
                next();
            } else if (phase == 4) {
                mc.setScreen(null);
                require(!mc.player.isSprinting(), "client rejects held/double-tap/direct sprint at tick " + phaseTicks);
                require(mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.101,
                        "client has no sprint speed modifier at tick " + phaseTicks);
                if (phaseTicks % 3 == 0) {
                    mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player,
                            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                }
                if (phaseTicks <= 26) require(ClientStaminaState.snapshot().legs() == 0,
                        "exhausted stamina does not immediately recover during cooldown");
                if (phaseTicks < recoveryDelayTicks + 8) return;
                double distance = mc.player.getZ() - movementStart.z;
                require(distance > 2.0 && distance / phaseTicks < sprintSpeed * 0.9,
                        "exhaustion allows walking but removes sprint speed: " + distance / phaseTicks);
                require(ClientStaminaState.snapshot().legs() > 0 && ClientStaminaState.snapshot().sprintBlocked(),
                        "nonzero partial recovery stays locked on client");
                onServer(p -> {
                    require(!p.isSprinting(), "repeated real START_SPRINTING packets rejected");
                    StaminaState state = StaminaState.load(p);
                    require(state.sprintBlocked(), "recovery lock saved in player NBT");
                    ServerPlayer replacement = new ServerPlayer(p.server, p.serverLevel(),
                            new GameProfile(UUID.randomUUID(), "StaminaCopy"));
                    replacement.setGameMode(GameType.SURVIVAL);
                    StaminaState.copy(p, replacement);
                    replacement.setSprinting(true);
                    require(!replacement.isSprinting() && StaminaState.load(replacement).sprintBlocked(),
                            "recreated player retains exhaustion lock during partial recovery");
                });
                next();
            } else if (phase == 5) {
                if (ClientStaminaState.snapshot().sprintBlocked()) return;
                require(ClientStaminaState.snapshot().legs() >= 15,
                        "client unlocks at authoritative default recovery threshold");
                mc.player.setSprinting(true);
                require(mc.player.isSprinting(), "client sprint resumes after sufficient recovery");
                onServer(p -> {
                    p.setSprinting(true);
                    require(p.isSprinting(), "server sprint resumes after sufficient recovery");
                    p.setGameMode(GameType.CREATIVE);
                    StaminaEvents.overwrite(p, 0, 0);
                    p.setSprinting(true);
                    require(p.isSprinting(), "creative mode is exempt from server exhaustion");
                });
                next();
            } else if (phase == 6) {
                if (ClientStaminaState.snapshot().enabled() || !mc.player.getAbilities().instabuild) return;
                mc.player.setSprinting(true);
                require(mc.player.isSprinting(), "creative mode is exempt from client exhaustion");
                onServer(p -> {
                    p.setGameMode(GameType.SURVIVAL);
                    p.setSprinting(false);
                    StaminaEvents.overwrite(p, 100, 100);
                    if (Boolean.getBoolean("wok.staminaTestTacz")) p.getInventory().setItem(0, gun());
                    p.getInventory().selected = 0;
                    p.inventoryMenu.broadcastChanges();
                });
                next();
            } else if (phase == 7) {
                if (!ClientStaminaState.snapshot().enabled() || phaseTicks < 20) return;
                if (Boolean.getBoolean("wok.staminaTestTacz")) {
                    require(TaczStaminaAdapter.isHoldingGun(mc.player), "real TaCZ gun reaches client render path");
                    aim(mc, true);
                    phase = 20;
                    phaseTicks = 0;
                    return;
                }
                prepareVisual(mc);
            } else if (phase == 8) {
                float reserve = RESERVES[visual % RESERVES.length];
                if (phaseTicks < 5) return;
                require(Math.abs(ClientStaminaState.snapshot().arms() - reserve) < 0.01F,
                        "visual stamina comes from server: " + reserve);
                boolean tacz = Boolean.getBoolean("wok.staminaTestTacz");
                require(reserve < 50 ? StaminaMath.swayIntensity(reserve, 100) > 0
                        : StaminaMath.swayIntensity(reserve, 100) == 0, "fifty-point sway boundary");
                if (tacz && reserve < 50) require(maximumCameraSway > 0.0001F,
                        "actual camera sway before exhaustion: " + reserve + " -> " + maximumCameraSway);
                if (reserve == 50 || !tacz) require(maximumCameraSway < 0.0001F,
                        "no fatigue camera sway at fifty or without TaCZ");
                checks.add("visual=" + reserve + ",scale=" + mc.options.guiScale().get()
                        + ",intensity=" + StaminaMath.swayIntensity(reserve, 100)
                        + ",cameraDelta=" + maximumCameraSway);
                capture = "stamina-" + (visual < 5 ? "320x240" : "960x720") + "-" + (int) reserve + ".png";
                next();
            } else if (phase == 9) {
                if (capture != null) return;
                if (++visual == RESERVES.length * 2) {
                    mc.player.setSprinting(false);
                    onServer(p -> recoveryChecks = StaminaRecoveryAcceptance.start(p));
                    phase = 10;
                    return;
                }
                prepareVisual(mc);
            } else if (phase == 10) {
                if (!recoveryChecks.isDone()) return;
                checks.addAll(recoveryChecks.join());
                finish(mc, null);
            } else if (phase == 20) {
                if (phaseTicks < 20) return;
                require(TaczStaminaAdapter.isAiming(mc.player) && ClientStaminaState.snapshot().arms() < 100,
                        "real TaCZ aiming consumes arm stamina: aiming=" + TaczStaminaAdapter.isAiming(mc.player)
                                + ", arms=" + ClientStaminaState.snapshot().arms());
                aim(mc, false);
                next();
            } else if (phase == 21) {
                if (phaseTicks < 8) return;
                require(!TaczStaminaAdapter.isAiming(mc.player), "real TaCZ aiming released");
                releasedAimStamina = ClientStaminaState.snapshot().arms();
                next();
            } else if (phase == 22) {
                if (phaseTicks <= 30) require(ClientStaminaState.snapshot().arms() == releasedAimStamina,
                        "arm stamina remains unchanged after releasing aim, tick " + phaseTicks);
                if (phaseTicks < recoveryDelayTicks + 6) return;
                require(ClientStaminaState.snapshot().arms() > releasedAimStamina,
                        "arm stamina recovers after the aiming cooldown");
                prepareVisual(mc);
            }
        } catch (Throwable failure) { finish(mc, failure); }
    }

    private static void prepareVisual(Minecraft mc) {
        mc.setScreen(null);
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.player.setSprinting(false);
        mc.getWindow().setWindowed(960, 720);
        mc.options.guiScale().set(visual < 5 ? 3 : 1);
        mc.resizeDisplay();
        int expectedWidth = visual < 5 ? 320 : 960;
        require(mc.getWindow().getGuiScaledWidth() == expectedWidth, "real logical screen width " + expectedWidth);
        float reserve = RESERVES[visual % RESERVES.length];
        mc.gui.setOverlayMessage(Component.literal("体力验收 / 手部 " + (int) reserve + " / 腿部 100"), false);
        maximumCameraSway = 0;
        onServer(p -> StaminaEvents.overwrite(p, reserve, 100));
        phase = 8;
        phaseTicks = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeCamera(ViewportEvent.ComputeCameraAngles event) {
        cameraYawBefore = event.getYaw();
        cameraPitchBefore = event.getPitch();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterCamera(ViewportEvent.ComputeCameraAngles event) {
        if (phase == 8 && phaseTicks >= 2) maximumCameraSway = Math.max(maximumCameraSway,
                Math.abs(event.getYaw() - cameraYawBefore) + Math.abs(event.getPitch() - cameraPitchBefore));
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var dir = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(dir);
            image.writeToFile(dir.resolve(capture));
            screenshots.add(capture);
            capture = null;
        } catch (Exception failure) { finish(mc, failure); }
    }

    private static ItemStack gun() {
        try {
            var type = Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder");
            Object builder = type.getMethod("create").invoke(null);
            builder = type.getMethod("setId", ResourceLocation.class).invoke(builder,
                    ResourceLocation.fromNamespaceAndPath("tacz", "m4a1"));
            return (ItemStack) type.getMethod("build").invoke(builder);
        } catch (Exception failure) { throw new IllegalStateException("Cannot create actual TaCZ fixture gun", failure); }
    }

    private static void aim(Minecraft mc, boolean aiming) throws ReflectiveOperationException {
        // TaCZ's held-input tick releases aim when its key is up, even after a direct aim call.
        aimKey = (net.minecraft.client.KeyMapping) Class.forName("com.tacz.guns.client.input.AimKey")
                .getField("AIM_KEY").get(null);
        aimKey.setDown(aiming);
        var type = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
        Object operator = type.getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
                .invoke(null, mc.player);
        type.getMethod("aim", boolean.class).invoke(operator, aiming);
    }

    private static void onServer(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        UUID id = mc.player.getUUID();
        serverWork = mc.getSingleplayerServer().submit(() -> action.accept(
                mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }

    private static void next() { phase++; phaseTicks = 0; }
    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException(label);
        checks.add("PASS " + label);
    }

    private static void finish(Minecraft mc, Throwable failure) {
        finished = true;
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        if (aimKey != null) aimKey.setDown(false);
        if (failure != null) WokInfantryMod.LOGGER.error("Stamina acceptance failed", failure);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("stamina-acceptance.txt"),
                    "status=" + (failure == null ? "PASS" : "FAIL") + "\nphase=" + phase
                            + "\ntacz=" + Boolean.getBoolean("wok.staminaTestTacz") + "\n"
                            + String.join("\n", checks) + "\nscreenshots=" + String.join(",", screenshots)
                            + "\nfailure=" + (failure == null ? "" : failure) + "\n", StandardCharsets.UTF_8);
        } catch (Exception error) { WokInfantryMod.LOGGER.error("Cannot save stamina result", error); }
        mc.stop();
    }
}
