package com.wok.infantry.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.block.VehicleDeploymentBlock;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.integration.tacz.TaczAdsSpeedAdapter;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.s2c.OpenWeaponTuningPacket;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.registry.InfantryBlocks;
import com.wok.infantry.stamina.StaminaEvents;
import com.wok.infantry.stamina.StaminaRules;
import com.wok.infantry.stamina.StaminaState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/** Minimal command surface for server setup and for exercising the authoritative squad API. */
public final class BattleCommands {
    private BattleCommands() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> deploymentBeaconCommand() {
        return Commands.literal("beacon")
                .requires(source -> source.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL))
                .then(Commands.literal("place")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Faction faction : Faction.values()) {
                                        builder.suggest(faction.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("dimension",
                                                DimensionArgument.dimension())
                                        .then(Commands.argument("position",
                                                        BlockPosArgument.blockPos())
                                                .executes(context -> placeDeploymentBeacon(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        DimensionArgument.getDimension(
                                                                context, "dimension"),
                                                        BlockPosArgument.getBlockPos(
                                                                context, "position"),
                                                        context.getSource().getRotation().y))
                                                .then(Commands.argument("yaw",
                                                                AngleArgument.angle())
                                                        .executes(context ->
                                                                placeDeploymentBeacon(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context, "faction"),
                                                                        DimensionArgument.getDimension(
                                                                                context, "dimension"),
                                                                        BlockPosArgument.getBlockPos(
                                                                                context, "position"),
                                                                        AngleArgument.getAngle(
                                                                                context, "yaw"))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(context -> removeDeploymentBeacon(
                                                context.getSource(),
                                                DimensionArgument.getDimension(
                                                        context, "dimension"),
                                                BlockPosArgument.getBlockPos(
                                                        context, "position"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vehicleDeploymentCommand() {
        return Commands.literal("vehicle")
                .requires(source -> source.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL))
                .then(Commands.literal("place")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Faction faction : Faction.values()) {
                                        builder.suggest(faction.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("dimension",
                                                DimensionArgument.dimension())
                                        .then(Commands.argument("position",
                                                        BlockPosArgument.blockPos())
                                                .executes(context -> placeVehicleDeployment(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        DimensionArgument.getDimension(
                                                                context, "dimension"),
                                                        BlockPosArgument.getBlockPos(
                                                                context, "position"),
                                                        context.getSource().getRotation().y))
                                                .then(Commands.argument("yaw",
                                                                AngleArgument.angle())
                                                        .executes(context ->
                                                                placeVehicleDeployment(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context, "faction"),
                                                                        DimensionArgument.getDimension(
                                                                                context, "dimension"),
                                                                        BlockPosArgument.getBlockPos(
                                                                                context, "position"),
                                                                        AngleArgument.getAngle(
                                                                                context, "yaw"))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(context -> removeVehicleDeployment(
                                                context.getSource(),
                                                DimensionArgument.getDimension(
                                                        context, "dimension"),
                                                BlockPosArgument.getBlockPos(
                                                        context, "position"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> formationCommand() {
        return Commands.literal("formation")
                .then(Commands.literal("catalog")
                        .executes(context -> formationCatalog(context.getSource())))
                .then(Commands.literal("join")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(context -> selectFaction(
                                        context.getSource(), StringArgumentType.getString(
                                                context, "faction")))))
                .then(Commands.literal("vote")
                        .then(Commands.argument("formation", StringArgumentType.word())
                                .executes(context -> castFormationVote(
                                        context.getSource(), StringArgumentType.getString(
                                                context, "formation")))))
                .then(Commands.literal("select")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    FormationService.get(context.getSource().getServer())
                                            .ifPresent(service -> service.catalog().factions()
                                                    .forEach(faction -> builder.suggest(faction.id())));
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("formation", StringArgumentType.word())
                                        .executes(context -> selectFormation(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "faction"),
                                                StringArgumentType.getString(
                                                        context, "formation"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ammoSupplyCommand() {
        return Commands.literal("ammo")
                .then(Commands.literal("status")
                        .executes(context -> ammoSupplyStatus(context.getSource())))
                .then(Commands.literal("limit")
                        .requires(source -> source.hasPermission(
                                BattleRules.ADMIN_PERMISSION_LEVEL))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(
                                        InfantryServerConfig.MIN_AMMO_RESERVE_LIMIT,
                                        InfantryServerConfig.MAX_AMMO_RESERVE_LIMIT))
                                .executes(context -> setAmmoSupplyLimit(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "rounds")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> formationAdminCommand() {
        return Commands.literal("formation")
                .then(Commands.literal("reload")
                        .executes(context -> reloadFormations(context.getSource())))
                .then(Commands.literal("assign")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .then(Commands.argument("formation",
                                                        StringArgumentType.word())
                                                .executes(context -> assignFormation(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(
                                                                context, "player"),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        StringArgumentType.getString(
                                                                context, "formation")))))))
                .then(Commands.literal("vote")
                        .then(Commands.literal("open")
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .then(Commands.argument("allowVoteChange",
                                                        BoolArgumentType.bool())
                                                .executes(context -> openFormationVote(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        BoolArgumentType.getBool(context,
                                                                "allowVoteChange"))))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .then(Commands.argument("formation",
                                                        StringArgumentType.word())
                                                .executes(context -> lockFormationVote(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        StringArgumentType.getString(
                                                                context, "formation")))))))
                .then(Commands.literal("vehicles")
                        .then(Commands.literal("deploy")
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .then(Commands.argument("formation",
                                                        StringArgumentType.word())
                                                .executes(context -> deployFormationVehicles(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context, "faction"),
                                                        StringArgumentType.getString(
                                                                context, "formation"))))))
                        .then(Commands.literal("reset")
                                .executes(context -> resetFormationVehicles(
                                        context.getSource())))
                        .then(Commands.literal("activate")
                                .executes(context -> activateFormationVehicles(
                                        context.getSource()))));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("battle")
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(ammoSupplyCommand())
                .then(formationCommand())
                .then(Commands.literal("squad")
                        .then(Commands.literal("create")
                                .then(Commands.argument("callsign", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (SquadCallsign callsign : SquadCallsign.values()) {
                                                builder.suggest(callsign.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> squadAction(context.getSource(),
                                                callsign(context, "callsign"), BattleService::createSquad))))
                        .then(Commands.literal("join")
                                .then(Commands.argument("callsign", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (SquadCallsign callsign : SquadCallsign.values()) {
                                                builder.suggest(callsign.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> squadAction(context.getSource(),
                                                callsign(context, "callsign"), BattleService::joinSquad))))
                        .then(Commands.literal("leave")
                                .executes(context -> playerAction(context.getSource(),
                                        BattleService::leaveSquad)))
                        .then(Commands.literal("disband")
                                .executes(context -> playerAction(context.getSource(),
                                        BattleService::disbandSquad)))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> targetAction(context.getSource(),
                                                EntityArgument.getPlayer(context, "player").getUUID(),
                                                BattleService::transferLeadership))))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> targetAction(context.getSource(),
                                                EntityArgument.getPlayer(context, "player").getUUID(),
                                                BattleService::kickMember)))))
                .then(Commands.literal("commander")
                        .then(Commands.literal("claim")
                                .executes(context -> playerAction(context.getSource(),
                                        BattleService::claimCommander)))
                        .then(Commands.literal("resign")
                                .executes(context -> playerAction(context.getSource(),
                                        BattleService::resignCommander)))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> targetAction(context.getSource(),
                                                EntityArgument.getPlayer(context, "player").getUUID(),
                                                BattleService::transferCommander)))))
                .then(Commands.literal("deployment")
                        .then(Commands.literal("status")
                                .executes(context -> deploymentStatus(context.getSource())))
                        .then(Commands.literal("select")
                                .then(Commands.argument("point", StringArgumentType.word())
                                        .executes(context -> selectDeploymentPoint(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "point")))))
                        .then(Commands.literal("deploy")
                                .executes(context -> deploymentAction(context.getSource(),
                                        DeploymentService::deploy)))
                        .then(Commands.literal("redeploy")
                                .executes(context -> deploymentAction(context.getSource(),
                                        DeploymentService::redeploy)))
                        .then(Commands.literal("resupply")
                                .executes(context -> deploymentAction(context.getSource(),
                                        DeploymentService::resupply)))
                        .then(Commands.literal("setbase")
                                .requires(source -> source.hasPermission(
                                        BattleRules.ADMIN_PERMISSION_LEVEL))
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (Faction faction : Faction.values()) {
                                                builder.suggest(faction.id());
                                            }
                                            return builder.buildFuture();
                                         })
                                         .executes(context -> setDeploymentBase(
                                                 context.getSource(),
                                                 StringArgumentType.getString(context, "faction")))
                                         .then(Commands.argument("dimension",
                                                         DimensionArgument.dimension())
                                                 .then(Commands.argument("position",
                                                                 BlockPosArgument.blockPos())
                                                         .executes(context -> setDeploymentBaseAt(
                                                                 context.getSource(),
                                                                 StringArgumentType.getString(
                                                                         context, "faction"),
                                                                 DimensionArgument.getDimension(
                                                                         context, "dimension"),
                                                                 BlockPosArgument.getBlockPos(
                                                                         context, "position"),
                                                                 context.getSource().getRotation().y))
                                                         .then(Commands.argument("yaw",
                                                                         AngleArgument.angle())
                                                                 .executes(context ->
                                                                         setDeploymentBaseAt(
                                                                                 context.getSource(),
                                                                                 StringArgumentType.getString(
                                                                                         context, "faction"),
                                                                                 DimensionArgument.getDimension(
                                                                                         context, "dimension"),
                                                                                 BlockPosArgument.getBlockPos(
                                                                                         context, "position"),
                                                                                 AngleArgument.getAngle(
                                                                                         context, "yaw"))))))))
                        .then(deploymentBeaconCommand())
                        .then(vehicleDeploymentCommand())
                        .then(Commands.literal("clearbase")
                                .requires(source -> source.hasPermission(
                                        BattleRules.ADMIN_PERMISSION_LEVEL))
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (Faction faction : Faction.values()) {
                                                builder.suggest(faction.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> clearDeploymentBase(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "faction"))))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL))
                        .then(formationAdminCommand())
                        .then(vehicleTestCommand())
                        .then(heldWeaponCommand())
                        .then(staminaAdminCommand())
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (Faction faction : Faction.values()) {
                                                        builder.suggest(faction.id());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> assignFaction(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "faction"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> targetAction(context.getSource(),
                                                EntityArgument.getPlayer(context, "player").getUUID(),
                                                BattleService::removeFromBattle))))
                        .then(Commands.literal("reset")
                                .executes(context -> resetBattle(context.getSource())))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vehicleTestCommand() {
        return Commands.literal("test")
                .then(Commands.literal("vehicle")
                        .then(Commands.literal("on")
                                .executes(context -> setVehicleTestMode(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(), true))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setVehicleTestMode(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                true))))
                        .then(Commands.literal("off")
                                .executes(context -> setVehicleTestMode(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(), false))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setVehicleTestMode(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                false))))
                        .then(Commands.literal("status")
                                .executes(context -> vehicleTestStatus(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> vehicleTestStatus(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> heldWeaponCommand() {
        return Commands.literal("weapon")
                .then(Commands.literal("edit")
                        .executes(context -> openHeldWeaponEditor(
                                context.getSource())))
                .then(Commands.literal("ads")
                        .then(Commands.literal("reset")
                                .executes(context -> resetHeldWeaponAds(
                                        context.getSource())))
                        .then(Commands.argument("slowdownPercent", IntegerArgumentType.integer(
                                        AdsSpeedPolicyBounds.MIN_PERCENT,
                                        AdsSpeedPolicyBounds.MAX_PERCENT))
                                .executes(context -> setHeldWeaponAds(
                                        context.getSource(), IntegerArgumentType.getInteger(
                                                context, "slowdownPercent")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> staminaAdminCommand() {
        return Commands.literal("stamina")
                .then(Commands.literal("status")
                        .executes(context -> staminaStatus(context.getSource())))
                .then(Commands.literal("reset")
                        .executes(context -> setStamina(context.getSource(),
                                StaminaRules.MAX_STAMINA, StaminaRules.MAX_STAMINA)))
                .then(Commands.literal("set")
                        .then(Commands.argument("arms", IntegerArgumentType.integer(
                                        0, (int) StaminaRules.MAX_STAMINA))
                                .then(Commands.argument("legs", IntegerArgumentType.integer(
                                                0, (int) StaminaRules.MAX_STAMINA))
                                        .executes(context -> setStamina(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(
                                                        context, "arms"),
                                                IntegerArgumentType.getInteger(
                                                        context, "legs"))))));
    }

    private static int staminaStatus(CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StaminaState state = StaminaState.load(player);
        source.sendSuccess(() -> Component.translatable(
                "message.wok_infantry.stamina.admin.status",
                Math.round(state.arms()), Math.round(state.legs())), false);
        return 1;
    }

    private static int setStamina(CommandSourceStack source, float arms, float legs)
            throws CommandSyntaxException {
        StaminaState state = StaminaEvents.overwrite(
                source.getPlayerOrException(), arms, legs);
        source.sendSuccess(() -> Component.translatable(
                "message.wok_infantry.stamina.admin.set",
                Math.round(state.arms()), Math.round(state.legs())), false);
        return 1;
    }

    public static int openHeldWeaponEditor(CommandSourceStack source)
            throws CommandSyntaxException {
        if (!source.hasPermission(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_tuning.permission"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!TaczAdsSpeedAdapter.available()) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.tacz_unavailable"));
            return 0;
        }
        if (!TaczAdsSpeedAdapter.isGun(stack)) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.not_gun"));
            return 0;
        }
        UUID editorId = TaczAdsSpeedAdapter.ensureEditorId(stack);
        if (editorId == null || !BattleNetwork.isInitialized()) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_tuning.unavailable"));
            return 0;
        }
        syncHeldStack(player);
        BattleNetwork.sendToPlayer(player, new OpenWeaponTuningPacket(editorId,
                stack.getHoverName(), TaczAdsSpeedAdapter.tuning(stack)));
        return 1;
    }

    private static int setHeldWeaponAds(CommandSourceStack source, int slowdownPercent)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!TaczAdsSpeedAdapter.available()) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.tacz_unavailable"));
            return 0;
        }
        if (!TaczAdsSpeedAdapter.setSlowdown(stack, slowdownPercent)) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.not_gun"));
            return 0;
        }
        syncHeldStack(player);
        TaczAdsSpeedAdapter.refresh(player, stack);
        float durationMultiplier = 1.0F / TaczAdsSpeedAdapter.speedScale(stack);
        source.sendSuccess(() -> Component.translatable(
                "message.wok_infantry.weapon_ads.applied", stack.getHoverName(),
                slowdownPercent, String.format(Locale.ROOT, "%.2f", durationMultiplier)), false);
        return 1;
    }

    private static int resetHeldWeaponAds(CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!TaczAdsSpeedAdapter.available()) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.tacz_unavailable"));
            return 0;
        }
        if (!TaczAdsSpeedAdapter.isGun(stack)) {
            source.sendFailure(Component.translatable(
                    "message.wok_infantry.weapon_ads.not_gun"));
            return 0;
        }
        TaczAdsSpeedAdapter.clearSlowdown(stack);
        syncHeldStack(player);
        TaczAdsSpeedAdapter.refresh(player, stack);
        source.sendSuccess(() -> Component.translatable(
                "message.wok_infantry.weapon_ads.reset", stack.getHoverName()), false);
        return 1;
    }

    private static void syncHeldStack(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static final class AdsSpeedPolicyBounds {
        private static final int MIN_PERCENT = 0;
        private static final int MAX_PERCENT = 90;

        private AdsSpeedPolicyBounds() {
        }
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return withService(source, service -> {
            String faction = service.factionOf(player.getUUID())
                    .map(Faction::id).orElse("unassigned");
            String formation = service.formationOf(player.getUUID()).orElse("unassigned");
            String squad = service.squadOf(player.getUUID())
                    .map(SquadCallsign::id).orElse("none");
            source.sendSuccess(() -> Component.literal(
                    "battleSide=" + faction + ", formation=" + formation + ", squad=" + squad
                            + ", class=" + service.assignedClass(player.getUUID())), false);
            return 1;
        });
    }

    private static int ammoSupplyStatus(CommandSourceStack source) {
        int limit = InfantryServerConfig.ammoReserveLimit();
        int cooldownSeconds = InfantryServerConfig.ammoSupplyCooldownTicks() / 20;
        source.sendSuccess(() -> Component.literal("小型箱 100 点，大型站 1500 点；"
                + "中间威力弹 1 点/发，全威力弹 3 点/发，爆炸物 50 点/发；"
                + "每种备用弹上限 " + limit + " 发；成功补给冷却 "
                + cooldownSeconds + " 秒"), false);
        return limit;
    }

    private static int setAmmoSupplyLimit(CommandSourceStack source, int rounds) {
        if (!InfantryServerConfig.setAmmoReserveLimit(rounds)) {
            source.sendFailure(Component.literal("服务端配置尚未载入，弹药上限未修改"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("弹药补给上限已设为每种 " + rounds
                + " 发（不会删除玩家当前超出的弹药）"), true);
        return rounds;
    }

    private static int formationCatalog(CommandSourceStack source) {
        FormationService service = FormationService.get(source.getServer()).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        String summary = service.catalog().factions().stream()
                .map(faction -> faction.id() + "=[" + faction.formations().stream()
                        .map(FormationDefinition::id)
                        .collect(java.util.stream.Collectors.joining(",")) + "]")
                .collect(java.util.stream.Collectors.joining("; "));
        source.sendSuccess(() -> Component.literal("generation=" + service.generation()
                + ", factions=" + summary), false);
        return 1;
    }

    private static int selectFormation(CommandSourceStack source, String factionId,
                                       String formationId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerRequestLimiter.allow(player,
                ServerRequestLimiter.Kind.FORMATION_SELECTION)) {
            source.sendFailure(Component.literal("阵营编制选择操作过于频繁，请稍后重试"));
            return 0;
        }
        FormationService service = FormationService.get(player).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ActionResult result = service.select(player, service.generation(), factionId,
                formationId);
        if (FormationNetwork.isInitialized()) {
            FormationNetwork.finishSelection(player, result);
        }
        return sendResult(source, result);
    }

    private static int selectFaction(CommandSourceStack source, String factionId)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerRequestLimiter.allow(player,
                ServerRequestLimiter.Kind.FORMATION_SELECTION)) {
            source.sendFailure(Component.literal("阵营选择操作过于频繁，请稍后重试"));
            return 0;
        }
        FormationService service = FormationService.get(player).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ActionResult result = service.selectFaction(player, service.generation(), factionId);
        if (FormationNetwork.isInitialized()) {
            FormationNetwork.finishFactionSelection(player, result);
        }
        return sendResult(source, result);
    }

    private static int castFormationVote(CommandSourceStack source, String formationId)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerRequestLimiter.allow(player,
                ServerRequestLimiter.Kind.FORMATION_SELECTION)) {
            source.sendFailure(Component.literal("编制投票操作过于频繁，请稍后重试"));
            return 0;
        }
        FormationService service = FormationService.get(player).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ActionResult result = service.castVote(player, service.generation(), formationId);
        if (FormationNetwork.isInitialized()) {
            FormationNetwork.finishVote(player, result);
        }
        return sendResult(source, result);
    }

