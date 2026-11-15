package com.igneko.aidirector.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Estado de combate visible en el cliente (solo presentación; el servidor decide). */
@OnlyIn(Dist.CLIENT)
public final class ClientCombatState {

    private static long cooldownStart;
    private static int cooldownTicks;
    private static boolean fatigued;

    private ClientCombatState() {}

    public static void onShove(int ticks, boolean isFatigued) {
        Minecraft mc = Minecraft.getInstance();
        cooldownStart = mc.level != null ? mc.level.getGameTime() : 0;
        cooldownTicks = ticks;
        fatigued = isFatigued;
    }

    /** Fracción restante del enfriamiento [0, 1]. */
    public static float remaining(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || cooldownTicks <= 0) return 0f;
        float elapsed = (mc.level.getGameTime() - cooldownStart) + partialTick;
        return Math.max(0f, 1f - elapsed / cooldownTicks);
    }

    public static boolean isFatigued() {
        return fatigued;
    }
}
