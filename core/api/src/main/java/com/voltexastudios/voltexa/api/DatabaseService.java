package com.voltexastudios.voltexa.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Shared database access for feature plugins. The Voltexa core owns the
 * connection pool; features borrow and release connections, or use
 * {@link #withConnection(ThrowingFunction)} for scoped access. Features never
 * touch the JDBC driver or the database URL directly.
 */
public interface DatabaseService {

    /**
     * Borrows a connection from the pool. Callers must return it with
     * {@link #releaseConnection(Connection)}, or prefer
     * {@link #withConnection(ThrowingFunction)}.
     *
     * @throws SQLException if no connection becomes available in time
     */
    Connection borrowConnection() throws SQLException;

    /** Returns a previously borrowed connection to the pool. */
    void releaseConnection(Connection connection);

    /**
     * Runs {@code block} on a pooled connection and always releases it, even
     * when the block throws.
     *
     * @throws SQLException if pool access or the block fails
     */
    <T> T withConnection(ThrowingFunction<Connection, T> block) throws SQLException;

    /** Product name of the underlying database (e.g. {@code H2}). */
    String databaseProduct();
}
