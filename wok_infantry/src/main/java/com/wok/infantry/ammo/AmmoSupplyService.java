package com.wok.infantry.ammo;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.block.entity.AmmoSupplyCrateBlockEntity;
import com.wok.infantry.block.entity.LargeAmmoSupplyStationBlockEntity;
import com.wok.infantry.block.entity.MediumAmmoSupplyCrateBlockEntity;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.integration.sbw.SbwVehicleAmmoAdapter;
import com.wok.infantry.integration.tacz.TaczAmmoAdapter;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.s2c.OpenAmmoSupplyPacket;
import com.wok.infantry.registry.InfantryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Finite-point, server-authoritative supply logic for portable crates and trucked stations. */
public final class AmmoSupplyService {
    public static final ResourceLocation LARGE_STATION_ENTITY =
            ResourceLocation.fromNamespaceAndPath("dragonrise_reforge", "ammo_supply_station");
    private static final String LARGE_REMAINING_POINTS_TAG =
            "wok_infantry_ammo_remaining_points";
    private static final String NEXT_SUPPLY_TICK_TAG = "wok_infantry_ammo_supply_next_tick";
    private static final int FIRST_SUPPLEMENT_SLOT = 9;
    private static final int LAST_MAIN_INVENTORY_SLOT = 35;
    private static final double MAX_USE_DISTANCE_SQUARED = 64.0D;
    private static final double VEHICLE_SUPPLY_RANGE = 24.0D;
    private static final double VEHICLE_SUPPLY_RANGE_SQUARED =
            VEHICLE_SUPPLY_RANGE * VEHICLE_SUPPLY_RANGE;
    private static final int MAX_NEARBY_VEHICLES = 16;
    private static final int MAX_VEHICLE_AMMO_OPTIONS = 128;
    private static final int MAX_VEHICLE_REQUEST_ROUNDS = 10_000;

    private AmmoSupplyService() {
    }

    public static void openSmallCrate(ServerPlayer player, BlockPos pos,
                                      AmmoSupplyCrateBlockEntity crate) {
        if (player == null) {
            return;
        }
        if (!TaczAmmoAdapter.available()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.tacz_unavailable"));
            return;
        }
        if (!canOpen(player) || crate == null || crate.isRemoved()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED) {
            return;
        }
        sendView(player, AmmoSupplyView.Target.smallCrate(pos),
                InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS, crate.remainingPoints());
    }

    public static void openMediumCrate(ServerPlayer player, BlockPos pos,
                                       MediumAmmoSupplyCrateBlockEntity crate) {
        if (player == null) {
            return;
        }
        if (!TaczAmmoAdapter.available()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.tacz_unavailable"));
            return;
        }
        if (!canOpen(player) || crate == null || crate.isRemoved()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED) {
            return;
        }
        sendView(player, AmmoSupplyView.Target.mediumCrate(pos),
                InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS, crate.remainingPoints());
    }

    public static void openLargeBlock(ServerPlayer player, BlockPos pos,
                                      LargeAmmoSupplyStationBlockEntity station) {
        if (player == null) {
            return;
        }
        if (!canOpen(player) || station == null || station.isRemoved()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED) {
            return;
        }
        sendView(player, AmmoSupplyView.Target.largeBlock(pos),
                InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                station.remainingPoints());
    }

    public static boolean isLargeStation(Entity entity) {
        return entity != null && LARGE_STATION_ENTITY.equals(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static void openLargeStation(ServerPlayer player, Entity station) {
        if (!canOpen(player) || !isLargeStation(station)
                || player.distanceToSqr(station) > MAX_USE_DISTANCE_SQUARED) {
            return;
        }
        disableNativeLargeStation(station);
        sendView(player, AmmoSupplyView.Target.largeStation(station.getId()),
                InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                largeStationRemainingPoints(station));
    }

    public static void refillLargeStation(ServerPlayer administrator, Entity station) {
        if (administrator == null || !administrator.hasPermissions(
                BattleRules.ADMIN_PERMISSION_LEVEL) || !isLargeStation(station)) {
            return;
        }
        disableNativeLargeStation(station);
        station.getPersistentData().putInt(LARGE_REMAINING_POINTS_TAG,
                InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS);
        administrator.sendSystemMessage(Component.translatable(
                "message.wok_infantry.ammo_supply.refilled",
                InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS));
    }

    public static void selectGun(ServerPlayer player, AmmoSupplyView.Target target,
                                 int inventorySlot) {
        if (player == null) {
            return;
        }
        if (!TaczAmmoAdapter.available()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.tacz_unavailable"));
            return;
        }
        if (!canOpen(player) || target == null || !isGunSlot(inventorySlot)) {
            return;
        }
        ResolvedSupply supply = resolveTarget(player, target);
        if (supply == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.target_invalid"));
            return;
        }
        long now = player.server.overworld().getGameTime();
        long next = player.getPersistentData().getLong(NEXT_SUPPLY_TICK_TAG);
        if (next > now) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.cooldown",
                    Math.max(1L, (next - now + 19L) / 20L)));
            return;
        }

