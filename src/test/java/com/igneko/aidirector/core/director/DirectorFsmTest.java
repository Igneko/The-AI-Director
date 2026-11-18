package com.igneko.aidirector.core.director;

import com.igneko.aidirector.core.director.DirectorFsm.Action;
import com.igneko.aidirector.core.director.DirectorFsm.Phase;
import com.igneko.aidirector.core.director.DirectorFsm.Rules;
import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectorFsmTest {

    private static final Rules RULES = Rules.DEFAULTS;
    /** Siempre la duración mínima, para que las pruebas sean deterministas. */
    private static final IntBinaryOperator MIN = (min, max) -> min;

    private static Action step(DirectorFsm fsm, float intensity, int threats) {
        return fsm.tick(RULES, intensity, threats, 0, false, false, MIN);
    }

    @Test
    void fullCycleFollowsTheSpecification() {
        DirectorFsm fsm = new DirectorFsm();
        assertEquals(Phase.BUILD_UP, fsm.phase());

        step(fsm, 0.9f, 10);
        assertEquals(Phase.SUSTAIN_PEAK, fsm.phase());

        for (int i = 0; i < RULES.sustainMinSeconds(); i++) step(fsm, 0.9f, 10);
        assertEquals(Phase.PEAK_FADE, fsm.phase());

        step(fsm, 0.5f, 6);
        assertEquals(Phase.PEAK_FADE, fsm.phase(), "sigue esperando mientras queden amenazas");
        step(fsm, 0.5f, 0);
        assertEquals(Phase.RELAX, fsm.phase());

        for (int i = 0; i < RULES.relaxMinSeconds(); i++) step(fsm, 0.1f, 0);
        assertEquals(Phase.BUILD_UP, fsm.phase());
    }

    @Test
    void buildUpWithoutPeakForcesAHorde() {
        DirectorFsm fsm = new DirectorFsm();
        int hordes = 0;
        for (int i = 0; i < RULES.buildUpMaxSeconds() + 1; i++) {
            if (step(fsm, 0.2f, 0) == Action.FORCE_HORDE) hordes++;
        }
        assertEquals(1, hordes);
        assertEquals(Phase.BUILD_UP, fsm.phase());
        assertEquals(0, fsm.seconds(), "la cuenta se reinicia tras la horda");
    }

    @Test
    void travellingEndsTheRelaxEarly() {
        DirectorFsm fsm = new DirectorFsm();
        fsm.force(Phase.RELAX, 45);
        fsm.tick(RULES, 0f, 0, 10, false, false, MIN);
        assertEquals(Phase.RELAX, fsm.phase());
        fsm.tick(RULES, 0f, 0, 70, false, false, MIN);
        assertEquals(Phase.BUILD_UP, fsm.phase());
    }

    @Test
    void safeRoomFreezesAndLeavingRestartsTheBuildUp() {
        DirectorFsm fsm = new DirectorFsm();
        for (int i = 0; i < 50; i++) step(fsm, 0.3f, 0);
        fsm.tick(RULES, 0.3f, 0, 0, true, false, MIN);
        assertEquals(Phase.SAFE_ROOM, fsm.phase());
        for (int i = 0; i < 500; i++) {
            assertEquals(Action.NONE, fsm.tick(RULES, 0.3f, 0, 0, true, false, MIN), "nunca hordas en el refugio");
        }
        step(fsm, 0.3f, 0);
        assertEquals(Phase.BUILD_UP, fsm.phase());
        assertEquals(1, fsm.seconds());
    }

    @Test
    void crescendoOverridesTheCalmAndFadesAfterwards() {
        DirectorFsm fsm = new DirectorFsm();
        fsm.force(Phase.RELAX, 45);
        fsm.tick(RULES, 0f, 0, 0, false, true, MIN);
        assertEquals(Phase.CRESCENDO, fsm.phase());
        assertTrue(fsm.phase().allowsHordes());
        step(fsm, 0.4f, 12);
        assertEquals(Phase.PEAK_FADE, fsm.phase());
    }

    @Test
    void onlyBuildUpAndCrescendoSpawnSpecials() {
        for (Phase p : Phase.values()) {
            boolean expected = p == Phase.BUILD_UP || p == Phase.CRESCENDO;
            assertEquals(expected, p.allowsSpecials(), p.name());
        }
    }
}
