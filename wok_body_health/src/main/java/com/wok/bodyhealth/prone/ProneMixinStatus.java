package com.wok.bodyhealth.prone;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Which prone injections actually landed. The mixin config plugin checks every target class
 * after it was mixed and writes {@code wok.bodyhealth.mixin.<key>} system properties; reading
 * them here works regardless of which class loader loaded the plugin. Read live on every call.
 */
public final class ProneMixinStatus {
    public static final String DISABLE_PROPERTY = "wok.bodyhealth.disableProneMixins";
    private static final String PREFIX = "wok.bodyhealth.mixin.";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean taczHead() {
        return get("tacz.head");
    }

    /** The clip wrapper is in place, so a MISS can let TaCZ return null on its own. */
    public static boolean taczWrap() {
        return get("tacz.wrap");
    }

    public static boolean taczScope() {
        return get("tacz.scope");
    }

    public static boolean sbwHead() {
        return get("sbw.head");
    }

    public static boolean sbwScope() {
        return get("sbw.scope");
    }

    /** The getHitResult HEAD hook of {@code c} is in place. */
    public static boolean head(ProneConsumer c) {
        return get(c.headKey());
    }

    public static boolean level() {
        return get("level");
    }

    /** One line per startup: which hooks are live and what each missing one costs. */
    public static void logSummary(boolean tacz, boolean sbw) {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            LOGGER.warn("Segmented prone hitboxes are switched off by -D{}=true; "
                    + "TaCZ/SBW keep their original hit detection.", DISABLE_PROPERTY);
            return;
        }
        // Loading a class runs the mixin transformer and with it the plugin's injection check.
        ClassLoader loader = ProneMixinStatus.class.getClassLoader();
        touch("net.minecraft.world.level.Level", loader);
        if (tacz) {
            touch("com.tacz.guns.util.EntityUtil", loader);
        }
        if (sbw) {
            touch("com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity", loader);
        }

        List<String> missing = new ArrayList<>();
        check(missing, "level", level(), "prone targets are not added to bullet broad-phase queries");
        if (tacz) {
            check(missing, "tacz.head", taczHead(), "TaCZ keeps the 0.6 block prone hitbox");
            check(missing, "tacz.wrap", taczWrap(),
                    "TaCZ misses return early, skipping proximity-fuse re-checks");
            check(missing, "tacz.scope", taczScope(), "TaCZ broad phase is not widened");
        }
        if (sbw) {
            check(missing, "sbw.head", sbwHead(), "SBW keeps the 0.6 block prone hitbox");
            check(missing, "sbw.scope", sbwScope(), "SBW broad phase is not widened");
        }
        if (missing.isEmpty()) {
            LOGGER.info("Segmented prone hitboxes active (TaCZ: {}, SBW: {}).", tacz, sbw);
        } else {
            LOGGER.warn("Segmented prone hitboxes are only partly active: {}", String.join("; ", missing));
        }
    }

    private static void check(List<String> missing, String key, boolean ok, String consequence) {
        if (!ok) {
            missing.add(key + " missing (" + consequence + ")");
        }
    }

    private static void touch(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError exception) {
            LOGGER.warn("Could not load {} to check the prone injections", name, exception);
        }
    }

    private static boolean get(String key) {
        return Boolean.getBoolean(PREFIX + key);
    }

    private ProneMixinStatus() {
    }
}
