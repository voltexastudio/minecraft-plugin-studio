package com.voltexastudios.voltexa.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voltexastudios.voltexa.log.DefaultLogService;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test proving the H2 file database is created on first use (the
 * zero-config, file-based guarantee) and is usable through the pool.
 */
class H2FileProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void databaseFileIsCreatedOnFirstUse() throws Exception {
        File dbFile = new File(tempDir.toFile(), "test.mv.db");
        DefaultLogService log = new DefaultLogService(Logger.getLogger("h2filetest"));
        H2FileProvider provider = new H2FileProvider(dbFile, log);
        ConnectionPool pool = new ConnectionPool(provider, 1, log);
        try {
            Connection connection = pool.borrow(5, TimeUnit.SECONDS);
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE probe (id INT PRIMARY KEY)");
                st.execute("INSERT INTO probe (id) VALUES (1)");
            }
            pool.release(connection);
        } finally {
            pool.close();
        }
        assertTrue(dbFile.exists(), "H2 database file must exist after first use");
        assertEquals("H2", provider.product());
    }
}
