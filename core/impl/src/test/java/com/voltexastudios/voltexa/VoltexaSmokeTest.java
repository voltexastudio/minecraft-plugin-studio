package com.voltexastudios.voltexa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Unit-level smoke test: verifies the plugin descriptor and main class are
 * consistent before the jar is loaded on a real Paper server. The server
 * smoke test is documented in {@code docs/plugins/voltexa/TESTPLAN.md}.
 */
class VoltexaSmokeTest {

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
        assertEquals("Voltexa", yml.get("name"));
        assertEquals("0.1.0", yml.get("version"));
        assertEquals("1.21", String.valueOf(yml.get("api-version")));
    }

    @Test
    void mainClassMatchesPluginYml() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        assertEquals(VoltexaPlugin.class.getName(), yml.get("main"));
    }

    @Test
    void mainClassExtendsJavaPlugin() {
        assertTrue(JavaPlugin.class.isAssignableFrom(VoltexaPlugin.class));
    }

    @Test
    void voltexaCommandIsDeclared() throws Exception {
        Map<String, Object> yml = loadPluginYml();
        Map<String, Object> commands = (Map<String, Object>) yml.get("commands");
        assertNotNull(commands, "commands section must exist");
        assertNotNull(commands.get("voltexa"), "/voltexa command must be declared");
    }
}
