package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Modo de la sirena: duración fija o interruptor (sigue hasta que alguien la apague). */
public class CrescendoAlarmBlockEntity extends BlockEntity {

    /** -1 = duración de la configuración; 0 = interruptor; resto = segundos. */
    private static final int[] MODES = {-1, 30, 60, 90, 120, 180, 0};

    private int mode;

    public CrescendoAlarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CRESCENDO_ALARM_BE.get(), pos, state);
    }

    public void setDuration(int seconds) {
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == seconds) mode = i;
        }
        setChanged();
    }

    void cycleMode() {
        mode = (mode + 1) % MODES.length;
        setChanged();
    }

    boolean isSwitchMode() {
        return MODES[mode] == 0;
    }

    /** Duración en segundos; en modo interruptor, un tope de seguridad de 30 minutos. */
    public int durationSeconds() {
        int m = MODES[mode];
        if (m < 0) return AIDirectorConfig.safeRoom().crescendoSeconds();
        return m == 0 ? 1800 : m;
    }

    Component describeMode() {
        int m = MODES[mode];
        if (m == 0) return Component.translatable("message.aidirector.alarm.mode.switch");
        return Component.translatable("message.aidirector.alarm.mode.seconds", durationSeconds());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Mode", mode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = Math.floorMod(tag.getInt("Mode"), MODES.length);
    }
}
