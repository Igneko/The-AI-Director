package com.igneko.aidirector.compat;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class TaczEventListener {
    
    private final Random random = new Random();
    private final Map<UUID, Long> playerNoiseCooldowns = new HashMap<>();

    @SubscribeEvent
    public void onBulletFired(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        // Bypass nativo de Forge: Detectamos la bala de TACZ sin usar su API
        if (event.getEntity().getClass().getSimpleName().equals("EntityKineticBullet")) {
            
            if (event.getEntity() instanceof Projectile bullet && bullet.getOwner() instanceof Player shooter) {
                
                long currentTime = level.getGameTime();
                
                // com.igneko.aidirector.core.StressManager.addStress(shooter, 15.0f);

                if (playerNoiseCooldowns.containsKey(shooter.getUUID())) {
                    if (currentTime - playerNoiseCooldowns.get(shooter.getUUID()) < 40) {
                        return; 
                    }
                }
                playerNoiseCooldowns.put(shooter.getUUID(), currentTime);

                ItemStack gunStack = shooter.getMainHandItem();
                
                if (gunStack.getTag() != null) {
                    System.out.println("=== TACZ NBT DUMP ===");
                    System.out.println(gunStack.getTag().toString());
                    System.out.println("=====================");
                }

                double noiseRadius = getWeaponNoiseRadius(gunStack);

                AABB searchBox = shooter.getBoundingBox().inflate(noiseRadius, noiseRadius, noiseRadius);
                List<Monster> nearbyHostiles = level.getEntitiesOfClass(Monster.class, searchBox);

                for (Monster mob : nearbyHostiles) {
                    distributeSwarmAggro(mob, shooter, currentTime);
                }
            }
        }
    }

    private double getWeaponNoiseRadius(ItemStack gunStack) {
        CompoundTag nbt = gunStack.getTag();
        if (nbt == null) return 50.0;

        String gunId = nbt.contains("GunId") ? nbt.getString("GunId").toLowerCase() : "";
        
        boolean hasSilencer = false;
        if (nbt.contains("Attachment")) {
            CompoundTag attachments = nbt.getCompound("Attachment");
            hasSilencer = attachments.contains("Muzzle") && !attachments.getString("Muzzle").isEmpty();
        }

        if (gunId.contains("m249") || gunId.contains("rpk") || gunId.contains("awp") || gunId.contains("m700")) {
            return hasSilencer ? 10.0 : 120.0;
        } else {
            return hasSilencer ? 4.0 : 50.0;
        }
    }

    private void distributeSwarmAggro(Monster mob, Player newShooter, long currentTime) {
        long lastDistractionTime = mob.getPersistentData().getLong("LastAcousticDistraction");
        if (currentTime - lastDistractionTime < 60) return; 

        Player currentTarget = mob.getTarget() instanceof Player ? (Player) mob.getTarget() : null;
        boolean changedTarget = false;

        if (currentTarget == null) {
            mob.setTarget(newShooter);
            mob.getNavigation().moveTo(newShooter, 1.2D);
            changedTarget = true;
        } else if (currentTarget != newShooter) {
            double distToCurrent = mob.distanceToSqr(currentTarget);
            double distToNew = mob.distanceToSqr(newShooter);

            if (distToNew < (distToCurrent * 0.5)) {
                mob.setTarget(newShooter);
                mob.getNavigation().moveTo(newShooter, 1.2D);
                changedTarget = true;
            } else if (Math.abs(distToNew - distToCurrent) < 100.0) { 
                if (random.nextDouble() < 0.3) {
                    mob.setTarget(newShooter);
                    mob.getNavigation().moveTo(newShooter, 1.2D);
                    changedTarget = true;
                }
            }
        }

        if (changedTarget) {
            mob.getPersistentData().putLong("LastAcousticDistraction", currentTime);
        }
    }
}