package com.igneko.aidirector.client.almanac;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Abre el almanaque. Vive aparte de {@code AlmanacBookItem} para que el servidor dedicado nunca
 * tenga que cargar (ni verificar) clases de pantalla del cliente.
 */
@OnlyIn(Dist.CLIENT)
public final class AlmanacOpener {

    private AlmanacOpener() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new AlmanacScreen());
    }
}
