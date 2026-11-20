package com.igneko.aidirector.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HordeSettingsTest {

    @Test
    void oldConfigAreaIsWidenedToLeaveRoomBeyondTheSafeRadius() {
        // Un archivo de configuración antiguo guarda 40: con 15 de radio seguro el área efectiva sube a 60.
        var old = new AIDirectorConfig.HordeSettings(30, 40, 8.0, 12, 0.3, true, 15, 6, 3);
        assertEquals(60, old.effectiveArea());
    }

    @Test
    void biggerConfiguredAreaIsKept() {
        var big = new AIDirectorConfig.HordeSettings(30, 100, 8.0, 12, 0.15, true, 15, 6, 3);
        assertEquals(100, big.effectiveArea());
    }

    @Test
    void largerSafeRadiusGrowsTheArea() {
        var wide = new AIDirectorConfig.HordeSettings(30, 60, 8.0, 12, 0.15, true, 30, 6, 3);
        assertEquals(90, wide.effectiveArea());
    }
}
