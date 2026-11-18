package com.igneko.aidirector.core.director;

import java.util.function.IntBinaryOperator;

/**
 * Máquina de estados del ritmo de juego (especificación §4.2), sin dependencias de Minecraft para
 * poder probarla de forma aislada. Se avanza una vez por segundo con la intensidad del equipo.
 *
 * <pre>
 *   BUILD_UP ──I ≥ pico──▶ SUSTAIN_PEAK ──3-5 s──▶ PEAK_FADE ──amenazas ≈ 0──▶ RELAX ──30-45 s / avance──▶ BUILD_UP
 *      └── t > máx. sin pico: fuerza una horda (y reinicia la cuenta)
 * </pre>
 * Dos estados superpuestos tienen prioridad: {@link Phase#SAFE_ROOM} (todo el equipo dentro de un
 * refugio sellado: el Director se congela) y {@link Phase#CRESCENDO} (evento de pánico: oleadas
 * continuas que ignoran la calma). Al terminar un Crescendo se pasa a PEAK_FADE; al salir del
 * refugio, a BUILD_UP desde cero.
 */
public final class DirectorFsm {

    public enum Phase {
        BUILD_UP, SUSTAIN_PEAK, PEAK_FADE, RELAX, SAFE_ROOM, CRESCENDO;

        /** ¿Puede el Director emboscar con Infectados Especiales? */
        public boolean allowsSpecials() {
            return this == BUILD_UP || this == CRESCENDO;
        }

        /** ¿Puede aparecer una horda (o un grupo de comunes) por iniciativa del Director? */
        public boolean allowsHordes() {
            return this == BUILD_UP || this == CRESCENDO;
        }

        /** Calma: casi sin infectados errantes. */
        public boolean isCalm() {
            return this == RELAX || this == SAFE_ROOM;
        }
    }

    public enum Action { NONE, FORCE_HORDE }

    /**
     * @param peak               umbral de intensidad del pico (0,85)
     * @param buildUpMaxSeconds  segundos de construcción sin pico antes de forzar una horda (120)
     * @param sustainMinSeconds  duración mínima del pico (3)
     * @param sustainMaxSeconds  duración máxima del pico (5)
     * @param fadeThreats        amenazas activas a partir de las cuales se considera "cerca de 0"
     * @param fadeMaxSeconds     tope de seguridad del desvanecimiento
     * @param relaxMinSeconds    calma mínima (30)
     * @param relaxMaxSeconds    calma máxima (45)
     * @param relaxTravelBlocks  avance del equipo que termina la calma antes de tiempo (60)
     */
    public record Rules(float peak, int buildUpMaxSeconds, int sustainMinSeconds, int sustainMaxSeconds,
                        int fadeThreats, int fadeMaxSeconds, int relaxMinSeconds, int relaxMaxSeconds,
                        double relaxTravelBlocks) {
        public static final Rules DEFAULTS = new Rules(0.85f, 120, 3, 5, 1, 90, 30, 45, 60.0);
    }

    private Phase phase = Phase.BUILD_UP;
    private int seconds;
    private int target;

    public Phase phase() {
        return phase;
    }

    /** Segundos transcurridos en la fase actual. */
    public int seconds() {
        return seconds;
    }

    /** Duración objetivo de la fase (pico y calma); 0 en las demás. */
    public int target() {
        return target;
    }

    /**
     * Avanza un segundo.
     *
     * @param teamIntensity   intensidad del equipo (máximo de los supervivientes)
     * @param activeThreats   infectados hostiles vivos cerca del equipo
     * @param travelled       bloques que ha avanzado el equipo desde que empezó la fase
     * @param allInSafeRoom   todo el equipo está dentro de un refugio sellado
     * @param crescendo       hay un evento de pánico en curso
     * @param random          generador de enteros en [min, max] (inyectable para pruebas)
     */
    public Action tick(Rules rules, float teamIntensity, int activeThreats, double travelled,
                       boolean allInSafeRoom, boolean crescendo, IntBinaryOperator random) {
        if (allInSafeRoom) {
            enter(Phase.SAFE_ROOM, 0);
            return Action.NONE;
        }
        if (crescendo) {
            enter(Phase.CRESCENDO, 0);
            return Action.NONE;
        }
        if (phase == Phase.SAFE_ROOM) {
            enter(Phase.BUILD_UP, 0);
        } else if (phase == Phase.CRESCENDO) {
            enter(Phase.PEAK_FADE, 0);
        }

        seconds++;
        switch (phase) {
            case BUILD_UP -> {
                if (teamIntensity >= rules.peak()) {
                    enter(Phase.SUSTAIN_PEAK, random.applyAsInt(rules.sustainMinSeconds(), rules.sustainMaxSeconds()));
                } else if (seconds > rules.buildUpMaxSeconds()) {
                    seconds = 0;
                    return Action.FORCE_HORDE;
                }
            }
            case SUSTAIN_PEAK -> {
                if (seconds >= target) enter(Phase.PEAK_FADE, 0);
            }
            case PEAK_FADE -> {
                if (activeThreats <= rules.fadeThreats() || seconds >= rules.fadeMaxSeconds()) {
                    enter(Phase.RELAX, random.applyAsInt(rules.relaxMinSeconds(), rules.relaxMaxSeconds()));
                }
            }
            case RELAX -> {
                if (seconds >= target || travelled >= rules.relaxTravelBlocks()) enter(Phase.BUILD_UP, 0);
            }
            default -> {}
        }
        return Action.NONE;
    }

    /** Fuerza una fase (comandos de depuración). */
    public void force(Phase next, int targetSeconds) {
        enter(next, targetSeconds);
    }

    private void enter(Phase next, int targetSeconds) {
        if (phase == next) return;
        phase = next;
        seconds = 0;
        target = targetSeconds;
    }
}
