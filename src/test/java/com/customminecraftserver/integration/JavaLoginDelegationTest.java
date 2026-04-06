package com.customminecraftserver.integration;

import com.customminecraftserver.configuration.AuthMode;
import com.customminecraftserver.javaedition.JavaLoginCoordinator;
import com.customminecraftserver.javaedition.JavaLoginDecision;
import com.customminecraftserver.javaedition.JavaLoginStartPacket;
import com.customminecraftserver.javaedition.OfflineJavaLoginAdmission;
import com.customminecraftserver.javaedition.OnlineJavaLoginAdmission;
import com.customminecraftserver.session.ConnectionEdition;
import com.customminecraftserver.session.ConnectionSession;
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
        assertEquals(com.customminecraftserver.javaedition.JavaLoginAction.DISCONNECT, decision.action());
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
        assertEquals(com.customminecraftserver.javaedition.JavaLoginAction.REQUEST_ENCRYPTION, decision.action());
        assertNotNull(decision.encryptionChallenge());
    }
}
