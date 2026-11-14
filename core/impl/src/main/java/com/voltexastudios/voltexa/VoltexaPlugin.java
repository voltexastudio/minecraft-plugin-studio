package com.voltexastudios.voltexa;

import com.voltexastudios.voltexa.api.ConfigService;
import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.LogService;
import com.voltexastudios.voltexa.api.Migration;
import com.voltexastudios.voltexa.api.MigrationService;
import com.voltexastudios.voltexa.api.VoltexaAPI;
import com.voltexastudios.voltexa.command.VoltexaCommand;
import com.voltexastudios.voltexa.config.DefaultConfigService;
import com.voltexastudios.voltexa.db.ConnectionPool;
import com.voltexastudios.voltexa.db.DefaultDatabaseService;
import com.voltexastudios.voltexa.db.H2FileProvider;
import com.voltexastudios.voltexa.log.DefaultLogService;
import com.voltexastudios.voltexa.migration.DefaultMigrationService;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Voltexa core plugin. Owns the shared infrastructure every VoltexaStudios
 * feature plugin builds on: the embedded H2 database (behind a JDBC
 * abstraction, file-based and zero-config), the versioned migration runner,
 * config loading, the logging facade, and the typed {@link VoltexaAPI} entry
 * point, registered through the Bukkit Services API. Feature plugins declare
 * {@code depend: [Voltexa]} and compile against the {@code voltexa-api}
 * artifact; they never ship duplicate shared code.
 */
public final class VoltexaPlugin extends JavaPlugin {

    private ConnectionPool pool;
    private DefaultLogService log;
    private DefaultConfigService config;
    private DefaultDatabaseService database;
    private DefaultMigrationService migrations;

    @Override
    public void onEnable() {
        log = new DefaultLogService(getLogger());
        config = new DefaultConfigService(this, log);
        database = new DefaultDatabaseService(createPool(), log);
        migrations = new DefaultMigrationService(database, log);

        if (!applyCoreMigrations()) {
            // Paper 26.3 closes the plugin classloader on disable, so stop
            // here: any further class load would fail on a closed jar.
            return;
        }

        Voltexa api = new Voltexa(this, database, config, log, migrations);
        registerServices(api);
        registerCommand();

        log.info("Voltexa %s ready - database %s, %d migration(s) applied",
                getVersion(), database.databaseProduct(), migrations.appliedMigrations().size());
    }

    @Override
    public void onDisable() {
        if (pool != null) {
            pool.close();
        }
        if (log != null) {
            log.info("Voltexa disabled.");
        }
    }

    private ConnectionPool createPool() {
        int size = config.core().getInt("database.pool-size", 5);
        String path = config.core().getString("database.path", "voltexa.mv.db");
        H2FileProvider provider = new H2FileProvider(new File(getDataFolder(), path), log);
        return new ConnectionPool(provider, size, log);
    }

    private boolean applyCoreMigrations() {
        try {
            migrations.applyMigrations("core", loadCoreMigrations());
            return true;
        } catch (SQLException e) {
            getLogger().severe("Voltexa failed to apply core migrations: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return false;
        } catch (IOException e) {
            getLogger().severe("Voltexa could not read core migration files: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private List<Migration> cachedCoreMigrations;

    private List<Migration> loadCoreMigrations() throws IOException {
        // Paper 26.3 closes the plugin classloader on disable, so the
        // parsed migration list is cached: a re-enable must not read the
        // migration files from the (closed) classloader again.
        if (cachedCoreMigrations != null) {
            return cachedCoreMigrations;
        }
        cachedCoreMigrations = MigrationFiles.loadMigrations(getClass().getClassLoader());
        return cachedCoreMigrations;
    }

    private void registerServices(Voltexa api) {
        // Versioned services: the typed, versioned interfaces are registered
        // at Normal priority; feature plugins resolve them by type.
        ServicesManager services = getServer().getServicesManager();
        services.register(VoltexaAPI.class, api, this, ServicePriority.Normal);
        services.register(DatabaseService.class, api.database(), this, ServicePriority.Normal);
        services.register(ConfigService.class, api.config(), this, ServicePriority.Normal);
        services.register(LogService.class, api.log(), this, ServicePriority.Normal);
        services.register(MigrationService.class, api.migrations(), this, ServicePriority.Normal);
    }

    private void registerCommand() {
        PluginCommand command = getCommand("voltexa");
        if (command != null) {
            VoltexaCommand executor = new VoltexaCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    /** Core plugin version from the descriptor. */
    public String getVersion() {
        return getDescription().getVersion();
    }

    public DefaultLogService log() {
        return log;
    }

    public DefaultConfigService config() {
        return config;
    }

    public DefaultDatabaseService database() {
        return database;
    }

    public DefaultMigrationService migrations() {
        return migrations;
    }
}
