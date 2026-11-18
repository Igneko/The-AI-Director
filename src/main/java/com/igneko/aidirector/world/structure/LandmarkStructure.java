package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Gran zona de interés generada en el mundo. Se centra en el chunk de origen (así cabe en los 8
 * chunks de alcance de una estructura) y se trocea en {@link LandmarkPiece}s.
 * <ul>
 *   <li><b>Puerto:</b> necesita costa: agua en el lado de la dársena y tierra baja en el del patio.
 *   Prueba las cuatro orientaciones hasta encontrar una que encaje.</li>
 *   <li><b>Domo y base avanzada:</b> terreno seco y razonablemente llano.</li>
 * </ul>
 */
public class LandmarkStructure extends Structure {

    public static final Codec<LandmarkStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Landmark.CODEC.fieldOf("landmark").forGetter(s -> s.landmark)
    ).apply(i, LandmarkStructure::new));

    private final Landmark landmark;

    public LandmarkStructure(StructureSettings settings, Landmark landmark) {
        super(settings);
        this.landmark = landmark;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        long seed = ctx.random().nextLong();
        int tries = landmark == Landmark.PORT ? 4 : 1;
        Direction o = first;
        for (int t = 0; t < tries; t++, o = o.getClockWise()) {
            Optional<GenerationStub> stub = landmark == Landmark.PORT ? port(ctx, o, seed) : land(ctx, o, seed);
            if (stub.isPresent()) return stub;
        }
        return Optional.empty();
    }

    /** Caja completa centrada en el chunk de origen, con la base a la altura {@code baseY}. */
    private BoundingBox wholeBox(ChunkPos chunk, Direction o, int baseY) {
        boolean alongZ = o.getAxis() == Direction.Axis.Z;
        int sx = alongZ ? landmark.width() : landmark.depth();
        int sz = alongZ ? landmark.depth() : landmark.width();
        int x0 = chunk.getMiddleBlockX() - sx / 2, z0 = chunk.getMiddleBlockZ() - sz / 2;
        return new BoundingBox(x0, baseY, z0, x0 + sx - 1, baseY + landmark.height() - 1, z0 + sz - 1);
    }

    private Optional<GenerationStub> port(GenerationContext ctx, Direction o, long seed) {
        int sea = ctx.chunkGenerator().getSeaLevel();
        BoundingBox whole = wholeBox(ctx.chunkPos(), o, sea - 1 - PortBuilder.WATER_TOP);
        int wet = 0, dry = 0;
        for (int i = 0; i < 8; i++) {
            int lx = 16 + i * 32;
            for (int lz : new int[]{6, 28, 50}) if (sample(ctx, whole, o, lx, lz)[0] > 0) wet++;
            for (int lz : new int[]{110, 150, 190}) {
                int[] s = sample(ctx, whole, o, lx, lz);
                if (s[0] == 0 && s[1] >= sea - 2 && s[1] <= sea + 24) dry++;
            }
        }
        // La dársena se excava y el patio se rellena y nivela solo: basta con que sea costa.
        if (wet < 14 || dry < 12) return Optional.empty();
        BlockPos center = new BlockPos(LandmarkPiece.worldX(whole, o, landmark.width() / 2, PortBuilder.QZ), sea,
                LandmarkPiece.worldZ(whole, o, landmark.width() / 2, PortBuilder.QZ));
        return Optional.of(stub(whole, o, seed, center));
    }

    private Optional<GenerationStub> land(GenerationContext ctx, Direction o, long seed) {
        BoundingBox probe = wholeBox(ctx.chunkPos(), o, 0);
        int[] heights = new int[49];
        int n = 0, wet = 0;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                int[] s = sample(ctx, probe, o, (landmark.width() - 1) * i / 6, (landmark.depth() - 1) * j / 6);
                // Los lagos y charcas se rellenan al construir: solo se descarta si hay mucha agua.
                if (s[0] > 0 && ++wet > 10) return Optional.empty();
                heights[n++] = s[1];
            }
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        if (sorted[44] - sorted[4] > landmark.maxSlope()) return Optional.empty();
        int ground = sorted[24];
        if (ground < ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        BoundingBox whole = wholeBox(ctx.chunkPos(), o, ground - landmark.ground());
        BlockPos center = new BlockPos(whole.getCenter().getX(), ground, whole.getCenter().getZ());
        return Optional.of(stub(whole, o, seed, center));
    }

    private GenerationStub stub(BoundingBox whole, Direction o, long seed, BlockPos center) {
        return new GenerationStub(center, builder -> {
            for (int[] tile : landmark.tiles()) builder.addPiece(new LandmarkPiece(landmark, whole, o, seed, tile));
        });
    }

    /** {agua encima (0/1), altura del fondo} en el punto local (lx, lz). */
    private static int[] sample(GenerationContext ctx, BoundingBox whole, Direction o, int lx, int lz) {
        int x = LandmarkPiece.worldX(whole, o, lx, lz), z = LandmarkPiece.worldZ(whole, o, lx, lz);
        int surface = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        return new int[]{surface > floor ? 1 : 0, floor};
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LANDMARK.get();
    }
}
