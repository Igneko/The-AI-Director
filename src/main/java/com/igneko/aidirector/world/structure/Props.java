package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import org.jetbrains.annotations.Nullable;

import static com.igneko.aidirector.world.structure.Canvas.facing;
import static com.igneko.aidirector.world.structure.Canvas.hangingLantern;
import static com.igneko.aidirector.world.structure.Canvas.trapdoor;

/** Objetos que se repiten en las grandes zonas: contenedores, camiones, bolardos, torres de luz. */
final class Props {

    private Props() {}

    static ResourceLocation loot(String name) {
        return new ResourceLocation(TheAIDirector.MOD_ID, "chests/" + name);
    }

    static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ------------------------------------------------------------------ contenedores

    /** Colores de contenedor: chapa principal y nervio (las ondas de la chapa se alternan). */
    enum Color {
        WHITE(Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ),
        RUST(Blocks.ORANGE_TERRACOTTA, Blocks.TERRACOTTA),
        RED(Blocks.RED_CONCRETE, Blocks.RED_TERRACOTTA),
        GREEN(Blocks.GREEN_CONCRETE, Blocks.GREEN_TERRACOTTA),
        BLUE(Blocks.BLUE_CONCRETE, Blocks.BLUE_TERRACOTTA),
        ORANGE(Blocks.ORANGE_CONCRETE, Blocks.ORANGE_TERRACOTTA),
        GRAY(Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE);

        final BlockState main, rib;

        Color(Block main, Block rib) {
            this.main = main.defaultBlockState();
            this.rib = rib.defaultBlockState();
        }

        /** Reparto típico de una terminal: muchos blancos y óxido, algo de rojo, verde y azul. */
        static Color pick(float roll) {
            if (roll < 0.34f) return WHITE;
            if (roll < 0.55f) return RUST;
            if (roll < 0.68f) return RED;
            if (roll < 0.80f) return BLUE;
            if (roll < 0.90f) return GREEN;
            if (roll < 0.96f) return ORANGE;
            return GRAY;
        }
    }

    enum Kind { CLOSED, OPEN, BREACHED }

    /**
     * Contenedor de 3×3 de sección y {@code len} de largo (7 o 13) a lo largo de +x, desde
     * (x, y, z) hasta (x + len − 1, y + 2, z + 2). La puerta está en el extremo este.
     */
    static void containerX(Canvas c, int x, int y, int z, int len, Color color, Kind kind) {
        container(new Frame(c, x, y, z + 2, Direction.EAST), len, color, kind);
    }

    /** Igual que {@link #containerX} pero a lo largo de +z (puerta en el extremo norte). */
    static void containerZ(Canvas c, int x, int y, int z, int len, Color color, Kind kind) {
        container(new Frame(c, x, y, z, Direction.NORTH), len, color, kind);
    }

