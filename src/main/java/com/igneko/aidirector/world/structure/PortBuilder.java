package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import static com.igneko.aidirector.world.structure.Canvas.facing;
import static com.igneko.aidirector.world.structure.Canvas.hangingLantern;
import static com.igneko.aidirector.world.structure.Canvas.slab;
import static com.igneko.aidirector.world.structure.Canvas.stairs;
import static com.igneko.aidirector.world.structure.Canvas.trapdoor;
import static com.igneko.aidirector.world.structure.Props.AIR;
import static com.igneko.aidirector.world.structure.Props.loot;
import static com.igneko.aidirector.world.structure.Props.s;

/**
 * Terminal portuaria y megabuque portacontenedores (256 × 216 bloques).
 *
 * <pre>
 *  z=0 ─────────────── dársena (agua hasta y=14, fondo y=0) ───────────────
 *        remolcador ▭        ┌──────────────── buque (x 30-229, z 32-61) ───────────────┐
 *   remolcador ▯ popa│ máquinas·chimenea·puente │ bodega1 │ bodega2 │ bodega3 │ bodega4 │ castillo de proa ▷
 *  z=64 ═══════════ muelle (y=16): defensas, bolardos, 5 grúas pórtico STS ═══════════════════
 *  z=96  bobinas │ laberinto de contenedores │ bloques de contenedores con grúas RTG │ grúa móvil
 *  z=213 ─────────────────────── valla y garita ────────────────────────────────────
 * </pre>
 */
final class PortBuilder {

    private PortBuilder() {}

    static final int W = 256, D = 216, H = 66;
    /** Última capa de agua, cota del muelle y primera fila de tierra. */
    static final int WATER_TOP = 14, Q = 16, QZ = 64;
    private static final int G = Q + 1;

    // Buque: eslora 200 (x 30..229), manga 30 (z 32..61), quilla en 4, cubierta en 30.
    private static final int SX0 = 30, SX1 = 229, SZ0 = 32, SZ1 = 61, KEEL = 4, DK = 30;
    private static final double SZC = 46.5;
    private static final int[] HOLDS = {72, 102, 132, 162};
    private static final int FORE = 192;

    private static final Palette SEABED = Palette.weighted(Blocks.GRAVEL, 3, Blocks.SAND, 2, Blocks.CLAY, 1);
    private static final Palette QUAY_WALL = Palette.weighted(Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 1,
            Blocks.MOSSY_STONE_BRICKS, 1, Blocks.ANDESITE, 1);
    private static final Palette APRON = Palette.weighted(Blocks.LIGHT_GRAY_CONCRETE, 5, Blocks.SMOOTH_STONE, 3,
            Blocks.POLISHED_ANDESITE, 1, Blocks.LIGHT_GRAY_CONCRETE_POWDER, 1);
    private static final Palette YARD = Palette.weighted(Blocks.GRAY_CONCRETE, 4, Blocks.LIGHT_GRAY_CONCRETE, 2,
            Blocks.POLISHED_ANDESITE, 1, Blocks.CYAN_TERRACOTTA, 1);
    private static final Palette HULL_RED = Palette.weighted(Blocks.RED_CONCRETE, 3, Blocks.RED_TERRACOTTA, 1);
    private static final Palette HULL_BLACK = Palette.weighted(Blocks.BLACK_CONCRETE, 4, Blocks.DEEPSLATE_TILES, 1,
            Blocks.POLISHED_DEEPSLATE, 1);
    private static final Palette BOW = Palette.weighted(Blocks.RED_TERRACOTTA, 2, Blocks.ORANGE_CONCRETE, 2, Blocks.EXPOSED_COPPER, 1);
    private static final Palette DECK = Palette.weighted(Blocks.GRAY_CONCRETE, 3, Blocks.CYAN_TERRACOTTA, 1);
    private static final Palette HATCH = Palette.weighted(Blocks.GREEN_TERRACOTTA, 2, Blocks.GRAY_CONCRETE, 1);
    private static final Palette STEEL = Palette.weighted(Blocks.BLACK_CONCRETE, 3, Blocks.POLISHED_BLACKSTONE, 1);
    private static final Palette WHITE = Palette.weighted(Blocks.WHITE_CONCRETE, 4, Blocks.SMOOTH_QUARTZ, 2);
    private static final Palette FUNNEL = Palette.weighted(Blocks.SMOOTH_SANDSTONE, 2, Blocks.YELLOW_TERRACOTTA, 2, Blocks.YELLOW_CONCRETE, 1);
    private static final Palette CRANE = Palette.weighted(Blocks.LIGHT_BLUE_CONCRETE, 4, Blocks.CYAN_TERRACOTTA, 1);

    static void build(Canvas c) {
        basin(c);
        quay(c);
        ship(c);
        tugboat(c, 150, 26, Direction.EAST);
        tugboat(c, 19, 30, Direction.NORTH);
        for (int i = 0; i < 5; i++) stsCrane(c, 40 + i * 40, i);
        yard(c);
        coils(c);
        apronProps(c);
        backFence(c);
        stashes(c);
    }

    // ================================================================== escondites

    /**
     * Cuartos secretos con botín mejor que el de los cofres a la vista: cuatro escotillas de mantenimiento
     * repartidas por el muelle, el patio, la oficina y la garita, y una cámara oculta dentro de la proa
     * del barco (se entra por una escotilla de la cubierta del castillo y una escalera de mano).
     */
    private static void stashes(Canvas c) {
        Props.stash(c, 100, Q, 78, "port_stash", "port_industrial", null);
        Props.stash(c, 246, Q, 70, "port_stash", null, SupplyCrateBlock.Kind.THROWABLES);
        Props.stash(c, 43, Q, 126, "port_stash", "port_industrial", null);
        Props.stash(c, 30, Q, 190, "port_stash", "port_commercial", SupplyCrateBlock.Kind.MEDICAL);
        Props.stash(c, 250, Q, 207, "port_stash", "port_industrial", null);
        bowStash(c);
    }

    /** Cámara oculta en la proa: 7 × 7 × 4, bajo cubierta, con escalera desde una escotilla del castillo. */
    private static void bowStash(Canvas c) {
        int x0 = 197, x1 = 203, z0 = 43, z1 = 49, y0 = 9, y1 = 12;
        if (!c.touches(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) return;
        c.air(x0, y0, z0, x1, y1, z1);
        c.air(x0, y1 + 1, 46, x0, DK + 2, 46);
        for (int y = y0; y <= DK + 2; y++) c.set(x0, y, 46, Canvas.ladder(Direction.EAST));
        c.set(x0, DK + 3, 46, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, false, false));
        c.container(x1, y0, 44, facing(Blocks.CHEST, Direction.WEST), loot("port_stash"));
        c.container(x1, y0, 46, facing(Blocks.CHEST, Direction.WEST), loot("port_military"));
        c.container(x1, y0, 48, facing(Blocks.CHEST, Direction.WEST), loot("port_stash"));
        c.crate(x1 - 1, y0, 49, Direction.NORTH, SupplyCrateBlock.Kind.ARMORY, false);
        c.crate(x1 - 3, y0, 49, Direction.NORTH, SupplyCrateBlock.Kind.MEDICAL, false);
        c.set(x1 - 1, y0, 43, facing(Blocks.BARREL, Direction.UP));
        for (int x = x0 + 2; x <= x1 - 1; x += 2) c.set(x, y1, 46, hangingLantern(Blocks.SOUL_LANTERN));
        c.set(x0 + 3, y0, 44, s(Blocks.CAULDRON));
        c.armorStand(x0 + 2, y0, 48, Direction.EAST, new ItemStack(Items.IRON_HELMET));
    }

    // ================================================================== agua y muelle

    private static void basin(Canvas c) {
        BlockState water = s(Blocks.WATER);
        c.columns(0, 0, W - 1, QZ - 1, (x, z) -> {
            c.set(x, -1, z, s(Blocks.STONE));
            c.set(x, 0, z, SEABED);
            c.fill(x, 1, z, x, WATER_TOP, z, water);
            c.fill(x, WATER_TOP + 1, z, x, H - 1, z, AIR);
        });
    }

