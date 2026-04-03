package com.customminecraftserver.javaedition;

import io.netty.buffer.ByteBuf;

public record JavaEncryptionResponsePacket(byte[] sharedSecret, byte[] verifyToken) {
    public static JavaEncryptionResponsePacket read(ByteBuf in) {
        return new JavaEncryptionResponsePacket(
                readByteArray(in),
                readByteArray(in)
        );
    }

    private static byte[] readByteArray(ByteBuf in) {
        int length = MinecraftVarInt.read(in);
        byte[] value = new byte[length];
        in.readBytes(value);
        return value;
    }
}
