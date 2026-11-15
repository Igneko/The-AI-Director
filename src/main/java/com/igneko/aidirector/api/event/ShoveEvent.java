package com.igneko.aidirector.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Se publica en el bus de Forge (servidor) por cada entidad alcanzada por un empujón.
 * Cancelarlo excluye a la entidad. Los Infectados Especiales (Fase 5) lo escuchan para
 * liberar a supervivientes atrapados.
 */
@Cancelable
public class ShoveEvent extends Event {
    private final ServerPlayer shover;
    private final LivingEntity target;
    private boolean stagger;

    public ShoveEvent(ServerPlayer shover, LivingEntity target, boolean stagger) {
        this.shover = shover;
        this.target = target;
        this.stagger = stagger;
    }

    public ServerPlayer getShover() { return shover; }
    public LivingEntity getTarget() { return target; }
    public boolean shouldStagger() { return stagger; }
    public void setStagger(boolean stagger) { this.stagger = stagger; }
}
