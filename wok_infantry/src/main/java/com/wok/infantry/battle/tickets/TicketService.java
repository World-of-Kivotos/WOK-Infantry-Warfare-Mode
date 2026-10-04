package com.wok.infantry.battle.tickets;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.config.BattleGameplayConfig;
import com.wok.infantry.deployment.DeploymentService;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "wok_infantry")
public final class TicketService {
    private static Object capture(MinecraftServer server, String methodName) {
        if (!ModList.get().isLoaded("wok_capture_points")) return null;
        try {
            Class<?> type = Class.forName("com.wok.capturepoints.capture.CaptureService");
            Object value = type.getMethod("get", MinecraftServer.class).invoke(null, server);
            if (value instanceof Optional<?> service && service.isPresent()) {
                Method method = type.getMethod(methodName);
                return method.invoke(service.get());
            }
        } catch (ReflectiveOperationException exception) {
            WokInfantryMod.LOGGER.error("Capture round integration failed: {}", methodName, exception);
        }
        return null;
    }

    public static boolean finished(MinecraftServer server) { return TicketSavedData.get(server).finished(); }

    public static void playerLoss(ServerPlayer player, UUID issueToken) {
        if (issueToken == null) return;
        BattleService.get(player).flatMap(service -> service.factionOf(player.getUUID())).ifPresent(side ->
                debit(player.server, side, BattleGameplayConfig.DEATH_COST.get(), issueToken));
    }

    private static void debit(MinecraftServer server, Faction side, int amount, UUID lossId) {
        TicketSavedData data = TicketSavedData.get(server);
        if (!data.debit(side, amount, lossId)) return;
        if (data.finished()) {
            DeploymentService.get(server).ifPresent(DeploymentService::endRound);
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "本局结束 · " + (data.winner() == Faction.BLUE ? "蓝方" : "红方") + "获胜 · 敌方兵力值耗尽"), false);
        }
        sync(server);
    }

    public static void reset(MinecraftServer server) {
        TicketSavedData.get(server).reset(BattleGameplayConfig.INITIAL_TICKETS.get());
        capture(server, "resetForNewRound");
        sync(server);
    }

    private static void sync(MinecraftServer server) {
        TicketSavedData data = TicketSavedData.get(server);
        int blue = data.remaining(Faction.BLUE);
        int red = data.remaining(Faction.RED);
        int max = barMaximum(BattleGameplayConfig.INITIAL_TICKETS.get(), blue, red);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TicketNetwork.send(player, new TicketNetwork.Snapshot(blue, red, max,
                    BattleService.get(player).flatMap(service ->
                    service.factionOf(player.getUUID())).isPresent()));
        }
    }

    /**
     * Full manpower bar sent to clients: the configured starting manpower, or a side's current
     * value when that is higher (e.g. the setting was lowered mid-round), within the wire bounds.
     */
    static int barMaximum(int initialTickets, int blue, int red) {
        int max = Math.max(initialTickets, Math.max(blue, red));
        return Math.max(1, Math.min(TicketNetwork.MAX_TICKETS, max));
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.overworld().getGameTime() % 20 != 0) return;
        sync(server);
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("battle").then(Commands.literal("tickets")
                .executes(context -> {
                    TicketSavedData data = TicketSavedData.get(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("兵力值 · 蓝方 "
                            + data.remaining(Faction.BLUE) + " / 红方 " + data.remaining(Faction.RED)
                            + (data.finished() ? " · 本局已结束" : " · 对局进行中")), false);
                    return 1;
                })
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    var result = BattleService.get(player).orElseThrow().resetBattle(player);
                    if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), true);
                    else context.getSource().sendFailure(Component.literal(result.message()));
                    return result.success() ? 1 : 0;
                }))));
    }
    private TicketService() {}
}
