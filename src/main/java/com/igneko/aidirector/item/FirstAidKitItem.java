package com.igneko.aidirector.item;

import com.igneko.aidirector.core.survivor.MedicalService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Botiquín de Primeros Auxilios: uso canalizado de 5 s (2,5 s con adrenalina). Mantén clic derecho
 * para curarte, o apuntando a un compañero para curarle a él. Restaura el 80 % de la salud
 * permanente que falta y elimina el estado Blanco y Negro.
 */
public class FirstAidKitItem extends Item {

    public static final int USE_TICKS = 100;
    private static final double TEAMMATE_RANGE = 2.5;

    /** Paciente de cada sesión de curación en curso (solo servidor): sanador → paciente. */
    private static final Map<UUID, UUID> PATIENTS = new HashMap<>();

    public FirstAidKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Player patient = findTeammateInSight(player).filter(p -> MedicalService.canUseFirstAid(p) == MedicalService.Check.OK)
                .orElse(player);

        MedicalService.Check check = MedicalService.canUseFirstAid(patient);
        if (check != MedicalService.Check.OK) {
            if (!level.isClientSide()) player.displayClientMessage(MedicalService.describe(check), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            PATIENTS.put(player.getUUID(), patient.getUUID());
            if (patient != player) {
                player.displayClientMessage(Component.translatable("message.aidirector.first_aid.healing", patient.getDisplayName()), true);
                patient.displayClientMessage(Component.translatable("message.aidirector.first_aid.being_healed", player.getDisplayName()), true);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(user instanceof ServerPlayer healer)) return;
        Player patient = patientOf(healer);
        if (patient == null || !patient.isAlive() || (patient != healer && healer.distanceTo(patient) > TEAMMATE_RANGE + 1.0)
                || MedicalService.canUseFirstAid(patient) != MedicalService.Check.OK) {
            healer.stopUsingItem();
            PATIENTS.remove(healer.getUUID());
            return;
        }
        int elapsed = USE_TICKS - remaining;
        if (elapsed % 20 == 0) {
            level.playSound(null, healer.getX(), healer.getY(), healer.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        // La adrenalina duplica la velocidad de uso del botiquín.
        if (MedicalService.hasAdrenaline(healer) && elapsed >= USE_TICKS / 2) {
            complete(healer, stack);
            healer.stopUsingItem();
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof ServerPlayer healer) complete(healer, stack);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!level.isClientSide()) PATIENTS.remove(user.getUUID());
    }

    private static void complete(ServerPlayer healer, ItemStack stack) {
        Player patient = patientOf(healer);
        PATIENTS.remove(healer.getUUID());
        if (!(patient instanceof ServerPlayer sp) || MedicalService.canUseFirstAid(sp) != MedicalService.Check.OK) return;
        MedicalService.applyFirstAid(sp);
        healer.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.6f);
        if (sp != healer) {
            healer.displayClientMessage(Component.translatable("message.aidirector.first_aid.healed_other", sp.getDisplayName()), true);
            sp.displayClientMessage(Component.translatable("message.aidirector.first_aid.healed_by", healer.getDisplayName()), true);
        }
        if (!healer.getAbilities().instabuild) stack.shrink(1);
    }

    @Nullable
    private static Player patientOf(ServerPlayer healer) {
        UUID id = PATIENTS.get(healer.getUUID());
        return id == null ? null : healer.server.getPlayerList().getPlayer(id);
    }

    /** Compañero al que se apunta, a corta distancia y con línea de visión (ambos lados). */
    private static Optional<Player> findTeammateInSight(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(TEAMMATE_RANGE));
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player other : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(TEAMMATE_RANGE + 1),
                p -> p != player && p.isAlive() && !p.isSpectator())) {
            AABB box = other.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isEmpty() && !box.contains(eye)) continue;
            double dist = hit.map(eye::distanceToSqr).orElse(0.0);
            if (dist < bestDist && player.hasLineOfSight(other)) {
                best = other;
                bestDist = dist;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aidirector.first_aid_kit").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aidirector.first_aid_kit.other").withStyle(ChatFormatting.DARK_GRAY));
    }
}
