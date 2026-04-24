package dev.tjxjnoobie.customminecraftserver.logging;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StructuredConnectionLoggerTest {
    @Test
    void logsLifecycleEventsWithoutFailure() {
        TestLogSupport.logTestStart("StructuredConnectionLoggerTest.logsLifecycleEventsWithoutFailure");
        StructuredConnectionLogger logger = new StructuredConnectionLogger();
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);

        assertDoesNotThrow(() -> logger.serverStartup(ServerSettings.defaults()));
        assertDoesNotThrow(() -> logger.serverStarted(ServerSettings.defaults()));
        assertDoesNotThrow(() -> logger.connectionOpened(session));
        assertDoesNotThrow(() -> logger.inboundPacket(session, "HANDSHAKE", Map.of("packetId", 0)));
        assertDoesNotThrow(() -> logger.outboundPacket(session, "STATUS", Map.of("packetId", 0)));
        assertDoesNotThrow(() -> logger.transition(session, SessionState.CONNECTED, SessionState.JAVA_HANDSHAKE, Map.of("reason", "test")));
        assertDoesNotThrow(() -> logger.warning(session, "warn", Map.of("detail", "test")));
        assertDoesNotThrow(() -> logger.connectionClosed(session, "done"));
    }
}
