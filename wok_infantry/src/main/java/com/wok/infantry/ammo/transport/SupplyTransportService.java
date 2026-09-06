package com.wok.infantry.ammo.transport;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.server.FormationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** Server authority for finite physical supply cargo carried by configured vehicles. */
public final class SupplyTransportService {
    private static final String CARGO_ROOT_TAG = "wok_infantry_supply_transport";
    private static final String VERSION_TAG = "Version";
    private static final String CARGO_TYPE_TAG = "CargoType";
    private static final String CAPACITY_TAG = "Capacity";
    private static final String REMAINING_TAG = "Remaining";
    private static final int LEGACY_LARGE_CARGO_VERSION = 1;
    private static final int CARGO_VERSION = 2;
    private static final UUID CARRY_SPEED_MODIFIER_ID =
            UUID.fromString("8d96cc43-d7e4-48f5-8c67-00e4f76c24d1");
    private static final String CARRY_SPEED_MODIFIER_NAME =
            "wok_infantry.large_ammo_crate_carry_speed";

    private SupplyTransportService() {
    }

    public static boolean isConfiguredVehicle(Entity entity) {
        return configuredProfile(entity) != null;
    }

    public static void initializeVehicle(Entity entity) {
        SupplyTransportProfile profile = configuredProfile(entity);
        if (profile != null) {
            readAndReconcile(entity, profile);
        }
    }

    public static void takeSupplyCrate(ServerPlayer player, Entity vehicle) {
        SupplyTransportProfile profile = configuredProfile(vehicle);
        if (player == null || vehicle == null || profile == null || !player.isAlive()
                || player.isSpectator()) {
            return;
        }
        if (!isOperational(vehicle)) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.vehicle_unavailable"));
            return;
        }
        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        boolean active = deployment != null && deployment.isActive(player.getUUID())
                && !deployment.isVehicleTestMode(player.getUUID());
        if (!active && !player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.not_deployed"));
            return;
        }
        if (active) {
            ActionResult authorization = FormationService.get(player)
                    .map(service -> service.authorizeMount(player, vehicle))
                    .orElseGet(() -> ActionResult.failure(
                            ActionResult.Code.FORMATION_UNAVAILABLE,
                            "阵营编制服务尚未就绪"));
            if (!authorization.success()) {
                player.sendSystemMessage(Component.translatable(
                        "message.wok_infantry.supply_transport.unauthorized",
                        authorization.message()));
                return;
            }
        }

