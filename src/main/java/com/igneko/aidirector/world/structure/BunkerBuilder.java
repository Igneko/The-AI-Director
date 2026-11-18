package com.igneko.aidirector.world.structure;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.registry.ModBlocks;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

import static com.igneko.aidirector.world.structure.Canvas.*;

/**
 * Variante 1 — Entrada a un búnker bajo tierra. Defensa por invisibilidad y cuello de botella:
 * trinchera en zigzag con dos giros de 90°, compuerta blindada camuflada en un montículo, escalera
 * a 45° de diez peldaños, esclusa de descontaminación, puesto de guardia con tronera y la sala
 * principal del refugio.
 *
 * <pre>
 *  z=0..10  trinchera (x 4-5 → giro → x 4-13 → giro → x 12-13)    suelo natural en y = G
 *  z=11     compuerta (puertas de refugio)                          ┐
 *  z=12..21 escalera                                                │ zona segura
 *  z=22..24 esclusa · x 16-20 puesto de guardia                     │ (indestructible)
 *  z=26..32 sala principal                                          ┘
 * </pre>
 */
final class BunkerBuilder {

    private static final int G = SafeRoomVariant.BUNKER.groundLevel();
    private static final int W = SafeRoomVariant.BUNKER.width(), D = SafeRoomVariant.BUNKER.depth(), H = SafeRoomVariant.BUNKER.height();

    private static final Palette MOUND_TOP = Palette.weighted(Blocks.GRASS_BLOCK, 4, Blocks.MOSS_BLOCK, 3, Blocks.PODZOL, 2,
            Blocks.COARSE_DIRT, 2, Blocks.ROOTED_DIRT, 1);
    private static final Palette MOUND_CORE = Palette.weighted(Blocks.DIRT, 6, Blocks.ROOTED_DIRT, 1, Blocks.COARSE_DIRT, 1);
    private static final Palette SHELL = Palette.weighted(Blocks.POLISHED_DEEPSLATE, 4, Blocks.DEEPSLATE_TILES, 3,
            Blocks.DEEPSLATE_BRICKS, 2, Blocks.CRACKED_DEEPSLATE_TILES, 1);
    private static final Palette FLOOR = Palette.weighted(Blocks.POLISHED_BLACKSTONE, 3, Blocks.POLISHED_DEEPSLATE, 2,
            Blocks.DEEPSLATE_TILES, 2);
    private static final Palette CEILING = Palette.weighted(Blocks.DEEPSLATE_TILES, 3, Blocks.POLISHED_DEEPSLATE, 2,
            Blocks.CRACKED_DEEPSLATE_TILES, 1);
    private static final Palette RETAIN = Palette.weighted(Blocks.MOSSY_COBBLESTONE, 4, Blocks.COBBLESTONE, 1,
            Blocks.DARK_OAK_PLANKS, 2, Blocks.TUFF, 1);
    private static final Palette TRENCH_FLOOR = Palette.weighted(Blocks.PACKED_MUD, 3, Blocks.GRAVEL, 2, Blocks.MUD_BRICKS, 1);
    private static final Palette FRAME = Palette.weighted(Blocks.POLISHED_DEEPSLATE, 3, Blocks.IRON_BLOCK, 1, Blocks.WAXED_OXIDIZED_COPPER, 1);

    private static final ResourceLocation RATIONS = loot("rations");
    private static final ResourceLocation AMMO = loot("ammo");
    private static final ResourceLocation SUPPLIES = loot("supplies");

    private BunkerBuilder() {}

    static ResourceLocation loot(String name) {
        return new ResourceLocation(TheAIDirector.MOD_ID, "chests/safe_room_" + name);
    }

    static void build(Canvas c) {
        terrain(c);
        trench(c);
        facade(c);
        staircase(c);
        airlock(c);
        guardPost(c);
        hall(c);
        traps(c);
    }

    // ------------------------------------------------------------------ exterior

    private static boolean inTrench(int x, int z) {
        return (x >= 4 && x <= 5 && z >= 0 && z <= 5)
                || (x >= 4 && x <= 13 && z >= 4 && z <= 5)
                || (x >= 12 && x <= 13 && z >= 4 && z <= 10);
    }

