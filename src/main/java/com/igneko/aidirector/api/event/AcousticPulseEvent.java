package com.igneko.aidirector.api.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Pulso acústico lógico (disparo, alarma, pipe bomb...). Se publica en el bus de Forge en el servidor;
 * la atracción de infectados y el AI Director (agitación de Enhanced Hordes) lo consumen.
 */
public class AcousticPulseEvent extends Event {

    public enum Source { GUNSHOT, SUPPRESSED_GUNSHOT, EXPLOSION, ALARM, LURE }

    private final ServerLevel level;
    private final Vec3 origin;
    private final double radius;
    private final Source source;
    @Nullable private final Entity emitter;

    public AcousticPulseEvent(ServerLevel level, Vec3 origin, double radius, Source source, @Nullable Entity emitter) {
        this.level = level;
        this.origin = origin;
        this.radius = radius;
        this.source = source;
        this.emitter = emitter;
    }

    public ServerLevel getLevel() { return level; }
    public Vec3 getOrigin() { return origin; }
    public double getRadius() { return radius; }
    public Source getSource() { return source; }
    @Nullable public Entity getEmitter() { return emitter; }
}
