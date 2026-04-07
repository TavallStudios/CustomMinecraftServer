package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramHandler;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockGamePacketWriter;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockJwtVerifier;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OfflineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.OnlineBedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.config.BedrockAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.JavaAuthenticationSettings;
import dev.tjxjnoobie.customminecraftserver.config.ServerSettings;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSessionRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;

import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.assertAckPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.clientCacheStatusPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.clientToServerHandshakePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.connectedDatagram;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.connectedReliableDatagram;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.connectionRequestPayload;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.datagram;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.onlineLoginBatch;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.openConnectionRequest1;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.openConnectionRequest2;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.assertPacketId;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readChunkRadiusUpdate;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readDisconnectMessage;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readPlayStatus;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readResourcePackStackVersion;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readResourcePacksInfoVersion;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readServerHandshakeToken;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.readStartGameRuntimeEntityId;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.requestChunkRadiusPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.requestNetworkSettingsBatch;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.resourcePackClientResponsePacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.setLocalPlayerAsInitializedPacket;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.singleBytePayload;
import static dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockIntegrationTestSupport.unconnectedPing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockOnlineAuthenticationTest {
    private final InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 19132);
    private final InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 50001);
    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void bedrockOnlineLoginVerifiesTrustedChainAndDisconnectsDeterministically() throws Exception {
        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair trustedRootKey = BedrockJwtTestSupport.generateEcKeyPair();
        KeyPair identityKey = BedrockJwtTestSupport.generateEcKeyPair();

        String trustedRootPublicKey = BedrockJwtTestSupport.toBase64Der(trustedRootKey);
        ConnectionSessionRegistry registry = new ConnectionSessionRegistry();
        BedrockJwtVerifier jwtVerifier = new BedrockJwtVerifier();
        BedrockAuthenticationSettings authSettings = new BedrockAuthenticationSettings(true, List.of(trustedRootPublicKey));

        channel = new EmbeddedChannel(new BedrockDatagramHandler(
                new ServerSettings(
                        "127.0.0.1",
                        25565,
                        19132,
                        "Custom server handshake reached successfully",
                        128,
                        true,
                        AuthMode.ONLINE,
                        JavaAuthenticationSettings.defaults(),
                        authSettings
                ),
                registry,
                new StructuredConnectionLogger(),
                new ProtocolVersionDetector(),
                new BedrockLoginCoordinator(
                        new OfflineBedrockLoginAdmission(jwtVerifier),
                        new OnlineBedrockLoginAdmission(jwtVerifier, authSettings)
                )
        ));

        channel.writeInbound(datagram(unconnectedPing(), serverAddress, clientAddress));
        channel.readOutbound();

        channel.writeInbound(datagram(openConnectionRequest1(11, 1400), serverAddress, clientAddress));
        assertPacketId(channel.readOutbound(), BedrockPacketIds.RAKNET_OPEN_CONNECTION_REPLY_1);

        channel.writeInbound(datagram(openConnectionRequest2(serverAddress, 1400, 55L), serverAddress, clientAddress));
        assertPacketId(channel.readOutbound(), BedrockPacketIds.RAKNET_OPEN_CONNECTION_REPLY_2);

        channel.writeInbound(datagram(connectedDatagram(0, 0, connectionRequestPayload(55L, 77L)), serverAddress, clientAddress));
        assertAckPacket(channel.readOutbound());
        channel.readOutbound();

        channel.writeInbound(datagram(
                connectedDatagram(1, 0, singleBytePayload(BedrockPacketIds.RAKNET_NEW_INCOMING_CONNECTION)),
                serverAddress,
                clientAddress
        ));
        assertAckPacket(channel.readOutbound());

        channel.writeInbound(datagram(connectedReliableDatagram(2, 0, 0, requestNetworkSettingsBatch(944)), serverAddress, clientAddress));
        assertAckPacket(channel.readOutbound());
        channel.readOutbound();

        String identityJson = BedrockJwtTestSupport.authenticatedIdentityJson(clientKey, trustedRootKey, identityKey);
        String clientJwt = BedrockJwtTestSupport.signEs384Jwt(
                identityKey,
                BedrockJwtTestSupport.toBase64Der(identityKey),
                "{\"ThirdPartyName\":\"VerifiedBedrock\"}"
        );
        channel.writeInbound(datagram(
                connectedReliableDatagram(3, 1, 1, onlineLoginBatch(944, identityJson, clientJwt)),
                serverAddress,
                clientAddress
        ));

        assertAckPacket(channel.readOutbound());
        DatagramPacket handshake = channel.readOutbound();
        BedrockClientSecureSession secureSession = new BedrockClientSecureSession(identityKey.getPrivate(), readServerHandshakeToken(handshake));

        channel.writeInbound(datagram(
                connectedReliableDatagram(4, 2, 2, secureSession.encrypt(clientToServerHandshakePacket())),
                serverAddress,
                clientAddress
        ));

        DatagramPacket ackForHandshake = channel.readOutbound();
        DatagramPacket playStatus = channel.readOutbound();
        DatagramPacket resourcePacksInfo = channel.readOutbound();
        assertAckPacket(ackForHandshake);
        assertEquals("login_success", readPlayStatus(playStatus, secureSession));
        assertEquals("0.0.0", readResourcePacksInfoVersion(resourcePacksInfo, secureSession));

        channel.writeInbound(datagram(
                connectedReliableDatagram(5, 3, 3, secureSession.encrypt(resourcePackClientResponsePacket())),
                serverAddress,
                clientAddress
        ));
        DatagramPacket ackForResourcePackResponse = channel.readOutbound();
        DatagramPacket resourcePackStack = channel.readOutbound();
        assertAckPacket(ackForResourcePackResponse);
        assertEquals("26.10", readResourcePackStackVersion(resourcePackStack, secureSession, 944));

        channel.writeInbound(datagram(
                connectedReliableDatagram(6, 4, 4, secureSession.encrypt(clientCacheStatusPacket(false))),
                serverAddress,
                clientAddress
        ));
        DatagramPacket ackForClientCacheStatus = channel.readOutbound();
        assertAckPacket(ackForClientCacheStatus);

        channel.writeInbound(datagram(
                connectedReliableDatagram(7, 5, 5, secureSession.encrypt(resourcePackClientResponsePacket())),
                serverAddress,
                clientAddress
        ));
        DatagramPacket ackForCompletedResourcePackNegotiation = channel.readOutbound();
        assertAckPacket(ackForCompletedResourcePackNegotiation);

        channel.writeInbound(datagram(
                connectedReliableDatagram(8, 6, 6, secureSession.encrypt(requestChunkRadiusPacket(10, 16))),
                serverAddress,
                clientAddress
        ));
        DatagramPacket ackForChunkRadiusRequest = channel.readOutbound();
        DatagramPacket chunkRadiusUpdate = channel.readOutbound();
        DatagramPacket startGame = channel.readOutbound();
        DatagramPacket playerSpawn = channel.readOutbound();
        assertAckPacket(ackForChunkRadiusRequest);
        assertEquals(10, readChunkRadiusUpdate(chunkRadiusUpdate, secureSession));
        assertEquals(BedrockGamePacketWriter.START_GAME_RUNTIME_ENTITY_ID, readStartGameRuntimeEntityId(startGame, secureSession));
        assertEquals("player_spawn", readPlayStatus(playerSpawn, secureSession));

        channel.writeInbound(datagram(
                connectedReliableDatagram(
                        9,
                        7,
                        7,
                        secureSession.encrypt(setLocalPlayerAsInitializedPacket(BedrockGamePacketWriter.START_GAME_RUNTIME_ENTITY_ID))
                ),
                serverAddress,
                clientAddress
        ));
        DatagramPacket ackForLocalPlayerInitialization = channel.readOutbound();
        DatagramPacket disconnect = channel.readOutbound();
        assertAckPacket(ackForLocalPlayerInitialization);
        String disconnectMessage = readDisconnectMessage(disconnect, secureSession);
        assertTrue(disconnectMessage.contains("BEDROCK protocol=944 ONLINE xuid=2535400000000001"));

        ConnectionSession session = registry.activeSessions().stream().findFirst().orElse(null);
        assertNotNull(session);
        assertEquals("VerifiedBedrock", session.username());
        assertEquals("bedrock-player-identity", session.authenticatedIdentity());
        assertEquals("2535400000000001", session.authenticatedXuid());
    }
}

