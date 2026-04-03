package com.customminecraftserver.javaedition;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public final class MinecraftStringCodec {
    private MinecraftStringCodec() {
    }

    public static String read(ByteBuf in, int maxCharacters) {
        int length = MinecraftVarInt.read(in);
        if (length < 0 || length > maxCharacters * 4) {
            throw new IllegalArgumentException("Invalid Minecraft string length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (value.length() > maxCharacters) {
            throw new IllegalArgumentException("Minecraft string exceeds character limit: " + value.length());
        }
        return value;
    }

    public static void write(ByteBuf out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        MinecraftVarInt.write(out, bytes.length);
        out.writeBytes(bytes);
    }
}
