package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModBlocks;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import static com.igneko.aidirector.world.structure.Canvas.*;

/**
 * Variante 3 — Campamento militar con 4 torres de vigilancia. Doble anillo defensivo (zanja con
 * estacas y muralla de 4 bloques con pasarela y concertina), torres de 15 bloques en las esquinas y,
 * dentro, centro de mando, barracones, enfermería, patio, huerto y zona de tiro.
 *
 * <pre>
 *  anillo 1-2  zanja       anillo 5  muralla (x/z = 5 y 43)       torres en (5,5) (43,5) (5,43) (43,43)
 *  puerta sur en x = 24-25 · centro de mando al norte · tiendas al oeste y al este · patio en el centro
 * </pre>
 */
final class CampBuilder {

    private static final int N = SafeRoomVariant.CAMP.width(), H = SafeRoomVariant.CAMP.height();
    private static final int LO = 5, HI = 43, GATE = 24;

    private static final Palette FLOOR = Palette.weighted(Blocks.DIRT_PATH, 4, Blocks.GRAVEL, 2, Blocks.PACKED_MUD, 2);
    private static final Palette WALL = Palette.weighted(Blocks.TUFF, 3, Blocks.COBBLESTONE, 2, Blocks.CRACKED_STONE_BRICKS, 2,
            Blocks.STONE_BRICKS, 2, Blocks.MOSSY_STONE_BRICKS, 1);
    private static final Palette TRENCH = Palette.weighted(Blocks.MUD, 1, Blocks.PACKED_MUD, 2, Blocks.COARSE_DIRT, 1);
    private static final Palette TOWER_BASE = Palette.weighted(Blocks.COBBLED_DEEPSLATE, 3, Blocks.MUD_BRICKS, 2);
    private static final Palette PLANKS = Palette.weighted(Blocks.SPRUCE_PLANKS, 3, Blocks.DARK_OAK_PLANKS, 1);
    private static final Palette ROOF = Palette.weighted(Blocks.WAXED_OXIDIZED_CUT_COPPER, 2, Blocks.WAXED_WEATHERED_CUT_COPPER, 1,
            Blocks.STRIPPED_SPRUCE_WOOD, 1);

    private static final ResourceLocation RATIONS = BunkerBuilder.loot("rations");
    private static final ResourceLocation AMMO = BunkerBuilder.loot("ammo");
    private static final ResourceLocation SUPPLIES = BunkerBuilder.loot("supplies");

    /** Peldaños de la escalera de caracol alrededor del poste central (u, v). */
    private static final int[][] SPIRAL = {{1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}};

    private CampBuilder() {}

    static void build(Canvas c) {
        ground(c);
        walls(c);
        walkway(c);
        gate(c);
        tower(c, LO, LO, 1, 1);
        tower(c, HI, LO, -1, 1);
        tower(c, LO, HI, 1, -1);
        tower(c, HI, HI, -1, -1);
        commandCenter(c);
        tent(c, 8, 15, Blocks.GREEN_WOOL, false);
        tent(c, 8, 24, Blocks.GRAY_WOOL, false);
        tent(c, 36, 15, Blocks.GREEN_WOOL, false);
        tent(c, 36, 24, Blocks.WHITE_WOOL, true);
        patio(c);
        garden(c);
        training(c);
        lamps(c);
    }

    private static int ring(int x, int z) {
        return Math.min(Math.min(x, z), Math.min(N - 1 - x, N - 1 - z));
    }

    // ------------------------------------------------------------------ perímetro

    /** Suelo apisonado dentro, hierba fuera, zanja de 2 de hondo con estacas y arbustos de bayas. */
    private static void ground(Canvas c) {
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                int r = ring(x, z);
                c.foundation(x, -1, z, Blocks.DIRT.defaultBlockState());
                c.air(x, 1, z, x, H - 1, z);
                boolean causeway = z <= 2 && x >= GATE - 2 && x <= GATE + 3;
                if ((r == 1 || r == 2) && !causeway) {
                    c.air(x, -1, z, x, 0, z);
                    c.set(x, -2, z, TRENCH);
                    if ((x * 3 + z) % 5 == 0) c.set(x, -1, z, unlitCampfire());
                } else if (inside(x, z)) {
                    c.set(x, 0, z, FLOOR);
                } else {
                    c.set(x, 0, z, causeway ? Blocks.GRAVEL.defaultBlockState()
                            : c.chance(x, 0, z, 0.25f) ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                    if ((r == 0 || r == 3) && !causeway && c.chance(x, 1, z, r == 0 ? 0.55f : 0.2f)) {
                        c.set(x, 1, z, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
                    }
                }
            }
        }
    }

