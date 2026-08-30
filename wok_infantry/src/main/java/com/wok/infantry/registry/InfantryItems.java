package com.wok.infantry.registry;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.deployment.DeploymentService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class InfantryItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WokInfantryMod.MOD_ID);

    public static final RegistryObject<Item> DEPLOYMENT_BEACON = ITEMS.register(
            "deployment_beacon",
            () -> new OperatorBlockItem(InfantryBlocks.DEPLOYMENT_BEACON.get(),
                    new Item.Properties(),
                    "message.wok_infantry.deployment_beacon.place_denied",
                    "message.wok_infantry.deployment_beacon.internal_denied"));

    public static final RegistryObject<Item> VEHICLE_DEPLOYMENT = ITEMS.register(
            "vehicle_deployment",
            () -> new OperatorBlockItem(InfantryBlocks.VEHICLE_DEPLOYMENT.get(),
                    new Item.Properties(),
                    "message.wok_infantry.vehicle_deployment.place_denied",
                    "message.wok_infantry.vehicle_deployment.internal_denied"));

    public static final RegistryObject<Item> AMMO_SUPPLY_CRATE = ITEMS.register(
            "ammo_supply_crate",
            () -> new BlockItem(InfantryBlocks.AMMO_SUPPLY_CRATE.get(),
                    new Item.Properties().stacksTo(1)));

    private InfantryItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** Block item whose placement is reserved for permission-level-2 administrators. */
    private static final class OperatorBlockItem extends BlockItem {
        private final String placeDeniedKey;
        private final String internalDeniedKey;

        private OperatorBlockItem(Block block, Properties properties,
                                  String placeDeniedKey, String internalDeniedKey) {
            super(block, properties);
            this.placeDeniedKey = placeDeniedKey;
            this.internalDeniedKey = internalDeniedKey;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!context.getLevel().isClientSide) {
                if (!(context.getPlayer() instanceof ServerPlayer player)
                        || !player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().sendSystemMessage(Component.translatable(
                                placeDeniedKey));
                    }
                    return InteractionResult.FAIL;
                }
                if (player.serverLevel().dimension().equals(DeploymentService.HOLDING_LEVEL)
                        || player.serverLevel().dimension().equals(
                        DeploymentService.LOBBY_LEVEL)) {
                    player.sendSystemMessage(Component.translatable(
                            internalDeniedKey));
                    return InteractionResult.FAIL;
                }
            }
            return super.useOn(context);
        }
    }
}
