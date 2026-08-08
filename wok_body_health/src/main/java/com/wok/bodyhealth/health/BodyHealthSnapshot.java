package com.wok.bodyhealth.health;

import com.wok.bodyhealth.config.BodyHealthConfig;

public record BodyHealthSnapshot(float[] current, float[] maximum) {
    public static BodyHealthSnapshot from(BodyHealthData data) {
        BodyPart[] parts = BodyPart.values();
        float[] current = new float[parts.length];
        float[] maximum = new float[parts.length];
        for (int index = 0; index < parts.length; index++) {
            BodyPart part = parts[index];
            current[index] = data.get(part);
            maximum[index] = BodyHealthConfig.maxHealth(part);
        }
        return new BodyHealthSnapshot(current, maximum);
    }
}
