package com.customminecraftserver.integration;

import com.customminecraftserver.bedrock.BedrockDatagramHandler;
import com.customminecraftserver.bedrock.BedrockGamePacketWriter;
import com.customminecraftserver.bedrock.BedrockJwtVerifier;
import com.customminecraftserver.bedrock.BedrockLoginCoordinator;
import com.customminecraftserver.bedrock.BedrockPacketIds;
import com.customminecraftserver.bedrock.OfflineBedrockLoginAdmission;
import com.customminecraftserver.bedrock.OnlineBedrockLoginAdmission;
import com.customminecraftserver.configuration.AuthMode;
import com.customminecraftserver.configuration.BedrockAuthenticationSettings;
import com.customminecraftserver.configuration.JavaAuthenticationSettings;
import com.customminecraftserver.configuration.ServerSettings;
import com.customminecraftserver.logging.StructuredConnectionLogger;
import com.customminecraftserver.networking.ProtocolVersionDetector;
import com.customminecraftserver.session.ConnectionSession;
import com.customminecraftserver.session.ConnectionSessionRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;

import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.assertAckPacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.clientCacheStatusPacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.clientToServerHandshakePacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.connectedDatagram;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.connectedReliableDatagram;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.connectionRequestPayload;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.datagram;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.onlineLoginBatch;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.openConnectionRequest1;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.openConnectionRequest2;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.assertPacketId;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readChunkRadiusUpdate;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readDisconnectMessage;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readPlayStatus;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readResourcePackStackVersion;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readResourcePacksInfoVersion;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readServerHandshakeToken;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.readStartGameRuntimeEntityId;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.requestChunkRadiusPacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.requestNetworkSettingsBatch;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.resourcePackClientResponsePacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.setLocalPlayerAsInitializedPacket;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.singleBytePayload;
import static com.customminecraftserver.integration.BedrockIntegrationTestSupport.unconnectedPing;
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
