package com.wok.commandersupport.recon;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.adapter.SupportIntelPublisher;
import com.wok.infantry.support.adapter.SupportProvider;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Six scans at five-second intervals; the final marker lease expires at the 30-second boundary.
 *
 * <p>Failures go through {@link ReconFailurePolicy}: a first scan that publishes nothing returns
 * the cooldown. A requester who left the accepted faction mid-mission makes the step publish
 * nothing instead of failing; the scheduler cancels the mission before its next step.</p>
 */
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
        if (context == null) {
            throw SupportSpawnException.endMission("侦察卫星任务上下文缺失");
        }
        try {
            scanAndPublish(context);
        } catch (Exception | LinkageError failure) {
            // Exception also covers SupportSpawnException; VirtualMachineError is never caught.
            throw ReconFailurePolicy.classify(context.stepIndex(), failure);
        }
    }

    private static void scanAndPublish(SupportSpawnContext context) throws SupportSpawnException {
        if (context.owner() instanceof ServerPlayer owner) {
            BattleService battle = BattleService.get(owner).orElse(null);
            // A missing battle service is reported by the scan with a player-visible reason.
            if (battle != null && requesterLeftMissionFaction(context.faction(),
                    battle.factionOf(owner.getUUID()).orElse(null))) {
                WokCommanderSupportMod.LOGGER.info(
                        "Recon satellite {} skipped scan {}: requester left faction {}",
                        context.callId(), context.stepIndex(), context.faction().id());
                return;
            }
        }
        List<SupportIntelContact> contacts = ReconSatelliteScanner.scan(context);
        SupportIntelPublisher.publish(context, contacts, CONTACT_TTL_TICKS);
    }

    /** Intel belongs to the accepted faction; an unassigned requester has left it as well. */
    static boolean requesterLeftMissionFaction(Faction missionFaction, Faction requesterFaction) {
        return missionFaction == null || requesterFaction != missionFaction;
    }
}
