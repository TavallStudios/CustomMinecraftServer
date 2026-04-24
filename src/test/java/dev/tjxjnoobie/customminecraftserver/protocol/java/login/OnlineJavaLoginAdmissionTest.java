package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallengeFactory;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OnlineJavaLoginAdmissionTest {
    @Test
    void requestsEncryptionChallenge() {
        TestLogSupport.logTestStart("OnlineJavaLoginAdmissionTest.requestsEncryptionChallenge");
        JavaEncryptionChallengeFactory factory = JavaEncryptionChallengeFactory.createGenerated(1024);
        OnlineJavaLoginAdmission admission = new OnlineJavaLoginAdmission(factory);

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.ONLINE);
        JavaLoginDecision decision = admission.decide(session, new JavaLoginStartPacket("User", null, ""));

        assertEquals(JavaLoginAction.REQUEST_ENCRYPTION, decision.action());
        assertNotNull(decision.encryptionChallenge());
    }
}
