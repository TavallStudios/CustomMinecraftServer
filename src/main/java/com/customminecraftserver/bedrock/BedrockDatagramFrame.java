package com.customminecraftserver.bedrock;

import io.netty.buffer.ByteBuf;

record BedrockDatagramFrame(boolean split, int splitCount, int splitId, int splitIndex, ByteBuf payload) {
}
