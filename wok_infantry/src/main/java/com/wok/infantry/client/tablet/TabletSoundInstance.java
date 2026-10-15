package com.wok.infantry.client.tablet;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.function.DoubleSupplier;

/**
 * One of the twelve tablet sounds playing on this client (IMPL_PLAN D5, review leftover ③): every
 * tablet sound is this one class. Player category ({@link SoundSource#PLAYERS}, the "Players"
 * volume slider), relative to the listener at the origin, no distance attenuation — it follows the
 * player and only they hear it. Volume {@value TabletAnimationModel#SFX_VOLUME} (as vanilla's UI
 * click); the sound engine multiplies in the event's balanced volume from sounds.json. Pitch 1, no
 * random pitch, no loop, no delay.
 *
 * <p>{@link #fadeOut} starts the cut: the engine ticks the sound 20 times a second and each tick
 * takes its volume from a {@link TabletSoundFade.Fader} until the sound stops.
 */
public final class TabletSoundInstance extends AbstractTickableSoundInstance {
    private final DoubleSupplier clock;
    private final TabletCue.Dir dir;
    private final TabletSoundFade.Fader fader;

    /**
     * @param event  the sound ({@code wok_infantry:tablet.<name>})
     * @param dir    the direction it belongs to (what a cut fades)
     * @param volume start volume (normally {@link TabletAnimationModel#SFX_VOLUME})
     * @param clock  milliseconds, the same clock the animation runs on
     */
    public TabletSoundInstance(SoundEvent event, TabletCue.Dir dir, float volume, DoubleSupplier clock) {
        super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.clock = clock;
        this.dir = dir;
        this.fader = new TabletSoundFade.Fader(volume);
        this.volume = volume;
        this.pitch = 1.0F;
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = false;
        this.delay = 0;
    }

    /** Starts the fade-out (once; later calls keep the first cut time). */
    public void fadeOut(double now) {
        fader.fadeOut(now);
    }

    @Override
    public void tick() {
        if (isStopped() || !fader.fading()) {
            return;
        }
        if (fader.tick(clock.getAsDouble())) {
            volume = 0.0F;
            stop();
            return;
        }
        volume = (float) fader.volume();
    }

    public TabletCue.Dir dir() {
        return dir;
    }

    /** Whether a cut is fading it. */
    public boolean fading() {
        return fader.fading();
    }
}
