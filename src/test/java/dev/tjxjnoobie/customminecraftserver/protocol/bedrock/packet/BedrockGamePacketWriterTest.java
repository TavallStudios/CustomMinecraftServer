package dev.tjxjnoobie.customminecraftserver.protocol.bedrock.packet;

import dev.tjxjnoobie.customminecraftserver.protocol.bedrock.raknet.BedrockRakNetCodec;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockGamePacketWriterTest {
    @Test
    void networkSettingsPacketHasExpectedId() {
        TestLogSupport.logTestStart("BedrockGamePacketWriterTest.networkSettingsPacketHasExpectedId");
        ByteBuf packet = BedrockGamePacketWriter.networkSettings(ByteBufAllocator.DEFAULT);
        try {
            int packetId = BedrockRakNetCodec.readUnsignedVarInt(packet);
            assertEquals(BedrockPacketIds.BEDROCK_NETWORK_SETTINGS, packetId);
        } finally {
            packet.release();
        }
    }

    @Test
    void playStatusPacketEncodesStatus() {
        TestLogSupport.logTestStart("BedrockGamePacketWriterTest.playStatusPacketEncodesStatus");
        ByteBuf packet = BedrockGamePacketWriter.playStatusLoginSuccess(ByteBufAllocator.DEFAULT);
        try {
            int packetId = BedrockRakNetCodec.readUnsignedVarInt(packet);
            int status = packet.readIntLE();
            assertEquals(BedrockPacketIds.BEDROCK_PLAY_STATUS, packetId);
            assertEquals(0, status);
        } finally {
            packet.release();
        }
    }

    @Test
    void startGameUsesTemplateBytes() {
        TestLogSupport.logTestStart("BedrockGamePacketWriterTest.startGameUsesTemplateBytes");
        ByteBuf packet = BedrockGamePacketWriter.startGame(ByteBufAllocator.DEFAULT, 944);
        try {
            assertTrue(packet.readableBytes() > 0);
        } finally {
            packet.release();
        }
    }
}
