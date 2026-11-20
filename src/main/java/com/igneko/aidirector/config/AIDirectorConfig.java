package com.igneko.aidirector.config;

import com.igneko.aidirector.core.combat.ShoveFatigue;
import com.igneko.aidirector.core.director.DirectorFsm;
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
    private static final ForgeConfigSpec.BooleanValue PILE_SINGLE_USE;

    // Hordas
    private static final ForgeConfigSpec.IntValue HORDE_SIZE;
    private static final ForgeConfigSpec.IntValue HORDE_AREA;
    private static final ForgeConfigSpec.DoubleValue HORDE_MIN_DISTANCE;
    private static final ForgeConfigSpec.IntValue HORDE_VERTICAL_SEARCH;
    private static final ForgeConfigSpec.DoubleValue HORDE_SPEED_BONUS;
    private static final ForgeConfigSpec.BooleanValue HORDE_FIRE_IMMUNE;
    private static final ForgeConfigSpec.IntValue HORDE_SAFE_RADIUS;
    private static final ForgeConfigSpec.IntValue HORDE_WARNING;
    private static final ForgeConfigSpec.IntValue HORDE_BATCHES;

    // Infectados especiales
    private static final ForgeConfigSpec.DoubleValue INF_HEALTH_MULT;
    private static final ForgeConfigSpec.DoubleValue INF_DAMAGE_MULT;
    private static final ForgeConfigSpec.DoubleValue BEHEMOTH_HEALTH;
    private static final ForgeConfigSpec.BooleanValue TEAM_WIPE;
    private static final ForgeConfigSpec.IntValue TEAM_WIPE_MIN;
    private static final ForgeConfigSpec.IntValue BILE_HORDE_SIZE;

    public record InfectedSettings(double healthMultiplier, double damageMultiplier, double behemothHealth,
                                   boolean teamWipe, int teamWipeMinPlayers, int bileHordeSize) {}

    // Aparición natural de especiales
    private static final ForgeConfigSpec.BooleanValue SPAWN_ENABLED;
    private static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    private static final ForgeConfigSpec.IntValue SPAWN_MAX_NEARBY;
    private static final ForgeConfigSpec.IntValue SPAWN_MIN_SECONDS;
    private static final ForgeConfigSpec.IntValue SPAWN_RELAX_SECONDS;
    private static final ForgeConfigSpec.DoubleValue BEHEMOTH_CHANCE;
    private static final ForgeConfigSpec.IntValue BEHEMOTH_COOLDOWN;

    // Director (máquina de estados del ritmo)
    private static final ForgeConfigSpec.BooleanValue DIR_ENABLED;
    private static final ForgeConfigSpec.DoubleValue DIR_PEAK;
    private static final ForgeConfigSpec.IntValue DIR_BUILDUP_MAX;
    private static final ForgeConfigSpec.IntValue DIR_SUSTAIN_MIN;
    private static final ForgeConfigSpec.IntValue DIR_SUSTAIN_MAX;
    private static final ForgeConfigSpec.IntValue DIR_FADE_THREATS;
    private static final ForgeConfigSpec.IntValue DIR_FADE_MAX;
    private static final ForgeConfigSpec.IntValue DIR_RELAX_MIN;
    private static final ForgeConfigSpec.IntValue DIR_RELAX_MAX;
    private static final ForgeConfigSpec.DoubleValue DIR_RELAX_TRAVEL;
    private static final ForgeConfigSpec.BooleanValue DIR_TRICKLE;
    private static final ForgeConfigSpec.IntValue DIR_TRICKLE_SECONDS;
    private static final ForgeConfigSpec.IntValue DIR_TRICKLE_MAX;
    private static final ForgeConfigSpec.DoubleValue DIR_RELAX_SUPPRESSION;

    public record DirectorSettings(boolean enabled, DirectorFsm.Rules fsm, boolean commonsTrickle,
                                   int trickleIntervalSeconds, int trickleMaxSize, double relaxSpawnSuppression) {}

    // Objetos tácticos
    private static final ForgeConfigSpec.IntValue MOLOTOV_SECONDS;
    private static final ForgeConfigSpec.DoubleValue MOLOTOV_RADIUS;
    private static final ForgeConfigSpec.DoubleValue MOLOTOV_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue MOLOTOV_FRIENDLY;
    private static final ForgeConfigSpec.BooleanValue MOLOTOV_IGNITES;
    private static final ForgeConfigSpec.DoubleValue DEFIB_SECONDS;
    private static final ForgeConfigSpec.DoubleValue DEFIB_HEALTH;
    private static final ForgeConfigSpec.IntValue BODY_MINUTES;

    public record ItemSettings(int molotovSeconds, float molotovRadius, float molotovDamage, float molotovFriendlyDamage,
                               boolean molotovIgnitesBlocks, int defibTicks, float defibHealth, int bodyMinutes) {}

    // Refugios y eventos Crescendo
    private static final ForgeConfigSpec.BooleanValue SR_PROTECT;
    private static final ForgeConfigSpec.BooleanValue SR_REPAIR;
    private static final ForgeConfigSpec.BooleanValue SR_RESET_BW;
    private static final ForgeConfigSpec.BooleanValue SR_REVIVE_DOWNED;
    private static final ForgeConfigSpec.DoubleValue SR_MIN_HEALTH;
    private static final ForgeConfigSpec.BooleanValue SR_SET_SPAWN;
    private static final ForgeConfigSpec.BooleanValue SR_RESCUE_DEAD;
    private static final ForgeConfigSpec.IntValue SR_COOLDOWN;
    private static final ForgeConfigSpec.IntValue SR_TEAM_RADIUS;
    private static final ForgeConfigSpec.IntValue CRESCENDO_SECONDS;
    private static final ForgeConfigSpec.IntValue CRESCENDO_WAVE_SECONDS;
    private static final ForgeConfigSpec.IntValue CRESCENDO_WAVE_SIZE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARMORY_TIER1;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARMORY_TIER2;

    public record SafeRoomSettings(boolean protectBlocks, boolean selfRepair, boolean resetBlackAndWhite,
                                   boolean reviveDowned, float minHealth, boolean setSpawn, boolean rescueDead,
                                   int cooldownMinutes, int teamRadius, int crescendoSeconds, int crescendoWaveSeconds,
                                   int crescendoWaveSize, List<String> tier1Guns, List<String> tier2Guns) {}

    // Escuadrón
    private static final ForgeConfigSpec.IntValue SQUAD_RANGE;
    private static final ForgeConfigSpec.BooleanValue SQUAD_TEAM_ONLY;
    private static final ForgeConfigSpec.BooleanValue SQUAD_OUTLINES;
    private static final ForgeConfigSpec.BooleanValue SQUAD_MARKERS;

    public record SquadSettings(int range, boolean sameTeamOnly, boolean outlines, boolean markers) {}

    public record SpawnSettings(boolean enabled, double baseChance, int maxNearby, int minSecondsBetween,
                                int relaxSeconds, double behemothChance, int behemothCooldownMinutes) {}

    public record HordeSettings(int size, int areaSize, double minDistance, int verticalSearch,
                                double speedBonus, boolean fireImmune, int safeRadius, int warningSeconds, int batches) {

        /** Lado efectivo del área: siempre deja al menos 15 bloques de margen más allá del radio de seguridad. */
        public int effectiveArea() {
            return Math.max(areaSize, 2 * (safeRadius + 15));
        }
    }

    public record CombatSettings(double shoveRange, double shoveHalfAngleCos, double knockback, int staggerTicks,
                                 double chainRadius, ShoveFatigue.Rules fatigue, boolean taczMeleeShoves) {}

    public record TaczSettings(double noiseLight, double noiseMedium, double noiseHeavy, double noiseSuppressed,
                               int noiseCooldownTicks, Set<String> lightTypes, Set<String> heavyTypes,
                               Set<String> incapTypes, String backupPistol, Set<String> pileExcludedTypes,
                               Set<String> pileExcludedGuns, int reserveMagazines, boolean ammoPileSingleUse) {}

    private static volatile SurvivorRules rules = SurvivorRules.DEFAULTS;
    private static volatile double reviveRange = 2.5;
    private static volatile boolean blockVanillaHealing = true;
    private static volatile CombatSettings combat = new CombatSettings(2.8, Math.cos(Math.toRadians(45)), 0.8, 30,
            1.2, ShoveFatigue.Rules.DEFAULTS, false);
    private static volatile TaczSettings tacz = new TaczSettings(45, 55, 65, 12, 10,
            Set.of("pistol", "smg"), Set.of("shotgun", "sniper", "mg", "rpg"), Set.of("pistol"),
            "tacz:glock_17", Set.of("rpg", "mg"), Set.of(), 6, true);

    private static volatile SpawnSettings spawning = new SpawnSettings(true, 0.05, 3, 25, 35, 0.03, 15);
    private static volatile InfectedSettings infected = new InfectedSettings(1.0, 1.0, 800.0, true, 2, 15);
    private static volatile DirectorSettings director = new DirectorSettings(true, DirectorFsm.Rules.DEFAULTS, true, 25, 8, 0.75);
    private static volatile ItemSettings items = new ItemSettings(15, 3.2f, 8f, 0.6f, false, 60, 50f, 10);
    private static volatile SafeRoomSettings safeRoom = new SafeRoomSettings(true, true, true, true, 0f, true, true, 10, 160,
            60, 12, 10, List.of("tacz:uzi", "tacz:ump45", "tacz:hk_mp5a5", "tacz:m870", "tacz:db_short"),
            List.of("tacz:ak47", "tacz:m4a1", "tacz:scar_l", "tacz:spas_12", "tacz:m1014", "tacz:aug"));
    private static volatile SquadSettings squad = new SquadSettings(256, true, true, true);
    private static volatile HordeSettings horde = new HordeSettings(30, 60, 8.0, 12, 0.15, true, 15, 6, 3);

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
        PILE_SINGLE_USE = b.comment("Si es true, el montón de munición desaparece tras reponer munición una vez")
                .define("ammoPileSingleUse", true);
        b.pop();

        b.comment("Hordas (generadas por el Director; Enhanced Hordes les aplica su IA de horda)").push("horde");
        HORDE_SIZE = b.comment("Infectados por horda")
                .defineInRange("hordeSize", 30, 1, 300);
        HORDE_AREA = b.comment("Lado del cuadrado (bloques) alrededor del jugador donde aparece la horda")
                .defineInRange("hordeAreaSize", 60, 8, 128);
        HORDE_MIN_DISTANCE = b.comment("Distancia mínima al jugador para no aparecer encima de él")
                .defineInRange("hordeMinDistance", 8.0, 0.0, 60.0);
        HORDE_VERTICAL_SEARCH = b.comment("Bloques arriba/abajo que se exploran buscando suelo")
                .defineInRange("hordeVerticalSearch", 12, 1, 64);
        HORDE_SPEED_BONUS = b.comment("Bonificación de velocidad de los infectados de horda (0.15 = +15 %)")
                .defineInRange("hordeSpeedBonus", 0.15, 0.0, 3.0);
        HORDE_FIRE_IMMUNE = b.comment("Los infectados de horda no mueren quemados por el sol")
                .define("hordeFireImmune", true);
        HORDE_SAFE_RADIUS = b.comment("Radio (bloques) alrededor de CUALQUIER jugador en el que no aparece ningún infectado de horda")
                .defineInRange("hordeSafeRadius", 15, 0, 48);
        HORDE_WARNING = b.comment("Segundos de aviso (sirena y mensaje) entre que el Director decide la horda y su aparición")
                .defineInRange("hordeWarningSeconds", 6, 0, 30);
        HORDE_BATCHES = b.comment("La horda aparece en tandas, una cada pocos segundos, en lugar de toda de golpe")
                .defineInRange("hordeBatches", 3, 1, 8);
        b.pop();

        b.comment("Infectados especiales (vida y daño en puntos vanilla: 1 = medio corazón; L4D2 / 5)").push("infected");
        INF_HEALTH_MULT = b.defineInRange("healthMultiplier", 1.0, 0.1, 20.0);
        INF_DAMAGE_MULT = b.defineInRange("damageMultiplier", 1.0, 0.0, 20.0);
        BEHEMOTH_HEALTH = b.comment("Vida base del Coloso en dificultad normal (fácil x0.75, difícil x2)")
                .defineInRange("behemothHealth", 800.0, 50.0, 10000.0);
        TEAM_WIPE = b.comment("Derrota inmediata si todos los supervivientes están incapacitados o atrapados")
                .define("teamWipe", true);
        TEAM_WIPE_MIN = b.comment("Supervivientes mínimos para aplicar la derrota de equipo (en solitario se puede forcejear)")
                .defineInRange("teamWipeMinPlayers", 2, 1, 64);
        BILE_HORDE_SIZE = b.comment("Infectados que acuden al ser cubierto de bilis")
                .defineInRange("bileHordeSize", 15, 0, 200);
        b.pop();

        b.comment("Aparición natural de Infectados Especiales guiada por la intensidad (estrés) de cada jugador").push("spawning");
        SPAWN_ENABLED = b.define("naturalSpawning", true);
        SPAWN_CHANCE = b.comment("Probabilidad base por comprobación (cada 2 s); crece con el tiempo sin especiales y con el estrés")
                .defineInRange("baseChance", 0.05, 0.0, 1.0);
        SPAWN_MAX_NEARBY = b.comment("Máximo de especiales vivos a 64 bloques de un jugador")
                .defineInRange("maxNearby", 3, 0, 32);
        SPAWN_MIN_SECONDS = b.comment("Segundos mínimos entre especiales cerca del mismo jugador")
                .defineInRange("minSecondsBetween", 25, 0, 3600);
        SPAWN_RELAX_SECONDS = b.comment("Calma tras alcanzar el pico de estrés (sin especiales nuevos)")
                .defineInRange("relaxSeconds", 35, 0, 600);
        BEHEMOTH_CHANCE = b.comment("Probabilidad de que una aparición sea un Coloso (si se cumplen sus condiciones)")
                .defineInRange("behemothChance", 0.03, 0.0, 1.0);
        BEHEMOTH_COOLDOWN = b.comment("Minutos mínimos entre Colosos")
                .defineInRange("behemothCooldownMinutes", 15, 0, 1440);
        b.pop();

        b.comment("AI Director: máquina de estados del ritmo (BUILD_UP -> SUSTAIN_PEAK -> PEAK_FADE -> RELAX)").push("director");
        DirectorFsm.Rules fr = DirectorFsm.Rules.DEFAULTS;
        DIR_ENABLED = b.comment("Si es false, el Director no fuerza hordas ni grupos de comunes (los especiales siguen su propia regla)")
                .define("enabled", true);
        DIR_PEAK = b.comment("Intensidad del equipo que marca el pico de estrés")
                .defineInRange("peakIntensity", (double) fr.peak(), 0.1, 1.0);
        DIR_BUILDUP_MAX = b.comment("Segundos de construcción sin alcanzar el pico antes de forzar una horda")
                .defineInRange("buildUpMaxSeconds", fr.buildUpMaxSeconds(), 10, 3600);
        DIR_SUSTAIN_MIN = b.defineInRange("sustainPeakMinSeconds", fr.sustainMinSeconds(), 1, 120);
        DIR_SUSTAIN_MAX = b.defineInRange("sustainPeakMaxSeconds", fr.sustainMaxSeconds(), 1, 120);
        DIR_FADE_THREATS = b.comment("Amenazas activas cerca del equipo que se consideran 'casi 0' para pasar a la calma")
                .defineInRange("peakFadeThreats", fr.fadeThreats(), 0, 64);
        DIR_FADE_MAX = b.comment("Tope de seguridad del desvanecimiento del pico")
                .defineInRange("peakFadeMaxSeconds", fr.fadeMaxSeconds(), 5, 3600);
        DIR_RELAX_MIN = b.defineInRange("relaxMinSeconds", fr.relaxMinSeconds(), 0, 3600);
        DIR_RELAX_MAX = b.defineInRange("relaxMaxSeconds", fr.relaxMaxSeconds(), 0, 3600);
        DIR_RELAX_TRAVEL = b.comment("Bloques que debe avanzar el equipo para terminar la calma antes de tiempo")
                .defineInRange("relaxTravelBlocks", fr.relaxTravelBlocks(), 0.0, 10000.0);
        DIR_TRICKLE = b.comment("Durante la construcción aparecen pequeños grupos de comunes fuera de la vista")
                .define("commonsTrickle", true);
        DIR_TRICKLE_SECONDS = b.defineInRange("commonsTrickleIntervalSeconds", 25, 5, 3600);
        DIR_TRICKLE_MAX = b.comment("Tamaño máximo de esos grupos (crece con el tiempo de construcción)")
                .defineInRange("commonsTrickleMaxSize", 8, 1, 64);
        DIR_RELAX_SUPPRESSION = b.comment("Fracción de apariciones naturales de monstruos que se cancelan durante la calma cerca del equipo")
                .defineInRange("relaxSpawnSuppression", 0.75, 0.0, 1.0);
        b.pop();

        b.comment("Objetos tácticos: cóctel molotov y desfibrilador").push("items");
        MOLOTOV_SECONDS = b.comment("Duración de la zona de fuego del molotov")
                .defineInRange("molotovSeconds", 15, 1, 120);
        MOLOTOV_RADIUS = b.defineInRange("molotovRadius", 3.2, 0.5, 12.0);
        MOLOTOV_DAMAGE = b.comment("Daño (vanilla) por medio segundo a los infectados dentro del fuego")
                .defineInRange("molotovDamage", 8.0, 0.0, 200.0);
        MOLOTOV_FRIENDLY = b.comment("Daño (vanilla) por medio segundo a los supervivientes dentro del fuego (fuego amigo)")
                .defineInRange("molotovFriendlyDamage", 0.6, 0.0, 20.0);
        MOLOTOV_IGNITES = b.comment("Si es true, el molotov también prende fuego a los bloques (puede quemar construcciones)")
                .define("molotovIgnitesBlocks", false);
        DEFIB_SECONDS = b.comment("Segundos de uso canalizado del desfibrilador")
                .defineInRange("defibrillatorSeconds", 3.0, 0.1, 30.0);
        DEFIB_HEALTH = b.comment("Salud permanente (escala L4D2) con la que vuelve el reanimado")
                .defineInRange("defibrillatorHealth", 50.0, 1.0, 100.0);
        BODY_MINUTES = b.comment("Minutos que permanece el cuerpo de un superviviente muerto esperando un desfibrilador")
                .defineInRange("bodyMinutes", 10, 1, 1440);
        b.pop();

        b.comment("Refugios (Safe Rooms) y eventos Crescendo").push("saferoom");
        SR_PROTECT = b.comment("Los mobs (hordas incluidas) y las explosiones no pueden romper la estructura de un refugio; los jugadores sí pueden reformarla")
                .define("protectBlocks", true);
        SR_REPAIR = b.comment("El refugio se repara solo si algo (p. ej. la horda) arranca un bloque")
                .define("selfRepair", true);
        SR_RESET_BW = b.comment("Al asegurar el refugio se borra el estado Blanco y Negro (contador de caídas a 0)")
                .define("resetBlackAndWhite", true);
        SR_REVIVE_DOWNED = b.comment("Al asegurar el refugio se levanta a los incapacitados que estén dentro")
                .define("reviveDowned", true);
        SR_MIN_HEALTH = b.comment("Salud permanente mínima garantizada al asegurar el refugio (0 = desactivado)")
                .defineInRange("minHealth", 0.0, 0.0, 100.0);
        SR_SET_SPAWN = b.comment("El refugio asegurado se convierte en el punto de reaparición (checkpoint)")
                .define("setSpawnPoint", true);
        SR_RESCUE_DEAD = b.comment("Los supervivientes muertos que aún no hayan reaparecido vuelven dentro del refugio")
                .define("rescueDead", true);
        SR_COOLDOWN = b.comment("Minutos antes de poder volver a asegurar el mismo refugio")
                .defineInRange("checkpointCooldownMinutes", 10, 0, 1440);
        SR_TEAM_RADIUS = b.comment("Radio en el que deben estar los supervivientes vivos para contar como 'el equipo'")
                .defineInRange("teamRadius", 160, 8, 4096);
        CRESCENDO_SECONDS = b.comment("Duración por defecto de una alarma Crescendo")
                .defineInRange("crescendoSeconds", 60, 5, 3600);
        CRESCENDO_WAVE_SECONDS = b.comment("Segundos entre oleadas durante un Crescendo")
                .defineInRange("crescendoWaveSeconds", 12, 2, 600);
        CRESCENDO_WAVE_SIZE = b.defineInRange("crescendoWaveSize", 10, 1, 200);
        ARMORY_TIER1 = b.comment("Armas TaCZ de las cajas de armería normales")
                .defineListAllowEmpty("armoryTier1Guns", List.of("tacz:uzi", "tacz:ump45", "tacz:hk_mp5a5", "tacz:m870", "tacz:db_short"),
                        o -> o instanceof String);
        ARMORY_TIER2 = b.comment("Armas TaCZ de las cajas de armería cerradas (recompensa de un Crescendo)")
                .defineListAllowEmpty("armoryTier2Guns", List.of("tacz:ak47", "tacz:m4a1", "tacz:scar_l", "tacz:spas_12", "tacz:m1014", "tacz:aug"),
                        o -> o instanceof String);
        b.pop();

        b.comment("Panel de escuadrón, siluetas y marcadores").push("squad");
        SQUAD_RANGE = b.comment("Distancia máxima a la que aparecen los compañeros en el panel")
                .defineInRange("range", 256, 8, 30000000);
        SQUAD_TEAM_ONLY = b.comment("Con equipos de marcador (/team), mostrar solo a los del mismo equipo")
                .define("sameTeamOnly", true);
        SQUAD_OUTLINES = b.comment("Siluetas de color de los compañeros a través de las paredes")
                .define("outlines", true);
        SQUAD_MARKERS = b.comment("Marcadores visibles a través de las paredes sobre compañeros caídos, atrapados o muertos")
                .define("markers", true);
        b.pop();

        SPEC = b.build();
    }

    private AIDirectorConfig() {}

    public static SurvivorRules rules() { return rules; }
    public static double reviveRange() { return reviveRange; }
    public static boolean blockVanillaHealing() { return blockVanillaHealing; }
    public static CombatSettings combat() { return combat; }
    public static TaczSettings tacz() { return tacz; }
    public static HordeSettings horde() { return horde; }
    public static InfectedSettings infected() { return infected; }
    public static SpawnSettings spawning() { return spawning; }
    public static DirectorSettings director() { return director; }
    public static ItemSettings items() { return items; }
    public static SafeRoomSettings safeRoom() { return safeRoom; }
    public static SquadSettings squad() { return squad; }

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
                set(PILE_EXCLUDED_TYPES), set(PILE_EXCLUDED_GUNS), RESERVE_MAGAZINES.get(),
                PILE_SINGLE_USE.get());

        spawning = new SpawnSettings(SPAWN_ENABLED.get(), SPAWN_CHANCE.get(), SPAWN_MAX_NEARBY.get(), SPAWN_MIN_SECONDS.get(),
                SPAWN_RELAX_SECONDS.get(), BEHEMOTH_CHANCE.get(), BEHEMOTH_COOLDOWN.get());
        infected = new InfectedSettings(INF_HEALTH_MULT.get(), INF_DAMAGE_MULT.get(), BEHEMOTH_HEALTH.get(),
                TEAM_WIPE.get(), TEAM_WIPE_MIN.get(), BILE_HORDE_SIZE.get());
        horde = new HordeSettings(HORDE_SIZE.get(), HORDE_AREA.get(), HORDE_MIN_DISTANCE.get(),
                HORDE_VERTICAL_SEARCH.get(), HORDE_SPEED_BONUS.get(), HORDE_FIRE_IMMUNE.get(),
                HORDE_SAFE_RADIUS.get(), HORDE_WARNING.get(), HORDE_BATCHES.get());
        int sustainMin = DIR_SUSTAIN_MIN.get();
        int relaxMin = DIR_RELAX_MIN.get();
        director = new DirectorSettings(DIR_ENABLED.get(), new DirectorFsm.Rules(DIR_PEAK.get().floatValue(), DIR_BUILDUP_MAX.get(),
                sustainMin, Math.max(sustainMin, DIR_SUSTAIN_MAX.get()), DIR_FADE_THREATS.get(), DIR_FADE_MAX.get(),
                relaxMin, Math.max(relaxMin, DIR_RELAX_MAX.get()), DIR_RELAX_TRAVEL.get()),
                DIR_TRICKLE.get(), DIR_TRICKLE_SECONDS.get(), DIR_TRICKLE_MAX.get(), DIR_RELAX_SUPPRESSION.get());
        items = new ItemSettings(MOLOTOV_SECONDS.get(), MOLOTOV_RADIUS.get().floatValue(), MOLOTOV_DAMAGE.get().floatValue(),
                MOLOTOV_FRIENDLY.get().floatValue(), MOLOTOV_IGNITES.get(), ticks(DEFIB_SECONDS),
                DEFIB_HEALTH.get().floatValue(), BODY_MINUTES.get());
        safeRoom = new SafeRoomSettings(SR_PROTECT.get(), SR_REPAIR.get(), SR_RESET_BW.get(), SR_REVIVE_DOWNED.get(),
                SR_MIN_HEALTH.get().floatValue(), SR_SET_SPAWN.get(), SR_RESCUE_DEAD.get(), SR_COOLDOWN.get(),
                SR_TEAM_RADIUS.get(), CRESCENDO_SECONDS.get(), CRESCENDO_WAVE_SECONDS.get(), CRESCENDO_WAVE_SIZE.get(),
                ARMORY_TIER1.get().stream().map(String::trim).toList(),
                ARMORY_TIER2.get().stream().map(String::trim).toList());
        squad = new SquadSettings(SQUAD_RANGE.get(), SQUAD_TEAM_ONLY.get(), SQUAD_OUTLINES.get(), SQUAD_MARKERS.get());
    }
}
