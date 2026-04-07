package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OfflineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OnlineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JavaLoginDelegationTest {
    @Test
    void offlineModeDelegatesToOfflineAdmissionUsingRealObjects() {
        OfflineJavaLoginAdmission offline = Mockito.spy(new OfflineJavaLoginAdmission());
        OnlineJavaLoginAdmission online = Mockito.spy(new OnlineJavaLoginAdmission());
        JavaLoginCoordinator coordinator = new JavaLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("127.0.0.1:25565", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.protocolFamily("JAVA_1_8_X");
        JavaLoginStartPacket packet = new JavaLoginStartPacket("LegacyUser", null, "");

        JavaLoginDecision decision = coordinator.decide(AuthMode.OFFLINE, session, packet);

        verify(offline, times(1)).decide(session, packet);
        verify(online, times(0)).decide(session, packet);
        assertEquals(dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginAction.DISCONNECT, decision.action());
        assertTrue(decision.disconnectMessage().contains("JAVA_1_8_X OFFLINE"));
    }

    @Test
    void onlineModeDelegatesToOnlineAdmissionUsingRealObjects() {
        OfflineJavaLoginAdmission offline = Mockito.spy(new OfflineJavaLoginAdmission());
        OnlineJavaLoginAdmission online = Mockito.spy(new OnlineJavaLoginAdmission());
        JavaLoginCoordinator coordinator = new JavaLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("127.0.0.1:25565", ConnectionEdition.JAVA, AuthMode.ONLINE);
        session.protocolFamily("JAVA_1_21_X");
        JavaLoginStartPacket packet = new JavaLoginStartPacket("ModernUser", null, "");

        JavaLoginDecision decision = coordinator.decide(AuthMode.ONLINE, session, packet);

        verify(online, times(1)).decide(session, packet);
        verify(offline, times(0)).decide(session, packet);
        assertEquals(dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginAction.REQUEST_ENCRYPTION, decision.action());
        assertNotNull(decision.encryptionChallenge());
    }
}

