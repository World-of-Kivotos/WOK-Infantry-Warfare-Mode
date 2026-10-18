package com.wok.commandersupport.registry;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.drone.ReconDroneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entities owned by this module. Only vanilla and Forge types appear here, so the module stays
 * installable without Superb Warfare or Create Big Cannons.
 */
public final class CommanderSupportEntities {
    public static final String RECON_DRONE_PATH = "recon_drone";
    /** Hit box of the airframe: wide enough to be hit around the fuselage and inner wing. */
    public static final float RECON_DRONE_WIDTH = 4.0F;
    public static final float RECON_DRONE_HEIGHT = 1.2F;
    /**
     * Tracking range in chunks (16 * 16 = 256 blocks, still capped by the server view distance),
     * far beyond the 5-chunk default, so the high orbit stays visible around the target.
     */
    public static final int RECON_DRONE_TRACKING_RANGE_CHUNKS = 16;
    public static final int RECON_DRONE_UPDATE_INTERVAL_TICKS = 2;

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
                    WokCommanderSupportMod.MOD_ID);

    /**
     * The drone never outlives its mission: it is not saved with chunks ({@code noSave}), so a
     * restart or an unloaded orbit drops it instead of leaving an orphan in the world.
     */
    public static final RegistryObject<EntityType<ReconDroneEntity>> RECON_DRONE =
            ENTITY_TYPES.register(RECON_DRONE_PATH,
                    () -> EntityType.Builder.<ReconDroneEntity>of(ReconDroneEntity::new,
                                    MobCategory.MISC)
                            .sized(RECON_DRONE_WIDTH, RECON_DRONE_HEIGHT)
                            .noSave()
                            .fireImmune()
                            .clientTrackingRange(RECON_DRONE_TRACKING_RANGE_CHUNKS)
                            .updateInterval(RECON_DRONE_UPDATE_INTERVAL_TICKS)
                            .build(RECON_DRONE_PATH));

    private CommanderSupportEntities() {
    }
}
