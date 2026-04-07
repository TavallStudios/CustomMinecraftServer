package dev.tjxjnoobie.customminecraftserver.bootstrap;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConsoleSnapshotTest {
    @Test
    void recordCarriesValues() {
        TestLogSupport.logTestStart("ServerConsoleSnapshotTest.recordCarriesValues");
        ServerConsoleSnapshot snapshot = new ServerConsoleSnapshot("host", 25565, 19132, AuthMode.ONLINE, 5, true);
        assertEquals("host", snapshot.host());
        assertEquals(25565, snapshot.javaTcpPort());
        assertEquals(19132, snapshot.bedrockUdpPort());
        assertEquals(AuthMode.ONLINE, snapshot.authMode());
        assertEquals(5, snapshot.activeSessions());
        assertEquals(true, snapshot.running());
    }
}
