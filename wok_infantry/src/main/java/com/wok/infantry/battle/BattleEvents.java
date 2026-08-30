package com.wok.infantry.battle;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.server.BattleCommands;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.server.LoadoutService;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.registry.InfantryBlocks;
import com.wok.infantry.registry.InfantryItems;
import com.wok.infantry.support.SupportService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge lifecycle bridge kept separate from the mod entrypoint. */
@Mod.EventBusSubscriber(modid = "wok_infantry", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattleEvents {
    private BattleEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        BattleService.start(event.getServer());
        DeploymentService.start(event.getServer());
        FormationService.start(event.getServer());
        SupportService.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BattleService.get(event.getServer()).ifPresent(service -> {
            long now = System.currentTimeMillis();
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                service.onPlayerDisconnected(player);
            }
            service.cleanupExpiredReservations(now);
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SupportService.stop(event.getServer());
        DeploymentService.stop(event.getServer());
        BattleService.stop(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleService.get(player).ifPresent(service -> {
                ActionResult admission = service.onPlayerConnected(player);
                DeploymentService deployment = DeploymentService.get(player).orElse(null);
                boolean participant = admission.success()
                        && service.factionOf(player.getUUID()).isPresent()
                        && service.formationOf(player.getUUID()).isPresent();
                if (deployment != null && participant) {
                    deployment.onPlayerConnected(player);
                } else if (deployment != null) {
                    // All admission failures fail closed. This also consumes an offline
                    // reservation expiry whose old issued kit may still be in player NBT.
                    deployment.revokePlayer(player);
                }
                if (!admission.success()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            admission.message()));
                }
                if (!participant && admission.success() && FormationNetwork.isInitialized()) {
                    BattleNetwork.sendClearToPlayer(player);
                    FormationNetwork.sendSnapshotToPlayer(player, true);
                } else if (BattleNetwork.isInitialized()) {
                    BattleOpenTarget target = participant && deployment != null
                            && !deployment.isActive(player.getUUID())
                            ? BattleOpenTarget.DEPLOYMENT : BattleOpenTarget.NONE;
                    BattleNetwork.sendSnapshotToPlayer(service, player, target);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeploymentService.get(player).ifPresent(service ->
                    service.onPlayerDisconnected(player));
            BattleService.get(player).ifPresent(service -> {
                service.onPlayerDisconnected(player);
            });
            ServerRequestLimiter.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeploymentService.get(player).ifPresent(service ->
                    service.onPlayerRespawned(player, event.isEndConquered()));
        }
        sendCurrentSnapshot(event, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeploymentService.get(player).ifPresent(service -> {
                if (service.isVehicleTestMode(player.getUUID())) {
                    return;
                }
                if (service.isActive(player.getUUID())) {
                    LoadoutService.get(player).ifPresent(loadout ->
                            loadout.clearManagedCombatInventory(player));
                }
                service.onPlayerDeath(player);
            });
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerAttack(LivingAttackEvent event) {
        net.minecraft.world.entity.Entity source = event.getSource().getEntity();
        ServerPlayer attacker;
        if (source instanceof ServerPlayer sourcePlayer) {
            attacker = sourcePlayer;
        } else {
            net.minecraft.world.entity.Entity direct = event.getSource().getDirectEntity();
            if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    && projectile.getOwner() instanceof ServerPlayer owner) {
                attacker = owner;
            } else {
                return;
            }
        }
        if (isWaitingParticipant(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isWaitingParticipant(player)) {
            // LivingAttackEvent does not cover armor stands, crystals, vehicles or reflected
            // projectiles, all of which may otherwise let a waiting player affect the battle.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            KitProvenance.removeIssuedDrops(event.getDrops());
        }
    }

    @SubscribeEvent
    public static void onIssuedItemToss(ItemTossEvent event) {
        boolean managed = event.getPlayer() instanceof ServerPlayer player
                && DeploymentService.get(player)
                .map(service -> !service.isVehicleTestMode(player.getUUID())
                        && (service.isActive(player.getUUID())
                        || service.isWaitingParticipant(player.getUUID()))).orElse(false);
        if (managed
                || KitProvenance.scanPayload(event.getEntity().getItem())
                != KitProvenance.PayloadScanResult.CLEAR) {
            // Forge restores the captured stack when ItemTossEvent is cancelled.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIssuedItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        KitProvenance.PayloadScanResult payload = KitProvenance.scanPayload(
                event.getItem().getItem());
        boolean managedDeployment = DeploymentService.get(player)
                .map(service -> !service.isVehicleTestMode(player.getUUID())
                        && (service.isActive(player.getUUID())
                        || service.isWaitingParticipant(player.getUUID()))).orElse(false);
        if (payload == KitProvenance.PayloadScanResult.CLEAR && !managedDeployment) {
            return;
        }
        // Issued stacks cannot legitimately reach the ground: toss is cancelled and death drops
        // are removed. Discard any entity created by another mod instead of enabling duplication.
        event.setCanceled(true);
        if (payload == KitProvenance.PayloadScanResult.ISSUED) {
            event.getItem().discard();
        }
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        boolean testMode = deployment != null
                && deployment.isVehicleTestMode(player.getUUID());
        boolean active = deployment != null && !testMode
                && deployment.isActive(player.getUUID());
        boolean waiting = deployment != null
                && !testMode
                && deployment.isWaitingParticipant(player.getUUID());
        if ((active || waiting) && player.containerMenu != player.inventoryMenu) {
            // Schedule after the open hook completes; managed lives cannot import arbitrary gear.
            player.server.execute(() -> {
                DeploymentService current = DeploymentService.get(player).orElse(null);
                if (current != null && !current.isVehicleTestMode(player.getUUID())
                        && (current.isActive(player.getUUID())
                        || current.isWaitingParticipant(player.getUUID()))) {
                    player.closeContainer();
                }
            });
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Close is the last reliable point at which the external menu slots remain visible.
            DeploymentService.get(player).ifPresent(service -> {
                if (!service.isVehicleTestMode(player.getUUID())) {
                    service.tickPlayer(player);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean administratorSetupBlock = player.hasPermissions(
                BattleRules.ADMIN_PERMISSION_LEVEL)
                && (event.getItemStack().is(InfantryItems.DEPLOYMENT_BEACON.get())
                || event.getItemStack().is(InfantryItems.AMMO_SUPPLY_CRATE.get()));
        if (isWaitingParticipant(player) && !administratorSetupBlock) {
            event.setCanceled(true);
            return;
        }
        boolean active = DeploymentService.get(player)
                .map(service -> !service.isVehicleTestMode(player.getUUID())
                        && service.isActive(player.getUUID())).orElse(false);
        boolean issuedPortableAmmoCrate = active
                && event.getItemStack().is(InfantryItems.AMMO_SUPPLY_CRATE.get())
                && DeploymentService.get(player)
                .map(service -> service.isValidIssuedStack(player,
                        event.getItemStack())).orElse(false);
        if (active && !administratorSetupBlock && !issuedPortableAmmoCrate
                && (event.getItemStack().getItem() instanceof BlockItem
                || event.getItemStack().getItem() instanceof BucketItem)) {
            // World blocks/fluids cannot carry item provenance and would persist beyond this life.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && isWaitingParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerMount(EntityMountEvent event) {
        if (!event.isMounting()
                || !(event.getEntityMounting() instanceof ServerPlayer player)) {
            return;
        }
        if (isVehicleTestMode(player)) {
            return;
        }
        ActionResult authorization = FormationService.get(player)
                .map(service -> service.authorizeMount(player, event.getEntityBeingMounted()))
                .orElseGet(() -> ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                        "阵营编制服务尚未就绪"));
        if (!authorization.success()) {
            event.setCanceled(true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    authorization.message()));
            return;
        }
        if (isWaitingParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFormationVehicleLoaded(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        FormationService.get(level.getServer()).ifPresent(service -> {
            ActionResult result = service.observeLoadedVehicle(event.getEntity());
            if (!result.success()) {
                event.setCanceled(true);
                WokInfantryMod.LOGGER.warn("Rejected formation vehicle ledger recovery for {}: {}",
                        event.getEntity().getUUID(), result.message());
            } else if (event.getEntity().isRemoved()) {
                // A pending reset was completed before the stale entity could re-enter the level.
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onFormationVehicleRemoved(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FormationService.get(level.getServer()).ifPresent(service -> {
                ActionResult result = service.observeRemovedVehicle(event.getEntity());
                if (!result.success()) {
                    WokInfantryMod.LOGGER.warn(
                            "Formation vehicle ledger removal mismatch for {}: {}",
                            event.getEntity().getUUID(), result.message());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onManagedPlayerTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeploymentService deployment = DeploymentService.get(player).orElse(null);
            if (deployment != null
                    && !deployment.isVehicleTestMode(player.getUUID())
                    && (deployment.isActive(player.getUUID())
                    || deployment.isWaitingParticipant(player.getUUID()))
                    && !deployment.isInternalTransition(player.getUUID())) {
                // Participants may not use Nether/End portals or cross-mod teleporters to leave
                // the authoritative battle dimension. Deployment-owned moves are wrapped by
                // teleportInternally and remain allowed.
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isWaitingParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onWaitingPlayerPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            boolean managed = DeploymentService.get(player)
                    .map(service -> !service.isVehicleTestMode(player.getUUID())
                            && (service.isActive(player.getUUID())
                            || service.isWaitingParticipant(player.getUUID()))).orElse(false);
            boolean administratorSetupBlock = player.hasPermissions(
                    BattleRules.ADMIN_PERMISSION_LEVEL)
                    && (event.getPlacedBlock().is(InfantryBlocks.DEPLOYMENT_BEACON.get())
                    || event.getPlacedBlock().is(InfantryBlocks.AMMO_SUPPLY_CRATE.get()))
                    && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                    && !level.dimension().equals(DeploymentService.HOLDING_LEVEL)
                    && !level.dimension().equals(DeploymentService.LOBBY_LEVEL);
            boolean portableAmmoDeployment = event.getPlacedBlock().is(
                    InfantryBlocks.AMMO_SUPPLY_CRATE.get())
                    && DeploymentService.get(player)
                    .map(service -> service.isActive(player.getUUID())
                            && !service.isVehicleTestMode(player.getUUID())).orElse(false);
            if (managed && !administratorSetupBlock && !portableAmmoDeployment) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            DeploymentService.get(player).ifPresent(service -> service.tickPlayer(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendCurrentSnapshot(event, false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        SupportService.get(event.getServer()).ifPresent(SupportService::tick);
        if (event.getServer().getTickCount() % 20 == 0) {
            BattleService.get(event.getServer()).ifPresent(service -> {
                service.tick();
                DeploymentService.get(event.getServer()).ifPresent(DeploymentService::tick);
                LoadoutService.get(event.getServer()).ifPresent(LoadoutService::tick);
                FormationService.get(event.getServer()).ifPresent(
                        FormationService::tickVehicles);
                if (BattleNetwork.isInitialized()) {
                    BattleNetwork.broadcastSnapshots(service, event.getServer());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BattleCommands.register(event.getDispatcher());
    }

    private static void sendCurrentSnapshot(PlayerEvent event, boolean openDeploymentWhenWaiting) {
        if (event.getEntity() instanceof ServerPlayer player && BattleNetwork.isInitialized()) {
            BattleService.get(player).ifPresent(service -> {
                if (service.factionOf(player.getUUID()).isEmpty()
                        || service.formationOf(player.getUUID()).isEmpty()) {
                    BattleNetwork.sendClearToPlayer(player);
                    if (FormationNetwork.isInitialized()) {
                        FormationNetwork.sendSnapshotToPlayer(player, true);
                    }
                    return;
                }
                boolean waiting = openDeploymentWhenWaiting
                        && DeploymentService.get(player)
                        .map(deployment -> !deployment.isVehicleTestMode(player.getUUID())
                                && service.factionOf(player.getUUID()).isPresent()
                                && !deployment.isActive(player.getUUID()))
                        .orElse(false);
                BattleNetwork.sendSnapshotToPlayer(service, player,
                        waiting ? BattleOpenTarget.DEPLOYMENT : BattleOpenTarget.NONE);
            });
        }
    }

    private static boolean isWaitingParticipant(ServerPlayer player) {
        return player != null && DeploymentService.get(player)
                .map(service -> !service.isVehicleTestMode(player.getUUID())
                        && service.isWaitingParticipant(player.getUUID()))
                .orElse(false);
    }

    private static boolean isVehicleTestMode(ServerPlayer player) {
        return player != null && DeploymentService.get(player)
                .map(service -> service.isVehicleTestMode(player.getUUID()))
                .orElse(false);
    }
}
