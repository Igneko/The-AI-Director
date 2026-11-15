package com.igneko.aidirector.config;

import com.igneko.aidirector.core.combat.ShoveFatigue;
import com.igneko.aidirector.core.survivor.SurvivorRules;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;

/**
 * Configuración COMMON del mod. Los valores de balance se traducen a objetos de valor inmutables
 * ({@link SurvivorRules}, {@link CombatSettings}, {@link TaczSettings}) para que la lógica no dependa de Forge.
 */
public final class AIDirectorConfig {

    public static final ForgeConfigSpec SPEC;

    // Superviviente
    private static final ForgeConfigSpec.DoubleValue TEMP_DECAY;
    private static final ForgeConfigSpec.DoubleValue BLEED_POOL;
    private static final ForgeConfigSpec.DoubleValue BLEED_PER_SECOND;
    private static final ForgeConfigSpec.DoubleValue INCAP_DAMAGE_MULT;
    private static final ForgeConfigSpec.IntValue MAX_INCAPS;
    private static final ForgeConfigSpec.DoubleValue REVIVE_SECONDS;
    private static final ForgeConfigSpec.DoubleValue REVIVE_SECONDS_ADRENALINE;
    private static final ForgeConfigSpec.DoubleValue REVIVE_RANGE;
    private static final ForgeConfigSpec.DoubleValue PILLS;
    private static final ForgeConfigSpec.DoubleValue ADRENALINE;
    private static final ForgeConfigSpec.DoubleValue ADRENALINE_SECONDS;
    private static final ForgeConfigSpec.DoubleValue FIRST_AID_RATIO;
    private static final ForgeConfigSpec.DoubleValue LIMP_SPEED;
    private static final ForgeConfigSpec.DoubleValue SEVERE_LIMP_SPEED;
    private static final ForgeConfigSpec.BooleanValue BLOCK_VANILLA_HEALING;

    // Combate
    private static final ForgeConfigSpec.DoubleValue SHOVE_RANGE;
    private static final ForgeConfigSpec.DoubleValue SHOVE_ANGLE;
    private static final ForgeConfigSpec.DoubleValue SHOVE_KNOCKBACK;
    private static final ForgeConfigSpec.DoubleValue STAGGER_SECONDS;
    private static final ForgeConfigSpec.DoubleValue CHAIN_RADIUS;
    private static final ForgeConfigSpec.IntValue QUICK_SHOVES;
    private static final ForgeConfigSpec.DoubleValue QUICK_COOLDOWN;
    private static final ForgeConfigSpec.DoubleValue FATIGUE_MIN;
    private static final ForgeConfigSpec.DoubleValue FATIGUE_MAX;
    private static final ForgeConfigSpec.DoubleValue FATIGUE_RESET;
    private static final ForgeConfigSpec.BooleanValue TACZ_MELEE_SHOVES;

    // TaCZ
    private static final ForgeConfigSpec.DoubleValue NOISE_LIGHT;
    private static final ForgeConfigSpec.DoubleValue NOISE_MEDIUM;
    private static final ForgeConfigSpec.DoubleValue NOISE_HEAVY;
    private static final ForgeConfigSpec.DoubleValue NOISE_SUPPRESSED;
    private static final ForgeConfigSpec.IntValue NOISE_COOLDOWN;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> LIGHT_TYPES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEAVY_TYPES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> INCAP_TYPES;
    private static final ForgeConfigSpec.ConfigValue<String> BACKUP_PISTOL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PILE_EXCLUDED_TYPES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PILE_EXCLUDED_GUNS;
    private static final ForgeConfigSpec.IntValue RESERVE_MAGAZINES;

    public record CombatSettings(double shoveRange, double shoveHalfAngleCos, double knockback, int staggerTicks,
                                 double chainRadius, ShoveFatigue.Rules fatigue, boolean taczMeleeShoves) {}

    public record TaczSettings(double noiseLight, double noiseMedium, double noiseHeavy, double noiseSuppressed,
                               int noiseCooldownTicks, Set<String> lightTypes, Set<String> heavyTypes,
                               Set<String> incapTypes, String backupPistol, Set<String> pileExcludedTypes,
                               Set<String> pileExcludedGuns, int reserveMagazines) {}

    private static volatile SurvivorRules rules = SurvivorRules.DEFAULTS;
    private static volatile double reviveRange = 2.5;
    private static volatile boolean blockVanillaHealing = true;
    private static volatile CombatSettings combat = new CombatSettings(2.8, Math.cos(Math.toRadians(45)), 0.8, 30,
            1.2, ShoveFatigue.Rules.DEFAULTS, false);
    private static volatile TaczSettings tacz = new TaczSettings(45, 55, 65, 12, 10,
            Set.of("pistol", "smg"), Set.of("shotgun", "sniper", "mg", "rpg"), Set.of("pistol"),
            "tacz:glock_17", Set.of("rpg", "mg"), Set.of(), 6);

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SurvivorRules d = SurvivorRules.DEFAULTS;

