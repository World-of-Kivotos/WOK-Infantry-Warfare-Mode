package com.wok.trauma.registry;

import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.effect.BleedingEffect;
import com.wok.trauma.effect.SimpleTraumaEffect;
import com.wok.trauma.effect.TimedRegenerationEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, WokTraumaMod.MOD_ID);

    public static final RegistryObject<MobEffect> ANALGESIA = EFFECTS.register(
            "analgesia", () -> new SimpleTraumaEffect(MobEffectCategory.BENEFICIAL, 0x36C7D9));
    public static final RegistryObject<MobEffect> REGENERATION = EFFECTS.register(
            "regeneration", () -> new TimedRegenerationEffect(0x20D766, 20, 1.0F));
    public static final RegistryObject<MobEffect> PROPITAL_REGENERATION = EFFECTS.register(
            "propital_regeneration", () -> new TimedRegenerationEffect(0xA5C43B, 60, 1.0F));
    public static final RegistryObject<MobEffect> PAIN = EFFECTS.register(
            "pain", () -> new SimpleTraumaEffect(MobEffectCategory.HARMFUL, 0x8E4054));
    public static final RegistryObject<MobEffect> BLEEDING = EFFECTS.register(
            "bleeding", () -> new BleedingEffect(1.0F, false, 0xB21F2D));
    public static final RegistryObject<MobEffect> MAJOR_BLEEDING = EFFECTS.register(
            "major_bleeding", () -> new BleedingEffect(2.0F, true, 0x65080D));
    public static final RegistryObject<MobEffect> TREMOR = EFFECTS.register(
            "tremor", () -> new SimpleTraumaEffect(MobEffectCategory.HARMFUL, 0x7555A8));
    public static final RegistryObject<MobEffect> CONCUSSION = EFFECTS.register(
            "concussion", () -> new SimpleTraumaEffect(MobEffectCategory.HARMFUL, 0xD6A93A));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private ModEffects() {
    }
}
