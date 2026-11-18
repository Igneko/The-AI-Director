package com.igneko.aidirector.item;

import com.igneko.aidirector.entity.projectile.MolotovEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/** Cóctel molotov: zona de fuego de 15 s al romperse. */
public class MolotovItem extends ThrowableItem {

    public MolotovItem(Properties properties) {
        super(properties, ModSounds.MOLOTOV_THROW);
    }

    @Override
    protected ThrowableProjectile createProjectile(Level level, Player thrower) {
        return new MolotovEntity(level, thrower);
    }
}
