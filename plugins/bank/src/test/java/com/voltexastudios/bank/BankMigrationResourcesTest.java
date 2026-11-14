package com.voltexastudios.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit-level contract test: verifies Bank's versioned migration resources
 * are present on the classpath and well-formed (zero-padded version
 * prefix, non-blank SQL, expected tables) before the jar is loaded on a
 * real Paper server, where the core migration runner applies them. The
 * server smoke test is documented in {@code docs/plugins/bank/TESTPLAN.md}.
 */
class BankMigrationResourcesTest {

    private static List<String> listMigrations() throws Exception {
        List<String> names = new ArrayList<>();
        for (URL url : Collections.list(BankPlugin.class.getClassLoader().getResources("migrations"))) {
            // The test classpath exposes the resources as a directory.
            File[] files = new File(url.getFile()).listFiles((d, n) -> n.endsWith(".sql"));
            if (files != null) {
                for (File file : files) {
                    names.add(file.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    @Test
    void shipsTheContractMigrationFiles() throws Exception {
        assertEquals(List.of(
                "0001_bank_accounts.sql",
                "0002_bank_transactions.sql",
                "0003_bank_transactions_account_index.sql"), listMigrations());
    }

    @Test
    void migrationFileNamesFollowTheVersionConvention() throws Exception {
        for (String name : listMigrations()) {
            // Zero-padded version prefix, an underscore-separated name, .sql.
            assertTrue(name.matches("\\d{4}_[a-z0-9_]+\\.sql"),
                    "migration file name must be <version>_<name>.sql: " + name);
        }
    }

    @Test
    void migrationSqlIsPresentAndNonBlank() throws Exception {
        for (String name : listMigrations()) {
            String sql = readSql(name);
            assertTrue(sql.strip().length() > 0, "migration SQL must not be blank: " + name);
        }
    }

    @Test
    void migrationsCreateTheContractTables() throws Exception {
        assertTrue(readSql("0001_bank_accounts.sql").contains("bank_accounts"));
        String transactions = readSql("0002_bank_transactions.sql");
        assertTrue(transactions.contains("bank_transactions"));
        assertTrue(transactions.contains("bank_accounts"),
                "the ledger must reference the accounts table (foreign key)");
        assertTrue(readSql("0003_bank_transactions_account_index.sql").contains("bank_transactions"));
    }

    private static String readSql(String fileName) throws Exception {
        try (InputStream in = BankPlugin.class.getClassLoader()
                .getResourceAsStream("migrations/" + fileName)) {
            assertNotNull(in, fileName + " must be on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
