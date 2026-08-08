package com.wok.trauma.client;

import com.wok.trauma.registry.ModEffects;
import com.wok.trauma.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;

public final class ConcussionTinnitusSound extends AbstractTickableSoundInstance {
    private int age;

    public ConcussionTinnitusSound() {
        super(ModSounds.CONCUSSION_TINNITUS.get(),
                SoundSource.PLAYERS, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0F;
        this.pitch = 1.0F;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            stop();
            return;
        }

        MobEffectInstance concussion =
                minecraft.player.getEffect(ModEffects.CONCUSSION.get());
        if (concussion == null) {
            stop();
            return;
        }

        age++;
        float fadeIn = Mth.clamp(age / 20.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(concussion.getDuration() / 60.0F, 0.0F, 1.0F);
        float pulse = 0.94F + 0.06F * Mth.sin(age * 0.12F);
        this.volume = 0.11F * fadeIn * fadeOut * pulse;
    }
}
