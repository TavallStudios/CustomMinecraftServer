package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet;

import io.netty.buffer.ByteBuf;

record BedrockDatagramFrame(boolean split, int splitCount, int splitId, int splitIndex, ByteBuf payload) {
}

