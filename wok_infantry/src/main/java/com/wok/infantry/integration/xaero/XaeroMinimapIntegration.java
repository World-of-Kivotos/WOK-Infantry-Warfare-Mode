package com.wok.infantry.integration.xaero;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.map.TacticalMapTerrainRegistry;
import net.minecraftforge.fml.ModList;

/** Installs the optional Xaero minimap terrain and UI bridge without a hard class dependency. */
public final class XaeroMinimapIntegration {
    public static final String FAIRPLAY_MOD_ID = "xaerominimapfair";
    public static final String STANDARD_MOD_ID = "xaerominimap";

    private static final XaeroTerrainProvider TERRAIN_PROVIDER = new XaeroTerrainProvider();
    private static boolean installed;

    private XaeroMinimapIntegration() {
    }

    public static void install() {
        if (installed || !isXaeroLoaded()) {
            return;
        }
        installed = true;
        // JourneyMap's public tile API provides persistent explored terrain and remains the
        // preferred provider when both map mods are present. Xaero is the local-terrain fallback.
        if (!ModList.get().isLoaded("journeymap")) {
            TacticalMapTerrainRegistry.register(TERRAIN_PROVIDER);
        }
        XaeroUiPolicy.install();
        WokInfantryMod.LOGGER.info(
                "Xaero minimap compatibility initialized (terrain provider: {})",
                ModList.get().isLoaded("journeymap") ? "journeymap" : TERRAIN_PROVIDER.id());
    }

    public static void onClientTick() {
        if (installed) {
            XaeroUiPolicy.enforceMinimapHidden();
        }
    }

    public static boolean isXaeroLoaded() {
        ModList mods = ModList.get();
        return mods.isLoaded(FAIRPLAY_MOD_ID) || mods.isLoaded(STANDARD_MOD_ID);
    }
}
