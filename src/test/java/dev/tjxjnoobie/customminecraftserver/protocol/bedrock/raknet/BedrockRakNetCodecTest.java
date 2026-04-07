package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockRakNetCodecTest {
    @Test
    void varIntAndZigZagRoundtrip() {
        TestLogSupport.logTestStart("BedrockRakNetCodecTest.varIntAndZigZagRoundtrip");
        ByteBuf buffer = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(buffer, 321);
        BedrockRakNetCodec.writeUnsignedVarLong(buffer, 5000L);
        BedrockRakNetCodec.writeZigZag32(buffer, -12);
        BedrockRakNetCodec.writeZigZag64(buffer, -99L);

        assertEquals(321, BedrockRakNetCodec.readUnsignedVarInt(buffer));
        assertEquals(5000L, BedrockRakNetCodec.readUnsignedVarLong(buffer));
        assertEquals(-12, BedrockRakNetCodec.decodeZigZag32(BedrockRakNetCodec.readUnsignedVarInt(buffer)));
        assertEquals(-99L, BedrockRakNetCodec.decodeZigZag64(BedrockRakNetCodec.readUnsignedVarLong(buffer)));
        buffer.release();
    }

    @Test
    void writesAndReadsAddressesAndBatchPackets() {
        TestLogSupport.logTestStart("BedrockRakNetCodecTest.writesAndReadsAddressesAndBatchPackets");
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 19132);
        ByteBuf buffer = Unpooled.buffer();
        BedrockRakNetCodec.writeAddress(buffer, address);
        InetSocketAddress decoded = BedrockRakNetCodec.readAddress(buffer);
        assertEquals(address.getHostString(), decoded.getHostString());
        assertEquals(address.getPort(), decoded.getPort());

        ByteBuf packet = Unpooled.buffer();
        BedrockRakNetCodec.writeUnsignedVarInt(packet, BedrockPacketIds.BEDROCK_CLIENT_CACHE_STATUS);
        packet.writeBoolean(true);

        ByteBuf batch = Unpooled.buffer();
        batch.writeByte(BedrockPacketIds.BEDROCK_BATCH);
        BedrockRakNetCodec.writeUnsignedVarInt(batch, packet.readableBytes());
        batch.writeBytes(packet, packet.readerIndex(), packet.readableBytes());

        ByteBuf batchCopy = batch.copy();
        List<ByteBuf> packets = BedrockRakNetCodec.readBatch(batchCopy, false);
        ByteBuf firstPacket = packets.get(0);
        assertEquals(BedrockPacketIds.BEDROCK_CLIENT_CACHE_STATUS, BedrockRakNetCodec.readUnsignedVarInt(firstPacket));
        firstPacket.release();
        batchCopy.release();

        buffer.release();
        packet.release();
        batch.release();
    }
}
