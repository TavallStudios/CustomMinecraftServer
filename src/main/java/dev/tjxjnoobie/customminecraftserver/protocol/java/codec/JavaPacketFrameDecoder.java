package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class JavaPacketFrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        Integer frameLength = MinecraftVarInt.tryRead(in);
        if (frameLength == null) {
            return;
        }
        if (in.readableBytes() < frameLength) {
            in.resetReaderIndex();
            return;
        }
        out.add(in.readRetainedSlice(frameLength));
    }
}

