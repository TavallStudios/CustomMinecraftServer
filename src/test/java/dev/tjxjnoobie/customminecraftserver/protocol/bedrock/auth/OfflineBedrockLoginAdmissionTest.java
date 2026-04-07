package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginPayload;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.BedrockJwtTestSupport;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OfflineBedrockLoginAdmissionTest {
    @Test
    void offlineAdmissionBuildsDecisionFromPayload() throws Exception {
        TestLogSupport.logTestStart("OfflineBedrockLoginAdmissionTest.offlineAdmissionBuildsDecisionFromPayload");
        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair trustedRootKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair identityKey = BedrockJwtTestSupport.generateEcKeyPair();

        String identityJson = BedrockJwtTestSupport.authenticatedIdentityJson(clientKey, trustedRootKey, identityKey);
        String clientPublic = BedrockJwtTestSupport.toBase64Der(clientKey);
        String clientJwt = BedrockJwtTestSupport.signEs384Jwt(clientKey, clientPublic, "{\"ThirdPartyName\":\"ClientUser\",\"cpk\":\"" + clientPublic + "\"}");

        BedrockLoginPayload payload = new BedrockLoginPayload(944, identityJson, clientJwt);
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.BEDROCK, AuthMode.OFFLINE);
        OfflineBedrockLoginAdmission admission = new OfflineBedrockLoginAdmission(new BedrockJwtVerifier());

        BedrockLoginDecision decision = admission.decide(session, payload);
        assertEquals("DISCONNECT", decision.responseType());
        assertNotNull(decision.authenticatedIdentity());
        assertEquals("VerifiedBedrock", decision.authenticatedIdentity().displayName());
        assertEquals(clientPublic, decision.authenticatedIdentity().handshakePublicKey());
    }
}
