package com.igneko.aidirector.core.survivor;

import java.util.UUID;

/**
 * Modelo de dominio de la salud híbrida L4D2 (Permanente + Temporal), incapacitación,
 * sangrado, reanimación y estado Blanco y Negro.
 *
 * <p>Java puro, sin dependencias de Minecraft: toda la lógica es determinista y testeable.
 * El servidor es la única autoridad que lo muta; el cliente solo recibe copias por red.</p>
 */
public final class SurvivorHealthData {

    public enum Outcome { NONE, DAMAGED, INCAPACITATED, KILLED }

    private float permHealth;
    private float tempHealth;
    private SurvivorState state = SurvivorState.ALIVE;
    private int incapCount;
    private float bleedHealth;
    private int adrenalineTicks;

    // Estado transitorio de reanimación (no se persiste en disco).
    private UUID reviverId;
    private float reviveProgress;
    private int reviveElapsedTicks;

    private boolean dirty = true;

    public SurvivorHealthData() {
        this(SurvivorRules.DEFAULTS);
    }

    public SurvivorHealthData(SurvivorRules rules) {
        this.permHealth = rules.maxHealth();
    }

    // ------------------------------------------------------------------ consultas

    public float permHealth() { return permHealth; }
    public float tempHealth() { return tempHealth; }
    public float totalHealth() { return permHealth + tempHealth; }
    public SurvivorState state() { return state; }
    public int incapCount() { return incapCount; }
    public float bleedHealth() { return bleedHealth; }
    public int adrenalineTicks() { return adrenalineTicks; }
    public boolean hasAdrenaline() { return adrenalineTicks > 0; }
    public UUID reviverId() { return reviverId; }
    public float reviveProgress() { return reviveProgress; }
    public boolean isBeingRevived() { return reviverId != null; }

    public boolean isAlive() { return state == SurvivorState.ALIVE; }
    public boolean isIncapacitated() { return state == SurvivorState.INCAPACITATED; }
    public boolean isDead() { return state == SurvivorState.DEAD; }

    /** Tercera vida: la siguiente caída es mortal. */
    public boolean isBlackAndWhite(SurvivorRules rules) {
        return state == SurvivorState.ALIVE && incapCount >= rules.maxIncaps();
    }

    /** Multiplicador de velocidad (Limp Mode). 0 si está incapacitado o muerto. */
    public float speedMultiplier(SurvivorRules rules) {
        if (state != SurvivorState.ALIVE) return 0f;
        if (hasAdrenaline()) return 1f; // la adrenalina anula la cojera, como en L4D2
        float total = totalHealth();
        if (total >= rules.maxHealth() * rules.limpThresholdRatio()) return 1f;
        if (total <= rules.maxHealth() * 0.01f) return rules.severeLimpSpeed();
        return rules.limpSpeed();
    }

    /**
     * Salud que debe mostrar la barra vanilla (espejo). Incapacitado: proporcional al sangrado.
     * Nunca devuelve 0 mientras no esté muerto, para que vanilla no dispare la muerte por su cuenta.
     */
    public float vanillaHealth(SurvivorRules rules, float vanillaMax) {
        float ratio = switch (state) {
            case ALIVE -> totalHealth() / rules.maxHealth();
            case INCAPACITATED -> bleedHealth / rules.bleedPool();
            case DEAD -> 0f;
        };
        if (state == SurvivorState.DEAD) return 0f;
        return Math.max(0.5f, Math.min(vanillaMax, ratio * vanillaMax));
    }

    public boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    public void markDirty() { dirty = true; }

    // ------------------------------------------------------------------ comandos de dominio

    /** Aplica daño en puntos L4D2. El daño se descuenta primero de la salud temporal. */
    public Outcome applyDamage(SurvivorRules rules, float amount) {
        if (amount <= 0f || state == SurvivorState.DEAD) return Outcome.NONE;
        dirty = true;

        if (state == SurvivorState.INCAPACITATED) {
            bleedHealth -= amount * rules.incapDamageMultiplier();
            if (bleedHealth <= 0f) return kill();
            return Outcome.DAMAGED;
        }

        float fromTemp = Math.min(tempHealth, amount);
        tempHealth -= fromTemp;
        permHealth -= amount - fromTemp;

        if (permHealth + tempHealth <= 0f) {
            permHealth = 0f;
            tempHealth = 0f;
            return incapCount < rules.maxIncaps() ? incapacitate(rules) : kill();
        }
        return Outcome.DAMAGED;
    }

    /** Un tick de servidor (1/20 s): decaimiento temporal, sangrado y adrenalina. */
    public Outcome tick(SurvivorRules rules) {
        if (state == SurvivorState.DEAD) return Outcome.NONE;
        float dt = 1f / SurvivorRules.TICKS_PER_SECOND;

        if (adrenalineTicks > 0) {
            adrenalineTicks--;
            if (adrenalineTicks == 0) dirty = true;
        }

        if (state == SurvivorState.ALIVE && tempHealth > 0f) {
            tempHealth = Math.max(0f, tempHealth - rules.tempDecayPerSecond() * dt);
            // La barra temporal no se sincroniza cada tick: el dueño del dato decide la cadencia.
        }

        if (state == SurvivorState.INCAPACITATED && !isBeingRevived()) {
            bleedHealth -= rules.bleedPerSecond() * dt;
            if (bleedHealth <= 0f) return kill();
        }
        return Outcome.NONE;
    }

    /** Fuerza la incapacitación (comandos, Charger, caída...). Respeta el límite de caídas. */
    public Outcome forceIncapacitate(SurvivorRules rules) {
        if (state != SurvivorState.ALIVE) return Outcome.NONE;
        permHealth = 0f;
        tempHealth = 0f;
        return incapCount < rules.maxIncaps() ? incapacitate(rules) : kill();
    }

