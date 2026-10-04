package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;

import java.util.List;

/**
 * Battle HUD (preview surface {@code 10-hud}), captured with no screen open.
 *
 * <p>Until the HUD batch (B6) rebuilds the overlays this is one report-only baseline of the
 * current HUD in the acceptance world on the two rule tiers; it also keeps the runner's HUD capture
 * path (probe between {@code RenderGuiEvent.Pre} and {@code Post}) exercised. B6 adds its states
 * here (battle, chat, downed, vote phases, lock notice, eight-member roster) as migrated cases.
 */
public final class HudCases {
    private HudCases() {
    }

    public static List<UiCase> cases() {
        return List.of(UiCase.builder("hud", "legacy")
                .tiers(UiTier.T320, UiTier.T960)
                .open(context -> null)
                .steps(UiStep.until("the battle snapshot", context ->
                        ClientBattleState.snapshot() != null))
                .check((context, capture) -> context.observe("hudLegacyCapture["
                        + context.tier().id() + "]=boxes:" + capture.frame().boxes().size()
                        + " texts:" + capture.frame().texts().size()))
                .build());
    }
}
