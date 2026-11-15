package com.igneko.aidirector.core.combat;

/**
 * Fatiga de empujón estilo L4D2 (Java puro). Los primeros {@code quickShoves} empujones consecutivos
 * tienen un enfriamiento corto; a partir del siguiente el enfriamiento crece progresivamente
 * hasta un máximo. El contador se reinicia tras {@code resetTicks} sin empujar.
 */
public final class ShoveFatigue {

    public record Rules(int quickShoves, int quickCooldownTicks, int fatigueBaseTicks,
                        int fatigueStepTicks, int fatigueMaxTicks, int resetTicks) {
        /** 4 rápidos a 0,5 s; luego 1,5 s → 2,0 s; reinicio a los 4 s. */
        public static final Rules DEFAULTS = new Rules(4, 10, 30, 5, 40, 80);

        int cooldownFor(int shoveNumber) {
            if (shoveNumber <= quickShoves) return quickCooldownTicks;
            return Math.min(fatigueMaxTicks, fatigueBaseTicks + (shoveNumber - quickShoves - 1) * fatigueStepTicks);
        }
    }

    private int consecutive;
    private long lastShoveTick = Long.MIN_VALUE / 2;
    private long nextAllowedTick = Long.MIN_VALUE / 2;
    private int lastCooldown;

    /** Intenta empujar en el tick {@code now}. Devuelve false si sigue en enfriamiento. */
    public boolean tryShove(Rules rules, long now) {
        if (now < nextAllowedTick) return false;
        if (now - lastShoveTick >= rules.resetTicks()) consecutive = 0;
        consecutive++;
        lastCooldown = rules.cooldownFor(consecutive);
        lastShoveTick = now;
        nextAllowedTick = now + lastCooldown;
        return true;
    }

    public int consecutive() { return consecutive; }
    public int lastCooldown() { return lastCooldown; }
    public long nextAllowedTick() { return nextAllowedTick; }

    public boolean isFatigued(Rules rules) {
        return consecutive > rules.quickShoves();
    }
}
