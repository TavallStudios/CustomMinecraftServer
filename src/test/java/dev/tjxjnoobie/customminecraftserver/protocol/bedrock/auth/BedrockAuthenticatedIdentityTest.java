package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockAuthenticatedIdentityTest {
    @Test
    void recordCarriesIdentityValues() {
        TestLogSupport.logTestStart("BedrockAuthenticatedIdentityTest.recordCarriesIdentityValues");
        BedrockAuthenticatedIdentity identity = new BedrockAuthenticatedIdentity(
                "display",
                "identity",
                "xuid",
                "identityKey",
                "handshakeKey"
        );
        assertEquals("display", identity.displayName());
        assertEquals("identity", identity.identity());
        assertEquals("xuid", identity.xuid());
        assertEquals("identityKey", identity.identityPublicKey());
        assertEquals("handshakeKey", identity.handshakePublicKey());
    }
}
