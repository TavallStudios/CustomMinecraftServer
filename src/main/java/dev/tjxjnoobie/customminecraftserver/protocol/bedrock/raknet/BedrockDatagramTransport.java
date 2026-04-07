package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet.BedrockPacketIds;
import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.session.BedrockPeerSession;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class BedrockDatagramTransport {
    public static final int RELIABILITY_UNRELIABLE = 0;
    public static final int RELIABILITY_RELIABLE_ORDERED = 3;

    public void sendAck(ChannelHandlerContext context, InetSocketAddress recipient, int sequenceNumber) {
        ByteBuf ack = Unpooled.buffer();
        ack.writeByte(BedrockPacketIds.RAKNET_ACK);
        ack.writeShort(1);
        ack.writeByte(1);
        BedrockRakNetCodec.writeLittleTriad(ack, sequenceNumber);
        context.writeAndFlush(new DatagramPacket(ack, recipient));
    }

    public void sendFrame(ChannelHandlerContext context, BedrockPeerSession peerSession, ByteBuf payload, int reliability) {
        ByteBuf datagram = Unpooled.buffer();
        datagram.writeByte(BedrockPacketIds.RAKNET_CONNECTED_DATAGRAM_MIN);
        BedrockRakNetCodec.writeLittleTriad(datagram, peerSession.nextSequenceNumber());

        datagram.writeByte(reliability << 5);
        datagram.writeShort(payload.readableBytes() << 3);
        if (reliability == RELIABILITY_RELIABLE_ORDERED) {
            BedrockRakNetCodec.writeLittleTriad(datagram, peerSession.nextMessageIndex());
            BedrockRakNetCodec.writeLittleTriad(datagram, peerSession.nextOrderIndex());
            datagram.writeByte(0);
        }
        datagram.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        context.writeAndFlush(new DatagramPacket(datagram, peerSession.remoteAddress()));
    }

    public ByteBuf wrapBatch(ByteBuf packet, boolean includeCompressorHeader) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(BedrockPacketIds.BEDROCK_BATCH);
        if (includeCompressorHeader) {
            out.writeByte(0xff);
        }
        BedrockRakNetCodec.writeUnsignedVarInt(out, packet.readableBytes());
        out.writeBytes(packet, packet.readerIndex(), packet.readableBytes());
        return out;
    }

    public BedrockDatagramFrame readFrame(ByteBuf in) {
        int header = in.readUnsignedByte();
        int reliability = (header & 0xe0) >> 5;
        boolean split = (header & 0x10) != 0;
        int length = in.readUnsignedShort() >> 3;

        readMessageIndex(in, reliability);
        readSequenceIndex(in, reliability);
        readOrderIndex(in, reliability);
        readOrderChannel(in, reliability);

        int splitCount = -1;
        int splitId = -1;
        int splitIndex = -1;
        if (split) {
            splitCount = in.readInt();
            splitId = in.readUnsignedShort();
            splitIndex = in.readInt();
        }

        ByteBuf payload = in.readRetainedSlice(length);
        return new BedrockDatagramFrame(split, splitCount, splitId, splitIndex, payload);
    }

    public ByteBuf reassemble(BedrockPeerSession peerSession, BedrockDatagramFrame frame) {
        if (!frame.split()) {
            return frame.payload();
        }

        Map<Integer, byte[]> parts = peerSession.splitPackets().computeIfAbsent(frame.splitId(), ignored -> new HashMap<>());
        parts.put(frame.splitIndex(), ByteBufUtil.getBytes(frame.payload()));
        frame.payload().release();
        if (parts.size() < frame.splitCount()) {
            return null;
        }

        ByteBuf combined = Unpooled.buffer();
        for (int index = 0; index < frame.splitCount(); index++) {
            combined.writeBytes(parts.get(index));
        }
        peerSession.splitPackets().remove(frame.splitId());
        return combined;
    }

    private int readMessageIndex(ByteBuf in, int reliability) {
        return switch (reliability) {
            case 2, 3, 4, 6, 7 -> BedrockRakNetCodec.readLittleTriad(in);
            default -> -1;
        };
    }

    private int readSequenceIndex(ByteBuf in, int reliability) {
        return switch (reliability) {
            case 1, 4 -> BedrockRakNetCodec.readLittleTriad(in);
            default -> -1;
        };
    }

    private int readOrderIndex(ByteBuf in, int reliability) {
        return switch (reliability) {
            case 1, 3, 4 -> BedrockRakNetCodec.readLittleTriad(in);
            default -> -1;
        };
    }

    private int readOrderChannel(ByteBuf in, int reliability) {
        return switch (reliability) {
            case 1, 3, 4 -> in.readUnsignedByte();
            default -> -1;
        };
    }
}


