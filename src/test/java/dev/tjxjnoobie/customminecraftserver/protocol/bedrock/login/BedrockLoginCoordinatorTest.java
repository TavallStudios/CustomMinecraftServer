package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockLoginCoordinatorTest {
    @Test
    void choosesAdmissionBasedOnAuthMode() {
        TestLogSupport.logTestStart("BedrockLoginCoordinatorTest.choosesAdmissionBasedOnAuthMode");
        BedrockLoginAdmission offline = (session, payload) -> new BedrockLoginDecision("offline", "OFFLINE", null);
        BedrockLoginAdmission online = (session, payload) -> new BedrockLoginDecision("online", "ONLINE", null);
        BedrockLoginCoordinator coordinator = new BedrockLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.BEDROCK, AuthMode.OFFLINE);
        BedrockLoginPayload payload = new BedrockLoginPayload(944, "identity", "jwt");

        BedrockLoginDecision offlineDecision = coordinator.decide(AuthMode.OFFLINE, session, payload);
        BedrockLoginDecision onlineDecision = coordinator.decide(AuthMode.ONLINE, session, payload);

        assertEquals("OFFLINE", offlineDecision.responseType());
        assertEquals("ONLINE", onlineDecision.responseType());
    }
}
