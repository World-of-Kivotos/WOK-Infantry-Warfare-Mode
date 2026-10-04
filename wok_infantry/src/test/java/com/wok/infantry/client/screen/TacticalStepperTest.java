package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalStepperTest {
    private static final double EPS = 1.0E-9D;

    private final List<Double> values = new ArrayList<>();

    /** 0–3 in whole steps at (0, 0, 60, 14). */
    private TacticalStepper stepper(double value) {
        return new TacticalStepper(0, 0, 60, 14, Component.literal("数量"),
                TacticalSliderScale.linear(0.0D, 3.0D, 1.0D), value, TacticalBoardSlider.INTEGER,
                values::add);
    }

    @Test
    void keysAreAsTallAsTheStepperAndFrameTheValueWell() {
        TacticalStepper.Layout layout = TacticalStepper.layout(new UiRect(0, 0, 60, 14));
        assertEquals(new UiRect(0, 0, 14, 14), layout.minus());
        assertEquals(new UiRect(46, 0, 60, 14), layout.plus());
        assertEquals(new UiRect(15, 0, 45, 14), layout.value(), "1px gap to each key");

        TacticalStepper.Layout tall = TacticalStepper.layout(new UiRect(0, 0, 80, 24));
        assertEquals(TacticalStepper.MAX_KEY, tall.minus().width(), "keys never exceed 16px");

        TacticalStepper.Layout narrow = TacticalStepper.layout(new UiRect(0, 0, 30, 20));
        assertEquals(10, narrow.minus().width(), "at most a third of the width");
        assertEquals(new UiRect(11, 0, 19, 20), narrow.value());
    }

    @Test
    void hitTellsTheKeysFromTheValue() {
        UiRect bounds = new UiRect(0, 0, 60, 14);
        assertEquals(-1, TacticalStepper.hit(bounds, 5, 5));
        assertEquals(1, TacticalStepper.hit(bounds, 50, 5));
        assertEquals(0, TacticalStepper.hit(bounds, 30, 5));
        assertEquals(0, TacticalStepper.hit(bounds, 14, 5), "the 1px gap belongs to no key");
        assertEquals(0, TacticalStepper.hit(bounds, 5, 20));
    }

    @Test
    void stepsStopAtBothLimitsAndTheKeysTurnDisabled() {
        TacticalStepper stepper = stepper(0.0D);

        assertFalse(stepper.canDecrease(), "− is disabled at the minimum");
        assertTrue(stepper.canIncrease());
        assertFalse(stepper.step(-1));
        assertTrue(stepper.step(1));
        assertEquals(1, stepper.selectedInt());
        assertTrue(stepper.step(10));
        assertEquals(3, stepper.selectedInt(), "clamped to the maximum");
        assertFalse(stepper.canIncrease(), "+ is disabled at the maximum");
        assertTrue(stepper.canDecrease());
        assertFalse(stepper.step(1));
        assertEquals(List.of(1.0D, 3.0D), values);
        assertTrue(stepper.touched());
        assertEquals("数量  3", stepper.getMessage().getString());
    }

    @Test
    void mouseKeysAndWheelAdjustTheValue() {
        TacticalStepper stepper = stepper(3.0D);

        assertTrue(stepper.mouseClicked(5, 5, 0));
        assertEquals(2, stepper.selectedInt(), "− key");
        assertTrue(stepper.mouseClicked(30, 5, 0), "the well takes the focus");
        assertEquals(2, stepper.selectedInt());
        assertTrue(stepper.mouseScrolled(30, 5, 1));
        assertEquals(3, stepper.selectedInt(), "wheel up = +");
        assertFalse(stepper.mouseScrolled(100, 100, 1), "not over the stepper");
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_MINUS, 0, 0));
        assertEquals(1, stepper.selectedInt());
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_EQUAL, 0, 0));
        assertEquals(2, stepper.selectedInt());
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_MOD_SHIFT));
        assertEquals(3, stepper.selectedInt(), "Shift: 5 steps, clamped");
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0));
        assertEquals(0, stepper.selectedInt());
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_END, 0, 0));
        assertEquals(3, stepper.selectedInt());
        assertFalse(stepper.keyPressed(GLFW.GLFW_KEY_UP, 0, 0), "↑/↓ stay with focus navigation");
        assertEquals(-5, TacticalStepper.keyStep(GLFW.GLFW_KEY_KP_SUBTRACT, GLFW.GLFW_MOD_SHIFT));
        assertEquals(1, TacticalStepper.keyStep(GLFW.GLFW_KEY_KP_ADD, 0));
    }

    @Test
    void typedPlusAndMinusAreOneStepEachEvenThoughPlusNeedsShift() {
        // US layout: "+" is Shift + "=", so Shift must not turn it into a coarse step.
        assertEquals(1, TacticalStepper.keyStep(GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_MOD_SHIFT));
        assertEquals(1, TacticalStepper.keyStep(GLFW.GLFW_KEY_EQUAL, 0));
        assertEquals(-1, TacticalStepper.keyStep(GLFW.GLFW_KEY_MINUS, GLFW.GLFW_MOD_SHIFT));
        assertEquals(-1, TacticalStepper.keyStep(GLFW.GLFW_KEY_MINUS, 0));
        assertEquals(5, TacticalStepper.keyStep(GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_MOD_SHIFT),
                "coarse steps stay on the arrows");

        TacticalStepper stepper = stepper(0.0D);
        assertTrue(stepper.keyPressed(GLFW.GLFW_KEY_EQUAL, 0, GLFW.GLFW_MOD_SHIFT));
        assertEquals(1, stepper.selectedInt(), "typing + moves one step");
    }

    @Test
    void inactiveStepperIgnoresEverything() {
        TacticalStepper stepper = stepper(1.0D);
        stepper.active = false;

        assertFalse(stepper.step(1));
        assertFalse(stepper.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertFalse(stepper.mouseClicked(50, 5, 0));
        assertFalse(stepper.mouseScrolled(30, 5, 1));
        assertEquals(1, stepper.selectedInt());
        assertTrue(values.isEmpty());
    }

    @Test
    void detentStepperSharesTheSliderScale() {
        TacticalStepper stepper = new TacticalStepper(0, 0, 60, 14, Component.empty(),
                TacticalSliderScale.roundDetents(30, 100), 45, TacticalBoardSlider.INTEGER,
                values::add);

        assertEquals(60, stepper.selectedInt(), "45 snaps to the nearest package");
        stepper.setSelectedValue(10);
        assertEquals(30, stepper.selectedInt());
        assertFalse(stepper.canDecrease());
        assertEquals(0.0D, stepper.setScale(TacticalSliderScale.roundDetents(30, 20), 30), EPS,
                "no whole package left");
        assertFalse(stepper.canIncrease(), "an empty scale has no steps");
        assertFalse(stepper.step(1));
        assertEquals(List.of(30.0D), values);
    }
}
