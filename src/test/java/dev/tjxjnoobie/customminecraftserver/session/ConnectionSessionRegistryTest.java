package dev.tjxjnoobie.customminecraftserver.session;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConnectionSessionRegistryTest {
    @Test
    void registersAndUnregistersJavaSessions() {
        TestLogSupport.logTestStart("ConnectionSessionRegistryTest.registersAndUnregistersJavaSessions");
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();

        ConnectionSession session = registry.registerJava(channel, AuthMode.OFFLINE);
        assertEquals(1, registry.activeSessions().size());
        assertEquals(SessionState.JAVA_HANDSHAKE, session.state());

        registry.unregisterJava(channel);
        assertEquals(0, registry.activeSessions().size());
    }

    @Test
    void reusesBedrockSessionForSameAddress() {
        TestLogSupport.logTestStart("ConnectionSessionRegistryTest.reusesBedrockSessionForSameAddress");
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 19132);

        ConnectionSession first = registry.registerBedrock(address, AuthMode.OFFLINE);
        ConnectionSession second = registry.registerBedrock(address, AuthMode.OFFLINE);
        assertSame(first, second);
    }
}
