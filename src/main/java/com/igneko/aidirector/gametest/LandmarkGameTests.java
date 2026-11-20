package com.igneko.aidirector.gametest;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.world.structure.Landmark;
import com.igneko.aidirector.world.structure.LandmarkPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
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

/**
 * Construye las grandes zonas de interés sobre una plataforma y vuelca los bloques visibles a
 * {@code aidirector_dump/landmark_<zona>.txt}. Es una prueba pesada (cientos de chunks), así que
 * solo se ejecuta con la variable de entorno {@code AIDIR_LANDMARKS} (por ejemplo "port,dome,fob").
 */
@GameTestHolder(TheAIDirector.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LandmarkGameTests {

    private LandmarkGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void buildLandmarks(GameTestHelper helper) {
        String env = System.getenv("AIDIR_LANDMARKS");
        if (env == null || env.isBlank()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO);
        int offset = 0;
        for (Landmark lm : Landmark.values()) {
            if (!env.contains(lm.getSerializedName())) continue;
            int x0 = base.getX() + 400 + offset, z0 = base.getZ() + 400, y0 = 100;
            offset += 320;
            BoundingBox whole = new BoundingBox(x0, y0, z0, x0 + lm.width() - 1, y0 + lm.height() - 1, z0 + lm.depth() - 1);
            platform(level, whole, y0 + lm.ground(), lm == Landmark.PORT ? y0 - 2 : y0 + lm.ground() - 10);
            BoundingBox all = new BoundingBox(whole.minX(), level.getMinBuildHeight(), whole.minZ(), whole.maxX(), level.getMaxBuildHeight() - 1, whole.maxZ());
            for (int[] tile : lm.tiles()) {
                LandmarkPiece piece = new LandmarkPiece(lm, whole, Direction.NORTH, 4242L, tile);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, all,
                        new ChunkPos(whole.getCenter()), whole.getCenter());
            }
            dump(level, lm, whole);
            lootReport(level, lm, whole, helper);
        }
        helper.succeed();
    }

    /** Cuenta los contenedores con botín de la zona por tabla y falla si alguna tabla no existe. */
    private static void lootReport(ServerLevel level, Landmark lm, BoundingBox box, GameTestHelper helper) {
        java.util.Map<String, Integer> perTable = new java.util.TreeMap<>();
        int containers = 0, crates = 0;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                for (net.minecraft.world.level.block.entity.BlockEntity be : new java.util.ArrayList<>(level.getChunk(cx, cz).getBlockEntities().values())) {
                    if (!box.isInside(be.getBlockPos())) continue;
                    if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity) {
                        net.minecraft.nbt.CompoundTag tag = be.saveWithoutMetadata();
                        String table = tag.getString("LootTable");
                        containers++;
                        perTable.merge(table.isEmpty() ? "(sin tabla)" : table, 1, Integer::sum);
                        if (!table.isEmpty()) {
                            net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(table);
                            if (level.getServer().getLootData().getLootTable(id) == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
                                helper.fail("La tabla de botín " + id + " no existe o no se pudo cargar (" + lm + ")");
                            }
                        }
                    } else if (be instanceof com.igneko.aidirector.world.saferoom.SupplyCrateBlockEntity) {
                        crates++;
                    }
                }
            }
        }
        Path dir = FMLPaths.GAMEDIR.get().resolve("aidirector_dump");
        try {
            Files.createDirectories(dir);
            StringBuilder out = new StringBuilder(lm + ": " + containers + " contenedores, " + crates + " cajas de suministros\n");
            perTable.forEach((k, v) -> out.append("  ").append(v).append("  ").append(k).append('\n'));
            Files.writeString(dir.resolve("landmark_loot_" + lm.getSerializedName() + ".txt"), out.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Plataforma maciza bajo la huella (con margen) y aire por encima. */
    private static void platform(ServerLevel level, BoundingBox box, int ground, int bottom) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int x = box.minX() - 4; x <= box.maxX() + 4; x++) {
            for (int z = box.minZ() - 4; z <= box.maxZ() + 4; z++) {
                for (int y = bottom; y <= ground; y++) level.setBlock(p.set(x, y, z), stone, 2);
                for (int y = ground + 1; y <= box.maxY() + 4; y++) {
                    if (!level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, air, 2);
                }
            }
        }
    }

    /** Vuelca solo los bloques que se ven (con aire, agua o cristal al lado o encima). */
    private static void dump(ServerLevel level, Landmark lm, BoundingBox box) {
        Path dir = FMLPaths.GAMEDIR.get().resolve("aidirector_dump");
        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(dir.resolve("landmark_" + lm.getSerializedName() + ".txt"), StandardCharsets.UTF_8)) {
                w.write(box.getXSpan() + " " + box.getYSpan() + " " + box.getZSpan() + "\n");
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(), q = new BlockPos.MutableBlockPos();
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    for (int x = box.minX(); x <= box.maxX(); x++) {
                        for (int z = box.minZ(); z <= box.maxZ(); z++) {
                            BlockState s = level.getBlockState(p.set(x, y, z));
                            if (s.isAir()) continue;
                            boolean visible = false;
                            for (Direction d : Direction.values()) {
                                if (d == Direction.DOWN) continue;
                                BlockState n = level.getBlockState(q.setWithOffset(p, d));
                                if (n.isAir() || !n.canOcclude() && n.getBlock() != s.getBlock()) {
                                    visible = true;
                                    break;
                                }
                            }
                            if (!visible) continue;
                            w.write((x - box.minX()) + " " + (y - box.minY()) + " " + (box.maxZ() - z) + " "
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
