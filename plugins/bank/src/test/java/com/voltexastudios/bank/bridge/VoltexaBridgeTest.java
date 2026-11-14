package com.voltexastudios.bank.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voltexastudios.bank.BankPlugin;
import com.voltexastudios.voltexa.api.ConfigService;
import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.LogService;
import com.voltexastudios.voltexa.api.Migration;
import com.voltexastudios.voltexa.api.MigrationRecord;
import com.voltexastudios.voltexa.api.MigrationService;
import com.voltexastudios.voltexa.api.VoltexaAPI;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the classloader bridge against a simulated two-classloader setup:
 * a custom classloader loads the {@code com.voltexastudios.voltexa.api.*}
 * contract fresh (the "core side"), while the test classloader holds Bank's
 * copy. This mirrors what Bukkit does when both plugins bundle the contract
 * — no JVM flags, no shared classpath entry.
 */
class VoltexaBridgeTest {

    private static ClassLoader coreCl;
    private static Connection sharedConnection;
    private static FakeCore core;

    @BeforeAll
    static void setUp() throws Exception {
        coreCl = new CoreClassLoader(VoltexaBridgeTest.class.getClassLoader());
        core = new FakeCore(coreCl);
        sharedConnection = (Connection) Proxy.newProxyInstance(
                VoltexaBridgeTest.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (p, m, a) -> null);
    }

    @BeforeEach
    void reset() {
        core.reset();
    }

    private static VoltexaAPI wrappedApi() {
        return VoltexaBridge.wrap(coreCl, core.api);
    }

    @Test
    void theTwoSidesLoadDifferentClassObjects() throws Exception {
        assertNotSame(VoltexaAPI.class,
                coreCl.loadClass("com.voltexastudios.voltexa.api.VoltexaAPI"));
    }

    @Test
    void versionPassesThrough() {
        assertEquals("2026.46.1", wrappedApi().version());
    }

    @Test
    void serviceAccessorsReturnBankSideProxies() {
        VoltexaAPI api = wrappedApi();
        assertTrue(DatabaseService.class.isInstance(api.database()));
        assertTrue(ConfigService.class.isInstance(api.config()));
        assertTrue(LogService.class.isInstance(api.log()));
        assertTrue(MigrationService.class.isInstance(api.migrations()));
    }

    @Test
    void withConnectionRoundTripsThroughTheCore() throws Exception {
        VoltexaAPI api = wrappedApi();
        int result = api.database().withConnection(conn -> {
            assertSame(sharedConnection, conn);
            return 42;
        });
        assertEquals(42, result);
    }

    @Test
    void withConnectionUnwrapsTheCoreException() {
        VoltexaAPI api = wrappedApi();
        core.failNextWithConnection = new SQLException("boom");
        SQLException e = assertThrows(SQLException.class,
                () -> api.database().withConnection(conn -> 1));
        assertEquals("boom", e.getMessage());
    }

    @Test
    void borrowAndReleasePassThrough() throws Exception {
        VoltexaAPI api = wrappedApi();
        Connection borrowed = api.database().borrowConnection();
        assertSame(sharedConnection, borrowed);
        api.database().releaseConnection(borrowed);
        assertSame(sharedConnection, core.releasedConnection);
    }

    @Test
    void databaseProductPassesThrough() {
        assertEquals("H2", wrappedApi().database().databaseProduct());
    }

