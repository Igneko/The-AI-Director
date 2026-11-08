🧠 The AI Director - Minecraft Forge Mod

The AI Director is a dynamic difficulty and pacing mod for Minecraft Forge 1.20.1, heavily inspired by the artificial intelligence systems found in games like Left 4 Dead. Instead of adding new monsters directly, this mod acts as an invisible "brain" that constantly monitors the player to create a dynamic gameplay rhythm, alternating between moments of high tension and calm recovery.

⚙️ Core Mechanics

📈 The Stress Meter

The core of the mod is the internal Stress Meter. The Director constantly calculates how pressured the player is in real-time.

Stress Increases When:

The player takes damage.

The player is surrounded by multiple hostiles.

The player makes excessive noise (e.g., firing guns).

Stress Decreases When:

The player is safe with full health.

The player stays in well-lit areas or established bases.

The player goes a significant amount of time without entering combat.

When stress reaches critical thresholds, the Director orchestrates events (like spawning a horde) and then forces a "cooldown period" to let the player recover.

🤝 Mod Synergy & Integrations

The AI Director is built to communicate with other community mods to enhance the survival experience:

[TACZ (Timeless and Classics Zero)]: The Director listens for gunshots to increase player stress based on noise and action.

[Enhanced Hordes]: The Director takes control of the spawning logic, deciding exactly WHEN and WHERE a horde spawns based on stress, overriding random timers.

[Improved Mobs]: The Director dynamically scales the difficulty and block-breaking capabilities of monsters depending on the required scene intensity.

🛠️ For Modders: Source Installation

This code follows the standard Minecraft Forge installation methodology. It applies patches to the vanilla MCP source code to grant access to necessary data and functions.

Setup Process

1. Clone the repository:

git clone https://github.com/Igneko/The-AI-Director.git


2. IDE Setup:

For Visual Studio Code (Recommended for this project):
Open the terminal in the project root and run:

./gradlew genVSCodeRuns


(Note: You will need to manually place the compiled .jar files of TACZ, Enhanced Hordes, and Improved Mobs in a /libs folder at the root of the project for the environment to compile successfully).

For IntelliJ IDEA:

Open IDEA and import the project by selecting the build.gradle file.

Run the following command in the terminal: ./gradlew genIntellijRuns

Refresh the Gradle Project in IDEA if required.

For Eclipse:

Run the following command: ./gradlew genEclipseRuns

Open Eclipse, Import > Existing Gradle Project > Select Folder.

Troubleshooting IDEs

If at any point you are missing libraries in your IDE, or you've run into problems, you can run:

./gradlew --refresh-dependencies (To refresh the local cache).

./gradlew clean (To reset everything, this does not affect your code), and then start the process again.

Mapping Names

By default, the MDK is configured to use the official mapping names from Mojang for methods and fields in the Minecraft codebase. These names are covered by a specific license (see MCPConfig Mojang.md).

📚 Additional Resources

Community Documentation: Minecraft Forge Docs 1.20.1

LexManos' Install Video: YouTube Tutorial

Forge Forums: forums.minecraftforge.net

Forge Discord: discord.minecraftforge.net

Created by Igneko.
