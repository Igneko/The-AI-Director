package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import static com.igneko.aidirector.world.structure.Canvas.facing;
import static com.igneko.aidirector.world.structure.Canvas.hangingLantern;
import static com.igneko.aidirector.world.structure.Canvas.slab;
import static com.igneko.aidirector.world.structure.Canvas.stairs;
import static com.igneko.aidirector.world.structure.Canvas.trapdoor;
import static com.igneko.aidirector.world.structure.Props.AIR;
import static com.igneko.aidirector.world.structure.Props.loot;
import static com.igneko.aidirector.world.structure.Props.s;

/**
 * Pabellón geodésico oscuro y complejo off-road (256 × 240).
 *
 * <pre>
 *  z=239 ┌────────────────────────────────────────────────────────────────────┐
 *        │ motocross      │plaza│ carpas blancas y pabellones │  autódromo     │
 *        │ (x 4-104,      │vehí-│  ┌── muro con pilares ──┐   │  (óvalo x 196- │
 *        │  z 40-216)     │culos│  │  ábside N: mando     │   │   255, puente  │
 *        │ curvas         │     │  │  nave: taller/arena  │ ⛺│   al norte)    │
 *        │ peraltadas,    │     │  │  ábside S: triaje    │   │                │
 *        │ saltos         │     │  └──────portón─────────┘   │                │
 *  z=0   └──────── bosque denso (z 0-36) que tapa la fachada del domo ────────┘
 * </pre>
 */
final class DomeBuilder {

    private DomeBuilder() {}

    static final int W = 256, D = 240, H = 44, G = 8;

    // Domo: 75 de largo (z 82..156), 32 de ancho (x 134..165), 16 de alto.
    private static final double XC = 149.5, ZS = 98, ZN = 140, R = 16, RH = 16.5, HMAX = 16;
    private static final int DZ0 = 82, DZ1 = 156, DX0 = 134, DX1 = 165;
    // Muro del recinto.
    private static final int WX0 = 124, WX1 = 176, WZ0 = 70, WZ1 = 170;
    // Autódromo: estadio con centros (227, 50) y (227, 194) y radio de la línea central 18.
    private static final int TX = 227, TZS = 50, TZN = 194;

    private static final Palette GRASS = Palette.weighted(Blocks.GRASS_BLOCK, 8, Blocks.COARSE_DIRT, 1, Blocks.PODZOL, 1);
    private static final Palette MUD_TRACK = Palette.weighted(Blocks.RED_SAND, 3, Blocks.ORANGE_TERRACOTTA, 2,
            Blocks.COARSE_DIRT, 2, Blocks.PACKED_MUD, 1);
    private static final Palette PLAZA = Palette.weighted(Blocks.GRAVEL, 2, Blocks.COARSE_DIRT, 2,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER, 1, Blocks.DIRT_PATH, 1);
    private static final Palette PAVING = Palette.weighted(Blocks.LIGHT_GRAY_CONCRETE, 5, Blocks.SMOOTH_STONE, 2, Blocks.POLISHED_ANDESITE, 1);
    private static final Palette ASPHALT = Palette.weighted(Blocks.GRAY_CONCRETE, 4, Blocks.CYAN_TERRACOTTA, 2, Blocks.LIGHT_GRAY_CONCRETE, 1);
    private static final Palette SHELL = Palette.weighted(Blocks.BLACK_CONCRETE, 3, Blocks.POLISHED_DEEPSLATE, 2);
    private static final Palette RIB = Palette.weighted(Blocks.SMOOTH_QUARTZ, 2, Blocks.CALCITE, 1);
    private static final Palette TENT = Palette.weighted(Blocks.WHITE_WOOL, 6, Blocks.SNOW_BLOCK, 1);
    private static final Palette WHITE = Palette.weighted(Blocks.WHITE_CONCRETE, 4, Blocks.SMOOTH_QUARTZ, 1);

    /** Trazado del motocross (circuito cerrado) en coordenadas locales. */
    private static final int[][] MX = {
            {24, 50}, {92, 50}, {94, 80}, {36, 84}, {32, 112}, {92, 114}, {94, 144},
            {36, 148}, {34, 176}, {92, 180}, {90, 206}, {14, 206}, {12, 56}
    };

    private static final int[][] TENTS = {{130, 188}, {148, 196}, {166, 190}, {138, 214}, {160, 220}, {182, 214}, {186, 92}, {186, 146}};

    static void build(Canvas c) {
        ground(c);
        racetrack(c);
        compound(c);
        dome(c);
        tents(c);
        vehicles(c);
        forest(c);
        bridge(c);
    }

    // ================================================================== terreno por zonas

