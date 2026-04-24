package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockJwtVerifier;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OfflineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OnlineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.assertPacketId;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.datagram;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.unconnectedPing;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockDatagramHandlerTest {
    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void respondsToUnconnectedPing() {
        TestLogSupport.logTestStart("BedrockDatagramHandlerTest.respondsToUnconnectedPing");
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        BedrockJwtVerifier jwtVerifier = new BedrockJwtVerifier();
        BedrockLoginCoordinator loginCoordinator = new BedrockLoginCoordinator(
                new OfflineBedrockLoginAdmission(jwtVerifier),
                new OnlineBedrockLoginAdmission(jwtVerifier, BedrockAuthenticationSettings.defaults())
        );
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

        channel = new EmbeddedChannel(new BedrockDatagramHandler(
                settings,
                registry,
                new StructuredConnectionLogger(),
                new ProtocolVersionDetector(),
                loginCoordinator
        ));

        InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 19132);
        InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 50001);
        channel.writeInbound(datagram(unconnectedPing(), serverAddress, clientAddress));

        DatagramPacket outbound = channel.readOutbound();
        assertNotNull(outbound);
        assertPacketId(outbound, BedrockPacketIds.RAKNET_UNCONNECTED_PONG);
    }
}
