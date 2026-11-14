package com.voltexastudios.voltexa.db;

import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.ThrowingFunction;
import com.voltexastudios.voltexa.log.DefaultLogService;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link DatabaseService}: hands out connections from the core's
 * pool. Feature plugins get every connection through this class.
 */
public final class DefaultDatabaseService implements DatabaseService {

    private static final long BORROW_TIMEOUT_SECONDS = 10;

    private final ConnectionPool pool;
    private final DefaultLogService log;

    public DefaultDatabaseService(ConnectionPool pool, DefaultLogService log) {
        this.pool = pool;
        this.log = log;
    }

    @Override
    public Connection borrowConnection() throws SQLException {
        try {
            return pool.borrow(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("interrupted while waiting for a database connection", e);
        }
    }

    @Override
    public void releaseConnection(Connection connection) {
        pool.release(connection);
    }

    @Override
    public <T> T withConnection(ThrowingFunction<Connection, T> block) throws SQLException {
        Connection connection = borrowConnection();
        try {
            return block.apply(connection);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("connection block failed", e);
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public String databaseProduct() {
        return pool.product();
    }

    /** The underlying pool (for status reporting). */
    public ConnectionPool pool() {
        return pool;
    }
}
