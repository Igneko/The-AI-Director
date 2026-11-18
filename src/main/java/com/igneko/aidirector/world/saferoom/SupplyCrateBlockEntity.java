package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.core.director.SupplyDirector;
import com.igneko.aidirector.registry.ModBlocks;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Recuerda qué supervivientes ya se han servido de la caja y si es una recompensa de Crescendo. */
public class SupplyCrateBlockEntity extends BlockEntity {

    private final Set<UUID> looted = new HashSet<>();
    /** Las cajas que empezaron cerradas guardan botín de nivel 2 (recompensa de un Crescendo). */
    private boolean reward;

    public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SUPPLY_CRATE_BE.get(), pos, state);
        reward = state.getValue(SupplyCrateBlock.LOCKED);
    }

    public void setReward(boolean reward) {
        this.reward = reward;
        setChanged();
    }

    void open(ServerPlayer player, SupplyCrateBlock.Kind kind) {
        if (!looted.add(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.aidirector.crate.taken").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        setChanged();
        ServerLevel level = player.serverLevel();
        List<ItemStack> loot = SupplyDirector.roll(kind, level, player, reward);
        BlockPos pos = getBlockPos();
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, stack);
            item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.12, 0.25, (level.random.nextDouble() - 0.5) * 0.12);
            item.setPickUpDelay(4);
            item.setTarget(player.getUUID());
            level.addFreshEntity(item);
        }
        level.playSound(null, pos, ModSounds.CRATE_OPEN.get(), SoundSource.BLOCKS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.01);
        player.displayClientMessage(Component.translatable("message.aidirector.crate.opened").withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (UUID id : looted) list.add(StringTag.valueOf(id.toString()));
        tag.put("Looted", list);
        tag.putBoolean("Reward", reward);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        looted.clear();
        for (Tag t : tag.getList("Looted", Tag.TAG_STRING)) {
            try {
                looted.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        reward = tag.getBoolean("Reward");
    }
}
