package com.voltexastudios.voltexa.api;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration access for feature plugins. The core owns its own
 * {@code config.yml}; features keep their own config files in their own data
 * folders, loaded through this facade so config handling stays consistent
 * across the studio.
 */
public interface ConfigService {

    /** The core plugin's configuration (defaults from the core's {@code config.yml}). */
    FileConfiguration core();

    /**
     * Loads the config file for a feature plugin, or returns an empty config
     * when the file does not exist yet.
     *
     * @param pluginName feature plugin name, used as the file name stem
     * @param dataFolder the feature plugin's data folder
     */
    FileConfiguration forPlugin(String pluginName, File dataFolder);
}
