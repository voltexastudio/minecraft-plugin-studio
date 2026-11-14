package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.DatabaseService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Bank domain logic: account open, balance lookups, deposit/withdraw, and the
 * account listing. Pure domain + JDBC — it never touches the JDBC driver, the
 * database URL, or the pool directly; every connection comes through the core's
 * {@link DatabaseService}. Each deposit/withdraw is one JDBC transaction (the
 * core's {@code withConnection} block): the account change and the ledger entry
 * commit or roll back together, and the {@code CHECK (balance >= 0)} constraint
 * is the last line of defense.
 */
public final class BankService {

    private final DatabaseService db;
    private final BankConfig config;

    public BankService(DatabaseService db, BankConfig config) {
        this.db = db;
        this.config = config;
    }

    /** The balance of {@code ownerUuid}, or empty when the account does not exist. */
    public OptionalLong balanceOf(String ownerUuid) throws SQLException {
        return db.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT balance FROM bank_accounts WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
                }
            }
        });
    }

    /**
     * Deposits {@code amount} into the caller's account, auto-creating it at
     * {@code starting-balance} when it does not exist yet. Returns the
     * resulting balance, or a rejection (below-min, or would exceed the cap).
     */
    public TransferResult deposit(String ownerUuid, String ownerName, long amount) throws SQLException {
        if (amount < config.minTransaction()) {
            return TransferResult.fail(TransferResult.Outcome.AMOUNT_BELOW_MIN);
        }
        return db.withConnection(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                // 1. Ensure the account exists (created at starting-balance if not).
                //    A SELECT-then-INSERT keeps this portable across backends
                //    (H2/Postgres/MySQL); the unique constraint on owner_uuid
                //    is the guard against a duplicate insert.
                boolean exists;
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT 1 FROM bank_accounts WHERE owner_uuid = ?")) {
                    check.setString(1, ownerUuid);
                    try (ResultSet rs = check.executeQuery()) {
                        exists = rs.next();
                    }
                }
                if (!exists) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO bank_accounts (owner_uuid, owner_name, balance, created_at, updated_at) "
                                    + "VALUES (?, ?, ?, ?, ?)")) {
                        insert.setString(1, ownerUuid);
                        insert.setString(2, ownerName);
                        insert.setLong(3, config.startingBalance());
                        insert.setTimestamp(4, now);
                        insert.setTimestamp(5, now);
                        insert.executeUpdate();
                    }
                }
                // 2. Read the account id and current balance.
                long accountId;
                long balance;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT account_id, balance FROM bank_accounts WHERE owner_uuid = ?")) {
                    select.setString(1, ownerUuid);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("account not found after insert: " + ownerUuid);
                        }
                        accountId = rs.getLong(1);
                        balance = rs.getLong(2);
                    }
                }
                // 3. Apply the deposit, guarding the cap so a concurrent top-up
                //    cannot push the balance past max-balance (0-row update).
                long newBalance;
                int updated;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE bank_accounts SET balance = balance + ?, updated_at = ? "
                                + "WHERE owner_uuid = ? AND balance + ? <= ?")) {
                    update.setLong(1, amount);
                    update.setTimestamp(2, now);
                    update.setString(3, ownerUuid);
                    update.setLong(4, amount);
                    update.setLong(5, config.maxBalance());
                    updated = update.executeUpdate();
                }
                if (updated == 0) {
                    connection.rollback();
                    return TransferResult.fail(TransferResult.Outcome.EXCEEDS_MAX_BALANCE);
                }
                // 4. Read back the resulting balance for the ledger entry.
                newBalance = readBalance(connection, ownerUuid);
                insertLedgerEntry(connection, accountId, "DEPOSIT", amount, newBalance, now);
                connection.commit();
                return TransferResult.ok(newBalance);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    /**
     * Withdraws {@code amount} from the caller's account. The account must
     * already exist. Returns the resulting balance, or a rejection (no account,
     * insufficient balance, or below-min).
     */
    public TransferResult withdraw(String ownerUuid, long amount) throws SQLException {
        if (amount < config.minTransaction()) {
            return TransferResult.fail(TransferResult.Outcome.AMOUNT_BELOW_MIN);
        }
        return db.withConnection(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                // 1. The account must exist.
                long accountId;
                long balance;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT account_id, balance FROM bank_accounts WHERE owner_uuid = ?")) {
                    select.setString(1, ownerUuid);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return TransferResult.fail(TransferResult.Outcome.NO_ACCOUNT);
                        }
                        accountId = rs.getLong(1);
                        balance = rs.getLong(2);
                    }
                }
                // 2. Apply the withdraw with a guard (balance >= amount) so a
                //    concurrent withdrawal makes this a 0-row update.
                int updated;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE bank_accounts SET balance = balance - ?, updated_at = ? "
                                + "WHERE owner_uuid = ? AND balance >= ?")) {
                    update.setLong(1, amount);
                    update.setTimestamp(2, now);
                    update.setString(3, ownerUuid);
                    update.setLong(4, amount);
                    updated = update.executeUpdate();
                }
                if (updated == 0) {
                    connection.rollback();
                    return TransferResult.fail(TransferResult.Outcome.INSUFFICIENT_BALANCE);
                }
                // 3. Read back the resulting balance for the ledger entry.
                long newBalance = readBalance(connection, ownerUuid);
                insertLedgerEntry(connection, accountId, "WITHDRAW", amount, newBalance, now);
                connection.commit();
                return TransferResult.ok(newBalance);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    /** Every account, ordered by account id (for {@code /bank accounts}). */
    public List<Account> listAccounts() throws SQLException {
        return db.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT account_id, owner_uuid, owner_name, balance FROM bank_accounts ORDER BY account_id");
                 ResultSet rs = ps.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (rs.next()) {
                    accounts.add(new Account(
                            rs.getLong("account_id"),
                            rs.getString("owner_uuid"),
                            rs.getString("owner_name"),
                            rs.getLong("balance")));
                }
                return accounts;
            }
        });
    }

    private static long readBalance(java.sql.Connection connection, String ownerUuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT balance FROM bank_accounts WHERE owner_uuid = ?")) {
            select.setString(1, ownerUuid);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("account vanished mid-transaction: " + ownerUuid);
                }
                return rs.getLong(1);
            }
        }
    }

    private static void insertLedgerEntry(java.sql.Connection connection, long accountId, String type,
                                         long amount, long balanceAfter, Timestamp now) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO bank_transactions (account_id, type, amount, balance_after, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            insert.setLong(1, accountId);
            insert.setString(2, type);
            insert.setLong(3, amount);
            insert.setLong(4, balanceAfter);
            insert.setTimestamp(5, now);
            insert.executeUpdate();
        }
    }
}
