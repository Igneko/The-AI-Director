package com.igneko.aidirector.item;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.DefibService;
import com.igneko.aidirector.entity.SurvivorBodyEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Desfibrilador: uso canalizado de 3 s sobre el cuerpo de un compañero muerto para devolverlo a la
 * vida con 50 de salud permanente (especificación §8). Moverse lejos o dejar de mirar el cuerpo
 * cancela la descarga.
 */
public class DefibrillatorItem extends Item {

    private static final double REACH = 3.0;

    public DefibrillatorItem(Properties properties) {
        super(properties);
    }

    /** Cuerpo al que apunta el jugador (o el más cercano a sus pies si no apunta a ninguno). */
    @Nullable
    public static SurvivorBodyEntity findBody(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(REACH));
        List<SurvivorBodyEntity> bodies = player.level().getEntitiesOfClass(SurvivorBodyEntity.class,
                player.getBoundingBox().inflate(REACH), SurvivorBodyEntity::isAlive);
        SurvivorBodyEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (SurvivorBodyEntity body : bodies) {
            AABB box = body.getBoundingBox().inflate(0.35);
            Optional<Vec3> hit = box.clip(eye, end);
            double d = hit.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (d < bestDist) {
                bestDist = d;
                best = body;
            }
        }
        if (best != null) return best;
        for (SurvivorBodyEntity body : bodies) {
            double d = body.distanceToSqr(player);
            if (d < 1.8 * 1.8 && d < bestDist) {
                bestDist = d;
                best = body;
            }
        }
        return best;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SurvivorBodyEntity body = findBody(player);
        if (body == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.aidirector.defib.no_body").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DEFIB_CHARGE.get(), SoundSource.PLAYERS, 1f, 1f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(user instanceof Player player) || remaining % 5 != 0) return;
        if (findBody(player) == null) player.stopUsingItem();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide() || !(user instanceof ServerPlayer player)) return stack;
        SurvivorBodyEntity body = findBody(player);
        UUID owner = body == null ? null : body.owner();
        ServerPlayer dead = owner == null ? null : player.server.getPlayerList().getPlayer(owner);
        if (body == null || dead == null) {
            player.displayClientMessage(Component.translatable("message.aidirector.defib.no_response").withStyle(ChatFormatting.RED), true);
            return stack;
        }
        if (DefibService.revive(dead, (ServerLevel) level, body.position(), body.getYRot(), player)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
        } else {
            player.displayClientMessage(Component.translatable("message.aidirector.defib.no_response").withStyle(ChatFormatting.RED), true);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return AIDirectorConfig.items().defibTicks();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aidirector.defibrillator").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aidirector.defibrillator.desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
