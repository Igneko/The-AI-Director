package com.igneko.aidirector.core.survivor;

/**
 * Parámetros de balance del sistema de salud híbrida (escala L4D2: 100 HP).
 * Objeto de valor inmutable: el dominio no depende de Forge; la configuración construye una instancia.
 */
public record SurvivorRules(
        float maxHealth,
        float tempDecayPerSecond,
        float bleedPool,
        float bleedPerSecond,
        float incapDamageMultiplier,
        int maxIncaps,
        float reviveSeconds,
        float reviveSecondsAdrenaline,
        float revivePermHealth,
        float reviveTempHealth,
        float pillsTempHealth,
        float adrenalineTempHealth,
        float adrenalineSeconds,
        float firstAidRatio,
        float limpThresholdRatio,
        float limpSpeed,
        float severeLimpSpeed
) {
    public static final int TICKS_PER_SECOND = 20;

    public static final SurvivorRules DEFAULTS = new SurvivorRules(
            100f,   // maxHealth
            0.2f,   // tempDecayPerSecond (1 punto cada 5 s)
            300f,   // bleedPool
            1f,     // bleedPerSecond
            1f,     // incapDamageMultiplier
            2,      // maxIncaps (la tercera caída mata)
            5f,     // reviveSeconds
            2.5f,   // reviveSecondsAdrenaline
            1f,     // revivePermHealth
            30f,    // reviveTempHealth
            50f,    // pillsTempHealth
            25f,    // adrenalineTempHealth
            15f,    // adrenalineSeconds
            0.8f,   // firstAidRatio
            0.4f,   // limpThresholdRatio
            0.85f,  // limpSpeed
            0.65f   // severeLimpSpeed
    );

    public int reviveTicks(boolean adrenaline) {
        return Math.max(1, Math.round((adrenaline ? reviveSecondsAdrenaline : reviveSeconds) * TICKS_PER_SECOND));
    }
}
