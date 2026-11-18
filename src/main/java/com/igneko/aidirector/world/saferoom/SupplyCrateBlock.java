package com.igneko.aidirector.world.saferoom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Caja de suministros: un "nodo de botín" que el Director rellena al abrirla (especificación §4.3).
 * Cada superviviente puede servirse una vez de cada caja, como los objetos de la mesa de un refugio
 * de L4D2. Las cajas cerradas con candado se abren al superar un evento Crescendo cercano.
 */
public class SupplyCrateBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public enum Kind implements StringRepresentable {
        /** Botiquín o desfibrilador si el equipo está mal; píldoras y adrenalina si está bien. */
        MEDICAL,
        /** Arma de TaCZ con munición (y a veces un arma cuerpo a cuerpo). */
        ARMORY,
        /** Bomba casera, molotov o frasco de bilis. */
        THROWABLES;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public SupplyCrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, Kind.MEDICAL)
                .setValue(LOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND, LOCKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // En creativo, agacharse y usar la caja cambia su tipo (herramienta de creadores de mapas).
        if (player.isCreative() && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                Kind next = Kind.values()[(state.getValue(KIND).ordinal() + 1) % Kind.values().length];
                boolean lock = next == Kind.MEDICAL ? !state.getValue(LOCKED) : state.getValue(LOCKED);
                level.setBlock(pos, state.setValue(KIND, next).setValue(LOCKED, lock), Block.UPDATE_ALL);
                player.displayClientMessage(Component.translatable("message.aidirector.crate.kind",
                        Component.translatable("block.aidirector.supply_crate." + next.getSerializedName()),
                        Component.translatable(lock ? "message.aidirector.crate.locked_flag" : "message.aidirector.crate.unlocked_flag")), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SupplyCrateBlockEntity crate) {
            if (state.getValue(LOCKED)) {
                sp.displayClientMessage(Component.translatable("message.aidirector.crate.locked").withStyle(ChatFormatting.RED), true);
            } else {
                crate.open(sp, state.getValue(KIND));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Pila de objeto con el tipo de caja ya fijado (se aplica al colocarla). */
    public static ItemStack withKind(ItemStack stack, Kind kind, boolean locked) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KIND.getName(), kind.getSerializedName());
        tag.putString(LOCKED.getName(), String.valueOf(locked));
        stack.getOrCreateTag().put("BlockStateTag", tag);
        return stack;
    }

    /** Nombre del objeto según el tipo guardado en su BlockStateTag. */
    public static Component nameOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockStateTag");
        String kind = tag == null ? "medical" : tag.getString(KIND.getName());
        if (kind.isEmpty()) kind = "medical";
        Component name = Component.translatable("block.aidirector.supply_crate." + kind);
        if (tag != null && "true".equals(tag.getString(LOCKED.getName()))) {
            return Component.translatable("block.aidirector.supply_crate.locked_name", name);
        }
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.aidirector.supply_crate.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyCrateBlockEntity(pos, state);
    }
}
