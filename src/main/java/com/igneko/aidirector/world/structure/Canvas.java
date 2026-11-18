package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModBlocks;
import com.igneko.aidirector.registry.ModRegistries;
import com.igneko.aidirector.world.saferoom.CrescendoAlarmBlock;
import com.igneko.aidirector.world.saferoom.CrescendoAlarmBlockEntity;
import com.igneko.aidirector.world.saferoom.SafeRoomRadioBlock;
import com.igneko.aidirector.world.saferoom.SafeRoomRadioBlockEntity;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import com.igneko.aidirector.world.supply.AmmoPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Lienzo de construcción en coordenadas locales de una pieza.
 *
 * <p><b>Convenio:</b> +x es el ESTE local y +z el NORTE local; la entrada de cada refugio está en
 * z = 0 y se avanza hacia el interior en +z. Los estados con dirección se escriben en esos términos
 * (por ejemplo, unas escaleras que suben hacia la salida miran al SUR) y la pieza los gira y refleja
 * al colocarlos.
 */
public final class Canvas {

    private final CanvasHost piece;
    private final WorldGenLevel level;
    @Nullable private final ChunkGenerator generator;
    private final BoundingBox box;
    private final long seed;
    final RandomSource random;
    /** Recorte en coordenadas locales: los bucles solo recorren lo que cae dentro del chunk. */
    private final int cx0, cx1, cy0, cy1, cz0, cz1;

    Canvas(CanvasHost piece, WorldGenLevel level, @Nullable ChunkGenerator generator, BoundingBox box, long seed, RandomSource random) {
        this.piece = piece;
        this.level = level;
        this.generator = generator;
        this.box = box;
        this.seed = seed;
        this.random = random;
        BlockPos o = piece.worldPos(0, 0, 0);
        BlockPos ex = piece.worldPos(1, 0, 0).subtract(o), ez = piece.worldPos(0, 0, 1).subtract(o);
        int lx0 = Integer.MAX_VALUE, lx1 = Integer.MIN_VALUE, lz0 = Integer.MAX_VALUE, lz1 = Integer.MIN_VALUE;
        for (int wx : new int[]{box.minX(), box.maxX()}) {
            for (int wz : new int[]{box.minZ(), box.maxZ()}) {
                int dx = wx - o.getX(), dz = wz - o.getZ();
                int lx = dx * ex.getX() + dz * ex.getZ(), lz = dx * ez.getX() + dz * ez.getZ();
                lx0 = Math.min(lx0, lx); lx1 = Math.max(lx1, lx);
                lz0 = Math.min(lz0, lz); lz1 = Math.max(lz1, lz);
            }
        }
        this.cx0 = lx0; this.cx1 = lx1; this.cz0 = lz0; this.cz1 = lz1;
        this.cy0 = box.minY() - o.getY(); this.cy1 = box.maxY() - o.getY();
    }

    // ------------------------------------------------------------------ recorte

    public int clipX0() { return cx0; }
    public int clipX1() { return cx1; }
    public int clipY0() { return cy0; }
    public int clipY1() { return cy1; }
    public int clipZ0() { return cz0; }
    public int clipZ1() { return cz1; }

    /** ¿Toca la caja local (x1..x2, z1..z2) el trozo que se está generando? */
    public boolean touches(int x1, int z1, int x2, int z2) {
        return Math.max(x1, x2) >= cx0 && Math.min(x1, x2) <= cx1 && Math.max(z1, z2) >= cz0 && Math.min(z1, z2) <= cz1;
    }

    public boolean touches(int x1, int y1, int z1, int x2, int y2, int z2) {
        return touches(x1, z1, x2, z2) && Math.max(y1, y2) >= cy0 && Math.min(y1, y2) <= cy1;
    }

