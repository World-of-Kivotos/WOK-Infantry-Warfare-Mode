package com.wok.infantry.gameplaytest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.*;
import com.wok.infantry.battle.tickets.*;
import com.wok.infantry.client.ClientProneStability;
import com.wok.infantry.deployment.*;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Disposable real Forge client/server scenario. No fixture class enters the production JAR. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT)
public final class GameplayAcceptanceHarness {
    private static final List<String> checks = new ArrayList<>(), messages = new ArrayList<>();
    private static CompletableFuture<Void> work;
    private static int phase, ticks, phaseTicks;
    private static boolean opened, finished, deployed, respawnRequested, medicalCompact;
    private static String capture;
    private static UUID lifeToken;
    private static ItemStack cargo;
    private static float earlyProne;
    private static Object point;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            phaseTicks++;
            if (++ticks > 5000) throw new IllegalStateException("Timed out at phase " + phase);
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (!opened && mc.screen instanceof TitleScreen && phaseTicks > 40) {
                    opened = true;
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "wok_gameplay_test");
                } else if (mc.screen instanceof ConfirmScreen screen && phaseTicks > 60) {
                    screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(button -> button.active).min(Comparator.comparingInt(Button::getX)).ifPresent(Button::onPress);
                }
                return;
            }
            mc.options.pauseOnLostFocus = false;
            if (phase > 0) mc.setScreen(null);
            if (work != null) { if (!work.isDone()) return; work.join(); work = null; }
            if (phase == 0) {
                if (phaseTicks < 100) return;
                mc.getTutorial().setStep(TutorialSteps.NONE);
                onServer(p -> {
                    p.server.getPlayerList().op(p.getGameProfile()); p.setGameMode(GameType.SURVIVAL);
                    var level = p.server.overworld();
                    for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y <= 105; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    BattleService battle = BattleService.get(p).orElseThrow();
                    success(battle.resetBattle(p), "reset disposable round");
                    var d = DeploymentService.get(p).orElseThrow();
                    success(d.setMainBaseAt(p, Faction.BLUE, level, new BlockPos(0, 101, 0), 0), "set main base");
                    success(d.setVehicleTestMode(p.createCommandSourceStack(), p, true), "release administrator for area setup");
                    p.teleportTo(level, 0.5, 101, 0.5, 0, 0);
                    p.getInventory().setItem(0, new ItemStack(InfantryItems.BASE_SUPPLY_TOOL.get())); p.getInventory().selected = 0;
                    var first = new PlayerInteractEvent.LeftClickBlock(p,
                            new BlockPos(-4, 101, -4), Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.START);
                    BaseSupplyEvents.first(first);
                    BlockPos second = new BlockPos(4, 104, 4);
                    var last = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, second,
                            new BlockHitResult(Vec3.atCenterOf(second), Direction.UP, second, false));
                    BaseSupplyEvents.second(last);
                    require(first.isCanceled() && last.isCanceled(), "selector handles both corners without breaking/placing terrain");
                    int result = p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "battle basearea set blue");
                    require(result == 1 && BaseSupplyAreas.get(p.server).area(Faction.BLUE)
                            .contains(level.dimension().location(), new BlockPos(0, 101, 0)), "production command stores selected base cuboid");
                    success(d.setVehicleTestMode(p.createCommandSourceStack(), p, false), "restore ordinary deployment rules");
                    p.setGameMode(GameType.SURVIVAL);
                    success(FormationService.get(p).orElseThrow().forceAssign(p, p, "academy", "default"), "assign formation");
                    success(battle.createSquad(p, SquadCallsign.ALPHA), "create squad");
                    success(LoadoutService.get(p).orElseThrow().assignBattleClass(p, "assault"), "select fixture kit");
                    success(d.selectPoint(p, d.mainBase(Faction.BLUE).orElseThrow().id()), "select spawn");
                }); next();
            } else if (phase == 1) {
                if (!deployed) {
                    onServer(p -> {
                        var d = DeploymentService.get(p).orElseThrow();
                        if (!d.viewFor(p).canDeploy()) return;
                        success(d.deploy(p), "deploy through real server timer");
                        require(d.isActive(p.getUUID()) && !d.isVehicleTestMode(p.getUUID()), "active normal deployment");
                        require(d.viewFor(p).canResupply() && d.viewFor(p).resupplyTicks() == 0, "base supply button has no obsolete sixty-second cooldown");
                        lifeToken = d.activeIssueToken(p).orElseThrow();
                        p.getInventory().getItem(5).setCount(1);
                        cargo = new ItemStack(InfantryItems.MEDIUM_AMMO_SUPPLY_CRATE.get());
                        KitProvenance.stampTransportCargo(cargo, d.sessionId(), p.getUUID(), lifeToken);
                        p.getInventory().setItem(20, cargo);
                        p.inventoryMenu.broadcastChanges();
                        p.server.setSingleplayerProfile(new com.mojang.authlib.GameProfile(
                                UUID.fromString("77cc0297-8c60-4530-a527-f5f6545b4ff1"), "GameplayTestOwner"));
                        p.server.getPlayerList().deop(p.getGameProfile());
                        require(!p.hasPermissions(2), "supply uses ordinary-player inventory validation");
                        require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 500, "initial manpower is 500");
                        deployed = true;
                    }); return;
                } next();
            } else if (phase == 2) {
                if (phaseTicks < 50) return;
                require(TicketNetwork.supplyHint().contains("基地补给中"), "server countdown reaches the dedicated client supply panel");
                mc.getToasts().clear();
                mc.options.guiScale().set(3); mc.resizeDisplay(); capture = "supply-320x240.png"; next();
            } else if (phase == 3) {
                if (capture != null || phaseTicks < 20) return;
                mc.getToasts().clear();
                mc.options.guiScale().set(1); mc.resizeDisplay(); capture = "supply-960x720.png";
                onServer(p -> {
                    require(p.getInventory().getItem(5).getCount() == 1, "no supply before fifteen seconds");
                    p.teleportTo(p.server.overworld(), 9.5, 101, 0.5, 0, 0);
                }); next();
            } else if (phase == 4) {
                if (capture != null || phaseTicks < 15) return;
                onServer(p -> p.teleportTo(p.server.overworld(), 0.5, 101, 0.5, 0, 0)); next();
            } else if (phase == 5) {
                if (phaseTicks == 200) onServer(p -> require(p.getInventory().getItem(5).getCount() == 1, "leaving area resets the full countdown"));
                if (phaseTicks < 325) return;
                onServer(p -> {
                    var d = DeploymentService.get(p).orElseThrow();
                    require(p.getInventory().getItem(5).getCount() == 16, "fifteen-second supply restores consumed stack");
                    require(p.getInventory().getItem(20) == cargo && d.isValidIssuedStack(p, cargo), "transport cargo retained with original provenance");
                    require(lifeToken.equals(d.activeIssueToken(p).orElseThrow()), "resupply preserves life issuance token");
                    if (ModList.get().isLoaded("wok_downed")) medicalAndCapture(p);
                }); next();
            } else if (phase == 6) {
                if (phaseTicks < 15) return;
                capture = "rescue-and-tickets-960x720.png"; next();
            } else if (phase == 7) {
                if (capture != null) return;
                if (!medicalCompact) {
                    medicalCompact = true;
                    mc.options.guiScale().set(3); mc.resizeDisplay();
                    capture = "rescue-and-tickets-320x240.png"; return;
                }
                mc.options.guiScale().set(1); mc.resizeDisplay();
                if (ModList.get().isLoaded("tacz")) {
                    onServer(p -> p.removeEffect(MobEffects.BLINDNESS));
                    type("com.tacz.guns.api.entity.IGunOperator").getMethod("crawl", boolean.class).invoke(mc.player, true);
                    ClientProneStability.reset();
                } next();
            } else if (phase == 8) {
                if (phaseTicks < 10) return;
                if (ModList.get().isLoaded("tacz")) {
                    earlyProne = proneFactor();
                    checks.add("PRONE early=" + earlyProne + " pose=" + mc.player.getPose()
                            + " forced=" + mc.player.getForcedPose() + " ground=" + mc.player.onGround()
                            + " downed=" + CombatantStatus.isDowned(mc.player) + " item=" + mc.player.getMainHandItem());
                    require(earlyProne > 0.3F && earlyProne < 1, "machine gun begins gradual prone reduction");
                } next();
            } else if (phase == 9) {
                if (phaseTicks < 45) return;
                if (ModList.get().isLoaded("tacz")) {
                    float settled = proneFactor();
                    require(settled < earlyProne && Math.abs(settled - 0.1F) < 0.01F, "stationary prone reaches original 0.1 factor");
                    Event fire = (Event) type("com.tacz.guns.api.event.common.GunFireEvent")
                            .getConstructor(LivingEntity.class, ItemStack.class, LogicalSide.class)
                            .newInstance(mc.player, mc.player.getMainHandItem(), LogicalSide.CLIENT);
                    MinecraftForge.EVENT_BUS.post(fire);
                    require(Arrays.stream(type("com.tacz.guns.client.event.CameraSetupEvent").getDeclaredMethods())
                            .anyMatch(method -> method.getName().contains("gradualProneRecoil")), "real TaCZ camera class has the per-shot recoil mixin");
                    var spline = type("com.tacz.guns.client.event.CameraSetupEvent").getDeclaredField("pitchSplineFunction");
                    spline.setAccessible(true);
                    require(spline.get(null) != null, "TaCZ fire event creates camera recoil through transformed code");
                    mc.options.keyUp.setDown(true);
                } next();
            } else if (phase == 10) {
                if (phaseTicks < 20) return;
                if (ModList.get().isLoaded("tacz")) {
                    mc.options.keyUp.setDown(false);
                    require(proneFactor() > 0.1F, "actual crawling movement loses stability");
                    type("com.tacz.guns.api.entity.IGunOperator").getMethod("crawl", boolean.class).invoke(mc.player, false);
                } next();
            } else if (phase == 11) {
                if (phaseTicks < 3) return;
                if (ModList.get().isLoaded("tacz")) require(Math.abs(proneFactor() - 1) < 0.001, "standing removes accumulated stability");
                onServer(p -> {
                    success(DeploymentService.get(p).orElseThrow().redeploy(p), "voluntary redeployment");
                    require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 499, "redeployment costs exactly one manpower");
                    TicketService.playerLoss(p, lifeToken);
                    require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 499, "duplicate life loss is ignored");
                    if (ModList.get().isLoaded("wok_capture_points")) {
                        require(!eligible(p), "waiting player is ineligible to capture");
                        call(point, "setControl", new Class<?>[]{double.class}, 1.0D);
                    }
                    var d = DeploymentService.get(p).orElseThrow();
                    success(d.selectPoint(p, d.mainBase(Faction.BLUE).orElseThrow().id()), "select next life spawn");
                }); next();
            } else if (phase == 12) {
                if (phaseTicks < 325) return;
                onServer(p -> {
                    if (ModList.get().isLoaded("wok_capture_points"))
                        require(TicketSavedData.get(p.server).remaining(Faction.RED) == 500, "full capture never spends manpower");
                    var d = DeploymentService.get(p).orElseThrow();
                    success(d.deploy(p), "deploy a new life for final death");
                    TicketSavedData.get(p.server).reset(1);
                    p.kill();
                    require(p.isDeadOrDying(), "final death follows the real player death event");
                    require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 0
                            && TicketSavedData.get(p.server).remaining(Faction.RED) == 1, "final death spends exactly one manpower");
                    require(TicketService.finished(p.server) && TicketSavedData.get(p.server).winner() == Faction.RED, "zero manpower ends round with red victory");
                    require(!d.deploy(p).success(), "finished round blocks deployment");
                    require(!d.viewFor(p).canDeploy(), "finished round also disables the deployment button");
                }); next();
            } else if (phase == 13) {
                if (!respawnRequested) {
                    if (!mc.player.isDeadOrDying()) return;
                    mc.player.respawn(); respawnRequested = true; phaseTicks = 0; return;
                }
                if (phaseTicks < 40) return;
                onServer(p -> {
                    require(p.isAlive() && !DeploymentService.get(p).orElseThrow().isActive(p.getUUID()), "respawn after defeat stays outside active combat");
                    require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 0
                            && TicketSavedData.get(p.server).remaining(Faction.RED) == 1, "respawn does not spend a second ticket");
                });
                mc.options.guiScale().set(3); mc.resizeDisplay(); capture = "victory-320x240.png"; next();
            } else if (phase == 14) {
                if (capture != null) return;
                onServer(p -> {
                    p.server.getPlayerList().op(p.getGameProfile());
                    success(BattleService.get(p).orElseThrow().resetBattle(p), "unified new round reset");
                    require(!TicketService.finished(p.server) && TicketSavedData.get(p.server).remaining(Faction.BLUE) == 500, "new round restores initial manpower");
                    if (point != null) require(((Number) call(point, "control", new Class<?>[0])).doubleValue() == 0, "new round neutralizes existing capture points");
                }); next();
            } else if (phase == 15) { finish(mc, null); }
        } catch (Throwable failure) { finish(mc, failure); }
    }

    private static void medicalAndCapture(ServerPlayer p) {
        Object service = ((Optional<?>) call(type("com.wok.capturepoints.capture.CaptureService"), "get",
                new Class<?>[]{net.minecraft.server.MinecraftServer.class}, p.server)).orElseThrow();
        try {
            point = type("com.wok.capturepoints.capture.CapturePoint").getConstructor(String.class, String.class,
                    ResourceLocation.class, BlockPos.class, BlockPos.class, int.class).newInstance("test", "验收据点",
                    p.level().dimension().location(), new BlockPos(-8, 101, -8), new BlockPos(8, 105, 8), 0);
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        Object data = call(service, "data", new Class<?>[0]);
        call(data, "put", new Class<?>[]{point.getClass()}, point);
        require(eligible(p), "living active player eligible to capture");
        call(type("com.wok.downed.state.DownedService"), "enterDowned", new Class<?>[]{ServerPlayer.class}, p);
        require(!eligible(p), "downed player excluded from capture");
        require(TicketSavedData.get(p.server).remaining(Faction.BLUE) == 500, "going down does not spend manpower");
        call(type("com.wok.downed.state.DownedService"), "revive", new Class<?>[]{ServerPlayer.class, ServerPlayer.class}, p, null);
        require(p.getHealth() == 1, "rescue leaves one vanilla health point");
        require(p.hasEffect(MobEffects.BLINDNESS) && p.getEffect(MobEffects.BLINDNESS).getDuration() == 100, "rescue applies five seconds of blindness");
        Class<?> bodyData = type("com.wok.bodyhealth.health.BodyHealthData"), partType = type("com.wok.bodyhealth.health.BodyPart");
        Object health = call(bodyData, "load", new Class<?>[]{net.minecraft.world.entity.player.Player.class}, p);
        for (Object part : partType.getEnumConstants()) require(((Number) call(health, "get", new Class<?>[]{partType}, part)).floatValue() == 1,
                "absolute one-point rescue: " + part);
        require(!p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "rescue does not grant old resistance V");
    }

    private static boolean eligible(ServerPlayer p) {
        return Boolean.TRUE.equals(call(type("com.wok.capturepoints.capture.CaptureEligibility"), "canCount", new Class<?>[]{ServerPlayer.class}, p));
    }
    private static float proneFactor() {
        Object index = ((Optional<?>) call(type("com.tacz.guns.api.TimelessAPI"), "getClientGunIndex",
                new Class<?>[]{ResourceLocation.class}, ResourceLocation.parse("tacz:m249"))).orElseThrow();
        return ClientProneStability.crawlMultiplier(call(index, "getGunData", new Class<?>[0]));
    }
    private static Class<?> type(String name) {
        try { return Class.forName(name); } catch (ClassNotFoundException error) { throw new RuntimeException(error); }
    }
    private static Object call(Object target, String name, Class<?>[] parameters, Object... args) {
        try {
            Class<?> owner = target instanceof Class<?> clazz ? clazz : target.getClass();
            Method method = owner.getDeclaredMethod(name, parameters); method.setAccessible(true);
            return method.invoke(target instanceof Class<?> ? null : target, args);
        } catch (ReflectiveOperationException error) { throw new RuntimeException(name, error); }
    }
    @SubscribeEvent public static void chat(ClientChatReceivedEvent event) { messages.add(event.getMessage().getString()); }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null || finished) return;
        var mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var dir = mc.gameDirectory.toPath().resolve("screenshots"); Files.createDirectories(dir);
            image.writeToFile(dir.resolve(capture)); capture = null;
        } catch (Exception failure) { finish(mc, failure); }
    }
    private static void next() { phase++; phaseTicks = 0; }
    private static void require(boolean valid, String label) { if (!valid) throw new IllegalStateException(label); checks.add("PASS " + label); }
    private static void success(ActionResult result, String label) { require(result.success(), label + ": " + result.message()); }
    private static void onServer(Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance(); UUID id = mc.player.getUUID();
        work = mc.getSingleplayerServer().submit(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }
    private static void finish(Minecraft mc, Throwable failure) {
        finished = true; mc.options.keyUp.setDown(false);
        if (failure != null) WokInfantryMod.LOGGER.error("Gameplay acceptance failed", failure);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("gameplay-acceptance.txt"),
                    "status=" + (failure == null ? "PASS" : "FAIL") + "\nphase=" + phase + "\n"
                            + String.join("\n", checks) + "\nmessages=" + String.join(" | ", messages)
                            + "\nfailure=" + (failure == null ? "" : failure) + "\n", StandardCharsets.UTF_8);
        } catch (Exception error) { WokInfantryMod.LOGGER.error("Cannot save gameplay result", error); }
        if (mc.getSingleplayerServer() != null) mc.getSingleplayerServer().halt(false);
        mc.stop();
    }
}
