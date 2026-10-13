package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalBoardSliderTest {
    private static final double EPS = 1.0E-9D;

    // ---- pure layout ----------------------------------------------------------------------------

    @Test
    void tallSliderWritesTextAboveTheTrack() {
        TacticalBoardSlider.Layout layout = TacticalBoardSlider.layout(10, 20, 210, 40, 40, 24);

        assertFalse(layout.compact());
        assertEquals(23, layout.textY());
        assertEquals(14, layout.labelX());
        assertEquals(162, layout.labelRoom(), "width minus value minus 14");
        assertEquals(182, layout.valueX(), "value right-aligned 4px from the edge");
        assertEquals(24, layout.valueRoom());
        assertEquals(15, layout.trackLeft());
        assertEquals(205, layout.trackRight());
        assertEquals(34, layout.trackY(), "2px track 6px above the bottom");
    }

    @Test
    void lowSliderPutsLabelTrackAndValueOnOneLine() {
        TacticalBoardSlider.Layout layout = TacticalBoardSlider.layout(0, 0, 200, 14, 40, 24);

        assertTrue(layout.compact());
        assertEquals(3, layout.textY());
        assertEquals(6, layout.trackY());
        assertEquals(50, layout.trackLeft(), "label width + 6 after the 4px padding");
        assertEquals(42, layout.labelRoom());
        assertEquals(166, layout.trackRight(), "value width + 10 from the right edge");
        assertEquals(172, layout.valueX());
    }

    @Test
    void oneLineLabelTakesAtMost45PercentAndNarrowSlidersDropTextBeforeTheTrack() {
        TacticalBoardSlider.Layout capped = TacticalBoardSlider.layout(0, 0, 100, 14, 80, 20);
        assertEquals(49, capped.trackLeft());
        assertEquals(41, capped.labelRoom(), "the label is ellipsized, not the track");
        assertTrue(capped.trackRight() - capped.trackLeft() >= TacticalBoardSlider.MIN_TRACK);

        // The 42px map icon-size slider at 320×240: first the label, then the value text go.
        TacticalBoardSlider.Layout narrow = TacticalBoardSlider.layout(0, 0, 42, 14, 40, 24);
        assertFalse(narrow.labelShown());
        assertFalse(narrow.valueShown());
        assertEquals(5, narrow.trackLeft());
        assertEquals(37, narrow.trackRight());

        TacticalBoardSlider.Layout valueOnly = TacticalBoardSlider.layout(0, 0, 60, 14, 40, 24);
        assertFalse(valueOnly.labelShown(), "label dropped first");
        assertTrue(valueOnly.valueShown());
        assertEquals(5, valueOnly.trackLeft());
        assertEquals(26, valueOnly.trackRight());

        TacticalBoardSlider.Layout unlabelled = TacticalBoardSlider.layout(0, 0, 100, 14, 0, 20);
        assertEquals(5, unlabelled.trackLeft());
        assertEquals(0, unlabelled.labelRoom());
    }

    @Test
    void pointerMapsOntoTheDrawnTrackSoBothEndsAreReachable() {
        TacticalBoardSlider.Layout layout = TacticalBoardSlider.layout(10, 20, 210, 40, 40, 24);

        assertEquals(0.0D, layout.positionAt(0), EPS);
        assertEquals(0.0D, layout.positionAt(15), EPS);
        assertEquals(0.5D, layout.positionAt(110), EPS);
        assertEquals(1.0D, layout.positionAt(205), EPS);
        assertEquals(1.0D, layout.positionAt(400), EPS);
        assertEquals(205, layout.knobX(1.0D), "the knob sits exactly at the track end");
        assertEquals(110, layout.knobX(0.5D));
        assertEquals(15, layout.knobX(Double.NaN));
        // TacticalScreen divides GUI coordinates by the 2x factor before they reach the slider.
        assertEquals(1.0D, layout.positionAt(UiScale.toLayout(205.0D * 2, 2)), EPS);
    }

    @Test
    void arrowKeyStepsAndShiftTakesFive() {
        assertEquals(-1, TacticalBoardSlider.keyStep(GLFW.GLFW_KEY_LEFT, 0));
        assertEquals(1, TacticalBoardSlider.keyStep(GLFW.GLFW_KEY_RIGHT, 0));
        assertEquals(5, TacticalBoardSlider.keyStep(GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_MOD_SHIFT));
        assertEquals(-5, TacticalBoardSlider.keyStep(GLFW.GLFW_KEY_LEFT,
                GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_NUM_LOCK));
        assertEquals(0, TacticalBoardSlider.keyStep(GLFW.GLFW_KEY_UP, 0),
                "↑/↓ stay with focus navigation");
    }

    // ---- widget ---------------------------------------------------------------------------------

    @Test
    void legacyConstructorKeepsSnappingPercentTextAndNoCallback() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = new TacticalBoardSlider(0, 0, 100, 20,
                Component.literal("后坐力"), 0.25D, 2.0D, 0.05D, 0.333D, values::add);

        assertEquals(0.35D, slider.selectedValue(), EPS);
        assertEquals("后坐力  35%", slider.getMessage().getString());
        assertTrue(values.isEmpty(), "construction reports nothing");
        assertFalse(slider.touched());

        TacticalBoardSlider unlabelled = new TacticalBoardSlider(0, 0, 42, 20, Component.empty(),
                0.75D, 1.75D, 0.05D, 1.0D, ignored -> { });
        assertEquals("100%", unlabelled.getMessage().getString());
    }

    @Test
    void keysMoveWholeStepsAndReportOnlyChanges() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = new TacticalBoardSlider(0, 0, 100, 20,
                Component.literal("散布"), 0.25D, 2.0D, 0.05D, 0.35D, values::add);

        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(0.4D, slider.selectedValue(), EPS);
        assertTrue(slider.touched());
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_MOD_SHIFT));
        assertEquals(0.65D, slider.selectedValue(), EPS);
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_END, 0, 0));
        assertEquals(2.0D, slider.selectedValue(), EPS);
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0), "the key stays taken at the end");
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0));
        assertEquals(List.of(0.4D, 0.65D, 2.0D, 0.25D), values, "no report without a change");
    }

    @Test
    void setSelectedValueReportsButIsNotAPlayerChange() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = new TacticalBoardSlider(0, 0, 100, 20, Component.empty(),
                0.25D, 2.0D, 0.05D, 0.5D, values::add);

        slider.setSelectedValue(1.0D);
        assertEquals(List.of(1.0D), values, "reset buttons report like before");
        assertFalse(slider.touched());
        assertEquals("100%", slider.getMessage().getString());
    }

    @Test
    void detentSliderAlwaysRestsOnAWholePackage() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = TacticalBoardSlider.builder(Component.literal("补给"),
                        values::add)
                .bounds(0, 0, 100, 20)
                .scale(TacticalSliderScale.roundDetents(30, 100))
                .value(70)
                .formatter(TacticalBoardSlider.INTEGER)
                .detentTicks(true)
                .build();

        assertEquals(60, slider.selectedInt(), "70 rounds snap to two packages");
        assertEquals("补给  60", slider.getMessage().getString());
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(90, slider.selectedInt());
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_MOD_SHIFT));
        assertEquals(30, slider.selectedInt());
        assertEquals(List.of(90.0D, 30.0D), values);
    }

    @Test
    void clickAndDragFollowTheDrawnTrack() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = TacticalBoardSlider.builder(Component.empty(), values::add)
                .bounds(0, 0, 100, 20)
                .scale(TacticalSliderScale.roundDetents(30, 300))
                .value(30)
                .build();
        // Without a client there is no font: the two-line track spans x = 5 … 95.
        assertEquals(new TacticalBoardSlider.Layout(false, 3, 4, 0, 96, 0, 5, 95, 14),
                slider.currentLayout());

        slider.onClick(50, 10);
        assertEquals(180, slider.selectedInt(), "middle of the track = detent 6 of 10");
        slider.onClick(1000, 10);
        assertEquals(300, slider.selectedInt(), "past the right end = maximum");
        slider.onClick(-50, 10);
        assertEquals(30, slider.selectedInt(), "past the left end = minimum");
        slider.onClick(5, 10);
        assertEquals(List.of(180.0D, 300.0D, 30.0D), values);
    }

    @Test
    void emptyOrInactiveSliderIgnoresInput() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider empty = TacticalBoardSlider.builder(Component.literal("补给"),
                        values::add)
                .bounds(0, 0, 100, 20).scale(TacticalSliderScale.roundDetents(30, 20)).build();
        assertFalse(empty.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        empty.onClick(90, 10);
        assertEquals(0, empty.selectedInt());

        TacticalBoardSlider inactive = new TacticalBoardSlider(0, 0, 100, 20, Component.empty(),
                0.0D, 1.0D, 0.1D, 0.5D, values::add);
        inactive.active = false;
        assertFalse(inactive.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        inactive.onClick(90, 10);
        assertEquals(0.5D, inactive.selectedValue(), EPS);
        assertTrue(values.isEmpty());
    }

    @Test
    void newScaleKeepsTheChosenAmountAsFarAsItCan() {
        List<Double> values = new ArrayList<>();
        TacticalBoardSlider slider = TacticalBoardSlider.builder(Component.empty(), values::add)
                .scale(TacticalSliderScale.roundDetents(30, 100)).value(60).build();

        assertEquals(60.0D, slider.setScale(TacticalSliderScale.roundDetents(30, 300), 60), EPS);
        assertEquals(60, slider.selectedInt());
        assertEquals(30.0D, slider.setScale(TacticalSliderScale.roundDetents(30, 45), 60), EPS,
                "fewer packages left: the nearest one that exists");
        assertTrue(values.isEmpty(), "the screen changed the scale, not the player");
    }
}
