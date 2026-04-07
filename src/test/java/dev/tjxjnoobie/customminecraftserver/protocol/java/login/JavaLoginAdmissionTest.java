package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaLoginAdmissionTest {
    @Test
    void admissionImplementationsCanDecideUsingRealPayload() {
        TestLogSupport.logTestStart("JavaLoginAdmissionTest.admissionImplementationsCanDecideUsingRealPayload");
        JavaLoginAdmission admission = (session, packet) -> JavaLoginDecision.disconnect("bye", "DISCONNECT");
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        JavaLoginStartPacket packet = new JavaLoginStartPacket("Player", null, "");

        JavaLoginDecision decision = admission.decide(session, packet);
        assertEquals("DISCONNECT", decision.responseType());
    }
}
