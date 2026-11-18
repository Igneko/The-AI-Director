package com.igneko.aidirector.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Arma cuerpo a cuerpo. Daño, cadencia, alcance extra y durabilidad propios; las anchas barren a
 * varios enemigos. Hacen su daño normal a cualquier mob, infectados incluidos.
 */
public class MeleeWeaponItem extends Item {

    /** Estadísticas de cada arma (daño total en puntos vanilla; velocidad relativa a 4 golpes/s). */
    public record Stats(float damage, float speed, float reach, int durability, boolean sweep) {}

    private static final UUID REACH_ID = UUID.fromString("e4c1b9a2-7f3d-4c6e-9a1b-2d3c4e5f6a7b");

    private final Stats stats;
    private final Multimap<Attribute, AttributeModifier> modifiers;

    public MeleeWeaponItem(Stats stats) {
        super(new Item.Properties().durability(stats.durability()));
        this.stats = stats;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                stats.damage() - 1.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                stats.speed(), AttributeModifier.Operation.ADDITION));
        if (stats.reach() != 0f) {
            b.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_ID, "Weapon reach",
                    stats.reach(), AttributeModifier.Operation.ADDITION));
        }
        this.modifiers = b.build();
    }

    public Stats stats() {
        return stats;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        Level level = attacker.level();
        if (!level.isClientSide()) {
            if (this == com.igneko.aidirector.registry.ModWeapons.FRYING_PAN.get()) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.FRYING_PAN_CLANG.get(), SoundSource.PLAYERS, 1f, 1f);
            }
        }
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return (stats.sweep() && action == ToolActions.SWORD_SWEEP) || super.canPerformAction(stack, action);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.WEAPON || enchantment.category == EnchantmentCategory.BREAKABLE;
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (stats.sweep()) tooltip.add(Component.translatable("tooltip.aidirector.melee.sweep").withStyle(ChatFormatting.DARK_GREEN));
    }
}
