package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.network.ProtocolVersionDetector;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.auth.BedrockLoginAdmission;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.login.BedrockLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockDatagramTransport;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockGameSessionCoordinatorTest {
    private EmbeddedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void requestNetworkSettingsSendsNetworkSettingsResponse() {
        TestLogSupport.logTestStart("BedrockGameSessionCoordinatorTest.requestNetworkSettingsSendsNetworkSettingsResponse");
        BedrockDatagramTransport transport = new BedrockDatagramTransport();
        BedrockLoginAdmission offline = (session, payload) -> null;
        BedrockLoginAdmission online = (session, payload) -> null;
        BedrockLoginCoordinator loginCoordinator = new BedrockLoginCoordinator(offline, online);
        BedrockGameSessionCoordinator coordinator = new BedrockGameSessionCoordinator(
                new StructuredConnectionLogger(),
                new ProtocolVersionDetector(),
                loginCoordinator,
                transport
        );

        channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();

        ConnectionSession session = new ConnectionSession("127.0.0.1:19132", ConnectionEdition.BEDROCK, AuthMode.OFFLINE);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 19132);
        BedrockPeerSession peerSession = new BedrockPeerSession(address);

        ByteBuf packet = io.netty.buffer.Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_REQUEST_NETWORK_SETTINGS);
        packet.writeInt(898);

        ByteBuf batch = io.netty.buffer.Unpooled.buffer();
        batch.writeByte(BedrockPacketIds.BEDROCK_BATCH);
        BedrockRakNetCodec.writeUnsignedVarInt(batch, packet.readableBytes());
        batch.writeBytes(packet, packet.readerIndex(), packet.readableBytes());

        coordinator.handleBatchPayload(context, session, peerSession, batch);

        DatagramPacket outbound = channel.readOutbound();
        assertNotNull(outbound);
        ByteBuf payload = readConnectedPayload(outbound);
        ByteBuf payloadCopy = payload.copy();
        List<ByteBuf> packets = BedrockRakNetCodec.readBatch(payloadCopy, false);
        ByteBuf firstPacket = packets.get(0);
        assertEquals(BedrockPacketIds.BEDROCK_NETWORK_SETTINGS, BedrockRakNetCodec.readUnsignedVarInt(firstPacket));
        firstPacket.release();
        payloadCopy.release();
        payload.release();

        assertTrue(peerSession.compressionNegotiated());
        packet.release();
        batch.release();
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
