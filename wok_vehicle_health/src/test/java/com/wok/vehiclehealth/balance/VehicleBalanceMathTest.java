package com.wok.vehiclehealth.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleBalanceMathTest {
    @Test
    void sameTierTanksTakeFiveFrontAndThreeSideRearHits() {
        assertTankHits(500.0F, 200.0F, 5, 3, 3);
        assertTankHits(650.0F, 260.0F, 5, 3, 3);
        assertTankHits(800.0F, 340.0F, 5, 3, 3);
    }

    @Test
    void t3TankNeedsFourFrontHitsAgainstT2WhileT2NeedsSevenAgainstT3() {
        assertEquals(4, hits(650.0F, VehicleBalanceMath.tankGunDamage(
                340.0F, VehicleCategory.TANK, ImpactAspect.FRONT)));
        assertEquals(7, hits(800.0F, VehicleBalanceMath.tankGunDamage(
                260.0F, VehicleCategory.TANK, ImpactAspect.FRONT)));
    }

    @Test
    void t3VersusT3TakesFiveFrontAndThreeSideRearHits() {
        assertTankHits(800.0F, 340.0F, 5, 3, 3);
    }

    @Test
    void t2AndT3TankGunsKillT2AfvsInTwoFrontOrOneSideRearHit() {
        for (float gunDamage : new float[]{260.0F, 340.0F}) {
            assertEquals(2, hits(300.0F, VehicleBalanceMath.tankGunDamage(
                    gunDamage, VehicleCategory.ARMORED_FIGHTING_VEHICLE,
                    ImpactAspect.FRONT)));
            assertEquals(1, hits(300.0F, VehicleBalanceMath.tankGunDamage(
                    gunDamage, VehicleCategory.ARMORED_FIGHTING_VEHICLE,
                    ImpactAspect.SIDE)));
            assertEquals(1, hits(300.0F, VehicleBalanceMath.tankGunDamage(
                    gunDamage, VehicleCategory.ARMORED_FIGHTING_VEHICLE,
                    ImpactAspect.REAR)));
        }
    }

    @Test
    void m3a3HeavyIfvTakesThreeSameTierFrontHitsAndTwoSideRearHits() {
        VehicleBalanceProfile bradley = VehicleBalanceProfiles.find("dragonrise_reforge:m3a3")
                .orElseThrow();
        assertEquals(3, hits(bradley.hullHealth(), VehicleBalanceMath.tankGunDamage(
                260.0F, VehicleCategory.ARMORED_FIGHTING_VEHICLE, ImpactAspect.FRONT)));
        assertEquals(2, hits(bradley.hullHealth(), VehicleBalanceMath.tankGunDamage(
                260.0F, VehicleCategory.ARMORED_FIGHTING_VEHICLE, ImpactAspect.SIDE)));
        assertEquals(2, hits(bradley.hullHealth(), VehicleBalanceMath.tankGunDamage(
                260.0F, VehicleCategory.ARMORED_FIGHTING_VEHICLE, ImpactAspect.REAR)));
    }

    @Test
    void carlGustafHeatMeetsTankAndAfvTargets() {
        assertInfantryAtHits(650.0F, VehicleCategory.TANK,
                InfantryAntiTankWeapon.CARL_GUSTAF_HEAT, 5, 3, 3);
        assertInfantryAtHits(800.0F, VehicleCategory.TANK,
                InfantryAntiTankWeapon.CARL_GUSTAF_HEAT, 5, 3, 3);
        assertInfantryAtHits(300.0F, VehicleCategory.ARMORED_FIGHTING_VEHICLE,
                InfantryAntiTankWeapon.CARL_GUSTAF_HEAT, 2, 2, 1);
    }

    @Test
    void impactAspectUsesSixtyDegreeFrontAndRearArcs() {
        assertEquals(ImpactAspect.FRONT, VehicleBalanceMath.impactAspect(0.75D));
        assertEquals(ImpactAspect.SIDE, VehicleBalanceMath.impactAspect(0.0D));
        assertEquals(ImpactAspect.REAR, VehicleBalanceMath.impactAspect(-0.75D));
    }

    private static void assertTankHits(float health,
                                       float gunDamage,
                                       int front,
                                       int side,
                                       int rear) {
        assertEquals(front, hits(health, VehicleBalanceMath.tankGunDamage(
                gunDamage, VehicleCategory.TANK, ImpactAspect.FRONT)));
        assertEquals(side, hits(health, VehicleBalanceMath.tankGunDamage(
                gunDamage, VehicleCategory.TANK, ImpactAspect.SIDE)));
        assertEquals(rear, hits(health, VehicleBalanceMath.tankGunDamage(
                gunDamage, VehicleCategory.TANK, ImpactAspect.REAR)));
    }

    private static void assertInfantryAtHits(float health,
                                             VehicleCategory category,
                                             InfantryAntiTankWeapon weapon,
                                             int front,
                                             int side,
                                             int rear) {
        assertEquals(front, hits(health, VehicleBalanceMath.infantryAntiTankDamage(
                weapon, category, ImpactAspect.FRONT)));
        assertEquals(side, hits(health, VehicleBalanceMath.infantryAntiTankDamage(
                weapon, category, ImpactAspect.SIDE)));
        assertEquals(rear, hits(health, VehicleBalanceMath.infantryAntiTankDamage(
                weapon, category, ImpactAspect.REAR)));
    }

    private static int hits(float health, float damage) {
        return VehicleBalanceMath.shotsToKill(health, damage);
    }
}
