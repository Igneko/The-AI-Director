package com.igneko.aidirector.compat;

import com.igneko.aidirector.core.StressManager;
import net.minecraft.world.entity.Mob;

// TODO: Importar la API de Improved Mobs cuando el .jar sea indexado
// import dev.coros.improvedmobs.api.DifficultyAPI;

public class ImprovedMobsCompat {

    public static void adjustMobDifficulty(Mob mob) {
        float currentStress = StressManager.getStress();
        
        // Si el jugador está bajo mucha presión (alta intensidad de escena), ajustamos la dificultad del mob[cite: 2]
        if (currentStress > 75.0f) {
            // TODO: Usar el método correspondiente de Improved Mobs para potenciar la IA o el daño
            // Ej: DifficultyAPI.increaseDifficulty(mob, 2.0f);
        }
    }
}