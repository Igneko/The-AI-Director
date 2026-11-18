package com.igneko.aidirector.world.saferoom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Puerta de refugio: plancha de acero roja que se abre a mano y con redstone. A diferencia de una
 * puerta de madera, para la IA vanilla no es "de madera" ({@link DoorBlock#isWoodenDoor}), así que
 * ningún zombi intenta derribarla; además pertenece a {@code forge:horde_unbreakable}, por lo que
 * la horda de Enhanced Hordes tampoco la rompe. Dentro de un refugio, cerrarla lo sella.
 */
public class SafeRoomDoorBlock extends DoorBlock {

    /** Acero: sonido metálico, pero sin "abrir a mano" para que la IA no la trate como madera. */
    public static final BlockSetType TYPE = BlockSetType.register(new BlockSetType("aidirector_safe_room", false,
            SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE,
            SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));

    public SafeRoomDoorBlock(Properties properties) {
        super(properties, TYPE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) setOpen(player, level, state, pos, !isOpen(state));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
