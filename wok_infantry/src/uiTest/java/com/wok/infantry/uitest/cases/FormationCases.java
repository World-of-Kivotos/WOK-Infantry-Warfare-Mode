package com.wok.infantry.uitest.cases;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.client.screen.FormationVoteModel;
import com.wok.infantry.client.screen.TacticalConfirmDialog;
import com.wok.infantry.client.screen.TacticalList;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiFind;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.FormationFixtures;
import com.wok.infantry.uitest.fixtures.MockData;
import com.wok.infantry.uitest.fixtures.ServerFixtures;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Faction/formation selection and voting (preview surface {@code 45-formation}).
 *
 * <p>The two legacy captures of the B0 baseline stay first and keep their names: {@code wok_ui_10}
 * (320×240, joined, vote not opened, administrator) and {@code wok_ui_11} (960×720, vote open,
 * administrator); the administrator key is found by its probe tag.
 *
 * <p>The migrated cases cover the preview's states plus {@code longcaps}. Each opens the page on
 * a fixture catalog ({@link FormationFixtures}) and reaches its state through real input: faction
 * keys, list rows, the details key and the join and lock keys are clicked by their probe ids, so
 * the confirmations are the page's own. The administrator states grant the fixture player the
 * operator level first; the player states take it back, so the page shows the ordinary view. All
 * of them fail the run on any layout violation of a required tier, and additionally check:
 * <ul>
 *   <li>the page reports the expected preview state ({@code UiSurfaceInfo});</li>
 *   <li>no internal id ({@code assault}, {@code wok_commander_support:…}) in the detail (player-09);</li>
 *   <li>no half row: every text drawn under a clip lies wholly inside it;</li>
 *   <li>the state's own semantics (browsing faction not solid blue, disabled keys with a reason,
 *   dangerous lock confirmation starting on "cancel", Esc closing the page before joining …).</li>
 * </ul>
 */
public final class FormationCases {
    /** uiId of the administrator "open vote" key (the page's {@code ADMIN_OPEN_UI_ID}). */
    public static final String ADMIN_OPEN_UI_ID = FormationSelectionScreen.ADMIN_OPEN_UI_ID;
    /** uiId of the administrator "lock vote" key (the page's {@code ADMIN_LOCK_UI_ID}). */
    public static final String ADMIN_LOCK_UI_ID = FormationSelectionScreen.ADMIN_LOCK_UI_ID;
    /** Lower-case ids, e.g. a class id or a support id, must never reach the detail. */
    private static final Pattern INTERNAL_ID = Pattern.compile(
            "^[a-z0-9_]+$|[a-z0-9_]+:[a-z0-9_/]+");

    private FormationCases() {
    }

