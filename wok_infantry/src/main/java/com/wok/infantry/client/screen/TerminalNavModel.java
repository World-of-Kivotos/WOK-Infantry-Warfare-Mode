package com.wok.infantry.client.screen;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Pure state of {@link BattleTerminalNav}, generic over the screen type so it can be unit-tested.
 *
 * <p>A terminal session is identified by its root return screen. Switching tabs replaces the
 * current terminal screen with one that returns to the same root (never to the screen it came
 * from), and instances registered for reuse (the tactical map) are handed out again as long as
 * they were created for the same root.
 */
final class TerminalNavModel<S> {
    private record Entry<S>(S root, S screen) {
    }

    private final Map<BattleTab, Entry<S>> reusable = new EnumMap<>(BattleTab.class);

    /**
     * Root a terminal screen opened from {@code current} must return to: the root of
     * {@code current} when it is itself a terminal screen, otherwise {@code current}.
     */
    S returnScreenFor(S current, Predicate<S> isTerminal, Function<S, S> rootOf) {
        if (current != null && isTerminal.test(current)) {
            return rootOf.apply(current);
        }
        return current;
    }

    /** Instance for {@code tab} in the session of {@code root}; created on first use. */
    S reuse(BattleTab tab, S root, Supplier<S> factory) {
        Entry<S> entry = reusable.get(tab);
        if (entry != null && entry.root() == root) {
            return entry.screen();
        }
        S screen = factory.get();
        reusable.put(tab, new Entry<>(root, screen));
        return screen;
    }

    /** Cached instance for {@code tab} (any root), or {@code null}. */
    S cached(BattleTab tab) {
        Entry<S> entry = reusable.get(tab);
        return entry == null ? null : entry.screen();
    }

    void forget(BattleTab tab) {
        reusable.remove(tab);
    }

    void clear() {
        reusable.clear();
    }
}
