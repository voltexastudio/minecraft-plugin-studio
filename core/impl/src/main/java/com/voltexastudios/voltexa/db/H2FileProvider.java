package com.voltexastudios.voltexa.db;

import com.voltexastudios.voltexa.log.DefaultLogService;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Zero-config, file-based H2 embedded database behind the JDBC abstraction.
 * The database file lives in the plugin data folder; no external service,
 * credentials, or config are required. The H2 driver is shaded into the
 * plugin jar and auto-registers with {@link DriverManager}.
 */
public final class H2FileProvider implements DatabaseProvider {

    private final String jdbcUrl;
    private final DefaultLogService log;

    public H2FileProvider(File dbFile, DefaultLogService log) {
        // DriverManager only auto-registers drivers visible from the system
        // classloader; the shaded H2 driver lives in the plugin classloader,
        // so load it explicitly (its static initializer self-registers).
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("H2 driver missing from the plugin jar", e);
        }
        // H2 appends ".mv.db" to the name in the URL; strip a configured
        // suffix so both "voltexa" and "voltexa.mv.db" produce voltexa.mv.db.
        String name = dbFile.getName();
        if (name.endsWith(".mv.db")) {
            name = name.substring(0, name.length() - ".mv.db".length());
        }
        File base = dbFile.getParentFile() != null
                ? new File(dbFile.getParentFile(), name)
                : new File(name);
        this.jdbcUrl = "jdbc:h2:file:" + base.getAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
        this.log = log;
    }

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
        // Connections are closed by the pool; the H2 file stays on disk for the next start.
        log.debug("H2 file provider closed (database file kept for next start)");
    }
}
