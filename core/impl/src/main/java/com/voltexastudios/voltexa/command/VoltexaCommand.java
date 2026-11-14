package com.voltexastudios.voltexa.command;

import com.voltexastudios.voltexa.VoltexaPlugin;
import com.voltexastudios.voltexa.db.ConnectionPool;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Handles {@code /voltexa}: {@code status} reports the core's health
 * (database, pool, applied migrations, config) and demonstrates the config
 * and logging facades; {@code version} reports the core version.
 */
public final class VoltexaCommand implements CommandExecutor, TabCompleter {

    private final VoltexaPlugin plugin;

    public VoltexaCommand(VoltexaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /voltexa <status|version>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender);
            case "version" -> sender.sendMessage("Voltexa " + plugin.getVersion());
            default -> sender.sendMessage("Unknown subcommand: " + args[0] + " (use status or version)");
        }
        return true;
    }

    private void status(CommandSender sender) {
        ConnectionPool pool = plugin.database().pool();
        int appliedMigrations = plugin.migrations().appliedMigrations().size();
        int configKeys = plugin.config().core().getKeys(false).size();
        sender.sendMessage("---- Voltexa " + plugin.getVersion() + " status ----");
        sender.sendMessage("database: " + plugin.database().databaseProduct() + " (file-based, zero-config)");
        sender.sendMessage("pool: " + pool.inUse() + "/" + pool.maxSize() + " connections in use");
        sender.sendMessage("migrations: " + appliedMigrations + " applied");
        sender.sendMessage("config: loaded (" + configKeys + " top-level keys)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("status", "version") : List.of();
    }
}
