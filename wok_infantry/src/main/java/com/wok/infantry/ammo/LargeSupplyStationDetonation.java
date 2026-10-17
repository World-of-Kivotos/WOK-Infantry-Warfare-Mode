package com.wok.infantry.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative, staged destruction effect for the 1500-point ammunition station. */
public final class LargeSupplyStationDetonation {
    private static final String STARTED_TAG = "wok_infantry_large_station_detonation_started";
    private static final String LARGE_REMAINING_POINTS_TAG =
            "wok_infantry_ammo_remaining_points";
    private static final float EXPLOSION_RADIUS = 24.0F;
    private static final double PREBLAST_COLUMN_HEIGHT = 64.0D;
    private static final double EFFECT_Y_OFFSET = 0.8D;
    private static final ResourceLocation FIRE_STAR =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "fire_star");
    private static final ResourceLocation WHITE_STAR =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "white_star");
    private static final ResourceLocation RISING_SMOKE =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "rising_smoke");
    private static final ResourceLocation TURRET_BURN_START =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "turret_burn_start");
    private static final ResourceLocation HUGE_EXPLOSION_CLOSE =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "huge_explosion_close");
    private static final ResourceLocation MINI_EXPLOSION =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "mini_explosion");
    private static final Map<UUID, Detonation> ACTIVE = new LinkedHashMap<>();

    private LargeSupplyStationDetonation() {
    }

    /** Starts exactly once when SBW enters its destruction routine for the large station. */
    public static void begin(Entity station) {
        if (!AmmoSupplyService.isLargeStation(station)
                || !(station.level() instanceof ServerLevel level)
                || ACTIVE.containsKey(station.getUUID())) {
            return;
        }
        station.getPersistentData().putBoolean(STARTED_TAG, true);
        station.getPersistentData().putInt(LARGE_REMAINING_POINTS_TAG, 0);
        AmmoSupplyService.disableNativeLargeStation(station);

        Detonation effect = new Detonation(station.getUUID(), level.dimension(),
                station.getX(), station.getY() + EFFECT_Y_OFFSET, station.getZ());
        ACTIVE.put(station.getUUID(), effect);
        ignite(level, effect);
    }

    /** Restarts an interrupted warning after a server reload while the wreck is still present. */
    public static void resumeIfInterrupted(Entity station) {
        if (station != null && station.getPersistentData().getBoolean(STARTED_TAG)) {
            begin(station);
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Detonation>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Detonation effect = iterator.next().getValue();
            ServerLevel level = server.getLevel(effect.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            effect.advance();
            switch (LargeSupplyDetonationTimeline.phaseAt(effect.ageTicks())) {
                case PREBLAST_FIRE_COLUMN -> preBlastFireColumn(level, effect);
                case PRIMARY_BLAST -> primaryBlast(level, effect);
                case AFTERBURN -> afterburn(level, effect);
                case COMPLETE -> iterator.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void ignite(ServerLevel level, Detonation effect) {
        playOptionalSound(level, effect, TURRET_BURN_START,
                SoundEvents.FIRECHARGE_USE, 3.5F, 0.72F);
        sendVisible(level, ParticleTypes.FLAME, effect.x(), effect.y(), effect.z(),
                52, 2.4D, 1.0D, 2.4D, 0.1D);
        sendVisible(level, particle(RISING_SMOKE, ParticleTypes.LARGE_SMOKE),
                effect.x(), effect.y() + 0.4D, effect.z(), 18,
                2.0D, 0.7D, 2.0D, 0.05D);
    }

    private static void preBlastFireColumn(ServerLevel level, Detonation effect) {
        RandomSource random = level.random;
        ParticleOptions fireStar = particle(FIRE_STAR, ParticleTypes.FLAME);
        double headHeight = Math.min(PREBLAST_COLUMN_HEIGHT,
                5.0D + effect.ageTicks() * 0.8D);
        for (int index = 0; index < 11; index++) {
            sendMoving(level, fireStar, effect.x() + randomOffset(random, 2.6D),
                    effect.y() + random.nextDouble(),
                    effect.z() + randomOffset(random, 2.6D),
                    randomOffset(random, 0.24D), 1.1D + random.nextDouble() * 1.1D,
                    randomOffset(random, 0.24D));
        }
        sendVisible(level, ParticleTypes.FLAME, effect.x(), effect.y() + headHeight,
                effect.z(), 18, 2.2D + headHeight * 0.035D, 1.4D,
                2.2D + headHeight * 0.035D, 0.065D);
        sendVisible(level, particle(RISING_SMOKE, ParticleTypes.LARGE_SMOKE),
                effect.x(), effect.y() + headHeight + 1.0D, effect.z(), 8,
                2.8D, 1.2D, 2.8D, 0.08D);
        if ((effect.ageTicks() & 1) == 0 && headHeight > 10.0D) {
            sendVisible(level, fireStar, effect.x(), effect.y() + headHeight * 0.34D,
                    effect.z(), 7, 2.0D, headHeight * 0.14D, 2.0D, 0.065D);
            sendVisible(level, ParticleTypes.FLAME, effect.x(),
                    effect.y() + headHeight * 0.7D, effect.z(), 9,
                    2.2D, headHeight * 0.1D, 2.2D, 0.06D);
        }
        if (effect.ageTicks() % 20 == 0) {
            level.playSound(null, BlockPos.containing(effect.x(), effect.y(), effect.z()),
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 3.0F,
                    0.72F + random.nextFloat() * 0.12F);
        }
        if (LargeSupplyDetonationTimeline.isPreBlastPop(effect.ageTicks())) {
            preBlastPop(level, effect, headHeight);
        }
    }

    private static void preBlastPop(ServerLevel level, Detonation effect,
                                    double headHeight) {
        RandomSource random = level.random;
        double popX = effect.x() + randomOffset(random, 3.0D);
        double popY = effect.y() + 1.0D
                + random.nextDouble() * Math.min(24.0D, headHeight * 0.45D);
        double popZ = effect.z() + randomOffset(random, 3.0D);
        playOptionalSound(level, effect, MINI_EXPLOSION,
                SoundEvents.GENERIC_EXPLODE, 4.5F, 0.82F + random.nextFloat() * 0.16F);
        sendVisible(level, ParticleTypes.EXPLOSION, popX, popY, popZ,
                5, 1.4D, 1.0D, 1.4D, 0.04D);
        sendVisible(level, particle(FIRE_STAR, ParticleTypes.FLAME),
                popX, popY, popZ, 26, 1.8D, 1.2D, 1.8D, 0.32D);
        sendVisible(level, ParticleTypes.LAVA, popX, popY, popZ,
                12, 1.2D, 0.8D, 1.2D, 0.24D);
    }

    private static void primaryBlast(ServerLevel level, Detonation effect) {
        Entity station = level.getEntity(effect.stationId());
        if (station != null) {
            station.discard();
        }
        playOptionalSound(level, effect, HUGE_EXPLOSION_CLOSE,
                SoundEvents.GENERIC_EXPLODE, 16.0F, 0.68F);
        level.playSound(null, effect.x(), effect.y(), effect.z(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 14.0F, 0.58F);

        sendVisible(level, ParticleTypes.FLASH, effect.x(), effect.y() + 1.0D,
                effect.z(), 18, 5.0D, 2.8D, 5.0D, 0.0D);
        sendVisible(level, ParticleTypes.EXPLOSION_EMITTER, effect.x(), effect.y() + 1.0D,
                effect.z(), 12, 5.5D, 3.0D, 5.5D, 0.0D);
        launchBlastParticles(level, effect);

        // Damage and terrain effects occur only at the main flash, after the visible warning.
        level.explode(null, effect.x(), effect.y(), effect.z(), EXPLOSION_RADIUS,
                true, Level.ExplosionInteraction.TNT);
    }

    private static void launchBlastParticles(ServerLevel level, Detonation effect) {
        RandomSource random = level.random;
        ParticleOptions fireStar = particle(FIRE_STAR, ParticleTypes.FLAME);
        ParticleOptions whiteStar = particle(WHITE_STAR, ParticleTypes.END_ROD);
        ParticleOptions risingSmoke = particle(RISING_SMOKE, ParticleTypes.LARGE_SMOKE);
        for (int index = 0; index < 160; index++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radialSpeed = 0.28D + random.nextDouble() * 2.15D;
            sendMoving(level, fireStar, effect.x(), effect.y() + random.nextDouble() * 2.0D,
                    effect.z(), Math.cos(angle) * radialSpeed,
                    0.65D + random.nextDouble() * 1.65D,
                    Math.sin(angle) * radialSpeed);
        }
        for (int index = 0; index < 64; index++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radialSpeed = 0.7D + random.nextDouble() * 2.25D;
            sendMoving(level, whiteStar, effect.x(), effect.y() + 1.0D, effect.z(),
                    Math.cos(angle) * radialSpeed,
                    0.15D + random.nextDouble() * 0.75D,
                    Math.sin(angle) * radialSpeed);
        }
        for (int index = 0; index < 44; index++) {
            sendMoving(level, risingSmoke,
                    effect.x() + randomOffset(random, 1.6D), effect.y(),
                    effect.z() + randomOffset(random, 1.6D),
                    randomOffset(random, 0.28D), 0.55D + random.nextDouble() * 0.9D,
                    randomOffset(random, 0.28D));
        }
        for (int index = 0; index < 72; index++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radialSpeed = 0.25D + random.nextDouble() * 0.9D;
            sendMoving(level, ParticleTypes.LAVA, effect.x(), effect.y() + 1.0D,
                    effect.z(), Math.cos(angle) * radialSpeed,
                    0.45D + random.nextDouble() * 1.0D,
                    Math.sin(angle) * radialSpeed);
        }
    }

    private static void afterburn(ServerLevel level, Detonation effect) {
        int ticksAfterBlast = effect.ageTicks()
                - LargeSupplyDetonationTimeline.PREBLAST_FIRE_COLUMN_TICKS;
        RandomSource random = level.random;
        double headHeight = Math.max(3.0D, 40.0D - ticksAfterBlast * 0.31D);
        int risingStars = ticksAfterBlast <= 45 ? 4 : 1;
        ParticleOptions fireStar = particle(FIRE_STAR, ParticleTypes.FLAME);
        for (int index = 0; index < risingStars; index++) {
            sendMoving(level, fireStar,
                    effect.x() + randomOffset(random, 1.5D), effect.y(),
                    effect.z() + randomOffset(random, 1.5D),
                    randomOffset(random, 0.16D),
                    0.72D + random.nextDouble() * 0.72D,
                    randomOffset(random, 0.16D));
        }

        sendVisible(level, ParticleTypes.FLAME, effect.x(), effect.y() + headHeight,
                effect.z(), ticksAfterBlast <= 45 ? 7 : 3,
                1.2D + headHeight * 0.025D, 0.8D,
                1.2D + headHeight * 0.025D, 0.045D);
        sendVisible(level, particle(RISING_SMOKE, ParticleTypes.LARGE_SMOKE),
                effect.x(), effect.y() + headHeight + 0.8D, effect.z(),
                ticksAfterBlast <= 60 ? 4 : 2,
                1.4D, 0.7D, 1.4D, 0.055D);

        if ((ticksAfterBlast & 1) == 0 && headHeight > 6.0D) {
            sendVisible(level, fireStar, effect.x(),
                    effect.y() + headHeight * 0.52D, effect.z(), 3,
                    1.0D, headHeight * 0.18D, 1.0D, 0.05D);
        }
        if (LargeSupplyDetonationTimeline.isAftershock(ticksAfterBlast)) {
            aftershock(level, effect, headHeight);
        }
        if (ticksAfterBlast % 20 == 0) {
            level.playSound(null, effect.x(), effect.y(), effect.z(),
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 4.0F,
                    0.62F + random.nextFloat() * 0.16F);
        }
    }

    private static void aftershock(ServerLevel level, Detonation effect, double headHeight) {
        playOptionalSound(level, effect, MINI_EXPLOSION,
                SoundEvents.GENERIC_EXPLODE, 4.0F, 0.85F);
        sendVisible(level, ParticleTypes.EXPLOSION, effect.x(),
                effect.y() + Math.min(8.0D, headHeight * 0.45D), effect.z(),
                9, 2.0D, 1.8D, 2.0D, 0.05D);
        sendVisible(level, particle(FIRE_STAR, ParticleTypes.FLAME),
                effect.x(), effect.y() + 0.5D, effect.z(),
                28, 2.0D, 1.0D, 2.0D, 0.28D);
    }

    private static ParticleOptions particle(ResourceLocation id, ParticleOptions fallback) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        return type instanceof ParticleOptions options ? options : fallback;
    }

    private static void playOptionalSound(ServerLevel level, Detonation effect,
                                          ResourceLocation soundId, SoundEvent fallback,
                                          float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(soundId).orElse(fallback);
        level.playSound(null, effect.x(), effect.y(), effect.z(), sound,
                SoundSource.BLOCKS, volume, pitch);
    }

    private static void sendMoving(ServerLevel level, ParticleOptions particle,
                                   double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ) {
        // A zero count makes the packet's offsets act as one particle's exact velocity.
        sendVisible(level, particle, x, y, z, 0,
                velocityX, velocityY, velocityZ, 1.0D);
    }

    private static void sendVisible(ServerLevel level, ParticleOptions particle,
                                    double x, double y, double z, int count,
                                    double offsetX, double offsetY, double offsetZ,
                                    double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particle, true, x, y, z, count,
                    offsetX, offsetY, offsetZ, speed);
        }
    }

    private static double randomOffset(RandomSource random, double radius) {
        return (random.nextDouble() * 2.0D - 1.0D) * radius;
    }

    private static final class Detonation {
        private final UUID stationId;
        private final ResourceKey<Level> dimension;
        private final double x;
        private final double y;
        private final double z;
        private int ageTicks;

        private Detonation(UUID stationId, ResourceKey<Level> dimension,
                           double x, double y, double z) {
            this.stationId = stationId;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void advance() {
            ageTicks++;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private UUID stationId() {
            return stationId;
        }

        private double x() {
            return x;
        }

        private double y() {
            return y;
        }

        private double z() {
            return z;
        }

        private int ageTicks() {
            return ageTicks;
        }
    }
}
