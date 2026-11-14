package com.voltexastudios.voltexa.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voltexastudios.voltexa.log.DefaultLogService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the fixed-size connection pool against an in-memory H2
 * database (no server, no file).
 */
class ConnectionPoolTest {

    /** Test provider backed by a per-test in-memory H2 database. */
    static final class InMemoryProvider implements DatabaseProvider {
        private final List<Connection> created = new ArrayList<>();
        // Unique database per test: no state may leak between test methods.
        private final String jdbcUrl = "jdbc:h2:mem:pool" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

        @Override
        public Connection createConnection() throws SQLException {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            created.add(connection);
            return connection;
        }

        @Override
        public String product() {
            return "H2";
        }

        @Override
        public void close() {
        }

        int createdCount() {
            return created.size();
        }
    }

    private final InMemoryProvider provider = new InMemoryProvider();
    private final DefaultLogService log = new DefaultLogService(Logger.getLogger("pooltest"));
    private ConnectionPool pool;

    @AfterEach
    void closePool() {
        if (pool != null) {
            pool.close();
        }
    }

    @Test
    void borrowAndReleaseReusesConnection() throws Exception {
        pool = new ConnectionPool(provider, 2, log);
        Connection first = pool.borrow(5, TimeUnit.SECONDS);
        pool.release(first);
        Connection second = pool.borrow(5, TimeUnit.SECONDS);
        assertSame(first, second, "released connection should be reused");
        assertEquals(1, provider.createdCount(), "only one connection should be created");
        pool.release(second);
    }

    @Test
    void borrowTimesOutWhenPoolExhausted() throws Exception {
        pool = new ConnectionPool(provider, 1, log);
        Connection held = pool.borrow(5, TimeUnit.SECONDS);
        try {
            assertThrows(SQLException.class, () -> pool.borrow(200, TimeUnit.MILLISECONDS));
        } finally {
            pool.release(held);
        }
    }

    @Test
    void borrowSucceedsAfterReleaseFreesSlot() throws Exception {
        pool = new ConnectionPool(provider, 1, log);
        Connection held = pool.borrow(5, TimeUnit.SECONDS);
        pool.release(held);
        Connection next = pool.borrow(5, TimeUnit.SECONDS);
        assertTrue(next.isValid(2));
        pool.release(next);
    }

    @Test
    void closedPoolRejectsBorrow() throws Exception {
        pool = new ConnectionPool(provider, 2, log);
        pool.close();
        assertThrows(SQLException.class, () -> pool.borrow(200, TimeUnit.MILLISECONDS));
    }
}