    @Test
    void appliedMigrationsComeBackAsBankSideRecords() {
        VoltexaAPI api = wrappedApi();
        List<MigrationRecord> records = api.migrations().appliedMigrations();
        assertEquals(2, records.size());
        MigrationRecord first = records.get(0);
        assertEquals(1, first.version());
        assertEquals("voltexa", first.owner());
        assertEquals("init", first.name());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), first.appliedAt());
    }

    @Test
    void applyMigrationsWithPluginClassPassesThrough() throws Exception {
        VoltexaAPI api = wrappedApi();
        api.migrations().applyMigrations("bank", BankPlugin.class);
        assertEquals("bank", core.appliedOwner);
        assertEquals(BankPlugin.class.getName(), core.appliedPluginClass);
    }

    @Test
    void applyMigrationsListIsReMaterializedOnTheCoreSide() throws Exception {
        VoltexaAPI api = wrappedApi();
        api.migrations().applyMigrations("bank",
                List.of(new Migration(1, "bank_accounts",
                        "CREATE TABLE bank_accounts (id INT)")));
        assertEquals("bank", core.appliedOwner);
        assertEquals(1, core.appliedMigrations.size());
        Object coreSide = core.appliedMigrations.get(0);
        // The core must see its own copy of the record.
        assertEquals(coreCl.loadClass("com.voltexastudios.voltexa.api.Migration"),
                coreSide.getClass());
        assertEquals(1, (int) coreSide.getClass().getMethod("version").invoke(coreSide));
        assertEquals("bank_accounts",
                coreSide.getClass().getMethod("name").invoke(coreSide));
    }

    @Test
    void logForwardingIncludesVarargs() {
        VoltexaAPI api = wrappedApi();
        api.log().info("Bank %s enabled", "2026.46.1");
        assertEquals("info", core.logMethod);
        assertEquals("Bank %s enabled", core.logFormat);
        assertEquals(1, core.logArgs.length);
        assertEquals("2026.46.1", core.logArgs[0]);
    }

    @Test
    void logForwardingWithoutArgs() {
        wrappedApi().log().warn("plain");
        assertEquals("warn", core.logMethod);
        assertEquals("plain", core.logFormat);
        assertTrue(core.logArgs.length == 0);
    }

    @Test
    void configForPluginPassesArgumentsThrough() throws Exception {
        VoltexaAPI api = wrappedApi();
        Path dataFolder = Files.createTempDirectory("voltexa-bridge-test");
        FileConfiguration config =
                api.config().forPlugin("Bank", dataFolder.toFile());
        assertNotNull(config);
        assertEquals("Bank", core.configPluginName);
        assertEquals(dataFolder.toFile(), core.configDataFolder);
    }

    @Test
    void proxyObjectMethodsBehave() {
        VoltexaAPI api = wrappedApi();
        assertTrue(api.toString().contains("VoltexaBridge"));
        assertTrue(api.hashCode() != 0);
        assertTrue(api.equals(api));
    }

    /**
     * Loads the contract fresh (its own {@code Class} objects) while
     * delegating everything else to the parent.
     */
    static final class CoreClassLoader extends ClassLoader {
        private static final String API_PREFIX = "com.voltexastudios.voltexa.api.";
        private final Path apiLocation;
        /** Classes already defined by this loader (mirrors real loader behavior). */
        private final java.util.Map<String, Class<?>> defined =
                new java.util.concurrent.ConcurrentHashMap<>();

        CoreClassLoader(ClassLoader parent) throws Exception {
            super(parent);
            URL url = VoltexaAPI.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            this.apiLocation = Path.of(url.toURI());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith(API_PREFIX)) {
                Class<?> loaded = defined.get(name);
                if (loaded != null) {
                    return loaded;
                }
                Class<?> clazz = findClass(name);
                defined.put(name, clazz);
                return clazz;
            }
            return super.loadClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String entry = "com/voltexastudios/voltexa/api/"
                    + name.substring(API_PREFIX.length()) + ".class";
            try {
                byte[] bytes;
                if (Files.isDirectory(apiLocation)) {
                    bytes = Files.readAllBytes(apiLocation.resolve(entry));
                } else {
                    try (JarFile jar = new JarFile(apiLocation.toFile())) {
                        var jarEntry = jar.getEntry(entry);
                        if (jarEntry == null) {
                            throw new ClassNotFoundException(name);
                        }
                        try (InputStream in = jar.getInputStream(jarEntry)) {
                            bytes = in.readAllBytes();
                        }
                    }
                }
                return defineClass(name, bytes, 0, bytes.length);
            } catch (ClassNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    /** A fake Voltexa core: core-side proxies implementing the core-side contract. */
    static final class FakeCore {
        final Object api;
        SQLException failNextWithConnection;
        Object releasedConnection;
        String appliedOwner;
        String appliedPluginClass;
        List<Object> appliedMigrations = new ArrayList<>();
        String logMethod;
        String logFormat;
        Object[] logArgs;
        String configPluginName;
        java.io.File configDataFolder;

        FakeCore(ClassLoader coreCl) throws Exception {
            Class<?> coreApi =
                    coreCl.loadClass("com.voltexastudios.voltexa.api.VoltexaAPI");
            Class<?> coreDb =
                    coreCl.loadClass("com.voltexastudios.voltexa.api.DatabaseService");
            Class<?> coreConfig =
                    coreCl.loadClass("com.voltexastudios.voltexa.api.ConfigService");
            Class<?> coreLog =
                    coreCl.loadClass("com.voltexastudios.voltexa.api.LogService");
            Class<?> coreMigrations =
                    coreCl.loadClass("com.voltexastudios.voltexa.api.MigrationService");

            Object db = Proxy.newProxyInstance(
                    coreCl, new Class<?>[] {coreDb}, dbHandler());
            Object config = Proxy.newProxyInstance(
                    coreCl, new Class<?>[] {coreConfig}, configHandler());
            Object log = Proxy.newProxyInstance(
                    coreCl, new Class<?>[] {coreLog}, logHandler());
            Object migrations = Proxy.newProxyInstance(
                    coreCl, new Class<?>[] {coreMigrations}, migrationsHandler());

            api = Proxy.newProxyInstance(coreCl, new Class<?>[] {coreApi},
                    (p, m, a) -> switch (m.getName()) {
                        case "version" -> "2026.46.1";
                        case "database" -> db;
                        case "config" -> config;
                        case "log" -> log;
                        case "migrations" -> migrations;
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
        }

        void reset() {
            failNextWithConnection = null;
            releasedConnection = null;
            appliedOwner = null;
            appliedPluginClass = null;
            appliedMigrations = new ArrayList<>();
            logMethod = null;
            logFormat = null;
            logArgs = null;
            configPluginName = null;
            configDataFolder = null;
        }

        private InvocationHandler dbHandler() {
            return (p, m, a) -> {
                switch (m.getName()) {
                    case "borrowConnection":
                        return sharedConnection;
                    case "releaseConnection":
                        releasedConnection = a[0];
                        return null;
                    case "withConnection":
                        if (failNextWithConnection != null) {
                            throw failNextWithConnection;
                        }
                        // a[0] is a core-side ThrowingFunction proxy.
                        return a[0].getClass().getMethod("apply", Object.class)
                                .invoke(a[0], sharedConnection);
                    case "databaseProduct":
                        return "H2";
                    default:
                        throw new UnsupportedOperationException(m.getName());
                }
            };
        }

        private InvocationHandler configHandler() {
            return (p, m, a) -> {
                switch (m.getName()) {
                    case "forPlugin":
                        configPluginName = (String) a[0];
                        configDataFolder = (java.io.File) a[1];
                        return fileConfigurationStub();
                    case "core":
                        return fileConfigurationStub();
                    default:
                        throw new UnsupportedOperationException(m.getName());
                }
            };
        }

        private InvocationHandler logHandler() {
            return (p, m, a) -> {
                logMethod = m.getName();
                logFormat = (String) a[0];
                logArgs = a.length > 1 ? (Object[]) a[1] : new Object[0];
                return null;
            };
        }

        private InvocationHandler migrationsHandler() {
            return (p, m, a) -> {
                switch (m.getName()) {
                    case "applyMigrations":
                        appliedOwner = (String) a[0];
                        if (a[1] instanceof Class<?> pluginClass) {
                            appliedPluginClass = pluginClass.getName();
                        } else {
                            appliedMigrations.addAll((List<?>) a[1]);
                        }
                        return null;
                    case "appliedMigrations":
                        return appliedRecords();
                    default:
                        throw new UnsupportedOperationException(m.getName());
                }
            };
        }

        private List<Object> appliedRecords() throws Exception {
            Class<?> recordClass = coreCl.loadClass(
                    "com.voltexastudios.voltexa.api.MigrationRecord");
            var constructor = recordClass.getConstructor(
                    int.class, String.class, String.class, Instant.class);
            List<Object> records = new ArrayList<>();
            records.add(constructor.newInstance(1, "voltexa", "init",
                    Instant.parse("2026-11-01T00:00:00Z")));
            records.add(constructor.newInstance(2, "bank", "bank_accounts",
                    Instant.parse("2026-11-02T00:00:00Z")));
            return records;
        }

        /** FileConfiguration is an abstract class (not an interface). */
        private Object fileConfigurationStub() {
            return new FileConfiguration() {
                @Override
                public void loadFromString(String src) {
                    // Stub: nothing to load.
                }

                @Override
                public String saveToString() {
                    return "";
                }
            };
        }
    }
}
