package com.wok.bodyhealth.client;

import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.config.BodyHealthConfig;
import com.wok.bodyhealth.health.BodyHealthService;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = WokBodyHealthMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ClientForgeEvents {
    @SubscribeEvent
    public static void hideVanillaHearts(RenderGuiOverlayEvent.Pre event) {
        if (BodyHealthConfig.REPLACE_VANILLA_HEARTS.get()
                && ClientBodyHealthState.get() != null
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }

    /**
     * Player jumps are simulated on the client and only reported to the
     * server as positions, so the destroyed-leg penalty is applied here.
     * The integrated server posts the same event for its ServerPlayer copy,
     * which the LocalPlayer check skips.
     */
    @SubscribeEvent
    public static void limitJumpWithDestroyedLegs(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        int destroyedLegs = (ClientBodyHealthState.isDestroyed(BodyPart.LEFT_LEG) ? 1 : 0)
                + (ClientBodyHealthState.isDestroyed(BodyPart.RIGHT_LEG) ? 1 : 0);
        if (destroyedLegs == 0) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x,
                movement.y * BodyHealthService.jumpVelocityMultiplier(destroyedLegs),
                movement.z);
    }

    private ClientForgeEvents() {
    }
}
