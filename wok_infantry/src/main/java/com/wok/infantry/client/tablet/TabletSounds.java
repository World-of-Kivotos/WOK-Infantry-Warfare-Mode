package com.wok.infantry.client.tablet;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * The twelve self-made tablet sounds in the game (batch B4, DESIGN 3.6): one
 * {@link TabletSoundBook} for the client, its output plays {@link TabletSoundInstance}s through
 * the client's own {@code SoundManager} — local only, nothing goes to the server or other
 * players. Client thread only.
 *
 * <p><b>Not registered</b> (IMPL_PLAN D1): the events {@code wok_infantry:tablet.<name>} are built
 * with {@link SoundEvent#createVariableRangeEvent} and never enter {@code ForgeRegistries.SOUND_EVENTS}.
 * The sound manager plays a sound by the location in {@code assets/wok_infantry/sounds.json} and
 * never looks at the registry; the sound-event registry is synced to clients on login, so twelve
 * registered entries would make a 0.5.0-beta.4 server reject 0.5.0-beta.1–3 clients ("missing
 * registry entries"), which {@code docs/VERSIONING.md} counts as an incompatible change (0.6.0).
 * Registering belongs to a later round that lets other players hear the tablet.
 *
 * <p>The files are synthesized offline ({@code ui-preview/tablet-anim/tools/make-sounds.mjs},
 * seed 20261010; see {@code assets/wok_infantry/sounds/tablet/readme.txt}).
 */
public final class TabletSounds {
    private static final Map<String, SoundEvent> EVENTS = new HashMap<>();
    private static boolean playWarned;

    private static final TabletSoundBook<TabletSoundInstance> BOOK = new TabletSoundBook<>(
            new TabletSoundBook.Output<>() {
                @Override
                public TabletSoundInstance play(String name, TabletCue.Dir dir) {
                    return start(name, dir);
                }

                @Override
                public void fadeOut(TabletSoundInstance sound, double now) {
                    sound.fadeOut(now);
                }
            });

    private TabletSounds() {
    }

    /** The event location of a sound name: {@code wok_infantry:tablet.<name>}. */
    public static ResourceLocation location(String name) {
        return new ResourceLocation(WokInfantryMod.MOD_ID, "tablet." + name);
    }

    /** The (unregistered, D1) event of a sound name. */
    public static SoundEvent event(String name) {
        return EVENTS.computeIfAbsent(name, key -> SoundEvent.createVariableRangeEvent(location(key)));
    }

    /** The milliseconds clock the animation runs on. */
    public static double clock() {
        return System.nanoTime() / 1.0E6D;
    }

    /** One event of the motion ({@link TabletSoundBook#onEvent}). */
    static void onEvent(TabletMotion.Event event, double now, boolean enabled, BooleanSupplier linkOk) {
        BOOK.onEvent(event, now, enabled, linkOk);
    }

    /** Once per render frame: the search chime and clean-up. */
    static void tick(double now, boolean enabled, BooleanSupplier linkOk, TabletMotion.State state) {
        BOOK.tick(now, enabled, linkOk, state);
    }

    /** Fades everything and stops the chime (logout, acceptance restart). */
    static void stopAll(double now) {
        BOOK.stopAll(now);
    }

    /** The last plays and cuts (debugging, uiTest), oldest first. */
    public static List<TabletSoundBook.Entry> recent() {
        return BOOK.log();
    }

    private static TabletSoundInstance start(String name, TabletCue.Dir dir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        try {
            TabletSoundInstance sound = new TabletSoundInstance(event(name), dir,
                    (float) TabletAnimationModel.SFX_VOLUME, TabletSounds::clock);
            minecraft.getSoundManager().play(sound);
            return sound;
        } catch (RuntimeException exception) {
            if (!playWarned) {
                playWarned = true;
                WokInfantryMod.LOGGER.warn("[TabletAnim] sound {} not played", name, exception);
            }
            return null;
        }
    }
}
