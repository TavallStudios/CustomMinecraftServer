package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramTransport;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.BedrockClientSessionCrypto;
import dev.tjxjnoobie.customminecraftserver.test.BedrockJwtTestSupport;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockSecureSessionCoordinatorTest {
    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void handlesClientToServerHandshakeWithEncryptedResponses() throws Exception {
        TestLogSupport.logTestStart("BedrockSecureSessionCoordinatorTest.handlesClientToServerHandshakeWithEncryptedResponses");
        BedrockDatagramTransport transport = new BedrockDatagramTransport();
        BedrockInitializationCoordinator initializationCoordinator = new BedrockInitializationCoordinator(
                new StructuredConnectionLogger(),
                transport
        );
        BedrockSecureSessionCoordinator coordinator = new BedrockSecureSessionCoordinator(
                new StructuredConnectionLogger(),
                transport,
                initializationCoordinator
        );

        channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();

        ConnectionSession session = new ConnectionSession("127.0.0.1:19132", ConnectionEdition.BEDROCK, AuthMode.OFFLINE);
        session.protocolVersion(898);

        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 19132);
        BedrockPeerSession peerSession = new BedrockPeerSession(address);

        KeyPair clientKey = BedrockJwtTestSupport.generateEcKeyPair();
        BedrockSecureSession secureSession = BedrockSecureSession.create(BedrockJwtTestSupport.toBase64Der(clientKey));
        peerSession.secureSession(secureSession);

        BedrockClientSessionCrypto clientCrypto = new BedrockClientSessionCrypto(
                clientKey.getPrivate(),
                secureSession.serverHandshakeToken()
        );

        ByteBuf handshakePacket = io.netty.buffer.Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(handshakePacket, BedrockPacketIds.BEDROCK_CLIENT_TO_SERVER_HANDSHAKE);
        ByteBuf encryptedHandshake = clientCrypto.encrypt(handshakePacket);

        coordinator.handleEncryptedBedrockBatch(context, session, peerSession, encryptedHandshake);

        DatagramPacket playStatusDatagram = channel.readOutbound();
        DatagramPacket resourcePacksDatagram = channel.readOutbound();
        assertNotNull(playStatusDatagram);
        assertNotNull(resourcePacksDatagram);

        ByteBuf playStatusPayload = readConnectedPayload(playStatusDatagram);
        ByteBuf playStatus = clientCrypto.decryptSinglePacket(playStatusPayload);
        assertEquals(BedrockPacketIds.BEDROCK_PLAY_STATUS, BedrockRakNetCodec.readUnsignedVarInt(playStatus));
        playStatus.release();
        playStatusPayload.release();

        ByteBuf packsPayload = readConnectedPayload(resourcePacksDatagram);
        ByteBuf resourcePacks = clientCrypto.decryptSinglePacket(packsPayload);
        assertEquals(BedrockPacketIds.BEDROCK_RESOURCE_PACKS_INFO, BedrockRakNetCodec.readUnsignedVarInt(resourcePacks));
        resourcePacks.release();
        packsPayload.release();

        handshakePacket.release();
        encryptedHandshake.release();
    }

    private ByteBuf readConnectedPayload(DatagramPacket datagram) {
        ByteBuf in = datagram.content().copy();
        in.readByte();
        BedrockRakNetCodec.readLittleTriad(in);
        int header = in.readUnsignedByte();
        int reliability = (header & 0xe0) >> 5;
        int length = in.readUnsignedShort() >> 3;
        if (reliability == BedrockDatagramTransport.RELIABILITY_RELIABLE_ORDERED) {
            BedrockRakNetCodec.readLittleTriad(in);
            BedrockRakNetCodec.readLittleTriad(in);
            in.readUnsignedByte();
        }
        return in.readSlice(length);
    }
}
