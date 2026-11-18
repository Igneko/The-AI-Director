package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.core.director.CrescendoManager;
import com.igneko.aidirector.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Disparador de evento de pánico (especificación §9, {@code CrescendoTriggerBlock}): una sirena de
 * emergencia. Al activarla (a mano o con un pulso de redstone) el Director ignora la calma y lanza
 * oleadas continuas durante un tiempo, o hasta que alguien la apague si está en modo interruptor.
 * Al terminar, las cajas cerradas cercanas se abren como recompensa.
 */
public class CrescendoAlarmBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Forma del modelo (base, cuello y seta del pulsador): no tapa las caras de los bloques vecinos. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 8, 15),
            Block.box(6, 8, 6, 10, 10, 10), Block.box(4.5, 10, 4.5, 11.5, 15, 11.5));

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }


    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CrescendoAlarmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CrescendoAlarmBlockEntity alarm)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        ServerLevel server = (ServerLevel) level;
        if (player.isCreative() && player.isShiftKeyDown()) {
            alarm.cycleMode();
            player.displayClientMessage(Component.translatable("message.aidirector.alarm.mode", alarm.describeMode()), true);
        } else if (CrescendoManager.isActiveAt(server, pos)) {
            if (alarm.isSwitchMode()) {
                CrescendoManager.stop(server, true);
            } else {
                player.displayClientMessage(Component.translatable("message.aidirector.alarm.jammed").withStyle(ChatFormatting.RED), true);
            }
        } else if (CrescendoManager.isActive(server)) {
            player.displayClientMessage(Component.translatable("message.aidirector.alarm.busy").withStyle(ChatFormatting.YELLOW), true);
        } else {
            CrescendoManager.start(server, pos, alarm.durationSeconds());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level instanceof ServerLevel server && !CrescendoManager.isActive(server)
                && level.getBlockEntity(pos) instanceof CrescendoAlarmBlockEntity alarm) {
            CrescendoManager.start(server, pos, alarm.durationSeconds());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.aidirector.crescendo_alarm.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrescendoAlarmBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlocks.CRESCENDO_ALARM_BE.get()) return null;
        return (lvl, pos, st, be) -> {
            // Tras reiniciar el servidor, una sirena que quedó encendida sin evento se apaga sola.
            if (lvl.getGameTime() % 20 == 0 && st.getValue(ACTIVE) && !CrescendoManager.isActiveAt((ServerLevel) lvl, pos)) {
                lvl.setBlock(pos, st.setValue(ACTIVE, false), Block.UPDATE_ALL);
            }
        };
    }
}
