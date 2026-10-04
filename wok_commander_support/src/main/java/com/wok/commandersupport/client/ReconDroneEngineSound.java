package com.wok.commandersupport.client;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.drone.ReconDroneAcoustics;
import com.wok.commandersupport.drone.ReconDroneAirframe;
import com.wok.commandersupport.drone.ReconDroneEntity;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Engine loop bound to one recon drone on the client. It plays without built-in attenuation and
 * fades linearly with the listener's distance instead (see {@link ReconDroneAcoustics}), so the
 * engine is heard for about "flight height + 64" blocks; it winds down after a shoot-down and
 * stops when the drone leaves the client level. Without Superb Warfare the wrapped event is
 * empty and nothing plays.
 */
public final class ReconDroneEngineSound extends AbstractTickableSoundInstance {
    private final ReconDroneEntity drone;

    private ReconDroneEngineSound(ReconDroneEntity drone) {
        super(CommanderSupportSounds.RECON_DRONE_ENGINE.get(), SoundSource.HOSTILE,
                SoundInstance.createUnseededRandom());
        this.drone = drone;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = false;
        follow();
    }

    /** Starts the loop for a drone that just joined the client level; never throws. */
    public static void start(ReconDroneEntity drone) {
        if (drone == null || drone.isRemoved() || drone.isSilent()) {
            return;
        }
        try {
            Minecraft.getInstance().getSoundManager().play(new ReconDroneEngineSound(drone));
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn("Recon drone {} could not start its engine sound",
                    drone.getUUID(), failure);
        }
    }

    @Override
    public boolean canStartSilent() {
        // The listener may still be out of range when the drone appears.
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !drone.isSilent();
    }

    @Override
    public void tick() {
        if (drone.isRemoved()) {
            stop();
            return;
        }
        follow();
        if (ReconDroneAcoustics.engineSilenced(drone.crashTicks(0.0F))) {
            stop();
        }
    }

    private void follow() {
        this.x = drone.getX();
        this.y = drone.getY() + ReconDroneAirframe.MODEL_CENTER_HEIGHT;
        this.z = drone.getZ();
        Vec3 listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = listener.distanceTo(new Vec3(this.x, this.y, this.z));
        double crashTicks = drone.crashTicks(0.0F);
        this.volume = ReconDroneAcoustics.engineVolume(distance,
                ReconDroneAcoustics.engineRange(drone.heightAboveTarget()), crashTicks);
        this.pitch = ReconDroneAcoustics.enginePitch(crashTicks);
    }
}
