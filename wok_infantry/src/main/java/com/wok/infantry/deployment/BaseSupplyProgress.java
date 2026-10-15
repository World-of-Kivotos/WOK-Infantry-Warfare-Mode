package com.wok.infantry.deployment;

/** One continuous visit; a disconnect, area change or ineligible tick invalidates it. */
public final class BaseSupplyProgress {
    public static final int DURATION_TICKS = 300;
    private long started;
    private long lastTick;
    private boolean completed;

    public BaseSupplyProgress(long now) { started = now; lastTick = now; }
    public void tick(long now) {
        if (now < lastTick || now - lastTick > 1) { started = now; completed = false; }
        lastTick = now;
    }
    public int remainingSeconds(long now) {
        return (int) Math.max(0, (DURATION_TICKS - (now - started) + 19) / 20);
    }
    public boolean ready(long now) { return !completed && now - started >= DURATION_TICKS; }
    public boolean completed() { return completed; }
    public void complete() { completed = true; }
}
