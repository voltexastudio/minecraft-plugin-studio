package com.voltexastudios.hellovoltexa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VoltexaStudios studio skeleton plugin.
 *
 * <p>First increment: prove the build + load pipeline end to end. Provides the
 * {@code /hellovoltexa} smoke command used by the QA pass (see
 * {@code docs/plugins/hello-voltexa/TESTPLAN.md}). The {@code /voltexa}
 * namespace belongs to the Voltexa core plugin.
 */
public final class HelloVoltexaPlugin extends JavaPlugin {

    private static final String SMOKE_MESSAGE = "Hello from VoltexaStudios! The studio skeleton is alive.";

    @Override
    public void onEnable() {
        PluginCommand command = getCommand("hellovoltexa");
        if (command != null) {
            command.setExecutor(this::onCommand);
        }
        getLogger().info("HelloVoltexa enabled - studio skeleton is alive.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HelloVoltexa disabled.");
    }

    /**
     * Executes the {@code /hellovoltexa} smoke command.
     *
     * <p>Public because {@code JavaPlugin} implements {@code CommandExecutor}, whose
     * {@code onCommand} is a {@code public default} method in the current Paper API.
     *
     * @return always {@code true} — the command takes no arguments
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(SMOKE_MESSAGE);
        return true;
    }
}
