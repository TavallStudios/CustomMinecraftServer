package com.customminecraftserver.integration;

import com.customminecraftserver.configuration.AuthMode;
import com.customminecraftserver.configuration.BedrockAuthenticationSettings;
import com.customminecraftserver.configuration.JavaAuthenticationSettings;
import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.javaedition.JavaConnectionHandler;
import com.customminecraftserver.javaedition.JavaLoginCoordinator;
import com.customminecraftserver.javaedition.JavaPacketFrameDecoder;
import com.customminecraftserver.javaedition.JavaSessionService;
import com.customminecraftserver.javaedition.OfflineJavaLoginAdmission;
import com.customminecraftserver.javaedition.OnlineJavaLoginAdmission;
import com.customminecraftserver.logging.StructuredConnectionLogger;
import com.customminecraftserver.networking.ProtocolVersionDetector;
import com.customminecraftserver.session.ConnectionSession;
import com.customminecraftserver.session.ConnectionSessionRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.customminecraftserver.integration.JavaIntegrationTestSupport.configurationFinishedPacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.handshakePacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.loginAcknowledgedPacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.loginStartPacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.playKeepAliveResponsePacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.playPongPacket;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readFinishConfiguration;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readLegacyLoginSuccess;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readLegacyPlayDisconnect;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readModernLoginSuccess;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readModernPlayDisconnect;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readPlayKeepAlive;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.readPlayPing;
import static com.customminecraftserver.integration.JavaIntegrationTestSupport.settingsPacket;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaHandshakeIntegrationTest {
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
