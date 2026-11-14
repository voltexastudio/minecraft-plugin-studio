package com.voltexastudios.bank;

import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.ThrowingFunction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Test-only {@link DatabaseService} over a single in-memory H2 connection.
 * It lets the domain-logic tests exercise the real H2 driver (the same one the
 * core shades) without a server. A fresh database is created per instance, so
 * each test gets an isolated schema. The single shared connection is returned
 * as-is; the caller (the service under test) is responsible for restoring
 * autocommit, which {@code BankService} does in a {@code finally}.
 */
final class InMemoryDatabaseService implements DatabaseService {

    private final Connection connection;

    InMemoryDatabaseService() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("H2 driver missing from the test classpath", e);
        }
        this.connection = DriverManager.getConnection(
                "jdbc:h2:mem:bank_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    @Override
    public Connection borrowConnection() {
        return connection;
    }

    @Override
    public void releaseConnection(Connection ignored) {
        // Single shared connection; nothing to return.
    }

    @Override
    public <T> T withConnection(ThrowingFunction<Connection, T> block) throws SQLException {
        try {
            return block.apply(connection);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("connection block failed", e);
        }
    }

    @Override
    public String databaseProduct() {
        return "H2";
    }

    /** Closes the underlying connection (the in-memory database is discarded). */
    void close() throws SQLException {
        connection.close();
    }
}
