package com.wok.infantry.uitest.cases;

import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.client.screen.AmmoSupplyScreen;
import com.wok.infantry.client.screen.UiTestWidgets;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Ammunition supply board (preview surface {@code 50-ammo}). The board is rebuilt by the
 * loadout/supply batch (B9); until then this holds the player-01 regression only: on 320×240 a
 * small crate's remaining points and their meter must not sit under the section header (the B1
 * fix stacks them with one cursor). The screen is not migrated, so its layout violations are only
 * reported; the geometry check fails the run.
 */
public final class AmmoCases {
    private AmmoCases() {
    }

    public static List<UiCase> cases() {
        return List.of(UiCase.builder("ammo", "small320")
                .tiers(UiTier.T320, UiTier.T480)
                .open(context -> new AmmoSupplyScreen(smallCrate()))
                .check(AmmoCases::checkPointsClear)
                .build());
    }

    /** A small crate with two guns (names are item names, as the server sends them). */
    static AmmoSupplyView smallCrate() {
        // A small crate holds 100 points (the board's own title says so).
        return new AmmoSupplyView(AmmoSupplyView.Target.smallCrate(BlockPos.ZERO), 100, 84,
                List.of(new AmmoSupplyView.GunOption(0, Items.CROSSBOW.getDescription(),
                                Items.ARROW.getDescription(), "minecraft:arrow", 2, 12, 64, 52),
                        new AmmoSupplyView.GunOption(1, Items.BOW.getDescription(),
                                Items.SPECTRAL_ARROW.getDescription(), "minecraft:spectral_arrow",
                                3, 4, 32, 28)),
                List.of());
    }

    private static void checkPointsClear(UiCaseContext context, UiCapture.Result capture)
            throws ReflectiveOperationException {
        if (!(context.screen() instanceof AmmoSupplyScreen screen)) {
            context.fail("the supply board is not open: " + context.screen());
            return;
        }
        Map<String, int[]> geometry = UiTestWidgets.ammoSupplyGeometry(screen);
        int[] points = geometry.get("points");
        int[] meter = geometry.get("meter");
        int[] section = geometry.get("section");
        int[] panel = geometry.get("panel");
        context.require(!overlaps(points, section), "the remaining points " + text(points)
                + " sit under the section header " + text(section));
        context.require(!overlaps(meter, section), "the points meter " + text(meter)
                + " sits under the section header " + text(section));
        context.require(points[1] >= panel[1] && points[3] <= section[1],
                "the remaining points " + text(points) + " are not between the panel top and"
                        + " the section header " + text(section));
        context.observe("ammoSmallCratePoints[" + context.tier().id() + "]=" + text(points)
                + " section=" + text(section));
    }

    private static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1];
    }

    private static String text(int[] rect) {
        return Arrays.toString(rect);
    }
}
