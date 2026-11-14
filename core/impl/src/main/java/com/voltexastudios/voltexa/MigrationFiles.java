package com.voltexastudios.voltexa;

import com.voltexastudios.voltexa.api.Migration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads versioned migration SQL files from a classloader. Works both from
 * an exploded classes directory (dev/test) and from a plugin jar (server).
 * The no-argument variants read the core's own migrations; the
 * classloader-parameterized variants let feature plugins load their own
 * {@code migrations} resources through the core (see
 * {@code MigrationService#applyMigrations(String, Class)}), so a feature
 * never ships its own file-loading code.
 *
 * <p>Public so the migration runner (in the {@code .migration} subpackage)
 * can load a feature plugin's files; it is an internal helper of the
 * {@code voltexa} jar, not part of the typed {@code voltexa-api} surface.
 */
public final class MigrationFiles {

    private static final String DIR = "migrations";

    private MigrationFiles() {
    }

    /** Names of the core's {@code .sql} files in the {@code migrations} classpath directory, sorted. */
    static List<String> listSqlFiles() throws IOException {
        return listSqlFiles(VoltexaPlugin.class.getClassLoader());
    }

    /** Names of the {@code .sql} files in the {@code migrations} classpath directory visible to {@code classLoader}, sorted. */
    static List<String> listSqlFiles(ClassLoader classLoader) throws IOException {
        List<String> names = new ArrayList<>();
        for (URL url : Collections.list(classLoader.getResources(DIR))) {
            names.addAll(listFromUrl(url));
        }
        Collections.sort(names);
        return names;
    }

    /** The {@code .sql} file names contributed by one classpath location (a directory or a jar). */
    static List<String> listFromUrl(URL url) throws IOException {
        List<String> names = new ArrayList<>();
        if ("file".equals(url.getProtocol())) {
            File dir = new File(url.getFile());
            File[] files = dir.listFiles((d, n) -> n.endsWith(".sql"));
            if (files != null) {
                for (File file : files) {
                    names.add(file.getName());
                }
            }
        } else if ("jar".equals(url.getProtocol())) {
            String spec = url.toString();
            int bang = spec.indexOf("!/");
            String jarPath = spec.substring("jar:".length(), bang);
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring("file:".length());
            }
            try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    // Guard before slicing: only entries inside migrations/ have
                    // the prefix to strip (other entries may be shorter than it).
                    if (entry.startsWith(DIR + "/") && entry.endsWith(".sql")) {
                        String base = entry.substring(DIR.length() + 1);
                        if (!base.contains("/")) {
                            names.add(base);
                        }
                    }
                }
            }
        }
        return names;
    }

    /**
     * Length of the leading zero-padded version prefix of a migration file
     * name ({@code 0001_core_meta.sql} → 4). The version is the digits, not
     * a whole underscore-separated token, so names may contain underscores.
     */
    static int versionPrefixLength(String fileName) {
        int i = 0;
        while (i < fileName.length() && Character.isDigit(fileName.charAt(i))) {
            i++;
        }
        if (i == 0) {
            throw new IllegalArgumentException("no version prefix in migration file name: " + fileName);
        }
        return i;
    }

    /** Reads one of the core's migration files' SQL from the classpath. */
    static String readSql(String fileName) throws IOException {
        return readSql(VoltexaPlugin.class.getClassLoader(), fileName);
    }

    /** Reads one migration file's SQL visible to {@code classLoader}. */
    static String readSql(ClassLoader classLoader, String fileName) throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(DIR + "/" + fileName)) {
            if (in == null) {
                throw new IOException("migration file not found on classpath: " + fileName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parses the versioned SQL files visible to {@code classLoader} into
     * migration records: the zero-padded version prefix is the version, the
     * rest of the file name (minus {@code .sql}) is the name. Public so
     * the migration runner (in the {@code .migration} subpackage) can load
     * a feature plugin's files.
     */
    public static List<Migration> loadMigrations(ClassLoader classLoader) throws IOException {
        List<Migration> result = new ArrayList<>();
        for (String fileName : listSqlFiles(classLoader)) {
            int prefixLen = versionPrefixLength(fileName);
            int version = Integer.parseInt(fileName.substring(0, prefixLen));
            String name = fileName.substring(prefixLen, fileName.length() - ".sql".length());
            // 0001_bank_accounts.sql → name "bank_accounts" (the separator
            // underscore is not part of the name).
            if (name.startsWith("_")) {
                name = name.substring(1);
            }
            result.add(new Migration(version, name, readSql(classLoader, fileName)));
        }
        return result;
    }
}
