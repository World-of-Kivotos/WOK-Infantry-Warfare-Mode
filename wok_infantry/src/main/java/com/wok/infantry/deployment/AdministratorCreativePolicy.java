package com.wok.infantry.deployment;

/** Keeps the administrator setup escape hatch separate from deployment state transitions. */
final class AdministratorCreativePolicy {
    private AdministratorCreativePolicy() {
    }

    static boolean preservesCreative(boolean administrator, boolean creativeMode) {
        return administrator && creativeMode;
    }
}
