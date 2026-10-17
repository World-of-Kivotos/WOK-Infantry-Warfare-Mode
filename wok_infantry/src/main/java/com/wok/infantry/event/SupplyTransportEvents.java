package com.wok.infantry.event;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.ammo.transport.SupplyTransportService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge hooks for configured vehicle cargo extraction and heavy-crate movement. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SupplyTransportEvents {
    private SupplyTransportEvents() {
    }

    @SubscribeEvent
    public static void onVehicleJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            SupplyTransportService.initializeVehicle(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVehicleInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getEntity().isShiftKeyDown()
                || !event.getItemStack().isEmpty()
                || !SupplyTransportService.isConfiguredVehicle(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof ServerPlayer player) {
            SupplyTransportService.takeSupplyCrate(player, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            SupplyTransportService.updateCarrySpeed(player);
        }
    }
}