    public static List<UiCase> cases() {
        List<UiCase> cases = new ArrayList<>();
        cases.add(legacyVote());
        // Player view (operator level taken back), in preview order.
        cases.add(player("join", () -> FormationFixtures.Scenario
                .unjoined(FormationVotePhase.NOT_STARTED).build())
                .check(FormationCases::checkJoin).build());
        cases.add(player("confirm", () -> FormationFixtures.Scenario
                .unjoined(FormationVotePhase.NOT_STARTED).build())
                .steps(UiStep.click(FormationSelectionScreen.JOIN_UI_ID),
                        UiStep.until("the join confirmation", FormationCases::modalOpen))
                .check(FormationCases::checkJoinConfirm).build());
        cases.add(player("facfull", () -> FormationFixtures.Scenario
                .unjoined(FormationVotePhase.NOT_STARTED)
                .population(MockData.VIEWER_FACTION, 40).build())
                .steps(UiStep.click(FormationSelectionScreen.FACTION_UI_ID_PREFIX
                                + MockData.VIEWER_FACTION),
                        UiStep.until("the full faction browsed", context ->
                                "facfull".equals(state(context))))
                .check(FormationCases::checkFactionFull).build());
        cases.add(player("vote", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).tally(tie()).build())
                .check(FormationCases::checkVoteOpen).build());
        cases.add(player("detail", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).own(MockData.OWN_VOTE).build())
                .steps(openDetailsWhenNarrow())
                .check(FormationCases::checkDetail).build());
        cases.add(player("locked", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.LOCKED).own(MockData.OWN_VOTE)
                .locked(MockData.LOCKED_FORMATION).build())
                .check(FormationCases::checkLocked).build());
        cases.add(player("latejoin", () -> FormationFixtures.Scenario
                .unjoined(FormationVotePhase.LOCKED).locked(MockData.LOCKED_FORMATION).build())
                .check(FormationCases::checkLateJoin).build());
        cases.add(player("lateconfirm", () -> FormationFixtures.Scenario
                .unjoined(FormationVotePhase.LOCKED).locked(MockData.LOCKED_FORMATION).build())
                .steps(UiStep.click(FormationSelectionScreen.JOIN_UI_ID),
                        UiStep.until("the join confirmation", FormationCases::modalOpen))
                .check(FormationCases::checkJoinConfirm).build());
        cases.add(player("waiting", () -> null)
                .check(FormationCases::checkWaiting).build());
        // B11a: the catalog request went unanswered for 3 seconds (the server drops requests over
        // its rate limit silently): the waiting page says so and offers the retry. Only the
        // client's request clock is set; no request is sent, so no catalog can answer it.
        cases.add(player("waitover", () -> null, "waiting")
                .steps(UiStep.action(context -> ClientFormationState.catalogRequested()),
                        UiStep.until("the catalog request overdue",
                                context -> ClientFormationState.catalogOverdue()))
                .check(FormationCases::checkWaitingOverdue)
                .cleanup(context -> ClientFormationState.update(ClientFormationState.snapshot()))
                .build());
        // The viewer's vote moved from the mobile formation to the long-capability one.
        cases.add(player("longcaps", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).own(FormationFixtures.LONG_CAPS_ID)
                .tally(Map.of(FormationFixtures.DEFAULT_ID, 3, FormationFixtures.MOBILE_ID, 4,
                        FormationFixtures.CAVALRY_ID, 1, FormationFixtures.LONG_CAPS_ID, 1))
                .withLongCapabilities().build(), "detail")
                .steps(openDetailsWhenNarrow(), scrollDetailToEnd())
                .check(FormationCases::checkLongCapabilities).build());
        // Administrator view.
        cases.add(admin("pending", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.NOT_STARTED).build())
                .steps(UiStep.when(context -> !context.tight(), UiStep.hover(
                        FormationSelectionScreen.FACTION_UI_ID_PREFIX + "caesar")))
                .check(FormationCases::checkPending).build());
        cases.add(admin("full", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).own(MockData.OWN_VOTE)
                .capacity(FormationFixtures.CAVALRY_ID, 12).withReserve().build())
                .steps(selectRow(FormationFixtures.CAVALRY_ID),
                        UiStep.until("the 12-seat formation highlighted", context ->
                                "full".equals(state(context))))
                .check(FormationCases::checkCapacityShortfall).build());
        cases.add(admin("admintie", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).tally(tie()).build())
                .check(FormationCases::checkAdminTie).build());
        cases.add(admin("admin", () -> FormationFixtures.Scenario
                .joined(FormationVotePhase.OPEN).build())
                .steps(UiStep.click(ADMIN_LOCK_UI_ID),
                        UiStep.until("the lock confirmation", FormationCases::modalOpen))
                .check(FormationCases::checkLockConfirm).build());
        return List.copyOf(cases);
    }

    // ---- legacy ---------------------------------------------------------------------------------

    private static UiCase legacyVote() {
        return UiCase.builder("formation", "legacy")
                .group(UiCase.LEGACY_GROUP)
                .tiers(UiTier.T320, UiTier.T960)
                .file(UiTier.T320, "wok_ui_10_formation_vote_320x240.png")
                .file(UiTier.T960, "wok_ui_11_formation_vote_960x720.png")
                .prepare(asAdministrator())
                .open(context -> new FormationSelectionScreen(context.tier() == UiTier.T320
                        ? FormationFixtures.pendingVote() : FormationFixtures.openVote(), null))
                .check((context, capture) -> {
                    boolean compact = context.tier() == UiTier.T320;
                    boolean visible = compact
                            ? UiFind.widget(context.screen(), ADMIN_OPEN_UI_ID,
                            "key:screen.wok_infantry.formation.admin.open").isPresent()
                            : UiFind.widget(context.screen(), ADMIN_LOCK_UI_ID,
                            "key:screen.wok_infantry.formation.admin.lock").isPresent();
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

    // ---- case builders ----------------------------------------------------------------------------

    private static UiCase.Builder player(String state, Supplier<FormationSelectionSnapshot> catalog) {
        return player(state, catalog, state);
    }

    /** A player-view state; {@code expectedState} is what the page must report. */
    private static UiCase.Builder player(String state, Supplier<FormationSelectionSnapshot> catalog,
                                         String expectedState) {
        return base(state, catalog, expectedState).prepare(asPlayer());
    }

    private static UiCase.Builder admin(String state, Supplier<FormationSelectionSnapshot> catalog) {
        return base(state, catalog, state).prepare(asAdministrator());
    }

    private static UiCase.Builder base(String state, Supplier<FormationSelectionSnapshot> catalog,
                                       String expectedState) {
        return UiCase.builder(FormationSelectionScreen.SURFACE_ID, state)
                .tiers(UiTier.ALL)
                .migrated(true)
                .open(context -> {
                    FormationSelectionSnapshot snapshot = catalog.get();
                    context.caseState().put("catalog", snapshot);
                    return new FormationSelectionScreen(snapshot, null,
                            FormationSelectionScreen.Entry.TERMINAL);
                })
                .check((context, capture) -> checkCommon(context, capture, expectedState))
                .budget(300);
    }

    private static UiStep[] asAdministrator() {
        return new UiStep[]{
                UiStep.serverForPlayer("grant the formation administrator view",
                        ServerFixtures::grantOperator),
                UiStep.until("client operator permission", context ->
                        context.minecraft().player != null
                                && context.minecraft().player.hasPermissions(
                                BattleRules.ADMIN_PERMISSION_LEVEL))};
    }

    private static UiStep[] asPlayer() {
        return new UiStep[]{
                UiStep.serverForPlayer("take back the fixture operator level",
                        ServerFixtures::revokeTemporaryOperator),
                UiStep.until("the player view (no operator level)", context ->
                        context.minecraft().player != null
                                && !context.minecraft().player.hasPermissions(
                                BattleRules.ADMIN_PERMISSION_LEVEL))};
    }

    /** The preview's tie: 常规编制 and 机动部队 4 votes each. */
    private static Map<String, Integer> tie() {
        return Map.of(FormationFixtures.DEFAULT_ID, 4, FormationFixtures.MOBILE_ID, 4,
                FormationFixtures.CAVALRY_ID, 0);
    }

    // ---- steps ------------------------------------------------------------------------------------

    /** On the narrow layout, opens the detail page with its key. */
    private static UiStep openDetailsWhenNarrow() {
        return UiStep.when(context -> narrow(context), new UiStep() {
            private boolean clicked;

            @Override
            public boolean run(UiCaseContext context) throws Exception {
                if (!clicked) {
                    AbstractWidget details = UiFind.widget(context.screen(),
                            FormationSelectionScreen.DETAILS_UI_ID).orElse(null);
                    if (details == null) {
                        if (context.stepTicks() > UiStep.WAIT_LIMIT) {
                            context.fail("the narrow list page has no details key");
                        }
                        return false;
                    }
                    UiInputDriver.clickWidget(context.minecraft(), details);
                    clicked = true;
                    return false;
                }
                if (UiFind.widget(context.screen(), FormationSelectionScreen.BACK_UI_ID)
                        .isPresent()) {
                    clicked = false;
                    return true;
                }
                if (context.stepTicks() > UiStep.WAIT_LIMIT) {
                    context.fail("the details key did not open the detail page");
                }
                return false;
            }
        });
    }

    /** Turns the wheel over the detail sections until their last line is shown. */
    private static UiStep scrollDetailToEnd() {
        return context -> {
            UiLayoutFrame frame = context.freshProbe();
            if (frame == null) {
                if (context.stepTicks() > UiStep.WAIT_LIMIT) {
                    context.fail("no probe frame to scroll the detail");
                }
                return false;
            }
            UiLayoutFrame.Box detail = frame.box(FormationSelectionScreen.DETAIL_BOX);
            context.require(detail != null, "the detail panel was not drawn");
            UiLayoutFrame.Rect rect = detail.rect();
            float x = rect.centerX();
            float y = rect.top() + rect.height() * 0.6F;
            for (int turn = 0; turn < 40; turn++) {
                UiInputDriver.scrollAt(context.minecraft(), x, y, -1.0D);
            }
            return true;
        };
    }

    /**
     * Highlights {@code formationId} with a real click on its list row (scrolled into view
     * first, as the wheel would).
     */
    private static UiStep selectRow(String formationId) {
        return context -> {
            if (!(context.screen() instanceof FormationSelectionScreen screen)
                    || screen.snapshot() == null) {
                context.fail("the formation page is not open");
                return false;
            }
            TacticalList<?> list = UiFind.widget(screen, FormationSelectionScreen.LIST_UI_ID)
                    .filter(TacticalList.class::isInstance).map(TacticalList.class::cast)
                    .orElse(null);
            if (list == null) {
                if (context.stepTicks() > UiStep.WAIT_LIMIT) {
                    context.fail("the formation list is not on screen");
                }
                return false;
            }
            int index = FormationVoteModel.orderedFormations(screen.snapshot()
                    .faction(screen.snapshot().selectedFactionId().isEmpty()
                            ? MockData.VIEWER_FACTION : screen.snapshot().selectedFactionId()))
                    .stream().map(view -> view.id()).toList().indexOf(formationId);
            context.require(index >= 0, "no list row for " + formationId);
            list.ensureVisible(index);
            UiRect row = list.rowBounds(index);
            context.require(row != null && !row.isEmpty(), "row of " + formationId
                    + " cannot be shown in the list");
            UiInputDriver.clickLayout(context.minecraft(), row.left() + row.width() / 2.0D,
                    row.top() + row.height() / 2.0D);
            return true;
        };
    }

    // ---- checks -----------------------------------------------------------------------------------

    private static FormationSelectionScreen page(UiCaseContext context) {
        if (!(context.screen() instanceof FormationSelectionScreen screen)) {
            context.fail("the formation page is not open: " + context.screen());
            throw new IllegalStateException();
        }
        return screen;
    }

    private static String state(UiCaseContext context) {
        return context.screen() instanceof FormationSelectionScreen screen
                ? screen.uiStateId() : "";
    }

    private static boolean modalOpen(UiCaseContext context) {
        return context.screen() instanceof FormationSelectionScreen screen && screen.hasModal();
    }

    private static boolean narrow(UiCaseContext context) {
        return context.screen() instanceof FormationSelectionScreen screen
                && com.wok.infantry.client.screen.FormationScreenLayout.narrow(screen.width,
                screen.height);
    }

    /**
     * Every state: the expected preview state, the fixture still shown (not replaced by a server
     * catalog), no internal id in the detail and no half-cut row under any clip.
     */
    private static void checkCommon(UiCaseContext context, UiCapture.Result capture,
                                    String expectedState) {
        FormationSelectionScreen screen = page(context);
        context.require(expectedState.equals(screen.uiStateId()),
                "the page shows state " + screen.uiStateId() + " instead of " + expectedState);
        Object catalog = context.caseState().get("catalog");
        context.require(screen.snapshot() == catalog,
                "the fixture catalog was replaced while capturing");
        UiLayoutFrame frame = capture.frame();
        List<UiLayoutFrame.Box> boxes = frame.boxes();
        for (UiLayoutFrame.Text text : frame.texts()) {
            if (insideBox(boxes, text.box(), FormationSelectionScreen.DETAIL_BOX)
                    && INTERNAL_ID.matcher(text.text().trim()).find()) {
                context.fail("internal id in the formation detail: '" + text.text() + "'");
            }
            if (text.clipped() && text.rect().width() > 0.0F
                    && !text.rect().within(text.clip(), 0.01F)) {
                context.fail("half-cut line '" + text.text() + "' " + text.rect()
                        + " under clip " + text.clip());
            }
        }
        context.observe("formationState[" + context.uiCase().stateId() + "@"
                + context.tier().id() + "]=" + screen.uiStateId());
    }

    private static boolean insideBox(List<UiLayoutFrame.Box> boxes, int index, String id) {
        for (int at = index; at >= 0 && at < boxes.size(); at = boxes.get(at).parent()) {
            if (boxes.get(at).id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static UiLayoutFrame.Control control(UiCaseContext context, UiCapture.Result capture,
                                                 String uiId) {
        UiLayoutFrame.Control control = capture.frame().control(uiId);
        context.require(control != null && control.visible(), uiId + " is not on screen");
        return control;
    }

    /** Join (user reports 1 and 4): browsing is an outline, not solid blue; Esc closes. */
    private static void checkJoin(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control academy = control(context, capture,
                FormationSelectionScreen.FACTION_UI_ID_PREFIX + MockData.VIEWER_FACTION);
        context.require("BROWSING".equals(academy.state()),
                "the browsed faction is drawn as " + academy.state() + ", not as browsing");
        for (UiLayoutFrame.Control control : capture.frame().controls()) {
            if (control.uiId().startsWith(FormationSelectionScreen.FACTION_UI_ID_PREFIX)) {
                context.require(!"CURRENT".equals(control.state())
                                && !"SELECTED".equals(control.state()),
                        control.uiId() + " looks joined before joining");
            }
        }
        UiLayoutFrame.Control join = control(context, capture, FormationSelectionScreen.JOIN_UI_ID);
        context.require(join.active(), "the join key is disabled while the faction has room");
        // User report 4: Esc closes the page even before joining.
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ESCAPE, 0);
        context.require(!(context.screen() instanceof FormationSelectionScreen),
                "Esc did not close the page before joining");
        context.observe("formationEscBeforeJoin[" + context.tier().id() + "]=closed");
    }

    private static void checkJoinConfirm(UiCaseContext context, UiCapture.Result capture) {
        FormationSelectionScreen screen = page(context);
        context.require(screen.modal() instanceof TacticalConfirmDialog dialog
                        && !dialog.danger(),
                "joining must ask an ordinary (not dangerous) confirmation");
        context.require(capture.frame().box("modal.confirm") != null,
                "the confirmation card was not drawn");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ESCAPE, 0);
        context.require(!screen.hasModal() && context.screen() == screen,
                "Esc must cancel the confirmation and keep the page");
    }

    private static void checkFactionFull(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control join = control(context, capture, FormationSelectionScreen.JOIN_UI_ID);
        context.require(!join.active() && !join.disabledReason().isBlank(),
                "the join key of a full faction must be disabled with its reason");
    }

    private static void checkVoteOpen(UiCaseContext context, UiCapture.Result capture) {
        if (narrow(context)) {
            return;
        }
        UiLayoutFrame.Control vote = control(context, capture, FormationSelectionScreen.VOTE_UI_ID);
        context.require(vote.active(), "the vote key is disabled during an open vote");
    }

    private static void checkDetail(UiCaseContext context, UiCapture.Result capture) {
        context.require(capture.frame().box(FormationSelectionScreen.DETAIL_BOX) != null,
                "the formation detail was not drawn");
    }

    private static void checkLocked(UiCaseContext context, UiCapture.Result capture) {
        if (narrow(context)) {
            return;
        }
        UiLayoutFrame.Control deploy = control(context, capture,
                FormationSelectionScreen.VOTE_UI_ID);
        context.require(deploy.active(), "the locked page must offer the deployment key");
    }

    private static void checkLateJoin(UiCaseContext context, UiCapture.Result capture) {
        FormationSelectionScreen screen = page(context);
        context.require(screen.snapshot().faction(MockData.VIEWER_FACTION).votePhase()
                == FormationVotePhase.LOCKED, "the late-join fixture is not locked");
        UiLayoutFrame.Control join = control(context, capture, FormationSelectionScreen.JOIN_UI_ID);
        context.require(join.active(), "joining a locked faction with room must stay possible");
    }

    private static void checkWaiting(UiCaseContext context, UiCapture.Result capture) {
        control(context, capture, FormationSelectionScreen.RETRY_UI_ID);
        context.require(capture.frame().box(FormationSelectionScreen.WAITING_BOX) != null,
                "the waiting panel was not drawn");
    }

    /** B11a timeout state: the overdue heading or step is on screen, with the retry key. */
    private static void checkWaitingOverdue(UiCaseContext context, UiCapture.Result capture) {
        checkWaiting(context, capture);
        List<String> overdue = List.of(
                I18n.get("screen.wok_infantry.formation.waiting.heading_overdue"),
                I18n.get("screen.wok_infantry.formation.step.sync_overdue"),
                I18n.get("screen.wok_infantry.formation.step.sync_overdue_short"));
        context.require(capture.frame().texts().stream().anyMatch(text ->
                        overdue.contains(text.text()) || text.truncated()
                                && overdue.contains(text.fullText())),
                "the waiting page does not say that the catalog did not arrive");
        context.observe("formationCatalogOverdue[" + context.tier().id() + "]=shown");
    }

    /** beta.7: the five allow-listed supports are named, never shown as ids. */
    private static void checkLongCapabilities(UiCaseContext context, UiCapture.Result capture) {
        List<String> names = MockData.SUPPORT_LABELS.stream()
                .filter(label -> MockData.LONG_CAPS_SUPPORTS.contains(label.id()))
                .map(MockData.SupportLabelData::fallbackName).toList();
        long shown = names.stream().filter(name -> capture.frame().texts().stream()
                .anyMatch(text -> text.text().equals(name)
                        || text.truncated() && text.fullText().equals(name))).count();
        context.require(shown > 0, "no allow-listed support name reached the detail");
        context.observe("formationLongCapabilities[" + context.tier().id() + "]=" + shown + "/"
                + names.size());
    }

    private static void checkPending(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control open = control(context, capture, ADMIN_OPEN_UI_ID);
        context.require(open.active(), "the administrator cannot open the vote");
        if (!context.tight()) {
            context.require(capture.frame().box("tooltip") != null,
                    "hovering the other faction must explain why it cannot be chosen");
        }
    }

    private static void checkCapacityShortfall(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control lock = control(context, capture, ADMIN_LOCK_UI_ID);
        context.require(!lock.active() && !lock.disabledReason().isBlank(),
                "locking a formation smaller than the faction must be disabled with its reason");
    }

    private static void checkAdminTie(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control lock = control(context, capture, ADMIN_LOCK_UI_ID);
        context.require(lock.active(), "the administrator cannot lock one of the tied formations");
    }

    /** The lock confirmation is dangerous and starts on "cancel" (Enter never locks). */
    private static void checkLockConfirm(UiCaseContext context, UiCapture.Result capture) {
        FormationSelectionScreen screen = page(context);
        context.require(screen.modal() instanceof TacticalConfirmDialog dialog && dialog.danger()
                        && !dialog.confirmFocused(),
                "locking must ask a dangerous confirmation with the focus on cancel");
        context.require(capture.frame().control(TacticalConfirmDialog.CONFIRM_UI_ID) != null,
                "the lock confirmation has no confirm key");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ESCAPE, 0);
        context.require(!screen.hasModal(), "Esc did not cancel the lock confirmation");
    }
}
