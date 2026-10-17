package com.wok.infantry.uitest.cases;

import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.client.screen.BattleTab;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalButtonStyle;
import com.wok.infantry.client.screen.TacticalConfirmDialog;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.UiTestWidgets;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiDeviceChecks;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.MockData;
import com.wok.infantry.uitest.fixtures.SquadFixtures;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle terminal: squad, class and deployment pages (preview surface {@code 20-squad}, 方案 5.1
 * 档 3). The fourteen preview states, plus the deployment page with 2, 8, 15 and 16 points, all on
 * the client fixture of {@link SquadFixtures}; the states that need a selection reach it through
 * real input (call-sign row, roster row, kick key) clicked by probe id. Every state is migrated:
 * any layout violation on a required tier fails the run, and additionally:
 * <ul>
 *   <li>the screen reports the expected preview state;</li>
 *   <li>no text drawn under a clip is cut through (half rows);</li>
 *   <li>kick asks a red (dangerous) confirmation that starts on "cancel" and ignores Enter;</li>
 *   <li>the point list's page count equals the real number of pages, the title says the same;</li>
 *   <li>before the lock no control creates or joins a squad, picks a class or deploys.</li>
 * </ul>
 *
 * <p>Liveries (0.5.0-beta.3): the states above are the Academy viewer's, so the terminal is
 * painted navy ({@code -academy}). Six states are repeated from the Caesar side ({@code -caesar}:
 * squads, kick, classes, deployment, active, loading) on the same fixture seen from the red side.
 * Neither is pinned: the terminal resolves its livery from the fixture (battle side, or the joined
 * catalog faction while loading), and the device checks ({@code UiDeviceChecks}) compare it with
 * the case's livery. The Caesar kick also checks the red confirmation over the red selection.
 */
public final class SquadCases {
    private SquadCases() {
    }

    public static List<UiCase> cases() {
        List<UiCase> cases = new ArrayList<>();
        SquadFixtures.Scenario leader = SquadFixtures.Scenario.ready(SquadFixtures.Role.LEADER);
        SquadFixtures.Scenario alone = SquadFixtures.Scenario.ready(SquadFixtures.Role.NONE);
        SquadFixtures.Scenario active = leader.withPhase(DeploymentPhase.ACTIVE, 58);
        SquadFixtures.Scenario loading = SquadFixtures.Scenario.of(SquadFixtures.Stage.LOADING);
        cases.add(squad("squads", leader, BattleTab.SQUADS).build());
        cases.add(squad("other", leader, BattleTab.SQUADS)
                .steps(viewSquad(SquadCallsign.BRAVO),
                        UiStep.until("the Bravo roster", context -> "other".equals(state(context))))
                .build());
        cases.add(squad("nosquad", alone, BattleTab.SQUADS).build());
        cases.add(squad("kick", leader, BattleTab.SQUADS)
                .steps(UiStep.click(SquadPageIds.ROSTER + "/1"),
                        UiStep.waitTicks(2),
                        UiStep.click(SquadScreen.ACTION_UI_ID_PREFIX + "kick"),
                        UiStep.until("the kick confirmation", context -> "kick".equals(
                                state(context))))
                .check(SquadCases::checkKick).build());
        cases.add(squad("classes", leader, BattleTab.CLASSES).build());
        cases.add(squad("classesnosquad", alone, BattleTab.CLASSES).build());
        cases.add(squad("classesactive", active, BattleTab.CLASSES).build());
        cases.add(squad("deployment", leader, BattleTab.DEPLOYMENT)
                .check((context, capture) -> checkPages(context, capture, 3)).build());
        cases.add(squad("active", active, BattleTab.DEPLOYMENT)
                .check((context, capture) -> checkPages(context, capture, 3)).build());
        cases.add(squad("loading", loading, BattleTab.SQUADS)
                .check(SquadCases::checkWaitingLink).build());
        cases.add(squad("votewait", SquadFixtures.Scenario.of(SquadFixtures.Stage.VOTE_WAIT),
                BattleTab.SQUADS).check(SquadCases::checkNothingBeforeLock).build());
        cases.add(squad("vote", SquadFixtures.Scenario.of(SquadFixtures.Stage.VOTE_OPEN),
                BattleTab.SQUADS).check(SquadCases::checkNothingBeforeLock).build());
        cases.add(squad("voteclasses", SquadFixtures.Scenario.of(SquadFixtures.Stage.VOTE_OPEN),
                BattleTab.CLASSES).check(SquadCases::checkNothingBeforeLock).build());
        cases.add(squad("votedeploy", SquadFixtures.Scenario.of(SquadFixtures.Stage.VOTE_OPEN),
                BattleTab.DEPLOYMENT).check(SquadCases::checkNothingBeforeLock).build());
        for (int points : new int[]{2, 8, 15, 16}) {
            cases.add(squad("points" + points, leader.withPoints(points), BattleTab.DEPLOYMENT,
                    "deployment").check((context, capture) -> checkPages(context, capture,
                    points)).build());
        }
        // The Caesar viewer (red side): same squads, classes and points in the red livery.
        MockData.Side caesar = MockData.Side.CAESAR;
        cases.add(squad("squads", leader.withSide(caesar), BattleTab.SQUADS).build());
        cases.add(squad("kick", leader.withSide(caesar), BattleTab.SQUADS)
                .steps(UiStep.click(SquadPageIds.ROSTER + "/1"),
                        UiStep.waitTicks(2),
                        UiStep.click(SquadScreen.ACTION_UI_ID_PREFIX + "kick"),
                        UiStep.until("the kick confirmation", context -> "kick".equals(
                                state(context))))
                .check(SquadCases::checkRedOverRed)
                .check(SquadCases::checkKick).build());
        cases.add(squad("classes", leader.withSide(caesar), BattleTab.CLASSES).build());
        cases.add(squad("deployment", leader.withSide(caesar), BattleTab.DEPLOYMENT)
                .check((context, capture) -> checkPages(context, capture, 3)).build());
        cases.add(squad("active", active.withSide(caesar), BattleTab.DEPLOYMENT)
                .check((context, capture) -> checkPages(context, capture, 3)).build());
        cases.add(squad("loading", loading.withSide(caesar), BattleTab.SQUADS)
                .check(SquadCases::checkWaitingLink).build());
        return List.copyOf(cases);
    }

