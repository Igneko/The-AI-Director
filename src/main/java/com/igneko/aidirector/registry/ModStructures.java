package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.world.structure.SafeRoomPiece;
import com.igneko.aidirector.world.structure.SafeRoomStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Tipos de estructura y de pieza de los refugios (la generación se define en data/aidirector/worldgen). */
public final class ModStructures {

    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, TheAIDirector.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, TheAIDirector.MOD_ID);

    public static final RegistryObject<StructureType<SafeRoomStructure>> SAFE_ROOM = TYPES.register("safe_room",
            () -> () -> SafeRoomStructure.CODEC);
    public static final RegistryObject<StructurePieceType> SAFE_ROOM_PIECE = PIECES.register("safe_room",
            () -> SafeRoomPiece::new);

    private ModStructures() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