    /** Muerte definitiva decidida fuera del flujo normal (vacío, /kill). */
    public Outcome forceDead() {
        return state == SurvivorState.DEAD ? Outcome.NONE : kill();
    }

    /** Añade salud temporal respetando el tope Hperm + Htemp ≤ Hmax. */
    public void addTempHealth(SurvivorRules rules, float amount) {
        if (state != SurvivorState.ALIVE || amount <= 0f) return;
        tempHealth = Math.min(tempHealth + amount, rules.maxHealth() - permHealth);
        dirty = true;
    }

    public void usePainPills(SurvivorRules rules) {
        addTempHealth(rules, rules.pillsTempHealth());
    }

    public void useAdrenaline(SurvivorRules rules) {
        if (state != SurvivorState.ALIVE) return;
        addTempHealth(rules, rules.adrenalineTempHealth());
        adrenalineTicks = Math.round(rules.adrenalineSeconds() * SurvivorRules.TICKS_PER_SECOND);
        dirty = true;
    }

    /** Botiquín: restaura el 80 % de la salud faltante y elimina el estado Blanco y Negro. */
    public void useFirstAid(SurvivorRules rules) {
        if (state != SurvivorState.ALIVE) return;
        permHealth = permHealth + rules.firstAidRatio() * (rules.maxHealth() - permHealth);
        tempHealth = Math.min(tempHealth, rules.maxHealth() - permHealth);
        incapCount = 0;
        dirty = true;
    }

    // ------------------------------------------------------------------ reanimación

    /** Inicia o continúa una reanimación. Devuelve false si otro superviviente ya está reanimando. */
    public boolean beginRevive(UUID reviver) {
        if (state != SurvivorState.INCAPACITATED) return false;
        if (reviverId != null && !reviverId.equals(reviver)) return false;
        if (reviverId == null) {
            reviverId = reviver;
            reviveProgress = 0f;
        reviveElapsedTicks = 0;
            dirty = true;
        }
        return true;
    }

    /** Avanza la reanimación un tick. Devuelve true cuando se completa. */
    public boolean advanceRevive(SurvivorRules rules, boolean reviverHasAdrenaline) {
        if (state != SurvivorState.INCAPACITATED || reviverId == null) return false;
        int needed = rules.reviveTicks(reviverHasAdrenaline);
        reviveElapsedTicks++;
        reviveProgress = Math.min(1f, (float) reviveElapsedTicks / needed);
        if (reviveElapsedTicks >= needed) {
            completeRevive(rules);
            return true;
        }
        return false;
    }

    public void cancelRevive() {
        if (reviverId == null) return;
        reviverId = null;
        reviveProgress = 0f;
        reviveElapsedTicks = 0;
        dirty = true;
    }

    /** Levantamiento completado: incapCount++, Hperm = 1, Htemp = 30. */
    public void completeRevive(SurvivorRules rules) {
        if (state != SurvivorState.INCAPACITATED) return;
        state = SurvivorState.ALIVE;
        incapCount++;
        permHealth = rules.revivePermHealth();
        tempHealth = Math.min(rules.reviveTempHealth(), rules.maxHealth() - permHealth);
        bleedHealth = 0f;
        reviverId = null;
        reviveProgress = 0f;
        reviveElapsedTicks = 0;
        dirty = true;
    }

    /** Estado limpio tras reaparecer. */
    public void reset(SurvivorRules rules) {
        permHealth = rules.maxHealth();
        tempHealth = 0f;
        state = SurvivorState.ALIVE;
        incapCount = 0;
        bleedHealth = 0f;
        adrenalineTicks = 0;
        reviverId = null;
        reviveProgress = 0f;
        reviveElapsedTicks = 0;
        dirty = true;
    }

    /** Asignación directa (persistencia, red y comandos de depuración). */
    public void restore(float perm, float temp, SurvivorState state, int incapCount, float bleed,
                        int adrenalineTicks, float reviveProgress, boolean beingRevived) {
        this.permHealth = Math.max(0f, perm);
        this.tempHealth = Math.max(0f, temp);
        this.state = state;
        this.incapCount = Math.max(0, incapCount);
        this.bleedHealth = Math.max(0f, bleed);
        this.adrenalineTicks = Math.max(0, adrenalineTicks);
        this.reviveProgress = reviveProgress;
        // En el cliente no conocemos el UUID del reanimador: basta con un marcador.
        this.reviverId = beingRevived ? (reviverId != null ? reviverId : CLIENT_REVIVER) : null;
        this.dirty = true;
    }

    /** Copia profunda (PlayerEvent.Clone al cambiar de dimensión). */
    public void copyFrom(SurvivorHealthData other) {
        restore(other.permHealth, other.tempHealth, other.state, other.incapCount, other.bleedHealth,
                other.adrenalineTicks, 0f, false);
    }

    private Outcome incapacitate(SurvivorRules rules) {
        state = SurvivorState.INCAPACITATED;
        bleedHealth = rules.bleedPool();
        reviverId = null;
        reviveProgress = 0f;
        reviveElapsedTicks = 0;
        dirty = true;
        return Outcome.INCAPACITATED;
    }

    private Outcome kill() {
        state = SurvivorState.DEAD;
        permHealth = 0f;
        tempHealth = 0f;
        bleedHealth = 0f;
        reviverId = null;
        reviveProgress = 0f;
        reviveElapsedTicks = 0;
        dirty = true;
        return Outcome.KILLED;
    }

    private static final UUID CLIENT_REVIVER = new UUID(0L, 0L);
}
