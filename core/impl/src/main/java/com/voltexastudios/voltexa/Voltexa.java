package com.voltexastudios.voltexa;

import com.voltexastudios.voltexa.api.ConfigService;
import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.LogService;
import com.voltexastudios.voltexa.api.MigrationService;
import com.voltexastudios.voltexa.api.VoltexaAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The concrete {@link VoltexaAPI} wired to the core's services. Registered
 * with the Bukkit Services API; feature plugins resolve it by type.
 */
public final class Voltexa implements VoltexaAPI {

    private final JavaPlugin plugin;
    private final DatabaseService database;
    private final ConfigService config;
    private final LogService log;
    private final MigrationService migrations;

    public Voltexa(JavaPlugin plugin, DatabaseService database, ConfigService config,
                   LogService log, MigrationService migrations) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
        this.log = log;
        this.migrations = migrations;
    }

    @Override
    public String version() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public DatabaseService database() {
        return database;
    }

    @Override
    public ConfigService config() {
        return config;
    }

    @Override
    public LogService log() {
        return log;
    }

    @Override
    public MigrationService migrations() {
        return migrations;
    }
}
