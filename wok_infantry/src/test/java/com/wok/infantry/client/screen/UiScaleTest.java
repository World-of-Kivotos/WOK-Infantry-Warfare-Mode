package com.wok.infantry.client.screen;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UiScaleTest {
    @ParameterizedTest(name = "gui {0}, {1}x{2}, enabled {3} -> {4}")
    @CsvSource({
            // GUI 1 windows of at least 640x480 get 2x
            "1.0, 960, 720, true, 2",
            "1.0, 640, 480, true, 2",
            "1.0, 1920, 1080, true, 2",
            // too small for a 320x240 layout at 2x
            "1.0, 639, 480, true, 1",
            "1.0, 640, 479, true, 1",
            // other GUI scales are untouched (the user's 1920x1008 at GUI 3 is 640x336)
            "3.0, 640, 336, true, 1",
            "2.0, 960, 540, true, 1",
            "4.0, 480, 270, true, 1",
            // the client option switches it off
            "1.0, 960, 720, false, 1"
    })
    void factorRule(double guiScale, int width, int height, boolean enabled, int expected) {
        assertEquals(expected, UiScale.factorFor(guiScale, width, height, enabled));
    }

    @Test
    void layoutSizeOfTheGui1Window() {
        assertEquals(480, UiScale.layoutSize(960, 2));
        assertEquals(360, UiScale.layoutSize(720, 2));
        assertEquals(480, UiScale.layoutSize(961, 2), "odd pixels are dropped, never rounded up");
        assertEquals(640, UiScale.layoutSize(640, 1));
    }

    @Test
    void mouseCoordinatesAreDividedByTheFactor() {
        assertEquals(100.5D, UiScale.toLayout(201.0D, 2));
        assertEquals(49.5D, UiScale.toLayout(99.0D, 2));
        assertEquals(100, UiScale.toLayout(201, 2), "render mouse rounds down");
        assertEquals(-1, UiScale.toLayout(-1, 2), "pointer left of the window stays outside");
        assertEquals(201.0D, UiScale.toLayout(201.0D, 1), "factor 1 passes through");
        assertEquals(402, UiScale.toGui(201, 2));
    }

    @Test
    void scissorFollowsThePoseScaleAndTranslation() {
        Matrix4f identity = new Matrix4f();
        assertArrayEquals(new int[]{10, 20, 110, 70},
                UiScale.scissorBounds(identity, 10, 20, 110, 70));

        Matrix4f doubled = new Matrix4f().scale(2.0F, 2.0F, 1.0F);
        assertArrayEquals(new int[]{20, 40, 220, 140},
                UiScale.scissorBounds(doubled, 10, 20, 110, 70));

        Matrix4f shifted = new Matrix4f().scale(2.0F, 2.0F, 1.0F).translate(5.0F, 0.0F, 400.0F);
        assertArrayEquals(new int[]{30, 40, 230, 140},
                UiScale.scissorBounds(shifted, 10, 20, 110, 70));
    }

    @Test
    void fractionalPosesRoundOutwards() {
        Matrix4f pose = new Matrix4f().translate(0.5F, 0.25F, 0.0F);

        assertArrayEquals(new int[]{10, 20, 111, 71},
                UiScale.scissorBounds(pose, 10, 20, 110, 70));
    }
}
