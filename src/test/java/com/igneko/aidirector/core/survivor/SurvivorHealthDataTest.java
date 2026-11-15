package com.igneko.aidirector.core.survivor;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SurvivorHealthDataTest {

    private static final SurvivorRules R = SurvivorRules.DEFAULTS;
    private static final float EPS = 1e-3f;

    private static void ticks(SurvivorHealthData d, int n) {
        for (int i = 0; i < n; i++) d.tick(R);
    }

    @Test
    void damageIsTakenFromTempHealthFirst() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.applyDamage(R, 60);           // perm 40
        d.usePainPills(R);              // temp 50 → total 90
        assertEquals(50, d.tempHealth(), EPS);
        d.applyDamage(R, 70);           // 50 del temporal, 20 del permanente
        assertEquals(0, d.tempHealth(), EPS);
        assertEquals(20, d.permHealth(), EPS);
    }

    @Test
    void tempHealthIsCappedByMaxMinusPerm() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.applyDamage(R, 20);           // perm 80
        d.usePainPills(R);
        assertEquals(20, d.tempHealth(), EPS);
        assertTrue(d.totalHealth() <= R.maxHealth() + EPS);
    }

    @Test
    void tempHealthDecaysOnePointEveryFiveSeconds() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.applyDamage(R, 60);
        d.usePainPills(R);
        ticks(d, 5 * 20);
        assertEquals(49, d.tempHealth(), 0.01f);
    }

    @Test
    void firstAidRestoresEightyPercentOfMissingAndClearsBlackAndWhite() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.restore(50, 0, SurvivorState.ALIVE, 2, 0, 0, 0, false);
        assertTrue(d.isBlackAndWhite(R));
        d.useFirstAid(R);
        assertEquals(90, d.permHealth(), EPS);
        assertEquals(0, d.incapCount());
        assertFalse(d.isBlackAndWhite(R));
    }

    @Test
    void lethalDamageIncapacitatesUntilThirdStrike() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        for (int strike = 0; strike < R.maxIncaps(); strike++) {
            assertEquals(SurvivorHealthData.Outcome.INCAPACITATED, d.applyDamage(R, 500));
            assertEquals(R.bleedPool(), d.bleedHealth(), EPS);
            d.completeRevive(R);
            assertEquals(1, d.permHealth(), EPS);
            assertEquals(30, d.tempHealth(), EPS);
        }
        assertTrue(d.isBlackAndWhite(R));
        assertEquals(SurvivorHealthData.Outcome.KILLED, d.applyDamage(R, 500));
        assertTrue(d.isDead());
    }

    @Test
    void bleedOutKillsAfterPoolDrains() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.forceIncapacitate(R);
        ticks(d, 299 * 20);
        assertTrue(d.isIncapacitated());
        ticks(d, 2 * 20);
        assertTrue(d.isDead());
    }

    @Test
    void bleedingPausesWhileBeingRevived() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.forceIncapacitate(R);
        assertTrue(d.beginRevive(UUID.randomUUID()));
        ticks(d, 100);
        assertEquals(R.bleedPool(), d.bleedHealth(), EPS);
    }

    @Test
    void reviveTakesFiveSecondsOrHalfWithAdrenaline() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.forceIncapacitate(R);
        UUID reviver = UUID.randomUUID();
        d.beginRevive(reviver);
        int t = 0;
        while (!d.advanceRevive(R, false)) t++;
        assertEquals(99, t);
        assertTrue(d.isAlive());
        assertEquals(1, d.incapCount());

        d.forceIncapacitate(R);
        d.beginRevive(reviver);
        t = 0;
        while (!d.advanceRevive(R, true)) t++;
        assertEquals(49, t);
    }

    @Test
    void secondReviverIsRejected() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        d.forceIncapacitate(R);
        assertTrue(d.beginRevive(UUID.randomUUID()));
        assertFalse(d.beginRevive(UUID.randomUUID()));
    }

    @Test
    void limpModeFollowsTotalHealth() {
        SurvivorHealthData d = new SurvivorHealthData(R);
        assertEquals(1f, d.speedMultiplier(R), EPS);
        d.applyDamage(R, 61);
        assertEquals(R.limpSpeed(), d.speedMultiplier(R), EPS);
        d.restore(1, 0, SurvivorState.ALIVE, 0, 0, 0, 0, false);
        assertEquals(R.severeLimpSpeed(), d.speedMultiplier(R), EPS);
        d.forceIncapacitate(R);
        assertEquals(0f, d.speedMultiplier(R), EPS);
    }
}
