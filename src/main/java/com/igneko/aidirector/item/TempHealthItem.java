package com.igneko.aidirector.item;

import com.igneko.aidirector.core.survivor.MedicalService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Suministro médico temporal de consumo instantáneo (Píldoras, Adrenalina).
 * Clic derecho: consumir. Clic derecho sobre un compañero: entregárselo, como en L4D2.
 */
public class TempHealthItem extends Item {

    private final Consumer<ServerPlayer> effect;
    private final Supplier<SoundEvent> sound;
    private final float pitch;
    private final String tooltipKey;

    public TempHealthItem(Properties properties, Consumer<ServerPlayer> effect, Supplier<SoundEvent> sound,
                          float pitch, String tooltipKey) {
        super(properties);
        this.effect = effect;
        this.sound = sound;
        this.pitch = pitch;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MedicalService.Check check = MedicalService.canUseTempHealth(player);
        if (check != MedicalService.Check.OK) {
            if (!level.isClientSide()) player.displayClientMessage(MedicalService.describe(check), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            effect.accept(sp);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.get(), SoundSource.PLAYERS, 1.0f, pitch);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Entregar a un compañero: el objeto pasa a su inventario. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player giver, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player receiver)) return InteractionResult.PASS;
        if (giver.level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack gift = stack.copyWithCount(1);
        if (!receiver.getInventory().add(gift)) {
            giver.displayClientMessage(Component.translatable("message.aidirector.give.full", receiver.getDisplayName()), true);
            return InteractionResult.FAIL;
        }
        if (!giver.getAbilities().instabuild) stack.shrink(1);
        giver.displayClientMessage(Component.translatable("message.aidirector.give.sent", stack.getHoverName(), receiver.getDisplayName()), true);
        receiver.displayClientMessage(Component.translatable("message.aidirector.give.received", giver.getDisplayName(), gift.getHoverName()), true);
        giver.level().playSound(null, receiver.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aidirector.give").withStyle(ChatFormatting.DARK_GRAY));
    }
}
