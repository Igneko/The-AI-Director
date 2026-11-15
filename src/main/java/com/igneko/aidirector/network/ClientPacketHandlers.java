package com.igneko.aidirector.network;

import com.igneko.aidirector.client.ClientCombatState;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Manejadores exclusivos del cliente. Nunca se cargan en un servidor dedicado. */
@OnlyIn(Dist.CLIENT)
final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    static void handleSurvivorSync(SyncSurvivorPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(msg.entityId());
        if (!(entity instanceof Player player)) return;

        SurvivorCapability.get(player).ifPresent(d -> d.restore(msg.perm(), msg.temp(), msg.state(),
                msg.incapCount(), msg.bleed(), msg.adrenalineTicks(), msg.reviveProgress(), msg.beingRevived()));

        // La pose se calcula localmente en cada tick: hay que forzarla también en el cliente.
        player.setForcedPose(msg.state() == SurvivorState.INCAPACITATED ? Pose.SWIMMING : null);
    }

    static void handleShoveCooldown(ShoveCooldownPacket msg) {
        ClientCombatState.onShove(msg.cooldownTicks(), msg.fatigued());
    }
}
