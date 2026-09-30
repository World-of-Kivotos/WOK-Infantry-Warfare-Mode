package com.wok.bodyhealth.event;

import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.health.BodyDamageTags;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

public final class BodyHealthEvents {
    private static final ThreadLocal<Boolean> BYPASS_BODY_HEALTH =
            ThreadLocal.withInitial(() -> false);
    /** The player currently receiving a finishing blow, and whether a death event followed. */
    private static final ThreadLocal<ServerPlayer> FINISHING_TARGET = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> FINISHING_DEATH_SEEN =
            ThreadLocal.withInitial(() -> false);
    /** Melee aim rays are compared against a slightly larger box to absorb rotation lag. */
    private static final double MELEE_AIM_TOLERANCE = 0.1D;

    public static void onLivingDamage(LivingDamageEvent event) {
        if (BYPASS_BODY_HEALTH.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= 0.0F
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        DamageSource source = event.getSource();
        DamageOutcome outcome;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // Void and /kill damage. Vanilla health is refilled every tick, so
            // it must go through the body parts or the void would never kill.
            outcome = BodyHealthService.applySystemic(player, event.getAmount());
        } else if (source.is(BodyDamageTags.EXPLOSION_SPREAD)) {
            outcome = BodyHealthService.applyExplosion(player, event.getAmount());
        } else if (source.is(BodyDamageTags.LEG_SPREAD)) {
            outcome = BodyHealthService.applyFall(player, event.getAmount());
        } else if (source.is(BodyDamageTags.SYSTEMIC_SPREAD)) {
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

    /** Registered with receiveCanceled so that a death turned into "downed" still counts. */
    public static void onLivingDeathObserved(LivingDeathEvent event) {
        if (event.getEntity() == FINISHING_TARGET.get()) {
            FINISHING_DEATH_SEEN.set(true);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && !event.player.level().isClientSide()
                && event.player instanceof ServerPlayer player) {
            BodyHealthService.updatePenalties(player);
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
            // A player who left on the death screen is saved with zero health;
            // refilling it here would revive them in place and skip respawn.
            if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
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
        boolean previousBypass = BYPASS_BODY_HEALTH.get();
        ServerPlayer previousTarget = FINISHING_TARGET.get();
        boolean previousDeathSeen = FINISHING_DEATH_SEEN.get();
        BYPASS_BODY_HEALTH.set(true);
        FINISHING_TARGET.set(player);
        FINISHING_DEATH_SEEN.set(false);
        try {
            // Silent bleeding is intentionally not stopped by the vanilla
            // ten-tick hurt cooldown. Clearing it here also guarantees that a
            // fatal systemic tick reaches vanilla's totem/death pipeline.
            player.invulnerableTime = 0;
            player.hurt(source, Math.max(1_024.0F, player.getHealth() + 1.0F));
            if (player.isAlive() && !FINISHING_DEATH_SEEN.get()
                    && BodyHealthService.hasDestroyedCriticalPart(player)) {
                // Another mod cancelled or absorbed the finishing blow. A
                // destroyed head or chest must not leave the player alive.
                forceDeath(player, source);
            }
        } finally {
            BYPASS_BODY_HEALTH.set(previousBypass);
            FINISHING_TARGET.set(previousTarget);
            FINISHING_DEATH_SEEN.set(previousDeathSeen);
        }
    }

    private static void forceDeath(ServerPlayer player, DamageSource source) {
        if (TotemHook.tryUse(player, source)) {
            return;
        }
        player.getCombatTracker().recordDamage(source, player.getHealth());
        player.setHealth(0.0F);
        player.die(source);
    }

    public static boolean isLocalizedDamage(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(BodyDamageTags.EXPLOSION_SPREAD)
                && !source.is(BodyDamageTags.LEG_SPREAD)
                && !source.is(BodyDamageTags.SYSTEMIC_SPREAD);
    }

    public static BodyPart resolveOrRecordArmorPart(ServerPlayer player, DamageSource source) {
        long gameTime = player.level().getGameTime();
        BodyPart part = PendingHitStore.peek(player, source, gameTime);
        if (part == null) {
            part = resolveGenericPart(player, source);
            PendingHitStore.record(player, part, gameTime, source.getDirectEntity(), source);
        }
        return part;
    }

    private static BodyPart resolveGenericPart(ServerPlayer player, DamageSource source) {
        if (source.is(BodyDamageTags.HEAD_HIT)) {
            return BodyPart.HEAD;
        }

        AABB bounds = player.getBoundingBox();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (direct != null && direct != attacker) {
            Vec3 impact = HitLocationResolver.traceImpact(
                    bounds, direct.position(), direct.getDeltaMovement());
            return HitLocationResolver.fromHitbox(player, bounds, impact, false);
        }

        if (direct instanceof Player playerAttacker && direct != player) {
            BodyPart aimed = aimedMeleePart(player, playerAttacker, bounds);
            if (aimed != null) {
                return aimed;
            }
        }

        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition != null && attacker == null) {
            return HitLocationResolver.fromHitbox(player, bounds,
                    HitLocationResolver.closestPoint(bounds, sourcePosition), false);
        }

        // Mobs look at their target's eyes, so their view ray would always
        // hit the head; their melee keeps the torso-weighted roll.
        return player.getRandom().nextFloat() < 0.72F
                ? BodyPart.CHEST : BodyPart.ABDOMEN;
    }

    /** Player melee lands where the attacker's crosshair enters the victim. */
    private static BodyPart aimedMeleePart(ServerPlayer victim, Player attacker, AABB bounds) {
        Vec3 eye = attacker.getEyePosition();
        double reach = eye.distanceTo(bounds.getCenter()) + 2.0D;
        Vec3 end = eye.add(attacker.getViewVector(1.0F).scale(reach));
        return bounds.inflate(MELEE_AIM_TOLERANCE).clip(eye, end)
                .map(point -> HitLocationResolver.fromHitbox(victim, bounds,
                        HitLocationResolver.closestPoint(bounds, point), false))
                .orElse(null);
    }

    /** Vanilla's private totem check, so a forced death still honours a held totem. */
    private static final class TotemHook {
        private static final Method CHECK_TOTEM = find();

        static boolean tryUse(LivingEntity entity, DamageSource source) {
            if (CHECK_TOTEM == null) {
                return false;
            }
            try {
                return Boolean.TRUE.equals(CHECK_TOTEM.invoke(entity, source));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                WokBodyHealthMod.LOGGER.error("Totem check for a forced body-health death failed", exception);
                return false;
            }
        }

        private static Method find() {
            try {
                return ObfuscationReflectionHelper.findMethod(
                        LivingEntity.class, "m_21262_", DamageSource.class);
            } catch (RuntimeException exception) {
                WokBodyHealthMod.LOGGER.error(
                        "Vanilla totem check is unavailable; forced body-health deaths ignore totems",
                        exception);
                return null;
            }
        }
    }

    private BodyHealthEvents() {
    }
}
