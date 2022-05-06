package xyz.kubasz.personalspace;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Arrays;

public class Config {

    private static class Defaults {
        public static final String[] allowedPresets = new String[]{
            //
        };
    }

    private static class Categories {
        public static final String general = "general";
    }

    public static String[] allowedPresets = Arrays.copyOf(Defaults.allowedPresets, Defaults.allowedPresets.length);

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();

        Property presetsProperty = configuration.get(Categories.general, "allowedPresets", Defaults.allowedPresets,
            "Allowed world configuration presets. Roughly follows superflat preset strings, with the following additions:" +
                "");
        allowedPresets = presetsProperty.getStringList();

        if(configuration.hasChanged()) {
            configuration.save();
        }
    }
}
