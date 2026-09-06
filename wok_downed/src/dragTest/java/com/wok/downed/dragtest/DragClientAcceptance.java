package com.wok.downed.dragtest;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedService;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Two real clients over loopback TCP; only teleport acknowledgements receive simulated latency. */
@Mod.EventBusSubscriber(modid = WokDownedMod.MOD_ID, value = Dist.CLIENT)
public final class DragClientAcceptance {
    private static final boolean HOST = System.getProperty("wok.downed.dragTestRole").equals("host");
    private static final String ROLE = HOST ? "host" : "guest";
    private static final Path ROOT = Path.of(System.getProperty("wok.downed.dragTestRoot"));
    private static final int PORT = 25576;
    private static final List<String> CHECKS = new ArrayList<>();
    private static final long DEADLINE = System.nanoTime() + TimeUnit.MINUTES.toNanos(4);
    private static boolean opened;
    private static boolean networkReady;
    private static volatile boolean finished;
    private static volatile int stage = -1;
    private static int elapsed;
    private static Vec3 start;
    private static Vec3 released;
    private static int capturedStage = -1;
    private static int capture = -1;
    private static int observedStage = -1;
    private static int stableTicks;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (!HOST || finished || event.phase != TickEvent.Phase.END) return;
        try {
            var server = event.getServer();
            if (!networkReady) {
                server.setUsesAuthentication(false);
                server.getConnection().startTcpServerListener(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), PORT);
                networkReady = true;
                Files.writeString(ROOT.resolve("ready"), "127.0.0.1:" + PORT);
            }
            var carrier = server.getPlayerList().getPlayerByName("DownedHost");
            var casualty = server.getPlayerList().getPlayerByName("DownedGuest");
            // The copied level metadata can place the host over water before the arena exists.
            // Keep it safe while the second real client starts and completes its login handshake.
            if (stage == -1 && carrier != null) {
                carrier.setGameMode(GameType.CREATIVE);
                carrier.setAirSupply(carrier.getMaxAirSupply());
            }
            if (carrier == null || casualty == null || !Files.exists(ROOT.resolve("guest-ready"))) return;
            if (stage == -1) {
                for (int x = -35; x <= 18; x++) for (int z = -8; z <= 22; z++) {
                    carrier.serverLevel().setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                    for (int y = 101; y <= 105; y++) carrier.serverLevel().setBlockAndUpdate(
                            new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                carrier.serverLevel().setDayTime(6000L);
                for (ServerPlayer p : List.of(carrier, casualty)) {
                    DownedService.clearState(p);
                    p.setGameMode(GameType.SURVIVAL);
                    p.setHealth(p.getMaxHealth());
                    p.fallDistance = 0;
                    p.setDeltaMovement(Vec3.ZERO);
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 4));
                }
                carrier.connection.teleport(0.5, 101, 0.5, 0, 0);
                casualty.connection.teleport(0.5, 101, -0.65, 0, 0);
                transition(0);
            }
            elapsed++;
            if (stage == 0 && elapsed > 80) {
                DownedService.enterDowned(casualty);
                require(DownedService.toggleDrag(carrier, casualty), "drag started; carrier=" + carrier.position()
                        + " downed=" + DownedService.isDowned(carrier) + " alive=" + carrier.isAlive()
                        + "; casualty=" + casualty.position() + " alive=" + casualty.isAlive());
                start = casualty.position();
                transition(1);
            } else if (stage == 1 && elapsed > 80) {
                transition(2);
            } else if (stage == 2 && seenByBoth(2)) {
                require(casualty.position().distanceToSqr(target(carrier)) < 0.04,
                        "server drag position survives connection tick with delayed acknowledgements");
                require(casualty.position().distanceToSqr(start) > 4, "server casualty moved with carrier");
                transition(3);
            } else if (stage == 3 && elapsed > 80) {
                transition(4);
            } else if (stage == 4 && seenByBoth(4)) {
                require(casualty.position().distanceToSqr(target(carrier)) < 0.04, "server follows turn");
                DownedService.stopDraggingByParticipant(carrier, false);
                released = casualty.position();
                transition(5);
            } else if (stage == 5 && elapsed > 60) {
                transition(6);
            } else if (stage == 6 && seenByBoth(6)) {
                require(horizontalDistanceSquared(casualty.position(), released) < 0.04, "released casualty stays put");
                DownedService.revive(casualty, null);
                transition(7);
            } else if (stage == 7 && seenByBoth(7)) {
                require(!DownedService.isDowned(casualty), "revive clears downed state");
                transition(8);
            }
            Vec3 expected = stage >= 5 && released != null ? released : target(carrier);
            Properties snapshot = new Properties();
            snapshot.setProperty("stage", Integer.toString(stage));
            snapshot.setProperty("casualty", Integer.toString(casualty.getId()));
            snapshot.setProperty("x", Double.toString(expected.x));
            snapshot.setProperty("y", Double.toString(expected.y));
            snapshot.setProperty("z", Double.toString(expected.z));
            if (start != null) {
                snapshot.setProperty("startX", Double.toString(start.x));
                snapshot.setProperty("startZ", Double.toString(start.z));
            }
            StringWriter out = new StringWriter();
            snapshot.store(out, "Live server drag target");
            Files.writeString(ROOT.resolve("state.tmp"), out.toString());
            // Immutable snapshots avoid replacing a file currently opened by a Windows reader.
            Files.move(ROOT.resolve("state.tmp"), ROOT.resolve(String.format("state-%010d.properties", server.getTickCount())),
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (System.nanoTime() > DEADLINE) throw new IllegalStateException("two-client acceptance timeout");
            if (Files.exists(ROOT.resolve("failure.txt"))) throw new IllegalStateException(Files.readString(ROOT.resolve("failure.txt")));
            if (!opened && mc.screen instanceof TitleScreen) {
                if (HOST) {
                    opened = true;
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "downed_drag_test");
                } else if (Files.exists(ROOT.resolve("ready"))) {
                    opened = true;
                    String address = "127.0.0.1:" + PORT;
                    ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(address),
                            new ServerData("Downed loopback acceptance", address, false), false);
                }
            }
            if (mc.player == null || mc.level == null) return;
            mc.options.pauseOnLostFocus = false;
            mc.getTutorial().setStep(TutorialSteps.NONE);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (!HOST && !networkReady) {
                var connection = mc.getConnection().getConnection();
                require(!connection.isMemoryConnection(), "casualty uses real TCP connection");
                require(ModList.get().isLoaded("moveslikemafuyu") == Boolean.getBoolean("wok.downed.dragTestMafuyu"), "expected Mafuyu installation");
                connection.channel().pipeline().addBefore("packet_handler", "downed_test_ack_delay", new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                        if (message instanceof ServerboundAcceptTeleportationPacket) {
                            context.executor().schedule(() -> context.writeAndFlush(message, promise), 150, TimeUnit.MILLISECONDS);
                        } else {
                            super.write(context, message, promise);
                        }
                    }
                });
                networkReady = true;
                Files.writeString(ROOT.resolve("guest-ready"), "teleport acknowledgements delayed 150ms");
            }
            Path latest;
            try (var files = Files.list(ROOT)) {
                latest = files.filter(path -> path.getFileName().toString().startsWith("state-")
                        && path.getFileName().toString().endsWith(".properties"))
                        .max(Comparator.comparing(path -> path.getFileName().toString())).orElse(null);
            }
            if (latest == null) return;
            Properties snapshot = new Properties();
            snapshot.load(new StringReader(Files.readString(latest)));
            int phase = Integer.parseInt(snapshot.getProperty("stage"));
            if (HOST) {
                mc.options.keyUp.setDown(phase == 1 || phase == 3 || phase == 5);
                if (phase == 1) mc.player.setYRot(0);
                if (phase == 3 || phase == 5) mc.player.setYRot(90);
            }
            if (phase == 8) {
                // Let the TCP guest record success before the host closes its integrated server.
                if (!HOST || Files.exists(ROOT.resolve("guest-result.txt"))) finish(null);
                return;
            }
            if (phase != observedStage) { observedStage = phase; stableTicks = 0; }
            if (++stableTicks < 20 || capturedStage == phase || capture >= 0
                    || (phase != 2 && phase != 4 && phase != 6 && phase != 7)) return;
            Player casualty = HOST ? (Player) mc.level.getEntity(Integer.parseInt(snapshot.getProperty("casualty"))) : mc.player;
            require(casualty != null, "casualty entity visible at phase " + phase);
            Vec3 expected = new Vec3(Double.parseDouble(snapshot.getProperty("x")),
                    Double.parseDouble(snapshot.getProperty("y")), Double.parseDouble(snapshot.getProperty("z")));
            require(horizontalDistanceSquared(casualty.position(), expected) < 0.16,
                    "phase " + phase + " visible casualty follows server target: actual=" + casualty.position() + " expected=" + expected);
            require(casualty.getPose() == (phase == 7 ? Pose.STANDING : Pose.SWIMMING),
                    "phase " + phase + " visible pose is correct");
            if (phase == 2) {
                Vec3 original = new Vec3(Double.parseDouble(snapshot.getProperty("startX")), expected.y,
                        Double.parseDouble(snapshot.getProperty("startZ")));
                require(horizontalDistanceSquared(casualty.position(), original) > 4, "visible casualty traveled over two blocks");
            }
            capture = phase;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || capture < 0) return;
        try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(ROOT.resolve(ROLE + "-phase-" + capture + ".png"));
            Files.writeString(ROOT.resolve(ROLE + "-seen-" + capture), "PASS");
            capturedStage = capture;
            capture = -1;
        } catch (Throwable failure) { finish(failure); }
    }

    private static Vec3 target(ServerPlayer carrier) {
        Vec3 look = carrier.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();
        return carrier.position().add(horizontal.scale(-DownedConfig.DRAG_FOLLOW_DISTANCE.get())).add(0, 0.05, 0);
    }

    private static double horizontalDistanceSquared(Vec3 a, Vec3 b) {
        return (a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z);
    }

    private static boolean seenByBoth(int phase) {
        return Files.exists(ROOT.resolve("host-seen-" + phase)) && Files.exists(ROOT.resolve("guest-seen-" + phase));
    }

    private static void transition(int value) { stage = value; elapsed = 0; }

    private static synchronized void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException(label);
        CHECKS.add("PASS " + label);
    }

    private static synchronized void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        try {
            String result = "status=" + (failure == null ? "PASS" : "FAIL") + "\n" + String.join("\n", CHECKS);
            if (failure != null) {
                result += "\nFAIL " + failure;
                Files.writeString(ROOT.resolve("failure.txt"), ROLE + ": " + failure);
                WokDownedMod.LOGGER.error("Drag client acceptance failed", failure);
            }
            Files.writeString(ROOT.resolve(ROLE + "-result.txt"), result);
        } catch (Exception e) { WokDownedMod.LOGGER.error("Could not write drag results", e); }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }
}
