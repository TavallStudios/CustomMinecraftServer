package dev.tjxjnoobie.customminecraftserver.session;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConnectionSessionTest {
    @Test
    void sessionTracksMutableState() {
        TestLogSupport.logTestStart("ConnectionSessionTest.sessionTracksMutableState");
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        assertNotNull(session.sessionId());
        assertEquals(SessionState.CONNECTED, session.state());

        session.protocolVersion(47);
        session.protocolFamily("JAVA_1_8_X");
        session.username("player");
        session.requestedHost("host");
        session.requestedPort(25565);
        session.authenticatedIdentity("identity");
        session.authenticatedXuid("xuid");
        session.state(SessionState.JAVA_LOGIN);

        assertEquals(47, session.protocolVersion());
        assertEquals("JAVA_1_8_X", session.protocolFamily());
        assertEquals("player", session.username());
        assertEquals("host", session.requestedHost());
        assertEquals(25565, session.requestedPort());
        assertEquals("identity", session.authenticatedIdentity());
        assertEquals("xuid", session.authenticatedXuid());
        assertEquals(SessionState.JAVA_LOGIN, session.state());
    }
}
