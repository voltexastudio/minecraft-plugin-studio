package com.voltxesa.hellovoltexa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VoltexaStudios studio skeleton plugin.
 *
 * <p>First increment: prove the build + load pipeline end to end. Provides the
 * {@code /voltexa} smoke command used by the QA pass (see
 * {@code docs/plugins/hello-voltexa/TESTPLAN.md}).
 */
public final class HelloVoltexaPlugin extends JavaPlugin {

    private static final String SMOKE_MESSAGE = "Hello from VoltexaStudios! The studio skeleton is alive.";

    @Override
    public void onEnable() {
        PluginCommand command = getCommand("voltexa");
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
     * Executes the {@code /voltexa} smoke command.
     *
     * @return always {@code true} — the command takes no arguments
     */
    private boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(SMOKE_MESSAGE);
        return true;
    }
}
