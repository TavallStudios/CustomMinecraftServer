package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

public final class JavaCipherDecoder extends ByteToMessageDecoder {
    private final Cipher cipher;

    public JavaCipherDecoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext context, io.netty.buffer.ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable()) {
            return;
        }
        byte[] encrypted = new byte[in.readableBytes()];
        in.readBytes(encrypted);
        byte[] decrypted = cipher.update(encrypted);
        if (decrypted != null && decrypted.length > 0) {
            out.add(Unpooled.wrappedBuffer(decrypted));
        }
    }
}

