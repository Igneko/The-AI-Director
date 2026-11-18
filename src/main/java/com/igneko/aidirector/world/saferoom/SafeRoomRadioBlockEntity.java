package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Datos de una zona segura: límites absolutos, punto de reaparición, puertas de refugio que la
 * sellan y una "foto" de sus bloques para repararla si la horda arranca alguno.
 *
 * <p>La foto se toma la primera vez que todos los chunks de la zona están cargados (normalmente
 * cuando llega el primer jugador): se guarda cada bloque sólido como un entero empaquetado
 * (desplazamiento x/y/z + índice de paleta).
 */
public class SafeRoomRadioBlockEntity extends BlockEntity {

    /** Zona por defecto de una radio colocada a mano (creativo / creadores de mapas). */
    private static final int DEFAULT_RADIUS = 6;

    @Nullable private BoundingBox zone;
    @Nullable private BlockPos spawn;
    /** Toda la estructura del refugio (muros, zanjas, torres): ahí los mobs no pueden romper nada. */
    @Nullable private BoundingBox bounds;
    private String variant = "custom";
    private final List<BlockPos> doors = new ArrayList<>();
    private final Map<UUID, Long> lastCheckpoint = new HashMap<>();

    // Foto para la autorreparación
    @Nullable private int[] snapshot;
    private final List<BlockState> palette = new ArrayList<>();
    private boolean snapshotRequested = true;

    // Estado en tiempo de ejecución (no se guarda)
    final Set<UUID> inside = new HashSet<>();
    int securedTicks;
    boolean sealed;

