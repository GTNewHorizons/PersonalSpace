package me.eigenraven.personalspace;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import net.minecraftforge.common.config.Configuration;

import me.eigenraven.personalspace.world.DimensionConfig;

public class Config {

    private static class Defaults {

        public static final String[] defaultPresets = new String[] { DimensionConfig.PRESET_UW_VOID,
                DimensionConfig.PRESET_UW_GARDEN, DimensionConfig.PRESET_UW_MINING, };

        public static final String[] allowedBlocks = new String[] { "minecraft:air", "minecraft:bedrock",
                "minecraft:stone", "minecraft:cobblestone", "minecraft:dirt", "minecraft:grass",
                "minecraft:double_stone_slab", "minecraft:netherrack" };

        public static final String[] allowedBiomes = new String[] { "Plains", "Ocean", "Desert", "Extreme Hills",
                "Forest", "Taiga", "Swampland", "River", "MushroomIsland", "Swampland", "Jungle", "Savanna", "Mesa" };

        public static final int firstDimensionId = 180;

        public static final boolean debugLogging = false;

        public static final boolean useBlockEventChecks = true;
    }

    private static class Categories {

        public static final String general = "general";
    }

    public static String[] defaultPresets = Arrays.copyOf(Defaults.defaultPresets, Defaults.defaultPresets.length);
    public static HashSet<String> allowedBlocks = new HashSet<>(Arrays.asList(Defaults.allowedBlocks));
    public static HashSet<String> allowedBiomes = new HashSet<>(Arrays.asList(Defaults.allowedBiomes));
    public static int firstDimensionId = Defaults.firstDimensionId;
    public static boolean debugLogging = Defaults.debugLogging;
    public static boolean useBlockEventChecks = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();

        defaultPresets = configuration.getStringList(
                Categories.general,
                "defaultPresets",
                Defaults.defaultPresets,
                "Default world configuration presets. Format: blockname*layers;blockname*layers;..., example preset: minecraft:bedrock;minecraft:dirt*3;minecraft:grass");

        allowedBlocks = new HashSet<>(
                Arrays.asList(
                        configuration.getStringList(
                                Categories.general,
                                "allowedBlocks",
                                Defaults.allowedBlocks,
                                "List of blocks allowed in the user-specified presets, keep in mind these are used in world generation, so will be available in infinite quantities for the player.")));

        allowedBiomes = Arrays
                .stream(
                        configuration.getStringList(
                                Categories.general,
                                "allowedBiomes",
                                Defaults.allowedBiomes,
                                "List of biomes allowed for the personal dimensions."))
                .map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));

        firstDimensionId = configuration.getInt(
                "firstDimensionId",
                Categories.general,
                Defaults.firstDimensionId,
                0,
                Integer.MAX_VALUE,
                "First dimension ID to use for newly generated worlds");

        debugLogging = configuration
                .getBoolean("debugLogging", Categories.general, Defaults.debugLogging, "Debug logging toggle");

        useBlockEventChecks = configuration.getBoolean(
                "useBlockEventChecks",
                Categories.general,
                Defaults.useBlockEventChecks,
                "Use fake Block break events to check for permissions, disable in case of broken event handlers");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
