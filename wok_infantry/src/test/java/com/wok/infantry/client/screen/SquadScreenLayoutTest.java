package com.wok.infantry.client.screen;

import com.wok.infantry.network.battle.BattleNetworkLimits;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadScreenLayoutTest {
    @Test
    void minimumCompactViewportPagesEveryProtocolDeploymentPoint() {
        int rows = SquadScreen.visibleDeploymentRows(138, 156, 18, 20);

        assertEquals(1, rows);
        assertEquals(BattleNetworkLimits.MAX_DEPLOYMENT_POINTS,
                SquadScreen.deploymentPageCount(
                        BattleNetworkLimits.MAX_DEPLOYMENT_POINTS, rows));

        List<Integer> reached = new ArrayList<>();
        int requestedPage = 0;
        while (true) {
            SquadScreen.DeploymentPagination page = SquadScreen.deploymentPagination(
                    BattleNetworkLimits.MAX_DEPLOYMENT_POINTS, rows, requestedPage);
            for (int index = page.startInclusive(); index < page.endExclusive(); index++) {
                reached.add(index);
            }
            if (!page.hasNext()) {
                assertEquals(page.page(), page.nextPage(),
                        "the final next-page control must remain bounded");
                break;
            }
            assertEquals(page.page() + 1, page.nextPage());
            requestedPage = page.nextPage();
        }
        assertEquals(16, reached.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 10, 11, 12, 13, 14, 15), reached);
    }

    @Test
    void standardViewportUsesAllRowsBeforePaging() {
        int rows = SquadScreen.visibleDeploymentRows(120, 396, 20, 23);

        assertEquals(12, rows);
        assertEquals(2, SquadScreen.deploymentPageCount(
                BattleNetworkLimits.MAX_DEPLOYMENT_POINTS, rows));
        SquadScreen.DeploymentPagination first = SquadScreen.deploymentPagination(16, rows, 0);
        SquadScreen.DeploymentPagination second = SquadScreen.deploymentPagination(
                16, rows, first.nextPage());
        assertEquals(0, first.startInclusive());
        assertEquals(12, first.endExclusive());
        assertFalse(first.hasPrevious());
        assertTrue(first.hasNext());
        assertEquals(12, second.startInclusive());
        assertEquals(16, second.endExclusive());
        assertTrue(second.hasPrevious());
        assertFalse(second.hasNext());
        assertEquals(0, second.previousPage());
    }

    @Test
    void emptyOrUnavailableViewportKeepsPagerMathSafe() {
        assertEquals(0, SquadScreen.visibleDeploymentRows(138, 155, 18, 20));
        assertEquals(1, SquadScreen.deploymentPageCount(16, 0));
        assertEquals(1, SquadScreen.deploymentPageCount(0, 1));
    }
}
