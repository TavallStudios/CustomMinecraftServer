package dev.tjxjnoobie.customminecraftserver.config;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerSettingsTest {
    @Test
    void defaultsApplyForBlankValues() {
        TestLogSupport.logTestStart("ServerSettingsTest.defaultsApplyForBlankValues");
        ServerSettings settings = new ServerSettings("", 0, 0, "", 0, true, null, null, null);
        assertEquals("0.0.0.0", settings.host());
        assertEquals(25565, settings.javaTcpPort());
        assertEquals(19132, settings.bedrockUdpPort());
        assertEquals("Custom server handshake reached successfully", settings.motd());
        assertEquals(128, settings.maxConnections());
        assertEquals(AuthMode.OFFLINE, settings.authMode());
    }

    @Test
    void defaultsFactoryUsesExpectedValues() {
        TestLogSupport.logTestStart("ServerSettingsTest.defaultsFactoryUsesExpectedValues");
        ServerSettings defaults = ServerSettings.defaults();
        assertEquals("0.0.0.0", defaults.host());
        assertEquals(25565, defaults.javaTcpPort());
        assertEquals(19132, defaults.bedrockUdpPort());
        assertEquals(AuthMode.OFFLINE, defaults.authMode());
    }
}