    private static int reloadFormations(CommandSourceStack source) {
        FormationService service = FormationService.get(source.getServer()).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        long previousGeneration = service.generation();
        ActionResult result = service.reload();
        boolean applied = service.generation() != previousGeneration;
        if (applied && FormationNetwork.isInitialized()) {
            source.getServer().getPlayerList().getPlayers().forEach(player -> {
                boolean required = service.snapshotFor(player).selectionRequired();
                if (required && BattleNetwork.isInitialized()) {
                    BattleNetwork.sendClearToPlayer(player);
                }
                FormationNetwork.sendSnapshotToPlayer(player, required);
            });
        }
        return sendResult(source, result);
    }

    private static int assignFormation(CommandSourceStack source, ServerPlayer target,
                                       String factionId, String formationId)
            throws CommandSyntaxException {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        if (formations == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ServerPlayer administrator = source.getPlayerOrException();
        ActionResult result = formations.forceAssign(administrator, target, factionId,
                formationId);
        if (result.success() && FormationNetwork.isInitialized()) {
            FormationNetwork.finishSelection(target, result);
        }
        return sendResult(source, result);
    }

    private static int openFormationVote(CommandSourceStack source, String factionId,
                                         boolean allowVoteChange)
            throws CommandSyntaxException {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        if (formations == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ActionResult result = formations.openVote(source.getPlayerOrException(), factionId,
                allowVoteChange);
        if (result.success()) {
            broadcastFormationState(source, formations, true);
        }
        return sendResult(source, result);
    }

    private static int lockFormationVote(CommandSourceStack source, String factionId,
                                         String formationId)
            throws CommandSyntaxException {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        if (formations == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        ActionResult result = formations.lockVote(source.getPlayerOrException(), factionId,
                formationId);
        if (result.success()) {
            broadcastFormationState(source, formations, false);
        }
        return sendResult(source, result);
    }

    private static void broadcastFormationState(CommandSourceStack source,
                                                FormationService formations,
                                                boolean openScreen) {
        if (!FormationNetwork.isInitialized()) {
            return;
        }
        source.getServer().getPlayerList().getPlayers().forEach(player -> {
            FormationNetwork.sendSnapshotToPlayer(player, openScreen);
            if (!openScreen && BattleNetwork.isInitialized()
                    && formations.selectedFormation(player.getUUID()).isPresent()) {
                BattleService.get(player).ifPresent(battle ->
                        BattleNetwork.sendSnapshotToPlayer(battle, player,
                                com.wok.infantry.network.battle.BattleOpenTarget.DEPLOYMENT));
            }
        });
    }

    private static int assignFaction(CommandSourceStack source, ServerPlayer target,
                                     String factionId)
            throws CommandSyntaxException {
        Faction battleSide = Faction.byId(factionId).orElse(null);
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        FactionDefinition publicFaction = battleSide == null || formations == null ? null
                : formations.catalog().findFaction(battleSide).orElse(null);
        if (battleSide == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        if (formations == null || publicFaction == null
                || publicFaction.findFormation("default").isEmpty()) {
            source.sendFailure(Component.literal(
                    "该战斗方没有可用的 default 编制；请使用 /battle admin formation assign"));
            return 0;
        }
        ServerPlayer administrator = source.getPlayerOrException();
        ActionResult result = formations.forceAssign(administrator, target,
                publicFaction.id(), "default");
        if (result.success() && FormationNetwork.isInitialized()) {
            FormationNetwork.finishSelection(target, result);
        }
        return sendResult(source, result);
    }

    private static int deployFormationVehicles(CommandSourceStack source, String factionId,
                                                String formationId) {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        if (formations == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        return sendResult(source,
                formations.deployVehicles(factionId, formationId).result());
    }

    private static int resetFormationVehicles(CommandSourceStack source) {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        if (formations == null) {
            source.sendFailure(Component.literal("阵营编制服务尚未启动"));
            return 0;
        }
        return sendResult(source, formations.resetActiveVehicleSession());
    }

    private static int activateFormationVehicles(CommandSourceStack source) {
        FormationService formations = FormationService.get(source.getServer()).orElse(null);
        DeploymentService deployment = DeploymentService.get(source.getServer()).orElse(null);
        if (formations == null || deployment == null) {
            source.sendFailure(Component.literal("阵营编制或部署服务尚未启动"));
            return 0;
        }
        return sendResult(source, formations.activateVehicleSession(deployment.sessionId()));
    }

    private static int setVehicleTestMode(CommandSourceStack source, ServerPlayer target,
                                          boolean enabled) {
        return withDeploymentService(source, service -> sendResult(source,
                service.setVehicleTestMode(source, target, enabled)));
    }

    private static int vehicleTestStatus(CommandSourceStack source, ServerPlayer target) {
        return withDeploymentService(source, service -> {
            boolean enabled = service.isVehicleTestMode(target.getUUID());
            source.sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                    + " 的载具测试模式：" + (enabled ? "已开启" : "已关闭")), false);
            return 1;
        });
    }

    private static int resetBattle(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer administrator = source.getPlayerOrException();
        return withService(source, service -> {
            ActionResult result = service.resetBattle(administrator);
            int response = sendResult(source, result);
            if (result.success() && FormationNetwork.isInitialized()) {
                source.getServer().getPlayerList().getPlayers().forEach(player -> {
                    if (BattleNetwork.isInitialized()) {
                        BattleNetwork.sendClearToPlayer(player);
                    }
                    FormationNetwork.sendSnapshotToPlayer(player, true);
                });
            }
            return response;
        });
    }

    private static int deploymentStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return withDeploymentService(source, service -> {
            DeploymentView view = service.viewFor(player);
            String point = view.selectedPointId() == null ? "none"
                    : view.selectedPointId().toString();
            String available = view.points().stream()
                    .map(candidate -> candidate.id() + "@" + candidate.dimension()
                            + ":" + candidate.position().toShortString())
                    .collect(java.util.stream.Collectors.joining(","));
            source.sendSuccess(() -> Component.literal("phase=" + view.phase().name().toLowerCase()
                    + ", waitTicks=" + view.waitingTicks()
                    + ", resupplyTicks=" + view.resupplyTicks()
                    + ", selected=" + point
                    + ", available=" + (available.isEmpty() ? "none" : available)), false);
            return 1;
        });
    }

    private static int selectDeploymentPoint(CommandSourceStack source, String encodedId)
            throws CommandSyntaxException {
        UUID pointId;
        try {
            pointId = UUID.fromString(encodedId);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("无效部署点 UUID"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        return withDeploymentService(source, service -> sendResult(source,
                service.selectPoint(player, pointId)));
    }

    private static int deploymentAction(CommandSourceStack source,
                                        DeploymentOperation operation)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return withDeploymentService(source, service -> sendResult(source,
                operation.apply(service, player)));
    }

    private static int clearDeploymentBase(CommandSourceStack source, String factionId) {
        Faction faction = Faction.byId(factionId).orElse(null);
        if (faction == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        return withDeploymentService(source, service -> sendResult(source,
                service.clearMainBase(source, faction)));
    }

    private static int setDeploymentBase(CommandSourceStack source, String factionId)
            throws CommandSyntaxException {
        Faction faction = Faction.byId(factionId).orElse(null);
        if (faction == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        return withDeploymentService(source, service -> sendResult(source,
                service.setMainBaseAt(player, faction, source.getLevel(),
                        net.minecraft.core.BlockPos.containing(source.getPosition()),
                        source.getRotation().y)));
    }

    private static int setDeploymentBaseAt(CommandSourceStack source, String factionId,
                                           ServerLevel level, BlockPos position, float yaw) {
        Faction faction = Faction.byId(factionId).orElse(null);
        if (faction == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        return withDeploymentService(source, service -> sendResult(source,
                service.setMainBaseAt(source, faction, level, position, yaw)));
    }

    private static int placeDeploymentBeacon(CommandSourceStack source, String factionId,
                                             ServerLevel level, BlockPos position, float yaw) {
        Faction faction = Faction.byId(factionId).orElse(null);
        if (faction == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        return withDeploymentService(source, service -> {
            BlockState previous = level.getBlockState(position);
            boolean existingBeacon = previous.is(InfantryBlocks.DEPLOYMENT_BEACON.get());
            if (!existingBeacon && !previous.isAir()) {
                source.sendFailure(Component.literal("目标坐标必须为空气或已有部署信标"));
                return 0;
            }
            if (!existingBeacon && !level.setBlock(position,
                    InfantryBlocks.DEPLOYMENT_BEACON.get().defaultBlockState(),
                    Block.UPDATE_ALL)) {
                source.sendFailure(Component.literal("无法在目标坐标放置部署信标"));
                return 0;
            }
            ActionResult result = service.bindBeacon(source, faction, level, position, yaw);
            if (!result.success() && !existingBeacon
                    && !level.setBlock(position, previous, Block.UPDATE_ALL)) {
                com.wok.infantry.WokInfantryMod.LOGGER.error(
                        "Could not roll back failed deployment beacon placement at {} {}",
                        level.dimension().location(), position);
            }
            return sendResult(source, result);
        });
    }

    private static int removeDeploymentBeacon(CommandSourceStack source, ServerLevel level,
                                               BlockPos position) {
        return withDeploymentService(source, service -> sendResult(source,
                service.removeBeacon(source, level, position)));
    }

    private static int placeVehicleDeployment(CommandSourceStack source, String factionId,
                                              ServerLevel level, BlockPos position, float yaw) {
        Faction faction = Faction.byId(factionId).orElse(null);
        if (faction == null) {
            source.sendFailure(Component.literal("无效阵营: " + factionId));
            return 0;
        }
        return withDeploymentService(source, service -> {
            BlockState previous = level.getBlockState(position);
            boolean existingBlock = previous.is(InfantryBlocks.VEHICLE_DEPLOYMENT.get());
            if (!existingBlock && !previous.isAir()) {
                source.sendFailure(Component.literal(
                        "目标坐标必须为空气或已有载具部署方块"));
                return 0;
            }
            Direction facing = Direction.fromYRot(yaw);
            BlockState desired = (existingBlock ? previous
                    : InfantryBlocks.VEHICLE_DEPLOYMENT.get().defaultBlockState())
                    .setValue(VehicleDeploymentBlock.FACING, facing);
            boolean stateChanged = !desired.equals(previous);
            if (stateChanged && !level.setBlock(position, desired, Block.UPDATE_ALL)) {
                source.sendFailure(Component.literal("无法在目标坐标放置载具部署方块"));
                return 0;
            }
            ActionResult result = service.bindVehicleDeployment(
                    source, faction, level, position);
            if (!result.success() && stateChanged
                    && !level.setBlock(position, previous, Block.UPDATE_ALL)) {
                com.wok.infantry.WokInfantryMod.LOGGER.error(
                        "Could not roll back failed vehicle deployment placement at {} {}",
                        level.dimension().location(), position);
            }
            return sendResult(source, result);
        });
    }

    private static int removeVehicleDeployment(CommandSourceStack source, ServerLevel level,
                                               BlockPos position) {
        return withDeploymentService(source, service -> sendResult(source,
                service.removeVehicleDeployment(source, level, position)));
    }

    private static SquadCallsign callsign(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                          String argument) {
        return SquadCallsign.byId(StringArgumentType.getString(context, argument)).orElse(null);
    }

    private static int squadAction(CommandSourceStack source, SquadCallsign callsign,
                                   SquadOperation operation) throws CommandSyntaxException {
        if (callsign == null) {
            source.sendFailure(Component.literal("无效小队呼号"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        return withService(source, service -> sendResult(source,
                operation.apply(service, player, callsign)));
    }

    private static int playerAction(CommandSourceStack source, PlayerOperation operation)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return withService(source, service -> sendResult(source,
                operation.apply(service, player)));
    }

    private static int targetAction(CommandSourceStack source, UUID targetId,
                                    TargetOperation operation) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return withService(source, service -> sendResult(source,
                operation.apply(service, player, targetId)));
    }

    private static int withService(CommandSourceStack source,
                                   Function<BattleService, Integer> operation) {
        return BattleService.get(source.getServer()).map(operation).orElseGet(() -> {
            source.sendFailure(Component.literal("战局服务尚未启动"));
            return 0;
        });
    }

    private static int withDeploymentService(CommandSourceStack source,
                                             Function<DeploymentService, Integer> operation) {
        return DeploymentService.get(source.getServer()).map(operation).orElseGet(() -> {
            source.sendFailure(Component.literal("部署服务尚未启动"));
            return 0;
        });
    }

    private static int sendResult(CommandSourceStack source, ActionResult result) {
        if (result.success()) {
            source.sendSuccess(() -> Component.literal(result.message()), false);
            return 1;
        }
        source.sendFailure(Component.literal("[" + result.code().name() + "] " + result.message()));
        return 0;
    }

    @FunctionalInterface
    private interface SquadOperation {
        ActionResult apply(BattleService service, ServerPlayer player, SquadCallsign callsign);
    }

    @FunctionalInterface
    private interface PlayerOperation {
        ActionResult apply(BattleService service, ServerPlayer player);
    }

    @FunctionalInterface
    private interface TargetOperation {
        ActionResult apply(BattleService service, ServerPlayer player, UUID targetId);
    }

    @FunctionalInterface
    private interface DeploymentOperation {
        ActionResult apply(DeploymentService service, ServerPlayer player);
    }

}
