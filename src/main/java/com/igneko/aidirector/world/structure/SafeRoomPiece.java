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
 * Pieza única de un refugio. El edificio se construye por código en {@link #postProcess}, chunk a
 * chunk, con coordenadas locales que giran y se reflejan con la orientación de la pieza (ver
 * {@link Canvas}). Toda la variación "aleatoria" sale de un hash de la posición y de la semilla de
 * la pieza, así que es idéntica en todos los chunks que comparte.
 */
public class SafeRoomPiece extends StructurePiece {

    private final SafeRoomVariant variant;
    private final long seed;

    public SafeRoomPiece(SafeRoomVariant variant, BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.SAFE_ROOM_PIECE.get(), 0, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation,
                variant.width(), variant.height(), variant.depth()));
        this.variant = variant;
        this.seed = seed;
        setOrientation(orientation);
    }

    public SafeRoomPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.SAFE_ROOM_PIECE.get(), tag);
        this.variant = SafeRoomVariant.byName(tag.getString("Variant"));
        this.seed = tag.getLong("Seed");
    }

    public SafeRoomVariant variant() {
        return variant;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Variant", variant.getSerializedName());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Canvas canvas = new Canvas(this, level, box, seed, random);
        switch (variant) {
            case BUNKER -> BunkerBuilder.build(canvas);
            case HANGAR -> HangarBuilder.build(canvas);
            case CAMP -> CampBuilder.build(canvas);
        }
    }

    // ------------------------------------------------------------------ acceso para Canvas

    void put(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        placeBlock(level, state, x, y, z, box);
    }

    BlockState read(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
        return getBlock(level, x, y, z, box);
    }

    BlockPos worldPos(int x, int y, int z) {
        return getWorldPos(x, y, z).immutable();
    }

    Rotation rotationValue() {
        return getRotation();
    }

    Mirror mirrorValue() {
        return getMirror();
    }
}
