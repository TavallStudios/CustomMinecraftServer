package dev.tjxjnoobie.customminecraftserver.protocol.java.codec;

import io.netty.buffer.ByteBuf;

public final class MinecraftVarInt {
    private MinecraftVarInt() {
    }

    public static int read(ByteBuf in) {
        int value = 0;
        int position = 0;
        byte currentByte;
        do {
            if (!in.isReadable()) {
                throw new IllegalStateException("Incomplete VarInt");
            }
            currentByte = in.readByte();
            value |= (currentByte & 0x7f) << position;
            position += 7;
            if (position > 35) {
                throw new IllegalArgumentException("VarInt is too large");
            }
        } while ((currentByte & 0x80) == 0x80);
        return value;
    }

    public static Integer tryRead(ByteBuf in) {
        in.markReaderIndex();
        try {
            return read(in);
        } catch (IllegalStateException ignored) {
            in.resetReaderIndex();
            return null;
        }
    }

    public static void write(ByteBuf out, int value) {
        int remaining = value;
        do {
            byte temp = (byte) (remaining & 0x7f);
            remaining >>>= 7;
            if (remaining != 0) {
                temp |= (byte) 0x80;
            }
            out.writeByte(temp);
        } while (remaining != 0);
    }
}

