package com.wok.infantry.registry;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.block.entity.AmmoSupplyCrateBlockEntity;
import com.wok.infantry.block.entity.LargeAmmoSupplyStationBlockEntity;
import com.wok.infantry.block.entity.MediumAmmoSupplyCrateBlockEntity;
import com.wok.infantry.block.entity.RallyRadioBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class InfantryBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WokInfantryMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<AmmoSupplyCrateBlockEntity>>
            AMMO_SUPPLY_CRATE = BLOCK_ENTITIES.register("ammo_supply_crate",
            () -> BlockEntityType.Builder.of(AmmoSupplyCrateBlockEntity::new,
                    InfantryBlocks.AMMO_SUPPLY_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MediumAmmoSupplyCrateBlockEntity>>
            MEDIUM_AMMO_SUPPLY_CRATE = BLOCK_ENTITIES.register("medium_ammo_supply_crate",
            () -> BlockEntityType.Builder.of(MediumAmmoSupplyCrateBlockEntity::new,
                    InfantryBlocks.MEDIUM_AMMO_SUPPLY_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<LargeAmmoSupplyStationBlockEntity>>
            LARGE_AMMO_SUPPLY_STATION = BLOCK_ENTITIES.register(
            "large_ammo_supply_station",
            () -> BlockEntityType.Builder.of(LargeAmmoSupplyStationBlockEntity::new,
                    InfantryBlocks.LARGE_AMMO_SUPPLY_STATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<RallyRadioBlockEntity>> RALLY_RADIO =
            BLOCK_ENTITIES.register("rally_radio",
                    () -> BlockEntityType.Builder.of(RallyRadioBlockEntity::new,
                            InfantryBlocks.RALLY_RADIO.get()).build(null));

    private InfantryBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
