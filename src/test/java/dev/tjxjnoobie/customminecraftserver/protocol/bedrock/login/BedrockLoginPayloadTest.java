package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockLoginPayloadTest {
    @Test
    void recordCarriesPayloadDetails() {
        TestLogSupport.logTestStart("BedrockLoginPayloadTest.recordCarriesPayloadDetails");
        BedrockLoginPayload payload = new BedrockLoginPayload(944, "identity", "jwt");
        assertEquals(944, payload.protocolVersion());
        assertEquals("identity", payload.identityJson());
        assertEquals("jwt", payload.clientJwt());
    }
}
