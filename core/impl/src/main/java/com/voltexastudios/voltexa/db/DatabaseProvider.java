package com.voltexastudios.voltexa.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC abstraction over the underlying database engine. v1 ships
 * {@link H2FileProvider} (embedded, file-based, zero-config); MySQL or
 * PostgreSQL backends are added later by implementing this interface and
 * selecting one in the core config.
 */
public interface DatabaseProvider {

    /** Creates a fresh connection to the database. */
    Connection createConnection() throws SQLException;

    /** Product name of the underlying database (e.g. {@code H2}). */
    String product();

    /** Releases any provider-level resources. */
    void close();
}
