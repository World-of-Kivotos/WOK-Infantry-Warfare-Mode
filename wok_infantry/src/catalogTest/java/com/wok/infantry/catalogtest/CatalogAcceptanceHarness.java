package com.wok.infantry.catalogtest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.ClientLoadoutState;
import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.client.screen.CatalogTransferScreen;
import com.wok.infantry.configtransfer.CatalogArchive;
import com.wok.infantry.configtransfer.CatalogFiles;
import com.wok.infantry.configtransfer.CatalogTransferAction;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.server.LoadoutService;
import com.wok.infantry.server.FormationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Real Forge client, real C2S/S2C, actual widget clicks and framebuffer screenshots. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT)
public final class CatalogAcceptanceHarness {
    private static int phase;
    private static int ticks;
    private static int phaseTicks;
    private static boolean worldRequested;
    private static boolean stopped;
    private static CompletableFuture<Void> serverWork;
    private static String capture;
    private static final List<String> captures = new ArrayList<>();
    private static final List<String> checks = new ArrayList<>();
    private static byte[] playerRecords;
    private static String originalClassName;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("wok.catalogTest") || stopped || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            ticks++;
            phaseTicks++;
            mc.options.pauseOnLostFocus = false;
            if (ticks > 1800) throw new IllegalStateException("timeout phase=" + phase);
            if (phase == 0) {
                if (mc.player == null || mc.getSingleplayerServer() == null) {
                    if (mc.screen instanceof ConfirmScreen screen && phaseTicks > 40) {
                        screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                .filter(button -> button.active).min(java.util.Comparator.comparingInt(Button::getX))
                                .ifPresent(Button::onPress);
                    } else if (!worldRequested && mc.screen instanceof TitleScreen && phaseTicks > 40) {
                        worldRequested = true;
                        mc.createWorldOpenFlows().loadLevel(mc.screen, "wok_catalog_test");
                    }
                    return;
                }
                if (phaseTicks < 100) return;
                mc.getTutorial().setStep(TutorialSteps.NONE);
                mc.getToasts().clear();
                mc.getWindow().setWindowed(960, 720);
                mc.options.guiScale().set(3);
                mc.resizeDisplay();
                serverWork = mc.getSingleplayerServer().submit(() -> {
                    var server = mc.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                    server.getPlayerList().op(player.getGameProfile());
                    var source = player.createCommandSourceStack();
                    var transfer = LoadoutService.get(server).orElseThrow().catalogTransfers();
                    for (var action : CatalogTransferAction.values()) {
                        require(!transfer.execute(source.withPermission(0), action, "denied", "", 0).success(),
                                "non-admin " + action + " must be denied");
                    }
                    checks.add("allTransferActionsRequireAdmin=true");
                    require(!transfer.execute(source, CatalogTransferAction.IMPORT, "roundtrip", "", 0).success(),
                            "import requires a preview token");
                    checks.add("unpreviewedImportDenied=true");
                });
                next();
                return;
            }
            if (phase == 1) {
                if (!serverWork.isDone()) return;
                serverWork.join();
                LoadoutNetwork.sendToServer(new OpenLoadoutPacket(true));
                next();
            } else if (phase == 2) {
                if (!(mc.screen instanceof AdminLoadoutScreen)) return;
                require(mc.screen.width == 320 && mc.screen.height == 240, "compact resolution");
                capture = "catalog-admin-320x240.png";
                if (phaseTicks < 12) return;
                capture = null;
                click(mc, "导入 / 导出");
                next();
            } else if (phase == 3) {
                require(mc.screen instanceof CatalogTransferScreen, "transfer screen opened by click");
                if (phaseTicks < 10) return;
                checkWidgetBounds(mc);
                require(!button(mc, "确认整体替换").active, "confirmation disabled before preview");
                capture = "catalog-idle-320x240.png";
                if (phaseTicks < 13) return;
                name(mc, "roundtrip");
                click(mc, "导出全部");
                next();
            } else if (phase == 4) {
                if (!ready(mc)) return;
                Path exported = root(mc).resolve("catalogs/roundtrip.json");
                require(Files.isRegularFile(exported), "export file exists");
                var archive = CatalogArchive.decode(Files.readAllBytes(exported));
                require(archive.formations().factions().size() == 2, "all factions exported");
                checks.add("uiExportIncludesAllFactionsAndLoadouts=true");
                originalClassName = archive.loadouts().findClass("support").orElseThrow().displayName();
                archive.loadouts().findClass("support").orElseThrow().updateMetadata("验收导入兵种", true, 2);
                Files.write(exported, archive.encode());
                Path selections = mc.gameDirectory.toPath().resolve("saves/wok_catalog_test/data/wok_infantry/player_loadouts.json");
                playerRecords = Files.exists(selections) ? Files.readAllBytes(selections) : null;
                click(mc, "预览导入");
                next();
            } else if (phase == 5) {
                if (!button(mc, "确认整体替换").active) return;
                if (phaseTicks < 12) return;
                capture = "catalog-preview-320x240.png";
                next();
            } else if (phase == 6) {
                if (phaseTicks < 12) return;
                // Change only data on disk after preview; server must reject the stale preview.
                Files.writeString(root(mc).resolve("catalogs/roundtrip.json"), "\n", StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND);
                click(mc, "确认整体替换");
                next();
            } else if (phase == 7) {
                if (!ready(mc)) return;
                require(!button(mc, "确认整体替换").active, "stale preview invalidated");
                require(new CatalogFiles(root(mc)).list().stream().noneMatch(name -> name.startsWith("before-import")),
                        "stale preview must not commit");
                checks.add("changedArchiveRejectedAfterPreview=true");
                capture = "catalog-stale-preview-320x240.png";
                next();
            } else if (phase == 8) {
                if (phaseTicks < 12) return;
                click(mc, "预览导入");
                next();
            } else if (phase == 9) {
                if (!button(mc, "确认整体替换").active) return;
                click(mc, "确认整体替换");
                next();
            } else if (phase == 10) {
                if (!ready(mc)) return;
                var archive = CatalogArchive.decode(Files.readAllBytes(root(mc).resolve("catalogs/roundtrip.json")));
                require(CatalogArchive.GSON.toJsonTree(archive.loadouts()).equals(CatalogArchive.GSON.toJsonTree(
                        ClientLoadoutState.snapshot().config())), "client loadouts refreshed");
                require(CatalogArchive.GSON.toJsonTree(archive.formations()).equals(CatalogArchive.GSON.toJsonTree(
                        ClientLoadoutState.snapshot().formations())), "client factions refreshed");
                require(new CatalogFiles(root(mc)).list().stream().anyMatch(name -> name.startsWith("before-import")),
                        "automatic backup exists");
                require("验收导入兵种".equals(ClientLoadoutState.snapshot().config()
                        .findClass("support").orElseThrow().displayName()), "changed class metadata applied");
                String backupName = new CatalogFiles(root(mc)).list().stream().filter(name -> name.startsWith("before-import"))
                        .findFirst().orElseThrow();
                var backup = CatalogArchive.decode(new CatalogFiles(root(mc)).read(backupName));
                require(originalClassName.equals(backup.loadouts().findClass("support").orElseThrow().displayName()),
                        "backup retains original metadata");
                Path selections = mc.gameDirectory.toPath().resolve("saves/wok_catalog_test/data/wok_infantry/player_loadouts.json");
                require(playerRecords == null ? !Files.exists(selections)
                        : java.util.Arrays.equals(playerRecords, Files.readAllBytes(selections)), "player records unchanged");
                checks.add("uiImportBackupAndClientRefresh=true");
                checks.add("changedMetadataAppliedAndOriginalBackedUp=true");
                checks.add("playerRecordsUnchanged=true");
                capture = "catalog-imported-320x240.png";
                next();
            } else if (phase == 11) {
                if (phaseTicks < 12) return;
                mc.options.guiScale().set(1);
                mc.resizeDisplay();
                next();
            } else if (phase == 12) {
                if (phaseTicks < 12) return;
                require(mc.screen.width == 960 && mc.screen.height == 720, "large resolution");
                checkWidgetBounds(mc);
                click(mc, "文件列表");
                next();
            } else if (phase == 13) {
                if (!ready(mc)) return;
                capture = "catalog-files-960x720.png";
                next();
            } else if (phase == 14) {
                if (phaseTicks < 12) return;
                name(mc, "roundtrip");
                click(mc, "预览导入");
                next();
            } else if (phase == 15) {
                if (!button(mc, "确认整体替换").active) return;
                capture = "catalog-preview-960x720.png";
                next();
            } else if (phase == 16) {
                if (phaseTicks < 12) return;
                click(mc, "返回配装管理");
                require(mc.screen instanceof AdminLoadoutScreen, "return to updated admin catalog");
                capture = "catalog-admin-960x720.png";
                checks.add("compactAndLargeWidgetsFit=true");
                next();
            } else if (phase == 17 && phaseTicks >= 20) {
                for (String name : captures) require(Files.size(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)) > 1000,
                        "non-empty screenshot " + name);
                finish(mc, null);
            }
        } catch (Throwable failure) {
            WokInfantryMod.LOGGER.error("Catalog acceptance failed at phase {}", phase, failure);
            finish(mc, failure.toString());
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (capture == null || event.phase != TickEvent.Phase.END) return;
        String name = capture;
        capture = null;
        if (captures.contains(name)) return;
        captures.add(name);
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static void next() { phase++; phaseTicks = 0; }
    private static Path root(Minecraft mc) { return mc.gameDirectory.toPath().resolve("config/wok_infantry"); }
    private static boolean ready(Minecraft mc) { return phaseTicks > 10 && button(mc, "导出全部").active; }

    private static Button button(Minecraft mc, String name) {
        return mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("missing button " + name));
    }

    private static void click(Minecraft mc, String name) {
        var button = button(mc, name);
        require(button.active, "button active: " + name);
        double x = button.getX() + button.getWidth() / 2.0;
        double y = button.getY() + button.getHeight() / 2.0;
        var screen = mc.screen;
        require(screen.mouseClicked(x, y, 0), "click accepted: " + name);
        screen.mouseReleased(x, y, 0);
    }

    private static void name(Minecraft mc, String name) {
        mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                .findFirst().orElseThrow().setValue(name);
    }

    private static void checkWidgetBounds(Minecraft mc) {
        var widgets = mc.screen.children().stream().filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast).filter(widget -> widget.visible).toList();
        for (var widget : widgets) {
            require(widget.getX() >= 0 && widget.getY() >= 0
                    && widget.getX() + widget.getWidth() <= mc.screen.width
                    && widget.getY() + widget.getHeight() <= mc.screen.height, "widget bounds");
            for (var other : widgets) if (widget != other) {
                require(widget.getX() + widget.getWidth() <= other.getX() || other.getX() + other.getWidth() <= widget.getX()
                        || widget.getY() + widget.getHeight() <= other.getY() || other.getY() + other.getHeight() <= widget.getY(),
                        "widgets do not overlap");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void finish(Minecraft mc, String error) {
        stopped = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("catalog-acceptance.txt"),
                    "status=" + (error == null ? "PASS" : "FAIL") + "\nphase=" + phase + "\n"
                            + String.join("\n", checks) + "\nscreenshots=" + String.join(",", captures)
                            + "\nfailure=" + (error == null ? "" : error) + "\n", StandardCharsets.UTF_8);
        } catch (Exception failure) { WokInfantryMod.LOGGER.error("Cannot save acceptance result", failure); }
        mc.stop();
    }
}