        b.comment("Sistema de salud híbrida estilo L4D2 (escala 100 HP; 1 HP vanilla = 5 HP L4D2 con 20 de vida máxima)")
                .push("survivor");
        TEMP_DECAY = b.comment("Puntos de salud temporal perdidos por segundo")
                .defineInRange("tempDecayPerSecond", d.tempDecayPerSecond(), 0.0, 10.0);
        BLEED_POOL = b.comment("Reserva de sangrado al quedar incapacitado")
                .defineInRange("bleedPool", d.bleedPool(), 1.0, 10000.0);
        BLEED_PER_SECOND = b.defineInRange("bleedPerSecond", d.bleedPerSecond(), 0.0, 100.0);
        INCAP_DAMAGE_MULT = b.comment("Multiplicador del daño recibido en el suelo (se resta del sangrado)")
                .defineInRange("incapDamageMultiplier", d.incapDamageMultiplier(), 0.0, 100.0);
        MAX_INCAPS = b.comment("Incapacitaciones permitidas antes de que la siguiente caída sea mortal")
                .defineInRange("maxIncaps", d.maxIncaps(), 0, 10);
        REVIVE_SECONDS = b.defineInRange("reviveSeconds", d.reviveSeconds(), 0.05, 60.0);
        REVIVE_SECONDS_ADRENALINE = b.defineInRange("reviveSecondsAdrenaline", d.reviveSecondsAdrenaline(), 0.05, 60.0);
        REVIVE_RANGE = b.comment("Distancia máxima (bloques) entre reanimador y caído")
                .defineInRange("reviveRange", 2.5, 1.0, 8.0);
        PILLS = b.defineInRange("pillsTempHealth", d.pillsTempHealth(), 0.0, 100.0);
        ADRENALINE = b.defineInRange("adrenalineTempHealth", d.adrenalineTempHealth(), 0.0, 100.0);
        ADRENALINE_SECONDS = b.defineInRange("adrenalineSeconds", d.adrenalineSeconds(), 0.0, 120.0);
        FIRST_AID_RATIO = b.comment("Fracción de la salud faltante que restaura un botiquín")
                .defineInRange("firstAidRatio", d.firstAidRatio(), 0.0, 1.0);
        LIMP_SPEED = b.defineInRange("limpSpeed", d.limpSpeed(), 0.05, 1.0);
        SEVERE_LIMP_SPEED = b.defineInRange("severeLimpSpeed", d.severeLimpSpeed(), 0.05, 1.0);
        BLOCK_VANILLA_HEALING = b.comment("Bloquea la regeneración vanilla (hambre, pociones): la salud permanente solo se recupera con botiquines")
                .define("blockVanillaHealing", true);
        b.pop();

        b.comment("Empujón táctico (Shove) y fatiga").push("combat");
        SHOVE_RANGE = b.defineInRange("shoveRange", 2.8, 1.0, 6.0);
        SHOVE_ANGLE = b.comment("Apertura total del cono frontal en grados")
                .defineInRange("shoveConeDegrees", 90.0, 10.0, 180.0);
        SHOVE_KNOCKBACK = b.defineInRange("shoveKnockback", 0.8, 0.0, 4.0);
        STAGGER_SECONDS = b.defineInRange("staggerSeconds", 1.5, 0.1, 10.0);
        CHAIN_RADIUS = b.comment("Radio en el que un infectado empujado hace tambalear a los que tiene detrás")
                .defineInRange("chainRadius", 1.2, 0.0, 4.0);
        QUICK_SHOVES = b.defineInRange("quickShoves", 4, 1, 20);
        QUICK_COOLDOWN = b.defineInRange("quickCooldownSeconds", 0.5, 0.05, 5.0);
        FATIGUE_MIN = b.defineInRange("fatigueCooldownMinSeconds", 1.5, 0.05, 10.0);
        FATIGUE_MAX = b.defineInRange("fatigueCooldownMaxSeconds", 2.0, 0.05, 10.0);
        FATIGUE_RESET = b.defineInRange("fatigueResetSeconds", 4.0, 0.5, 30.0);
        TACZ_MELEE_SHOVES = b.comment("Si es true, el culatazo de TaCZ (tecla V) también ejecuta un empujón")
                .define("taczMeleeTriggersShove", false);
        b.pop();

