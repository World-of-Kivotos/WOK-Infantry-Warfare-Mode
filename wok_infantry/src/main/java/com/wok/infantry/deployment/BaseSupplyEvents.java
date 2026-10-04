package com.wok.infantry.deployment;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.CombatantStatus;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.registry.InfantryItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "wok_infantry")
public final class BaseSupplyEvents {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
    private static final Map<UUID, Visit> VISITS = new HashMap<>();
    private record Selection(ResourceLocation dimension, BlockPos first, BlockPos second) {}
    private record Visit(BaseSupplyArea area, UUID issueToken, BaseSupplyProgress progress) {}

    private static boolean selector(PlayerInteractEvent event) {
        return event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity().hasPermissions(2)
                && event.getEntity().getMainHandItem().is(InfantryItems.BASE_SUPPLY_TOOL.get());
    }

    private static void select(ServerPlayer player, BlockPos position, boolean first) {
        ResourceLocation dimension = player.level().dimension().location();
        Selection old = SELECTIONS.get(player.getUUID());
        if (old != null && !old.dimension.equals(dimension)) old = null;
        SELECTIONS.put(player.getUUID(), new Selection(dimension,
                first ? position.immutable() : old == null ? null : old.first,
                !first ? position.immutable() : old == null ? null : old.second));
        player.displayClientMessage(Component.literal("基地范围点 " + (first ? "A" : "B") + "："
                + position.toShortString()), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void first(PlayerInteractEvent.LeftClickBlock event) {
        if (!selector(event)) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) select(player, event.getPos(), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void second(PlayerInteractEvent.RightClickBlock event) {
        if (!selector(event)) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) select(player, event.getPos(), false);
    }

    public static boolean inside(ServerPlayer player) {
        return areaFor(player) != null;
    }

    private static BaseSupplyArea areaFor(ServerPlayer player) {
        Faction faction = BattleService.get(player).flatMap(service -> service.factionOf(player.getUUID())).orElse(null);
        BaseSupplyArea area = BaseSupplyAreas.get(player.server).area(faction);
        DeploymentPoint base = DeploymentService.get(player).flatMap(service -> service.mainBase(faction)).orElse(null);
        return area != null && base != null && area.contains(base.dimension(), base.position())
                && area.contains(player.level().dimension().location(), player.blockPosition()) ? area : null;
    }

    /** Existing command/button requests share exactly the same timed path as automatic entry. */
    public static void request(ServerPlayer player) {
        Visit visit = VISITS.get(player.getUUID());
        if (visit == null || visit.progress.completed()) VISITS.remove(player.getUUID());
        TicketNetwork.sendSupplyHint(player, "请在己方基地补给区停留 15 秒");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        UUID token = deployment == null ? null : deployment.activeIssueToken(player).orElse(null);
        BaseSupplyArea area = player.isAlive() && !player.isSpectator() && !CombatantStatus.isDowned(player)
                && deployment != null && deployment.isActive(player.getUUID())
                && !deployment.isVehicleTestMode(player.getUUID()) ? areaFor(player) : null;
        Visit visit = VISITS.get(player.getUUID());
        if (area == null || token == null) {
            if (VISITS.remove(player.getUUID()) != null && visit != null && !visit.progress.completed())
                TicketNetwork.sendSupplyHint(player, "基地补给已中断，返回后重新计时");
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (visit == null || !visit.area.equals(area) || !visit.issueToken.equals(token)) {
            visit = new Visit(area, token, new BaseSupplyProgress(now));
            VISITS.put(player.getUUID(), visit);
        }
        visit.progress.tick(now);
        if (visit.progress.ready(now)) {
            var result = deployment.completeBaseResupply(player);
            visit.progress.complete();
            TicketNetwork.sendSupplyHint(player, result.message());
            player.sendSystemMessage(Component.literal(result.message()));
        } else if (!visit.progress.completed() && now % 20 == 0) {
            TicketNetwork.sendSupplyHint(player, "基地补给中 · "
                    + visit.progress.remainingSeconds(now) + " 秒 · 离开区域将中断");
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
        VISITS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SELECTIONS.clear(); VISITS.clear(); }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("basearea").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("tool").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (!player.getInventory().add(new ItemStack(InfantryItems.BASE_SUPPLY_TOOL.get()))) {
                context.getSource().sendFailure(Component.literal("背包已满")); return 0;
            }
            context.getSource().sendSuccess(() -> Component.literal(
                    "基地圈地工具：左键 A 点，右键 B 点（含高度）；再用 /battle basearea set <阵营> 保存"), false);
            return 1;
        }));
        for (String action : new String[]{"set", "clear", "show"}) {
            root.then(Commands.literal(action).then(Commands.argument("faction", StringArgumentType.word())
                    .suggests((context, builder) -> { for (Faction faction : Faction.values()) builder.suggest(faction.id()); return builder.buildFuture(); })
                    .executes(context -> {
                        Faction faction = Faction.byId(StringArgumentType.getString(context, "faction")).orElse(null);
                        if (faction == null) { context.getSource().sendFailure(Component.literal("阵营不存在")); return 0; }
                        BaseSupplyAreas data = BaseSupplyAreas.get(context.getSource().getServer());
                        if (action.equals("clear")) data.set(faction, null);
                        if (action.equals("set")) {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Selection selection = SELECTIONS.get(player.getUUID());
                            if (selection == null || selection.first == null || selection.second == null
                                    || !selection.dimension.equals(player.level().dimension().location())) {
                                context.getSource().sendFailure(Component.literal("请在当前维度用圈地工具选择 A、B 两点")); return 0;
                            }
                            try {
                                BaseSupplyArea area = new BaseSupplyArea(selection.dimension, selection.first, selection.second);
                                DeploymentPoint base = DeploymentService.get(player).flatMap(service -> service.mainBase(faction)).orElse(null);
                                if (base == null || !area.contains(base.dimension(), base.position())) {
                                    context.getSource().sendFailure(Component.literal("范围必须包含该阵营已设置的主基地部署点")); return 0;
                                }
                                data.set(faction, area);
                            } catch (IllegalArgumentException exception) {
                                context.getSource().sendFailure(Component.literal(exception.getMessage())); return 0;
                            }
                        }
                        BaseSupplyArea area = data.area(faction);
                        context.getSource().sendSuccess(() -> Component.literal(faction.id() + " 基地补给区："
                                + (area == null ? "未设置" : area.dimension() + " " + area.min().toShortString()
                                + " → " + area.max().toShortString())), true);
                        return 1;
                    })));
        }
        event.getDispatcher().register(Commands.literal("battle").then(root));
    }
}
