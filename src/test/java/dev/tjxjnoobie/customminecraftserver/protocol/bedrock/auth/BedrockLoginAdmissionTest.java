package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginPayload;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockLoginAdmissionTest {
    @Test
    void admissionImplementationsCanDecideUsingRealPayload() {
        TestLogSupport.logTestStart("BedrockLoginAdmissionTest.admissionImplementationsCanDecideUsingRealPayload");
        BedrockLoginAdmission admission = (session, payload) -> new BedrockLoginDecision("ok", "DECISION", null);
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.BEDROCK, AuthMode.OFFLINE);
        BedrockLoginPayload payload = new BedrockLoginPayload(898, "identity", "jwt");

        BedrockLoginDecision decision = admission.decide(session, payload);
        assertEquals("DECISION", decision.responseType());
    }
}
