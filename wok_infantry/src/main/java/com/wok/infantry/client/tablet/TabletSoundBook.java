package com.wok.infantry.client.tablet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * What the animation's events do to the sounds (IMPL_PLAN 4.4, DESIGN 3.6; preview
 * {@code app.js step()} + {@code audio.js}). Pure bookkeeping, the playing itself goes to an
 * {@link Output} ({@link TabletSounds} in the game, a fake in tests):
 * <ul>
 *   <li>SOUND → play it, once per {@value TabletAnimationModel#SFX_NO_REPEAT_MS} ms per name (the
 *   motion already applies this rule to its cues; here it also covers the link chime). A "ready"
 *   cue without a battle link starts the {@link TabletLinkChime} instead.</li>
 *   <li>CUT → every sound of that direction (open / close / all) that is still in the book fades
 *   out ({@link TabletSoundFade}); the other direction keeps playing.</li>
 *   <li>STATE CLOSING / IDLE → the chime stops.</li>
 *   <li>{@code ui.tabletSounds} off → nothing is played (the log still records it).</li>
 * </ul>
 *
 * @param <H> a playing sound
 */
public final class TabletSoundBook<H> {
    /** How long a played sound stays in the book (the longest file is 520 ms). */
    public static final double LIFE_MS = 1000.0D;
    /** Lines kept in {@link #log()}. */
    public static final int LOG_SIZE = 64;

    /** Where the sounds go. */
    public interface Output<H> {
        /** Starts {@code name} (a sound of sounds.json, {@code tablet.<name>}); {@code null} if it did not start. */
        H play(String name, TabletCue.Dir dir);

        /** Fades {@code sound} out from {@code now} on. */
        void fadeOut(H sound, double now);
    }

    /**
     * One line of the log (debugging, uiTest).
     *
     * @param kind    what happened
     * @param name    sound name; for CUT the cut (open / close / all)
     * @param dir     the sound's direction (null for CUT)
     * @param atMs    when
     * @param audible PLAY: a sound actually started; CUT: at least one sound was faded
     */
    public record Entry(Kind kind, String name, TabletCue.Dir dir, double atMs, boolean audible) {
        public enum Kind {
            /** A sound was asked for (audible = it started; not when sounds are off). */
            PLAY,
            /** Dropped: the same sound played less than 150 ms before. */
            REPEAT,
            /** "ready" without a battle link: the search chime started instead. */
            WAIT,
            /** A cut. */
            CUT
        }
    }

    private record Active<H>(String name, TabletCue.Dir dir, H sound, double startedAt) {
    }

    private final Output<H> out;
    private final List<Active<H>> active = new ArrayList<>();
    private final Map<String, Double> lastPlayed = new HashMap<>();
    private final TabletLinkChime chime = new TabletLinkChime();
    private final ArrayDeque<Entry> log = new ArrayDeque<>();

    public TabletSoundBook(Output<H> out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    /**
     * Applies one event of {@link TabletMotion#drainEvents()} at {@code now}. {@code linkOk} is
     * asked only for a "ready" cue.
     */
    public void onEvent(TabletMotion.Event event, double now, boolean enabled, BooleanSupplier linkOk) {
        switch (event.type()) {
            case STATE -> {
                String state = event.name();
                if (TabletMotion.State.CLOSING.name().equals(state)
                        || TabletMotion.State.IDLE.name().equals(state)) {
                    chime.stop();
                }
            }
            case SOUND -> {
                TabletCue.Dir dir = event.dir() == null ? TabletCue.Dir.OPEN : event.dir();
                if (event.link() && !linkOk.getAsBoolean()) {
                    chime.start(now);
                    record(new Entry(Entry.Kind.WAIT, event.name(), dir, now, false));
                    return;
                }
                play(event.name(), dir, now, enabled);
            }
            case CUT -> cut(TabletMotion.Cut.valueOf(event.name().toUpperCase(Locale.ROOT)), now);
        }
    }

    /**
     * Once per frame: the search chime ({@code linkOk} is asked only while it runs) and the
     * clean-up of finished sounds.
     */
    public void tick(double now, boolean enabled, BooleanSupplier linkOk, TabletMotion.State state) {
        prune(now);
        if (!chime.active()) {
            return;
        }
        for (String name : chime.update(now, linkOk.getAsBoolean(), state)) {
            play(name, TabletCue.Dir.OPEN, now, enabled);
        }
    }

    /** Plays {@code name} unless it played less than 150 ms ago; returns whether a sound started. */
    public boolean play(String name, TabletCue.Dir dir, double now, boolean enabled) {
        Double last = lastPlayed.get(name);
        if (last != null && now - last < TabletAnimationModel.SFX_NO_REPEAT_MS) {
            record(new Entry(Entry.Kind.REPEAT, name, dir, now, false));
            return false;
        }
        lastPlayed.put(name, now);
        prune(now);
        H sound = enabled ? out.play(name, dir) : null;
        if (sound != null) {
            active.add(new Active<>(name, dir, sound, now));
        }
        record(new Entry(Entry.Kind.PLAY, name, dir, now, sound != null));
        return sound != null;
    }

    /** Fades out the sounds of {@code which}; returns how many. */
    public int cut(TabletMotion.Cut which, double now) {
        prune(now);
        int count = 0;
        for (Iterator<Active<H>> it = active.iterator(); it.hasNext(); ) {
            Active<H> a = it.next();
            if (which == TabletMotion.Cut.ALL || a.dir().name().equals(which.name())) {
                out.fadeOut(a.sound(), now);
                it.remove();
                count++;
            }
        }
        record(new Entry(Entry.Kind.CUT, which.previewName(), null, now, count > 0));
        return count;
    }

    /** Everything fades and the chime stops (logout, a fresh start for acceptance). */
    public void stopAll(double now) {
        chime.stop();
        if (!active.isEmpty()) {
            cut(TabletMotion.Cut.ALL, now);
        }
    }

    /** The sounds still in the book, oldest first (names). */
    public List<String> activeNames() {
        List<String> names = new ArrayList<>(active.size());
        for (Active<H> a : active) {
            names.add(a.name());
        }
        return names;
    }

    /** The sounds still in the book, oldest first. */
    public List<H> activeSounds() {
        List<H> sounds = new ArrayList<>(active.size());
        for (Active<H> a : active) {
            sounds.add(a.sound());
        }
        return sounds;
    }

    public TabletLinkChime chime() {
        return chime;
    }

    /** The last {@value #LOG_SIZE} lines, oldest first. */
    public List<Entry> log() {
        return Collections.unmodifiableList(new ArrayList<>(log));
    }

    private void prune(double now) {
        active.removeIf(a -> now - a.startedAt() > LIFE_MS);
    }

    private void record(Entry entry) {
        log.addLast(entry);
        while (log.size() > LOG_SIZE) {
            log.removeFirst();
        }
    }
}