    private static void ground(Canvas c) {
        c.columns(0, 0, W - 1, D - 1, (x, z) -> {
            boolean forest = z <= 36 && x >= 38 && x <= 194;
            c.fill(x, G - 3, z, x, G - 1, z, s(Blocks.DIRT));
            c.foundation(x, G - 4, z, s(Blocks.DIRT));
            if (!forest) {
                c.fill(x, G + 1, z, x, H - 1, z, AIR);
            } else {
                // En el bosque se despeja el terreno sobrante pero se respetan los árboles (que pueden
                // venir de un chunk vecino ya generado).
                for (int y = G + 1; y < H; y++) {
                    BlockState here = c.get(x, y, z);
                    if (!here.isAir() && !here.is(BlockTags.LOGS) && !here.is(BlockTags.LEAVES)) c.set(x, y, z, AIR);
                }
            }
            if (x >= 4 && x <= 104 && z >= 40 && z <= 216) {
                motocross(c, x, z);
            } else if (x >= 106 && x <= 122 && z >= 40 && z <= 216) {
                c.set(x, G, z, PLAZA);
            } else if (x >= WX0 && x <= WX1 && z >= WZ0 && z <= WZ1) {
                c.set(x, G, z, PAVING);
            } else {
                c.set(x, G, z, GRASS);
            }
        });
    }

