package com.igneko.aidirector.world.structure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Grandes zonas de interés (no son refugios): terminal portuaria, complejo del domo y base
 * avanzada del desierto. Miden hasta 256 bloques, el máximo que admite una estructura de Minecraft
 * (8 chunks a cada lado del chunk de origen).
 *
 * @param ground   altura local del bloque de suelo (el agua del puerto llega hasta la altura 14)
 * @param maxSlope desnivel máximo (percentil 90 − percentil 10) que se acepta; la zona se nivela
 *                 despejando hasta su altura y rellenando por debajo
 */
public enum Landmark implements StringRepresentable {
    PORT(256, 66, 216, PortBuilder.Q, 0),
    DOME(256, 44, 240, 8, 28),
    FOB(130, 44, 110, 8, 24);

    public static final Codec<Landmark> CODEC = StringRepresentable.fromEnum(Landmark::values);

    /** Lado de las piezas en que se trocea cada zona (solo se dibuja lo que toca a cada chunk). */
    private static final int TILE = 64;

    private final int width, height, depth, ground, maxSlope;

    Landmark(int width, int height, int depth, int ground, int maxSlope) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.ground = ground;
        this.maxSlope = maxSlope;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int ground() { return ground; }
    public int maxSlope() { return maxSlope; }

    /**
     * Piezas (cajas locales x0, y0, z0, x1, y1, z1 y cuánto pueden escribir por debajo). La base de
     * cada pieza es la cota a la que Minecraft aplana el terreno: en el puerto, la dársena se
     * excava hasta el fondo y el muelle se nivela a la altura del muelle.
     */
    public List<int[]> tiles() {
        List<int[]> out = new ArrayList<>();
        for (int x = 0; x < width; x += TILE) {
            int x1 = Math.min(width, x + TILE) - 1;
            if (this == PORT) {
                out.add(new int[]{x, 0, 0, x1, height - 1, PortBuilder.QZ - 1, 2});
                for (int z = PortBuilder.QZ; z < depth; z += TILE) {
                    out.add(new int[]{x, ground, z, x1, height - 1, Math.min(depth, z + TILE) - 1, ground + 2});
                }
            } else {
                for (int z = 0; z < depth; z += TILE) {
                    out.add(new int[]{x, ground, z, x1, height - 1, Math.min(depth, z + TILE) - 1, ground + 2});
                }
            }
        }
        return out;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Landmark byName(String name) {
        for (Landmark l : values()) if (l.getSerializedName().equals(name)) return l;
        return FOB;
    }
}
