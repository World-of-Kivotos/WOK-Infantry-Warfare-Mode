package com.wok.infantry.integration.tacz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WeaponTuningTest {
    @Test
    void defaultTuningIsValidAndRecognized() {
        assertTrue(WeaponTuning.DEFAULT.valid());
        assertTrue(WeaponTuning.DEFAULT.defaultValues());
    }

    @Test
    void editorRangeAcceptsFifteenTimesScaleForEveryProperty() {
        assertTrue(new WeaponTuning(15.0F, 15.0F, 15.0F, 15.0F).valid());
    }

    @Test
    void editorRangeRejectsNonFiniteAndOutOfRangeValues() {
        assertFalse(new WeaponTuning(Float.NaN, 1.0F, 1.0F, 1.0F).valid());
        assertFalse(new WeaponTuning(1.0F, 0.24F, 1.0F, 1.0F).valid());
        assertFalse(new WeaponTuning(15.01F, 1.0F, 1.0F, 1.0F).valid());
        assertFalse(new WeaponTuning(1.0F, 15.01F, 1.0F, 1.0F).valid());
        assertFalse(new WeaponTuning(1.0F, 1.0F, 15.01F, 1.0F).valid());
        assertFalse(new WeaponTuning(1.0F, 1.0F, 1.0F, 15.01F).valid());
    }

    @Test
    void anyEditedFieldMakesTuningNonDefault() {
        assertFalse(new WeaponTuning(0.70F, 1.0F, 1.0F, 1.0F).defaultValues());
    }
}
