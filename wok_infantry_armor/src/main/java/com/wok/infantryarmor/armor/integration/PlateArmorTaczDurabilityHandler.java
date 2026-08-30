package com.wok.infantryarmor.armor.integration;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** TaCZ 一弹一次的插板磨损入口；Post 与 Kill 互斥，不按普通段/穿甲段重复扣耐久。 */
public final class PlateArmorTaczDurabilityHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBeforeHurtByGun(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide().isServer() && event.getHurtEntity() instanceof Player player) {
            double referenceDamage = event.getBaseAmount();
            if (event.isHeadShot()) {
                referenceDamage *= Math.max(0.0F, event.getHeadshotMultiplier());
            }
            PlateArmorTaczWearLedger.capture(
                    event.getBullet().getUUID(),
                    player,
                    event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING),
                    referenceDamage);
        }
    }

    @SubscribeEvent
    public void onHurtByGun(EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide().isServer() && event.getHurtEntity() instanceof Player player) {
            PlateArmorTaczWearLedger.settle(event.getBullet().getUUID(), player, event.getBaseAmount());
        }
    }

    @SubscribeEvent
    public void onKilledByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide().isServer() && event.getKilledEntity() instanceof Player player) {
            PlateArmorTaczWearLedger.settle(event.getBullet().getUUID(), player, event.getBaseDamage());
        }
    }
}

