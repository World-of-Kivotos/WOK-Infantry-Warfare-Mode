package com.wok.bodyhealth.prone;

/** The gun mod whose hit test is being taken over; failures are counted per consumer. */
public enum ProneConsumer {
    TACZ("tacz.head"),
    SBW("sbw.head");

    private final String headKey;

    ProneConsumer(String headKey) {
        this.headKey = headKey;
    }

    /** Key of this consumer's getHitResult HEAD hook in {@link ProneMixinGuard} and {@link ProneMixinStatus}. */
    public String headKey() {
        return headKey;
    }
}
