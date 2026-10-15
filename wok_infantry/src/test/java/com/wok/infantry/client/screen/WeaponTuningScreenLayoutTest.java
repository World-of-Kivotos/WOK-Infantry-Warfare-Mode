package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WeaponTuningScreenLayoutTest {
    @Test
    void compactLayoutKeepsAllRowsAndActionsInsideBoard() {
        assertLayout(320, 240);
    }

    @Test
    void largeLayoutKeepsEditorCenteredAndBounded() {
        WeaponTuningScreen.EditorLayout layout = assertLayout(960, 720);
        assertTrue(layout.left() > 100);
        assertTrue(layout.right() < 860);
    }

    private static WeaponTuningScreen.EditorLayout assertLayout(int width, int height) {
        TacticalMapLayout.Layout board = TacticalMapLayout.compute(width, height);
        WeaponTuningScreen.EditorLayout editor = WeaponTuningScreen.computeEditorLayout(
                width, height, board);
        assertTrue(editor.left() >= 0);
        assertTrue(editor.right() <= width);
        assertTrue(editor.top() >= board.header().bottom());
        assertTrue(editor.bottom() <= board.footer().top());
        assertTrue(editor.rowsTop() + editor.rowHeight() * 4 <= editor.actionsTop());
        assertTrue(editor.actionsTop() + 22 <= editor.bottom());
        return editor;
    }
}
