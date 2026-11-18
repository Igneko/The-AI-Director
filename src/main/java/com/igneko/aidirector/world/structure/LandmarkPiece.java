package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Un trozo (pieza) de una gran zona de interés. Todas las piezas comparten el mismo sistema de
 * coordenadas locales que la zona completa: cada una ejecuta el constructor entero, pero el
 * {@link Canvas} solo escribe dentro de su caja, así que cada bloque se coloca una sola vez.
 */
public class LandmarkPiece extends StructurePiece implements CanvasHost {

    private final Landmark landmark;
    private final long seed;
    /** Esquina local de la pieza dentro de la zona y cuánto puede escribir por debajo de su base. */
    private final int ox, oy, oz, down;
    private final BoundingBox whole;

    public LandmarkPiece(Landmark landmark, BoundingBox whole, Direction orientation, long seed, int[] tile) {
        super(ModStructures.LANDMARK_PIECE.get(), 0, tileBox(whole, orientation, tile));
        this.landmark = landmark;
        this.seed = seed;
        this.ox = tile[0];
        this.oy = tile[1];
        this.oz = tile[2];
        this.down = tile[6];
        this.whole = whole;
        setOrientation(orientation);
    }

    public LandmarkPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.LANDMARK_PIECE.get(), tag);
        this.landmark = Landmark.byName(tag.getString("Landmark"));
        this.seed = tag.getLong("Seed");
        int[] o = tag.getIntArray("Offset");
        this.ox = o.length == 4 ? o[0] : 0;
        this.oy = o.length == 4 ? o[1] : 0;
        this.oz = o.length == 4 ? o[2] : 0;
        this.down = o.length == 4 ? o[3] : 0;
        int[] w = tag.getIntArray("Whole");
        this.whole = w.length == 6 ? new BoundingBox(w[0], w[1], w[2], w[3], w[4], w[5]) : getBoundingBox();
    }

    /** Caja en el mundo de un trozo local, con la misma transformación que la zona completa. */
    static BoundingBox tileBox(BoundingBox whole, Direction o, int[] t) {
        return BoundingBox.fromCorners(
                new BlockPos(worldX(whole, o, t[0], t[2]), whole.minY() + t[1], worldZ(whole, o, t[0], t[2])),
                new BlockPos(worldX(whole, o, t[3], t[5]), whole.minY() + t[4], worldZ(whole, o, t[3], t[5])));
    }

    static int worldX(BoundingBox b, Direction o, int x, int z) {
        return switch (o) {
            case WEST -> b.maxX() - z;
            case EAST -> b.minX() + z;
            default -> b.minX() + x;
        };
    }

    static int worldZ(BoundingBox b, Direction o, int x, int z) {
        return switch (o) {
            case NORTH -> b.maxZ() - z;
            case SOUTH -> b.minZ() + z;
            default -> b.minZ() + x;
        };
    }

    public Landmark landmark() {
        return landmark;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Landmark", landmark.getSerializedName());
        tag.putLong("Seed", seed);
        tag.putIntArray("Offset", new int[]{ox, oy, oz, down});
        tag.putIntArray("Whole", new int[]{whole.minX(), whole.minY(), whole.minZ(), whole.maxX(), whole.maxY(), whole.maxZ()});
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox own = getBoundingBox();
        int minX = Math.max(box.minX(), own.minX()), maxX = Math.min(box.maxX(), own.maxX());
        int minZ = Math.max(box.minZ(), own.minZ()), maxZ = Math.min(box.maxZ(), own.maxZ());
        int minY = Math.max(box.minY(), own.minY() - down), maxY = Math.min(box.maxY(), own.maxY());
        if (minX > maxX || minZ > maxZ || minY > maxY) return;
        Canvas canvas = new Canvas(this, level, generator, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ), seed, random);
        switch (landmark) {
            case PORT -> PortBuilder.build(canvas);
            case DOME -> DomeBuilder.build(canvas);
            case FOB -> FobBuilder.build(canvas);
        }
    }

    // ------------------------------------------------------------------ CanvasHost

    @Override
    public void put(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        placeBlock(level, state, x - ox, y - oy, z - oz, box);
    }

    @Override
    public BlockState read(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        return getBlock(level, x - ox, y - oy, z - oz, box);
    }

    @Override
    public BlockPos worldPos(int x, int y, int z) {
        return getWorldPos(x - ox, y - oy, z - oz).immutable();
    }

    @Override
    public Rotation rotationValue() {
        return getRotation();
    }

    @Override
    public Mirror mirrorValue() {
        return getMirror();
    }

    @Override
    public BoundingBox structureBounds() {
        return whole;
    }
}
