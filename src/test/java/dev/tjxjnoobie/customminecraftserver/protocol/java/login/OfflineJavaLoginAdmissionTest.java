package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineJavaLoginAdmissionTest {
    @Test
    void buildsOfflineDisconnectMessage() {
        TestLogSupport.logTestStart("OfflineJavaLoginAdmissionTest.buildsOfflineDisconnectMessage");
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.protocolFamily("JAVA_1_8_X");
        JavaLoginDecision decision = new OfflineJavaLoginAdmission().decide(session, new JavaLoginStartPacket("User", null, ""));

        assertEquals(JavaLoginAction.DISCONNECT, decision.action());
        assertTrue(decision.disconnectMessage().contains("JAVA_1_8_X OFFLINE"));
    }
}
