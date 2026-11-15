package com.igneko.aidirector.core;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.igneko.aidirector.core.survivor.MedicalService;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorRules;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommandRegistry {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("aidirector")
            .requires(source -> source.hasPermission(2)) // Requiere permisos de OP/Admin
            
            // Subcomando: /aidirector info
            .then(Commands.literal("info").executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayerOrException();
                
                long secondsLeft = StressManager.getSecondsLeft(player);
                long minutes = secondsLeft / 60;
                long seconds = secondsLeft % 60;

                float stress = com.igneko.aidirector.core.director.IntensityTracker.get(player);
                source.sendSuccess(() -> Component.literal(
                    "§e[AI Director] §fTiempo estimado para horda: §c" + minutes + "m " + seconds + "s"
                    + " §f| Estrés: §c" + Math.round(stress * 100) + "%"
                ), false);
                
                return 1;
            }))
            
            // Subcomando: /aidirector force
            // Genera la horda al instante (funciona también en creativo). Opcional: número de infectados.
            .then(Commands.literal("force")
                .executes(context -> forceHorde(context.getSource(), AIDirectorConfig.horde().size()))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 300))
                    .executes(context -> forceHorde(context.getSource(), IntegerArgumentType.getInteger(context, "count")))))
            
            // Subcomando: /aidirector reset
            .then(Commands.literal("reset").executes(context -> {
                CommandSourceStack source = context.getSource();
                ServerPlayer player = source.getPlayerOrException();
                
                StressManager.resetTimer(player);
                source.sendSuccess(() -> Component.literal("§e[AI Director] §fReloj reseteado a 20 minutos."), false);
                
                return 1;
            }))

            // Subcomandos de depuración del sistema de salud: /aidirector survivor <acción> [jugador]
            .then(Commands.literal("survivor")
                .then(survivorAction("info", (p, d) -> {}))
                .then(survivorAction("pills", (p, d) -> MedicalService.applyPainPills(p)))
                .then(survivorAction("adrenaline", (p, d) -> MedicalService.applyAdrenaline(p)))
                .then(survivorAction("firstaid", (p, d) -> MedicalService.applyFirstAid(p)))
                .then(survivorAction("incap", (p, d) -> {
                    SurvivorHealthData.Outcome o = d.forceIncapacitate(AIDirectorConfig.rules());
                    if (o == SurvivorHealthData.Outcome.KILLED) SurvivorService.killForReal(p);
                    else SurvivorService.onOutcome(p, d, o);
                }))
                .then(survivorAction("revive", (p, d) -> d.completeRevive(AIDirectorConfig.rules())))
                .then(survivorAction("reset", (p, d) -> d.reset(AIDirectorConfig.rules())))
                .then(Commands.literal("set")
                    .then(Commands.argument("perm", FloatArgumentType.floatArg(1, 100))
                        .then(Commands.argument("temp", FloatArgumentType.floatArg(0, 100))
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                float perm = FloatArgumentType.getFloat(ctx, "perm");
                                float temp = Math.min(FloatArgumentType.getFloat(ctx, "temp"), 100 - perm);
                                return runSurvivor(ctx, p, (pl, d) -> d.restore(perm, temp, d.state(), d.incapCount(),
                                        d.bleedHealth(), d.adrenalineTicks(), 0f, false));
                            })))))
        );
    }

    private static int forceHorde(CommandSourceStack source, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int spawned = EnhancedHordesCompat.triggerPanicEvent(player.serverLevel(), player.blockPosition(), count, player);
        StressManager.resetTimer(player);
        int area = AIDirectorConfig.horde().areaSize();
        source.sendSuccess(() -> Component.literal(String.format(
            "§e[AI Director] §fHorda forzada: §c%d§f/%d infectados en un área de %dx%d bloques.",
            spawned, count, area, area)), false);
        return spawned;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> survivorAction(
            String name, BiConsumer<ServerPlayer, SurvivorHealthData> action) {
        return Commands.literal(name)
            .executes(ctx -> runSurvivor(ctx, ctx.getSource().getPlayerOrException(), action))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> runSurvivor(ctx, EntityArgument.getPlayer(ctx, "target"), action)));
    }

    private static int runSurvivor(CommandContext<CommandSourceStack> ctx, ServerPlayer target,
                                   BiConsumer<ServerPlayer, SurvivorHealthData> action) throws CommandSyntaxException {
        SurvivorHealthData d = SurvivorCapability.get(target).orElse(null);
        if (d == null) return 0;
        action.accept(target, d);
        if (!d.isDead()) {
            SurvivorService.applyEffects(target, d);
            SurvivorService.sync(target, d);
        }
        SurvivorRules r = AIDirectorConfig.rules();
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
            "§e[AI Director] §f%s: §a%.1f perm §7+ %.1f temp §f| %s | caídas %d/%d%s | velocidad x%.2f",
            target.getGameProfile().getName(), d.permHealth(), d.tempHealth(), d.state(), d.incapCount(),
            r.maxIncaps(), d.isIncapacitated() ? String.format(" | sangrado %.0f", d.bleedHealth()) : "",
            d.speedMultiplier(r))), false);
        return 1;
    }
}