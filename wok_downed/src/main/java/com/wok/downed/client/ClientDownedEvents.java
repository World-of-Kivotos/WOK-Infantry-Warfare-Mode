package com.wok.downed.client;

import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedPose;
import com.wok.downed.state.DownedService;
import com.wok.downed.state.DownedWeaponPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WokDownedMod.MOD_ID, value = Dist.CLIENT)
public final class ClientDownedEvents {
    private static LocalPlayer lockedPlayer;

    public static boolean isLocalPlayerDowned() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && DownedService.isDowned(player);
    }

    /**
     * Whether the local casualty is holding a sidearm it is allowed to fire, so the client must
     * let the attack input through. When this is false the casualty stays fully frozen.
     */
    public static boolean mayLocalPlayerFire() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        return DownedService.isDowned(player)
                && DownedConfig.ALLOW_SIDEARM_FIRE.get()
                && DownedWeaponPolicy.isAllowedSidearm(held, DownedConfig.ALLOWED_SIDEARMS.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof LocalPlayer player)) {
            return;
        }
        if (DownedService.isDowned(player)) {
            lockedPlayer = player;
            DownedPose.lock(player);
            suppressInput(player.input);
            double vertical = player.onGround() ? 0.0D : Math.min(0.0D, player.getDeltaMovement().y);
            player.setDeltaMovement(0.0D, vertical, 0.0D);
        } else if (lockedPlayer == player) {
            // Only release a pose we owned; leave a new pose from another system alone.
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
            }
            lockedPlayer = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (DownedService.isDowned(event.getEntity())) {
            suppressInput(event.getInput());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isLocalPlayerDowned()) {
            return;
        }
        // Attack is the only input a casualty keeps, and only while a listed sidearm is in hand.
        if (event.isAttack() && mayLocalPlayerFire()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        lockedPlayer = null;
        DownedTraumaEffects.reset();
    }

    private static void suppressInput(Input input) {
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        // 1.20.1's Input carries movement only; firing and aiming arrive through
        // InputEvent.InteractionKeyMappingTriggered, which onInteraction below lets through for a
        // listed sidearm. Nothing here needs to preserve a "use" flag.
    }

    private ClientDownedEvents() {
    }
}