    private static boolean inside(int x, int z) {
        return x > LO && x < HI && z > LO && z < HI;
    }

    /** Muralla de 4 bloques con coronación de barrotes en ángulo (concertina). */
    private static void walls(Canvas c) {
        for (int i = LO; i <= HI; i++) {
            int[][] cells = {{i, LO}, {i, HI}, {LO, i}, {HI, i}};
            for (int[] p : cells) {
                c.fill(p[0], 1, p[1], p[0], 4, p[1], WALL);
                if ((p[0] + p[1]) % 2 == 0) c.set(p[0], 5, p[1], Blocks.IRON_BARS.defaultBlockState());
            }
        }
    }

    /** Pasarela de madera de 2 bloques por la cara interna, a media altura, con dos escaleras de acceso. */
    private static void walkway(Canvas c) {
        for (int x = LO + 1; x <= HI - 1; x++) {
            for (int z = LO + 1; z <= HI - 1; z++) {
                int dx = Math.min(x - LO, HI - x), dz = Math.min(z - LO, HI - z);
                int d = Math.min(dx, dz);
                if (d < 1 || d > 2 || nearTower(x, z)) continue;
                c.set(x, 3, z, slab(Blocks.SPRUCE_SLAB, true));
                boolean gatePath = z <= LO + 2 && x >= GATE - 2 && x <= GATE + 3;
                if (d == 2 && (x + z) % 4 == 0 && !gatePath) {
                    c.set(x, 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                    c.set(x, 2, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                }
            }
        }
        stairFlight(c, 15, LO + 3, Direction.EAST);
        stairFlight(c, HI - 9, HI - 3, Direction.WEST);
    }

    private static boolean nearTower(int x, int z) {
        boolean nx = Math.abs(x - LO) <= 2 || Math.abs(x - HI) <= 2;
        boolean nz = Math.abs(z - LO) <= 2 || Math.abs(z - HI) <= 2;
        return nx && nz;
    }

    /** Tres peldaños con su apoyo, subiendo hacia {@code dir} desde (x, z). */
    private static void stairFlight(Canvas c, int x, int z, Direction dir) {
        for (int i = 0; i < 3; i++) {
            int px = x + dir.getStepX() * i;
            c.fill(px, 1, z, px, i, z, PLANKS);
            c.set(px, i + 1, z, stairs(Blocks.SPRUCE_STAIRS, dir));
        }
    }

    /** Puerta principal: doble puerta de refugio entre columnas de hierro, garitas y barreras en zigzag. */
    private static void gate(Canvas c) {
        c.air(GATE - 1, 1, LO, GATE + 2, 5, LO);
        c.fill(GATE - 1, 1, LO, GATE - 1, 4, LO, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(GATE + 2, 1, LO, GATE + 2, 4, LO, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(GATE, 3, LO, GATE + 1, 3, LO, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(GATE, 4, LO, GATE + 1, 4, LO, WALL);
        c.set(GATE, 5, LO, Blocks.IRON_BARS.defaultBlockState());
        c.set(GATE + 1, 5, LO, Blocks.IRON_BARS.defaultBlockState());
        c.doubleDoor(GATE, 1, LO, ModBlocks.SAFE_ROOM_DOOR.get());
        c.set(GATE - 1, 5, LO, Blocks.LANTERN.defaultBlockState());
        c.set(GATE + 2, 5, LO, Blocks.LANTERN.defaultBlockState());

        // Garitas de sacos terreros a ambos lados.
        Palette sandbags = Palette.weighted(Blocks.TERRACOTTA, 2, Blocks.BROWN_MUSHROOM_BLOCK, 1);
        for (int side = 0; side < 2; side++) {
            int x0 = side == 0 ? GATE - 5 : GATE + 4;
            c.fill(x0, 1, 3, x0 + 2, 1, 4, sandbags);
            c.fill(x0, 2, 3, x0, 2, 4, sandbags);
            c.fill(x0 + 2, 2, 3, x0 + 2, 2, 4, sandbags);
            c.set(x0 + 1, 2, 3, sandbags.pick(c.noise(x0, 2, 3)));
        }
        // Barreras en zigzag sobre la pasarela de la zanja.
        c.fill(GATE - 2, 1, 0, GATE, 1, 0, Blocks.COBBLESTONE_WALL.defaultBlockState());
        c.fill(GATE + 1, 1, 2, GATE + 3, 1, 2, Blocks.COBBLESTONE_WALL.defaultBlockState());

        // Esclusa interior: pasillo vallado y una segunda puerta de valla.
        for (int z = LO + 1; z <= LO + 5; z++) {
            c.set(GATE - 2, 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
            c.set(GATE + 3, 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        for (int x = GATE - 1; x <= GATE + 2; x++) {
            c.set(x, 1, LO + 5, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        }
    }

    // ------------------------------------------------------------------ torres

    /**
     * Torre de vigilancia de 15 bloques en una esquina. (u, v) son coordenadas de torre: el interior
     * del campamento queda hacia +u/+v; {@code sx}/{@code sz} las orientan en el lienzo.
     */
    private static void tower(Canvas c, int cx, int cz, int sx, int sz) {
        TowerFrame t = new TowerFrame(c, cx, cz, sx, sz);
        // Bloques 1-4: búnker base de 5×5 con puerta hacia el campamento.
        for (int u = -2; u <= 2; u++) {
            for (int v = -2; v <= 2; v++) {
                t.foundation(u, v);
                boolean ring = Math.abs(u) == 2 || Math.abs(v) == 2;
                for (int y = 1; y <= 12; y++) t.set(u, y, v, Blocks.AIR.defaultBlockState());
                if (ring) for (int y = 1; y <= 4; y++) t.set(u, y, v, TOWER_BASE.pick(c.noise(cx + u, y, cz + v)));
                else t.set(u, 0, v, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        t.door(1, 2);
        t.container(0, 1, -1, AMMO);
        t.container(1, 1, -1, AMMO);

        // Bloques 5-11: pilares de abeto, vigas, arriostramiento en diagonal y barandilla de vallas.
        for (int u = -2; u <= 2; u++) {
            for (int v = -2; v <= 2; v++) {
                boolean corner = Math.abs(u) == 2 && Math.abs(v) == 2;
                boolean ring = Math.abs(u) == 2 || Math.abs(v) == 2;
                if (!ring) continue;
                for (int y = 5; y <= 11; y++) {
                    BlockState s;
                    if (corner) s = log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y);
                    else if (y == 8 || y == 11) s = log(Blocks.STRIPPED_SPRUCE_LOG, Math.abs(v) == 2 ? t.axisU() : t.axisV());
                    else {
                        int along = Math.abs(v) == 2 ? u : v;
                        boolean brace = (y == 6 || y == 9) && along != 0 || (y == 7 || y == 10) && along == 0;
                        s = brace ? Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState() : Blocks.SPRUCE_FENCE.defaultBlockState();
                    }
                    t.set(u, y, v, s);
                }
            }
        }
        // Poste central y escalera de caracol de 12 peldaños (madera oscura y piedra negra).
        for (int y = 1; y <= 12; y++) t.set(0, y, 0, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
        int[] prev = {1, 2};
        for (int i = 0; i < 12; i++) {
            int[] p = SPIRAL[i % 8];
            Direction dir = t.dir(p[0] - prev[0], p[1] - prev[1]);
            Block step = i % 2 == 0 ? Blocks.DARK_OAK_STAIRS : Blocks.POLISHED_BLACKSTONE_STAIRS;
            t.set(p[0], i + 1, p[1], stairs(step, dir));
            prev = p;
        }
        // Bloque 11: ménsulas; bloque 12: plataforma de 7×7 con el hueco de la escalera.
        for (int u = -3; u <= 3; u++) {
            for (int v = -3; v <= 3; v++) {
                boolean outer = Math.abs(u) == 3 || Math.abs(v) == 3;
                if (outer && !(Math.abs(u) == 3 && Math.abs(v) == 3)) {
                    Direction in = Math.abs(v) == 3 ? t.dir(0, -Integer.signum(v)) : t.dir(-Integer.signum(u), 0);
                    t.set(u, 11, v, stairsTop(Blocks.SPRUCE_STAIRS, in));
                }
                if (u == 0 && v == 0) continue;
                boolean hole = (u == 0 && v == 1) || (u == -1 && v == 1) || (u == -1 && v == 0);
                if (!hole) t.set(u, 12, v, PLANKS.pick(c.noise(cx + u, 12, cz + v)));
            }
        }
        // Bloque 13: parapeto de muros con troneras de trampilla; 14: postes; 15-16: tejado de cobre.
        for (int u = -3; u <= 3; u++) {
            for (int v = -3; v <= 3; v++) {
                boolean outer = Math.abs(u) == 3 || Math.abs(v) == 3;
                boolean corner = Math.abs(u) == 3 && Math.abs(v) == 3;
                if (outer) {
                    if (corner || (u + v) % 2 == 0) t.set(u, 13, v, Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState());
                    else {
                        Direction out = Math.abs(v) == 3 ? t.dir(0, Integer.signum(v)) : t.dir(Integer.signum(u), 0);
                        t.set(u, 13, v, trapdoor(Blocks.IRON_TRAPDOOR, out, false, true));
                    }
                    if (corner) t.set(u, 14, v, Blocks.SPRUCE_FENCE.defaultBlockState());
                    t.set(u, 15, v, slab(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, false));
                } else {
                    t.set(u, 15, v, ROOF.pick(c.noise(cx + u, 15, cz + v)));
                    if (Math.abs(u) <= 1 && Math.abs(v) <= 1) t.set(u, 16, v, slab(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, false));
                }
            }
        }
        for (int[] p : new int[][]{{2, 2}, {-2, -2}}) t.set(p[0], 16, p[1], leaves(Blocks.AZALEA_LEAVES));
        // Foco: lámpara de redstone bajo el tejado, encendida por una palanca.
        t.set(0, 14, -3, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        t.set(1, 14, -3, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, t.dir(1, 0)).setValue(LeverBlock.POWERED, true));
    }

    /** Transformación de coordenadas de torre (u, v) al lienzo. */
    private record TowerFrame(Canvas c, int cx, int cz, int sx, int sz) {
        int x(int u) { return cx + sx * u; }
        int z(int v) { return cz + sz * v; }

        void set(int u, int y, int v, BlockState s) {
            c.set(x(u), y, z(v), s);
        }

        void foundation(int u, int v) {
            c.foundation(x(u), -1, z(v), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        }

        Direction dir(int du, int dv) {
            Direction d = Direction.fromDelta(du * sx, 0, dv * sz);
            return d == null ? Direction.NORTH : d;
        }

        Direction.Axis axisU() { return Direction.Axis.X; }
        Direction.Axis axisV() { return Direction.Axis.Z; }

        /** Puerta en la cara +v de la base; quien entra camina hacia -v. */
        void door(int u, int v) {
            Direction entering = dir(0, -1);
            DoorHingeSide hinge = sx * sz > 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            c.door(x(u), 1, z(v), Blocks.SPRUCE_DOOR, entering, hinge);
        }

        void container(int u, int y, int v, ResourceLocation loot) {
            c.container(x(u), y, z(v), facing(Blocks.BARREL, Direction.UP), loot);
        }
    }

    // ------------------------------------------------------------------ interior

    /** Centro de mando: barracón reforzado con tejado de lona verde y gris, mesa táctica y radio. */
    private static void commandCenter(Canvas c) {
        int x0 = 18, x1 = 30, z0 = 33, z1 = 40;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                c.set(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                for (int y = 1; y <= 4; y++) {
                    c.set(x, y, z, !edge ? Blocks.AIR.defaultBlockState()
                            : corner ? log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y) : PLANKS.pick(c.noise(x, y, z)));
                }
            }
        }
        // Tejado a dos aguas de lona (cumbrera a lo largo de x).
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            Block wool = x % 2 == 0 ? Blocks.GREEN_WOOL : Blocks.GRAY_WOOL;
            for (int z = z0; z <= z1; z++) {
                int y = 5 + Math.min(z - z0, z1 - z);
                c.set(x, y, z, wool.defaultBlockState());
                if ((x == x0 || x == x1) && y > 5) c.fill(x, 5, z, x, y - 1, z, PLANKS);
            }
        }
        // Puerta, ventanas y estandartes.
        c.door(24, 1, z0, Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        for (int x : new int[]{21, 22, 26, 27}) c.set(x, 2, z0, Blocks.GLASS_PANE.defaultBlockState());
        for (int z : new int[]{36, 37}) {
            c.set(x0, 2, z, Blocks.GLASS_PANE.defaultBlockState());
            c.set(x1, 2, z, Blocks.GLASS_PANE.defaultBlockState());
        }
        c.set(20, 3, z0 - 1, Blocks.GREEN_WALL_BANNER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        c.set(28, 3, z0 - 1, Blocks.GRAY_WALL_BANNER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));

        // Mesa táctica con mapas y brújulas en marcos.
        c.fill(22, 1, 36, 26, 1, 37, slab(Blocks.SPRUCE_SLAB, true));
        c.itemFrame(23, 2, 36, Direction.UP, new ItemStack(Items.MAP));
        c.itemFrame(25, 2, 37, Direction.UP, new ItemStack(Items.COMPASS));
        c.itemFrame(24, 2, 36, Direction.UP, new ItemStack(Items.PAPER));
        c.set(24, 7, 36, Blocks.CHAIN.defaultBlockState());
        c.set(24, 6, 36, Blocks.CHAIN.defaultBlockState());
        c.set(24, 5, 36, hangingLantern(Blocks.LANTERN));
        c.set(20, 1, 35, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        c.set(20, 1, 38, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));

        // Equipo de radio improvisado (con la radio del refugio) y antena exterior.
        c.set(21, 1, 39, Blocks.JUKEBOX.defaultBlockState());
        c.radio(21, 2, 39, Direction.SOUTH, LO - 2, -2, LO - 2, HI + 2, H - 1, HI + 2, GATE, 1, 28, "camp");
        c.set(22, 1, 39, facing(Blocks.OBSERVER, Direction.UP));
        c.set(23, 1, 39, Blocks.NOTE_BLOCK.defaultBlockState());
        c.set(22, 2, 39, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.SOUTH));
        c.set(23, 2, 39, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.SOUTH));
        for (int y = 7; y <= 11; y++) c.set(21, y, 39, Blocks.IRON_BARS.defaultBlockState());
        c.set(21, 12, 39, Blocks.LIGHTNING_ROD.defaultBlockState());

        // Señal de socorro (Crescendo): atrae una horda; si se resiste, se abre la armería.
        c.alarm(26, 1, 39, Direction.SOUTH, 120);
        c.crate(29, 1, 39, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, true);
        c.crate(29, 1, 38, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, false);
        c.crate(29, 1, 35, Direction.WEST, SupplyCrateBlock.Kind.THROWABLES, false);
        c.ammoPile(29, 1, 36);
        c.container(19, 1, 39, facing(Blocks.CHEST, Direction.EAST), SUPPLIES);
        c.container(19, 1, 34, facing(Blocks.CHEST, Direction.EAST), AMMO);
    }

    /** Tienda de campaña en A de 5 × 7 abierta hacia el sur; la enfermería lleva bandera blanca y roja. */
    private static void tent(Canvas c, int x0, int z0, Block wool, boolean infirmary) {
        BlockState w = wool.defaultBlockState();
        for (int z = z0; z <= z0 + 6; z++) {
            boolean front = z == z0, back = z == z0 + 6;
            for (int y = 1; y <= 2; y++) {
                c.set(x0, y, z, w);
                c.set(x0 + 4, y, z, w);
            }
            c.set(x0 + 1, 3, z, w);
            c.set(x0 + 3, 3, z, w);
            c.set(x0 + 2, 4, z, w);
            if (back) {
                c.fill(x0 + 1, 1, z, x0 + 3, 2, z, w);
                c.set(x0 + 2, 3, z, w);
            } else if (front) {
                c.set(x0 + 1, 1, z, w);
                c.set(x0 + 3, 1, z, w);
                c.set(x0 + 2, 3, z, w);
            }
        }
        for (int z = z0 + 1; z <= z0 + 5; z++) {
            c.set(x0 + 2, 1, z, (c.chance(x0, 1, z, 0.5f) ? Blocks.BROWN_CARPET : Blocks.GRAY_CARPET).defaultBlockState());
        }
        c.set(x0 + 2, 3, z0 + 3, hangingLantern(Blocks.LANTERN));
        if (infirmary) {
            c.bed(x0 + 1, 1, z0 + 4, Blocks.WHITE_BED, Direction.NORTH);
            c.set(x0 + 3, 1, z0 + 5, Blocks.BREWING_STAND.defaultBlockState());
            c.set(x0 + 3, 1, z0 + 4, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
            c.crate(x0 + 3, 1, z0 + 2, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
            c.crate(x0 + 3, 1, z0 + 1, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
            c.container(x0 + 1, 1, z0 + 2, facing(Blocks.BARREL, Direction.UP), SUPPLIES);
            c.set(x0, 1, z0 - 1, Blocks.WHITE_BANNER.defaultBlockState());
            c.set(x0 + 4, 1, z0 - 1, Blocks.RED_BANNER.defaultBlockState());
        } else {
            c.bed(x0 + 1, 1, z0 + 4, Blocks.GRAY_BED, Direction.NORTH);
            c.bed(x0 + 3, 1, z0 + 4, Blocks.BROWN_BED, Direction.NORTH);
            c.container(x0 + 1, 1, z0 + 3, facing(Blocks.BARREL, Direction.UP), RATIONS);
            c.container(x0 + 3, 1, z0 + 3, facing(Blocks.BARREL, Direction.UP), RATIONS);
            c.armorStand(x0 + 2, 1, z0 + 5, Direction.SOUTH, new ItemStack(Items.CHAINMAIL_HELMET),
                    new ItemStack(Items.LEATHER_CHESTPLATE));
        }
    }

    /** Patio común: hoguera central con troncos como asientos, ahumadores y barriles de provisiones. */
    private static void patio(Canvas c) {
        int x = GATE, z = 22;
        c.set(x, 1, z, Blocks.CAMPFIRE.defaultBlockState());
        for (int d = 2; d <= 3; d++) {
            c.set(x - d, 1, z, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
            c.set(x + d, 1, z, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
            c.set(x, 1, z - d, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.X));
            c.set(x, 1, z + d, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.X));
        }
        c.set(x - 4, 1, z + 3, facing(Blocks.SMOKER, Direction.EAST));
        c.set(x + 4, 1, z + 3, facing(Blocks.SMOKER, Direction.WEST));
        c.container(x - 4, 1, z - 3, facing(Blocks.BARREL, Direction.UP), RATIONS);
        c.container(x + 4, 1, z - 3, facing(Blocks.BARREL, Direction.UP), RATIONS);
    }

    /** Huerto bajo una pérgola de vallas y trampillas, con canalones de tolvas sobre calderos. */
    private static void garden(Canvas c) {
        int x0 = 30, x1 = 38, z0 = 8, z1 = 13;
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                if (z == 10 && (x == 34)) {
                    c.set(x, 0, z, Blocks.WATER.defaultBlockState());
                    continue;
                }
                c.set(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                Block crop = x % 3 == 0 ? Blocks.CARROTS : x % 3 == 1 ? Blocks.POTATOES : Blocks.WHEAT;
                c.set(x, 1, z, crop.defaultBlockState().setValue(CropBlock.AGE, 2 + (int) (c.noise(x, 1, z) * 5)));
            }
        }
        for (int[] p : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            for (int y = 1; y <= 3; y++) c.set(p[0], y, p[1], Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (z % 2 == 0) c.set(x, 4, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true, false));
            }
        }
        for (int x = x0; x <= x1; x += 2) {
            c.set(x, 1, z1 + 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 1 + (x % 3)));
            c.set(x, 2, z1 + 2, facing(Blocks.HOPPER, Direction.DOWN));
        }
    }

    /** Zona de instrucción: dianas en la muralla y muñecos de práctica. */
    private static void training(Canvas c) {
        for (int z = 9; z <= 13; z += 2) {
            c.set(LO, 2, z, Blocks.TARGET.defaultBlockState());
            c.set(LO, 3, z, Blocks.TARGET.defaultBlockState());
            c.armorStand(11, 1, z, Direction.WEST, new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE));
        }
        c.set(13, 1, 8, Blocks.HAY_BLOCK.defaultBlockState());
        c.set(13, 1, 14, Blocks.HAY_BLOCK.defaultBlockState());
    }

    private static void lamps(Canvas c) {
        int[][] posts = {{16, 22}, {32, 22}, {GATE, 30}, {GATE - 3, 12}, {16, 31}, {32, 31}};
        for (int[] p : posts) {
            c.set(p[0], 1, p[1], Blocks.SPRUCE_FENCE.defaultBlockState());
            c.set(p[0], 2, p[1], Blocks.LANTERN.defaultBlockState());
        }
    }
}
