package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

public final class JavaCipherEncoder extends MessageToByteEncoder<ByteBuf> {
    private final Cipher cipher;

    public JavaCipherEncoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext context, ByteBuf message, ByteBuf out) throws Exception {
        byte[] plain = new byte[message.readableBytes()];
        message.getBytes(message.readerIndex(), plain);
        byte[] encrypted = cipher.update(plain);
        if (encrypted != null && encrypted.length > 0) {
            out.writeBytes(encrypted);
        }
    }
}

