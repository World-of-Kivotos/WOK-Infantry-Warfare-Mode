package com.wok.commandersupport.artillery;

import com.wok.commandersupport.airstrike.MillenniumJdamProvider;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;

/**
 * Barrage sound events. All of them are this module's own fixed-range events wrapping Superb
 * Warfare or Create Big Cannons sounds by id, so nothing here links against either mod.
 */
final class ArtillerySounds {
    private ArtillerySounds() {
    }

    static RegistryObject<SoundEvent> report(ArtilleryProfile.Caliber caliber) {
        return switch (caliber) {
            case HOWITZER_155 -> CommanderSupportSounds.HOWITZER_155_REPORT;
            case HOWITZER_105 -> CommanderSupportSounds.HOWITZER_105_REPORT;
        };
    }

    /** Create Big Cannons' shell whistle when that mod is present, Superb Warfare's otherwise. */
    static RegistryObject<SoundEvent> incoming() {
        return ModList.get().isLoaded(MillenniumJdamProvider.CBC_MOD_ID)
                ? CommanderSupportSounds.ARTILLERY_INCOMING_CBC
                : CommanderSupportSounds.ARTILLERY_INCOMING_SBW;
    }

    /** Heard by everyone in range, friend and foe alike. */
    static void play(ServerLevel level, double x, double y, double z,
                     RegistryObject<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, x, y, z, sound.get(), SoundSource.HOSTILE, volume, pitch);
    }
}
