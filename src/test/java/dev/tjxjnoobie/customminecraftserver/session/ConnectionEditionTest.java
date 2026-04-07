package dev.tjxjnoobie.customminecraftserver.session;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionEditionTest {
    @Test
    void enumContainsExpectedEditions() {
        TestLogSupport.logTestStart("ConnectionEditionTest.enumContainsExpectedEditions");
        assertEquals(ConnectionEdition.JAVA, ConnectionEdition.valueOf("JAVA"));
        assertEquals(ConnectionEdition.BEDROCK, ConnectionEdition.valueOf("BEDROCK"));
    }
}
