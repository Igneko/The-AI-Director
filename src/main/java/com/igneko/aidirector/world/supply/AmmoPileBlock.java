package com.igneko.aidirector.world.supply;

import com.igneko.aidirector.compat.tacz.AmmoRefillService;
import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Montón de munición L4D2: al usarlo con un arma de TaCZ en la mano, rellena la reserva del calibre
 * del arma hasta el máximo y, por defecto, se consume (un solo uso). Las armas de nivel 3 (lanzagranadas, M60...) no se pueden recargar aquí.
 */
public class AmmoPileBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 9, 15);
    /** Montón inagotable de los refugios (como en L4D2): nunca se consume. */
    public static final BooleanProperty INFINITE = BooleanProperty.create("infinite");

    public AmmoPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INFINITE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INFINITE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer sp
                && AmmoRefillService.refill(sp) == AmmoRefillService.Result.REFILLED
                && AIDirectorConfig.tacz().ammoPileSingleUse() && !state.getValue(INFINITE)) {
            // Montón de un solo uso: se consume (con partículas y sonido de rotura, sin soltar el bloque).
            level.destroyBlock(pos, false, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
