package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogTransferScreenLayoutTest {
    @Test void coreActionsAndScrollablePreviewFitCompactAndLargeViewports() {
        for (int[] size : new int[][]{{320, 240}, {960, 720}}) {
            var layout = CatalogTransferScreen.computeLayout(size[0], size[1]);
            assertTrue(layout.left() >= 0 && layout.right() <= size[0]);
            assertTrue(layout.fieldY() + 20 < layout.buttonsY());
            assertTrue(layout.buttonsY() + 20 < layout.statusY());
            assertTrue(layout.statusY() + 33 < layout.actionsY());
            assertTrue(layout.actionsY() + 20 <= layout.bottom());
            assertTrue(layout.bottom() < size[1]);
        }
    }
}
