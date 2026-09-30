package com.wok.infantry.rallytest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.*;
import com.wok.infantry.block.entity.RallyRadioBlockEntity;
import com.wok.infantry.deployment.*;
import com.wok.infantry.registry.InfantryBlocks;
import com.wok.infantry.registry.InfantryItems;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Uses a real issued kit and client use-on packets; no test-mode bypass of deployment. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT)
public final class RallyAcceptanceHarness {
    private static final BlockPos FLOOR = new BlockPos(0, 100, 0);
    private static final BlockPos SECOND_FLOOR = new BlockPos(0, 100, 1);
    private static final List<String> checks = new ArrayList<>();
    private static final List<String> messages = new ArrayList<>();
    private static final List<String> placements = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static CompletableFuture<Void> work;
    private static int phase, ticks, phaseTicks;
    private static boolean opened, finished;
    private static volatile boolean deployed;
    private static ItemStack issuedRadio;
    private static UUID rallyId;
    private static String capture;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            phaseTicks++;
            if (++ticks > 1600) throw new IllegalStateException("Rally acceptance timed out at phase " + phase);
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (!opened && mc.screen instanceof TitleScreen && phaseTicks > 40) {
                    opened = true;
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "wok_rally_test");
                } else if (mc.screen instanceof ConfirmScreen screen && phaseTicks > 60) {
                    screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(button -> button.active).min(java.util.Comparator.comparingInt(Button::getX))
                            .ifPresent(Button::onPress);
                }
                return;
            }
            mc.options.pauseOnLostFocus = false;
            if (phase > 0) mc.setScreen(null);
            if (work != null) {
                if (!work.isDone()) return;
                work.join();
                work = null;
            }
            if (phase == 0) {
                if (phaseTicks < 100) return;
                mc.getTutorial().setStep(TutorialSteps.NONE);
                onServer(p -> {
                    p.server.getPlayerList().op(p.getGameProfile());
                    p.setGameMode(GameType.SURVIVAL);
                    var level = p.server.overworld();
                    for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y <= 105; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    BattleService battle = BattleService.get(p).orElseThrow();
                    success(battle.resetBattle(p), "reset disposable battle");
                    var deployment = DeploymentService.get(p).orElseThrow();
                    success(deployment.setMainBaseAt(p, Faction.BLUE, level, new BlockPos(-3, 101, 0), 270), "set main base");
                    success(FormationService.get(p).orElseThrow().forceAssign(p, p, "academy", "default"), "assign enabled formation");
                    success(battle.createSquad(p, SquadCallsign.ALPHA), "create Alpha as squad leader");
                    success(LoadoutService.get(p).orElseThrow().assignBattleClass(p, "assault"), "select test class");
                    success(deployment.selectPoint(p, deployment.mainBase(Faction.BLUE).orElseThrow().id()), "select base");
                });
                next();
            } else if (phase == 1) {
                if (!deployed) {
                    onServer(p -> {
                        var d = DeploymentService.get(p).orElseThrow();
                        if (!d.viewFor(p).canDeploy()) return;
                        success(d.deploy(p), "actual survival deployment");
                        require(d.isActive(p.getUUID()) && !d.isVehicleTestMode(p.getUUID()), "active without administrator test mode");
                        require(p.getInventory().getItem(0).is(InfantryItems.RALLY_RADIO.get()), "radio comes from the issued primary slot");
                        issuedRadio = p.getInventory().getItem(0).copy();
                        require(d.isValidIssuedStack(p, issuedRadio), "radio has valid kit provenance");
                        p.getInventory().selected = 0;
                        p.inventoryMenu.broadcastChanges();
                        checks.add("BEFORE state=" + p.serverLevel().getBlockState(FLOOR.above())
                                + " be=" + p.serverLevel().getBlockEntity(FLOOR.above())
                                + " mode=" + p.gameMode.getGameModeForPlayer() + " thread=" + Thread.currentThread().getName());
                        deployed = true;
                    });
                    return;
                }
                onServer(p -> {
                    checkInteractionGate(p);
                    // The client now acts as an ordinary squad leader, including during the real
                    // placement packets. Change only this disposable integrated server's owner.
                    p.server.setSingleplayerProfile(new com.mojang.authlib.GameProfile(
                            UUID.fromString("77cc0297-8c60-4530-a527-f5f6545b4ff1"), "RallyTestOwner"));
                    p.server.getPlayerList().deop(p.getGameProfile());
                    require(!p.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL),
                            "actual client has no administrator placement bypass");
                });
                next();
            } else if (phase == 2) {
                if (phaseTicks < 15 || !mc.player.getMainHandItem().is(InfantryItems.RALLY_RADIO.get())) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit(FLOOR));
                next();
            } else if (phase == 3) {
                if (phaseTicks < 15) return;
                onServer(p -> {
                    var d = DeploymentService.get(p).orElseThrow();
                    require(p.serverLevel().getBlockEntity(FLOOR.above()) instanceof RallyRadioBlockEntity,
                            "real right-click packet places a radio on flat ground");
                    var radio = (RallyRadioBlockEntity) p.serverLevel().getBlockEntity(FLOOR.above());
                    checks.add("RADIO " + radio.saveWithFullMetadata() + " inventory=" + p.getMainHandItem()
                            + " capture=" + p.serverLevel().captureBlockSnapshots
                            + " snapshots=" + p.serverLevel().capturedBlockSnapshots.size()
                            + " state=" + p.serverLevel().getBlockState(FLOOR.above()));
                    require(radio.initialized() && radio.health() == 100, "placed radio has squad identity and 100 health");
                    var point = d.pointsFor(p).stream().filter(it -> it.kind() == DeploymentPointKind.RALLY).findFirst().orElseThrow();
                    rallyId = point.id();
                    require(point.position().getY() == 101 && !point.position().equals(FLOOR.above()),
                            "rally spawn is beside the radio at ground level");
                    require(p.getInventory().getItem(0).isEmpty(), "successful placement consumes exactly one issued radio");
                    p.getInventory().setItem(0, issuedRadio.copy());
                    p.inventoryMenu.broadcastChanges();
                });
                next();
            } else if (phase == 4) {
                if (phaseTicks < 10) return;
                messages.clear();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit(SECOND_FLOOR));
                next();
            } else if (phase == 5) {
                if (phaseTicks < 15) return;
                onServer(p -> {
                    require(p.serverLevel().isEmptyBlock(SECOND_FLOOR.above()), "cooldown rejection rolls back the second placement");
                    require(p.getInventory().getItem(0).is(InfantryItems.RALLY_RADIO.get()), "rejected placement preserves the item");
                    require(DeploymentService.get(p).orElseThrow().pointsFor(p).stream()
                            .filter(it -> it.kind() == DeploymentPointKind.RALLY).count() == 1,
                            "rejected placement leaves exactly one rally deployment point");
                });
                next();
            } else if (phase == 6) {
                require(messages.stream().anyMatch(message -> message.contains("队包部署冷却中")), "player receives the squad cooldown reason");
                mc.player.setYRot(270);
                mc.player.setXRot(25);
                mc.options.guiScale().set(3);
                mc.resizeDisplay();
                capture = "rally-320x240.png";
                next();
            } else if (phase == 7) {
                if (capture != null || phaseTicks < 5) return;
                mc.options.guiScale().set(1);
                mc.resizeDisplay();
                capture = "rally-960x720.png";
                next();
            } else if (phase == 8) {
                if (capture != null) return;
                onServer(p -> {
                    var d = DeploymentService.get(p).orElseThrow();
                    success(d.redeploy(p), "enter redeployment");
                    success(d.selectPoint(p, rallyId), "own squad rally remains selectable for respawn");
                });
                next();
            } else if (phase == 9) {
                onServer(p -> {
                    var d = DeploymentService.get(p).orElseThrow();
                    if (!d.viewFor(p).canDeploy()) return;
                    success(d.deploy(p), "actual deployment from own squad rally");
                    require(p.serverLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD
                                    && p.blockPosition().getY() == 101
                                    && p.blockPosition().distSqr(FLOOR.above()) <= 8,
                            "rally deployment arrives safely beside the radio");
                    phase = 10;
                    phaseTicks = 0;
                });
            } else if (phase == 10) {
                if (phaseTicks < 20) return;
                require(mc.player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
                                && mc.player.blockPosition().getY() == 101
                                && mc.player.blockPosition().distSqr(FLOOR.above()) <= 8,
                        "client confirms arrival after the teleport acknowledgement");
                finish(mc, null);
            }
        } catch (Throwable failure) { finish(mc, failure); }
    }

    private static void checkInteractionGate(ServerPlayer p) {
        // Same issued identity and deployment, with permission level zero: exercise the ordinary
        // squad leader path separately from the integrated-server owner's administrator rights.
        ServerPlayer leader = new ServerPlayer(p.server, p.serverLevel(), p.getGameProfile()) {
            @Override public boolean hasPermissions(int permissionLevel) { return permissionLevel <= 0; }
        };
        leader.setGameMode(GameType.SURVIVAL);
        leader.getInventory().setItem(0, issuedRadio.copy());
        var event = new PlayerInteractEvent.RightClickBlock(leader, InteractionHand.MAIN_HAND, FLOOR, hit(FLOOR));
        BattleEvents.onWaitingPlayerInteract(event);
        require(!event.isCanceled(), "ordinary leader may use a legitimately issued rally radio");
        leader.getInventory().setItem(0, new ItemStack(InfantryItems.RALLY_RADIO.get()));
        event = new PlayerInteractEvent.RightClickBlock(leader, InteractionHand.MAIN_HAND, FLOOR, hit(FLOOR));
        BattleEvents.onWaitingPlayerInteract(event);
        require(event.isCanceled(), "ordinary player cannot use an unissued radio");
        leader.getInventory().setItem(0, new ItemStack(Items.DIRT));
        event = new PlayerInteractEvent.RightClickBlock(leader, InteractionHand.MAIN_HAND, FLOOR, hit(FLOOR));
        BattleEvents.onWaitingPlayerInteract(event);
        require(event.isCanceled(), "ordinary terrain building remains forbidden");
    }

    private static BlockHitResult hit(BlockPos floor) {
        return new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
    }

    @SubscribeEvent
    public static void chat(ClientChatReceivedEvent event) { messages.add(event.getMessage().getString()); }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        placements.add("PLACE entity=" + event.getEntity() + " level=" + event.getLevel().getClass().getName()
                + " pos=" + event.getPos() + " state=" + event.getPlacedBlock() + " canceled=" + event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void clicked(PlayerInteractEvent.RightClickBlock event) {
        placements.add("CLICK entity=" + event.getEntity().getClass().getSimpleName()
                + " level=" + event.getLevel().getClass().getName() + " pos=" + event.getPos()
                + " item=" + event.getItemStack() + " canceled=" + event.isCanceled());
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null || finished) return;
        var mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var dir = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(dir);
            image.writeToFile(dir.resolve(capture));
            capture = null;
        } catch (Exception failure) { finish(mc, failure); }
    }

    private static void next() { phase++; phaseTicks = 0; }
    private static void require(boolean valid, String label) {
        if (!valid) throw new IllegalStateException(label);
        checks.add("PASS " + label);
    }
    private static void success(ActionResult result, String label) { require(result.success(), label + ": " + result.message()); }
    private static void onServer(Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        UUID id = mc.player.getUUID();
        work = mc.getSingleplayerServer().submit(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }
    private static void finish(Minecraft mc, Throwable failure) {
        finished = true;
        if (failure != null) WokInfantryMod.LOGGER.error("Rally acceptance failed", failure);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("rally-acceptance.txt"),
                    "status=" + (failure == null ? "PASS" : "FAIL") + "\nphase=" + phase + "\n"
                            + String.join("\n", checks) + "\nmessages=" + String.join(" | ", messages)
                            + "\nplacements=" + String.join(" | ", placements)
                            + "\nfailure=" + (failure == null ? "" : failure) + "\n", StandardCharsets.UTF_8);
        } catch (Exception error) { WokInfantryMod.LOGGER.error("Cannot save rally result", error); }
        // The non-operator scenario deliberately changes the singleplayer owner. Explicitly
        // stop its disposable server rather than relying on owner-disconnect auto shutdown.
        if (mc.getSingleplayerServer() != null) mc.getSingleplayerServer().halt(false);
        mc.stop();
    }
}
