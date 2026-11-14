package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.VoltexaAPI;
import java.sql.SQLException;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VoltexaStudios bank plugin (v1). Ships the bank data model (versioned SQL
 * migrations in {@code resources/migrations}, applied through the core migration
 * runner) and the {@code /bank} command: accounts, deposit/withdraw, and
 * {@code /bank balance}. Declares {@code depend: [Voltexa]} and compiles
 * against {@code voltexa-api}; it ships no duplicate shared code and never
 * touches the JDBC driver, the database URL, or the pool directly — all
 * connections come through the core's {@code VoltexaAPI}.
 */
public final class BankPlugin extends JavaPlugin {

    private VoltexaAPI api;
    private BankService service;
    private BankConfig config;
    private boolean migrationsApplied;

    @Override
    public void onEnable() {
        api = getServer().getServicesManager().load(VoltexaAPI.class);
        if (api == null) {
            getLogger().severe("VoltexaAPI service not found - the Voltexa core must be enabled "
                    + "before Bank (plugin.yml declares depend: [Voltexa]).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Apply Bank's versioned migrations through the core migration runner
        // (Bank owns its tables; the core owns the pool and the
        // schema_migrations record). Paper 26.3 closes the plugin classloader
        // on disable, so a re-enable must not re-read the migration files from
        // the (closed) classloader - same pattern as the core's cached
        // migration list.
        try {
            if (!migrationsApplied) {
                api.migrations().applyMigrations("bank", BankPlugin.class);
                migrationsApplied = true;
            }
        } catch (SQLException e) {
            getLogger().severe("Bank failed to apply migrations: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Load Bank's config through the core facade (Bank ships no
        // config-loading code); a missing file falls back to the defaults.
        FileConfiguration fileConfig = api.config().forPlugin("Bank", getDataFolder());
        config = new BankConfig(fileConfig);
        service = new BankService(api.database(), config);
        registerCommand();
        long applied = api.migrations().appliedMigrations().stream()
                .filter(record -> record.owner().equals("bank")).count();
        // Log through the core facade (printf-style); Plugin.getLogger() is a
        // plain java.util.logging.Logger without format overloads.
        api.log().info("Bank %s enabled - VoltexaAPI %s resolved, %d bank migration(s) applied, %s currency",
                getDescription().getVersion(), api.version(), applied, config.currencyName());
    }

    @Override
    public void onDisable() {
        getLogger().info("Bank disabled.");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("bank");
        if (command != null) {
            BankCommand executor = new BankCommand(service, config, api.log());
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    /** The core API, available after a successful enable. */
    public VoltexaAPI api() {
        return api;
    }

    /** The bank domain service, available after a successful enable. */
    public BankService service() {
        return service;
    }

    /** The bank configuration, available after a successful enable. */
    public BankConfig config() {
        return config;
    }
}
