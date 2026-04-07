package dev.tjxjnoobie.customminecraftserver.protocol.java.connection;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaConnectionHandler;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaPacketFrameDecoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaSessionService;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OfflineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OnlineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.configurationFinishedPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.handshakePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.loginAcknowledgedPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.loginStartPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.playKeepAliveResponsePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.playPongPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readFinishConfiguration;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readLegacyLoginSuccess;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readLegacyPlayDisconnect;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readModernLoginSuccess;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readModernPlayDisconnect;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readPlayKeepAlive;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readPlayPing;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.settingsPacket;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaHandshakeTest {
    private static final AttributeKey<ConnectionSession> SESSION_KEY = AttributeKey.valueOf("java-session");

    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void legacyLoginHandshakeProducesDeterministicDisconnect() {
        TestLogSupport.logTestStart("JavaHandshakeTest.legacyLoginHandshakeProducesDeterministicDisconnect");
        channel = createChannel(AuthMode.OFFLINE);

        channel.writeInbound(handshakePacket(47, "localhost", 25565, 2));
        channel.writeInbound(loginStartPacket("LegacyUser", null));

        JavaIntegrationTestSupport.LoginSuccess loginSuccess = readLegacyLoginSuccess(channel.readOutbound());
        String disconnectJson = readLegacyPlayDisconnect(channel.readOutbound());
        ConnectionSession session = channel.attr(SESSION_KEY).get();

        assertNotNull(session);
        assertEquals("JAVA_1_8_X", session.protocolFamily());
        assertEquals("LegacyUser", session.username());
        assertEquals("LegacyUser", loginSuccess.username());
        assertTrue(disconnectJson.contains("Custom server handshake reached successfully"));
        assertTrue(disconnectJson.contains("JAVA_1_8_X OFFLINE"));
    }

    @Test
    void modernLoginHandshakeProducesDeterministicDisconnect() {
        TestLogSupport.logTestStart("JavaHandshakeTest.modernLoginHandshakeProducesDeterministicDisconnect");
        channel = createChannel(AuthMode.OFFLINE);

        channel.writeInbound(handshakePacket(769, "localhost", 25565, 2));
        channel.writeInbound(loginStartPacket("ModernUser", UUID.fromString("11111111-2222-3333-4444-555555555555")));

        JavaIntegrationTestSupport.LoginSuccess loginSuccess = readModernLoginSuccess(channel.readOutbound());
        channel.writeInbound(loginAcknowledgedPacket());
        readFinishConfiguration(channel.readOutbound());
        channel.writeInbound(configurationFinishedPacket());
        long keepAliveId = readPlayKeepAlive(channel.readOutbound());
        channel.writeInbound(playKeepAliveResponsePacket(keepAliveId));
        channel.writeInbound(settingsPacket());
        int pingId = readPlayPing(channel.readOutbound());
        channel.writeInbound(playPongPacket(pingId));

        String disconnectJson = readModernPlayDisconnect(channel.readOutbound());
        ConnectionSession session = channel.attr(SESSION_KEY).get();

        assertNotNull(session);
        assertEquals("JAVA_1_21_X", session.protocolFamily());
        assertEquals("ModernUser", session.username());
        assertEquals("ModernUser", loginSuccess.username());
        assertTrue(disconnectJson.contains("JAVA_1_21_X OFFLINE"));
    }

    private EmbeddedChannel createChannel(AuthMode authMode) {
        ServerSettings settings = new ServerSettings(
                "127.0.0.1",
                25565,
                19132,
                "Custom server handshake reached successfully",
                128,
                true,
                authMode,
                JavaAuthenticationSettings.defaults(),
                BedrockAuthenticationSettings.defaults()
        );
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        JavaSessionService unusedSessionService = request -> java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("Java session service should not be used in OFFLINE tests")
        );
        JavaConnectionHandler handler = new JavaConnectionHandler(
                settings,
                registry,
                new StructuredConnectionLogger(),
                new ProtocolVersionDetector(),
                new JavaLoginCoordinator(new OfflineJavaLoginAdmission(), new OnlineJavaLoginAdmission()),
                unusedSessionService
        );
        return new EmbeddedChannel(new JavaPacketFrameDecoder(), handler);
    }
}

