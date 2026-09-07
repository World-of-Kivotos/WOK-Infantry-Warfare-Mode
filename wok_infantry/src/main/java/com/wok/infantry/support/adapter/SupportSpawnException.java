package com.wok.infantry.support.adapter;

/** Checked boundary prevents optional provider faults from escaping the server tick. */
public final class SupportSpawnException extends Exception {
    public SupportSpawnException(String message) {
        super(message);
    }

    public SupportSpawnException(String message, Throwable cause) {
        super(message, cause);
    }
}
