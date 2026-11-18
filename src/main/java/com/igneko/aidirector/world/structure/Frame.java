package com.igneko.aidirector.world.structure;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marco local de un objeto (camión, remolcador, carpa...) dibujado sobre un {@link Canvas}.
 * <p>
 * El objeto se diseña siempre "mirando al norte": {@code a} avanza hacia delante (norte, +z del
 * lienzo), {@code b} hacia su derecha (este, +x) e {@code y} hacia arriba. El marco lo gira hacia
 * {@code facing} y gira también los estados con dirección.
 */
public final class Frame {

    private final Canvas c;
    private final int ox, oy, oz;
    private final int fx, fz, rx, rz;
    private final Rotation rotation;

    public Frame(Canvas c, int ox, int oy, int oz, Direction facing) {
        this.c = c;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        Direction right = facing.getClockWise();
        // En el lienzo +z es el norte, al revés que en el mundo.
        this.fx = facing.getStepX();
        this.fz = -facing.getStepZ();
        this.rx = right.getStepX();
        this.rz = -right.getStepZ();
        this.rotation = switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public int x(int a, int b) {
        return ox + a * fx + b * rx;
    }

    public int z(int a, int b) {
        return oz + a * fz + b * rz;
    }

    public Direction dir(Direction canonical) {
        return canonical.getAxis() == Direction.Axis.Y ? canonical : rotation.rotate(canonical);
    }

    public BlockState rot(BlockState s) {
        return s.rotate(rotation);
    }

    public void set(int a, int y, int b, BlockState s) {
        c.set(x(a, b), oy + y, z(a, b), rot(s));
    }

    public void set(int a, int y, int b, Palette p) {
        int x = x(a, b), z = z(a, b);
        c.set(x, oy + y, z, rot(p.pick(c.noise(x, oy + y, z))));
    }

    public void fill(int a1, int y1, int b1, int a2, int y2, int b2, BlockState s) {
        c.fill(x(a1, b1), oy + y1, z(a1, b1), x(a2, b2), oy + y2, z(a2, b2), rot(s));
    }

    public void fill(int a1, int y1, int b1, int a2, int y2, int b2, Palette p) {
        int xa = Math.min(x(a1, b1), x(a2, b2)), xb = Math.max(x(a1, b1), x(a2, b2));
        int za = Math.min(z(a1, b1), z(a2, b2)), zb = Math.max(z(a1, b1), z(a2, b2));
        if (!c.touches(xa, za, xb, zb)) return;
        for (int x = Math.max(xa, c.clipX0()); x <= Math.min(xb, c.clipX1()); x++) {
            for (int z = Math.max(za, c.clipZ0()); z <= Math.min(zb, c.clipZ1()); z++) {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                    c.set(x, oy + y, z, rot(p.pick(c.noise(x, oy + y, z))));
                }
            }
        }
    }

    public void air(int a1, int y1, int b1, int a2, int y2, int b2) {
        c.air(x(a1, b1), oy + y1, z(a1, b1), x(a2, b2), oy + y2, z(a2, b2));
    }

    public void line(int a1, int y1, int b1, int a2, int y2, int b2, BlockState s) {
        c.line(x(a1, b1), oy + y1, z(a1, b1), x(a2, b2), oy + y2, z(a2, b2), rot(s));
    }

    public void container(int a, int y, int b, BlockState s, ResourceLocation loot) {
        c.container(x(a, b), oy + y, z(a, b), rot(s), loot);
    }

    public boolean touches(int a1, int b1, int a2, int b2) {
        return c.touches(Math.min(x(a1, b1), x(a2, b2)), Math.min(z(a1, b1), z(a2, b2)),
                Math.max(x(a1, b1), x(a2, b2)), Math.max(z(a1, b1), z(a2, b2)));
    }

    public Canvas canvas() {
        return c;
    }

    public int worldY(int y) {
        return oy + y;
    }
}
