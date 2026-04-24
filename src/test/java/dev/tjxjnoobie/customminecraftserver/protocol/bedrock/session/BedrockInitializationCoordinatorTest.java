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

class BedrockInitializationCoordinatorTest {
    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void sendsDisconnectWhenRuntimeEntityIdIsUnexpected() throws Exception {
        TestLogSupport.logTestStart("BedrockInitializationCoordinatorTest.sendsDisconnectWhenRuntimeEntityIdIsUnexpected");
        BedrockDatagramTransport transport = new BedrockDatagramTransport();
        BedrockInitializationCoordinator coordinator = new BedrockInitializationCoordinator(
                new StructuredConnectionLogger(),
                transport
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
        peerSession.terminalMessage("Bedrock disconnect");
        peerSession.waitingForLocalPlayerInitialization(true);

        BedrockClientSessionCrypto clientCrypto = new BedrockClientSessionCrypto(
                clientKey.getPrivate(),
                secureSession.serverHandshakeToken()
        );

        ByteBuf packet = io.netty.buffer.Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarLong(packet, 99L);

        coordinator.handleSetLocalPlayerAsInitialized(context, session, peerSession, packet);

        DatagramPacket disconnectDatagram = channel.readOutbound();
        assertNotNull(disconnectDatagram);

        ByteBuf encryptedPayload = readConnectedPayload(disconnectDatagram);
        ByteBuf decrypted = clientCrypto.decryptSinglePacket(encryptedPayload);
        int packetId = BedrockRakNetCodec.readUnsignedVarInt(decrypted);
        assertEquals(BedrockPacketIds.BEDROCK_DISCONNECT, packetId);
        decrypted.release();
        encryptedPayload.release();
        packet.release();
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
