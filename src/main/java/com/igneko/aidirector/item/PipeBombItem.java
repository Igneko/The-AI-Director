package com.igneko.aidirector.item;

import com.igneko.aidirector.entity.projectile.PipeBombEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/** Bomba casera: pita y parpadea, atrae a los comunes y estalla a los 5 s. */
public class PipeBombItem extends ThrowableItem {

    public PipeBombItem(Properties properties) {
        super(properties, ModSounds.PIPEBOMB_THROW);
    }

    @Override
    protected ThrowableProjectile createProjectile(Level level, Player thrower) {
        return new PipeBombEntity(level, thrower);
    }
}
