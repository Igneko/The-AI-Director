package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Eventos de sonido del mod (generado por tools: gen_sounds.py). Cada uno se define en sounds.json
 * reutilizando audio vanilla con otro tono, y puede sustituirse con un resource pack.
 */
public final class ModSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TheAIDirector.MOD_ID);

    public static final RegistryObject<SoundEvent> LURKER_AMBIENT = reg("lurker.ambient");
    public static final RegistryObject<SoundEvent> LURKER_POUNCE = reg("lurker.pounce");
    public static final RegistryObject<SoundEvent> LURKER_HURT = reg("lurker.hurt");
    public static final RegistryObject<SoundEvent> LURKER_DEATH = reg("lurker.death");
    public static final RegistryObject<SoundEvent> LURKER_SPAWN = reg("lurker.spawn");
    public static final RegistryObject<SoundEvent> STRANGLER_AMBIENT = reg("strangler.ambient");
    public static final RegistryObject<SoundEvent> STRANGLER_TONGUE = reg("strangler.tongue");
    public static final RegistryObject<SoundEvent> STRANGLER_HURT = reg("strangler.hurt");
    public static final RegistryObject<SoundEvent> STRANGLER_DEATH = reg("strangler.death");
    public static final RegistryObject<SoundEvent> STRANGLER_SPAWN = reg("strangler.spawn");
    public static final RegistryObject<SoundEvent> SMOKE_COUGH = reg("smoke.cough");
    public static final RegistryObject<SoundEvent> BLOATER_AMBIENT = reg("bloater.ambient");
    public static final RegistryObject<SoundEvent> BLOATER_VOMIT = reg("bloater.vomit");
    public static final RegistryObject<SoundEvent> BLOATER_EXPLODE = reg("bloater.explode");
    public static final RegistryObject<SoundEvent> BLOATER_HURT = reg("bloater.hurt");
    public static final RegistryObject<SoundEvent> BLOATER_SPAWN = reg("bloater.spawn");
    public static final RegistryObject<SoundEvent> CORRODER_AMBIENT = reg("corroder.ambient");
    public static final RegistryObject<SoundEvent> CORRODER_SPIT = reg("corroder.spit");
    public static final RegistryObject<SoundEvent> CORRODER_HURT = reg("corroder.hurt");
    public static final RegistryObject<SoundEvent> CORRODER_DEATH = reg("corroder.death");
    public static final RegistryObject<SoundEvent> CORRODER_SPAWN = reg("corroder.spawn");
    public static final RegistryObject<SoundEvent> ACID_SIZZLE = reg("acid.sizzle");
    public static final RegistryObject<SoundEvent> LEECH_AMBIENT = reg("leech.ambient");
    public static final RegistryObject<SoundEvent> LEECH_LEAP = reg("leech.leap");
    public static final RegistryObject<SoundEvent> LEECH_HURT = reg("leech.hurt");
    public static final RegistryObject<SoundEvent> LEECH_DEATH = reg("leech.death");
    public static final RegistryObject<SoundEvent> LEECH_SPAWN = reg("leech.spawn");
    public static final RegistryObject<SoundEvent> RAMMER_AMBIENT = reg("rammer.ambient");
    public static final RegistryObject<SoundEvent> RAMMER_ROAR = reg("rammer.roar");
    public static final RegistryObject<SoundEvent> RAMMER_IMPACT = reg("rammer.impact");
    public static final RegistryObject<SoundEvent> RAMMER_HURT = reg("rammer.hurt");
    public static final RegistryObject<SoundEvent> RAMMER_DEATH = reg("rammer.death");
    public static final RegistryObject<SoundEvent> RAMMER_SPAWN = reg("rammer.spawn");
    public static final RegistryObject<SoundEvent> WAILER_CRY = reg("wailer.cry");
    public static final RegistryObject<SoundEvent> WAILER_GROWL = reg("wailer.growl");
    public static final RegistryObject<SoundEvent> WAILER_RAGE = reg("wailer.rage");
    public static final RegistryObject<SoundEvent> WAILER_HURT = reg("wailer.hurt");
    public static final RegistryObject<SoundEvent> WAILER_DEATH = reg("wailer.death");
    public static final RegistryObject<SoundEvent> BEHEMOTH_AMBIENT = reg("behemoth.ambient");
    public static final RegistryObject<SoundEvent> BEHEMOTH_ROAR = reg("behemoth.roar");
    public static final RegistryObject<SoundEvent> BEHEMOTH_PUNCH = reg("behemoth.punch");
    public static final RegistryObject<SoundEvent> BEHEMOTH_THROW = reg("behemoth.throw");
    public static final RegistryObject<SoundEvent> BEHEMOTH_STEP = reg("behemoth.step");
    public static final RegistryObject<SoundEvent> BEHEMOTH_HURT = reg("behemoth.hurt");
    public static final RegistryObject<SoundEvent> BEHEMOTH_DEATH = reg("behemoth.death");
    public static final RegistryObject<SoundEvent> BEHEMOTH_SPAWN = reg("behemoth.spawn");
    public static final RegistryObject<SoundEvent> ROCK_SHATTER = reg("rock.shatter");
    public static final RegistryObject<SoundEvent> CHAINSAW_REV = reg("chainsaw.rev");
    public static final RegistryObject<SoundEvent> FRYING_PAN_CLANG = reg("frying_pan.clang");
    public static final RegistryObject<SoundEvent> MELEE_SWING = reg("melee.swing");
    public static final RegistryObject<SoundEvent> PIPEBOMB_THROW = reg("pipebomb.throw");
    public static final RegistryObject<SoundEvent> PIPEBOMB_BEEP = reg("pipebomb.beep");
    public static final RegistryObject<SoundEvent> PIPEBOMB_EXPLODE = reg("pipebomb.explode");
    public static final RegistryObject<SoundEvent> BILEJAR_THROW = reg("bilejar.throw");
    public static final RegistryObject<SoundEvent> BILEJAR_SHATTER = reg("bilejar.shatter");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TheAIDirector.MOD_ID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
