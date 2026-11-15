package com.igneko.aidirector.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** Base de los arrojables: clic derecho para lanzarlos con un arco natural. */
public abstract class ThrowableItem extends Item {

    private final Supplier<SoundEvent> throwSound;

    protected ThrowableItem(Properties properties, Supplier<SoundEvent> throwSound) {
        super(properties);
        this.throwSound = throwSound;
    }

    protected abstract ThrowableProjectile createProjectile(Level level, Player thrower);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), throwSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        if (!level.isClientSide()) {
            ThrowableProjectile projectile = createProjectile(level, player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -8f, 1.05f, 0.5f);
            level.addFreshEntity(projectile);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
