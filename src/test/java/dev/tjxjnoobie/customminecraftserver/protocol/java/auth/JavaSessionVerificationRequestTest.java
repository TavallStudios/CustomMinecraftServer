package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSessionVerificationRequestTest {
    @Test
    void recordCarriesSessionVerificationFields() {
        TestLogSupport.logTestStart("JavaSessionVerificationRequestTest.recordCarriesSessionVerificationFields");
        JavaSessionVerificationRequest request = new JavaSessionVerificationRequest(
                "Player",
                "server",
                new byte[]{1},
                new byte[]{2},
                "127.0.0.1:25565"
        );

        assertEquals("Player", request.username());
        assertEquals("server", request.serverId());
        assertEquals("127.0.0.1:25565", request.remoteAddress());
    }
}
