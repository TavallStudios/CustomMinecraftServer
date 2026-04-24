package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLoginDelegationTest {
    @Test
    void offlineModeDelegatesToOfflineAdmissionUsingRealObjects() {
        TestLogSupport.logTestStart("JavaLoginDelegationTest.offlineModeDelegatesToOfflineAdmissionUsingRealObjects");
        AtomicInteger offlineCalls = new AtomicInteger();
        AtomicInteger onlineCalls = new AtomicInteger();
        JavaLoginAdmission offline = (session, packet) -> {
            offlineCalls.incrementAndGet();
            return new OfflineJavaLoginAdmission().decide(session, packet);
        };
        JavaLoginAdmission online = (session, packet) -> {
            onlineCalls.incrementAndGet();
            return new OnlineJavaLoginAdmission().decide(session, packet);
        };
        JavaLoginCoordinator coordinator = new JavaLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("127.0.0.1:25565", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.protocolFamily("JAVA_1_8_X");
        JavaLoginStartPacket packet = new JavaLoginStartPacket("LegacyUser", null, "");

        JavaLoginDecision decision = coordinator.decide(AuthMode.OFFLINE, session, packet);

        assertEquals(1, offlineCalls.get());
        assertEquals(0, onlineCalls.get());
        assertEquals(JavaLoginAction.DISCONNECT, decision.action());
        assertTrue(decision.disconnectMessage().contains("JAVA_1_8_X OFFLINE"));
    }

    @Test
    void onlineModeDelegatesToOnlineAdmissionUsingRealObjects() {
        TestLogSupport.logTestStart("JavaLoginDelegationTest.onlineModeDelegatesToOnlineAdmissionUsingRealObjects");
        AtomicInteger offlineCalls = new AtomicInteger();
        AtomicInteger onlineCalls = new AtomicInteger();
        JavaLoginAdmission offline = (session, packet) -> {
            offlineCalls.incrementAndGet();
            return new OfflineJavaLoginAdmission().decide(session, packet);
        };
        JavaLoginAdmission online = (session, packet) -> {
            onlineCalls.incrementAndGet();
            return new OnlineJavaLoginAdmission().decide(session, packet);
        };
        JavaLoginCoordinator coordinator = new JavaLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("127.0.0.1:25565", ConnectionEdition.JAVA, AuthMode.ONLINE);
        session.protocolFamily("JAVA_1_21_X");
        JavaLoginStartPacket packet = new JavaLoginStartPacket("ModernUser", null, "");

        JavaLoginDecision decision = coordinator.decide(AuthMode.ONLINE, session, packet);

        assertEquals(1, onlineCalls.get());
        assertEquals(0, offlineCalls.get());
        assertEquals(JavaLoginAction.REQUEST_ENCRYPTION, decision.action());
        assertNotNull(decision.encryptionChallenge());
    }
}

