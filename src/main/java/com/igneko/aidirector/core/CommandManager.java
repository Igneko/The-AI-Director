package com.igneko.aidirector.core;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID)
public class CommandManager {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("aidirector")
            .then(Commands.literal("stress")
                .executes(context -> {
                    float currentStress = StressManager.getStress();
                    // Redondea a 1 decimal
                    String formattedStress = String.format("%.1f", currentStress); 
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§e[The AI Director] Nivel de estrés actual: §c" + formattedStress + " / 100.0"), false);
                    return 1;
                })
            )
        );
    }
}