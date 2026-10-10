package com.wok.infantry.client.tablet;

/**
 * Input while the tablet comes out (DESIGN 2.5), as pure rules over one event: what a key means
 * and where a mouse press lands. The controller only reads the event and carries out the
 * {@link Delivery}.
 *
 * <p>Coordinates: mouse events arrive in GUI-scaled pixels. A terminal frame ({@link
 * TabletPath2D.TermFrame}) is in layout pixels (GUI / f, {@code UiScale.factor}); a map frame
 * ({@link TabletPath2D.MapFrame}) is in GUI pixels (full-screen pages have no 2x layout).
 */
public final class TabletInput {
    /**
     * What to do with one mouse press, scroll or drag.
     *
     * @param cancel  cancel the original event (the screen must not see it)
     * @param deliver hand it to the screen at ({@code x}, {@code y}) instead
     * @param x       GUI x for the screen (only with {@code deliver})
     * @param y       GUI y for the screen
     * @param jump    jump the animation to SHOWN
     */
    public record Delivery(boolean cancel, boolean deliver, double x, double y, boolean jump) {
        /** Leave the event alone. */
        public static final Delivery PASS = new Delivery(false, false, 0.0D, 0.0D, false);
        /** Swallow it without effect (a drag of a button held since before the tablet came out). */
        public static final Delivery SWALLOW = new Delivery(true, false, 0.0D, 0.0D, false);
        /** Swallow it and jump to the end. */
        public static final Delivery BLOCK = new Delivery(true, false, 0.0D, 0.0D, true);
    }

    private TabletInput() {
    }

    /** The input a key press in a tablet screen is (Esc first, then the WOK keys, then hotbar). */
    public static TabletMotion.Input classifyKey(boolean escape, boolean terminal, boolean mapKey,
                                                 boolean hotbar) {
        if (escape) {
            return TabletMotion.Input.ESC;
        }
        if (terminal) {
            return TabletMotion.Input.TERMINAL;
        }
        if (mapKey) {
            return TabletMotion.Input.MAP_KEY;
        }
        return hotbar ? TabletMotion.Input.HOTBAR : TabletMotion.Input.KEY;
    }

    /**
     * Whether a key press finishes the opening within 100 ms: any key the rules call "another key"
     * while opening, unless it has been held since the tablet came out (its repeats are no new
     * input).
     */
    public static boolean finishes(TabletPath path, TabletMotion.State state, double p,
                                   TabletMotion.Input input, TabletScreenKind kind,
                                   boolean heldSinceOpen) {
        if (heldSinceOpen || state != TabletMotion.State.OPENING) {
            return false;
        }
        return TabletMotion.inputAction(path, state, p, input, kind, true).effect()
                == TabletMotion.Effect.FINISH;
    }

    /**
     * A mouse press, scroll or drag at GUI ({@code guiX}, {@code guiY}) while the tablet is
     * {@code state} on {@code path} (scheme B or the quick setting; {@code term} / {@code map} is
     * the frame on screen, {@code null} when none). {@code factor} is the terminal's layout factor.
     * {@code heldSinceOpen}: a drag of a button pressed before the screen opened, which is no
     * input to the screen and is only swallowed.
     */
    public static Delivery decide(TabletPath path, TabletMotion.State state, double p,
                                  TabletMotion.Input input, TabletScreenKind kind,
                                  TabletPath2D.TermFrame term, TabletPath2D.MapFrame map,
                                  double guiX, double guiY, int factor, boolean heldSinceOpen) {
        return decide(path, state, p, input, kind, term, map, null, guiX, guiY, factor, 1,
                heldSinceOpen);
    }

    /**
     * {@link #decide(TabletPath, TabletMotion.State, double, TabletMotion.Input, TabletScreenKind,
     * TabletPath2D.TermFrame, TabletPath2D.MapFrame, double, double, int, boolean)} with scheme A's
     * frame {@code a} (or {@code null}): the press is visible where {@link
     * TabletPose3D#contentVisible} says so (physical pixel = GUI × {@code guiScale}) and is moved
     * back from the frame's screen rectangle ({@link TabletMotion#remapPoint}: u = (mouse − rect
     * origin) / s); a press that lands outside the screen is swallowed and jumps to the end.
     */
    public static Delivery decide(TabletPath path, TabletMotion.State state, double p,
                                  TabletMotion.Input input, TabletScreenKind kind,
                                  TabletPath2D.TermFrame term, TabletPath2D.MapFrame map,
                                  TabletFrame a, double guiX, double guiY, int factor, int guiScale,
                                  boolean heldSinceOpen) {
        if (state != TabletMotion.State.OPENING) {
            return Delivery.PASS;
        }
        if (heldSinceOpen) {
            return Delivery.SWALLOW;
        }
        if (a != null) {
            return decideA(path, state, p, input, kind, a, guiX, guiY, Math.max(1, guiScale));
        }
        int f = Math.max(1, factor);
        boolean visible = true;
        if (term != null) {
            visible = TabletPath2D.termVisible(term, guiX / f, guiY / f);
        } else if (map != null) {
            visible = TabletPath2D.mapVisible(map, guiX, guiY);
        }
        TabletMotion.InputDecision decision = TabletMotion.inputAction(path, state, p, input, kind,
                visible);
        if (!decision.intercept()) {
            return Delivery.PASS;
        }
        if (decision.deliver() != TabletMotion.Deliver.REMAPPED) {
            return Delivery.BLOCK;
        }
        if (term != null) {
            double[] at = shift(guiX, guiY, term.offsetX(), term.offsetY(), f);
            return new Delivery(true, true, at[0], at[1], true);
        }
        if (map != null) {
            // The map's top-left is pinned to the inner screen; a press on the frame or the world
            // around it does not reach the map.
            if (!map.inner().contains(guiX, guiY)) {
                return Delivery.BLOCK;
            }
            double[] at = shift(guiX, guiY, map.offsetX(), map.offsetY(), 1);
            return new Delivery(true, true, at[0], at[1], true);
        }
        return new Delivery(true, true, guiX, guiY, true);
    }

    private static Delivery decideA(TabletPath path, TabletMotion.State state, double p,
                                    TabletMotion.Input input, TabletScreenKind kind, TabletFrame a,
                                    double guiX, double guiY, int guiScale) {
        double px = guiX * guiScale;
        double py = guiY * guiScale;
        boolean visible = TabletPose3D.contentVisible(a, px, py);
        TabletMotion.InputDecision decision = TabletMotion.inputAction(path, state, p, input, kind,
                visible);
        if (!decision.intercept()) {
            return Delivery.PASS;
        }
        TabletPose3D.RectPx rect = a.ui().rectPx();
        if (decision.deliver() != TabletMotion.Deliver.REMAPPED || rect == null) {
            return Delivery.BLOCK;
        }
        TabletUnits units = a.ctx().units();
        TabletMotion.Remap at = TabletMotion.remapPoint(px, py, rect.x(), rect.y(), rect.scale(),
                units.pxW(), units.pxH());
        if (!at.inside()) {
            return Delivery.BLOCK;
        }
        return new Delivery(true, true, at.x() / guiScale, at.y() / guiScale, true);
    }

    /** GUI point minus a content offset given in layout pixels of {@code factor}. */
    public static double[] shift(double guiX, double guiY, int offsetX, int offsetY, int factor) {
        int f = Math.max(1, factor);
        return new double[]{guiX - (double) offsetX * f, guiY - (double) offsetY * f};
    }
}
