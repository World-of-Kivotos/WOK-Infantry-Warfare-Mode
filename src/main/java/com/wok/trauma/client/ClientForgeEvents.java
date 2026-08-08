package com.wok.trauma.client;

import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.compat.bodyhealth.BodyHealthCompat;
import com.wok.trauma.item.MedicalKitItem;
import com.wok.trauma.item.TreatmentSelection;
import com.wok.trauma.network.TraumaNetwork;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = WokTraumaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static ConcussionTinnitusSound tinnitusSound;
    private static UUID selectionOwner;
    private static TreatmentSelection treatmentSelection = TreatmentSelection.AUTO;

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.isUsingItem()
                || !minecraft.player.isShiftKeyDown()
                || event.getScrollDelta() == 0.0D
                || !BodyHealthCompat.isAvailable()
                || !(minecraft.player.getMainHandItem().getItem() instanceof MedicalKitItem)
                && !(minecraft.player.getOffhandItem().getItem() instanceof MedicalKitItem)) {
            return;
        }

        UUID playerId = minecraft.player.getUUID();
        if (!playerId.equals(selectionOwner)) {
            selectionOwner = playerId;
            treatmentSelection = TreatmentSelection.AUTO;
        }

        int direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        treatmentSelection = treatmentSelection.cycle(direction);
        TraumaNetwork.sendToServer(treatmentSelection);
        minecraft.player.displayClientMessage(
                Component.translatable(
                        "message.wok_trauma.treatment_selection",
                        Component.translatable(treatmentSelection.translationKey())),
                true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ModList.get().isLoaded("tacz")) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            TremorHandShake.apply(event.getPoseStack(), minecraft.player, event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || event.getCamera().getEntity() != minecraft.player) {
            return;
        }

        MobEffectInstance concussion =
                minecraft.player.getEffect(ModEffects.CONCUSSION.get());
        if (concussion == null) {
            return;
        }

        float time = minecraft.player.tickCount + (float) event.getPartialTick();
        float fadeOut = Mth.clamp(concussion.getDuration() / 40.0F, 0.0F, 1.0F);
        float accessibilityScale =
                minecraft.options.screenEffectScale().get().floatValue();
        float combinedEffectScale =
                minecraft.player.hasEffect(ModEffects.TREMOR.get()) ? 0.65F : 1.0F;
        float strength = fadeOut * accessibilityScale * combinedEffectScale;

        event.setPitch(event.getPitch()
                + Mth.sin(time * 0.115F) * 0.12F * strength);
        event.setRoll(event.getRoll()
                + Mth.sin(time * 0.083F + 1.2F) * 0.45F * strength);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            selectionOwner = null;
            treatmentSelection = TreatmentSelection.AUTO;
        }
        boolean concussed = minecraft.player != null
                && minecraft.player.hasEffect(ModEffects.CONCUSSION.get());
        if (concussed && (tinnitusSound == null || tinnitusSound.isStopped())) {
            tinnitusSound = new ConcussionTinnitusSound();
            minecraft.getSoundManager().play(tinnitusSound);
        }
    }

    private ClientForgeEvents() {
    }
}
