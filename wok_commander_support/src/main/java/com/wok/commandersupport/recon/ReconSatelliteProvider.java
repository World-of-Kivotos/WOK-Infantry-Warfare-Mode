package com.wok.commandersupport.recon;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.adapter.SupportIntelPublisher;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Six scans at five-second intervals; the final marker lease expires at the 30-second boundary. */
public final class ReconSatelliteProvider implements SupportProvider {
    public static final int CONTACT_TTL_TICKS = 110;

    @Override
    public ResourceLocation supportId() {
        return WokCommanderSupportMod.RECON_SATELLITE_ID;
    }

    @Override
    public ProviderAvailability availability() {
        return ProviderAvailability.present();
    }

    @Override
    public void executeStep(SupportSpawnContext context) throws SupportSpawnException {
        List<SupportIntelContact> contacts = ReconSatelliteScanner.scan(context);
        SupportIntelPublisher.publish(context, contacts, CONTACT_TTL_TICKS);
    }
}
