package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import static com.igneko.aidirector.world.structure.Canvas.facing;
import static com.igneko.aidirector.world.structure.Canvas.hangingLantern;
import static com.igneko.aidirector.world.structure.Canvas.log;
import static com.igneko.aidirector.world.structure.Canvas.slab;
import static com.igneko.aidirector.world.structure.Canvas.stairs;
import static com.igneko.aidirector.world.structure.Canvas.trapdoor;
import static com.igneko.aidirector.world.structure.Props.AIR;
import static com.igneko.aidirector.world.structure.Props.loot;
import static com.igneko.aidirector.world.structure.Props.s;

/**
 * Base de operaciones avanzada en el desierto (130 × 110).
 *
 * <pre>
 *  z=109 ┌── talud + doble fila de bastiones de gaviones con concertina ────────┐
 *        │ lanzacohetes  antena  torre de radio  hangar semicilíndrico  tiendas │
 *        │                                                                     │
 *        │                   cuartel de hormigón en ruinas      helipuerto      │
 *        │ tienda TOC │ barracón │ enfermería      ═══ camino ═══               │
 *        │                                            garita ▣  │ barrera      │
 *  z=0   └──────────────────────────────────────────── acceso SE ─┘───────────┘
 * </pre>
 */
final class FobBuilder {

    private FobBuilder() {}

    static final int W = 130, D = 110, H = 44, G = 8;

    private static final Palette SAND = Palette.weighted(Blocks.SAND, 5, Blocks.COARSE_DIRT, 1, Blocks.GRAVEL, 1);
    private static final Palette BERM = Palette.weighted(Blocks.SAND, 3, Blocks.SANDSTONE, 2, Blocks.SMOOTH_SANDSTONE, 1);
    private static final Palette HESCO = Palette.weighted(Blocks.WHITE_TERRACOTTA, 3, Blocks.SMOOTH_SANDSTONE, 2, Blocks.SANDSTONE, 1);
    private static final Palette ROAD = Palette.weighted(Blocks.GRAVEL, 2, Blocks.COARSE_DIRT, 2, Blocks.DIRT_PATH, 1);
    private static final Palette CANVAS = Palette.weighted(Blocks.SMOOTH_SANDSTONE, 4, Blocks.BONE_BLOCK, 1, Blocks.SANDSTONE, 1);
    private static final Palette CONCRETE = Palette.weighted(Blocks.WHITE_CONCRETE, 3, Blocks.LIGHT_GRAY_CONCRETE, 2, Blocks.SMOOTH_STONE, 1);
    private static final Palette RUBBLE = Palette.weighted(Blocks.COBBLESTONE_SLAB, 3, Blocks.GRAVEL, 2,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER, 2, Blocks.COBBLESTONE_STAIRS, 1);

    private static final int GATE_X0 = 100, GATE_X1 = 109;

    static void build(Canvas c) {
        ground(c);
        perimeter(c);
        gate(c);
        for (int i = 0; i < 3; i++) modularTent(c, 18 + i * 14, i);
        pergolas(c);
        barracks(c);
        mlrs(c);
        antenna(c, 32, 90, 18);
        radioTower(c);
        quonset(c);
        smallTent(c, 78);
        smallTent(c, 86);
        eastYard(c);
    }

    /** Distancia al borde de la huella (0 en el borde). */
    private static int edge(int x, int z) {
        return Math.min(Math.min(x, W - 1 - x), Math.min(z, D - 1 - z));
    }

    private static boolean gateCut(int x, int z) {
        return x >= GATE_X0 && x <= GATE_X1 && z <= 15;
    }

    // ================================================================== terreno y perímetro

    private static void ground(Canvas c) {
        c.columns(0, 0, W - 1, D - 1, (x, z) -> {
            c.fill(x, G - 3, z, x, G - 1, z, s(Blocks.SANDSTONE));
            c.foundation(x, G - 4, z, s(Blocks.SANDSTONE));
            c.fill(x, G + 1, z, x, H - 1, z, AIR);
            boolean road = (x >= 101 && x <= 108 && z <= 47) || (x >= 58 && x <= 108 && z >= 44 && z <= 47);
            c.set(x, G, z, road ? ROAD : SAND);
            if (!road && edge(x, z) >= 16 && c.chance(x, G + 1, z, 0.012f)) c.set(x, G + 1, z, s(Blocks.DEAD_BUSH));
            if (edge(x, z) < 2) {
                if (c.chance(x, 3, z, 0.02f)) c.set(x, G + 1, z, s(Blocks.DEAD_BUSH));
                else if (c.chance(x, 4, z, 0.006f)) c.fill(x, G + 1, z, x, G + 2, z, s(Blocks.CACTUS));
            }
        });
    }