    /** Altura del montículo (0 fuera de él). */
    private static int moundHeight(int x, int z) {
        double dx = (x - 12) / 11.5, dz = (z - 21) / 12.5;
        double d2 = dx * dx + dz * dz;
        return d2 >= 1 ? 0 : (int) Math.round(7 * Math.sqrt(1 - d2));
    }

    /** Patio delantero nivelado a la altura G y montículo de tierra con vegetación. */
    private static void terrain(Canvas c) {
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                int mound = moundHeight(x, z);
                boolean yard = z <= 9;
                if (!yard && mound == 0) continue;
                int top = G + mound;
                c.foundation(x, G - 1, z, Blocks.DIRT.defaultBlockState());
                c.fill(x, G - 3, z, x, top - 1, z, MOUND_CORE);
                c.set(x, top, z, MOUND_TOP);
                c.air(x, top + 1, z, x, H - 1, z);
                // Vegetación: helechos, hierba, azaleas, alfombras de musgo y algún arbusto de bayas.
                float r = c.noise(x, top, z);
                BlockState plant = r < 0.18f ? Blocks.FERN.defaultBlockState()
                        : r < 0.32f ? Blocks.GRASS.defaultBlockState()
                        : r < 0.38f ? Blocks.AZALEA.defaultBlockState()
                        : r < 0.47f ? Blocks.MOSS_CARPET.defaultBlockState()
                        : r < 0.50f ? Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 2)
                        : null;
                if (plant != null && !inTrench(x, z)) c.set(x, top + 1, z, plant);
                if (mound >= 3 && c.chance(x, top, z, 0.05f)) {
                    c.set(x, top + 1, z, leaves(Blocks.AZALEA_LEAVES));
                    c.set(x, top + 2, z, leaves(Blocks.FLOWERING_AZALEA_LEAVES));
                }
            }
        }
    }

    /** Trinchera de aproximación de 2 de ancho y 2 de hondo, con dos giros de 90°. */
    private static void trench(Canvas c) {
        for (int x = 1; x <= 16; x++) {
            for (int z = 0; z <= 11; z++) {
                if (inTrench(x, z)) continue;
                boolean edge = inTrench(x + 1, z) || inTrench(x - 1, z) || inTrench(x, z + 1) || inTrench(x, z - 1);
                if (!edge) continue;
                // Muro de contención: tablones de roble oscuro, troncos descortezados y adoquín musgoso.
                boolean post = (x + z) % 3 == 0;
                for (int y = G - 2; y <= G; y++) {
                    c.set(x, y, z, post ? log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y) : RETAIN.pick(c.noise(x, y, z)));
                }
            }
        }
        for (int x = 4; x <= 13; x++) {
            for (int z = 0; z <= 10; z++) {
                if (!inTrench(x, z)) continue;
                c.set(x, G - 3, z, Blocks.DIRT.defaultBlockState());
                c.set(x, G - 2, z, TRENCH_FLOOR);
                c.air(x, G - 1, z, x, H - 1, z);
            }
        }
        // Rampa de entrada desde el suelo.
        for (int x = 4; x <= 5; x++) {
            c.set(x, G, 0, stairs(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH));
            c.set(x, G - 1, 1, stairs(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH));
            c.set(x, G - 2, 0, Blocks.PACKED_MUD.defaultBlockState());
        }
        // Alambre trampa a mitad del recorrido: un bloque musical da la alarma.
        int tx = 8;
        c.set(tx, G - 1, 2, Blocks.NOTE_BLOCK.defaultBlockState());
        c.set(tx, G - 1, 7, Blocks.NOTE_BLOCK.defaultBlockState());
        c.set(tx, G - 1, 3, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH)
                .setValue(TripWireHookBlock.ATTACHED, true));
        c.set(tx, G - 1, 6, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.SOUTH)
                .setValue(TripWireHookBlock.ATTACHED, true));
        BlockState wire = Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.ATTACHED, true)
                .setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true);
        c.set(tx, G - 1, 4, wire);
        c.set(tx, G - 1, 5, wire);
    }

    /** Fachada mimetizada: marco de pizarra y hierro, alero cubierto de musgo y enredaderas. */
    private static void facade(Canvas c) {
        for (int y = G - 2; y <= G + 1; y++) {
            c.set(11, y, 11, FRAME);
            c.set(14, y, 11, FRAME);
        }
        c.fill(11, G + 1, 11, 14, G + 1, 11, FRAME);
        c.set(11, G + 1, 11, Blocks.IRON_BLOCK.defaultBlockState());
        c.set(14, G + 1, 11, Blocks.IRON_BLOCK.defaultBlockState());
        c.fill(12, G - 2, 11, 13, G - 2, 11, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        c.doubleDoor(12, G - 1, 11, ModBlocks.SAFE_ROOM_DOOR.get());

        // Alero: sobresale dos bloques sobre la trinchera y continúa la pendiente.
        for (int z = 9; z <= 10; z++) {
            for (int x = 11; x <= 14; x++) {
                c.set(x, G + 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                c.set(x, G + 2, z, c.chance(x, G + 2, z, 0.5f) ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                c.set(x, G + 3, z, c.chance(x, G + 3, z, 0.6f)
                        ? (c.chance(x, 1, z, 0.5f) ? Blocks.FERN.defaultBlockState() : Blocks.MOSS_CARPET.defaultBlockState())
                        : Blocks.AIR.defaultBlockState());
            }
        }
        // Enredaderas que cuelgan del frente del alero (cada una sujeta a la de arriba) y hojas a los lados.
        for (int x = 12; x <= 13; x++) {
            int len = 2 + (int) (c.noise(x, 0, 8) * 1.99f); // 2-3: nunca llega al suelo de la trinchera
            for (int i = 0; i < len; i++) c.set(x, G + 1 - i, 8, Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true));
        }
        for (int x : new int[]{11, 14}) {
            if (c.chance(x, 2, 8, 0.6f)) c.set(x, G + 1, 8, leaves(Blocks.AZALEA_LEAVES));
        }
    }

    // ------------------------------------------------------------------ interior (zona segura)

    /** Escalera de piedra negra a 45°, diez peldaños, con luces de emergencia. */
    private static void staircase(Canvas c) {
        for (int z = 12; z <= 21; z++) {
            int f = G - 3 - (z - 12);
            c.fill(11, f - 1, z, 14, f + 4, z, SHELL);
            c.fill(11, f + 4, z, 14, f + 4, z, CEILING);
            c.fill(12, f, z, 13, f, z, stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH));
            c.air(12, f + 1, z, 13, f + 3, z);
            if ((z - 12) % 3 == 1) {
                c.set(12 + (z % 2), f + 3, z, hangingLantern(Blocks.SOUL_LANTERN));
            } else if ((z - 12) % 3 == 2) {
                c.set(12, f + 2, z, wallTorch(Blocks.REDSTONE_WALL_TORCH, Direction.EAST));
                c.set(13, f + 2, z, wallTorch(Blocks.REDSTONE_WALL_TORCH, Direction.WEST));
            }
        }
        // Detrás de la compuerta: rellano bajo el alero.
        c.fill(11, G - 3, 12, 14, G + 2, 12, SHELL);
        c.fill(12, G - 3, 12, 13, G - 3, 12, stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH));
        c.air(12, G - 2, 12, 13, G, 12);
        c.set(12, G - 1, 12, Blocks.AIR.defaultBlockState());
    }

    /** Esclusa de descontaminación de 4×3 con regaderas de tolva, desagües y doble puerta de hierro. */
    private static void airlock(Canvas c) {
        c.fill(10, 1, 21, 15, 1, 25, SHELL);
        c.room(10, 2, 21, 15, 6, 25, SHELL, FLOOR, CEILING);
        c.fill(11, 2, 21, 14, 2, 21, FLOOR);
        c.fill(12, 2, 21, 13, 2, 21, stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH));
        c.air(11, 3, 21, 14, 5, 21);
        c.fill(11, 3, 21, 11, 5, 21, SHELL);
        c.fill(14, 3, 21, 14, 5, 21, SHELL);
        for (int z = 22; z <= 24; z++) {
            c.set(12, 6, z, facing(Blocks.HOPPER, Direction.DOWN));
            c.set(13, 6, z, facing(Blocks.HOPPER, Direction.DOWN));
            c.set(11, 5, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, true, false));
            c.set(14, 5, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.WEST, true, false));
        }
        for (int x = 12; x <= 13; x++) {
            c.set(x, 1, 23, Blocks.COAL_BLOCK.defaultBlockState());
            c.set(x, 2, 23, trapdoor(Blocks.IRON_TRAPDOOR, Direction.NORTH, true, false));
        }
        // Compuerta interior de hierro con botones a ambos lados y palancas de bloqueo.
        c.door(12, 3, 25, Blocks.IRON_DOOR, Direction.NORTH, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
        c.door(13, 3, 25, Blocks.IRON_DOOR, Direction.NORTH, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
        for (int side = 0; side < 2; side++) {
            int fx = side == 0 ? 11 : 14;
            c.set(fx, 4, 24, button(Direction.SOUTH));
            c.set(fx, 4, 26, button(Direction.NORTH));
        }
        c.set(11, 4, 22, lever(Direction.EAST));
        c.set(11, 4, 23, lever(Direction.EAST));
    }

    /** Puesto de guardia de 5×5 con tronera blindada hacia la esclusa y la escalera. */
    private static void guardPost(Canvas c) {
        c.fill(15, 1, 19, 21, 1, 25, SHELL);
        c.room(15, 2, 19, 21, 6, 25, SHELL, FLOOR, CEILING);
        c.set(15, 3, 22, Blocks.IRON_BARS.defaultBlockState());
        c.set(15, 3, 23, Blocks.IRON_BARS.defaultBlockState());
        c.set(15, 4, 22, Blocks.TINTED_GLASS.defaultBlockState());
        c.set(15, 4, 23, Blocks.TINTED_GLASS.defaultBlockState());
        c.air(17, 3, 25, 18, 4, 25);
        c.container(20, 3, 20, facing(Blocks.BARREL, Direction.UP), RATIONS);
        c.container(20, 3, 21, facing(Blocks.BARREL, Direction.UP), RATIONS);
        c.container(16, 3, 20, facing(Blocks.CHEST, Direction.EAST), AMMO);
        c.set(18, 3, 20, facing(Blocks.DAMAGED_ANVIL, Direction.EAST));
        c.set(16, 3, 22, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
        c.set(16, 3, 23, Blocks.SPRUCE_PLANKS.defaultBlockState());
        c.set(16, 4, 23, Blocks.LANTERN.defaultBlockState());
        c.armorStand(20, 3, 23, Direction.WEST, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS));
        lichen(c, 16, 20, 20, 24, 3, 5);
        c.set(18, 5, 22, hangingLantern(Blocks.LANTERN));
    }

    /** Sala principal: literas, puesto médico, armería, radio y montón de munición inagotable. */
    private static void hall(Canvas c) {
        c.fill(2, 1, 25, 22, 1, 33, SHELL);
        c.room(2, 2, 25, 22, 7, 33, SHELL, FLOOR, CEILING);
        // Las paredes compartidas se reabren hacia la esclusa y el puesto de guardia.
        c.door(12, 3, 25, Blocks.IRON_DOOR, Direction.NORTH, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
        c.door(13, 3, 25, Blocks.IRON_DOOR, Direction.NORTH, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
        c.air(17, 3, 25, 18, 4, 25);
        c.set(11, 4, 26, button(Direction.NORTH));
        c.set(14, 4, 26, button(Direction.NORTH));
        c.fill(3, 2, 28, 21, 2, 30, Palette.weighted(Blocks.POLISHED_BLACKSTONE_BRICKS, 3, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 1));

        // Literas (dos camas por litera) junto a la pared oeste.
        for (int z = 27; z <= 31; z += 2) {
            c.bed(4, 3, z, Blocks.GRAY_BED, Direction.WEST);
            c.fill(3, 4, z, 4, 4, z, slab(Blocks.SPRUCE_SLAB, true));
            c.bed(4, 5, z, Blocks.BROWN_BED, Direction.WEST);
            c.container(5, 3, z, facing(Blocks.BARREL, Direction.UP), RATIONS);
        }
        // Puesto médico al fondo.
        c.crate(8, 3, 32, Direction.SOUTH, SupplyCrateBlock.Kind.MEDICAL, false);
        c.crate(9, 3, 32, Direction.SOUTH, SupplyCrateBlock.Kind.MEDICAL, false);
        c.set(10, 3, 32, Blocks.BREWING_STAND.defaultBlockState());
        c.set(7, 3, 32, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        // Radio del refugio en un escritorio, con la zona segura desde la compuerta hasta el fondo.
        c.set(12, 3, 32, Blocks.SMITHING_TABLE.defaultBlockState());
        c.set(13, 3, 32, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        c.radio(12, 4, 32, Direction.SOUTH, 2, 1, 11, 22, G + 1, 33, 12, 3, 29, "bunker");
        c.set(13, 4, 32, Blocks.LANTERN.defaultBlockState());
        c.set(12, 3, 31, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
        c.set(14, 3, 32, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        // Armería al este.
        c.crate(21, 3, 31, Direction.WEST, SupplyCrateBlock.Kind.ARMORY, false);
        c.crate(21, 3, 30, Direction.WEST, SupplyCrateBlock.Kind.THROWABLES, false);
        c.ammoPile(21, 3, 28);
        c.container(21, 3, 27, facing(Blocks.CHEST, Direction.WEST), AMMO);
        c.container(21, 3, 32, facing(Blocks.CHEST, Direction.WEST), SUPPLIES);
        c.armorStand(19, 3, 32, Direction.SOUTH, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE));
        c.armorStand(17, 3, 32, Direction.SOUTH, new ItemStack(Items.CHAINMAIL_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        c.set(16, 3, 32, Blocks.CRAFTING_TABLE.defaultBlockState());
        c.set(15, 3, 32, facing(Blocks.SMOKER, Direction.SOUTH));
        // Luz y humedad subterránea.
        for (int x = 5; x <= 19; x += 7) c.set(x, 6, 29, hangingLantern(Blocks.LANTERN));
        c.set(12, 6, 27, hangingLantern(Blocks.SOUL_LANTERN));
        lichen(c, 3, 26, 21, 32, 3, 6);
    }

    /** Fosos camuflados con estalagmitas en los bordes del montículo. */
    private static void traps(Canvas c) {
        int[][] pits = {{2, 14}, {22, 16}, {1, 9}};
        for (int[] p : pits) {
            int x = p[0], z = p[1];
            int top = G + moundHeight(x, z);
            c.set(x, top - 4, z, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
            c.set(x, top - 3, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.UP)
                    .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, DripstoneThickness.TIP));
            c.air(x, top - 2, z, x, top - 1, z);
            c.set(x, top, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true, false));
            c.set(x, top + 1, z, Blocks.AIR.defaultBlockState());
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Liquen brillante en las celdas interiores pegadas a la pared sur y a la pared este (filtraciones). */
    static void lichen(Canvas c, int xMin, int zMin, int xMax, int zMax, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = xMin; x <= xMax; x++) {
                if (c.chance(x, y, zMin, 0.12f) && c.get(x, y, zMin).isAir() && c.get(x, y, zMin - 1).canOcclude()) {
                    c.set(x, y, zMin, Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.SOUTH, true));
                }
            }
            for (int z = zMin; z <= zMax; z++) {
                if (c.chance(xMax, y, z, 0.12f) && c.get(xMax, y, z).isAir() && c.get(xMax + 1, y, z).canOcclude()) {
                    c.set(xMax, y, z, Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.EAST, true));
                }
            }
        }
    }

    static BlockState button(Direction facing) {
        return Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL)
                .setValue(ButtonBlock.FACING, facing);
    }

    static BlockState lever(Direction facing) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, facing);
    }
}