    /** Probe ids of the squad page's lists (they live in a package-private painter). */
    private static final class SquadPageIds {
        static final String STRIP = "squad.strip/";
        static final String LIST = "squad.list";
        static final String ROSTER = "squad.roster";
        static final String POINTS = "squad.points";
    }

    private static UiCase.Builder squad(String state, SquadFixtures.Scenario scenario,
                                        BattleTab page) {
        return squad(state, scenario, page, state);
    }

    /**
     * A terminal state on {@code scenario}; its livery is the scenario side's (blue → academy,
     * red → caesar), resolved by the terminal itself, never pinned.
     */
    private static UiCase.Builder squad(String state, SquadFixtures.Scenario scenario,
                                        BattleTab page, String expectedState) {
        return UiCase.builder(SquadScreen.SURFACE_ID, state)
                .tiers(UiTier.ALL)
                .migrated(true)
                .livery(TacticalLivery.forSide(scenario.side().faction()))
                .open(context -> {
                    SquadFixtures.start(scenario);
                    return SquadScreen.forTab(null, page);
                })
                .steps(UiStep.until("the squad fixture", context -> SquadFixtures.applied()),
                        UiStep.waitTicks(2))
                .check((context, capture) -> checkCommon(context, capture, expectedState))
                .cleanup(context -> SquadFixtures.stop())
                .budget(300);
    }

    /** Opens {@code callsign}'s roster with a click on its strip cell or list row. */
    private static UiStep viewSquad(SquadCallsign callsign) {
        return context -> {
            UiLayoutFrame frame = context.freshProbe();
            if (frame == null) {
                if (context.stepTicks() > UiStep.WAIT_LIMIT) {
                    context.fail("no probe frame to find the call sign " + callsign.id());
                }
                return false;
            }
            UiLayoutFrame.Control control = frame.control(SquadPageIds.STRIP + callsign.id());
            if (control == null) {
                control = frame.control(SquadPageIds.LIST + "/" + callsign.ordinal());
            }
            context.require(control != null && control.visible(),
                    "no strip cell or list row for " + callsign.id());
            UiInputDriver.click(context.minecraft(), control);
            return true;
        };
    }

    private static SquadScreen screen(UiCaseContext context) {
        if (!(context.screen() instanceof SquadScreen screen)) {
            context.fail("the squad terminal is not open: " + context.screen());
            throw new IllegalStateException();
        }
        return screen;
    }

    private static String state(UiCaseContext context) {
        return context.screen() instanceof SquadScreen screen ? screen.uiStateId() : "";
    }

    /** Every state: the expected preview state and no half-cut line under a clip. */
    private static void checkCommon(UiCaseContext context, UiCapture.Result capture,
                                    String expectedState) {
        SquadScreen screen = screen(context);
        context.require(expectedState.equals(screen.uiStateId()),
                "the terminal shows state " + screen.uiStateId() + " instead of " + expectedState);
        context.require(SquadFixtures.applied(), "the squad fixture was replaced while capturing");
        for (UiLayoutFrame.Text text : capture.frame().texts()) {
            if (text.clipped() && text.rect().width() > 0.0F
                    && !text.rect().within(text.clip(), 0.01F)) {
                context.fail("half-cut line '" + text.text() + "' " + text.rect()
                        + " under clip " + text.clip());
            }
        }
        context.observe("squadState[" + context.uiCase().stateKey() + "@" + context.tier().id()
                + "]=" + screen.uiStateId());
    }

