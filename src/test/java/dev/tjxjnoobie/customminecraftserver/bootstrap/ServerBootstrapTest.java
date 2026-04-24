package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerBootstrapTest {
    @Test
    void snapshotReflectsDefaultsBeforeStart() {
        TestLogSupport.logTestStart("ServerBootstrapTest.snapshotReflectsDefaultsBeforeStart");
        ServerSettings settings = ServerSettings.defaults();
        try (ServerBootstrap bootstrap = new ServerBootstrap(settings)) {
            ServerConsoleSnapshot snapshot = bootstrap.snapshot();
            assertNotNull(snapshot);
            assertEquals(settings.host(), snapshot.host());
            assertEquals(settings.javaTcpPort(), snapshot.javaTcpPort());
            assertEquals(settings.bedrockUdpPort(), snapshot.bedrockUdpPort());
            assertEquals(settings.authMode(), snapshot.authMode());
            assertEquals(0, snapshot.activeSessions());
            assertFalse(snapshot.running());
            assertTrue(bootstrap.activeSessionSummaryLines().isEmpty());
        }
    }
}
