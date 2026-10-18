package com.wok.downed.client;

import com.wok.downed.config.DownedConfig;
import com.wok.downed.registry.DownedSounds;
import com.wok.downed.state.DownedService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The tinnitus loop a casualty hears in their own head.
 *
 * <p>One looping instance per downed episode, played relative to the listener on the player
 * category so the "Players" volume slider governs it and no other client hears it. Volume eases
 * in and out so entering or leaving the downed state does not click.
 */
public final class DownedTinnitus {
    private static final int FADE_TICKS = 14;
    private static Loop loop;

    /** Called every GUI frame while the local player is down; starts the loop on first call. */
    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || !DownedService.isDowned(player)
                || !DownedConfig.TINNITUS_ENABLED.get()
                || DownedConfig.TINNITUS_VOLUME.get() <= 0.0D) {
            stop(minecraft);
            return;
        }
        if (loop == null || loop.isStopped()) {
            loop = new Loop(DownedConfig.TINNITUS_VOLUME.get().floatValue());
            minecraft.getSoundManager().play(loop);
        }
    }

    /** Fades the loop out; safe to call repeatedly and when nothing is playing. */
    public static void stop(Minecraft minecraft) {
        if (loop != null && !loop.isStopped()) {
            loop.fadeOut();
            loop = null;
        }
    }

    /** Test seam: the current instance, if any. */
    static Loop current() {
        return loop;
    }

    static final class Loop extends AbstractTickableSoundInstance {
        private final float targetVolume;
        private float current;
        private boolean fadingOut;

        Loop(float targetVolume) {
            super(DownedSounds.TINNITUS.get(), SoundSource.PLAYERS,
                    SoundInstance.createUnseededRandom());
            this.targetVolume = targetVolume;
            this.current = 0.0F;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.looping = true;
            this.delay = 0;
        }

        void fadeOut() {
            fadingOut = true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                current -= targetVolume / FADE_TICKS;
                if (current <= 0.0F) {
                    current = 0.0F;
                    volume = 0.0F;
                    stop();
                    return;
                }
            } else if (current < targetVolume) {
                current = Math.min(targetVolume, current + targetVolume / FADE_TICKS);
            }
            volume = current;
        }
    }

    private DownedTinnitus() {
    }
}
