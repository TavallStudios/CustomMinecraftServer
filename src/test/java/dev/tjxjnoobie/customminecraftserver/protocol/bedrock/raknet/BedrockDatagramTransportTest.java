package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session.BedrockPeerSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BedrockDatagramTransportTest {
    @Test
    void wrapBatchPrefixesPacketId() {
        TestLogSupport.logTestStart("BedrockDatagramTransportTest.wrapBatchPrefixesPacketId");
        BedrockDatagramTransport transport = new BedrockDatagramTransport();
        ByteBuf payload = Unpooled.buffer();
        payload.writeByte(0x7a);

        ByteBuf wrapped = transport.wrapBatch(payload, false);
        try {
            int packetId = wrapped.readUnsignedByte();
            assertEquals(BedrockPacketIds.BEDROCK_BATCH, packetId);
        } finally {
            payload.release();
            wrapped.release();
        }
    }

    @Test
    void readFrameReturnsPayloadForUnsplitPacket() {
        TestLogSupport.logTestStart("BedrockDatagramTransportTest.readFrameReturnsPayloadForUnsplitPacket");
        BedrockDatagramTransport transport = new BedrockDatagramTransport();
        ByteBuf in = Unpooled.buffer();
        ByteBuf payload = Unpooled.buffer();
        payload.writeByte(0x42);

        in.writeByte(0); // reliability=0, split=false
        in.writeShort(payload.readableBytes() << 3);
        in.writeBytes(payload, payload.readerIndex(), payload.readableBytes());

        BedrockDatagramFrame frame = transport.readFrame(in);
        BedrockPeerSession peerSession = new BedrockPeerSession(new java.net.InetSocketAddress("127.0.0.1", 19132));
        ByteBuf reassembled = transport.reassemble(peerSession, frame);
        assertSame(frame.payload(), reassembled);

        payload.release();
        frame.payload().release();
        in.release();
    }
}
