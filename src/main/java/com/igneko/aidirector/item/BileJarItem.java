package com.igneko.aidirector.item;

import com.igneko.aidirector.entity.projectile.BileJarEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/** Frasco de bilis: llama a la horda hacia donde se rompe (o contra quien recibe el impacto). */
public class BileJarItem extends ThrowableItem {

    public BileJarItem(Properties properties) {
        super(properties, ModSounds.BILEJAR_THROW);
    }

    @Override
    protected ThrowableProjectile createProjectile(Level level, Player thrower) {
        return new BileJarEntity(level, thrower);
    }
}
