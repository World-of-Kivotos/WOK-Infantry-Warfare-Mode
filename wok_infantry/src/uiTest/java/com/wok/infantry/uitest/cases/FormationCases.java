package com.wok.infantry.uitest.cases;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiFind;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.FormationFixtures;
import com.wok.infantry.uitest.fixtures.ServerFixtures;

import java.util.List;

/**
 * Faction/formation selection and voting (preview surface {@code 45-formation}).
 *
 * <p>Until the formation batch (B5) rebuilds the screen, this holds the two legacy captures of the
 * B0 baseline: {@code wok_ui_10} (320×240, joined, vote not opened) and {@code wok_ui_11}
 * (960×720, vote open). Both need the administrator controls, so the case grants the fixture
 * player operator rights (revoked by the harness cleanup) and checks the administrator key by
 * uiId first ({@code formation.admin.open} / {@code formation.admin.lock}), then by its label —
 * the current Chinese literal or the label the B5 design gives it. B5 adds its new states here.
 */
public final class FormationCases {
    /** uiId the formation screen should give its administrator "open vote" key. */
    public static final String ADMIN_OPEN_UI_ID = "formation.admin.open";
    /** uiId the formation screen should give its administrator "lock vote" key. */
    public static final String ADMIN_LOCK_UI_ID = "formation.admin.lock";

    private FormationCases() {
    }

    public static List<UiCase> cases() {
        return List.of(legacyVote());
    }

    private static UiCase legacyVote() {
        return UiCase.builder("formation", "legacy")
                .group("legacy")
                .tiers(UiTier.T320, UiTier.T960)
                .file(UiTier.T320, "wok_ui_10_formation_vote_320x240.png")
                .file(UiTier.T960, "wok_ui_11_formation_vote_960x720.png")
                .prepare(UiStep.serverForPlayer("grant the formation administrator view",
                                ServerFixtures::grantOperator),
                        UiStep.until("client operator permission", context ->
                                context.minecraft().player != null
                                        && context.minecraft().player.hasPermissions(
                                        BattleRules.ADMIN_PERMISSION_LEVEL)))
                .open(context -> new FormationSelectionScreen(context.tier() == UiTier.T320
                        ? FormationFixtures.pendingVote() : FormationFixtures.openVote(), null))
                .check((context, capture) -> {
                    boolean compact = context.tier() == UiTier.T320;
                    boolean visible = compact
                            ? UiFind.widget(context.screen(), ADMIN_OPEN_UI_ID,
                            "管理员开启投票", "开启编制投票").isPresent()
                            : UiFind.widget(context.screen(), ADMIN_LOCK_UI_ID,
                            "管理员锁定", "锁定投票结果").isPresent();
                    context.require(visible, "Formation vote screen omitted the administrator "
                            + (compact ? "open" : "lock") + " control");
                    String size = compact ? "compact" : "large";
                    context.observe(size + "FormationLogicalSize="
                            + capture.guiWidth() + "x" + capture.guiHeight());
                    context.observe(size + "FormationAdministratorLockVisible=true");
                })
                .budget(400)
                .build();
    }
}
