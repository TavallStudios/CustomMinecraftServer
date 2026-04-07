package dev.tjxjnoobie.customminecraftserver.config;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthModeTest {
    @Test
    void enumContainsExpectedModes() {
        TestLogSupport.logTestStart("AuthModeTest.enumContainsExpectedModes");
        assertNotNull(AuthMode.valueOf("OFFLINE"));
        assertNotNull(AuthMode.valueOf("ONLINE"));
        assertEquals(2, AuthMode.values().length);
    }
}
