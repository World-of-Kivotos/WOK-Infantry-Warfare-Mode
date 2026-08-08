package com.wok.trauma.registry;

import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.item.HemostaticItem;
import com.wok.trauma.item.InjectorItem;
import com.wok.trauma.item.MedicalKitItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WokTraumaMod.MOD_ID);

    public static final RegistryObject<Item> CIVILIAN_MEDICAL_KIT = ITEMS.register(
            "civilian_medical_kit",
            () -> new MedicalKitItem(new Item.Properties().durability(30),
                    20, 1.0F, 0, 0, 0, 0));
    public static final RegistryObject<Item> IFAK = ITEMS.register(
            "ifak",
            () -> new MedicalKitItem(new Item.Properties().durability(80),
                    10, 1.0F, 5 * 20, 20, 0, 0));
    public static final RegistryObject<Item> AFAK = ITEMS.register(
            "afak",
            () -> new MedicalKitItem(new Item.Properties().durability(120),
                    10, 1.0F, 5 * 20, 20, 8 * 20, 35));
    public static final RegistryObject<Item> SALEWA_MEDICAL_KIT = ITEMS.register(
            "salewa_medical_kit",
            () -> new MedicalKitItem(new Item.Properties().durability(60),
                    10, 1.5F, 3 * 20, 30, 0, 0));
    public static final RegistryObject<Item> CIVILIAN_BANDAGE = ITEMS.register(
            "civilian_bandage",
            () -> HemostaticItem.forBleeding(
                    new Item.Properties().stacksTo(16), 2 * 20, 1));
    public static final RegistryObject<Item> MILITARY_BANDAGE = ITEMS.register(
            "military_bandage",
            () -> HemostaticItem.forBleeding(
                    new Item.Properties().durability(3), 1 * 20, 3));
    public static final RegistryObject<Item> CAT_TOURNIQUET = ITEMS.register(
            "cat_tourniquet",
            () -> HemostaticItem.forMajorBleeding(
                    new Item.Properties().stacksTo(16), 3 * 20, 1));
    public static final RegistryObject<Item> CALOK_B = ITEMS.register(
            "calok_b",
            () -> HemostaticItem.forMajorBleeding(
                    new Item.Properties().durability(3), 3 * 20, 3));
    public static final RegistryObject<Item> PROPITAL = ITEMS.register(
            "propital",
            () -> new InjectorItem(new Item.Properties().stacksTo(16),
                    240 * 20, 180 * 20, 3 * 20, ModEffects.PROPITAL_REGENERATION));
    public static final RegistryObject<Item> ETG_CHANGE = ITEMS.register(
            "etg_change",
            () -> new InjectorItem(new Item.Properties().stacksTo(16),
                    0, 60 * 20, 20, ModEffects.REGENERATION));
    public static final RegistryObject<Item> MORPHINE = ITEMS.register(
            "morphine",
            () -> new InjectorItem(new Item.Properties().stacksTo(16),
                    360 * 20, 0, 0, null));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
