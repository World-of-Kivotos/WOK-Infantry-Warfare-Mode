package com.wok.bodyhealth.compat.tacz;

import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.HitboxHelper;
import com.wok.bodyhealth.prone.ProneConsumer;
import com.wok.bodyhealth.prone.ProneHistory;
import com.wok.bodyhealth.prone.ProneHitService;
import com.wok.bodyhealth.prone.ProneProjectileKind;
import com.wok.bodyhealth.prone.RewindPolicy;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/** TaCZ's real bullet and the rewind rules of {@code HitboxHelper.getFixedBoundingBox}. */
public final class TaczProneKind implements ProneProjectileKind {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TaczProneKind INSTANCE = new TaczProneKind();
    /** Used only when neither SAVE_TICK nor the config can be read; TaCZ's default is 1000 ms. */
    private static final int DEFAULT_SAVE_TICKS = 20;
    private static volatile int cachedSaveTicks = -1;

    /** Called reflectively by {@link ProneHitService#bootstrap}. */
    public static void register() {
        ProneHitService.registerKind(INSTANCE);
        ProneHitService.setGunProbe(stack -> IGun.getIGunOrNull(stack) != null);
    }

    public static boolean isBullet(Entity e) {
        return e instanceof EntityKineticBullet;
    }

    /**
     * LATENCY_FIX and OFFSET are read live like {@code getFixedBoundingBox} does; the history
     * length is TaCZ's SAVE_TICK, which TaCZ itself fixes at class initialisation.
     */
    public static RewindPolicy policy() {
        return new RewindPolicy(OtherConfig.SERVER_HITBOX_LATENCY_FIX.get(),
                OtherConfig.SERVER_HITBOX_OFFSET.get(), saveTicks());
    }

    @Override
    public boolean matches(Entity projectile) {
        return isBullet(projectile);
    }

    @Override
    public ProneConsumer consumer() {
        return ProneConsumer.TACZ;
    }

    @Override
    public Entity shooter(Entity projectile) {
        // Through the vanilla type: reobf cannot follow inherited members of SRG-named TaCZ classes.
        return projectile instanceof Projectile bullet ? bullet.getOwner() : null;
    }

    @Override
    public RewindPolicy policy(Entity projectile) {
        return policy();
    }

    private static int saveTicks() {
        int cached = cachedSaveTicks;
        if (cached > 0) {
            return cached;
        }
        int ticks;
        try {
            Field field = HitboxHelper.class.getDeclaredField("SAVE_TICK");
            field.setAccessible(true);
            ticks = field.getInt(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            try {
                ticks = Math.max(1, Mth.floor(
                        OtherConfig.SERVER_HITBOX_LATENCY_MAX_SAVE_MS.get() / 1000.0D * 20.0D + 0.5D));
                LOGGER.warn("TaCZ HitboxHelper.SAVE_TICK is unreadable; using {} ticks from the config", ticks,
                        exception);
            } catch (RuntimeException configNotReady) {
                // Not cached: the config may simply not be loaded yet.
                return DEFAULT_SAVE_TICKS;
            }
        }
        cachedSaveTicks = Mth.clamp(ticks, 1, ProneHistory.CAPACITY);
        return cachedSaveTicks;
    }

    private TaczProneKind() {
    }
}
