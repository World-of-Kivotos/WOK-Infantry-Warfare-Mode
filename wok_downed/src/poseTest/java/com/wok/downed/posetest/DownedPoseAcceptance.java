package com.wok.downed.posetest;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.downed.WokDownedMod;
import com.wok.downed.state.DownedService;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Drives actual synchronized player state and real Mafuyu key subscribers, then captures rendering. */
@Mod.EventBusSubscriber(modid = WokDownedMod.MOD_ID, value = Dist.CLIENT)
public final class DownedPoseAcceptance {
    private static final List<String> checks = new ArrayList<>();
    private static int ticks;
    private static int stage;
    private static int stageTick;
    private static boolean opened;
    private static boolean finished;
    private static CompletableFuture<Void> serverWork;
    private static Vec3 downedPosition;
    private static String capture;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            check(++ticks < 1800, "client acceptance timeout");
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (!opened && mc.screen instanceof TitleScreen) {
                    opened = true;
                    prepareFixture(mc);
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "downed_pose_test");
                }
                return;
            }
            if (serverWork != null) {
                if (!serverWork.isDone()) return;
                serverWork.join();
                serverWork = null;
            }
            LocalPlayer player = mc.player;
            boolean mafuyu = ModList.get().isLoaded("moveslikemafuyu");
            switch (stage) {
                case 0 -> {
                    check(mafuyu == Boolean.getBoolean("wok.downed.poseTestMafuyu"), "expected Mafuyu presence");
                    mc.getTutorial().setStep(TutorialSteps.NONE);
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    onServer(p -> {
                        p.setGameMode(GameType.SURVIVAL);
                        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                            p.serverLevel().setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                            for (int y = 101; y < 105; y++) {
                                p.serverLevel().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                            }
                        }
                        p.teleportTo(0.5, 101, 0.5);
                        p.setHealth(p.getMaxHealth());
                        p.setForcedPose(Pose.CROUCHING);
                        check(p.getForcedPose() == Pose.CROUCHING, "healthy pose requests pass through");
                        p.setForcedPose(null);
                    });
                    next();
                }
                case 1 -> {
                    // ServerPlayer grants 60 ticks of login/respawn damage immunity.
                    if (ticks - stageTick < 80) return;
                    onServer(p -> {
                        p.addTag("craw");
                        p.addTag("slide");
                        p.hurt(p.damageSources().generic(), 1000.0F);
                        check(DownedService.isDowned(p), "lethal damage enters downed state");
                        check(p.getPose() == Pose.SWIMMING, "server prone immediately");
                        p.setForcedPose(null);
                        check(p.getForcedPose() == Pose.SWIMMING, "server rejects null pose override");
                        p.setForcedPose(Pose.STANDING);
                        check(p.getForcedPose() == Pose.SWIMMING, "server rejects standing pose override");
                        if (mafuyu) {
                            check(!p.getTags().contains("craw") && !p.getTags().contains("slide"), "pre-existing Mafuyu states cleared");
                        }
                    });
                    next();
                }
                case 2 -> {
                    if (ticks - stageTick < 30) return;
                    check(DownedService.isDowned(player), "downed effect synchronized to client");
                    check(player.getPose() == Pose.SWIMMING, "local player prone");
                    player.setForcedPose(null);
                    check(player.getForcedPose() == Pose.SWIMMING, "client rejects pose clear");
                    downedPosition = player.position();
                    mc.options.keyUp.setDown(true);
                    mc.options.keyJump.setDown(true);
                    mc.options.keySprint.setDown(true);
                    press(mc.options.keyShift.getKey().getValue());
                    press(mc.options.keyShift.getKey().getValue());
                    press(mc.options.keySprint.getKey().getValue());
                    press(mc.options.keyJump.getKey().getValue());
                    check(!player.getTags().contains("craw") && !player.getTags().contains("slide"), "downed keys cannot enable crawl or slide");
                    next();
                }
                case 3 -> {
                    if (ticks - stageTick < 30) return;
                    check(player.getPose() == Pose.SWIMMING, "pose remains prone after movement keys");
                    check(player.position().distanceToSqr(downedPosition) < 0.01, "movement and jump blocked");
                    check(player.getBbHeight() < 0.7F, "prone collision height");
                    onServer(p -> {
                        check(p.getPose() == Pose.SWIMMING && p.getForcedPose() == Pose.SWIMMING,
                                "server remains prone through real movement-mod ticks");
                        if (mafuyu) check(!p.getTags().contains("craw") && !p.getTags().contains("slide"),
                                "no movement tags reach server while downed");
                    });
                    capture = "downed.png";
                    mc.options.keyUp.setDown(false);
                    mc.options.keyJump.setDown(false);
                    mc.options.keySprint.setDown(false);
                    next();
                }
                case 4 -> {
                    if (capture != null) return;
                    onServer(p -> {
                        DownedService.revive(p, null);
                        check(!DownedService.isDowned(p), "revived server state cleared");
                        check(p.getForcedPose() == null, "revived server pose released");
                        p.setForcedPose(Pose.CROUCHING);
                        check(p.getForcedPose() == Pose.CROUCHING, "healthy pose control restored");
                        p.setForcedPose(null);
                    });
                    next();
                }
                case 5 -> {
                    if (ticks - stageTick < 30) return;
                    check(!DownedService.isDowned(player), "revive synchronized");
                    check(player.getForcedPose() == null && player.getPose() == Pose.STANDING, "revived client stands normally");
                    capture = "revived.png";
                    if (mafuyu) {
                        press(mc.options.keyShift.getKey().getValue());
                        press(mc.options.keyShift.getKey().getValue());
                        check(player.getTags().contains("craw"), "Mafuyu crawling works again after revive");
                        press(mc.options.keyShift.getKey().getValue());
                        check(!player.getTags().contains("craw"), "Mafuyu crawl cancellation restored");
                    }
                    next();
                }
                case 6 -> {
                    if (capture == null) finish("PASS", null);
                }
            }
        } catch (Throwable failure) {
            finish("FAIL", failure);
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null || finished) return;
        try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            Path dir = results();
            Files.createDirectories(dir);
            image.writeToFile(dir.resolve(capture));
            capture = null;
        } catch (Throwable failure) {
            finish("FAIL", failure);
        }
    }

    private static void press(int key) {
        MinecraftForge.EVENT_BUS.post(new InputEvent.Key(key, 0, GLFW.GLFW_PRESS, 0));
        MinecraftForge.EVENT_BUS.post(new InputEvent.Key(key, 0, GLFW.GLFW_RELEASE, 0));
    }

    private static void prepareFixture(Minecraft mc) throws Exception {
        Path levelFile = mc.gameDirectory.toPath().resolve("saves/downed_pose_test/level.dat");
        CompoundTag root = NbtIo.readCompressed(levelFile.toFile());
        CompoundTag data = root.getCompound("Data");
        CompoundTag dimensions = data.getCompound("WorldGenSettings").getCompound("dimensions");
        for (String id : List.copyOf(dimensions.getAllKeys())) {
            if (!id.startsWith("minecraft:")) dimensions.remove(id);
        }
        ListTag enabled = new ListTag();
        enabled.add(StringTag.valueOf("vanilla"));
        data.getCompound("DataPacks").put("Enabled", enabled);
        data.remove("Player");
        data.putString("LevelName", "Downed pose isolated acceptance");
        NbtIo.writeCompressed(root, levelFile.toFile());
    }

    private static void onServer(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        var id = mc.player.getUUID();
        serverWork = mc.getSingleplayerServer().submit(() -> action.accept(
                mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }

    private static void check(boolean passed, String label) {
        if (!passed) throw new IllegalStateException(label);
        if (!label.equals("client acceptance timeout")) checks.add("PASS " + label);
    }

    private static void next() { stage++; stageTick = ticks; }

    private static Path results() { return Minecraft.getInstance().gameDirectory.toPath().resolve("pose-test-results"); }

    private static void finish(String status, Throwable failure) {
        finished = true;
        if (failure != null) {
            WokDownedMod.LOGGER.error("Downed pose acceptance failed", failure);
            checks.add("FAIL " + failure);
        }
        try {
            Files.createDirectories(results());
            Files.writeString(results().resolve("result.txt"), "status=" + status + "\n" + String.join("\n", checks));
        } catch (Exception e) {
            WokDownedMod.LOGGER.error("Could not write acceptance results", e);
        }
        Minecraft.getInstance().stop();
    }
}