    public SafeRoomRadioBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SAFE_ROOM_RADIO_BE.get(), pos, state);
    }

    /** Configuración desde la estructura generada. */
    public void configure(BoundingBox zone, BoundingBox bounds, BlockPos spawn, String variant) {
        this.zone = zone;
        this.bounds = bounds;
        this.spawn = spawn.immutable();
        this.variant = variant;
        this.snapshot = null;
        this.snapshotRequested = true;
        setChanged();
    }

    public BoundingBox zone() {
        if (zone == null) {
            BlockPos p = getBlockPos();
            zone = new BoundingBox(p.getX() - DEFAULT_RADIUS, p.getY() - 2, p.getZ() - DEFAULT_RADIUS,
                    p.getX() + DEFAULT_RADIUS, p.getY() + 5, p.getZ() + DEFAULT_RADIUS);
        }
        return zone;
    }

    /** Área protegida frente a los mobs; en refugios antiguos o colocados a mano, la zona con margen. */
    public BoundingBox bounds() {
        if (bounds != null) return bounds;
        BoundingBox z = zone();
        return new BoundingBox(z.minX() - 8, z.minY() - 4, z.minZ() - 8, z.maxX() + 8, z.maxY() + 4, z.maxZ() + 8);
    }

    public BlockPos spawn() {
        if (spawn == null) {
            Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                    ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
            spawn = getBlockPos().relative(facing);
        }
        return spawn;
    }

    public String variant() {
        return variant;
    }

    public List<BlockPos> doors() {
        return doors;
    }

    public boolean contains(BlockPos pos) {
        return zone().isInside(pos);
    }

    public boolean onCooldown(UUID player, long now, long cooldownTicks) {
        Long last = lastCheckpoint.get(player);
        return last != null && now - last < cooldownTicks;
    }

    public void markCheckpoint(UUID player, long now) {
        lastCheckpoint.put(player, now);
        setChanged();
    }

    // ------------------------------------------------------------------ foto y reparación

    public boolean hasSnapshot() {
        return snapshot != null;
    }

    public void requestSnapshot() {
        snapshotRequested = true;
    }

    boolean wantsSnapshot() {
        return snapshotRequested || snapshot == null;
    }

    /** ¿Se guarda este bloque en la foto? Se excluyen plantas, fluidos, puertas y bloques con datos. */
    private static boolean worthKeeping(BlockState state) {
        if (state.isAir() || state.liquid() || state.hasBlockEntity()) return false;
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BushBlock) return false;
        return !state.canBeReplaced();
    }

    /** Recorre la zona: guarda los bloques y localiza las puertas de refugio (mitad inferior). */
    void takeSnapshot(Level level) {
        BoundingBox z = zone();
        List<Integer> packed = new ArrayList<>();
        Map<BlockState, Integer> index = new IdentityHashMap<>();
        palette.clear();
        doors.clear();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = z.minX(); x <= z.maxX(); x++) {
            for (int y = z.minY(); y <= z.maxY(); y++) {
                for (int dz = z.minZ(); dz <= z.maxZ(); dz++) {
                    cursor.set(x, y, dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof SafeRoomDoorBlock
                            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                        doors.add(cursor.immutable());
                    }
                    if (!worthKeeping(state)) continue;
                    int dx = x - z.minX(), dy = y - z.minY(), ddz = dz - z.minZ();
                    if (dx > 127 || dy > 63 || ddz > 127) continue;
                    Integer id = index.get(state);
                    if (id == null) {
                        if (palette.size() >= 4095) continue;
                        id = palette.size();
                        palette.add(state);
                        index.put(state, id);
                    }
                    packed.add(dx | dy << 7 | ddz << 13 | id << 20);
                }
            }
        }
        snapshot = packed.stream().mapToInt(Integer::intValue).toArray();
        snapshotRequested = false;
        setChanged();
    }

    /** ¿El bloque de esa posición forma parte de la estructura original? */
    boolean isOriginal(BlockPos pos, BlockState current) {
        if (snapshot == null) return true;
        BoundingBox z = zone();
        int dx = pos.getX() - z.minX(), dy = pos.getY() - z.minY(), dz = pos.getZ() - z.minZ();
        int key = dx | dy << 7 | dz << 13;
        for (int v : snapshot) {
            if ((v & 0xFFFFF) == key) return palette.get(v >>> 20).getBlock() == current.getBlock();
        }
        return false;
    }

    /**
     * Restaura los bloques originales que hayan desaparecido (aire, fluido o fuego en su lugar).
     * Respeta lo que los jugadores coloquen encima: solo rellena huecos.
     *
     * @return número de bloques reparados
     */
    int repair(Level level) {
        if (snapshot == null) return 0;
        BoundingBox z = zone();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int repaired = 0;
        for (int v : snapshot) {
            cursor.set(z.minX() + (v & 0x7F), z.minY() + (v >>> 7 & 0x3F), z.minZ() + (v >>> 13 & 0x7F));
            BlockState now = level.getBlockState(cursor);
            if (!(now.isAir() || now.liquid() || now.is(net.minecraft.tags.BlockTags.FIRE))) continue;
            BlockState original = palette.get(v >>> 20);
            if (now.getBlock() == original.getBlock()) continue;
            level.setBlock(cursor, original, Block.UPDATE_ALL);
            repaired++;
        }
        return repaired;
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) SafeZoneManager.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) SafeZoneManager.unregister(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide()) SafeZoneManager.unregister(this);
    }

    // ------------------------------------------------------------------ persistencia

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Variant", variant);
        if (zone != null) {
            tag.putIntArray("Zone", new int[]{zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ()});
        }
        if (bounds != null) {
            tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        }
        if (spawn != null) tag.put("Spawn", NbtUtils.writeBlockPos(spawn));
        ListTag doorList = new ListTag();
        for (BlockPos d : doors) doorList.add(NbtUtils.writeBlockPos(d));
        tag.put("Doors", doorList);
        CompoundTag checkpoints = new CompoundTag();
        lastCheckpoint.forEach((id, t) -> checkpoints.putLong(id.toString(), t));
        tag.put("Checkpoints", checkpoints);
        if (snapshot != null) {
            tag.putIntArray("Snapshot", snapshot);
            ListTag pal = new ListTag();
            for (BlockState s : palette) pal.add(NbtUtils.writeBlockState(s));
            tag.put("Palette", pal);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        variant = tag.getString("Variant");
        int[] z = tag.getIntArray("Zone");
        zone = z.length == 6 ? new BoundingBox(z[0], z[1], z[2], z[3], z[4], z[5]) : null;
        int[] b = tag.getIntArray("Bounds");
        bounds = b.length == 6 ? new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]) : null;
        spawn = tag.contains("Spawn") ? NbtUtils.readBlockPos(tag.getCompound("Spawn")) : null;
        doors.clear();
        for (Tag t : tag.getList("Doors", Tag.TAG_COMPOUND)) doors.add(NbtUtils.readBlockPos((CompoundTag) t));
        lastCheckpoint.clear();
        CompoundTag checkpoints = tag.getCompound("Checkpoints");
        for (String key : checkpoints.getAllKeys()) {
            try {
                lastCheckpoint.put(UUID.fromString(key), checkpoints.getLong(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        palette.clear();
        if (tag.contains("Snapshot")) {
            HolderGetter<Block> blocks = level != null ? level.holderLookup(Registries.BLOCK)
                    : net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup();
            for (Tag t : tag.getList("Palette", Tag.TAG_COMPOUND)) palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) t));
            snapshot = tag.getIntArray("Snapshot");
            snapshotRequested = false;
        } else {
            snapshot = null;
            snapshotRequested = true;
        }
    }
}
