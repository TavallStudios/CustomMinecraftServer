package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaLoginStartPacket;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaLoginCoordinatorTest {
    @Test
    void choosesAdmissionByAuthMode() {
        TestLogSupport.logTestStart("JavaLoginCoordinatorTest.choosesAdmissionByAuthMode");
        AtomicInteger offlineCalls = new AtomicInteger();
        AtomicInteger onlineCalls = new AtomicInteger();
        JavaLoginAdmission offline = (session, packet) -> {
            offlineCalls.incrementAndGet();
            return JavaLoginDecision.disconnect("offline", "OFFLINE");
        };
        JavaLoginAdmission online = (session, packet) -> {
            onlineCalls.incrementAndGet();
            return JavaLoginDecision.disconnect("online", "ONLINE");
        };
        JavaLoginCoordinator coordinator = new JavaLoginCoordinator(offline, online);

        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        JavaLoginStartPacket packet = new JavaLoginStartPacket("Player", null, "");

        JavaLoginDecision offlineDecision = coordinator.decide(AuthMode.OFFLINE, session, packet);
        JavaLoginDecision onlineDecision = coordinator.decide(AuthMode.ONLINE, session, packet);

        assertEquals("OFFLINE", offlineDecision.responseType());
        assertEquals("ONLINE", onlineDecision.responseType());
        assertEquals(1, offlineCalls.get());
        assertEquals(1, onlineCalls.get());
    }
}