        Item crateItem = ForgeRegistries.ITEMS.getValue(profile.cargoType().itemId());
        if (crateItem == null || crateItem == Items.AIR
                || !ForgeRegistries.ITEMS.containsKey(profile.cargoType().itemId())) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.cargo_unavailable",
                    Component.translatable(profile.cargoType().translationKey())));
            return;
        }
        ItemStack extracted = new ItemStack(crateItem);
        Component cargoName = extracted.getHoverName();
        SupplyCargoState cargo = readAndReconcile(vehicle, profile);
        if (cargo.empty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.empty",
                    vehicle.getDisplayName(), cargoName, cargo.capacity()));
            return;
        }

        if (active) {
            UUID issueToken = deployment.activeIssueToken(player).orElse(null);
            if (issueToken == null) {
                player.sendSystemMessage(Component.translatable(
                        "message.wok_infantry.ammo_supply.internal_error"));
                return;
            }
            KitProvenance.stampTransportCargo(extracted, deployment.sessionId(),
                    player.getUUID(), issueToken);
        }
        if (!player.getInventory().add(extracted) || !extracted.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.inventory_full", cargoName));
            return;
        }

        SupplyCargoState after = cargo.takeOne();
        write(vehicle, profile.cargoType(), after);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, vehicle.blockPosition(), SoundEvents.BARREL_OPEN,
                SoundSource.PLAYERS, 0.9F, 0.8F);
        if (profile.cargoType() == SupplyCargoType.LARGE) {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.taken_heavy",
                    vehicle.getDisplayName(), cargoName, after.remaining(), after.capacity(),
                    Math.round(InfantryServerConfig.largeAmmoCrateCarrySpeedMultiplier()
                            * 100.0D)));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.supply_transport.taken",
                    vehicle.getDisplayName(), cargoName,
                    after.remaining(), after.capacity()));
        }
    }

    public static void updateCarrySpeed(ServerPlayer player) {
        if (player == null) {
            return;
        }
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        boolean carrying = carriesLargeAmmoCrate(player);
        AttributeModifier current = movement.getModifier(CARRY_SPEED_MODIFIER_ID);
        if (!carrying) {
            if (current != null) {
                movement.removeModifier(CARRY_SPEED_MODIFIER_ID);
            }
            return;
        }

        double amount = InfantryServerConfig.largeAmmoCrateCarrySpeedMultiplier() - 1.0D;
        if (current == null || Double.compare(current.getAmount(), amount) != 0) {
            if (current != null) {
                movement.removeModifier(CARRY_SPEED_MODIFIER_ID);
            }
            movement.addTransientModifier(new AttributeModifier(CARRY_SPEED_MODIFIER_ID,
                    CARRY_SPEED_MODIFIER_NAME, amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        player.setSprinting(false);
    }

    public static boolean isLargeAmmoCrate(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && SupplyCargoType.LARGE.itemId().equals(
                        ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static boolean carriesLargeAmmoCrate(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isLargeAmmoCrate(player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static SupplyTransportProfile configuredProfile(Entity entity) {
        ResourceLocation entityId = entity == null ? null
                : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return entityId == null ? null : InfantryServerConfig.supplyTransportProfile(entityId);
    }

    /** SBW 0.8.9 keeps destroyed vehicles as same-type wreck entities for a while. */
    private static boolean isOperational(Entity vehicle) {
        if (vehicle.isRemoved() || !vehicle.isAlive()) {
            return false;
        }
        try {
            Object wreck = vehicle.getClass().getMethod("isWreck").invoke(vehicle);
            return !(wreck instanceof Boolean value) || !value;
        } catch (NoSuchMethodException exception) {
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static SupplyCargoState readAndReconcile(Entity vehicle,
                                                     SupplyTransportProfile profile) {
        CompoundTag persistent = vehicle.getPersistentData();
        SupplyCargoState state;
        if (!persistent.contains(CARGO_ROOT_TAG, Tag.TAG_COMPOUND)) {
            state = SupplyCargoState.full(profile.capacity());
        } else {
            CompoundTag cargo = persistent.getCompound(CARGO_ROOT_TAG);
            int version = cargo.getInt(VERSION_TAG);
            boolean typedCargo = version == CARGO_VERSION
                    && cargo.contains(CARGO_TYPE_TAG, Tag.TAG_STRING)
                    && profile.cargoType().id().equals(cargo.getString(CARGO_TYPE_TAG));
            boolean legacyLargeCargo = version == LEGACY_LARGE_CARGO_VERSION
                    && profile.cargoType() == SupplyCargoType.LARGE;
            boolean valid = (typedCargo || legacyLargeCargo)
                    && cargo.contains(REMAINING_TAG, Tag.TAG_INT);
            state = valid ? new SupplyCargoState(
                    profile.capacity(), cargo.getInt(REMAINING_TAG))
                    : new SupplyCargoState(profile.capacity(), 0);
        }
        write(vehicle, profile.cargoType(), state);
        return state;
    }

    private static void write(Entity vehicle, SupplyCargoType cargoType,
                              SupplyCargoState state) {
        CompoundTag cargo = new CompoundTag();
        cargo.putInt(VERSION_TAG, CARGO_VERSION);
        cargo.putString(CARGO_TYPE_TAG, cargoType.id());
        cargo.putInt(CAPACITY_TAG, state.capacity());
        cargo.putInt(REMAINING_TAG, state.remaining());
        vehicle.getPersistentData().put(CARGO_ROOT_TAG, cargo);
    }
}
