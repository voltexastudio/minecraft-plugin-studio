package com.voltexastudios.voltexa.config;

import com.voltexastudios.voltexa.api.ConfigService;
import com.voltexastudios.voltexa.log.DefaultLogService;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Config loading facade. The core owns {@code config.yml} in its data
 * folder; feature plugins load their own config files through
 * {@link #forPlugin} so config handling stays consistent across the studio.
 */
public final class DefaultConfigService implements ConfigService {

    private final JavaPlugin plugin;
    private final DefaultLogService log;
    private FileConfiguration coreConfig;

    public DefaultConfigService(JavaPlugin plugin, DefaultLogService log) {
        this.plugin = plugin;
        this.log = log;
    }

    @Override
    public FileConfiguration core() {
        if (coreConfig == null) {
            plugin.saveDefaultConfig();
            coreConfig = plugin.getConfig();
            log.debug("core config loaded from %s", new File(plugin.getDataFolder(), "config.yml"));
        }
        return coreConfig;
    }

    @Override
    public FileConfiguration forPlugin(String pluginName, File dataFolder) {
        File file = new File(dataFolder, pluginName + ".yml");
        FileConfiguration config = new YamlConfiguration();
        if (file.exists()) {
            try {
                config.load(file);
            } catch (Exception e) {
                log.warn("could not load %s: %s", file.getName(), e.getMessage());
            }
        }
        return config;
    }

    /** Persists a config to {@code file}, creating parent folders as needed. */
    public void save(FileConfiguration config, File file) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            config.save(file);
        } catch (Exception e) {
            log.warn("could not save %s: %s", file.getName(), e.getMessage());
        }
    }
}
