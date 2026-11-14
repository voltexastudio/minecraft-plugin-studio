package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.VoltexaAPI;
import java.sql.SQLException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VoltexaStudios bank plugin - contract stage. Ships the bank data model
 * (versioned SQL migrations in {@code resources/migrations}, applied through
 * the core migration runner) and the {@code /bank} command namespace.
 * Declares {@code depend: [Voltexa]} and compiles against {@code voltexa-api};
 * it ships no duplicate shared code and no domain logic - feature work
 * (accounts, deposit/withdraw, {@code /bank} executors) lands in Bank v1.
 */
public final class BankPlugin extends JavaPlugin {

    private VoltexaAPI api;
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
        // Contract bootstrap: apply Bank's versioned migrations through the
        // core migration runner (Bank owns its tables; the core owns the
        // pool and the schema_migrations record). Paper 26.3 closes the
        // plugin classloader on disable, so a re-enable must not re-read
        // the migration files from the (closed) classloader - same pattern
        // as the core's cached migration list.
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
        long applied = api.migrations().appliedMigrations().stream()
                .filter(record -> record.owner().equals("bank")).count();
        // Log through the core facade (printf-style); Plugin.getLogger() is a
        // plain java.util.logging.Logger without format overloads.
        api.log().info("Bank %s enabled (contract) - VoltexaAPI %s resolved, %d bank migration(s) applied.",
                getDescription().getVersion(), api.version(), applied);
    }

    @Override
    public void onDisable() {
        getLogger().info("Bank disabled.");
    }

    /** The core API, available after a successful enable. */
    public VoltexaAPI api() {
        return api;
    }
}
