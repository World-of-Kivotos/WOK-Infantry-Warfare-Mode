package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletPassMarks.Mark;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Render.Pre / Render.Post pairing of the full-screen pages (IMPL_PLAN 3). */
class TabletPassMarksTest {
    @Test
    void aPostUndoesWhatItsPreDidExactlyOnce() {
        TabletPassMarks<String> marks = new TabletPassMarks<>();
        Object screen = new Object();
        marks.begin(screen, Mark.PUSHED, "frame");
        assertTrue(marks.isOpen(screen));
        TabletPassMarks.Open<String> pass = marks.end(screen);
        assertEquals(Mark.PUSHED, pass.mark());
        assertEquals("frame", pass.frame());
        assertEquals(Mark.NONE, marks.end(screen).mark(), "a second Post finds nothing");
        assertFalse(marks.isOpen(screen));
    }

    @Test
    void aCancelledPreStillGetsItsPost() {
        TabletPassMarks<String> marks = new TabletPassMarks<>();
        Object screen = new Object();
        marks.begin(screen, Mark.CANCELLED, "dark");
        TabletPassMarks.Open<String> pass = marks.end(screen);
        assertEquals(Mark.CANCELLED, pass.mark(), "Forge posts Render.Post after a cancelled Pre");
        assertSame("dark", pass.frame());
    }

    @Test
    void aPostWithoutAPreAndAPreThatChangedNothingAreNoOps() {
        TabletPassMarks<String> marks = new TabletPassMarks<>();
        Object screen = new Object();
        assertEquals(Mark.NONE, marks.end(screen).mark());
        assertNull(marks.end(screen).frame());
        marks.begin(screen, Mark.NONE, "x");
        assertFalse(marks.isOpen(screen));
        marks.begin(null, Mark.PUSHED, "x");
        assertEquals(Mark.NONE, marks.end(null).mark());
    }

    @Test
    void layeredScreensArePairedByInstance() {
        TabletPassMarks<String> marks = new TabletPassMarks<>();
        Object background = new Object();
        Object top = new Object();
        marks.begin(background, Mark.PUSHED, "below");
        marks.begin(top, Mark.CANCELLED, "above");
        assertEquals("above", marks.end(top).frame());
        assertEquals("below", marks.end(background).frame());
        marks.begin(top, Mark.PUSHED, "again");
        marks.clear();
        assertFalse(marks.isOpen(top), "logout forgets open passes");
    }
}
