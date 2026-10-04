package com.wok.infantry.client.ui.probe;

/**
 * Optional self-description of a WOK步战 screen for the UI acceptance: which surface of the layout
 * preview it implements and which of the preview's states it currently shows. The acceptance
 * asserts surfaces and states through this instead of screen class names, so a page that moves into
 * a terminal tab keeps its test cases.
 *
 * <p>Ids follow the preview ({@code ui-preview/surfaces/*.js}): surface {@code "formation"}, state
 * {@code "vote"}, and so on. Implementing it has no effect outside the acceptance.
 */
public interface UiSurfaceInfo {
    /** Preview surface id, e.g. {@code "kit"}, {@code "formation"}, {@code "squad"}. */
    String uiSurfaceId();

    /** Preview state id of what is shown now; {@code "default"} when the surface has one state. */
    default String uiStateId() {
        return "default";
    }
}
