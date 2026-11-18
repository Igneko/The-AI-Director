package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Los mobs no pueden romper la estructura de un refugio.
 * <p>
 * Enhanced Hordes rompe bloques desde su propio {@link LivingEvent.LivingTickEvent}: primero suelta el
 * botín del bloque a mano y después lo borra, sin ningún evento que se pueda cancelar. Por eso, cuando
 * un mob está junto a un refugio, se apunta el estado de los bloques que tiene alrededor al empezar
 * su tick (prioridad más alta) y al acabar (prioridad más baja) se devuelve todo a su sitio, con el
 * contenido de cofres incluido. Mientras tanto, cualquier objeto, orbe o bloque que cae y que aparece
 * en esa ventana se anula: los zombis no pueden romper el refugio ni, por tanto, duplicar objetos.
 * Los jugadores siguen pudiendo reformarlo a mano.
 */
public final class StructureGuard {

    /** Alcance de los golpes de Enhanced Hordes: 3×3×3 alrededor del mob más el bloque de encima. */
    private static final int REACH_XZ = 2, REACH_DOWN = 2, REACH_UP = 3;

    private record Saved(BlockPos pos, BlockState state, @Nullable CompoundTag data) {}

    @Nullable private static LivingEntity guarded;
    @Nullable private static ServerLevel guardedLevel;
    private static int minX, minY, minZ, maxX, maxY, maxZ;
    private static final List<Saved> SAVED = new ArrayList<>();

    public StructureGuard() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob) || !(entity.level() instanceof ServerLevel level)) return;
        if (!AIDirectorConfig.safeRoom().protectBlocks()) return;
        BlockPos at = entity.blockPosition();
        if (!SafeZoneManager.nearGuard(level, at, REACH_XZ + 1)) return;

        SAVED.clear();
        guarded = entity;
        guardedLevel = level;
        minX = at.getX() - REACH_XZ; maxX = at.getX() + REACH_XZ;
        minY = at.getY() - REACH_DOWN; maxY = at.getY() + REACH_UP;
        minZ = at.getZ() - REACH_XZ; maxZ = at.getZ() + REACH_XZ;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
                    if (SafeZoneManager.guardAt(level, p) == null) continue;
                    BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(p) : null;
                    SAVED.add(new Saved(p.immutable(), state, be == null ? null : be.saveWithoutMetadata()));
                }
            }
        }
        if (SAVED.isEmpty()) guarded = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterTick(LivingEvent.LivingTickEvent event) {
        if (guarded == null || event.getEntity() != guarded) return;
        ServerLevel level = guardedLevel;
        guarded = null;
        guardedLevel = null;
        for (Saved s : SAVED) {
            BlockState now = level.getBlockState(s.pos());
            if (now.getBlock() == s.state().getBlock()) continue; // puertas abiertas, etc.: no es una rotura
            level.setBlock(s.pos(), s.state(), 3);
            if (s.data() != null) {
                BlockEntity be = level.getBlockEntity(s.pos());
                if (be != null) {
                    be.load(s.data());
                    be.setChanged();
                }
            }
        }
        SAVED.clear();
    }

    /** Nada de botín de bloques arrancados por un mob junto al refugio. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onJoin(EntityJoinLevelEvent event) {
        if (guarded == null || event.getLevel() != guardedLevel) return;
        Entity e = event.getEntity();
        if (!(e instanceof ItemEntity || e instanceof ExperienceOrb || e instanceof FallingBlockEntity)) return;
        BlockPos p = e.blockPosition();
        if (p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY
                && p.getZ() >= minZ && p.getZ() <= maxZ) {
            event.setCanceled(true);
        }
    }
}
