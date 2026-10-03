package com.wok.bodyhealth.prone;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reads Tacz-Animation-Additions' server prone state ({@code ProneServer.state(Player)}) through
 * method handles, so this module never links against TAA. Only {@code state} and the record
 * accessors are called; ProneServer's references to client classes are never resolved by them.
 */
public final class TaaProneBridge {
    public static final String MOD_ID = "locknar_doorkick";
    private static final String SERVER_CLASS = "com.locknar.taczammocheck.prone.ProneServer";
    private static final String STATE_CLASS = SERVER_CLASS + "$State";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Mirror of TAA's {@code ProneServer$State}; phase 1 enter, 2 prone, 3 exit, 4 reorient. */
    public record State(int phase, long start, float anchor, int duration, float target) {
    }

    private record Handles(MethodHandle state, MethodHandle phase, MethodHandle start, MethodHandle anchor,
                           MethodHandle duration, MethodHandle target) {
    }

    private static volatile Boolean installed;
    private static volatile Handles handles;
    private static volatile boolean resolved;
    private static volatile boolean broken;

    public static boolean installed() {
        Boolean cached = installed;
        if (cached == null) {
            ModList mods = ModList.get();
            if (mods == null) {
                return false;
            }
            cached = mods.isLoaded(MOD_ID);
            installed = cached;
        }
        return cached;
    }

    /** Installed, resolved and not broken at runtime. */
    public static boolean available() {
        return installed() && !broken && resolve() != null;
    }

    /**
     * TAA state of the player, or null when TAA has none (standing or vanilla crawl). A runtime
     * failure marks the bridge broken for good; callers then treat prone players as TAA_ASSUMED.
     */
    public static State read(Player p) {
        Handles h = available() ? handles : null;
        if (h == null) {
            return null;
        }
        try {
            Object state = h.state().invoke(p);
            if (state == null) {
                return null;
            }
            return new State((int) h.phase().invoke(state), (long) h.start().invoke(state),
                    (float) h.anchor().invoke(state), (int) h.duration().invoke(state),
                    (float) h.target().invoke(state));
        } catch (Throwable throwable) {
            broken = true;
            LOGGER.warn("Reading the Tacz-Animation-Additions prone state failed; prone players are "
                    + "assumed to lie in TAA's steady pose from now on.", throwable);
            return null;
        }
    }

    private static Handles resolve() {
        if (!resolved) {
            synchronized (TaaProneBridge.class) {
                if (!resolved) {
                    handles = lookup();
                    resolved = true;
                }
            }
        }
        return handles;
    }

    private static Handles lookup() {
        try {
            ClassLoader loader = TaaProneBridge.class.getClassLoader();
            Class<?> server = Class.forName(SERVER_CLASS, false, loader);
            Class<?> state = Class.forName(STATE_CLASS, false, loader);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            return new Handles(
                    lookup.findStatic(server, "state", MethodType.methodType(state, Player.class))
                            .asType(MethodType.methodType(Object.class, Player.class)),
                    accessor(lookup, state, "phase", int.class),
                    accessor(lookup, state, "start", long.class),
                    accessor(lookup, state, "anchor", float.class),
                    accessor(lookup, state, "duration", int.class),
                    accessor(lookup, state, "target", float.class));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.warn("Tacz-Animation-Additions is installed but its prone state cannot be read; "
                    + "prone players are assumed to lie in TAA's steady pose.", exception);
            return null;
        }
    }

    private static MethodHandle accessor(MethodHandles.Lookup lookup, Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        return lookup.findVirtual(owner, name, MethodType.methodType(type))
                .asType(MethodType.methodType(type, Object.class));
    }

    private TaaProneBridge() {
    }
}
