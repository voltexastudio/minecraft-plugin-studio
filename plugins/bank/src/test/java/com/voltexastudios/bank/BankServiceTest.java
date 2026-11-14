package com.voltexastudios.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit-level domain-logic test: exercises {@link BankService} against the real
 * H2 driver (in-memory, the same engine the core shades) with the contract
 * tables. It proves the deposit/withdraw/balance/account logic end-to-end —
 * including the rejection paths and the append-only ledger — before the jar is
 * loaded on a real Paper server. The server smoke test is documented in
 * {@code docs/plugins/bank/TESTPLAN.md}.
 */
class BankServiceTest {

    private static final String UUID = "00000000-0000-0000-0000-000000000001";
    private static final String OTHER_UUID = "00000000-0000-0000-0000-000000000002";

    private InMemoryDatabaseService db;
    private BankService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new InMemoryDatabaseService();
        createBankTables();
        service = new BankService(db, bankConfig(0L, 1_000_000_000_000L, 1L));
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void balanceOfUnknownPlayerIsEmpty() throws Exception {
        assertTrue(service.balanceOf(UUID).isEmpty());
    }

    @Test
    void depositCreatesAccountAtStartingBalance() throws Exception {
        service = new BankService(db, bankConfig(50L, 1_000_000_000_000L, 1L));
        TransferResult result = service.deposit(UUID, "Steve", 100L);
        assertEquals(TransferResult.Outcome.OK, result.outcome());
        // Auto-created at starting-balance (50), then the deposit (100) applied.
        assertEquals(150L, result.balance());
        assertEquals(150L, service.balanceOf(UUID).orElseThrow());
    }

    @Test
    void depositAddsToExistingAccount() throws Exception {
        assertEquals(100L, service.deposit(UUID, "Steve", 100L).balance());
        assertEquals(150L, service.deposit(UUID, "Steve", 50L).balance());
        assertEquals(150L, service.balanceOf(UUID).orElseThrow());
    }

    @Test
    void depositDoesNotCreateADuplicateAccountOnSecondDeposit() throws Exception {
        service.deposit(UUID, "Steve", 10L);
        service.deposit(UUID, "Steve", 10L);
        List<Account> accounts = service.listAccounts();
        assertEquals(1, accounts.size());
    }

    @Test
    void withdrawReducesBalance() throws Exception {
        service.deposit(UUID, "Steve", 100L);
        TransferResult result = service.withdraw(UUID, 30L);
        assertEquals(TransferResult.Outcome.OK, result.outcome());
        assertEquals(70L, result.balance());
        assertEquals(70L, service.balanceOf(UUID).orElseThrow());
    }

    @Test
    void withdrawInsufficientBalanceIsRejectedAndBalanceUnchanged() throws Exception {
        service.deposit(UUID, "Steve", 50L);
        TransferResult result = service.withdraw(UUID, 100L);
        assertEquals(TransferResult.Outcome.INSUFFICIENT_BALANCE, result.outcome());
        assertEquals(50L, service.balanceOf(UUID).orElseThrow());
    }

    @Test
    void withdrawWithoutAccountIsRejected() throws Exception {
        TransferResult result = service.withdraw(UUID, 10L);
        assertEquals(TransferResult.Outcome.NO_ACCOUNT, result.outcome());
    }

    @Test
    void depositExceedingMaxBalanceIsRejectedAndCreatesNoAccount() throws Exception {
        service = new BankService(db, bankConfig(0L, 1000L, 1L));
        TransferResult result = service.deposit(UUID, "Steve", 1500L);
        assertEquals(TransferResult.Outcome.EXCEEDS_MAX_BALANCE, result.outcome());
        // The rejected deposit rolls back the auto-created account too.
        assertTrue(service.balanceOf(UUID).isEmpty());
    }

    @Test
    void depositBelowMinTransactionIsRejected() throws Exception {
        service = new BankService(db, bankConfig(0L, 1_000_000_000_000L, 10L));
        TransferResult result = service.deposit(UUID, "Steve", 5L);
        assertEquals(TransferResult.Outcome.AMOUNT_BELOW_MIN, result.outcome());
        assertTrue(service.balanceOf(UUID).isEmpty());
    }

