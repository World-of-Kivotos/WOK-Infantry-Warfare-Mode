package com.wok.infantry.event;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.battle.BattleRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Optional DragonRise station integration without linking its classes into WOK core. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmmoSupplyEvents {
    private AmmoSupplyEvents() {
    }

    @SubscribeEvent
    public static void onLargeStationJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && AmmoSupplyService.isLargeStation(event.getEntity())) {
            AmmoSupplyService.disableNativeLargeStation(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onStationInteract(PlayerInteractEvent.EntityInteract event) {
        if (!AmmoSupplyService.isLargeStation(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isShiftKeyDown()
                && player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            AmmoSupplyService.refillLargeStation(player, event.getTarget());
        } else {
            AmmoSupplyService.openLargeStation(player, event.getTarget());
        }
    }
}
