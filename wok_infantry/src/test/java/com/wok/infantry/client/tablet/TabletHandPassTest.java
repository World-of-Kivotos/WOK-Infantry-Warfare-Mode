package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand-pass rules and their pose bookkeeping ({@link TabletHandPass}; IMPL_PLAN 3, D17):
 * press-down until 0.30 then cancel, the tablet drawn once per frame, pushes popped per event
 * instance (cancelled events included) so the shared stack stays balanced, and the end-of-frame
 * reaction when the pass did not run.
 */
class TabletHandPassTest {
    /** Stand-in for a {@code RenderHandEvent}: identity matters, not content. */
    private static final class Event {
        boolean cancelled;
    }

    @Test
    void heldItemIsPressedDownUntil030ThenCancelled() {
        assertEquals(TabletHandPass.Action.NONE, TabletHandPass.decide(false, 0.1D, 5, 4).action());
        assertEquals(TabletHandPass.Action.PUSH, TabletHandPass.decide(true, 0.0D, 5, 4).action());
        assertEquals(TabletHandPass.Action.PUSH, TabletHandPass.decide(true, 0.2999D, 5, 4).action());
        assertEquals(TabletHandPass.Action.CANCEL, TabletHandPass.decide(true, 0.30D, 5, 4).action());
        assertEquals(TabletHandPass.Action.CANCEL, TabletHandPass.decide(true, 0.9D, 5, 4).action());
        assertTrue(TabletHandPass.decide(true, 0.5D, 5, 4).drawTablet(), "first event of frame 5");
        assertFalse(TabletHandPass.decide(true, 0.5D, 5, 5).drawTablet(), "already drawn in frame 5");
        assertFalse(TabletHandPass.decide(false, 0.5D, 5, 4).drawTablet(), "inactive draws nothing");
    }

    /** One hand pass: main and off hand at HIGH, a listener in between, LOWEST for both. */
    private static void pass(TabletHandPass hands, PoseStack pose, long frame, double p,
                             boolean between) {
        Event[] events = {new Event(), new Event()};
        for (Event event : events) {
            TabletHandPass.Decision d = hands.onHigh(true, p, frame);
            if (d.action() == TabletHandPass.Action.CANCEL) {
                event.cancelled = true;
            } else if (d.action() == TabletHandPass.Action.PUSH) {
                pose.pushPose();
                pose.translate(0.06D, -0.7D, 0.05D);
                hands.pushed(event);
            }
            if (between && !event.cancelled) {
                // TaCZ: cancels the event and draws its gun itself (balanced on its own).
                pose.pushPose();
                pose.popPose();
                event.cancelled = true;
            }
            // LOWEST receives cancelled events.
            if (hands.popFor(event)) {
                pose.popPose();
            }
            assertFalse(hands.popFor(event), "popped at most once");
        }
    }

    @Test
    void poseStackStaysBalancedWhateverHappensBetween() {
        TabletHandPass hands = new TabletHandPass();
        PoseStack pose = new PoseStack();
        Matrix4f before = new Matrix4f(pose.last().pose());
        long frame = 1;
        for (double p = 0.0D; p <= 1.0D; p += 0.05D) {
            pass(hands, pose, frame, p, false);
            pass(hands, pose, frame, p, true);   // a nested second pass in the same frame
            assertTrue(pose.clear(), "pose stack back to its base at p=" + p);
            assertEquals(before, pose.last().pose());
            assertEquals(0, hands.open());
            assertTrue(hands.ranIn(frame));
            frame++;
        }
    }

    @Test
    void tabletIsDrawnByTheFirstHandEventOfEachFrameOnly() {
        TabletHandPass hands = new TabletHandPass();
        int drawn = 0;
        for (long frame = 1; frame <= 3; frame++) {
            for (int event = 0; event < 4; event++) {   // two hands, the pass run twice
                if (hands.onHigh(true, 0.5D, frame).drawTablet()) {
                    drawn++;
                }
            }
        }
        assertEquals(3, drawn);
    }

    @Test
    void reactionWhenTheHandPassDidNotRun() {
        assertEquals(TabletHandPass.Reaction.NONE, TabletHandPass.react(TabletPath.A3D,
                TabletMotion.State.OPENING, true, true), "pass ran");
        assertEquals(TabletHandPass.Reaction.NONE, TabletHandPass.react(TabletPath.B2D,
                TabletMotion.State.OPENING, false, true), "scheme B needs no hand pass");
        assertEquals(TabletHandPass.Reaction.NONE, TabletHandPass.react(TabletPath.A3D,
                TabletMotion.State.SHOWN, false, false), "shown: nothing to lose");
        assertEquals(TabletHandPass.Reaction.SWITCH_TO_B, TabletHandPass.react(TabletPath.A3D,
                TabletMotion.State.OPENING, false, true), "first frame: restart on B");
        assertEquals(TabletHandPass.Reaction.LOSE, TabletHandPass.react(TabletPath.A3D,
                TabletMotion.State.OPENING, false, false), "later: shown at once");
        assertEquals(TabletHandPass.Reaction.LOSE, TabletHandPass.react(TabletPath.A3D,
                TabletMotion.State.CLOSING, false, true), "closing: ends at once");
    }

    @Test
    void reactionsDriveTheMotionAsDesigned() {
        // First frame: B from the moment it opened (the motion's 1-frame window).
        TabletMotion m = new TabletMotion(TabletMotion.Config.DEFAULT);
        m.open(1000.0D);
        m.update(1012.0D);
        assertTrue(m.switchPath(TabletPath.B2D, 1012.0D - m.elapsed(1012.0D)));
        assertEquals(TabletPath.B2D, m.path());
        assertEquals(TabletMotion.State.OPENING, m.state());
        // Later: an opening on A is shown, a close ends.
        TabletMotion late = new TabletMotion(TabletMotion.Config.DEFAULT);
        late.open(0.0D);
        late.update(400.0D);
        assertTrue(late.loseHandRender(400.0D));
        assertEquals(TabletMotion.State.SHOWN, late.state());
        late.close(500.0D);
        assertTrue(late.loseHandRender(510.0D));
        assertEquals(TabletMotion.State.IDLE, late.state());
    }
}
