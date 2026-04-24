package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.test.BedrockJwtTestSupport;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockJwtVerifierTest {
    @Test
    void verifiesIdentityChainAndExtractsHandshakeKey() throws Exception {
        TestLogSupport.logTestStart("BedrockJwtVerifierTest.verifiesIdentityChainAndExtractsHandshakeKey");
        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair trustedRootKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair identityKey = BedrockJwtTestSupport.generateEcKeyPair();

        String identityJson = BedrockJwtTestSupport.authenticatedIdentityJson(clientKey, trustedRootKey, identityKey);
        String clientPublic = BedrockJwtTestSupport.toBase64Der(clientKey);
        String identityPublic = BedrockJwtTestSupport.toBase64Der(identityKey);
        String clientJwt = BedrockJwtTestSupport.signEs384Jwt(
                identityKey,
                identityPublic,
                "{\"ThirdPartyName\":\"ClientUser\",\"identity\":\"client-identity\",\"XUID\":\"1\",\"cpk\":\"" + clientPublic + "\"}"
        );

        BedrockAuthenticationSettings settings = new BedrockAuthenticationSettings(true, List.of(
                BedrockJwtTestSupport.toBase64Der(trustedRootKey)
        ));

        BedrockJwtVerifier verifier = new BedrockJwtVerifier();
        BedrockAuthenticatedIdentity identity = verifier.verify(identityJson, clientJwt, settings);

        assertEquals("VerifiedBedrock", identity.displayName());
        assertEquals("bedrock-player-identity", identity.identity());
        assertEquals("2535400000000001", identity.xuid());
        assertNotNull(identity.handshakePublicKey());
        assertEquals(clientPublic, identity.handshakePublicKey());
        assertEquals(BedrockJwtTestSupport.toBase64Der(identityKey), identity.identityPublicKey());
    }

    @Test
    void extractsDisplayNameWithoutVerifying() throws Exception {
        TestLogSupport.logTestStart("BedrockJwtVerifierTest.extractsDisplayNameWithoutVerifying");
        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair trustedRootKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair identityKey = BedrockJwtTestSupport.generateEcKeyPair();

        String identityJson = BedrockJwtTestSupport.authenticatedIdentityJson(clientKey, trustedRootKey, identityKey);
        String clientPublic = BedrockJwtTestSupport.toBase64Der(clientKey);
        String clientJwt = BedrockJwtTestSupport.signEs384Jwt(clientKey, clientPublic, "{\"ThirdPartyName\":\"ClientUser\"}");

        BedrockJwtVerifier verifier = new BedrockJwtVerifier();
        assertEquals("VerifiedBedrock", verifier.extractDisplayName(identityJson, clientJwt));
    }
}
