package com.voltexastudios.voltexa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for classpath migration discovery (directory and jar branches).
 */
class MigrationFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void listsSqlFilesFromDirectoryBranch() throws IOException {
        // The main resources (migrations/0001_core_meta.sql) are on the test
        // classpath as a directory: the file: branch must find the baseline.
        List<String> names = MigrationFiles.listSqlFiles();
        assertTrue(names.contains("0001_core_meta.sql"), "baseline migration must be listed from the class directory: " + names);
    }

    @Test
    void readsBaselineMigrationSql() throws IOException {
        String sql = MigrationFiles.readSql("0001_core_meta.sql");
        assertTrue(sql.contains("voltexa_meta"), "baseline SQL must create the voltexa_meta table");
    }

    @Test
    void jarBranchSkipsForeignEntriesAndNestedFiles() throws IOException {
        // Build a jar that mixes migration files with unrelated entries,
        // including short names (regression: substring before the prefix
        // guard crashed on entries shorter than the prefix).
        Path jar = tempDir.resolve("plugin.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            putEntry(out, "plugin.yml", "name: test");
            putEntry(out, "config.yml", "x: 1");
            putEntry(out, "migrations/0001_first.sql", "CREATE TABLE a (id INT);");
            putEntry(out, "migrations/0002_second.sql", "CREATE TABLE b (id INT);");
            putEntry(out, "migrations/nested/0003_not_top_level.sql", "SELECT 1;");
            putEntry(out, "migrations/notes.txt", "not sql");
        }
        URL jarUrl = new URL("jar:file:" + jar.toAbsolutePath() + "!/");
        List<String> names = MigrationFiles.listFromUrl(jarUrl);
        assertEquals(List.of("0001_first.sql", "0002_second.sql"), names);
    }

    @Test
    void versionPrefixIsTheLeadingDigitRun() {
        // Regression: parsing at the last underscore broke on names that
        // contain underscores (0001_core_meta.sql).
        assertEquals(4, MigrationFiles.versionPrefixLength("0001_core_meta.sql"));
        assertEquals(4, MigrationFiles.versionPrefixLength("0012_extra.sql"));
        assertEquals(4, MigrationFiles.versionPrefixLength("1234_x.sql"));
        assertThrows(IllegalArgumentException.class, () -> MigrationFiles.versionPrefixLength("no_prefix.sql"));
    }

    private static void putEntry(JarOutputStream out, String name, String content) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
