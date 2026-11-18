package com.igneko.aidirector.world.structure;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Paleta ponderada de bloques: da variedad (desgaste, musgo, grietas) de forma determinista. */
public final class Palette {

    private final BlockState[] states;
    private final float[] cumulative;

    private Palette(BlockState[] states, float[] weights) {
        this.states = states;
        this.cumulative = new float[weights.length];
        float total = 0;
        for (float w : weights) total += w;
        float acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i] / total;
            cumulative[i] = acc;
        }
    }

    /** Pesos iguales. */
    public static Palette of(Block... blocks) {
        BlockState[] s = new BlockState[blocks.length];
        float[] w = new float[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            s[i] = blocks[i].defaultBlockState();
            w[i] = 1;
        }
        return new Palette(s, w);
    }

    /** Pares (bloque, peso): {@code weighted(STONE, 3, COBBLESTONE, 1)}. */
    public static Palette weighted(Object... pairs) {
        int n = pairs.length / 2;
        BlockState[] s = new BlockState[n];
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            Object o = pairs[i * 2];
            s[i] = o instanceof Block b ? b.defaultBlockState() : (BlockState) o;
            w[i] = ((Number) pairs[i * 2 + 1]).floatValue();
        }
        return new Palette(s, w);
    }

    public static Palette single(BlockState state) {
        return new Palette(new BlockState[]{state}, new float[]{1});
    }

    public BlockState pick(float roll) {
        for (int i = 0; i < cumulative.length; i++) {
            if (roll < cumulative[i]) return states[i];
        }
        return states[states.length - 1];
    }
}
