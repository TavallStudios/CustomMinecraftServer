package dev.tjxjnoobie.customminecraftserver.protocol.java.connection;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.protocol.java.auth.JavaSessionService;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaPacketFrameDecoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftVarInt;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OfflineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.OnlineJavaLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaPacketIds;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaIntegrationTestSupport.handshakePacket;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaConnectionHandlerTest {
    @Test
    void statusRequestReturnsStatusResponse() {
        TestLogSupport.logTestStart("JavaConnectionHandlerTest.statusRequestReturnsStatusResponse");
        ServerSettings settings = new ServerSettings(
                "127.0.0.1",
                25565,
                19132,
                "Custom server handshake reached successfully",
                128,
                true,
                AuthMode.OFFLINE,
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

        EmbeddedChannel channel = new EmbeddedChannel(new JavaPacketFrameDecoder(), handler);
        channel.writeInbound(handshakePacket(47, "localhost", 25565, 1));

        ByteBuf statusRequest = framedPacket(JavaPacketIds.STATUS_REQUEST);
        channel.writeInbound(statusRequest);

        ByteBuf response = channel.readOutbound();
        assertNotNull(response);
        MinecraftVarInt.read(response);
        assertEquals(JavaPacketIds.STATUS_RESPONSE, MinecraftVarInt.read(response));
        String json = MinecraftStringCodec.read(response, 512);
        assertNotNull(json);
        response.release();
        channel.finishAndReleaseAll();
    }

    private ByteBuf framedPacket(int packetId) {
        ByteBuf payload = Unpooled.buffer();
        ByteBuf frame = Unpooled.buffer();
        MinecraftVarInt.write(payload, packetId);
        MinecraftVarInt.write(frame, payload.readableBytes());
        frame.writeBytes(payload);
        payload.release();
        return frame;
    }
}