        ItemStack gun = player.getInventory().getItem(inventorySlot);
        Optional<ResourceLocation> detectedAmmo = TaczAmmoAdapter.ammunitionForGun(gun);
        if (detectedAmmo.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.weapon_changed"));
            sendView(player, target, supply.capacity(), supply.remaining());
            return;
        }
        ResourceLocation ammoId = detectedAmmo.get();
        int cost = AmmoPointCostPolicy.pointsPerRound(ammoId);
        int reserveLimit = ammoReserveLimit(gun);
        int deficit = AmmoSupplyPlanner.deficits(List.of(ammoId),
                id -> countAmmo(player, id), reserveLimit)
                .get(ammoId);
        if (deficit == 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.selected_at_limit",
                    gun.getHoverName(), reserveLimit));
            sendView(player, target, supply.capacity(), supply.remaining());
            return;
        }
        int affordable = Math.min(deficit, supply.remaining() / cost);
        if (affordable < 1) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.points_insufficient",
                    cost, supply.remaining()));
            return;
        }

        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        boolean active = deployment != null && deployment.isActive(player.getUUID())
                && !deployment.isVehicleTestMode(player.getUUID());
        UUID issueToken = active ? deployment.activeIssueToken(player).orElse(null) : null;
        if (active && issueToken == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.internal_error"));
            return;
        }
        int added = addAmmo(player, ammoId, affordable, active,
                deployment == null ? null : deployment.sessionId(), issueToken);
        if (added < 1) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.inventory_full"));
            return;
        }
        int spent = added * cost;
        if (supply.consume(spent) != spent) {
            throw new IllegalStateException("Supply points changed during main-thread transaction");
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.getPersistentData().putLong(NEXT_SUPPLY_TICK_TAG,
                now + InfantryServerConfig.ammoSupplyCooldownTicks());
        player.sendSystemMessage(Component.translatable(
                "message.wok_infantry.ammo_supply.selected_success", gun.getHoverName(),
                added, spent, supply.remaining()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_DISPENSE,
                SoundSource.BLOCKS, 0.8F, 0.85F);
        sendView(player, target, supply.capacity(), supply.remaining());
    }

    /**
     * Installs the no-cost reserve ammunition for a player's first deployment of a battle
     * session. The deployment state machine owns the once-per-session gate; this method derives
     * the equipped guns' per-item limits and creates only server-stamped physical TaCZ ammunition.
     */
    public static InitialReserveGrant grantInitialReserve(ServerPlayer player, UUID sessionId,
                                                           UUID issueToken) {
        if (player == null || sessionId == null || issueToken == null
                || !TaczAmmoAdapter.available()) {
            return InitialReserveGrant.none();
        }
        LinkedHashMap<ResourceLocation, Integer> reserveLimits = new LinkedHashMap<>();
        for (int slot = 0; slot <= LAST_MAIN_INVENTORY_SLOT; slot++) {
            addInitialReserveRequirement(player, slot, sessionId, issueToken, reserveLimits);
        }
        addInitialReserveRequirement(player, 40, sessionId, issueToken, reserveLimits);
        if (reserveLimits.isEmpty()) {
            return InitialReserveGrant.none();
        }

        Map<ResourceLocation, Integer> deficits = AmmoSupplyPlanner.deficitsByType(
                reserveLimits, ammoId -> countAmmo(player, ammoId));
        int requested = 0;
        int added = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : deficits.entrySet()) {
            int deficit = entry.getValue();
            requested += deficit;
            added += addAmmo(player, entry.getKey(), deficit, true, sessionId, issueToken);
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return new InitialReserveGrant(reserveLimits.size(), requested, added);
    }

    /** Performs one explicit, package-aligned vehicle ammunition transaction. */
    public static void supplyVehicleAmmo(ServerPlayer player,
                                         AmmoSupplyView.Target stationTarget,
                                         int vehicleEntityId, String weaponKey,
                                         int consumerIndex, int requestedRounds) {
        if (!canOpen(player) || stationTarget == null || !stationTarget.isLarge()
                || weaponKey == null || weaponKey.isBlank()
                || weaponKey.length() > 128 || consumerIndex < 0 || consumerIndex > 255
                || requestedRounds < 1 || requestedRounds > MAX_VEHICLE_REQUEST_ROUNDS) {
            return;
        }
        ResolvedSupply supply = resolveTarget(player, stationTarget);
        StationOrigin origin = stationOrigin(player, stationTarget);
        Entity vehicle = player.level().getEntity(vehicleEntityId);
        if (supply == null || origin == null
                || !SbwVehicleAmmoAdapter.isVehicle(vehicle)
                || vehicle == origin.excludedEntity()
                || origin.distanceToSqr(vehicle) > VEHICLE_SUPPLY_RANGE_SQUARED) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.vehicle_invalid"));
            return;
        }
        if (origin.excludedEntity() != null) {
            disableNativeLargeStation(origin.excludedEntity());
        }
        long now = player.server.overworld().getGameTime();
        long next = player.getPersistentData().getLong(NEXT_SUPPLY_TICK_TAG);
        if (next > now) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.cooldown",
                    Math.max(1L, (next - now + 19L) / 20L)));
            return;
        }
        SbwVehicleAmmoAdapter.AmmoTarget ammunition = SbwVehicleAmmoAdapter.find(
                vehicle, weaponKey, consumerIndex);
        if (ammunition == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.vehicle_ammo_changed"));
            sendView(player, stationTarget, supply.capacity(), supply.remaining());
            return;
        }
        int cost = AmmoPointCostPolicy.pointsPerRound(ammunition.classificationKey());
        VehicleAmmoSupplyMath.Plan plan = VehicleAmmoSupplyMath.plan(requestedRounds,
                ammunition.roundsPerPackage(), cost, supply.remaining(),
                ammunition.packageCapacity());
        if (plan.packages() < 1) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.vehicle_cannot_supply",
                    cost, supply.remaining()));
            sendView(player, stationTarget, supply.capacity(), supply.remaining());
            return;
        }
        int insertedPackages = SbwVehicleAmmoAdapter.insertPackages(ammunition,
                plan.packages());
        if (insertedPackages < 1) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.vehicle_inventory_full"));
            sendView(player, stationTarget, supply.capacity(), supply.remaining());
            return;
        }
        int suppliedRounds = insertedPackages * ammunition.roundsPerPackage();
        int spent = suppliedRounds * cost;
        if (supply.consume(spent) != spent) {
            throw new IllegalStateException("Supply points changed during vehicle transaction");
        }
        player.getPersistentData().putLong(NEXT_SUPPLY_TICK_TAG,
                now + InfantryServerConfig.ammoSupplyCooldownTicks());
        player.sendSystemMessage(Component.translatable(
                "message.wok_infantry.ammo_supply.vehicle_success",
                vehicle.getDisplayName(), ammunition.ammunitionName(), suppliedRounds,
                spent, supply.remaining()));
        player.level().playSound(null, origin.soundPos(), SoundEvents.DISPENSER_DISPENSE,
                SoundSource.BLOCKS, 1.0F, 0.72F);
        sendView(player, stationTarget, supply.capacity(), supply.remaining());
    }

    /** DragonRise's native station auto-fill would bypass the manual selector and point ledger. */
    public static void disableNativeLargeStation(Entity station) {
        if (!isLargeStation(station)) {
            return;
        }
        try {
            station.getClass().getMethod("setActive", boolean.class).invoke(station, false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Older station builds may not expose the toggle; interaction interception still works.
        }
    }

    private static boolean canOpen(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        boolean active = deployment != null && deployment.isActive(player.getUUID())
                && !deployment.isVehicleTestMode(player.getUUID());
        if (!active && !player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.not_deployed"));
            return false;
        }
        return true;
    }

    /** Per-issued-gun limit; unissued administrator test guns use the server fallback. */
    static int ammoReserveLimit(ItemStack gun) {
        return KitProvenance.ammoReserveLimit(gun)
                .orElse(InfantryServerConfig.ammoReserveLimit());
    }

    private static void sendView(ServerPlayer player, AmmoSupplyView.Target target,
                                 int capacity, int remaining) {
        List<AmmoSupplyView.GunOption> guns = new ArrayList<>();
        for (int slot = 0; slot <= LAST_MAIN_INVENTORY_SLOT; slot++) {
            addGunOption(player, guns, slot, remaining);
        }
        addGunOption(player, guns, 40, remaining);
        List<AmmoSupplyView.VehicleAmmoOption> vehicleAmmunition = target.isLarge()
                ? nearbyVehicleAmmunition(player, target, remaining) : List.of();
        BattleNetwork.sendToPlayer(player, new OpenAmmoSupplyPacket(new AmmoSupplyView(target,
                capacity, remaining, guns,
                vehicleAmmunition)));
    }

    private static List<AmmoSupplyView.VehicleAmmoOption> nearbyVehicleAmmunition(
            ServerPlayer player, AmmoSupplyView.Target target, int remainingPoints) {
        StationOrigin origin = stationOrigin(player, target);
        if (origin == null) {
            return List.of();
        }
        if (origin.excludedEntity() != null) {
            disableNativeLargeStation(origin.excludedEntity());
        }
        AABB area = new AABB(origin.x() - VEHICLE_SUPPLY_RANGE,
                origin.y() - VEHICLE_SUPPLY_RANGE,
                origin.z() - VEHICLE_SUPPLY_RANGE,
                origin.x() + VEHICLE_SUPPLY_RANGE,
                origin.y() + VEHICLE_SUPPLY_RANGE,
                origin.z() + VEHICLE_SUPPLY_RANGE);
        Entity excluded = origin.excludedEntity() == null
                ? player : origin.excludedEntity();
        List<Entity> vehicles = player.level().getEntities(excluded, area,
                        entity -> SbwVehicleAmmoAdapter.isVehicle(entity)
                                && origin.distanceToSqr(entity)
                                <= VEHICLE_SUPPLY_RANGE_SQUARED)
                .stream().sorted((left, right) -> Double.compare(
                        origin.distanceToSqr(left), origin.distanceToSqr(right)))
                .limit(MAX_NEARBY_VEHICLES).toList();
        List<AmmoSupplyView.VehicleAmmoOption> options = new ArrayList<>();
        for (Entity vehicle : vehicles) {
            for (SbwVehicleAmmoAdapter.AmmoTarget ammunition
                    : SbwVehicleAmmoAdapter.ammunition(vehicle)) {
                int cost = AmmoPointCostPolicy.pointsPerRound(
                        ammunition.classificationKey());
                long pointsPerPackage = (long) cost * ammunition.roundsPerPackage();
                int affordablePackages = pointsPerPackage > remainingPoints ? 0
                        : (int) Math.min(Integer.MAX_VALUE,
                        remainingPoints / pointsPerPackage);
                int packages = Math.min(ammunition.packageCapacity(), affordablePackages);
                packages = Math.min(packages,
                        MAX_VEHICLE_REQUEST_ROUNDS / ammunition.roundsPerPackage());
                int maxRounds = packages * ammunition.roundsPerPackage();
                options.add(new AmmoSupplyView.VehicleAmmoOption(vehicle.getId(),
                        ammunition.weaponKey(), ammunition.consumerIndex(),
                        vehicle.getDisplayName(), ammunition.weaponName(),
                        ammunition.ammunitionName(), cost, ammunition.storedRounds(),
                        ammunition.roundsPerPackage(), maxRounds));
                if (options.size() >= MAX_VEHICLE_AMMO_OPTIONS) {
                    return List.copyOf(options);
                }
            }
        }
        return List.copyOf(options);
    }

    private static void addGunOption(ServerPlayer player, List<AmmoSupplyView.GunOption> guns,
                                     int slot, int remaining) {
        ItemStack gun = player.getInventory().getItem(slot);
        Optional<ResourceLocation> ammoId = TaczAmmoAdapter.ammunitionForGun(gun);
        if (ammoId.isEmpty()) {
            return;
        }
        ItemStack ammo = TaczAmmoAdapter.createAmmo(ammoId.get(), 1);
        if (ammo.isEmpty()) {
            return;
        }
        int current = countAmmo(player, ammoId.get());
        int reserveLimit = ammoReserveLimit(gun);
        int cost = AmmoPointCostPolicy.pointsPerRound(ammoId.get());
        int receivable = Math.min(Math.max(0,
                reserveLimit - current), remaining / cost);
        guns.add(new AmmoSupplyView.GunOption(slot, gun.getHoverName(), ammo.getHoverName(),
                ammoId.get().toString(), cost, current, reserveLimit, receivable));
    }

    private static void addInitialReserveRequirement(ServerPlayer player, int slot,
                                                     UUID sessionId, UUID issueToken,
                                                     Map<ResourceLocation, Integer> limits) {
        ItemStack gun = player.getInventory().getItem(slot);
        if (!KitProvenance.isValid(gun, sessionId, player.getUUID(), issueToken)) {
            return;
        }
        TaczAmmoAdapter.ammunitionForGun(gun).ifPresent(ammoId -> limits.merge(ammoId,
                ammoReserveLimit(gun), Math::max));
    }

    private static ResolvedSupply resolveTarget(ServerPlayer player,
                                                AmmoSupplyView.Target target) {
        if (target.kind() == AmmoSupplyView.TargetKind.SMALL_CRATE) {
            BlockPos pos = target.blockPos();
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED
                    || !player.level().getBlockState(pos).is(
                    InfantryBlocks.AMMO_SUPPLY_CRATE.get())
                    || !(player.level().getBlockEntity(pos)
                    instanceof AmmoSupplyCrateBlockEntity crate)) {
                return null;
            }
            return new ResolvedSupply(InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS,
                    crate::remainingPoints, crate::consumePoints);
        }
        if (target.kind() == AmmoSupplyView.TargetKind.MEDIUM_CRATE) {
            BlockPos pos = target.blockPos();
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED
                    || !player.level().getBlockState(pos).is(
                    InfantryBlocks.MEDIUM_AMMO_SUPPLY_CRATE.get())
                    || !(player.level().getBlockEntity(pos)
                    instanceof MediumAmmoSupplyCrateBlockEntity crate)) {
                return null;
            }
            return new ResolvedSupply(InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS,
                    crate::remainingPoints, crate::consumePoints);
        }
        if (target.kind() == AmmoSupplyView.TargetKind.LARGE_BLOCK) {
            BlockPos pos = target.blockPos();
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) > MAX_USE_DISTANCE_SQUARED
                    || !player.level().getBlockState(pos).is(
                    InfantryBlocks.LARGE_AMMO_SUPPLY_STATION.get())
                    || !(player.level().getBlockEntity(pos)
                    instanceof LargeAmmoSupplyStationBlockEntity station)) {
                return null;
            }
            return new ResolvedSupply(InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                    station::remainingPoints, station::consumePoints);
        }
        Entity entity = player.level().getEntity(target.entityId());
        if (!isLargeStation(entity) || player.distanceToSqr(entity) > MAX_USE_DISTANCE_SQUARED) {
            return null;
        }
        disableNativeLargeStation(entity);
        return new ResolvedSupply(InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                () -> largeStationRemainingPoints(entity), amount -> {
            int current = largeStationRemainingPoints(entity);
            int consumed = Math.min(Math.max(0, amount), current);
            entity.getPersistentData().putInt(LARGE_REMAINING_POINTS_TAG, current - consumed);
            return consumed;
        });
    }

    private static StationOrigin stationOrigin(ServerPlayer player,
                                               AmmoSupplyView.Target target) {
        if (target.kind() == AmmoSupplyView.TargetKind.LARGE_BLOCK) {
            BlockPos pos = target.blockPos();
            if (!player.level().getBlockState(pos).is(
                    InfantryBlocks.LARGE_AMMO_SUPPLY_STATION.get())
                    || !(player.level().getBlockEntity(pos)
                    instanceof LargeAmmoSupplyStationBlockEntity)) {
                return null;
            }
            return new StationOrigin(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, pos, null);
        }
        if (target.kind() != AmmoSupplyView.TargetKind.LARGE_STATION) {
            return null;
        }
        Entity station = player.level().getEntity(target.entityId());
        return isLargeStation(station)
                ? new StationOrigin(station.getX(), station.getY(), station.getZ(),
                station.blockPosition(), station) : null;
    }

    private static int largeStationRemainingPoints(Entity entity) {
        if (!entity.getPersistentData().contains(LARGE_REMAINING_POINTS_TAG)) {
            entity.getPersistentData().putInt(LARGE_REMAINING_POINTS_TAG,
                    InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS);
        }
        return Math.max(0, Math.min(InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                entity.getPersistentData().getInt(LARGE_REMAINING_POINTS_TAG)));
    }

    private static int countAmmo(ServerPlayer player, ResourceLocation ammoId) {
        int total = 0;
        for (int slot = 0; slot <= LAST_MAIN_INVENTORY_SLOT; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matchesAmmo(stack, ammoId)) {
                total += stack.getCount();
            }
        }
        ItemStack offhand = player.getInventory().getItem(40);
        return matchesAmmo(offhand, ammoId) ? total + offhand.getCount() : total;
    }

    private static int addAmmo(ServerPlayer player, ResourceLocation ammoId, int requested,
                               boolean active, UUID sessionId, UUID issueToken) {
        ItemStack template = TaczAmmoAdapter.createAmmo(ammoId, 1);
        if (template.isEmpty() || requested < 1) {
            return 0;
        }
        int remaining = requested;
        for (int slot = 0; slot <= LAST_MAIN_INVENTORY_SLOT && remaining > 0; slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (!matchesAmmo(existing, ammoId)) {
                continue;
            }
            int moved = Math.min(Math.max(0,
                    existing.getMaxStackSize() - existing.getCount()), remaining);
            existing.grow(moved);
            remaining -= moved;
        }
        for (int slot = active ? FIRST_SUPPLEMENT_SLOT : 0;
             slot <= LAST_MAIN_INVENTORY_SLOT && remaining > 0; slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) {
                continue;
            }
            ItemStack supplied = template.copy();
            int moved = Math.min(supplied.getMaxStackSize(), remaining);
            supplied.setCount(moved);
            if (active) {
                KitProvenance.stamp(supplied, sessionId, player.getUUID(), issueToken, slot);
            }
            player.getInventory().setItem(slot, supplied);
            remaining -= moved;
        }
        return requested - remaining;
    }

    private static boolean matchesAmmo(ItemStack stack, ResourceLocation ammoId) {
        return TaczAmmoAdapter.ammoId(stack).filter(ammoId::equals).isPresent();
    }

    private static boolean isGunSlot(int slot) {
        return slot >= 0 && slot <= LAST_MAIN_INVENTORY_SLOT || slot == 40;
    }

    public record InitialReserveGrant(int ammunitionTypes, int requestedRounds,
                                      int addedRounds) {
        public InitialReserveGrant {
            ammunitionTypes = Math.max(0, ammunitionTypes);
            requestedRounds = Math.max(0, requestedRounds);
            addedRounds = Math.max(0, Math.min(requestedRounds, addedRounds));
        }

        public static InitialReserveGrant none() {
            return new InitialReserveGrant(0, 0, 0);
        }

        public boolean complete() {
            return addedRounds == requestedRounds;
        }
    }

    private interface PointGetter { int get(); }
    private interface PointConsumer { int consume(int amount); }

    private record ResolvedSupply(int capacity, PointGetter getter, PointConsumer consumer) {
        int remaining() { return getter.get(); }
        int consume(int amount) { return consumer.consume(amount); }
    }

    private record StationOrigin(double x, double y, double z, BlockPos soundPos,
                                 Entity excludedEntity) {
        double distanceToSqr(Entity entity) {
            return entity == null ? Double.POSITIVE_INFINITY
                    : entity.distanceToSqr(x, y, z);
        }
    }
}
