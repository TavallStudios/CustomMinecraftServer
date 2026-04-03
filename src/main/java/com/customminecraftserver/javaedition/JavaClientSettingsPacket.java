package com.customminecraftserver.javaedition;

import io.netty.buffer.ByteBuf;

public record JavaClientSettingsPacket(
        String locale,
        byte viewDistance,
        int chatFlags,
        boolean chatColors,
        short skinParts,
        int mainHand,
        boolean enableTextFiltering,
        boolean enableServerListing,
        int particleStatus
) {
    public static JavaClientSettingsPacket read(ByteBuf frame) {
        return new JavaClientSettingsPacket(
                MinecraftStringCodec.read(frame, 32),
                frame.readByte(),
                MinecraftVarInt.read(frame),
                frame.readBoolean(),
                (short) frame.readUnsignedByte(),
                MinecraftVarInt.read(frame),
                frame.readBoolean(),
                frame.readBoolean(),
                MinecraftVarInt.read(frame)
        );
    }
}
