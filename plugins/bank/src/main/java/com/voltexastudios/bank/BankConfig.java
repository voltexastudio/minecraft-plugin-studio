package com.voltexastudios.bank;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over Bank's {@code bank.*} configuration keys. The config file
 * itself is loaded through the core's {@code ConfigService.forPlugin} (Bank
 * ships no config-loading code); this class only reads the keys with the
 * contract defaults, so a missing file falls back to the documented values.
 */
public final class BankConfig {

    /** Display name of the currency in command output (e.g. {@code coins}). */
    private final String currencyName;
    /** Balance of auto-created accounts (smallest currency unit). */
    private final long startingBalance;
    /** Safety cap on a single account's balance (smallest currency unit). */
    private final long maxBalance;
    /** Smallest allowed transaction magnitude (smallest currency unit). */
    private final long minTransaction;

    public BankConfig(FileConfiguration config) {
        this.currencyName = config.getString("bank.currency-name", "coins");
        this.startingBalance = config.getLong("bank.starting-balance", 0L);
        this.maxBalance = config.getLong("bank.max-balance", 1_000_000_000_000L);
        this.minTransaction = config.getLong("bank.min-transaction", 1L);
    }

    public String currencyName() {
        return currencyName;
    }

    public long startingBalance() {
        return startingBalance;
    }

    public long maxBalance() {
        return maxBalance;
    }

    public long minTransaction() {
        return minTransaction;
    }
}