    @Test
    void withdrawBelowMinTransactionIsRejected() throws Exception {
        service = new BankService(db, bankConfig(0L, 1_000_000_000_000L, 10L));
        service.deposit(UUID, "Steve", 100L);
        TransferResult result = service.withdraw(UUID, 5L);
        assertEquals(TransferResult.Outcome.AMOUNT_BELOW_MIN, result.outcome());
        assertEquals(100L, service.balanceOf(UUID).orElseThrow());
    }

    @Test
    void listAccountsReturnsAllAccounts() throws Exception {
        service.deposit(UUID, "Steve", 100L);
        service.deposit(OTHER_UUID, "Alex", 25L);
        List<Account> accounts = service.listAccounts();
        assertEquals(2, accounts.size());
        // Ordered by account id; the first-created account is first.
        assertEquals("Steve", accounts.get(0).ownerName());
        assertEquals(100L, accounts.get(0).balance());
        assertEquals("Alex", accounts.get(1).ownerName());
        assertEquals(25L, accounts.get(1).balance());
    }

    @Test
    void ledgerRecordsDepositAndWithdrawWithResultingBalance() throws Exception {
        service.deposit(UUID, "Steve", 100L);
        service.withdraw(UUID, 30L);
        List<long[]> rows = queryLedger();
        assertEquals(2, rows.size());
        // row: [typeOrdinal, amount, balanceAfter]
        assertEquals("DEPOSIT", typeOf(rows.get(0)));
        assertEquals(100L, rows.get(0)[1]);
        assertEquals(100L, rows.get(0)[2]);
        assertEquals("WITHDRAW", typeOf(rows.get(1)));
        assertEquals(30L, rows.get(1)[1]);
        assertEquals(70L, rows.get(1)[2]);
    }

    @Test
    void balanceReflectsDepositThenWithdraw() throws Exception {
        // The acceptance flow: create account, deposit, withdraw, balance reflects it.
        assertTrue(service.balanceOf(UUID).isEmpty());
        service.deposit(UUID, "Steve", 200L);
        assertEquals(200L, service.balanceOf(UUID).orElseThrow());
        service.withdraw(UUID, 80L);
        assertEquals(120L, service.balanceOf(UUID).orElseThrow());
    }

    // --- helpers ---

    private void createBankTables() throws Exception {
        for (String file : List.of(
                "0001_bank_accounts.sql",
                "0002_bank_transactions.sql",
                "0003_bank_transactions_account_index.sql")) {
            String sql = readMigration(file);
            db.withConnection(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
                return null;
            });
        }
    }

    private static BankConfig bankConfig(long startingBalance, long maxBalance, long minTransaction) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("bank.currency-name", "coins");
        config.set("bank.starting-balance", startingBalance);
        config.set("bank.max-balance", maxBalance);
        config.set("bank.min-transaction", minTransaction);
        FileConfiguration asFile = config;
        return new BankConfig(asFile);
    }

    private List<long[]> queryLedger() throws Exception {
        return db.withConnection(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT type, amount, balance_after FROM bank_transactions ORDER BY transaction_id")) {
                List<long[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new long[]{typeOrdinal(rs.getString(1)), rs.getLong(2), rs.getLong(3)});
                }
                return rows;
            }
        });
    }

    private static int typeOrdinal(String type) {
        return switch (type) {
            case "DEPOSIT" -> 0;
            case "WITHDRAW" -> 1;
            default -> -1;
        };
    }

    private static String typeOf(long[] row) {
        return switch ((int) row[0]) {
            case 0 -> "DEPOSIT";
            case 1 -> "WITHDRAW";
            default -> "?";
        };
    }

    private static String readMigration(String fileName) throws Exception {
        try (InputStream in = BankPlugin.class.getClassLoader()
                .getResourceAsStream("migrations/" + fileName)) {
            assertTrue(in != null, fileName + " must be on the classpath");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