    private static void quay(Canvas c) {
        c.columns(0, QZ, W - 1, D - 1, (x, z) -> {
            if (z <= QZ + 1) {
                c.fill(x, 0, z, x, Q - 1, z, QUAY_WALL);
            } else {
                c.fill(x, Q - 3, z, x, Q - 1, z, s(Blocks.STONE));
                c.foundation(x, Q - 4, z, s(Blocks.STONE));
            }
            c.set(x, Q, z, surface(c, x, z));
            c.fill(x, G, z, x, H - 1, z, AIR);
        });
        // Defensas de neumático a ras del agua, escalas de gato y bolardos dobles.
        for (int x = 4; x < W - 4; x += 8) {
            if (!c.touches(x, QZ - 1, x + 1, QZ - 1)) continue;
            c.fill(x, WATER_TOP - 2, QZ - 1, x + 1, Q - 1, QZ - 1, s(Blocks.COAL_BLOCK));
            c.fill(x, Q - 1, QZ - 1, x + 1, Q - 1, QZ - 1, s(Blocks.BLACK_CONCRETE));
        }
        for (int x = 24; x < W; x += 48) Props.ladder(c, x, 2, Q, QZ - 1, Direction.SOUTH);
        for (int x = 10; x < W - 4; x += 14) Props.bollard(c, x, G, QZ + 2, true);
    }

    /** Pavimento del muelle con sus líneas de seguridad, carriles de grúa y marcas de calle. */
    private static BlockState surface(Canvas c, int x, int z) {
        if (z == QZ) return s((x / 2) % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE);
        if (z < 96) {
            if (z == 67 || z == 94) return s(Blocks.YELLOW_CONCRETE);
            if (z == 69 || z == 91) return s(Blocks.POLISHED_BLACKSTONE);
            return APRON.pick(c.noise(x, Q, z));
        }
        // Calles del patio: líneas discontinuas blancas en el centro de cada pasillo.
        for (int lane : new int[]{121, 147, 173, 203}) {
            if (z == lane && (x / 4) % 2 == 0) return s(Blocks.WHITE_CONCRETE);
        }
        return YARD.pick(c.noise(x, Q, z));
    }

    // ================================================================== buque

    /** ¿Está (x, y, z) dentro del casco? (proa afinada, bulbo, popa recogida y pantoque redondeado) */
    private static boolean inHull(int x, int y, int z) {
        int u = x - SX0;
        double dz = z - SZC;
        if (u < 0 || u > SX1 - SX0 || y < KEEL) return false;
        if (y > (x >= FORE ? DK + 2 : DK)) return false;
        if (bulb(u, y, dz)) return true;
        double hw = 15.0;
        double t = (u - 160 + (DK - y) * 0.4) / 39.0;
        if (t > 0) {
            if (t >= 1) return false;
            double yy = (y - KEEL) / (double) (DK - KEEL);
            double vee = 1 - (0.45 - 0.45 * yy) * Math.min(1, t * 3);
            hw *= Math.sqrt(1 - t * t) * vee;
        }
        if (u < 16 && y < 12) hw -= (16 - u) * 0.5 * (12 - y) / 8.0;
        if (y < KEEL + 3 && Math.abs(dz) > hw - 3) {
            double a = Math.abs(dz) - (hw - 3), b = KEEL + 3 - y;
            if (a * a + b * b > 9) return false;
        }
        return Math.abs(dz) < hw;
    }

    private static boolean bulb(int u, int y, double dz) {
        double a = (u - 192) / 7.0, b = (y - 8) / 3.2, d = dz / 3.2;
        return u <= 199 && a * a + b * b + d * d <= 1;
    }

    private static boolean shell(int x, int y, int z) {
        return !inHull(x - 1, y, z) || !inHull(x + 1, y, z) || !inHull(x, y, z - 1) || !inHull(x, y, z + 1)
                || !inHull(x, y - 1, z);
    }

    private static int holdAt(int x) {
        for (int i = 0; i < HOLDS.length; i++) if (x >= HOLDS[i] && x <= HOLDS[i] + 27) return i;
        return -1;
    }

    private static boolean underHatch(int x, int z) {
        int h = holdAt(x);
        return h >= 0 && x > HOLDS[h] && x < HOLDS[h] + 27 && z >= 34 && z <= 59;
    }

    private static void ship(Canvas c) {
        if (!c.touches(SX0 - 12, SZ0 - 4, SX1 + 2, SZ1 + 3)) return;
        BlockState air = AIR;
        c.columns(SX0, SZ0 - 1, SX1, SZ1 + 1, (x, z) -> {
            for (int y = KEEL; y <= DK + 2; y++) {
                if (!inHull(x, y, z)) continue;
                if (shell(x, y, z)) {
                    Palette p = y <= 13 ? HULL_RED : (x > FORE - 20 && y >= 26 ? BOW : HULL_BLACK);
                    c.set(x, y, z, p);
                } else {
                    BlockState in = interior(c, x, y, z);
                    c.set(x, y, z, in == null ? air : in);
                }
            }
            // Amurada con barandilla.
            if (x < FORE && inHull(x, DK, z) && (!inHull(x, DK, z - 1) || !inHull(x, DK, z + 1))) {
                c.set(x, DK + 1, z, HULL_BLACK);
                c.set(x, DK + 2, z, s(Blocks.IRON_BARS));
            }
            if (x >= FORE && inHull(x, DK + 2, z) && (!inHull(x, DK + 2, z - 1) || !inHull(x, DK + 2, z + 1) || !inHull(x + 1, DK + 2, z))) {
                c.set(x, DK + 3, z, BOW);
            }
            // Tapas de escotilla.
            if (underHatch(x, z)) c.set(x, DK + 1, z, HATCH);
        });
        hullLettering(c);
        engineRoom(c);
        funnel(c);
        superstructure(c);
        holds(c);
        deckCargo(c);
        forecastle(c);
        sternDeck(c);
        gangway(c);
    }

    /** Relleno del interior del casco: cubiertas, mamparos, doble fondo, bodegas y sala de máquinas. */
    private static BlockState interior(Canvas c, int x, int y, int z) {
        if (y <= 5) return y == 5 ? s(Blocks.POLISHED_BLACKSTONE) : STEEL.pick(c.noise(x, y, z));
        if (x >= FORE) return y == DK + 2 ? BOW.pick(c.noise(x, y, z)) : STEEL.pick(c.noise(x, y, z));
        if (y == DK) return underHatch(x, z) ? null : DECK.pick(c.noise(x, y, z));
        int h = holdAt(x);
        if (h >= 0) return null; // bodega
        if (x >= SX0 + 4 && x <= SX0 + 24) { // sala de máquinas, tres pisos
            if (y == 13 || y == 21) return slab(Blocks.SMOOTH_STONE_SLAB, true);
            return null;
        }
        return STEEL.pick(c.noise(x, y, z));
    }