    /** Hondonada del motocross: pista de tierra roja, peraltes en las curvas, mesetas de salto y vallas blancas. */
    private static void motocross(Canvas c, int x, int z) {
        int edge = Math.min(Math.min(x - 4, 104 - x), Math.min(z - 40, 216 - z));
        int base = edge < 2 ? G - 1 : G - 2;
        double best = Double.MAX_VALUE;
        int seg = 0;
        double along = 0;
        for (int i = 0; i < MX.length; i++) {
            int[] a = MX[i], b = MX[(i + 1) % MX.length];
            double dx = b[0] - a[0], dz = b[1] - a[1], len2 = dx * dx + dz * dz;
            double t = Math.max(0, Math.min(1, ((x - a[0]) * dx + (z - a[1]) * dz) / len2));
            double px = a[0] + t * dx - x, pz = a[1] + t * dz - z, d = Math.sqrt(px * px + pz * pz);
            if (d < best) {
                best = d;
                seg = i;
                along = t * Math.sqrt(len2);
            }
        }
        int h = 0;
        boolean track = best <= 3.5;
        // Peralte: muro de tierra en el exterior de cada curva.
        for (int i = 0; i < MX.length; i++) {
            int[] p = MX[(i + MX.length - 1) % MX.length], v = MX[i], n = MX[(i + 1) % MX.length];
            double ix = v[0] - p[0], iz = v[1] - p[1], il = Math.hypot(ix, iz);
            double ox = n[0] - v[0], oz = n[1] - v[1], ol = Math.hypot(ox, oz);
            double outX = ix / il - ox / ol, outZ = iz / il - oz / ol;
            double rx = x - v[0], rz = z - v[1];
            if (rx * rx + rz * rz > 81 || rx * outX + rz * outZ <= 0) continue;
            if (best > 3.5 && best <= 6.5) h = Math.max(h, (int) Math.round(3 - (best - 3.5)));
            else if (track && best > 1.5) h = Math.max(h, 1);
        }
        // Mesetas de salto en las rectas largas.
        if (track) {
            int[] a = MX[seg], b = MX[(seg + 1) % MX.length];
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            if (len > 50) {
                int jumps = len > 100 ? 2 : 1;
                for (int k = 1; k <= jumps; k++) {
                    double mid = len * k / (jumps + 1), s = along - mid;
                    if (Math.abs(s) <= 3) h = Math.max(h, 3);
                    else if (Math.abs(s) <= 7) h = Math.max(h, (int) Math.round((7 - Math.abs(s)) * 0.75));
                }
            }
        }
        int top = base + h;
        c.fill(x, G - 3, z, x, top - 1, z, s(Blocks.PACKED_MUD));
        c.fill(x, top + 1, z, x, G, z, AIR);
        if (track || h > 0) {
            c.set(x, top, z, MUD_TRACK);
        } else {
            c.set(x, top, z, c.chance(x, 1, z, 0.25f) ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK));
            if (best > 3.5 && best <= 4.5) {
                float r = c.noise(x, 7, z);
                c.set(x, top + 1, z, r < 0.5f ? slab(Blocks.QUARTZ_SLAB, false) : r < 0.85f ? s(Blocks.WHITE_CARPET) : s(Blocks.DIORITE_WALL));
            }
        }
    }

    /** Distancia a la línea central del óvalo del autódromo. */
    private static double radial(int x, int z) {
        if (z >= TZS && z <= TZN) return Math.abs(x - TX);
        int cz = z < TZS ? TZS : TZN;
        return Math.hypot(x - TX, z - cz);
    }

    private static void racetrack(Canvas c) {
        if (!c.touches(190, 14, W - 1, D - 1)) return;
        c.columns(190, 14, W - 1, D - 1, (x, z) -> {
            double r = radial(x, z);
            boolean curve = z < TZS || z > TZN;
            double off = Math.abs(r - 18);
            if (off <= 9) {
                BlockState st;
                if (curve && off > 7.5) st = s(((x + z) / 2) % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE);
                else if (!curve && off > 8.2) st = s(Blocks.WHITE_CONCRETE);
                else st = ASPHALT.pick(c.noise(x, G, z));
                // Línea de salida a cuadros en la recta oeste.
                if (!curve && x < TX && (z == 120 || z == 121)) st = s((x + z) % 2 == 0 ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE);
                c.set(x, G, z, st);
            } else if (r > 27 && r <= 31) {
                c.set(x, G, z, curve ? (c.chance(x, 2, z, 0.75f) ? s(Blocks.GRAVEL) : s(Blocks.SAND)) : GRASS.pick(c.noise(x, 0, z)));
            } else if (r > 31 && r <= 32) {
                // Muro de neumáticos (negro y blanco) por fuera.
                c.set(x, G + 1, z, s(((x + z) / 2) % 2 == 0 ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE));
                c.set(x, G + 2, z, s(Blocks.BLACK_CONCRETE));
            } else if (r < 9 && r >= 8 && !curve) {
                c.set(x, G + 1, z, s(Blocks.LIGHT_GRAY_CONCRETE));
            }
        });
        // Boxes en el interior del óvalo.
        if (c.touches(219, 80, 235, 160)) {
            c.fill(221, G + 1, 80, 233, G + 5, 160, WHITE);
            c.air(222, G + 1, 81, 232, G + 4, 159);
            for (int z = 84; z <= 156; z += 8) c.air(221, G + 1, z, 221, G + 3, z + 3);
            c.fill(221, G + 6, 80, 233, G + 6, 160, s(Blocks.GRAY_CONCRETE));
            for (int z = 86; z <= 154; z += 16) {
                c.container(231, G + 1, z, facing(Blocks.CHEST, Direction.WEST), loot("dome_mechanic"));
                c.set(230, G + 1, z + 2, s(Blocks.ANVIL));
            }
        }
        // Coches abandonados y control policial en la recta oeste.
        Props.car(c, 206, G + 1, 60, Direction.NORTH, Blocks.WHITE_CONCRETE, false, "dome_police");
        Props.car(c, 212, G + 1, 98, Direction.SOUTH, Blocks.RED_CONCRETE, false, "dome_police");
        Props.car(c, 240, G + 1, 30, Direction.WEST, Blocks.BLUE_CONCRETE, true, "dome_police");
        Props.jersey(c, 200, G + 1, 150, true, 8);
        Props.jersey(c, 210, G + 1, 155, true, 8);
        policeCar(c, 203, G + 1, 158);
    }

    /** Coche patrulla blanco y negro con sirena. */
    private static void policeCar(Canvas c, int x, int y, int z) {
        Props.car(c, x, y, z, Direction.EAST, Blocks.WHITE_CONCRETE, false, "dome_police");
        Frame f = new Frame(c, x, y, z, Direction.EAST);
        f.fill(0, 1, 0, 4, 1, 0, s(Blocks.BLACK_CONCRETE));
        f.fill(0, 1, 2, 4, 1, 2, s(Blocks.BLACK_CONCRETE));
        f.set(2, 3, 0, s(Blocks.RED_STAINED_GLASS));
        f.set(2, 3, 2, s(Blocks.BLUE_STAINED_GLASS));
    }

    // ================================================================== recinto y domo

    private static void compound(Canvas c) {
        if (!c.touches(WX0 - 4, WZ0 - 4, WX1 + 4, WZ1 + 4)) return;
        BlockState wall = s(Blocks.GRAY_CONCRETE);
        for (int x = WX0; x <= WX1; x++) {
            for (int z : new int[]{WZ0, WZ1}) {
                boolean gate = z == WZ0 && x >= 145 && x <= 154;
                if (!gate) c.fill(x, G + 1, z, x, G + 4, z, wall);
                if (!gate && (x - WX0) % 6 == 0) buttress(c, x, z, z == WZ0 ? -1 : 1, true);
            }
        }
        for (int z = WZ0; z <= WZ1; z++) {
            for (int x : new int[]{WX0, WX1}) {
                boolean gate = x == WX1 && z >= 115 && z <= 124;
                if (!gate) c.fill(x, G + 1, z, x, G + 4, z, wall);
                if (!gate && (z - WZ0) % 6 == 0) buttress(c, x, z, x == WX0 ? -1 : 1, false);
            }
        }
        // Portones oscuros retráctiles: dintel, hoja medio recogida y carril en el suelo.
        c.fill(144, G + 5, WZ0, 155, G + 5, WZ0, s(Blocks.BLACK_CONCRETE));
        c.fill(145, G + 1, WZ0 + 1, 148, G + 4, WZ0 + 1, s(Blocks.BLACKSTONE));
        c.fill(145, G, WZ0, 154, G, WZ0, s(Blocks.POLISHED_BLACKSTONE));
        c.fill(WX1, G + 5, 114, WX1, G + 5, 125, s(Blocks.BLACK_CONCRETE));
        c.fill(WX1 - 1, G + 1, 121, WX1 - 1, G + 4, 124, s(Blocks.BLACKSTONE));
        // Plazas de aparcamiento pintadas.
        for (int x = 128; x <= 172; x += 4) {
            if (x > 134 && x < 165) continue;
            c.fill(x, G, 74, x, G, 79, s(Blocks.YELLOW_CONCRETE));
        }
        Props.lightMast(c, 128, G + 1, 74, 14);
        Props.lightMast(c, 172, G + 1, 166, 14);
    }

    /** Pilar blanco inclinado hacia fuera del muro. */
    private static void buttress(Canvas c, int x, int z, int out, boolean alongX) {
        c.fill(x, G + 1, z, x, G + 5, z, s(Blocks.QUARTZ_PILLAR));
        for (int k = 1; k <= 3; k++) {
            int bx = alongX ? x : x + out * k, bz = alongX ? z + out * k : z;
            c.fill(bx, G + 1, bz, bx, G + 4 - k, bz, s(Blocks.SMOOTH_QUARTZ));
            Direction d = alongX ? (out < 0 ? Direction.NORTH : Direction.SOUTH) : (out < 0 ? Direction.EAST : Direction.WEST);
            c.set(bx, G + 5 - k, bz, stairs(Blocks.QUARTZ_STAIRS, d));
        }
    }

    /** Radio en planta del domo (nave recta + dos ábsides semicirculares). */
    private static double domeR(int x, int z) {
        double dx = x - XC;
        if (z < ZS) return Math.hypot(dx, z - ZS);
        if (z > ZN) return Math.hypot(dx, z - ZN);
        return Math.abs(dx);
    }

    private static double domeH(double r) {
        return r >= RH ? 0 : HMAX * Math.sqrt(1 - (r / RH) * (r / RH));
    }

    private static void dome(Canvas c) {
        if (!c.touches(DX0 - 1, DZ0 - 1, DX1 + 1, DZ1 + 1)) return;
        c.columns(DX0, DZ0, DX1, DZ1, (x, z) -> {
            double r = domeR(x, z);
            if (r > R) return;
            int top = G + (int) Math.round(domeH(r));
            int low = r + 0.8 >= RH ? G + 1 : Math.min(top, G + (int) Math.round(domeH(r + 0.8)));
            c.set(x, G, z, s(Blocks.LIGHT_GRAY_CONCRETE));
            c.fill(x, G + 1, z, x, low - 1, z, AIR);
            for (int y = low; y <= top; y++) c.set(x, y, z, shellBlock(c, x, y, z, r));
        });
        // Puertas: principal en el ábside sur y lateral en la nave (lado este).
        c.air(146, G + 1, DZ0, 153, G + 4, DZ0 + 4);
        c.fill(145, G + 1, DZ0, 145, G + 5, DZ0 + 2, RIB.pick(0.1f));
        c.fill(154, G + 1, DZ0, 154, G + 5, DZ0 + 2, RIB.pick(0.1f));
        c.air(DX1 - 3, G + 1, 116, DX1, G + 4, 121);
        decon(c);
        nave(c);
        command(c);
        // Faroles colgados de las nervaduras centrales.
        for (int z = 103; z <= 133; z += 10) {
            c.fill(149, G + 11, z, 149, G + 14, z, s(Blocks.CHAIN));
            c.set(149, G + 10, z, hangingLantern(Blocks.LANTERN));
        }
    }

    private static BlockState shellBlock(Canvas c, int x, int y, int z, double r) {
        boolean rib;
        if (z >= ZS && z <= ZN) {
            rib = (z - (int) ZS) % 5 == 0;
            if (!rib && r >= 3 && r <= 6) return s(Blocks.BLACK_STAINED_GLASS); // tragaluces
        } else {
            double cz = z < ZS ? ZS : ZN;
            double ang = Math.atan2(z - cz, x - XC);
            double step = Math.PI / 8;
            double k = ang / step;
            rib = Math.abs(k - Math.round(k)) * step * r < 0.6 || r < 2.5;
        }
        return rib ? RIB.pick(c.noise(x, y, z)) : SHELL.pick(c.noise(x, y, z));
    }

    /** Ábside sur: zona de descontaminación (esclusas y duchas químicas) y triaje con camillas. */
    private static void decon(Canvas c) {
        for (int zw : new int[]{92, 96}) {
            for (int x = DX0; x <= DX1; x++) {
                if (domeR(x, zw) > R - 1) continue;
                if (x == 149 || x == 150) continue;
                BlockState pane = s((x / 2) % 2 == 0 ? Blocks.WHITE_STAINED_GLASS_PANE : Blocks.YELLOW_STAINED_GLASS_PANE);
                c.fill(x, G + 1, zw, x, G + 4, zw, pane);
            }
        }
        for (int x : new int[]{146, 149, 152}) {
            c.set(x, G + 4, 94, facing(Blocks.HOPPER, Direction.DOWN));
            c.set(x, G + 1, 94, s(Blocks.WATER_CAULDRON).setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 2));
        }
        for (int z : new int[]{85, 88}) {
            for (int x : new int[]{139, 142, 155, 158}) {
                if (domeR(x, z) > R - 2 || domeR(x + 1, z) > R - 2) continue;
                c.bed(x, G + 1, z, Blocks.WHITE_BED, Direction.EAST);
                c.set(x - 1, G + 1, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, false, false));
            }
        }
        c.crate(137, G + 1, 91, Direction.EAST, SupplyCrateBlock.Kind.MEDICAL, false);
        c.container(162, G + 1, 91, facing(Blocks.CHEST, Direction.WEST), loot("dome_medical"));
        for (int x : new int[]{142, 157}) {
            c.fill(x, G + 7, 87, x, G + 10, 87, s(Blocks.CHAIN));
            c.set(x, G + 6, 87, hangingLantern(Blocks.LANTERN));
        }
    }

    /** Nave central: taller con elevadores y blindados, armeros enrejados, bancos y pasarelas colgadas. */
    private static void nave(Canvas c) {
        // Marcas tácticas en el suelo y círculo de la arena.
        for (int z = 99; z <= 139; z++) {
            c.set(143, G, z, s(Blocks.YELLOW_CONCRETE));
            c.set(156, G, z, s(Blocks.YELLOW_CONCRETE));
        }
        c.columns(143, 113, 156, 126, (x, z) -> {
            double d = Math.hypot(x - XC, z - 119.5);
            if (d > 5 && d <= 6) c.set(x, G, z, s(((x + z) % 2 == 0) ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE));
        });
        // Elevadores hidráulicos con blindados ligeros encima.
        for (int z0 : new int[]{102, 114, 126}) {
            for (int[] p : new int[][]{{137, z0}, {140, z0}, {137, z0 + 7}, {140, z0 + 7}}) {
                c.set(p[0], G + 1, p[1], facing(Blocks.PISTON, Direction.UP));
            }
            c.fill(137, G + 2, z0, 140, G + 2, z0 + 7, s(Blocks.SMOOTH_STONE));
            armoredCar(c, 137, G + 3, z0);
        }
        // Armero enrejado.
        c.fill(158, G + 1, 100, 163, G + 4, 108, s(Blocks.IRON_BARS));
        c.air(159, G + 1, 101, 162, G + 3, 107);
        c.fill(158, G + 4, 100, 163, G + 4, 108, s(Blocks.IRON_BLOCK));
        c.set(160, G + 1, 100, AIR);
        c.set(160, G + 2, 100, AIR);
        c.crate(162, G + 1, 102, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, false);
        c.crate(162, G + 1, 105, Direction.WEST, SupplyCrateBlock.Kind.THROWABLES, false);
        c.armorStand(160, G + 1, 107, Direction.SOUTH, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE));
        // Bancos de trabajo mecánico.
        Block[] bench = {Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.ANVIL, Blocks.CRAFTING_TABLE, Blocks.BLAST_FURNACE};
        for (int i = 0; i < 10; i++) c.set(162, G + 1, 112 + i * 2, bench[i % bench.length].defaultBlockState());
        c.container(161, G + 1, 133, facing(Blocks.CHEST, Direction.WEST), loot("dome_mechanic"));
        c.container(161, G + 1, 135, facing(Blocks.BARREL, Direction.UP), loot("dome_mechanic"));
        // Pasarelas metálicas colgadas de los arcos.
        for (int side = 0; side < 2; side++) {
            int xa = side == 0 ? 137 : 160, xb = xa + 2, rail = side == 0 ? 140 : 159, hang = side == 0 ? 137 : 162;
            c.fill(xa, G + 7, 99, xb, G + 7, 139, slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
            c.fill(rail, G + 8, 99, rail, G + 8, 139, s(Blocks.IRON_BARS));
            for (int z = 103; z <= 138; z += 5) c.fill(hang, G + 8, z, hang, G + 9, z, s(Blocks.CHAIN));
            int col = side == 0 ? 137 : 162, lad = side == 0 ? 138 : 161;
            c.fill(col, G + 1, 99, col, G + 6, 99, s(Blocks.IRON_BLOCK));
            Props.ladder(c, lad, G + 1, G + 7, 99, side == 0 ? Direction.EAST : Direction.WEST);
        }
        c.container(138, G + 8, 130, facing(Blocks.CHEST, Direction.EAST), loot("port_tactical"));
    }

    /** Blindado ligero verde oliva (4 × 7) sobre un elevador. */
    private static void armoredCar(Canvas c, int x, int y, int z) {
        c.fill(x, y, z, x + 3, y, z + 6, s(Blocks.BLACK_CONCRETE));
        c.fill(x, y + 1, z, x + 3, y + 2, z + 6, s(Blocks.GREEN_TERRACOTTA));
        c.fill(x + 1, y + 3, z + 2, x + 2, y + 3, z + 4, s(Blocks.GREEN_TERRACOTTA));
        c.set(x + 1, y + 3, z + 5, s(Blocks.IRON_BARS));
        c.fill(x, y + 2, z + 6, x + 3, y + 2, z + 6, s(Blocks.GRAY_STAINED_GLASS));
        c.set(x, y + 1, z + 1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.WEST, false, true));
        c.set(x + 3, y + 1, z + 1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, false, true));
    }

    /** Ábside norte: sala blindada de comunicaciones, cajas bajo redes y generadores con cables. */
    private static void command(Canvas c) {
        int x0 = 143, x1 = 156, z0 = 144, z1 = 152;
        c.fill(x0, G + 1, z0, x1, G + 4, z1, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
        c.fill(x0, G + 5, z0, x1, G + 5, z1, s(Blocks.IRON_BLOCK));
        c.air(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 4, z1 - 1);
        c.door(149, G + 1, z0, Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        c.door(150, G + 1, z0, Blocks.DARK_OAK_DOOR, Direction.NORTH, DoorHingeSide.RIGHT);
        for (int x = x0 + 1; x < x1; x++) {
            c.set(x, G + 1, z1 - 1, s(Blocks.POLISHED_BLACKSTONE));
            c.set(x, G + 2, z1 - 1, switch (x % 4) {
                case 0 -> s(Blocks.NOTE_BLOCK);
                case 1 -> s(Blocks.REDSTONE_LAMP);
                case 2 -> s(Blocks.OBSERVER);
                default -> s(Blocks.LECTERN);
            });
        }
        c.set(x0 + 2, G + 1, z0 + 3, s(Blocks.CARTOGRAPHY_TABLE));
        c.set(x0 + 3, G + 1, z0 + 3, Props.LEVER);
        c.crate(x0 + 2, G + 1, z0 + 5, Direction.EAST, SupplyCrateBlock.Kind.ARMORY, true);
        c.crate(x1 - 2, G + 1, z0 + 5, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, true);
        c.crate(x1 - 2, G + 1, z0 + 3, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, true);
        c.container(x0 + 5, G + 1, z1 - 2, facing(Blocks.CHEST, Direction.SOUTH), loot("dome_research"));
        c.container(x1 - 5, G + 1, z1 - 2, facing(Blocks.CHEST, Direction.SOUTH), loot("dome_research"));
        c.alarm(149, G + 1, z0 + 4, Direction.SOUTH, 120);
        c.set(149, G + 4, z0 + 4, hangingLantern(Blocks.LANTERN));
        // Pilas de cajas bajo redes.
        for (int i = 0; i < 4; i++) {
            int x = 137 + (i % 2) * 2, z = 142 + (i / 2) * 3;
            SupplyCrateBlock.Kind k = i % 2 == 0 ? SupplyCrateBlock.Kind.MEDICAL : SupplyCrateBlock.Kind.THROWABLES;
            c.crate(x, G + 1, z, Direction.EAST, k, false);
            c.set(x, G + 2, z, s(Blocks.GREEN_CONCRETE));
            c.set(x, G + 3, z, s(Blocks.COBWEB));
        }
        // Generadores industriales y cables hacia las carpas.
        for (int z = 142; z <= 148; z += 3) {
            c.set(160, G + 1, z, facing(Blocks.BLAST_FURNACE, Direction.WEST));
            c.set(161, G + 1, z, s(Blocks.IRON_BLOCK));
            c.set(161, G + 2, z, s(Blocks.LIGHTNING_ROD));
        }
        c.fill(160, G + 3, 142, 160, G + 3, 155, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        c.fill(149, G + 2, 154, 160, G + 2, 154, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.X));
        c.air(149, G + 2, 155, 150, G + 2, DZ1 + 1);
        c.fill(149, G + 2, 155, 149, G + 2, 186, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        for (int z = 158; z <= 186; z += 6) c.fill(149, G + 1, z, 149, G + 1, z, s(Blocks.SPRUCE_FENCE));
    }

    // ================================================================== carpas, vehículos, bosque y puente

    private static void tents(Canvas c) {
        for (int i = 0; i < TENTS.length; i++) tent(c, TENTS[i][0], TENTS[i][1], i);
        pavilion(c, 116, 224, 130, 236);
        pavilion(c, 178, 176, 192, 192);
    }

    /** Carpa blanca de techo cónico en punta (radio 5, alto 8) con su interior. */
    private static void tent(Canvas c, int cx, int cz, int index) {
        int r = 5, h = 8;
        if (!c.touches(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1)) return;
        c.columns(cx - r, cz - r, cx + r, cz + r, (x, z) -> {
            double d = Math.hypot(x - cx, z - cz);
            if (d > r + 0.4) return;
            c.set(x, G, z, s(Blocks.WHITE_CONCRETE_POWDER));
            for (int k = 1; k <= h; k++) {
                double rk = (r + 0.4) * (1 - (k - 1) / (double) h);
                boolean wall = k <= 2 ? d > r - 0.6 : d > rk - 1.2 && d <= rk;
                if (wall) c.set(x, G + k, z, TENT);
            }
        });
        c.fill(cx, G + 1, cz, cx, G + h + 1, cz, s(Blocks.IRON_BARS));
        c.air(cx, G + 1, cz - r, cx + 1, G + 2, cz - r + 1);
        c.set(cx, G + 4, cz + 1, hangingLantern(Blocks.LANTERN));
        c.bed(cx - 3, G + 1, cz + 1, Blocks.WHITE_BED, Direction.EAST);
        c.container(cx + 3, G + 1, cz + 1, facing(Blocks.CHEST, Direction.WEST), loot(index % 3 == 2 ? "dome_mechanic" : "dome_medical"));
        if (index % 2 == 0) c.crate(cx + 2, G + 1, cz - 2, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
    }

    /** Pabellón rectangular menor (blanco, techo gris) con mesas y suministros. */
    private static void pavilion(Canvas c, int x0, int z0, int x1, int z1) {
        if (!c.touches(x0, z0, x1, z1)) return;
        c.fill(x0, G + 1, z0, x1, G + 4, z1, WHITE);
        c.fill(x0, G + 5, z0, x1, G + 5, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
        c.air(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 4, z1 - 1);
        c.fill(x0, G + 2, z0 + 2, x0, G + 3, z1 - 2, s(Blocks.GLASS_PANE));
        c.air((x0 + x1) / 2, G + 1, z0, (x0 + x1) / 2 + 1, G + 3, z0);
        for (int x = x0 + 2; x < x1 - 1; x += 3) {
            c.set(x, G + 1, (z0 + z1) / 2, slab(Blocks.SMOOTH_STONE_SLAB, true));
            c.set(x, G + 1, (z0 + z1) / 2 + 1, slab(Blocks.SMOOTH_STONE_SLAB, true));
        }
        c.container(x1 - 1, G + 1, z1 - 1, facing(Blocks.CHEST, Direction.WEST), loot("dome_medical"));
        c.container(x1 - 1, G + 1, z0 + 1, facing(Blocks.BARREL, Direction.UP), loot("safe_room_rations"));
        c.set((x0 + x1) / 2, G + 4, (z0 + z1) / 2, hangingLantern(Blocks.LANTERN));
    }

    private static void vehicles(Canvas c) {
        Props.truck(c, 108, G + 1, 56, Direction.NORTH, Props.Color.WHITE, Props.Color.WHITE, "dome_mechanic");
        Props.truck(c, 116, G + 1, 92, Direction.NORTH, Props.Color.WHITE, Props.Color.WHITE, null);
        Props.truck(c, 128, G + 1, 66, Direction.EAST, Props.Color.WHITE, Props.Color.WHITE, "dome_mechanic");
        Props.car(c, 108, G + 1, 130, Direction.NORTH, Blocks.WHITE_CONCRETE, true, "dome_mechanic");
        Props.car(c, 116, G + 1, 150, Direction.SOUTH, Blocks.LIGHT_GRAY_CONCRETE, true, null);
        Props.car(c, 108, G + 1, 190, Direction.NORTH, Blocks.WHITE_CONCRETE, true, "dome_medical");
        buggy(c, 114, G + 1, 170, Direction.NORTH);
        buggy(c, 118, G + 1, 176, Direction.EAST);
        // Motos abandonadas a mitad del circuito de barro.
        bike(c, 58, 50, true);
        bike(c, 62, 113, true);
        bike(c, 13, 140, false);
    }

    /** Buggy tubular: jaula de barrotes sobre cuatro ruedas. */
    private static void buggy(Canvas c, int x, int y, int z, Direction facing) {
        Frame f = new Frame(c, x, y, z, facing);
        if (!f.touches(-1, -1, 5, 3)) return;
        for (int a : new int[]{0, 3}) {
            f.set(a, 0, 0, s(Blocks.BLACK_CONCRETE));
            f.set(a, 0, 2, s(Blocks.BLACK_CONCRETE));
        }
        f.fill(0, 1, 0, 3, 1, 2, slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
        f.fill(1, 1, 1, 2, 1, 1, s(Blocks.ORANGE_CONCRETE));
        f.fill(1, 2, 0, 2, 3, 0, s(Blocks.IRON_BARS));
        f.fill(1, 2, 2, 2, 3, 2, s(Blocks.IRON_BARS));
        f.fill(1, 3, 0, 2, 3, 2, s(Blocks.IRON_BARS));
    }

    /** Moto de motocross tirada en el barro. */
    private static void bike(Canvas c, int x, int z, boolean alongX) {
        if (!c.touches(x - 1, z - 1, x + 3, z + 3)) return;
        int y = G - 1;
        for (int i = 0; i < 3; i++) {
            int bx = alongX ? x + i : x, bz = alongX ? z : z + i;
            int top = y;
            while (top < G + 4 && !c.get(bx, top + 1, bz).isAir()) top++;
            c.set(bx, top + 1, bz, i == 1 ? s(Blocks.ORANGE_CONCRETE) : s(Blocks.BLACK_CONCRETE));
            if (i == 1) c.set(bx, top + 2, bz, s(Blocks.IRON_BARS));
        }
    }

    @SuppressWarnings("unchecked")
    private static final ResourceKey<ConfiguredFeature<?, ?>>[] TREES = new ResourceKey[]{
            TreeFeatures.DARK_OAK, TreeFeatures.SPRUCE, TreeFeatures.PINE, TreeFeatures.OAK, TreeFeatures.SPRUCE
    };

    /** Bosque denso al sur que tapa la fachada del domo, con dos puestos de francotirador y trampas. */
    private static void forest(Canvas c) {
        if (!c.touches(38, 0, 194, 36)) return;
        c.columns(38, 0, 194, 36, (x, z) -> c.set(x, G, z, GRASS));
        for (int x = 40; x <= 192; x += 5) {
            for (int z = 2; z <= 34; z += 5) {
                int jx = x + (int) (c.noise(x, 1, z) * 3), jz = z + (int) (c.noise(x, 2, z) * 3);
                if (c.noise(x, 3, z) < 0.15f) continue;
                if (Math.abs(jx - 75) <= 2 && Math.abs(jz - 18) <= 2 || Math.abs(jx - 168) <= 2 && Math.abs(jz - 16) <= 2) continue;
                c.feature(jx, G + 1, jz, TREES[(int) (c.noise(x, 4, z) * TREES.length)]);
            }
        }
        sniperNest(c, 75, 18);
        sniperNest(c, 168, 16);
        if (c.touches(100, 36, 130, 38)) Props.tripwireTrap(c, 104, G + 1, 37, 114);
        if (c.touches(150, 36, 180, 38)) Props.tripwireTrap(c, 160, G + 1, 37, 170);
    }

    /** Puesto de francotirador camuflado entre los árboles. */
    private static void sniperNest(Canvas c, int x, int z) {
        if (!c.touches(x - 2, z - 2, x + 2, z + 2)) return;
        c.fill(x, G + 1, z, x, G + 8, z, Canvas.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        Props.ladder(c, x, G + 1, G + 8, z - 1, Direction.SOUTH);
        c.fill(x - 2, G + 9, z - 2, x + 2, G + 9, z + 2, s(Blocks.SPRUCE_PLANKS));
        c.set(x, G + 9, z - 1, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH, true, false));
        for (int i = -2; i <= 2; i++) {
            for (int[] p : new int[][]{{x + i, z - 2}, {x + i, z + 2}, {x - 2, z + i}, {x + 2, z + i}}) {
                c.set(p[0], G + 10, p[1], s(Blocks.SPRUCE_FENCE));
                if (c.chance(p[0], G + 11, p[1], 0.7f)) c.set(p[0], G + 11, p[1], Canvas.leaves(Blocks.SPRUCE_LEAVES));
            }
        }
        c.fill(x - 2, G + 12, z - 2, x + 2, G + 12, z + 2, Canvas.leaves(Blocks.SPRUCE_LEAVES));
        c.container(x + 1, G + 10, z + 1, facing(Blocks.CHEST, Direction.WEST), loot("port_tactical"));
    }

    /** Puente elevado sobre la curva norte del autódromo, con torres de escaleras y un control policial. */
    private static void bridge(Canvas c) {
        int z0 = 205, z1 = 209, y = G + 7, x0 = 194, x1 = 250;
        if (!c.touches(x0 - 4, z0 - 1, x1 + 4, z1 + 1)) return;
        c.fill(x0, y, z0, x1, y, z1, s(Blocks.GRAY_CONCRETE));
        c.fill(x0, y - 1, z0, x1, y - 1, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
        for (int x = x0; x <= x1; x++) {
            c.set(x, y + 1, z0, s(Blocks.ANDESITE_WALL));
            c.set(x, y + 1, z1, s(Blocks.ANDESITE_WALL));
            if (x % 4 == 0) c.set(x, y, 207, s(Blocks.YELLOW_CONCRETE));
        }
        for (int[] tw : new int[][]{{x0 - 4, x0 - 1}, {x1 + 1, Math.min(W - 1, x1 + 4)}}) {
            c.fill(tw[0], G + 1, z0, tw[1], y + 1, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
            c.air(tw[0] + 1, G + 1, z0 + 1, tw[1] - 1, y + 1, z1 - 1);
            int lx = tw[0] + 1, lz = z0 + 1;
            Props.ladder(c, lx, G + 1, y, lz, Direction.NORTH);
            c.air(lx + 1, G + 1, z0, lx + 1, G + 2, z0);
            c.air(tw[0] == x0 - 4 ? tw[1] : tw[0], y + 1, z0 + 2, tw[0] == x0 - 4 ? tw[1] : tw[0], y + 2, z0 + 2);
        }
        Props.jersey(c, 218, y + 1, z0 + 1, false, 3);
        policeCar(c, 224, y + 1, z0 + 3);
        c.container(236, y + 1, z0 + 2, facing(Blocks.CHEST, Direction.WEST), loot("dome_police"));
    }
}
