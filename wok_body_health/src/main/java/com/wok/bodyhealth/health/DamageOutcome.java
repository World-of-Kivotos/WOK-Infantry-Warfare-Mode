package com.wok.bodyhealth.health;

public record DamageOutcome(boolean fatal, BodyPart primaryPart, float bodyDamage,
                            boolean propagatedFatal) {
}
