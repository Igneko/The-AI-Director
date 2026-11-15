package com.igneko.aidirector.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShoveFatigueTest {

    private static final ShoveFatigue.Rules R = ShoveFatigue.Rules.DEFAULTS;

    @Test
    void fourQuickShovesThenProgressiveFatigue() {
        ShoveFatigue f = new ShoveFatigue();
        long t = 1000;
        for (int i = 1; i <= 4; i++) {
            assertTrue(f.tryShove(R, t));
            assertEquals(10, f.lastCooldown());
            t += 10;
        }
        assertTrue(f.tryShove(R, t));
        assertEquals(30, f.lastCooldown());
        assertTrue(f.isFatigued(R));
        t += 30;
        assertTrue(f.tryShove(R, t));
        assertEquals(35, f.lastCooldown());
        t += 35;
        assertTrue(f.tryShove(R, t));
        assertEquals(40, f.lastCooldown());
        t += 40;
        assertTrue(f.tryShove(R, t));
        assertEquals(40, f.lastCooldown(), "el enfriamiento no supera 2,0 s");
    }

    @Test
    void cannotShoveDuringCooldown() {
        ShoveFatigue f = new ShoveFatigue();
        assertTrue(f.tryShove(R, 0));
        assertFalse(f.tryShove(R, 9));
        assertTrue(f.tryShove(R, 10));
    }

    @Test
    void fatigueResetsAfterFourSecondsIdle() {
        ShoveFatigue f = new ShoveFatigue();
        long t = 0;
        for (int i = 0; i < 6; i++) {
            while (!f.tryShove(R, t)) t++;
        }
        assertTrue(f.isFatigued(R));
        t = f.nextAllowedTick() + 80;
        assertTrue(f.tryShove(R, t));
        assertEquals(1, f.consecutive());
        assertEquals(10, f.lastCooldown());
    }
}
