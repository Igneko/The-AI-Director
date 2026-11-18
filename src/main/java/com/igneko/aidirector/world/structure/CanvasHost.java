package com.igneko.aidirector.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Pieza de estructura sobre la que dibuja un {@link Canvas} (coordenadas locales → mundo). */
interface CanvasHost {

    void put(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box);

    BlockState read(WorldGenLevel level, int x, int y, int z, BoundingBox box);

    BlockPos worldPos(int x, int y, int z);

    Rotation rotationValue();

    Mirror mirrorValue();

    /** Caja de la estructura completa (la que protege la radio de un refugio). */
    BoundingBox structureBounds();
}
