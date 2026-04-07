package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockAuthenticatedIdentity;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockLoginDecisionTest {
    @Test
    void recordCarriesDecisionDetails() {
        TestLogSupport.logTestStart("BedrockLoginDecisionTest.recordCarriesDecisionDetails");
        BedrockAuthenticatedIdentity identity = new BedrockAuthenticatedIdentity("name", "id", "xuid", "identityKey", "handshakeKey");
        BedrockLoginDecision decision = new BedrockLoginDecision("message", "DISCONNECT", identity);
        assertEquals("message", decision.disconnectMessage());
        assertEquals("DISCONNECT", decision.responseType());
        assertEquals(identity, decision.authenticatedIdentity());
    }
}
