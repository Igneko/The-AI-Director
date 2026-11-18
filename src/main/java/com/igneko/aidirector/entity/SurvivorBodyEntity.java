package com.igneko.aidirector.entity;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.DefibService;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Cuerpo de un superviviente muerto (especificación §5: "el cadáver queda en el suelo esperando un
 * Desfibrilador"). Muestra el aspecto del jugador tumbado y desaparece si este reaparece por su
 * cuenta o pasado el tiempo configurado.
 */
public class SurvivorBodyEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(SurvivorBodyEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(SurvivorBodyEntity.class,
            EntityDataSerializers.STRING);

    private int age;

    public SurvivorBodyEntity(EntityType<? extends SurvivorBodyEntity> type, Level level) {
        super(type, level);
    }

    public static SurvivorBodyEntity of(ServerPlayer player) {
        SurvivorBodyEntity body = new SurvivorBodyEntity(ModEntities.SURVIVOR_BODY.get(), player.level());
        body.entityData.set(OWNER, Optional.of(player.getUUID()));
        body.entityData.set(NAME, player.getGameProfile().getName());
        body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
        return body;
    }

    @Nullable
    public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public String ownerName() {
        return entityData.get(NAME);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, Optional.empty());
        entityData.define(NAME, "");
    }

    @Override
    public void tick() {
        super.tick();
        // Cae hasta el suelo y se queda quieto.
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0).multiply(0.9, 0.98, 0.9));
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level().isClientSide()) {
            if (random.nextInt(30) == 0) {
                level().addParticle(ParticleTypes.FALLING_DRIPSTONE_LAVA, getX() + random.nextGaussian() * 0.3, getY() + 0.2,
                        getZ() + random.nextGaussian() * 0.3, 0, 0, 0);
            }
            return;
        }
        age++;
        UUID id = owner();
        if (id == null || age > AIDirectorConfig.items().bodyMinutes() * 1200) {
            discard();
            return;
        }
        if (age % 20 == 0) {
            ServerPlayer player = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(id);
            // Si ya reapareció por su cuenta, el cuerpo sobra.
            if (player != null && !player.isDeadOrDying()) {
                discard();
                return;
            }
            DefibService.track(this);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(ModRegistries.DEFIBRILLATOR.get())) return InteractionResult.PASS;
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.translatable("message.aidirector.body.need_defib", ownerName())
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide()) DefibService.untrack(this);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(NAME, tag.getString("OwnerName"));
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID id = owner();
        if (id != null) tag.putUUID("Owner", id);
        tag.putString("OwnerName", ownerName());
        tag.putInt("Age", age);
    }
}
