package com.wok.infantry.client.screen;

import java.util.function.Predicate;

/**
 * Pure selection rules of the administrator loadout terminal (admin-01, admin-04, admin-07).
 *
 * <p>Kept free of Minecraft types so the rules can be unit tested; the screen only feeds in
 * what it currently shows.</p>
 */
final class AdminLoadoutSelection {
    static final int MAX_SLOT_ID_LENGTH = 64;

    private AdminLoadoutSelection() {
    }

    /**
     * Why "职业管理" cannot open, or an empty string when it can. The class settings page edits
     * the selected formation's rule for the selected profession, so both must exist.
     */
    static String classSettingsBlockedReason(boolean formationSelected,
                                             int formationClassCount,
                                             boolean ruleFound, String className) {
        if (!formationSelected) {
            return "未选择编制，无法管理职业";
        }
        if (formationClassCount <= 0) {
            return "当前编制还没有任何职业规则，无法管理职业";
        }
        if (!ruleFound) {
            String name = className == null || className.isBlank()
                    ? "当前职业" : "“" + className + "”";
            return "当前编制没有" + name + "的职业规则，无法管理职业";
        }
        return "";
    }

    /**
     * Page of slot tabs to show. While the selection drives the page (first open, a new
     * snapshot, another class, a resize that changes the tab count) the page holding the
     * selected slot is shown; otherwise the page the administrator flipped to is kept. Paging
     * never changes the selected slot.
     */
    static int slotPage(int selectedIndex, int slotsPerPage, int slotCount, int currentPage,
                        boolean followSelection) {
        int perPage = Math.max(1, slotsPerPage);
        int pageCount = Math.max(1, (Math.max(0, slotCount) + perPage - 1) / perPage);
        int page = followSelection && selectedIndex >= 0 && selectedIndex < slotCount
                ? selectedIndex / perPage : currentPage;
        return Math.max(0, Math.min(page, pageCount - 1));
    }

    /**
     * Selection after a server refresh: a just-created id becomes selected only once the
     * refreshed snapshot actually contains it, otherwise the current selection stays.
     */
    static String confirmedSelection(String pendingId, Predicate<String> existsInSnapshot,
                                     String currentId) {
        if (pendingId == null || pendingId.isBlank() || !existsInSnapshot.test(pendingId)) {
            return currentId;
        }
        return pendingId;
    }

    /**
     * Input filter for the slot id field: the partial text may only contain the characters
     * {@code LoadoutClassDefinition.validSlotId} accepts ({@code [a-z0-9_.-]}). An empty draft
     * is allowed while typing; the server still validates the final id.
     */
    static boolean slotIdDraft(String value) {
        if (value == null || value.length() > MAX_SLOT_ID_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!(character >= 'a' && character <= 'z') && !(character >= '0' && character <= '9')
                    && character != '_' && character != '-' && character != '.') {
                return false;
            }
        }
        return true;
    }
}
