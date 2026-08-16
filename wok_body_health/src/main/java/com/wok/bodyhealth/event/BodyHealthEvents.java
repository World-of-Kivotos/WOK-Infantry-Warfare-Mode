package com.wok.bodyhealth.event;

import com.wok.bodyhealth.health.BodyHealthData;
import com.wok.bodyhealth.health.BodyHealthService;
import com.wok.bodyhealth.health.BodyPartKillDamage;
import com.wok.bodyhealth.health.BodyPart;
import com.wok.bodyhealth.health.DamageOutcome;
import com.wok.bodyhealth.health.HitLocationResolver;
import com.wok.bodyhealth.health.PendingHitStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class BodyHealthEvents {
    private static final ThreadLocal<Boolean> BYPASS_BODY_HEALTH =
            ThreadLocal.withInitial(() -> false);

    public static void onLivingDamage(LivingDamageEvent event) {
        if (BYPASS_BODY_HEALTH.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= 0.0F
                || player.isCreative()
                || player.isSpectator()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        DamageSource source = event.getSource();
        DamageOutcome outcome;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            outcome = BodyHealthService.applyExplosion(player, event.getAmount());
        } else if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            outcome = BodyHealthService.applyFall(player, event.getAmount());
        } else if (isSystemic(source)) {
            outcome = BodyHealthService.applySystemic(player, event.getAmount());
        } else {
            BodyPart part = PendingHitStore.consume(
                    player, source, player.level().getGameTime());
            if (part == null) {
                part = resolveGenericPart(player, source);
            }
            outcome = BodyHealthService.applyLocalized(player, part, event.getAmount());
        }

        if (outcome.fatal()) {
            if (source.getEntity() instanceof ServerPlayer killer && killer != player) {
                event.setCanceled(true);
                hurtBypassingBodyHealth(player, BodyPartKillDamage.create(
                        player, killer, source, outcome.primaryPart(),
                        outcome.propagatedFatal()));
            } else {
                event.setAmount(Math.max(event.getAmount(), player.getHealth() + 1.0F));
            }
        } else {
            event.setCanceled(true);
        }
    }

    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            BodyHealthService.heal(player, event.getAmount());
            event.setCanceled(true);
        }
    }

    public static void onLivingUseTotem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BodyHealthService.restoreAfterTotem(player);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && !event.player.level().isClientSide()
                && event.player instanceof ServerPlayer player) {
            BodyHealthService.updatePenalties(player);
        }
    }

    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BodyHealthService.applyJumpPenalty(player);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            BodyHealthData.reset(event.getEntity());
        } else {
            BodyHealthData.copy(event.getOriginal(), event.getEntity());
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setHealth(player.getMaxHealth());
            BodyHealthService.sync(player);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BodyHealthService.sync(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BodyHealthService.sync(player);
        }
    }

    public static void hurtBypassingBodyHealth(ServerPlayer player, DamageSource source) {
        boolean previous = BYPASS_BODY_HEALTH.get();
        BYPASS_BODY_HEALTH.set(true);
        try {
            // Silent bleeding is intentionally not stopped by the vanilla
            // ten-tick hurt cooldown. Clearing it here also guarantees that a
            // fatal systemic tick reaches vanilla's totem/death pipeline.
            player.invulnerableTime = 0;
            player.hurt(source, Math.max(1_024.0F, player.getHealth() + 1.0F));
        } finally {
            BYPASS_BODY_HEALTH.set(previous);
        }
    }

    private static boolean isSystemic(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.FREEZE);
    }

    public static boolean isLocalizedDamage(DamageSource source) {
        return !source.is(DamageTypeTags.IS_EXPLOSION)
                && !source.is(DamageTypes.FALL)
                && !source.is(DamageTypes.FLY_INTO_WALL)
                && !isSystemic(source);
    }

    public static BodyPart resolveOrRecordArmorPart(ServerPlayer player, DamageSource source) {
        long gameTime = player.level().getGameTime();
        BodyPart part = PendingHitStore.peek(player, source, gameTime);
        if (part == null) {
            part = PendingHitStore.peekLatest(player, gameTime);
        }
        if (part == null) {
            part = resolveGenericPart(player, source);
            PendingHitStore.record(player, part, gameTime, source);
        }
        return part;
    }

    private static BodyPart resolveGenericPart(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.IN_WALL)) {
            return BodyPart.HEAD;
        }

        Entity direct = source.getDirectEntity();
        if (direct != null && direct != source.getEntity()) {
            return HitLocationResolver.fromImpact(player, direct.position(), false);
        }

        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition != null && source.getEntity() == null) {
            return HitLocationResolver.fromImpact(player, sourcePosition, false);
        }

        return player.getRandom().nextFloat() < 0.72F
                ? BodyPart.CHEST : BodyPart.ABDOMEN;
    }

    private BodyHealthEvents() {
    }
}