    /**
     * Kick asks a dangerous confirmation, focused on cancel; with the focus moved to the red key
     * Enter and Space still keep it open, Esc cancels.
     */
    private static void checkKick(UiCaseContext context, UiCapture.Result capture) {
        SquadScreen screen = screen(context);
        context.require(screen.modal() instanceof TacticalConfirmDialog dialog && dialog.danger()
                        && !dialog.confirmFocused(),
                "kicking must ask a red confirmation with the focus on cancel");
        context.require(capture.frame().control(TacticalConfirmDialog.CONFIRM_UI_ID) != null,
                "the kick confirmation has no confirm key");
        // Tab moves the focus to the red confirm key; Enter and Space still do not press it.
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_TAB, 0);
        context.require(screen.modal() instanceof TacticalConfirmDialog dialog
                && dialog.confirmFocused(), "Tab must move the focus to the confirm key");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ENTER, 0);
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_SPACE, 0);
        context.require(screen.hasModal() && !((TacticalConfirmDialog) screen.modal()).closed(),
                "Enter or Space must not confirm a kick");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ESCAPE, 0);
        context.require(!screen.hasModal() && context.screen() == screen,
                "Esc must cancel the kick and keep the terminal");
    }

    /**
     * Caesar kick (plan 6): the red, armed confirmation key sits over the red (crimson) roster
     * selection; they are told apart by shape, not by colour alone — the confirm key carries the
     * hazard stripes (device checks), the selected row its light bar.
     */
    private static void checkRedOverRed(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        UiLayoutFrame.Control confirm = frame.control(TacticalConfirmDialog.CONFIRM_UI_ID);
        context.require(confirm != null && TacticalButtonStyle.State.DANGER_ARMED.name()
                        .equals(confirm.state()),
                "the kick confirmation key is not the armed red key: "
                        + (confirm == null ? "missing" : confirm.state()));
        UiLayoutFrame.Control row = frame.control(SquadPageIds.ROSTER + "/1");
        context.require(row != null && "SELECTED".equals(row.state()),
                "the kicked member's roster row is not shown selected under the confirmation: "
                        + (row == null ? "missing" : row.state()));
        context.observe("squadRedOverRed[" + context.tier().id() + "]=confirm "
                + confirm.state() + " over row " + row.state() + " livery="
                + UiDeviceChecks.shellNote(frame).get("livery"));
    }

    /** Without a battle snapshot the link is still waiting: amber LED, two bars. */
    private static void checkWaitingLink(UiCaseContext context, UiCapture.Result capture) {
        String link = UiDeviceChecks.shellNote(capture.frame()).get("link");
        context.require(TacticalBoardChrome.LinkState.WAIT.name().equals(link),
                "the loading terminal shows link " + link + " instead of WAIT");
    }

    /**
     * The point list's page count is the real number of pages for {@code points} points, every
     * row of the first page is on screen, and the title meta names the same count.
     */
    private static void checkPages(UiCaseContext context, UiCapture.Result capture, int points) {
        SquadScreen screen = screen(context);
        int[] page = UiTestWidgets.squadPointPage(screen);
        context.require(page != null, "the deployment page has no point list");
        int rows = page[3] - page[2];
        context.require(rows > 0, "the point list shows no row");
        int expected = (points + rows - 1) / rows;
        context.require(page[1] == expected, "the point list says " + page[1]
                + " page(s) for " + points + " points at " + rows + " per page (expected "
                + expected + ")");
        long shown = capture.frame().controls().stream().filter(control ->
                control.uiId().startsWith(SquadPageIds.POINTS + "/")).count();
        context.require(shown == rows, "the list draws " + shown + " rows, the page has " + rows);
        if (page[1] > 1) {
            String label = "1/" + page[1];
            context.require(capture.frame().texts().stream().anyMatch(text ->
                            text.text().contains(label) || text.fullText().contains(label)),
                    "the title does not name page " + label);
        }
        context.observe("squadPointPages[" + context.uiCase().stateKey() + "@"
                + context.tier().id() + "]=" + rows + "x" + page[1]);
    }

    /** Before the lock no control creates or joins a squad, picks a class or deploys. */
    private static void checkNothingBeforeLock(UiCaseContext context, UiCapture.Result capture) {
        for (UiLayoutFrame.Control control : capture.frame().controls()) {
            String id = control.uiId();
            boolean squadControl = id.startsWith(SquadScreen.ACTION_UI_ID_PREFIX)
                    || id.startsWith(SquadPageIds.STRIP) || id.startsWith(SquadPageIds.LIST + "/")
                    || id.startsWith("squad.classes/") || id.startsWith(SquadPageIds.POINTS + "/");
            context.require(!squadControl || !control.active(),
                    id + " is clickable before the formation is locked");
        }
        context.require(capture.frame().control(SquadScreen.VOTE_UI_ID) != null,
                "the vote block offers no way to the formation tab");
    }
}