    /** Recorre solo las columnas de (x1..x2, z1..z2) que caen en el trozo actual. */
    public void columns(int x1, int z1, int x2, int z2, ColumnVisitor visitor) {
        int xa = Math.max(Math.min(x1, x2), cx0), xb = Math.min(Math.max(x1, x2), cx1);
        int za = Math.max(Math.min(z1, z2), cz0), zb = Math.min(Math.max(z1, z2), cz1);
        for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) visitor.visit(x, z);
    }

    @FunctionalInterface
    public interface ColumnVisitor {
        void visit(int x, int z);
    }

    @Nullable
    public ChunkGenerator generator() {
        return generator;
    }

    public WorldGenLevel level() {
        return level;
    }

    // ------------------------------------------------------------------ bloques

    public void set(int x, int y, int z, BlockState state) {
        BlockPos world = piece.worldPos(x, y, z);
        if (!box.isInside(world)) return;
        piece.put(level, state, x, y, z, box);
        Block b = state.getBlock();
        if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock) {
            level.getChunk(world).markPosForPostprocessing(world);
        }
    }

    public void set(int x, int y, int z, Palette palette) {
        set(x, y, z, palette.pick(noise(x, y, z)));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int xa = Math.max(Math.min(x1, x2), cx0), xb = Math.min(Math.max(x1, x2), cx1);
        int ya = Math.max(Math.min(y1, y2), cy0), yb = Math.min(Math.max(y1, y2), cy1);
        int za = Math.max(Math.min(z1, z2), cz0), zb = Math.min(Math.max(z1, z2), cz1);
        for (int x = xa; x <= xb; x++)
            for (int y = ya; y <= yb; y++)
                for (int z = za; z <= zb; z++) set(x, y, z, state);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
        int xa = Math.max(Math.min(x1, x2), cx0), xb = Math.min(Math.max(x1, x2), cx1);
        int ya = Math.max(Math.min(y1, y2), cy0), yb = Math.min(Math.max(y1, y2), cy1);
        int za = Math.max(Math.min(z1, z2), cz0), zb = Math.min(Math.max(z1, z2), cz1);
        for (int x = xa; x <= xb; x++)
            for (int y = ya; y <= yb; y++)
                for (int z = za; z <= zb; z++) set(x, y, z, palette);
    }

    /** Línea recta de bloques (tensores, cables, vigas en diagonal). */
    public void line(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int n = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        if (!touches(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2))) return;
        if (n == 0) {
            set(x1, y1, z1, state);
            return;
        }
        for (int i = 0; i <= n; i++) {
            set(x1 + Math.round((x2 - x1) * i / (float) n), y1 + Math.round((y2 - y1) * i / (float) n),
                    z1 + Math.round((z2 - z1) * i / (float) n), state);
        }
    }

    /** Coloca un árbol (u otra característica) con el tronco en (x, y, z) si ese punto cae en este trozo. */
    public void feature(int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        BlockPos world = world(x, y, z);
        if (generator == null || !box.isInside(world)) return;
        level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).getHolder(key)
                .ifPresent(f -> f.value().place(level, generator, RandomSource.create(seed ^ world.asLong()), world));
    }

    public void air(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    /** Caja hueca: paredes, suelo y techo con la paleta; interior de aire. */
    public void room(int x1, int y1, int z1, int x2, int y2, int z2, Palette walls, Palette floor, Palette ceiling) {
        fill(x1, y1, z1, x2, y2, z2, walls);
        fill(x1, y1, z1, x2, y1, z2, floor);
        fill(x1, y2, z1, x2, y2, z2, ceiling);
        air(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1);
    }

    /** Rellena hacia abajo desde {@code y} mientras haya aire o líquido (cimientos). */
    public void foundation(int x, int y, int z, BlockState state) {
        BlockPos world = piece.worldPos(x, y, z);
        if (!box.isInside(world)) return;
        BlockPos.MutableBlockPos p = world.mutable();
        for (int i = 0; i < 24 && p.getY() > level.getMinBuildHeight() + 1; i++) {
            BlockState here = level.getBlockState(p);
            if (!(here.isAir() || here.liquid() || here.canBeReplaced())) break;
            level.setBlock(p, state, 2);
            p.move(Direction.DOWN);
        }
    }

    public BlockState get(int x, int y, int z) {
        return piece.read(level, x, y, z, box);
    }

    public boolean inside(int x, int y, int z) {
        return box.isInside(piece.worldPos(x, y, z));
    }

    public BlockPos world(int x, int y, int z) {
        return piece.worldPos(x, y, z);
    }

    /** La pieza está reflejada (orientaciones sur y oeste): los textos se escriben al revés. */
    public boolean mirrored() {
        return piece.mirrorValue() != net.minecraft.world.level.block.Mirror.NONE;
    }

    /** Dirección local → dirección real en el mundo. */
    public Direction dir(Direction local) {
        if (local.getAxis() == Direction.Axis.Y) return local;
        return piece.rotationValue().rotate(piece.mirrorValue().mirror(local));
    }

    /** Ruido determinista en [0, 1) a partir de la posición local y la semilla de la pieza. */
    public float noise(int x, int y, int z) {
        long h = Mth.getSeed(x, y, z) ^ seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }

    public boolean chance(int x, int y, int z, float p) {
        return noise(x * 7 + 3, y * 13 + 1, z * 5 + 11) < p;
    }

    // ------------------------------------------------------------------ piezas compuestas

    /** Puerta de dos bloques; {@code facing} es hacia donde mira quien la cruza para entrar. */
    public void door(int x, int y, int z, Block door, Direction facing, DoorHingeSide hinge) {
        BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, false);
        set(x, y, z, lower);
        set(x, y + 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Pareja de puertas: la izquierda en {@code x}, la derecha en {@code x + 1} (mirando hacia +z). */
    public void doubleDoor(int x, int y, int z, Block door) {
        door(x, y, z, door, Direction.NORTH, DoorHingeSide.LEFT);
        door(x + 1, y, z, door, Direction.NORTH, DoorHingeSide.RIGHT);
    }

    /** Cama: los pies en (x, y, z) y la cabecera hacia {@code toward}. */
    public void bed(int x, int y, int z, Block bed, Direction toward) {
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, toward).setValue(BedBlock.PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x + toward.getStepX(), y, z + toward.getStepZ(), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    public void container(int x, int y, int z, BlockState state, ResourceLocation lootTable) {
        set(x, y, z, state);
        BlockPos world = world(x, y, z);
        if (!box.isInside(world)) return;
        BlockEntity be = level.getBlockEntity(world);
        if (be instanceof RandomizableContainerBlockEntity c) c.setLootTable(lootTable, random.nextLong());
    }

    /** Radio del refugio con su zona segura en coordenadas locales. */
    public void radio(int x, int y, int z, Direction facing, int x1, int y1, int z1, int x2, int y2, int z2,
                      int sx, int sy, int sz, String variant) {
        set(x, y, z, ModBlocks.SAFE_ROOM_RADIO.get().defaultBlockState().setValue(SafeRoomRadioBlock.FACING, facing));
        BlockPos world = world(x, y, z);
        if (!box.isInside(world) || !(level.getBlockEntity(world) instanceof SafeRoomRadioBlockEntity radio)) return;
        BlockPos a = world(x1, y1, z1), b = world(x2, y2, z2);
        radio.configure(BoundingBox.fromCorners(a, b), piece.structureBounds(), world(sx, sy, sz), variant);
    }

    public void crate(int x, int y, int z, Direction facing, SupplyCrateBlock.Kind kind, boolean locked) {
        set(x, y, z, ModBlocks.SUPPLY_CRATE.get().defaultBlockState().setValue(SupplyCrateBlock.FACING, facing)
                .setValue(SupplyCrateBlock.KIND, kind).setValue(SupplyCrateBlock.LOCKED, locked));
    }

    public void alarm(int x, int y, int z, Direction facing, int seconds) {
        set(x, y, z, ModBlocks.CRESCENDO_ALARM.get().defaultBlockState().setValue(CrescendoAlarmBlock.FACING, facing));
        BlockPos world = world(x, y, z);
        if (box.isInside(world) && level.getBlockEntity(world) instanceof CrescendoAlarmBlockEntity alarm) alarm.setDuration(seconds);
    }

    public void ammoPile(int x, int y, int z) {
        set(x, y, z, ModRegistries.AMMO_PILE.get().defaultBlockState().setValue(AmmoPileBlock.INFINITE, true));
    }

    /** Soporte de armadura mirando hacia {@code facing} (local) con el equipo indicado (cabeza, pecho, piernas, pies). */
    public void armorStand(int x, int y, int z, Direction facing, ItemStack... gear) {
        BlockPos world = world(x, y, z);
        if (!box.isInside(world)) return;
        ArmorStand stand = EntityType.ARMOR_STAND.create(level.getLevel());
        if (stand == null) return;
        stand.moveTo(world.getX() + 0.5, world.getY(), world.getZ() + 0.5, dir(facing).toYRot(), 0f);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < gear.length && i < slots.length; i++) stand.setItemSlot(slots[i], gear[i]);
        level.addFreshEntity(stand);
    }

    /** Marco en el bloque (x, y, z) colgado de la pared que tiene detrás; {@code facing} es hacia dónde mira. */
    public void itemFrame(int x, int y, int z, Direction facing, ItemStack item) {
        BlockPos world = world(x, y, z);
        if (!box.isInside(world)) return;
        ItemFrame frame = new ItemFrame(level.getLevel(), world, dir(facing));
        frame.setItem(item, false);
        level.addFreshEntity(frame);
    }

    /** Marco luminoso (para mapas y pantallas); {@code facing} es hacia dónde mira. */
    public void glowFrame(int x, int y, int z, Direction facing, ItemStack item) {
        BlockPos world = world(x, y, z);
        if (!box.isInside(world)) return;
        net.minecraft.world.entity.decoration.GlowItemFrame frame =
                new net.minecraft.world.entity.decoration.GlowItemFrame(level.getLevel(), world, dir(facing));
        frame.setItem(item, false);
        level.addFreshEntity(frame);
    }

    // ------------------------------------------------------------------ estados frecuentes

    public static BlockState stairs(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    public static BlockState stairsTop(Block block, Direction facing) {
        return stairs(block, facing).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
    }

    public static BlockState slab(Block block, boolean top) {
        return block.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE,
                top ? net.minecraft.world.level.block.state.properties.SlabType.TOP : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
    }

    public static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
    }

    public static BlockState facing(Block block, Direction facing) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return s.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        if (s.hasProperty(BlockStateProperties.FACING)) return s.setValue(BlockStateProperties.FACING, facing);
        return s;
    }

    public static BlockState trapdoor(Block block, Direction facing, boolean top, boolean open) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, top ? net.minecraft.world.level.block.state.properties.Half.TOP
                        : net.minecraft.world.level.block.state.properties.Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, open);
    }

    public static BlockState hangingLantern(Block lantern) {
        return lantern.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
    }

    public static BlockState wallTorch(Block torch, Direction facing) {
        return torch.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    public static BlockState leaves(Block leaves) {
        return leaves.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
    }

    public static BlockState unlitCampfire() {
        return Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false);
    }

    public static BlockState ladder(Direction facing) {
        return Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }
}
