package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.registry.ModBlocks;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static com.igneko.aidirector.world.structure.Canvas.*;

/**
 * Variante 2 — Hangar de entrada reducida. Bóveda semicilíndrica de 17 × 28 × 11 con franjas de
 * cobre envejecido y costillas de refuerzo cada 4 bloques. El gran portón está sellado en un 75 %
 * con contenedores apilados y planchas de hierro; solo queda un paso de 3 bloques cerrado por
 * puertas de refugio y una reja. Dentro: altillo de tiro, taller, almacén, vehículo blindado y grúa.
 *
 * <pre>
 *  z=0..5   explanada con "dientes de dragón"
 *  z=6      fachada sellada (contenedores + paso con puertas)   ┐
 *  z=7..32  nave (x 2-18)                                       │ zona segura
 *  z=33     muro trasero                                        ┘
 * </pre>
 */
final class HangarBuilder {

    private static final int W = SafeRoomVariant.HANGAR.width(), D = SafeRoomVariant.HANGAR.depth(), H = SafeRoomVariant.HANGAR.height();
    private static final int CX = 10, Z0 = 6, Z1 = 33;
    private static final double A = 8.6, B = 11.2;

    private static final Palette FLOOR = Palette.weighted(Blocks.POLISHED_ANDESITE, 4, Blocks.SMOOTH_STONE, 2, Blocks.STONE, 1,
            Blocks.ANDESITE, 1);
    private static final Palette APRON = Palette.weighted(Blocks.GRAVEL, 3, Blocks.PACKED_MUD, 2, Blocks.COBBLESTONE, 1, Blocks.ANDESITE, 1);
    private static final Palette RIB = Palette.weighted(Blocks.POLISHED_ANDESITE, 3, Blocks.TUFF, 2, Blocks.POLISHED_BLACKSTONE, 1);
    private static final Palette END_WALL = Palette.weighted(Blocks.LIGHT_GRAY_CONCRETE, 3, Blocks.CYAN_TERRACOTTA, 2,
            Blocks.WAXED_WEATHERED_COPPER, 1, Blocks.WAXED_OXIDIZED_COPPER, 1);
    private static final ResourceLocation PARTS = BunkerBuilder.loot("parts");

    private HangarBuilder() {}

    static void build(Canvas c) {
        ground(c);
        shell(c);
        frontWall(c);
        backWall(c);
        exterior(c);
        interior(c);
    }

    /** ¿La celda (dx desde el eje, y) está dentro de la sección de la bóveda? */
    private static boolean inVault(int dx, int y) {
        if (y < 1) return false;
        double e = (dx / A) * (dx / A) + (y / B) * (y / B);
        return e <= 1.0;
    }

    private static boolean isShell(int dx, int y) {
        return inVault(dx, y) && (!inVault(dx + 1, y) || !inVault(dx - 1, y) || !inVault(dx, y + 1));
    }

    /** Material de la cubierta según la franja (a lo largo de z). */
    private static BlockState stripe(int z, boolean stairs, Direction facing) {
        int band = Math.floorMod(z * 7 + 3, 5);
        if (stairs) {
            Block s = switch (band) {
                case 0 -> Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS;
                case 1 -> Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS;
                case 2 -> Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS;
                case 3 -> Blocks.POLISHED_ANDESITE_STAIRS;
                default -> Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS;
            };
            return stairs(s, facing);
        }
        return switch (band) {
            case 0 -> Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();
            case 1 -> Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
            case 2 -> Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();
            case 3 -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            default -> Blocks.CYAN_TERRACOTTA.defaultBlockState();
        };
    }

