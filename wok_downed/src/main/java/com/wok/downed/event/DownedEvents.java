package com.wok.downed.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedPose;
import com.wok.downed.state.DownedService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

public final class DownedEvents {
    private static final TagKey<DamageType> BYPASSES_DOWNED = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    WokDownedMod.MOD_ID, "bypasses_downed"));

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || DownedService.isForcingDeath()) {
            return;
        }
        if (!DownedConfig.ENABLED.get() || event.getSource().is(BYPASSES_DOWNED)
                || player.isCreative() || player.isSpectator()) {
            DownedService.cancelByParticipant(player, true);
            DownedService.stopDraggingByParticipant(player, true);
            if (DownedService.isDowned(player)) {
                DownedService.clearState(player);
            }
            return;
        }
        event.setCanceled(true);
        if (!DownedService.isDowned(player)) {
            DownedService.enterDowned(player);
        } else {
            player.setHealth(Math.max(1.0F, player.getHealth()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer casualty
                && DownedService.isDowned(casualty)) {
            event.setCanceled(true);
            DownedService.cancelByParticipant(casualty, true);
            DownedService.stopDraggingByParticipant(casualty, true);
            if (event.getSource().is(BYPASSES_DOWNED)
                    || (DownedConfig.ALLOW_FINISHING_DAMAGE.get()
                    && !DownedService.inFinishingGrace(casualty))) {
                DownedService.scheduleFinishingDeath(casualty, event.getSource());
            }
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && DownedService.isDowned(attacker)) {
            event.setCanceled(true);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            DownedService.cancelByParticipant(player, true);
            DownedService.stopDraggingByParticipant(player, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide()
                && event.player instanceof ServerPlayer player) {
            if (DownedService.isDowned(player)) {
                DownedPose.lock(player);
            }
            if (event.phase == TickEvent.Phase.END) {
                DownedService.tickPlayer(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ServerPlayer casualty)
                && !(event.getTarget() instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }
        if (!(event.getTarget() instanceof net.minecraft.world.entity.player.Player target)
                || !DownedService.isDowned(target)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer rescuer)) {
            return;
        }
        if (rescuer.isShiftKeyDown()) {
            DownedService.toggleDrag(rescuer, (ServerPlayer) target);
        } else {
            DownedService.startRescue(rescuer, (ServerPlayer) target);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (DownedService.isDowned(event.getEntity()) && event.isCancelable()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (DownedService.isDowned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player
                && DownedService.isDowned(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (DownedService.isDowned(event.getEntity())) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting()
                && event.getEntityMounting() instanceof ServerPlayer player
                && DownedService.isDowned(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (DownedService.isDowned(player)) {
                event.setCanceled(true);
            } else {
                DownedService.stopDraggingByParticipant(player, true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DownedService.cancelByParticipant(player, true);
            DownedService.stopDraggingByParticipant(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DownedService.cancelByParticipant(player, false);
            DownedService.stopDraggingByParticipant(player, false);
            DownedService.clearState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().remove("wok_downed.active");
        event.getEntity().getPersistentData().remove("wok_downed.start_time");
        event.getEntity().getPersistentData().remove("wok_downed.stabilize_pending");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DownedService.clearRuntimeState();
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wokdowned")
                .then(Commands.literal("giveup")
                        .executes(context -> giveUp(context.getSource().getPlayerOrException())))
                .then(Commands.literal("revive")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(DownedEvents::reviveTargets))));
    }

    private static int giveUp(ServerPlayer player) {
        DownedService.giveUp(player);
        return 1;
    }

    private static int reviveTargets(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int revived = 0;
        for (ServerPlayer target : targets) {
            if (DownedService.isDowned(target)) {
                DownedService.revive(target, null);
                revived++;
            }
        }
        int count = revived;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.wok_downed.revived_count", count), true);
        return revived;
    }

    private DownedEvents() {
    }
}
