package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.VoltexaAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VoltexaStudios bank plugin - placeholder. Feature work (accounts,
 * deposit/withdraw, {@code /bank} commands) lands in a follow-up issue.
 * This placeholder proves the feature-plugin wiring: it declares
 * {@code depend: [Voltexa]}, compiles against {@code voltexa-api}, and
 * resolves the core's typed API through the Bukkit Services API at load
 * time. It ships no duplicate shared code.
 */
public final class BankPlugin extends JavaPlugin {

    private VoltexaAPI api;

    @Override
    public void onEnable() {
        api = getServer().getServicesManager().load(VoltexaAPI.class);
        if (api == null) {
            getLogger().severe("VoltexaAPI service not found - the Voltexa core must be enabled "
                    + "before Bank (plugin.yml declares depend: [Voltexa]).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Log through the core facade (printf-style); Plugin.getLogger() is a
        // plain java.util.logging.Logger without format overloads.
        api.log().info("Bank %s enabled (placeholder) - VoltexaAPI %s resolved from core.",
                getDescription().getVersion(), api.version());
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