    /** Talud de arena de 3 bloques coronado por una doble fila escalonada de gaviones con concertina. */
    private static void perimeter(Canvas c) {
        c.columns(0, 0, W - 1, D - 1, (x, z) -> {
            int d = edge(x, z);
            if (d < 2 || d > 15 || gateCut(x, z)) return;
            int h = d <= 4 ? d - 1 : d <= 13 ? 3 : 16 - d;
            c.fill(x, G, z, x, G + h - 1, z, BERM);
            c.set(x, G + h, z, s(Blocks.SAND));
            // Lado del anillo: dirección hacia fuera y coordenada a lo largo del muro.
            Direction out;
            int along;
            if (d == z) { out = Direction.SOUTH; along = x; }
            else if (d == D - 1 - z) { out = Direction.NORTH; along = x; }
            else if (d == x) { out = Direction.WEST; along = z; }
            else { out = Direction.EAST; along = z; }
            int top = G + 3;
            if (d >= 6 && d <= 8) {
                boolean corner = along % 3 == 0 && (d == 6 || d == 8);
                c.fill(x, top + 1, z, x, top + 3, z, corner ? log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y) : HESCO.pick(c.noise(x, top, z)));
                if (d == 7) {
                    float r = c.noise(x, top + 4, z);
                    c.set(x, top + 4, z, r < 0.4f ? s(Blocks.COBWEB) : r < 0.75f ? s(Blocks.IRON_BARS) : s(Blocks.DEAD_BUSH));
                    if (c.chance(x, top + 5, z, 0.3f)) c.set(x, top + 5, z, s(Blocks.COBWEB));
                }
            } else if (d == 5) {
                // Malla y geotextil de la cara exterior.
                for (int y = top + 1; y <= top + 3; y++) {
                    float r = c.noise(x, y, z);
                    if (r < 0.35f) c.set(x, y, z, trapdoor(Blocks.BIRCH_TRAPDOOR, out, y == top + 3, true));
                    else if (r < 0.55f) c.set(x, y, z, trapdoor(Blocks.OAK_TRAPDOOR, out, y == top + 3, true));
                }
            } else if (d >= 10 && d <= 11) {
                boolean corner = along % 2 == 0;
                c.fill(x, top + 1, z, x, top + 2, z, corner ? log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y) : HESCO.pick(c.noise(x, top + 1, z)));
            }
        });
        // Nidos de ametralladora de sacos terreros en tres esquinas del talud.
        for (int[] n : new int[][]{{12, 12}, {12, 96}, {116, 96}}) {
            int x = n[0], z = n[1];
            if (!c.touches(x - 1, z - 1, x + 2, z + 2)) continue;
            for (int[] p : new int[][]{{x - 1, z - 1}, {x, z - 1}, {x + 1, z - 1}, {x - 1, z}, {x - 1, z + 1}}) {
                c.set(p[0], G + 4, p[1], s(Blocks.SANDSTONE_WALL));
            }
            c.container(x + 1, G + 4, z + 1, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_guard"));
        }
    }

    /** Acceso sureste: chicane de barreras, barrera basculante a rayas y garita blindada. */
    private static void gate(Canvas c) {
        if (!c.touches(GATE_X0 - 2, 0, 118, 24)) return;
        Props.jersey(c, 100, G + 1, 6, true, 4);
        Props.jersey(c, 106, G + 1, 10, true, 4);
        c.fill(GATE_X0 - 1, G + 1, 15, GATE_X0 - 1, G + 2, 15, s(Blocks.BLACK_CONCRETE));
        c.set(GATE_X0 - 1, G + 3, 15, Props.LEVER);
        for (int x = GATE_X0; x <= GATE_X1; x++) {
            c.set(x, G + 2, 15, (x / 2) % 2 == 0 ? s(Blocks.BIRCH_FENCE) : s(Blocks.POLISHED_BLACKSTONE_WALL));
        }
        // Garita 5 × 5 × 4: muros blancos, techo marrón plano, cristal en tres caras.
        int x0 = 111, z0 = 18;
        c.fill(x0, G + 1, z0, x0 + 4, G + 3, z0 + 4, s(Blocks.WHITE_CONCRETE));
        c.fill(x0, G + 4, z0, x0 + 4, G + 4, z0 + 4, s(Blocks.BROWN_CONCRETE));
        c.air(x0 + 1, G + 1, z0 + 1, x0 + 3, G + 3, z0 + 3);
        c.fill(x0, G + 2, z0 + 1, x0, G + 3, z0 + 3, s(Blocks.GLASS_PANE));
        c.fill(x0 + 1, G + 2, z0, x0 + 3, G + 3, z0, s(Blocks.GLASS_PANE));
        c.fill(x0 + 1, G + 2, z0 + 4, x0 + 3, G + 3, z0 + 4, s(Blocks.GLASS_PANE));
        c.door(x0 + 4, G + 1, z0 + 2, Blocks.DARK_OAK_DOOR, Direction.WEST, DoorHingeSide.LEFT);
        c.container(x0 + 3, G + 1, z0 + 1, facing(Blocks.CHEST, Direction.NORTH), loot("fob_guard"));
        c.set(x0 + 1, G + 1, z0 + 2, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        c.set(x0 + 2, G + 3, z0 + 2, hangingLantern(Blocks.LANTERN));
    }

    // ================================================================== tiendas modulares

    /** Tienda abovedada de 10 × 18 × 6 con arcos, vestíbulo, paneles solares y aire acondicionado. */
    private static void modularTent(Canvas c, int x0, int kind) {
        int z0 = 24, z1 = 41;
        if (!c.touches(x0 - 3, 19, x0 + 12, z1 + 1)) return;
        double xc = x0 + 4.5;
        c.columns(x0, z0, x0 + 9, z1, (x, z) -> {
            double dx = Math.abs(x - xc);
            int top = G + (int) Math.round(vault(dx, 6, 5.2));
            int low = dx + 1 > 5.2 ? G + 1 : G + (int) Math.round(vault(dx + 1, 6, 5.2));
            boolean rib = (z - z0) % 4 == 0;
            boolean end = z == z0 || z == z1;
            c.set(x, G, z, s(Blocks.SMOOTH_SANDSTONE));
            for (int y = G + 1; y <= top; y++) {
                if (end || y >= low) c.set(x, y, z, rib ? s(Blocks.BONE_BLOCK) : CANVAS.pick(c.noise(x, y, z)));
            }
            if (!rib && !end && dx < 1 && z > z0 + 1 && z < z1 - 1) {
                c.set(x, top + 1, z, s(Blocks.DAYLIGHT_DETECTOR).setValue(BlockStateProperties.INVERTED, true));
            }
        });
        // Faldón de escaleras de abedul y arcos que sobresalen en la base.
        for (int z = z0; z <= z1; z++) {
            boolean rib = (z - z0) % 4 == 0;
            c.set(x0 - 1, G + 1, z, rib ? s(Blocks.BONE_BLOCK) : stairs(Blocks.BIRCH_STAIRS, Direction.EAST));
            c.set(x0 + 10, G + 1, z, rib ? s(Blocks.BONE_BLOCK) : stairs(Blocks.BIRCH_STAIRS, Direction.WEST));
        }
        // Vestíbulo de entrada.
        double vc = x0 + 4.5;
        c.columns(x0 + 2, z0 - 4, x0 + 7, z0 - 1, (x, z) -> {
            double dx = Math.abs(x - vc);
            int top = G + (int) Math.round(vault(dx, 4, 3.2));
            int low = dx + 1 > 3.2 ? G + 1 : G + (int) Math.round(vault(dx + 1, 4, 3.2));
            c.set(x, G, z, s(Blocks.SMOOTH_SANDSTONE));
            for (int y = G + 1; y <= top; y++) if (z == z0 - 4 || y >= low) c.set(x, y, z, CANVAS.pick(c.noise(x, y, z)));
        });
        c.air(x0 + 4, G + 1, z0 - 4, x0 + 5, G + 2, z0);
        c.air(x0 + 4, G + 3, z0, x0 + 5, G + 3, z0);
        // Aire acondicionado adosado al costado.
        c.fill(x0 - 2, G + 1, 31, x0 - 1, G + 2, 32, s(Blocks.LIGHT_GRAY_CONCRETE));
        c.set(x0 - 2, G + 2, 31, s(Blocks.IRON_BARS));
        c.set(x0 - 2, G + 2, 32, s(Blocks.IRON_BARS));
        c.set(x0 + 4, G + 5, z1 - 2, hangingLantern(Blocks.LANTERN));
        c.set(x0 + 4, G + 5, z0 + 4, hangingLantern(Blocks.LANTERN));
        switch (kind) {
            case 0 -> toc(c, x0);
            case 1 -> troops(c, x0);
            default -> infirmary(c, x0);
        }
    }

    private static double vault(double d, double h, double r) {
        return d >= r ? 0 : h * Math.sqrt(1 - (d / r) * (d / r));
    }

    /** Tienda 1: centro de operaciones tácticas. */
    private static void toc(Canvas c, int x0) {
        for (int x = x0 + 3; x <= x0 + 6; x++) {
            for (int z = 30; z <= 34; z += 2) {
                c.set(x, G + 1, z, s(Blocks.DARK_OAK_PLANKS));
                if ((x + z) % 2 == 0) c.glowFrame(x, G + 2, z, Direction.UP, new ItemStack(Items.MAP));
            }
        }
        for (int z = 30; z <= 34; z += 2) {
            c.set(x0 + 2, G + 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
            c.set(x0 + 7, G + 1, z, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        }
        // Pantallas en el fondo, radios y cajas verdes de armamento.
        c.fill(x0 + 3, G + 2, 40, x0 + 6, G + 3, 40, s(Blocks.BLACK_CONCRETE));
        for (int x = x0 + 3; x <= x0 + 6; x++) c.glowFrame(x, G + 2, 39, Direction.SOUTH, new ItemStack(Items.MAP));
        c.set(x0 + 1, G + 1, 27, s(Blocks.NOTE_BLOCK));
        c.set(x0 + 1, G + 1, 28, s(Blocks.JUKEBOX));
        c.set(x0 + 1, G + 2, 27, Props.LEVER);
        c.crate(x0 + 8, G + 1, 27, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, false);
        c.set(x0 + 8, G + 1, 28, s(Blocks.GREEN_CONCRETE));
        c.container(x0 + 8, G + 1, 37, facing(Blocks.CHEST, Direction.WEST), loot("fob_tactical"));
    }

    /** Tienda 2: barracón de tropa con literas, taquillas y equipo del desierto. */
    private static void troops(Canvas c, int x0) {
        for (int z = 26; z <= 38; z += 3) {
            c.bed(x0 + 2, G + 1, z, Blocks.BROWN_BED, Direction.EAST);
            c.bed(x0 + 2, G + 3, z, Blocks.GREEN_BED, Direction.EAST);
            c.bed(x0 + 7, G + 1, z, Blocks.BROWN_BED, Direction.WEST);
            c.set(x0 + 2, G + 2, z, s(Blocks.SPRUCE_FENCE));
            c.set(x0 + 7, G + 2, z, s(Blocks.SPRUCE_FENCE));
        }
        for (int x : new int[]{x0 + 2, x0 + 4, x0 + 6}) {
            c.container(x, G + 1, 40, facing(Blocks.BARREL, Direction.SOUTH), loot("fob_tactical"));
            c.door(x, G + 1, 39, Blocks.IRON_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        }
        ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE), legs = new ItemStack(Items.LEATHER_LEGGINGS);
        ((DyeableLeatherItem) chest.getItem()).setColor(chest, 0xC2A36B);
        ((DyeableLeatherItem) legs.getItem()).setColor(legs, 0xA88B5A);
        c.armorStand(x0 + 5, G + 1, 27, Direction.WEST, new ItemStack(Items.CHAINMAIL_HELMET), chest, legs);
    }

    /** Tienda 3: enfermería de campaña. */
    private static void infirmary(Canvas c, int x0) {
        for (int z = 26; z <= 36; z += 5) {
            c.bed(x0 + 2, G + 1, z, Blocks.WHITE_BED, Direction.EAST);
            c.set(x0 + 4, G + 1, z + 1, s(Blocks.BREWING_STAND));
        }
        c.set(x0 + 7, G + 1, 27, s(Blocks.CAULDRON));
        c.set(x0 + 7, G + 1, 29, s(Blocks.WATER_CAULDRON).setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        c.crate(x0 + 7, G + 1, 32, Direction.WEST, SupplyCrateBlock.Kind.MEDICAL, false);
        c.container(x0 + 7, G + 1, 35, facing(Blocks.CHEST, Direction.WEST), loot("fob_tactical"));
        c.container(x0 + 7, G + 1, 38, facing(Blocks.CHEST, Direction.WEST), loot("dome_medical"));
    }

    /** Pérgolas con redes de camuflaje desértico entre las tiendas. */
    private static void pergolas(Canvas c) {
        for (int x0 : new int[]{28, 42}) {
            if (!c.touches(x0, 26, x0 + 3, 39)) continue;
            for (int[] p : new int[][]{{x0, 26}, {x0 + 3, 26}, {x0, 39}, {x0 + 3, 39}}) {
                c.fill(p[0], G + 1, p[1], p[0], G + 3, p[1], s(Blocks.OAK_FENCE));
            }
            c.columns(x0, 26, x0 + 3, 39, (x, z) -> {
                float r = c.noise(x, G + 4, z);
                if (r < 0.55f) c.set(x, G + 4, z, trapdoor(Blocks.OAK_TRAPDOOR, Direction.NORTH, true, false));
                else if (r < 0.85f) c.set(x, G + 4, z, Canvas.leaves(Blocks.ACACIA_LEAVES));
            });
            c.container(x0 + 1, G + 1, 32, facing(Blocks.BARREL, Direction.UP), loot("safe_room_rations"));
        }
    }

    // ================================================================== cuartel en ruinas

    /** Cuartel de hormigón de 28 × 9 × 6 con el 70 % del techo hundido y vigas en X a la vista. */
    private static void barracks(Canvas c) {
        int x0 = 52, x1 = 79, z0 = 50, z1 = 58, top = G + 6;
        if (!c.touches(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) return;
        c.fill(x0, G, z0, x1, G, z1, s(Blocks.SMOOTH_STONE));
        c.fill(x0, G + 1, z0, x1, G + 5, z1, CONCRETE);
        c.air(x0 + 1, G + 1, z0 + 1, x1 - 1, G + 5, z1 - 1);
        int[] walls = {59, 66, 73};
        for (int w : walls) c.fill(w, G + 1, z0 + 1, w, G + 5, z1 - 1, CONCRETE);
        // Coronación, vigas en X y lo poco que queda de la losa.
        c.fill(x0, top, z0, x1, top, z0, s(Blocks.LIGHT_GRAY_CONCRETE));
        c.fill(x0, top, z1, x1, top, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
        int[] edges = {x0, 59, 66, 73, x1};
        for (int i = 0; i < edges.length - 1; i++) {
            int xa = edges[i], xb = edges[i + 1];
            c.fill(xb, top, z0, xb, top, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
            c.line(xa, top, z0, xb, top, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
            c.line(xa, top, z1, xb, top, z0, s(Blocks.LIGHT_GRAY_CONCRETE));
        }
        c.fill(x0, top, z0, x0, top, z1, s(Blocks.LIGHT_GRAY_CONCRETE));
        c.columns(x0 + 1, z0 + 1, x1 - 1, z1 - 1, (x, z) -> {
            if (c.get(x, top, z).isAir() && c.chance(x, top, z, 0.3f)) c.set(x, top, z, slab(Blocks.SMOOTH_STONE_SLAB, false));
            // Escombros por el suelo, sobre todo bajo los huecos del techo.
            if (c.get(x, G + 1, z).isAir()) {
                float r = c.noise(x, G + 1, z);
                if (r < 0.22f) c.set(x, G + 1, z, RUBBLE.pick(c.noise(z, x, G)));
                else if (r < 0.27f) c.set(x, G + 1, z, Props.BUTTON);
            }
        });
        // Muros interiores perforados y boquetes exteriores.
        for (int w : walls) {
            int hz = z0 + 2 + (int) (c.noise(w, 1, z0) * 4);
            c.air(w, G + 1, hz, w, G + 2, hz + 1);
        }
        c.air(63, G + 1, z0, 65, G + 3, z0);
        c.air(75, G + 2, z1, 76, G + 3, z1);
        // Puertas en los testeros y ventanas con sacos terreros (nidos de ametralladora).
        c.air(x0, G + 1, 54, x0, G + 2, 54);
        c.air(x1, G + 1, 54, x1, G + 2, 54);
        for (int x = x0 + 3; x < x1; x += 4) {
            for (int z : new int[]{z0, z1}) {
                if (c.get(x, G + 2, z).isAir()) continue;
                c.air(x, G + 2, z, x, G + 3, z);
                if ((x / 4) % 2 == 0) c.set(x, G + 2, z, slab(Blocks.SANDSTONE_SLAB, false));
            }
        }
        c.container(56, G + 1, z1 - 1, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_assault"));
        c.container(70, G + 1, z1 - 1, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_assault"));
        // Cajas fuertes medio enterradas bajo el techo caído.
        for (int x : new int[]{62, 76}) {
            c.container(x, G, 55, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_assault"));
            c.set(x, G + 1, 55, slab(Blocks.COBBLESTONE_SLAB, false));
        }
        c.crate(68, G + 1, 52, Direction.NORTH, SupplyCrateBlock.Kind.THROWABLES, false);
        // Redes de camuflaje desgarradas colgando de la fachada.
        for (int x = x0; x <= x1; x++) {
            for (int y = G + 2; y <= G + 5; y++) {
                if (!c.get(x, y, z0).isAir() && c.chance(x, y, z0 - 1, 0.35f)) {
                    c.set(x, y, z0 - 1, s(Blocks.VINE).setValue(BlockStateProperties.NORTH, true));
                }
                if (!c.get(x, y, z1).isAir() && c.chance(x, y, z1 + 1, 0.25f)) {
                    c.set(x, y, z1 + 1, Canvas.leaves(Blocks.SPRUCE_LEAVES));
                }
            }
        }
    }

    // ================================================================== sector norte

    /** Camión lanzacohetes múltiple junto al muro del noroeste, con la jaula apuntando a la bahía. */
    private static void mlrs(Canvas c) {
        int x0 = 17, x1 = 27, z0 = 86, z1 = 90;
        if (!c.touches(x0 - 1, z0, x1 + 1, z1)) return;
        for (int x : new int[]{18, 20, 22, 25, 27}) {
            c.set(x, G + 1, z0, s(Blocks.BLACK_CONCRETE));
            c.set(x, G + 1, z1, s(Blocks.BLACK_CONCRETE));
        }
        c.fill(x0, G + 2, z0, x1, G + 2, z1, s(Blocks.GREEN_TERRACOTTA));
        c.fill(24, G + 3, z0, x1, G + 5, z1, s(Blocks.SMOOTH_SANDSTONE));
        c.fill(24, G + 3, z0, x1, G + 3, z1, s(Blocks.GREEN_TERRACOTTA));
        c.fill(x1, G + 4, z0 + 1, x1, G + 4, z1 - 1, s(Blocks.GRAY_STAINED_GLASS));
        c.container(25, G + 4, 88, facing(Blocks.CHEST, Direction.EAST), loot("fob_artillery"));
        // Jaula lanzadora inclinada a 45° hacia el oeste.
        for (int k = 0; k <= 5; k++) {
            c.fill(22 - k, G + 3 + k, z0 + 1, 22 - k, G + 4 + k, z1 - 1, s(Blocks.GREEN_TERRACOTTA));
        }
        c.fill(17, G + 8, z0 + 1, 17, G + 9, z1 - 1, s(Blocks.BLACK_CONCRETE));
        c.fill(20, G + 3, 88, 20, G + 4, 88, s(Blocks.IRON_BARS));
    }

    /** Antena secundaria: mástil de celosía con pararrayos y baliza. */
    private static void antenna(Canvas c, int x, int z, int h) {
        if (!c.touches(x - 1, z - 1, x + 1, z + 1)) return;
        c.fill(x, G + 1, z, x, G + h, z, s(Blocks.IRON_BARS));
        c.set(x, G + h + 1, z, s(Blocks.RED_CONCRETE));
        c.set(x, G + h + 2, z, s(Blocks.LIGHTNING_ROD));
        for (int y = G + 4; y < G + h; y += 6) {
            c.set(x - 1, y, z, s(Blocks.END_ROD).setValue(BlockStateProperties.FACING, Direction.WEST));
            c.set(x + 1, y, z, s(Blocks.END_ROD).setValue(BlockStateProperties.FACING, Direction.EAST));
        }
    }

    /** Torre de comunicaciones de 32 bloques a franjas rojas y blancas con tres parábolas rojas. */
    private static void radioTower(Canvas c) {
        int x0 = 44, z0 = 82;
        if (!c.touches(x0 - 2, z0 - 2, x0 + 6, z0 + 6)) return;
        for (int y = G + 1; y <= G + 32; y++) {
            boolean upper = y > G + 16;
            int a = upper ? 1 : 0, b = upper ? 3 : 4;
            BlockState corner = ((y - G - 1) / 4) % 2 == 0 ? s(Blocks.BRICK_WALL) : s(Blocks.DIORITE_WALL);
            for (int[] p : new int[][]{{a, a}, {b, a}, {a, b}, {b, b}}) c.set(x0 + p[0], y, z0 + p[1], corner);
            if ((y - G) % 2 == 0) {
                for (int i = a + 1; i < b; i++) {
                    c.set(x0 + i, y, z0 + a, s(Blocks.IRON_BARS));
                    c.set(x0 + i, y, z0 + b, s(Blocks.IRON_BARS));
                    c.set(x0 + a, y, z0 + i, s(Blocks.IRON_BARS));
                    c.set(x0 + b, y, z0 + i, s(Blocks.IRON_BARS));
                }
            }
            c.set(x0 + 2, y, z0 + 2, s(Blocks.POLISHED_BLACKSTONE));
            c.set(x0 + 2, y, z0 + 1, Canvas.ladder(Direction.SOUTH));
        }
        c.fill(x0, G + 16, z0, x0 + 4, G + 16, z0 + 4, s(Blocks.POLISHED_BLACKSTONE_SLAB));
        c.set(x0 + 2, G + 16, z0 + 1, Canvas.ladder(Direction.SOUTH));
        c.fill(x0 + 1, G + 33, z0 + 1, x0 + 3, G + 33, z0 + 3, s(Blocks.POLISHED_BLACKSTONE_SLAB));
        c.set(x0 + 2, G + 33, z0 + 1, AIR);
        c.set(x0 + 2, G + 34, z0 + 2, s(Blocks.LIGHTNING_ROD));
        // Parábolas: oeste, norte y este.
        for (int y = G + 28; y <= G + 30; y++) {
            for (int i = 1; i <= 3; i++) {
                c.set(x0, y, z0 + i, s(Blocks.RED_TERRACOTTA));
                c.set(x0 + i, y, z0 + 4, s(Blocks.RED_TERRACOTTA));
                c.set(x0 + 4, y, z0 + i, s(Blocks.RED_TERRACOTTA));
            }
        }
        c.set(x0 - 1, G + 29, z0 + 2, s(Blocks.RED_CONCRETE));
        c.set(x0 + 2, G + 29, z0 + 5, s(Blocks.RED_CONCRETE));
        c.set(x0 + 5, G + 29, z0 + 2, s(Blocks.RED_CONCRETE));
    }

    /** Hangar semicilíndrico de chapa ondulada: taller de vehículos y polvorín de artillería. */
    private static void quonset(Canvas c) {
        int x0 = 56, x1 = 73, z0 = 80, z1 = 91;
        if (!c.touches(x0 - 1, z0 - 1, x1 + 1, z1 + 1)) return;
        double zc = 85.5;
        c.columns(x0, z0, x1, z1, (x, z) -> {
            double dz = Math.abs(z - zc);
            int top = G + (int) Math.round(vault(dz, 8, 6.2));
            int low = dz + 1 > 6.2 ? G + 1 : G + (int) Math.round(vault(dz + 1, 8, 6.2));
            boolean end = x == x0 || x == x1;
            c.set(x, G, z, s(Blocks.SMOOTH_STONE));
            BlockState skin = s(x % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE);
            for (int y = G + 1; y <= top; y++) if (end || y >= low) c.set(x, y, z, end ? s(Blocks.LIGHT_GRAY_CONCRETE) : skin);
        });
        c.air(x1, G + 1, 83, x1, G + 5, 88);
        // Vehículo en mantenimiento.
        Props.car(c, 60, G + 1, 84, Direction.EAST, Blocks.SMOOTH_SANDSTONE, true, "fob_tactical");
        // Misiles de repuesto en bastidores horizontales.
        for (int x : new int[]{57, 62, 67}) c.fill(x, G + 1, z1 - 1, x, G + 4, z1 - 1, s(Blocks.IRON_BARS));
        for (int shelf : new int[]{G + 1, G + 3}) {
            for (int start : new int[]{58, 63}) {
                for (int x = start; x < start + 3; x++) {
                    c.set(x, shelf, z1 - 1, s(Blocks.LIGHTNING_ROD).setValue(BlockStateProperties.FACING, Direction.EAST));
                }
                c.set(start + 3, shelf, z1 - 1, s(Blocks.END_ROD).setValue(BlockStateProperties.FACING, Direction.EAST));
            }
            if (shelf == G + 1) c.fill(58, G + 2, z1 - 1, 66, G + 2, z1 - 1, slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
        }
        // Polipasto de cadena con su carga.
        c.fill(66, G + 4, 85, 66, G + 7, 85, s(Blocks.CHAIN));
        c.set(66, G + 3, 85, s(Blocks.IRON_BLOCK));
        // Bidones de diésel tumbados.
        for (int x = 70; x <= 72; x++) {
            for (int z = 81; z <= 82; z++) c.set(x, G + 1, z, facing(Blocks.BARREL, Direction.EAST));
            c.set(x, G + 2, 81, facing(Blocks.BARREL, Direction.EAST));
        }
        c.container(72, G + 1, 83, facing(Blocks.BARREL, Direction.UP), loot("fob_artillery"));
        // Cuadro eléctrico principal en el testero oeste.
        for (int z = 84; z <= 87; z++) {
            c.set(x0, G + 2, z, s(Blocks.IRON_BLOCK));
            c.set(x0, G + 3, z, s(Blocks.REDSTONE_LAMP));
            c.set(x0 + 1, G + 2, z, s(Blocks.LEVER).setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        }
        // Suministros de operaciones especiales, bajo llave hasta superar el Crescendo.
        c.crate(68, G + 1, z1 - 1, Direction.SOUTH, SupplyCrateBlock.Kind.ARMORY, true);
        c.crate(69, G + 1, z1 - 1, Direction.SOUTH, SupplyCrateBlock.Kind.ARMORY, true);
        c.crate(70, G + 1, z1 - 1, Direction.SOUTH, SupplyCrateBlock.Kind.THROWABLES, true);
        c.container(71, G + 1, z1 - 1, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_artillery"));
        c.alarm(64, G + 1, 88, Direction.SOUTH, 120);
        c.set(64, G + 7, 86, hangingLantern(Blocks.LANTERN));
    }

    /** Tienda menor de dos aguas (5 × 7). */
    private static void smallTent(Canvas c, int x0) {
        int z0 = 82, z1 = 88;
        if (!c.touches(x0, z0, x0 + 4, z1)) return;
        for (int z = z0; z <= z1; z++) {
            c.set(x0, G + 1, z, stairs(Blocks.SANDSTONE_STAIRS, Direction.EAST));
            c.set(x0 + 1, G + 2, z, stairs(Blocks.SANDSTONE_STAIRS, Direction.EAST));
            c.set(x0 + 2, G + 3, z, slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
            c.set(x0 + 3, G + 2, z, stairs(Blocks.SANDSTONE_STAIRS, Direction.WEST));
            c.set(x0 + 4, G + 1, z, stairs(Blocks.SANDSTONE_STAIRS, Direction.WEST));
        }
        c.fill(x0 + 1, G + 1, z1, x0 + 3, G + 1, z1, s(Blocks.WHITE_WOOL));
        c.set(x0 + 2, G + 2, z1, s(Blocks.WHITE_WOOL));
        c.bed(x0 + 1, G + 1, z0 + 2, Blocks.GREEN_BED, Direction.EAST);
        c.container(x0 + 2, G + 1, z1 - 1, facing(Blocks.CHEST, Direction.SOUTH), loot("fob_guard"));
    }

    // ================================================================== patio este

    /** Helipuerto, depósitos de agua, generador, mástil de bandera, vehículos y torres de luz. */
    private static void eastYard(Canvas c) {
        if (c.touches(92, 54, 108, 70)) {
            c.columns(92, 54, 108, 70, (x, z) -> {
                double d = Math.hypot(x - 100, z - 62);
                if (d > 7.5) return;
                c.set(x, G, z, d > 6.5 ? s(Blocks.YELLOW_CONCRETE) : s(Blocks.LIGHT_GRAY_CONCRETE));
            });
            for (int z = 59; z <= 65; z++) {
                c.set(98, G, z, s(Blocks.WHITE_CONCRETE));
                c.set(102, G, z, s(Blocks.WHITE_CONCRETE));
            }
            c.fill(99, G, 62, 101, G, 62, s(Blocks.WHITE_CONCRETE));
        }
        for (int z : new int[]{29, 35}) {
            if (!c.touches(88, z, 92, z + 3)) continue;
            c.fill(89, G + 1, z, 91, G + 4, z + 2, s(Blocks.WHITE_CONCRETE));
            c.set(89, G + 4, z, AIR);
            c.set(91, G + 4, z + 2, AIR);
            c.set(90, G + 5, z + 1, s(Blocks.IRON_BARS));
        }
        c.set(95, G + 1, 30, facing(Blocks.BLAST_FURNACE, Direction.WEST));
        c.set(96, G + 1, 30, s(Blocks.IRON_BLOCK));
        c.set(96, G + 2, 30, s(Blocks.LIGHTNING_ROD));
        c.fill(96, G + 3, 30, 96, G + 3, 33, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z));
        c.fill(88, G + 1, 76, 88, G + 12, 76, s(Blocks.IRON_BARS));
        c.fill(89, G + 10, 76, 91, G + 12, 76, s(Blocks.GREEN_WOOL));
        c.fill(89, G + 11, 76, 91, G + 11, 76, s(Blocks.WHITE_WOOL));
        Props.car(c, 84, G + 1, 52, Direction.NORTH, Blocks.SMOOTH_SANDSTONE, false, "fob_tactical");
        Props.car(c, 88, G + 1, 52, Direction.NORTH, Blocks.SMOOTH_SANDSTONE, true, null);
        Props.lightMast(c, 60, G + 1, 70, 12);
        Props.lightMast(c, 110, G + 1, 80, 12);
        // Puesto de sacos terreros junto al camino.
        c.fill(96, G + 1, 40, 99, G + 1, 40, s(Blocks.SANDSTONE_WALL));
        c.fill(96, G + 1, 41, 96, G + 1, 42, s(Blocks.SANDSTONE_WALL));
        c.container(97, G + 1, 41, facing(Blocks.CHEST, Direction.NORTH), loot("fob_guard"));
    }
}
