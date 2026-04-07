package dev.tjxjnoobie.customminecraftserver.config;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSettingsLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDefaultSettingsWhenMissing() throws Exception {
        TestLogSupport.logTestStart("ServerSettingsLoaderTest.createsDefaultSettingsWhenMissing");
        Path settingsPath = tempDir.resolve("server-settings.json");
        ServerSettingsLoader loader = new ServerSettingsLoader();
        ServerSettings settings = loader.load(settingsPath);

        assertTrue(Files.exists(settingsPath));
        assertEquals(ServerSettings.defaults().host(), settings.host());
        assertEquals(ServerSettings.defaults().javaTcpPort(), settings.javaTcpPort());
    }
}
