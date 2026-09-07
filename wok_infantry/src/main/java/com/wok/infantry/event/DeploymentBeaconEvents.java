package com.wok.infantry.event;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.registry.InfantryBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Protects administrator-owned map setup without weakening normal battle block rules. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeploymentBeaconEvents {
    private DeploymentBeaconEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        boolean fieldBeacon = event.getState().is(InfantryBlocks.DEPLOYMENT_BEACON.get());
        boolean vehicleDeployment = event.getState().is(
                InfantryBlocks.VEHICLE_DEPLOYMENT.get());
        if (!fieldBeacon && !vehicleDeployment) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            event.setCanceled(true);
            if (event.getPlayer() != null) {
                event.getPlayer().sendSystemMessage(Component.translatable(
                        vehicleDeployment
                                ? "message.wok_infantry.vehicle_deployment.break_denied"
                                : "message.wok_infantry.deployment_beacon.break_denied"));
            }
        }
    }
}
