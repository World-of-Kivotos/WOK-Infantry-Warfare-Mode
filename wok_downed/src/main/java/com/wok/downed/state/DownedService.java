package com.wok.downed.state;

import com.wok.downed.config.DownedConfig;
import com.wok.downed.compat.MedicalCompat;
import com.wok.downed.registry.DownedEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class DownedService {
    private static final String ACTIVE_KEY = "wok_downed.active";
    private static final String START_TIME_KEY = "wok_downed.start_time";
    private static final String STABILIZE_PENDING_KEY = "wok_downed.stabilize_pending";
    private static final Map<UUID, RescueSession> RESCUES = new HashMap<>();
    private static final Map<UUID, DragSession> DRAGS = new HashMap<>();
    private static final Map<UUID, DamageSource> PENDING_DEATHS = new HashMap<>();
    private static final UUID DRAG_SPEED_MODIFIER_ID = UUID.fromString(
            "6f2cc8d8-371c-44b7-97f6-f76a6920c075");
    private static final ThreadLocal<Boolean> FORCING_DEATH =
            ThreadLocal.withInitial(() -> false);

    public static boolean isDowned(Player player) {
        return player.getPersistentData().getBoolean(ACTIVE_KEY)
                || player.hasEffect(DownedEffects.DOWNED.get());
    }

    public static boolean isForcingDeath() {
        return FORCING_DEATH.get();
    }

    public static void enterDowned(ServerPlayer player) {
        cancelByParticipant(player, false);
        stopDraggingByParticipant(player, false);
        PENDING_DEATHS.remove(player.getUUID());
        player.getPersistentData().putBoolean(ACTIVE_KEY, true);
        player.getPersistentData().putLong(
                START_TIME_KEY, player.level().getGameTime());
        player.getPersistentData().putBoolean(STABILIZE_PENDING_KEY, true);
        player.setHealth(Math.min(player.getMaxHealth(), 1.0F));
        player.invulnerableTime = 0;
        player.stopUsingItem();
        player.stopRiding();
        DownedPose.lock(player);
        player.addEffect(new MobEffectInstance(
                DownedEffects.DOWNED.get(),
                DownedTiming.secondsToTicks(DownedConfig.BLEEDOUT_SECONDS.get()),
                0, false, false, false));
        player.displayClientMessage(
                Component.translatable("message.wok_downed.entered"), true);
    }

    public static boolean inFinishingGrace(ServerPlayer player) {
        long elapsed = player.level().getGameTime()
                - player.getPersistentData().getLong(START_TIME_KEY);
        return elapsed < DownedTiming.secondsToTicks(
                DownedConfig.FINISHING_DAMAGE_GRACE_SECONDS.get());
    }

    public static void scheduleFinishingDeath(ServerPlayer player, DamageSource source) {
        if (!PENDING_DEATHS.containsKey(player.getUUID())) {
            PENDING_DEATHS.put(player.getUUID(), source);
        }
        cancelByParticipant(player, true);
        stopDraggingByParticipant(player, true);
    }

    public static boolean startRescue(ServerPlayer rescuer, ServerPlayer casualty) {
        if (rescuer == casualty || isDowned(rescuer) || !isDowned(casualty)
                || !rescuer.isAlive() || !casualty.isAlive()
                || rescuer.level() != casualty.level()
                || rescuer.distanceToSqr(casualty) > rescueDistanceSquared()
                || isDragged(casualty) || isDragging(rescuer)) {
            rescuer.displayClientMessage(
                    Component.translatable("message.wok_downed.rescue_invalid"), true);
            return false;
        }
        RescueSession existing = RESCUES.get(casualty.getUUID());
        if (existing != null) {
            if (!existing.rescuerId.equals(rescuer.getUUID())) {
                rescuer.displayClientMessage(
                        Component.translatable("message.wok_downed.rescue_busy"), true);
            }
            return false;
        }
        if (isRescuing(rescuer)) {
            rescuer.displayClientMessage(
                    Component.translatable("message.wok_downed.rescue_invalid"), true);
            return false;
        }

        int totalTicks = DownedTiming.secondsToTicks(DownedConfig.RESCUE_SECONDS.get());
        RESCUES.put(casualty.getUUID(), new RescueSession(
                rescuer.getUUID(), casualty.getUUID(), rescuer.position(),
                casualty.position(), totalTicks));
        rescuer.displayClientMessage(Component.translatable(
                "message.wok_downed.rescue_started", casualty.getDisplayName()), true);
        casualty.displayClientMessage(Component.translatable(
                "message.wok_downed.being_rescued", rescuer.getDisplayName()), true);
        return true;
    }

    public static boolean toggleDrag(ServerPlayer carrier, ServerPlayer casualty) {
        DragSession existing = DRAGS.get(casualty.getUUID());
        if (existing != null && existing.carrierId.equals(carrier.getUUID())) {
            stopDraggingByParticipant(carrier, true);
            return true;
        }
        if (carrier == casualty || isDowned(carrier) || !isDowned(casualty)
                || !carrier.isAlive() || !casualty.isAlive()
                || carrier.level() != casualty.level()
                || carrier.distanceToSqr(casualty) > rescueDistanceSquared()
                || existing != null || isDragging(carrier)) {
            carrier.displayClientMessage(
                    Component.translatable("message.wok_downed.drag_invalid"), true);
            return false;
        }

        cancelByParticipant(carrier, true);
        cancelByParticipant(casualty, true);
        DRAGS.put(casualty.getUUID(), new DragSession(
                carrier.getUUID(), casualty.getUUID()));
        applyDragSpeed(carrier);
        carrier.displayClientMessage(Component.translatable(
                "message.wok_downed.drag_started", casualty.getDisplayName()), true);
        casualty.displayClientMessage(Component.translatable(
                "message.wok_downed.being_dragged", carrier.getDisplayName()), true);
        return true;
    }

    public static void tickPlayer(ServerPlayer player) {
        DamageSource pendingDeath = PENDING_DEATHS.remove(player.getUUID());
        if (pendingDeath != null && isDowned(player)) {
            forceDeath(player, pendingDeath);
            return;
        }

        if (isDowned(player)) {
            if (player.getPersistentData().getBoolean(STABILIZE_PENDING_KEY)) {
                player.getPersistentData().remove(STABILIZE_PENDING_KEY);
                MedicalCompat.stabilize(player, 1.0F);
            }
            freeze(player);
            DownedPose.lock(player);
            if (!player.hasEffect(DownedEffects.DOWNED.get())) {
                forceDeath(player, player.damageSources().genericKill());
                return;
            }
            tickDrag(player);
            tickRescue(player);
        } else if (isRescuing(player)) {
            freeze(player);
        } else if (isDragging(player)) {
            player.setSprinting(false);
            applyDragSpeed(player);
        }
    }

    public static void revive(ServerPlayer casualty, ServerPlayer rescuer) {
        RESCUES.remove(casualty.getUUID());
        stopDraggingByParticipant(casualty, false);
        PENDING_DEATHS.remove(casualty.getUUID());
        clearState(casualty);
        float health = (float) Math.min(casualty.getMaxHealth(),
                DownedConfig.REVIVE_HEALTH.get());
        casualty.setHealth(Math.max(1.0F, health));
        casualty.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, true));
        MedicalCompat.stabilize(casualty,
                DownedConfig.BODY_HEALTH_REVIVE_AMOUNT.get().floatValue());
        casualty.displayClientMessage(
                Component.translatable("message.wok_downed.revived"), true);
        if (rescuer != null) {
            rescuer.displayClientMessage(
                    Component.translatable("message.wok_downed.revived"), true);
        }
    }

    public static void giveUp(ServerPlayer player) {
        if (!isDowned(player)) {
            player.displayClientMessage(
                    Component.translatable("command.wok_downed.not_downed"), false);
            return;
        }
        player.displayClientMessage(
                Component.translatable("message.wok_downed.gave_up"), false);
        forceDeath(player, player.damageSources().genericKill());
    }

    public static void clearState(ServerPlayer player) {
        player.getPersistentData().remove(ACTIVE_KEY);
        player.getPersistentData().remove(START_TIME_KEY);
        player.getPersistentData().remove(STABILIZE_PENDING_KEY);
        player.removeEffect(DownedEffects.DOWNED.get());
        player.setForcedPose(null);
    }

    public static void cancelByParticipant(ServerPlayer player, boolean notify) {
        Iterator<Map.Entry<UUID, RescueSession>> iterator = RESCUES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RescueSession> entry = iterator.next();
            RescueSession session = entry.getValue();
            if (!session.casualtyId.equals(player.getUUID())
                    && !session.rescuerId.equals(player.getUUID())) {
                continue;
            }
            iterator.remove();
            if (notify && player.getServer() != null) {
                ServerPlayer casualty = player.getServer().getPlayerList()
                        .getPlayer(session.casualtyId);
                ServerPlayer rescuer = player.getServer().getPlayerList()
                        .getPlayer(session.rescuerId);
                notifyCanceled(casualty);
                notifyCanceled(rescuer);
            }
        }
    }

    public static void stopDraggingByParticipant(ServerPlayer player, boolean notify) {
        Iterator<Map.Entry<UUID, DragSession>> iterator = DRAGS.entrySet().iterator();
        while (iterator.hasNext()) {
            DragSession session = iterator.next().getValue();
            if (!session.casualtyId.equals(player.getUUID())
                    && !session.carrierId.equals(player.getUUID())) {
                continue;
            }
            iterator.remove();
            if (player.getServer() != null) {
                ServerPlayer carrier = player.getServer().getPlayerList()
                        .getPlayer(session.carrierId);
                ServerPlayer casualty = player.getServer().getPlayerList()
                        .getPlayer(session.casualtyId);
                removeDragSpeed(carrier);
                if (notify) {
                    notifyDragStopped(carrier);
                    notifyDragStopped(casualty);
                }
            } else {
                removeDragSpeed(player);
            }
        }
    }

    public static void clearRuntimeState() {
        RESCUES.clear();
        DRAGS.clear();
        PENDING_DEATHS.clear();
    }

    private static void tickDrag(ServerPlayer casualty) {
        DragSession session = DRAGS.get(casualty.getUUID());
        if (session == null || casualty.getServer() == null) {
            return;
        }
        ServerPlayer carrier = casualty.getServer().getPlayerList()
                .getPlayer(session.carrierId);
        if (carrier == null || !carrier.isAlive() || isDowned(carrier)
                || carrier.level() != casualty.level()) {
            stopDraggingByParticipant(casualty, true);
            return;
        }

        carrier.setSprinting(false);
        applyDragSpeed(carrier);
        Vec3 look = carrier.getLookAngle();
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        double directionX = horizontalLength < 1.0E-4D
                ? 0.0D : look.x / horizontalLength;
        double directionZ = horizontalLength < 1.0E-4D
                ? 1.0D : look.z / horizontalLength;
        double distance = DownedConfig.DRAG_FOLLOW_DISTANCE.get();
        casualty.teleportTo(
                carrier.getX() - directionX * distance,
                carrier.getY() + 0.05D,
                carrier.getZ() - directionZ * distance);
        casualty.setYRot(carrier.getYRot());
        // PlayerTickEvent runs inside connection.tick(), which restores firstGoodXYZ after
        // player.doTick(). Commit this server-driven move to that baseline so trackers see it,
        // even when the casualty's teleport acknowledgement arrives several ticks later.
        casualty.connection.resetPosition();
        casualty.fallDistance = 0.0F;
    }

    private static void tickRescue(ServerPlayer casualty) {
        RescueSession session = RESCUES.get(casualty.getUUID());
        if (session == null || casualty.getServer() == null) {
            return;
        }
        ServerPlayer rescuer = casualty.getServer().getPlayerList()
                .getPlayer(session.rescuerId);
        if (rescuer == null || isDowned(rescuer) || !rescuer.isAlive()
                || rescuer.level() != casualty.level()
                || rescuer.distanceToSqr(casualty) > rescueDistanceSquared()
                || DownedTiming.movedTooFar(
                        rescuer.position().distanceToSqr(session.rescuerStart),
                        DownedConfig.MOVEMENT_TOLERANCE.get())
                || DownedTiming.movedTooFar(
                        casualty.position().distanceToSqr(session.casualtyStart),
                        DownedConfig.MOVEMENT_TOLERANCE.get())) {
            RESCUES.remove(casualty.getUUID());
            notifyCanceled(casualty);
            notifyCanceled(rescuer);
            return;
        }

        session.remainingTicks--;
        if (session.remainingTicks <= 0) {
            revive(casualty, rescuer);
            return;
        }
        if (session.remainingTicks % 10 == 0) {
            int progress = DownedTiming.progressPercent(
                    session.totalTicks, session.remainingTicks);
            rescuer.displayClientMessage(Component.translatable(
                    "message.wok_downed.rescue_progress", progress), true);
        }
    }

    private static void forceDeath(ServerPlayer player, DamageSource source) {
        cancelByParticipant(player, true);
        stopDraggingByParticipant(player, true);
        PENDING_DEATHS.remove(player.getUUID());
        clearState(player);
        boolean previous = FORCING_DEATH.get();
        FORCING_DEATH.set(true);
        try {
            player.setHealth(0.0F);
            player.die(source);
        } finally {
            FORCING_DEATH.set(previous);
        }
    }

    private static void freeze(ServerPlayer player) {
        double vertical = player.onGround()
                ? 0.0D : Math.min(0.0D, player.getDeltaMovement().y);
        player.setDeltaMovement(0.0D, vertical, 0.0D);
        player.hurtMarked = true;
        player.stopUsingItem();
    }

    private static boolean isRescuing(ServerPlayer player) {
        UUID id = player.getUUID();
        return RESCUES.values().stream().anyMatch(session ->
                session.rescuerId.equals(id));
    }

    private static boolean isDragging(ServerPlayer player) {
        UUID id = player.getUUID();
        return DRAGS.values().stream().anyMatch(session ->
                session.carrierId.equals(id));
    }

    private static boolean isDragged(ServerPlayer player) {
        return DRAGS.containsKey(player.getUUID());
    }

    private static void applyDragSpeed(ServerPlayer carrier) {
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.getModifier(DRAG_SPEED_MODIFIER_ID) != null) {
            return;
        }
        double multiplier = DownedConfig.DRAG_SPEED_MULTIPLIER.get();
        speed.addTransientModifier(new AttributeModifier(
                DRAG_SPEED_MODIFIER_ID, "WOK downed casualty drag",
                multiplier - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeDragSpeed(ServerPlayer carrier) {
        if (carrier == null) {
            return;
        }
        AttributeInstance speed = carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(DRAG_SPEED_MODIFIER_ID);
        }
    }

    private static double rescueDistanceSquared() {
        double distance = DownedConfig.RESCUE_DISTANCE.get();
        return distance * distance;
    }

    private static void notifyCanceled(ServerPlayer player) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.wok_downed.rescue_canceled"), true);
        }
    }

    private static void notifyDragStopped(ServerPlayer player) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.wok_downed.drag_stopped"), true);
        }
    }

    private static final class RescueSession {
        private final UUID rescuerId;
        private final UUID casualtyId;
        private final Vec3 rescuerStart;
        private final Vec3 casualtyStart;
        private final int totalTicks;
        private int remainingTicks;

        private RescueSession(UUID rescuerId, UUID casualtyId,
                              Vec3 rescuerStart, Vec3 casualtyStart,
                              int totalTicks) {
            this.rescuerId = rescuerId;
            this.casualtyId = casualtyId;
            this.rescuerStart = rescuerStart;
            this.casualtyStart = casualtyStart;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
        }
    }

    private record DragSession(UUID carrierId, UUID casualtyId) {
    }

    private DownedService() {
    }
}
