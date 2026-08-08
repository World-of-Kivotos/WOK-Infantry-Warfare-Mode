package com.wok.trauma.compat.tacz;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.config.TraumaConfig;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.DistExecutor;

import java.util.Map;
import java.util.WeakHashMap;

public final class TaczCompat {
    private static final int GUNSHOT_BLEEDING_DURATION_TICKS = 3 * 60 * 20;
    private static final Map<LivingEntity, HitStamp> LAST_HITS = new WeakHashMap<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TaczCompat::onGunHurt);
        MinecraftForge.EVENT_BUS.addListener(TaczCompat::onBulletJoin);
        MinecraftForge.EVENT_BUS.addListener(TaczCompat::onPlayerTick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TaczClientCompat::register);
    }

    private static void onGunHurt(EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide() != LogicalSide.SERVER
                || !(event.getHurtEntity() instanceof LivingEntity target)
                || !target.isAlive()) {
            return;
        }

        Entity attacker = event.getAttacker();
        HitStamp previous = LAST_HITS.get(target);
        long gameTime = target.level().getGameTime();
        int attackerId = attacker == null ? -1 : attacker.getId();
        if (previous != null && previous.gameTime == gameTime && previous.attackerId == attackerId) {
            return;
        }
        LAST_HITS.put(target, new HitStamp(gameTime, attackerId));

        if (!target.hasEffect(ModEffects.ANALGESIA.get())) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.PAIN.get(), TraumaConfig.PAIN_DURATION_TICKS.get(),
                    0, false, true, true));
        }

        float roll = target.getRandom().nextFloat();
        float majorChance = TraumaConfig.MAJOR_BLEEDING_CHANCE.get().floatValue();
        float normalChance = TraumaConfig.BLEEDING_CHANCE.get().floatValue();
        if (roll < majorChance) {
            target.removeEffect(ModEffects.BLEEDING.get());
            target.addEffect(new MobEffectInstance(
                    ModEffects.MAJOR_BLEEDING.get(), GUNSHOT_BLEEDING_DURATION_TICKS,
                    0, false, true, true));
        } else if (roll < majorChance + normalChance
                && !target.hasEffect(ModEffects.MAJOR_BLEEDING.get())) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.BLEEDING.get(), GUNSHOT_BLEEDING_DURATION_TICKS,
                    0, false, true, true));
        }
    }

    private static void onBulletJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof EntityKineticBullet bullet)) {
            return;
        }

        Vec3 velocity = bullet.getDeltaMovement();
        double speedSquared = velocity.lengthSqr();
        if (!Double.isFinite(speedSquared) || speedSquared > 100_000_000.0D) {
            WokTraumaMod.LOGGER.warn(
                    "Discarded a TaCZ bullet with invalid velocity to prevent a world crash: {}",
                    velocity);
            event.setCanceled(true);
            return;
        }

        Entity owner = bullet.getOwner();
        if (!(owner instanceof LivingEntity shooter)
                || !shooter.hasEffect(ModEffects.TREMOR.get())
                || speedSquared < 1.0E-8D) {
            return;
        }

        double spreadMultiplier = TraumaConfig.TREMOR_SPREAD_MULTIPLIER.get();
        double jitter = Math.max(0.0D, spreadMultiplier - 1.0D) * 0.03D;
        Vec3 direction = velocity.normalize().add(
                bullet.getRandom().triangle(0.0D, jitter),
                bullet.getRandom().triangle(0.0D, jitter),
                bullet.getRandom().triangle(0.0D, jitter)).normalize();
        if (Double.isFinite(direction.x)
                && Double.isFinite(direction.y)
                && Double.isFinite(direction.z)) {
            bullet.setDeltaMovement(direction.scale(Math.sqrt(speedSquared)));
        }
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.CLIENT) {
            TaczWeaponPenalty.update(event.player);
        }
    }

    private record HitStamp(long gameTime, int attackerId) {
    }

    private TaczCompat() {
    }
}