        b.comment("Integración con TaCZ").push("tacz");
        NOISE_LIGHT = b.comment("Radio acústico de armas ligeras (pistola, subfusil)")
                .defineInRange("noiseRadiusLight", 45.0, 0.0, 256.0);
        NOISE_MEDIUM = b.comment("Radio acústico de fusiles y demás tipos")
                .defineInRange("noiseRadiusMedium", 55.0, 0.0, 256.0);
        NOISE_HEAVY = b.comment("Radio acústico de escopetas, francotiradores y armas pesadas")
                .defineInRange("noiseRadiusHeavy", 65.0, 0.0, 256.0);
        NOISE_SUPPRESSED = b.defineInRange("noiseRadiusSuppressed", 12.0, 0.0, 256.0);
        NOISE_COOLDOWN = b.comment("Ticks mínimos entre pulsos acústicos del mismo tirador (evita saturar con armas automáticas)")
                .defineInRange("noiseCooldownTicks", 10, 0, 200);
        LIGHT_TYPES = b.defineListAllowEmpty("lightGunTypes", List.of("pistol", "smg"), o -> o instanceof String);
        HEAVY_TYPES = b.defineListAllowEmpty("heavyGunTypes", List.of("shotgun", "sniper", "mg", "rpg"), o -> o instanceof String);
        INCAP_TYPES = b.comment("Tipos de arma TaCZ que se pueden usar estando incapacitado")
                .defineListAllowEmpty("incapGunTypes", List.of("pistol"), o -> o instanceof String);
        BACKUP_PISTOL = b.comment("Pistola de respaldo que se entrega al caer si no llevas ninguna (vacío = ninguna)")
                .define("backupPistol", "tacz:glock_17");
        PILE_EXCLUDED_TYPES = b.comment("Tipos de arma que NO se recargan en montones de munición (nivel 3)")
                .defineListAllowEmpty("ammoPileExcludedTypes", List.of("rpg", "mg"), o -> o instanceof String);
        PILE_EXCLUDED_GUNS = b.comment("IDs concretos de arma excluidos de los montones de munición")
                .defineListAllowEmpty("ammoPileExcludedGuns", List.of(), o -> o instanceof String);
        RESERVE_MAGAZINES = b.comment("Reserva máxima de munición = tamaño de cargador x este valor")
                .defineInRange("reserveMagazines", 6, 1, 64);
        b.pop();

        SPEC = b.build();
    }

    private AIDirectorConfig() {}

    public static SurvivorRules rules() { return rules; }
    public static double reviveRange() { return reviveRange; }
    public static boolean blockVanillaHealing() { return blockVanillaHealing; }
    public static CombatSettings combat() { return combat; }
    public static TaczSettings tacz() { return tacz; }

    private static Set<String> set(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        return Set.copyOf(value.get().stream().map(String::toLowerCase).toList());
    }

    private static int ticks(ForgeConfigSpec.DoubleValue seconds) {
        return Math.max(1, (int) Math.round(seconds.get() * SurvivorRules.TICKS_PER_SECOND));
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        SurvivorRules d = SurvivorRules.DEFAULTS;
        rules = new SurvivorRules(
                d.maxHealth(),
                TEMP_DECAY.get().floatValue(),
                BLEED_POOL.get().floatValue(),
                BLEED_PER_SECOND.get().floatValue(),
                INCAP_DAMAGE_MULT.get().floatValue(),
                MAX_INCAPS.get(),
                REVIVE_SECONDS.get().floatValue(),
                REVIVE_SECONDS_ADRENALINE.get().floatValue(),
                d.revivePermHealth(),
                d.reviveTempHealth(),
                PILLS.get().floatValue(),
                ADRENALINE.get().floatValue(),
                ADRENALINE_SECONDS.get().floatValue(),
                FIRST_AID_RATIO.get().floatValue(),
                d.limpThresholdRatio(),
                LIMP_SPEED.get().floatValue(),
                SEVERE_LIMP_SPEED.get().floatValue()
        );
        reviveRange = REVIVE_RANGE.get();
        blockVanillaHealing = BLOCK_VANILLA_HEALING.get();

        int fatigueMin = ticks(FATIGUE_MIN);
        int fatigueMax = Math.max(fatigueMin, ticks(FATIGUE_MAX));
        combat = new CombatSettings(SHOVE_RANGE.get(), Math.cos(Math.toRadians(SHOVE_ANGLE.get() / 2.0)),
                SHOVE_KNOCKBACK.get(), ticks(STAGGER_SECONDS), CHAIN_RADIUS.get(),
                new ShoveFatigue.Rules(QUICK_SHOVES.get(), ticks(QUICK_COOLDOWN), fatigueMin,
                        Math.max(1, (fatigueMax - fatigueMin) / 2), fatigueMax, ticks(FATIGUE_RESET)),
                TACZ_MELEE_SHOVES.get());

        tacz = new TaczSettings(NOISE_LIGHT.get(), NOISE_MEDIUM.get(), NOISE_HEAVY.get(), NOISE_SUPPRESSED.get(),
                NOISE_COOLDOWN.get(), set(LIGHT_TYPES), set(HEAVY_TYPES), set(INCAP_TYPES), BACKUP_PISTOL.get().trim(),
                set(PILE_EXCLUDED_TYPES), set(PILE_EXCLUDED_GUNS), RESERVE_MAGAZINES.get());
    }
}
