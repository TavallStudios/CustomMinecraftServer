package com.customminecraftserver.javaedition;

import io.netty.buffer.ByteBuf;

public record JavaHandshakePacket(int protocolVersion, String serverAddress, int port, JavaNextState nextState) {
    public static JavaHandshakePacket read(ByteBuf in) {
        int protocolVersion = MinecraftVarInt.read(in);
        String serverAddress = MinecraftStringCodec.read(in, 255);
        int port = in.readUnsignedShort();
        JavaNextState nextState = JavaNextState.fromId(MinecraftVarInt.read(in));
        return new JavaHandshakePacket(protocolVersion, serverAddress, port, nextState);
    }
}
