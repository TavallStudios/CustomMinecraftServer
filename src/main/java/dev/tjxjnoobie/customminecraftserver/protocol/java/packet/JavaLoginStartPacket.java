package dev.tjxjnoobie.customminecraftserver.protocol.java.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.MinecraftStringCodec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.UUID;

public record JavaLoginStartPacket(String username, UUID playerUuid, String trailingBytesHex) {
    public static JavaLoginStartPacket read(ByteBuf in) {
        String username = MinecraftStringCodec.read(in, 16);
        UUID uuid = null;
        if (in.readableBytes() >= 16) {
            in.markReaderIndex();
            try {
                uuid = new UUID(in.readLong(), in.readLong());
            } catch (IndexOutOfBoundsException ignored) {
                in.resetReaderIndex();
            }
        }
        String trailingBytesHex = in.isReadable()
                ? ByteBufUtil.hexDump(in, in.readerIndex(), in.readableBytes())
                : "";
        return new JavaLoginStartPacket(username, uuid, trailingBytesHex);
    }
}