    /** Siglas de la naviera (ficticia) en blanco sobre el costado que da al mar. */
    private static void hullLettering(Canvas c) {
        String[][] glyphs = {
                {"..###..", ".##.##.", "##...##", "##...##", "#######", "#######", "##...##", "##...##", "##...##"},
                {"######", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "######"},
                {"#####..", "##..##.", "##...##", "##...##", "##...##", "##...##", "##...##", "##..##.", "#####.."}
        };
        int start = 118, x = start;
        int[] xs = new int[glyphs.length];
        for (int g = 0; g < glyphs.length; g++) {
            xs[g] = x;
            x += glyphs[g][0].length() + 3;
        }
        int end = x - 4;
        if (!c.touches(start, SZ0, end, SZ0)) return;
        BlockState quartz = s(Blocks.QUARTZ_BLOCK);
        for (int g = 0; g < glyphs.length; g++) {
            for (int row = 0; row < 9; row++) {
                String line = glyphs[g][row];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) != '#') continue;
                    int lx = xs[g] + col;
                    if (c.mirrored()) lx = start + end - lx;
                    c.set(lx, 27 - row, SZ0, quartz);
                }
            }
        }
    }

    private static void engineRoom(Canvas c) {
        int x0 = SX0 + 4, x1 = SX0 + 24;
        if (!c.touches(x0, SZ0, x1, SZ1)) return;
        // Hueco del motor a través del primer entrepiso.
        c.air(x0 + 2, 13, 42, x0 + 17, 13, 51);
        // Motor diésel de 15 bloques: bancada, bloque, culatas con pistones, turbo y tuberías.
        int ex0 = x0 + 2, ex1 = ex0 + 14;
        c.fill(ex0, 6, 43, ex1, 7, 50, s(Blocks.IRON_BLOCK));
        c.fill(ex0, 8, 44, ex1, 11, 49, s(Blocks.IRON_BLOCK));
        for (int x = ex0; x <= ex1; x += 2) {
            c.set(x, 9, 43, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
            c.set(x, 9, 50, facing(Blocks.BLAST_FURNACE, Direction.NORTH));
            c.set(x, 12, 46, facing(Blocks.PISTON, Direction.UP));
            c.set(x, 12, 47, facing(Blocks.PISTON, Direction.UP));
        }
        c.fill(ex0, 12, 44, ex1, 12, 44, s(Blocks.OXIDIZED_CUT_COPPER));
        c.fill(ex0, 12, 49, ex1, 12, 49, s(Blocks.OXIDIZED_CUT_COPPER));
        c.fill(ex1 + 1, 8, 45, ex1 + 2, 11, 48, s(Blocks.WEATHERED_CUT_COPPER));
        c.set(ex1 + 1, 12, 46, facing(Blocks.HOPPER, Direction.DOWN));
        // Eje de la hélice hacia popa.
        for (int x = SX0 + 1; x < ex0; x++) c.set(x, 8, 46, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.X));
        // Escape hasta la chimenea.
        c.fill(ex0 + 10, 12, 46, ex0 + 10, DK + 26, 46, s(Blocks.BLACK_CONCRETE));
        // Calderos de aceite, generadores y bancos de taller.
        for (int x = x0 + 1; x <= x0 + 7; x += 2) c.set(x, 6, 35, s(Blocks.CAULDRON));
        for (int x = x0 + 2; x <= x0 + 12; x += 5) {
            c.set(x, 14, 35, facing(Blocks.BLAST_FURNACE, Direction.NORTH));
            c.set(x + 1, 14, 35, s(Blocks.IRON_BLOCK));
            c.set(x + 1, 15, 35, s(Blocks.LIGHTNING_ROD));
        }
        c.set(x0 + 4, 14, 58, s(Blocks.ANVIL));
        c.set(x0 + 5, 14, 58, s(Blocks.GRINDSTONE));
        c.set(x0 + 6, 14, 58, s(Blocks.SMITHING_TABLE));
        c.container(x0 + 7, 14, 58, facing(Blocks.BARREL, Direction.UP), loot("port_industrial"));
        // Sala de control con vista al motor.
        c.fill(x0 + 6, 22, 34, x0 + 14, 25, 39, s(Blocks.GLASS_PANE));
        c.air(x0 + 7, 22, 34, x0 + 13, 25, 38);
        c.fill(x0 + 6, 25, 34, x0 + 14, 25, 39, s(Blocks.WHITE_CONCRETE));
        c.air(x0 + 7, 22, 39, x0 + 7, 23, 39);
        for (int x = x0 + 8; x <= x0 + 12; x++) {
            c.set(x, 22, 35, s(Blocks.POLISHED_BLACKSTONE));
            c.set(x, 23, 35, x % 2 == 0 ? Props.LEVER : s(Blocks.REDSTONE_LAMP));
        }
        c.set(x0 + 9, 22, 37, s(Blocks.NOTE_BLOCK));
        c.container(x0 + 12, 22, 37, facing(Blocks.CHEST, Direction.WEST), loot("port_industrial"));
        // Luz y escalas entre pisos (junto al mamparo de proa).
        for (int x = x0 + 3; x <= x1 - 2; x += 6) {
            c.set(x, 12, 56, hangingLantern(Blocks.LANTERN));
            c.set(x, 20, 38, hangingLantern(Blocks.LANTERN));
            c.set(x, 29, 56, hangingLantern(Blocks.LANTERN));
        }
        c.air(x1, 13, 34, x1, 13, 34);
        c.air(x1, 21, 34, x1, 21, 34);
        Props.ladder(c, x1, 6, DK - 1, 34, Direction.WEST);
        c.set(x1, DK, 34, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, true));
    }

    private static void funnel(Canvas c) {
        int x0 = SX0 + 12, x1 = SX0 + 21;
        if (!c.touches(x0, 41, x1, 52)) return;
        c.fill(x0, DK + 1, 41, x1, DK + 24, 52, FUNNEL);
        c.fill(x0, DK + 25, 41, x1, DK + 26, 52, s(Blocks.BLACK_CONCRETE));
        c.air(x0 + 1, DK + 1, 42, x1 - 1, DK + 26, 51);
        c.fill(x0 + 3, DK + 22, 44, x0 + 4, DK + 28, 45, s(Blocks.BLACK_CONCRETE));
        c.fill(x0 + 5, DK + 22, 48, x0 + 6, DK + 27, 49, s(Blocks.BLACK_CONCRETE));
        c.fill(x0 + 4, DK + 1, 46, x0 + 4, DK + 26, 46, s(Blocks.BLACK_CONCRETE));
    }

    /** Superestructura blanca de 24 × 22 con alerones, bote salvavidas y cinco niveles. */
    private static void superstructure(Canvas c) {
        int x0 = SX0 + 26, x1 = SX0 + 37, z0 = 35, z1 = 58, roof = DK + 20;
        if (!c.touches(x0 - 2, 29, x1 + 2, 64)) return;
        c.fill(x0, DK + 1, z0, x1, roof, z1, WHITE);
        int[] floors = {DK, DK + 4, DK + 8, DK + 12, DK + 16};
        for (int f : floors) {
            c.air(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 3, z1 - 1);
            if (f > DK) c.fill(x0 + 1, f, z0 + 1, x1 - 1, f, z1 - 1, s(Blocks.SMOOTH_STONE));
            // Ojos de buey a los costados y en la popa.
            for (int x = x0 + 2; x < x1; x += 2) {
                c.set(x, f + 2, z0, s(Blocks.GLASS_PANE));
                c.set(x, f + 2, z1, s(Blocks.GLASS_PANE));
            }
            for (int z = z0 + 3; z < z1; z += 3) c.set(x0, f + 2, z, s(Blocks.GLASS_PANE));
            if (f < DK + 16) for (int z = z0 + 2; z < z1; z += 2) c.set(x1, f + 2, z, s(Blocks.GLASS_PANE));
            // Hueco de la escala.
            if (f > DK) c.set(x0 + 1, f, z0 + 2, AIR);
        }
        Props.ladder(c, x0 + 1, DK + 1, roof - 1, z0 + 2, Direction.EAST);
        c.set(x0 + 1, roof, z0 + 2, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, true, true));
        // Puertas a la cubierta.
        c.door(x0, DK + 1, 46, Blocks.DARK_OAK_DOOR, Direction.EAST, DoorHingeSide.LEFT);
        c.door(x0 + 5, DK + 1, z0, Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        c.door(x0 + 5, DK + 1, z1, Blocks.SPRUCE_DOOR, Direction.SOUTH, DoorHingeSide.LEFT);

        // Nivel 1: cocina industrial y comedor.
        int y = DK + 1;
        for (int z = z0 + 1; z <= z0 + 9; z += 2) {
            c.set(x1 - 1, y, z, facing(Blocks.SMOKER, Direction.WEST));
            c.set(x1 - 1, y + 1, z, facing(Blocks.FURNACE, Direction.WEST));
            c.set(x1 - 1, y + 2, z, facing(Blocks.HOPPER, Direction.DOWN));
        }
        c.set(x1 - 3, y, z0 + 3, s(Blocks.CAULDRON));
        c.container(x1 - 1, y, z0 + 10, facing(Blocks.BARREL, Direction.UP), loot("safe_room_rations"));
        for (int z = z0 + 13; z <= z1 - 2; z++) {
            for (int x : new int[]{x0 + 3, x0 + 7}) {
                c.set(x, y, z, slab(Blocks.SMOOTH_STONE_SLAB, true));
                if (c.chance(x, y, z, 0.6f)) c.set(x - 1, y, z, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
            }
        }
        c.fill(x0 + 1, y, z0 + 11, x1 - 1, y + 2, z0 + 11, WHITE);
        c.set(x0 + 5, y, z0 + 11, AIR);
        c.set(x0 + 5, y + 1, z0 + 11, AIR);

        // Nivel 2: camarotes con literas (algunas volcadas).
        y = DK + 5;
        for (int z = z0 + 5; z < z1; z += 5) c.fill(x0 + 1, y, z, x1 - 1, y + 2, z, WHITE);
        c.fill(x0 + 5, y, z0 + 1, x0 + 6, y + 1, z1 - 1, AIR); // pasillo
        for (int z = z0 + 1; z < z1 - 3; z += 5) {
            for (int x : new int[]{x0 + 2, x1 - 3}) {
                if (c.chance(x, y, z, 0.7f)) {
                    c.bed(x, y, z + 1, Blocks.BROWN_BED, Direction.EAST);
                    c.bed(x, y + 2, z + 1, Blocks.BROWN_BED, Direction.EAST);
                } else {
                    c.set(x, y, z + 1, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, false, true));
                    c.set(x + 1, y, z + 2, s(Blocks.BROWN_CARPET));
                }
                c.container(x, y, z + 3, facing(Blocks.BARREL, Direction.UP), loot("port_textile"));
            }
        }

        // Nivel 3: enfermería saqueada.
        y = DK + 9;
        for (int z = z0 + 2; z < z0 + 12; z += 3) c.bed(x0 + 2, y, z, Blocks.WHITE_BED, Direction.EAST);
        c.set(x1 - 2, y, z0 + 2, s(Blocks.BREWING_STAND));
        c.set(x1 - 3, y, z0 + 2, s(Blocks.CAULDRON));
        c.crate(x1 - 2, y, z0 + 5, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
        c.container(x1 - 2, y, z0 + 8, facing(Blocks.CHEST, Direction.WEST), loot("port_medical"));
        for (int i = 0; i < 6; i++) {
            int x = x0 + 2 + (i * 3) % 8, z = z0 + 3 + i * 3;
            c.set(x, y + 2, z, s(Blocks.COBWEB));
        }
        c.container(x0 + 3, y, z1 - 2, facing(Blocks.CHEST, Direction.NORTH), loot("port_commercial"));

        // Nivel 4: radio y camarote del capitán.
        y = DK + 13;
        c.fill(x0 + 1, y, z0 + 12, x1 - 1, y + 2, z0 + 12, WHITE);
        c.set(x0 + 6, y, z0 + 12, AIR);
        c.set(x0 + 6, y + 1, z0 + 12, AIR);
        for (int z = z0 + 2; z <= z0 + 9; z++) {
            c.set(x1 - 1, y, z, s(Blocks.POLISHED_BLACKSTONE));
            c.set(x1 - 1, y + 1, z, z % 3 == 0 ? s(Blocks.NOTE_BLOCK) : z % 3 == 1 ? s(Blocks.REDSTONE_LAMP) : s(Blocks.OBSERVER));
        }
        c.bed(x0 + 2, y, z1 - 3, Blocks.BLUE_BED, Direction.EAST);
        c.set(x0 + 2, y, z1 - 1, s(Blocks.BOOKSHELF));
        c.set(x0 + 3, y, z1 - 1, s(Blocks.BOOKSHELF));
        c.container(x1 - 2, y, z1 - 2, facing(Blocks.CHEST, Direction.WEST), loot("port_military"));

        // Nivel 5: puente de mando con ventanal panorámico tintado, alerones y consolas.
        y = DK + 17;
        c.fill(x1, y, z0, x1, y + 2, z1, s(Blocks.GRAY_STAINED_GLASS));
        for (int z = z0; z <= z1; z += 6) c.fill(x1, y, z, x1, y + 2, z, s(Blocks.WHITE_CONCRETE));
        for (int z = z0 + 2; z <= z1 - 2; z++) {
            c.set(x1 - 1, y, z, s(Blocks.POLISHED_BLACKSTONE));
            BlockState top = switch (Math.floorMod(z, 4)) {
                case 0 -> Props.LEVER;
                case 1 -> s(Blocks.DAYLIGHT_DETECTOR);
                case 2 -> Props.BUTTON;
                default -> AIR;
            };
            c.set(x1 - 1, y + 1, z, top);
        }
        c.set(x1 - 3, y, 46, s(Blocks.OBSERVER));
        c.set(x1 - 3, y, 47, s(Blocks.OBSERVER));
        c.set(x1 - 3, y + 1, 46, Props.LEVER);
        c.itemFrame(x1 - 1, y + 2, 41, Direction.WEST, new ItemStack(Items.COMPASS));
        c.itemFrame(x1 - 1, y + 2, 52, Direction.WEST, new ItemStack(Items.CLOCK));
        c.set(x0 + 4, y, 46, s(Blocks.CARTOGRAPHY_TABLE));
        c.set(x0 + 4, y, 47, s(Blocks.CARTOGRAPHY_TABLE));
        c.container(x0 + 3, y, z1 - 2, facing(Blocks.CHEST, Direction.EAST), loot("port_military"));
        for (int z : new int[]{z0 + 3, z1 - 3}) c.set(x0 + 6, y, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
        for (int side = 0; side < 2; side++) {
            int za = side == 0 ? z0 - 5 : z1 + 1, zb = side == 0 ? z0 - 1 : z1 + 5;
            c.fill(x1 - 4, y - 1, za, x1, y - 1, zb, s(Blocks.WHITE_CONCRETE));
            c.air(x1 - 4, y, za, x1, y + 2, zb);
            int edge = side == 0 ? za : zb;
            c.fill(x1 - 4, y, edge, x1, y, edge, s(Blocks.ORANGE_STAINED_GLASS_PANE));
            c.fill(x1, y, za, x1, y, zb, s(Blocks.ORANGE_STAINED_GLASS_PANE));
            c.set(x1 - 1, y, side == 0 ? za + 1 : zb - 1, Props.LEVER);
            c.air(x1 - 3, y, side == 0 ? z0 : z1, x1 - 3, y + 1, side == 0 ? z0 : z1);
        }
        // Cubierta superior con barandilla naranja, mástil de radar y antenas.
        c.fill(x0, roof + 1, z0, x1, roof + 1, z0, s(Blocks.ORANGE_STAINED_GLASS_PANE));
        c.fill(x0, roof + 1, z1, x1, roof + 1, z1, s(Blocks.ORANGE_STAINED_GLASS_PANE));
        c.fill(x0, roof + 1, z0, x0, roof + 1, z1, s(Blocks.ORANGE_STAINED_GLASS_PANE));
        c.fill(x1, roof + 1, z0, x1, roof + 1, z1, s(Blocks.ORANGE_STAINED_GLASS_PANE));
        c.fill(x0 + 5, roof + 1, 46, x0 + 5, roof + 8, 46, s(Blocks.IRON_BARS));
        c.fill(x0 + 4, roof + 6, 46, x0 + 6, roof + 6, 46, s(Blocks.IRON_BLOCK));
        c.set(x0 + 4, roof + 7, 46, s(Blocks.DAYLIGHT_DETECTOR));
        c.set(x0 + 6, roof + 7, 46, s(Blocks.DAYLIGHT_DETECTOR));
        c.set(x0 + 5, roof + 9, 46, s(Blocks.LANTERN));
        c.set(x0 + 8, roof + 1, 40, s(Blocks.LIGHTNING_ROD));
        c.set(x0 + 8, roof + 1, 53, s(Blocks.LIGHTNING_ROD));
        // Bote salvavidas naranja en sus pescantes (costado del mar).
        c.fill(x0 + 2, DK + 6, 31, x0 + 8, DK + 6, 33, s(Blocks.ORANGE_CONCRETE));
        c.fill(x0 + 3, DK + 7, 31, x0 + 7, DK + 7, 33, s(Blocks.WHITE_CONCRETE));
        c.set(x0 + 2, DK + 6, 31, AIR);
        c.set(x0 + 8, DK + 6, 33, AIR);
        for (int x : new int[]{x0 + 3, x0 + 7}) c.fill(x, DK + 8, 32, x, DK + 8, 34, s(Blocks.IRON_BARS));
    }

    private static void holds(Canvas c) {
        for (int h = 0; h < HOLDS.length; h++) {
            int x0 = HOLDS[h], x1 = x0 + 27;
            if (!c.touches(x0, SZ0, x1, SZ1)) continue;
            // Guías de celda en los costados, oscuridad y agua estancada en la sentina.
            for (int x = x0 + 2; x < x1; x += 3) {
                for (int z : new int[]{33, 60}) c.fill(x, 6, z, x, DK - 1, z, s(Blocks.IRON_BARS));
            }
            boolean sealed = h == 2;
            if (!sealed) {
                c.columns(x0, 33, x1, 60, (x, z) -> {
                    if (inHull(x, 6, z) && !shell(x, 6, z)) c.set(x, 6, z, s(Blocks.WATER));
                });
                Props.containerX(c, x0 + 4, 7, 40, 13, Props.Color.pick(c.noise(x0, 7, 40)), Props.Kind.CLOSED);
                Props.containerX(c, x0 + 6, 7, 49, 7, Props.Color.pick(c.noise(x0, 7, 49)), Props.Kind.BREACHED);
                c.container(x0 + 20, 7, 45, facing(Blocks.BARREL, Direction.UP), loot("port_industrial"));
                for (int i = 0; i < 5; i++) c.set(x0 + 3 + i * 5, DK - 1, 36 + i * 4, s(Blocks.COBWEB));
            } else {
                // Bodega sellada: armamento de contrabando bajo llave hasta superar el Crescendo.
                for (int i = 0; i < 4; i++) c.crate(x0 + 6 + i * 4, 6, 44, Direction.SOUTH, SupplyCrateBlock.Kind.ARMORY, true);
                c.crate(x0 + 6, 6, 49, Direction.NORTH, SupplyCrateBlock.Kind.MEDICAL, true);
                c.crate(x0 + 10, 6, 49, Direction.NORTH, SupplyCrateBlock.Kind.THROWABLES, true);
                c.container(x0 + 14, 6, 49, facing(Blocks.CHEST, Direction.NORTH), loot("port_military"));
                c.container(x0 + 18, 6, 49, facing(Blocks.CHEST, Direction.NORTH), loot("port_military"));
                c.alarm(x0 + 13, 6, 40, Direction.NORTH, 90);
                Props.containerX(c, x0 + 3, 6, 54, 13, Props.Color.GRAY, Props.Kind.OPEN);
                Props.containerX(c, x0 + 3, 6, 35, 13, Props.Color.GREEN, Props.Kind.CLOSED);
                for (int x = x0 + 4; x < x1; x += 7) c.set(x, DK - 1, 46, hangingLantern(Blocks.SOUL_LANTERN));
            }
            // Escotilla vertical con escala junto al costado.
            c.set(x0, DK, 33, trapdoor(sealed ? Blocks.IRON_TRAPDOOR : Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true, !sealed));
            Props.ladder(c, x0, 6, DK - 1, 33, Direction.NORTH);
        }
    }

    /** Pilas de contenedores sobre las tapas de escotilla (dos bahías de 13 por bodega). */
    private static void deckCargo(Canvas c) {
        for (int x0 : HOLDS) {
            if (!c.touches(x0, 34, x0 + 27, 59)) continue;
            for (int bay = 0; bay < 2; bay++) {
                int bx = x0 + 1 + bay * 13;
                for (int j = 0; j < 8; j++) {
                    int z = 35 + j * 3;
                    int tiers = 2 + (int) (c.noise(bx, 3, z) * 4);
                    for (int t = 0; t < tiers; t++) {
                        float roll = c.noise(bx + t, 9, z);
                        Props.Color col = roll < 0.55f ? Props.Color.WHITE : Props.Color.pick(c.noise(bx, t, z + 1));
                        Props.containerX(c, bx, DK + 2 + t * 3, z, 13, col, Props.Kind.CLOSED);
                    }
                }
            }
        }
    }

    /** Castillo de proa: rompeolas, molinetes, cadenas, cabrestantes, respiraderos y bolardos. */
    private static void forecastle(Canvas c) {
        if (!c.touches(FORE, SZ0 - 2, SX1, SZ1 + 2)) return;
        int y = DK + 3;
        // Rompeolas en V.
        for (int z = 33; z <= 60; z++) {
            int x = FORE + 3 + (int) (Math.abs(z - SZC) / 3);
            if (inHull(x, DK + 2, z)) c.fill(x, y, z, x, y + 1, z, s(Blocks.ORANGE_CONCRETE));
        }
        for (int[] w : new int[][]{{212, 41}, {212, 52}}) {
            int x = w[0], z = w[1];
            c.fill(x - 1, y, z - 1, x + 1, y, z + 1, s(Blocks.IRON_BLOCK));
            c.set(x, y + 1, z, s(Blocks.GRINDSTONE));
            c.set(x - 1, y + 1, z, s(Blocks.IRON_BLOCK));
            c.set(x + 1, y + 1, z, s(Blocks.IRON_BLOCK));
            for (int cx = x + 2; cx <= 221; cx++) c.set(cx, y, z, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.X));
            c.fill(222, 24, z, 222, y - 1, z, s(Blocks.CHAIN));
            anchor(c, 222, z < SZC);
        }
        for (int z : new int[]{38, 55}) {
            c.set(204, y, z, s(Blocks.WHITE_CONCRETE));
            c.set(204, y + 1, z, s(Blocks.RED_CONCRETE));
            c.set(204, y + 2, z, slab(Blocks.SMOOTH_STONE_SLAB, false));
        }
        for (int[] v : new int[][]{{207, 46}, {216, 44}, {216, 49}}) {
            c.set(v[0], y, v[1], s(Blocks.IRON_BARS));
            c.set(v[0], y + 1, v[1], s(Blocks.IRON_BARS));
            c.set(v[0], y + 2, v[1], facing(Blocks.HOPPER, Direction.DOWN));
        }
        for (int x : new int[]{198, 218}) {
            for (int z = 33; z <= 60; z++) {
                if (inHull(x, DK + 2, z) && (!inHull(x, DK + 2, z - 2) || !inHull(x, DK + 2, z + 2))) {
                    Props.bollard(c, x, y, z, true);
                    break;
                }
            }
        }
        c.fill(225, y, 46, 225, y + 11, 46, s(Blocks.IRON_BARS));
        c.set(225, y + 12, 46, s(Blocks.LANTERN));
        c.set(225, y + 8, 47, s(Blocks.END_ROD));
        c.container(200, y, 46, facing(Blocks.CHEST, Direction.WEST), loot("port_industrial"));
    }

    /** Ancla encajada en el escobén, en el costado del casco (z menor o mayor). */
    private static void anchor(Canvas c, int x, boolean seaward) {
        for (int y = 22; y <= 26; y++) {
            int z = seaward ? SZ0 : SZ1;
            for (int s = 0; s < 16; s++) {
                int zz = seaward ? (int) SZC - s : (int) SZC + 1 + s;
                if (!inHull(x, y, zz)) {
                    z = seaward ? zz + 1 : zz - 1;
                    break;
                }
            }
            c.set(x, y, z, y == 22 ? s(Blocks.ANVIL) : s(Blocks.IRON_BLOCK));
        }
    }

    private static void sternDeck(Canvas c) {
        if (!c.touches(SX0, SZ0, SX0 + 11, SZ1)) return;
        for (int z : new int[]{35, 58}) {
            Props.bollard(c, SX0 + 3, DK + 1, z, true);
            Props.bollard(c, SX0 + 9, DK + 1, z, true);
            c.set(SX0 + 6, DK + 1, z, s(Blocks.WHITE_CONCRETE));
            c.set(SX0 + 6, DK + 2, z, s(Blocks.RED_CONCRETE));
        }
        c.fill(SX0 + 4, DK + 1, 45, SX0 + 6, DK + 1, 48, s(Blocks.IRON_BLOCK));
        c.fill(SX0 + 5, DK + 2, 45, SX0 + 5, DK + 2, 48, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        // Timón bajo la popa.
        c.fill(SX0 - 1, KEEL + 1, 46, SX0 - 1, 12, 47, s(Blocks.BLACK_CONCRETE));
    }

    /** Escala real desde el muelle hasta la cubierta, sobre el hueco de las defensas. */
    private static void gangway(Canvas c) {
        int x0 = 70;
        if (!c.touches(x0, 62, x0 + 17, 63)) return;
        for (int i = 0; i <= DK - G; i++) {
            for (int z = 62; z <= 63; z++) {
                c.set(x0 + i, G + i, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
                c.set(x0 + i, G + i - 1, z, slab(Blocks.DARK_OAK_SLAB, true));
            }
            c.set(x0 + i, G + i + 1, 63, s(Blocks.IRON_BARS));
        }
        int top = x0 + DK - G + 1;
        c.fill(top, DK, 62, top + 2, DK, 63, s(Blocks.DARK_OAK_PLANKS));
        c.air(top + 1, DK + 1, SZ1, top + 1, DK + 2, SZ1);
        c.air(top + 1, DK + 1, SZ1 - 1, top + 1, DK + 2, SZ1 - 1);
    }

    // ================================================================== remolcadores

    /** Remolcador de 22 × 9 (casco negro y azul, cubierta de madera, neumáticos, cabina blanca). */
    private static void tugboat(Canvas c, int x, int z, Direction facing) {
        Frame f = new Frame(c, x, 0, z, facing);
        if (!f.touches(-1, -1, 22, 9)) return;
        for (int a = 0; a < 22; a++) {
            double hw = a <= 14 ? 4.5 : 4.5 * Math.sqrt(Math.max(0, 1 - Math.pow((a - 14) / 8.0, 2)));
            for (int b = 0; b <= 8; b++) {
                double d = Math.abs(b - 4);
                if (d > hw) continue;
                boolean edge = d > hw - 1 || a == 0;
                for (int y = 10; y <= 16; y++) {
                    BlockState st;
                    if (y == 16) st = a <= 9 ? s(Blocks.DARK_OAK_PLANKS) : s(Blocks.GRAY_CONCRETE);
                    else if (edge || y == 10) st = y == 14 ? s(Blocks.BLUE_CONCRETE) : s(Blocks.BLACK_CONCRETE);
                    else st = s(Blocks.BLACK_CONCRETE);
                    f.set(a, y, b, st);
                }
                if (edge) {
                    f.set(a, 17, b, s(Blocks.BLACK_CONCRETE));
                    if (a % 3 == 1 && a < 20) f.set(a, 15, b, s(Blocks.COAL_BLOCK));
                }
                f.air(a, 18, b, a, 30, b);
            }
        }
        f.fill(20, 13, 3, 21, 16, 5, s(Blocks.COAL_BLOCK)); // defensa de proa
        // Cabina y puente.
        f.fill(10, 17, 2, 16, 19, 6, WHITE);
        f.air(11, 17, 3, 15, 18, 5);
        f.fill(11, 18, 2, 15, 18, 2, s(Blocks.GLASS_PANE));
        f.fill(11, 18, 6, 15, 18, 6, s(Blocks.GLASS_PANE));
        f.set(10, 17, 4, AIR);
        f.set(10, 18, 4, AIR);
        f.container(15, 17, 3, facing(Blocks.CHEST, Direction.WEST), loot("port_tugboat"));
        f.container(15, 17, 5, facing(Blocks.BARREL, Direction.UP), loot("port_tugboat"));
        f.fill(12, 20, 2, 15, 22, 6, s(Blocks.GLASS));
        f.fill(12, 20, 2, 15, 20, 6, s(Blocks.WHITE_CONCRETE));
        f.air(13, 20, 3, 14, 22, 5);
        f.fill(12, 23, 2, 15, 23, 6, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
        f.set(14, 20, 4, Props.LEVER);
        // Mástil con radares, antenas y luz.
        f.fill(14, 24, 4, 14, 29, 4, s(Blocks.IRON_BARS));
        f.fill(13, 26, 4, 15, 26, 4, s(Blocks.IRON_BLOCK));
        f.set(13, 27, 4, s(Blocks.DAYLIGHT_DETECTOR));
        f.set(15, 27, 4, s(Blocks.DAYLIGHT_DETECTOR));
        f.set(14, 30, 4, s(Blocks.LANTERN));
        f.set(12, 24, 2, s(Blocks.LIGHTNING_ROD));
        f.set(12, 24, 6, s(Blocks.END_ROD));
        // Gancho de remolque y bitas en la cubierta de popa.
        f.set(3, 17, 4, s(Blocks.ANVIL));
        f.set(6, 17, 2, s(Blocks.POLISHED_BLACKSTONE_WALL));
        f.set(6, 17, 6, s(Blocks.POLISHED_BLACKSTONE_WALL));
        f.fill(1, 17, 3, 2, 17, 5, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        c.armorStand(f.x(8, 3), f.worldY(17), f.z(8, 3), f.dir(Direction.SOUTH), new ItemStack(Items.TURTLE_HELMET));
    }

    // ================================================================== grúas pórtico

    /** Grúa STS de 45 de alto con pluma a franjas que vuela 38 bloques sobre el agua y cabina colgante. */
    private static void stsCrane(Canvas c, int x, int index) {
        if (!c.touches(x - 1, 24, x + 14, 108)) return;
        int top = G + 28, boom = G + 29, apex = Q + 45;
        // Patas (2×2) sobre los carriles y vigas del pórtico.
        for (int lx : new int[]{x, x + 12}) {
            for (int lz : new int[]{68, 90}) {
                c.fill(lx, G, lz, lx + 1, top, lz + 1, CRANE);
                c.fill(lx, G, lz, lx + 1, G, lz + 1, s(Blocks.BLACK_CONCRETE));
                for (int y = G + 6; y < top; y += 8) c.fill(lx, y, lz, lx + 1, y, lz + 1, s(Blocks.CYAN_TERRACOTTA));
            }
            c.fill(lx, top - 1, 68, lx + 1, top, 91, CRANE);
            c.fill(lx, G + 14, 70, lx + 1, G + 14, 89, s(Blocks.ANDESITE_WALL));
            c.line(lx, G + 2, 70, lx, G + 13, 89, s(Blocks.ANDESITE_WALL));
        }
        for (int lz : new int[]{68, 90}) c.fill(x, top - 1, lz, x + 13, top, lz + 1, CRANE);
        // Pluma: piso, celosía lateral y cordones superiores a franjas rojas y blancas.
        for (int z = 26; z <= 106; z++) {
            BlockState stripe = s(((z / 3) % 2 == 0) ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE);
            c.fill(x + 5, boom, z, x + 8, boom, z, stripe);
            for (int bx : new int[]{x + 5, x + 8}) {
                c.set(bx, boom + 1, z, z % 4 == 0 ? stripe : s(Blocks.IRON_BARS));
                c.set(bx, boom + 2, z, z % 4 == 0 ? stripe : s(Blocks.IRON_BARS));
                c.set(bx, boom + 3, z, stripe);
            }
        }
        c.fill(x + 6, boom + 1, 26, x + 7, boom + 2, 26, s(Blocks.IRON_BARS));
        // Caballete (A) y tensores diagonales.
        for (int bx : new int[]{x + 4, x + 9}) {
            c.line(bx, boom + 3, 68, bx, apex, 80, CRANE.pick(0.1f));
            c.line(bx, boom + 3, 91, bx, apex, 80, CRANE.pick(0.1f));
            c.line(bx, apex, 80, bx + (bx == x + 4 ? 1 : -1), boom + 3, 28, s(Blocks.IRON_BARS));
            c.line(bx, apex, 80, bx + (bx == x + 4 ? 1 : -1), boom + 3, 48, s(Blocks.IRON_BARS));
            c.line(bx, apex, 80, bx + (bx == x + 4 ? 1 : -1), boom + 3, 104, s(Blocks.IRON_BARS));
        }
        c.fill(x + 4, apex, 79, x + 9, apex, 81, CRANE);
        c.set(x + 6, apex + 1, 80, s(Blocks.LIGHTNING_ROD));
        c.set(x + 7, apex + 1, 80, s(Blocks.REDSTONE_LAMP));
        // Sala de máquinas sobre el pórtico (lado de tierra).
        c.fill(x + 3, boom + 4, 92, x + 10, boom + 8, 100, WHITE);
        c.fill(x + 3, boom + 6, 92, x + 10, boom + 6, 100, s(Blocks.LIGHT_BLUE_CONCRETE));
        // Cabina del operador colgada bajo la pluma, con botín de francotirador.
        int zc = 36 + (index % 3) * 6, cy = boom - 4;
        c.fill(x + 5, cy, zc, x + 8, boom - 1, zc + 3, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
        c.fill(x + 5, cy, zc, x + 8, cy, zc + 3, s(Blocks.IRON_BLOCK));
        c.fill(x + 5, boom - 1, zc, x + 8, boom - 1, zc + 3, s(Blocks.WHITE_CONCRETE));
        c.air(x + 6, cy + 1, zc + 1, x + 7, boom - 2, zc + 2);
        c.container(x + 7, cy + 1, zc + 2, facing(Blocks.CHEST, Direction.WEST), loot("port_tactical"));
        c.set(x + 6, boom, zc + 1, AIR);
        c.set(x + 6, boom - 1, zc + 1, AIR);
        Props.ladder(c, x + 6, cy + 1, boom, zc + 1, Direction.EAST);
        c.set(x + 5, boom, zc + 1, CRANE.pick(0.1f));
        // Escala por la pata del lado de tierra hasta el pórtico.
        Props.ladder(c, x + 1, G, top, 89, Direction.SOUTH);
        c.fill(x, top + 1, 89, x + 4, top + 1, 89, s(Blocks.IRON_BARS));
        c.set(x + 1, top + 1, 89, AIR);
    }

    // ================================================================== patio de contenedores

    private static final int[] BANDS = {100, 126, 152, 178};
    private static final int[] BLOCKS_X = {50, 112, 174};

    private static void yard(Canvas c) {
        for (int bi = 0; bi < BANDS.length; bi++) {
            for (int xi = 0; xi < BLOCKS_X.length; xi++) {
                int bx = BLOCKS_X[xi], bz = BANDS[bi];
                if (!c.touches(bx - 3, bz - 3, bx + 58, bz + 20)) continue;
                boolean maze = xi == 0 && bi <= 1;
                if (maze) maze(c, bx, bz, bi);
                else stacks(c, bx, bz);
                if (!maze && (bi + xi) % 2 == 0) rtg(c, bx + 26, bz, bz + 17);
            }
        }
    }

    /** Bloque normal: 4 × 6 pilas de 1 a 5 alturas (con algún hueco). */
    private static void stacks(Canvas c, int bx, int bz) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                int x = bx + i * 14, z = bz + j * 3;
                float r = c.noise(x, 1, z);
                if (r < 0.08f) continue;
                int tiers = 1 + (int) (c.noise(x, 2, z) * 5);
                for (int t = 0; t < tiers; t++) {
                    Props.containerX(c, x, G + t * 3, z, 13, Props.Color.pick(c.noise(x, t + 5, z)), Props.Kind.CLOSED);
                }
            }
        }
    }

    /**
     * Laberinto de combate cercano: contenedores forzados o reventados unidos por dentro con tablones
     * y escalas; esconden refugios, talleres, almacenes de botín y trampas de cable.
     */
    private static void maze(Canvas c, int bx, int bz, int band) {
        String[] loots = {"port_medical", "port_textile", "port_electronics", "port_commercial"};
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                int x = bx + i * 14, z = bz + j * 3;
                float r = c.noise(x, 11, z);
                if (r < 0.12f) continue; // callejón
                int tiers = r < 0.7f ? 1 : 2;
                for (int t = 0; t < tiers; t++) {
                    Props.Kind kind = c.noise(x, t, z + 7) < 0.55f ? Props.Kind.BREACHED : Props.Kind.OPEN;
                    Props.containerX(c, x, G + t * 3, z, 13, Props.Color.pick(c.noise(x, t + 3, z)), kind);
                }
                int room = Math.floorMod((int) (c.noise(x, 13, z) * 100) + band, 6);
                int y = G;
                switch (room) {
                    case 0 -> { // refugio escondido
                        c.bed(x + 2, y, z + 1, Blocks.GRAY_BED, Direction.EAST);
                        c.container(x + 6, y, z + 1, facing(Blocks.CHEST, Direction.WEST), loot(loots[(i + j) % loots.length]));
                        c.set(x + 8, y + 1, z + 1, hangingLantern(Blocks.LANTERN));
                    }
                    case 1 -> { // taller clandestino
                        c.set(x + 2, y, z + 1, s(Blocks.CRAFTING_TABLE));
                        c.set(x + 3, y, z + 1, s(Blocks.SMITHING_TABLE));
                        c.set(x + 4, y, z + 1, s(Blocks.ANVIL));
                        c.container(x + 5, y, z + 1, facing(Blocks.BARREL, Direction.UP), loot("port_industrial"));
                    }
                    case 2 -> { // almacén de mercancía
                        c.container(x + 3, y, z + 1, facing(Blocks.CHEST, Direction.WEST), loot(loots[(i * 3 + j) % loots.length]));
                        c.container(x + 9, y, z + 1, facing(Blocks.BARREL, Direction.UP), loot(loots[(i + j * 2) % loots.length]));
                    }
                    case 3 -> Props.tripwireTrap(c, x + 3, y, z + 1, x + 9); // trampa de cable
                    default -> {
                    }
                }
                // Pasarelas de tablones entre los techos de las pilas dobles y escalas de mano.
                if (tiers == 2) {
                    c.fill(x + 13, G + 5, z + 1, x + 13, G + 5, z + 1, s(Blocks.SPRUCE_PLANKS));
                    Props.ladder(c, x - 1, G, G + 5, z + 1, Direction.WEST);
                    c.set(x + 1, G + 5, z + 1, AIR);
                }
            }
        }
    }

    /** Grúa de patio RTG azul que cabalga sobre un bloque de contenedores. */
    private static void rtg(Canvas c, int x, int z0, int z1) {
        int za = z0 - 3, zb = z1 + 3, top = G + 18;
        if (!c.touches(x - 1, za, x + 8, zb)) return;
        for (int lx : new int[]{x, x + 7}) {
            for (int lz : new int[]{za, zb}) {
                c.fill(lx, G, lz, lx, top, lz, s(Blocks.BLUE_CONCRETE));
                c.set(lx, G, lz, s(Blocks.BLACK_CONCRETE));
            }
            c.fill(lx, top - 1, za, lx, top, zb, s(Blocks.BLUE_CONCRETE));
            c.fill(lx, G + 3, za, lx, G + 3, za, s(Blocks.YELLOW_CONCRETE));
        }
        for (int lz : new int[]{za, zb}) c.fill(x, top, lz, x + 7, top, lz, s(Blocks.BLUE_CONCRETE));
        int zm = (za + zb) / 2;
        c.fill(x + 2, top - 3, zm - 1, x + 5, top - 1, zm + 1, s(Blocks.WHITE_CONCRETE));
        c.fill(x + 2, top - 3, zm - 1, x + 2, top - 2, zm + 1, s(Blocks.LIGHT_BLUE_STAINED_GLASS));
        c.fill(x + 1, top + 1, zb - 4, x + 6, top + 3, zb, s(Blocks.BLUE_TERRACOTTA));
        Props.ladder(c, x + 1, G, top, zb, Direction.EAST);
    }

    // ================================================================== bobinas, camiones y equipos

    /** Zona de bobinas de acero negro y tuberías industriales apiladas (esquina inferior izquierda). */
    private static void coils(Canvas c) {
        if (!c.touches(2, 98, 44, 196)) return;
        for (int row = 0; row < 4; row++) {
            for (int k = 0; k < 6; k++) {
                int x = 4 + k * 6, z = 100 + row * 6;
                c.fill(x, G, z, x + 2, G, z + 3, slab(Blocks.SPRUCE_SLAB, false));
                for (int dz = 0; dz < 3; dz++) {
                    for (int dy = 0; dy < 3; dy++) {
                        if ((dz == 0 || dz == 2) && (dy == 0 || dy == 2)) continue;
                        BlockState st = dz == 1 && dy == 1 ? s(Blocks.COAL_BLOCK) : s(Blocks.BLACK_CONCRETE);
                        c.fill(x, G + 1 + dy, z + dz, x + 2, G + 1 + dy, z + dz, st);
                    }
                }
            }
        }
        BlockState pipe = s(Blocks.POLISHED_BASALT).setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
        for (int stack = 0; stack < 3; stack++) {
            int x0 = 4 + stack * 13;
            for (int layer = 0; layer < 4; layer++) {
                for (int k = 0; k < 8 - layer * 2; k++) {
                    c.fill(x0 + layer + k, G + layer, 128, x0 + layer + k, G + layer, 146, pipe);
                }
            }
        }
        for (int i = 0; i < 5; i++) c.container(6 + i * 7, G, 150, facing(Blocks.BARREL, Direction.UP), loot("port_industrial"));
        // Carretilla elevadora.
        c.fill(30, G, 154, 32, G + 1, 156, s(Blocks.YELLOW_CONCRETE));
        c.fill(30, G + 2, 154, 30, G + 4, 156, s(Blocks.IRON_BARS));
        c.fill(33, G, 155, 34, G, 155, s(Blocks.IRON_BARS));
        // Oficina de la terminal.
        c.fill(4, G, 170, 40, G + 5, 196, WHITE);
        c.air(5, G, 171, 39, G + 4, 195);
        c.fill(4, G + 2, 172, 4, G + 3, 194, s(Blocks.GLASS_PANE));
        c.fill(5, G + 5, 171, 39, G + 5, 195, s(Blocks.GRAY_CONCRETE));
        c.door(40, G, 182, Blocks.SPRUCE_DOOR, Direction.WEST, DoorHingeSide.LEFT);
        for (int z = 173; z <= 193; z += 5) {
            c.set(10, G, z, s(Blocks.SPRUCE_PLANKS));
            c.set(11, G, z, s(Blocks.SPRUCE_PLANKS));
            c.set(10, G + 1, z, s(Blocks.REDSTONE_LAMP));
            c.set(12, G, z, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
        }
        c.container(36, G, 173, facing(Blocks.CHEST, Direction.WEST), loot("port_military"));
        c.container(36, G, 176, facing(Blocks.CHEST, Direction.WEST), loot("port_industrial"));
        c.crate(36, G, 190, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
        c.itemFrame(20, G + 2, 195, Direction.SOUTH, new ItemStack(Items.MAP));
        c.set(25, G + 4, 183, hangingLantern(Blocks.LANTERN));
    }

    private static void apronProps(Canvas c) {
        Props.truck(c, 56, G, 72, Direction.EAST, Props.Color.RED, Props.Color.WHITE, "port_industrial");
        Props.truck(c, 130, G, 82, Direction.EAST, Props.Color.BLUE, null, "port_industrial");
        Props.truck(c, 186, G, 74, Direction.WEST, Props.Color.WHITE, Props.Color.RUST, null);
        Props.truck(c, 120, G, 121, Direction.EAST, Props.Color.GREEN, Props.Color.BLUE, "port_industrial");
        Props.truck(c, 200, G, 146, Direction.WEST, Props.Color.ORANGE, Props.Color.WHITE, null);
        Props.truck(c, 238, G, 170, Direction.NORTH, Props.Color.WHITE, Props.Color.RED, "port_industrial");
        // Estructuras azules de celosía (balancines de contenedor) en el delantal.
        for (int k = 0; k < 3; k++) {
            int x = 8 + k * 9;
            if (!c.touches(x, 72, x + 3, 86)) continue;
            c.fill(x, G, 72, x + 3, G, 86, s(Blocks.LIGHT_BLUE_CONCRETE));
            c.air(x + 1, G, 73, x + 2, G, 85);
            for (int z = 74; z <= 84; z += 5) c.fill(x, G, z, x + 3, G, z, s(Blocks.IRON_BARS));
            c.fill(x, G + 1, 72, x, G + 2, 72, s(Blocks.IRON_BARS));
            c.fill(x + 3, G + 1, 86, x + 3, G + 2, 86, s(Blocks.IRON_BARS));
        }
        // Grúa móvil secundaria.
        if (c.touches(232, 98, 254, 116)) {
            c.fill(236, G, 100, 246, G + 1, 106, s(Blocks.YELLOW_CONCRETE));
            c.fill(236, G, 100, 246, G, 100, s(Blocks.BLACK_CONCRETE));
            c.fill(236, G, 106, 246, G, 106, s(Blocks.BLACK_CONCRETE));
            c.fill(243, G + 2, 101, 246, G + 4, 105, s(Blocks.YELLOW_TERRACOTTA));
            c.fill(244, G + 3, 101, 246, G + 3, 101, s(Blocks.GLASS_PANE));
            c.line(240, G + 2, 103, 240, G + 22, 116, s(Blocks.YELLOW_CONCRETE));
            c.line(240, G + 22, 116, 240, G + 10, 116, s(Blocks.CHAIN));
            c.container(245, G + 2, 103, facing(Blocks.CHEST, Direction.WEST), loot("port_tactical"));
        }
        // Torres de iluminación.
        Props.lightMast(c, 108, G, 122, 22);
        Props.lightMast(c, 170, G, 148, 22);
        Props.lightMast(c, 232, G, 174, 22);
        Props.lightMast(c, 46, G, 200, 22);
        Props.lightMast(c, 250, G, 92, 20);
        // Barreras de hormigón en el acceso a la zona de grúas.
        Props.jersey(c, 2, G, 96, true, 40);
    }

    private static void backFence(Canvas c) {
        if (!c.touches(0, 206, W - 1, 215)) return;
        for (int x = 0; x < W; x++) {
            if (x >= 236 && x <= 246) continue; // portón abierto
            c.set(x, G, 214, x % 6 == 0 ? s(Blocks.ANDESITE_WALL) : s(Blocks.IRON_BARS));
            c.set(x, G + 1, 214, x % 6 == 0 ? s(Blocks.ANDESITE_WALL) : s(Blocks.IRON_BARS));
            c.set(x, G + 2, 214, x % 6 == 0 ? s(Blocks.ANDESITE_WALL) : s(Blocks.IRON_BARS));
            if (c.chance(x, G + 3, 214, 0.5f)) c.set(x, G + 3, 214, s(Blocks.COBWEB));
        }
        // Barrera basculante y garita.
        c.fill(236, G, 210, 236, G + 1, 210, s(Blocks.BLACK_CONCRETE));
        c.fill(237, G + 1, 210, 245, G + 1, 210, s(Blocks.BIRCH_FENCE));
        for (int x = 238; x <= 245; x += 2) c.set(x, G + 1, 210, s(Blocks.RED_CONCRETE));
        c.fill(248, G, 205, 254, G + 4, 211, WHITE);
        c.air(249, G, 206, 253, G + 3, 210);
        c.fill(248, G + 2, 206, 248, G + 3, 210, s(Blocks.GLASS_PANE));
        c.fill(249, G + 2, 205, 253, G + 3, 205, s(Blocks.GLASS_PANE));
        c.door(250, G, 211, Blocks.SPRUCE_DOOR, Direction.SOUTH, DoorHingeSide.LEFT);
        c.container(253, G, 207, facing(Blocks.CHEST, Direction.WEST), loot("port_industrial"));
        c.set(251, G + 3, 208, hangingLantern(Blocks.LANTERN));
    }
}
