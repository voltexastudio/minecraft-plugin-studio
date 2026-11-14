package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.LogService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles {@code /bank}: {@code balance} (the caller's, or another player's
 * for an operator), {@code deposit}/{@code withdraw} (self-only), and
 * {@code accounts} (operator). No permission nodes in v1 — the permission
 * system is deferred to core v2, so OP checks ({@code isOp()}) stand in. Each
 * error case is a distinct message.
 */
public final class BankCommand implements CommandExecutor, TabCompleter {

    private final BankService service;
    private final BankConfig config;
    private final LogService log;

    public BankCommand(BankService service, BankConfig config, LogService log) {
        this.service = service;
        this.config = config;
        this.log = log;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                // /bank is an alias for /bank balance (the caller's account).
                ownBalance(sender);
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "balance" -> {
                    if (args.length > 1) {
                        otherBalance(sender, args[1]);
                    } else {
                        ownBalance(sender);
                    }
                }
                case "deposit" -> transfer(sender, true, args.length > 1 ? args[1] : null);
                case "withdraw" -> transfer(sender, false, args.length > 1 ? args[1] : null);
                case "accounts" -> accounts(sender);
                default -> sender.sendMessage("Unknown subcommand: " + args[0]
                        + " (use balance, deposit, withdraw, accounts)");
            }
        } catch (SQLException e) {
            log.error("bank command failed: %s", e.getMessage());
            sender.sendMessage("The bank is temporarily unavailable. Please try again.");
        }
        return true;
    }

    private void ownBalance(CommandSender sender) throws SQLException {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can check their own balance (use /bank balance <player>).");
            return;
        }
        showBalance(sender, player.getUniqueId(), player.getName());
    }

    private void otherBalance(CommandSender sender, String name) throws SQLException {
        if (!isOp(sender)) {
            sender.sendMessage("Only an operator can view another player's balance.");
            return;
        }
        Optional<UUID> uuid = resolvePlayer(name);
        if (uuid.isEmpty()) {
            sender.sendMessage("Unknown player: " + name);
            return;
        }
        showBalance(sender, uuid.get(), name);
    }

    private void showBalance(CommandSender sender, UUID uuid, String displayName) throws SQLException {
        OptionalLong balance = service.balanceOf(uuid.toString());
        if (balance.isEmpty()) {
            sender.sendMessage(displayName + " has no bank account yet.");
        } else {
            sender.sendMessage(displayName + "'s balance: " + balance.getAsLong() + " " + config.currencyName());
        }
    }

    private void transfer(CommandSender sender, boolean deposit, String amountText) throws SQLException {
        String verb = deposit ? "deposit" : "withdraw";
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can " + verb + ".");
            return;
        }
        if (amountText == null) {
            sender.sendMessage("Usage: /bank " + verb + " <amount>");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            sender.sendMessage("Invalid amount: " + amountText + " (must be a whole number).");
            return;
        }
        TransferResult result = deposit
                ? service.deposit(player.getUniqueId().toString(), player.getName(), amount)
                : service.withdraw(player.getUniqueId().toString(), amount);
        switch (result.outcome()) {
            case OK -> sender.sendMessage(
                    (deposit ? "Deposited " : "Withdrew ") + amount + " " + config.currencyName()
                            + ". New balance: " + result.balance() + " " + config.currencyName());
            case NO_ACCOUNT -> sender.sendMessage("You have no bank account yet. Deposit to open one.");
            case INSUFFICIENT_BALANCE -> sender.sendMessage("Insufficient balance to withdraw " + amount + ".");
            case EXCEEDS_MAX_BALANCE -> sender.sendMessage("That deposit would exceed the maximum balance.");
            case AMOUNT_BELOW_MIN -> sender.sendMessage(
                    "Amount must be at least " + config.minTransaction() + ".");
        }
    }

    private void accounts(CommandSender sender) throws SQLException {
        if (!isOp(sender)) {
            sender.sendMessage("Only an operator can list all accounts.");
            return;
        }
        List<Account> accounts = service.listAccounts();
        if (accounts.isEmpty()) {
            sender.sendMessage("No bank accounts yet.");
            return;
        }
        sender.sendMessage("---- Bank accounts (" + accounts.size() + ") ----");
        for (Account account : accounts) {
            sender.sendMessage("#" + account.accountId() + " " + account.ownerName() + ": "
                    + account.balance() + " " + config.currencyName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(List.of("balance", "deposit", "withdraw"));
            if (isOp(sender)) {
                subcommands.add("accounts");
            }
            return filter(subcommands, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("balance")) {
            return filter(onlinePlayerNames(), args[1]);
        }
        // No completion for amounts.
        return List.of();
    }

    private static List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private Optional<UUID> resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online.getUniqueId());
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.hasPlayedBefore()) {
            return Optional.of(offline.getUniqueId());
        }
        return Optional.empty();
    }

    private static boolean isOp(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.isOp();
        }
        // The console has all permissions.
        return sender instanceof ConsoleCommandSender;
    }
}
