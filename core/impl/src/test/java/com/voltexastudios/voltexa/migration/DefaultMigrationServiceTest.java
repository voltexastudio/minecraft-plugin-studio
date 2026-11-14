package com.voltexastudios.voltexa.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voltexastudios.voltexa.api.Migration;
import com.voltexastudios.voltexa.api.MigrationRecord;
import com.voltexastudios.voltexa.db.ConnectionPool;
import com.voltexastudios.voltexa.db.DefaultDatabaseService;
import com.voltexastudios.voltexa.db.DatabaseProvider;
import com.voltexastudios.voltexa.log.DefaultLogService;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the versioned migration runner against an in-memory H2
 * database (no server, no file).
 */
class DefaultMigrationServiceTest {

    private DefaultDatabaseService database;
    private DefaultMigrationService migrations;

    @BeforeEach
    void setUp() throws Exception {
        // Unique database per test: the in-memory DB must not leak state
        // between test methods.
        String jdbcUrl = "jdbc:h2:mem:migration" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        DatabaseProvider provider = new DatabaseProvider() {
            @Override
            public Connection createConnection() throws SQLException {
                return DriverManager.getConnection(jdbcUrl);
            }

            @Override
            public String product() {
                return "H2";
            }

            @Override
            public void close() {
            }
        };
        DefaultLogService log = new DefaultLogService(Logger.getLogger("migrationtest"));
        database = new DefaultDatabaseService(new ConnectionPool(provider, 2, log), log);
        migrations = new DefaultMigrationService(database, log);
    }

    private boolean tableExists(String table) throws Exception {
        // H2 stores unquoted identifiers in upper case.
        return database.withConnection(connection -> {
            try (ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = UPPER('" + table + "')")) {
                return rs.next() && rs.getInt(1) > 0;
            }
        });
    }

    @Test
    void baselineAppliesAndIsRecorded() throws Exception {
        migrations.applyMigrations("core", List.of(
                new Migration(1, "core_meta", "CREATE TABLE voltexa_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(4096) NOT NULL, updated_at TIMESTAMP NOT NULL)")));

        assertTrue(tableExists("voltexa_meta"), "baseline table must exist after apply");
        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(1, applied.size());
        assertEquals(1, applied.get(0).version());
        assertEquals("core", applied.get(0).owner());
        assertEquals("core_meta", applied.get(0).name());
    }

    @Test
    void reapplyIsIdempotent() throws Exception {
        List<Migration> baseline = List.of(new Migration(1, "core_meta",
                "CREATE TABLE voltexa_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(4096) NOT NULL, updated_at TIMESTAMP NOT NULL)"));
        migrations.applyMigrations("core", baseline);
        migrations.applyMigrations("core", baseline);

        assertEquals(1, migrations.appliedMigrations().size(), "migration must not be re-applied");
    }

    @Test
    void newerVersionAppliesOnTopOfRecordedOnes() throws Exception {
        migrations.applyMigrations("core", List.of(new Migration(1, "core_meta",
                "CREATE TABLE voltexa_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(4096) NOT NULL, updated_at TIMESTAMP NOT NULL)")));
        migrations.applyMigrations("core", List.of(new Migration(2, "extra_table",
                "CREATE TABLE extra_table (id INT PRIMARY KEY)")));

        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(2, applied.size());
        assertTrue(tableExists("extra_table"));
    }

    @Test
    void inputOrderDoesNotChangeApplyOrder() throws Exception {
        migrations.applyMigrations("core", List.of(
                new Migration(2, "second", "CREATE TABLE second_table (id INT PRIMARY KEY)"),
                new Migration(1, "first", "CREATE TABLE first_table (id INT PRIMARY KEY)")));

        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(2, applied.size());
        assertEquals(1, applied.get(0).version(), "version 1 must be recorded first");
        assertEquals(2, applied.get(1).version());
    }

    @Test
    void failedMigrationIsNotRecorded() throws Exception {
        migrations.applyMigrations("core", List.of(new Migration(1, "core_meta",
                "CREATE TABLE voltexa_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(4096) NOT NULL, updated_at TIMESTAMP NOT NULL)")));
        Migration broken = new Migration(2, "broken",
                "CREATE TABLE broken_table (id INT PRIMARY KEY, id INT)");
        assertThrows(SQLException.class, () -> migrations.applyMigrations("core", List.of(broken)));

        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(1, applied.size(), "failed migration must not be recorded");
        assertFalse(applied.stream().anyMatch(r -> r.version() == 2));
    }

    @Test
    void differentOwnersAreTrackedSeparately() throws Exception {
        migrations.applyMigrations("core", List.of(new Migration(1, "core_meta",
                "CREATE TABLE voltexa_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(4096) NOT NULL, updated_at TIMESTAMP NOT NULL)")));
        migrations.applyMigrations("bank", List.of(new Migration(1, "bank_accounts",
                "CREATE TABLE bank_accounts (id INT PRIMARY KEY)")));

        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(2, applied.size(), "same version under different owners must both be tracked");
        assertTrue(applied.stream().anyMatch(r -> r.owner().equals("bank")));
    }

    @TempDir
    Path tempDir;

    @Test
    void classOverloadAppliesThePluginsOwnMigrationFiles() throws Exception {
        // Feature-plugin path: a "plugin" directory with its own migrations/
        // plus a compiled marker class, loaded through a URLClassLoader so
        // the Class overload's classloader lookup sees the plugin's files.
        Path pluginDir = tempDir.resolve("bank");
        Path migrationsDir = pluginDir.resolve("migrations");
        Files.createDirectories(migrationsDir);
        Files.writeString(migrationsDir.resolve("0001_bank_accounts.sql"),
                "CREATE TABLE bank_accounts (account_id BIGINT PRIMARY KEY, balance BIGINT NOT NULL)");
        Files.writeString(migrationsDir.resolve("0002_bank_transactions.sql"),
                "CREATE TABLE bank_transactions (transaction_id BIGINT PRIMARY KEY, account_id BIGINT NOT NULL)");

        Path classDir = tempDir.resolve("bank-classes");
        Files.createDirectories(classDir);
        Path source = tempDir.resolve("BankMarker.java");
        Files.writeString(source, "public class BankMarker {}");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(
                null, null, null, "-d", classDir.toString(), source.toString()),
                "marker class must compile");
        URLClassLoader loader = new URLClassLoader(
                new URL[]{pluginDir.toUri().toURL(), classDir.toUri().toURL()}, null);
        Class<?> pluginClass = Class.forName("BankMarker", true, loader);

        migrations.applyMigrations("bank", pluginClass);

        assertTrue(tableExists("bank_accounts"), "accounts table must exist after apply");
        assertTrue(tableExists("bank_transactions"), "transactions table must exist after apply");
        List<MigrationRecord> applied = migrations.appliedMigrations();
        assertEquals(2, applied.size());
        assertTrue(applied.stream().allMatch(r -> r.owner().equals("bank")));
        assertEquals("bank_accounts", applied.get(0).name());
        assertEquals("bank_transactions", applied.get(1).name());

        // Re-enable path: re-apply through the same classloader is a no-op.
        migrations.applyMigrations("bank", pluginClass);
        assertEquals(2, migrations.appliedMigrations().size(), "migrations must not be re-applied");
    }
}
