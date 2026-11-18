package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Refugio generado en el mundo (búnker, hangar o campamento). Busca un terreno razonablemente
 * llano y seco, elige una orientación al azar y coloca la pieza con el suelo a la altura mediana.
 */
public class SafeRoomStructure extends Structure {

    public static final Codec<SafeRoomStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            SafeRoomVariant.CODEC.fieldOf("variant").forGetter(s -> s.variant)
    ).apply(i, SafeRoomStructure::new));

    private final SafeRoomVariant variant;

    public SafeRoomStructure(StructureSettings settings, SafeRoomVariant variant) {
        super(settings);
        this.variant = variant;
    }

    /**
     * Desplazamientos (en chunks) que se prueban si el sitio del propio chunk no vale: así un lago o una
     * ladera no dejan sin refugio toda la celda. Se quedan a menos de 8 chunks, el alcance de las
     * referencias de estructura.
     */
    private static final int[][] CANDIDATES = {
            {0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}
    };

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Direction orientation = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        boolean alongZ = orientation.getAxis() == Direction.Axis.Z;
        int sizeX = alongZ ? variant.width() : variant.depth();
        int sizeZ = alongZ ? variant.depth() : variant.width();
        long seed = ctx.random().nextLong();
        ChunkPos chunk = ctx.chunkPos();
        for (int[] c : CANDIDATES) {
            int x0 = chunk.getMinBlockX() + c[0] * 16, z0 = chunk.getMinBlockZ() + c[1] * 16;
            int ground = groundAt(ctx, x0, z0, sizeX, sizeZ);
            if (ground == Integer.MIN_VALUE) continue;
            BlockPos center = new BlockPos(x0 + sizeX / 2, ground, z0 + sizeZ / 2);
            // El bioma lo vuelve a comprobar Minecraft en el centro: se descarta aquí para probar el siguiente.
            if (!ctx.validBiome().test(ctx.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()),
                    QuartPos.fromBlock(center.getY()), QuartPos.fromBlock(center.getZ()), ctx.randomState().sampler()))) continue;
            BlockPos origin = new BlockPos(x0, ground - variant.groundLevel(), z0);
            return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new SafeRoomPiece(variant, origin, orientation, seed))));
        }
        return Optional.empty();
    }

    /** Altura del suelo para la huella con esquina en (x0, z0), o {@code MIN_VALUE} si hay agua o mucha pendiente. */
    private int groundAt(GenerationContext ctx, int x0, int z0, int sizeX, int sizeZ) {
        // Muestreo de 5×5 alturas sobre la huella: rechaza agua y pendientes fuertes.
        int[] heights = new int[25];
        int n = 0, wet = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int x = x0 + (sizeX - 1) * i / 4, z = z0 + (sizeZ - 1) * j / 4;
                int surface = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                int floor = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                if (surface > floor && ++wet > 2) return Integer.MIN_VALUE;
                heights[n++] = floor;
            }
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        // Se ignoran los extremos (un árbol o un hoyo aislado no descartan el sitio).
        if (sorted[22] - sorted[2] > variant.maxSlope()) return Integer.MIN_VALUE;
        int ground = sorted[12];
        if (ground < ctx.chunkGenerator().getSeaLevel() - 2) return Integer.MIN_VALUE;
        return ground;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SAFE_ROOM.get();
    }
}
