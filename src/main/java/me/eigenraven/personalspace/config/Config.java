package me.eigenraven.personalspace.config;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;

import me.eigenraven.personalspace.PersonalSpaceMod;
import me.eigenraven.personalspace.world.DimensionConfig;

public class Config {

    private static class Defaults {

        public static final String[] defaultPresets = new String[] { DimensionConfig.PRESET_UW_VOID,
                DimensionConfig.PRESET_UW_GARDEN, DimensionConfig.PRESET_UW_MINING, };

        public static final String[] allowedBlocks = new String[] { "minecraft:air", "minecraft:bedrock",
                "minecraft:stone", "minecraft:cobblestone", "minecraft:dirt", "minecraft:grass",
                "minecraft:double_stone_slab", "minecraft:netherrack" };

        public static final String[] allowedBoundaryBlocks = new String[] { "minecraft:wool:0-15" };

        public static final String[] allowedGapBlocks = new String[] { "minecraft:wool:0-15" };

        public static final String[] allowedCenterBlocks = new String[] { "minecraft:wool:0-15" };

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
    public static HashSet<String> allowedBoundaryBlocks = new HashSet<>(Arrays.asList(Defaults.allowedBoundaryBlocks));
    public static HashSet<String> allowedGapBlocks = new HashSet<>(Arrays.asList(Defaults.allowedGapBlocks));
    public static HashSet<String> allowedCenterBlocks = new HashSet<>(Arrays.asList(Defaults.allowedCenterBlocks));
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

        allowedBoundaryBlocks = new HashSet<>(
                Arrays.asList(
                        configuration.getStringList(
                                Categories.general,
                                "allowedBoundaryBlocks",
                                Defaults.allowedBoundaryBlocks,
                                "Allowed boundary blocks with meta ranges. Format: modid:block:damage  damage: 0, 0-12, !5, 0-15,!3. Example: minecraft:stone:0-6")));

        allowedGapBlocks = new HashSet<>(
                Arrays.asList(
                        configuration.getStringList(
                                Categories.general,
                                "allowedGapBlocks",
                                Defaults.allowedGapBlocks,
                                "Allowed gap blocks with meta ranges. Format: modid:block:damage  damage: 0, 0-12, !5, 0-15,!3. Example: minecraft:wool:0-15")));

        allowedCenterBlocks = new HashSet<>(
                Arrays.asList(
                        configuration.getStringList(
                                Categories.general,
                                "allowedCenterBlocks",
                                Defaults.allowedCenterBlocks,
                                "Allowed center marker blocks with meta ranges. Format: modid:block:damage  damage: 0, 0-12, !5, 0-15,!3. Example: minecraft:wool:0-15")));

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

    public static void validateBlocks() {
        filterInvalidBlocks(allowedBlocks, false);
        filterInvalidBlocks(allowedBoundaryBlocks, true);
        filterInvalidBlocks(allowedGapBlocks, true);
        filterInvalidBlocks(allowedCenterBlocks, true);
    }

    private static void filterInvalidBlocks(HashSet<String> blockSet, boolean hasMetaSpec) {
        Iterator<String> it = blockSet.iterator();
        while (it.hasNext()) {
            String entry = it.next();
            String blockName;
            if (hasMetaSpec) {
                AllowedBoundaryBlock parsed = null;
                try {
                    parsed = AllowedBoundaryBlock.parse(entry);
                } catch (Exception ignored) {}
                if (parsed == null) {
                    PersonalSpaceMod.LOG.warn("Removing invalid block config entry: {}", entry);
                    it.remove();
                    continue;
                }
                blockName = parsed.blockName();
            } else {
                blockName = entry;
            }
            Block block = DimensionConfig.blockFromString(blockName);
            if (block == null) {
                PersonalSpaceMod.LOG.warn("Removing non-existent block from config: {}", entry);
                it.remove();
            }
        }
    }
}
