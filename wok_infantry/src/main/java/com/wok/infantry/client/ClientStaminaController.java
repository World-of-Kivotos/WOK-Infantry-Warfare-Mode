package com.wok.infantry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wok.infantry.integration.tacz.TaczStaminaAdapter;
import com.wok.infantry.stamina.StaminaMath;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Applies visible fatigue motion to both the camera and TaCZ's actual hand-render pose. */
public final class ClientStaminaController {
    private static final float CAMERA_MAX_YAW = 0.55F;
    private static final float CAMERA_MAX_PITCH = 0.42F;
    private static final float GUN_MAX_YAW = 1.80F;
    private static final float GUN_MAX_PITCH = 1.30F;
    private static final float GUN_MAX_ROLL = 0.48F;

    private ClientStaminaController() {
    }

    public static void register() {
        // Run before TaCZ consumes RenderHandEvent and renders its custom gun model.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                ClientStaminaController::onRenderHand);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW,
                ClientStaminaController::onCameraAngles);
    }

    private static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        SwaySample sway = sample(event.getPartialTick());
        if (sway == null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(sway.yawWave() * GUN_MAX_YAW * sway.intensity()));
        pose.mulPose(Axis.XP.rotationDegrees(sway.pitchWave() * GUN_MAX_PITCH * sway.intensity()));
        pose.mulPose(Axis.ZP.rotationDegrees(sway.rollWave() * GUN_MAX_ROLL * sway.intensity()));
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        SwaySample sway = sample((float) event.getPartialTick());
        if (sway == null) {
            return;
        }
        event.setYaw(event.getYaw()
                + sway.yawWave() * CAMERA_MAX_YAW * sway.intensity());
        event.setPitch(event.getPitch()
                + sway.pitchWave() * CAMERA_MAX_PITCH * sway.intensity());
    }

    private static SwaySample sample(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        StaminaSnapshot snapshot = ClientStaminaState.snapshot();
        if (player == null || !snapshot.enabled() || !TaczStaminaAdapter.isHoldingGun(player)) {
            return null;
        }
        float intensity = StaminaMath.swayIntensity(snapshot.arms(), snapshot.legs());
        if (intensity <= 0.0F) {
            return null;
        }
        // ADS exposes the complete motion; hip-fire retains a clearly perceptible 55%.
        intensity *= TaczStaminaAdapter.isAiming(player) ? 1.0F : 0.55F;
        double time = player.tickCount + partialTick;
        float yawWave = (float) (Math.sin(time * 0.155D)
                + Math.sin(time * 0.071D + 1.35D) * 0.38D) / 1.38F;
        float pitchWave = (float) (Math.sin(time * 0.119D + 0.72D)
                + Math.sin(time * 0.047D + 2.10D) * 0.32D) / 1.32F;
        float rollWave = (float) Math.sin(time * 0.083D + 0.35D);
        return new SwaySample(intensity, yawWave, pitchWave, rollWave);
    }

    public static void reset() {
        // Render-event transforms are frame-local and need no persistent cleanup.
    }

    private record SwaySample(float intensity, float yawWave,
                              float pitchWave, float rollWave) {
    }
}
