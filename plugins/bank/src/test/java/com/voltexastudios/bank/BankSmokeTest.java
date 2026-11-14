package com.voltexastudios.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Unit-level smoke test: verifies the plugin descriptor and main class are
 * consistent before the jar is loaded on a real Paper server. The server
 * smoke test is documented in {@code docs/plugins/bank/TESTPLAN.md}.
 */
class BankSmokeTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadPluginYml() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be on the classpath");
            return new Yaml().load(in);
        }
    }

    @Test
    void pluginYmlHasExpectedIdentity() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        assertEquals("Bank", yml.get("name"));
        assertEquals("0.1.0", yml.get("version"));
        assertEquals("1.21", String.valueOf(yml.get("api-version")));
    }

    @Test
    void mainClassMatchesPluginYml() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        assertEquals(BankPlugin.class.getName(), yml.get("main"));
    }

    @Test
    void mainClassExtendsJavaPlugin() {
        assertTrue(JavaPlugin.class.isAssignableFrom(BankPlugin.class));
    }

    @Test
    void dependsOnVoltexaCore() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        Object depend = yml.get("depend");
        List<String> depends = depend instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of(String.valueOf(depend));
        assertTrue(depends.contains("Voltexa"), "Bank must depend on the Voltexa core");
    }

    @Test
    void declaresTheBankCommand() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        Map<String, Object> commands = yml.get("commands") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        assertTrue(commands.containsKey("bank"), "the /bank command must be declared in plugin.yml");
    }
}