    /** Contenedor en el marco {@code f}: a = largo, b = ancho (0..2), y = alto (0..2). */
    static void container(Frame f, int len, Color color, Kind kind) {
        if (!f.touches(-1, -1, len, 3)) return;
        f.fill(0, 0, 0, len - 1, 2, 2, color.main);
        for (int a = 1; a < len - 1; a += 2) {
            f.fill(a, 0, 0, a, 2, 0, color.rib);
            f.fill(a, 0, 2, a, 2, 2, color.rib);
        }
        // Puertas: dos hojas de chapa con sus barras de cierre (trampillas de hierro) y su marco.
        BlockState bar = trapdoor(Blocks.IRON_TRAPDOOR, Direction.NORTH, false, true);
        if (kind == Kind.CLOSED) {
            for (int b = 0; b <= 2; b += 2) f.set(len, 0, b, bar);
            f.set(len, 1, 1, bar);
            return;
        }
        f.air(1, 0, 1, len - 2, 1, 1);
        f.air(len - 1, 0, 1, len - 1, 1, 1);
        // Hojas abiertas de par en par contra los costados.
        f.set(len, 0, -1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, false, true));
        f.set(len, 1, -1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, true, true));
        f.set(len, 0, 3, trapdoor(Blocks.IRON_TRAPDOOR, Direction.WEST, false, true));
        if (kind == Kind.BREACHED) {
            Canvas c = f.canvas();
            for (int a = 2; a < len - 2; a += 3) {
                int wx = f.x(a, 0), wz = f.z(a, 0);
                if (c.chance(wx, f.worldY(0), wz, 0.45f)) f.air(a, 0, 0, a, 1, 0);
                if (c.chance(wx + 1, f.worldY(0), wz + 3, 0.45f)) f.air(a, 0, 2, a, 1, 2);
            }
            f.set(0, 1, 1, AIR); // boquete en el fondo
        }
    }

    // ------------------------------------------------------------------ vehículos

    /**
     * Cabeza tractora con remolque esqueleto (19 de largo, 3 de ancho) mirando hacia {@code facing};
     * (x, z) es la esquina trasera izquierda del remolque. Con {@code load} lleva un contenedor de 13.
     */
    static void truck(Canvas c, int x, int y, int z, Direction facing, Color cab, Color load, String cabLoot) {
        Frame f = new Frame(c, x, y, z, facing);
        if (!f.touches(-1, -1, 19, 3)) return;
        BlockState wheel = s(Blocks.BLACK_CONCRETE), rail = s(Blocks.POLISHED_BLACKSTONE_SLAB);
        // Remolque esqueleto: dos largueros, ejes traseros y "cuello de cisne".
        for (int a = 0; a <= 12; a++) {
            f.set(a, 1, 0, rail);
            f.set(a, 1, 2, rail);
        }
        f.fill(12, 1, 0, 12, 1, 2, s(Blocks.POLISHED_BLACKSTONE));
        for (int a : new int[]{1, 2, 3}) {
            f.set(a, 0, 0, wheel);
            f.set(a, 0, 2, wheel);
        }
        f.set(9, 0, 1, s(Blocks.IRON_BARS)); // patas de apoyo
        if (load != null) {
            container(new Frame(c, f.x(0, 0), y + 2, f.z(0, 0), facing), 13, load, Kind.CLOSED);
        }
        // Cabeza tractora.
        for (int a : new int[]{14, 17}) {
            f.set(a, 0, 0, wheel);
            f.set(a, 0, 2, wheel);
        }
        f.fill(13, 1, 0, 18, 1, 2, s(Blocks.GRAY_CONCRETE));
        f.fill(16, 2, 0, 18, 4, 2, cab.main);
        f.fill(18, 3, 0, 18, 3, 2, s(Blocks.GRAY_STAINED_GLASS));
        f.set(16, 3, 0, s(Blocks.GLASS_PANE));
        f.set(16, 3, 2, s(Blocks.GLASS_PANE));
        f.set(18, 2, 0, s(Blocks.REDSTONE_LAMP));
        f.set(18, 2, 2, s(Blocks.REDSTONE_LAMP));
        f.set(15, 2, 0, s(Blocks.IRON_BARS));
        f.set(15, 3, 0, s(Blocks.IRON_BARS));
        f.set(15, 4, 0, s(Blocks.IRON_BARS)); // tubo de escape
        if (cabLoot != null) f.container(17, 2, 1, facing(Blocks.CHEST, Direction.SOUTH), loot(cabLoot));
    }

    /** Coche o furgoneta abandonada (largo 5 o 6, ancho 3), opcionalmente con botín en el maletero. */
    static void car(Canvas c, int x, int y, int z, Direction facing, Block body, boolean van, String trunkLoot) {
        Frame f = new Frame(c, x, y, z, facing);
        int len = van ? 6 : 5;
        if (!f.touches(-1, -1, len, 3)) return;
        BlockState wheel = s(Blocks.BLACK_CONCRETE);
        f.fill(0, 1, 0, len - 1, 1, 2, body.defaultBlockState());
        for (int a : new int[]{0, len - 1}) {
            f.set(a, 0, 0, wheel);
            f.set(a, 0, 2, wheel);
        }
        int top = van ? 3 : 2;
        f.fill(1, 2, 0, len - 2, top, 2, body.defaultBlockState());
        f.fill(len - 2, 2, 0, len - 2, top, 2, s(Blocks.GRAY_STAINED_GLASS));
        if (!van) f.fill(1, 2, 0, 1, 2, 2, s(Blocks.GRAY_STAINED_GLASS));
        f.set(len - 1, 1, 0, s(Blocks.REDSTONE_LAMP));
        f.set(len - 1, 1, 2, s(Blocks.REDSTONE_LAMP));
        if (trunkLoot != null) f.container(0, 1, 1, facing(Blocks.CHEST, Direction.SOUTH), loot(trunkLoot));
    }

    // ------------------------------------------------------------------ mobiliario urbano

    static void bollard(Canvas c, int x, int y, int z, boolean doubled) {
        c.set(x, y, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
        c.set(x, y + 1, z, s(Blocks.POLISHED_BLACKSTONE_BUTTON).setValue(BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        if (doubled) {
            c.set(x + 1, y, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
            c.set(x + 1, y + 1, z, s(Blocks.POLISHED_BLACKSTONE_BUTTON).setValue(BlockStateProperties.ATTACH_FACE,
                    net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        }
    }

    /** Torre de iluminación: mástil de {@code h} bloques con corona de focos y faroles colgantes. */
    static void lightMast(Canvas c, int x, int y, int z, int h) {
        if (!c.touches(x - 2, z - 2, x + 2, z + 2)) return;
        c.fill(x, y, z, x, y + h - 1, z, s(Blocks.ANDESITE_WALL));
        c.fill(x - 1, y + h, z - 1, x + 1, y + h, z + 1, s(Blocks.IRON_BLOCK));
        c.set(x, y + h + 1, z, s(Blocks.LIGHTNING_ROD));
        for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            c.set(x + d[0], y + h - 1, z + d[1], hangingLantern(Blocks.LANTERN));
        }
    }

    /** Barrera de hormigón con franjas amarillas y negras. */
    static void jersey(Canvas c, int x, int y, int z, boolean alongX, int len) {
        for (int i = 0; i < len; i++) {
            int bx = alongX ? x + i : x, bz = alongX ? z : z + i;
            c.set(bx, y, bz, s((i / 2) % 2 == 0 ? Blocks.YELLOW_CONCRETE : Blocks.BLACK_CONCRETE));
            c.set(bx, y + 1, bz, s(Blocks.SMOOTH_STONE_SLAB));
        }
    }

    /** Escalera de mano vertical en (x, y1..y2, z) apoyada en la pared que tiene detrás. */
    static void ladder(Canvas c, int x, int y1, int y2, int z, Direction facing) {
        for (int y = y1; y <= y2; y++) c.set(x, y, z, Canvas.ladder(facing));
    }

    /**
     * Trampa de cable a lo largo de +x: dispensador con flechas en x1 − 1 (mirando al este), gancho
     * apoyado en él, hilo hasta x2 y segundo gancho apoyado en un barril. Al pisar el hilo, el gancho
     * activa el dispensador y dispara por el pasillo.
     */
    static void tripwireTrap(Canvas c, int x1, int y, int z, int x2) {
        c.container(x1 - 1, y, z, facing(Blocks.DISPENSER, Direction.EAST), loot("trap_arrows"));
        c.set(x1, y, z, s(Blocks.TRIPWIRE_HOOK).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.ATTACHED, true));
        c.set(x2, y, z, s(Blocks.TRIPWIRE_HOOK).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.ATTACHED, true));
        for (int x = x1 + 1; x < x2; x++) {
            c.set(x, y, z, s(Blocks.TRIPWIRE).setValue(BlockStateProperties.ATTACHED, true)
                    .setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true));
        }
        c.set(x2 + 1, y, z, s(Blocks.BARREL));
    }

    private static final Palette STASH_WALL = Palette.weighted(Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 1,
            Blocks.MOSSY_STONE_BRICKS, 1);

    /**
     * Escondite subterráneo de 3×3×2 bajo una escotilla de madera a ras de suelo, con escalera, un cofre
     * (y opcionalmente un barril y una caja de suministros). {@code (hx, hy, hz)} es el bloque de suelo
     * donde va la escotilla; el cuarto se excava debajo, rodeado de ladrillo para no depender del terreno.
     */
    static void stash(Canvas c, int hx, int hy, int hz, String chestLoot, @Nullable String barrelLoot,
                      @Nullable SupplyCrateBlock.Kind crate) {
        if (!c.touches(hx - 1, hy - 3, hz - 1, hx + 3, hy + 2, hz + 3)) return;
        c.fill(hx - 1, hy - 3, hz - 1, hx + 3, hy - 1, hz + 3, STASH_WALL);
        c.air(hx, hy - 2, hz, hx + 2, hy - 1, hz + 2);
        c.air(hx, hy + 1, hz, hx, hy + 2, hz);
        for (int y = hy - 2; y <= hy - 1; y++) c.set(hx, y, hz, Canvas.ladder(Direction.EAST));
        c.set(hx, hy, hz, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, false, false));
        c.container(hx + 2, hy - 2, hz + 1, facing(Blocks.CHEST, Direction.WEST), loot(chestLoot));
        if (barrelLoot != null) c.container(hx + 2, hy - 2, hz + 2, facing(Blocks.BARREL, Direction.UP), loot(barrelLoot));
        if (crate != null) c.crate(hx + 1, hy - 2, hz + 2, Direction.NORTH, crate, false);
        c.set(hx + 1, hy - 1, hz + 1, hangingLantern(Blocks.LANTERN));
    }

    /** Palanca y botón apoyados en el suelo (los de pared se caerían sin soporte). */
    static final BlockState LEVER = s(Blocks.LEVER).setValue(BlockStateProperties.ATTACH_FACE,
            net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
    static final BlockState BUTTON = s(Blocks.STONE_BUTTON).setValue(BlockStateProperties.ATTACH_FACE,
            net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
}
