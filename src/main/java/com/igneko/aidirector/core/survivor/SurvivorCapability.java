package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Adaptador Forge: adjunta {@link SurvivorHealthData} a cada jugador y lo persiste en NBT.
 */
public final class SurvivorCapability {

    public static final Capability<SurvivorHealthData> SURVIVOR = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(TheAIDirector.MOD_ID, "survivor_health");

    private SurvivorCapability() {}

    public static Optional<SurvivorHealthData> get(Player player) {
        return player.getCapability(SURVIVOR).resolve();
    }

    public static CompoundTag write(SurvivorHealthData d) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Perm", d.permHealth());
        tag.putFloat("Temp", d.tempHealth());
        tag.putInt("State", d.state().ordinal());
        tag.putInt("IncapCount", d.incapCount());
        tag.putFloat("Bleed", d.bleedHealth());
        tag.putInt("Adrenaline", d.adrenalineTicks());
        return tag;
    }

    public static void read(SurvivorHealthData d, CompoundTag tag) {
        if (!tag.contains("Perm")) return;
        d.restore(tag.getFloat("Perm"), tag.getFloat("Temp"), SurvivorState.byOrdinal(tag.getInt("State")),
                tag.getInt("IncapCount"), tag.getFloat("Bleed"), tag.getInt("Adrenaline"), 0f, false);
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final SurvivorHealthData data = new SurvivorHealthData();
        private final LazyOptional<SurvivorHealthData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return SURVIVOR.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return write(data);
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            read(data, tag);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
