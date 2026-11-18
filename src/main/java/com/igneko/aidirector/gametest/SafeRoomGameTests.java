package com.igneko.aidirector.gametest;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.world.saferoom.SafeRoomRadioBlockEntity;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import com.igneko.aidirector.world.structure.SafeRoomPiece;
import com.igneko.aidirector.world.structure.SafeRoomVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Pruebas de juego de los refugios (solo se cargan con {@code forge.enabledGameTestNamespaces}):
 * construye cada variante, comprueba que su radio registra la zona segura y vuelca los bloques a
 * {@code aidirector_dump/<variante>.txt} para revisarlos fuera del juego.
 */
@GameTestHolder(TheAIDirector.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SafeRoomGameTests {

    private SafeRoomGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void buildSafeRooms(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO);
        int ground = 110;
        int offset = 0;
        for (SafeRoomVariant variant : SafeRoomVariant.values()) {
            int x = base.getX() + 160 + offset, z = base.getZ() + 160;
            for (Direction dir : List.of(Direction.NORTH, Direction.EAST)) {
                int dz = dir == Direction.NORTH ? 0 : 90;
                BlockPos origin = new BlockPos(x, ground - variant.groundLevel(), z + dz);
                SafeRoomPiece piece = new SafeRoomPiece(variant, origin, dir, 1234L);
                BoundingBox pb = piece.getBoundingBox();
                // Los chunks de la prueba quedan forzados: así la radio hace "tick" como cerca de un jugador.
                for (int cx = (pb.minX() - 4) >> 4; cx <= (pb.maxX() + 4) >> 4; cx++) {
                    for (int cz = (pb.minZ() - 4) >> 4; cz <= (pb.maxZ() + 4) >> 4; cz++) level.setChunkForced(cx, cz, true);
                }
                platform(level, pb, ground);
                BoundingBox all = new BoundingBox(pb.minX(), level.getMinBuildHeight(), pb.minZ(), pb.maxX(), level.getMaxBuildHeight() - 1, pb.maxZ());
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, all,
                        new ChunkPos(origin), origin);
                if (dir == Direction.NORTH) dump(level, variant, pb);
            }
            offset += 70;
        }
        helper.runAfterDelay(40, () -> {
            List<SafeRoomRadioBlockEntity> zones = SafeZoneManager.zones(level);
            if (zones.size() < SafeRoomVariant.values().length * 2) helper.fail("Solo se registraron " + zones.size() + " zonas seguras");
            for (SafeRoomRadioBlockEntity radio : zones) {
                if (!SafeZoneManager.prime(radio)) helper.fail("La zona " + radio.variant() + " no está cargada");
                if (radio.doors().size() != 2) helper.fail("La zona " + radio.variant() + " tiene " + radio.doors().size() + " puertas de refugio (esperadas 2)");
                if (!radio.contains(radio.spawn())) helper.fail("El punto de reaparición de " + radio.variant() + " está fuera de la zona");
                if (!level.getBlockState(radio.spawn()).isAir() || !level.getBlockState(radio.spawn().above()).isAir()) {
                    helper.fail("El punto de reaparición de " + radio.variant() + " está obstruido en " + radio.spawn().toShortString() + " por "
                            + level.getBlockState(radio.spawn()) + " / " + level.getBlockState(radio.spawn().above()));
                }
            }
            helper.succeed();
        });
    }

    /**
     * Informe de separación (solo con la variable de entorno {@code AIDIR_SPACING="spacing,separation;..."}):
     * genera el ruido del mundo normal para varias semillas, calcula qué celdas consiguen refugio y mide
     * la distancia entre refugios vecinos y la que da {@code /locate} desde puntos al azar.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void spacingReport(GameTestHelper helper) {
        String env = System.getenv("AIDIR_SPACING");
        if (env == null || env.isBlank()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        net.minecraft.core.RegistryAccess ra = level.registryAccess();
        var noise = ra.registryOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
                .getHolderOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD);
        var params = ra.registryOrThrow(net.minecraft.core.registries.Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        var biomes = net.minecraft.world.level.biome.MultiNoiseBiomeSource.createFromPreset(params);
        var gen = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(biomes, noise);
        var height = net.minecraft.world.level.LevelHeightAccessor.create(-64, 384);
        var structures = ra.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        boolean landmarks = "landmarks".equals(System.getenv("AIDIR_SPACING_SET"));
        int salt = landmarks ? 51893517 : 74126831;
        List<String> names = landmarks ? List.of("landmark_port", "landmark_dome", "landmark_fob")
                : List.of("safe_room_bunker", "safe_room_hangar", "safe_room_camp");
        List<net.minecraft.world.level.levelgen.structure.Structure> rooms = names.stream()
                .map(n -> structures.get(new net.minecraft.resources.ResourceLocation(TheAIDirector.MOD_ID, n))).toList();
        StringBuilder out = new StringBuilder();
        int radius = landmarks ? 4 : 5;
        for (String spec : env.split(";")) {
            String[] sp = spec.split(",");
            int spacing = Integer.parseInt(sp[0].trim()), separation = Integer.parseInt(sp[1].trim());
            var placement = new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(
                    spacing, separation, net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.TRIANGULAR, salt);
            List<Double> nearest = new java.util.ArrayList<>(), locate = new java.util.ArrayList<>();
            int cells = 0, found = 0;
            for (long seed : new long[]{1L, 42L, 2024L, 987654321L}) {
                var rs = net.minecraft.world.level.levelgen.RandomState.create(ra.asGetterLookup(),
                        net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD, seed);
                List<BlockPos> hits = new java.util.ArrayList<>();
                for (int cx = -radius; cx <= radius; cx++) {
                    for (int cz = -radius; cz <= radius; cz++) {
                        cells++;
                        ChunkPos chunk = placement.getPotentialStructureChunk(seed, cx * spacing, cz * spacing);
                        for (var room : rooms) {
                            var ctx = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(ra, gen, biomes, rs,
                                    level.getStructureManager(), seed, chunk, height, room.biomes()::contains);
                            var stub = room.findValidGenerationPoint(ctx);
                            if (stub.isPresent()) {
                                hits.add(stub.get().position());
                                found++;
                                break;
                            }
                        }
                    }
                }
                // Solo cuentan los refugios del interior (los del borde no tienen todos sus vecinos calculados).
                int inner = (radius - 1) * spacing * 16;
                for (BlockPos a : hits) {
                    if (Math.abs(a.getX()) > inner || Math.abs(a.getZ()) > inner) continue;
                    double best = Double.MAX_VALUE;
                    for (BlockPos b : hits) if (a != b) best = Math.min(best, Math.sqrt(a.distSqr(new BlockPos(b.getX(), a.getY(), b.getZ()))));
                    nearest.add(best);
                }
                java.util.Random r = new java.util.Random(seed);
                for (int i = 0; i < 400; i++) {
                    int x = r.nextInt(inner * 2) - inner, z = r.nextInt(inner * 2) - inner;
                    double best = Double.MAX_VALUE;
                    for (BlockPos b : hits) best = Math.min(best, Math.hypot(b.getX() - x, b.getZ() - z));
                    locate.add(best);
                }
            }
            java.util.Collections.sort(nearest);
            java.util.Collections.sort(locate);
            out.append(String.format("spacing=%d separation=%d -> celdas con refugio %d/%d (%.0f%%)%n", spacing, separation, found, cells, 100.0 * found / cells));
            out.append(String.format("  vecino mas cercano: mediana %.0f  p90 %.0f  max %.0f%n", pct(nearest, 0.5), pct(nearest, 0.9), pct(nearest, 1.0)));
            out.append(String.format("  /locate desde punto al azar: mediana %.0f  p90 %.0f  max %.0f%n", pct(locate, 0.5), pct(locate, 0.9), pct(locate, 1.0)));
        }
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("aidirector_dump");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("spacing.txt"), out.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        helper.succeed();
    }

    private static double pct(List<Double> sorted, double q) {
        if (sorted.isEmpty()) return -1;
        return sorted.get(Math.min(sorted.size() - 1, (int) Math.floor(q * (sorted.size() - 1))));
    }

    /** Terreno de prueba plano: piedra y hierba hasta {@code ground}, aire por encima. */
    private static void platform(ServerLevel level, BoundingBox box, int ground) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX() - 4; x <= box.maxX() + 4; x++) {
            for (int z = box.minZ() - 4; z <= box.maxZ() + 4; z++) {
                for (int y = ground - 18; y <= ground + 36; y++) {
                    BlockState s = y < ground - 1 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                            : y < ground ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()
                            : y == ground ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()
                            : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    level.setBlock(p.set(x, y, z), s, 2);
                }
            }
        }
    }

    private static void dump(ServerLevel level, SafeRoomVariant variant, BoundingBox box) {
        Path dir = FMLPaths.GAMEDIR.get().resolve("aidirector_dump");
        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(dir.resolve(variant.getSerializedName() + ".txt"), StandardCharsets.UTF_8)) {
                w.write(box.getXSpan() + " " + (box.getYSpan() + 8) + " " + box.getZSpan() + "\n");
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int y = box.minY() - 4; y <= box.maxY() + 3; y++) {
                    for (int x = box.minX(); x <= box.maxX(); x++) {
                        for (int z = box.minZ(); z <= box.maxZ(); z++) {
                            BlockState s = level.getBlockState(p.set(x, y, z));
                            if (s.isAir()) continue;
                            w.write((x - box.minX()) + " " + (y - box.minY() + 4) + " " + (z - box.minZ()) + " "
                                    + BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath() + "\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
