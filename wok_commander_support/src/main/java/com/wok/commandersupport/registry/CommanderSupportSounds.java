package com.wok.commandersupport.registry;

import com.wok.commandersupport.WokCommanderSupportMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events of this module.
 *
 * <p>The range passed to {@link SoundEvent#createFixedRangeEvent} is the server broadcast radius:
 * only players inside it receive the packet, whatever volume the server plays it at. How far the
 * client then hears it is {@code max(volume, 1) x attenuation_distance}, where the volume is the
 * play volume multiplied by the sounds.json volume (see SoundAssetContractTest). The artillery and
 * drone events wrap Superb Warfare / Create Big Cannons events by registry id
 * ({@code "type": "event"}); a wrapper keeps the wrapped sound's attenuation distance and only
 * multiplies volume and pitch. No third-party audio is bundled, and without that mod the wrapped
 * event simply stays silent.</p>
 */
public final class CommanderSupportSounds {
    /**
     * Howitzer reports are heard from the virtual battery (up to 180 blocks behind the target)
     * out past the far edge of the danger area, by friend and foe alike.
     */
    public static final float HOWITZER_REPORT_RANGE = 384.0F;
    /**
     * Both incoming-shell whistles are played about 20 blocks above an impact point and only
     * need to reach the danger area and its surroundings, whichever mod supplies the clip.
     */
    public static final float ARTILLERY_INCOMING_RANGE = 128.0F;
    /** The engine loop is started by the client; this range only bounds server-side playback. */
    public static final float RECON_DRONE_ENGINE_RANGE = 160.0F;
    /** Shoot-down in the air and the crash on the ground, heard across the scanned area. */
    public static final float RECON_DRONE_DESTROYED_RANGE = 256.0F;

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    WokCommanderSupportMod.MOD_ID);

    public static final RegistryObject<SoundEvent> JDAM_F15_APPROACH = register(
            "jdam_f15_approach", 256.0F);
    public static final RegistryObject<SoundEvent> JDAM_BOMB_TAIL = register(
            "jdam_bomb_tail", 256.0F);
    /** Reuses the F-15E field recordings at a higher pitch (see sounds.json). */
    public static final RegistryObject<SoundEvent> PAVEWAY_F16_APPROACH = register(
            "paveway_f16_approach", 256.0F);
    public static final RegistryObject<SoundEvent> PAVEWAY_BOMB_TAIL = register(
            "paveway_bomb_tail", 256.0F);
    /** Wraps {@code superbwarfare:plz_05_veryfar}. */
    public static final RegistryObject<SoundEvent> HOWITZER_155_REPORT = register(
            "howitzer_155_report", HOWITZER_REPORT_RANGE);
    /** Wraps {@code superbwarfare:mk_42_veryfar}. */
    public static final RegistryObject<SoundEvent> HOWITZER_105_REPORT = register(
            "howitzer_105_report", HOWITZER_REPORT_RANGE);
    /** Wraps {@code createbigcannons:shell_flying}; used only while Create Big Cannons is loaded. */
    public static final RegistryObject<SoundEvent> ARTILLERY_INCOMING_CBC = register(
            "artillery_incoming_cbc", ARTILLERY_INCOMING_RANGE);
    /** Wraps {@code superbwarfare:shell_fly}; the fallback without Create Big Cannons. */
    public static final RegistryObject<SoundEvent> ARTILLERY_INCOMING_SBW = register(
            "artillery_incoming_sbw", ARTILLERY_INCOMING_RANGE);
    /** Wraps {@code superbwarfare:ju_87_engine}; looped by the client while a drone is tracked. */
    public static final RegistryObject<SoundEvent> RECON_DRONE_ENGINE = register(
            "recon_drone_engine", RECON_DRONE_ENGINE_RANGE);
    /** Wraps {@code superbwarfare:explosion_air}; played on the shoot-down and on the crash. */
    public static final RegistryObject<SoundEvent> RECON_DRONE_DESTROYED = register(
            "recon_drone_destroyed", RECON_DRONE_DESTROYED_RANGE);

    private CommanderSupportSounds() {
    }

    private static RegistryObject<SoundEvent> register(String path, float range) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                WokCommanderSupportMod.MOD_ID, path);
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createFixedRangeEvent(id, range));
    }
}
