package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.screen.BattleTab;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.tablet.TabletAnimationController;
import com.wok.infantry.client.tablet.TabletMode;
import com.wok.infantry.client.tablet.TabletMotion;
import com.wok.infantry.client.tablet.TabletPath;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.SquadFixtures;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Smoke captures of scheme A (0.5.0-beta.4 batch B3, group {@code tabletsmoke}): the animation is
 * switched on for these cases only ({@link TabletAnimationController#overrideForAcceptance}) and
 * frozen at a few progress values, opening and closing, on a whole-pixel tier (320×240 at GUI 3)
 * and the smooth-downscale tiers. They show the 3D tablet and hands, the page laid into its
 * rectangle with the wake effects, and the C1 close's replay; they are report-only (no layout or
 * device checks) and only fail when scheme A did not run or the hand pass never reached it.
 * Batch B5 replaces them with the full frozen-frame acceptance.
 */
public final class TabletSmokeCases {
    private static final String SURFACE = "tabletsmoke";

    private TabletSmokeCases() {
    }

    public static List<UiCase> cases() {
        List<UiCase> cases = new ArrayList<>();
        SquadFixtures.Scenario leader = SquadFixtures.Scenario.ready(SquadFixtures.Role.LEADER);
        cases.add(open("open0200", leader, 0.20D, UiTier.T320));
        cases.add(open("open0450", leader, 0.45D, UiTier.T320, UiTier.T960));
        // The hand-over at READ: the 3D face just before 0.61 and the 2D device at 0.61.
        cases.add(open("open0609", leader, 0.6099D, UiTier.T320, UiTier.T960));
        cases.add(open("open0610", leader, 0.61D, UiTier.T320, UiTier.T960));
        cases.add(open("open0660", leader, 0.66D, UiTier.T320, UiTier.T960));
        cases.add(open("open0870", leader, 0.87D, UiTier.T320, UiTier.T960, UiTier.T480));
        cases.add(close("close0870", leader, 0.87D, UiTier.T320, UiTier.T960));
        cases.add(close("close0660", leader, 0.66D, UiTier.T320));
        cases.add(close("close0550", leader, 0.55D, UiTier.T320));
        cases.add(map("map0450", 0.45D, UiTier.T320));
        cases.add(map("map0870", 0.87D, UiTier.T320, UiTier.T960));
        // The test world holds the player invisible before deployment (DeploymentService
        // holdPlayer), and an invisible player raises the tablet without hands: these show them.
        cases.add(visible(open("hands0250", leader, 0.25D, (ItemStack) null, UiTier.T320, UiTier.T960)));
        cases.add(visible(open("hands0450", leader, 0.45D, (ItemStack) null, UiTier.T320, UiTier.T960)));
        cases.add(visible(open("hands0700", leader, 0.70D, (ItemStack) null, UiTier.T320)));
        cases.add(visible(open("item0200", leader, 0.20D, new ItemStack(Items.COMPASS),
                UiTier.T320)));
        cases.add(visible(closeBuilder("hands0500", leader, 0.50D, UiTier.T320)));
        return List.copyOf(cases);
    }

    /** {@code builder} with the client player made visible for the capture, then restored. */
    private static UiCase visible(UiCase.Builder builder) {
        return builder
                .steps(UiStep.action(context -> {
                    if (context.minecraft().player != null) {
                        context.minecraft().player.setInvisible(false);
                    }
                }), UiStep.waitTicks(2))
                .cleanup(context -> {
                    if (context.minecraft().player != null) {
                        context.minecraft().player.setInvisible(true);
                    }
                })
                .build();
    }

    private static TabletAnimationController.AcceptanceOverride freeze(double p, boolean close) {
        return new TabletAnimationController.AcceptanceOverride(TabletMode.FULL, false, null, p,
                close, null);
    }

    /** Opens the squad page from the world, frozen at {@code p}. */
    private static UiCase open(String state, SquadFixtures.Scenario scenario, double p,
                               UiTier... tiers) {
        return open(state, scenario, p, null, tiers).build();
    }

    /**
     * Opens the squad page from the world, frozen at {@code p}; {@code held} (client side only)
     * goes into the selected hotbar slot first, so the tablet comes out of that hand.
     */
    private static UiCase.Builder open(String state, SquadFixtures.Scenario scenario, double p,
                                       ItemStack held, UiTier... tiers) {
        // The fixture first, in the world (applying it may move the player, which ends an
        // animation as a change of level would), then the tablet from the world.
        return base(state, tiers)
                .open(context -> {
                    SquadFixtures.start(scenario);
                    if (held != null && context.minecraft().player != null) {
                        Inventory inventory = context.minecraft().player.getInventory();
                        inventory.setItem(inventory.selected, held.copy());
                    }
                    return null;
                })
                .steps(UiStep.until("the squad fixture", context -> SquadFixtures.applied()),
                        UiStep.waitTicks(5),
                        UiStep.action(context -> {
                            TabletAnimationController.overrideForAcceptance(freeze(p, false));
                            context.minecraft().setScreen(SquadScreen.forTab(null, BattleTab.SQUADS));
                        }),
                        UiStep.waitTicks(2))
                .check((context, capture) -> checkA(context, capture, TabletMotion.State.OPENING, p))
                .cleanup(context -> {
                    SquadFixtures.stop();
                    if (held != null && context.minecraft().player != null) {
                        Inventory inventory = context.minecraft().player.getInventory();
                        inventory.setItem(inventory.selected, ItemStack.EMPTY);
                    }
                });
    }

    /** Opens the squad page, lets it be shown, puts it away and freezes the close at {@code p}. */
    private static UiCase close(String state, SquadFixtures.Scenario scenario, double p,
                                UiTier... tiers) {
        return closeBuilder(state, scenario, p, tiers).build();
    }

    private static UiCase.Builder closeBuilder(String state, SquadFixtures.Scenario scenario,
                                               double p, UiTier... tiers) {
        return base(state, tiers)
                .open(context -> {
                    SquadFixtures.start(scenario);
                    return null;
                })
                .steps(UiStep.until("the squad fixture", context -> SquadFixtures.applied()),
                        UiStep.waitTicks(5),
                        UiStep.action(context -> {
                            TabletAnimationController.overrideForAcceptance(freeze(p, true));
                            context.minecraft().setScreen(SquadScreen.forTab(null, BattleTab.SQUADS));
                        }),
                        UiStep.until("the tablet shown", context ->
                                TabletAnimationController.debugSnapshot().state()
                                        == TabletMotion.State.SHOWN),
                        UiStep.waitTicks(3),
                        UiStep.action(context -> context.minecraft().setScreen(null)),
                        UiStep.waitTicks(2))
                .check((context, capture) -> checkA(context, capture, TabletMotion.State.CLOSING, p))
                .cleanup(context -> SquadFixtures.stop());
    }

    /** Opens the tactical map from the world, frozen at {@code p}. */
    private static UiCase map(String state, double p, UiTier... tiers) {
        return base(state, tiers)
                .open(context -> {
                    TabletAnimationController.overrideForAcceptance(freeze(p, false));
                    context.minecraft().setScreen(null);
                    Screen screen = new TacticalMapScreen(null);
                    return screen;
                })
                .steps(UiStep.waitTicks(4))
                .check((context, capture) -> checkA(context, capture, TabletMotion.State.OPENING, p))
                .build();
    }

    private static UiCase.Builder base(String state, UiTier... tiers) {
        return UiCase.builder(SURFACE, state)
                .group(SURFACE)
                .tiers(tiers)
                .migrated(false)
                .hudCapture(true)
                .budget(400)
                .cleanup(context -> TabletAnimationController.clearAcceptanceOverride());
    }

    private static void checkA(UiCaseContext context, UiCapture.Result capture,
                               TabletMotion.State state, double p) {
        TabletAnimationController.DebugSnapshot snap = TabletAnimationController.debugSnapshot();
        context.observe(String.format(Locale.ROOT,
                "tabletSmoke[%s@%s]=state %s path %s p %.3f hand %s handPass %s capture %s a %s",
                context.uiCase().stateKey(), context.tier().id(), snap.state(), snap.path(), snap.p(),
                snap.hand(), snap.handPass(), snap.capture(), snap.a() == null ? "-"
                        : "tablet " + snap.a().tablet().draw() + " hands " + snap.a().hands().draw()
                        + " ui " + snap.a().ui().draw() + "/" + snap.a().ui().smooth()
                        + " cap " + snap.a().capture().draw()));
        context.require(snap.path() == TabletPath.A3D, "scheme A did not run: path " + snap.path());
        context.require(snap.state() == state, "state " + snap.state() + " instead of " + state);
        context.require(Math.abs(snap.p() - p) < 1e-6, "p " + snap.p() + " instead of " + p);
        context.require(snap.handPass(), "the hand pass never reached the animation");
        context.require(snap.a() != null, "no scheme-A frame");
    }
}
