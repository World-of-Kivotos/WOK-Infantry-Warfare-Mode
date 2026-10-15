package com.wok.bodyhealth.prone;

import java.util.Objects;

/**
 * What a mixin HEAD hook should do: PASS lets the original method run, MISS returns null and HIT
 * returns {@code result} (the gun mod's own hit-result object).
 */
public record ProneDecision(Kind kind, Object result) {
    public enum Kind { PASS, MISS, HIT }

    public static final ProneDecision PASS = new ProneDecision(Kind.PASS, null);
    public static final ProneDecision MISS = new ProneDecision(Kind.MISS, null);

    public static ProneDecision hit(Object result) {
        return new ProneDecision(Kind.HIT, Objects.requireNonNull(result, "result"));
    }

    public boolean cancels() {
        return kind != Kind.PASS;
    }
}
