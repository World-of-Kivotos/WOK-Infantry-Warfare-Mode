package com.wok.vehiclehealth.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.wok.vehiclehealth.WokVehicleHealthMod;
import com.wok.vehiclehealth.config.VehicleModuleConfig;
import com.wok.vehiclehealth.module.VehicleModuleMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(
        modid = WokVehicleHealthMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ClientVehicleModuleEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ClientOpticsInterference.tick();
            ClientTurretAuthority.tick();
        }
    }

    /**
     * Superb Warfare normally lets the camera follow the gunner's mouse target while the turret
     * catches up. A damaged ring is deliberately much slower, so that behavior makes the sight
     * point somewhere the physical barrel cannot. Apply this after the native camera handler and
     * derive the sight angles from the interpolated physical turret instead.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)
                || vehicle.getSeatIndex(player) != vehicle.getTurretControllerIndex()) {
            return;
        }
        float multiplier = VehicleModuleMath.rotationMultiplier(
                vehicle.getTurretHealth(), vehicle.getTurretMaxHealth(),
                VehicleModuleConfig.TURRET_DAMAGED_SPEED_MULTIPLIER.get().floatValue(),
                VehicleModuleConfig.TURRET_DESTROYED_SPEED_MULTIPLIER.get().floatValue());
        if (multiplier >= 0.999F
                || (!ClientEventHandler.zoomVehicle
                && !minecraft.options.getCameraType().isFirstPerson())) {
            return;
        }

        Vec3 direction = vehicle.getTurretVector((float) event.getPartialTick()).normalize();
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        event.setYaw(VehicleModuleMath.cameraYaw(direction.x, direction.z));
        event.setPitch(VehicleModuleMath.cameraPitch(direction.x, direction.y, direction.z));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientOpticsInterference.activeForLocalPlayer()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float alpha = ClientOpticsInterference.alpha();
        int veilAlpha = Math.min(210, Math.max(35, Math.round(alpha * 180.0F)));
        graphics.fill(0, 0, width, height, veilAlpha << 24 | 0x101615);

        long worldTick = Minecraft.getInstance().level == null
                ? 0L
                : Minecraft.getInstance().level.getGameTime();
        long seed = worldTick * 341873128712L
                + ClientOpticsInterference.remainingTicks() * 132897987541L;
        Random random = new Random(seed);
        int samples = Math.max(80, Math.min(360, width * height / 2200));
        for (int index = 0; index < samples; index++) {
            int x = random.nextInt(Math.max(1, width));
            int y = random.nextInt(Math.max(1, height));
            int noiseWidth = 1 + random.nextInt(Math.max(2, width / 36));
            int noiseHeight = 1 + random.nextInt(3);
            int gray = 72 + random.nextInt(176);
            int noiseAlpha = Math.min(230, 35 + Math.round(alpha * (70 + random.nextInt(120))));
            int color = noiseAlpha << 24 | gray << 16 | gray << 8 | gray;
            graphics.fill(x, y, Math.min(width, x + noiseWidth),
                    Math.min(height, y + noiseHeight), color);
        }

        int scanlineAlpha = Math.min(100, Math.round(alpha * 80.0F));
        for (int y = (int) (worldTick & 3L); y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, scanlineAlpha << 24 | 0xD5DDD7);
        }
    }

    private ClientVehicleModuleEvents() {
    }
}
