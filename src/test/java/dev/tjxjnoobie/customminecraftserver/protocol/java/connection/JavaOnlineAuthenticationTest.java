package dev.tjxjnoobie.customminecraftserver.protocol.java.connection;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaConnectionHandler;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaEncryptionChallengeFactory;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaPacketFrameDecoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaSessionDigest;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.MojangJavaSessionService;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OfflineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OnlineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.netty.buffer.ByteBuf;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.awaitOutboundFrame;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.configurationFinishedPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.encryptionResponsePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.handshakePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.loginAcknowledgedPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.loginStartPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.playKeepAliveResponsePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.playPongPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.readEncryptionRequest;
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

class JavaOnlineAuthenticationTest {
    private static final AttributeKey<ConnectionSession> SESSION_KEY = AttributeKey.valueOf("java-session");

    private EmbeddedChannel channel;
    private HttpServer sessionServer;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
        if (sessionServer != null) {
            sessionServer.stop(0);
        }
    }

    @Test
    void legacyOnlineLoginPerformsSessionVerificationAndEncryptedDisconnect() throws Exception {
        TestLogSupport.logTestStart("JavaOnlineAuthenticationTest.legacyOnlineLoginPerformsSessionVerificationAndEncryptedDisconnect");
        OnlineFlowResult result = runOnlineFlow(
                47,
                "LegacyOnlineUser",
                null,
                false,
                "VerifiedLegacy",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                false
        );

        assertTrue(result.queryParameters().get("serverId").startsWith("-") || !result.queryParameters().get("serverId").isBlank());
        assertTrue(result.disconnectJson().contains("JAVA_1_8_X ONLINE profile=VerifiedLegacy"));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", result.session().authenticatedIdentity());
        assertEquals("VerifiedLegacy", result.session().username());
        assertEquals("VerifiedLegacy", result.loginSuccess().username());
    }

    @Test
    void modernOnlineLoginPerformsSessionVerificationAndEncryptedDisconnect() throws Exception {
        TestLogSupport.logTestStart("JavaOnlineAuthenticationTest.modernOnlineLoginPerformsSessionVerificationAndEncryptedDisconnect");
        OnlineFlowResult result = runOnlineFlow(
                769,
                "ModernOnlineUser",
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                true,
                "VerifiedModern",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                true
        );

        assertEquals("ModernOnlineUser", result.queryParameters().get("username"));
        assertTrue(result.disconnectJson().contains("JAVA_1_21_X ONLINE profile=VerifiedModern"));
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", result.session().authenticatedIdentity());
        assertEquals("VerifiedModern", result.session().username());
        assertEquals("VerifiedModern", result.loginSuccess().username());
    }

    private OnlineFlowResult runOnlineFlow(
            int protocolVersion,
            String loginUsername,
            UUID loginUuid,
            boolean expectShouldAuthenticate,
            String verifiedUsername,
            String verifiedProfileId,
            boolean modernPostLogin
    ) throws Exception {
        CompletableFuture<String> observedQuery = new CompletableFuture<>();
        sessionServer = startSessionServer(observedQuery, verifiedUsername, verifiedProfileId);

        JavaAuthenticationSettings authSettings = new JavaAuthenticationSettings(
                "http://127.0.0.1:" + sessionServer.getAddress().getPort(),
                false,
                1024
        );
        ServerSettings settings = new ServerSettings(
                "127.0.0.1",
                25565,
                19132,
                "Custom server handshake reached successfully",
                128,
                true,
                AuthMode.ONLINE,
                authSettings,
                BedrockAuthenticationSettings.defaults()
        );

        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        JavaConnectionHandler handler = new JavaConnectionHandler(
                settings,
                registry,
                new StructuredConnectionLogger(),
                new ProtocolVersionDetector(),
                new JavaLoginCoordinator(
                        new OfflineJavaLoginAdmission(),
                        new OnlineJavaLoginAdmission(JavaEncryptionChallengeFactory.createGenerated(authSettings.rsaKeySizeBits()))
                ),
                new MojangJavaSessionService(authSettings)
        );

        channel = new EmbeddedChannel(new JavaPacketFrameDecoder(), handler);

        channel.writeInbound(handshakePacket(protocolVersion, "localhost", 25565, 2));
        channel.writeInbound(loginStartPacket(loginUsername, loginUuid));

        JavaIntegrationTestSupport.EncryptionRequest request = readEncryptionRequest(awaitOutboundFrame(channel));
        assertEquals(expectShouldAuthenticate, request.shouldAuthenticate());

        byte[] sharedSecret = new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        };
        String expectedServerHash = JavaSessionDigest.serverHash(request.serverId(), sharedSecret, request.publicKeyBytes());
        JavaIntegrationTestSupport.JavaCipherStream cipherStream = new JavaIntegrationTestSupport.JavaCipherStream(sharedSecret);

        channel.writeInbound(encryptionResponsePacket(request.publicKeyBytes(), request.verifyToken(), sharedSecret));

        String rawQuery = observedQuery.get(5, TimeUnit.SECONDS);
        Map<String, String> queryParameters = parseQuery(rawQuery);
        assertEquals(loginUsername, queryParameters.get("username"));
        assertEquals(expectedServerHash, queryParameters.get("serverId"));

        JavaIntegrationTestSupport.LoginSuccess loginSuccess = modernPostLogin
                ? readModernLoginSuccess(cipherStream.decrypt(awaitOutboundFrame(channel)))
                : readLegacyLoginSuccess(cipherStream.decrypt(awaitOutboundFrame(channel)));
        if (modernPostLogin) {
            channel.writeInbound(cipherStream.encrypt(loginAcknowledgedPacket()));
            readFinishConfiguration(cipherStream.decrypt(awaitOutboundFrame(channel)));
            channel.writeInbound(cipherStream.encrypt(configurationFinishedPacket()));
            long keepAliveId = readPlayKeepAlive(cipherStream.decrypt(awaitOutboundFrame(channel)));
            channel.writeInbound(cipherStream.encrypt(playKeepAliveResponsePacket(keepAliveId)));
            channel.writeInbound(cipherStream.encrypt(settingsPacket()));
            int pingId = readPlayPing(cipherStream.decrypt(awaitOutboundFrame(channel)));
            channel.writeInbound(cipherStream.encrypt(playPongPacket(pingId)));
        }

        ByteBuf encryptedDisconnectFrame = awaitOutboundFrame(channel);
        String disconnectJson = modernPostLogin
                ? readModernPlayDisconnect(cipherStream.decrypt(encryptedDisconnectFrame))
                : readLegacyPlayDisconnect(cipherStream.decrypt(encryptedDisconnectFrame));
        ConnectionSession session = channel.attr(SESSION_KEY).get();
        assertNotNull(session);

        return new OnlineFlowResult(queryParameters, loginSuccess, disconnectJson, session);
    }

    private HttpServer startSessionServer(
            CompletableFuture<String> observedQuery,
            String verifiedUsername,
            String verifiedProfileId
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/session/minecraft/hasJoined", exchange -> writeVerificationResponse(
                exchange,
                observedQuery,
                verifiedUsername,
                verifiedProfileId
        ));
        server.start();
        return server;
    }

    private void writeVerificationResponse(
            HttpExchange exchange,
            CompletableFuture<String> observedQuery,
            String verifiedUsername,
            String verifiedProfileId
    ) throws IOException {
        observedQuery.complete(exchange.getRequestURI().getQuery());
        byte[] body = ("{\"id\":\"" + verifiedProfileId + "\",\"name\":\"" + verifiedUsername + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private Map<String, String> parseQuery(String query) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : ""
            );
        }
        return values;
    }

    private record OnlineFlowResult(
            Map<String, String> queryParameters,
            JavaIntegrationTestSupport.LoginSuccess loginSuccess,
            String disconnectJson,
            ConnectionSession session
    ) {
    }
}

