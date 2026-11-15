package com.igneko.aidirector.compat.enhancedhordes;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Puente con Enhanced Hordes. Enhanced Hordes no expone comandos ni API de aparición: aplica su IA de
 * horda (apilarse, romper bloques, multiplicarse) a cualquier entidad del tag {@code forge:hordes}.
 * Por eso el mod genera él mismo los infectados de ese tag y Enhanced Hordes se encarga del resto.
 */
public final class EnhancedHordesCompat {

    /** Marca de persistencia para reconocer a los miembros de horda generados por el Director. */
    public static final String HORDE_TAG = TheAIDirector.MOD_ID + ":horde";
    public static final TagKey<EntityType<?>> HORDE_ENTITIES = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("forge", "hordes"));

    private static final UUID SPEED_ID = UUID.fromString("a1c4e7d2-5b3f-4e8a-9c61-2d7f0b8e4a95");
    private static final UUID FOLLOW_ID = UUID.fromString("b7e2f9a1-3c5d-4f6b-8a9e-1c2d3e4f5a6b");

    private EnhancedHordesCompat() {}

    /** Horda estándar alrededor de un jugador (temporizador del Director y comando /aidirector force). */
    public static int spawnHordeForPlayer(Player player) {
        if (!(player instanceof ServerPlayer sp)) return 0;
        return triggerPanicEvent(sp.serverLevel(), sp.blockPosition(), AIDirectorConfig.horde().size(), sp);
    }

    /**
     * Evento de pánico: genera {@code count} infectados repartidos en un cuadrado de
     * {@code areaSize × areaSize} bloques centrado en {@code center}, sin aparecer encima de nadie,
     * y los lanza contra {@code target} (o contra el jugador más cercano).
     *
     * @return número de infectados generados realmente
     */
    public static int triggerPanicEvent(ServerLevel level, BlockPos center, int count, @Nullable ServerPlayer target) {
        AIDirectorConfig.HordeSettings cfg = AIDirectorConfig.horde();
        RandomSource random = level.getRandom();
        int half = Math.max(2, cfg.areaSize() / 2);
        double minDistSq = cfg.minDistance() * cfg.minDistance();

        int spawned = 0;
        int attempts = count * 12;
        while (spawned < count && attempts-- > 0) {
            int x = center.getX() + random.nextInt(half * 2 + 1) - half;
            int z = center.getZ() + random.nextInt(half * 2 + 1) - half;
            double dx = x + 0.5 - center.getX() - 0.5;
            double dz = z + 0.5 - center.getZ() - 0.5;
            if (dx * dx + dz * dz < minDistSq) continue;

            BlockPos pos = findGround(level, x, center.getY(), z, cfg.verticalSearch());
            if (pos == null) continue;

            EntityType<? extends Mob> type = chooseType(level, pos, random);
            if (!level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;

            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            prepareHordeMember(mob, cfg);
            if (!level.addFreshEntity(mob)) continue;

            Player victim = target != null ? target : level.getNearestPlayer(mob, 64);
            if (victim != null && !victim.isCreative() && !victim.isSpectator()) {
                mob.setTarget(victim);
                mob.getNavigation().moveTo(victim, 1.2);
            }
            spawned++;
        }
        return spawned;
    }

    /** Busca, en vertical cerca de {@code baseY}, una celda de 2 bloques libres sobre suelo firme y seco. */
    @Nullable
    private static BlockPos findGround(ServerLevel level, int x, int baseY, int z, int range) {
        for (int i = 0; i <= range * 2; i++) {
            // Se alterna arriba/abajo empezando por la altura del jugador: 0, +1, -1, +2, -2...
            int offset = (i % 2 == 0) ? i / 2 : -(i / 2 + 1);
            BlockPos pos = new BlockPos(x, baseY + offset, z);
            if (!level.isInWorldBounds(pos)) continue;
            BlockState below = level.getBlockState(pos.below());
            if (!below.isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) continue;
            if (!level.getFluidState(pos).isEmpty() || level.getFluidState(pos.below()).is(FluidTags.LAVA)) continue;
            return pos;
        }
        return null;
    }

    private static EntityType<? extends Mob> chooseType(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBiome(pos).is(BiomeTags.HAS_DESERT_PYRAMID) && random.nextFloat() < 0.7f) return EntityType.HUSK;
        return random.nextFloat() < 0.12f ? EntityType.ZOMBIE_VILLAGER : EntityType.ZOMBIE;
    }

    /** Infectado de horda: corre más, rastrea de lejos, no se quema al sol y no desaparece por distancia. */
    private static void prepareHordeMember(Mob mob, AIDirectorConfig.HordeSettings cfg) {
        mob.getPersistentData().putBoolean(HORDE_TAG, true);
        mob.setPersistenceRequired();
        addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, "aidirector:horde_speed",
                cfg.speedBonus(), AttributeModifier.Operation.MULTIPLY_BASE);
        addModifier(mob.getAttribute(Attributes.FOLLOW_RANGE), FOLLOW_ID, "aidirector:horde_follow",
                48.0, AttributeModifier.Operation.ADDITION);
        if (cfg.fireImmune()) {
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
        }
    }

    private static void addModifier(@Nullable AttributeInstance attr, UUID id, String name, double amount,
                                    AttributeModifier.Operation op) {
        if (attr == null || amount == 0 || attr.getModifier(id) != null) return;
        attr.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }
}
