package com.igneko.aidirector.world.structure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Las tres variantes de refugio y su huella (ancho x, alto y, fondo z en coordenadas locales).
 *
 * @param groundLevel altura local del suelo natural (el búnker está casi todo bajo tierra)
 * @param maxSlope    desnivel máximo del terreno que se acepta para construirlo
 */
public enum SafeRoomVariant implements StringRepresentable {
    BUNKER(25, 24, 34, 14, 12),
    HANGAR(21, 15, 34, 0, 9),
    CAMP(49, 18, 49, 0, 9);

    public static final Codec<SafeRoomVariant> CODEC = StringRepresentable.fromEnum(SafeRoomVariant::values);

    private final int width, height, depth, groundLevel, maxSlope;

    SafeRoomVariant(int width, int height, int depth, int groundLevel, int maxSlope) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.groundLevel = groundLevel;
        this.maxSlope = maxSlope;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int groundLevel() { return groundLevel; }
    public int maxSlope() { return maxSlope; }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SafeRoomVariant byName(String name) {
        for (SafeRoomVariant v : values()) if (v.getSerializedName().equals(name)) return v;
        return BUNKER;
    }
}
