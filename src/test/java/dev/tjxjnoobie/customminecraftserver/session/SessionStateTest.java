package dev.tjxjnoobie.customminecraftserver.session;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionStateTest {
    @Test
    void enumDefinesExpectedStates() {
        TestLogSupport.logTestStart("SessionStateTest.enumDefinesExpectedStates");
        assertTrue(SessionState.valueOf("JAVA_HANDSHAKE") != null);
        assertTrue(SessionState.valueOf("BEDROCK_LOGIN") != null);
        assertTrue(SessionState.values().length > 5);
    }
}
