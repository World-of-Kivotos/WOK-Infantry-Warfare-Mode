package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.screen.TacticalScreen;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scheme B's device scope: whole layout pixels on the screen's (2x) pose, undone exactly once. */
class TabletDeviceScopeTest {
    @Test
    void theStaticPictureAndAFullyLitRestingDeviceNeedNoScope() {
        TabletUnits term = TabletVectors.termUnits("640x360");
        assertSame(TacticalScreen.DeviceScope.NONE, TabletDeviceScope.of(null, null));
        assertSame(TacticalScreen.DeviceScope.NONE, TabletDeviceScope.of(null,
                TabletPath2D.termFrame(1.0D, term, true, false)), "p = 1 is the static picture");
        assertSame(TacticalScreen.DeviceScope.NONE, TabletDeviceScope.of(null,
                TabletPath2D.termFrame(1.0D, term, true, true)));
        assertTrue(TabletDeviceScope.needed(TabletPath2D.termFrame(0.3D, term, true, false)),
                "rising, dark glass");
        assertTrue(TabletDeviceScope.needed(TabletPath2D.termFrame(0.8D, term, true, false)),
                "scanning in place");
        assertTrue(TabletDeviceScope.needed(TabletPath2D.termFrame(0.7D, term, true, true)),
                "quick: lit, still sliding");
    }

    @Test
    void oneLayoutPixelAtGuiOneWithTheMinimum2xIsTwoPhysicalPixels() {
        TabletUnits term = TabletVectors.termUnits("960x720");
        assertEquals(1, term.guiScale());
        assertEquals(2, term.factor());
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(0.7D, term, true, true);
        assertTrue(frame.dy() > 0);

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.scale(term.factor(), term.factor(), 1.0F); // what TacticalScreen.render does
        List<String> drawn = new ArrayList<>();
        TabletDeviceScope scope = new TabletDeviceScope(pose, frame.dy(), () -> drawn.add("glass"));
        Matrix4f moved = pose.last().pose();
        double physicalPerLayout = term.guiScale() * term.factor();
        assertEquals(frame.dy() * physicalPerLayout, moved.m31() * term.guiScale(), 1e-4,
                "dy layout pixels = dy · S · f physical pixels");
        assertEquals(0.0F, moved.m30(), 0.0F, "never sideways");

        scope.drawEffects();
        assertEquals(List.of("glass"), drawn, "the glass is drawn once the move has ended");
        assertEquals(0.0F, pose.last().pose().m31(), 0.0F, "the move ended");
        scope.close();
        scope.close();
        pose.popPose();
        assertTrue(pose.clear(), "the scope popped exactly what it pushed");
    }

    @Test
    void closeEndsAMoveThatTheFrameNeverFinished() {
        PoseStack pose = new PoseStack();
        List<String> drawn = new ArrayList<>();
        TabletDeviceScope scope = new TabletDeviceScope(pose, 37, () -> drawn.add("glass"));
        assertFalse(pose.clear(), "moved");
        scope.close();
        assertTrue(pose.clear(), "an exception path still restores the pose");
        assertTrue(drawn.isEmpty(), "no effects on a frame that threw");
    }

    @Test
    void aDeviceAlreadyInPlaceIsNotMovedButItsGlassIsStillDrawn() {
        PoseStack pose = new PoseStack();
        List<String> drawn = new ArrayList<>();
        TabletDeviceScope scope = new TabletDeviceScope(pose, 0, () -> drawn.add("glass"));
        assertTrue(pose.clear(), "dy 0 pushes nothing");
        scope.drawEffects();
        scope.close();
        assertEquals(List.of("glass"), drawn);
        assertTrue(pose.clear());
    }
}
