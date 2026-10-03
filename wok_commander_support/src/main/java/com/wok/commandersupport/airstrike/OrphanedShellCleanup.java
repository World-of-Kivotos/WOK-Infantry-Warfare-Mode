package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Drops released airstrike shells that come back from chunk storage.
 *
 * <p>No mission outlives the chunk its shell is in: the core cancels a mission whose footprint
 * unloads, and nothing survives a restart. A cancelled mission removes its shell through
 * {@code abandon}, but a shell inside an unloaded chunk cannot be reached; it is saved with the
 * chunk instead. Any marked shell read back from disk is therefore orphaned and would otherwise
 * sit at the target as an inert prop or a stray bomb, so it is refused on load.</p>
 */
public final class OrphanedShellCleanup {
    private OrphanedShellCleanup() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only the entity's own saved data is read here: the level may still be loading.
        if (!event.loadedFromDisk() || event.getLevel().isClientSide()
                || !isSupportShell(event.getEntity().getPersistentData())) {
            return;
        }
        event.setCanceled(true);
        WokCommanderSupportMod.LOGGER.info(
                "Dropped orphaned commander-support shell {} loaded from chunk storage",
                event.getEntity().getUUID());
    }

    /** True for a JDAM or Paveway shell released by this module. */
    static boolean isSupportShell(CompoundTag persistentData) {
        return persistentData != null
                && (persistentData.getBoolean(MillenniumJdamProvider.JDAM_MARKER)
                || persistentData.getBoolean(F16PavewayProvider.MARKER));
    }
}
