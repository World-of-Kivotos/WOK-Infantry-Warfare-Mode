package com.wok.infantry.stamina;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Standalone fallback for running this package while unrelated legacy tests are uncompilable. */
public final class StaminaTestLauncher {
    private StaminaTestLauncher() {
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(StaminaMathTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        listener.getSummary().printTo(new java.io.PrintWriter(System.out, true));
        if (listener.getSummary().getTestsFailedCount() != 0
                || listener.getSummary().getTestsSucceededCount() == 0) {
            throw new AssertionError("Stamina tests did not pass");
        }
    }
}