    /** Explanada despejada y nivelada; cimientos bajo toda la huella. */
    private static void ground(Canvas c) {
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                c.foundation(x, -1, z, Blocks.STONE.defaultBlockState());
                c.set(x, 0, z, z < Z0 || Math.abs(x - CX) > 8 ? APRON.pick(c.noise(x, 0, z)) : FLOOR.pick(c.noise(x, 0, z)));
                c.air(x, 1, z, x, H - 1, z);
            }
        }
    }

    /** Bóveda: cubierta escalonada con escaleras en el contorno y costillas en relieve cada 4 bloques. */
    private static void shell(Canvas c) {
        for (int z = Z0; z <= Z1; z++) {
            boolean rib = (z - Z0) % 4 == 0 || z == Z1;
            for (int dx = -9; dx <= 9; dx++) {
                for (int y = 1; y <= 12; y++) {
                    int x = CX + dx;
                    if (isShell(dx, y)) {
                        boolean corner = !inVault(dx, y + 1) && !inVault(dx + Integer.signum(dx), y) && dx != 0;
                        Direction in = dx > 0 ? Direction.WEST : Direction.EAST;
                        if (rib) c.set(x, y, z, corner ? stairs(Blocks.POLISHED_ANDESITE_STAIRS, in) : RIB.pick(c.noise(x, y, z)));
                        else c.set(x, y, z, stripe(z, corner, in));
                    } else if (rib && !inVault(dx, y) && touchesVault(dx, y)) {
                        // Relieve de la costilla por fuera de la cubierta.
                        c.set(x, y, z, Blocks.TUFF.defaultBlockState());
                    }
                }
            }
        }
    }

    private static boolean touchesVault(int dx, int y) {
        return inVault(dx + 1, y) || inVault(dx - 1, y) || inVault(dx, y - 1);
    }

    /**
     * Fachada: el vano de 11 bloques está tapado por dos contenedores abajo, otro encima y planchas de
     * hierro; en el centro, el paso de 3 × 4 con puertas de refugio y una reja levadiza fija encima.
     */
    private static void frontWall(Canvas c) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int y = 1; y <= 12; y++) {
                if (!inVault(dx, y) || isShell(dx, y)) continue;
                c.set(CX + dx, y, Z0, END_WALL.pick(c.noise(dx, y, Z0)));
            }
        }
        // Contenedores inferiores (rojo a la izquierda, azul a la derecha) y uno superior.
        c.fill(5, 1, Z0, 8, 3, Z0, Blocks.RED_TERRACOTTA.defaultBlockState());
        c.fill(12, 1, Z0, 15, 3, Z0, Blocks.BLUE_TERRACOTTA.defaultBlockState());
        c.fill(5, 5, Z0, 15, 7, Z0, Blocks.BROWN_TERRACOTTA.defaultBlockState());
        c.fill(5, 4, Z0, 8, 4, Z0, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(12, 4, Z0, 15, 4, Z0, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(5, 8, Z0, 15, 8, Z0, Blocks.IRON_BLOCK.defaultBlockState());
        // Puertas corrugadas de los contenedores (trampillas abiertas pegadas a la fachada).
        for (int x = 5; x <= 15; x++) {
            if (x >= 9 && x <= 11) continue;
            for (int y = 1; y <= 3; y++) c.set(x, y, Z0 - 1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.SOUTH, false, true));
        }
        for (int x = 5; x <= 15; x += 2) c.set(x, 6, Z0 - 1, trapdoor(Blocks.IRON_TRAPDOOR, Direction.SOUTH, false, true));
        // Troneras para el altillo.
        for (int x : new int[]{6, 8, 12, 14}) {
            c.set(x, 7, Z0, Blocks.IRON_BARS.defaultBlockState());
            c.set(x, 8, Z0, Blocks.IRON_BARS.defaultBlockState());
        }
        // Paso central: puertas de refugio + barrotes, y reja levadiza fija encima.
        c.air(9, 1, Z0, 11, 4, Z0);
        c.doubleDoor(9, 1, Z0, ModBlocks.SAFE_ROOM_DOOR.get());
        c.set(11, 1, Z0, Blocks.IRON_BARS.defaultBlockState());
        c.set(11, 2, Z0, Blocks.IRON_BARS.defaultBlockState());
        for (int x = 9; x <= 11; x++) {
            c.set(x, 3, Z0, x == 10 ? Blocks.NETHER_BRICK_FENCE.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState());
            c.set(x, 4, Z0, Blocks.NETHER_BRICK_FENCE.defaultBlockState());
        }
        c.fill(8, 1, Z0, 8, 4, Z0, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(12, 1, Z0, 12, 4, Z0, Blocks.IRON_BLOCK.defaultBlockState());
    }

    private static void backWall(Canvas c) {
        for (int dx = -9; dx <= 9; dx++) {
            for (int y = 1; y <= 12; y++) {
                if (inVault(dx, y) && !isShell(dx, y)) c.set(CX + dx, y, Z1, END_WALL.pick(c.noise(dx, y, Z1)));
            }
        }
        for (int x = 9; x <= 11; x++) c.set(x, 7, Z1, Blocks.IRON_BARS.defaultBlockState());
    }

    /** Dientes de dragón, extractores laterales y chimeneas improvisadas. */
    private static void exterior(Canvas c) {
        for (int x = 1; x < W - 1; x++) {
            for (int z = 1; z <= 3; z++) {
                if (x >= 8 && x <= 12) continue;
                if ((x + z) % 2 != 0) continue;
                boolean slate = c.chance(x, 1, z, 0.5f);
                c.set(x, 1, z, (slate ? Blocks.COBBLED_DEEPSLATE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState());
                if (c.chance(x, 2, z, 0.4f)) c.set(x, 2, z, (slate ? Blocks.COBBLED_DEEPSLATE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState());
            }
        }
        for (int z = Z0 + 6; z <= Z1 - 4; z += 8) {
            for (int side = -1; side <= 1; side += 2) {
                int x = CX + side * 9;
                Direction out = side > 0 ? Direction.EAST : Direction.WEST;
                c.set(x, 1, z, stairsTop(Blocks.POLISHED_BLACKSTONE_STAIRS, out.getOpposite()));
                c.set(x, 2, z, trapdoor(Blocks.IRON_TRAPDOOR, out, false, true));
                c.set(x, 3, z, trapdoor(Blocks.IRON_TRAPDOOR, out, false, true));
            }
        }
        for (int z : new int[]{Z0 + 7, Z1 - 7}) {
            int x = CX - 6;
            for (int y = 9; y <= 10; y++) c.set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            c.set(x, 11, z, unlitCampfire());
        }
    }

    private static void interior(Canvas c) {
        // Vigas transversales de piedra negra con faroles colgando a distintas alturas.
        for (int z = Z0 + 4; z < Z1; z += 4) {
            for (int dx = -6; dx <= 6; dx++) c.set(CX + dx, 8, z, log(Blocks.POLISHED_BASALT, Direction.Axis.X));
            for (int dx : new int[]{-4, 4}) {
                int len = 1 + (int) (c.noise(dx, 7, z) * 2);
                for (int i = 0; i < len; i++) c.set(CX + dx, 7 - i, z, Blocks.CHAIN.defaultBlockState());
                c.set(CX + dx, 7 - len, z, hangingLantern(Blocks.LANTERN));
            }
        }
        // Puente grúa: riel bajo la cumbrera, carro de carbón y observadores, gancho colgante.
        for (int z = Z0 + 2; z <= Z1 - 2; z++) c.set(CX, 10, z, slab(Blocks.POLISHED_BLACKSTONE_SLAB, true));
        int cz = 19;
        c.set(CX, 9, cz, Blocks.COAL_BLOCK.defaultBlockState());
        c.set(CX - 1, 9, cz, facing(Blocks.OBSERVER, Direction.EAST));
        c.set(CX + 1, 9, cz, facing(Blocks.OBSERVER, Direction.WEST));
        c.set(CX, 9, cz + 1, facing(Blocks.PISTON, Direction.DOWN));
        for (int y = 6; y <= 8; y++) c.set(CX, y, cz, Blocks.CHAIN.defaultBlockState());
        c.set(CX, 5, cz, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));

        vehicle(c);
        workshop(c);
        storage(c);
        mezzanine(c);
        safeRoom(c);
    }

    /** Vehículo blindado improvisado en el eje de la nave. */
    private static void vehicle(Canvas c) {
        int z0 = 15, z1 = 23;
        c.fill(9, 1, z0, 11, 1, z1, Blocks.IRON_BLOCK.defaultBlockState());
        for (int z : new int[]{16, 17, 21, 22}) {
            c.set(8, 1, z, Blocks.COAL_BLOCK.defaultBlockState());
            c.set(12, 1, z, Blocks.COAL_BLOCK.defaultBlockState());
            c.set(7, 1, z, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.WEST));
            c.set(13, 1, z, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.EAST));
        }
        c.fill(8, 2, z0, 12, 2, z1, Palette.weighted(Blocks.IRON_BLOCK, 3, Blocks.LIGHT_GRAY_CONCRETE, 1));
        // Cabina con cristal tintado.
        c.fill(9, 3, 20, 11, 3, z1, Blocks.TINTED_GLASS.defaultBlockState());
        c.fill(9, 4, 20, 11, 4, z1, Blocks.IRON_BLOCK.defaultBlockState());
        c.set(10, 3, 21, Blocks.AIR.defaultBlockState());
        // Caja de carga con laterales de trampilla.
        for (int z = z0; z <= 19; z++) {
            c.set(8, 3, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, false, true));
            c.set(12, 3, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.WEST, false, true));
        }
        c.container(10, 3, 16, facing(Blocks.BARREL, Direction.UP), PARTS);
        c.set(9, 3, 17, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        // Defensa frontal.
        for (int x = 8; x <= 12; x++) {
            c.set(x, 1, z1 + 1, Blocks.IRON_BARS.defaultBlockState());
            c.set(x, 2, z1 + 1, Blocks.IRON_BARS.defaultBlockState());
        }
    }

    /** Lateral izquierdo: taller mecánico (y el generador de emergencia, una alarma Crescendo). */
    private static void workshop(Canvas c) {
        int x = 3;
        for (int z = 10; z <= 30; z++) {
            BlockState s = switch ((z - 10) % 7) {
                case 0 -> Blocks.SMITHING_TABLE.defaultBlockState();
                case 1 -> facing(Blocks.BLAST_FURNACE, Direction.EAST);
                case 2 -> Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR)
                        .setValue(GrindstoneBlock.FACING, Direction.NORTH);
                case 3 -> facing(Blocks.ANVIL, Direction.NORTH);
                case 4 -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3);
                case 5 -> Blocks.CRAFTING_TABLE.defaultBlockState();
                default -> null;
            };
            if (s != null) c.set(x, 1, z, s);
        }
        // Generador de emergencia: activarlo desata un evento de pánico; si se resiste, se abre la armería.
        c.fill(3, 1, 25, 4, 2, 26, Blocks.IRON_BLOCK.defaultBlockState());
        c.set(4, 1, 27, facing(Blocks.BLAST_FURNACE, Direction.EAST));
        c.alarm(5, 1, 26, Direction.EAST, 90);
        c.set(4, 3, 25, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    /** Lateral derecho: estanterías de tres niveles con barriles, cofres y bidones. */
    private static void storage(Canvas c) {
        for (int z = 10; z <= 28; z++) {
            if (z == 19 || z == 20) continue;
            c.container(16, 1, z, facing(Blocks.BARREL, Direction.WEST), PARTS);
            c.set(16, 2, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            if (c.chance(16, 3, z, 0.6f)) c.set(16, 3, z, facing(Blocks.BARREL, Direction.WEST));
            c.set(15, 4, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            c.set(16, 4, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true, false));
            if (c.chance(15, 5, z, 0.5f)) c.set(15, 5, z, facing(Blocks.BARREL, Direction.WEST));
        }
        // Bidones de combustible: bloques de cobre y terracota esmaltada con una "válvula".
        for (int z = 11; z <= 15; z += 2) {
            c.set(14, 1, z, Blocks.WAXED_EXPOSED_COPPER.defaultBlockState());
            c.set(14, 2, z, Blocks.ORANGE_GLAZED_TERRACOTTA.defaultBlockState());
            c.set(14, 3, z, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                    .setValue(LeverBlock.FACING, Direction.NORTH));
        }
    }

    /** Altillo de tiro sobre la entrada: pasarela de trampillas de hierro con barandilla. */
    private static void mezzanine(Canvas c) {
        for (int x = 4; x <= 16; x++) {
            for (int z = Z0 + 1; z <= Z0 + 4; z++) c.set(x, 5, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.NORTH, true, false));
            if (x > 4) c.set(x, 6, Z0 + 5, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int x : new int[]{4, 16}) {
            for (int y = 1; y <= 5; y++) c.set(x, y, Z0 + 4, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        }
        for (int y = 1; y <= 5; y++) c.set(4, y, Z0 + 5, ladder(Direction.NORTH));
    }

    /** Fondo de la nave: el refugio en sí (radio, suministros, munición, camas). */
    private static void safeRoom(Canvas c) {
        c.set(10, 1, 32, Blocks.SMITHING_TABLE.defaultBlockState());
        c.radio(10, 2, 32, Direction.SOUTH, CX - 8, 0, Z0, CX + 8, 12, Z1, 10, 1, 28, "hangar");
        c.set(9, 1, 32, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        c.set(9, 2, 32, Blocks.LANTERN.defaultBlockState());
        c.crate(5, 1, 32, Direction.SOUTH, SupplyCrateBlock.Kind.MEDICAL, false);
        c.crate(6, 1, 32, Direction.SOUTH, SupplyCrateBlock.Kind.MEDICAL, false);
        c.crate(7, 1, 32, Direction.SOUTH, SupplyCrateBlock.Kind.THROWABLES, false);
        c.crate(14, 1, 32, Direction.SOUTH, SupplyCrateBlock.Kind.ARMORY, false);
        c.crate(15, 1, 32, Direction.SOUTH, SupplyCrateBlock.Kind.ARMORY, true);
        c.ammoPile(13, 1, 31);
        c.container(12, 1, 32, facing(Blocks.CHEST, Direction.SOUTH), BunkerBuilder.loot("ammo"));
        for (int z = 29; z <= 31; z += 2) c.bed(17, 1, z, Blocks.GREEN_BED, Direction.WEST);
        for (int x = 5; x <= 8; x++) c.set(x, 1, 30, Blocks.GRAY_CARPET.defaultBlockState());
        c.armorStand(11, 1, 32, Direction.SOUTH, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.IRON_BOOTS));
        c.set(10, 5, 30, hangingLantern(Blocks.SOUL_LANTERN));
    }
}
