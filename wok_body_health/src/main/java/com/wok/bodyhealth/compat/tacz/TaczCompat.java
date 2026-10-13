package com.wok.bodyhealth.compat.tacz;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import com.tacz.guns.util.HitboxHelper;
import com.wok.bodyhealth.health.BodyPart;
import com.wok.bodyhealth.health.HitLocationResolver;
import com.wok.bodyhealth.health.PendingHitStore;
import com.wok.bodyhealth.prone.ProneHitService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.LogicalSide;

public final class TaczCompat {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TaczCompat::onGunHurt);
    }

    private static void onGunHurt(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER
                || !(event.getHurtEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity bulletEntity = event.getBullet();
        long gameTime = player.level().getGameTime();
        // A prone segment hit already knows its part; the 0.6 box below would misplace it.
        BodyPart segment = bulletEntity == null ? null
                : ProneHitService.findPart(player, bulletEntity, gameTime);
        if (segment != null) {
            boolean head = segment == BodyPart.HEAD;
            if (event.isHeadShot() != head) {
                // Keeps the flag consistent with the part; the headshot multiplier is untouched.
                event.setHeadshot(head);
            }
            PendingHitStore.record(
                    player, segment, gameTime, bulletEntity,
                    event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING),
                    event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING));
            return;
        }

        AABB taczHitbox;
        Vec3 impact;
        if (bulletEntity instanceof Projectile projectile) {
            // This is the same lag-compensated AABB and line clipping used by
            // TaCZ EntityUtil.getHitResult for the event being handled.
            taczHitbox = HitboxHelper.getFixedBoundingBox(player, projectile.getOwner());
            Vec3 rayStart = projectile.position();
            Vec3 rayEnd = rayStart.add(projectile.getDeltaMovement());
            impact = taczHitbox.clip(rayStart, rayEnd).orElse(rayStart);
        } else {
            // Defensive support for an API-compatible custom TaCZ bullet.
            taczHitbox = player.getBoundingBox();
            impact = bulletEntity.position();
        }

        BodyPart part = HitLocationResolver.fromHitbox(
                player, taczHitbox, impact, event.isHeadShot());
        PendingHitStore.record(
                player, part, gameTime, bulletEntity,
                event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING),
                event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING));
    }

    private TaczCompat() {
    }
}
