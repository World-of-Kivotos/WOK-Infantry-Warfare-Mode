package com.wok.infantry.client.tablet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Scheme A's part of the first-person hand pass (IMPL_PLAN 3, DESIGN 6.4), as rules plus the
 * push / pop bookkeeping:
 *
 * <ul>
 *   <li>{@code RenderHandEvent} HIGH, per hand: while the held item is still on screen
 *   (p &lt; 0.30) its press-down is pushed on the event's pose and the event goes on (TaCZ or
 *   vanilla draws the whole gun, item or arm under it); from 0.30 on the event is cancelled.
 *   The tablet and both hands are drawn once per frame, by the first hand event of the frame
 *   (a mod that runs the hand pass twice, such as a night-vision overlay, draws them once).</li>
 *   <li>{@code RenderHandEvent} LOWEST, cancelled events included: the pose pushed for exactly this
 *   event instance is popped, so the shared pose stack is balanced whatever a listener in between
 *   did (TaCZ cancels the event and draws its gun itself).</li>
 * </ul>
 *
 * <p>Client render thread only.
 */
public final class TabletHandPass {
    /** What HIGH does with one hand event. */
    public enum Action {
        /** Not animating on scheme A: leave the event alone. */
        NONE,
        /** Push the press-down and let the event go on. */
        PUSH,
        /** Cancel the event: the held item is out of the picture. */
        CANCEL
    }

    /**
     * The decision for one hand event.
     *
     * @param action       what to do with the event
     * @param drawTablet   draw the 3D tablet and the hands with this event (first one of the frame)
     */
    public record Decision(Action action, boolean drawTablet) {
        public static final Decision NONE = new Decision(Action.NONE, false);
    }

    /** What the end of a frame does when scheme A's hand pass did not run (IMPL_PLAN D17). */
    public enum Reaction {
        /** Nothing: the pass ran, or scheme A is not animating. */
        NONE,
        /** The opening's first frame: start again on scheme B from p 0. */
        SWITCH_TO_B,
        /** Later: an opening is shown at once, a close ends at once (DESIGN 3.7). */
        LOSE
    }

    /**
     * D17's reaction at the end of a frame: on scheme A, opening or closing, the hand pass did not
     * reach HIGH (F1, a mod cancelling the hand pass at HIGHEST, a camera that is not the player).
     */
    public static Reaction react(TabletPath path, TabletMotion.State state, boolean handPassRan,
                                 boolean firstFrameOfOpening) {
        if (path != TabletPath.A3D || handPassRan
                || state != TabletMotion.State.OPENING && state != TabletMotion.State.CLOSING) {
            return Reaction.NONE;
        }
        return state == TabletMotion.State.OPENING && firstFrameOfOpening ? Reaction.SWITCH_TO_B
                : Reaction.LOSE;
    }

    private final Set<Object> pushed = Collections.newSetFromMap(new IdentityHashMap<>());
    private long drawnFrame = Long.MIN_VALUE;
    private long seenFrame = Long.MIN_VALUE;

    /**
     * Pure rule: on scheme A ({@code active}: opening or closing with a frame) the held item is
     * pressed down until p 0.30 and cancelled from there; the tablet is drawn by the first event
     * of frame {@code frame} only.
     */
    public static Decision decide(boolean active, double p, long frame, long drawnFrame) {
        if (!active) {
            return Decision.NONE;
        }
        Action action = p < TabletAnimationModel.GUN_HIDE_AT ? Action.PUSH : Action.CANCEL;
        return new Decision(action, drawnFrame != frame);
    }

    /** HIGH: decides for this event and remembers that the hand pass ran in {@code frame}. */
    public Decision onHigh(boolean active, double p, long frame) {
        seenFrame = frame;
        Decision decision = decide(active, p, frame, drawnFrame);
        if (decision.drawTablet()) {
            drawnFrame = frame;
        }
        return decision;
    }

    /** HIGH pushed a pose for {@code event}. */
    public void pushed(Object event) {
        if (event != null) {
            pushed.add(event);
        }
    }

    /** LOWEST: whether a pose pushed for exactly this event must be popped (at most once). */
    public boolean popFor(Object event) {
        return event != null && pushed.remove(event);
    }

    /** Whether the hand pass reached HIGH in {@code frame}. */
    public boolean ranIn(long frame) {
        return seenFrame == frame;
    }

    /** Pushes still waiting for their LOWEST (0 between frames when the pass is balanced). */
    public int open() {
        return pushed.size();
    }

    /** Forgets everything (logout); a push still open is the event's own business now. */
    public void clear() {
        pushed.clear();
        drawnFrame = Long.MIN_VALUE;
        seenFrame = Long.MIN_VALUE;
    }
}
